package com.vinh.dyvat.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite

data class SettingsItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit,
    onNavigateToUnits: () -> Unit,
    onNavigateToSuppliers: () -> Unit
) {
    val settingsItems = listOf(
        SettingsItem(
            title = "Loại sản phẩm",
            subtitle = "Quản lý các loại sản phẩm",
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight
        ),
        SettingsItem(
            title = "Đơn vị tính",
            subtitle = "Quản lý đơn vị tính",
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight
        ),
        SettingsItem(
            title = "Nhà cung cấp",
            subtitle = "Quản lý nhà cung cấp",
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight
        )
    )

    Scaffold(
        containerColor = NearBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cài đặt",
                        color = TextWhite
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Quản lý danh mục",
                color = TextSilver,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SettingsItemCard(
                title = settingsItems[0].title,
                subtitle = settingsItems[0].subtitle,
                onClick = onNavigateToCategories
            )
            SettingsItemCard(
                title = settingsItems[1].title,
                subtitle = settingsItems[1].subtitle,
                onClick = onNavigateToUnits
            )
            SettingsItemCard(
                title = settingsItems[2].title,
                subtitle = settingsItems[2].subtitle,
                onClick = onNavigateToSuppliers
            )
        }
    }
}

@Composable
private fun SettingsItemCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = TextWhite,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = TextSilver,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSilver
            )
        }
    }
}
