package org.photocollection.core

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoveExecutorTest {

    private fun sourceFile(folder: Path, name: String, content: String): PhotoFile {
        val path = folder.resolve(name)
        path.writeText(content)
        return PhotoFile(path)
    }

    // MoveExecutor ignores dateResult; supply a representative EXIF result so each item matches its target.
    private fun exif(year: Int, month: Int, day: Int): DateResult =
        DateResult.Found(CaptureDate(LocalDate.of(year, month, day), DateSource.EXIF))

    // spec — "Successful execution": nested year/date folders and the shared no-exif folder
    @Test
    fun `creates nested date and no-exif subfolders and moves files reporting success`() {
        val folder = Files.createTempDirectory("exec-test")
        val a = sourceFile(folder, "A.jpg", "a-content")
        val b = sourceFile(folder, "B.jpg", "b-content")
        val c = sourceFile(folder, "C.jpg", "c-content")
        val datedTarget = folder.resolve("2024").resolve("2024-03-15").resolve("A.jpg")
        val yearOnlyTarget = folder.resolve("2008").resolve("2008-00-00").resolve("B.jpg")
        val noExifTarget = folder.resolve("no-exif").resolve("C.jpg")
        val plan = MovePlan(
            moves = listOf(
                MoveItem(a, datedTarget, exif(2024, 3, 15)),
                MoveItem(b, yearOnlyTarget, DateResult.YearOnly(2008)),
                MoveItem(c, noExifTarget, DateResult.NoDate),
            ),
            errors = listOf(c),
        )

        val outcomes = MoveExecutor.execute(plan)

        assertTrue(outcomes.all { it.success })
        assertTrue(datedTarget.exists())
        assertTrue(yearOnlyTarget.exists())
        assertTrue(noExifTarget.exists())
        assertFalse(a.path.exists())
        assertFalse(b.path.exists())
        assertFalse(c.path.exists())
        assertEquals("a-content", datedTarget.readText())
    }

    // contract — no-exif/ collision: two undated files share a name in the shared folder (a new
    // collision surface; the flat version never moved undated files so they could not collide).
    // The two sources live in separate subfolders so they can share the file name on disk.
    @Test
    fun `skips same-name collision within the no-exif folder leaving the loser unchanged`() {
        val folder = Files.createTempDirectory("exec-test")
        val winner = sourceFile(folder.resolve("a").createDirectories(), "screenshot.png", "winner-content")
        val loser = sourceFile(folder.resolve("b").createDirectories(), "screenshot.png", "loser-content")
        val sharedTarget = folder.resolve("no-exif").resolve("screenshot.png")
        val plan = MovePlan(
            moves = listOf(
                MoveItem(winner, sharedTarget, DateResult.NoDate),
                MoveItem(loser, sharedTarget, DateResult.NoDate),
            ),
            errors = listOf(winner, loser),
        )

        val outcomes = MoveExecutor.execute(plan)

        assertTrue(outcomes[0].success)
        assertFalse(outcomes[1].success)
        assertEquals("winner-content", sharedTarget.readText())
        assertEquals("loser-content", loser.path.readText())
    }

    // spec — "execution with pre-existing and intra-run conflicts"
    @Test
    fun `skips pre-existing and intra-run conflicts leaving failed sources unchanged`() {
        val folder = Files.createTempDirectory("exec-test")
        val dateFolder = folder.resolve("2024").resolve("2024-03-15").createDirectories()
        dateFolder.resolve("A.jpg").writeText("existing-A")

        val a = sourceFile(folder, "A.jpg", "source-A")
        val b = sourceFile(folder, "B.jpg", "source-B")
        val c = sourceFile(folder, "C.jpg", "source-C")
        val sharedTarget = dateFolder.resolve("SAME.jpg")
        val plan = MovePlan(
            moves = listOf(
                MoveItem(a, dateFolder.resolve("A.jpg"), exif(2024, 3, 15)),
                MoveItem(b, sharedTarget, exif(2024, 3, 15)),
                MoveItem(c, sharedTarget, exif(2024, 3, 15)),
            ),
            errors = emptyList(),
        )

        val outcomes = MoveExecutor.execute(plan)

        // A.jpg fails on the pre-existing target
        assertFalse(outcomes[0].success)
        assertEquals("source-A", a.path.readText())
        assertEquals("existing-A", dateFolder.resolve("A.jpg").readText())
        // B.jpg wins the shared target, C.jpg loses the intra-run conflict
        assertTrue(outcomes[1].success)
        assertFalse(outcomes[2].success)
        assertEquals("source-B", sharedTarget.readText())
        assertEquals("source-C", c.path.readText())
    }

    // contract — target path occupied by a non-directory: item fails, batch continues
    @Test
    fun `marks item failed when date path is occupied by a non-directory without aborting`() {
        val folder = Files.createTempDirectory("exec-test")
        folder.resolve("2024-03-15").writeText("not a directory")

        val blocked = sourceFile(folder, "D.jpg", "d-content")
        val ok = sourceFile(folder, "E.jpg", "e-content")
        val plan = MovePlan(
            moves = listOf(
                MoveItem(blocked, folder.resolve("2024-03-15").resolve("D.jpg"), exif(2024, 3, 15)),
                MoveItem(ok, folder.resolve("2024-03-16").resolve("E.jpg"), exif(2024, 3, 16)),
            ),
            errors = emptyList(),
        )

        val outcomes = MoveExecutor.execute(plan)

        assertFalse(outcomes[0].success)
        assertEquals("d-content", blocked.path.readText())
        assertTrue(outcomes[1].success)
        assertTrue(folder.resolve("2024-03-16").resolve("E.jpg").exists())
    }
}
