package moe.GetTheNya.AniForge.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CatalogDatabaseProviderTest {

    private lateinit var context: Context
    private lateinit var settingsProvider: SettingsProvider
    private lateinit var databaseProvider: CatalogDatabaseProvider
    
    private lateinit var fileA: File
    private lateinit var fileB: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val fakeUserSettingDao = object : moe.GetTheNya.AniForge.core.database.dao.UserSettingDao {
            override fun observeSetting(key: String): kotlinx.coroutines.flow.Flow<String?> =
                kotlinx.coroutines.flow.flowOf(null)
            override suspend fun getSettingSync(key: String): String? = null
            override suspend fun insertOrUpdate(setting: moe.GetTheNya.AniForge.core.database.entity.UserSettingEntity) {}
        }
        val fakeSettingsRepository = moe.GetTheNya.AniForge.core.database.repository.SettingsRepository(fakeUserSettingDao)
        settingsProvider = SettingsProvider(context, fakeSettingsRepository)
        
        // Reset settings
        settingsProvider.setActiveCatalog("catalog_a.db", 1L)

        fileA = context.getDatabasePath("catalog_a.db")
        fileB = context.getDatabasePath("catalog_b.db")

        // Ensure clean state
        deleteDbFiles()

        // Create catalog_a database and insert dummy record
        val openHelperA = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("catalog_a.db")
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS anime (anilist_id INTEGER PRIMARY KEY, title_romaji TEXT, is_adult INTEGER, has_uk_translation INTEGER, updated_at INTEGER);")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val dbA = openHelperA.writableDatabase
        dbA.execSQL(
            "INSERT INTO anime (anilist_id, title_romaji, is_adult, has_uk_translation, updated_at) " +
            "VALUES (101, 'Anime Slot A', 0, 1, ${System.currentTimeMillis()});"
        )
        openHelperA.close()

        // Create catalog_b database and insert dummy record
        val openHelperB = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("catalog_b.db")
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS anime (anilist_id INTEGER PRIMARY KEY, title_romaji TEXT, is_adult INTEGER, has_uk_translation INTEGER, updated_at INTEGER);")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val dbB = openHelperB.writableDatabase
        dbB.execSQL(
            "INSERT INTO anime (anilist_id, title_romaji, is_adult, has_uk_translation, updated_at) " +
            "VALUES (202, 'Anime Slot B', 0, 1, ${System.currentTimeMillis()});"
        )
        openHelperB.close()

        databaseProvider = CatalogDatabaseProvider(context, settingsProvider)
    }

    @After
    fun tearDown() {
        deleteDbFiles()
    }

    private fun deleteDbFiles() {
        if (fileA.exists()) fileA.delete()
        if (fileB.exists()) fileB.delete()
        
        val journalA = File(fileA.absolutePath + "-journal")
        if (journalA.exists()) journalA.delete()
        
        val journalB = File(fileB.absolutePath + "-journal")
        if (journalB.exists()) journalB.delete()
        
        val walA = File(fileA.absolutePath + "-wal")
        if (walA.exists()) walA.delete()
        
        val walB = File(fileB.absolutePath + "-wal")
        if (walB.exists()) walB.delete()
    }

    @Test
    fun testDynamicHotSwapRouting() = runBlocking {
        // 1. Initial State: Catalog A is active
        assertEquals("catalog_a.db", settingsProvider.getActiveCatalogFileName())
        
        val db1 = databaseProvider.getDatabase()
        var title1: String? = null
        db1.query("SELECT title_romaji FROM anime WHERE anilist_id = 101").use { cursor ->
            if (cursor.moveToNext()) {
                title1 = cursor.getString(0)
            }
        }
        assertNotNull(title1)
        assertEquals("Anime Slot A", title1)

        // 2. Perform Hot Swap to Standby (Slot B)
        val swapSuccess = databaseProvider.hotSwapToStandby(2L)
        assertTrue(swapSuccess)

        // 3. Verify Active Slot in Settings changed to Catalog B
        assertEquals("catalog_b.db", settingsProvider.getActiveCatalogFileName())
        assertEquals(2L, settingsProvider.getCatalogVersion())

        // 4. Verify Provider fetches from Database B now
        val db2 = databaseProvider.getDatabase()
        
        // Assert we get the record from database B
        var title2: String? = null
        db2.query("SELECT title_romaji FROM anime WHERE anilist_id = 202").use { cursor ->
            if (cursor.moveToNext()) {
                title2 = cursor.getString(0)
            }
        }
        assertNotNull(title2)
        assertEquals("Anime Slot B", title2)

        // Assert record from database A is not visible in database B connection
        var title1AfterSwap: String? = null
        db2.query("SELECT title_romaji FROM anime WHERE anilist_id = 101").use { cursor ->
            if (cursor.moveToNext()) {
                title1AfterSwap = cursor.getString(0)
            }
        }
        assertNull(title1AfterSwap)
    }
}
