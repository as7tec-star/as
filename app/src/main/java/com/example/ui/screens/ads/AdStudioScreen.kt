package com.example.ui.screens.ads

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.OpenInBrowser
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.AdPost
import com.example.data.repository.AiMarketingRepository
import com.example.ui.components.AdStatusBadge
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal
import com.example.util.IntentUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdStudioScreen(
    ads: List<AdPost>,
    isLoading: Boolean,
    aiRepository: AiMarketingRepository,
    onAddAd: (AdPost) -> Unit,
    onUpdateAd: (AdPost) -> Unit,
    onDeleteAd: (String) -> Unit,
    onUpdateStatus: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var adToEdit by remember { mutableStateOf<AdPost?>(null) }
    var adToDelete by remember { mutableStateOf<AdPost?>(null) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date()) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab Switcher: "الإعلانات والمنشورات" vs "تقويم المحتوى"
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = HubNavy
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("جدول المنشورات (${ads.size})", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تقويم المحتوى", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
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
            } else if (selectedTab == 1) {
                ContentCalendarView(
                    ads = ads,
                    onAdClick = { adToEdit = it },
                    onRenewAd = { ad ->
                        onUpdateStatus(ad.id, "نُشر", todayStr)
                    }
                )
            } else {
                // Ads Table / List
                if (ads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "لم تقم بإنشاء أي إعلانات بعد",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HubNavy
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "اضغط على زر (+) لتوليد إعلانات ذكية مخصصة لجميع المنصات بـ Gemini",
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
                        items(ads, key = { it.id }) { ad ->
                            AdPostCard(
                                ad = ad,
                                onEdit = { adToEdit = ad },
                                onDelete = { adToDelete = ad },
                                onStatusChange = { newStatus ->
                                    val pubDate = if (newStatus == "نُشر") todayStr else ad.publishedAt
                                    onUpdateStatus(ad.id, newStatus, pubDate)
                                },
                                onCopyPlatformText = { platform, text ->
                                    IntentUtils.copyToClipboard(context, text, platform)
                                },
                                onOpenPlatform = { platform ->
                                    IntentUtils.openPlatformUrl(context, platform, ad.postUrl)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button to Create New Ad
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("create_ad_button"),
            containerColor = HubTeal,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة وتوليد إعلان")
        }
    }

    if (showCreateDialog) {
        CreateAdDialog(
            adToEdit = null,
            aiRepository = aiRepository,
            onDismiss = { showCreateDialog = false },
            onSaveAd = { newAd ->
                showCreateDialog = false
                onAddAd(newAd)
            }
        )
    }

    if (adToEdit != null) {
        CreateAdDialog(
            adToEdit = adToEdit,
            aiRepository = aiRepository,
            onDismiss = { adToEdit = null },
            onSaveAd = { updated ->
                adToEdit = null
                onUpdateAd(updated)
            }
        )
    }

    if (adToDelete != null) {
        AlertDialog(
            onDismissRequest = { adToDelete = null },
            title = { Text("تأكيد حذف الإعلان", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف إعلان '${adToDelete?.title}'؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        adToDelete?.id?.let { onDeleteAd(it) }
                        adToDelete = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { adToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun AdPostCard(
    ad: AdPost,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (String) -> Unit,
    onCopyPlatformText: (String, String) -> Unit,
    onOpenPlatform: (String) -> Unit
) {
    val cardContext = LocalContext.current
    var statusMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Title, Price, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ad.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HubNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (ad.price.isNotBlank()) {
                            Text(
                                text = "${ad.price} ر.س",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = HubTeal
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (ad.city.isNotBlank()) {
                            Text(
                                text = "• ${ad.city}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                Box {
                    Box(modifier = Modifier.clickable { statusMenuExpanded = true }) {
                        AdStatusBadge(status = ad.status)
                    }

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        AdPost.STATUS_LIST.forEach { statusOption ->
                            DropdownMenuItem(
                                text = { Text(statusOption) },
                                onClick = {
                                    statusMenuExpanded = false
                                    onStatusChange(statusOption)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Short description preview
            Text(
                text = ad.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Platform copy snippets chips
            if (ad.generatedVersions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "النسخ الجاهزة للنشر بنقرة:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ad.generatedVersions.forEach { (platform, text) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = platform,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = HubNavy
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onCopyPlatformText(platform, text) },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "نسخ صيغة $platform",
                                        tint = HubNavy,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onOpenPlatform(platform) },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInBrowser,
                                        contentDescription = "فتح $platform",
                                        tint = HubTeal,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer row: status changer, edit, delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ad.postUrl.isNotBlank()) {
                        IconButton(
                            onClick = { IntentUtils.openPlatformUrl(cardContext, "", ad.postUrl) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "فتح الرابط", tint = HubNavy)
                        }
                    }
                    if (ad.publishedAt.isNotBlank()) {
                        Text(
                            text = "نشر في: ${ad.publishedAt}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل الإعلان", tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف الإعلان", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
