package com.example.ui.screens.analytics

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.ui.components.SourceBadge
import com.example.ui.theme.HubAmber
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal
import java.util.concurrent.TimeUnit

@Composable
fun AnalyticsScreen(
    clients: List<Client>
) {
    val totalClients = clients.size
    val wonClients = clients.count { it.status == "تم البيع" }
    val conversionRate = remember(totalClients, wonClients) {
        if (totalClients > 0) (wonClients.toDouble() / totalClients * 100).toInt() else 0
    }

    // New clients this week and month based on createdAt or client count
    val newThisWeek = remember(clients) {
        val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        clients.count { (it.createdAt?.toDate()?.time ?: System.currentTimeMillis()) >= weekAgo }
    }

    val newThisMonth = remember(clients) {
        val monthAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        clients.count { (it.createdAt?.toDate()?.time ?: System.currentTimeMillis()) >= monthAgo }
    }

    val totalDealValue = remember(clients) {
        clients.filter { it.status == "تم البيع" }.sumOf { it.dealValue }
    }

    val pipelineValue = remember(clients) {
        clients.filter { it.status == "مهتم" || it.status == "تفاوض" }.sumOf { it.dealValue }
    }

    // Source breakdown
    val sourceCounts = remember(clients) {
        Client.SOURCES.associateWith { source ->
            clients.count { it.source == source }
        }.toList().sortedByDescending { it.second }
    }

    val bestSource = remember(sourceCounts) {
        sourceCounts.firstOrNull()?.first ?: "حراج"
    }

    // Status breakdown
    val statusCounts = remember(clients) {
        Client.STATUSES.associateWith { status ->
            clients.count { it.status == status }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "لوحة مؤشرات الأداء والمبيعات",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = HubNavy
        )
        Text(
            text = "نظرة شمولية على تحويلات العملاء والقنوات الأكثر فعالية",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Key Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "معدل التحويل",
                value = "$conversionRate%",
                sub = "$wonClients تم البيع من $totalClients",
                icon = Icons.Default.Percent,
                iconColor = HubTeal,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "أفضل مصدر",
                value = bestSource,
                sub = "${sourceCounts.firstOrNull()?.second ?: 0} عميل",
                icon = Icons.Default.Leaderboard,
                iconColor = HubAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "عملاء هذا الأسبوع",
                value = "$newThisWeek",
                sub = "خلال آخر 7 أيام",
                icon = Icons.Default.GroupAdd,
                iconColor = HubNavy,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "عملاء هذا الشهر",
                value = "$newThisMonth",
                sub = "خلال آخر 30 يوماً",
                icon = Icons.Default.TrendingUp,
                iconColor = Color(0xFF2B6CB0),
                modifier = Modifier.weight(1f)
            )
        }

        if (totalDealValue > 0 || pipelineValue > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "مبيعات محققة",
                    value = "${totalDealValue.toInt()} ر.س",
                    sub = "صفقات تم إغلاقها",
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color(0xFF2F855A),
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "صفقات قيد التفاوض",
                    value = "${pipelineValue.toInt()} ر.س",
                    sub = "فرص بيعية نشطة",
                    icon = Icons.Default.AttachMoney,
                    iconColor = HubAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Visual Source Performance Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "توزيع العملاء بحسب المنصة والمصدر",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "أي المنصات تدر أكبر عدد من المشترين لمتجرك؟",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(14.dp))

                sourceCounts.forEach { (sourceName, count) ->
                    val percentage = if (totalClients > 0) (count.toFloat() / totalClients) else 0f
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SourceBadge(source = sourceName)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$count عميل",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = "${(percentage * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = percentage.coerceAtLeast(0.02f))
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HubNavy)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Visual Pipeline Funnel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "قمع مسار المبيعات (Pipeline Funnel)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Spacer(modifier = Modifier.height(12.dp))

                Client.STATUSES.forEach { statusName ->
                    val count = statusCounts[statusName] ?: 0
                    val fraction = if (totalClients > 0) (count.toFloat() / totalClients) else 0f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusName,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF334155),
                            modifier = Modifier.width(90.dp)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = fraction.coerceAtLeast(0.03f))
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (statusName) {
                                            "تم البيع" -> Color(0xFF38A169)
                                            "خسرناه" -> Color(0xFFE53E3E)
                                            "تفاوض" -> HubAmber
                                            "مهتم" -> Color(0xFF805AD5)
                                            else -> HubNavy
                                        }
                                    ),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (count > 0) {
                                    Text(
                                        text = "$count ",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    sub: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                fontWeight = FontWeight.Bold,
                color = HubNavy
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
