package moe.GetTheNya.AniForge.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

data class UserInfo(
    val id: String,
    val username: String
)

@Singleton
class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    val currentUser: Flow<UserInfo?> = supabaseClient.auth.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user ?: return@map null
                    val metadata = user.userMetadata
                    val username = metadata?.get("full_name")?.jsonPrimitive?.contentOrNull
                        ?: metadata?.get("name")?.jsonPrimitive?.contentOrNull
                        ?: user.email?.substringBefore("@")
                        ?: "User"
                    UserInfo(id = user.id, username = username)
                }
                else -> null
            }
        }

    val currentSessionUser: UserInfo?
        get() {
            val user = supabaseClient.auth.currentUserOrNull() ?: return null
            val metadata = user.userMetadata
            val username = metadata?.get("full_name")?.jsonPrimitive?.contentOrNull
                ?: metadata?.get("name")?.jsonPrimitive?.contentOrNull
                ?: user.email?.substringBefore("@")
                ?: "User"
            return UserInfo(id = user.id, username = username)
        }

    suspend fun signInWithIdToken(idToken: String): Result<Unit> {
        return try {
            supabaseClient.auth.signInWith(IDToken) {
                this.idToken = idToken
                this.provider = Google
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            supabaseClient.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
