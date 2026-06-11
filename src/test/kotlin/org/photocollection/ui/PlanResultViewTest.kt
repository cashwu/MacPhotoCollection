package org.photocollection.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.photocollection.core.CaptureDate
import org.photocollection.core.DateResult
import org.photocollection.core.DateSource
import org.photocollection.core.MoveItem
import org.photocollection.core.MoveOutcome
import org.photocollection.core.MovePlan
import org.photocollection.core.PhotoFile
import java.nio.file.Paths
import java.time.LocalDate
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PlanResultViewTest {

    @Test
    fun `shows each move's target folder with its date source and the per-source summary`() = runComposeUiTest {
        val full = PhotoFile(Paths.get("/photos/FULL.jpg"))
        val fullTarget = Paths.get("/photos/2024/03/2024-03-15/FULL.jpg")
        val fullResult = DateResult.Found(CaptureDate(LocalDate.of(2024, 3, 15), DateSource.EXIF))
        val shot = PhotoFile(Paths.get("/photos/SHOT.png"))
        val shotTarget = Paths.get("/photos/2025/10/2025-10-28/SHOT.png")
        val shotResult = DateResult.Found(CaptureDate(LocalDate.of(2025, 10, 28), DateSource.FILENAME))
        val yearOnly = PhotoFile(Paths.get("/photos/YEAR.png"))
        val yearOnlyTarget = Paths.get("/photos/2008/00/2008-00-00/YEAR.png")
        val undated = PhotoFile(Paths.get("/photos/NONE.png"))
        val undatedTarget = Paths.get("/photos/no-exif/NONE.png")
        val plan = MovePlan(
            moves = listOf(
                MoveItem(full, fullTarget, fullResult),
                MoveItem(shot, shotTarget, shotResult),
                MoveItem(yearOnly, yearOnlyTarget, DateResult.YearOnly(2008)),
                MoveItem(undated, undatedTarget, DateResult.NoDate),
            ),
            errors = listOf(undated),
        )
        val outcomes = listOf(MoveOutcome(MoveItem(full, fullTarget, fullResult), success = false, reason = "目標已存在"))

        setContent {
            // Render inside a Column with a weighted sibling, matching how PlanSection sits below
            // the main weighted Row in the real layout.
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f))
                PlanResultView(plan = plan, outcomes = outcomes)
            }
        }

        // Each row shows the relative target folder path and its date source; EXIF and FILENAME
        // differ, the year-only row is marked EXIF, and the no-date row carries no source marker.
        onNodeWithText("FULL.jpg  →  2024/03/2024-03-15  [EXIF]").assertIsDisplayed()
        onNodeWithText("SHOT.png  →  2025/10/2025-10-28  [FILENAME]").assertIsDisplayed()
        onNodeWithText("YEAR.png  →  2008/00/2008-00-00  [EXIF]").assertIsDisplayed()
        onNodeWithText("NONE.png  →  no-exif").assertIsDisplayed()
        // Summary: total moves and the no-date count (new wording), plus per-source counts with the
        // zero-count source still shown.
        onNodeWithText("計畫：4 筆將搬移，其中 1 筆無法判斷日期搬往 no-exif/").assertIsDisplayed()
        onNodeWithText("日期來源：EXIF 2、FILENAME 1、FILE_SYSTEM 0").assertIsDisplayed()
        // Execution summary plus the per-item failure line.
        onNodeWithText("執行結果：成功 0、失敗 1").assertIsDisplayed()
        onNodeWithText("✗ FULL.jpg：目標已存在").assertIsDisplayed()
    }
}
