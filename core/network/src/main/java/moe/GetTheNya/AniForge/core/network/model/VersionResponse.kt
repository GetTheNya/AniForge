package moe.GetTheNya.AniForge.core.network.model

import com.google.gson.annotations.SerializedName

data class VersionResponse(
    @SerializedName("version")
    val version: Long,
    @SerializedName("updated_at")
    val updatedAt: Long? = null
)
