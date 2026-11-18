package com.example.data.model

import androidx.annotation.Keep
import com.example.util.FinancialCalculator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class Employee(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val jobTitle: String = "",
    val department: String = "المبيعات",
    val phone: String = "",
    val email: String = "",
    val nationalId: String = "",
    val hireDate: String = "",
    val baseSalary: Double = 0.0,
    val housingAllowance: Double = 0.0,
    val transportAllowance: Double = 0.0,
    val otherAllowances: Double = 0.0,
    val defaultDeductions: Double = 0.0,
    val bankIban: String = "",
    val status: String = "نشط",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    fun totalAllowances(): Double {
        return FinancialCalculator.round2(housingAllowance + transportAllowance + otherAllowances)
    }

    fun estimatedNetSalary(): Double {
        return FinancialCalculator.calculatePayroll(
            baseSalary = baseSalary,
            allowances = totalAllowances(),
            bonuses = 0.0,
            deductions = defaultDeductions
        ).netSalary
    }

    companion object {
        val DEPARTMENTS = listOf("المبيعات", "التسويق", "خدمة العملاء", "التوصيل واللوجستيات", "المحاسبة", "الإدارة", "أخرى")
        val STATUSES = listOf("نشط", "إجازة", "منتهي الخدمة")
    }
}
