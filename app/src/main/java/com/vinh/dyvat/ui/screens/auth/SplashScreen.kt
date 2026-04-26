package com.vinh.dyvat.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vinh.dyvat.R
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkAuthState()
    }

    LaunchedEffect(uiState.authState) {
        when (uiState.authState) {
            is AuthState.LoggedIn -> onNavigateToMain()
            is AuthState.NotLoggedIn -> onNavigateToLogin()
            is AuthState.Error -> onNavigateToLogin()
            is AuthState.Unknown -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_dyvat_logo),
                contentDescription = "Dyvat Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Dyvat",
                color = SpotifyGreen,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Quản lý nhập & bán hàng",
                color = TextSilver,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = SpotifyGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
