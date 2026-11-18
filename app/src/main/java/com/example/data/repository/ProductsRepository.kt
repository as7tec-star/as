package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BusinessTransaction
import com.example.data.model.Product
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
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class ProductsRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول باستخدام حساب جوجل للوصول إلى المخزون.")
    }

    fun observeProducts(): Flow<List<Product>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/products"
        emitAll(
            db.collection("users").document(uid).collection("products")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Product::class.java)
                        .sortedWith(
                            compareByDescending<Product> { it.isLowStock() }
                                .thenBy { it.quantity }
                                .thenBy { it.name }
                        )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addProduct(product: Product): String {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("products")
        val data = hashMapOf(
            "userId" to uid,
            "name" to product.name.trim(),
            "sku" to product.sku.trim(),
            "category" to product.category,
            "unit" to product.unit,
            "quantity" to product.quantity.coerceAtLeast(0.0),
            "minThreshold" to product.minThreshold.coerceAtLeast(0.0),
            "unitCost" to product.unitCost.coerceAtLeast(0.0),
            "sellingPrice" to product.sellingPrice.coerceAtLeast(0.0),
            "supplierName" to product.supplierName.trim(),
            "supplierPhone" to product.supplierPhone.trim(),
            "alertsEnabled" to product.alertsEnabled,
            "notes" to product.notes.trim(),
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

    suspend fun addProductsBatch(products: List<Product>): Int {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("products")
        var count = 0
        try {
            products.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { product ->
                    val docRef = collection.document()
                    val data = hashMapOf(
                        "userId" to uid,
                        "name" to product.name.trim(),
                        "sku" to product.sku.trim(),
                        "category" to product.category,
                        "unit" to product.unit,
                        "quantity" to product.quantity.coerceAtLeast(0.0),
                        "minThreshold" to product.minThreshold.coerceAtLeast(0.0),
                        "unitCost" to product.unitCost.coerceAtLeast(0.0),
                        "sellingPrice" to product.sellingPrice.coerceAtLeast(0.0),
                        "supplierName" to product.supplierName.trim(),
                        "supplierPhone" to product.supplierPhone.trim(),
                        "alertsEnabled" to product.alertsEnabled,
                        "notes" to product.notes.trim(),
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

    suspend fun updateProduct(product: Product) {
        val uid = requireUserId()
        require(product.id.isNotBlank()) { "Product ID cannot be blank" }
        val docRef = db.collection("users").document(uid).collection("products").document(product.id)
        val data = hashMapOf(
            "userId" to uid,
            "name" to product.name.trim(),
            "sku" to product.sku.trim(),
            "category" to product.category,
            "unit" to product.unit,
            "quantity" to product.quantity.coerceAtLeast(0.0),
            "minThreshold" to product.minThreshold.coerceAtLeast(0.0),
            "unitCost" to product.unitCost.coerceAtLeast(0.0),
            "sellingPrice" to product.sellingPrice.coerceAtLeast(0.0),
            "supplierName" to product.supplierName.trim(),
            "supplierPhone" to product.supplierPhone.trim(),
            "alertsEnabled" to product.alertsEnabled,
            "notes" to product.notes.trim(),
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

    suspend fun adjustQuantity(productId: String, newQuantity: Double) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("products").document(productId)
        val safeQty = newQuantity.coerceAtLeast(0.0)
        try {
            docRef.update(
                mapOf(
                    "quantity" to safeQty,
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

    suspend fun toggleAlerts(productId: String, enabled: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("products").document(productId)
        try {
            docRef.update(
                mapOf(
                    "alertsEnabled" to enabled,
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

    suspend fun deleteProduct(productId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("products").document(productId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    /**
     * Automatically deducts stock for Sales ("مبيعات") or increases stock for Purchases ("مشتريات")
     * when line items match products in inventory by normalized name or SKU.
     * Returns updated products that are now in low-stock alert state.
     */
    suspend fun applyTransactionToStock(
        tx: BusinessTransaction,
        currentProducts: List<Product>
    ): List<Product> {
        if (currentProducts.isEmpty()) return emptyList()
        val uid = requireUserId()
        val items = tx.getTypedItems()
        if (items.isEmpty()) return emptyList()

        val isSale = tx.type == "مبيعات"
        val updatedMap = mutableMapOf<String, Product>()

        items.forEach { line ->
            val key = line.name.trim().lowercase(Locale.ROOT)
            if (key.isBlank() || line.quantity <= 0.0) return@forEach

            val matched = currentProducts.firstOrNull { product ->
                product.name.trim().lowercase(Locale.ROOT) == key ||
                    (product.sku.isNotBlank() && product.sku.trim().lowercase(Locale.ROOT) == key)
            } ?: return@forEach

            val baseProduct = updatedMap[matched.id] ?: matched
            val delta = if (isSale) -line.quantity else line.quantity
            val newQty = (baseProduct.quantity + delta).coerceAtLeast(0.0)
            updatedMap[matched.id] = baseProduct.copy(quantity = newQty)
        }

        if (updatedMap.isEmpty()) return emptyList()

        val productsCollection = db.collection("users").document(uid).collection("products")
        try {
            updatedMap.values.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { updated ->
                    val docRef = productsCollection.document(updated.id)
                    batch.update(
                        docRef,
                        mapOf(
                            "quantity" to updated.quantity,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            return updatedMap.values.filter { it.isLowStock() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, productsCollection.path)
            throw e
        }
    }
}
