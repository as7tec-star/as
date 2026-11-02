package com.example.ui.screens.clients

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal

enum class ClientsViewMode {
    KANBAN, TABLE
}

@Composable
fun ClientsScreen(
    clients: List<Client>,
    isLoading: Boolean,
    onAddClient: (Client) -> Unit,
    onUpdateClient: (Client) -> Unit,
    onDeleteClient: (String) -> Unit,
    onMoveStatus: (Client, String) -> Unit
) {
    var viewMode by remember { mutableStateOf(ClientsViewMode.KANBAN) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("الكل") }
    var selectedSourceFilter by remember { mutableStateOf("الكل") }

    var clientToEdit by remember { mutableStateOf<Client?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var clientToDelete by remember { mutableStateOf<Client?>(null) }

    val filteredClients = remember(clients, searchQuery, selectedStatusFilter, selectedSourceFilter) {
        clients.filter { client ->
            val matchesQuery = searchQuery.isBlank() ||
                    client.name.contains(searchQuery, ignoreCase = true) ||
                    client.phone.contains(searchQuery)
            val matchesStatus = selectedStatusFilter == "الكل" || client.status == selectedStatusFilter
            val matchesSource = selectedSourceFilter == "الكل" || client.source == selectedSourceFilter
            matchesQuery && matchesStatus && matchesSource
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search and View Switcher Header
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث بالاسم أو رقم الهاتف...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "مسح")
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // View mode toggle
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ViewToggleButton(
                                    icon = Icons.Default.ViewKanban,
                                    isSelected = viewMode == ClientsViewMode.KANBAN,
                                    onClick = { viewMode = ClientsViewMode.KANBAN },
                                    label = "مسار"
                                )
                                ViewToggleButton(
                                    icon = Icons.Default.TableRows,
                                    isSelected = viewMode == ClientsViewMode.TABLE,
                                    onClick = { viewMode = ClientsViewMode.TABLE },
                                    label = "جدول"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Filters Scroll Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statusOptions = listOf("الكل") + Client.STATUSES
                        statusOptions.forEach { statusOption ->
                            val isSelected = selectedStatusFilter == statusOption
                            FilterChip(
                                text = statusOption,
                                isSelected = isSelected,
                                onClick = { selectedStatusFilter = statusOption }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Source Filters Scroll Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val sourceOptions = listOf("الكل") + Client.SOURCES
                        sourceOptions.forEach { sourceOption ->
                            val isSelected = selectedSourceFilter == sourceOption
                            FilterChip(
                                text = "المصدر: $sourceOption",
                                isSelected = isSelected,
                                onClick = { selectedSourceFilter = sourceOption }
                            )
                        }
                    }
                }
            }

            // Main Content Area
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = HubNavy)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (viewMode) {
                        ClientsViewMode.KANBAN -> {
                            ClientKanbanView(
                                clients = filteredClients,
                                onEditClient = { clientToEdit = it },
                                onDeleteClient = { clientToDelete = it },
                                onMoveStatus = onMoveStatus
                            )
                        }
                        ClientsViewMode.TABLE -> {
                            ClientTableView(
                                clients = filteredClients,
                                onEditClient = { clientToEdit = it },
                                onDeleteClient = { clientToDelete = it }
                            )
                        }
                    }
                }
            }
        }

        // Add Client FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_client_button"),
            containerColor = HubNavy,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة عميل")
        }
    }

    // Add/Edit Dialog
    if (showAddDialog) {
        ClientDialog(
            clientToEdit = null,
            onDismiss = { showAddDialog = false },
            onSave = { newClient ->
                showAddDialog = false
                onAddClient(newClient)
            }
        )
    }

    if (clientToEdit != null) {
        ClientDialog(
            clientToEdit = clientToEdit,
            onDismiss = { clientToEdit = null },
            onSave = { updated ->
                clientToEdit = null
                onUpdateClient(updated)
            }
        )
    }

    // Delete Confirmation Dialog
    if (clientToDelete != null) {
        AlertDialog(
            onDismissRequest = { clientToDelete = null },
            title = { Text("تأكيد حذف العميل", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف العميل '${clientToDelete?.name}' نهائياً؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        clientToDelete?.id?.let { onDeleteClient(it) }
                        clientToDelete = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { clientToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ViewToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) HubNavy else Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                ),
                color = if (isSelected) HubNavy else Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) HubNavy else Color(0xFFF1F5F9))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            ),
            color = if (isSelected) Color.White else Color(0xFF475569)
        )
    }
}
