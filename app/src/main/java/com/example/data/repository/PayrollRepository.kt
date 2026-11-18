package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Employee
import com.example.data.model.PayrollRecord
import com.example.util.FinancialCalculator
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

class PayrollRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول باستخدام حساب جوجل للوصول إلى الرواتب.")
    }

    fun observePayrolls(): Flow<List<PayrollRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/payrolls"
        emitAll(
            db.collection("users").document(uid).collection("payrolls")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PayrollRecord::class.java)
                        .sortedByDescending { it.monthYear }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun generateMonthlyPayroll(
        monthYear: String,
        activeEmployees: List<Employee>,
        existingForMonth: List<PayrollRecord>
    ): Int {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("payrolls")
        val existingEmpIds = existingForMonth.map { it.employeeId }.toSet()
        val missingEmployees = activeEmployees.filter { it.status == "نشط" && it.id !in existingEmpIds }

        if (missingEmployees.isEmpty()) return 0

        try {
            val batch = db.batch()
            missingEmployees.forEach { emp ->
                val allowances = emp.totalAllowances()
                val calc = FinancialCalculator.calculatePayroll(
                    baseSalary = emp.baseSalary,
                    allowances = allowances,
                    bonuses = 0.0,
                    deductions = emp.defaultDeductions
                )
                val docRef = collection.document()
                val data = hashMapOf(
                    "userId" to uid,
                    "employeeId" to emp.id,
                    "employeeName" to emp.name,
                    "jobTitle" to emp.jobTitle,
                    "employeePhone" to emp.phone,
                    "monthYear" to monthYear,
                    "baseSalary" to emp.baseSalary,
                    "allowances" to allowances,
                    "bonuses" to 0.0,
                    "deductions" to emp.defaultDeductions,
                    "netSalary" to calc.netSalary,
                    "paymentStatus" to "مسودة",
                    "paymentDate" to "",
                    "notes" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, data)
            }
            batch.commit().await()
            return missingEmployees.size
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, collection.path)
            throw e
        }
    }

    suspend fun updatePayrollRecord(record: PayrollRecord) {
        val uid = requireUserId()
        require(record.id.isNotBlank()) { "PayrollRecord ID cannot be blank" }
        val docRef = db.collection("users").document(uid).collection("payrolls").document(record.id)
        val calc = FinancialCalculator.calculatePayroll(
            baseSalary = record.baseSalary,
            allowances = record.allowances,
            bonuses = record.bonuses,
            deductions = record.deductions
        )
        val data = hashMapOf(
            "userId" to uid,
            "employeeId" to record.employeeId,
            "employeeName" to record.employeeName.trim(),
            "jobTitle" to record.jobTitle.trim(),
            "employeePhone" to record.employeePhone.trim(),
            "monthYear" to record.monthYear,
            "baseSalary" to record.baseSalary,
            "allowances" to record.allowances,
            "bonuses" to record.bonuses,
            "deductions" to record.deductions,
            "netSalary" to calc.netSalary,
            "paymentStatus" to record.paymentStatus,
            "paymentDate" to record.paymentDate,
            "notes" to record.notes.trim(),
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

    suspend fun markAllPaid(records: List<PayrollRecord>, paymentDate: String) {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("payrolls")
        try {
            val batch = db.batch()
            records.filter { it.paymentStatus != "مدفوع" }.forEach { record ->
                val docRef = collection.document(record.id)
                batch.update(
                    docRef,
                    mapOf(
                        "paymentStatus" to "مدفوع",
                        "paymentDate" to paymentDate,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, collection.path)
            throw e
        }
    }

    suspend fun deletePayrollRecord(recordId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("payrolls").document(recordId)
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
