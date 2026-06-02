package moe.GetTheNya.AniForge.core.database.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

object GzipDecompressor {
    /**
     * Decompresses an input stream containing GZIP data into a destination file.
     */
    suspend fun decompress(inputStream: InputStream, destinationFile: File): Unit = withContext(Dispatchers.IO) {
        GZIPInputStream(inputStream.buffered()).use { gzipInput ->
            destinationFile.outputStream().buffered().use { fileOutput ->
                gzipInput.copyTo(fileOutput)
            }
        }
    }

    /**
     * Decompresses a GZIP file into a destination file.
     */
    suspend fun decompressFile(gzipFile: File, destinationFile: File): Unit = withContext(Dispatchers.IO) {
        FileInputStream(gzipFile).use { fileInput ->
            decompress(fileInput, destinationFile)
        }
    }
}
