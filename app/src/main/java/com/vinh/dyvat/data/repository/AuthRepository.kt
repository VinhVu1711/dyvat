package com.vinh.dyvat.data.repository

import android.util.Log
import com.vinh.dyvat.data.model.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    private val tag = "AuthRepository"

    fun signInWithGoogle(idToken: String, rawNonce: String): Flow<Result<UserInfo>> = flow {
        emit(Result.Loading)
        Log.d(tag, "=== Supabase Sign-In Started ===")
        Log.d(tag, "Token length: ${idToken.length}")
        Log.d(tag, "Raw nonce: $rawNonce")
        try {
            Log.d(tag, "Calling supabaseClient.auth.signInWith(IDToken)...")
            supabaseClient.auth.signInWith(IDToken) {
                this.idToken = idToken
                this.provider = Google
                this.nonce = rawNonce
            }
            Log.d(tag, "signInWith completed. Getting current session...")
            val session = supabaseClient.auth.currentSessionOrNull()
            Log.d(tag, "Session after sign-in: ${if (session != null) "available" else "missing"}")
            val user = supabaseClient.auth.retrieveUserForCurrentSession()
            Log.d(tag, "User retrieved: ${user.id}")
            emit(Result.Success(user))
        } catch (e: Exception) {
            Log.e(tag, "Supabase signInWith error: ${e::class.simpleName}: ${e.message}")
            e.cause?.let { Log.e(tag, "Caused by: ${it::class.simpleName}: ${it.message}") }
            emit(Result.Error(e.message ?: "Dang nhap Google that bai", e))
        }
    }

    fun getCurrentSession(): Flow<Result<UserInfo?>> = flow {
        emit(Result.Loading)
        Log.d(tag, "=== Restore Session Started ===")
        try {
            Log.d(tag, "Waiting for Supabase Auth initialization...")
            supabaseClient.auth.awaitInitialization()

            var session = supabaseClient.auth.currentSessionOrNull()
            Log.d(tag, "Session after initialization: ${if (session != null) "available" else "missing"}")

            if (session == null) {
                Log.d(tag, "No in-memory session. Loading session from storage...")
                val loadedFromStorage = supabaseClient.auth.loadFromStorage()
                Log.d(tag, "loadFromStorage result: $loadedFromStorage")
                session = supabaseClient.auth.currentSessionOrNull()
                Log.d(tag, "Session after storage load: ${if (session != null) "available" else "missing"}")
            }

            if (session != null) {
                Log.d(tag, "Refreshing current session before retrieving user...")
                runCatching {
                    supabaseClient.auth.refreshCurrentSession()
                }.onSuccess {
                    Log.d(tag, "Session refresh completed")
                }.onFailure { refreshError ->
                    Log.w(tag, "Session refresh failed, will try current session user: ${refreshError::class.simpleName}: ${refreshError.message}")
                }

                val user = supabaseClient.auth.retrieveUserForCurrentSession()
                Log.d(tag, "Restored user: ${user.id}")
                emit(Result.Success(user))
            } else {
                Log.d(tag, "No persisted session found. User is not logged in.")
                emit(Result.Success(null))
            }
        } catch (e: Exception) {
            Log.e(tag, "Restore session failed: ${e::class.simpleName}: ${e.message}", e)
            emit(Result.Error(e.message ?: "Khoi phuc phien dang nhap that bai", e))
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            Log.d(tag, "Signing out...")
            supabaseClient.auth.signOut()
            Log.d(tag, "Sign out completed")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Sign out failed: ${e::class.simpleName}: ${e.message}", e)
            Result.Error(e.message ?: "Dang xuat that bai", e)
        }
    }
}
