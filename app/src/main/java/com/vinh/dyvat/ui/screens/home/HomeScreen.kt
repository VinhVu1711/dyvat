package com.vinh.dyvat.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vinh.dyvat.ui.components.DyvatTopBar
import com.vinh.dyvat.ui.navigation.Screen
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite

data class HomeModule(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color = SpotifyGreen,
    val route: String
)

private val homeModules = listOf(
    HomeModule(
        title = "San pham",
        subtitle = "Quan ly SP",
        icon = Icons.Default.Store,
        route = Screen.Products.route
    ),
    HomeModule(
        title = "Nhap hang",
        subtitle = "Tao phieu nhap",
        icon = Icons.Default.ShoppingCart,
        route = Screen.PurchaseList.route
    ),
    HomeModule(
        title = "Ban hang",
        subtitle = "Tao phieu ban",
        icon = Icons.Default.AttachMoney,
        route = Screen.Sales.route
    ),
    HomeModule(
        title = "Kho",
        subtitle = "Ton kho",
        icon = Icons.Default.Inventory,
        route = Screen.Inventory.route
    ),
    HomeModule(
        title = "Thong ke",
        subtitle = "Doanh thu",
        icon = Icons.Default.BarChart,
        route = Screen.Statistics.route
    ),
    HomeModule(
        title = "Loai SP",
        subtitle = "Danh muc",
        icon = Icons.Default.Category,
        route = Screen.Categories.route
    ),
    HomeModule(
        title = "Don vi",
        subtitle = "DVT",
        icon = Icons.Default.Straighten,
        route = Screen.Units.route
    ),
    HomeModule(
        title = "Nha CC",
        subtitle = "NCC",
        icon = Icons.Default.LocalShipping,
        route = Screen.Suppliers.route
    )
)

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit
) {
    Scaffold(
        containerColor = NearBlack,
        topBar = {
            DyvatTopBar(title = "Dyvat")
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .background(NearBlack)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(homeModules) { module ->
                HomeCard(
                    module = module,
                    onClick = { onNavigate(module.route) }
                )
            }
        }
    }
}

@Composable
private fun HomeCard(
    module: HomeModule,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(module.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = module.icon,
                    contentDescription = module.title,
                    tint = module.accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = module.title,
                color = TextWhite,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = module.subtitle,
                color = TextSilver,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
