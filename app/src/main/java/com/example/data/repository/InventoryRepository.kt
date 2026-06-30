package com.example.data.repository

import com.example.data.local.CartDao
import com.example.data.local.ProductDao
import com.example.data.local.TransactionDao
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InventoryRepository(
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao,
    private val cartDao: CartDao
) {
    // Products
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    suspend fun getProductById(id: Int): Product? = productDao.getProductById(id)

    suspend fun insertProduct(product: Product): Long = productDao.insertProduct(product)

    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)

    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)

    // Stock In (Product Received)
    suspend fun stockIn(productId: Int, quantity: Double, notes: String): Boolean {
        val product = productDao.getProductById(productId) ?: return false
        val newStock = product.currentStock + quantity
        productDao.updateStock(productId, newStock)

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val transaction = Transaction(
            productId = product.id,
            productUuid = product.uuid,
            productNameEn = product.nameEn,
            productNameBn = product.nameBn,
            quantity = quantity,
            type = "STOCK_IN",
            date = currentDate,
            notes = notes
        )
        transactionDao.insertTransaction(transaction)
        return true
    }

    // Transactions
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsInDateRange(startDate: String, endDate: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsInDateRange(startDate, endDate)
    }

    fun getTransactionsForDate(date: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsForDate(date)
    }

    suspend fun insertTransaction(transaction: Transaction) = transactionDao.insertTransaction(transaction)

    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)

    // Cart Operations
    val allCartItems: Flow<List<CartItem>> = cartDao.getAllCartItems()

    suspend fun addToCart(productId: Int, quantity: Double, destination: String): Boolean {
        // Validate product exists
        val product = productDao.getProductById(productId) ?: return false
        
        // Check if item already exists in cart for the SAME destination
        val existingItem = cartDao.getCartItemByProductAndDestination(productId, destination)
        if (existingItem != null) {
            val newQty = existingItem.quantity + quantity
            cartDao.updateCartItemQuantity(existingItem.id, newQty)
        } else {
            val cartItem = CartItem(
                productId = productId,
                quantity = quantity,
                destination = destination
            )
            cartDao.insertCartItem(cartItem)
        }
        return true
    }

    suspend fun updateCartItemQuantity(id: Int, quantity: Double) {
        if (quantity <= 0) {
            // If quantity goes to 0 or negative, remove from cart
            // But we can do this in the caller, or check here
            // Just update quantity or remove if caller sends 0
            cartDao.updateCartItemQuantity(id, quantity)
        } else {
            cartDao.updateCartItemQuantity(id, quantity)
        }
    }

    suspend fun deleteCartItem(cartItem: CartItem) = cartDao.deleteCartItem(cartItem)

    suspend fun clearCart() = cartDao.clearCart()

    // Place Order / Checkout
    // This reduces the product stock and creates the outgoing stock transactions (Kitchen, Family, Male)
    suspend fun checkoutCart(notes: String): Pair<Boolean, String> {
        val cartItems = cartDao.getAllCartItemsSync()
        if (cartItems.isEmpty()) {
            return Pair(false, "কার্ট খালি! (Cart is empty!)")
        }

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // Process each cart item
        for (item in cartItems) {
            val product = productDao.getProductById(item.productId)
            if (product == null) {
                // If product not found, skip or return error
                continue
            }

            // Check if stock is sufficient
            if (product.currentStock < item.quantity) {
                return Pair(
                    false, 
                    "পর্যাপ্ত স্টক নেই: ${product.displayName()} (প্রয়োজন: ${item.quantity} ${product.unit}, বর্তমান স্টক: ${product.currentStock} ${product.unit})"
                )
            }
        }

        // All checks passed, let's deduct stock and insert transactions
        for (item in cartItems) {
            val product = productDao.getProductById(item.productId)!!
            val newStock = product.currentStock - item.quantity
            productDao.updateStock(product.id, newStock)

            val transactionType = when (item.destination.uppercase()) {
                "KITCHEN" -> "OUT_KITCHEN"
                "FAMILY" -> "OUT_FAMILY"
                "MALE" -> "OUT_MALE"
                else -> "OUT_KITCHEN"
            }

            val transaction = Transaction(
                productId = product.id,
                productUuid = product.uuid,
                productNameEn = product.nameEn,
                productNameBn = product.nameBn,
                quantity = item.quantity,
                type = transactionType,
                date = currentDate,
                notes = notes
            )
            transactionDao.insertTransaction(transaction)
        }

        // Clear the cart
        cartDao.clearCart()
        return Pair(true, "অর্ডার সফলভাবে সম্পন্ন হয়েছে এবং স্টক আপডেট করা হয়েছে! (Order successfully placed!)")
    }
}
