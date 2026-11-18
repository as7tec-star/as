package com.example.ui.screens.accounting

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessTransaction
import com.example.data.model.Client
import com.example.data.model.Product
import com.example.ui.theme.HubAmber
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal
import com.example.util.ExcelCell
import com.example.util.ExcelCsvUtils
import com.example.util.FinancialCalculator
import com.example.util.GoogleSheetsUtils
import com.example.util.ImportResult
import com.example.util.IntentUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AccountingImportTarget {
    TRANSACTIONS, CLIENTS
}

private sealed class PendingImportPreview {
    data class Transactions(val result: ImportResult<BusinessTransaction>) : PendingImportPreview()
    data class Clients(val result: ImportResult<Client>) : PendingImportPreview()
}

@Composable
fun AccountingScreen(
    transactions: List<BusinessTransaction>,
    clients: List<Client>,
    products: List<Product> = emptyList(),
    isLoading: Boolean,
    onAddTransaction: (BusinessTransaction) -> Unit,
    onUpdateTransaction: (BusinessTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onUpdatePaymentStatus: (String, String) -> Unit,
    onImportTransactionsBatch: suspend (List<BusinessTransaction>) -> Int,
    onImportClientsBatch: suspend (List<Client>) -> Int,
    onOpenInventory: () -> Unit = {},
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedSubTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("الكل") }
    var statusFilter by remember { mutableStateOf("الكل") }

    var showAddDialog by remember { mutableStateOf(false) }
    var txToEdit by remember { mutableStateOf<BusinessTransaction?>(null) }
    var txToDelete by remember { mutableStateOf<BusinessTransaction?>(null) }

    // SAF Export & Import state
    var pendingExportSheetName by remember { mutableStateOf("Sheet1") }
    var pendingExportTable by remember { mutableStateOf<Pair<List<String>, List<List<ExcelCell>>>?>(null) }
    var pendingImportTarget by remember { mutableStateOf(AccountingImportTarget.TRANSACTIONS) }
    var importPreview by remember { mutableStateOf<PendingImportPreview?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    val createXlsxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        val table = pendingExportTable
        if (uri != null && table != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            ExcelCsvUtils.writeXlsx(out, pendingExportSheetName, table.first, table.second)
                        }
                    }
                    onShowMessage("تم تصدير ملف Excel (.xlsx) بنجاح")
                } catch (e: Exception) {
                    onShowMessage("فشل تصدير ملف Excel: ${e.message}")
                }
            }
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val table = pendingExportTable
        if (uri != null && table != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            ExcelCsvUtils.writeCsv(out, table.first, table.second)
                        }
                    }
                    onShowMessage("تم تصدير ملف CSV بنجاح (متوافق مع Excel و Google Sheets)")
                } catch (e: Exception) {
                    onShowMessage("فشل تصدير CSV: ${e.message}")
                }
            }
        }
    }

    val openImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val (format, rows) = withContext(Dispatchers.IO) {
                        ExcelCsvUtils.readRowsFromUri(context, uri)
                    }
                    when (pendingImportTarget) {
                        AccountingImportTarget.TRANSACTIONS -> {
                            val parsed = ExcelCsvUtils.parseTransactionsFromRows(rows, format)
                            if (parsed.items.isEmpty()) {
                                onShowMessage("لم يتم العثور على فواتير صالحة في الملف. تأكد من استخدام القالب المعتمد.")
                            } else {
                                importPreview = PendingImportPreview.Transactions(parsed)
                            }
                        }
                        AccountingImportTarget.CLIENTS -> {
                            val parsed = ExcelCsvUtils.parseClientsFromRows(rows, format)
                            if (parsed.items.isEmpty()) {
                                onShowMessage("لم يتم العثور على بيانات عملاء صالحة في الملف.")
                            } else {
                                importPreview = PendingImportPreview.Clients(parsed)
                            }
                        }
                    }
                } catch (e: Exception) {
                    onShowMessage("تعذر قراءة الملف: ${e.message}")
                }
            }
        }
    }

    fun triggerXlsxExport(fileName: String, sheetName: String, table: Pair<List<String>, List<List<ExcelCell>>>) {
        pendingExportSheetName = sheetName
        pendingExportTable = table
        createXlsxLauncher.launch(fileName)
    }

    fun triggerCsvExport(fileName: String, table: Pair<List<String>, List<List<ExcelCell>>>) {
        pendingExportTable = table
        createCsvLauncher.launch(fileName)
    }

    val vatSummary = remember(transactions) {
        FinancialCalculator.calculateVatSummary(transactions)
    }

    val filteredTransactions = remember(transactions, searchQuery, typeFilter, statusFilter) {
        transactions.filter { tx ->
            val matchesQuery = searchQuery.isBlank() ||
                    tx.partyName.contains(searchQuery, ignoreCase = true) ||
                    tx.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    tx.category.contains(searchQuery, ignoreCase = true)
            val matchesType = typeFilter == "الكل" || tx.type == typeFilter
            val matchesStatus = statusFilter == "الكل" || tx.paymentStatus == statusFilter
            matchesQuery && matchesType && matchesStatus
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            // Financial KPI Strip
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
                        MiniKpiBox(
                            label = "المبيعات (${vatSummary.salesCount})",
                            value = "${FinancialCalculator.formatMoney(vatSummary.salesTotal)} ر.س",
                            accent = HubTeal,
                            modifier = Modifier.weight(1f)
                        )
                        MiniKpiBox(
                            label = "المشتريات (${vatSummary.purchasesCount})",
                            value = "${FinancialCalculator.formatMoney(vatSummary.purchasesTotal)} ر.س",
                            accent = HubAmber,
                            modifier = Modifier.weight(1f)
                        )
                        MiniKpiBox(
                            label = if (vatSummary.netVatPayable >= 0) "صافي الضريبة" else "رصيد ضريبي",
                            value = "${FinancialCalculator.formatMoney(kotlin.math.abs(vatSummary.netVatPayable))} ر.س",
                            accent = HubNavy,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val lowStockCount = remember(products) { products.count { it.isLowStock() } }
                    if (lowStockCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenInventory() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "تنبيه المخزون: $lowStockCount منتجات منخفضة أو نفدت",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                                Text(
                                    text = "فتح المخزون",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HubNavy
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    TabRow(
                        selectedTabIndex = selectedSubTab,
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = HubNavy,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = selectedSubTab == 0,
                            onClick = { selectedSubTab = 0 },
                            text = { Text("الفواتير", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedSubTab == 1,
                            onClick = { selectedSubTab = 1 },
                            text = { Text("الضرائب (VAT)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedSubTab == 2,
                            onClick = { selectedSubTab = 2 },
                            text = { Text("Excel / Sheets", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            when (selectedSubTab) {
                0 -> {
                    // Ledger Tab (Sales & Purchases)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("ابحث برقم الفاتورة أو العميل أو المورد...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("الكل", "مبيعات", "مشتريات").forEach { t ->
                                    FilterBadgeChip(
                                        text = t,
                                        selected = typeFilter == t,
                                        onClick = { typeFilter = t }
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                listOf("الكل", "مدفوع", "جزئي", "آجل").forEach { st ->
                                    FilterBadgeChip(
                                        text = if (st == "الكل") "كل الحالات" else st,
                                        selected = statusFilter == st,
                                        onClick = { statusFilter = st }
                                    )
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
                        } else if (filteredTransactions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = Color(0xFFCBD5E1),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "لا توجد فواتير مبيعات أو مشتريات مطابقة",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = HubNavy
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "اضغط على زر (+) لإضافة فاتورة أو استورد ملف Excel جاهز من تبويب Excel / Sheets",
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
                                items(filteredTransactions, key = { it.id }) { tx ->
                                    TransactionCard(
                                        tx = tx,
                                        onEdit = { txToEdit = tx },
                                        onDelete = { txToDelete = tx },
                                        onStatusChange = { newStatus -> onUpdatePaymentStatus(tx.id, newStatus) },
                                        onShareWhatsApp = {
                                            val text = formatTransactionText(tx)
                                            IntentUtils.openWhatsApp(context, tx.partyPhone, text)
                                        },
                                        onCopy = {
                                            val text = formatTransactionText(tx)
                                            IntentUtils.copyToClipboard(context, text, "فاتورة ${tx.invoiceNumber}")
                                        }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(72.dp)) }
                            }
                        }
                    }
                }

                1 -> {
                    // VAT Summary & Interactive Tax Calculator
                    VatAndCalculatorTab(
                        vatSummary = vatSummary,
                        onExportVatXlsx = {
                            triggerXlsxExport(
                                fileName = "ملخص_الضريبة_VAT.xlsx",
                                sheetName = "ملخص الضريبة",
                                table = ExcelCsvUtils.vatSummaryTable(vatSummary)
                            )
                        },
                        onExportVatCsv = {
                            triggerCsvExport(
                                fileName = "ملخص_الضريبة_VAT.csv",
                                table = ExcelCsvUtils.vatSummaryTable(vatSummary)
                            )
                        },
                        onCopyCalc = { txt ->
                            IntentUtils.copyToClipboard(context, txt, "حسبة الضريبة")
                        }
                    )
                }

                2 -> {
                    // Excel / CSV / Google Sheets Import & Export Hub
                    ExcelSheetsHubTab(
                        transactionsCount = transactions.size,
                        clientsCount = clients.size,
                        onExportTransactionsXlsx = {
                            triggerXlsxExport(
                                fileName = "المبيعات_والمشتريات.xlsx",
                                sheetName = "المبيعات والمشتريات",
                                table = ExcelCsvUtils.transactionsTable(transactions)
                            )
                        },
                        onExportTransactionsCsv = {
                            triggerCsvExport(
                                fileName = "المبيعات_والمشتريات.csv",
                                table = ExcelCsvUtils.transactionsTable(transactions)
                            )
                        },
                        onDownloadTransactionsTemplateXlsx = {
                            triggerXlsxExport(
                                fileName = "قالب_المبيعات_والمشتريات.xlsx",
                                sheetName = "قالب الفواتير",
                                table = ExcelCsvUtils.transactionsTemplateTable()
                            )
                        },
                        onImportTransactions = {
                            pendingImportTarget = AccountingImportTarget.TRANSACTIONS
                            openImportLauncher.launch(arrayOf("*/*"))
                        },
                        onExportClientsXlsx = {
                            triggerXlsxExport(
                                fileName = "قائمة_العملاء.xlsx",
                                sheetName = "العملاء",
                                table = ExcelCsvUtils.clientsTable(clients)
                            )
                        },
                        onExportClientsCsv = {
                            triggerCsvExport(
                                fileName = "قائمة_العملاء.csv",
                                table = ExcelCsvUtils.clientsTable(clients)
                            )
                        },
                        onDownloadClientsTemplateXlsx = {
                            triggerXlsxExport(
                                fileName = "قالب_العملاء.xlsx",
                                sheetName = "قالب العملاء",
                                table = ExcelCsvUtils.clientsTemplateTable()
                            )
                        },
                        onImportClients = {
                            pendingImportTarget = AccountingImportTarget.CLIENTS
                            openImportLauncher.launch(arrayOf("*/*"))
                        },
                        onOpenGoogleSheets = {
                            GoogleSheetsUtils.openGoogleSheet(context, "")
                        }
                    )
                }
            }
        }

        if (selectedSubTab == 0) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("add_transaction_button"),
                containerColor = HubNavy,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة فاتورة")
            }
        }
    }

    if (showAddDialog) {
        TransactionDialog(
            txToEdit = null,
            clients = clients,
            products = products,
            onDismiss = { showAddDialog = false },
            onSave = { newTx ->
                showAddDialog = false
                onAddTransaction(newTx)
            }
        )
    }

    if (txToEdit != null) {
        TransactionDialog(
            txToEdit = txToEdit,
            clients = clients,
            products = products,
            onDismiss = { txToEdit = null },
            onSave = { updated ->
                txToEdit = null
                onUpdateTransaction(updated)
            }
        )
    }

    if (txToDelete != null) {
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text("تأكيد حذف الفاتورة", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف الفاتورة رقم '${txToDelete?.invoiceNumber}'؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        txToDelete?.id?.let { onDeleteTransaction(it) }
                        txToDelete = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Import Preview Dialog
    val preview = importPreview
    if (preview != null) {
        AlertDialog(
            onDismissRequest = { if (!isImporting) importPreview = null },
            title = { Text("معاينة الاستيراد", fontWeight = FontWeight.Bold, color = HubNavy) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    when (preview) {
                        is PendingImportPreview.Transactions -> {
                            Text("الصيغة المكتشفة: ${preview.result.detectedFormat}", fontWeight = FontWeight.SemiBold)
                            Text("عدد الفواتير الجاهزة للاستيراد: ${preview.result.items.size}")
                            if (preview.result.skippedRows > 0) {
                                Text("صفوف فارغة تم تخطيها: ${preview.result.skippedRows}", color = Color(0xFFB45309))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            preview.result.items.take(4).forEach { tx ->
                                Text(
                                    text = "• [${tx.type}] ${tx.invoiceNumber} — ${tx.partyName}: ${FinancialCalculator.formatMoney(tx.grandTotal)} ر.س",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (preview.result.items.size > 4) {
                                Text("... و${preview.result.items.size - 4} فواتير أخرى", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        is PendingImportPreview.Clients -> {
                            Text("الصيغة المكتشفة: ${preview.result.detectedFormat}", fontWeight = FontWeight.SemiBold)
                            Text("عدد العملاء الجاهزين للاستيراد: ${preview.result.items.size}")
                            if (preview.result.skippedRows > 0) {
                                Text("صفوف فارغة تم تخطيها: ${preview.result.skippedRows}", color = Color(0xFFB45309))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            preview.result.items.take(4).forEach { c ->
                                Text("• ${c.name} (${c.phone}) — ${c.status}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
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
                                val count = when (preview) {
                                    is PendingImportPreview.Transactions -> onImportTransactionsBatch(preview.result.items)
                                    is PendingImportPreview.Clients -> onImportClientsBatch(preview.result.items)
                                }
                                importPreview = null
                                onShowMessage("تم استيراد $count سجل وحفظه في السحابة بنجاح")
                            } catch (e: Exception) {
                                onShowMessage("فشل الاستيراد: ${e.message}")
                            } finally {
                                isImporting = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HubNavy),
                    modifier = Modifier.testTag("confirm_import_button")
                ) {
                    Text(if (isImporting) "جارٍ الاستيراد..." else "تأكيد الاستيراد")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isImporting,
                    onClick = { importPreview = null }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun TransactionCard(
    tx: BusinessTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (String) -> Unit,
    onShareWhatsApp: () -> Unit,
    onCopy: () -> Unit
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }
    val isSale = tx.type == "مبيعات"
    val accentColor = if (isSale) HubTeal else HubAmber

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tx.type,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tx.invoiceNumber,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = HubNavy
                    )
                }

                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (tx.paymentStatus) {
                                    "مدفوع" -> Color(0xFFDCFCE7)
                                    "جزئي" -> Color(0xFFFEF3C7)
                                    else -> Color(0xFFFEE2E2)
                                }
                            )
                            .clickable { statusMenuExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tx.paymentStatus,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (tx.paymentStatus) {
                                "مدفوع" -> Color(0xFF15803D)
                                "جزئي" -> Color(0xFFB45309)
                                else -> Color(0xFFB91C1C)
                            }
                        )
                    }
                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        BusinessTransaction.PAYMENT_STATUSES.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = {
                                    statusMenuExpanded = false
                                    onStatusChange(st)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${if (isSale) "العميل:" else "المورد:"} ${tx.partyName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )

            Text(
                text = "${tx.category} • ${tx.paymentMethod} • ${tx.date}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tax & Totals Strip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("الصافي", fontSize = 10.sp, color = Color(0xFF64748B))
                        Text("${FinancialCalculator.formatMoney(tx.subtotal)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text("الضريبة (${tx.taxRate}%)", fontSize = 10.sp, color = Color(0xFF64748B))
                        Text("${FinancialCalculator.formatMoney(tx.taxAmount)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HubTeal)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("الإجمالي النهائي", fontSize = 10.sp, color = Color(0xFF64748B))
                        Text("${FinancialCalculator.formatMoney(tx.grandTotal)} ر.س", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HubNavy)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onShareWhatsApp,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E7E34)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إرسال الفاتورة", fontSize = 11.sp)
                }

                Row {
                    IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الفاتورة", tint = HubNavy, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun VatAndCalculatorTab(
    vatSummary: com.example.util.VatSummary,
    onExportVatXlsx: () -> Unit,
    onExportVatCsv: () -> Unit,
    onCopyCalc: (String) -> Unit
) {
    var calcAmountText by remember { mutableStateOf("1000") }
    var calcTaxMode by remember { mutableStateOf("غير شامل الضريبة") }
    var calcTaxRateText by remember { mutableStateOf("15") }

    val amount = calcAmountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val rate = if (calcTaxMode == "معفى") 0.0 else (calcTaxRateText.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 15.0)
    val calcResult = FinancialCalculator.calculateLineItem(
        quantity = 1.0,
        unitPrice = amount,
        discount = 0.0,
        taxRate = rate,
        taxMode = calcTaxMode
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1) VAT Return Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ملخص الإقرار الضريبي (ضريبة القيمة المضافة VAT)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Spacer(modifier = Modifier.height(12.dp))

                VatRow("مبيعات خاضعة (صافي):", "${FinancialCalculator.formatMoney(vatSummary.salesNet)} ر.س")
                VatRow("ضريبة المخرجات (Output VAT):", "${FinancialCalculator.formatMoney(vatSummary.outputVat)} ر.س", HubTeal)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                VatRow("مشتريات ومصروفات (صافي):", "${FinancialCalculator.formatMoney(vatSummary.purchasesNet)} ر.س")
                VatRow("ضريبة المدخلات (Input VAT):", "${FinancialCalculator.formatMoney(vatSummary.inputVat)} ر.س", HubAmber)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (vatSummary.netVatPayable >= 0) "صافي الضريبة المستحقة للدفع:" else "رصيد ضريبي مسترد لك:",
                        fontWeight = FontWeight.Bold,
                        color = HubNavy
                    )
                    Text(
                        text = "${FinancialCalculator.formatMoney(kotlin.math.abs(vatSummary.netVatPayable))} ر.س",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (vatSummary.netVatPayable >= 0) Color(0xFFB91C1C) else Color(0xFF15803D)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                VatRow(
                    "مجمل الربح التشغيلي (قبل الرواتب):",
                    "${FinancialCalculator.formatMoney(vatSummary.grossOperatingProfit)} ر.س",
                    if (vatSummary.grossOperatingProfit >= 0) Color(0xFF15803D) else Color(0xFFB91C1C)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportVatXlsx,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير Excel", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onExportVatCsv,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = HubNavy, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير CSV", fontSize = 12.sp, color = HubNavy)
                    }
                }
            }
        }

        // 2) Interactive Professional VAT Calculator
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = HubTeal, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "حاسبة الضريبة الدقيقة (BigDecimal)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HubNavy
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = calcAmountText,
                        onValueChange = { calcAmountText = it },
                        label = { Text("المبلغ (ر.س)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.4f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = if (calcTaxMode == "معفى") "0" else calcTaxRateText,
                        onValueChange = { calcTaxRateText = it },
                        enabled = calcTaxMode != "معفى",
                        label = { Text("النسبة %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BusinessTransaction.TAX_MODES.forEach { mode ->
                        FilterBadgeChip(
                            text = mode,
                            selected = calcTaxMode == mode,
                            onClick = { calcTaxMode = mode }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        VatRow("المبلغ الصافي قبل الضريبة:", "${FinancialCalculator.formatMoney(calcResult.net)} ر.س")
                        Spacer(modifier = Modifier.height(4.dp))
                        VatRow("مبلغ الضريبة (${rate}%):", "${FinancialCalculator.formatMoney(calcResult.tax)} ر.س", HubTeal)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        VatRow("الإجمالي النهائي شامل الضريبة:", "${FinancialCalculator.formatMoney(calcResult.total)} ر.س", HubNavy)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        val txt = """
                            حسبة ضريبة القيمة المضافة ($calcTaxMode - ${rate}%):
                            • الصافي: ${FinancialCalculator.formatMoney(calcResult.net)} ر.س
                            • الضريبة: ${FinancialCalculator.formatMoney(calcResult.tax)} ر.س
                            • الإجمالي: ${FinancialCalculator.formatMoney(calcResult.total)} ر.س
                        """.trimIndent()
                        onCopyCalc(txt)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نسخ تفاصيل الحسبة")
                }
            }
        }
    }
}

@Composable
private fun ExcelSheetsHubTab(
    transactionsCount: Int,
    clientsCount: Int,
    onExportTransactionsXlsx: () -> Unit,
    onExportTransactionsCsv: () -> Unit,
    onDownloadTransactionsTemplateXlsx: () -> Unit,
    onImportTransactions: () -> Unit,
    onExportClientsXlsx: () -> Unit,
    onExportClientsCsv: () -> Unit,
    onDownloadClientsTemplateXlsx: () -> Unit,
    onImportClients: () -> Unit,
    onOpenGoogleSheets: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Transactions Excel & CSV
        ImportExportSectionCard(
            title = "المبيعات والمشتريات ($transactionsCount فاتورة)",
            subtitle = "تصدير واستيراد جداول الفواتير بصيغة Excel (.xlsx) و CSV المتوافقة مع Google Sheets",
            onExportXlsx = onExportTransactionsXlsx,
            onExportCsv = onExportTransactionsCsv,
            onDownloadTemplate = onDownloadTransactionsTemplateXlsx,
            onImportFile = onImportTransactions
        )

        // Card 2: Clients Excel & CSV
        ImportExportSectionCard(
            title = "بيانات العملاء ($clientsCount عميل)",
            subtitle = "نسخ احتياطي أو استيراد جماعي لقائمة العملاء من ملفات Excel أو Google Sheets",
            onExportXlsx = onExportClientsXlsx,
            onExportCsv = onExportClientsCsv,
            onDownloadTemplate = onDownloadClientsTemplateXlsx,
            onImportFile = onImportClients
        )

        // Card 3: Direct Google Sheets Launcher
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "التكامل المباشر مع Google Sheets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "يمكنك تصدير أي جدول بصيغة Excel أو CSV ثم استيراده في Google Sheets فوراً أو فتح جدول جديد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenGoogleSheets,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("فتح / إنشاء جدول Google Sheets جديد")
                }
            }
        }
    }
}

@Composable
private fun ImportExportSectionCard(
    title: String,
    subtitle: String,
    onExportXlsx: () -> Unit,
    onExportCsv: () -> Unit,
    onDownloadTemplate: () -> Unit,
    onImportFile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HubNavy)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportXlsx,
                    colors = ButtonDefaults.buttonColors(containerColor = HubNavy),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تصدير Excel", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onExportCsv,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("تصدير CSV", fontSize = 12.sp, color = HubNavy)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onImportFile,
                    colors = ButtonDefaults.buttonColors(containerColor = HubTeal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استيراد ملف", fontSize = 12.sp)
                }
                TextButton(
                    onClick = onDownloadTemplate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("تحميل القالب (.xlsx)", fontSize = 12.sp, color = HubTeal, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MiniKpiBox(
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
        Column(modifier = Modifier.padding(8.dp)) {
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FilterBadgeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) HubNavy else Color(0xFFF1F5F9))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF475569)
        )
    }
}

@Composable
private fun VatRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF1E293B)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

private fun formatTransactionText(tx: BusinessTransaction): String {
    val sb = StringBuilder()
    sb.append("🧾 *فاتورة ${tx.type} - مركز عملي*\n")
    sb.append("رقم الفاتورة: ${tx.invoiceNumber}\n")
    sb.append("التاريخ: ${tx.date}\n")
    sb.append("${if (tx.type == "مبيعات") "العميل" else "المورد"}: ${tx.partyName}\n")
    if (tx.partyTaxNumber.isNotBlank()) {
        sb.append("الرقم الضريبي: ${tx.partyTaxNumber}\n")
    }
    sb.append("نظام الضريبة: ${tx.taxMode} (${tx.taxRate}%)\n")
    sb.append("━━━━━━━━━━━━━━━━━━━\n")
    tx.getTypedItems().forEachIndexed { idx, item ->
        sb.append("${idx + 1}) ${item.name} × ${item.quantity} = ${FinancialCalculator.formatMoney(item.total)} ر.س\n")
    }
    sb.append("━━━━━━━━━━━━━━━━━━━\n")
    sb.append("الصافي قبل الضريبة: ${FinancialCalculator.formatMoney(tx.subtotal)} ر.س\n")
    sb.append("الضريبة: ${FinancialCalculator.formatMoney(tx.taxAmount)} ر.س\n")
    sb.append("*الإجمالي النهائي: ${FinancialCalculator.formatMoney(tx.grandTotal)} ر.س*\n")
    sb.append("حالة الدفع: ${tx.paymentStatus} (${tx.paymentMethod})")
    return sb.toString()
}
