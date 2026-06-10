package org.photocollection.ui

import org.photocollection.core.CaptureDate
import org.photocollection.core.DateResult
import org.photocollection.core.DateSource
import org.photocollection.core.PhotoFile
import java.nio.file.Paths
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Covers the scan mapping the organizer UI uses to split scanned files into the organizable "file
 * list" and the no-date "error list". A full date and a year-only result are both organizable (and
 * must show their target folder); a no-date file is not, so it falls to the error list. No scanned
 * file may vanish from both lists while still being moved.
 */
class OrganizerScanMappingTest {

    private fun photo(name: String) = PhotoFile(Paths.get("/photos/$name"))

    @Test
    fun `full date maps to its yyyy-MM-dd target folder`() {
        assertEquals("2024-03-15", organizableTargetFolder(DateResult.Found(CaptureDate(LocalDate.of(2024, 3, 15), DateSource.EXIF))))
    }

    @Test
    fun `year-only maps to its yyyy-00-00 target folder`() {
        assertEquals("2008-00-00", organizableTargetFolder(DateResult.YearOnly(2008)))
    }

    @Test
    fun `no-date maps to null so it stays out of the organizable list`() {
        assertNull(organizableTargetFolder(DateResult.NoDate))
    }

    @Test
    fun `scan with full year-only and no-date partitions every file into exactly one list`() {
        val scanned = listOf(
            photo("full.jpg") to DateResult.Found(CaptureDate(LocalDate.of(2024, 3, 15), DateSource.EXIF)),
            photo("year.png") to DateResult.YearOnly(2008),
            photo("none.png") to DateResult.NoDate,
        )

        val organizable = scanned.mapNotNull { (p, r) -> organizableTargetFolder(r)?.let { p to it } }
        val errors = scanned.filter { it.second is DateResult.NoDate }.map { it.first }

        assertEquals(
            listOf("full.jpg" to "2024-03-15", "year.png" to "2008-00-00"),
            organizable.map { it.first.fileName to it.second },
        )
        assertEquals(listOf("none.png"), errors.map { it.fileName })
        // No file is in both lists and none is lost: organizable + errors == scanned.
        assertEquals(scanned.size, organizable.size + errors.size)
    }
}
