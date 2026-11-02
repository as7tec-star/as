package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.MessageTemplate
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

class TemplatesRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول باستخدام حساب جوجل للوصول إلى القوالب.")
    }

    fun observeTemplates(): Flow<List<MessageTemplate>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/templates"
        emitAll(
            db.collection("users").document(uid).collection("templates")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(MessageTemplate::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun seedDefaultsIfEmpty() {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("templates")
        try {
            val snapshot = collection.limit(1).get().await()
            if (snapshot.isEmpty) {
                val defaults = MessageTemplate.defaultTemplates(uid)
                for (template in defaults) {
                    collection.add(
                        mapOf(
                            "userId" to uid,
                            "title" to template.title,
                            "type" to template.type,
                            "content" to template.content,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, collection.path)
        }
    }

    suspend fun addTemplate(template: MessageTemplate): String {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("templates")
        val data = hashMapOf(
            "userId" to uid,
            "title" to template.title.trim(),
            "type" to template.type,
            "content" to template.content.trim(),
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

    suspend fun updateTemplate(template: MessageTemplate) {
        val uid = requireUserId()
        require(template.id.isNotBlank()) { "Template ID cannot be blank" }
        val docRef = db.collection("users").document(uid).collection("templates").document(template.id)
        val data = hashMapOf(
            "userId" to uid,
            "title" to template.title.trim(),
            "type" to template.type,
            "content" to template.content.trim(),
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

    suspend fun deleteTemplate(templateId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("templates").document(templateId)
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
