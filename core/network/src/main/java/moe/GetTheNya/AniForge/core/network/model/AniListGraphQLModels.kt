package moe.GetTheNya.AniForge.core.network.model

import com.google.gson.annotations.SerializedName

data class AniListGraphQLPayload(
    @SerializedName("query") val query: String,
    @SerializedName("variables") val variables: Map<String, Any?>
)

data class AniListGraphQLResponse(
    @SerializedName("data") val data: AniListResponseData?
)

data class AniListResponseData(
    @SerializedName("Media") val Media: AniListMedia?
)

data class AniListMedia(
    @SerializedName("id") val id: Long,
    @SerializedName("idMal") val idMal: Long?
)
