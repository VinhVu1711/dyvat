package com.vinh.dyvat.ui.screens.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import com.vinh.dyvat.ui.components.DyvatTopBar
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.TextSilver

@Composable
fun InventoryScreen() {
    Scaffold(
        containerColor = NearBlack,
        topBar = {
            DyvatTopBar(title = "Kho")
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Module Kho\n(Đang phát triển - Phase 5)",
                color = TextSilver,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
