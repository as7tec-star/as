package com.example.ui.screens.hubs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdPost
import com.example.data.model.BusinessTransaction
import com.example.data.model.Client
import com.example.data.model.MessageTemplate
import com.example.data.model.Product
import com.example.data.model.Quote
import com.example.data.repository.AiMarketingRepository
import com.example.data.repository.TemplatesRepository
import com.example.ui.screens.accounting.AccountingScreen
import com.example.ui.screens.ads.AdStudioScreen
import com.example.ui.screens.followup.FollowUpScreen
import com.example.ui.screens.inventory.InventoryScreen
import com.example.ui.screens.messaging.MessagingScreen
import com.example.ui.screens.quotes.QuotesScreen
import com.example.ui.theme.HubNavy

@Composable
fun FinanceHubScreen(
    transactions: List<BusinessTransaction>,
    quotes: List<Quote>,
    clients: List<Client>,
    products: List<Product>,
    isTransactionsLoading: Boolean,
    isQuotesLoading: Boolean,
    isProductsLoading: Boolean,
    onAddTransaction: (BusinessTransaction) -> Unit,
    onUpdateTransaction: (BusinessTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onUpdateTransactionStatus: (String, String) -> Unit,
    onImportTransactionsBatch: suspend (List<BusinessTransaction>) -> Int,
    onImportClientsBatch: suspend (List<Client>) -> Int,
    onAddQuote: (Quote) -> Unit,
    onUpdateQuote: (Quote) -> Unit,
    onDeleteQuote: (String) -> Unit,
    onUpdateQuoteStatus: (String, String) -> Unit,
    onAddProduct: (Product) -> Unit,
    onUpdateProduct: (Product) -> Unit,
    onAdjustProductQuantity: (String, Double) -> Unit,
    onToggleProductAlerts: (String, Boolean) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onImportProductsBatch: suspend (List<Product>) -> Int,
    onShowMessage: (String) -> Unit
) {
    var selectedSection by remember { mutableIntStateOf(0) }
    val lowStockCount = remember(products) { products.count { it.isLowStock() } }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HubSegmentButton(
                    title = "المبيعات والضرائب",
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    modifier = Modifier.weight(1.1f)
                )
                HubSegmentButton(
                    title = if (lowStockCount > 0) "المخزون ($lowStockCount)" else "المخزون",
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    alertHighlight = lowStockCount > 0 && selectedSection != 1,
                    modifier = Modifier.weight(0.95f)
                )
                HubSegmentButton(
                    title = "عروض الأسعار (${quotes.size})",
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    modifier = Modifier.weight(1.05f)
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedSection) {
                0 -> AccountingScreen(
                    transactions = transactions,
                    clients = clients,
                    products = products,
                    isLoading = isTransactionsLoading,
                    onAddTransaction = onAddTransaction,
                    onUpdateTransaction = onUpdateTransaction,
                    onDeleteTransaction = onDeleteTransaction,
                    onUpdatePaymentStatus = onUpdateTransactionStatus,
                    onImportTransactionsBatch = onImportTransactionsBatch,
                    onImportClientsBatch = onImportClientsBatch,
                    onOpenInventory = { selectedSection = 1 },
                    onShowMessage = onShowMessage
                )
                1 -> InventoryScreen(
                    products = products,
                    isLoading = isProductsLoading,
                    onAddProduct = onAddProduct,
                    onUpdateProduct = onUpdateProduct,
                    onAdjustQuantity = onAdjustProductQuantity,
                    onToggleAlerts = onToggleProductAlerts,
                    onDeleteProduct = onDeleteProduct,
                    onImportProductsBatch = onImportProductsBatch,
                    onShowMessage = onShowMessage
                )
                2 -> QuotesScreen(
                    quotes = quotes,
                    clients = clients,
                    isLoading = isQuotesLoading,
                    onAddQuote = onAddQuote,
                    onUpdateQuote = onUpdateQuote,
                    onDeleteQuote = onDeleteQuote,
                    onUpdateStatus = onUpdateQuoteStatus
                )
            }
        }
    }
}

@Composable
fun MarketingHubScreen(
    clients: List<Client>,
    ads: List<AdPost>,
    templates: List<MessageTemplate>,
    isAdsLoading: Boolean,
    overdueCount: Int,
    templatesRepository: TemplatesRepository,
    aiRepository: AiMarketingRepository,
    onUpdateLastContact: (Client, String) -> Unit,
    onAddAd: (AdPost) -> Unit,
    onUpdateAd: (AdPost) -> Unit,
    onDeleteAd: (String) -> Unit,
    onUpdateAdStatus: (String, String, String) -> Unit
) {
    var selectedSection by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val followUpLabel = if (overdueCount > 0) "مهام اليوم ($overdueCount)" else "مهام اليوم"
                HubSegmentButton(
                    title = followUpLabel,
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    modifier = Modifier.weight(1f)
                )
                HubSegmentButton(
                    title = "استوديو الإعلانات",
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    modifier = Modifier.weight(1f)
                )
                HubSegmentButton(
                    title = "المراسلة",
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    modifier = Modifier.weight(0.9f)
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedSection) {
                0 -> FollowUpScreen(
                    clients = clients,
                    onUpdateLastContact = onUpdateLastContact
                )
                1 -> AdStudioScreen(
                    ads = ads,
                    isLoading = isAdsLoading,
                    aiRepository = aiRepository,
                    onAddAd = onAddAd,
                    onUpdateAd = onUpdateAd,
                    onDeleteAd = onDeleteAd,
                    onUpdateStatus = onUpdateAdStatus
                )
                2 -> MessagingScreen(
                    clients = clients,
                    templates = templates,
                    templatesRepository = templatesRepository,
                    aiRepository = aiRepository
                )
            }
        }
    }
}

@Composable
private fun HubSegmentButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alertHighlight: Boolean = false
) {
    val bgColor = when {
        selected -> HubNavy
        alertHighlight -> Color(0xFFFEE2E2)
        else -> Color(0xFFF1F5F9)
    }
    val textColor = when {
        selected -> Color.White
        alertHighlight -> Color(0xFFB91C1C)
        else -> Color(0xFF475569)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (selected || alertHighlight) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}
