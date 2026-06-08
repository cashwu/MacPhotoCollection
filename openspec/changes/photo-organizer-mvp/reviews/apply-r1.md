# Apply Plus Review — Round 1

## Reviewer Findings

### Critical

（無）

### Warning

（confidence filter 後無 Warning 留存；原始兩筆 Warning 因 confidence 落在 [50,80) 已降級為 Suggestion。）

### Suggestion

- severity: Suggestion｜confidence: 70｜reviewer: B
  - location: `src/main/kotlin/org/photocollection/ui/App.kt`（`scan()` / `execute()` 的 `scope.launch` 區塊）
  - summary: launch 區塊僅於結尾設定 `busy = false`，未以 try/finally 保護；若 IO 區塊拋例外（例如使用者選到無權限列舉的資料夾，`PhotoScanner.scan` 拋 `AccessDeniedException`），例外未被攔截逃逸，`busy` 永遠停留 true，導致工具列與計畫按鈕全數被停用。
  - recommendation: 以 try/catch/finally 包住 launch 內容，確保 `busy` 必定被清除，並透過 `statusMessage` 呈現錯誤。

- severity: Suggestion｜confidence: 55｜reviewer: B
  - location: `src/main/kotlin/org/photocollection/core/PhotoScanner.kt:27-38`
  - summary: `PhotoScanner.scan` 未攔截 `Files.newDirectoryStream` 的 `IOException`；存在但無法列舉（權限不足）的資料夾會拋例外而非回傳可區分結果，正是上一筆 stuck-UI 的觸發來源。
  - recommendation: 在系統邊界（UI orchestration）攔截該 IO 失敗並清除 busy／顯示訊息。spec 僅要求區分 folder-not-found 與 empty，故不更動 `PhotoScanner` 既有結果型別，於 UI 邊界處理即可。

（已丟棄：Reviewer B 第三筆「DateTimeDigitized 對 blank 不退回」confidence 45 < 50，且 spec「absent」語意等同 null，依 filter 規則不納入。）

## Rating

- quality_score: 8.3
- critical_gap: false
- rationale: 13 項任務全數完成、17 個單元測試零失敗，核心邏輯（PhotoScanner、ExifReader、MovePlanner、MoveExecutor）皆對照 spec 範例表格驗證；所有 spec 涵蓋路徑（含 folder-not-found 與 empty 的區分）正確，UI 可編譯且能啟動。唯一未決項為兩筆關聯的 Suggestion：使用者選到「存在但無法讀取」資料夾時，未攔截的 IO 例外會使 UI 卡在 busy 狀態。此為真實但狹窄的 UX 缺陷，值得加上 try/finally 防護；惟 permission-denied 並非 spec 指名情境。無 Critical 留存，故無 critical gap；分數因此狹窄邊界扣分但整體為 spec-complete 交付。

## Fix Actions

依未通過（quality_score 8.3 未達 > 9）於本輪修正下列 Suggestion（屬系統邊界＝外部檔案系統輸入的錯誤處理，符合 Surgical/Simplicity 紀律允許範圍）：

- 修改 `src/main/kotlin/org/photocollection/ui/App.kt`：將 `scan()` 與 `execute()` 的 `scope.launch` 內容改以 try/catch/finally 包覆，確保 `busy` 必被清除，IO 例外以 `statusMessage` 呈現。
- 不更動 `PhotoScanner` 的結果型別（避免改動已測試契約與過度設計）；於 UI 邊界統一處理 IO 失敗已足以消除 stuck-UI。

## Decision

next_round
