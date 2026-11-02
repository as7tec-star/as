package com.example.ui.screens.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdPost
import com.example.data.repository.AiMarketingRepository
import com.example.ui.theme.HubAmber
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal
import com.example.util.IntentUtils
import kotlinx.coroutines.launch

@Composable
fun CreateAdDialog(
    adToEdit: AdPost? = null,
    aiRepository: AiMarketingRepository,
    onDismiss: () -> Unit,
    onSaveAd: (AdPost) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(adToEdit?.title ?: "") }
    var price by remember { mutableStateOf(adToEdit?.price ?: "") }
    var description by remember { mutableStateOf(adToEdit?.description ?: "") }
    var city by remember { mutableStateOf(adToEdit?.city ?: "الرياض") }
    var renewalDaysText by remember { mutableStateOf(adToEdit?.renewalDays?.toString() ?: "7") }
    var postUrl by remember { mutableStateOf(adToEdit?.postUrl ?: "") }
    var scheduledDate by remember { mutableStateOf(adToEdit?.scheduledDate ?: "") }

    // Dialect preference: "دارجة" or "فصحى"
    var selectedDialect by remember { mutableStateOf("دارجة") }

    // Generated platform copy map
    var generatedVersions by remember { mutableStateOf(adToEdit?.generatedVersions ?: emptyMap()) }
    var selectedPlatformTab by remember { mutableIntStateOf(0) }

    var isGenerating by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val platformKeys = listOf("حراج", "مرجان", "تيك توك", "فيسبوك", "واتساب")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (adToEdit == null) "استوديو الإعلانات الذكي" else "تعديل الإعلان",
                    style = MaterialTheme.typography.titleLarge,
                    color = HubNavy,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = HubAmber,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Text(
                    text = "١) بيانات السلعة أو الخدمة",
                    style = MaterialTheme.typography.titleSmall,
                    color = HubNavy,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم المنتج أو الخدمة *") },
                    placeholder = { Text("مثال: آيفون 15 برو ماكس جديد") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("السعر (ريال)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("المدينة") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف المنتج ومميزاته *") },
                    placeholder = { Text("اكتب نبذة ومواصفات المنتج ليتولى Gemini صياغتها...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Dialect Choice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "أسلوب الكتابة بالذكاء الاصطناعي:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("دارجة" to "خليجية دارجة", "فصحى" to "فصحى تسويقية").forEach { (key, label) ->
                            val isSelected = selectedDialect == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) HubNavy else Color(0xFFF1F5F9))
                                    .clickable { selectedDialect = key }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = label,
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

                Spacer(modifier = Modifier.height(12.dp))

                // AI Generate Button
                Button(
                    onClick = {
                        if (title.isBlank() || description.isBlank()) {
                            errorMsg = "يرجى تعبئة اسم المنتج والوصف لتوليد الإعلانات"
                            return@Button
                        }
                        errorMsg = null
                        isGenerating = true
                        scope.launch {
                            try {
                                val generated = aiRepository.generateMultiPlatformAds(
                                    title = title,
                                    price = price,
                                    description = description,
                                    city = city,
                                    dialect = selectedDialect
                                )
                                generatedVersions = generated
                            } catch (e: Exception) {
                                errorMsg = e.message ?: "فشل توليد الإعلانات"
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_ads_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HubTeal),
                    enabled = !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري صياغة الإعلانات بـ Gemini...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("توليد نسخ مخصصة لجميع المنصات")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Generated Versions Display & Switcher
                if (generatedVersions.isNotEmpty()) {
                    Text(
                        text = "٢) النسخ المولدة بحسب كل منصة",
                        style = MaterialTheme.typography.titleSmall,
                        color = HubNavy,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    TabRow(
                        selectedTabIndex = selectedPlatformTab,
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = HubNavy
                    ) {
                        platformKeys.forEachIndexed { index, platformName ->
                            Tab(
                                selected = selectedPlatformTab == index,
                                onClick = { selectedPlatformTab = index },
                                text = {
                                    Text(
                                        text = platformName,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedPlatformTab == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val currentPlatform = platformKeys[selectedPlatformTab]
                    val currentText = generatedVersions[currentPlatform] ?: ""

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Copy and Open Platform Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "صيغة $currentPlatform الجاهزة:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HubNavy
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            IntentUtils.copyToClipboard(context, currentText, currentPlatform)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("نسخ", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            IntentUtils.openPlatformUrl(context, currentPlatform, postUrl)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = HubNavy),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInBrowser, contentDescription = "فتح المنصة", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("فتح المنصة", fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = currentText,
                                onValueChange = { newText ->
                                    val updatedMap = generatedVersions.toMutableMap()
                                    updatedMap[currentPlatform] = newText
                                    generatedVersions = updatedMap
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 5
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Settings: Renewal reminder & URL
                Text(
                    text = "٣) إعدادات النشر والمتابعة",
                    style = MaterialTheme.typography.titleSmall,
                    color = HubNavy,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = renewalDaysText,
                        onValueChange = { renewalDaysText = it },
                        label = { Text("أيام التجديد") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = scheduledDate,
                        onValueChange = { scheduledDate = it },
                        label = { Text("تاريخ النشر المجدول") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = postUrl,
                    onValueChange = { postUrl = it },
                    label = { Text("رابط الإعلان بعد النشر (اختياري)") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || description.isBlank()) {
                        errorMsg = "يرجى تعبئة اسم المنتج والوصف"
                        return@Button
                    }
                    val days = renewalDaysText.toLongOrNull() ?: 7L
                    val updated = (adToEdit ?: AdPost()).copy(
                        title = title.trim(),
                        price = price.trim(),
                        description = description.trim(),
                        city = city.trim(),
                        generatedVersions = generatedVersions,
                        renewalDays = days,
                        postUrl = postUrl.trim(),
                        scheduledDate = scheduledDate.trim()
                    )
                    onSaveAd(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HubNavy),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("حفظ الإعلان")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("إلغاء")
            }
        }
    )
}
