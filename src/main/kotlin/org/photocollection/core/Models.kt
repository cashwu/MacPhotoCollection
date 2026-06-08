package org.photocollection.core

import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** A scanned image file, identified by its absolute path. */
data class PhotoFile(val path: Path) {
    val fileName: String get() = path.fileName.toString()
}

/** A capture date taken from EXIF, with no time-of-day or timezone component. */
data class CaptureDate(val date: LocalDate) {
    /** The `yyyy-MM-dd` form used as the target date-folder name. */
    fun folderName(): String = date.format(ISO_DATE)

    companion object {
        private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}

/** A single planned move: a source photo and its absolute target path. */
data class MoveItem(val source: PhotoFile, val target: Path)

/** A computed move plan: files to move plus files with no usable capture date. */
data class MovePlan(val moves: List<MoveItem>, val errors: List<PhotoFile>)
