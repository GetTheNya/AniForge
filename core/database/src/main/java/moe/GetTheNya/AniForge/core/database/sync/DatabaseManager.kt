package moe.GetTheNya.AniForge.core.database.sync

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import moe.GetTheNya.AniForge.core.database.CatalogDatabaseProvider
import moe.GetTheNya.AniForge.core.model.sync.CatalogDownloader
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.database.util.AppLogger
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
    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    /**
     * Checks if a catalog update is available from the remote API.
     * If an update is found, downloads, decompresses, verifies, and hot-swaps it.
     * Returns true if a swap was successfully completed, false otherwise.
     */
    suspend fun updateCatalogIfAvailable(): Boolean = withContext(Dispatchers.IO) {
        _isUpdating.value = true
        val currentVersion = settingsProvider.getCatalogVersion()
        AppLogger.i("DatabaseManager", "Checking for catalog database update. Current local version: $currentVersion")
        
        var tempGzipFile: File? = null
        var standbyFile: File? = null
        
        try {
            val latestVersion = catalogDownloader.fetchLatestVersion()
            if (latestVersion == null) {
                AppLogger.w("DatabaseManager", "Failed to fetch latest catalog version from remote CDN.")
                return@withContext false
            }

            if (latestVersion <= currentVersion) {
                AppLogger.i("DatabaseManager", "Catalog is already up to date. Version: $currentVersion")
                return@withContext false
            }

            val standbyFileName = settingsProvider.getStandbyCatalogFileName()
            val file = context.getDatabasePath(standbyFileName)
            standbyFile = file
            
            AppLogger.i("DatabaseManager", "New catalog version available: $latestVersion. Preparing download to standby slot: $standbyFileName")

            // Ensure parent database folder exists
            file.parentFile?.mkdirs()

            val tempFile = File(context.cacheDir, "catalog_download_${latestVersion}.db.gz")
            tempGzipFile = tempFile

            // 1. Download database gzip archive
            AppLogger.i("DatabaseManager", "Downloading catalog archive for version: $latestVersion...")
            val downloadSuccess = catalogDownloader.downloadCatalog(latestVersion, tempFile)
            if (!downloadSuccess || !tempFile.exists()) {
                AppLogger.e("DatabaseManager", "Failed to download catalog archive from remote CDN.")
                tempFile.delete()
                return@withContext false
            }

            // 2. Decompress directly into the standby file slot
            if (file.exists()) {
                file.delete()
            }
            AppLogger.i("DatabaseManager", "Download complete. Decompressing gzip archive into slot: $standbyFileName")
            GzipDecompressor.decompressFile(tempFile, file)
            tempFile.delete()

            // 3. Verify SQLite Integrity
            AppLogger.i("DatabaseManager", "Decompression successful. Verifying SQLite database integrity...")
            val isVerified = verifyDatabaseIntegrity(file)
            if (!isVerified) {
                AppLogger.e("DatabaseManager", "Database integrity verification failed for slot: $standbyFileName")
                file.delete()
                return@withContext false
            }

            // 4. Perform the dynamic Room connection hotswap
            AppLogger.i("DatabaseManager", "Integrity check passed. Hot swapping to slot: $standbyFileName")
            val swapSuccess = databaseProvider.hotSwapToStandby(latestVersion)
            if (swapSuccess) {
                AppLogger.i("DatabaseManager", "Database hot-swap completed successfully! Active catalog slot: $standbyFileName (v$latestVersion)")
            } else {
                AppLogger.e("DatabaseManager", "Database connection swap failed.")
            }
            return@withContext swapSuccess
        } catch (e: Exception) {
            AppLogger.e("DatabaseManager", "Error occurred during catalog database update", e)
            // Clean up any remaining temporary files
            if (tempGzipFile != null && tempGzipFile.exists()) tempGzipFile.delete()
            if (standbyFile != null && standbyFile.exists()) standbyFile.delete()
            false
        } finally {
            _isUpdating.value = false
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
