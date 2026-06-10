package org.photocollection.core

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExifReaderTest {

    // A fixed "today" for combineTiers seam tests, so filename validation bounds are deterministic.
    private val today = LocalDate.of(2026, 6, 10)

    private fun dateOf(result: DateResult): LocalDate? =
        (result as? DateResult.Found)?.date?.date

    // spec `photo-organization` — "date source resolution" table

    @Test
    fun `DateTimeOriginal is used and the evening time does not shift the day`() {
        val result = ExifReader.resolve(dateTimeOriginal = "2024:03:15 23:50:00", dateTimeDigitized = null)
        assertEquals(LocalDate.of(2024, 3, 15), dateOf(result))
    }

    @Test
    fun `falls back to DateTimeDigitized when original is absent`() {
        val result = ExifReader.resolve(dateTimeOriginal = null, dateTimeDigitized = "2022:01:02 08:30:00")
        assertEquals(LocalDate.of(2022, 1, 2), dateOf(result))
    }

    @Test
    fun `zero month and day yield year-only with the parsed year`() {
        val result = ExifReader.resolve(dateTimeOriginal = "2008:00:00 00:00:00", dateTimeDigitized = null)
        assertEquals(DateResult.YearOnly(2008), result)
    }

    @Test
    fun `out-of-range month yields year-only with the parsed year`() {
        val result = ExifReader.resolve(dateTimeOriginal = "2008:13:45 00:00:00", dateTimeDigitized = null)
        assertEquals(DateResult.YearOnly(2008), result)
    }

    @Test
    fun `valid month with out-of-range day yields year-only via LocalDate failure`() {
        val result = ExifReader.resolve(dateTimeOriginal = "2008:02:30 00:00:00", dateTimeDigitized = null)
        assertEquals(DateResult.YearOnly(2008), result)
    }

    @Test
    fun `zero sentinel is treated as no date`() {
        val result = ExifReader.resolve(dateTimeOriginal = "0000:00:00 00:00:00", dateTimeDigitized = null)
        assertIs<DateResult.NoDate>(result)
    }

    @Test
    fun `year outside 1 to 9999 is treated as no date`() {
        val result = ExifReader.resolve(dateTimeOriginal = "12024:03:15 00:00:00", dateTimeDigitized = null)
        assertIs<DateResult.NoDate>(result)
    }

    @Test
    fun `absent EXIF block yields no date`() {
        val result = ExifReader.resolve(dateTimeOriginal = null, dateTimeDigitized = null)
        assertIs<DateResult.NoDate>(result)
    }

    @Test
    fun `corrupt date bytes yield no date without throwing`() {
        val result = ExifReader.resolve(dateTimeOriginal = "not-a-date", dateTimeDigitized = null)
        assertIs<DateResult.NoDate>(result)
    }

    // spec "full tier resolution" — a malformed DateTimeOriginal does not fall back to
    // DateTimeDigitized; existing tier 1 semantics are kept (the result is no date, not 2022-01-02).
    @Test
    fun `malformed original does not fall back to digitized`() {
        val result = ExifReader.resolve(dateTimeOriginal = "garbage", dateTimeDigitized = "2022:01:02 08:30:00")
        assertIs<DateResult.NoDate>(result)
    }

    // spec "full tier resolution" — rows whose EXIF result is known, tested on the combineTiers seam
    // (the repo has no EXIF fixtures, and the seam takes the EXIF result directly).

    @Test
    fun `EXIF full date wins over a filename date`() {
        val exif = DateResult.Found(CaptureDate(LocalDate.of(2024, 3, 15), DateSource.EXIF))
        val result = ExifReader.combineTiers(exif, "SCR-20251028-jljd.png", today) { LocalDate.of(2025, 10, 30) }
        assertEquals(exif, result)
    }

    @Test
    fun `year-only EXIF result does not fall through to later tiers`() {
        val result = ExifReader.combineTiers(DateResult.YearOnly(2008), "SCR-20251028-jljd.png", today) {
            LocalDate.of(2025, 10, 30)
        }
        assertEquals(DateResult.YearOnly(2008), result)
    }

    @Test
    fun `no EXIF date falls through to a filename date`() {
        val result = ExifReader.combineTiers(DateResult.NoDate, "SCR-20251028-jljd.png", today) {
            LocalDate.of(2025, 10, 30)
        }
        assertEquals(DateResult.Found(CaptureDate(LocalDate.of(2025, 10, 28), DateSource.FILENAME)), result)
    }

    @Test
    fun `no EXIF and no filename date falls through to the file system date`() {
        val result = ExifReader.combineTiers(DateResult.NoDate, "12345678.png", today) {
            LocalDate.of(2025, 10, 30)
        }
        assertEquals(DateResult.Found(CaptureDate(LocalDate.of(2025, 10, 30), DateSource.FILE_SYSTEM)), result)
    }

    @Test
    fun `no date in any tier yields no date`() {
        val result = ExifReader.combineTiers(DateResult.NoDate, "random.png", today) { null }
        assertIs<DateResult.NoDate>(result)
    }

    // read()-level rows: no EXIF fixture, so use real temp files. The exception path (corrupt image
    // bytes) is treated as an absent EXIF date and falls through the chain.

    @Test
    fun `read treats a corrupt image with a dated name as a filename date`() {
        val folder: Path = Files.createTempDirectory("exif-test")
        val file = folder.resolve("SCR-20251028-jljd.png").also { it.writeText("not a real png") }

        val result = ExifReader.read(PhotoFile(file))

        assertEquals(DateResult.Found(CaptureDate(LocalDate.of(2025, 10, 28), DateSource.FILENAME)), result)
    }

    @Test
    fun `read falls to the file system date for a corrupt image with no dated name`() {
        val folder: Path = Files.createTempDirectory("exif-test")
        val file = folder.resolve("random.png").also { it.writeText("not a real png") }
        val instant = Instant.parse("2020-05-15T12:00:00Z")
        Files.setLastModifiedTime(file, FileTime.from(instant))
        val expected = instant.atZone(ZoneId.systemDefault()).toLocalDate()

        val result = ExifReader.read(PhotoFile(file))

        assertEquals(DateResult.Found(CaptureDate(expected, DateSource.FILE_SYSTEM)), result)
    }

    @Test
    fun `readAll continues past a corrupt file`() {
        val folder: Path = Files.createTempDirectory("exif-test")
        val a = folder.resolve("a.png").also { it.writeText("garbage") }
        val b = folder.resolve("b.png").also { it.writeText("more garbage") }

        val results = ExifReader.readAll(listOf(PhotoFile(a), PhotoFile(b)))

        assertEquals(2, results.size)
    }
}
