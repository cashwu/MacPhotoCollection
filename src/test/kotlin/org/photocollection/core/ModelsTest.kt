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
}
