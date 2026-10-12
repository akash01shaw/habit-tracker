package com.loopwise.app.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val fileNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm")

/** Writes a CSV string to the app's cache dir and returns a FileProvider content:// Uri for sharing. */
class CsvFileWriter(private val context: Context) {
    fun write(csv: String): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "loopwise-export-${fileNameFormatter.format(LocalDateTime.now())}.csv")
        file.writeText(csv)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
