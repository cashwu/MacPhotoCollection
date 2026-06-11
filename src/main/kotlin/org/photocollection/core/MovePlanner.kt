package org.photocollection.core

import java.nio.file.Path

object MovePlanner {

    /**
     * Compute a [MovePlan] from date-classified [entries] without touching the file system. Every
     * target is built segment by segment with the path API (not string concatenation) so planning
     * and execution agree on the exact path. A full date targets
     * `<sourceFolder>/<yyyy>/<MM>/<yyyy-MM-dd>/<fileName>`, a year-only result targets
     * `<sourceFolder>/<yyyy>/00/<yyyy-00-00>/<fileName>`, and a no-date file targets
     * `<sourceFolder>/no-exif/<fileName>`. A no-date file is also recorded in [MovePlan.errors] so
     * the UI can show the count of files whose date could not be resolved; it therefore appears both
     * as a move item and in the error list, and the error list means "no resolvable date (still moved
     * to `no-exif/`)", not "not moved".
     */
    fun plan(sourceFolder: Path, entries: List<Pair<PhotoFile, DateResult>>): MovePlan {
        val root = sourceFolder.toAbsolutePath()
        val moves = mutableListOf<MoveItem>()
        val errors = mutableListOf<PhotoFile>()
        for ((photo, result) in entries) {
            val target = when (result) {
                is DateResult.Found ->
                    root
                        .resolve(result.date.yearFolderName())
                        .resolve(result.date.monthFolderName())
                        .resolve(result.date.folderName())
                is DateResult.YearOnly ->
                    root
                        .resolve(yearFolderName(result.year))
                        .resolve(YEAR_ONLY_MONTH_FOLDER)
                        .resolve(yearOnlyFolderName(result.year))
                is DateResult.NoDate -> {
                    errors += photo
                    root.resolve(NO_EXIF_FOLDER)
                }
            }
            moves += MoveItem(photo, target.resolve(photo.fileName), result)
        }
        return MovePlan(moves, errors)
    }
}
