package org.photocollection.core

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import java.time.DateTimeException
import java.time.LocalDate

/** Outcome of reading a single file's EXIF capture date. */
sealed interface DateResult {
    data class Found(val date: CaptureDate) : DateResult
    data object NoDate : DateResult
}

object ExifReader {

    /**
     * Read [photo]'s EXIF capture date. Any failure reading the file is caught and reported as
     * [DateResult.NoDate] so a single bad file never aborts a batch.
     */
    fun read(photo: PhotoFile): DateResult = try {
        val dir = ImageMetadataReader.readMetadata(photo.path.toFile())
            .getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
        resolve(
            dateTimeOriginal = dir?.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL),
            dateTimeDigitized = dir?.getString(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED),
        )
    } catch (e: Exception) {
        DateResult.NoDate
    }

    /** Read EXIF dates for every file; never throws, so one unreadable file cannot stop the batch. */
    fun readAll(photos: List<PhotoFile>): List<Pair<PhotoFile, DateResult>> =
        photos.map { it to read(it) }

    /**
     * Resolve a [DateResult] from the raw EXIF tag strings: `DateTimeOriginal` is primary and
     * `DateTimeDigitized` is the fallback only when the primary tag is absent. The date portion of
     * the raw string is parsed directly (no instant/epoch accessor), so no timezone conversion is
     * applied. Absent, malformed, or zero-sentinel values yield [DateResult.NoDate].
     */
    internal fun resolve(dateTimeOriginal: String?, dateTimeDigitized: String?): DateResult {
        val raw = dateTimeOriginal ?: dateTimeDigitized ?: return DateResult.NoDate
        val date = parseExifDate(raw) ?: return DateResult.NoDate
        return DateResult.Found(CaptureDate(date))
    }

    /** Parse the `yyyy:MM:dd` date portion of an EXIF `yyyy:MM:dd HH:mm:ss` value, or null. */
    private fun parseExifDate(raw: String): LocalDate? {
        val parts = raw.trim().substringBefore(' ').split(':')
        if (parts.size != 3) return null
        val (year, month, day) = parts.map { it.toIntOrNull() ?: return null }
        if (year == 0 || month == 0 || day == 0) return null
        return try {
            LocalDate.of(year, month, day)
        } catch (e: DateTimeException) {
            null
        }
    }
}
