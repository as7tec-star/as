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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.credentials.CredentialManager
import com.example.data.model.AdPost
import com.example.data.model.Client
import com.example.data.model.MessageTemplate
import com.example.data.model.Quote
import com.example.data.repository.AdPostsRepository
import com.example.data.repository.AiMarketingRepository
import com.example.data.repository.ClientsRepository
import com.example.data.repository.QuotesRepository
import com.example.data.repository.TemplatesRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.AppNavTab
import com.example.ui.components.BottomNav
import com.example.ui.components.TopHeader
import com.example.ui.screens.ads.AdStudioScreen
import com.example.ui.screens.analytics.AnalyticsScreen
import com.example.ui.screens.clients.ClientsScreen
import com.example.ui.screens.followup.FollowUpScreen
import com.example.ui.screens.messaging.MessagingScreen
import com.example.ui.screens.quotes.QuotesScreen
import com.example.ui.theme.MarkazAmaliTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
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

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        MainBusinessHubScreen(
            userEmail = currentUser?.email ?: "",
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
    val adPostsRepository = remember { AdPostsRepository(context) }
    val templatesRepository = remember { TemplatesRepository(context) }
    val aiRepository = remember { AiMarketingRepository() }

    var currentTab by remember { mutableStateOf(AppNavTab.CLIENTS) }

    var clientsList by remember { mutableStateOf<List<Client>>(emptyList()) }
    var isClientsLoading by remember { mutableStateOf(true) }

    var quotesList by remember { mutableStateOf<List<Quote>>(emptyList()) }
    var isQuotesLoading by remember { mutableStateOf(true) }

    var adsList by remember { mutableStateOf<List<AdPost>>(emptyList()) }
    var isAdsLoading by remember { mutableStateOf(true) }

    var templatesList by remember { mutableStateOf<List<MessageTemplate>>(emptyList()) }

    // Realtime listeners
    LaunchedEffect(Unit) {
        clientsRepository.observeClients()
            .catch { e ->
                isClientsLoading = false
                scope.launch { snackbarHostState.showSnackbar("خطأ في تحميل العملاء: ${e.message}") }
            }
            .collect { list ->
                clientsList = list
                isClientsLoading = false
            }
    }

    LaunchedEffect(Unit) {
        adPostsRepository.observeAds()
            .catch { e ->
                isAdsLoading = false
                scope.launch { snackbarHostState.showSnackbar("خطأ في تحميل الإعلانات: ${e.message}") }
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
                scope.launch { snackbarHostState.showSnackbar("خطأ في تحميل عروض الأسعار: ${e.message}") }
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
                } catch (e: Exception) {
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
                followUpCount = overdueCount
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
                                    snackbarHostState.showSnackbar("تمت إضافة العميل بنجاح")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("فشلت الإضافة: ${e.message}")
                                }
                            }
                        },
                        onUpdateClient = { updated ->
                            scope.launch {
                                try {
                                    clientsRepository.updateClient(updated)
                                    snackbarHostState.showSnackbar("تم تحديث العميل بنجاح")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("فشل التحديث: ${e.message}")
                                }
                            }
                        },
                        onDeleteClient = { clientId ->
                            scope.launch {
                                try {
                                    clientsRepository.deleteClient(clientId)
                                    snackbarHostState.showSnackbar("تم حذف العميل بنجاح")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("فشل الحذف: ${e.message}")
                                }
                            }
                        },
                        onMoveStatus = { client, newStatus ->
                            scope.launch {
                                try {
                                    clientsRepository.updateClientStatus(client.id, newStatus)
                                    snackbarHostState.showSnackbar("تم نقل العميل إلى مرحلة: $newStatus")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("فشل نقل المرحلة: ${e.message}")
                                }
                            }
                        }
                    )
                }
                AppNavTab.QUOTES -> {
                    QuotesScreen(
                        quotes = quotesList,
                        clients = clientsList,
                        isLoading = isQuotesLoading,
                        onAddQuote = { newQuote ->
                            scope.launch {
                                try {
                                    quotesRepository.addQuote(newQuote)
                                    snackbarHostState.showSnackbar("تم حفظ عرض السعر بنجاح")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("فشل حفظ عرض السعر: ${e.message}")
                                }
                            }
                        },
                        onUpdateQuote = { updated ->
                            scope.launch {
                                try {
                                    quotesRepository.updateQuote(updated)
                                    snackbarHostState.showSnackbar("تم تحديث عرض السعر")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("فشل التحديث: ${e.message}")
                                }
                            }
                        },
                        onDeleteQuote = { quoteId ->
                            scope.launch {
                                try {
                                    quotesRepository.deleteQuote(quoteId)
                                    snackbarHostState.showSnackbar("تم حذف عرض السعر")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("فشل الحذف: ${e.message}")
                                }
                            }
                        },
                        onUpdateStatus = { quoteId, newStatus ->
                            scope.launch {
                                try {
                                    quotesRepository.updateQuoteStatus(quoteId, newStatus)
                                    snackbarHostState.showSnackbar("تم تحديث الحالة إلى: $newStatus")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("فشل التحديث: ${e.message}")
                                }
                            }
                        }
                    )
                }
                AppNavTab.FOLLOW_UP -> {
                    FollowUpScreen(
                        clients = clientsList,
                        onUpdateLastContact = { client, dateStr ->
                            scope.launch {
                                try {
                                    clientsRepository.updateLastContactDate(client.id, dateStr)
                                    snackbarHostState.showSnackbar("تم تحديث تاريخ التواصل لـ ${client.name}")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("فشل التحديث: ${e.message}")
                                }
                            }
                        }
                    )
                }
                AppNavTab.ADS -> {
                    AdStudioScreen(
                        ads = adsList,
                        isLoading = isAdsLoading,
                        aiRepository = aiRepository,
                        onAddAd = { newAd ->
                            scope.launch {
                                try {
                                    adPostsRepository.addAd(newAd)
                                    snackbarHostState.showSnackbar("تم حفظ الإعلان بنجاح")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("فشل حفظ الإعلان: ${e.message}")
                                }
                            }
                        },
                        onUpdateAd = { updated ->
                            scope.launch {
                                try {
                                    adPostsRepository.updateAd(updated)
                                    snackbarHostState.showSnackbar("تم تحديث الإعلان")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("فشل التحديث: ${e.message}")
                                }
                            }
                        },
                        onDeleteAd = { adId ->
                            scope.launch {
                                try {
                                    adPostsRepository.deleteAd(adId)
                                    snackbarHostState.showSnackbar("تم حذف الإعلان")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("فشل الحذف: ${e.message}")
                                }
                            }
                        },
                        onUpdateStatus = { adId, newStatus, pubDate ->
                            scope.launch {
                                try {
                                    adPostsRepository.updateAdStatus(adId, newStatus, pubDate)
                                    snackbarHostState.showSnackbar("تم تحديث حالة الإعلان إلى: $newStatus")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("فشل التحديث: ${e.message}")
                                }
                            }
                        }
                    )
                }
                AppNavTab.MESSAGING -> {
                    MessagingScreen(
                        clients = clientsList,
                        templates = templatesList,
                        templatesRepository = templatesRepository,
                        aiRepository = aiRepository
                    )
                }
                AppNavTab.ANALYTICS -> {
                    AnalyticsScreen(
                        clients = clientsList
                    )
                }
            }
        }
    }
}
