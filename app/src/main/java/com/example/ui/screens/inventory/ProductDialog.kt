package com.example.ui.screens.inventory

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.Product
import com.example.ui.theme.HubAmber
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal

@Composable
fun ProductDialog(
    productToEdit: Product? = null,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var sku by remember { mutableStateOf(productToEdit?.sku ?: "") }
    var category by remember { mutableStateOf(productToEdit?.category ?: Product.CATEGORIES.first()) }
    var unit by remember { mutableStateOf(productToEdit?.unit ?: Product.UNITS.first()) }
    var quantityText by remember {
        mutableStateOf(
            productToEdit?.let { Product.formatNumber(it.quantity) } ?: "10"
        )
    }
    var minThresholdText by remember {
        mutableStateOf(
            productToEdit?.let { Product.formatNumber(it.minThreshold) } ?: "5"
        )
    }
    var unitCostText by remember {
        mutableStateOf(
            productToEdit?.takeIf { it.unitCost > 0.0 }?.let { Product.formatNumber(it.unitCost) } ?: ""
        )
    }
    var sellingPriceText by remember {
        mutableStateOf(
            productToEdit?.takeIf { it.sellingPrice > 0.0 }?.let { Product.formatNumber(it.sellingPrice) } ?: ""
        )
    }
    var supplierName by remember { mutableStateOf(productToEdit?.supplierName ?: "") }
    var supplierPhone by remember { mutableStateOf(productToEdit?.supplierPhone ?: "") }
    var alertsEnabled by remember { mutableStateOf(productToEdit?.alertsEnabled ?: true) }
    var notes by remember { mutableStateOf(productToEdit?.notes ?: "") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val parsedQty = quantityText.toDoubleOrNull() ?: 0.0
    val parsedThreshold = minThresholdText.toDoubleOrNull() ?: 0.0
    val previewLowStock = alertsEnabled && parsedQty <= parsedThreshold

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (productToEdit == null) "إضافة منتج للمخزون" else "تعديل بيانات المنتج",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Icon(
                    imageVector = Icons.Default.Inventory2,
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
                    label = { Text("اسم المنتج / الصنف *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { if (it.length <= 80) sku = it },
                        label = { Text("رمز SKU / باركود") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("الوحدة") },
                            trailingIcon = {
                                IconButton(onClick = { unitExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "اختيار الوحدة")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        DropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            Product.UNITS.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("التصنيف") },
                        trailingIcon = {
                            IconButton(onClick = { categoryExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "اختيار التصنيف")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        Product.CATEGORIES.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = {
                                    category = c
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "الكمية وحد التنبيه التلقائي:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("الكمية الحالية *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_quantity_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minThresholdText,
                        onValueChange = { minThresholdText = it },
                        label = { Text("حد التنبيه الأدنى *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_threshold_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (previewLowStock) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (alertsEnabled) HubAmber else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "تنبيه انخفاض المخزون",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = HubNavy
                                )
                                Text(
                                    text = if (previewLowStock) {
                                        "سيرسل النظام إشعاراً فورياً لأن الكمية ≤ الحد الأدنى"
                                    } else {
                                        "يرسل إشعاراً تلقائياً عند وصول الكمية إلى ${Product.formatNumber(parsedThreshold)} أو أقل"
                                    },
                                    fontSize = 10.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                        Switch(
                            checked = alertsEnabled,
                            onCheckedChange = { alertsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = HubTeal
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = unitCostText,
                        onValueChange = { unitCostText = it },
                        label = { Text("تكلفة الشراء (ر.س)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = { sellingPriceText = it },
                        label = { Text("سعر البيع (ر.س)") },
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
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text("اسم المورد (للتوريد)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = supplierPhone,
                        onValueChange = { supplierPhone = it },
                        label = { Text("هاتف المورد") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات الصنف") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isEmpty()) {
                        errorMsg = "يرجى إدخال اسم المنتج"
                        return@Button
                    }
                    val qty = quantityText.toDoubleOrNull()
                    if (qty == null || qty < 0.0) {
                        errorMsg = "يرجى إدخال كمية صالحة (0 أو أكثر)"
                        return@Button
                    }
                    val threshold = minThresholdText.toDoubleOrNull()
                    if (threshold == null || threshold < 0.0) {
                        errorMsg = "يرجى إدخال حد تنبيه صالح (0 أو أكثر)"
                        return@Button
                    }
                    val cost = unitCostText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val price = sellingPriceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

                    val saved = (productToEdit ?: Product()).copy(
                        name = trimmedName,
                        sku = sku.trim(),
                        category = category,
                        unit = unit,
                        quantity = qty,
                        minThreshold = threshold,
                        unitCost = cost,
                        sellingPrice = price,
                        supplierName = supplierName.trim(),
                        supplierPhone = supplierPhone.trim(),
                        alertsEnabled = alertsEnabled,
                        notes = notes.trim()
                    )
                    onSave(saved)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HubNavy),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("حفظ المنتج")
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
