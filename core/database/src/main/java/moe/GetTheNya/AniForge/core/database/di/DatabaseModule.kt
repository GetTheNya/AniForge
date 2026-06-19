package moe.GetTheNya.AniForge.core.database.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import moe.GetTheNya.AniForge.core.database.UserDatabase
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import javax.inject.Singleton

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
