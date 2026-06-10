## 1. EXIF 三態判讀

- [x] 1.1 [規格需求 Extract capture date from EXIF；設計 D1：`DateResult` 新增 `YearOnly(year: Int)`] 在 src/main/kotlin/org/photocollection/core/ExifReader.kt 的 `DateResult` sealed interface 新增 `data class YearOnly(val year: Int) : DateResult`，與既有 `Found`、`NoDate` 並列。
- [x] 1.2 [設計 D2：`parseExifDate` 區分「只有年份」與「無日期」] 改寫 `ExifReader.parseExifDate`（或拆出判讀邏輯），讓它回傳三態：解析 `yyyy:MM:dd` 三段後，`year` 在 `1..9999` 且月、日皆 `>0` 且 `LocalDate.of` 成立回 `Found`（`1..9999` 上界同時套用於 Found 與 YearOnly，確保年份資料夾不超過四位）；`year` 在 `1..9999` 但無法組成合法日期（月或日為 0、或月/日非 0 但 `LocalDate.of` 失敗，例如 `2008:13:45`）回 `YearOnly(year)`；`year==0` 或落在 `1..9999` 之外（例如 5 位數 `12024`）、段數不為 3、任一段非數字、或值缺失回 `NoDate`。`read` 仍以 try/catch 把任何例外收斂為 `NoDate`。
- [x] 1.3 更新 src/test/kotlin/org/photocollection/core/ExifReaderTest.kt：新增涵蓋 `2024:03:15...→Found(2024-03-15)`、`2008:00:00...→YearOnly(2008)`、`2008:13:45...→YearOnly(2008)`、`2008:02:30...→YearOnly(2008)`（月合法、日越界，走 `LocalDate.of` 失敗路徑）、`0000:00:00...→NoDate`、`12024:03:15...→NoDate`（年越界 1..9999）、缺 EXIF/壞值→`NoDate`、且讀檔例外不外逸的案例。

## 2. 資料夾命名（Models）

- [x] 2.1 [設計 D3：資料夾命名集中在 Models.kt] 在 src/main/kotlin/org/photocollection/core/Models.kt 新增 `NO_EXIF_FOLDER = "no-exif"` 常數，以及 `yearFolderName(year: Int): String`（回 `"%04d".format(year)`）與 `yearOnlyFolderName(year: Int): String`（回 `"%04d-00-00".format(year)`）；為 `CaptureDate` 新增 `yearFolderName()` 回 `yearFolderName(date.year)`。`folderName()` 維持 `yyyy-MM-dd` 不變。
- [x] 2.2 更新 src/test/kotlin/org/photocollection/core/ModelsTest.kt：驗證 `yearFolderName(2008)=="2008"`、`yearOnlyFolderName(2008)=="2008-00-00"`、年份補零（`yearFolderName(7)=="0007"`、`yearOnlyFolderName(7)=="0007-00-00"`）、`CaptureDate(2008-03-15).yearFolderName()=="2008"`。

## 3. 兩層 + no-exif 的 move plan

- [x] 3.1 [規格需求 Compute move plan grouped by capture date；設計 D4：`MovePlanner.plan` 三向對應，全部用 path API 組路徑] 改寫 src/main/kotlin/org/photocollection/core/MovePlanner.kt 的 `plan`：用 `when` 覆蓋三態，`Found(d)` → `root.resolve(d.yearFolderName()).resolve(d.folderName()).resolve(photo.fileName)`；`YearOnly(y)` → `root.resolve(yearFolderName(y)).resolve(yearOnlyFolderName(y)).resolve(photo.fileName)`；`NoDate` → `root.resolve(NO_EXIF_FOLDER).resolve(photo.fileName)` 並將該 `photo` 加入 `errors`。一律用 `Path.resolve` 逐段組合，不用字串相接。
- [x] 3.2 [設計 D5：`NoDate` 檔案同時進 `moves` 與 `errors`（保留 UI 相容）] 更新 `MovePlan`/`MovePlanner` 的 KDoc，註明 `errors` 語意改為「無 EXIF 日期的檔案（仍搬往 `no-exif/`）」，非「不搬移」。
- [x] 3.3 更新 src/test/kotlin/org/photocollection/core/MovePlannerTest.kt：以含 full date、year-only、no-date 的輸入驗證三向目標路徑為絕對且以 `root` 逐段 resolve；驗證 `NoDate` 檔案同時出現在 `moves`（目標 `no-exif/`）與 `errors`；驗證計算 plan 不動檔案系統。

## 4. 巢狀執行確認與用語更新

- [x] 4.1 [規格需求 Execute move plan；設計 D6：`MoveExecutor` 不需修改] 確認 src/main/kotlin/org/photocollection/core/MoveExecutor.kt 不需邏輯變更（`Files.createDirectories(item.target.parent)` 已建立 `root/yyyy/yyyy-MM-dd` 等全部缺少父層）；僅更新其 KDoc，將「date subfolder」改述為「巢狀年份/日期或 no-exif 子資料夾」。
- [x] 4.2 更新 src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt：新增一個 plan 含巢狀年份/日期目標與 no-exif 目標的成功搬移案例，驗證父資料夾被建立、檔案搬入；新增一個 `no-exif/` 內同名碰撞案例（兩個無 EXIF 檔搬往同一 `no-exif/` 且同檔名——此為本次新增的碰撞面，平鋪版本中無日期檔根本不搬故不會碰撞），驗證先到者成功、後到者標記失敗且來源不動；確認既有「目標已存在跳過」「同 run 同名衝突」案例在新路徑下仍通過。

## 5. 掃描清單顯示 year-only（UI）

- [x] 5.1 [規格需求 Display file list and error list；設計 D7：掃描清單把 year-only 併入「檔案清單」] 改寫 src/main/kotlin/org/photocollection/ui/App.kt 的掃描與重掃映射（約 293-296、338-341）：把 `DateResult.Found` 與 `DateResult.YearOnly` 一起放進「檔案清單」資料來源，每筆帶其目標資料夾顯示名（`Found`→`yyyy-MM-dd`、`YearOnly`→`yyyy-00-00`）；`undatedPhotos` 維持只收 `DateResult.NoDate`。為承載兩態目標名，將 `datedPhotos` 元素型別由 `Pair<PhotoFile, CaptureDate>` 調整為能表達目標資料夾名的形狀（例如 `Pair<PhotoFile, String>` 或小型 display data class），並同步更新「檔案清單」面板渲染（App.kt:70-74）與 `selectedPhoto` 取值。
- [x] 5.2 驗證掃描映射：驗證掃描含 full date、year-only、no-date 三類後，「檔案清單」含 full date 與 year-only（year-only 顯示 `yyyy-00-00`）、「錯誤清單」只含 no-date，且無檔案在兩清單外漏失。若 `OrganizerModel` 為 private 無法直接測，提升其掃描映射為可測（抽成 internal 函式或放寬可見度），不要以「另開測試」當作跳過更新既有 UI 測試的藉口。
- [x] 5.3 [設計 D5 注意事項：`PlanResultView` 標籤語意改變] 改寫 src/main/kotlin/org/photocollection/ui/App.kt 的 `PlanResultView` 標籤（App.kt:195），從「N 筆將搬移，M 筆無日期」（兩數字現已重疊）改為不重疊語意，例如「N 筆將搬移，其中 M 筆無 EXIF 日期搬往 `no-exif/`」；更新 src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt，以含 year-only（`yyyy-00-00`）與 no-exif 列的 plan 驗證標籤文字與搬移明細列渲染正確。

## 6. 驗證

- [x] 6.1 執行 `./gradlew test` 確認全部測試通過。
- [x] 6.2 手動冒煙：在含完整日期、僅年份（`2008:00:00`）、與無 EXIF 三類照片的暫存資料夾上跑一次整理，確認「檔案清單」同時列出完整日期與僅年份照片、「錯誤清單」列出無 EXIF 照片，且產生 `<root>/yyyy/yyyy-MM-dd/`、`<root>/yyyy/yyyy-00-00/`、`<root>/no-exif/` 三種結構且檔案就位。
