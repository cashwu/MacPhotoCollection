# Apply Plus Review — Round 1
## Reviewer Findings
### Critical
None.

### Warning
- severity: Warning
  confidence: 100
  location: README.md:8
  summary: README 仍寫無法判斷日期的檔案進入錯誤清單且不參與搬移，和 spec 要求 no-date files 仍搬到 shared `no-exif/` 的契約矛盾。
  recommendation: 更新 README bullet，說明 unresolved-date files 會顯示在錯誤清單 / count 中，並搬到 shared `no-exif/`。
  reviewer: A+B

- severity: Warning
  confidence: 90
  location: src/main/kotlin/org/photocollection/ui/App.kt:214
  summary: Plan preview 從 `dateResult` 重建顯示路徑，而不是使用 `move.target` 的實際 target folder，可能讓預覽和 executor 實際使用的 move target 不一致。
  recommendation: 從 `move.target.parent` 的實際 path segments 產生 preview text，並保持 `no-exif` 與 `/` display formatting 符合 spec examples。
  reviewer: B

### Suggestion
None.

## Rating
quality_score: 8
critical_gap: false

Rater 認為 implementation 已通過驗證，但 README 與 no-date/no-exif 行為矛盾，且 plan preview 沒直接從實際 move target 顯示，存在 user-facing consistency 風險。兩者都不是 critical gap，但需要修正後再進下一輪。

## Fix Actions
- 將更新 README.md，讓無法判斷日期的檔案描述符合「錯誤清單可見，仍搬到 `no-exif/`」契約。
- 將更新 src/main/kotlin/org/photocollection/ui/App.kt，讓 plan preview 從 `move.target.parent` 的實際 path segments 顯示 target folder。
- 將重跑 `./gradlew test` 與 `spectra validate add-month-folder-to-photo-layout`。

## Decision
next_round
