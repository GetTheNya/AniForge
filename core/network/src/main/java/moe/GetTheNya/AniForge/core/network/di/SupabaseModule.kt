package moe.GetTheNya.AniForge.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.createSupabaseClient
import moe.GetTheNya.AniForge.core.network.BuildConfig
import android.util.Log
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    private const val TAG = "SupabaseModule"

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        val url = BuildConfig.SUPABASE_URL.ifBlank {
            Log.e(TAG, "SUPABASE_URL is blank! Please configure local.properties.")
            "https://placeholder.supabase.co"
        }
        val key = BuildConfig.SUPABASE_ANON_KEY.ifBlank {
            Log.e(TAG, "SUPABASE_ANON_KEY is blank! Please configure local.properties.")
            "placeholder"
        }

        return createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            install(Auth) {
                // By default, Supabase Kotlin SDK persists the session on Android.
            }
            install(Postgrest)
            install(Storage)
        }
    }
}
