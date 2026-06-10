package org.photocollection.core

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExifReaderTest {

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

    // batch resilience: a single unreadable file must not abort the run

    @Test
    fun `read of an unreadable file returns no date instead of throwing`() {
        val folder: Path = Files.createTempDirectory("exif-test")
        val garbage = folder.resolve("corrupt.jpg")
        garbage.writeText("this is not a real jpeg")

        val result = ExifReader.read(PhotoFile(garbage))

        assertIs<DateResult.NoDate>(result)
    }

    @Test
    fun `readAll continues past an unreadable file`() {
        val folder: Path = Files.createTempDirectory("exif-test")
        val a = folder.resolve("a.jpg").also { it.writeText("garbage") }
        val b = folder.resolve("b.jpg").also { it.writeText("more garbage") }

        val results = ExifReader.readAll(listOf(PhotoFile(a), PhotoFile(b)))

        assertEquals(2, results.size)
        results.forEach { assertIs<DateResult.NoDate>(it.second) }
    }
}
