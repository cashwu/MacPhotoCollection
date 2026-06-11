## Summary

調整照片整理後的日期資料夾結構，在年份資料夾和日期資料夾之間加入月份資料夾。

## Motivation

目前同一年份下所有日期資料夾都直接平鋪在 `yyyy/` 下面，年份資料夾照片量變大時不利於瀏覽。加入月份層可讓使用者更快定位某個月份，同時保留既有 `yyyy-MM-dd` 與 `yyyy-00-00` 日期資料夾命名。

## Proposed Solution

- 完整日期照片的目標路徑從 `yyyy/yyyy-MM-dd/<file>` 改為 `yyyy/MM/yyyy-MM-dd/<file>`，例如 `2019/02/2019-02-04/IMG.jpg`。
- year-only 照片也使用一致的三層結構，目標路徑從 `yyyy/yyyy-00-00/<file>` 改為 `yyyy/00/yyyy-00-00/<file>`。
- 無法判斷日期的照片仍放在 shared `no-exif/`，不建立年份或月份資料夾。
- plan preview 和掃描列表需要顯示足以辨識新目標資料夾的相對資料夾路徑，避免只顯示最後一層時看不到月份分組。

## Non-Goals

- 不新增使用者可切換的 layout 設定。
- 不改變日期來源 fallback chain、EXIF 解析、檔名日期解析或檔案系統日期解析規則。
- 不改變 `no-exif/` 的 shared folder 行為。
- 不遞迴掃描已整理完成的子資料夾。

## Alternatives Considered

- 只讓完整日期照片加月份層，year-only 維持 `yyyy/yyyy-00-00/`：會造成可整理照片的目標深度不一致，使用者已決定「都用一樣的規則」。
- 將日期資料夾改名成 `yyyy-MMdd`：這是原始討論中的誤植，已確認日期資料夾應保留現有 `yyyy-MM-dd` 格式。

## Impact

- Affected specs: photo-organization, organizer-ui
- Affected code:
  - Modified: src/main/kotlin/org/photocollection/core/Models.kt
  - Modified: src/main/kotlin/org/photocollection/core/MovePlanner.kt
  - Modified: src/main/kotlin/org/photocollection/core/MoveExecutor.kt
  - Modified: src/main/kotlin/org/photocollection/ui/App.kt
  - Modified: src/test/kotlin/org/photocollection/core/ModelsTest.kt
  - Modified: src/test/kotlin/org/photocollection/core/MovePlannerTest.kt
  - Modified: src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt
  - Modified: src/test/kotlin/org/photocollection/ui/OrganizerScanMappingTest.kt
  - Modified: src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt
  - New: none
  - Removed: none
