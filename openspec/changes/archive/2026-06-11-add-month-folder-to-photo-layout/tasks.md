## 1. Core Layout Contract

- [x] 1.1 依照設計決策 Keep `MovePlanner` as the layout owner，讓 `Compute move plan grouped by capture date` 產生 full-date target parent `yyyy/MM/yyyy-MM-dd`、year-only target parent `yyyy/00/yyyy-00-00`、no-date target parent `no-exif`；以 `src/test/kotlin/org/photocollection/core/MovePlannerTest.kt` 驗證 full-date、year-only、no-date 的絕對 target paths。
- [x] 1.2 依照設計決策 Preserve date folder names and use `00` for year-only month，補齊月份 folder formatting helper 或等價集中邏輯，確保 full-date 月份為 two-digit `01` through `12`、year-only 月份為 `00`，且日期 folder name 仍為 `yyyy-MM-dd` / `yyyy-00-00`；以 `src/test/kotlin/org/photocollection/core/ModelsTest.kt` 和 `src/test/kotlin/org/photocollection/core/MovePlannerTest.kt` 驗證。
- [x] 1.3 依照設計決策 Keep `MovePlanner` as the layout owner 和 Preserve date folder names and use `00` for year-only month，讓 `Execute move plan` 使用新增月份層後的 target parent 建立所有 missing directories，且維持 no-replace move conflict behavior、不覆蓋既有檔案；以 `src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt` 驗證 successful execution、pre-existing conflict、same-run conflict。

## 2. UI Display Contract

- [x] 2.1 依照設計決策 Show relative target folder paths in UI previews，讓 `Display file list and error list` 對 full-date 顯示 `yyyy/MM/yyyy-MM-dd`，對 year-only 顯示 `yyyy/00/yyyy-00-00`，no-date 仍只出現在 no-date list；以 `src/test/kotlin/org/photocollection/ui/OrganizerScanMappingTest.kt` 驗證 scan mapping 顯示字串。
- [x] 2.2 依照設計決策 Show relative target folder paths in UI previews，讓 `Preview move plan before execution` 顯示相對於 source folder 的 target folder path，full-date 例如 `2024/03/2024-03-15`，year-only 例如 `2008/00/2008-00-00`，no-date 顯示 `no-exif`，且仍顯示 date source marker；以 `src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt` 驗證 preview 文案。

## 3. Verification

- [x] 3.1 依照 `Compute move plan grouped by capture date`、`Execute move plan`、`Display file list and error list`、`Preview move plan before execution` 和設計決策 Show relative target folder paths in UI previews，更新受影響的 README 或註解文字，使文件描述與 `yyyy/MM/yyyy-MM-dd`、`yyyy/00/yyyy-00-00`、`no-exif` contract 一致；以 content review 驗證不再留下二層 layout 的錯誤描述。
- [x] 3.2 執行 `./gradlew test`，確認 `Compute move plan grouped by capture date`、`Execute move plan`、`Display file list and error list`、`Preview move plan before execution` 相關測試全部通過，並以 `spectra validate add-month-folder-to-photo-layout` 驗證 proposal、design、specs、tasks 的 artifact contract 仍有效。
