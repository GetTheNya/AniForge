package moe.GetTheNya.AniForge.core.model.sync

import java.io.File

interface CatalogDownloader {
    /**
     * Fetches the latest catalog version info from the CDN.
     * Returns null if the request fails.
     */
    suspend fun fetchLatestVersionInfo(): CatalogVersionInfo?

    /**
     * Downloads the catalog.db.gz file for the given version and writes it to the destination file.
     * Returns true if the download and save succeeded, false otherwise.
     */
    suspend fun downloadCatalog(
        version: Long,
        destinationFile: File,
        onProgress: (progress: Float) -> Unit = {}
    ): Boolean
}
