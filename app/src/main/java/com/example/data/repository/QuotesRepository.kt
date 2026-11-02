package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Quote
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

class QuotesRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول باستخدام حساب جوجل للوصول إلى عروض الأسعار.")
    }

    fun observeQuotes(): Flow<List<Quote>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/quotes"
        emitAll(
            db.collection("users").document(uid).collection("quotes")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Quote::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addQuote(quote: Quote): String {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("quotes")
        val data = hashMapOf(
            "userId" to uid,
            "quoteNumber" to quote.quoteNumber.trim(),
            "clientId" to quote.clientId,
            "clientName" to quote.clientName.trim(),
            "clientPhone" to quote.clientPhone.trim(),
            "date" to quote.date.trim(),
            "expiryDate" to quote.expiryDate.trim(),
            "status" to quote.status,
            "items" to quote.items,
            "subtotal" to quote.subtotal,
            "taxRate" to quote.taxRate,
            "taxAmount" to quote.taxAmount,
            "grandTotal" to quote.grandTotal,
            "notes" to quote.notes.trim(),
            "googleSheetUrl" to quote.googleSheetUrl.trim(),
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

    suspend fun updateQuote(quote: Quote) {
        val uid = requireUserId()
        require(quote.id.isNotBlank()) { "Quote ID cannot be blank" }
        val docRef = db.collection("users").document(uid).collection("quotes").document(quote.id)
        val data = hashMapOf(
            "userId" to uid,
            "quoteNumber" to quote.quoteNumber.trim(),
            "clientId" to quote.clientId,
            "clientName" to quote.clientName.trim(),
            "clientPhone" to quote.clientPhone.trim(),
            "date" to quote.date.trim(),
            "expiryDate" to quote.expiryDate.trim(),
            "status" to quote.status,
            "items" to quote.items,
            "subtotal" to quote.subtotal,
            "taxRate" to quote.taxRate,
            "taxAmount" to quote.taxAmount,
            "grandTotal" to quote.grandTotal,
            "notes" to quote.notes.trim(),
            "googleSheetUrl" to quote.googleSheetUrl.trim(),
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

    suspend fun updateQuoteStatus(quoteId: String, newStatus: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("quotes").document(quoteId)
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

    suspend fun deleteQuote(quoteId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("quotes").document(quoteId)
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
