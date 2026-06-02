package moe.GetTheNya.AniForge.core.network.api

import moe.GetTheNya.AniForge.core.network.model.VersionResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

interface AniForgeApiService {
    @GET("version.json")
    suspend fun getVersion(
        @Query("t") timestamp: Long = System.currentTimeMillis()
    ): VersionResponse

    @GET("catalog.db.gz")
    @Streaming
    suspend fun downloadCatalog(
        @Query("v") version: Long
    ): Response<ResponseBody>

    companion object {
        const val BASE_URL = "https://aniforge-api.romaro422.workers.dev/"
    }
}
