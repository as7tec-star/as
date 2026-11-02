package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ColorFacebook
import com.example.ui.theme.ColorHaraj
import com.example.ui.theme.ColorMourjan
import com.example.ui.theme.ColorTikTok
import com.example.ui.theme.ColorWhatsApp
import com.example.ui.theme.StatusContacted
import com.example.ui.theme.StatusContactedBg
import com.example.ui.theme.StatusInterested
import com.example.ui.theme.StatusInterestedBg
import com.example.ui.theme.StatusLost
import com.example.ui.theme.StatusLostBg
import com.example.ui.theme.StatusNegotiating
import com.example.ui.theme.StatusNegotiatingBg
import com.example.ui.theme.StatusNew
import com.example.ui.theme.StatusNewBg
import com.example.ui.theme.StatusWon
import com.example.ui.theme.StatusWonBg

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "جديد" -> StatusNewBg to StatusNew
        "تم التواصل" -> StatusContactedBg to StatusContacted
        "مهتم" -> StatusInterestedBg to StatusInterested
        "تفاوض" -> StatusNegotiatingBg to StatusNegotiating
        "تم البيع" -> StatusWonBg to StatusWon
        "خسرناه" -> StatusLostBg to StatusLost
        else -> Color(0xFFF1F5F9) to Color(0xFF475569)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = textColor
        )
    }
}

@Composable
fun SourceBadge(source: String) {
    val (bgColor, textColor) = when (source) {
        "حراج" -> ColorHaraj.copy(alpha = 0.12f) to ColorHaraj
        "مرجان" -> ColorMourjan.copy(alpha = 0.12f) to ColorMourjan
        "تيك توك" -> Color(0xFF1E293B).copy(alpha = 0.12f) to Color(0xFF0F172A)
        "فيسبوك" -> ColorFacebook.copy(alpha = 0.12f) to ColorFacebook
        "واتساب" -> ColorWhatsApp.copy(alpha = 0.12f) to Color(0xFF1E7E34)
        else -> Color(0xFFE2E8F0) to Color(0xFF475569)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = source,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
            color = textColor
        )
    }
}

@Composable
fun AdStatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "لم ينشر" -> Color(0xFFF1F5F9) to Color(0xFF64748B)
        "نُشر" -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        "يحتاج تجديد" -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        else -> Color(0xFFF1F5F9) to Color(0xFF64748B)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = textColor
        )
    }
}
