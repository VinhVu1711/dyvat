package com.vinh.dyvat.ui.screens.statistics

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
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
import com.vinh.dyvat.ui.components.toVnd
import com.vinh.dyvat.ui.theme.AnnouncementBlue
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.NegativeRed
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite
import com.vinh.dyvat.ui.theme.WarningOrange

// ---------------------------------------------------------------------------
// Summary cards
// ---------------------------------------------------------------------------

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextSilver,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
internal fun SummarySection(state: StatisticsUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(
                label = "Tiền nhập",
                value = state.totalPurchaseVnd.toVnd(),
                icon = Icons.Default.ShoppingCart,
                iconTint = WarningOrange,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label = "Tiền bán",
                value = state.totalSaleVnd.toVnd(),
                icon = Icons.Default.Store,
                iconTint = SpotifyGreen,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(
                label = "Giá vốn",
                value = state.totalCostVnd.toVnd(),
                icon = Icons.Default.TrendingDown,
                iconTint = NegativeRed,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label = "Lợi nhuận",
                value = state.profitVnd.toVnd(),
                icon = Icons.Default.TrendingUp,
                iconTint = if (state.profitVnd >= 0) SpotifyGreen else NegativeRed,
                valueColor = if (state.profitVnd >= 0) SpotifyGreen else NegativeRed,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(
                label = "Phiếu nhập",
                value = "${state.purchaseTicketCount} phiếu",
                icon = Icons.Default.Inventory,
                iconTint = AnnouncementBlue,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label = "Phiếu bán",
                value = "${state.saleTicketCount} phiếu",
                icon = Icons.Default.Receipt,
                iconTint = AnnouncementBlue,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    valueColor: Color = TextWhite
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(text = label, color = TextSilver, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(text = value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
