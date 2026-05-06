package com.vinh.dyvat.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vinh.dyvat.ui.navigation.Screen
import com.vinh.dyvat.ui.screens.auth.AuthState
import com.vinh.dyvat.ui.screens.auth.AuthViewModel
import com.vinh.dyvat.ui.theme.AnnouncementBlue
import com.vinh.dyvat.ui.theme.BorderGray
import com.vinh.dyvat.ui.theme.DarkCard
import com.vinh.dyvat.ui.theme.DarkSurface
import com.vinh.dyvat.ui.theme.MidDark
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import com.vinh.dyvat.ui.theme.TextWhite
import com.vinh.dyvat.ui.theme.WarningOrange

data class HomeModule(
    val title: String,
    val icon: ImageVector,
    val iconTint: Color = SpotifyGreen,
    val route: String
)

data class QuickAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color,
    val bgColor: Color,
    val route: String
)

private val quickActions = listOf(
    QuickAction(
        title = "Nhập hàng",
        subtitle = "Tạo phiếu nhập mới",
        icon = Icons.Default.ShoppingCart,
        iconTint = SpotifyGreen,
        bgColor = SpotifyGreen.copy(alpha = 0.12f),
        route = Screen.PurchaseList.route
    ),
    QuickAction(
        title = "Bán hàng",
        subtitle = "Tạo phiếu bán mới",
        icon = Icons.Default.Store,
        iconTint = AnnouncementBlue,
        bgColor = AnnouncementBlue.copy(alpha = 0.12f),
        route = Screen.Sales.route
    )
)

private val secondaryModules = listOf(
    HomeModule(
        title = "Kho hàng",
        icon = Icons.Default.Inventory,
        iconTint = WarningOrange,
        route = Screen.Inventory.route
    ),
    HomeModule(
        title = "Thống kê",
        icon = Icons.Default.BarChart,
        iconTint = SpotifyGreen,
        route = Screen.Statistics.route
    ),
    HomeModule(
        title = "Sản phẩm",
        icon = Icons.Default.Store,
        iconTint = AnnouncementBlue,
        route = Screen.Products.route
    ),
    HomeModule(
        title = "Loại SP",
        icon = Icons.Default.Category,
        iconTint = TextSilver,
        route = Screen.Categories.route
    ),
    HomeModule(
        title = "Đơn vị",
        icon = Icons.Default.Straighten,
        iconTint = TextSilver,
        route = Screen.Units.route
    ),
    HomeModule(
        title = "Nhà cung cấp",
        icon = Icons.Default.LocalShipping,
        iconTint = TextSilver,
        route = Screen.Suppliers.route
    )
)

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onSignedOut: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authUiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(authUiState.authState) {
        if (authUiState.authState is AuthState.NotLoggedIn) {
            onSignedOut()
        }
    }

    Scaffold(containerColor = NearBlack) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NearBlack)
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { HomeTopBar(onSignOut = { authViewModel.signOut() }) }
            item { GreetingSection() }
            item { QuickActionsSection(quickActions = quickActions, onNavigate = onNavigate) }
            item {
                SectionDivider()
                SectionHeader(title = "Danh mục")
            }
            item { SecondaryModulesGrid(modules = secondaryModules, onNavigate = onNavigate) }
        }
    }
}

@Composable
private fun HomeTopBar(onSignOut: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SpotifyGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "D",
                    color = NearBlack,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = "Dyvat",
                color = TextWhite,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MidDark)
                .clickable { onSignOut() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Đăng xuất",
                tint = TextSilver,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun GreetingSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 24.dp)
    ) {
        Text(
            text = "Xin chào 👋",
            color = TextSilver,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = "Quản lý cửa hàng\ncủa bạn",
            color = TextWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp
        )
    }
}

@Composable
private fun QuickActionsSection(
    quickActions: List<QuickAction>,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        quickActions.forEach { action ->
            QuickActionCard(action = action, onClick = { onNavigate(action.route) })
        }
    }
}

@Composable
private fun QuickActionCard(action: QuickAction, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(action.bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                tint = action.iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action.title,
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = action.subtitle,
                color = TextSilver,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = BorderGray,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextSilver,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun SecondaryModulesGrid(
    modules: List<HomeModule>,
    onNavigate: (String) -> Unit
) {
    val rows = modules.chunked(2)
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { rowModules ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowModules.forEach { module ->
                    SecondaryModuleCard(
                        module = module,
                        onClick = { onNavigate(module.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(2 - rowModules.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SecondaryModuleCard(
    module: HomeModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(module.iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = module.icon,
                contentDescription = module.title,
                tint = module.iconTint,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = module.title,
            color = TextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
