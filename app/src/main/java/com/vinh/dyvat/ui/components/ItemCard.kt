package com.vinh.dyvat.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.NegativeRed
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite

@Composable
fun ItemCard(
    name: String,
    subtitle: String? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    color = TextWhite,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = TextSilver,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Sửa",
                    tint = TextSilver
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xóa",
                    tint = NegativeRed
                )
            }
        }
    }
}
