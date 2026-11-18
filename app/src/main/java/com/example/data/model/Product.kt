package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val sku: String = "",
    val category: String = "بضاعة",
    val unit: String = "حبة",
    val quantity: Double = 0.0,
    val minThreshold: Double = 5.0,
    val unitCost: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val supplierName: String = "",
    val supplierPhone: String = "",
    val alertsEnabled: Boolean = true,
    val notes: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    @Exclude
    fun isLowStock(): Boolean = alertsEnabled && quantity <= minThreshold

    @Exclude
    fun isOutOfStock(): Boolean = quantity <= 0.0

    @Exclude
    fun stockStatusLabel(): String = when {
        quantity <= 0.0 -> "نفد المخزون"
        alertsEnabled && quantity <= minThreshold -> "مخزون منخفض"
        else -> "متوفر"
    }

    @Exclude
    fun formattedQuantity(): String = formatNumber(quantity)

    @Exclude
    fun formattedThreshold(): String = formatNumber(minThreshold)

    @Exclude
    fun stockRatio(): Float {
        if (minThreshold <= 0.0) return if (quantity > 0.0) 1f else 0f
        val referenceMax = (minThreshold * 2.0).coerceAtLeast(1.0)
        return (quantity / referenceMax).toFloat().coerceIn(0f, 1f)
    }

    companion object {
        val CATEGORIES = listOf(
            "بضاعة",
            "إلكترونيات",
            "ملابس وإكسسوارات",
            "مستلزمات منزلية",
            "مواد غذائية",
            "قطع غيار",
            "أخرى"
        )

        val UNITS = listOf(
            "حبة",
            "كرتون",
            "علبة",
            "طقم",
            "كجم",
            "متر",
            "لتر"
        )

        fun formatNumber(value: Double): String {
            return if (value % 1.0 == 0.0) {
                value.toLong().toString()
            } else {
                String.format(Locale.ENGLISH, "%.2f", value).trimEnd('0').trimEnd('.')
            }
        }
    }
}
