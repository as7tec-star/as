package com.example.ui.screens.payroll

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Employee
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal
import com.example.util.FinancialCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EmployeeDialog(
    employeeToEdit: Employee? = null,
    onDismiss: () -> Unit,
    onSave: (Employee) -> Unit
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date()) }

    var name by remember { mutableStateOf(employeeToEdit?.name ?: "") }
    var jobTitle by remember { mutableStateOf(employeeToEdit?.jobTitle ?: "") }
    var department by remember { mutableStateOf(employeeToEdit?.department ?: "المبيعات") }
    var phone by remember { mutableStateOf(employeeToEdit?.phone ?: "") }
    var email by remember { mutableStateOf(employeeToEdit?.email ?: "") }
    var nationalId by remember { mutableStateOf(employeeToEdit?.nationalId ?: "") }
    var hireDate by remember { mutableStateOf(employeeToEdit?.hireDate ?: todayStr) }
    var bankIban by remember { mutableStateOf(employeeToEdit?.bankIban ?: "") }
    var status by remember { mutableStateOf(employeeToEdit?.status ?: "نشط") }

    var baseSalaryText by remember {
        mutableStateOf(if ((employeeToEdit?.baseSalary ?: 0.0) > 0) employeeToEdit!!.baseSalary.toString() else "")
    }
    var housingText by remember {
        mutableStateOf(if ((employeeToEdit?.housingAllowance ?: 0.0) > 0) employeeToEdit!!.housingAllowance.toString() else "")
    }
    var transportText by remember {
        mutableStateOf(if ((employeeToEdit?.transportAllowance ?: 0.0) > 0) employeeToEdit!!.transportAllowance.toString() else "")
    }
    var otherAllowancesText by remember {
        mutableStateOf(if ((employeeToEdit?.otherAllowances ?: 0.0) > 0) employeeToEdit!!.otherAllowances.toString() else "")
    }
    var deductionsText by remember {
        mutableStateOf(if ((employeeToEdit?.defaultDeductions ?: 0.0) > 0) employeeToEdit!!.defaultDeductions.toString() else "")
    }

    var deptDropdownExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val baseSalary = baseSalaryText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val housing = housingText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val transport = transportText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val otherAllow = otherAllowancesText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val deductions = deductionsText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val totalAllowances = FinancialCalculator.round2(housing + transport + otherAllow)
    val payrollPreview = FinancialCalculator.calculatePayroll(
        baseSalary = baseSalary,
        allowances = totalAllowances,
        bonuses = 0.0,
        deductions = deductions
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (employeeToEdit == null) "إضافة موظف جديد" else "تعديل بيانات الموظف",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    tint = HubTeal,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 200) name = it },
                    label = { Text("الاسم الكامل *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { if (it.length <= 150) jobTitle = it },
                        label = { Text("المسمى الوظيفي *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = department,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("القسم") },
                            trailingIcon = {
                                IconButton(onClick = { deptDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "اختيار القسم")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        DropdownMenu(
                            expanded = deptDropdownExpanded,
                            onDismissRequest = { deptDropdownExpanded = false }
                        ) {
                            Employee.DEPARTMENTS.forEach { dept ->
                                DropdownMenuItem(
                                    text = { Text(dept) },
                                    onClick = {
                                        department = dept
                                        deptDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الجوال") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = nationalId,
                        onValueChange = { nationalId = it },
                        label = { Text("رقم الهوية / الإقامة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = hireDate,
                        onValueChange = { hireDate = it },
                        label = { Text("تاريخ التعيين") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bankIban,
                    onValueChange = { bankIban = it },
                    label = { Text("رقم الآيبان البنكي (IBAN)") },
                    placeholder = { Text("SA...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "الحالة الوظيفية:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Employee.STATUSES.forEach { st ->
                        val selected = status == st
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) HubNavy else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { status = st }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = st,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "هيكل الراتب والبدلات (ر.س):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = baseSalaryText,
                        onValueChange = { baseSalaryText = it },
                        label = { Text("الراتب الأساسي") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = housingText,
                        onValueChange = { housingText = it },
                        label = { Text("بدل السكن") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = transportText,
                        onValueChange = { transportText = it },
                        label = { Text("بدل النقل") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = otherAllowancesText,
                        onValueChange = { otherAllowancesText = it },
                        label = { Text("بدلات أخرى") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = deductionsText,
                    onValueChange = { deductionsText = it },
                    label = { Text("الاستقطاعات الثابتة (تأمينات / خصم شهري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Live Payroll Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEDF2F7))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي البدلات:", style = MaterialTheme.typography.bodySmall)
                            Text("${FinancialCalculator.formatMoney(totalAllowances)} ر.س", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي الراتب قبل الخصم:", style = MaterialTheme.typography.bodySmall)
                            Text("${FinancialCalculator.formatMoney(payrollPreview.totalEarnings)} ر.س", fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "صافي الراتب الشهري التقديري:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = HubNavy
                            )
                            Text(
                                "${FinancialCalculator.formatMoney(payrollPreview.netSalary)} ر.س",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = HubTeal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "يرجى إدخال اسم الموظف"
                        return@Button
                    }
                    if (jobTitle.isBlank()) {
                        errorMsg = "يرجى إدخال المسمى الوظيفي"
                        return@Button
                    }

                    val emp = (employeeToEdit ?: Employee()).copy(
                        name = name.trim(),
                        jobTitle = jobTitle.trim(),
                        department = department,
                        phone = phone.trim(),
                        email = email.trim(),
                        nationalId = nationalId.trim(),
                        hireDate = hireDate.trim().ifBlank { todayStr },
                        baseSalary = FinancialCalculator.round2(baseSalary),
                        housingAllowance = FinancialCalculator.round2(housing),
                        transportAllowance = FinancialCalculator.round2(transport),
                        otherAllowances = FinancialCalculator.round2(otherAllow),
                        defaultDeductions = FinancialCalculator.round2(deductions),
                        bankIban = bankIban.trim(),
                        status = status
                    )
                    onSave(emp)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HubNavy),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_employee_button")
            ) {
                Text("حفظ الموظف")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("إلغاء")
            }
        }
    )
}
