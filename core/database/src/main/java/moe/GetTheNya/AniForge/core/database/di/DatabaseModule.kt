package moe.GetTheNya.AniForge.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
        return Room.databaseBuilder(
            context,
            UserDatabase::class.java,
            "user_data.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideUserTrackingDao(
        userDatabase: UserDatabase
    ): UserTrackingDao {
        return userDatabase.userTrackingDao()
    }
}
