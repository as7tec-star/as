package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Employee
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

class EmployeesRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول باستخدام حساب جوجل للوصول إلى الموظفين.")
    }

    fun observeEmployees(): Flow<List<Employee>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/employees"
        emitAll(
            db.collection("users").document(uid).collection("employees")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Employee::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addEmployee(emp: Employee): String {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("employees")
        val data = hashMapOf(
            "userId" to uid,
            "name" to emp.name.trim(),
            "jobTitle" to emp.jobTitle.trim(),
            "department" to emp.department,
            "phone" to emp.phone.trim(),
            "email" to emp.email.trim(),
            "nationalId" to emp.nationalId.trim(),
            "hireDate" to emp.hireDate.trim(),
            "baseSalary" to emp.baseSalary,
            "housingAllowance" to emp.housingAllowance,
            "transportAllowance" to emp.transportAllowance,
            "otherAllowances" to emp.otherAllowances,
            "defaultDeductions" to emp.defaultDeductions,
            "bankIban" to emp.bankIban.trim(),
            "status" to emp.status,
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

    suspend fun addEmployeesBatch(employees: List<Employee>): Int {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("employees")
        var count = 0
        try {
            employees.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { emp ->
                    val docRef = collection.document()
                    val data = hashMapOf(
                        "userId" to uid,
                        "name" to emp.name.trim(),
                        "jobTitle" to emp.jobTitle.trim(),
                        "department" to emp.department,
                        "phone" to emp.phone.trim(),
                        "email" to emp.email.trim(),
                        "nationalId" to emp.nationalId.trim(),
                        "hireDate" to emp.hireDate.trim(),
                        "baseSalary" to emp.baseSalary,
                        "housingAllowance" to emp.housingAllowance,
                        "transportAllowance" to emp.transportAllowance,
                        "otherAllowances" to emp.otherAllowances,
                        "defaultDeductions" to emp.defaultDeductions,
                        "bankIban" to emp.bankIban.trim(),
                        "status" to emp.status,
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

    suspend fun updateEmployee(emp: Employee) {
        val uid = requireUserId()
        require(emp.id.isNotBlank()) { "Employee ID cannot be blank" }
        val docRef = db.collection("users").document(uid).collection("employees").document(emp.id)
        val data = hashMapOf(
            "userId" to uid,
            "name" to emp.name.trim(),
            "jobTitle" to emp.jobTitle.trim(),
            "department" to emp.department,
            "phone" to emp.phone.trim(),
            "email" to emp.email.trim(),
            "nationalId" to emp.nationalId.trim(),
            "hireDate" to emp.hireDate.trim(),
            "baseSalary" to emp.baseSalary,
            "housingAllowance" to emp.housingAllowance,
            "transportAllowance" to emp.transportAllowance,
            "otherAllowances" to emp.otherAllowances,
            "defaultDeductions" to emp.defaultDeductions,
            "bankIban" to emp.bankIban.trim(),
            "status" to emp.status,
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

    suspend fun deleteEmployee(empId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("employees").document(empId)
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
