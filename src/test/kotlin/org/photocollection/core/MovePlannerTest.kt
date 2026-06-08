package org.photocollection.core

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.createFile
import kotlin.io.path.listDirectoryEntries
import kotlin.test.Test
import kotlin.test.assertEquals

class MovePlannerTest {

    private fun dated(folder: Path, name: String, date: LocalDate): Pair<PhotoFile, DateResult> =
        PhotoFile(folder.resolve(name)) to DateResult.Found(CaptureDate(date))

    private fun undated(folder: Path, name: String): Pair<PhotoFile, DateResult> =
        PhotoFile(folder.resolve(name)) to DateResult.NoDate

    // spec `photo-organization` — "plan grouping" example
    @Test
    fun `dated files target their date folder and undated files go to the error list`() {
        val folder = Files.createTempDirectory("planner-test")
        val march15 = LocalDate.of(2024, 3, 15)
        val entries = listOf(
            dated(folder, "IMG1.jpg", march15),
            dated(folder, "IMG2.jpg", march15),
            undated(folder, "IMG3.jpg"),
        )

        val plan = MovePlanner.plan(folder, entries)

        assertEquals(
            listOf("IMG1.jpg", "IMG2.jpg"),
            plan.moves.map { it.source.fileName },
        )
        assertEquals(
            folder.resolve("2024-03-15").resolve("IMG1.jpg"),
            plan.moves[0].target,
        )
        assertEquals(
            folder.resolve("2024-03-15").resolve("IMG2.jpg"),
            plan.moves[1].target,
        )
        assertEquals(listOf("IMG3.jpg"), plan.errors.map { it.fileName })
    }

    @Test
    fun `planning does not change the file system`() {
        val folder = Files.createTempDirectory("planner-test")
        val img = folder.resolve("IMG1.jpg").createFile()
        val before = folder.listDirectoryEntries().map { it.fileName.toString() }.toSet()

        MovePlanner.plan(folder, listOf(PhotoFile(img) to DateResult.Found(CaptureDate(LocalDate.of(2024, 3, 15)))))

        val after = folder.listDirectoryEntries().map { it.fileName.toString() }.toSet()
        assertEquals(before, after)
    }
}
