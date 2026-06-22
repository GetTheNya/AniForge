package moe.GetTheNya.AniForge.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.database.util.AppLogger
import io.requery.android.database.sqlite.SQLiteDatabase
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class CatalogDatabaseProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsProvider: SettingsProvider
) {
    private val mutex = Mutex()
    private var currentInstance: SupportSQLiteDatabase? = null
    private var currentHelper: SupportSQLiteOpenHelper? = null

    private val _swapSignal = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    
    /**
     * Emits a signal whenever a database swap occurs, notifying downstream flows to re-subscribe.
     */
    val swapSignal: SharedFlow<Unit> = _swapSignal.asSharedFlow()

    /**
     * Thread-safe access to the currently active SupportSQLiteDatabase instance.
     */
    open suspend fun getDatabase(): SupportSQLiteDatabase = mutex.withLock {
        if (currentInstance == null) {
            val activeFileName = settingsProvider.getActiveCatalogFileName()
            val (helper, db) = buildDatabase(activeFileName)
            currentHelper = helper
            currentInstance = db
        }
        return currentInstance!!
    }

    /**
     * Swaps active catalog database connections dynamically. 
     * Closes the active database connection, updates SettingsProvider, re-instantiates SQLite
     * open helper pointing to the standby slot, and emits a swap signal to reload active UI flows.
     */
    suspend fun hotSwapToStandby(newVersion: Long, generatedAt: String? = null): Boolean = mutex.withLock {
        val currentActive = settingsProvider.getActiveCatalogFileName()
        val standbyFileName = settingsProvider.getStandbyCatalogFileName()
        
        // 1. Double check the standby file exists
        val standbyFile = context.getDatabasePath(standbyFileName)
        if (!standbyFile.exists()) {
            AppLogger.e("CatalogDatabaseProvider", "Standby database file does not exist: $standbyFileName")
            return false
        }

        try {
            // 2. Close active instance connection cleanly
            AppLogger.i("CatalogDatabaseProvider", "Closing active catalog connection: $currentActive")
            currentInstance?.close()
            currentHelper?.close()
            currentInstance = null
            currentHelper = null

            // 3. Commit slot changes to SettingsProvider
            settingsProvider.setActiveCatalog(standbyFileName, newVersion, generatedAt)

            // 4. Rebuild the instance pointing to the new active file
            AppLogger.i("CatalogDatabaseProvider", "Opening connection to new active catalog: $standbyFileName")
            val (helper, db) = buildDatabase(standbyFileName)
            currentHelper = helper
            currentInstance = db

            // 5. Signal reloading
            AppLogger.i("CatalogDatabaseProvider", "Database swap complete. Broadcasting reload signal.")
            _swapSignal.emit(Unit)
            true
        } catch (e: Exception) {
            AppLogger.e("CatalogDatabaseProvider", "Error swapping database to slot $standbyFileName, rolling back to $currentActive", e)
            // Fallback recovery: attempt to restore previous connection if swap crashed
            try {
                currentInstance?.close()
                currentHelper?.close()
            } catch (_: Exception) {}
            val (helper, db) = buildDatabase(currentActive)
            currentHelper = helper
            currentInstance = db
            false
        }
    }

    private fun buildDatabase(fileName: String): Pair<SupportSQLiteOpenHelper, SupportSQLiteDatabase> {
        val dbFile = context.getDatabasePath(fileName)
        if (dbFile.exists()) {
            ensureFtsSupport(dbFile)
        }
        
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(fileName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Pre-populated catalog, no creation needed
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // Pre-populated read-only catalog, no upgrades needed via open helper callbacks
                }
            })
            .build()
            
        val helper = RequerySQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase
        return Pair(helper, db)
    }

    private fun ensureFtsSupport(dbFile: File) {
        try {
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            ).use { db ->
                // Check if FTS5 is supported
                var fts5Supported = false
                try {
                    db.rawQuery("SELECT name FROM pragma_module_list();", null).use { cursor ->
                        while (cursor.moveToNext()) {
                            val moduleName = cursor.getString(0)
                            if (moduleName.equals("fts5", ignoreCase = true)) {
                                fts5Supported = true
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e("CatalogDatabaseProvider", "Failed to check FTS5", e)
                } finally {
                    AppLogger.i("CatalogDatabaseProvider", "FTS5 check complete, result = $fts5Supported")
                }

                if (!fts5Supported) {
                    val tablesToRecreate = listOf(
                        "anime_search" to Pair(
                            "CREATE VIRTUAL TABLE anime_search USING fts4(title_uk, title_romaji, title_en, description_uk, description_en, synonyms_flat);",
                            "INSERT INTO anime_search (rowid, title_uk, title_romaji, title_en, description_uk, description_en, synonyms_flat) " +
                            "SELECT anilist_id, title_uk, title_romaji, title_en, description_uk, description_en, synonyms_flat FROM anime;"
                        ),
                        "franchises_search" to Pair(
                            "CREATE VIRTUAL TABLE franchises_search USING fts4(name_uk, name_en);",
                            "INSERT INTO franchises_search (rowid, name_uk, name_en) " +
                            "SELECT franchise_id, name_uk, name_en FROM franchises;"
                        ),
                        "staff_search" to Pair(
                            "CREATE VIRTUAL TABLE staff_search USING fts4(full_name);",
                            "INSERT INTO staff_search (rowid, full_name) " +
                            "SELECT staff_id, full_name FROM staff;"
                        ),
                        "studios_search" to Pair(
                            "CREATE VIRTUAL TABLE studios_search USING fts4(name);",
                            "INSERT INTO studios_search (rowid, name) " +
                            "SELECT studio_id, name FROM studios;"
                        )
                    )

                    for ((tableName, queries) in tablesToRecreate) {
                        var isFts5Table = false
                        try {
                            db.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='$tableName'", null).use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val sql = cursor.getString(0) ?: ""
                                    if (sql.contains("fts5", ignoreCase = true)) {
                                        isFts5Table = true
                                    }
                                }
                            }
                        } catch (_: Exception) {}

                        if (isFts5Table) {
                            AppLogger.w("CatalogDatabaseProvider", "FTS5 not supported on device. Recreating $tableName as FTS4.")
                            db.beginTransaction()
                            try {
                                db.execSQL("DROP TABLE IF EXISTS $tableName;")
                                db.execSQL(queries.first)
                                db.execSQL(queries.second)
                                db.setTransactionSuccessful()
                            } catch (e: Exception) {
                                AppLogger.e("CatalogDatabaseProvider", "Failed to recreate FTS4 table $tableName", e)
                            } finally {
                                db.endTransaction()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("CatalogDatabaseProvider", "Failed to open database for FTS check", e)
        }
    }
}
