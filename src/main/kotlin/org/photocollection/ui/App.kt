package org.photocollection.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import org.photocollection.core.DateResult
import org.photocollection.core.DateSource
import org.photocollection.core.ExifReader
import org.photocollection.core.MoveExecutor
import org.photocollection.core.MoveItem
import org.photocollection.core.MoveOutcome
import org.photocollection.core.MovePlan
import org.photocollection.core.MovePlanner
import org.photocollection.core.PhotoFile
import org.photocollection.core.PhotoScanner
import org.photocollection.core.ScanResult
import org.photocollection.core.YEAR_ONLY_MONTH_FOLDER
import org.photocollection.core.yearFolderName
import org.photocollection.core.yearOnlyFolderName
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JFileChooser

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val model = remember { OrganizerModel(scope) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Toolbar(model)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    PhotoListPane(
                        title = "檔案清單 (${model.datedPhotos.size})",
                        names = model.datedPhotos.map { "${it.first.fileName}  →  ${it.second}" },
                        selectedIndex = model.selectedIndex,
                        onSelect = { model.select(it) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    PhotoListPane(
                        title = "錯誤清單 (${model.undatedPhotos.size})",
                        names = model.undatedPhotos.map { it.fileName },
                        selectedIndex = -1,
                        onSelect = {},
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    PreviewPane(
                        photo = model.selectedPhoto,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                PlanSection(model)
            }
        }
    }
}

@Composable
private fun Toolbar(model: OrganizerModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { model.chooseFolder() }, enabled = !model.busy) {
            Text("選擇資料夾")
        }
        Text(
            text = model.sourceFolder?.toString() ?: "尚未選擇資料夾",
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        if (model.busy) {
            CircularProgressIndicator(modifier = Modifier.width(24.dp))
            Text(model.statusMessage ?: "處理中…", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun PhotoListPane(
    title: String,
    names: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(end = 8.dp)) {
        Text(title, style = MaterialTheme.typography.subtitle1)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(names.size) { index ->
                val background = if (index == selectedIndex) Color(0xFFD0E4FF) else Color.Transparent
                Text(
                    text = names[index],
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(index) }
                        .background(background)
                        .padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun PreviewPane(photo: PhotoFile?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("預覽", style = MaterialTheme.typography.subtitle1)
        Box(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)).background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center,
        ) {
            if (photo == null) {
                Text("選取左側檔案以預覽")
                return@Box
            }
            val bitmap by produceState<ImageBitmap?>(initialValue = null, photo) {
                value = loadPreview(photo.path)
            }
            val image = bitmap
            if (image != null) {
                androidx.compose.foundation.Image(
                    bitmap = image,
                    contentDescription = photo.fileName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                )
            } else {
                Text("預覽不可用")
            }
        }
    }
}

@Composable
private fun PlanSection(model: OrganizerModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { model.previewPlan() },
                enabled = !model.busy && model.sourceFolder != null,
            ) { Text("預覽搬移計畫") }
            Button(
                onClick = { model.execute() },
                enabled = !model.busy && (model.plan?.moves?.isNotEmpty() == true),
            ) { Text("確認搬移") }
        }
        PlanResultView(plan = model.plan, outcomes = model.outcomes)
    }
}

/**
 * Renders the plan detail and execution-result lists. Each scrolling list is capped with
 * `heightIn(max)` so a large plan or failure set scrolls within a bounded area instead of
 * growing tall enough to squeeze the file/preview panes above it. Kept as an internal,
 * data-only composable so its rendering can be covered by a UI test.
 */
@Composable
internal fun PlanResultView(plan: MovePlan?, outcomes: List<MoveOutcome>) {
    plan?.let {
        Text(
            "計畫：${plan.moves.size} 筆將搬移，其中 ${plan.errors.size} 筆無法判斷日期搬往 no-exif/",
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            "日期來源：" + DateSource.values().joinToString("、") { source ->
                "$source ${plan.moves.count { dateSourceOf(it.dateResult) == source }}"
            },
            modifier = Modifier.padding(top = 4.dp),
        )
        if (plan.moves.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).padding(top = 4.dp)) {
                items(plan.moves) { move ->
                    val source = dateSourceOf(move.dateResult)
                    val marker = if (source != null) "  [$source]" else ""
                    Text("${move.source.fileName}  →  ${moveTargetFolder(move)}$marker")
                }
            }
        }
    }
    if (outcomes.isNotEmpty()) {
        val succeeded = outcomes.count { it.success }
        val failed = outcomes.size - succeeded
        Text("執行結果：成功 $succeeded、失敗 $failed", modifier = Modifier.padding(top = 8.dp))
        val failures = outcomes.filter { !it.success }
        if (failures.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).padding(top = 4.dp)) {
                items(failures) { outcome ->
                    Text(
                        "✗ ${outcome.item.source.fileName}：${outcome.reason}",
                        color = Color(0xFFB00020),
                    )
                }
            }
        }
    }
}

/** Decode an image for preview off the UI thread; returns null when the format cannot be decoded. */
private suspend fun loadPreview(path: Path): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        SkiaImage.makeFromEncoded(Files.readAllBytes(path)).toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}

/**
 * The target folder path for an organizable file — a full date shows `yyyy/MM/yyyy-MM-dd` and a
 * year-only result shows `yyyy/00/yyyy-00-00` — or null for a no-date file, which belongs in the
 * error list rather than the organizable file list. Both organizable outcomes are moved, so neither
 * may disappear from the displayed lists. Kept internal so the scan mapping is directly testable
 * without the private [OrganizerModel].
 */
internal fun organizableTargetFolder(result: DateResult): String? = when (result) {
    is DateResult.Found -> "${result.date.yearFolderName()}/${result.date.monthFolderName()}/${result.date.folderName()}"
    is DateResult.YearOnly -> "${yearFolderName(result.year)}/$YEAR_ONLY_MONTH_FOLDER/${yearOnlyFolderName(result.year)}"
    is DateResult.NoDate -> null
}

/** The target folder path to show for any planned move, derived from the actual planned target. */
internal fun moveTargetFolder(move: MoveItem): String {
    val parent = move.target.parent
    return when (move.dateResult) {
        is DateResult.Found, is DateResult.YearOnly -> listOf(
            parent.parent.parent.fileName,
            parent.parent.fileName,
            parent.fileName,
        ).joinToString("/") { it.toString() }
        is DateResult.NoDate -> parent.fileName.toString()
    }
}

/**
 * The date source to show beside a planned move in the preview — the tier that produced its date.
 * A year-only result is only produced by EXIF; a no-date file has no source (it goes to `no-exif/`).
 */
internal fun dateSourceOf(result: DateResult): DateSource? = when (result) {
    is DateResult.Found -> result.date.source
    is DateResult.YearOnly -> DateSource.EXIF
    is DateResult.NoDate -> null
}

/** Holds organizer UI state and runs scan / plan / execute off the UI thread. */
private class OrganizerModel(private val scope: CoroutineScope) {
    var sourceFolder by mutableStateOf<Path?>(null)
        private set
    var datedPhotos by mutableStateOf<List<Pair<PhotoFile, String>>>(emptyList())
        private set
    var undatedPhotos by mutableStateOf<List<PhotoFile>>(emptyList())
        private set
    var selectedIndex by mutableStateOf(-1)
        private set
    var plan by mutableStateOf<MovePlan?>(null)
        private set
    var outcomes by mutableStateOf<List<MoveOutcome>>(emptyList())
        private set
    var busy by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set

    private var entries: List<Pair<PhotoFile, DateResult>> = emptyList()

    val selectedPhoto: PhotoFile?
        get() = datedPhotos.getOrNull(selectedIndex)?.first

    fun select(index: Int) {
        selectedIndex = index
    }

    fun chooseFolder() {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "選擇來源資料夾"
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            scan(chooser.selectedFile.toPath())
        }
    }

    private fun scan(folder: Path) {
        busy = true
        statusMessage = "掃描中…"
        plan = null
        outcomes = emptyList()
        selectedIndex = -1
        scope.launch {
            try {
                val scanned = withContext(Dispatchers.IO) {
                    when (val result = PhotoScanner.scan(folder)) {
                        is ScanResult.FolderNotFound -> null
                        is ScanResult.Success -> ExifReader.readAll(result.photos)
                    }
                }
                sourceFolder = folder
                if (scanned == null) {
                    entries = emptyList()
                    datedPhotos = emptyList()
                    undatedPhotos = emptyList()
                    statusMessage = "找不到資料夾"
                } else {
                    entries = scanned
                    datedPhotos = scanned.mapNotNull { (photo, result) ->
                        organizableTargetFolder(result)?.let { photo to it }
                    }
                    undatedPhotos = scanned.filter { it.second is DateResult.NoDate }.map { it.first }
                    statusMessage = null
                }
            } catch (e: Exception) {
                // Drop any prior folder's results so a failed scan leaves no stale, executable state.
                sourceFolder = folder
                entries = emptyList()
                datedPhotos = emptyList()
                undatedPhotos = emptyList()
                statusMessage = "掃描失敗：${e.message ?: e.javaClass.simpleName}"
            } finally {
                busy = false
            }
        }
    }

    fun previewPlan() {
        val folder = sourceFolder ?: return
        plan = MovePlanner.plan(folder, entries)
        outcomes = emptyList()
    }

    fun execute() {
        val current = plan ?: return
        busy = true
        statusMessage = "搬移中…"
        // Clear any prior run's report up front (mirrors scan()), so a failure before the new
        // results are assigned can't leave a stale success/failure report on screen.
        outcomes = emptyList()
        scope.launch {
            try {
                val results = withContext(Dispatchers.IO) { MoveExecutor.execute(current) }
                outcomes = results
                plan = null
                // Refresh from disk so moved items disappear and cannot be executed again; the
                // failure report above remains visible because `outcomes` is not cleared.
                val folder = sourceFolder
                if (folder != null) {
                    val rescanned = withContext(Dispatchers.IO) {
                        when (val result = PhotoScanner.scan(folder)) {
                            is ScanResult.FolderNotFound -> emptyList()
                            is ScanResult.Success -> ExifReader.readAll(result.photos)
                        }
                    }
                    entries = rescanned
                    datedPhotos = rescanned.mapNotNull { (photo, result) ->
                        organizableTargetFolder(result)?.let { photo to it }
                    }
                    undatedPhotos = rescanned.filter { it.second is DateResult.NoDate }.map { it.first }
                    selectedIndex = -1
                }
                statusMessage = null
            } catch (e: CancellationException) {
                // Let coroutine cancellation propagate instead of swallowing it and wiping state.
                throw e
            } catch (e: Exception) {
                // Drop the pre-move lists and plan so a failed rescan can't leave stale state that
                // would re-plan or re-execute already-moved files; the failure report stays visible
                // (`outcomes` kept).
                plan = null
                entries = emptyList()
                datedPhotos = emptyList()
                undatedPhotos = emptyList()
                selectedIndex = -1
                statusMessage = "搬移後刷新失敗：${e.message ?: e.javaClass.simpleName}"
            } finally {
                busy = false
            }
        }
    }
}
