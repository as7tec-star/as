package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HubNavy

enum class AppNavTab(val title: String, val icon: ImageVector) {
    CLIENTS("العملاء", Icons.Default.People),
    FINANCE("المالية", Icons.Default.AccountBalanceWallet),
    HR_PAYROLL("الموظفون", Icons.Default.Badge),
    MARKETING("التسويق", Icons.Default.Campaign),
    ANALYTICS("الإحصائيات", Icons.Default.BarChart)
}

@Composable
fun BottomNav(
    selectedTab: AppNavTab,
    onTabSelected: (AppNavTab) -> Unit,
    followUpCount: Int = 0,
    financeAlertCount: Int = 0
) {
    Surface(
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppNavTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val tint = if (isSelected) HubNavy else Color(0xFF94A3B8)
                val bgColor = if (isSelected) HubNavy.copy(alpha = 0.08f) else Color.Transparent

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val badgeCount = when (tab) {
                            AppNavTab.MARKETING -> followUpCount
                            AppNavTab.FINANCE -> financeAlertCount
                            else -> 0
                        }
                        if (badgeCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = Color(0xFFE53E3E),
                                        contentColor = Color.White
                                    ) {
                                        Text("$badgeCount", fontSize = 10.sp)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = tint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = tint,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = tint
                        )
                    }
                }
            }
        }
    }
}
