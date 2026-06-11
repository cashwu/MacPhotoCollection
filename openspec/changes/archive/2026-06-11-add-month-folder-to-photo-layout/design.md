## Context

目前照片整理流程已經把日期解析、move plan 計算、move execution 和 Compose UI 預覽分開。`MovePlanner` 負責用 `Path.resolve` 組出絕對目標路徑，`MoveExecutor` 只依照 plan 建立 parent directories 並搬移檔案，UI 則顯示掃描後的目標資料夾與確認前的 move plan。

這次需求只改 date-based folder layout：在年份資料夾與日期資料夾中間加入月份資料夾。完整日期保留既有 `yyyy-MM-dd` 日期資料夾命名；year-only 使用 `00` 作為月份 segment 並保留 `yyyy-00-00` 日期資料夾；無日期檔案仍使用 shared `no-exif/`。

## Goals / Non-Goals

**Goals:**

- 讓完整日期照片搬到 `yyyy/MM/yyyy-MM-dd/<file>`。
- 讓 year-only 照片搬到 `yyyy/00/yyyy-00-00/<file>`。
- 讓 `MovePlanner`、`MoveExecutor`、scan list、plan preview 和測試都使用同一個目標 layout 契約。
- 讓 UI 顯示足以辨識月份層的相對目標資料夾，而不是只顯示最後一層日期資料夾。

**Non-Goals:**

- 不新增 layout preference、設定檔或 migration command。
- 不改變 EXIF、filename、file-system date fallback chain。
- 不改變 `no-exif/` 分類與衝突處理。
- 不改變 non-recursive scan 行為。

## Decisions

### Keep `MovePlanner` as the layout owner

`MovePlanner` 仍是唯一負責把 date-classified entries 轉成 move target paths 的 runtime 邊界。實作應在現有 `Models.kt` helper 裡補上月份 segment 的格式化能力，並讓 `MovePlanner.plan()` 逐段 resolve `yyyy`、`MM`、`yyyy-MM-dd` 或 `yyyy`、`00`、`yyyy-00-00`。

替代方案是新增 layout strategy 或設定物件，但這次沒有使用者可切換 layout 的需求；新增抽象會增加測試面和狀態傳遞，沒有相對收益。

### Preserve date folder names and use `00` for year-only month

完整日期的最後一層 folder name 保留 `yyyy-MM-dd`，year-only 的最後一層保留 `yyyy-00-00`。中間月份層對完整日期使用 two-digit month，對 year-only 使用 `00`。

替代方案是把日期 folder 改成 `yyyy-MMdd`，但那是討論中的誤植，且會破壞既有日期資料夾命名。另一個替代方案是 year-only 維持二層，使用者已明確要求「都用一樣的規則」。

### Show relative target folder paths in UI previews

scan list 和 plan preview 應顯示可辨識月份分組的 target folder path，例如 `2024/03/2024-03-15` 與 `2008/00/2008-00-00`。`no-exif` 仍顯示 `no-exif`。

替代方案是維持只顯示最後一層 folder name，但加入月份層後使用者會看不到檔案將進入哪個月份資料夾，預覽資訊不足。

## Implementation Contract

- Behavior: 完整日期照片的 move target parent SHALL be `source/yyyy/MM/yyyy-MM-dd`；year-only 照片的 move target parent SHALL be `source/yyyy/00/yyyy-00-00`；no-date 照片的 move target parent SHALL remain `source/no-exif`。
- Interface / data shape: 不新增 public command、IPC contract 或外部設定。現有 `MovePlan`、`MoveItem`、`DateResult` data shape 維持不變；變更只反映在 target `Path` 值與 UI display strings。
- Failure modes: `MoveExecutor` 仍以 `Files.createDirectories(item.target.parent)` 建立所有 missing parent directories，並以 no-replace `Files.move` 保留既有 conflict behavior。加入月份層不得新增 pre-check 或覆蓋行為。
- Acceptance criteria: `MovePlanner` tests 覆蓋 full-date、year-only、no-date target paths；`MoveExecutor` tests 覆蓋三層 parent directory 建立與既有 conflict behavior；UI mapping / preview tests 覆蓋 `yyyy/MM/yyyy-MM-dd`、`yyyy/00/yyyy-00-00` 和 `no-exif` 顯示；`./gradlew test` 通過。
- Scope boundaries: 實作只能調整 date-based layout 與相關顯示、註解和測試期望；不得改變 date resolution priority、supported extensions、scan recursion、或 `no-exif/` behavior。

## Risks / Trade-offs

- [Risk] UI 若只顯示 `target.parent.fileName`，新增月份層後仍只看到最後一層日期資料夾。→ Mitigation: UI display helper 需產生相對於 source folder 的 target parent path，或使用等價方式顯示完整 target folder path segment。
- [Risk] year-only 使用 `00` 月份可能讓月份資料夾包含非真實月份。→ Mitigation: `00` 與既有 `yyyy-00-00` 語意一致，明確代表月份未知。
- [Risk] 既有測試和 README 仍描述二層 layout。→ Mitigation: apply 階段更新受影響測試與必要文件文字；runtime contract 以 specs 為準。
