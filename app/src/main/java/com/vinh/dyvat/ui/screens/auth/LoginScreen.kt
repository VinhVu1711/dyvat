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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
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
import java.security.MessageDigest

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var credentialLoading by remember { mutableStateOf(false) }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }
    val isSigningIn = credentialLoading || uiState.isLoading

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

    LaunchedEffect(localErrorMessage) {
        localErrorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            localErrorMessage = null
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
                    text = "Quản lý nhập và bán hàng",
                    color = TextSilver,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(64.dp))

                Button(
                    onClick = {
                        credentialLoading = true
                        val rawNonce = generateNonce()
                        performGoogleSignIn(
                            context = context,
                            rawNonce = rawNonce,
                            onIdTokenReceived = { idToken ->
                                credentialLoading = false
                                viewModel.signInWithGoogle(idToken, rawNonce)
                            },
                            onError = { errorMessage ->
                                credentialLoading = false
                                Log.e("LoginScreen", "Google Sign-In error: $errorMessage")
                                localErrorMessage = errorMessage
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .alpha(if (isSigningIn) 0.72f else 1f)
                        .blur(if (isSigningIn) 0.35.dp else 0.dp),
                    enabled = !isSigningIn,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = NearBlack
                    )
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            color = NearBlack,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = "Đang đăng nhập...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Đăng nhập bằng Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Đăng nhập để truy cập ứng dụng",
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
    rawNonce: String,
    onIdTokenReceived: (String) -> Unit,
    onError: (String) -> Unit
) {
    val tag = "GoogleSignIn"
    val clientId = context.getString(R.string.google_oauth_client_id)

    Log.d(tag, "=== Google Sign-In Flow Started ===")
    Log.d(tag, "Using Client ID: $clientId")
    Log.d(tag, "Raw nonce: $rawNonce")

    val credentialManager = try {
        CredentialManager.create(context)
    } catch (e: Exception) {
        Log.e(tag, "CredentialManager.create() failed: ${e.message}")
        onError("Không thể khởi tạo CredentialManager: ${e.message}")
        return
    }
    Log.d(tag, "CredentialManager created successfully")

    val hashedNonce = hashNonce(rawNonce)
    Log.d(tag, "Hashed nonce: $hashedNonce")

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setNonce(hashedNonce)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    Log.d(tag, "GetCredentialRequest built. Requesting credential...")

    CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()).launch {
        try {
            Log.d(tag, "Calling credentialManager.getCredential()...")
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )
            Log.d(tag, "getCredential succeeded. Credential type: ${result.credential.type}")
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val idToken = googleIdTokenCredential.idToken
            Log.d(tag, "ID Token obtained. Length: ${idToken.length}")
            onIdTokenReceived(idToken)
        } catch (e: GetCredentialException) {
            Log.e(tag, "GetCredentialException: ${e::class.simpleName}")
            Log.e(tag, "Exception message: ${e.message}")
            Log.e(tag, "Exception type: ${e.type}")

            when (e) {
                is NoCredentialException -> {
                    Log.w(tag, "NoCredentialException caught. Trying fallback (unfiltered accounts)...")
                    val fallbackOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(clientId)
                        .setNonce(hashedNonce)
                        .build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(fallbackOption)
                        .build()
                    try {
                        Log.d(tag, "Calling credentialManager.getCredential() [fallback]...")
                        val fallbackResult = credentialManager.getCredential(
                            request = fallbackRequest,
                            context = context
                        )
                        Log.d(tag, "Fallback getCredential succeeded.")
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(fallbackResult.credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        Log.d(tag, "Fallback ID Token obtained.")
                        onIdTokenReceived(idToken)
                    } catch (e2: GetCredentialException) {
                        Log.e(tag, "Fallback GetCredentialException: ${e2::class.simpleName}")
                        Log.e(tag, "Fallback exception message: ${e2.message}")
                        Log.e(tag, "Fallback exception type: ${e2.type}")
                        onError(getErrorMessage(e2))
                    }
                }
                is GetCredentialCancellationException -> {
                    Log.w(tag, "Sign-in was cancelled by user")
                    onError("Đăng nhập bị hủy")
                }
                else -> {
                    Log.e(tag, "Unhandled GetCredentialException type")
                    onError(getErrorMessage(e))
                }
            }
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(tag, "GoogleIdTokenParsingException: ${e.message}")
            onError("Token không hợp lệ")
        } catch (e: Exception) {
            Log.e(tag, "Unexpected exception: ${e::class.simpleName}: ${e.message}")
            onError(e.message ?: "Lỗi không xác định")
        }
    }
}

private fun generateNonce(): String {
    return java.util.UUID.randomUUID().toString()
}

private fun hashNonce(rawNonce: String): String {
    val bytes = rawNonce.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}

private fun getErrorMessage(e: GetCredentialException): String {
    val tag = "GoogleSignIn"
    val typeExtra = try {
        // e.type is available in some versions, fall back to message inspection
        e.type?.let { "type=$it" }
    } catch (_: Exception) { null }
    Log.e(tag, "Error details — message: '${e.message}', $typeExtra")
    return when {
        e.message?.contains("DEVELOPER_ERROR", ignoreCase = true) == true ->
            "Lỗi cấu hình Google. Vui lòng kiểm tra SHA-1 và OAuth Client ID."
        e.message?.contains("network", ignoreCase = true) == true ->
            "Lỗi mạng. Vui lòng kiểm tra kết nối internet."
        e.message?.contains("SIGN_IN_FAILED", ignoreCase = true) == true ->
            "Đăng nhập Google thất bại. Kiểm tra SHA-1 và Client ID."
        e.message?.contains("invalid_client", ignoreCase = true) == true ->
            "OAuth Client ID không hợp lệ."
        else -> e.message ?: "Lỗi đăng nhập Google"
    }
}
