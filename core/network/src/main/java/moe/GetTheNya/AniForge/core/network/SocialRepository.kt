package moe.GetTheNya.AniForge.core.network

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
enum class FriendshipStatus {
    @SerialName("PENDING") PENDING,
    @SerialName("ACCEPTED") ACCEPTED,
    @SerialName("BLOCKED") BLOCKED
}

@Serializable
data class FriendshipDto(
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("status") val status: FriendshipStatus,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class UserProfileDto(
    val id: String,
    val username: String
)

@Serializable
data class FriendshipInsertDto(
    @SerialName("receiver_id") val receiverId: String
)

@Serializable
data class FriendshipUpdateDto(
    @SerialName("status") val status: FriendshipStatus
)

@Serializable
data class SupabaseUserTrackingDto(
    @SerialName("user_id") val userId: String,
    @SerialName("anilist_id") val anilistId: Long,
    @SerialName("watch_status") val watchStatus: String,
    @SerialName("episode_progress") val episodeProgress: Int,
    @SerialName("score") val score: Double? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("last_modified") val lastModified: String,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class SupabaseCollectionDto(
    @SerialName("user_id") val userId: String,
    @SerialName("collection_id") val collectionId: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("last_modified") val lastModified: String,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class SupabaseCollectionAnimeCrossRefDto(
    @SerialName("user_id") val userId: String,
    @SerialName("collection_id") val collectionId: String,
    @SerialName("anime_id") val animeId: Long,
    @SerialName("order_index") val orderIndex: Int,
    @SerialName("last_modified") val lastModified: String,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)


@Singleton
class SocialRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    companion object {
        private const val TAG = "SocialRepository"
    }

    suspend fun searchUsers(query: String, currentUserId: String): List<UserProfileDto> = withContext(Dispatchers.IO) {
        try {
            supabaseClient.from("user_profiles")
                .select {
                    filter {
                        ilike("username", "%$query%")
                        neq("id", currentUserId)
                    }
                }
                .decodeList<UserProfileDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getFriends(userId: String): List<UserProfileDto> = withContext(Dispatchers.IO) {
        try {
            val friendships = supabaseClient.from("friendships")
                .select {
                    filter {
                        eq("status", "ACCEPTED")
                        or {
                            eq("sender_id", userId)
                            eq("receiver_id", userId)
                        }
                    }
                }
                .decodeList<FriendshipDto>()

            val friendIds = friendships.map {
                if (it.senderId == userId) it.receiverId else it.senderId
            }
            if (friendIds.isEmpty()) return@withContext emptyList()

            supabaseClient.from("user_profiles")
                .select {
                    filter {
                        isIn("id", friendIds)
                    }
                }
                .decodeList<UserProfileDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching friends: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getIncomingRequests(userId: String): List<UserProfileDto> = withContext(Dispatchers.IO) {
        try {
            val requests = supabaseClient.from("friendships")
                .select {
                    filter {
                        eq("receiver_id", userId)
                        eq("status", "PENDING")
                    }
                }
                .decodeList<FriendshipDto>()

            val senderIds = requests.map { it.senderId }
            if (senderIds.isEmpty()) return@withContext emptyList()

            supabaseClient.from("user_profiles")
                .select {
                    filter {
                        isIn("id", senderIds)
                    }
                }
                .decodeList<UserProfileDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching incoming requests: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getOutgoingRequests(userId: String): List<UserProfileDto> = withContext(Dispatchers.IO) {
        try {
            val requests = supabaseClient.from("friendships")
                .select {
                    filter {
                        eq("sender_id", userId)
                        eq("status", "PENDING")
                    }
                }
                .decodeList<FriendshipDto>()

            val receiverIds = requests.map { it.receiverId }
            if (receiverIds.isEmpty()) return@withContext emptyList()

            supabaseClient.from("user_profiles")
                .select {
                    filter {
                        isIn("id", receiverIds)
                    }
                }
                .decodeList<UserProfileDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching outgoing requests: ${e.message}", e)
            emptyList()
        }
    }


    suspend fun getMyFriendships(userId: String): List<FriendshipDto> = withContext(Dispatchers.IO) {
        try {
            supabaseClient.from("friendships")
                .select {
                    filter {
                        or {
                            eq("sender_id", userId)
                            eq("receiver_id", userId)
                        }
                    }
                }
                .decodeList<FriendshipDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching friendships: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun sendFriendRequest(receiverId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabaseClient.from("friendships")
                .insert(FriendshipInsertDto(receiverId = receiverId))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending friend request: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(senderId: String, receiverId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabaseClient.from("friendships")
                .update(FriendshipUpdateDto(status = FriendshipStatus.ACCEPTED)) {
                    filter {
                        eq("sender_id", senderId)
                        eq("receiver_id", receiverId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun declineOrRemoveFriendship(senderId: String, receiverId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabaseClient.from("friendships")
                .delete {
                    filter {
                        or {
                            and {
                                eq("sender_id", senderId)
                                eq("receiver_id", receiverId)
                            }
                            and {
                                eq("sender_id", receiverId)
                                eq("receiver_id", senderId)
                            }
                        }
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error declining or removing friendship: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getFriendTrackingList(friendUserId: String): List<SupabaseUserTrackingDto> = withContext(Dispatchers.IO) {
        try {
            supabaseClient.from("user_tracking")
                .select {
                    filter {
                        eq("user_id", friendUserId)
                        eq("watch_status", "CURRENT")
                        eq("is_deleted", false)
                    }
                }
                .decodeList<SupabaseUserTrackingDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user tracking list: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getRemoteUserTrackingList(targetUserId: String): List<SupabaseUserTrackingDto> = withContext(Dispatchers.IO) {
        try {
            val allItems = mutableListOf<SupabaseUserTrackingDto>()
            var page = 0
            var hasMore = true
            while (hasMore) {
                val start = page * 1000
                val end = start + 999
                val pageItems = supabaseClient.from("user_tracking")
                    .select {
                        filter {
                            eq("user_id", targetUserId)
                            eq("is_deleted", false)
                        }
                        range(start.toLong(), end.toLong())
                    }
                    .decodeList<SupabaseUserTrackingDto>()
                allItems.addAll(pageItems)
                if (pageItems.size < 1000) {
                    hasMore = false
                } else {
                    page++
                }
            }
            allItems
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote user tracking list: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getRemoteCollections(targetUserId: String): List<SupabaseCollectionDto> = withContext(Dispatchers.IO) {
        try {
            val allItems = mutableListOf<SupabaseCollectionDto>()
            var page = 0
            var hasMore = true
            while (hasMore) {
                val start = page * 1000
                val end = start + 999
                val pageItems = supabaseClient.from("collections")
                    .select {
                        filter {
                            eq("user_id", targetUserId)
                            eq("is_deleted", false)
                        }
                        range(start.toLong(), end.toLong())
                    }
                    .decodeList<SupabaseCollectionDto>()
                allItems.addAll(pageItems)
                if (pageItems.size < 1000) {
                    hasMore = false
                } else {
                    page++
                }
            }
            allItems
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote collections: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getRemoteCollectionCrossRefs(targetUserId: String): List<SupabaseCollectionAnimeCrossRefDto> = withContext(Dispatchers.IO) {
        try {
            val allItems = mutableListOf<SupabaseCollectionAnimeCrossRefDto>()
            var page = 0
            var hasMore = true
            while (hasMore) {
                val start = page * 1000
                val end = start + 999
                val pageItems = supabaseClient.from("collection_anime_cross_ref")
                    .select {
                        filter {
                            eq("user_id", targetUserId)
                            eq("is_deleted", false)
                        }
                        range(start.toLong(), end.toLong())
                    }
                    .decodeList<SupabaseCollectionAnimeCrossRefDto>()
                allItems.addAll(pageItems)
                if (pageItems.size < 1000) {
                    hasMore = false
                } else {
                    page++
                }
            }
            allItems
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote collection cross refs: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getRemoteCollection(targetUserId: String, collectionId: String): SupabaseCollectionDto? = withContext(Dispatchers.IO) {
        try {
            supabaseClient.from("collections")
                .select {
                    filter {
                        eq("user_id", targetUserId)
                        eq("collection_id", collectionId)
                        eq("is_deleted", false)
                    }
                }
                .decodeSingleOrNull<SupabaseCollectionDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote collection: ${e.message}", e)
            null
        }
    }

    suspend fun getRemoteCollectionCrossRefsForCollection(targetUserId: String, collectionId: String): List<SupabaseCollectionAnimeCrossRefDto> = withContext(Dispatchers.IO) {
        try {
            val allItems = mutableListOf<SupabaseCollectionAnimeCrossRefDto>()
            var page = 0
            var hasMore = true
            while (hasMore) {
                val start = page * 1000
                val end = start + 999
                val pageItems = supabaseClient.from("collection_anime_cross_ref")
                    .select {
                        filter {
                            eq("user_id", targetUserId)
                            eq("collection_id", collectionId)
                            eq("is_deleted", false)
                        }
                        range(start.toLong(), end.toLong())
                    }
                    .decodeList<SupabaseCollectionAnimeCrossRefDto>()
                allItems.addAll(pageItems)
                if (pageItems.size < 1000) {
                    hasMore = false
                } else {
                    page++
                }
            }
            allItems
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote collection cross refs: ${e.message}", e)
            emptyList()
        }
    }
}
