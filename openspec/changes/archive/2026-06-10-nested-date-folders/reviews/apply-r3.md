# Apply Plus Review — Round 3

## Reviewer Findings

兩位全新 reviewer 平行獨立執行，重點驗證 Round 2 修正（cancellation guard、`plan` 清空、stale-state 清理）是否正確落地且未引入新問題。

### Critical

（無）

### Warning

（無存活；Reviewer B 的 4 筆「Important」經可達性驗證後皆過濾為 Suggestion，理由見下）

### Suggestion

- severity: Suggestion（Reviewer B 評為 Important，過濾後）
  - confidence: 30（重評；唯一可達 catch 入口為 rescan 失敗，該情境行為正確）
  - location: `src/main/kotlin/org/photocollection/ui/App.kt`，`execute()` catch 與 `statusMessage`
  - summary: 若 `MoveExecutor.execute()` 自身拋例外，catch 保留的 `outcomes` 會是舊值、訊息「搬移後刷新失敗」亦不精確。
  - recommendation: `MoveExecutor.execute()` 內部全程 `runCatching/fold`、結構上不會拋例外，唯一可達 catch 入口是 rescan 失敗（此時 `outcomes` 已是本次正確結果、訊息也準確），故為不可達路徑。仍以最小且對稱 `scan()` 的一行 `outcomes = emptyList()`（execute 入口）關閉 stale-outcomes 根因。提出者: B（F1+F2）。

- severity: Suggestion（Reviewer B 評為 Important，過濾後）
  - confidence: 30
  - location: `src/main/kotlin/org/photocollection/ui/App.kt`，`execute()` 成功路徑 `if (folder != null)` 分支
  - summary: `selectedIndex = -1` 只在 `folder != null` 分支執行，`folder == null` 為結構不一致。
  - recommendation: 該 null 分支在現行邏輯不可達（`plan` 存在即 `sourceFolder` 非 null），catch 路徑亦已重置 `selectedIndex`；屬防禦性死分支，依 Simplicity 紀律不處理。提出者: B（F3）。

- severity: Suggestion（Reviewer B 評為 Important，過濾後）
  - confidence: 25
  - location: `src/test/kotlin/org/photocollection/core/ExifReaderTest.kt` / spec photo-organization Example table
  - summary: 未測試 `DateTimeOriginal = ""`（空字串）搭配非空 `DateTimeDigitized` 的 fallback。
  - recommendation: spec Example「DateTimeOriginal absent」列已被既有 `falls back to DateTimeDigitized when original is absent`（傳 null）覆蓋；library 對缺席 tag 回 `null` 而非 `""`，空字串屬臆測，且 `resolve` 的 `?:` fallback 為本次 diff 未修改的 pre-existing 行。判定臆測+pre-existing，不處理。提出者: B（F4）。

- severity: Suggestion（Reviewer A，文件 nit）
  - confidence: 20
  - location: `openspec/changes/nested-date-folders/implementation-notes.md`
  - summary: 未明記「`finally { busy = false }` 在取消路徑仍執行」是刻意行為。Reviewer A 明述此「不是 bug」。
  - recommendation: 屬資深工程師不會在 review 阻擋的文件吹毛求疵，不處理。提出者: A。

## Rating

- quality_score: 10
- critical_gap: false
- 理由：Rater 親自檢視實際 diff 與測試，確認三輪修正皆正確落地——execute() 入口 `outcomes = emptyList()`（對稱 scan()）、Round 2 的 `catch (CancellationException) { throw e }` 與 catch 內 `plan = null` 均在位，`./gradlew test` 全綠。對過濾後 finding 重做可達性驗證：F1/F2 唯一可達 catch 入口為 rescan 失敗（此時 outcomes 與訊息皆正確），且 `MoveExecutor.execute()` 全程 `runCatching/fold` 結構上不拋例外，stale-outcomes 屬不可達路徑仍以最小對稱修正關閉；F3 為不可達死分支；F4 之空字串臆測缺依據且屬 pre-existing 未改行；Reviewer A 為文件 nit。無未解之實質 in-scope 問題、無 critical gap，達 `quality_score > 9` 且 `critical_gap == false` 通過門檻。

## Fix Actions

- `src/main/kotlin/org/photocollection/ui/App.kt`：在 `execute()` 入口（`busy = true` 之後、`scope.launch` 之前）新增 `outcomes = emptyList()`，對稱 `scan()` 入口行為，消除 stale-outcomes 根因（Reviewer B F1/F2）。`./gradlew test` 全綠。
- `openspec/changes/nested-date-folders/implementation-notes.md`：追加 2026-06-10 14:38 條目記錄此修正與 F3/F4 不處理之理由。
- F3（不可達死分支 selectedIndex）、F4（空字串 fallback 臆測測試）、Reviewer A 文件 nit：依 Simplicity/Surgical 紀律與誤報規則不處理。

## Decision

passed
