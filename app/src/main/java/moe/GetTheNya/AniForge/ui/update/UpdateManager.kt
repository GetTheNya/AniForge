package moe.GetTheNya.AniForge.ui.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.GetTheNya.AniForge.BuildConfig
import moe.GetTheNya.AniForge.core.database.util.AppLogger
import moe.GetTheNya.AniForge.core.network.api.GitHubApiService
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gitHubApiService: GitHubApiService,
    private val okHttpClient: OkHttpClient
) {
    sealed interface UpdateState {
        object Idle : UpdateState
        object Checking : UpdateState
        data class UpdateAvailable(val versionName: String, val downloadUrl: String) : UpdateState
        data class Downloading(val progress: Float) : UpdateState
        data class ReadyToInstall(val apkFile: File) : UpdateState
        data class Error(val message: String) : UpdateState
    }

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    // Application lifetime scope for background downloading
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        cleanUpOldApks()
    }

    private fun cleanUpOldApks() {
        try {
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".apk")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            AppLogger.e("UpdateManager", "Failed to clean old APKs", e)
        }
    }

    suspend fun checkForUpdates(silent: Boolean = false): UpdateState = withContext(Dispatchers.IO) {
        val currentState = _updateState.value
        if (currentState is UpdateState.Checking || currentState is UpdateState.Downloading || currentState is UpdateState.ReadyToInstall) {
            return@withContext currentState
        }

        if (!silent) {
            _updateState.value = UpdateState.Checking
        }
        try {
            val latestRelease = gitHubApiService.getLatestRelease()
            val remoteTag = latestRelease.tagName
            val cleanRemote = remoteTag.trim().removePrefix("v")
            val cleanLocal = BuildConfig.VERSION_NAME.trim().removePrefix("v")

            if (isVersionGreater(cleanRemote, cleanLocal)) {
                val apkAsset = latestRelease.assets.firstOrNull { it.name.endsWith(".apk") }
                val downloadUrl = apkAsset?.browserDownloadUrl ?: latestRelease.htmlUrl
                val state = UpdateState.UpdateAvailable(remoteTag, downloadUrl)
                _updateState.value = state
                state
            } else {
                val state = UpdateState.Idle
                _updateState.value = state
                state
            }
        } catch (e: Exception) {
            AppLogger.e("UpdateManager", "Check for updates failed", e)
            val state = UpdateState.Error(e.localizedMessage ?: "Unknown update check error")
            if (!silent) {
                _updateState.value = state
            }
            state
        }
    }

    private fun isVersionGreater(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
        val maxLength = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    fun startDownload(downloadUrl: String) {
        val currentState = _updateState.value
        if (currentState is UpdateState.Downloading) return

        scope.launch {
            _updateState.value = UpdateState.Downloading(0f)
            try {
                val request = Request.Builder().url(downloadUrl).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Failed to download file, response code: ${response.code}")
                    }
                    val body = response.body ?: throw Exception("Empty response body")
                    
                    val cacheDir = context.externalCacheDir ?: context.cacheDir
                    val apkFile = File(cacheDir, "update.apk")
                    if (apkFile.exists()) {
                        apkFile.delete()
                    }

                    val totalBytes = body.contentLength()
                    var bytesDownloaded = 0L
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    body.byteStream().use { input ->
                        apkFile.outputStream().use { output ->
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                bytesDownloaded += bytesRead
                                if (totalBytes > 0) {
                                    val progress = bytesDownloaded.toFloat() / totalBytes
                                    _updateState.value = UpdateState.Downloading(progress)
                                }
                            }
                        }
                    }

                    _updateState.value = UpdateState.ReadyToInstall(apkFile)
                }
            } catch (e: java.util.concurrent.CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e("UpdateManager", "APK download failed", e)
                _updateState.value = UpdateState.Error(e.localizedMessage ?: "Download failed")
            }
        }
    }

    fun installUpdate(apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.e("UpdateManager", "Failed to trigger package installer", e)
            _updateState.value = UpdateState.Error(e.localizedMessage ?: "Installation failed")
        }
    }

    fun reset() {
        _updateState.value = UpdateState.Idle
    }
}
