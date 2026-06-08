## Why

舊版 `LabPhotoCollection`（Windows / C# WinForms）能依 EXIF 拍攝日期把照片整理進日期資料夾，但只能在 Windows 上跑，且按下 Move 會直接搬檔、無法先預覽。本變更要在 Mac 上以 Kotlin + Compose Multiplatform（desktop / JVM）重建這支工具，並補上「先預覽搬移計畫、確認後再執行」的安全機制。同時作為實際練習 Kotlin Multiplatform 桌面開發與打包流程的載體。

## What Changes

- 新增一支 Compose Multiplatform **desktop-only（JVM target）** 桌面應用。
- 選擇來源資料夾，掃描其中的影像檔（jpg / jpeg / png / gif，以及 metadata-extractor 支援的 HEIC / RAW）。
- 讀取每張照片 EXIF 的拍攝日期；讀不到日期者進入「錯誤清單」，不參與搬移。
- 點選清單項目時於畫面預覽該張照片。
- 依拍攝日期（`yyyy-MM-dd`）計算搬移計畫（plan），於畫面顯示「哪些檔案將搬往哪個日期資料夾」。
- 使用者確認後才執行（execute）實際搬移：在來源資料夾下建立日期子資料夾並搬入對應檔案。
- `plan`（計算計畫）與 `execute`（實際搬檔）在程式碼層面拆開，邏輯與 UI 分屬不同 package，邏輯為純 Kotlin function。
- 以 Compose Gradle plugin 的 `packageDmg`（jpackage 底層）打包為 `.dmg`。

## Non-Goals

- 不支援 Android / iOS，不導入 `expect`/`actual` 跨平台抽象（僅 desktop JVM target）。
- 不導入資料庫或任何持久化儲存層。
- 不提供 undo / 復原功能（預覽確認機制已足以避免誤搬）。
- 不做雲端同步、人臉辨識、相簿管理等延伸功能。
- 不提供同名檔案衝突的進階處理策略（例如自動改名）；僅採最小「跳過並回報失敗」策略，規則於 spec 內定義。
- 不對沒有 EXIF 拍攝日期的格式（PNG / GIF 通常無 EXIF 日期）特別處理；這類檔案一律歸入錯誤清單，屬預期行為而非缺陷。

## Capabilities

### New Capabilities

- `photo-organization`: 核心領域邏輯 —— 掃描影像檔、解析 EXIF 拍攝日期、依日期分組計算搬移計畫、執行搬移、以及無 EXIF 日期檔案的錯誤歸類與同名衝突處理。
- `organizer-ui`: 桌面互動介面 —— 選擇來源資料夾、顯示檔案清單與錯誤清單、預覽選取的照片、預覽搬移計畫、確認後觸發執行。

### Modified Capabilities

(none)

## Impact

- Affected specs: 新增 `photo-organization`、`organizer-ui` 兩個 capability。
- Affected code:
  - New:
    - build.gradle.kts
    - settings.gradle.kts
    - gradle.properties
    - src/main/kotlin/org/photocollection/core/PhotoScanner.kt
    - src/main/kotlin/org/photocollection/core/ExifReader.kt
    - src/main/kotlin/org/photocollection/core/MovePlanner.kt
    - src/main/kotlin/org/photocollection/core/MoveExecutor.kt
    - src/main/kotlin/org/photocollection/core/Models.kt
    - src/main/kotlin/org/photocollection/ui/App.kt
    - src/main/kotlin/org/photocollection/Main.kt
  - Modified: (none)
  - Removed: (none)
- Dependencies: Kotlin、Compose Multiplatform（desktop）、`metadata-extractor`（EXIF 解析，JVM）。
