package com.proxyproof.app.attendance


import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

object CsvExporter {

    /**
     * Creates a CSV file from attendance records and opens Android's
     * share sheet so the organiser can save or send the export.
     */
    fun export(
        context: Context,
        classCode: String,
        records: List<VerifiedAttendance>
    ): File {
        val safeClassCode = classCode.trim()
            .uppercase()
            .replace(Regex("[^A-Z0-9_-]"), "_")
            .ifBlank { "attendance" }

        val exportDirectory = File(
            context.cacheDir,
            "attendance_exports"
        )

        if (!exportDirectory.exists() && !exportDirectory.mkdirs()) {
            throw IOException("Could not create export directory.")
        }

        val outputFile = File(
            exportDirectory,
            "${safeClassCode}_${System.currentTimeMillis()}.csv"
        )

        outputFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write("Student ID,Student Name,Class Code,Marked At,Signal Strength")
            writer.newLine()

            records.forEach { record ->
                writer.write(
                    listOf(
                        record.studentId,
                        record.studentName,
                        record.classCode,
                        record.markedAt.toString(),
                        record.signalStrength.toString()
                    ).joinToString(",") { escapeCsv(it) }
                )
                writer.newLine()
            }
        }

        return outputFile
    }

    /**
     * Escapes CSV values containing commas, quotes or newlines.
     */
    private fun escapeCsv(value: String): String {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    /**
     * Shares an exported file using Android's chooser.
     * Requires a configured FileProvider in AndroidManifest.xml.
     */
    fun share(
        context: Context,
        file: File
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(intent, "Export attendance CSV")
        )
    }
}
