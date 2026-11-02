package com.example.ui.screens.quotes

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.data.model.Quote
import com.example.ui.theme.ColorWhatsApp
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal
import com.example.util.GoogleSheetsUtils
import com.example.util.IntentUtils

@Composable
fun QuotesScreen(
    quotes: List<Quote>,
    clients: List<Client>,
    isLoading: Boolean,
    onAddQuote: (Quote) -> Unit,
    onUpdateQuote: (Quote) -> Unit,
    onDeleteQuote: (String) -> Unit,
    onUpdateStatus: (String, String) -> Unit
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var quoteToEdit by remember { mutableStateOf<Quote?>(null) }
    var quoteToDelete by remember { mutableStateOf<Quote?>(null) }

    val totalValue = remember(quotes) { quotes.sumOf { it.grandTotal } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            // Summary Header Card
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(HubNavy.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = HubNavy,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "إدارة عروض الأسعار (Quotes)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HubNavy
                                )
                                Text(
                                    text = "التكامل السحابي مع Google Sheets وواتساب",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // Open Google Sheets Button
                        OutlinedButton(
                            onClick = { GoogleSheetsUtils.openGoogleSheet(context, "") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = Color(0xFF107C41),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Google Sheets", fontSize = 11.sp, color = Color(0xFF107C41), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "إجمالي العروض: ${quotes.size} عرض سعر",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "القيمة الإجمالية: ${totalValue.toInt()} ر.س",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = HubTeal
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
            } else if (quotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد عروض أسعار مسجلة حتى الآن",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HubNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اضغط على زر (+) لإنشاء عرض سعر احترافي بقوالب جاهزة ومزامنة Google Sheets",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(quotes, key = { it.id }) { quote ->
                        QuoteCard(
                            quote = quote,
                            onEdit = { quoteToEdit = quote },
                            onDelete = { quoteToDelete = quote },
                            onStatusChange = { newStatus -> onUpdateStatus(quote.id, newStatus) },
                            onSendWhatsApp = {
                                val quoteText = GoogleSheetsUtils.formatQuoteText(quote)
                                IntentUtils.openWhatsApp(context, quote.clientPhone, quoteText)
                            },
                            onExportToSheets = {
                                GoogleSheetsUtils.shareQuoteCsv(context, quote)
                            },
                            onOpenSheet = {
                                GoogleSheetsUtils.openGoogleSheet(context, quote.googleSheetUrl)
                            },
                            onCopyText = {
                                val quoteText = GoogleSheetsUtils.formatQuoteText(quote)
                                IntentUtils.copyToClipboard(context, quoteText, "عرض سعر ${quote.quoteNumber}")
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_quote_button"),
            containerColor = HubNavy,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "إنشاء عرض سعر")
        }
    }

    if (showCreateDialog) {
        QuoteDialog(
            quoteToEdit = null,
            clients = clients,
            onDismiss = { showCreateDialog = false },
            onSaveQuote = { newQuote ->
                showCreateDialog = false
                onAddQuote(newQuote)
            }
        )
    }

    if (quoteToEdit != null) {
        QuoteDialog(
            quoteToEdit = quoteToEdit,
            clients = clients,
            onDismiss = { quoteToEdit = null },
            onSaveQuote = { updated ->
                quoteToEdit = null
                onUpdateQuote(updated)
            }
        )
    }

    if (quoteToDelete != null) {
        AlertDialog(
            onDismissRequest = { quoteToDelete = null },
            title = { Text("تأكيد حذف عرض السعر", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف عرض السعر رقم '${quoteToDelete?.quoteNumber}' الخاص بالعميل '${quoteToDelete?.clientName}'؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        quoteToDelete?.id?.let { onDeleteQuote(it) }
                        quoteToDelete = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { quoteToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun QuoteCard(
    quote: Quote,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (String) -> Unit,
    onSendWhatsApp: () -> Unit,
    onExportToSheets: () -> Unit,
    onOpenSheet: () -> Unit,
    onCopyText: () -> Unit
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }
    val items = quote.getTypedItems()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Quote Number, Date, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = quote.quoteNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HubNavy
                    )
                    Text(
                        text = "العميل: ${quote.clientName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (quote.status) {
                                    "مقبول" -> Color(0xFFDCFCE7)
                                    "مرفوض" -> Color(0xFFFEE2E2)
                                    "مرسل" -> Color(0xFFFEF3C7)
                                    else -> Color(0xFFF1F5F9)
                                }
                            )
                            .clickable { statusMenuExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = quote.status,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (quote.status) {
                                "مقبول" -> Color(0xFF15803D)
                                "مرفوض" -> Color(0xFFB91C1C)
                                "مرسل" -> Color(0xFFB45309)
                                else -> Color(0xFF475569)
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        Quote.STATUS_LIST.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = {
                                    statusMenuExpanded = false
                                    onStatusChange(s)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Items Summary
            if (items.isNotEmpty()) {
                Text(
                    text = "البنود (${items.size}): ${items.joinToString(" • ") { it.name }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grand Total and Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الإجمالي: ${quote.grandTotal} ر.س",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = HubTeal
                )

                Text(
                    text = "التاريخ: ${quote.date}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Actions Row: WhatsApp, Google Sheets, Copy, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WhatsApp Button
                Button(
                    onClick = onSendWhatsApp,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E7E34)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("واتساب", fontSize = 11.sp)
                }

                // Sheets Export Button
                OutlinedButton(
                    onClick = onExportToSheets,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF107C41), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Google Sheets", fontSize = 11.sp, color = Color(0xFF107C41))
                }

                Row {
                    IconButton(onClick = onCopyText, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ النص", tint = HubNavy, modifier = Modifier.size(16.dp))
                    }
                    if (quote.googleSheetUrl.isNotBlank()) {
                        IconButton(onClick = onOpenSheet, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "فتح Sheet", tint = HubTeal, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
