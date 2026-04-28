package com.vinh.dyvat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextWhite

enum class DyvatCardVariant {
    DEFAULT,
    NAVIGATION,
    ELEVATED,
    OUTLINED
}

@Composable
fun DyvatCard(
    modifier: Modifier = Modifier,
    variant: DyvatCardVariant = DyvatCardVariant.DEFAULT,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val backgroundColor = when (variant) {
        DyvatCardVariant.DEFAULT -> DarkSurface
        DyvatCardVariant.NAVIGATION -> DarkSurface
        DyvatCardVariant.ELEVATED -> DarkCard
        DyvatCardVariant.OUTLINED -> Color.Transparent
    }

    val shape = when (variant) {
        DyvatCardVariant.NAVIGATION -> RoundedCornerShape(12.dp)
        else -> RoundedCornerShape(8.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun NavigationCard(
    title: String,
    subtitle: String? = null,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DyvatCard(
        modifier = modifier,
        variant = DyvatCardVariant.NAVIGATION,
        onClick = onClick
    ) {
        Column {
            icon()
            Text(
                text = title,
                color = TextWhite,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = SpotifyGreen.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun DataCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    DyvatCard(
        modifier = modifier,
        variant = DyvatCardVariant.DEFAULT,
        content = content
    )
}
