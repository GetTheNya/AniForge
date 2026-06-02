package moe.GetTheNya.AniForge.core.database.sync

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.GetTheNya.AniForge.core.database.CatalogDatabaseProvider
import moe.GetTheNya.AniForge.core.model.sync.CatalogDownloader
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.database.util.GzipDecompressor
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseProvider: CatalogDatabaseProvider,
    private val settingsProvider: SettingsProvider,
    private val catalogDownloader: CatalogDownloader
) {
    /**
     * Checks if a catalog update is available from the remote API.
     * If an update is found, downloads, decompresses, verifies, and hot-swaps it.
     * Returns true if a swap was successfully completed, false otherwise.
     */
    suspend fun updateCatalogIfAvailable(): Boolean = withContext(Dispatchers.IO) {
        val currentVersion = settingsProvider.getCatalogVersion()
        val latestVersion = catalogDownloader.fetchLatestVersion() ?: return@withContext false

        if (latestVersion <= currentVersion) {
            // Already up-to-date or remote version is older
            return@withContext false
        }

        val standbyFileName = settingsProvider.getStandbyCatalogFileName()
        val standbyFile = context.getDatabasePath(standbyFileName)
        
        // Ensure parent database folder exists
        standbyFile.parentFile?.mkdirs()

        val tempGzipFile = File(context.cacheDir, "catalog_download_${latestVersion}.db.gz")

        try {
            // 1. Download database gzip archive
            val downloadSuccess = catalogDownloader.downloadCatalog(latestVersion, tempGzipFile)
            if (!downloadSuccess || !tempGzipFile.exists()) {
                tempGzipFile.delete()
                return@withContext false
            }

            // 2. Decompress directly into the standby file slot
            if (standbyFile.exists()) {
                standbyFile.delete()
            }
            GzipDecompressor.decompressFile(tempGzipFile, standbyFile)
            tempGzipFile.delete()

            // 3. Verify SQLite Integrity
            val isVerified = verifyDatabaseIntegrity(standbyFile)
            if (!isVerified) {
                standbyFile.delete()
                return@withContext false
            }

            // 4. Perform the dynamic Room connection hotswap
            return@withContext databaseProvider.hotSwapToStandby(latestVersion)
        } catch (e: Exception) {
            // Clean up any remaining temporary files
            if (tempGzipFile.exists()) tempGzipFile.delete()
            if (standbyFile.exists()) standbyFile.delete()
            false
        }
    }

    /**
     * Opens a temporary direct SQLite connection to run integrity checks on the database file.
     */
    private fun verifyDatabaseIntegrity(dbFile: File): Boolean {
        return try {
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath, 
                null, 
                SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                db.rawQuery("PRAGMA integrity_check;", null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val result = cursor.getString(0)
                        result.equals("ok", ignoreCase = true)
                    } else {
                        false
                    }
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}
