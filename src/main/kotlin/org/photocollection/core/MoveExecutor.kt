package org.photocollection.core

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files

/** Per-item result of executing a move. [reason] is null on success, a message on failure. */
data class MoveOutcome(val item: MoveItem, val success: Boolean, val reason: String?)

object MoveExecutor {

    /**
     * Execute [plan]'s moves item by item. Each item creates its date subfolder idempotently
     * (`Files.createDirectories`) and moves the file with `Files.move` invoked with no
     * replace-existing option, so an already-occupied target throws and is caught as a conflict.
     * One item's failure never aborts the rest, and a failed item leaves its source file unchanged.
     */
    fun execute(plan: MovePlan): List<MoveOutcome> = plan.moves.map { item ->
        runCatching {
            Files.createDirectories(item.target.parent)
            Files.move(item.source.path, item.target)
        }.fold(
            onSuccess = { MoveOutcome(item, success = true, reason = null) },
            onFailure = { error ->
                val reason = when (error) {
                    is FileAlreadyExistsException -> "目標已存在，跳過以避免覆蓋：${item.target}"
                    else -> error.message ?: error.javaClass.simpleName
                }
                MoveOutcome(item, success = false, reason = reason)
            },
        )
    }
}
