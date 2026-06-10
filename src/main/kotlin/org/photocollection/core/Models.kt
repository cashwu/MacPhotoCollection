package org.photocollection.core

import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** A scanned image file, identified by its absolute path. */
data class PhotoFile(val path: Path) {
    val fileName: String get() = path.fileName.toString()
}

/** Shared folder name for files whose date could not be resolved by any tier. */
const val NO_EXIF_FOLDER: String = "no-exif"

/** The four-digit zero-padded year folder name (for example year 7 renders as `0007`). */
fun yearFolderName(year: Int): String = "%04d".format(year)

/** The `yyyy-00-00` date-folder name for a year-only result, nested under [yearFolderName]. */
fun yearOnlyFolderName(year: Int): String = "%04d-00-00".format(year)

/** Which tier of the date fallback chain produced a capture date. */
enum class DateSource { EXIF, FILENAME, FILE_SYSTEM }

/** A capture date with no time-of-day or timezone component, tagged with the tier that produced it. */
data class CaptureDate(val date: LocalDate, val source: DateSource) {
    /** The `yyyy-MM-dd` form used as the target date-folder name. */
    fun folderName(): String = date.format(ISO_DATE)

    /** The four-digit year folder this date's date folder nests under. */
    fun yearFolderName(): String = yearFolderName(date.year)

    companion object {
        private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}

/**
 * A single planned move: a source photo, its absolute target path, and the date result that
 * produced the target. [dateResult] carries the resolved date and its [DateSource] so the plan
 * preview can show which fallback tier dated the file; the planner stores it without branching.
 */
data class MoveItem(val source: PhotoFile, val target: Path, val dateResult: DateResult)

/**
 * A computed move plan. [moves] holds every scanned file's target — dated files to a nested
 * year/date folder, year-only files to a `yyyy-00-00` folder, and no-date files to the shared
 * `no-exif/` folder. [errors] lists the no-date files (which are still moved into `no-exif/`) so
 * the UI can show their count; it is classification info, not "files that are not moved".
 */
data class MovePlan(val moves: List<MoveItem>, val errors: List<PhotoFile>)
