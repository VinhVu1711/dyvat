package com.vinh.dyvat.ui.screens.auth

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.vinh.dyvat.R
import com.vinh.dyvat.ui.theme.NearBlack
import com.vinh.dyvat.ui.theme.SpotifyGreen
import com.vinh.dyvat.ui.theme.TextSilver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.Base64

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.authState) {
        if (uiState.authState is AuthState.LoggedIn) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = NearBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(NearBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_dyvat_logo),
                    contentDescription = "Dyvat Logo",
                    modifier = Modifier.size(100.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Dyvat",
                    color = SpotifyGreen,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Quan ly nhap & ban hang",
                    color = TextSilver,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(64.dp))

                Button(
                    onClick = {
                        performGoogleSignIn(
                            context = context,
                            onIdTokenReceived = { idToken ->
                                viewModel.signInWithGoogle(idToken)
                            },
                            onError = { errorMessage ->
                                Log.e("LoginScreen", "Google Sign-In error: $errorMessage")
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = NearBlack
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = NearBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Dang nhap bang Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Dang nhap de truy cap ung dung",
                    color = TextSilver,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun performGoogleSignIn(
    context: android.content.Context,
    onIdTokenReceived: (String) -> Unit,
    onError: (String) -> Unit
) {
    val credentialManager = CredentialManager.create(context)

    val rawNonce = generateNonce()
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(context.getString(R.string.google_oauth_client_id))
        .setNonce(rawNonce)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()).launch {
        try {
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            onIdTokenReceived(googleIdTokenCredential.idToken)
        } catch (e: GetCredentialException) {
            when (e) {
                is NoCredentialException -> {
                    val fallbackOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(context.getString(R.string.google_oauth_client_id))
                        .setNonce(rawNonce)
                        .build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(fallbackOption)
                        .build()
                    try {
                        val fallbackResult = credentialManager.getCredential(
                            request = fallbackRequest,
                            context = context
                        )
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(fallbackResult.credential.data)
                        onIdTokenReceived(googleIdTokenCredential.idToken)
                    } catch (e2: GetCredentialException) {
                        onError(getErrorMessage(e2))
                    }
                }
                is GetCredentialCancellationException -> {
                    onError("Dang nhap bi huy")
                }
                else -> onError(getErrorMessage(e))
            }
        } catch (e: GoogleIdTokenParsingException) {
            onError("Token khong hop le")
        } catch (e: Exception) {
            onError(e.message ?: "Loi khong xac dinh")
        }
    }
}

private fun generateNonce(): String {
    val bytes = ByteArray(16)
    SecureRandom().nextBytes(bytes)
    return Base64.getEncoder().withoutPadding().encodeToString(bytes)
}

private fun getErrorMessage(e: GetCredentialException): String {
    return when {
        e.message?.contains("DEVELOPER_ERROR", ignoreCase = true) == true ->
            "Loi cau hinh Google. Vui long kiem tra SHA-1 va OAuth Client ID."
        e.message?.contains("network", ignoreCase = true) == true ->
            "Loi mang. Vui long kiem tra ket noi internet."
        else -> e.message ?: "Loi dang nhap Google"
    }
}
