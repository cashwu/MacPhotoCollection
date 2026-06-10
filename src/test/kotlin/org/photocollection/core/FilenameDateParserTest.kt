package org.photocollection.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class FilenameDateParserTest {

    // A fixed "today" so the not-in-the-future bound is deterministic; matches the spec's
    // "current date is 2026-06-10" example row.
    private val today = LocalDate.of(2026, 6, 10)

    // spec `photo-organization` — "filename date parsing" example table (all sixteen rows).
    @Test
    fun `parses the spec filename date parsing example table`() {
        val cases: List<Pair<String, LocalDate?>> = listOf(
            "SCR-20251028-jljd.png" to LocalDate.of(2025, 10, 28),
            "Screenshot 2025-10-28 at 10.17.png" to LocalDate.of(2025, 10, 28),
            "IMG_2024_03_15_home.png" to LocalDate.of(2024, 3, 15),
            "pic_19900101.png" to LocalDate.of(1990, 1, 1),
            "shot_2026-06-10.png" to LocalDate.of(2026, 6, 10),
            "12345678.png" to null,
            "IMG_20991231.png" to null,
            "photo_19891231.png" to null,
            "IMG_202510281017.png" to null,
            "920251028.png" to null,
            "IMG_20991231_20251028.png" to LocalDate.of(2025, 10, 28),
            "2025_10_28_20240101.png" to LocalDate.of(2025, 10, 28),
            "12025-10-28.png" to null,
            "shot_2025-10-281.png" to null,
            "mix_2025-10_28.png" to null,
            "random.png" to null,
        )

        for ((fileName, expected) in cases) {
            assertEquals(expected, FilenameDateParser.parse(fileName, today), "for file name '$fileName'")
        }
    }
}
