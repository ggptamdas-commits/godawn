package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.Product
import com.example.data.model.Transaction
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class FirebaseSyncHelper(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("resto_sync_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "FirebaseSyncHelper"
    }

    // Save and retrieve Firebase Settings
    fun saveFirebaseConfig(dbUrl: String, projectId: String, apiKey: String, deviceName: String) {
        prefs.edit()
            .putString("db_url", dbUrl.trim())
            .putString("project_id", projectId.trim())
            .putString("api_key", apiKey.trim())
            .putString("device_name", deviceName.trim())
            .apply()
        
        // Re-initialize after configuration change
        initializeFirebase()
    }

    fun getDbUrl(): String = prefs.getString("db_url", "") ?: ""
    fun getProjectId(): String = prefs.getString("project_id", "") ?: ""
    fun getApiKey(): String = prefs.getString("api_key", "") ?: ""
    fun getDeviceName(): String {
        val savedName = prefs.getString("device_name", "") ?: ""
        return if (savedName.isNotBlank()) savedName else "Device-${android.os.Build.MODEL}"
    }

    fun isConfigured(): Boolean {
        return getDbUrl().isNotBlank() && getProjectId().isNotBlank()
    }

    // Initialize programmatically if configured
    fun initializeFirebase(): Boolean {
        try {
            val dbUrl = getDbUrl()
            val projectId = getProjectId()
            val apiKey = getApiKey()

            if (dbUrl.isBlank() || projectId.isBlank()) {
                Log.w(TAG, "Firebase not initialized: Configuration is incomplete")
                return false
            }

            // Check if already initialized to avoid duplicate exception
            val apps = FirebaseApp.getApps(context)
            for (app in apps) {
                if (app.name == FirebaseApp.DEFAULT_APP_NAME) {
                    return true
                }
            }

            val builder = FirebaseOptions.Builder()
                .setProjectId(projectId)
                .setDatabaseUrl(dbUrl)
                
            if (apiKey.isNotBlank()) {
                builder.setApiKey(apiKey)
                builder.setApplicationId("1:$projectId:android:resto_godown")
            } else {
                builder.setApiKey("AIzaSyFakeKeyJustToInitializeApplet")
                builder.setApplicationId("1:$projectId:android:resto_godown")
            }

            FirebaseApp.initializeApp(context, builder.build())
            Log.d(TAG, "Firebase programmatically initialized successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
            return false
        }
    }

    private fun getDatabase(): FirebaseDatabase? {
        return if (initializeFirebase()) {
            FirebaseDatabase.getInstance()
        } else {
            null
        }
    }

    private suspend fun signInAnonymouslyIfNeeded(): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            if (!initializeFirebase()) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser != null) {
                continuation.resume(true)
                return@suspendCancellableCoroutine
            }
            auth.signInAnonymously()
                .addOnSuccessListener {
                    Log.d(TAG, "Signed in anonymously")
                    if (continuation.isActive) continuation.resume(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Anonymous auth failed: ${e.message}")
                    // Resume true anyway to try direct DB access if security rules are open
                    if (continuation.isActive) continuation.resume(true)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in auth: ${e.message}")
            if (continuation.isActive) continuation.resume(true)
        }
    }

    // Backup Local Data to Firebase
    suspend fun backupData(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Pair(false, "দয়া করে প্রথমে ক্লাউড সিঙ্ক সেটিংস কনফিগার করুন। (Please configure cloud sync settings first.)")
        }

        try {
            signInAnonymouslyIfNeeded()
            val db = getDatabase() ?: return@withContext Pair(false, "ফায়ারবেইজ ডাটাবেজ সংযোগ ব্যর্থ হয়েছে। (Firebase connection failed.)")
            
            val deviceId = getDeviceName().replace(".", "_")
                .replace("#", "_")
                .replace("$", "_")
                .replace("[", "_")
                .replace("]", "_")
                .replace("/", "_")
            
            val backupRef = db.getReference("resto_godown_backup").child(deviceId)

            val localDb = AppDatabase.getDatabase(context)
            val products = localDb.productDao().getAllProducts().first()
            val transactions = localDb.transactionDao().getAllTransactions().first()

            // Prepare products map for Room values
            val productsMap = products.associate { it.uuid to mapOf(
                "uuid" to it.uuid,
                "nameEn" to it.nameEn,
                "nameBn" to it.nameBn,
                "category" to it.category,
                "currentStock" to it.currentStock,
                "unit" to it.unit,
                "minStockAlert" to it.minStockAlert,
                "lastUpdated" to it.lastUpdated
            )}

            // Prepare transactions map
            val transactionsMap = transactions.associate { it.uuid to mapOf(
                "uuid" to it.uuid,
                "productId" to it.productId,
                "productUuid" to it.productUuid,
                "productNameEn" to it.productNameEn,
                "productNameBn" to it.productNameBn,
                "quantity" to it.quantity,
                "type" to it.type,
                "date" to it.date,
                "timestamp" to it.timestamp,
                "notes" to it.notes
            )}

            // Write products and transactions
            val productsSuccess = suspendCancellableCoroutine<Boolean> { cont ->
                backupRef.child("products").setValue(productsMap)
                    .addOnSuccessListener { if (cont.isActive) cont.resume(true) }
                    .addOnFailureListener { e -> 
                        Log.e(TAG, "Failed to write products: ${e.message}")
                        if (cont.isActive) cont.resume(false) 
                    }
            }

            if (!productsSuccess) {
                return@withContext Pair(false, "পণ্য ব্যাকআপ করতে ব্যর্থ হয়েছে। ফায়ারবেইজ রুলস চেক করুন। (Failed to backup products. Check rules.)")
            }

            val transactionsSuccess = suspendCancellableCoroutine<Boolean> { cont ->
                backupRef.child("transactions").setValue(transactionsMap)
                    .addOnSuccessListener { if (cont.isActive) cont.resume(true) }
                    .addOnFailureListener { e -> 
                        Log.e(TAG, "Failed to write transactions: ${e.message}")
                        if (cont.isActive) cont.resume(false) 
                    }
            }

            if (!transactionsSuccess) {
                return@withContext Pair(false, "লেনদেন ব্যাকআপ করতে ব্যর্থ হয়েছে। (Failed to backup transactions.)")
            }

            // Mark local transactions as synced
            for (tx in transactions) {
                if (!tx.isSynced) {
                    localDb.transactionDao().markAsSynced(tx.id)
                }
            }

            Pair(true, "সাফল্যের সাথে '$deviceId' ডিভাইস নামে ক্লাউডে ব্যাকআপ নেওয়া হয়েছে! (Successfully backed up in Cloud!)")
        } catch (e: Exception) {
            Log.e(TAG, "Backup exception: ${e.message}", e)
            Pair(false, "ব্যাকআপ ব্যতিক্রম: ${e.message}")
        }
    }

    // Restore Data from Firebase Backup
    suspend fun restoreData(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Pair(false, "দয়া করে প্রথমে ক্লাউড সিঙ্ক সেটিংস কনফিগার করুন। (Please configure cloud sync settings first.)")
        }

        try {
            signInAnonymouslyIfNeeded()
            val db = getDatabase() ?: return@withContext Pair(false, "ফায়ারবেইজ ডাটাবেজ সংযোগ ব্যর্থ হয়েছে। (Firebase connection failed.)")
            
            val deviceId = getDeviceName().replace(".", "_")
                .replace("#", "_")
                .replace("$", "_")
                .replace("[", "_")
                .replace("]", "_")
                .replace("/", "_")
            
            val backupRef = db.getReference("resto_godown_backup").child(deviceId)

            // Read products from Cloud
            val cloudProductsSnapshot = suspendCancellableCoroutine<DataSnapshot?> { cont ->
                backupRef.child("products").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (cont.isActive) cont.resume(snapshot)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Failed to read products from Firebase: ${error.message}")
                        if (cont.isActive) cont.resume(null)
                    }
                })
            }

            // Read transactions from Cloud
            val cloudTransactionsSnapshot = suspendCancellableCoroutine<DataSnapshot?> { cont ->
                backupRef.child("transactions").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (cont.isActive) cont.resume(snapshot)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Failed to read transactions from Firebase: ${error.message}")
                        if (cont.isActive) cont.resume(null)
                    }
                })
            }

            if (cloudProductsSnapshot == null) {
                return@withContext Pair(false, "ক্লাউড থেকে ডাটা পড়া সম্ভব হয়নি অথবা ডাটা খালি। (Failed to fetch cloud data.)")
            }

            val localDb = AppDatabase.getDatabase(context)
            val productDao = localDb.productDao()
            val transactionDao = localDb.transactionDao()

            var productsMerged = 0
            var transactionsMerged = 0

            // Merge Products
            for (prodSnapshot in cloudProductsSnapshot.children) {
                try {
                    val uuid = prodSnapshot.child("uuid").value as? String ?: continue
                    val nameEn = prodSnapshot.child("nameEn").value as? String ?: ""
                    val nameBn = prodSnapshot.child("nameBn").value as? String ?: ""
                    val category = prodSnapshot.child("category").value as? String ?: "General"
                    val currentStock = (prodSnapshot.child("currentStock").value as? Number)?.toDouble() ?: 0.0
                    val unit = prodSnapshot.child("unit").value as? String ?: "Kg"
                    val minStockAlert = (prodSnapshot.child("minStockAlert").value as? Number)?.toDouble() ?: 5.0
                    val lastUpdated = (prodSnapshot.child("lastUpdated").value as? Number)?.toLong() ?: System.currentTimeMillis()

                    val existingProduct = productDao.getProductByUuid(uuid)
                    if (existingProduct != null) {
                        // If cloud is newer or local is older, update
                        if (lastUpdated > existingProduct.lastUpdated) {
                            val updatedProduct = existingProduct.copy(
                                nameEn = nameEn,
                                nameBn = nameBn,
                                category = category,
                                currentStock = currentStock,
                                unit = unit,
                                minStockAlert = minStockAlert,
                                lastUpdated = lastUpdated
                            )
                            productDao.updateProduct(updatedProduct)
                            productsMerged++
                        }
                    } else {
                        // New product from cloud
                        val newProduct = Product(
                            uuid = uuid,
                            nameEn = nameEn,
                            nameBn = nameBn,
                            category = category,
                            currentStock = currentStock,
                            unit = unit,
                            minStockAlert = minStockAlert,
                            lastUpdated = lastUpdated
                        )
                        productDao.insertProduct(newProduct)
                        productsMerged++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error merging product: ${e.message}")
                }
            }

            // Merge Transactions
            if (cloudTransactionsSnapshot != null) {
                for (txSnapshot in cloudTransactionsSnapshot.children) {
                    try {
                        val uuid = txSnapshot.child("uuid").value as? String ?: continue
                        val productIdVal = (txSnapshot.child("productId").value as? Number)?.toInt() ?: 0
                        val productUuid = txSnapshot.child("productUuid").value as? String ?: ""
                        val productNameEn = txSnapshot.child("productNameEn").value as? String ?: ""
                        val productNameBn = txSnapshot.child("productNameBn").value as? String ?: ""
                        val quantity = (txSnapshot.child("quantity").value as? Number)?.toDouble() ?: 0.0
                        val type = txSnapshot.child("type").value as? String ?: "STOCK_IN"
                        val date = txSnapshot.child("date").value as? String ?: ""
                        val timestamp = (txSnapshot.child("timestamp").value as? Number)?.toLong() ?: System.currentTimeMillis()
                        val notes = txSnapshot.child("notes").value as? String ?: ""

                        // Look up local product by cloud productUuid to link correct local productId
                        val linkedLocalProduct = productDao.getProductByUuid(productUuid)
                        val finalProductId = linkedLocalProduct?.id ?: productIdVal

                        val localTransactions = transactionDao.getAllTransactions().first()
                        val transactionExists = localTransactions.any { it.uuid == uuid }

                        if (!transactionExists) {
                            val newTx = Transaction(
                                uuid = uuid,
                                productId = finalProductId,
                                productUuid = productUuid,
                                productNameEn = productNameEn,
                                productNameBn = productNameBn,
                                quantity = quantity,
                                type = type,
                                date = date,
                                timestamp = timestamp,
                                notes = notes,
                                isSynced = true
                            )
                            transactionDao.insertTransaction(newTx)
                            transactionsMerged++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error merging transaction: ${e.message}")
                    }
                }
            }

            Pair(true, "রিস্টোর সফল হয়েছে! $productsMerged টি পণ্য এবং $transactionsMerged টি লেনদেন সিঙ্ক/আপডেট করা হয়েছে। (Restore successful!)")
        } catch (e: Exception) {
            Log.e(TAG, "Restore exception: ${e.message}", e)
            Pair(false, "রিস্টোর ব্যর্থ: ${e.message}")
        }
    }
}
