## 1. 專案骨架與建置

- [x] 1.1 建立 Compose Multiplatform desktop（JVM 單 target）Gradle 專案，含 settings.gradle.kts、build.gradle.kts、gradle.properties，以及應用程式進入點 Main.kt（Compose `application { Window { } }`）。驗證：`./gradlew tasks` 成功列出含 Compose desktop 的任務、`./gradlew compileKotlin` 通過、`./gradlew run` 能開出空白視窗。
- [x] 1.2 在 build.gradle.kts 加入 `metadata-extractor` 依賴與 Compose desktop 套件。驗證：`./gradlew dependencies` 可見 metadata-extractor，且專案可編譯。

## 2. 核心資料模型（core）

- [x] 2.1 定義 `PhotoFile`、`CaptureDate`（可格式化為 `yyyy-MM-dd`）、`MovePlan`（搬移項目集合 + 錯誤檔案清單）資料模型於 Models.kt。驗證：單元測試斷言 `CaptureDate` 對 2024-03-15 格式化為字串 "2024-03-15"。

## 3. 掃描與 EXIF（core）

- [x] 3.1 實作 PhotoScanner：給定來源資料夾回傳支援副檔名（jpg、jpeg、png、gif、HEIC、RAW，比對不分大小寫）的 `PhotoFile` 清單、略過點開頭檔案（`._*`、`.DS_Store`）、不遞迴子資料夾；資料夾不存在時回報可區分的 folder-not-found 錯誤、存在但無影像檔回傳空清單。驗證：單元測試覆蓋 spec `photo-organization` 的「filtering by extension」表格（含混合檔案、空清單、資料夾不存在三種情境），並覆蓋「Already-organized folder」情境（根目錄僅含日期子資料夾時回傳空清單、不遞迴）。
- [x] 3.2 實作 ExifReader：以 `DateTimeOriginal`(0x9003) 為主、缺漏退回 `DateTimeDigitized`(0x9004) 讀取拍攝日期，讀原始 EXIF 字串值直接解析日期部分（不用回傳 instant/epoch 的存取器）以確保不做時區換算；值缺漏 / 格式錯誤 / 零哨兵 `0000:00:00 00:00:00` 皆回傳可識別的「無日期」結果，單檔例外被攔截不中斷整批。驗證：單元測試覆蓋 spec `photo-organization` 的「date source resolution」表格（DateTimeOriginal、退回 DateTimeDigitized、零哨兵、無 EXIF、corrupt 五情境），並斷言 23:50 樣本仍落在當日、批次不因單檔中斷。

## 4. 計畫與執行（core）

- [x] 4.1 實作 MovePlanner.plan()：將 `PhotoFile` 清單分流為「依 `yyyy-MM-dd` 對應日期資料夾的搬移項目」與「無日期錯誤清單」，目標路徑以平台 path API 建為絕對路徑（`<sourceFolder>/<yyyy-MM-dd>/<fileName>`，非字串相接），且不對檔案系統做任何寫入。驗證：單元測試覆蓋 spec 的「plan grouping」範例（IMG1/IMG2 → 日期資料夾、IMG3 進錯誤清單），斷言目標路徑等於 `Path(sourceFolder).resolve("yyyy-MM-dd").resolve(fileName)`，並斷言執行前後磁碟內容不變。
- [x] 4.2 實作 MoveExecutor.execute()：依 `MovePlan` 以 idempotent `Files.createDirectories` 建立日期子資料夾並搬檔，每筆以不帶任何 replace-existing 選項的 `Files.move` 進行、靠攔截已存在失敗偵測衝突（不先做 `exists()` 再搬，避免 TOCTOU）；目標資料夾已存在不算錯誤、若該路徑被非資料夾佔用則該項標記失敗不中斷；衝突（執行前已存在同名 或 同批次內兩檔對應同一目標路徑）皆跳過、標記失敗並繼續，失敗項來源檔案保持不變，回傳每項成功/失敗結果。驗證：單元測試斷言 (a)「Successful execution」無衝突路徑（日期子資料夾建立、檔案搬入、回報成功）、(b) spec 的「execution with pre-existing and intra-run conflicts」範例（A.jpg 因既有檔失敗、同名對中第一個成功第二個因批次內衝突失敗、所有失敗項原檔不變）、(c) 目標路徑被非資料夾佔用時該項回報失敗且整批不中斷。

## 5. 桌面介面（ui）

- [x] 5.1 實作來源資料夾選擇與掃描串接：選定資料夾後觸發掃描並更新畫面。驗證：手動執行 app，選擇含影像檔資料夾後檔案清單出現對應項目。
- [x] 5.2 實作檔案清單、錯誤清單（各含數量）與選取項目的照片預覽，無法解碼的格式（HEIC / RAW）顯示「預覽不可用」佔位。驗證：手動執行 app，選 5 張有日期 + 2 張無日期之資料夾時，檔案清單顯示 5（count 5）、錯誤清單顯示 2（count 2）；點選 jpg 顯示該圖預覽、點選 HEIC 顯示「預覽不可用」佔位而非崩潰或留白。
- [x] 5.3 實作「預覽搬移計畫」：呼叫 plan() 並顯示各檔案的目標日期資料夾，確認前不搬任何檔案。驗證：手動執行 app，按預覽後可見各檔目標日期資料夾，且磁碟內容未變動。
- [x] 5.4 實作確認後執行：使用者確認才呼叫 execute()，顯示成功/失敗數並逐項列出失敗檔案與原因，且於執行後刷新顯示狀態（重新掃描或清除已搬項目）避免對已搬檔案重複執行。驗證：手動執行 app，刻意製造一筆同名衝突，確認後檔案被搬入日期資料夾、畫面顯示成功與失敗數量並逐項列出該失敗檔案與衝突原因，已搬項目不再出現於清單，且刷新後失敗清單（檔案 + 原因）仍可見。
- [x] 5.5 將掃描、EXIF 批次讀取與執行搬移放到 UI 執行緒外（coroutine / `Dispatchers.IO`），執行期間顯示進行中狀態。驗證：手動執行 app，對含數千張影像的資料夾掃描時介面不凍結並可見進行中指示。

## 6. 打包

- [x] 6.1 設定 Compose Gradle plugin 的原生發佈（packageDmg）。驗證：`./gradlew packageDmg` 產出可開啟的 `.dmg`，雙擊後 app 可啟動。
