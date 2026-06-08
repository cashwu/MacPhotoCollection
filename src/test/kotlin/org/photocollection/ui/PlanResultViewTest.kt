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
        val source = PhotoFile(Paths.get("/photos/IMG1.jpg"))
        val target = Paths.get("/photos/2024-03-15/IMG1.jpg")
        val plan = MovePlan(moves = listOf(MoveItem(source, target)), errors = emptyList())
        val outcomes = listOf(MoveOutcome(MoveItem(source, target), success = false, reason = "目標已存在"))

        setContent {
            // Render inside a Column with a weighted sibling, matching how PlanSection sits below
            // the main weighted Row in the real layout.
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f))
                PlanResultView(plan = plan, outcomes = outcomes)
            }
        }

        // Plan detail lists each file's target date folder (P2① from review).
        onNodeWithText("IMG1.jpg  →  2024-03-15").assertIsDisplayed()
        onNodeWithText("計畫：1 筆將搬移，0 筆無日期").assertIsDisplayed()
        // Execution summary plus the per-item failure line.
        onNodeWithText("執行結果：成功 0、失敗 1").assertIsDisplayed()
        onNodeWithText("✗ IMG1.jpg：目標已存在").assertIsDisplayed()
    }
}
