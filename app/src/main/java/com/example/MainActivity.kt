package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.credentials.CredentialManager
import androidx.compose.runtime.key
import com.example.data.model.AdPost
import com.example.data.model.BusinessTransaction
import com.example.data.model.Client
import com.example.data.model.Employee
import com.example.data.model.MessageTemplate
import com.example.data.model.PayrollRecord
import com.example.data.model.Product
import com.example.data.model.Quote
import com.example.data.repository.AdPostsRepository
import com.example.data.repository.AiMarketingRepository
import com.example.data.repository.ClientsRepository
import com.example.data.repository.EmployeesRepository
import com.example.data.repository.PayrollRepository
import com.example.data.repository.ProductsRepository
import com.example.data.repository.QuotesRepository
import com.example.data.repository.TemplatesRepository
import com.example.data.repository.TransactionsRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.AppNavTab
import com.example.ui.components.BottomNav
import com.example.ui.components.TopHeader
import com.example.ui.screens.analytics.AnalyticsScreen
import com.example.ui.screens.clients.ClientsScreen
import com.example.ui.screens.hubs.FinanceHubScreen
import com.example.ui.screens.hubs.MarketingHubScreen
import com.example.ui.screens.payroll.EmployeesPayrollScreen
import com.example.ui.theme.MarkazAmaliTheme
import com.example.util.StockAlertNotifier
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            MarkazAmaliTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppContent()
                }
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        key(user.uid) {
            MainBusinessHubScreen(
                userEmail = user.email ?: "",
                onSignOut = {
                    signOutUser(
                        context = context,
                        credentialManager = credentialManager,
                        onSignOutComplete = { currentUser = null },
                        scope = scope
                    )
                }
            )
        }
    }
}

@Composable
fun MainBusinessHubScreen(
    userEmail: String,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val clientsRepository = remember { ClientsRepository(context) }
    val quotesRepository = remember { QuotesRepository(context) }
    val transactionsRepository = remember { TransactionsRepository(context) }
    val productsRepository = remember { ProductsRepository(context) }
    val employeesRepository = remember { EmployeesRepository(context) }
    val payrollRepository = remember { PayrollRepository(context) }
    val adPostsRepository = remember { AdPostsRepository(context) }
    val templatesRepository = remember { TemplatesRepository(context) }
    val aiRepository = remember { AiMarketingRepository() }

    var currentTab by remember { mutableStateOf(AppNavTab.CLIENTS) }

    var clientsList by remember { mutableStateOf<List<Client>>(emptyList()) }
    var isClientsLoading by remember { mutableStateOf(true) }

    var quotesList by remember { mutableStateOf<List<Quote>>(emptyList()) }
    var isQuotesLoading by remember { mutableStateOf(true) }

    var transactionsList by remember { mutableStateOf<List<BusinessTransaction>>(emptyList()) }
    var isTransactionsLoading by remember { mutableStateOf(true) }

    var productsList by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isProductsLoading by remember { mutableStateOf(true) }
    val notifiedProductLevels = remember { mutableStateMapOf<String, Double>() }

    var employeesList by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var isEmployeesLoading by remember { mutableStateOf(true) }

    var payrollsList by remember { mutableStateOf<List<PayrollRecord>>(emptyList()) }
    var isPayrollsLoading by remember { mutableStateOf(true) }

    var adsList by remember { mutableStateOf<List<AdPost>>(emptyList()) }
    var isAdsLoading by remember { mutableStateOf(true) }

    var templatesList by remember { mutableStateOf<List<MessageTemplate>>(emptyList()) }

    fun showMessage(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    // Realtime listeners
    LaunchedEffect(Unit) {
        clientsRepository.observeClients()
            .catch { e ->
                isClientsLoading = false
                showMessage("خطأ في تحميل العملاء: ${e.message}")
            }
            .collect { list ->
                clientsList = list
                isClientsLoading = false
            }
    }

    LaunchedEffect(Unit) {
        transactionsRepository.observeTransactions()
            .catch { e ->
                isTransactionsLoading = false
                showMessage("خطأ في تحميل المبيعات والمشتريات: ${e.message}")
            }
            .collect { list ->
                transactionsList = list
                isTransactionsLoading = false
            }
    }

    LaunchedEffect(Unit) {
        employeesRepository.observeEmployees()
            .catch { e ->
                isEmployeesLoading = false
                showMessage("خطأ في تحميل الموظفين: ${e.message}")
            }
            .collect { list ->
                employeesList = list
                isEmployeesLoading = false
            }
    }

    LaunchedEffect(Unit) {
        payrollRepository.observePayrolls()
            .catch { e ->
                isPayrollsLoading = false
                showMessage("خطأ في تحميل مسيرات الرواتب: ${e.message}")
            }
            .collect { list ->
                payrollsList = list
                isPayrollsLoading = false
            }
    }

    LaunchedEffect(Unit) {
        adPostsRepository.observeAds()
            .catch { e ->
                isAdsLoading = false
                showMessage("خطأ في تحميل الإعلانات: ${e.message}")
            }
            .collect { list ->
                adsList = list
                isAdsLoading = false
            }
    }

    LaunchedEffect(Unit) {
        quotesRepository.observeQuotes()
            .catch { e ->
                isQuotesLoading = false
                showMessage("خطأ في تحميل عروض الأسعار: ${e.message}")
            }
            .collect { list ->
                quotesList = list
                isQuotesLoading = false
            }
    }

    LaunchedEffect(Unit) {
        templatesRepository.observeTemplates()
            .catch { }
            .collect { list ->
                templatesList = list
            }
    }

    LaunchedEffect(Unit) {
        StockAlertNotifier.createChannel(context)
        productsRepository.observeProducts()
            .catch { e ->
                isProductsLoading = false
                showMessage("خطأ في تحميل المخزون: ${e.message}")
            }
            .collect { list ->
                productsList = list
                isProductsLoading = false
            }
    }

    val lowStockProducts = remember(productsList) {
        productsList.filter { it.isLowStock() }
    }

    LaunchedEffect(productsList) {
        if (isProductsLoading) return@LaunchedEffect
        val lowIds = lowStockProducts.map { it.id }.toSet()
        notifiedProductLevels.keys.toList().forEach { id ->
            if (id !in lowIds) notifiedProductLevels.remove(id)
        }
        val newlyLow = lowStockProducts.filter { p ->
            val prevQty = notifiedProductLevels[p.id]
            prevQty == null || p.quantity < prevQty
        }
        if (newlyLow.isNotEmpty()) {
            newlyLow.forEach { p -> notifiedProductLevels[p.id] = p.quantity }
            StockAlertNotifier.notifyLowStockProducts(context, lowStockProducts)
            val summaryNames = newlyLow.take(2).joinToString("، ") { "${it.name} (${it.formattedQuantity()})" }
            val extra = if (newlyLow.size > 2) " و+${newlyLow.size - 2}" else ""
            showMessage("تنبيه مخزون منخفض: $summaryNames$extra")
        }
    }

    // Follow-up overdue count calculation (>= 3 days)
    val overdueCount = remember(clientsList) {
        val nowMillis = System.currentTimeMillis()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        clientsList.count { client ->
            if (client.status == "تم البيع" || client.status == "خسرناه") {
                false
            } else if (client.lastContactDate.isBlank()) {
                true
            } else {
                try {
                    val date = format.parse(client.lastContactDate)
                    if (date != null) {
                        val diffDays = TimeUnit.MILLISECONDS.toDays(nowMillis - date.time)
                        diffDays >= 3
                    } else {
                        true
                    }
                } catch (_: Exception) {
                    true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopHeader(
                userEmail = userEmail,
                onSignOutClick = onSignOut
            )
        },
        bottomBar = {
            BottomNav(
                selectedTab = currentTab,
                onTabSelected = { currentTab = it },
                followUpCount = overdueCount,
                financeAlertCount = lowStockProducts.size
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppNavTab.CLIENTS -> {
                    ClientsScreen(
                        clients = clientsList,
                        isLoading = isClientsLoading,
                        onAddClient = { newClient ->
                            scope.launch {
                                try {
                                    clientsRepository.addClient(newClient)
                                    showMessage("تمت إضافة العميل بنجاح")
                                } catch (e: Exception) {
                                    showMessage("فشلت الإضافة: ${e.message}")
                                }
                            }
                        },
                        onUpdateClient = { updated ->
                            scope.launch {
                                try {
                                    clientsRepository.updateClient(updated)
                                    showMessage("تم تحديث العميل بنجاح")
                                } catch (e: Exception) {
                                    showMessage("فشل التحديث: ${e.message}")
                                }
                            }
                        },
                        onDeleteClient = { clientId ->
                            scope.launch {
                                try {
                                    clientsRepository.deleteClient(clientId)
                                    showMessage("تم حذف العميل بنجاح")
                                } catch (e: Exception) {
                                    showMessage("فشل الحذف: ${e.message}")
                                }
                            }
                        },
                        onMoveStatus = { client, newStatus ->
                            scope.launch {
                                try {
                                    clientsRepository.updateClientStatus(client.id, newStatus)
                                    showMessage("تم نقل العميل إلى مرحلة: $newStatus")
                                } catch (e: Exception) {
                                    showMessage("فشل نقل المرحلة: ${e.message}")
                                }
                            }
                        },
                        onImportClientsBatch = { imported ->
                            clientsRepository.addClientsBatch(imported)
                        },
                        onShowMessage = ::showMessage
                    )
                }

                AppNavTab.FINANCE -> {
                    FinanceHubScreen(
                        transactions = transactionsList,
                        quotes = quotesList,
                        clients = clientsList,
                        products = productsList,
                        isTransactionsLoading = isTransactionsLoading,
                        isQuotesLoading = isQuotesLoading,
                        isProductsLoading = isProductsLoading,
                        onAddTransaction = { newTx ->
                            scope.launch {
                                try {
                                    transactionsRepository.addTransaction(newTx)
                                    val affectedLow = productsRepository.applyTransactionToStock(newTx, productsList)
                                    if (affectedLow.isNotEmpty()) {
                                        StockAlertNotifier.notifyLowStockProducts(context, affectedLow)
                                    }
                                    showMessage("تم حفظ الفاتورة وتحديث المخزون بنجاح")
                                } catch (e: Exception) {
                                    showMessage("فشل حفظ الفاتورة: ${e.message}")
                                }
                            }
                        },
                        onUpdateTransaction = { updated ->
                            scope.launch {
                                try {
                                    transactionsRepository.updateTransaction(updated)
                                    showMessage("تم تحديث الفاتورة بنجاح")
                                } catch (e: Exception) {
                                    showMessage("فشل تحديث الفاتورة: ${e.message}")
                                }
                            }
                        },
                        onDeleteTransaction = { txId ->
                            scope.launch {
                                try {
                                    transactionsRepository.deleteTransaction(txId)
                                    showMessage("تم حذف الفاتورة")
                                } catch (e: Exception) {
                                    showMessage("فشل الحذف: ${e.message}")
                                }
                            }
                        },
                        onUpdateTransactionStatus = { txId, status ->
                            scope.launch {
                                try {
                                    transactionsRepository.updatePaymentStatus(txId, status)
                                    showMessage("تم تحديث حالة الدفع إلى: $status")
                                } catch (e: Exception) {
                                    showMessage("فشل التحديث: ${e.message}")
                                }
                            }
                        },
                        onImportTransactionsBatch = { imported ->
                            transactionsRepository.addTransactionsBatch(imported)
                        },
                        onImportClientsBatch = { imported ->
                            clientsRepository.addClientsBatch(imported)
                        },
                        onAddQuote = { newQuote ->
                            scope.launch {
                                try {
                                    quotesRepository.addQuote(newQuote)
                                    showMessage("تم حفظ عرض السعر بنجاح")
                                } catch (e: Exception) {
                                    showMessage("فشل حفظ عرض السعر: ${e.message}")
                                }
                            }
                        },
                        onUpdateQuote = { updated ->
                            scope.launch {
                                try {
                                    quotesRepository.updateQuote(updated)
                                    showMessage("تم تحديث عرض السعر")
                                } catch (e: Exception) {
                                    showMessage("فشل التحديث: ${e.message}")
                                }
                            }
                        },
                        onDeleteQuote = { quoteId ->
                            scope.launch {
                                try {
                                    quotesRepository.deleteQuote(quoteId)
                                    showMessage("تم حذف عرض السعر")
                                } catch (e: Exception) {
                                    showMessage("فشل الحذف: ${e.message}")
                                }
                            }
                        },
                        onUpdateQuoteStatus = { quoteId, newStatus ->
                            scope.launch {
                                try {
                                    quotesRepository.updateQuoteStatus(quoteId, newStatus)
                                    showMessage("تم تحديث الحالة إلى: $newStatus")
                                } catch (e: Exception) {
                                    showMessage("فشل التحديث: ${e.message}")
                                }
                            }
                        },
                        onAddProduct = { newProduct ->
                            scope.launch {
                                try {
                                    productsRepository.addProduct(newProduct)
                                    showMessage("تمت إضافة المنتج للمخزون")
                                } catch (e: Exception) {
                                    showMessage("فشلت الإضافة: ${e.message}")
                                }
                            }
                        },
                        onUpdateProduct = { updated ->
                            scope.launch {
                                try {
                                    productsRepository.updateProduct(updated)
                                    showMessage("تم تحديث بيانات المنتج")
                                } catch (e: Exception) {
                                    showMessage("فشل التحديث: ${e.message}")
                                }
                            }
                        },
                        onAdjustProductQuantity = { productId, newQty ->
                            scope.launch {
                                try {
                                    productsRepository.adjustQuantity(productId, newQty)
                                } catch (e: Exception) {
                                    showMessage("فشل تعديل الكمية: ${e.message}")
                                }
                            }
                        },
                        onToggleProductAlerts = { productId, enabled ->
                            scope.launch {
                                try {
                                    productsRepository.toggleAlerts(productId, enabled)
                                    showMessage(if (enabled) "تم تفعيل التنبيه التلقائي" else "تم إيقاف التنبيه لهذا المنتج")
                                } catch (e: Exception) {
                                    showMessage("فشل التحديث: ${e.message}")
                                }
                            }
                        },
                        onDeleteProduct = { productId ->
                            scope.launch {
                                try {
                                    productsRepository.deleteProduct(productId)
                                    showMessage("تم حذف المنتج من المخزون")
                                } catch (e: Exception) {
                                    showMessage("فشل الحذف: ${e.message}")
                                }
                            }
                        },
                        onImportProductsBatch = { imported ->
                            productsRepository.addProductsBatch(imported)
                        },
                        onShowMessage = ::showMessage
                    )
                }

                AppNavTab.HR_PAYROLL -> {
                    EmployeesPayrollScreen(
                        employees = employeesList,
                        payrolls = payrollsList,
                        isEmployeesLoading = isEmployeesLoading,
                        isPayrollsLoading = isPayrollsLoading,
                        onAddEmployee = { emp ->
                            scope.launch {
                                try {
                                    employeesRepository.addEmployee(emp)
                                    showMessage("تمت إضافة الموظف بنجاح")
                                } catch (e: Exception) {
                                    showMessage("فشلت الإضافة: ${e.message}")
                                }
                            }
                        },
                        onUpdateEmployee = { updated ->
                            scope.launch {
                                try {
                                    employeesRepository.updateEmployee(updated)
                                    showMessage("تم تحديث بيانات الموظف")
                                } catch (e: Exception) {
                                    showMessage("فشل التحديث: ${e.message}")
                                }
                            }
                        },
                        onDeleteEmployee = { empId ->
                            scope.launch {
                                try {
                                    employeesRepository.deleteEmployee(empId)
                                    showMessage("تم حذف الموظف")
                                } catch (e: Exception) {
                                    showMessage("فشل الحذف: ${e.message}")
                                }
                            }
                        },
                        onImportEmployeesBatch = { imported ->
                            employeesRepository.addEmployeesBatch(imported)
                        },
                        onGenerateMonthlyPayroll = { monthYear, activeEmps, existing ->
                            payrollRepository.generateMonthlyPayroll(monthYear, activeEmps, existing)
                        },
                        onUpdatePayrollRecord = { updated ->
                            scope.launch {
                                try {
                                    payrollRepository.updatePayrollRecord(updated)
                                    showMessage("تم تحديث سجل الراتب")
                                } catch (e: Exception) {
                                    showMessage("فشل التحديث: ${e.message}")
                                }
                            }
                        },
                        onMarkAllPaid = { records, paymentDate ->
                            scope.launch {
                                try {
                                    payrollRepository.markAllPaid(records, paymentDate)
                                    showMessage("تم اعتماد وصرف جميع الرواتب المحددة")
                                } catch (e: Exception) {
                                    showMessage("فشل الصرف: ${e.message}")
                                }
                            }
                        },
                        onDeletePayrollRecord = { recordId ->
                            scope.launch {
                                try {
                                    payrollRepository.deletePayrollRecord(recordId)
                                    showMessage("تم حذف سجل الراتب")
                                } catch (e: Exception) {
                                    showMessage("فشل الحذف: ${e.message}")
                                }
                            }
                        },
                        onShowMessage = ::showMessage
                    )
                }

                AppNavTab.MARKETING -> {
                    MarketingHubScreen(
                        clients = clientsList,
                        ads = adsList,
                        templates = templatesList,
                        isAdsLoading = isAdsLoading,
                        overdueCount = overdueCount,
                        templatesRepository = templatesRepository,
                        aiRepository = aiRepository,
                        onUpdateLastContact = { client, dateStr ->
                            scope.launch {
                                try {
                                    clientsRepository.updateLastContactDate(client.id, dateStr)
                                    showMessage("تم تحديث تاريخ التواصل لـ ${client.name}")
                                } catch (e: Exception) {
                                    showMessage("فشل التحديث: ${e.message}")
                                }
                            }
                        },
                        onAddAd = { newAd ->
                            scope.launch {
                                try {
                                    adPostsRepository.addAd(newAd)
                                    showMessage("تم حفظ الإعلان بنجاح")
                                } catch (e: Exception) {
                                    showMessage("فشل حفظ الإعلان: ${e.message}")
                                }
                            }
                        },
                        onUpdateAd = { updated ->
                            scope.launch {
                                try {
                                    adPostsRepository.updateAd(updated)
                                    showMessage("تم تحديث الإعلان")
                                } catch (e: Exception) {
                                    showMessage("فشل التحديث: ${e.message}")
                                }
                            }
                        },
                        onDeleteAd = { adId ->
                            scope.launch {
                                try {
                                    adPostsRepository.deleteAd(adId)
                                    showMessage("تم حذف الإعلان")
                                } catch (e: Exception) {
                                    showMessage("فشل الحذف: ${e.message}")
                                }
                            }
                        },
                        onUpdateAdStatus = { adId, newStatus, pubDate ->
                            scope.launch {
                                try {
                                    adPostsRepository.updateAdStatus(adId, newStatus, pubDate)
                                    showMessage("تم تحديث حالة الإعلان إلى: $newStatus")
                                } catch (e: Exception) {
                                    showMessage("فشل التحديث: ${e.message}")
                                }
                            }
                        }
                    )
                }

                AppNavTab.ANALYTICS -> {
                    AnalyticsScreen(
                        clients = clientsList,
                        transactions = transactionsList,
                        employees = employeesList,
                        payrolls = payrollsList
                    )
                }
            }
        }
    }
}
