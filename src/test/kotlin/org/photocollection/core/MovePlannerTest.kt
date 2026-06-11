package org.photocollection.core

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import kotlin.io.path.createFile
import kotlin.io.path.listDirectoryEntries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MovePlannerTest {

    private fun dated(
        folder: Path,
        name: String,
        date: LocalDate,
        source: DateSource = DateSource.EXIF,
    ): Pair<PhotoFile, DateResult> =
        PhotoFile(folder.resolve(name)) to DateResult.Found(CaptureDate(date, source))

    private fun yearOnly(folder: Path, name: String, year: Int): Pair<PhotoFile, DateResult> =
        PhotoFile(folder.resolve(name)) to DateResult.YearOnly(year)

    private fun undated(folder: Path, name: String): Pair<PhotoFile, DateResult> =
        PhotoFile(folder.resolve(name)) to DateResult.NoDate

    private fun targetOf(plan: MovePlan, name: String): Path =
        plan.moves.first { it.source.fileName == name }.target

    // spec `photo-organization` — "plan grouping" example, extended with year-only and no-date
    @Test
    fun `full year-only and undated files map to nested year month date and no-exif targets`() {
        val folder = Files.createTempDirectory("planner-test")
        val march15 = LocalDate.of(2024, 3, 15)
        val entries = listOf(
            dated(folder, "IMG1.jpg", march15),
            dated(folder, "IMG2.jpg", march15),
            yearOnly(folder, "IMG3.jpg", 2008),
            undated(folder, "IMG4.jpg"),
        )

        val plan = MovePlanner.plan(folder, entries)

        // Every scanned file receives a move target, built segment by segment from the root.
        assertEquals(
            folder.resolve("2024").resolve("03").resolve("2024-03-15").resolve("IMG1.jpg"),
            targetOf(plan, "IMG1.jpg"),
        )
        assertEquals(
            folder.resolve("2024").resolve("03").resolve("2024-03-15").resolve("IMG2.jpg"),
            targetOf(plan, "IMG2.jpg"),
        )
        assertEquals(
            folder.resolve("2008").resolve("00").resolve("2008-00-00").resolve("IMG3.jpg"),
            targetOf(plan, "IMG3.jpg"),
        )
        assertEquals(
            folder.resolve("no-exif").resolve("IMG4.jpg"),
            targetOf(plan, "IMG4.jpg"),
        )
        // The no-date file appears both as a move item (above) and in the error list.
        assertEquals(listOf("IMG4.jpg"), plan.errors.map { it.fileName })
    }

    // spec `photo-organization` — "plan grouping" example: each move item carries the file's
    // resolved date result and source, regardless of which tier produced it.
    @Test
    fun `each move item carries the date result that produced its target`() {
        val folder = Files.createTempDirectory("planner-test")
        val march15 = LocalDate.of(2024, 3, 15)
        val entries = listOf(
            dated(folder, "IMG1.jpg", march15, DateSource.EXIF),
            dated(folder, "IMG2.jpg", march15, DateSource.FILENAME),
            yearOnly(folder, "IMG3.jpg", 2008),
            undated(folder, "IMG4.jpg"),
        )

        val plan = MovePlanner.plan(folder, entries)

        fun resultOf(name: String): DateResult = plan.moves.first { it.source.fileName == name }.dateResult
        assertEquals(DateResult.Found(CaptureDate(march15, DateSource.EXIF)), resultOf("IMG1.jpg"))
        assertEquals(DateResult.Found(CaptureDate(march15, DateSource.FILENAME)), resultOf("IMG2.jpg"))
        assertEquals(DateResult.YearOnly(2008), resultOf("IMG3.jpg"))
        assertEquals(DateResult.NoDate, resultOf("IMG4.jpg"))
    }

    @Test
    fun `target paths are absolute even when the source folder is relative`() {
        val relativeFolder = Paths.get("photos")
        val entries = listOf(
            dated(relativeFolder, "IMG1.jpg", LocalDate.of(2024, 3, 15)),
            yearOnly(relativeFolder, "IMG2.jpg", 2008),
            undated(relativeFolder, "IMG3.jpg"),
        )

        val plan = MovePlanner.plan(relativeFolder, entries)

        val root = relativeFolder.toAbsolutePath()
        assertTrue(plan.moves.all { it.target.isAbsolute }, "every target must be an absolute path")
        assertEquals(
            root.resolve("2024").resolve("03").resolve("2024-03-15").resolve("IMG1.jpg"),
            targetOf(plan, "IMG1.jpg"),
        )
        assertEquals(
            root.resolve("2008").resolve("00").resolve("2008-00-00").resolve("IMG2.jpg"),
            targetOf(plan, "IMG2.jpg"),
        )
        assertEquals(root.resolve("no-exif").resolve("IMG3.jpg"), targetOf(plan, "IMG3.jpg"))
    }

    @Test
    fun `planning does not change the file system`() {
        val folder = Files.createTempDirectory("planner-test")
        val img = folder.resolve("IMG1.jpg").createFile()
        val before = folder.listDirectoryEntries().map { it.fileName.toString() }.toSet()

        MovePlanner.plan(folder, listOf(PhotoFile(img) to DateResult.Found(CaptureDate(LocalDate.of(2024, 3, 15), DateSource.EXIF))))

        val after = folder.listDirectoryEntries().map { it.fileName.toString() }.toSet()
        assertEquals(before, after)
    }
}
