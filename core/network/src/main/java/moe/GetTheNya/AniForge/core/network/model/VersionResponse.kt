package moe.GetTheNya.AniForge.core.network.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class VersionResponse(
    @SerializedName("version")
    val version: Long,
    @SerializedName("updated_at")
    val updatedAt: Long? = null
)
