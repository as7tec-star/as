package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class MessageTemplate(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val type: String = "ترحيب",
    val content: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    companion object {
        val TYPES = listOf("ترحيب", "متابعة", "عرض سعر", "شكر", "تذكير")

        fun defaultTemplates(userId: String): List<MessageTemplate> = listOf(
            MessageTemplate(
                title = "رسالة ترحيبية بالعميل الجديد",
                type = "ترحيب",
                content = "أهلاً بك أخي {name}! سعداء بتواصلك معنا بخصوص اهتمامك بمنتجاتنا. كيف يمكننا خدمتك اليوم؟",
                userId = userId
            ),
            MessageTemplate(
                title = "متابعة بعد التواصل الأول",
                type = "متابعة",
                content = "مرحباً {name}، نود الاطمئنان عليك ومتابعة استفسارك السابق. هل ما زلت مهتماً بالمنتج أو لديك أي استفسار آخر؟ نحن بالخدمة دائماً.",
                userId = userId
            ),
            MessageTemplate(
                title = "تقديم عرض سعر خاص",
                type = "عرض سعر",
                content = "أهلاً {name}، يسعدنا أن نقدم لك عرضنا الحصري بخصوص {product} بسعر خاص {price} ريال مع إمكانية التوصيل السريع. هل ترغب بحجز طلبك؟",
                userId = userId
            ),
            MessageTemplate(
                title = "شكر وتقدير بعد إتمام البيع",
                type = "شكر",
                content = "شكراً جزيلاً لك أخي {name} على ثقتك واختيارك لنا! تم تسليم طلبك بنجاح، ويسعدنا دائماً تقييمك واستمرار تواصلك معنا.",
                userId = userId
            ),
            MessageTemplate(
                title = "تذكير وتجديد اهتمام",
                type = "تذكير",
                content = "مرحباً {name}، تذكير سريع بأن العرض الخاص الذي تحدثنا عنه متاح لفترة محدودة. إذا كنت مستعداً لتأكيد رغبتك فنحن جاهزون لإتمام طلبك مباشرة.",
                userId = userId
            )
        )
    }
}
