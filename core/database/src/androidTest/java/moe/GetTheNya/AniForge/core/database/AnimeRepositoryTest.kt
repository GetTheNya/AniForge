package moe.GetTheNya.AniForge.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
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
    private lateinit var openHelper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        settingsProvider = SettingsProvider(context)
        
        // Target catalog_test.db
        settingsProvider.setActiveCatalog("catalog_test.db", 1L)

        val testFile = context.getDatabasePath("catalog_test.db")
        if (testFile.exists()) testFile.delete()

        // Build database instance
        openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("catalog_test.db")
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS anime (" +
                            "anilist_id INTEGER PRIMARY KEY, " +
                            "mal_id INTEGER, " +
                            "title_uk TEXT, " +
                            "title_romaji TEXT, " +
                            "title_en TEXT, " +
                            "description_uk TEXT, " +
                            "description_en TEXT, " +
                            "format TEXT, " +
                            "status TEXT, " +
                            "episodes INTEGER, " +
                            "duration INTEGER, " +
                            "season_year INTEGER, " +
                            "season TEXT, " +
                            "is_adult INTEGER, " +
                            "score_mal REAL, " +
                            "cover_extra_large TEXT, " +
                            "cover_large TEXT, " +
                            "cover_medium TEXT, " +
                            "cover_color TEXT, " +
                            "banner_image TEXT, " +
                            "has_uk_translation INTEGER, " +
                            "updated_at INTEGER" +
                            ");"
                        )
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        db = openHelper.writableDatabase

        // Populate tables
        runBlocking {
            // 1. Create relational lookup tables (SQLite holds them)
            db.execSQL("CREATE TABLE IF NOT EXISTS anime_genres (anilist_id INTEGER, genre_slug TEXT);")
            db.execSQL("CREATE TABLE IF NOT EXISTS anime_studios (anilist_id INTEGER, studio_id INTEGER);")
            
            // Diagnostics: Print SQLite version and compile options
            try {
                db.query(androidx.sqlite.db.SimpleSQLiteQuery("SELECT sqlite_version()")).use { cursor ->
                    if (cursor.moveToFirst()) {
                        android.util.Log.d("SQL_DIAG", "SQLite Version: " + cursor.getString(0))
                    }
                }
                db.query(androidx.sqlite.db.SimpleSQLiteQuery("PRAGMA compile_options")).use { cursor ->
                    val options = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        options.add(cursor.getString(0))
                    }
                    android.util.Log.d("SQL_DIAG", "Compile Options: " + options.joinToString(", "))
                }
            } catch (e: Exception) {
                android.util.Log.e("SQL_DIAG", "Failed to query SQLite version/options", e)
            }

            try {
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS anime_search USING fts5(title_uk, title_romaji, synonyms_flat, tokenize='unicode61 remove_diacritics 2');")
                android.util.Log.d("SQL_DIAG", "Successfully created FTS5 table with tokenize options")
            } catch (e: Exception) {
                android.util.Log.e("SQL_DIAG", "Failed to create FTS5 table with tokenize options: " + e.message)
                try {
                    db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS anime_search USING fts5(title_uk, title_romaji, synonyms_flat);")
                    android.util.Log.d("SQL_DIAG", "Successfully created FTS5 table without tokenize options")
                } catch (e2: Exception) {
                    android.util.Log.e("SQL_DIAG", "Failed to create FTS5 table without tokenize options: " + e2.message)
                    try {
                        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS anime_search USING fts4(title_uk, title_romaji, synonyms_flat);")
                        android.util.Log.d("SQL_DIAG", "Successfully created FTS4 table instead of FTS5")
                    } catch (e3: Exception) {
                        android.util.Log.e("SQL_DIAG", "Failed to create FTS4 table: " + e3.message)
                    }
                }
            }

            // 2. Insert Anime Records
            db.execSQL(
                "INSERT INTO anime (anilist_id, title_romaji, score_mal, season_year, is_adult, has_uk_translation, updated_at) " +
                "VALUES (1, 'Shingeki no Kyojin', 9.1, 2013, 0, 1, 1000);"
            )
            db.execSQL(
                "INSERT INTO anime (anilist_id, title_romaji, score_mal, season_year, is_adult, has_uk_translation, updated_at) " +
                "VALUES (2, 'Boku no Hero Academia', 8.2, 2016, 0, 1, 1001);"
            )
            db.execSQL(
                "INSERT INTO anime (anilist_id, title_romaji, score_mal, season_year, is_adult, has_uk_translation, updated_at) " +
                "VALUES (3, 'Sword Art Online', 7.2, 2012, 0, 1, 1002);"
            )

            // 3. Insert FTS Search Records
            db.execSQL(
                "INSERT INTO anime_search (rowid, title_uk, title_romaji, synonyms_flat) " +
                "VALUES (1, 'Атака титанів', 'Shingeki no Kyojin', 'Attack on Titan SnK');"
            )
            db.execSQL(
                "INSERT INTO anime_search (rowid, title_uk, title_romaji, synonyms_flat) " +
                "VALUES (2, 'Моя геройська академія', 'Boku no Hero Academia', 'My Hero Academia MHA');"
            )
            db.execSQL(
                "INSERT INTO anime_search (rowid, title_uk, title_romaji, synonyms_flat) " +
                "VALUES (3, 'Майстри меча онлайн', 'Sword Art Online', 'SAO');"
            )

            // 4. Insert Genre Records
            db.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (1, 'action');")
            db.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (1, 'drama');")
            db.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (2, 'action');")
            db.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (2, 'comedy');")
            db.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (3, 'action');")
            db.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (3, 'fantasy');")

            // 5. Insert Studio Records
            db.execSQL("INSERT INTO anime_studios (anilist_id, studio_id) VALUES (1, 10);") // Wit Studio
            db.execSQL("INSERT INTO anime_studios (anilist_id, studio_id) VALUES (2, 20);") // Bones
            db.execSQL("INSERT INTO anime_studios (anilist_id, studio_id) VALUES (3, 30);") // A-1 Pictures
        }

        // Mock databaseProvider to return our pre-populated DB
        databaseProvider = object : CatalogDatabaseProvider(context, settingsProvider) {
            override suspend fun getDatabase(): SupportSQLiteDatabase = db
        }

        repository = AnimeRepository(databaseProvider)
    }

    @After
    fun tearDown() {
        openHelper.close()
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
