package moe.GetTheNya.AniForge.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.GetTheNya.AniForge.AniForgeApplication
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest

object ImageCacheManager {

    suspend fun getCachedImage(context: Context, url: String): File? = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "fullscreen_media")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val fileName = hashString(url) + ".jpg"
            val cacheFile = File(cacheDir, fileName)

            // If it already exists and is not empty, return it immediately
            if (cacheFile.exists() && cacheFile.length() > 0) {
                return@withContext cacheFile
            }

            // Retrieve OkHttpClient from application instance
            val app = context.applicationContext as? AniForgeApplication ?: return@withContext null
            val okHttpClient = app.okHttpClient

            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bytes = response.body?.bytes() ?: return@withContext null

                // Pre-decode bounds to check raw dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                val width = options.outWidth
                val height = options.outHeight

                if (width <= 0 || height <= 0) return@withContext null

                val tmpFile = File(cacheDir, "$fileName.tmp")

                if (width > 1080 || height > 1080) {
                    val maxCeiling = 1080f
                    val targetWidth: Int
                    val targetHeight: Int
                    if (width > height) {
                        targetWidth = 1080
                        targetHeight = ((height.toFloat() / width.toFloat()) * maxCeiling).toInt()
                    } else {
                        targetHeight = 1080
                        targetWidth = ((width.toFloat() / height.toFloat()) * maxCeiling).toInt()
                    }

                    // Calculate optimal inSampleSize to reduce memory allocation
                    val inSampleSize = calculateInSampleSize(width, height, targetWidth, targetHeight)
                    val decodeOptions = BitmapFactory.Options().apply {
                        this.inSampleSize = inSampleSize
                    }
                    val decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                        ?: return@withContext null

                    // Perform exact downscale to the 1080px ceiling if necessary
                    val finalBitmap = if (decodedBitmap.width != targetWidth || decodedBitmap.height != targetHeight) {
                        Bitmap.createScaledBitmap(decodedBitmap, targetWidth, targetHeight, true).also {
                            if (it != decodedBitmap) {
                                decodedBitmap.recycle()
                            }
                        }
                    } else {
                        decodedBitmap
                    }

                    try {
                        // Compress as high-quality JPEG (90% quality) and save to temporary file
                        tmpFile.outputStream().use { fos ->
                            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                        }
                        // Perform atomic rename to finalize cached file
                        if (!tmpFile.renameTo(cacheFile)) {
                            throw IOException("Failed to rename temp file $tmpFile to $cacheFile")
                        }
                    } catch (e: Exception) {
                        if (tmpFile.exists()) {
                            tmpFile.delete()
                        }
                        e.printStackTrace()
                        return@withContext null
                    } finally {
                        // Enforce recycling finalBitmap to prevent native heap out-of-memory errors
                        finalBitmap.recycle()
                    }
                } else {
                    // No downscaling needed, store original image directly via temporary file
                    try {
                        tmpFile.writeBytes(bytes)
                        if (!tmpFile.renameTo(cacheFile)) {
                            throw IOException("Failed to rename temp file $tmpFile to $cacheFile")
                        }
                    } catch (e: Exception) {
                        if (tmpFile.exists()) {
                            tmpFile.delete()
                        }
                        e.printStackTrace()
                        return@withContext null
                    }
                }
            }

            if (cacheFile.exists() && cacheFile.length() > 0) {
                cacheFile
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
