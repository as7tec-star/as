package com.example.ui.screens.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HubAmber
import com.example.ui.theme.HubNavy
import com.example.ui.theme.HubTeal
import com.example.util.FinancialCalculator
import com.example.util.MonthlyVolumePoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

enum class DateRangePreset(val label: String) {
    ALL("الكل"),
    LAST_3_MONTHS("آخر 3 أشهر"),
    LAST_6_MONTHS("آخر 6 أشهر"),
    THIS_YEAR("هذه السنة"),
    CUSTOM("تاريخ مخصص")
}

enum class MonthlyChartMode(val label: String) {
    BAR("أعمدة"),
    AREA_LINE("منحنى مساحي")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterSection(
    selectedPreset: DateRangePreset,
    startDate: String?,
    endDate: String?,
    onPresetSelected: (DateRangePreset) -> Unit,
    onCustomDatesChanged: (String?, String?) -> Unit,
    onResetFilter: () -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = HubNavy,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "فلترة البيانات حسب التاريخ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = HubNavy
                    )
                }

                if (selectedPreset != DateRangePreset.ALL || startDate != null || endDate != null) {
                    TextButton(
                        onClick = onResetFilter,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reset_date_filter_button")
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إعادة ضبط", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DateRangePreset.entries.forEach { preset ->
                    val isSelected = selectedPreset == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) HubNavy else Color(0xFFF1F5F9))
                            .clickable { onPresetSelected(preset) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("date_preset_${preset.name.lowercase(Locale.ENGLISH)}")
                    ) {
                        Text(
                            text = preset.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateFieldBox(
                    label = "من تاريخ",
                    value = startDate ?: "البداية",
                    onClick = { showStartPicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filter_start_date_button")
                )
                DateFieldBox(
                    label = "إلى تاريخ",
                    value = endDate ?: "اليوم",
                    onClick = { showEndPicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filter_end_date_button")
                )
            }
        }
    }

    if (showStartPicker) {
        val initialMillis = remember(startDate) {
            try {
                if (startDate != null) dateFormat.parse(startDate)?.time else null
            } catch (_: Exception) {
                null
            }
        }
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = state.selectedDateMillis?.let { dateFormat.format(Date(it)) }
                        onCustomDatesChanged(picked, endDate)
                        showStartPicker = false
                    }
                ) {
                    Text("تأكيد", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("إلغاء") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showEndPicker) {
        val initialMillis = remember(endDate) {
            try {
                if (endDate != null) dateFormat.parse(endDate)?.time else null
            } catch (_: Exception) {
                null
            }
        }
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = state.selectedDateMillis?.let { dateFormat.format(Date(it)) }
                        onCustomDatesChanged(startDate, picked)
                        showEndPicker = false
                    }
                ) {
                    Text("تأكيد", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("إلغاء") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun DateFieldBox(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        modifier = modifier
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = label, fontSize = 10.sp, color = Color(0xFF64748B))
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
            }
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = label,
                tint = HubTeal,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun InteractiveMonthlyVolumeChartCard(
    monthlyData: List<MonthlyVolumePoint>,
    onResetFilter: () -> Unit
) {
    var chartMode by remember { mutableStateOf(MonthlyChartMode.BAR) }
    var useNetAmount by remember { mutableStateOf(false) }
    var showSalesSeries by remember { mutableStateOf(true) }
    var showPurchasesSeries by remember { mutableStateOf(true) }
    var showProfitSeries by remember { mutableStateOf(true) }

    var selectedIndex by remember {
        mutableIntStateOf(if (monthlyData.isNotEmpty()) monthlyData.lastIndex else -1)
    }

    LaunchedEffect(monthlyData) {
        selectedIndex = if (monthlyData.isNotEmpty()) monthlyData.lastIndex else -1
    }

    val animProgress by animateFloatAsState(
        targetValue = if (monthlyData.isEmpty()) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "chartReveal"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header + Chart Type Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "حجم المبيعات والمشتريات الشهرية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HubNavy
                    )
                    Text(
                        text = "المس أو اسحب على الرسم لاستعراض تفاصيل كل شهر",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }

                // Chart Mode Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        ChartToggleIconPill(
                            icon = Icons.Default.BarChart,
                            label = MonthlyChartMode.BAR.label,
                            selected = chartMode == MonthlyChartMode.BAR,
                            onClick = { chartMode = MonthlyChartMode.BAR },
                            tag = "chart_mode_bar"
                        )
                        ChartToggleIconPill(
                            icon = Icons.Default.ShowChart,
                            label = MonthlyChartMode.AREA_LINE.label,
                            selected = chartMode == MonthlyChartMode.AREA_LINE,
                            onClick = { chartMode = MonthlyChartMode.AREA_LINE },
                            tag = "chart_mode_line"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Value Mode Toggle (Gross with VAT vs Net before VAT)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniChoiceChip(
                    label = "شامل الضريبة",
                    selected = !useNetAmount,
                    onClick = { useNetAmount = false }
                )
                MiniChoiceChip(
                    label = "الصافي قبل الضريبة",
                    selected = useNetAmount,
                    onClick = { useNetAmount = true }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Series Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LegendToggleChip(
                    label = "المبيعات",
                    color = HubTeal,
                    enabled = showSalesSeries,
                    onClick = { showSalesSeries = !showSalesSeries }
                )
                LegendToggleChip(
                    label = "المشتريات",
                    color = HubAmber,
                    enabled = showPurchasesSeries,
                    onClick = { showPurchasesSeries = !showPurchasesSeries }
                )
                if (chartMode == MonthlyChartMode.AREA_LINE) {
                    LegendToggleChip(
                        label = "صافي التشغيل",
                        color = HubNavy,
                        enabled = showProfitSeries,
                        onClick = { showProfitSeries = !showProfitSeries }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (monthlyData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "لا توجد فواتير مبيعات أو مشتريات في هذه الفترة",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = HubNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = onResetFilter) {
                            Text("عرض جميع الفترات", color = HubTeal, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Selected Month Interactive Tooltip Card
                val activePoint = monthlyData.getOrNull(selectedIndex)
                AnimatedVisibility(visible = activePoint != null) {
                    if (activePoint != null) {
                        val salesVal = if (useNetAmount) activePoint.salesNet else activePoint.salesTotal
                        val purchVal = if (useNetAmount) activePoint.purchasesNet else activePoint.purchasesTotal
                        val prevPoint = monthlyData.getOrNull(selectedIndex - 1)
                        val prevSales = prevPoint?.let { if (useNetAmount) it.salesNet else it.salesTotal }
                        val growthPct = if (prevSales != null && prevSales > 0.0) {
                            (((salesVal - prevSales) / prevSales) * 100.0).roundToInt()
                        } else null

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${activePoint.fullLabel} (${activePoint.transactionCount} فاتورة)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = HubNavy
                                    )
                                    if (growthPct != null) {
                                        val isPositive = growthPct >= 0
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isPositive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "${if (isPositive) "+" else ""}$growthPct% عن الشهر السابق",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPositive) Color(0xFF15803D) else Color(0xFFB91C1C)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TooltipMetric(
                                        title = "المبيعات",
                                        value = "${FinancialCalculator.formatMoney(salesVal)} ر.س",
                                        color = HubTeal
                                    )
                                    TooltipMetric(
                                        title = "المشتريات",
                                        value = "${FinancialCalculator.formatMoney(purchVal)} ر.س",
                                        color = HubAmber
                                    )
                                    TooltipMetric(
                                        title = "الربح التشغيلي",
                                        value = "${FinancialCalculator.formatMoney(activePoint.operatingProfit)} ر.س",
                                        color = if (activePoint.operatingProfit >= 0) Color(0xFF15803D) else Color(0xFFB91C1C)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Canvas Chart
                MonthlyChartCanvas(
                    data = monthlyData,
                    chartMode = chartMode,
                    useNetAmount = useNetAmount,
                    showSales = showSalesSeries,
                    showPurchases = showPurchasesSeries,
                    showProfit = showProfitSeries,
                    selectedIndex = selectedIndex,
                    animProgress = animProgress,
                    onSelectIndex = { idx ->
                        if (idx in monthlyData.indices) selectedIndex = idx
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(8.dp))

                // Compact Monthly Breakdown List
                Text(
                    text = "جدول التفصيل الشهري",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = HubNavy
                )
                Spacer(modifier = Modifier.height(6.dp))

                monthlyData.asReversed().forEach { point ->
                    val s = if (useNetAmount) point.salesNet else point.salesTotal
                    val p = if (useNetAmount) point.purchasesNet else point.purchasesTotal
                    val idxInOrig = monthlyData.indexOf(point)
                    val isRowSelected = idxInOrig == selectedIndex

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isRowSelected) HubNavy.copy(alpha = 0.06f) else Color.Transparent)
                            .clickable { if (idxInOrig >= 0) selectedIndex = idxInOrig }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = point.fullLabel,
                            fontSize = 12.sp,
                            fontWeight = if (isRowSelected) FontWeight.Bold else FontWeight.Medium,
                            color = HubNavy,
                            modifier = Modifier.weight(1.1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "+${FinancialCalculator.formatMoney(s)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HubTeal,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "-${FinancialCalculator.formatMoney(p)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HubAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyChartCanvas(
    data: List<MonthlyVolumePoint>,
    chartMode: MonthlyChartMode,
    useNetAmount: Boolean,
    showSales: Boolean,
    showPurchases: Boolean,
    showProfit: Boolean,
    selectedIndex: Int,
    animProgress: Float,
    onSelectIndex: (Int) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val axisLabelStyle = TextStyle(fontSize = 10.sp, color = Color(0xFF64748B))
    val selectedAxisStyle = TextStyle(fontSize = 10.sp, color = HubNavy, fontWeight = FontWeight.Bold)

    // Compute scale bounds cleanly
    val maxVal = remember(data, useNetAmount, showSales, showPurchases, showProfit, chartMode) {
        var peak = 100.0
        for (pt in data) {
            val s = if (useNetAmount) pt.salesNet else pt.salesTotal
            val p = if (useNetAmount) pt.purchasesNet else pt.purchasesTotal
            if (showSales) peak = max(peak, s)
            if (showPurchases) peak = max(peak, p)
            if (chartMode == MonthlyChartMode.AREA_LINE && showProfit) {
                peak = max(peak, kotlin.math.abs(pt.operatingProfit))
            }
        }
        peak * 1.15
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        val minSlotWidth = 62.dp
        val neededWidth = minSlotWidth * data.size
        val canvasWidth = if (neededWidth > availableWidth) neededWidth else availableWidth
        val scrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(canvasWidth)
                    .height(235.dp)
                    .testTag("monthly_analytics_chart")
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            val leftPad = 50.dp.toPx()
                            val rightPad = 14.dp.toPx()
                            val plotW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                            val relX = (offset.x - leftPad).coerceIn(0f, plotW)
                            val slotW = plotW / data.size.coerceAtLeast(1)
                            val idx = (relX / slotW).toInt().coerceIn(0, data.lastIndex)
                            onSelectIndex(idx)
                        }
                    }
                    .pointerInput(data) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val leftPad = 50.dp.toPx()
                            val rightPad = 14.dp.toPx()
                            val plotW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                            val relX = (change.position.x - leftPad).coerceIn(0f, plotW)
                            val slotW = plotW / data.size.coerceAtLeast(1)
                            val idx = (relX / slotW).toInt().coerceIn(0, data.lastIndex)
                            onSelectIndex(idx)
                        }
                    }
            ) {
                val leftPad = 50.dp.toPx()
                val rightPad = 14.dp.toPx()
                val topPad = 14.dp.toPx()
                val bottomPad = 34.dp.toPx()

                val plotW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                val plotH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                val baseY = topPad + plotH

                // 1. Horizontal Gridlines + Y-axis labels
                val gridCount = 4
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                for (i in 0..gridCount) {
                    val frac = i.toFloat() / gridCount
                    val y = baseY - (plotH * frac)
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(leftPad, y),
                        end = Offset(leftPad + plotW, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (i == 0) null else dashEffect
                    )

                    val labelVal = maxVal * frac
                    val formatted = formatCompactNumber(labelVal)
                    val measured = textMeasurer.measure(formatted, axisLabelStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = (leftPad - measured.size.width - 6.dp.toPx()).coerceAtLeast(0f),
                            y = y - measured.size.height / 2f
                        )
                    )
                }

                val slotW = plotW / data.size.coerceAtLeast(1)

                // 2. Highlight Selected Slot Background / Crosshair
                if (selectedIndex in data.indices) {
                    val centerX = leftPad + slotW * selectedIndex + slotW / 2f
                    drawRect(
                        color = HubNavy.copy(alpha = 0.05f),
                        topLeft = Offset(leftPad + slotW * selectedIndex, topPad),
                        size = Size(slotW, plotH)
                    )
                    drawLine(
                        color = HubNavy.copy(alpha = 0.35f),
                        start = Offset(centerX, topPad),
                        end = Offset(centerX, baseY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                // 3. Draw Bars or Area/Lines
                if (chartMode == MonthlyChartMode.BAR) {
                    val activeSeriesCount = (if (showSales) 1 else 0) + (if (showPurchases) 1 else 0)
                    val barW = ((slotW * 0.62f) / max(1, activeSeriesCount)).coerceAtMost(24.dp.toPx())
                    val gap = 4.dp.toPx()

                    data.forEachIndexed { idx, point ->
                        val slotCenter = leftPad + slotW * idx + slotW / 2f
                        val sVal = if (useNetAmount) point.salesNet else point.salesTotal
                        val pVal = if (useNetAmount) point.purchasesNet else point.purchasesTotal

                        val totalBarsWidth = activeSeriesCount * barW + (activeSeriesCount - 1).coerceAtLeast(0) * gap
                        var curX = slotCenter - totalBarsWidth / 2f

                        if (showSales) {
                            val h = ((sVal / maxVal).toFloat() * plotH * animProgress).coerceAtLeast(if (sVal > 0) 4f else 0f)
                            drawRoundRect(
                                color = if (idx == selectedIndex) HubTeal else HubTeal.copy(alpha = 0.85f),
                                topLeft = Offset(curX, baseY - h),
                                size = Size(barW, h),
                                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                            )
                            curX += barW + gap
                        }
                        if (showPurchases) {
                            val h = ((pVal / maxVal).toFloat() * plotH * animProgress).coerceAtLeast(if (pVal > 0) 4f else 0f)
                            drawRoundRect(
                                color = if (idx == selectedIndex) HubAmber else HubAmber.copy(alpha = 0.85f),
                                topLeft = Offset(curX, baseY - h),
                                size = Size(barW, h),
                                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                            )
                        }
                    }
                } else {
                    // Area / Line Chart Mode
                    fun yFor(value: Double): Float {
                        val ratio = (value.coerceAtLeast(0.0) / maxVal).toFloat().coerceIn(0f, 1f)
                        return baseY - (plotH * ratio * animProgress)
                    }

                    if (showSales) {
                        val pts = data.mapIndexed { idx, pt ->
                            val v = if (useNetAmount) pt.salesNet else pt.salesTotal
                            Offset(leftPad + slotW * idx + slotW / 2f, yFor(v))
                        }
                        drawSmoothAreaSeries(pts, baseY, HubTeal, selectedIndex)
                    }
                    if (showPurchases) {
                        val pts = data.mapIndexed { idx, pt ->
                            val v = if (useNetAmount) pt.purchasesNet else pt.purchasesTotal
                            Offset(leftPad + slotW * idx + slotW / 2f, yFor(v))
                        }
                        drawSmoothAreaSeries(pts, baseY, HubAmber, selectedIndex)
                    }
                    if (showProfit) {
                        val pts = data.mapIndexed { idx, pt ->
                            Offset(leftPad + slotW * idx + slotW / 2f, yFor(pt.operatingProfit.coerceAtLeast(0.0)))
                        }
                        drawSmoothAreaSeries(pts, baseY, HubNavy, selectedIndex, fillAlpha = 0.08f)
                    }
                }

                // 4. X-Axis Month Labels
                data.forEachIndexed { idx, pt ->
                    val centerX = leftPad + slotW * idx + slotW / 2f
                    val style = if (idx == selectedIndex) selectedAxisStyle else axisLabelStyle
                    val measured = textMeasurer.measure(pt.shortLabel, style)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = (centerX - measured.size.width / 2f).coerceIn(leftPad, size.width - measured.size.width),
                            y = baseY + 8.dp.toPx()
                        )
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothAreaSeries(
    points: List<Offset>,
    baseY: Float,
    color: Color,
    selectedIndex: Int,
    fillAlpha: Float = 0.20f
) {
    if (points.isEmpty()) return

    if (points.size >= 2) {
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.lastIndex) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val ctrlX = (p0.x + p1.x) / 2f
                cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
            }
        }
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, baseY)
            lineTo(points.first().x, baseY)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = fillAlpha), color.copy(alpha = 0.0f)),
                startY = 0f,
                endY = baseY
            )
        )
        drawPath(
            path = linePath,
            color = color,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    points.forEachIndexed { idx, pt ->
        val isSelected = idx == selectedIndex
        if (isSelected) {
            drawCircle(
                color = color.copy(alpha = 0.25f),
                radius = 8.dp.toPx(),
                center = pt
            )
        }
        drawCircle(
            color = Color.White,
            radius = if (isSelected) 4.5.dp.toPx() else 3.5.dp.toPx(),
            center = pt
        )
        drawCircle(
            color = color,
            radius = if (isSelected) 3.5.dp.toPx() else 2.5.dp.toPx(),
            center = pt
        )
    }
}

private fun formatCompactNumber(value: Double): String {
    return when {
        value >= 1_000_000.0 -> String.format(Locale.ENGLISH, "%.1fM", value / 1_000_000.0)
        value >= 1_000.0 -> String.format(Locale.ENGLISH, "%.1fk", value / 1_000.0).replace(".0k", "k")
        else -> value.roundToInt().toString()
    }
}

fun resolveDatePresetBounds(preset: DateRangePreset): Pair<String?, String?> {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    val cal = Calendar.getInstance()
    val today = fmt.format(cal.time)
    return when (preset) {
        DateRangePreset.ALL -> null to null
        DateRangePreset.LAST_3_MONTHS -> {
            cal.add(Calendar.MONTH, -2)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            fmt.format(cal.time) to today
        }
        DateRangePreset.LAST_6_MONTHS -> {
            cal.add(Calendar.MONTH, -5)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            fmt.format(cal.time) to today
        }
        DateRangePreset.THIS_YEAR -> {
            cal.set(Calendar.MONTH, Calendar.JANUARY)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            fmt.format(cal.time) to today
        }
        DateRangePreset.CUSTOM -> null to null
    }
}

@Composable
private fun ChartToggleIconPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) HubNavy else Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) HubNavy else Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun MiniChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) HubNavy else Color(0xFFF1F5F9))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF475569)
        )
    }
}

@Composable
private fun LegendToggleChip(
    label: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) color.copy(alpha = 0.12f) else Color(0xFFF1F5F9))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (enabled) color else Color(0xFFCBD5E1))
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) color else Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun TooltipMetric(
    title: String,
    value: String,
    color: Color
) {
    Column {
        Text(text = title, fontSize = 10.sp, color = Color(0xFF64748B))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
