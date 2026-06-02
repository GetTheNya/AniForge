package moe.GetTheNya.AniForge.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.model.SearchFilterQuery
import moe.GetTheNya.AniForge.core.model.SortOption
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AnimeRepositoryTest {

    private lateinit var context: Context
    private lateinit var settingsProvider: SettingsProvider
    private lateinit var databaseProvider: CatalogDatabaseProvider
    private lateinit var repository: AnimeRepository
    private lateinit var db: CatalogDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        settingsProvider = SettingsProvider(context)
        
        // Target catalog_test.db
        settingsProvider.setActiveCatalog("catalog_test.db", 1L)

        val testFile = context.getDatabasePath("catalog_test.db")
        if (testFile.exists()) testFile.delete()

        // Build database instance
        db = Room.databaseBuilder(context, CatalogDatabase::class.java, "catalog_test.db").build()

        // Populate tables
        runBlocking {
            // Setup tables structure
            db.animeDao().getAnimeById(1L)
            
            val dbWrite = db.openHelper.writableDatabase

            // 1. Create relational lookup tables (Room doesn't manage their entities, but SQLite holds them)
            dbWrite.execSQL("CREATE TABLE IF NOT EXISTS anime_genres (anilist_id INTEGER, genre_slug TEXT);")
            dbWrite.execSQL("CREATE TABLE IF NOT EXISTS anime_studios (anilist_id INTEGER, studio_id INTEGER);")

            // 2. Insert Anime Records
            dbWrite.execSQL(
                "INSERT INTO anime (anilist_id, title_romaji, score_mal, season_year, is_adult, has_uk_translation, updated_at) " +
                "VALUES (1, 'Shingeki no Kyojin', 9.1, 2013, 0, 1, 1000);"
            )
            dbWrite.execSQL(
                "INSERT INTO anime (anilist_id, title_romaji, score_mal, season_year, is_adult, has_uk_translation, updated_at) " +
                "VALUES (2, 'Boku no Hero Academia', 8.2, 2016, 0, 1, 1001);"
            )
            dbWrite.execSQL(
                "INSERT INTO anime (anilist_id, title_romaji, score_mal, season_year, is_adult, has_uk_translation, updated_at) " +
                "VALUES (3, 'Sword Art Online', 7.2, 2012, 0, 1, 1002);"
            )

            // 3. Insert FTS Search Records
            dbWrite.execSQL(
                "INSERT INTO anime_search (rowid, title_uk, title_romaji, synonyms_flat) " +
                "VALUES (1, 'Атака титанів', 'Shingeki no Kyojin', 'Attack on Titan SnK');"
            )
            dbWrite.execSQL(
                "INSERT INTO anime_search (rowid, title_uk, title_romaji, synonyms_flat) " +
                "VALUES (2, 'Моя геройська академія', 'Boku no Hero Academia', 'My Hero Academia MHA');"
            )
            dbWrite.execSQL(
                "INSERT INTO anime_search (rowid, title_uk, title_romaji, synonyms_flat) " +
                "VALUES (3, 'Майстри меча онлайн', 'Sword Art Online', 'SAO');"
            )

            // 4. Insert Genre Records
            dbWrite.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (1, 'action');")
            dbWrite.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (1, 'drama');")
            dbWrite.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (2, 'action');")
            dbWrite.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (2, 'comedy');")
            dbWrite.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (3, 'action');")
            dbWrite.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (3, 'fantasy');")

            // 5. Insert Studio Records
            dbWrite.execSQL("INSERT INTO anime_studios (anilist_id, studio_id) VALUES (1, 10);") // Wit Studio
            dbWrite.execSQL("INSERT INTO anime_studios (anilist_id, studio_id) VALUES (2, 20);") // Bones
            dbWrite.execSQL("INSERT INTO anime_studios (anilist_id, studio_id) VALUES (3, 30);") // A-1 Pictures
        }

        // Mock databaseProvider to return our pre-populated DB
        databaseProvider = object : CatalogDatabaseProvider(context, settingsProvider) {
            override suspend fun getDatabase(): CatalogDatabase = db
        }

        repository = AnimeRepository(databaseProvider)
    }

    @After
    fun tearDown() {
        db.close()
        val testFile = context.getDatabasePath("catalog_test.db")
        if (testFile.exists()) testFile.delete()
    }

    @Test
    fun testFullTextSearchMatching() = runBlocking {
        // FTS match on title_romaji partial matching
        val results1 = repository.queryAnime(SearchFilterQuery(textQuery = "shingeki"))
        assertEquals(1, results1.size)
        assertEquals(1L, results1[0].anilistId)

        // FTS match on synonyms
        val results2 = repository.queryAnime(SearchFilterQuery(textQuery = "titan"))
        assertEquals(1, results2.size)
        assertEquals(1L, results2[0].anilistId)

        // FTS match on ukrainian title
        val results3 = repository.queryAnime(SearchFilterQuery(textQuery = "академія"))
        assertEquals(1, results3.size)
        assertEquals(2L, results3[0].anilistId)
    }

    @Test
    fun testMultiLayeredGenreFiltering() = runBlocking {
        // Single genre action matches all three
        val results1 = repository.queryAnime(SearchFilterQuery(genres = listOf("action")))
        assertEquals(3, results1.size)

        // Intersecting action + drama matches only Titan
        val results2 = repository.queryAnime(SearchFilterQuery(genres = listOf("action", "drama")))
        assertEquals(1, results2.size)
        assertEquals(1L, results2[0].anilistId)

        // Intersecting action + comedy matches Hero Academia
        val results3 = repository.queryAnime(SearchFilterQuery(genres = listOf("action", "comedy")))
        assertEquals(1, results3.size)
        assertEquals(2L, results3[0].anilistId)
    }

    @Test
    fun testSorting() = runBlocking {
        // Sort by score
        val results1 = repository.queryAnime(SearchFilterQuery(sortBy = SortOption.SCORE))
        assertEquals(3, results1.size)
        assertEquals(1L, results1[0].anilistId) // Score 9.1
        assertEquals(2L, results1[1].anilistId) // Score 8.2
        assertEquals(3L, results1[2].anilistId) // Score 7.2

        // Sort by year desc
        val results2 = repository.queryAnime(SearchFilterQuery(sortBy = SortOption.YEAR_DESC))
        assertEquals(3, results2.size)
        assertEquals(2L, results2[0].anilistId) // Year 2016
        assertEquals(1L, results2[1].anilistId) // Year 2013
        assertEquals(3L, results2[2].anilistId) // Year 2012
    }
}
