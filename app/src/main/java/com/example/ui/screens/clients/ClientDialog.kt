package com.example.ui.screens.clients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Client
import com.example.ui.theme.HubNavy

@Composable
fun ClientDialog(
    clientToEdit: Client? = null,
    onDismiss: () -> Unit,
    onSave: (Client) -> Unit
) {
    var name by remember { mutableStateOf(clientToEdit?.name ?: "") }
    var phone by remember { mutableStateOf(clientToEdit?.phone ?: "") }
    var email by remember { mutableStateOf(clientToEdit?.email ?: "") }
    var source by remember { mutableStateOf(clientToEdit?.source ?: "حراج") }
    var status by remember { mutableStateOf(clientToEdit?.status ?: "جديد") }
    var notes by remember { mutableStateOf(clientToEdit?.notes ?: "") }
    var dealValueText by remember { mutableStateOf(if ((clientToEdit?.dealValue ?: 0.0) > 0) clientToEdit?.dealValue.toString() else "") }
    var lastContactDate by remember { mutableStateOf(clientToEdit?.lastContactDate ?: "") }

    var isSourceMenuExpanded by remember { mutableStateOf(false) }
    var isStatusMenuExpanded by remember { mutableStateOf(false) }

    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (clientToEdit == null) "إضافة عميل جديد" else "تعديل بيانات العميل",
                style = MaterialTheme.typography.titleLarge,
                color = HubNavy,
                fontWeight = FontWeight.Bold
            )
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
                    onValueChange = { name = it },
                    label = { Text("اسم العميل *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف / واتساب *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_phone_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Source selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = source,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("مصدر العميل") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "قائمة المصادر")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSourceMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = isSourceMenuExpanded,
                        onDismissRequest = { isSourceMenuExpanded = false }
                    ) {
                        Client.SOURCES.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    source = item
                                    isSourceMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("حالة مسار البيع") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "قائمة الحالات")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isStatusMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = isStatusMenuExpanded,
                        onDismissRequest = { isStatusMenuExpanded = false }
                    ) {
                        Client.STATUSES.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    status = item
                                    isStatusMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dealValueText,
                    onValueChange = { dealValueText = it },
                    label = { Text("قيمة الصفقة التقديرية (ريال)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات وتفاصيل الاحتياج") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "يرجى إدخال اسم العميل"
                        return@Button
                    }
                    if (phone.isBlank()) {
                        errorMsg = "يرجى إدخال رقم الهاتف"
                        return@Button
                    }
                    val parsedDeal = dealValueText.toDoubleOrNull() ?: 0.0
                    val updated = (clientToEdit ?: Client()).copy(
                        name = name.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        source = source,
                        status = status,
                        dealValue = parsedDeal,
                        notes = notes.trim(),
                        lastContactDate = lastContactDate
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HubNavy),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_client_button")
            ) {
                Text("حفظ")
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
