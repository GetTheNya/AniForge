package moe.GetTheNya.AniForge.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import moe.GetTheNya.AniForge.core.database.entity.AnimeEntity
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
        settingsProvider = SettingsProvider(context)
        
        // Reset settings
        settingsProvider.setActiveCatalog("catalog_a.db", 1L)

        fileA = context.getDatabasePath("catalog_a.db")
        fileB = context.getDatabasePath("catalog_b.db")

        // Ensure clean state
        deleteDbFiles()

        // Create catalog_a database and insert dummy record
        val dbA = Room.databaseBuilder(context, CatalogDatabase::class.java, "catalog_a.db").build()
        runBlocking {
            dbA.animeDao().getAnimeById(1L) // creates the file
            // Let's insert a custom record directly to verify content
            dbA.openHelper.writableDatabase.execSQL(
                "INSERT INTO anime (anilist_id, title_romaji, is_adult, has_uk_translation, updated_at) " +
                "VALUES (101, 'Anime Slot A', 0, 1, ${System.currentTimeMillis()});"
            )
        }
        dbA.close()

        // Create catalog_b database and insert dummy record
        val dbB = Room.databaseBuilder(context, CatalogDatabase::class.java, "catalog_b.db").build()
        runBlocking {
            dbB.animeDao().getAnimeById(1L) // creates the file
            dbB.openHelper.writableDatabase.execSQL(
                "INSERT INTO anime (anilist_id, title_romaji, is_adult, has_uk_translation, updated_at) " +
                "VALUES (202, 'Anime Slot B', 0, 1, ${System.currentTimeMillis()});"
            )
        }
        dbB.close()

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
        val anime1 = db1.animeDao().getAnimeById(101L)
        assertNotNull(anime1)
        assertEquals("Anime Slot A", anime1?.titleRomaji)

        // 2. Perform Hot Swap to Standby (Slot B)
        val swapSuccess = databaseProvider.hotSwapToStandby(2L)
        assertTrue(swapSuccess)

        // 3. Verify Active Slot in Settings changed to Catalog B
        assertEquals("catalog_b.db", settingsProvider.getActiveCatalogFileName())
        assertEquals(2L, settingsProvider.getCatalogVersion())

        // 4. Verify Provider fetches from Database B now
        val db2 = databaseProvider.getDatabase()
        
        // Assert we get the record from database B
        val anime2 = db2.animeDao().getAnimeById(202L)
        assertNotNull(anime2)
        assertEquals("Anime Slot B", anime2?.titleRomaji)

        // Assert record from database A is not visible in database B connection
        val anime1AfterSwap = db2.animeDao().getAnimeById(101L)
        assertNull(anime1AfterSwap)
    }
}
