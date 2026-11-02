package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class AdPost(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val price: String = "",
    val description: String = "",
    val city: String = "",
    val platform: String = "عام",
    val status: String = "لم ينشر",
    val generatedVersions: Map<String, String> = emptyMap(),
    val renewalDays: Long = 7L,
    val postUrl: String = "",
    val scheduledDate: String = "",
    val publishedAt: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    companion object {
        val STATUS_LIST = listOf("لم ينشر", "نُشر", "يحتاج تجديد")
        val PLATFORMS = listOf("حراج", "مرجان", "تيك توك", "فيسبوك", "واتساب")
    }
}
