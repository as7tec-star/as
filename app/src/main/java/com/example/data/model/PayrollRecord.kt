package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class PayrollRecord(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val jobTitle: String = "",
    val employeePhone: String = "",
    val monthYear: String = "",
    val baseSalary: Double = 0.0,
    val allowances: Double = 0.0,
    val bonuses: Double = 0.0,
    val deductions: Double = 0.0,
    val netSalary: Double = 0.0,
    val paymentStatus: String = "مسودة", // "مسودة", "معتمد", "مدفوع"
    val paymentDate: String = "",
    val notes: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    companion object {
        val PAYMENT_STATUSES = listOf("مسودة", "معتمد", "مدفوع")
    }
}
