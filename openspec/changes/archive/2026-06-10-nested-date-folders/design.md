## Context

現況（讀 src/main/kotlin/org/photocollection/core）：

- `ExifReader.resolve` 只回傳兩種 `DateResult`：`Found(CaptureDate)` 或 `NoDate`。`parseExifDate` 在 `year==0 || month==0 || day==0` 時一律回 `null` → `NoDate`，所以「只有年份」的部分 EXIF 與「完全沒日期」被混為一談。
- `MovePlanner.plan` 把 `Found` 排成 `<root>/yyyy-MM-dd/檔名`（平鋪一層），`NoDate` 丟進 `MovePlan.errors`（不搬移）。
- `MoveExecutor.execute` 對每個 `MoveItem` 先 `Files.createDirectories(target.parent)` 再 `Files.move`（不帶 replace-existing），衝突丟 `FileAlreadyExistsException` 被視為跳過。`createDirectories` 會建立**所有**缺少的父層。
- UI（App.kt）的掃描階段用 `DateResult.NoDate` 過濾 `undatedPhotos`；預覽標籤讀 `plan.moves.size` 與 `plan.errors.size`。

使用者需求：改成 `年份/日期` 兩層；部分 EXIF（只有年）放 `yyyy-00-00`；完全無 EXIF 統一放 `no-exif/`。

## Goals / Non-Goals

**Goals**
- 目標路徑改為兩層 `<root>/yyyy/yyyy-MM-dd/檔名`。
- EXIF 判讀支援三態：完整日期、只有年份、無日期。
- 只有年份 → `<root>/yyyy/yyyy-00-00/檔名`；無 EXIF → `<root>/no-exif/檔名`。
- 沿用既有的 idempotent 建資料夾與同名衝突跳過行為。

**Non-Goals**
- 不從檔名、原資料夾名、檔案系統時間戳推導年份或日期（年份只來自 EXIF）。
- 不改 EXIF tag 優先序（DateTimeOriginal → DateTimeDigitized）。
- 不改掃描規則（不遞迴、略過點開頭檔案）。
- 不為 year-only 照片新增獨立的第三個 UI 清單欄；year-only 併入既有「檔案清單」，不另開面板。

## Decisions

### D1：`DateResult` 新增 `YearOnly(year: Int)`
在 `ExifReader.kt` 的 sealed interface 加入 `data class YearOnly(val year: Int)`。
- 替代方案：用 `Found` 搭一個「部分日期」旗標 → 否決，會讓 `CaptureDate` 語意變模糊（它保證是完整 `LocalDate`）。
- 三態彼此互斥，呼叫端用 `when` 完整覆蓋。

### D2：`parseExifDate` 區分「只有年份」與「無日期」
解析 `yyyy:MM:dd` 三段後：
- 三段皆能轉整數、`year` 落在 `1..9999`、`month/day` 都 `>0`、且 `LocalDate.of` 成立 → `Found`。`1..9999` 上界同時套用於 Found 與 YearOnly，確保年份資料夾不超過四位（避免 5 位數年份產生 `12024/` 之類資料夾）。
- `year` 落在合法範圍 `1..9999`，但 `month==0` 或 `day==0`，或 month/day 雖非 0 但 `LocalDate.of` 不成立（例如 `2008:13:45`）→ `YearOnly(year)`（年份可靠就不丟進 no-exif）。
- `year==0` 或落在 `1..9999` 之外、段數不為 3、任何段非數字、或原始值缺失 → `NoDate`（涵蓋全零 `0000:00:00`、5 位數年份與無 EXIF）。
- 替代方案：只在 `month==0||day==0` 才算 YearOnly、其他不合法一律 NoDate → 否決，會把「年份明確但月日壞掉」的照片誤丟 no-exif，違反「抓得到年份就歸到該年」的意圖。

### D3：資料夾命名集中在 Models.kt
在 `Models.kt` 提供命名來源，避免字串散落：
- `CaptureDate.folderName()` 維持 `yyyy-MM-dd`（不動）。
- 新增 `CaptureDate.yearFolderName()` 回傳 `"%04d".format(date.year)`。
- 新增頂層常數/函式：`NO_EXIF_FOLDER = "no-exif"`、`yearFolderName(year: Int) = "%04d".format(year)`、`yearOnlyFolderName(year: Int) = "%04d-00-00".format(year)`。
- 年份一律補零成 4 位，確保資料夾名穩定。

### D4：`MovePlanner.plan` 三向對應，全部用 path API 組路徑
- `Found(d)` → `root.resolve(d.yearFolderName()).resolve(d.folderName()).resolve(fileName)`。
- `YearOnly(y)` → `root.resolve(yearFolderName(y)).resolve(yearOnlyFolderName(y)).resolve(fileName)`。
- `NoDate` → `root.resolve(NO_EXIF_FOLDER).resolve(fileName)`，**且**仍把該 `PhotoFile` 加入 `plan.errors`。
- 一律用 `Path.resolve` 逐段組合（不用字串相接），讓 planning 與 execution 路徑一致。

### D5：`NoDate` 檔案同時進 `moves` 與 `errors`（保留 UI 相容）
無 EXIF 檔案現在會被搬到 `no-exif/`，所以它是一個 `MoveItem`；同時仍列入 `MovePlan.errors` 供 UI 顯示「無日期」計數。`errors` 的語意從「不搬移」改為「無 EXIF 日期（仍搬到 no-exif/）」。
- 注意（語意改變，需同步調整標籤）：本次後 `plan.moves` 不再只含完整日期檔，也含 year-only（`yyyy-00-00`）與 no-exif 檔；且 no-date 檔同時計入 `moves.size` 與 `errors.size`，兩者**不再互斥**。因此 `PlanResultView` 既有標籤「N 筆將搬移，M 筆無日期」會讓兩數字重疊（例如 1 full + 1 no-date 顯示「2 筆將搬移，1 筆無日期」），且其下方搬移明細會新出現 `yyyy-00-00`/`no-exif` 列。標籤需改寫成不重疊的語意（例如「N 筆將搬移，其中 M 筆無 EXIF 日期搬往 `no-exif/`」），並同步更新 `PlanResultViewTest`（見 tasks 5.3）。
- 替代方案：移除 `errors` 欄位 → 否決，會破壞 `MovePlan` 簽章、UI 與 `MovePlannerTest`。

### D6：`MoveExecutor` 不需修改
`Files.createDirectories(item.target.parent)` 已會建立 `root/yyyy/yyyy-MM-dd` 全部缺少的父層；衝突跳過邏輯不變。巢狀只是更深的 parent，行為不變。僅更新其 KDoc/規格用語從「date subfolder」改述為「巢狀年份/日期（或 no-exif）子資料夾」。

### D7：掃描清單把 year-only 併入「檔案清單」，避免 UI 隱形
現況 `App.kt` 把掃描結果拆成兩個面板：`datedPhotos`（只收 `DateResult.Found`）→「檔案清單」、`undatedPhotos`（只收 `DateResult.NoDate`）→「錯誤清單」。新增 `YearOnly` 後，year-only 照片既非 `Found` 也非 `NoDate`，會從兩個清單同時消失，卻仍被 `MovePlanner` 排入 `moves` 搬走——使用者沒在任何清單看過卻被搬移，且「檔案清單」計數短少。
- 決策：把 `YearOnly` 與 `Found` 一起歸入「檔案清單」（兩者都是會被搬移的可整理檔案），每筆顯示其目標資料夾名——`Found` 顯示 `yyyy-MM-dd`、`YearOnly` 顯示 `yyyy-00-00`；`NoDate` 維持在「錯誤清單」。
- 實作影響：`datedPhotos` 的元素型別需從 `Pair<PhotoFile, CaptureDate>` 改為能同時承載兩態目標資料夾名的形狀（例如 `Pair<PhotoFile, String>`，String 為目標資料夾顯示名，或一個小型 display data class）。掃描與重掃兩處映射（App.kt 約 293-296、338-341）都要更新；`selectedPhoto` 仍由「檔案清單」當前選取項解析，year-only 照片可正常預覽。
- 替代方案：為 year-only 另開第三個面板 → 否決，與既有兩欄版面不協調、超出本次最小變更意圖（見 Non-Goals）。

## Implementation Contract

**可觀察行為**
- 給定來源 `/photos` 與檔案：A(2008-03-15)、B(EXIF=`2008:00:00`)、C(無 EXIF)，計算 plan 後：
  - A → `/photos/2008/2008-03-15/A`（`MoveItem`）
  - B → `/photos/2008/2008-00-00/B`（`MoveItem`）
  - C → `/photos/no-exif/C`（`MoveItem`），且 C 出現在 `plan.errors`
- 執行 plan 後，上述父資料夾被 idempotent 建立、檔案搬入；目標已存在同名檔則該筆標記失敗、來源不動、其餘繼續。

**介面/資料形狀**
- `DateResult = Found(CaptureDate) | YearOnly(year: Int) | NoDate`。
- `MovePlan(moves: List<MoveItem>, errors: List<PhotoFile>)` 簽章不變；`errors` = 無 EXIF 日期的檔案（同時也在 `moves` 內，目標為 `no-exif/`）。
- Models.kt 公開：`CaptureDate.yearFolderName()`、`yearFolderName(Int)`、`yearOnlyFolderName(Int)`、`NO_EXIF_FOLDER`。

**驗收條件（以測試表示）**
- `ExifReaderTest`：`2024:03:15...`→`Found(2024-03-15)`；`2008:00:00...`→`YearOnly(2008)`；`2008:13:45...`→`YearOnly(2008)`；`0000:00:00...`→`NoDate`；無 EXIF/壞值→`NoDate`；不丟例外。
- `MovePlannerTest`：三態各自對應到上述兩層/no-exif 目標；`NoDate` 同時進 `moves`(no-exif) 與 `errors`；路徑為絕對且以 `root` resolve。
- `ModelsTest`：`yearFolderName(2008)=="2008"`、`yearOnlyFolderName(2008)=="2008-00-00"`、補零（如年 7 →`"0007"`）。
- `MoveExecutorTest`：巢狀年份/日期父層被建立、檔案搬入；既有衝突跳過案例仍通過。
- UI（`OrganizerModel` 掃描映射或對應 UI 測試）：掃描含 full date、year-only、no-date 三類後，「檔案清單」同時含 full date 與 year-only（year-only 顯示 `yyyy-00-00` 目標），「錯誤清單」只含 no-date；計數正確、無檔案在兩清單外隱形。

**範圍邊界**
- In scope：ExifReader.kt、Models.kt、MovePlanner.kt 與其單元測試；MoveExecutor.kt 僅文件用語更新；App.kt 掃描清單映射（把 `YearOnly` 併入「檔案清單」並顯示 `yyyy-00-00` 目標）、`PlanResultView` 計畫預覽標籤改寫為不重疊語意，與 organizer-ui 規格「Display file list and error list」更新為三態。
- Out of scope：掃描規則、EXIF tag 優先序、為 year-only 另開第三個 UI 面板。

## Risks / Trade-offs

- [`NoDate` 同時出現在 `moves` 與 `errors` 可能讓未來讀者誤以為重複] → 在 `MovePlan` 與 planner 的 KDoc 明確註明 `errors` 是「無 EXIF 日期且搬往 no-exif/」的分類資訊，非額外動作。
- [把「年份合法但月日不合法」歸為 YearOnly 可能收進原本會被視為壞值的檔案] → 這是刻意取捨：只要年份可靠就保留歸年，符合使用者意圖；`year==0` 仍正確落入 `NoDate`。
- [`no-exif/` 內同名檔案會碰撞被跳過] → 沿用既有衝突跳過行為，來源不動，使用者可手動處理；列在失敗報告中。
- [新增 `YearOnly` 後若不同步改 UI，year-only 照片會從「檔案清單」「錯誤清單」兩邊隱形卻仍被搬移] → 本次連同 App.kt 掃描映射與 organizer-ui 規格一併更新（D7），把 year-only 併入「檔案清單」顯示，消除隱形與計數短少。

## Migration Plan

- 純向前變更，無資料遷移：既有已整理的平鋪 `yyyy-MM-dd` 舊資料夾不受影響（掃描不遞迴，不會重排）。新整理一律走兩層。
- 無設定、無資料庫、無 API 對外相容性顧慮。

## Open Questions

（無）
