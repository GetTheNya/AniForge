package moe.GetTheNya.AniForge

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltAndroidApp
class AniForgeApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun newImageLoader(): ImageLoader {
        // Dedicated thread pool for image decoding to prevent main/IO thread starvation
        val imageExecutor = Executors.newFixedThreadPool(4) { runnable ->
            Thread(runnable, "coil-decode-thread")
        }

        return ImageLoader.Builder(this)
            .okHttpClient { okHttpClient }
            // Memory Caching: allocate 25% of available heap space
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            // Disk Caching: store up to 100MB of imagery
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100 MB
                    .build()
            }
            .allowHardware(true) // zero-copy GPU memory access
            .fetcherDispatcher(Dispatchers.IO)
            .decoderDispatcher(imageExecutor.asCoroutineDispatcher())
            .build()
    }
}
