package moe.GetTheNya.AniForge.core.database.util

import android.content.Context
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import io.requery.android.database.sqlite.SQLiteDatabase
import moe.GetTheNya.AniForge.core.database.UserDatabase
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class BackupMetadata(
    val backupVersion: Int,
    val appVersionCode: Int,
    val animeCatalogDbVersion: Long,
    val timestamp: Long,
    val includeSettings: Boolean,
    val includeTracking: Boolean,
    val includeCollections: Boolean
)

class IncompatibleBackupException(message: String) : Exception(message)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDatabase: UserDatabase,
    private val settingsProvider: SettingsProvider
) {

    fun getAppVersionCode(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: Exception) {
            AppLogger.e("BackupManager", "Failed to retrieve app version code", e)
            1
        }
    }

    /**
     * Creates a temporary copy of user_data.db, executes cleanups on unselected modules,
     * writes backup_metadata.json, and packages everything in a ZIP file at the provided destination SAF Uri.
     */
    suspend fun exportBackup(
        uri: Uri,
        includeSettings: Boolean,
        includeTracking: Boolean,
        includeCollections: Boolean
    ): Result<Unit> = kotlin.runCatching {
        AppLogger.i("BackupManager", "Starting backup export. Settings: $includeSettings, Tracking: $includeTracking, Collections: $includeCollections")

        // 1. Force a full synchronous checkpoint to flush WAL sidecar files directly into user_data.db
        try {
            val db = userDatabase.openHelper.writableDatabase
            db.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
                cursor.moveToFirst()
            }
            AppLogger.i("BackupManager", "Database WAL checkpoint completed successfully.")
        } catch (e: Exception) {
            AppLogger.e("BackupManager", "Failed to perform WAL checkpoint before cloning", e)
        }

        // 2. Clone active user_data.db to a temp file
        val dbFile = context.getDatabasePath("user_data.db")
        if (!dbFile.exists()) {
            throw IllegalStateException("Active user database file does not exist")
        }

        val tempDbFile = File(context.cacheDir, "temp_backup.db")
        if (tempDbFile.exists()) tempDbFile.delete()

        dbFile.inputStream().use { input ->
            tempDbFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // 3. Perform SQL cleanups on the cloned database
        SQLiteDatabase.openDatabase(tempDbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            if (!includeSettings) {
                db.execSQL("DELETE FROM user_settings;")
                AppLogger.i("BackupManager", "Cleaned settings from backup database clone.")
            }
            if (!includeTracking) {
                db.execSQL("DELETE FROM user_tracking;")
                db.execSQL("DELETE FROM user_stats;")
                try {
                    db.execSQL("DELETE FROM pending_imports;")
                } catch (_: Exception) {}
                AppLogger.i("BackupManager", "Cleaned tracking lists, user stats, and pending imports from backup database clone.")
            }
            if (!includeCollections) {
                db.execSQL("DELETE FROM collections;")
                db.execSQL("DELETE FROM collection_anime_cross_ref;")
                AppLogger.i("BackupManager", "Cleaned collections from backup database clone.")
            }
            try {
                db.execSQL("VACUUM;")
            } catch (e: Exception) {
                AppLogger.e("BackupManager", "Failed to VACUUM temporary backup database clone", e)
            }
        }

        // 4. Create metadata descriptor
        val appVersionCode = getAppVersionCode()
        val animeCatalogDbVersion = settingsProvider.getCatalogVersion()
        val timestamp = System.currentTimeMillis()

        val metadataJson = JSONObject().apply {
            put("backup_version", 1)
            put("app_version_code", appVersionCode)
            put("anime_catalog_db_version", animeCatalogDbVersion)
            put("timestamp", timestamp)
            put("included_modules", JSONObject().apply {
                put("settings", includeSettings)
                put("tracking_lists", includeTracking)
                put("custom_collections", includeCollections)
            })
        }.toString(2)

        // 5. ZIP and write output to destination SAF Uri
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            ZipOutputStream(outputStream).use { zipOut ->
                // Write backup_metadata.json
                zipOut.putNextEntry(ZipEntry("backup_metadata.json"))
                zipOut.write(metadataJson.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()

                // Write user_data.db
                zipOut.putNextEntry(ZipEntry("user_data.db"))
                tempDbFile.inputStream().use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
        } ?: throw IllegalStateException("Could not open destination output stream")

        // 6. Clean up temp files
        tempDbFile.delete()
        try {
            File(tempDbFile.path + "-journal").delete()
            File(tempDbFile.path + "-wal").delete()
            File(tempDbFile.path + "-shm").delete()
        } catch (_: Exception) {}

        AppLogger.i("BackupManager", "Backup successfully exported to SAF URI: $uri")
    }

    /**
     * Extracts and parses the backup_metadata.json from the zip archive at the given Uri.
     */
    suspend fun parseBackupMetadata(uri: Uri): Result<BackupMetadata> = kotlin.runCatching {
        AppLogger.i("BackupManager", "Parsing backup metadata from: $uri")
        var metadataJsonString: String? = null

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    if (entry.name == "backup_metadata.json") {
                        metadataJsonString = zipIn.bufferedReader(Charsets.UTF_8).readText()
                        break
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        }

        if (metadataJsonString == null) {
            throw IllegalArgumentException("The backup file does not contain a valid metadata descriptor block")
        }

        val json = JSONObject(metadataJsonString!!)
        val backupVersion = json.optInt("backup_version", 1)
        val appVersionCode = json.optInt("app_version_code", 0)
        val animeCatalogDbVersion = json.optLong("anime_catalog_db_version", 0L)
        val timestamp = json.optLong("timestamp", 0L)
        
        val includedModules = json.optJSONObject("included_modules")
        val includeSettings = includedModules?.optBoolean("settings") ?: false
        val includeTracking = includedModules?.optBoolean("tracking_lists") ?: false
        val includeCollections = includedModules?.optBoolean("custom_collections") ?: false

        val metadata = BackupMetadata(
            backupVersion = backupVersion,
            appVersionCode = appVersionCode,
            animeCatalogDbVersion = animeCatalogDbVersion,
            timestamp = timestamp,
            includeSettings = includeSettings,
            includeTracking = includeTracking,
            includeCollections = includeCollections
        )

        // Strict version validation check
        val currentAppVersion = getAppVersionCode()
        val currentCatalogVersion = settingsProvider.getCatalogVersion()

        if (currentAppVersion < metadata.appVersionCode || currentCatalogVersion < metadata.animeCatalogDbVersion) {
            throw IncompatibleBackupException(
                "Incompatible Backup: This backup was created on a newer version of AniForge or with a newer Anime Catalog. Please update your application and the global anime catalog database before restoring."
            )
        }

        metadata
    }

    /**
     * Performs a granular database merge/overwrite, closing current connections, copying tables,
     * recalculating stats, and clearing temporary caches.
     */
    suspend fun restoreBackup(
        uri: Uri,
        restoreSettings: Boolean,
        restoreTracking: Boolean,
        restoreCollections: Boolean
    ): Result<Unit> = kotlin.runCatching {
        AppLogger.i("BackupManager", "Restoring backup. Settings: $restoreSettings, Tracking: $restoreTracking, Collections: $restoreCollections")

        // 1. Extract database from the zip file to a temp location
        val tempDbFile = File(context.cacheDir, "temp_restore.db")
        if (tempDbFile.exists()) tempDbFile.delete()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zipIn ->
                var entry = zipIn.nextEntry
                var foundDb = false
                while (entry != null) {
                    if (entry.name == "user_data.db") {
                        tempDbFile.outputStream().use { output ->
                            zipIn.copyTo(output)
                        }
                        foundDb = true
                        break
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
                if (!foundDb) {
                    throw IllegalArgumentException("The backup file does not contain user_data.db")
                }
            }
        } ?: throw IllegalStateException("Could not open source input stream")

        // 2. Close active Room/SQLite connections
        userDatabase.close()
        AppLogger.i("BackupManager", "Active Room database closed for restoration.")

        // 3. Attach and merge selected tables
        val activeDbFile = context.getDatabasePath("user_data.db")
        SQLiteDatabase.openDatabase(activeDbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { activeDb ->
            activeDb.beginTransaction()
            try {
                activeDb.execSQL("ATTACH DATABASE '${tempDbFile.absolutePath}' AS temp_db;")

                if (restoreSettings) {
                    copyTableIfExists(activeDb, "user_settings")
                    copyTableIfExists(activeDb, "widget_config")
                }

                if (restoreTracking) {
                    copyTableIfExists(activeDb, "user_tracking")
                    copyTableIfExists(activeDb, "user_stats")
                    copyTableIfExists(activeDb, "pending_imports")
                }

                if (restoreCollections) {
                    copyTableIfExists(activeDb, "collections")
                    copyTableIfExists(activeDb, "collection_anime_cross_ref")
                }

                activeDb.setTransactionSuccessful()
                AppLogger.i("BackupManager", "Granular SQL merge completed successfully.")
            } finally {
                activeDb.endTransaction()
                try {
                    activeDb.execSQL("DETACH DATABASE temp_db;")
                } catch (_: Exception) {}
            }
        }

        // 4. Force reopening of Room database
        userDatabase.openHelper.writableDatabase
        AppLogger.i("BackupManager", "Room database connection re-established.")

        // 5. Clean temporary files
        cleanTempFiles()
    }

    private fun copyTableIfExists(activeDb: SQLiteDatabase, tableName: String) {
        var tableExists = false
        try {
            activeDb.rawQuery("SELECT name FROM temp_db.sqlite_master WHERE type='table' AND name=?;", arrayOf(tableName)).use { cursor ->
                if (cursor.moveToFirst()) {
                    tableExists = true
                }
            }
        } catch (e: Exception) {
            AppLogger.e("BackupManager", "Error verifying table existence: $tableName", e)
        }

        if (tableExists) {
            activeDb.execSQL("DELETE FROM $tableName;")
            activeDb.execSQL("INSERT INTO $tableName SELECT * FROM temp_db.$tableName;")
            AppLogger.i("BackupManager", "Successfully merged table: $tableName")
        } else {
            AppLogger.w("BackupManager", "Table '$tableName' is not present in the backup database. Skipping.")
        }
    }

    fun cleanTempFiles() {
        try {
            val cacheFiles = context.cacheDir.listFiles()
            cacheFiles?.forEach { file ->
                if (file.name.contains("temp_backup") || file.name.contains("temp_restore")) {
                    file.delete()
                }
            }
            AppLogger.i("BackupManager", "Temporary backup/restore cache files cleaned.")
        } catch (e: Exception) {
            AppLogger.e("BackupManager", "Failed to delete temporary files", e)
        }
    }
}
