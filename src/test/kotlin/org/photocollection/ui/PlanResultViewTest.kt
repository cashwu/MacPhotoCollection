package org.photocollection.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.photocollection.core.MoveItem
import org.photocollection.core.MoveOutcome
import org.photocollection.core.MovePlan
import org.photocollection.core.PhotoFile
import java.nio.file.Paths
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PlanResultViewTest {

    @Test
    fun `shows each move's target date folder and the execution summary`() = runComposeUiTest {
        val full = PhotoFile(Paths.get("/photos/FULL.jpg"))
        val fullTarget = Paths.get("/photos/2024/2024-03-15/FULL.jpg")
        val yearOnly = PhotoFile(Paths.get("/photos/YEAR.png"))
        val yearOnlyTarget = Paths.get("/photos/2008/2008-00-00/YEAR.png")
        val undated = PhotoFile(Paths.get("/photos/NONE.png"))
        val undatedTarget = Paths.get("/photos/no-exif/NONE.png")
        val plan = MovePlan(
            moves = listOf(
                MoveItem(full, fullTarget),
                MoveItem(yearOnly, yearOnlyTarget),
                MoveItem(undated, undatedTarget),
            ),
            errors = listOf(undated),
        )
        val outcomes = listOf(MoveOutcome(MoveItem(full, fullTarget), success = false, reason = "目標已存在"))

        setContent {
            // Render inside a Column with a weighted sibling, matching how PlanSection sits below
            // the main weighted Row in the real layout.
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f))
                PlanResultView(plan = plan, outcomes = outcomes)
            }
        }

        // Plan detail lists each file's target leaf folder, including the year-only and no-exif rows.
        onNodeWithText("FULL.jpg  →  2024-03-15").assertIsDisplayed()
        onNodeWithText("YEAR.png  →  2008-00-00").assertIsDisplayed()
        onNodeWithText("NONE.png  →  no-exif").assertIsDisplayed()
        // Non-overlapping label: total moves, of which N have no EXIF date.
        onNodeWithText("計畫：3 筆將搬移，其中 1 筆無 EXIF 日期搬往 no-exif/").assertIsDisplayed()
        // Execution summary plus the per-item failure line.
        onNodeWithText("執行結果：成功 0、失敗 1").assertIsDisplayed()
        onNodeWithText("✗ FULL.jpg：目標已存在").assertIsDisplayed()
    }
}
