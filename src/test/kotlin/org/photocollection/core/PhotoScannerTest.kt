package org.photocollection.core

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotoScannerTest {

    private fun tempFolder(): Path = Files.createTempDirectory("scanner-test")

    private fun ScanResult.fileNames(): List<String> =
        (this as ScanResult.Success).photos.map { it.fileName }

    @Test
    fun `returns only supported image files from a mixed folder`() {
        val folder = tempFolder()
        listOf("a.jpg", "b.PNG", "IMG.JPG", "._IMG.JPG", ".DS_Store", "notes.txt")
            .forEach { folder.resolve(it).createFile() }
        folder.resolve("sub").createDirectories()

        val result = PhotoScanner.scan(folder)

        assertEquals(listOf("IMG.JPG", "a.jpg", "b.PNG"), result.fileNames())
    }

    @Test
    fun `returns empty list when folder has no supported images`() {
        val folder = tempFolder()
        listOf("report.pdf", "data.csv").forEach { folder.resolve(it).createFile() }

        val result = PhotoScanner.scan(folder)

        assertEquals(emptyList(), result.fileNames())
    }

    @Test
    fun `reports folder not found when folder does not exist`() {
        val missing = tempFolder().resolve("does-not-exist")

        val result = PhotoScanner.scan(missing)

        assertTrue(result is ScanResult.FolderNotFound)
    }

    @Test
    fun `already-organized folder returns empty list without recursing into date subfolders`() {
        val folder = tempFolder()
        val dateSub = folder.resolve("2024").resolve("03").resolve("2024-03-15").createDirectories()
        dateSub.resolve("IMG_0001.jpg").createFile()

        val result = PhotoScanner.scan(folder)

        assertEquals(emptyList(), result.fileNames())
    }
}
