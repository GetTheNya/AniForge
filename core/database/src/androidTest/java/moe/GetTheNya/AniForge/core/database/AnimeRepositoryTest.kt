package moe.GetTheNya.AniForge.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.model.SearchFilterQuery
import moe.GetTheNya.AniForge.core.model.SortOption
import moe.GetTheNya.AniForge.core.model.AnimeStaff
import moe.GetTheNya.AniForge.core.model.Ranking
import moe.GetTheNya.AniForge.core.model.Franchise
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
        val fakeUserSettingDao = object : moe.GetTheNya.AniForge.core.database.dao.UserSettingDao {
            private val settings = mutableMapOf<String, kotlinx.coroutines.flow.MutableStateFlow<String?>>()
            
            private fun getOrCreateFlow(key: String): kotlinx.coroutines.flow.MutableStateFlow<String?> {
                return settings.getOrPut(key) { kotlinx.coroutines.flow.MutableStateFlow(null) }
            }

            override fun observeSetting(key: String): kotlinx.coroutines.flow.Flow<String?> =
                getOrCreateFlow(key)

            override suspend fun getSettingSync(key: String): String? = 
                getOrCreateFlow(key).value

            override suspend fun insertOrUpdate(setting: moe.GetTheNya.AniForge.core.database.entity.UserSettingEntity) {
                getOrCreateFlow(setting.key).value = setting.value
            }
        }
        val fakeSettingsRepository = moe.GetTheNya.AniForge.core.database.repository.SettingsRepository(fakeUserSettingDao)
        settingsProvider = SettingsProvider(context, fakeSettingsRepository)
        
        // Target catalog_test.db
        settingsProvider.setActiveCatalog("catalog_test.db", 1L)

        val testFile = context.getDatabasePath("catalog_test.db")
        if (testFile.exists()) testFile.delete()

        // Build database instance
        openHelper = RequerySQLiteOpenHelperFactory().create(
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
                            "updated_at INTEGER, " +
                            "airing_at INTEGER, " +
                            "airing_episode INTEGER, " +
                            "trailer_id TEXT, " +
                            "trailer_site TEXT, " +
                            "trailer_thumbnail TEXT, " +
                            "start_date_year INTEGER, " +
                            "start_date_month INTEGER, " +
                            "start_date_day INTEGER, " +
                            "popularity INTEGER, " +
                            "source TEXT, " +
                            "synonyms_flat TEXT" +
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
            db.execSQL("CREATE TABLE IF NOT EXISTS studios (studio_id INTEGER PRIMARY KEY, name TEXT NOT NULL);")
            db.execSQL("CREATE TABLE IF NOT EXISTS staff (staff_id INTEGER PRIMARY KEY, full_name TEXT NOT NULL, image_large TEXT);")
            db.execSQL("CREATE TABLE IF NOT EXISTS anime_staff (anilist_id INTEGER NOT NULL, staff_id INTEGER NOT NULL, role TEXT NOT NULL, PRIMARY KEY (anilist_id, staff_id, role), FOREIGN KEY (anilist_id) REFERENCES anime(anilist_id) ON DELETE CASCADE, FOREIGN KEY (staff_id) REFERENCES staff(staff_id) ON DELETE CASCADE);")
            db.execSQL("CREATE TABLE IF NOT EXISTS rankings (id INTEGER PRIMARY KEY AUTOINCREMENT, anilist_id INTEGER NOT NULL, rank INTEGER NOT NULL, context TEXT NOT NULL, all_time INTEGER, season TEXT, year INTEGER, UNIQUE(anilist_id, context), FOREIGN KEY (anilist_id) REFERENCES anime(anilist_id) ON DELETE CASCADE);")
            db.execSQL("CREATE TABLE IF NOT EXISTS anime_recommendations (source_anilist_id INTEGER NOT NULL, recommended_anilist_id INTEGER NOT NULL, PRIMARY KEY (source_anilist_id, recommended_anilist_id), FOREIGN KEY (source_anilist_id) REFERENCES anime(anilist_id) ON DELETE CASCADE);")
            db.execSQL("CREATE TABLE IF NOT EXISTS franchises (franchise_id INTEGER PRIMARY KEY AUTOINCREMENT, main_anilist_id INTEGER NOT NULL, name_en TEXT, name_uk TEXT, FOREIGN KEY (main_anilist_id) REFERENCES anime(anilist_id) ON DELETE CASCADE);")
            db.execSQL("CREATE TABLE IF NOT EXISTS anime_franchises (anilist_id INTEGER NOT NULL, franchise_id INTEGER NOT NULL, PRIMARY KEY (anilist_id), FOREIGN KEY (anilist_id) REFERENCES anime(anilist_id) ON DELETE CASCADE, FOREIGN KEY (franchise_id) REFERENCES franchises(franchise_id) ON DELETE CASCADE);")
            
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

            val ftsTables = listOf(
                "anime_search" to listOf("title_uk", "title_romaji", "title_en", "description_uk", "description_en", "synonyms_flat"),
                "franchises_search" to listOf("name_uk", "name_en"),
                "staff_search" to listOf("full_name"),
                "studios_search" to listOf("name")
            )

            for ((tableName, cols) in ftsTables) {
                val colList = cols.joinToString(", ")
                try {
                    db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS $tableName USING fts5($colList, tokenize='unicode61 remove_diacritics 2');")
                    android.util.Log.d("SQL_DIAG", "Successfully created FTS5 table $tableName with tokenize options")
                } catch (e: Exception) {
                    android.util.Log.e("SQL_DIAG", "Failed to create FTS5 table $tableName with tokenize options: " + e.message)
                    try {
                        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS $tableName USING fts5($colList);")
                        android.util.Log.d("SQL_DIAG", "Successfully created FTS5 table $tableName without tokenize options")
                    } catch (e2: Exception) {
                        android.util.Log.e("SQL_DIAG", "Failed to create FTS5 table $tableName without tokenize options: " + e2.message)
                        try {
                            db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS $tableName USING fts4($colList);")
                            android.util.Log.d("SQL_DIAG", "Successfully created FTS4 table $tableName instead of FTS5")
                        } catch (e3: Exception) {
                            android.util.Log.e("SQL_DIAG", "Failed to create FTS4 table $tableName: " + e3.message)
                        }
                    }
                }
            }

            // 2. Insert Anime Records
            db.execSQL(
                "INSERT INTO anime (anilist_id, title_romaji, score_mal, season_year, is_adult, has_uk_translation, updated_at, airing_at, airing_episode, trailer_id, trailer_site, trailer_thumbnail, start_date_year, start_date_month, start_date_day, popularity, source, synonyms_flat) " +
                "VALUES (1, 'Shingeki no Kyojin', 9.1, 2013, 0, 1, 1000, 1622976000, 75, 'AdTzUx76Hjg', 'youtube', 'https://img.youtube.com/vi/AdTzUx76Hjg/0.jpg', 2013, 4, 7, 1250000, 'MANGA', 'Attack on Titan SnK');"
            )
            db.execSQL(
                "INSERT INTO anime (anilist_id, title_romaji, score_mal, season_year, is_adult, has_uk_translation, updated_at, synonyms_flat) " +
                "VALUES (2, 'Boku no Hero Academia', 8.2, 2016, 0, 1, 1001, 'My Hero Academia MHA');"
            )
            db.execSQL(
                "INSERT INTO anime (anilist_id, title_romaji, score_mal, season_year, is_adult, has_uk_translation, updated_at, synonyms_flat) " +
                "VALUES (3, 'Sword Art Online', 7.2, 2012, 0, 1, 1002, 'SAO');"
            )

            // 3. Insert FTS Search Records
            db.execSQL(
                "INSERT INTO anime_search (rowid, title_uk, title_romaji, title_en, description_uk, description_en, synonyms_flat) " +
                "VALUES (1, 'Атака титанів', 'Shingeki no Kyojin', 'Attack on Titan', 'Опис Атаки титанів', 'Description of Attack on Titan', 'Attack on Titan SnK');"
            )
            db.execSQL(
                "INSERT INTO anime_search (rowid, title_uk, title_romaji, title_en, description_uk, description_en, synonyms_flat) " +
                "VALUES (2, 'Моя геройська академія', 'Boku no Hero Academia', 'My Hero Academia', 'Опис Моя геройська академія', 'Description of My Hero Academia', 'My Hero Academia MHA');"
            )
            db.execSQL(
                "INSERT INTO anime_search (rowid, title_uk, title_romaji, title_en, description_uk, description_en, synonyms_flat) " +
                "VALUES (3, 'Майстри меча онлайн', 'Sword Art Online', 'Sword Art Online', 'Опис Майстри меча онлайн', 'Description of Sword Art Online', 'SAO');"
            )

            // 4. Insert Genre Records
            db.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (1, 'action');")
            db.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (1, 'drama');")
            db.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (2, 'action');")
            db.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (2, 'comedy');")
            db.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (3, 'action');")
            db.execSQL("INSERT INTO anime_genres (anilist_id, genre_slug) VALUES (3, 'fantasy');")

            // 5. Insert Studio Records
            db.execSQL("INSERT INTO studios (studio_id, name) VALUES (10, 'Wit Studio');")
            db.execSQL("INSERT INTO studios (studio_id, name) VALUES (20, 'Bones');")
            db.execSQL("INSERT INTO studios (studio_id, name) VALUES (30, 'A-1 Pictures');")

            db.execSQL("INSERT INTO anime_studios (anilist_id, studio_id) VALUES (1, 10);") // Wit Studio
            db.execSQL("INSERT INTO anime_studios (anilist_id, studio_id) VALUES (2, 20);") // Bones
            db.execSQL("INSERT INTO anime_studios (anilist_id, studio_id) VALUES (3, 30);") // A-1 Pictures

            db.execSQL("INSERT INTO studios_search (rowid, name) VALUES (10, 'Wit Studio');")
            db.execSQL("INSERT INTO studios_search (rowid, name) VALUES (20, 'Bones');")
            db.execSQL("INSERT INTO studios_search (rowid, name) VALUES (30, 'A-1 Pictures');")

            // 6. Insert Staff records
            db.execSQL("INSERT INTO staff (staff_id, full_name, image_large) VALUES (1001, 'Tetsurou Araki', 'https://img.staff/araki.png');")
            db.execSQL("INSERT INTO staff (staff_id, full_name, image_large) VALUES (1002, 'Yuki Kaji', 'https://img.staff/kaji.png');")
            db.execSQL("INSERT INTO anime_staff (anilist_id, staff_id, role) VALUES (1, 1001, 'Director');")
            db.execSQL("INSERT INTO anime_staff (anilist_id, staff_id, role) VALUES (1, 1002, 'Eren Yeager');")

            db.execSQL("INSERT INTO staff_search (rowid, full_name) VALUES (1001, 'Tetsurou Araki');")
            db.execSQL("INSERT INTO staff_search (rowid, full_name) VALUES (1002, 'Yuki Kaji');")

            // 7. Insert Rankings records
            db.execSQL("INSERT INTO rankings (anilist_id, rank, context, all_time, season, year) VALUES (1, 1, 'POPULARITY', 1, 'SPRING', 2013);")
            db.execSQL("INSERT INTO rankings (anilist_id, rank, context, all_time, season, year) VALUES (1, 5, 'RATING', 0, NULL, NULL);")

            // 8. Insert Recommendation records
            db.execSQL("INSERT INTO anime_recommendations (source_anilist_id, recommended_anilist_id) VALUES (1, 2);")
            db.execSQL("INSERT INTO anime_recommendations (source_anilist_id, recommended_anilist_id) VALUES (1, 3);")

            // 9. Insert Franchise records
            db.execSQL("INSERT INTO franchises (franchise_id, main_anilist_id, name_en, name_uk) VALUES (100, 1, 'Attack on Titan', 'Атака титанів');")
            db.execSQL("INSERT INTO anime_franchises (anilist_id, franchise_id) VALUES (1, 100);")
            db.execSQL("INSERT INTO anime_franchises (anilist_id, franchise_id) VALUES (2, 100);")

            db.execSQL("INSERT INTO franchises_search (rowid, name_uk, name_en) VALUES (100, 'Атака титанів', 'Attack on Titan');")
        }

        // Mock databaseProvider to return our pre-populated DB
        databaseProvider = object : CatalogDatabaseProvider(context, settingsProvider) {
            override suspend fun getDatabase(): SupportSQLiteDatabase = db
        }

        repository = AnimeRepository(databaseProvider, settingsProvider)
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

    @Test
    fun testAnimeFieldsParsing() = runBlocking {
        val anime = repository.getAnimeById(1L)
        assertNotNull(anime)
        assertEquals(1622976000L, anime!!.airingAt)
        assertEquals(75, anime.airingEpisode)
        assertEquals("AdTzUx76Hjg", anime.trailerId)
        assertEquals("youtube", anime.trailerSite)
        assertEquals("https://img.youtube.com/vi/AdTzUx76Hjg/0.jpg", anime.trailerThumbnail)
        assertEquals(2013, anime.startDateYear)
        assertEquals(4, anime.startDateMonth)
        assertEquals(7, anime.startDateDay)
        assertEquals(1250000, anime.popularity)
        assertEquals("MANGA", anime.source)
        assertEquals("Attack on Titan SnK", anime.synonymsFlat)
    }

    @Test
    fun testGetStaffForAnime() = runBlocking {
        val staff = repository.getStaffForAnime(1L)
        assertEquals(2, staff.size)
        
        val first = staff[0]
        assertEquals(1001L, first.staffId)
        assertEquals("Tetsurou Araki", first.fullName)
        assertEquals("https://img.staff/araki.png", first.imageLarge)
        assertEquals("Director", first.role)

        val second = staff[1]
        assertEquals(1002L, second.staffId)
        assertEquals("Yuki Kaji", second.fullName)
        assertEquals("https://img.staff/kaji.png", second.imageLarge)
        assertEquals("Eren Yeager", second.role)
    }

    @Test
    fun testGetRankingsForAnime() = runBlocking {
        val rankings = repository.getRankingsForAnime(1L)
        assertEquals(2, rankings.size)

        val popularity = rankings.find { it.context == "POPULARITY" }
        assertNotNull(popularity)
        assertEquals(1, popularity!!.rank)
        assertTrue(popularity.allTime)
        assertEquals("SPRING", popularity.season)
        assertEquals(2013, popularity.year)

        val rating = rankings.find { it.context == "RATING" }
        assertNotNull(rating)
        assertEquals(5, rating!!.rank)
        assertFalse(rating.allTime)
        assertNull(rating.season)
        assertNull(rating.year)
    }

    @Test
    fun testGetRecommendationsForAnime() = runBlocking {
        val recs = repository.getRecommendationsForAnime(1L)
        assertEquals(2, recs.size)
        // Ordered by score_mal DESC: Boku no Hero (8.2), then Sword Art (7.2)
        assertEquals(2L, recs[0].anilistId)
        assertEquals(3L, recs[1].anilistId)
    }

    @Test
    fun testGetFranchiseForAnime() = runBlocking {
        val franchise = repository.getFranchiseForAnime(1L)
        assertNotNull(franchise)
        assertEquals(100L, franchise!!.franchiseId)
        assertEquals(1L, franchise.mainAnilistId)
        assertEquals("Attack on Titan", franchise.nameEn)
        assertEquals("Атака титанів", franchise.nameUk)

        val franchiseAnime = repository.getFranchiseAnime(100L)
        assertEquals(2, franchiseAnime.size)
        // Ordered by season_year ASC: Titan (2013), Hero (2016)
        assertEquals(1L, franchiseAnime[0].anilistId)
        assertEquals(2L, franchiseAnime[1].anilistId)
    }

    @Test
    fun testRelevanceSorting() = runBlocking {
        db.execSQL("INSERT INTO anime (anilist_id, title_uk, title_en, popularity) VALUES (10, 'Тест A', 'Test A', 1000);")
        db.execSQL("INSERT INTO anime_search (rowid, title_uk, title_en, description_en) VALUES (10, 'Тест A', 'Test A', 'Contains searchword');")

        db.execSQL("INSERT INTO anime (anilist_id, title_uk, title_en, popularity) VALUES (11, 'Тест B', 'Test B searchword', 10);")
        db.execSQL("INSERT INTO anime_search (rowid, title_uk, title_en, description_en) VALUES (11, 'Тест B', 'Test B searchword', 'Some desc');")

        db.execSQL("INSERT INTO anime (anilist_id, title_uk, title_en, popularity) VALUES (12, 'Тест C searchword', 'Test C', 5);")
        db.execSQL("INSERT INTO anime_search (rowid, title_uk, title_en, description_en) VALUES (12, 'Тест C searchword', 'Test C', 'Some desc');")

        db.execSQL("INSERT INTO anime (anilist_id, title_uk, title_en, popularity) VALUES (13, 'Тест D', 'Test D searchword', 2000);")
        db.execSQL("INSERT INTO anime_search (rowid, title_uk, title_en, description_en) VALUES (13, 'Тест D', 'Test D searchword', 'Some desc');")

        val results = repository.queryAnime(SearchFilterQuery(textQuery = "searchword", sortBy = SortOption.RELEVANCE))
        assertEquals(4, results.size)
        assertEquals(12L, results[0].anilistId) // Tier 1: title_uk
        assertEquals(13L, results[1].anilistId) // Tier 2: title_en (popularity 2000)
        assertEquals(11L, results[2].anilistId) // Tier 2: title_en (popularity 10)
        assertEquals(10L, results[3].anilistId) // Tier 3: description
    }

    @Test
    fun testGetScreenshotsFiltersOutNoneAndNonLinks() = runBlocking {
        // Create screenshots table
        db.execSQL("CREATE TABLE IF NOT EXISTS screenshots (anilist_id INTEGER, image_url TEXT);")
        
        // Insert sample screenshot URLs
        db.execSQL("INSERT INTO screenshots (anilist_id, image_url) VALUES (1, 'https://example.com/image1.jpg');")
        db.execSQL("INSERT INTO screenshots (anilist_id, image_url) VALUES (1, 'none');")
        db.execSQL("INSERT INTO screenshots (anilist_id, image_url) VALUES (1, 'None');")
        db.execSQL("INSERT INTO screenshots (anilist_id, image_url) VALUES (1, 'http://example.com/image2.png');")
        db.execSQL("INSERT INTO screenshots (anilist_id, image_url) VALUES (1, 'not_a_link');")
        db.execSQL("INSERT INTO screenshots (anilist_id, image_url) VALUES (1, '  ');")
        
        val screenshots = repository.getScreenshots(1L)
        assertEquals(2, screenshots.size)
        assertTrue(screenshots.contains("https://example.com/image1.jpg"))
        assertTrue(screenshots.contains("http://example.com/image2.png"))
        assertFalse(screenshots.contains("none"))
        assertFalse(screenshots.contains("None"))
        assertFalse(screenshots.contains("not_a_link"))
    }

    @Test
    fun testFranchiseSmartSorting() = runBlocking {
        // Clear existing test data from relevant tables to avoid interference
        db.execSQL("DELETE FROM anime_franchises;")
        db.execSQL("DELETE FROM franchises;")
        db.execSQL("DELETE FROM anime;")

        // Let's create 3 franchises:
        // Franchise A (ID 1): Has 1 TV title (popularity 100) and 1 OVA title (popularity 200).
        // Since it has a TV title, its weight should be only the TV title (100).
        db.execSQL("INSERT INTO franchises (franchise_id, main_anilist_id, name_en) VALUES (1, 10, 'Franchise A');")
        db.execSQL("INSERT INTO anime (anilist_id, title_romaji, format, popularity, is_adult) VALUES (10, 'A-TV', 'TV', 100, 0);")
        db.execSQL("INSERT INTO anime (anilist_id, title_romaji, format, popularity, is_adult) VALUES (11, 'A-OVA', 'OVA', 200, 0);")
        db.execSQL("INSERT INTO anime_franchises (anilist_id, franchise_id) VALUES (10, 1);")
        db.execSQL("INSERT INTO anime_franchises (anilist_id, franchise_id) VALUES (11, 1);")

        // Franchise B (ID 2): Has only OVA/ONA formats: 1 OVA (popularity 150) and 1 ONA (popularity 50).
        // Count of TV/MOVIE = 0.
        // Fallback average = (150 + 50) / 2 = 100.
        // Penalty weight = 100 * 0.8 = 80.
        db.execSQL("INSERT INTO franchises (franchise_id, main_anilist_id, name_en) VALUES (2, 20, 'Franchise B');")
        db.execSQL("INSERT INTO anime (anilist_id, title_romaji, format, popularity, is_adult) VALUES (20, 'B-OVA', 'OVA', 150, 0);")
        db.execSQL("INSERT INTO anime (anilist_id, title_romaji, format, popularity, is_adult) VALUES (21, 'B-ONA', 'ONA', 50, 0);")
        db.execSQL("INSERT INTO anime_franchises (anilist_id, franchise_id) VALUES (20, 2);")
        db.execSQL("INSERT INTO anime_franchises (anilist_id, franchise_id) VALUES (21, 2);")

        // Franchise C (ID 3): Has 1 TV title (popularity 90).
        // Its weight should be 90.
        db.execSQL("INSERT INTO franchises (franchise_id, main_anilist_id, name_en) VALUES (3, 30, 'Franchise C');")
        db.execSQL("INSERT INTO anime (anilist_id, title_romaji, format, popularity, is_adult) VALUES (30, 'C-TV', 'TV', 90, 0);")
        db.execSQL("INSERT INTO anime_franchises (anilist_id, franchise_id) VALUES (30, 3);")

        // 1. Verify default sort order when NSFW is OFF (default).
        // Expected sorted order of franchises based on weight:
        // Franchise A (weight 100) -> Franchise C (weight 90) -> Franchise B (weight 80)
        
        var resultsDefault = repository.queryFranchisesPaged(query = "", limit = 10, offset = 0)
        assertEquals(3, resultsDefault.size)
        assertEquals("Franchise A", resultsDefault[0].first.nameEn)
        assertEquals("Franchise C", resultsDefault[1].first.nameEn)
        assertEquals("Franchise B", resultsDefault[2].first.nameEn)

        // 2. Verify search querying works and retains same smart sorting.
        val resultsSearch = repository.queryFranchisesPaged(query = "Franchise", limit = 10, offset = 0)
        assertEquals(3, resultsSearch.size)
        assertEquals("Franchise A", resultsSearch[0].first.nameEn)
        assertEquals("Franchise C", resultsSearch[1].first.nameEn)
        assertEquals("Franchise B", resultsSearch[2].first.nameEn)

        // 3. Test NSFW (18+) isolation within the weight subquery block.
        // Add an 18+ TV title with high popularity (500) to Franchise B.
        db.execSQL("INSERT INTO anime (anilist_id, title_romaji, format, popularity, is_adult) VALUES (22, 'B-Adult-TV', 'TV', 500, 1);")
        db.execSQL("INSERT INTO anime_franchises (anilist_id, franchise_id) VALUES (22, 2);")

        // Since show18Plus is false, the adult TV title must be ignored.
        // Franchise B weight should still be 80.
        // Franchise A (100) -> Franchise C (90) -> Franchise B (80).
        var resultsNsfwOff = repository.queryFranchisesPaged(query = "", limit = 10, offset = 0)
        assertEquals(3, resultsNsfwOff.size)
        assertEquals("Franchise A", resultsNsfwOff[0].first.nameEn)
        assertEquals("Franchise C", resultsNsfwOff[1].first.nameEn)
        assertEquals("Franchise B", resultsNsfwOff[2].first.nameEn)

        // 4. Test NSFW (18+) enabled.
        // If show18Plus is true, the adult TV title is included.
        // Franchise B now has a TV title, so its primary weight evaluates to the average of TV formats: 500.
        // Expected order: Franchise B (500) -> Franchise A (100) -> Franchise C (90)
        settingsProvider.setShow18Plus(true)
        settingsProvider.show18Plus.first { it } // Wait until settings provider updates

        var resultsNsfwOn = repository.queryFranchisesPaged(query = "", limit = 10, offset = 0)
        assertEquals(3, resultsNsfwOn.size)
        assertEquals("Franchise B", resultsNsfwOn[0].first.nameEn)
        assertEquals("Franchise A", resultsNsfwOn[1].first.nameEn)
        assertEquals("Franchise C", resultsNsfwOn[2].first.nameEn)
    }
}
