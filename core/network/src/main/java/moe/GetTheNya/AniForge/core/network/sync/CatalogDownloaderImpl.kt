package moe.GetTheNya.AniForge.core.network.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.GetTheNya.AniForge.core.model.sync.CatalogDownloader
import moe.GetTheNya.AniForge.core.network.api.AniForgeApiService
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogDownloaderImpl @Inject constructor(
    private val apiService: AniForgeApiService
) : CatalogDownloader {

    override suspend fun fetchLatestVersion(): Long? = withContext(Dispatchers.IO) {
        val response = apiService.getVersion()
        response.version
    }

    override suspend fun downloadCatalog(version: Long, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.downloadCatalog(version)
            if (!response.isSuccessful) {
                android.util.Log.e("CatalogDownloader", "downloadCatalog response unsuccessful: ${response.code()}")
                return@withContext false
            }

            val body = response.body() ?: return@withContext false
            val tempFile = File(destinationFile.absolutePath + ".tmp")
            
            tempFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    input.copyTo(output)
                }
            }

            if (tempFile.exists()) {
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }
                tempFile.renameTo(destinationFile)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("CatalogDownloader", "downloadCatalog failed", e)
            false
        }
    }
}
