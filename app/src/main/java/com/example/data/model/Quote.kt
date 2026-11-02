package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class QuoteItem(
    val name: String = "",
    val description: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "description" to description,
        "quantity" to quantity,
        "unitPrice" to unitPrice,
        "discount" to discount,
        "total" to total
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): QuoteItem {
            return QuoteItem(
                name = map["name"] as? String ?: "",
                description = map["description"] as? String ?: "",
                quantity = (map["quantity"] as? Number)?.toDouble() ?: 1.0,
                unitPrice = (map["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                discount = (map["discount"] as? Number)?.toDouble() ?: 0.0,
                total = (map["total"] as? Number)?.toDouble() ?: 0.0
            )
        }
    }
}

@IgnoreExtraProperties
data class Quote(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val quoteNumber: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val date: String = "",
    val expiryDate: String = "",
    val status: String = "مسودة", // مسودة, مرسل, مقبول, مرفوض
    val items: List<Map<String, Any>> = emptyList(),
    val subtotal: Double = 0.0,
    val taxRate: Double = 15.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val notes: String = "",
    val googleSheetUrl: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    fun getTypedItems(): List<QuoteItem> = items.map { QuoteItem.fromMap(it) }

    companion object {
        val STATUS_LIST = listOf("مسودة", "مرسل", "مقبول", "مرفوض")

        // Pre-built quotation templates for quick data entry
        fun getPrebuiltTemplates(): List<Pair<String, List<QuoteItem>>> = listOf(
            "قالب بضائع ومنتجات تجارية" to listOf(
                QuoteItem(name = "المنتج الأساسي - درجة أولى", description = "شامل الضمان لمدة سنة والتغليف الفاخر", quantity = 1.0, unitPrice = 500.0, total = 500.0),
                QuoteItem(name = "ملحقات وإكسسوارات إضافية", description = "شاحن سريع وحافظة حماية أصلية", quantity = 1.0, unitPrice = 80.0, total = 80.0),
                QuoteItem(name = "رسوم الشحن والتوصيل السريع", description = "توصيل خلال 24 ساعة لباب المنزل", quantity = 1.0, unitPrice = 35.0, total = 35.0)
            ),
            "قالب خدمات تسويقية وإعلانية" to listOf(
                QuoteItem(name = "إدارة ونشر إعلانات حراج ومرجان", description = "تحديث وتجديد الإعلان يومياً لمدة شهر", quantity = 1.0, unitPrice = 600.0, total = 600.0),
                QuoteItem(name = "تصميم منشورات وفيديوهات تيك توك", description = "إنتاج 4 فيديوهات إعلانية قصيرة وجذابة", quantity = 4.0, unitPrice = 150.0, total = 600.0),
                QuoteItem(name = "حملة مراسلات واتساب مخصصة", description = "إرسال 200 رسالة ترويجية للعملاء المهتمين", quantity = 1.0, unitPrice = 300.0, total = 300.0)
            ),
            "قالب خدمات واستشارات مبيعات" to listOf(
                QuoteItem(name = "جلسة تخطيط واستراتيجية بيع", description = "تحديد الجمهور المستهدف وبناء مسار التحويل", quantity = 1.0, unitPrice = 450.0, total = 450.0),
                QuoteItem(name = "إعداد قوالب وتدريب الردود السريعة", description = "صياغة نصوص الرد على الاعتراضات وإغلاق البيع", quantity = 1.0, unitPrice = 350.0, total = 350.0)
            )
        )
    }
}
