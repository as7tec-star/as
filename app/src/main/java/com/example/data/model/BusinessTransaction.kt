package com.example.data.model

import androidx.annotation.Keep
import com.example.util.FinancialCalculator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class TransactionItem(
    val name: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val discount: Double = 0.0,
    val netAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val total: Double = 0.0
) {
    fun recalculated(taxRate: Double, taxMode: String): TransactionItem {
        val res = FinancialCalculator.calculateLineItem(quantity, unitPrice, discount, taxRate, taxMode)
        return copy(
            netAmount = res.net,
            taxAmount = res.tax,
            total = res.total
        )
    }

    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "quantity" to quantity,
        "unitPrice" to unitPrice,
        "discount" to discount,
        "netAmount" to netAmount,
        "taxAmount" to taxAmount,
        "total" to total
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): TransactionItem {
            return TransactionItem(
                name = map["name"] as? String ?: "",
                quantity = (map["quantity"] as? Number)?.toDouble() ?: 1.0,
                unitPrice = (map["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                discount = (map["discount"] as? Number)?.toDouble() ?: 0.0,
                netAmount = (map["netAmount"] as? Number)?.toDouble() ?: 0.0,
                taxAmount = (map["taxAmount"] as? Number)?.toDouble() ?: 0.0,
                total = (map["total"] as? Number)?.toDouble() ?: 0.0
            )
        }
    }
}

@Keep
@IgnoreExtraProperties
data class BusinessTransaction(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val type: String = "مبيعات", // "مبيعات" or "مشتريات"
    val invoiceNumber: String = "",
    val partyName: String = "",
    val partyPhone: String = "",
    val partyTaxNumber: String = "",
    val category: String = "بضاعة",
    val date: String = "",
    val paymentMethod: String = "تحويل بنكي",
    val paymentStatus: String = "مدفوع",
    val taxMode: String = "غير شامل الضريبة", // "غير شامل الضريبة", "شامل الضريبة", "معفى"
    val taxRate: Double = 15.0,
    val items: List<Map<String, Any>> = emptyList(),
    val subtotal: Double = 0.0,
    val discountTotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    fun getTypedItems(): List<TransactionItem> = items.map { TransactionItem.fromMap(it) }

    companion object {
        val TYPES = listOf("مبيعات", "مشتريات")
        val TAX_MODES = listOf("غير شامل الضريبة", "شامل الضريبة", "معفى")
        val PAYMENT_METHODS = listOf("نقدي", "تحويل بنكي", "شبكة / مدى", "آجل")
        val PAYMENT_STATUSES = listOf("مدفوع", "جزئي", "آجل")
        val CATEGORIES = listOf("بضاعة", "خدمات", "تسويق وإعلانات", "شحن وتوصيل", "إيجار ومرافق", "تجهيزات", "أخرى")
    }
}
