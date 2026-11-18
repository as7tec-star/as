package com.example.ui.screens.inventory

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.theme.HubAmber
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal
import com.example.util.ExcelCell
import com.example.util.ExcelCsvUtils
import com.example.util.FinancialCalculator
import com.example.util.ImportResult
import com.example.util.IntentUtils
import com.example.util.StockAlertNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun InventoryScreen(
    products: List<Product>,
    isLoading: Boolean,
    onAddProduct: (Product) -> Unit,
    onUpdateProduct: (Product) -> Unit,
    onAdjustQuantity: (String, Double) -> Unit,
    onToggleAlerts: (String, Boolean) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onImportProductsBatch: suspend (List<Product>) -> Int,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasNotifPermission by remember {
        mutableStateOf(StockAlertNotifier.hasNotificationPermission(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted || StockAlertNotifier.hasNotificationPermission(context)
        if (granted) {
            val lows = products.filter { it.isLowStock() }
            if (lows.isNotEmpty()) {
                StockAlertNotifier.notifyLowStockProducts(context, lows)
                onShowMessage("تم تفعيل إشعارات انخفاض المخزون وإرسال تنبيه فوري")
            } else {
                onShowMessage("تم تفعيل إشعارات انخفاض المخزون بنجاح")
            }
        } else {
            onShowMessage("تم رفض إذن الإشعارات — ستبقى التنبيهات ظاهرة داخل التطبيق")
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    // Excel/CSV Export & Import
    var pendingExportTable by remember { mutableStateOf<Pair<List<String>, List<List<ExcelCell>>>?>(null) }
    var importPreview by remember { mutableStateOf<ImportResult<Product>?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    val exportXlsxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        val table = pendingExportTable
        if (uri != null && table != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            ExcelCsvUtils.writeXlsx(out, "المخزون", table.first, table.second)
                        }
                    }
                    onShowMessage("تم تصدير جدول المخزون إلى Excel بنجاح")
                } catch (e: Exception) {
                    onShowMessage("فشل التصدير: ${e.message}")
                }
            }
        }
    }

    val importProductsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val (format, rows) = withContext(Dispatchers.IO) {
                        ExcelCsvUtils.readRowsFromUri(context, uri)
                    }
                    val parsed = ExcelCsvUtils.parseProductsFromRows(rows, format)
                    if (parsed.items.isEmpty()) {
                        onShowMessage("لم يتم العثور على منتجات صالحة في الملف")
                    } else {
                        importPreview = parsed
                    }
                } catch (e: Exception) {
                    onShowMessage("تعذر قراءة الملف: ${e.message}")
                }
            }
        }
    }

    val lowStockProducts = remember(products) {
        products.filter { it.isLowStock() }
    }
    val outOfStockCount = remember(products) {
        products.count { it.isOutOfStock() }
    }
    val inventoryValue = remember(products) {
        products.sumOf { p ->
            val price = if (p.unitCost > 0.0) p.unitCost else p.sellingPrice
            p.quantity * price
        }
    }

    val filteredProducts = remember(products, searchQuery, selectedFilter) {
        products.filter { p ->
            val matchesQuery = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.sku.contains(searchQuery, ignoreCase = true) ||
                p.supplierName.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "تنبيهات النقص" -> p.isLowStock()
                "نفد المخزون" -> p.isOutOfStock()
                "متوفر" -> !p.isLowStock() && !p.isOutOfStock()
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            // Inventory KPI Summary & Permission Header
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InventoryKpiCard(
                            label = "إجمالي المنتجات",
                            value = "${products.size}",
                            accent = HubNavy,
                            modifier = Modifier.weight(1f)
                        )
                        InventoryKpiCard(
                            label = "تنبيهات النقص",
                            value = "${lowStockProducts.size}",
                            accent = if (lowStockProducts.isNotEmpty()) Color(0xFFB91C1C) else HubTeal,
                            modifier = Modifier.weight(1f)
                        )
                        InventoryKpiCard(
                            label = "قيمة المخزون",
                            value = "${FinancialCalculator.formatMoney(inventoryValue)} ر.س",
                            accent = HubTeal,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Android 13+ Notification Permission or Manual Trigger Bar
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color(0xFFB45309),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "فعّل إشعارات النظام ليصلك تنبيه تلقائي فور نقص أي منتج",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF78350F)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = HubNavy),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("enable_stock_notifications_button")
                                ) {
                                    Text("تفعيل", fontSize = 11.sp)
                                }
                            }
                        }
                    } else if (lowStockProducts.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = Color(0xFFB91C1C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تنبيه: ${lowStockProducts.size} منتجات تحت الحد الأدنى (${outOfStockCount} نفد)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF991B1B)
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        val sent = StockAlertNotifier.notifyLowStockProducts(context, lowStockProducts)
                                        if (sent) {
                                            onShowMessage("تم إرسال إشعار التنبيه إلى شريط إشعارات الجهاز")
                                        } else {
                                            onShowMessage("تحقق من تفعيل إذن الإشعارات من إعدادات الجهاز")
                                        }
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("trigger_stock_notification_button")
                                ) {
                                    Text("إرسال إشعار", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                                }
                            }
                        }
                    }
                }
            }

            // Search, Filter Chips & Excel Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث باسم المنتج أو SKU أو المورد...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filters = listOf(
                        "الكل" to products.size,
                        "تنبيهات النقص" to lowStockProducts.size,
                        "نفد المخزون" to outOfStockCount,
                        "متوفر" to products.count { !it.isLowStock() && !it.isOutOfStock() }
                    )
                    filters.forEach { (label, count) ->
                        val selected = selectedFilter == label
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) HubNavy else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .clickable { selectedFilter = label }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$label ($count)",
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            pendingExportTable = ExcelCsvUtils.productsTable(products)
                            exportXlsxLauncher.launch("جرد_المخزون.xlsx")
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "تصدير Excel", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { importProductsLauncher.launch(arrayOf("*/*")) },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "استيراد", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("استيراد", fontSize = 11.sp)
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = HubNavy)
                }
            } else if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (products.isEmpty()) "لا توجد منتجات في المخزون بعد" else "لا توجد منتجات مطابقة للتصفية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HubNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "أضف منتجاتك وحدد حد التنبيه الأدنى ليتم إشعارك تلقائياً عند نقص الكمية أو البيع",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onDecrement = {
                                val next = (product.quantity - 1.0).coerceAtLeast(0.0)
                                onAdjustQuantity(product.id, next)
                            },
                            onIncrement = { delta ->
                                onAdjustQuantity(product.id, product.quantity + delta)
                            },
                            onToggleAlerts = { enabled ->
                                onToggleAlerts(product.id, enabled)
                            },
                            onOrderSupplier = {
                                val msg = StockAlertNotifier.buildLowStockWhatsAppOrder(product)
                                IntentUtils.openWhatsApp(context, product.supplierPhone, msg)
                            },
                            onEdit = { productToEdit = product },
                            onDelete = { productToDelete = product }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(76.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_product_button"),
            containerColor = HubNavy,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة منتج")
        }
    }

    if (showAddDialog) {
        ProductDialog(
            productToEdit = null,
            onDismiss = { showAddDialog = false },
            onSave = { newProduct ->
                showAddDialog = false
                onAddProduct(newProduct)
            }
        )
    }

    if (productToEdit != null) {
        ProductDialog(
            productToEdit = productToEdit,
            onDismiss = { productToEdit = null },
            onSave = { updated ->
                productToEdit = null
                onUpdateProduct(updated)
            }
        )
    }

    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("تأكيد حذف المنتج", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف المنتج '${productToDelete?.name}' من المخزون؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        productToDelete?.id?.let { onDeleteProduct(it) }
                        productToDelete = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    val preview = importPreview
    if (preview != null) {
        AlertDialog(
            onDismissRequest = { if (!isImporting) importPreview = null },
            title = { Text("معاينة استيراد المخزون", fontWeight = FontWeight.Bold, color = HubNavy) },
            text = {
                Column {
                    Text("الصيغة المكتشفة: ${preview.detectedFormat}", fontWeight = FontWeight.SemiBold)
                    Text("عدد المنتجات الجاهزة: ${preview.items.size}")
                    if (preview.skippedRows > 0) {
                        Text("صفوف فارغة تم تخطيها: ${preview.skippedRows}", color = Color(0xFFB45309))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    preview.items.take(4).forEach { p ->
                        Text(
                            text = "• ${p.name} (الكمية: ${p.formattedQuantity()} | الحد: ${p.formattedThreshold()})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isImporting,
                    onClick = {
                        isImporting = true
                        scope.launch {
                            try {
                                val count = onImportProductsBatch(preview.items)
                                importPreview = null
                                onShowMessage("تم استيراد $count منتج للمخزون بنجاح")
                            } catch (e: Exception) {
                                onShowMessage("فشل الاستيراد: ${e.message}")
                            } finally {
                                isImporting = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HubNavy)
                ) {
                    Text(if (isImporting) "جارٍ الاستيراد..." else "تأكيد الاستيراد")
                }
            },
            dismissButton = {
                TextButton(enabled = !isImporting, onClick = { importPreview = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun InventoryKpiCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF475569),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onDecrement: () -> Unit,
    onIncrement: (Double) -> Unit,
    onToggleAlerts: (Boolean) -> Unit,
    onOrderSupplier: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isOut = product.isOutOfStock()
    val isLow = product.isLowStock()

    val statusBg = when {
        isOut -> Color(0xFFFEE2E2)
        isLow -> Color(0xFFFEF3C7)
        else -> Color(0xFFDCFCE7)
    }
    val statusColor = when {
        isOut -> Color(0xFFB91C1C)
        isLow -> Color(0xFFB45309)
        else -> Color(0xFF15803D)
    }
    val barColor = when {
        isOut -> Color(0xFFDC2626)
        isLow -> HubAmber
        else -> HubTeal
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HubNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(product.category)
                            if (product.sku.isNotBlank()) append(" • SKU: ${product.sku}")
                            if (product.sellingPrice > 0.0) append(" • البيع: ${FinancialCalculator.formatMoney(product.sellingPrice)} ر.س")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = product.stockStatusLabel(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stock progress bar & threshold indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المتوفر: ${product.formattedQuantity()} ${product.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOut || isLow) statusColor else HubNavy
                )
                Text(
                    text = "حد التنبيه: ${product.formattedThreshold()} ${product.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { product.stockRatio() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick quantity adjustment controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(enabled = product.quantity > 0.0) { onDecrement() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "إنقاص الكمية بمقدار 1",
                                tint = if (product.quantity > 0.0) HubNavy else Color(0xFFCBD5E1)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HubTeal.copy(alpha = 0.12f),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onIncrement(1.0) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "زيادة الكمية بمقدار 1",
                                tint = HubTeal
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HubNavy.copy(alpha = 0.08f),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clickable { onIncrement(10.0) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "+10 توريد",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HubNavy
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onToggleAlerts(!product.alertsEnabled) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (product.alertsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = if (product.alertsEnabled) "إيقاف التنبيه التلقائي" else "تفعيل التنبيه التلقائي",
                            tint = if (product.alertsEnabled) HubAmber else Color(0xFF94A3B8)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل المنتج", tint = Color(0xFF64748B))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف المنتج", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (isLow || isOut) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (product.supplierName.isNotBlank()) "المورد: ${product.supplierName}" else "يحتاج إعادة توريد عاجل",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Button(
                        onClick = onOrderSupplier,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E7E34)),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("reorder_supplier_button")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طلب توريد واتساب", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
