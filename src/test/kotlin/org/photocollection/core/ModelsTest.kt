package org.photocollection.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelsTest {

    @Test
    fun `CaptureDate formats to yyyy-MM-dd`() {
        val date = CaptureDate(LocalDate.of(2024, 3, 15), DateSource.EXIF)
        assertEquals("2024-03-15", date.folderName())
    }

    @Test
    fun `CaptureDate carries its date source`() {
        val date = LocalDate.of(2024, 3, 15)
        assertEquals(DateSource.EXIF, CaptureDate(date, DateSource.EXIF).source)
        assertEquals(DateSource.FILENAME, CaptureDate(date, DateSource.FILENAME).source)
        assertEquals(DateSource.FILE_SYSTEM, CaptureDate(date, DateSource.FILE_SYSTEM).source)
    }

    @Test
    fun `year folder name is the four-digit year`() {
        assertEquals("2008", yearFolderName(2008))
    }

    @Test
    fun `year-only folder name appends zero month and day`() {
        assertEquals("2008-00-00", yearOnlyFolderName(2008))
    }

    @Test
    fun `month folder names zero-pad to two digits`() {
        assertEquals("03", monthFolderName(3))
        assertEquals("12", monthFolderName(12))
    }

    @Test
    fun `year-only month folder is zero month`() {
        assertEquals("00", YEAR_ONLY_MONTH_FOLDER)
    }

    @Test
    fun `year folder names zero-pad to four digits`() {
        assertEquals("0007", yearFolderName(7))
        assertEquals("0007-00-00", yearOnlyFolderName(7))
    }

    @Test
    fun `CaptureDate exposes its four-digit year folder`() {
        assertEquals("2008", CaptureDate(LocalDate.of(2008, 3, 15), DateSource.EXIF).yearFolderName())
    }

    @Test
    fun `CaptureDate exposes its two-digit month folder`() {
        assertEquals("03", CaptureDate(LocalDate.of(2008, 3, 15), DateSource.EXIF).monthFolderName())
    }
}
