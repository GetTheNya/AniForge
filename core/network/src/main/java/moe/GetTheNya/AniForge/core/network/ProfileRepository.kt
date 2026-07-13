package moe.GetTheNya.AniForge.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    suspend fun uploadUserAvatar(byteArray: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(IllegalStateException("No active user session"))
            
            val path = "$currentUserId/avatar.webp"
            supabaseClient.storage.from("avatars").upload(path, byteArray) {
                upsert = true
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
