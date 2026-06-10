package org.photocollection.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelsTest {

    @Test
    fun `CaptureDate formats to yyyy-MM-dd`() {
        val date = CaptureDate(LocalDate.of(2024, 3, 15))
        assertEquals("2024-03-15", date.folderName())
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
    fun `year folder names zero-pad to four digits`() {
        assertEquals("0007", yearFolderName(7))
        assertEquals("0007-00-00", yearOnlyFolderName(7))
    }

    @Test
    fun `CaptureDate exposes its four-digit year folder`() {
        assertEquals("2008", CaptureDate(LocalDate.of(2008, 3, 15)).yearFolderName())
    }
}
