## Why

目前照片整理把所有日期資料夾平鋪在來源資料夾底下（`<來源>/2008-03-15/`），年份一多就難以瀏覽；而且只有兩種日期判讀結果——「有完整日期」或「沒日期」，沒日期的照片完全不搬、只留在錯誤清單。使用者希望改成「年份/日期」兩層結構方便依年瀏覽，並把「只抓得到年份」與「完全沒 EXIF 日期」的照片也各自歸位，而不是放著不管。

## What Changes

- **BREAKING** 目標路徑從平的 `<來源>/yyyy-MM-dd/檔名` 改為兩層 `<來源>/yyyy/yyyy-MM-dd/檔名`（年份資料夾 → 日期資料夾）。
- EXIF 日期判讀從兩種狀態擴充為三種：
  - **完整年月日** → `<來源>/2008/2008-03-15/`
  - **只有年份（部分 EXIF）**：EXIF 日期值的月/日為 `00`（例如 `2008:00:00`）但年份可解析 → `<來源>/2008/2008-00-00/`
  - **完全沒有可用 EXIF 日期**（值缺失、無法解析、或全零 `0000:00:00`）→ 統一放進單一的 `<來源>/no-exif/` 資料夾
- 年份來源**僅限**部分 EXIF 日期；不從檔名或原資料夾名抽取年份（明確排除）。
- 沿用既有行為：建立資料夾用 idempotent 的 createDirectories（資料夾已存在就放進去）、同名檔案衝突時跳過該檔並標記失敗、來源檔案不被覆寫。

## Non-Goals (optional)

- 不從檔名、原資料夾名或檔案系統時間戳抽取年份或日期。
- 不調整 EXIF tag 的優先順序（仍是 DateTimeOriginal 優先、DateTimeDigitized 備援）。
- 不改變掃描來源資料夾的規則（仍不遞迴、仍略過點開頭檔案）。
- 不為 `no-exif/` 或 `yyyy-00-00/` 內的照片做進一步分類或去重。

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `photo-organization`: 修改「Extract capture date from EXIF」要支援「只有年份」的部分日期狀態；修改「Compute move plan grouped by capture date」改用兩層 `yyyy/yyyy-MM-dd` 目標路徑、新增 year-only 與 no-exif 的目標規則；修改「Execute move plan」以建立巢狀資料夾並沿用衝突跳過行為。
- `organizer-ui`: 修改「Display file list and error list」，讓 year-only 照片（會被搬到 `yyyy-00-00/`，屬可整理檔案）也顯示在檔案清單並標示其目標資料夾；避免新增第三態後 year-only 照片在掃描清單兩邊都隱形、卻仍被搬移。

## Impact

- Affected specs: `photo-organization`, `organizer-ui`
- Affected code:
  - Modified:
    - src/main/kotlin/org/photocollection/core/ExifReader.kt
    - src/main/kotlin/org/photocollection/core/Models.kt
    - src/main/kotlin/org/photocollection/core/MovePlanner.kt
    - src/main/kotlin/org/photocollection/core/MoveExecutor.kt
    - src/main/kotlin/org/photocollection/ui/App.kt
  - New: (none)
  - Removed: (none)
