package org.photocollection.core

import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.LocalDate
import java.time.ZoneId

/**
 * Tier 3 of the date fallback chain: derive a capture date from the file system's creation and
 * last-modified times, taking the earlier of the two as the closest estimate of when the file was
 * produced. Returns null when reading the attributes fails or both dates are sentinels.
 */
object FileSystemDateReader {

    /** Dates before this are sentinels (epoch-zero birth time, DOS-epoch mtime), not real dates. */
    private val MIN_DATE: LocalDate = LocalDate.of(1990, 1, 1)

    /**
     * The earlier of the file's creation and last-modified dates, or null on an I/O failure or when
     * both are sentinels. Future dates are accepted as-is (tier 3 is the last resort).
     */
    fun read(photo: PhotoFile): LocalDate? = try {
        val attrs = Files.readAttributes(photo.path, BasicFileAttributes::class.java)
        pick(civilDate(attrs.creationTime()), civilDate(attrs.lastModifiedTime()))
    } catch (e: IOException) {
        null
    }

    /**
     * The earlier of [creationDate] and [modifiedDate], ignoring either one that is a sentinel
     * (before [MIN_DATE]) and falling back to the other; null when both are sentinels or absent.
     * Isolated from file I/O so the earlier-of / symmetric-sentinel logic is directly testable.
     */
    internal fun pick(creationDate: LocalDate?, modifiedDate: LocalDate?): LocalDate? {
        val creation = creationDate?.takeUnless { it.isBefore(MIN_DATE) }
        val modified = modifiedDate?.takeUnless { it.isBefore(MIN_DATE) }
        if (creation != null && modified != null) {
            return if (creation.isBefore(modified)) creation else modified
        }
        return creation ?: modified
    }

    private fun civilDate(fileTime: FileTime): LocalDate =
        fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}
