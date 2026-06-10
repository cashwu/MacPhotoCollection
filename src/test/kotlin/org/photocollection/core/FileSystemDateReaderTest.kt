package org.photocollection.core

import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FileSystemDateReaderTest {

    // spec `photo-organization` — "earlier of creation and modification" example table (all seven
    // rows), exercised on the pick() seam because the JVM cannot reliably set a file's creationTime.
    @Test
    fun `pick covers the earlier-of and sentinel example table`() {
        fun d(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day)

        assertEquals(d(2025, 10, 30), FileSystemDateReader.pick(d(2025, 11, 1), d(2025, 10, 30)))
        assertEquals(d(2025, 10, 28), FileSystemDateReader.pick(d(2025, 10, 28), d(2025, 10, 28)))
        assertEquals(d(2025, 10, 28), FileSystemDateReader.pick(d(2025, 10, 28), d(2025, 12, 1)))
        assertEquals(d(2025, 10, 30), FileSystemDateReader.pick(d(1970, 1, 1), d(2025, 10, 30)))
        assertEquals(d(2025, 11, 1), FileSystemDateReader.pick(d(2025, 11, 1), d(1980, 1, 1)))
        assertNull(FileSystemDateReader.pick(d(1970, 1, 1), d(1980, 1, 1)))
        assertEquals(d(2026, 12, 31), FileSystemDateReader.pick(d(2026, 12, 31), d(2026, 12, 31)))
    }

    // Happy path on a real file: the expected date is derived from the written mtime instant via the
    // same ZoneId.systemDefault() conversion, never hard-coded, so the test is time-zone stable.
    @Test
    fun `read returns the civil date of a real file's mtime`() {
        val folder = Files.createTempDirectory("fsdate-test")
        val file = folder.resolve("screenshot.png").also { it.writeText("bytes") }
        val instant = Instant.parse("2020-05-15T12:00:00Z")
        Files.setLastModifiedTime(file, FileTime.from(instant))
        val expected = instant.atZone(ZoneId.systemDefault()).toLocalDate()

        // creationTime is the file's birth time (~now) and so never earlier than the 2020 mtime;
        // on APFS, setting an earlier mtime also pulls birth time down to it — either way the
        // earlier of the two equals the mtime's civil date.
        assertEquals(expected, FileSystemDateReader.read(PhotoFile(file)))
    }

    @Test
    fun `read returns null when the file attributes cannot be read`() {
        val folder = Files.createTempDirectory("fsdate-test")
        val missing = folder.resolve("does-not-exist.png")

        assertNull(FileSystemDateReader.read(PhotoFile(missing)))
    }
}
