package org.photocollection.core

import java.nio.file.Files
import java.nio.file.Path
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

    // spec — "Successful execution"
    @Test
    fun `creates date subfolders and moves files reporting success`() {
        val folder = Files.createTempDirectory("exec-test")
        val a = sourceFile(folder, "A.jpg", "a-content")
        val b = sourceFile(folder, "B.jpg", "b-content")
        val plan = MovePlan(
            moves = listOf(
                MoveItem(a, folder.resolve("2024-03-15").resolve("A.jpg")),
                MoveItem(b, folder.resolve("2024-03-15").resolve("B.jpg")),
            ),
            errors = emptyList(),
        )

        val outcomes = MoveExecutor.execute(plan)

        assertTrue(outcomes.all { it.success })
        assertTrue(folder.resolve("2024-03-15").resolve("A.jpg").exists())
        assertTrue(folder.resolve("2024-03-15").resolve("B.jpg").exists())
        assertFalse(a.path.exists())
        assertFalse(b.path.exists())
        assertEquals("a-content", folder.resolve("2024-03-15").resolve("A.jpg").readText())
    }

    // spec — "execution with pre-existing and intra-run conflicts"
    @Test
    fun `skips pre-existing and intra-run conflicts leaving failed sources unchanged`() {
        val folder = Files.createTempDirectory("exec-test")
        val dateFolder = folder.resolve("2024-03-15").createDirectories()
        dateFolder.resolve("A.jpg").writeText("existing-A")

        val a = sourceFile(folder, "A.jpg", "source-A")
        val b = sourceFile(folder, "B.jpg", "source-B")
        val c = sourceFile(folder, "C.jpg", "source-C")
        val sharedTarget = dateFolder.resolve("SAME.jpg")
        val plan = MovePlan(
            moves = listOf(
                MoveItem(a, dateFolder.resolve("A.jpg")),
                MoveItem(b, sharedTarget),
                MoveItem(c, sharedTarget),
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
                MoveItem(blocked, folder.resolve("2024-03-15").resolve("D.jpg")),
                MoveItem(ok, folder.resolve("2024-03-16").resolve("E.jpg")),
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
