package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Client
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class ClientsRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول باستخدام حساب جوجل للوصول إلى البيانات.")
    }

    fun observeClients(): Flow<List<Client>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/clients"
        emitAll(
            db.collection("users").document(uid).collection("clients")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Client::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addClient(client: Client): String {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("clients")
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        val data = hashMapOf(
            "userId" to uid,
            "name" to client.name.trim(),
            "phone" to client.phone.trim(),
            "email" to client.email.trim(),
            "source" to client.source,
            "status" to client.status,
            "notes" to client.notes.trim(),
            "lastContactDate" to if (client.lastContactDate.isNotBlank()) client.lastContactDate else todayStr,
            "dealValue" to client.dealValue,
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

    suspend fun updateClient(client: Client) {
        val uid = requireUserId()
        require(client.id.isNotBlank()) { "Client ID cannot be blank" }
        val docRef = db.collection("users").document(uid).collection("clients").document(client.id)
        val data = hashMapOf(
            "userId" to uid,
            "name" to client.name.trim(),
            "phone" to client.phone.trim(),
            "email" to client.email.trim(),
            "source" to client.source,
            "status" to client.status,
            "notes" to client.notes.trim(),
            "lastContactDate" to client.lastContactDate,
            "dealValue" to client.dealValue,
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

    suspend fun updateClientStatus(clientId: String, newStatus: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("clients").document(clientId)
        try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateLastContactDate(clientId: String, dateStr: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("clients").document(clientId)
        try {
            docRef.update(
                mapOf(
                    "lastContactDate" to dateStr,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteClient(clientId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("clients").document(clientId)
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
