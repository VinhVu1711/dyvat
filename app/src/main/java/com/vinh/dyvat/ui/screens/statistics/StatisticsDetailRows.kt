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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinh.dyvat.data.model.DailySummary
import com.vinh.dyvat.ui.components.toVnd
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.NegativeRed
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite
import com.vinh.dyvat.ui.theme.WarningOrange

// ---------------------------------------------------------------------------
// Detail rows
// ---------------------------------------------------------------------------

@Composable
internal fun DailyRow(summary: DailySummary) {
    val parts = summary.businessDate.split("-")
    val displayDate = if (parts.size == 3) "${parts[2]}/${parts[1]}" else summary.businessDate

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MidDark),
            contentAlignment = Alignment.Center
        ) {
            Text(text = displayDate, color = TextSilver, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Bán", color = TextSilver, fontSize = 12.sp)
                Text(text = summary.totalSaleVnd.toVnd(), color = SpotifyGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(3.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Nhập", color = TextSilver, fontSize = 12.sp)
                Text(text = summary.totalPurchaseVnd.toVnd(), color = WarningOrange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            if (summary.profitVnd != 0L) {
                Spacer(Modifier.height(3.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Lãi", color = TextSilver, fontSize = 12.sp)
                    Text(
                        text = summary.profitVnd.toVnd(),
                        color = if (summary.profitVnd >= 0) SpotifyGreen else NegativeRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (summary.saleTicketCount > 0) Text(text = "${summary.saleTicketCount} bán", color = TextSilver, fontSize = 11.sp)
            if (summary.purchaseTicketCount > 0) Text(text = "${summary.purchaseTicketCount} nhập", color = TextSilver, fontSize = 11.sp)
        }
    }
}

@Composable
internal fun MonthlyGroupRow(monthKey: String, days: List<DailySummary>) {
    val parts = monthKey.split("-")
    val label = if (parts.size == 2) "Tháng ${parts[1].trimStart('0')}/${parts[0]}" else monthKey
    val totalSale = days.sumOf { it.totalSaleVnd }
    val totalPurchase = days.sumOf { it.totalPurchaseVnd }
    val totalProfit = days.sumOf { it.profitVnd }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MidDark),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (parts.size == 2) "Th.${parts[1].trimStart('0')}" else "?",
                color = TextSilver,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Bán: ${totalSale.toVnd()}", color = SpotifyGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Lãi: ${totalProfit.toVnd()}",
                    color = if (totalProfit >= 0) SpotifyGreen else NegativeRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(text = "Nhập: ${totalPurchase.toVnd()}", color = WarningOrange, fontSize = 12.sp)
        }
    }
}
