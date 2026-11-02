package com.example.ui.screens.messaging

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.data.model.MessageTemplate
import com.example.data.repository.AiMarketingRepository
import com.example.data.repository.TemplatesRepository
import com.example.ui.components.StatusBadge
import com.example.ui.theme.ColorWhatsApp
import com.example.ui.theme.HubAmber
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal
import com.example.util.IntentUtils
import kotlinx.coroutines.launch

@Composable
fun MessagingScreen(
    clients: List<Client>,
    templates: List<MessageTemplate>,
    templatesRepository: TemplatesRepository,
    aiRepository: AiMarketingRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) } // 0: رسالة لعميل محدد، 1: حملة جماعية، 2: القوالب

    LaunchedEffect(Unit) {
        templatesRepository.seedDefaultsIfEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Tab Row
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.White,
                contentColor = HubNavy
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("صياغة رسالة", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("حملة جماعية", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("قوالب الرسائل", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }
        }

        when (activeTab) {
            0 -> SingleClientMessagingTab(clients, templates, aiRepository)
            1 -> CampaignMessagingTab(clients, aiRepository)
            2 -> TemplatesManagementTab(templates, templatesRepository)
        }
    }
}

@Composable
fun SingleClientMessagingTab(
    clients: List<Client>,
    templates: List<MessageTemplate>,
    aiRepository: AiMarketingRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedClient by remember { mutableStateOf<Client?>(clients.firstOrNull()) }
    var selectedTemplateType by remember { mutableStateOf("ترحيب") }
    var customNotes by remember { mutableStateOf("") }
    var dialect by remember { mutableStateOf("دارجة") }

    var messageBody by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "اختيار العميل المستهدف",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (clients.isEmpty()) {
                    Text(
                        text = "لا يوجد عملاء مضافون بعد، يرجى إضافة عملاء من تبويب العملاء أولاً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        clients.forEach { client ->
                            val isSelected = selectedClient?.id == client.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) HubNavy else Color(0xFFF1F5F9))
                                    .clickable {
                                        selectedClient = client
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = client.name,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else HubNavy
                                    )
                                    Text(
                                        text = client.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color(0xFFCBD5E1) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(12.dp))

                // Template purpose buttons
                Text(
                    text = "نوع وقالب الرسالة:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MessageTemplate.TYPES.forEach { type ->
                        val isSelected = selectedTemplateType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) HubTeal else Color(0xFFF1F5F9))
                                .clickable { selectedTemplateType = type }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = type,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dialect selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("اللهجة:", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("دارجة" to "خليجية دارجة", "فصحى" to "عربية فصحى").forEach { (d, label) ->
                            val isSel = dialect == d
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) HubNavy else Color(0xFFF1F5F9))
                                    .clickable { dialect = d }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = if (isSel) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Gemini Generate Message Button
                Button(
                    onClick = {
                        val client = selectedClient
                        if (client == null) return@Button
                        isGenerating = true
                        scope.launch {
                            try {
                                val generated = aiRepository.generatePersonalizedMessage(
                                    clientName = client.name,
                                    status = client.status,
                                    purpose = selectedTemplateType,
                                    notes = client.notes,
                                    dialect = dialect
                                )
                                messageBody = generated
                            } catch (e: Exception) {
                                messageBody = "مرحباً ${client.name}، يسعدنا تواصلك معنا."
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HubTeal),
                    enabled = !isGenerating && selectedClient != null
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري صياغة الرسالة بـ Gemini...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("صياغة رسالة مخصصة بالذكاء الاصطناعي")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Message Editing & Sending Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "نص الرسالة (يمكنك التعديل قبل الإرسال)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = messageBody,
                    onValueChange = { messageBody = it },
                    placeholder = { Text("اكتب الرسالة أو اضغط زر التوليد بالأعلى...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: WhatsApp & Gmail Draft
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Send WhatsApp wa.me
                    Button(
                        onClick = {
                            val client = selectedClient
                            if (client != null && messageBody.isNotBlank()) {
                                IntentUtils.openWhatsApp(context, client.phone, messageBody)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E7E34)),
                        enabled = selectedClient != null && messageBody.isNotBlank()
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرسال عبر واتساب", fontSize = 12.sp)
                    }

                    // Save as Gmail Draft
                    Button(
                        onClick = {
                            val client = selectedClient
                            if (client != null && messageBody.isNotBlank()) {
                                IntentUtils.openGmailDraft(
                                    context,
                                    client.email,
                                    "تواصل بخصوص استفسارك - مركز عملي",
                                    messageBody
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HubNavy),
                        enabled = selectedClient != null && messageBody.isNotBlank()
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مسودة في Gmail", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CampaignMessagingTab(
    clients: List<Client>,
    aiRepository: AiMarketingRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var targetStage by remember { mutableStateOf("مهتم") }
    var campaignTopic by remember { mutableStateOf("عرض خاص وخصم حصري هذا الأسبوع") }

    val targetClients = remember(clients, targetStage) {
        clients.filter { it.status == targetStage }
    }

    val generatedMessages = remember { mutableStateMapOf<String, String>() }
    val sentSet = remember { mutableStateMapOf<String, Boolean>() }
    var isGeneratingAll by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "إطلاق حملة رسائل موجهة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "اختر شريحة من العملاء لتجهيز رسالة مخصصة لكل عميل وإرسالها يدوياً بأمان بنقرة واحدة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Stage Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Client.STATUSES.forEach { stage ->
                        val count = clients.count { it.status == stage }
                        val isSelected = targetStage == stage
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) HubNavy else Color(0xFFF1F5F9))
                                .clickable { targetStage = stage }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$stage ($count)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = campaignTopic,
                    onValueChange = { campaignTopic = it },
                    label = { Text("موضوع أو عرض الحملة") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        isGeneratingAll = true
                        scope.launch {
                            try {
                                targetClients.forEach { client ->
                                    val msg = aiRepository.generatePersonalizedMessage(
                                        clientName = client.name,
                                        status = client.status,
                                        purpose = campaignTopic,
                                        notes = client.notes,
                                        dialect = "دارجة"
                                    )
                                    generatedMessages[client.id] = msg
                                }
                            } finally {
                                isGeneratingAll = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HubTeal),
                    enabled = !isGeneratingAll && targetClients.isNotEmpty()
                ) {
                    if (isGeneratingAll) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري تجهيز رسائل الشريحة بـ Gemini...")
                    } else {
                        Icon(Icons.Default.Campaign, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تجهيز رسائل مخصصة لـ ${targetClients.size} عميل")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Target Clients List with 1-click send
        if (targetClients.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا يوجد عملاء حالياً في مرحلة '$targetStage'",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(targetClients, key = { it.id }) { client ->
                    val clientMsg = generatedMessages[client.id]
                        ?: "أهلاً بك أخي ${client.name}، نود إطلاعك على $campaignTopic."
                    val isSent = sentSet[client.id] == true

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = client.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = HubNavy
                                )
                                if (isSent) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = HubTeal,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تم الإرسال", fontSize = 11.sp, color = HubTeal, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = clientMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // One-by-one send button
                            Button(
                                onClick = {
                                    sentSet[client.id] = true
                                    IntentUtils.openWhatsApp(context, client.phone, clientMsg)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSent) Color(0xFF64748B) else Color(0xFF1E7E34)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isSent) "إعادة الإرسال عبر واتساب" else "إرسال هذا العميل الآن", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TemplatesManagementTab(
    templates: List<MessageTemplate>,
    templatesRepository: TemplatesRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (templates.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = HubNavy)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(templates, key = { it.id }) { template ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
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
                            text = template.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HubNavy
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(HubTeal.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = template.type,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = HubTeal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = template.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                IntentUtils.copyToClipboard(context, template.content, template.title)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ القالب", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
