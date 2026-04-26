package com.vinh.dyvat.data.repository

import com.vinh.dyvat.data.model.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
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

    fun signInWithGoogle(idToken: String): Flow<Result<UserInfo>> = flow {
        emit(Result.Loading)
        try {
            supabaseClient.auth.signInWith(IDToken) {
                this.idToken = idToken
            }
            val user = supabaseClient.auth.retrieveUserForCurrentSession()
            emit(Result.Success(user))
        } catch (e: Exception) {
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
