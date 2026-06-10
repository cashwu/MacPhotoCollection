# Apply Plus Review — Round 2

## Reviewer Findings

兩位全新 reviewer 平行獨立執行，重點複查 Round 1 後依使用者核可新增的 `execute()` catch 狀態清理修正。

### Critical

（無）

### Warning

- severity: Warning
  - confidence: 85
  - location: `src/main/kotlin/org/photocollection/ui/App.kt`，`OrganizerModel.execute()` catch block
  - summary: Round 1 的 stale-state 修正不完整——catch 清空了 `entries`/`datedPhotos`/`undatedPhotos`，卻未清空 `plan`。move 階段若拋例外，`plan` 仍為非 null，「確認搬移」按鈕（啟用條件 `plan?.moves?.isNotEmpty() == true`）會重新啟用，使用者可對已部分搬移的舊 plan 二次執行。
  - recommendation: 在 catch 內補 `plan = null`，與 `scan()` 入口即 `plan = null` 的狀態對稱，完整關閉 stale-state 二次計畫/執行的路徑。
  - 提出者: A

- severity: Warning
  - confidence: 83
  - location: `src/main/kotlin/org/photocollection/ui/App.kt`，`OrganizerModel.execute()` catch block
  - summary: `catch (e: Exception)` 會一併捕獲 `CancellationException`（Kotlin coroutines 反模式），不 re-throw；且 Round 1 在此 catch 新增的清單清空會在協程取消時被誤執行，誤清 UI 狀態並顯示誤導性的「搬移後刷新失敗」訊息。
  - recommendation: 在 `catch (e: Exception)` 之前加 `catch (e: CancellationException) { throw e }`，讓取消正常傳播、不誤清狀態。`scan()` 的同款 `catch (e: Exception)` 屬本次 diff 未觸及的 pre-existing 行，不在本次處理範圍。
  - 提出者: B

### Suggestion

- severity: Suggestion（Reviewer A 評為 Warning，依附 F1 過濾為 Suggestion）
  - confidence: 60
  - location: `openspec/changes/nested-date-folders/implementation-notes.md`，最後一筆 deviation
  - summary: implementation-notes 記載 catch 修正「保留 outcomes」但未提及 `plan` 未清空，文件與程式狀態不完全同步。
  - recommendation: 修正 Warning F1（補 `plan = null`）後，於 implementation-notes 追加條目說明 `plan` 亦清空與 cancellation guard。
  - 提出者: A

## Rating

- quality_score: 6
- critical_gap: false
- 理由：本輪兩筆存活 Warning 皆為真實且在範圍內，指向 Round 1 stale-state 修正本身的不完整與副作用，而非全盤問題，故扣分集中於該修正品質。Reviewer A F1（conf 85）的漏清 `plan` 屬使用者核可之 stale-state 修正應涵蓋的完整範圍，具明確誤操作風險；Reviewer B F1（conf 83）的 `CancellationException` 吞噬雖源於 pre-existing catch 子句，但本輪新增行使取消路徑變差，屬本次 diff 觸及而值得修。兩者皆 Warning、無 Critical，`critical_gap` 為 false；但存在在範圍內、未解的實質問題，未達 `> 9` 通過門檻，評 6 分，next_round。Adherence 其餘各項（三態 EXIF、三向路徑、Models 命名、MoveExecutor 僅 KDoc、UI 映射與標籤、測試覆蓋）reviewer 均確認與 artifacts 一致、無 finding。

## Fix Actions

- `src/main/kotlin/org/photocollection/ui/App.kt`：在 `execute()` 的 `catch (e: Exception)` 之前新增 `catch (e: CancellationException) { throw e }`（並 import `kotlinx.coroutines.CancellationException`），讓協程取消正常傳播；在 `catch (e: Exception)` 內新增 `plan = null`，完成 stale-state 對稱清理。原因：完整關閉 Reviewer A F1 的二次執行路徑與 Reviewer B F1 的取消誤清狀態。`./gradlew test` 全綠。
- `openspec/changes/nested-date-folders/implementation-notes.md`：追加 2026-06-10 14:31 條目，記錄 `plan` 清空與 cancellation guard 的補強，解決 Suggestion 的文件同步。
- `scan()` 的同款 `CancellationException` 吞噬：本次 diff 未觸及之 pre-existing 行，依 Surgical 紀律不擴大處理，未列入 fix。

## Decision

next_round
