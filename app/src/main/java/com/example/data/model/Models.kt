package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val nameEn: String, // English Name
    val nameBn: String, // Bengali Name
    val category: String, // e.g. Grocery, Vegetables, Spices, Oils, Meat, Dry Goods, etc.
    val currentStock: Double, // Real-time available stock
    val unit: String, // Kg, Litre, Piece, Bag, Box, etc.
    val minStockAlert: Double = 5.0, // Low stock threshold
    val lastUpdated: Long = System.currentTimeMillis()
) {
    // Return formatted name based on local availability (or both)
    fun displayName(): String {
        return if (nameBn.isNotBlank() && nameEn.isNotBlank()) {
            "$nameBn ($nameEn)"
        } else if (nameBn.isNotBlank()) {
            nameBn
        } else {
            nameEn
        }
    }
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val productId: Int,
    val productUuid: String,
    val productNameEn: String,
    val productNameBn: String,
    val quantity: Double,
    val type: String, // "STOCK_IN" (Received), "OUT_KITCHEN" (Kitchen), "OUT_FAMILY" (Family Section), "OUT_MALE" (Male Section)
    val date: String, // "YYYY-MM-DD" for easy filtering and grouping
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isSynced: Boolean = false
) {
    fun getDestinationLabel(): String {
        return when (type) {
            "STOCK_IN" -> "স্টক ইন / গুদামে প্রবেশ (Stock In)"
            "OUT_KITCHEN" -> "রান্নাঘর (Kitchen)"
            "OUT_FAMILY" -> "ফ্যামিলি সেক্টর (Family Section)"
            "OUT_MALE" -> "পুরুষ সেক্টর (Men's Section)"
            else -> type
        }
    }
}

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val quantity: Double,
    val destination: String // "KITCHEN", "FAMILY", "MALE"
)
