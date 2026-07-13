package moe.GetTheNya.AniForge.core.network.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import moe.GetTheNya.AniForge.core.model.sync.CatalogDownloader
import moe.GetTheNya.AniForge.core.network.api.AniForgeApiService
import moe.GetTheNya.AniForge.core.network.api.GitHubApiService
import moe.GetTheNya.AniForge.core.network.api.AniListApiService
import moe.GetTheNya.AniForge.core.network.sync.CatalogDownloaderImpl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cache
import java.io.File

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindCatalogDownloader(
        impl: CatalogDownloaderImpl
    ): CatalogDownloader

    companion object {

        @Provides
        @Singleton
        fun provideLoggingInterceptor(): HttpLoggingInterceptor {
            return HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }
        }

        @Provides
        @Singleton
        fun provideOkHttpClient(
            @ApplicationContext context: Context,
            loggingInterceptor: HttpLoggingInterceptor
        ): OkHttpClient {
            val cacheSize = 10 * 1024 * 1024L // 10 MB
            val cacheDir = File(context.cacheDir, "http_cache")
            val cache = Cache(cacheDir, cacheSize)

            return OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(loggingInterceptor)
                .addNetworkInterceptor { chain ->
                    val request = chain.request()
                    val response = chain.proceed(request)
                    if (request.url.toString().contains("avatar.webp")) {
                        response.newBuilder()
                            .header("Cache-Control", "public, max-age=31536000")
                            .build()
                    } else {
                        response
                    }
                }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        @Provides
        @Singleton
        fun provideRetrofit(
            okHttpClient: OkHttpClient
        ): Retrofit {
            return Retrofit.Builder()
                .baseUrl(AniForgeApiService.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        @Provides
        @Singleton
        fun provideApiService(
            retrofit: Retrofit
        ): AniForgeApiService {
            return retrofit.create(AniForgeApiService::class.java)
        }

        @Provides
        @Singleton
        fun provideGitHubApiService(
            retrofit: Retrofit
        ): GitHubApiService {
            return retrofit.create(GitHubApiService::class.java)
        }

        @Provides
        @Singleton
        fun provideAniListApiService(
            retrofit: Retrofit
        ): AniListApiService {
            return retrofit.create(AniListApiService::class.java)
        }
    }
}
