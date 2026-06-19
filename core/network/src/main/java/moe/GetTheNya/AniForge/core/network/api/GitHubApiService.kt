package moe.GetTheNya.AniForge.core.network.api

import moe.GetTheNya.AniForge.core.network.model.GitHubReleaseResponse
import retrofit2.http.GET
import retrofit2.http.Headers

interface GitHubApiService {
    @Headers("User-Agent: AniForge")
    @GET("https://api.github.com/repos/GetTheNya/AniForge/releases/latest")
    suspend fun getLatestRelease(): GitHubReleaseResponse
}
