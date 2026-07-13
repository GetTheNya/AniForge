package moe.GetTheNya.AniForge.core.network

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

class UsernameTakenException(message: String, cause: Throwable? = null) : Exception(message, cause)
class UsernameFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Serializable
private data class DbUserProfile(
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
private data class DbUserProfileUpdate(
    val username: String
)

data class UserInfo(
    val id: String,
    val username: String,
    val avatarUrl: String? = null,
    val isProfileLoading: Boolean = false
)

@Singleton
class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _customUsername = MutableStateFlow<String?>(null)
    private val _avatarUrl = MutableStateFlow<String?>(null)
    private val _isProfileLoading = MutableStateFlow(false)

    init {
        repositoryScope.launch {
            supabaseClient.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        if (user != null) {
                            fetchAndCacheUsername(user.id)
                        }
                    }
                    else -> {
                        _customUsername.value = null
                        _avatarUrl.value = null
                        _isProfileLoading.value = false
                    }
                }
            }
        }
    }

    val currentUser: Flow<UserInfo?> = combine(
        supabaseClient.auth.sessionStatus,
        _customUsername,
        _avatarUrl,
        _isProfileLoading
    ) { status, customName, avatar, isLoading ->
        when (status) {
            is SessionStatus.Authenticated -> {
                val user = status.session.user ?: return@combine null
                val metadata = user.userMetadata
                val defaultUsername = metadata?.get("full_name")?.jsonPrimitive?.contentOrNull
                    ?: metadata?.get("name")?.jsonPrimitive?.contentOrNull
                    ?: user.email?.substringBefore("@")
                    ?: "User"
                UserInfo(
                    id = user.id,
                    username = customName ?: defaultUsername,
                    avatarUrl = avatar,
                    isProfileLoading = isLoading
                )
            }
            else -> null
        }
    }

    val currentSessionUser: UserInfo?
        get() {
            val user = supabaseClient.auth.currentUserOrNull() ?: return null
            val metadata = user.userMetadata
            val defaultUsername = metadata?.get("full_name")?.jsonPrimitive?.contentOrNull
                ?: metadata?.get("name")?.jsonPrimitive?.contentOrNull
                ?: user.email?.substringBefore("@")
                ?: "User"
            return UserInfo(
                id = user.id,
                username = _customUsername.value ?: defaultUsername,
                avatarUrl = _avatarUrl.value,
                isProfileLoading = _isProfileLoading.value
            )
        }

    suspend fun fetchUserProfile() {
        val user = supabaseClient.auth.currentUserOrNull() ?: return
        fetchAndCacheUsername(user.id)
    }

    private suspend fun fetchAndCacheUsername(userId: String) {
        _isProfileLoading.value = true
        try {
            val profile = supabaseClient.from("user_profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<DbUserProfile>()
            if (profile != null) {
                _customUsername.value = profile.username
                _avatarUrl.value = profile.avatarUrl
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching username from user_profiles: ${e.message}", e)
        } finally {
            _isProfileLoading.value = false
        }
    }

    suspend fun updateUsername(userId: String, username: String): Result<Unit> {
        return try {
            supabaseClient.from("user_profiles")
                .update(DbUserProfileUpdate(username = username)) {
                    filter {
                        eq("id", userId)
                    }
                }
            _customUsername.value = username
            Result.success(Unit)
        } catch (e: PostgrestRestException) {
            Log.e("AuthRepository", "Postgrest error updating username: code=${e.code}, message=${e.message}", e)
            when (e.code) {
                "23505" -> Result.failure(UsernameTakenException(e.message ?: "Username already taken", e))
                "23514" -> Result.failure(UsernameFormatException(e.message ?: "Invalid username format on server", e))
                else -> Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Generic error updating username: ${e.message}", e)
            Result.failure(e)
        }
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
