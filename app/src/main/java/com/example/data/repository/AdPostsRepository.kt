package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdPost
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class AdPostsRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول باستخدام حساب جوجل للوصول إلى الإعلانات.")
    }

    fun observeAds(): Flow<List<AdPost>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/ads"
        emitAll(
            db.collection("users").document(uid).collection("ads")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(AdPost::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addAd(ad: AdPost): String {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("ads")
        val data = hashMapOf(
            "userId" to uid,
            "title" to ad.title.trim(),
            "price" to ad.price.trim(),
            "description" to ad.description.trim(),
            "city" to ad.city.trim(),
            "platform" to ad.platform,
            "status" to ad.status,
            "generatedVersions" to ad.generatedVersions,
            "renewalDays" to ad.renewalDays,
            "postUrl" to ad.postUrl.trim(),
            "scheduledDate" to ad.scheduledDate.trim(),
            "publishedAt" to ad.publishedAt.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            val docRef = collection.add(data).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, collection.path)
            throw e
        }
    }

    suspend fun updateAd(ad: AdPost) {
        val uid = requireUserId()
        require(ad.id.isNotBlank()) { "Ad ID cannot be blank" }
        val docRef = db.collection("users").document(uid).collection("ads").document(ad.id)
        val data = hashMapOf(
            "userId" to uid,
            "title" to ad.title.trim(),
            "price" to ad.price.trim(),
            "description" to ad.description.trim(),
            "city" to ad.city.trim(),
            "platform" to ad.platform,
            "status" to ad.status,
            "generatedVersions" to ad.generatedVersions,
            "renewalDays" to ad.renewalDays,
            "postUrl" to ad.postUrl.trim(),
            "scheduledDate" to ad.scheduledDate.trim(),
            "publishedAt" to ad.publishedAt.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(data as Map<String, Any>).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateAdStatus(adId: String, newStatus: String, publishedAt: String = "") {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("ads").document(adId)
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (publishedAt.isNotBlank()) {
            updates["publishedAt"] = publishedAt
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAd(adId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("ads").document(adId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
