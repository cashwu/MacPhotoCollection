package org.photocollection.core

import java.nio.file.Path

object MovePlanner {

    /**
     * Compute a [MovePlan] from EXIF-resolved [entries] without touching the file system. Dated
     * files become move items targeting `<sourceFolder>/<yyyy-MM-dd>/<fileName>` (built with the
     * path API, not string concatenation); undated files go to the error list.
     */
    fun plan(sourceFolder: Path, entries: List<Pair<PhotoFile, DateResult>>): MovePlan {
        val root = sourceFolder.toAbsolutePath()
        val moves = mutableListOf<MoveItem>()
        val errors = mutableListOf<PhotoFile>()
        for ((photo, result) in entries) {
            when (result) {
                is DateResult.Found -> {
                    val target = root.resolve(result.date.folderName()).resolve(photo.fileName)
                    moves += MoveItem(photo, target)
                }
                is DateResult.NoDate -> errors += photo
            }
        }
        return MovePlan(moves, errors)
    }
}
