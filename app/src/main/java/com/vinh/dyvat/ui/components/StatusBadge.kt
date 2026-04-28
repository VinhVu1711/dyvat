package com.vinh.dyvat.ui.components

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
import androidx.compose.ui.unit.dp
import com.vinh.dyvat.ui.theme.AnnouncementBlue
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.NegativeRed
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextWhite
import com.vinh.dyvat.ui.theme.WarningOrange

enum class StatusType {
    ACTIVE,
    CANCELLED,
    IN_STOCK,
    OUT_OF_STOCK,
    HAS_EXPIRED,
    INFO,
    WARNING
}

@Composable
fun StatusBadge(
    label: String,
    type: StatusType,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (type) {
        StatusType.ACTIVE -> SpotifyGreen
        StatusType.CANCELLED -> NegativeRed
        StatusType.IN_STOCK -> SpotifyGreen.copy(alpha = 0.2f)
        StatusType.OUT_OF_STOCK -> WarningOrange.copy(alpha = 0.2f)
        StatusType.HAS_EXPIRED -> NegativeRed.copy(alpha = 0.2f)
        StatusType.INFO -> AnnouncementBlue
        StatusType.WARNING -> WarningOrange
    }

    val textColor = when (type) {
        StatusType.ACTIVE -> NearBlack
        StatusType.CANCELLED -> TextWhite
        StatusType.IN_STOCK -> SpotifyGreen
        StatusType.OUT_OF_STOCK -> WarningOrange
        StatusType.HAS_EXPIRED -> NegativeRed
        StatusType.INFO -> NearBlack
        StatusType.WARNING -> NearBlack
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
