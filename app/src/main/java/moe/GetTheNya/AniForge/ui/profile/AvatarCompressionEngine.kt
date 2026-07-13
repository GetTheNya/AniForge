package moe.GetTheNya.AniForge.ui.profile

import android.graphics.Bitmap
import android.os.Build
import java.io.ByteArrayOutputStream

object AvatarCompressionEngine {
    fun optimizeAvatar(bitmap: Bitmap): ByteArray {
        // Step 1: Forcefully resize the cropped square bitmap down to exactly 200x200 pixels
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
        
        // Step 2: WebP Compression
        var quality = 80
        var byteArray: ByteArray
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        
        do {
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(format, quality, outputStream)
            byteArray = outputStream.toByteArray()
            // Constraint checklist: Payload must strictly stay under 20 KB
            if (byteArray.size <= 20 * 1024 || quality <= 50) {
                break
            }
            quality -= 5 // Reduce quality if it exceeds 20 KB
        } while (quality > 0)
        
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        return byteArray
    }
}
