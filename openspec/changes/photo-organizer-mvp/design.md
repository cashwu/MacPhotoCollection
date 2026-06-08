## Context

舊版 `LabPhotoCollection` 是 Windows / C# WinForms 單檔工具（約 224 行），核心流程為：選資料夾 → 掃 jpg/gif → 讀 EXIF 拍攝日期 → 預覽 → 按 Move 直接依日期搬檔，無預覽、無復原。本變更在 Mac 上以 Kotlin + Compose Multiplatform（desktop / JVM）重建，並補上「先預覽計畫、確認後執行」。

限制與前提（來自先前討論定案）：

- 範圍為 **純桌面、最簡單**：只開 desktop（JVM）target，不導入 `expect`/`actual`、不做手機、不做資料庫、不做 undo。
- 目的兼具「快速可用的工具」與「實際練習 Kotlin / Compose 桌面開發與打包」。

## Goals / Non-Goals

**Goals:**

- 重建舊版核心：掃描、EXIF 日期讀取、依日期分組搬檔。
- 新增搬移前的計畫預覽與確認步驟。
- 邏輯（scan / plan / execute）與 UI 分離，邏輯為純 Kotlin function、可單元測試。
- 產出可執行的 `.dmg`。

**Non-Goals:**

- 不支援 Android / iOS，不導入 `expect`/`actual` 抽象。
- 不導入持久化儲存層。
- 不提供 undo / 復原。
- 不做雲端同步、人臉辨識、相簿管理。

## Decisions

### 採用 Compose Multiplatform desktop 單一 JVM target

選 Compose Multiplatform（desktop）而非 SwiftUI 原生或完整 KMP 多 target。理由：使用者目標是練 Kotlin / Compose；純桌面情境下完整 KMP 的跨平台抽象用不到且拖慢進度，因此只開 JVM target。代價：產物為 JVM 桌面 app，非原生觀感、體積較大——已知情並接受。替代方案：SwiftUI 原生（最快但偏離學習目標）、完整 KMP 多 target（過度設計，違反 YAGNI）。

### 邏輯核心與 UI 分 package，且 plan 與 execute 拆開

程式碼分 `core`（純 Kotlin 邏輯）與 `ui`（Compose）兩個 package。核心將「計算搬移計畫 `plan()`」與「實際搬檔 `execute()`」拆成兩個獨立步驟：`plan()` 不觸碰檔案系統的寫入、只回傳計畫資料；`execute()` 才執行搬移。理由：UI 能先顯示計畫供使用者確認，避免舊版直接搬檔無法反悔的問題；純函式邏輯也天然可測。替代方案：沿用舊版單一 Move 動作（被否決，無法預覽）。

### EXIF 解析使用 metadata-extractor

採用 `metadata-extractor`（JVM Java 庫）讀取 EXIF 拍攝日期，取代舊版的 ExifLib。理由：支援格式更廣（含 HEIC / RAW）、維護活躍。因僅 desktop JVM target，直接依賴即可、無需 `expect`/`actual` 包裝。替代方案：自寫 EXIF parser（成本高且易錯，否決）。

### 以 Compose Gradle plugin 的 packageDmg 打包

使用 Compose Gradle plugin 的原生發佈任務（`packageDmg`，底層為 jpackage）輸出 `.dmg`。理由：官方支援、設定簡單。替代方案：Conveyor（功能多但對單人練習過重，否決）。

## Implementation Contract

**核心資料模型（`core`）：**

- `PhotoFile`：代表掃描到的影像檔（至少含絕對路徑、檔名）。
- `CaptureDate`：代表自 EXIF 取得的拍攝日期（可格式化為 `yyyy-MM-dd`）。
- `MovePlan`：可搬移項目的集合，每項為「來源檔案 → 目標日期資料夾路徑」（目標路徑為 `<sourceFolder>/<yyyy-MM-dd>`，以平台 path API 建構之絕對路徑，避免字串相接錯誤）；另含無法判定日期的錯誤檔案清單。

**核心行為（`core`，皆為純 Kotlin function）：**

- 掃描：給定來源資料夾路徑，回傳該資料夾下符合支援副檔名（jpg、jpeg、png、gif，及 metadata-extractor 支援的 HEIC / RAW）的 `PhotoFile` 清單；副檔名比對 **不分大小寫**（相機常輸出 `.JPG`），並 **略過點開頭檔案**（如 macOS AppleDouble `._IMG.JPG`、`.DS_Store`）。**不遞迴子資料夾**（使重複執行同一資料夾時，已整理進日期子資料夾的檔案不會被再次掃描）。資料夾不存在須回報可區分的 folder-not-found 錯誤、存在但無影像檔回傳空清單。
- EXIF 日期讀取：給定 `PhotoFile`，回傳 `CaptureDate` 或在無拍攝日期時回傳可識別的「無日期」結果。日期來源以 `DateTimeOriginal`(0x9003) 為主、缺漏時退回 `DateTimeDigitized`(0x9004)；取 EXIF 值中的當地民用日期、**不做時區換算**（與舊版一致，避免午夜前後落到錯誤日期）。為確保不做時區換算，須讀取該 tag 的 **原始 EXIF 字串值並直接解析日期部分**，不得使用會回傳 instant / epoch 的日期存取器（會套用時區 / offset、可能跨午夜位移）。值缺漏、格式錯誤或為零哨兵 `0000:00:00 00:00:00` 皆視為「無日期」；單檔讀取的例外須被攔截、不得中斷整批。
- 計畫計算 `plan()`：給定 `PhotoFile` 清單，回傳 `MovePlan` —— 有日期者依 `yyyy-MM-dd` 對應到「來源資料夾 / 日期」子資料夾路徑，無日期者歸入錯誤清單。此步驟 **不得** 對檔案系統做任何寫入或搬移。
- 執行 `execute()`：給定 `MovePlan`，以 **idempotent 的 `Files.createDirectories`** 建立所需日期子資料夾（同日多筆不會失敗）並搬移檔案；回傳每項的成功 / 失敗結果。目標資料夾已存在不視為錯誤；日期子資料夾若因任何原因無法建立（含非資料夾佔用該路徑、來源資料夾已不存在），受影響項目標記失敗、不中斷整批。每筆搬移以 **不帶任何 replace-existing 選項的 `Files.move`** 進行（衝突保證來自「省略 replace-existing 選項」使目標已存在時拋失敗，而非請求 `ATOMIC_MOVE` 選項——後者 fail-on-existing 行為因平台而異），靠攔截「目標已存在」的失敗來偵測衝突，**不得**先做 `exists()` 檢查再搬（避免 TOCTOU）。此單一機制同時涵蓋兩種衝突——執行前已存在的目標、同批次較早項目已搬入的目標——並在不分大小寫的檔案系統上一併涵蓋僅大小寫不同的名稱碰撞；衝突項一律跳過、標記失敗、不覆蓋，其餘繼續。目標皆為來源子資料夾，預期同一檔案系統，但任何搬移拋例外仍視為該項失敗。任一項失敗時，其來源檔案須保持不變（不得部分複製或截斷）。

**UI 行為（`ui` / Compose desktop）：**

- 使用者可選擇來源資料夾。
- 選定後顯示：可整理的檔案清單、無 EXIF 日期的錯誤清單（各含數量）。
- 點選清單項目可在畫面預覽該照片；桌面影像解碼器無法解碼的格式（如 HEIC / RAW，仍可整理）顯示「預覽不可用」佔位，不崩潰或留白。
- 提供「預覽計畫」呈現 `plan()` 結果（哪些檔案將搬往哪個日期資料夾）。
- 使用者確認後才呼叫 `execute()`；執行後回報結果（成功 / 失敗數），並 **逐項列出失敗檔案與失敗原因**（如目標同名衝突），讓使用者能定位並手動處理卡住的檔案；同時刷新顯示狀態（重新掃描或清除已搬項目），避免對已搬檔案重複執行同一計畫。
- 掃描、EXIF 批次讀取與執行搬移皆在 UI 執行緒外進行（coroutine / `Dispatchers.IO`），執行期間 UI 保持回應並顯示進行中狀態。

**驗收標準：**

- `plan()` 對含 / 不含 EXIF 日期的混合輸入，能正確分流為搬移項目與錯誤清單，且未變更任何檔案（可由單元測試驗證）。
- `execute()` 能在來源資料夾下建立 `yyyy-MM-dd` 子資料夾並搬入對應檔案；「執行前已存在同名」與「同批次內兩檔對應同一目標」兩種衝突皆被跳過並回報失敗，且失敗項來源檔案保持不變（可由單元測試驗證）。
- EXIF 讀取對缺漏 / 錯誤 / 零哨兵值回傳「無日期」且不拋例外中斷整批（可由單元測試驗證）。
- 應用程式可透過 `packageDmg` 產出可開啟的 `.dmg`。

**Scope 邊界：**

- 範圍內：上述 `core` 與 `ui` 行為、`.dmg` 打包。
- 範圍外：手機端、`expect`/`actual`、持久化、undo、進階衝突策略（如自動改名）、雲端 / 辨識功能。

## Risks / Trade-offs

- [JVM 桌面 app 非原生觀感、體積較大] → 已於決策中接受；符合「練 Compose」目標。
- [metadata-extractor 對部分檔案可能讀不到拍攝日期] → 歸入錯誤清單而非中斷整批，使用者可見。
- [搬移過程中途失敗（權限 / 磁碟滿）可能造成部分搬移] → `execute()` 逐項回報成功 / 失敗，UI 顯示結果；不做自動 rollback（已列為 Non-Goal）。
- [同名衝突採「跳過」可能使部分檔案未整理] → 最小可行策略，明確回報為失敗項，未來可再擴充改名策略。
- [同名檔案（如不同相機的 `IMG_0001.jpg` 同日）會永久卡在來源根目錄、每次重跑都因衝突再次失敗] → MVP 接受此行為（自動改名為 Non-Goal）；以 UI 逐項列出失敗檔案與原因作為唯一回饋管道，使用者可手動改名後再跑。
- [HEIC / RAW 雖列為支援格式，但日期讀取正確性實際依賴 metadata-extractor] → 單元測試以 jpg 樣本驗證日期解析邏輯；HEIC / RAW 的解析正確性倚賴函式庫、不另行逐格式驗證，屬已知範圍。
