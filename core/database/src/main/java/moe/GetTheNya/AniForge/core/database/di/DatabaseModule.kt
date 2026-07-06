package moe.GetTheNya.AniForge.core.database.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import moe.GetTheNya.AniForge.core.database.UserDatabase
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.dao.PendingImportDao
import javax.inject.Singleton

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pending_imports` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`raw_row_text` TEXT NOT NULL, " +
            "`russian_title` TEXT NOT NULL, " +
            "`original_title` TEXT NOT NULL, " +
            "`alternative_titles` TEXT NOT NULL, " +
            "`target_status` TEXT NOT NULL, " +
            "`target_score` REAL, " +
            "`is_favorite` INTEGER NOT NULL)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `pending_imports` ADD COLUMN `matched_anime_id` INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE `pending_imports` ADD COLUMN `import_status` TEXT NOT NULL DEFAULT 'PENDING'")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `user_tracking` ADD COLUMN `is_synced` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `user_tracking` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `collections_new` (" +
            "`id` TEXT NOT NULL, " +
            "`title` TEXT NOT NULL, " +
            "`description` TEXT NOT NULL, " +
            "`createdAt` INTEGER NOT NULL, " +
            "`is_synced` INTEGER NOT NULL DEFAULT 0, " +
            "`is_deleted` INTEGER NOT NULL DEFAULT 0, " +
            "`last_modified` INTEGER NOT NULL DEFAULT 0, " +
            "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `collection_anime_cross_ref_new` (" +
            "`collectionId` TEXT NOT NULL, " +
            "`animeId` INTEGER NOT NULL, " +
            "`orderIndex` INTEGER NOT NULL, " +
            "`is_synced` INTEGER NOT NULL DEFAULT 0, " +
            "`is_deleted` INTEGER NOT NULL DEFAULT 0, " +
            "`last_modified` INTEGER NOT NULL DEFAULT 0, " +
            "PRIMARY KEY(`collectionId`, `animeId`))"
        )

        val idMap = mutableMapOf<String, String>()
        val collectionsCursor = db.query("SELECT id, title, description, createdAt FROM collections")
        try {
            while (collectionsCursor.moveToNext()) {
                val oldId = collectionsCursor.getString(0)
                val title = collectionsCursor.getString(1)
                val description = collectionsCursor.getString(2)
                val createdAt = collectionsCursor.getLong(3)

                val newUuid = if (oldId.length == 36 && oldId.contains("-")) {
                    oldId
                } else {
                    java.util.UUID.randomUUID().toString()
                }
                idMap[oldId] = newUuid

                db.execSQL(
                    "INSERT INTO `collections_new` (id, title, description, createdAt, is_synced, is_deleted, last_modified) VALUES (?, ?, ?, ?, 0, 0, 0)",
                    arrayOf(newUuid, title, description, createdAt)
                )
            }
        } finally {
            collectionsCursor.close()
        }

        val refsCursor = db.query("SELECT collectionId, animeId, orderIndex FROM collection_anime_cross_ref")
        try {
            while (refsCursor.moveToNext()) {
                val oldCollectionId = refsCursor.getString(0)
                val animeId = refsCursor.getLong(1)
                val orderIndex = refsCursor.getInt(2)

                val newUuid = idMap[oldCollectionId] ?: oldCollectionId
                db.execSQL(
                    "INSERT INTO `collection_anime_cross_ref_new` (collectionId, animeId, orderIndex, is_synced, is_deleted, last_modified) VALUES (?, ?, ?, 0, 0, 0)",
                    arrayOf(newUuid, animeId, orderIndex)
                )
            }
        } finally {
            refsCursor.close()
        }

        db.execSQL("DROP TABLE IF EXISTS `collection_anime_cross_ref`")
        db.execSQL("DROP TABLE IF EXISTS `collections`")
        db.execSQL("ALTER TABLE `collections_new` RENAME TO `collections`")
        db.execSQL("ALTER TABLE `collection_anime_cross_ref_new` RENAME TO `collection_anime_cross_ref`")
    }
}


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideUserDatabase(
        @ApplicationContext context: Context
    ): UserDatabase {
        val builder = Room.databaseBuilder(
            context,
            UserDatabase::class.java,
            "user_data.db"
        )
        .openHelperFactory(RequerySQLiteOpenHelperFactory())
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            builder.fallbackToDestructiveMigration(true)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideUserTrackingDao(
        userDatabase: UserDatabase
    ): UserTrackingDao {
        return userDatabase.userTrackingDao()
    }

    @Provides
    @Singleton
    fun providePendingImportDao(
        userDatabase: UserDatabase
    ): PendingImportDao {
        return userDatabase.pendingImportDao()
    }

    @Provides
    @Singleton
    fun provideUserSettingDao(
        userDatabase: UserDatabase
    ): moe.GetTheNya.AniForge.core.database.dao.UserSettingDao {
        return userDatabase.userSettingDao()
    }

    @Provides
    @Singleton
    fun provideCollectionDao(
        userDatabase: UserDatabase
    ): moe.GetTheNya.AniForge.core.database.dao.CollectionDao {
        return userDatabase.collectionDao()
    }

    @Provides
    @Singleton
    fun provideUserStatsDao(
        userDatabase: UserDatabase
    ): moe.GetTheNya.AniForge.core.database.dao.UserStatsDao {
        return userDatabase.userStatsDao()
    }

    @Provides
    @Singleton
    fun provideWidgetConfigDao(
        userDatabase: UserDatabase
    ): moe.GetTheNya.AniForge.core.database.dao.WidgetConfigDao {
        return userDatabase.widgetConfigDao()
    }
}
