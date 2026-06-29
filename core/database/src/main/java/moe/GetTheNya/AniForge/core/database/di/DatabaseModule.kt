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
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        
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
