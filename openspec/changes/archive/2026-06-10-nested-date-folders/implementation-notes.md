<!-- apply-plus implementation notes | change: nested-date-folders | initialized: 2026-06-10 14:10 | no entries below means no deviations or open questions were recorded -->

## 2026-06-10 14:17 — 6.2 手動 GUI 冒煙以自動化測試替代
- 類別：deviation
- 任務：6.2
- 內容：task 6.2 要求在含完整日期、僅年份（`2008:00:00`）、無 EXIF 三類照片的暫存資料夾上實際跑一次 GUI 整理，目視確認「檔案清單」「錯誤清單」與 `<root>/yyyy/yyyy-MM-dd/`、`<root>/yyyy/yyyy-00-00/`、`<root>/no-exif/` 三種磁碟結構。此環境為 headless，無法驅動 `JFileChooser` 互動式 GUI，也無法即時產生帶指定 EXIF 標籤的真實 JPEG。改以等價自動化覆蓋驗證：磁碟三結構與檔案就位由 `MoveExecutorTest.creates nested date and no-exif subfolders and moves files reporting success`（真實 temp 目錄）驗證；檔案清單同時含 full date 與 year-only（顯示 `yyyy-00-00`）、錯誤清單只含 no-date 由 `OrganizerScanMappingTest` 驗證；計畫預覽標籤與三類明細列渲染由 `PlanResultViewTest` 驗證。`./gradlew test` 全綠。
- 原因：互動式 GUI 點擊與真實 EXIF 照片無法在目前 headless 環境執行；6.2 的每項具體斷言已被上述自動化測試逐項覆蓋。仍建議使用者在真機上以真實照片跑一次 GUI 做最終目視確認。

## 2026-06-10 14:22 — App.kt execute() rescan 失敗未清舊狀態（pre-existing，out-of-scope）
- 類別：open-question
- 任務：n/a
- 內容：review loop（Reviewer B）指出 `App.kt` 的 `OrganizerModel.execute()` 在搬移後重掃（rescan）若拋例外，其 `catch` 只更新 `statusMessage`，未像 `scan()` 錯誤路徑那樣清空 `entries`/`datedPhotos`/`undatedPhotos`。理論上使用者之後再按「預覽搬移計畫」會對已搬走的舊清單再次計畫，執行時全部以 `FileAlreadyExistsException` 失敗。此 `catch` block 不在本次 `nested-date-folders` 變更範圍（本次只改 try 內的清單 mapping），本次變更未使其惡化，屬 pre-existing 既有行為。
- 原因：依 Surgical Changes 紀律不在本次順手修不相關的既有問題，交由使用者決定是否另開 change 修正（建議在 `execute()` 的 catch 比照 `scan()` 清空三個清單狀態）。

## 2026-06-10 14:27 — 上述 open-question 已解決（使用者選擇本次一併修）
- 類別：deviation
- 任務：n/a
- 內容：使用者於 review loop 中選擇「本次一併修」。已在 `App.kt` 的 `OrganizerModel.execute()` rescan `catch` block 內，比照 `scan()` 錯誤路徑加入清空 `entries`/`datedPhotos`/`undatedPhotos` 與重置 `selectedIndex`，並保留 `outcomes`（失敗報告仍可見）。`./gradlew test` 全綠。此修正超出原 `nested-date-folders` artifacts 描述的範圍，屬使用者核可的範圍擴張。
- 原因：使用者明確核可於本次 change 內修正此 pre-existing 狀態洩漏；修正為同一方法內、鏡像既有 `scan()` 模式的安全變更。

## 2026-06-10 14:31 — Round 2 補強 execute() catch（plan 清空 + cancellation guard）
- 類別：deviation
- 任務：n/a
- 內容：Round 2 reviewer 指出前一筆修正不完整：execute() catch 清了三個清單但未清 `plan`，理論上 move 階段若拋例外，`plan` 仍非 null 會讓「確認搬移」可對舊 plan 二次執行；另外 catch 新增的清單清空會在協程取消（CancellationException）時也執行，等於讓取消路徑誤清 UI 狀態並顯示誤導訊息。已補：(1) 在 `catch (e: Exception)` 前加 `catch (e: CancellationException) { throw e }` 讓取消正常傳播；(2) 在 catch 內加 `plan = null` 完成 stale-state 對稱清理。`outcomes` 仍保留。`./gradlew test` 全綠。
- 原因：完成使用者核可之 stale-state 修正的完整正確版本；皆在同一 `execute()` 方法內。`scan()` 的同款 `catch (e: Exception)` 吞 CancellationException 屬本次 diff 未觸及的 pre-existing 行，依 Surgical 紀律不擴大處理。

## 2026-06-10 14:38 — Round 3 在 execute() 入口重置 outcomes（對稱 scan()）
- 類別：deviation
- 任務：n/a
- 內容：Round 3 reviewer 指出若 `MoveExecutor.execute()` 自身拋例外（理論上不可達，因其內部全程 `runCatching`），catch 保留的 `outcomes` 會是前一次執行的舊值。為對稱 `scan()` 入口即重置 `outcomes` 的既有模式，已在 `execute()` 進入點（`busy=true` 之後、`scope.launch` 之前）加入 `outcomes = emptyList()`，使任何在新結果指派前發生的失敗都不會殘留舊報告。rescan 失敗情境不受影響（`outcomes` 已於 try 內設為本次結果）。`./gradlew test` 全綠。其餘 Round 3 findings（`folder==null` 死分支的 selectedIndex、空字串 DateTimeOriginal fallback 測試）經評估為不可達或臆測且屬 pre-existing fallback 行，依 Simplicity/Surgical 紀律不處理。
- 原因：以最小且與既有 `scan()` 對稱的一行修正消除 stale-outcomes 根因，避免不必要的內層 try/catch 重構。
