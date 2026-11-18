package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BusinessTransaction
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

class TransactionsRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول باستخدام حساب جوجل للوصول إلى الفواتير.")
    }

    fun observeTransactions(): Flow<List<BusinessTransaction>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/transactions"
        emitAll(
            db.collection("users").document(uid).collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(BusinessTransaction::class.java)
                        .sortedByDescending { it.date }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addTransaction(tx: BusinessTransaction): String {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("transactions")
        val data = hashMapOf(
            "userId" to uid,
            "type" to tx.type,
            "invoiceNumber" to tx.invoiceNumber.trim(),
            "partyName" to tx.partyName.trim(),
            "partyPhone" to tx.partyPhone.trim(),
            "partyTaxNumber" to tx.partyTaxNumber.trim(),
            "category" to tx.category,
            "date" to tx.date.trim(),
            "paymentMethod" to tx.paymentMethod,
            "paymentStatus" to tx.paymentStatus,
            "taxMode" to tx.taxMode,
            "taxRate" to tx.taxRate,
            "items" to tx.items,
            "subtotal" to tx.subtotal,
            "discountTotal" to tx.discountTotal,
            "taxAmount" to tx.taxAmount,
            "grandTotal" to tx.grandTotal,
            "notes" to tx.notes.trim(),
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

    suspend fun addTransactionsBatch(transactions: List<BusinessTransaction>): Int {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("transactions")
        var count = 0
        try {
            transactions.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { tx ->
                    val docRef = collection.document()
                    val data = hashMapOf(
                        "userId" to uid,
                        "type" to tx.type,
                        "invoiceNumber" to tx.invoiceNumber.trim(),
                        "partyName" to tx.partyName.trim(),
                        "partyPhone" to tx.partyPhone.trim(),
                        "partyTaxNumber" to tx.partyTaxNumber.trim(),
                        "category" to tx.category,
                        "date" to tx.date.trim(),
                        "paymentMethod" to tx.paymentMethod,
                        "paymentStatus" to tx.paymentStatus,
                        "taxMode" to tx.taxMode,
                        "taxRate" to tx.taxRate,
                        "items" to tx.items,
                        "subtotal" to tx.subtotal,
                        "discountTotal" to tx.discountTotal,
                        "taxAmount" to tx.taxAmount,
                        "grandTotal" to tx.grandTotal,
                        "notes" to tx.notes.trim(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, data)
                    count++
                }
                batch.commit().await()
            }
            return count
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, collection.path)
            throw e
        }
    }

    suspend fun updateTransaction(tx: BusinessTransaction) {
        val uid = requireUserId()
        require(tx.id.isNotBlank()) { "Transaction ID cannot be blank" }
        val docRef = db.collection("users").document(uid).collection("transactions").document(tx.id)
        val data = hashMapOf(
            "userId" to uid,
            "type" to tx.type,
            "invoiceNumber" to tx.invoiceNumber.trim(),
            "partyName" to tx.partyName.trim(),
            "partyPhone" to tx.partyPhone.trim(),
            "partyTaxNumber" to tx.partyTaxNumber.trim(),
            "category" to tx.category,
            "date" to tx.date.trim(),
            "paymentMethod" to tx.paymentMethod,
            "paymentStatus" to tx.paymentStatus,
            "taxMode" to tx.taxMode,
            "taxRate" to tx.taxRate,
            "items" to tx.items,
            "subtotal" to tx.subtotal,
            "discountTotal" to tx.discountTotal,
            "taxAmount" to tx.taxAmount,
            "grandTotal" to tx.grandTotal,
            "notes" to tx.notes.trim(),
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

    suspend fun updatePaymentStatus(txId: String, newStatus: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("transactions").document(txId)
        try {
            docRef.update(
                mapOf(
                    "paymentStatus" to newStatus,
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

    suspend fun deleteTransaction(txId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("transactions").document(txId)
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
