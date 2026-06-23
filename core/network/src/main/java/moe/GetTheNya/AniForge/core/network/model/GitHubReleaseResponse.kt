package moe.GetTheNya.AniForge.core.network.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class GitHubReleaseResponse(
    @SerializedName("tag_name")
    val tagName: String,
    @SerializedName("html_url")
    val htmlUrl: String,
    @SerializedName("assets")
    val assets: List<GitHubAssetResponse> = emptyList()
)

@Keep
data class GitHubAssetResponse(
    @SerializedName("name")
    val name: String,
    @SerializedName("browser_download_url")
    val browserDownloadUrl: String
)
