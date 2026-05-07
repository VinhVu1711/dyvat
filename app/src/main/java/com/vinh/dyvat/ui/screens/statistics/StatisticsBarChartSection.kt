package com.vinh.dyvat.ui.screens.statistics

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinh.dyvat.ui.components.toVnd
import com.vinh.dyvat.ui.theme.BorderGray
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.NegativeRed
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------
// Bar chart
// ---------------------------------------------------------------------------

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text = label, color = TextSilver, fontSize = 11.sp)
    }
}

@Composable
internal fun BarChartSection(
    bars: List<BarGroup>,
    bar1Color: Color,
    bar2Color: Color,
    bar1Label: String,
    bar2Label: String,
    negativeBar2Color: Color = NegativeRed,
    chartFileName: String
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedIdx by remember(bars) { mutableStateOf<Int?>(null) }

    val chartHeightDp = 150.dp
    val barWidthDp = 11.dp
    val barGapDp = 3.dp
    val groupGapDp = 8.dp
    val maxVal = bars.maxOfOrNull { maxOf(it.bar1, it.bar2.coerceAtLeast(0L)) }?.coerceAtLeast(1L) ?: 1L

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .padding(bottom = 12.dp)
        ) {
            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendDot(color = bar1Color, label = bar1Label)
                LegendDot(color = bar2Color, label = bar2Label)
            }

            // Tooltip for selected bar
            selectedIdx?.let { idx ->
                val g = bars[idx]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MidDark)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = g.label, color = TextSilver, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text(text = "$bar1Label: ${g.bar1.toVnd()}", color = bar1Color, fontSize = 11.sp)
                    Text(
                        text = "$bar2Label: ${g.bar2.toVnd()}",
                        color = if (g.bar2 < 0) negativeBar2Color else bar2Color,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Scrollable bars
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .height(chartHeightDp + 22.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(groupGapDp)
            ) {
                bars.forEachIndexed { idx, g ->
                    val selected = selectedIdx == idx
                    Column(
                        modifier = Modifier
                            .width(barWidthDp * 2 + barGapDp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { selectedIdx = if (selected) null else idx },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.height(chartHeightDp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(barGapDp)
                        ) {
                            // Bar 1
                            val h1 = if (g.bar1 > 0)
                                (chartHeightDp.value * g.bar1.toFloat() / maxVal).coerceAtLeast(2f).dp
                            else 0.dp
                            Box(
                                modifier = Modifier
                                    .width(barWidthDp)
                                    .height(h1)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(bar1Color.copy(alpha = if (selected) 1f else 0.75f))
                            )
                            // Bar 2
                            val b2pos = g.bar2.coerceAtLeast(0L)
                            val h2 = if (b2pos > 0)
                                (chartHeightDp.value * b2pos.toFloat() / maxVal).coerceAtLeast(2f).dp
                            else 0.dp
                            val b2color = if (g.bar2 < 0) negativeBar2Color else bar2Color
                            Box(
                                modifier = Modifier
                                    .width(barWidthDp)
                                    .height(h2)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(b2color.copy(alpha = if (selected) 1f else 0.75f))
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = g.label,
                            color = if (selected) TextWhite else TextSilver,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(barWidthDp * 2 + barGapDp)
                        )
                    }
                }
            }
        }

        // Save button
        Spacer(Modifier.height(6.dp))
        val b1Int = bar1Color.toArgb()
        val b2Int = bar2Color.toArgb()
        val negInt = negativeBar2Color.toArgb()
        OutlinedButton(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val bitmap = renderChartToBitmap(bars, b1Int, b2Int, negInt)
                    saveToGallery(context, bitmap, chartFileName)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Đã lưu ảnh biểu đồ vào Thư viện", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSilver),
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("Lưu ảnh biểu đồ", fontSize = 13.sp)
        }
    }
}
