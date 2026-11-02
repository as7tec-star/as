package com.example.ui.screens.quotes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.data.model.Quote
import com.example.data.model.QuoteItem
import com.example.ui.theme.HubAmber
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal
import com.example.util.GoogleSheetsUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QuoteDialog(
    quoteToEdit: Quote? = null,
    clients: List<Client>,
    onDismiss: () -> Unit,
    onSaveQuote: (Quote) -> Unit
) {
    val context = LocalContext.current
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date()) }

    var quoteNumber by remember {
        mutableStateOf(quoteToEdit?.quoteNumber ?: "Q-${SimpleDateFormat("yyMMdd-HHmm", Locale.ENGLISH).format(Date())}")
    }
    var selectedClient by remember {
        mutableStateOf(clients.find { it.id == quoteToEdit?.clientId } ?: clients.firstOrNull())
    }
    var clientName by remember { mutableStateOf(quoteToEdit?.clientName ?: selectedClient?.name ?: "") }
    var clientPhone by remember { mutableStateOf(quoteToEdit?.clientPhone ?: selectedClient?.phone ?: "") }
    var date by remember { mutableStateOf(quoteToEdit?.date ?: todayStr) }
    var expiryDate by remember { mutableStateOf(quoteToEdit?.expiryDate ?: "") }
    var status by remember { mutableStateOf(quoteToEdit?.status ?: "مسودة") }
    var notes by remember {
        mutableStateOf(
            quoteToEdit?.notes ?: "الشروط: الدفع 50% مقدماً و50% عند التسليم. مدة التوريد خلال 3 أيام عمل."
        )
    }
    var googleSheetUrl by remember { mutableStateOf(quoteToEdit?.googleSheetUrl ?: "") }
    var taxRate by remember { mutableDoubleStateOf(quoteToEdit?.taxRate ?: 15.0) }

    // Line items list
    val itemsList = remember {
        mutableStateListOf<QuoteItem>().apply {
            if (quoteToEdit != null) {
                addAll(quoteToEdit.getTypedItems())
            } else {
                // start with one default line item
                add(QuoteItem(name = "", quantity = 1.0, unitPrice = 0.0, total = 0.0))
            }
        }
    }

    var isClientDropdownExpanded by remember { mutableStateOf(false) }
    var isStatusDropdownExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Dynamic calculations
    val subtotal = itemsList.sumOf { it.total }
    val taxAmount = (subtotal * taxRate) / 100.0
    val grandTotal = subtotal + taxAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (quoteToEdit == null) "إنشاء عرض سعر جديد" else "تعديل عرض السعر",
                    style = MaterialTheme.typography.titleLarge,
                    color = HubNavy,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Receipt,
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

                // 1) Templates selector bar
                Text(
                    text = "قوالب عروض أسعار جاهزة للتعبئة السريعة:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Quote.getPrebuiltTemplates().forEach { (templateName, templateItems) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HubNavy.copy(alpha = 0.08f),
                            modifier = Modifier.clickable {
                                itemsList.clear()
                                itemsList.addAll(templateItems)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, tint = HubNavy, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = templateName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HubNavy
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                // 2) Quote Information
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quoteNumber,
                        onValueChange = { quoteNumber = it },
                        label = { Text("رقم العرض *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("التاريخ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Client Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("اسم العميل *") },
                        trailingIcon = {
                            if (clients.isNotEmpty()) {
                                IconButton(onClick = { isClientDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "اختيار عميل")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    DropdownMenu(
                        expanded = isClientDropdownExpanded,
                        onDismissRequest = { isClientDropdownExpanded = false }
                    ) {
                        clients.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.name} (${c.phone})") },
                                onClick = {
                                    selectedClient = c
                                    clientName = c.name
                                    clientPhone = c.phone
                                    isClientDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("رقم الهاتف") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = { Text("صلاحية العرض حتى") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                // 3) Line Items Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "جدول البنود والأسعار (${itemsList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HubNavy
                    )

                    Button(
                        onClick = {
                            itemsList.add(QuoteItem(name = "", quantity = 1.0, unitPrice = 0.0, total = 0.0))
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HubTeal),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة بند", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                itemsList.forEachIndexed { index, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "بند ${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HubNavy
                                )
                                if (itemsList.size > 1) {
                                    IconButton(
                                        onClick = { itemsList.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "حذف البند",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = item.name,
                                onValueChange = { newName ->
                                    itemsList[index] = item.copy(name = newName)
                                },
                                label = { Text("اسم البند / الخدمة *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = item.description,
                                onValueChange = { newDesc ->
                                    itemsList[index] = item.copy(description = newDesc)
                                },
                                label = { Text("الوصف / المواصفات") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = if (item.quantity > 0) item.quantity.toString() else "",
                                    onValueChange = { qtyText ->
                                        val qty = qtyText.toDoubleOrNull() ?: 1.0
                                        val tot = qty * item.unitPrice
                                        itemsList[index] = item.copy(quantity = qty, total = tot)
                                    },
                                    label = { Text("الكمية") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = if (item.unitPrice > 0) item.unitPrice.toString() else "",
                                    onValueChange = { priceText ->
                                        val price = priceText.toDoubleOrNull() ?: 0.0
                                        val tot = item.quantity * price
                                        itemsList[index] = item.copy(unitPrice = price, total = tot)
                                    },
                                    label = { Text("سعر الوحدة") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = "${item.total}",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("المجموع") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4) Financial Totals
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
                            Text("المجموع الفرعي:", style = MaterialTheme.typography.bodyMedium)
                            Text("${subtotal} ر.س", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ضريبة القيمة المضافة (15%):", style = MaterialTheme.typography.bodyMedium)
                            Text("${taxAmount} ر.س", fontWeight = FontWeight.Bold, color = HubTeal)
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الإجمالي الكلي المستحق:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HubNavy)
                            Text("${grandTotal} ر.س", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HubNavy)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5) Terms and Google Sheets Link
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("الشروط والملاحظات وطريقة الدفع") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Google Sheets URL & Create New Sheet button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التكامل مع Google Sheets:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = HubNavy
                    )

                    TextButton(
                        onClick = { GoogleSheetsUtils.openGoogleSheet(context, "") }
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إنشاء Sheet جديد بـ Google", fontSize = 11.sp, color = HubTeal)
                    }
                }

                OutlinedTextField(
                    value = googleSheetUrl,
                    onValueChange = { googleSheetUrl = it },
                    label = { Text("رابط Google Sheet الخاص بعرض السعر") },
                    placeholder = { Text("https://docs.google.com/spreadsheets/...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isBlank()) {
                        errorMsg = "يرجى إدخال اسم العميل"
                        return@Button
                    }
                    if (itemsList.isEmpty() || itemsList.all { it.name.isBlank() }) {
                        errorMsg = "يرجى إضافة بند واحد على الأقل مع الاسم"
                        return@Button
                    }
                    val validItems = itemsList.filter { it.name.isNotBlank() }.map { it.toMap() }
                    val quote = (quoteToEdit ?: Quote()).copy(
                        quoteNumber = quoteNumber.trim(),
                        clientId = selectedClient?.id ?: "",
                        clientName = clientName.trim(),
                        clientPhone = clientPhone.trim(),
                        date = date.trim(),
                        expiryDate = expiryDate.trim(),
                        status = status,
                        items = validItems,
                        subtotal = subtotal,
                        taxRate = taxRate,
                        taxAmount = taxAmount,
                        grandTotal = grandTotal,
                        notes = notes.trim(),
                        googleSheetUrl = googleSheetUrl.trim()
                    )
                    onSaveQuote(quote)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HubNavy),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_quote_button")
            ) {
                Text("حفظ عرض السعر")
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
