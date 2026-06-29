package moe.GetTheNya.AniForge.core.network.api

import moe.GetTheNya.AniForge.core.network.model.AniListGraphQLPayload
import moe.GetTheNya.AniForge.core.network.model.AniListGraphQLResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AniListApiService {
    @POST("https://graphql.anilist.co")
    suspend fun queryAniList(
        @Body payload: AniListGraphQLPayload
    ): AniListGraphQLResponse
}
