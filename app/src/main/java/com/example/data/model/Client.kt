package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Client(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val source: String = "حراج",
    val status: String = "جديد",
    val notes: String = "",
    val lastContactDate: String = "",
    val dealValue: Double = 0.0,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    companion object {
        val SOURCES = listOf("حراج", "مرجان", "تيك توك", "فيسبوك", "واتساب", "أخرى")
        val STATUSES = listOf("جديد", "تم التواصل", "مهتم", "تفاوض", "تم البيع", "خسرناه")
    }
}
