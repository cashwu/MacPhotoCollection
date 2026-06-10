# Apply Plus Review — Round 1

## Reviewer Findings

兩位 fresh reviewer(A — Adherence、B — Quality)獨立審查,主代理彙整去重並套用信心過濾(drop `confidence < 50`;`[50, 80)` 降為 Suggestion;僅 `≥ 80` 可為 Critical/Warning)。Reviewer A 回報兩個 Suggestion(`confidence` 30、25),Reviewer B 回報五個 Suggestion(`confidence` 55、40、35、30、25)。過濾後僅一筆存活,其餘因 `confidence < 50` 被丟棄(read() 廣捕為刻意的批次安全網;dotfile `.20251028`;兩列 Example 以 seam 間接覆蓋且為 design 測試策略所明示允許;負數月份段落仍落在安全的 year-only 分支;OrganizerModel 內聯 mapping 重複)。

### Critical

無。無任何 `confidence ≥ 80` 的 Critical/Warning 存活。

### Warning

無。

### Suggestion

- severity: Suggestion
- confidence: 55
- location: src/main/kotlin/org/photocollection/core/FilenameDateParser.kt:32(`fileName.substringBeforeLast('.')`)
- summary: 副檔名剝除會把「點號後的日期字串」當成副檔名移除;名為 `shot.20251028`(無真正副檔名)的檔案會解析不到檔名日期而落到 tier 3。
- recommendation: 視為可接受的 edge(真實圖片檔一律帶 jpg/heic/png 等真正副檔名,日期落在 stem 可正常解析);非任何 spec `##### Example:` 列,spec「excluding the extension」字面上也正是此行為。由 Reviewer B 提出。

## Rating

- quality_score: 9.4
- critical_gap: false

七項實作任務全數完成,`./gradlew test` 全綠,`spectra validate add-date-fallback` 通過。經信心過濾後唯一存活者為單一低信心(55)Suggestion,涉及不切實際的檔名(`shot.20251028`,無真正副檔名),不在任何 spec Example 涵蓋範圍內。經驗證 FilenameDateParser.kt:32 行為屬實,但只有當「點號後的日期字串」是唯一的尾段且無真正副檔名時才會發生——真實圖片檔(恆帶 jpg/heic/png 等)不會產生此情形。屬合理的 accepted edge,非 spec 缺口或正確性缺陷,不足以將實作壓在 9 分以下。無任何 `confidence ≥ 80` 的 Critical,故 `critical_gap` 為 false。

## Fix Actions

None; pass condition met.

(存活的單一 Suggestion 為 accepted edge,依 Simplicity First 不做防禦性處理:該情境非 spec/contract 要求,且 `shot.20251028` 這類無真正副檔名的圖片在實務上不存在;修正反而會為不切實際的案例增加複雜度。)

## Decision

passed

通過條件(`quality_score > 9` 且 `critical_gap == false`)於第 1 輪即達成,停止 review loop。
