package com.example.ui.screens.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.ui.components.SourceBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.ColorWhatsApp
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal
import com.example.util.IntentUtils

@Composable
fun ClientKanbanView(
    clients: List<Client>,
    onEditClient: (Client) -> Unit,
    onDeleteClient: (Client) -> Unit,
    onMoveStatus: (Client, String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Client.STATUSES.forEach { columnStatus ->
            val columnClients = clients.filter { it.status == columnStatus }
            val totalValue = columnClients.sumOf { it.dealValue }

            KanbanColumn(
                statusTitle = columnStatus,
                clients = columnClients,
                totalValue = totalValue,
                onEditClient = onEditClient,
                onDeleteClient = onDeleteClient,
                onMoveStatus = onMoveStatus,
                onWhatsAppClick = { client ->
                    IntentUtils.openWhatsApp(
                        context,
                        client.phone,
                        "مرحباً ${client.name}، يسعدنا تواصلك معنا."
                    )
                }
            )
        }
    }
}

@Composable
fun KanbanColumn(
    statusTitle: String,
    clients: List<Client>,
    totalValue: Double,
    onEditClient: (Client) -> Unit,
    onDeleteClient: (Client) -> Unit,
    onMoveStatus: (Client, String) -> Unit,
    onWhatsAppClick: (Client) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF1F5F9),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            // Column Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = statusTitle)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${clients.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF334155)
                        )
                    }
                }

                if (totalValue > 0) {
                    Text(
                        text = "${totalValue.toInt()} ر.س",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = HubTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Client Cards List
            if (clients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا يوجد عملاء في هذه المرحلة",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clients, key = { it.id }) { client ->
                        KanbanClientCard(
                            client = client,
                            onEdit = { onEditClient(client) },
                            onDelete = { onDeleteClient(client) },
                            onMoveStatus = { newStatus -> onMoveStatus(client, newStatus) },
                            onWhatsApp = { onWhatsAppClick(client) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KanbanClientCard(
    client: Client,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveStatus: (String) -> Unit,
    onWhatsApp: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var moveMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = client.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Bold,
                    color = HubNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "خيارات العميل",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("تعديل") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("نقل إلى مرحلة أخرى...") },
                            onClick = {
                                menuExpanded = false
                                moveMenuExpanded = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف العميل", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = moveMenuExpanded,
                        onDismissRequest = { moveMenuExpanded = false }
                    ) {
                        Client.STATUSES.forEach { targetStatus ->
                            if (targetStatus != client.status) {
                                DropdownMenuItem(
                                    text = { Text("إلى: $targetStatus") },
                                    onClick = {
                                        moveMenuExpanded = false
                                        onMoveStatus(targetStatus)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = client.phone,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569)
            )

            if (client.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = client.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SourceBadge(source = client.source)

                if (client.dealValue > 0) {
                    Text(
                        text = "${client.dealValue.toInt()} ر.س",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = HubTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row: WhatsApp quick tap & Next stage arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WhatsApp quick button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ColorWhatsApp.copy(alpha = 0.1f))
                        .clickable { onWhatsApp() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "واتساب",
                            tint = Color(0xFF1E7E34),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "واتساب",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E7E34)
                        )
                    }
                }

                // Stage Stepper
                val currentIndex = Client.STATUSES.indexOf(client.status)
                Row {
                    if (currentIndex > 0) {
                        IconButton(
                            onClick = { onMoveStatus(Client.STATUSES[currentIndex - 1]) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "المرحلة السابقة",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF64748B)
                            )
                        }
                    }
                    if (currentIndex < Client.STATUSES.size - 1) {
                        IconButton(
                            onClick = { onMoveStatus(Client.STATUSES[currentIndex + 1]) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "المرحلة التالية",
                                modifier = Modifier.size(16.dp),
                                tint = HubNavy
                            )
                        }
                    }
                }
            }
        }
    }
}
