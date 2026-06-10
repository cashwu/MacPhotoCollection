package org.photocollection.core

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import java.time.DateTimeException
import java.time.LocalDate

/** Outcome of reading a single file's EXIF capture date. */
sealed interface DateResult {
    data class Found(val date: CaptureDate) : DateResult
    data class YearOnly(val year: Int) : DateResult
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
     * applied. Absent or unreadable values yield [DateResult.NoDate].
     */
    internal fun resolve(dateTimeOriginal: String?, dateTimeDigitized: String?): DateResult {
        val raw = dateTimeOriginal ?: dateTimeDigitized ?: return DateResult.NoDate
        return parseExifDate(raw)
    }

    /**
     * Classify the `yyyy:MM:dd` date portion of an EXIF `yyyy:MM:dd HH:mm:ss` value into three
     * outcomes. A year in `1..9999` with month and day both `> 0` forming a valid calendar date is
     * [DateResult.Found]; a year in `1..9999` whose month/day cannot form a valid date (zero, or
     * non-zero but out of range like `2008:13:45`) is [DateResult.YearOnly] because the year is
     * still reliable; a zero or out-of-range year, a non-numeric segment, or fewer than three
     * segments is [DateResult.NoDate]. The `1..9999` bound keeps year folders at four digits.
     */
    private fun parseExifDate(raw: String): DateResult {
        val parts = raw.trim().substringBefore(' ').split(':')
        if (parts.size != 3) return DateResult.NoDate
        val (year, month, day) = parts.map { it.toIntOrNull() ?: return DateResult.NoDate }
        if (year !in 1..9999) return DateResult.NoDate
        if (month > 0 && day > 0) {
            return try {
                DateResult.Found(CaptureDate(LocalDate.of(year, month, day)))
            } catch (e: DateTimeException) {
                DateResult.YearOnly(year)
            }
        }
        return DateResult.YearOnly(year)
    }
}
