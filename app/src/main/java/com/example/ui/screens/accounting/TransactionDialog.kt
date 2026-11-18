package com.example.ui.screens.accounting

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
import androidx.compose.material.icons.filled.ReceiptLong
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessTransaction
import com.example.data.model.Client
import com.example.data.model.Product
import com.example.data.model.TransactionItem
import com.example.ui.theme.HubAmber
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal
import com.example.util.FinancialCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDialog(
    txToEdit: BusinessTransaction? = null,
    clients: List<Client>,
    products: List<Product> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (BusinessTransaction) -> Unit
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date()) }
    val timeSuffix = remember { SimpleDateFormat("yyMMdd-HHmm", Locale.ENGLISH).format(Date()) }

    var type by remember { mutableStateOf(txToEdit?.type ?: "مبيعات") }
    var invoiceNumber by remember {
        mutableStateOf(
            txToEdit?.invoiceNumber ?: "INV-$timeSuffix"
        )
    }
    var partyName by remember { mutableStateOf(txToEdit?.partyName ?: "") }
    var partyPhone by remember { mutableStateOf(txToEdit?.partyPhone ?: "") }
    var partyTaxNumber by remember { mutableStateOf(txToEdit?.partyTaxNumber ?: "") }
    var category by remember { mutableStateOf(txToEdit?.category ?: "بضاعة") }
    var date by remember { mutableStateOf(txToEdit?.date ?: todayStr) }
    var paymentMethod by remember { mutableStateOf(txToEdit?.paymentMethod ?: "تحويل بنكي") }
    var paymentStatus by remember { mutableStateOf(txToEdit?.paymentStatus ?: "مدفوع") }
    var taxMode by remember { mutableStateOf(txToEdit?.taxMode ?: "غير شامل الضريبة") }
    var taxRateText by remember {
        mutableStateOf(
            (txToEdit?.taxRate ?: 15.0).let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            }
        )
    }
    var notes by remember { mutableStateOf(txToEdit?.notes ?: "") }

    val itemsList = remember {
        mutableStateListOf<TransactionItem>().apply {
            if (txToEdit != null && txToEdit.items.isNotEmpty()) {
                addAll(txToEdit.getTypedItems())
            } else {
                add(TransactionItem(name = "", quantity = 1.0, unitPrice = 0.0, discount = 0.0))
            }
        }
    }

    var clientDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var paymentMethodDropdownExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val effectiveTaxRate = if (taxMode == "معفى") 0.0 else (taxRateText.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0)
    val totals = FinancialCalculator.calculateInvoiceTotals(
        items = itemsList,
        taxRate = effectiveTaxRate,
        taxMode = taxMode
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
                    text = if (txToEdit == null) "إضافة فاتورة / حركة مالية" else "تعديل الفاتورة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
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

                // Type Switcher: Sales vs Purchases
                Text(
                    text = "نوع العملية:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BusinessTransaction.TYPES.forEach { t ->
                        val selected = type == t
                        val bg = when {
                            selected && t == "مبيعات" -> HubTeal
                            selected && t == "مشتريات" -> HubAmber
                            else -> Color(0xFFF1F5F9)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .clickable {
                                    type = t
                                    if (txToEdit == null) {
                                        if (t == "مبيعات" && invoiceNumber.startsWith("PUR-")) {
                                            invoiceNumber = "INV-$timeSuffix"
                                        } else if (t == "مشتريات" && invoiceNumber.startsWith("INV-")) {
                                            invoiceNumber = "PUR-$timeSuffix"
                                        }
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (t == "مبيعات") "فاتورة مبيعات (إيراد)" else "فاتورة مشتريات (مصروف)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Invoice number & Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { if (it.length <= 100) invoiceNumber = it },
                        label = { Text("رقم الفاتورة *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("التاريخ") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Party Name with Client Dropdown for Sales
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = partyName,
                        onValueChange = { if (it.length <= 200) partyName = it },
                        label = { Text(if (type == "مبيعات") "اسم العميل *" else "اسم المورد / الجهة *") },
                        trailingIcon = {
                            if (type == "مبيعات" && clients.isNotEmpty()) {
                                IconButton(onClick = { clientDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "اختيار عميل")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = clientDropdownExpanded,
                        onDismissRequest = { clientDropdownExpanded = false }
                    ) {
                        clients.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.name} (${c.phone})") },
                                onClick = {
                                    partyName = c.name
                                    if (c.phone.isNotBlank()) partyPhone = c.phone
                                    clientDropdownExpanded = false
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
                        value = partyPhone,
                        onValueChange = { partyPhone = it },
                        label = { Text("رقم الهاتف") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = partyTaxNumber,
                        onValueChange = { partyTaxNumber = it },
                        label = { Text("الرقم الضريبي (اختياري)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category & Payment Method
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("التصنيف") },
                            trailingIcon = {
                                IconButton(onClick = { categoryDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "اختيار التصنيف")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            BusinessTransaction.CATEGORIES.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("طريقة الدفع") },
                            trailingIcon = {
                                IconButton(onClick = { paymentMethodDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "اختيار طريقة الدفع")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        DropdownMenu(
                            expanded = paymentMethodDropdownExpanded,
                            onDismissRequest = { paymentMethodDropdownExpanded = false }
                        ) {
                            BusinessTransaction.PAYMENT_METHODS.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        paymentMethod = method
                                        paymentMethodDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Payment Status Chips
                Text(
                    text = "حالة الدفع:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BusinessTransaction.PAYMENT_STATUSES.forEach { st ->
                        val selected = paymentStatus == st
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) HubNavy else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { paymentStatus = st }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = st,
                                    fontSize = 12.sp,
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

                // Tax Configuration Section
                Text(
                    text = "إعدادات الضريبة (VAT):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BusinessTransaction.TAX_MODES.forEach { mode ->
                        val selected = taxMode == mode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) HubTeal else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable {
                                taxMode = mode
                                if (mode == "معفى") {
                                    taxRateText = "0"
                                } else if (taxRateText == "0") {
                                    taxRateText = "15"
                                }
                            }
                        ) {
                            Text(
                                text = mode,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                if (taxMode != "معفى") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = taxRateText,
                            onValueChange = { input ->
                                val parsed = input.toDoubleOrNull()
                                if (input.isEmpty() || (parsed != null && parsed in 0.0..100.0)) {
                                    taxRateText = input
                                }
                            },
                            label = { Text("نسبة الضريبة %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        listOf("15", "5", "0").forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (taxRateText == preset) HubNavy else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable { taxRateText = preset }
                            ) {
                                Text(
                                    text = "$preset%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (taxRateText == preset) Color.White else HubNavy,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))

                // Line Items Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "بنود الفاتورة (${itemsList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HubNavy
                    )
                    Button(
                        onClick = {
                            itemsList.add(TransactionItem(name = "", quantity = 1.0, unitPrice = 0.0, discount = 0.0))
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
                    val lineCalc = FinancialCalculator.calculateLineItem(
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        discount = item.discount,
                        taxRate = effectiveTaxRate,
                        taxMode = taxMode
                    )
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
                                    text = "بند ${index + 1} • الإجمالي: ${FinancialCalculator.formatMoney(lineCalc.total)} ر.س",
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

                            Spacer(modifier = Modifier.height(6.dp))

                            var productPickerExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = item.name,
                                    onValueChange = { itemsList[index] = item.copy(name = it) },
                                    label = { Text("اسم الصنف / البيان *") },
                                    trailingIcon = {
                                        if (products.isNotEmpty()) {
                                            IconButton(onClick = { productPickerExpanded = true }) {
                                                Icon(
                                                    Icons.Default.ArrowDropDown,
                                                    contentDescription = "اختيار من المخزون"
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                DropdownMenu(
                                    expanded = productPickerExpanded,
                                    onDismissRequest = { productPickerExpanded = false }
                                ) {
                                    products.forEach { product ->
                                        val suggestedPrice = if (type == "مبيعات") {
                                            if (product.sellingPrice > 0.0) product.sellingPrice else product.unitCost
                                        } else {
                                            if (product.unitCost > 0.0) product.unitCost else product.sellingPrice
                                        }
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${product.name} (متوفر: ${product.formattedQuantity()} ${product.unit})",
                                                    color = if (product.isLowStock()) Color(0xFFB91C1C) else HubNavy
                                                )
                                            },
                                            onClick = {
                                                itemsList[index] = item.copy(
                                                    name = product.name,
                                                    unitPrice = if (suggestedPrice > 0.0) suggestedPrice else item.unitPrice
                                                )
                                                productPickerExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = if (item.quantity > 0.0) {
                                        if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                                    } else "",
                                    onValueChange = { txt ->
                                        val q = txt.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                        itemsList[index] = item.copy(quantity = q)
                                    },
                                    label = { Text("الكمية") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = if (item.unitPrice > 0.0) item.unitPrice.toString() else "",
                                    onValueChange = { txt ->
                                        val p = txt.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                        itemsList[index] = item.copy(unitPrice = p)
                                    },
                                    label = { Text("السعر") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = if (item.discount > 0.0) item.discount.toString() else "",
                                    onValueChange = { txt ->
                                        val d = txt.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                        itemsList[index] = item.copy(discount = d)
                                    },
                                    label = { Text("الخصم") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Financial Precision Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEDF2F7))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SummaryRow("الصافي قبل الضريبة:", "${FinancialCalculator.formatMoney(totals.subtotal)} ر.س")
                        if (totals.discountTotal > 0.0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            SummaryRow("إجمالي الخصومات:", "- ${FinancialCalculator.formatMoney(totals.discountTotal)} ر.س", Color(0xFFB45309))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        SummaryRow(
                            "ضريبة القيمة المضافة (${effectiveTaxRate}%):",
                            "${FinancialCalculator.formatMoney(totals.taxAmount)} ر.س",
                            HubTeal
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "الإجمالي النهائي:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HubNavy
                            )
                            Text(
                                text = "${FinancialCalculator.formatMoney(totals.grandTotal)} ر.س",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HubNavy
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات الفاتورة") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (invoiceNumber.isBlank()) {
                        errorMsg = "يرجى إدخال رقم الفاتورة"
                        return@Button
                    }
                    if (partyName.isBlank()) {
                        errorMsg = if (type == "مبيعات") "يرجى إدخال اسم العميل" else "يرجى إدخال اسم المورد"
                        return@Button
                    }
                    val validItems = itemsList.filter { it.name.isNotBlank() }
                    if (validItems.isEmpty()) {
                        errorMsg = "يرجى إدخال اسم بند واحد على الأقل"
                        return@Button
                    }

                    val recalculatedItems = validItems.map { it.recalculated(effectiveTaxRate, taxMode) }
                    val finalTotals = FinancialCalculator.calculateInvoiceTotals(
                        items = recalculatedItems,
                        taxRate = effectiveTaxRate,
                        taxMode = taxMode
                    )

                    val tx = (txToEdit ?: BusinessTransaction()).copy(
                        type = type,
                        invoiceNumber = invoiceNumber.trim(),
                        partyName = partyName.trim(),
                        partyPhone = partyPhone.trim(),
                        partyTaxNumber = partyTaxNumber.trim(),
                        category = category,
                        date = date.trim().ifBlank { todayStr },
                        paymentMethod = paymentMethod,
                        paymentStatus = paymentStatus,
                        taxMode = taxMode,
                        taxRate = effectiveTaxRate,
                        items = recalculatedItems.map { it.toMap() },
                        subtotal = finalTotals.subtotal,
                        discountTotal = finalTotals.discountTotal,
                        taxAmount = finalTotals.taxAmount,
                        grandTotal = finalTotals.grandTotal,
                        notes = notes.trim()
                    )
                    onSave(tx)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HubNavy),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_transaction_button")
            ) {
                Text("حفظ الفاتورة")
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

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF1E293B)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
