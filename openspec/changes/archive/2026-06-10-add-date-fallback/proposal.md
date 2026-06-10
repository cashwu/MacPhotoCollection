## Why

目前系統只用 EXIF(`DateTimeOriginal` / `DateTimeDigitized`)判斷照片日期,截圖、通訊軟體存檔等無 EXIF 的圖片(典型 PNG/GIF)一律落入 `no-exif/` 並進入 error list,無法分到真正的日期資料夾。這類檔案其實帶有可用的日期訊號——檔名中的日期樣式(如 `SCR-20251028-jljd.png`)與檔案系統日期(建立/修改日期)——使用者希望能自動利用這些訊號完成歸檔。

## What Changes

- EXIF 無可用日期時,改以三層備援接續判斷(在現有的「年份/日期巢狀資料夾 + year-only + no-exif」模型上擴充,不改變既有資料夾結構):
  1. EXIF(現行邏輯,不變):full date → `<yyyy>/<yyyy-MM-dd>`,year-only → `<yyyy>/<yyyy-00-00>`
  2. 檔名日期解析:在檔名中尋找 `yyyyMMdd`、`yyyy-MM-dd`、`yyyy_MM_dd` 日期樣式,驗證為合法且合理(1990 年起、不晚於今天)的日期;多組候選時取起始位置最前且通過驗證者
  3. 檔案系統日期:取建立日期(birth time)與修改日期(mtime)兩者較早者
- 備援僅在 EXIF 回傳「無日期」(`NoDate`)時觸發;EXIF 的 full date 與 year-only 仍為第一層結果,維持現行歸檔行為
- 第三層幾乎永遠有值,因此原本因無 EXIF 而落入 `no-exif/` 的圖片大多能分到日期資料夾;`no-exif/` 與 error list 只剩 IO 錯誤、或兩個檔案系統日期皆早於 1990-01-01(雙 sentinel)的極端情況
- `CaptureDate` 增加日期來源(`EXIF` / `FILENAME` / `FILE_SYSTEM`)欄位,讓備援判定的結果可往上傳遞;move item 攜帶其日期結果,供計畫預覽顯示來源
- UI 在計畫預覽中標示每個檔案的日期來源,且計畫摘要顯示各來源筆數,使用者能檢查可能分錯的備援判定檔案;error list 語意從「無 EXIF 日期」改為「無法判斷日期(僅剩 IO 錯誤或雙 sentinel)」

## Non-Goals (optional)

<!-- 記於 design.md 的 Goals / Non-Goals -->

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `photo-organization`:「Extract capture date from EXIF」需求擴充為三層日期解析(EXIF → 檔名 → 檔案系統日期),EXIF 的 full-date 與 year-only 結果不變、僅 EXIF 無日期時觸發備援,並新增日期來源標記;「Compute move plan grouped by capture date」需求新增 move item 攜帶 resolved date result 與 date source(供計畫預覽顯示來源),既有的巢狀年份/日期、year-only、no-exif 目標結構不變
- `organizer-ui`:計畫預覽需呈現每個檔案的日期來源並在摘要顯示各來源筆數(檔案清單不標示來源);error list 語意從「無 EXIF 日期」改為「無法判斷日期(僅剩 IO 錯誤或雙 sentinel)」

## Impact

- Affected specs: `photo-organization`, `organizer-ui`
- Affected code:
  - New: src/main/kotlin/org/photocollection/core/FilenameDateParser.kt, src/main/kotlin/org/photocollection/core/FileSystemDateReader.kt, src/test/kotlin/org/photocollection/core/FilenameDateParserTest.kt, src/test/kotlin/org/photocollection/core/FileSystemDateReaderTest.kt
  - Modified: src/main/kotlin/org/photocollection/core/ExifReader.kt, src/main/kotlin/org/photocollection/core/Models.kt, src/main/kotlin/org/photocollection/core/MovePlanner.kt, src/main/kotlin/org/photocollection/ui/App.kt, src/test/kotlin/org/photocollection/core/ExifReaderTest.kt, src/test/kotlin/org/photocollection/core/ModelsTest.kt, src/test/kotlin/org/photocollection/core/MovePlannerTest.kt, src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt, src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt
  - Removed: (none)
