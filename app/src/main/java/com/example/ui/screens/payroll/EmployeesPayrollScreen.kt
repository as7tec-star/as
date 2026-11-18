package com.example.ui.screens.payroll

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Employee
import com.example.data.model.PayrollRecord
import com.example.ui.theme.HubAmber
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal
import com.example.util.ExcelCell
import com.example.util.ExcelCsvUtils
import com.example.util.FinancialCalculator
import com.example.util.ImportResult
import com.example.util.IntentUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun EmployeesPayrollScreen(
    employees: List<Employee>,
    payrolls: List<PayrollRecord>,
    isEmployeesLoading: Boolean,
    isPayrollsLoading: Boolean,
    onAddEmployee: (Employee) -> Unit,
    onUpdateEmployee: (Employee) -> Unit,
    onDeleteEmployee: (String) -> Unit,
    onImportEmployeesBatch: suspend (List<Employee>) -> Int,
    onGenerateMonthlyPayroll: suspend (String, List<Employee>, List<PayrollRecord>) -> Int,
    onUpdatePayrollRecord: (PayrollRecord) -> Unit,
    onMarkAllPaid: (List<PayrollRecord>, String) -> Unit,
    onDeletePayrollRecord: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedSubTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("الكل") }

    // Month selection (yyyy-MM)
    val monthFormat = remember { SimpleDateFormat("yyyy-MM", Locale.ENGLISH) }
    val todayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    var selectedMonth by remember { mutableStateOf(monthFormat.format(Date())) }

    // Dialog states
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<Employee?>(null) }
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }

    var payrollToEdit by remember { mutableStateOf<PayrollRecord?>(null) }
    var payrollToDelete by remember { mutableStateOf<PayrollRecord?>(null) }

    // SAF Export & Import
    var pendingExportSheetName by remember { mutableStateOf("Sheet1") }
    var pendingExportTable by remember { mutableStateOf<Pair<List<String>, List<List<ExcelCell>>>?>(null) }
    var importPreview by remember { mutableStateOf<ImportResult<Employee>?>(null) }
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
                    onShowMessage("تم تصدير ملف CSV بنجاح")
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
                    val parsed = ExcelCsvUtils.parseEmployeesFromRows(rows, format)
                    if (parsed.items.isEmpty()) {
                        onShowMessage("لم يتم العثور على بيانات موظفين صالحة في الملف.")
                    } else {
                        importPreview = parsed
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

    fun shiftMonth(delta: Int) {
        try {
            val date = monthFormat.parse(selectedMonth) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.MONTH, delta)
            selectedMonth = monthFormat.format(cal.time)
        } catch (_: Exception) {
        }
    }

    val activeEmployees = remember(employees) { employees.filter { it.status == "نشط" } }
    val estimatedMonthlyPayroll = remember(activeEmployees) {
        activeEmployees.sumOf { it.estimatedNetSalary() }
    }

    val filteredEmployees = remember(employees, searchQuery, statusFilter) {
        employees.filter { emp ->
            val matchesQuery = searchQuery.isBlank() ||
                    emp.name.contains(searchQuery, ignoreCase = true) ||
                    emp.jobTitle.contains(searchQuery, ignoreCase = true) ||
                    emp.department.contains(searchQuery, ignoreCase = true)
            val matchesStatus = statusFilter == "الكل" || emp.status == statusFilter
            matchesQuery && matchesStatus
        }
    }

    val monthPayrolls = remember(payrolls, selectedMonth) {
        payrolls.filter { it.monthYear == selectedMonth }
    }
    val monthTotalNet = remember(monthPayrolls) {
        FinancialCalculator.totalPayrollCost(monthPayrolls)
    }
    val monthPaidTotal = remember(monthPayrolls) {
        FinancialCalculator.totalPayrollCost(monthPayrolls.filter { it.paymentStatus == "مدفوع" })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            // Header Summary & Sub-Tabs
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
                        HrKpiTile(
                            title = "الموظفون النشطون",
                            value = "${activeEmployees.size} / ${employees.size}",
                            color = HubNavy,
                            modifier = Modifier.weight(1f)
                        )
                        HrKpiTile(
                            title = "الرواتب المقدرة",
                            value = "${FinancialCalculator.formatMoney(estimatedMonthlyPayroll)} ر.س",
                            color = HubTeal,
                            modifier = Modifier.weight(1f)
                        )
                        HrKpiTile(
                            title = "مسير $selectedMonth",
                            value = "${FinancialCalculator.formatMoney(monthTotalNet)} ر.س",
                            color = HubAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TabRow(
                        selectedTabIndex = selectedSubTab,
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = HubNavy,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = selectedSubTab == 0,
                            onClick = { selectedSubTab = 0 },
                            text = { Text("دليل الموظفين", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedSubTab == 1,
                            onClick = { selectedSubTab = 1 },
                            text = { Text("مسير الرواتب الشهري", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            if (selectedSubTab == 0) {
                // Employees Directory Tab
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
                            placeholder = { Text("ابحث بالاسم أو المسمى الوظيفي أو القسم...") },
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
                            (listOf("الكل") + Employee.STATUSES).forEach { st ->
                                val selected = statusFilter == st
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (selected) HubNavy else Color(0xFFF1F5F9))
                                        .clickable { statusFilter = st }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = st,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Excel / CSV Quick Action Row for Employees
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    triggerXlsxExport(
                                        fileName = "سجل_الموظفين.xlsx",
                                        sheetName = "الموظفون",
                                        table = ExcelCsvUtils.employeesTable(employees)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF107C41), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تصدير Excel", fontSize = 11.sp, color = Color(0xFF107C41))
                            }
                            OutlinedButton(
                                onClick = {
                                    triggerCsvExport(
                                        fileName = "سجل_الموظفين.csv",
                                        table = ExcelCsvUtils.employeesTable(employees)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("تصدير CSV", fontSize = 11.sp, color = HubNavy)
                            }
                            OutlinedButton(
                                onClick = { openImportLauncher.launch(arrayOf("*/*")) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, tint = HubTeal, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استيراد موظفين", fontSize = 11.sp, color = HubTeal)
                            }
                            TextButton(
                                onClick = {
                                    triggerXlsxExport(
                                        fileName = "قالب_الموظفين.xlsx",
                                        sheetName = "قالب الموظفين",
                                        table = ExcelCsvUtils.employeesTemplateTable()
                                    )
                                },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("قالب Excel", fontSize = 11.sp, color = HubNavy)
                            }
                        }
                    }

                    if (isEmployeesLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = HubNavy)
                        }
                    } else if (filteredEmployees.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("لا يوجد موظفون مسجلون حالياً", fontWeight = FontWeight.Bold, color = HubNavy)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "اضغط على (+) لإضافة موظف وتحديد راتبه الأساسي والبدلات، أو استورد من Excel",
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
                            items(filteredEmployees, key = { it.id }) { emp ->
                                EmployeeCard(
                                    employee = emp,
                                    onEdit = { employeeToEdit = emp },
                                    onDelete = { employeeToDelete = emp },
                                    onWhatsApp = {
                                        val msg = "مرحباً ${emp.name}، نرجو التواصل بخصوص شؤون الموارد البشرية في مركز عملي."
                                        IntentUtils.openWhatsApp(context, emp.phone, msg)
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }
            } else {
                // Monthly Payroll Tab
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(12.dp)
                    ) {
                        // Month Switcher Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { shiftMonth(-1) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "الشهر السابق", tint = HubNavy)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "مسير رواتب شهر: $selectedMonth",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HubNavy
                                )
                                Text(
                                    text = "المدفوع: ${FinancialCalculator.formatMoney(monthPaidTotal)} من ${FinancialCalculator.formatMoney(monthTotalNet)} ر.س",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HubTeal
                                )
                            }
                            IconButton(onClick = { shiftMonth(1) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "الشهر التالي", tint = HubNavy)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Payroll Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (activeEmployees.isEmpty()) {
                                        onShowMessage("لا يوجد موظفون نشطون لتوليد المسير. أضف موظفاً أولاً.")
                                        return@Button
                                    }
                                    scope.launch {
                                        try {
                                            val added = onGenerateMonthlyPayroll(selectedMonth, activeEmployees, monthPayrolls)
                                            if (added > 0) {
                                                onShowMessage("تم توليد مسير الرواتب لـ $added موظف لشهر $selectedMonth")
                                            } else {
                                                onShowMessage("جميع الموظفين النشطين مضافون بالفعل لمسير $selectedMonth")
                                            }
                                        } catch (e: Exception) {
                                            onShowMessage("فشل توليد المسير: ${e.message}")
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HubNavy),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_payroll_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("توليد المسير تلقائياً", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    if (monthPayrolls.isEmpty()) {
                                        onShowMessage("لا توجد سجلات رواتب في هذا الشهر لصرفها.")
                                        return@Button
                                    }
                                    onMarkAllPaid(monthPayrolls, todayFormat.format(Date()))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HubTeal),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mark_all_paid_button")
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("صرف الكل", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    triggerXlsxExport(
                                        fileName = "مسير_الرواتب_$selectedMonth.xlsx",
                                        sheetName = "مسير $selectedMonth",
                                        table = ExcelCsvUtils.payrollTable(monthPayrolls)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF107C41), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تصدير المسير Excel", fontSize = 11.sp, color = Color(0xFF107C41))
                            }
                            OutlinedButton(
                                onClick = {
                                    triggerCsvExport(
                                        fileName = "مسير_الرواتب_$selectedMonth.csv",
                                        table = ExcelCsvUtils.payrollTable(monthPayrolls)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Text("تصدير CSV", fontSize = 11.sp, color = HubNavy)
                            }
                        }
                    }

                    if (isPayrollsLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = HubNavy)
                        }
                    } else if (monthPayrolls.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("لا يوجد مسير رواتب لشهر $selectedMonth", fontWeight = FontWeight.Bold, color = HubNavy)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "اضغط على \"توليد المسير تلقائياً\" لإنشاء كشوف رواتب جميع الموظفين النشطين بدقة",
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
                            items(monthPayrolls, key = { it.id }) { record ->
                                PayrollSlipCard(
                                    record = record,
                                    onEdit = { payrollToEdit = record },
                                    onDelete = { payrollToDelete = record },
                                    onSendSlipWhatsApp = {
                                        val slipText = formatPayrollSlipText(record)
                                        IntentUtils.openWhatsApp(context, record.employeePhone, slipText)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedSubTab == 0) {
            FloatingActionButton(
                onClick = { showAddEmployeeDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("add_employee_button"),
                containerColor = HubNavy,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة موظف")
            }
        }
    }

    if (showAddEmployeeDialog) {
        EmployeeDialog(
            employeeToEdit = null,
            onDismiss = { showAddEmployeeDialog = false },
            onSave = { newEmp ->
                showAddEmployeeDialog = false
                onAddEmployee(newEmp)
            }
        )
    }

    if (employeeToEdit != null) {
        EmployeeDialog(
            employeeToEdit = employeeToEdit,
            onDismiss = { employeeToEdit = null },
            onSave = { updated ->
                employeeToEdit = null
                onUpdateEmployee(updated)
            }
        )
    }

    if (employeeToDelete != null) {
        AlertDialog(
            onDismissRequest = { employeeToDelete = null },
            title = { Text("تأكيد حذف الموظف", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف بيانات الموظف '${employeeToDelete?.name}'؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        employeeToDelete?.id?.let { onDeleteEmployee(it) }
                        employeeToDelete = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { employeeToDelete = null }) { Text("إلغاء") }
            }
        )
    }

    if (payrollToEdit != null) {
        PayrollEditDialog(
            record = payrollToEdit!!,
            onDismiss = { payrollToEdit = null },
            onSave = { updated ->
                payrollToEdit = null
                onUpdatePayrollRecord(updated)
            }
        )
    }

    if (payrollToDelete != null) {
        AlertDialog(
            onDismissRequest = { payrollToDelete = null },
            title = { Text("تأكيد حذف كشف الراتب", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد حذف سجل راتب '${payrollToDelete?.employeeName}' لشهر ${payrollToDelete?.monthYear}؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        payrollToDelete?.id?.let { onDeletePayrollRecord(it) }
                        payrollToDelete = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { payrollToDelete = null }) { Text("إلغاء") }
            }
        )
    }

    val preview = importPreview
    if (preview != null) {
        AlertDialog(
            onDismissRequest = { if (!isImporting) importPreview = null },
            title = { Text("معاينة استيراد الموظفين", fontWeight = FontWeight.Bold, color = HubNavy) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("الصيغة: ${preview.detectedFormat}", fontWeight = FontWeight.SemiBold)
                    Text("عدد الموظفين الجاهزين: ${preview.items.size}")
                    if (preview.skippedRows > 0) {
                        Text("صفوف فارغة تم تخطيها: ${preview.skippedRows}", color = Color(0xFFB45309))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    preview.items.take(4).forEach { e ->
                        Text(
                            text = "• ${e.name} (${e.jobTitle}) — صافي تقديري: ${FinancialCalculator.formatMoney(e.estimatedNetSalary())} ر.س",
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
                                val count = onImportEmployeesBatch(preview.items)
                                importPreview = null
                                onShowMessage("تم استيراد $count موظف بنجاح")
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
private fun EmployeeCard(
    employee: Employee,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onWhatsApp: () -> Unit
) {
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
                        text = employee.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HubNavy
                    )
                    Text(
                        text = "${employee.jobTitle} • ${employee.department}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (employee.status) {
                                "نشط" -> Color(0xFFDCFCE7)
                                "إجازة" -> Color(0xFFFEF3C7)
                                else -> Color(0xFFFEE2E2)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = employee.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (employee.status) {
                            "نشط" -> Color(0xFF15803D)
                            "إجازة" -> Color(0xFFB45309)
                            else -> Color(0xFFB91C1C)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                        Text("الأساسي", fontSize = 10.sp, color = Color(0xFF64748B))
                        Text(FinancialCalculator.formatMoney(employee.baseSalary), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text("البدلات", fontSize = 10.sp, color = Color(0xFF64748B))
                        Text(FinancialCalculator.formatMoney(employee.totalAllowances()), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HubTeal)
                    }
                    Column {
                        Text("الخصم", fontSize = 10.sp, color = Color(0xFF64748B))
                        Text(FinancialCalculator.formatMoney(employee.defaultDeductions), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB91C1C))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("صافي الراتب", fontSize = 10.sp, color = Color(0xFF64748B))
                        Text("${FinancialCalculator.formatMoney(employee.estimatedNetSalary())} ر.س", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HubNavy)
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
                    onClick = onWhatsApp,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E7E34)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مراسلة واتساب", fontSize = 11.sp)
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل الموظف", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف الموظف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PayrollSlipCard(
    record: PayrollRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSendSlipWhatsApp: () -> Unit
) {
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
                        text = record.employeeName,
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

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (record.paymentStatus) {
                                "مدفوع" -> Color(0xFFDCFCE7)
                                "معتمد" -> Color(0xFFFEF3C7)
                                else -> Color(0xFFF1F5F9)
                            }
                        )
                        .clickable { onEdit() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = record.paymentStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (record.paymentStatus) {
                            "مدفوع" -> Color(0xFF15803D)
                            "معتمد" -> Color(0xFFB45309)
                            else -> Color(0xFF475569)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الأساسي + البدلات:", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(
                            "${FinancialCalculator.formatMoney(record.baseSalary + record.allowances)} ر.س",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المكافآت / الخصومات:", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(
                            "+${FinancialCalculator.formatMoney(record.bonuses)} / -${FinancialCalculator.formatMoney(record.deductions)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("صافي الراتب المستحق:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HubNavy)
                        Text(
                            "${FinancialCalculator.formatMoney(record.netSalary)} ر.س",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = HubTeal
                        )
                    }
                }
            }

            if (record.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ملاحظة: ${record.notes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
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
                    onClick = onSendSlipWhatsApp,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E7E34)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إرسال كشف الراتب", fontSize = 11.sp)
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل المسير", tint = HubNavy, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف المسير", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HrKpiTile(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = title,
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
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatPayrollSlipText(record: PayrollRecord): String {
    return buildString {
        append("💼 *كشف راتب شهر ${record.monthYear} - مركز عملي*\n")
        append("━━━━━━━━━━━━━━━━━━━\n")
        append("👤 الموظف: ${record.employeeName}\n")
        append("🏷️ الوظيفة: ${record.jobTitle}\n")
        append("━━━━━━━━━━━━━━━━━━━\n")
        append("• الراتب الأساسي: ${FinancialCalculator.formatMoney(record.baseSalary)} ر.س\n")
        append("• إجمالي البدلات: ${FinancialCalculator.formatMoney(record.allowances)} ر.س\n")
        if (record.bonuses > 0) {
            append("• المكافآت / الإضافي: +${FinancialCalculator.formatMoney(record.bonuses)} ر.س\n")
        }
        if (record.deductions > 0) {
            append("• الاستقطاعات / الخصم: -${FinancialCalculator.formatMoney(record.deductions)} ر.س\n")
        }
        append("━━━━━━━━━━━━━━━━━━━\n")
        append("💰 *صافي الراتب المستحق: ${FinancialCalculator.formatMoney(record.netSalary)} ر.س*\n")
        append("📌 حالة الصرف: ${record.paymentStatus}")
        if (record.paymentDate.isNotBlank()) {
            append(" (${record.paymentDate})")
        }
        if (record.notes.isNotBlank()) {
            append("\n📝 ملاحظات: ${record.notes}")
        }
    }
}
