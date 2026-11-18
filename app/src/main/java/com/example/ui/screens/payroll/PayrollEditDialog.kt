package com.example.ui.screens.payroll

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.data.model.PayrollRecord
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal
import com.example.util.FinancialCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PayrollEditDialog(
    record: PayrollRecord,
    onDismiss: () -> Unit,
    onSave: (PayrollRecord) -> Unit
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date()) }

    var baseSalaryText by remember { mutableStateOf(record.baseSalary.toString()) }
    var allowancesText by remember { mutableStateOf(record.allowances.toString()) }
    var bonusesText by remember { mutableStateOf(if (record.bonuses > 0) record.bonuses.toString() else "0") }
    var deductionsText by remember { mutableStateOf(if (record.deductions > 0) record.deductions.toString() else "0") }
    var paymentStatus by remember { mutableStateOf(record.paymentStatus) }
    var paymentDate by remember { mutableStateOf(record.paymentDate) }
    var notes by remember { mutableStateOf(record.notes) }

    val baseSalary = baseSalaryText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val allowances = allowancesText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val bonuses = bonusesText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val deductions = deductionsText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    val calc = FinancialCalculator.calculatePayroll(baseSalary, allowances, bonuses, deductions)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "تعديل مسير راتب: ${record.employeeName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Text(
                    text = "${record.jobTitle} • شهر ${record.monthYear}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = baseSalaryText,
                        onValueChange = { baseSalaryText = it },
                        label = { Text("الأساسي") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = allowancesText,
                        onValueChange = { allowancesText = it },
                        label = { Text("البدلات") },
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
                        value = bonusesText,
                        onValueChange = { bonusesText = it },
                        label = { Text("المكافآت / الإضافي") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = deductionsText,
                        onValueChange = { deductionsText = it },
                        label = { Text("الخصومات / السلف") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "حالة الصرف:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PayrollRecord.PAYMENT_STATUSES.forEach { st ->
                        val selected = paymentStatus == st
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) HubNavy else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    paymentStatus = st
                                    if (st == "مدفوع" && paymentDate.isBlank()) {
                                        paymentDate = todayStr
                                    }
                                }
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

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = paymentDate,
                    onValueChange = { paymentDate = it },
                    label = { Text("تاريخ الصرف") },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات المسير") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                            Text("إجمالي الاستحقاقات:", style = MaterialTheme.typography.bodySmall)
                            Text("${FinancialCalculator.formatMoney(calc.totalEarnings)} ر.س", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي الخصومات:", style = MaterialTheme.typography.bodySmall)
                            Text("- ${FinancialCalculator.formatMoney(calc.totalDeductions)} ر.س", fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("صافي الراتب المستحق:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = HubNavy)
                            Text("${FinancialCalculator.formatMoney(calc.netSalary)} ر.س", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = HubTeal)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = record.copy(
                        baseSalary = FinancialCalculator.round2(baseSalary),
                        allowances = FinancialCalculator.round2(allowances),
                        bonuses = FinancialCalculator.round2(bonuses),
                        deductions = FinancialCalculator.round2(deductions),
                        netSalary = calc.netSalary,
                        paymentStatus = paymentStatus,
                        paymentDate = paymentDate.trim(),
                        notes = notes.trim()
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HubNavy),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_payroll_button")
            ) {
                Text("حفظ التعديل")
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
