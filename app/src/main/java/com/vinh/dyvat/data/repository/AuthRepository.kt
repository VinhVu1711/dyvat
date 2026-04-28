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
        try {
            val session = supabaseClient.auth.currentSessionOrNull()
            if (session != null) {
                val user = supabaseClient.auth.retrieveUserForCurrentSession()
                emit(Result.Success(user))
            } else {
                emit(Result.Success(null))
            }
        } catch (e: Exception) {
            emit(Result.Success(null))
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            supabaseClient.auth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Dang xuat that bai", e)
        }
    }
}
