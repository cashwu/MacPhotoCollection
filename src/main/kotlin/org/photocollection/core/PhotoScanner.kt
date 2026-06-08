package org.photocollection.core

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension

/** Result of scanning a source folder, distinguishing a missing folder from an empty result. */
sealed interface ScanResult {
    data class Success(val photos: List<PhotoFile>) : ScanResult
    data object FolderNotFound : ScanResult
}

object PhotoScanner {

    /** Image extensions (lower-case) we treat as organizable, including HEIC and common RAW formats. */
    private val SUPPORTED_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "gif", "heic", "heif",
        "nef", "cr2", "cr3", "arw", "rw2", "orf", "raf", "dng", "sr2", "pef",
    )

    /**
     * Scan the immediate contents of [sourceFolder] for supported image files.
     *
     * Extension matching is case-insensitive; dot-prefixed files and subfolders are ignored; the
     * scan does not recurse. Returns [ScanResult.FolderNotFound] when the path is not a directory.
     */
    fun scan(sourceFolder: Path): ScanResult {
        if (!Files.isDirectory(sourceFolder)) return ScanResult.FolderNotFound
        val photos = Files.newDirectoryStream(sourceFolder).use { entries ->
            entries
                .filter { Files.isRegularFile(it) }
                .filter { !it.fileName.toString().startsWith(".") }
                .filter { it.extension.lowercase() in SUPPORTED_EXTENSIONS }
                .map { PhotoFile(it.toAbsolutePath()) }
                .sortedBy { it.fileName }
        }
        return ScanResult.Success(photos)
    }
}
