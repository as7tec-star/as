package com.example.ui.screens.followup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.ui.components.SourceBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.ColorWhatsApp
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal
import com.example.util.IntentUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun FollowUpScreen(
    clients: List<Client>,
    onUpdateLastContact: (Client, String) -> Unit
) {
    val context = LocalContext.current
    var thresholdDays by remember { mutableIntStateOf(3) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date()) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }

    // Filter clients who have not been contacted for >= thresholdDays or never contacted (ignoring won/lost)
    val overdueClients = remember(clients, thresholdDays) {
        val nowMillis = System.currentTimeMillis()
        clients.filter { client ->
            if (client.status == "تم البيع" || client.status == "خسرناه") {
                false
            } else if (client.lastContactDate.isBlank()) {
                true
            } else {
                try {
                    val date = dateFormat.parse(client.lastContactDate)
                    if (date != null) {
                        val diffMillis = nowMillis - date.time
                        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)
                        diffDays >= thresholdDays
                    } else {
                        true
                    }
                } catch (e: Exception) {
                    true
                }
            }
        }.sortedByDescending {
            // prioritize high deal values and negotiating/interested stages
            when (it.status) {
                "تفاوض" -> 4
                "مهتم" -> 3
                "تم التواصل" -> 2
                "جديد" -> 1
                else -> 0
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        // Summary & Days Setting Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 2.dp
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مهام المتابعة اليومية",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HubNavy
                            )
                            Text(
                                text = "العملاء الذين يحتاجون متابعة وتواصل نشط",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${overdueClients.size} عميل",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFDC2626)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                // Threshold Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التنبيه عند مرور:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 3, 5, 7).forEach { days ->
                            val isSelected = thresholdDays == days
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) HubNavy else Color(0xFFF1F5F9))
                                    .clickable { thresholdDays = days }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "$days أيام",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSelected) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (overdueClients.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "تمت المتابعة",
                        tint = HubTeal,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "رائع! لا يوجد أي عملاء متأخرين في المتابعة حالياً.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HubNavy
                    )
                    Text(
                        text = "جميع العملاء تم التواصل معهم خلال فترة $thresholdDays أيام.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(overdueClients, key = { it.id }) { client ->
                    OverdueClientCard(
                        client = client,
                        todayStr = todayStr,
                        onWhatsApp = {
                            IntentUtils.openWhatsApp(
                                context,
                                client.phone,
                                "مرحباً ${client.name}، نود الاطمئنان عليك ومتابعة استفسارك السابق."
                            )
                            onUpdateLastContact(client, todayStr)
                        },
                        onMarkContactedToday = {
                            onUpdateLastContact(client, todayStr)
                        },
                        onCalendarReminder = {
                            IntentUtils.createCalendarEvent(
                                context,
                                client.name,
                                "متابعة تواصل مع العميل: ${client.name}\nهاتف: ${client.phone}\nملاحظات: ${client.notes}"
                            )
                        },
                        onGoogleTaskReminder = {
                            IntentUtils.createReminderOrTask(
                                context,
                                client.name,
                                client.phone,
                                client.notes
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OverdueClientCard(
    client: Client,
    todayStr: String,
    onWhatsApp: () -> Unit,
    onMarkContactedToday: () -> Unit,
    onCalendarReminder: () -> Unit,
    onGoogleTaskReminder: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HubNavy
                    )
                    Text(
                        text = client.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SourceBadge(source = client.source)
                    StatusBadge(status = client.status)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Last contact note
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (client.lastContactDate.isNotBlank()) {
                        "آخر تواصل كان بتاريخ: ${client.lastContactDate}"
                    } else {
                        "لم يتم تسجيل أي تواصل سابق مع هذا العميل!"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB45309)
                )
            }

            if (client.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ملاحظات: ${client.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // One-click WhatsApp message
                Button(
                    onClick = onWhatsApp,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E7E34)),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("واتساب ومتابعة", fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Google Calendar Reminder Button
                    OutlinedButton(
                        onClick = onCalendarReminder,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "تقويم جوجل",
                            tint = HubNavy,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("التقويم", fontSize = 11.sp, color = HubNavy)
                    }

                    // Google Tasks / Note Reminder Button
                    OutlinedButton(
                        onClick = onGoogleTaskReminder,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = "مهام جوجل",
                            tint = HubTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مهام", fontSize = 11.sp, color = HubTeal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick "Mark Contacted Today" button
            TextButton(
                onClick = onMarkContactedToday,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "تحديث تاريخ التواصل لليوم دون إرسال",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
