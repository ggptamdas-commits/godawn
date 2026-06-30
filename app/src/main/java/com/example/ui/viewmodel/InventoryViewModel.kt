package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.Transaction
import com.example.data.repository.InventoryRepository
import com.example.data.sync.FirebaseSyncHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CartItemWithProduct(
    val cartItem: CartItem,
    val product: Product
)

sealed interface SyncState {
    object Idle : SyncState
    object Loading : SyncState
    data class Success(val message: String) : SyncState
    data class Error(val message: String) : SyncState
}

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: InventoryRepository
    private val firebaseSyncHelper: FirebaseSyncHelper

    init {
        val db = AppDatabase.getDatabase(application)
        repository = InventoryRepository(db.productDao(), db.transactionDao(), db.cartDao())
        firebaseSyncHelper = FirebaseSyncHelper(application)
        firebaseSyncHelper.initializeFirebase() // auto initialize on start
    }

    // Exposed States
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dynamic Cart mapping with full product information
    val cartItemsWithProducts: StateFlow<List<CartItemWithProduct>> = repository.allCartItems
        .combine(repository.allProducts) { cartItems, products ->
            cartItems.mapNotNull { item ->
                val product = products.find { it.id == item.productId }
                if (product != null) {
                    CartItemWithProduct(item, product)
                } else {
                    null
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtering states
    private val _startDate = MutableStateFlow("")
    val startDate: StateFlow<String> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow("")
    val endDate: StateFlow<String> = _endDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        repository.allTransactions,
        _startDate,
        _endDate
    ) { transactions, start, end ->
        transactions.filter { tx ->
            val date = tx.date
            val matchesStart = start.isBlank() || date >= start
            val matchesEnd = end.isBlank() || date <= end
            matchesStart && matchesEnd
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Cloud Sync State
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Config parameters
    val firebaseDbUrl: String get() = firebaseSyncHelper.getDbUrl()
    val firebaseProjectId: String get() = firebaseSyncHelper.getProjectId()
    val firebaseApiKey: String get() = firebaseSyncHelper.getApiKey()
    val deviceName: String get() = firebaseSyncHelper.getDeviceName()
    val isCloudConfigured: Boolean get() = firebaseSyncHelper.isConfigured()

    // Setup helper methods
    fun setDateRange(start: String, end: String) {
        _startDate.value = start
        _endDate.value = end
    }

    fun clearDateFilter() {
        _startDate.value = ""
        _endDate.value = ""
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Product CRUD Operations
    fun addProduct(nameBn: String, nameEn: String, category: String, initialStock: Double, unit: String, minAlert: Double) {
        viewModelScope.launch {
            val product = Product(
                nameEn = nameEn.trim(),
                nameBn = nameBn.trim(),
                category = category.trim(),
                currentStock = initialStock,
                unit = unit.trim(),
                minStockAlert = minAlert
            )
            repository.insertProduct(product)
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(lastUpdated = System.currentTimeMillis()))
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // Stock In
    fun stockIn(productId: Int, quantity: Double, notes: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.stockIn(productId, quantity, notes.trim())
            onResult(result)
        }
    }

    // Cart operations
    fun addToCart(productId: Int, quantity: Double, destination: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.addToCart(productId, quantity, destination)
            onResult(result)
        }
    }

    fun updateCartItemQty(id: Int, quantity: Double) {
        viewModelScope.launch {
            repository.updateCartItemQuantity(id, quantity)
        }
    }

    fun deleteCartItem(item: CartItem) {
        viewModelScope.launch {
            repository.deleteCartItem(item)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
        }
    }

    fun checkoutCart(notes: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (success, message) = repository.checkoutCart(notes.trim())
            onResult(success, message)
        }
    }

    // Firebase Backup / Sync
    fun saveCloudConfig(dbUrl: String, projectId: String, apiKey: String, deviceName: String) {
        firebaseSyncHelper.saveFirebaseConfig(dbUrl, projectId, apiKey, deviceName)
    }

    fun backupToCloud() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            val (success, msg) = firebaseSyncHelper.backupData()
            if (success) {
                _syncState.value = SyncState.Success(msg)
            } else {
                _syncState.value = SyncState.Error(msg)
            }
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            val (success, msg) = firebaseSyncHelper.restoreData()
            if (success) {
                _syncState.value = SyncState.Success(msg)
            } else {
                _syncState.value = SyncState.Error(msg)
            }
        }
    }

    fun clearSyncState() {
        _syncState.value = SyncState.Idle
    }
}
