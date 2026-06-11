# Mac Photo Collection

依照 EXIF 拍攝日期，把照片整理進 `yyyy/MM/yyyy-MM-dd` 日期資料夾的 macOS 桌面工具；只有年份的檔案使用 `yyyy/00/yyyy-00-00`，無法判斷日期的檔案進入 `no-exif/`。以 Kotlin + Compose Multiplatform（desktop / JVM）重建自舊版 Windows / C# WinForms 工具 `LabPhotoCollection`，並新增「**先預覽搬移計畫、確認後再執行**」的安全機制——舊版按下 Move 會直接搬檔、無法反悔。

## 功能

- **選擇來源資料夾**並掃描其中的影像檔（不遞迴子資料夾）。
- **讀取 EXIF 拍攝日期**：以 `DateTimeOriginal` 為主、缺漏時退回 `DateTimeDigitized`；讀不到日期的檔案進入「錯誤清單」供確認，並搬入 shared `no-exif/`。
- **照片預覽**：點選清單項目即在畫面預覽；桌面解碼器無法解碼的格式（HEIC / RAW）顯示「預覽不可用」佔位而非崩潰。
- **預覽搬移計畫**：先以純函式計算計畫並逐項顯示「哪個檔案 → 哪個目標資料夾」，確認前不動任何檔案。
- **確認後執行**：在來源資料夾下建立年 / 月 / 日期子資料夾並搬入對應檔案；逐項回報成功 / 失敗與失敗原因。
- **同名衝突安全處理**：採「跳過並標記失敗、不覆蓋」策略，失敗檔案來源保持不變，使用者可手動處理後重跑。
- **介面不凍結**：掃描、EXIF 批次讀取、搬移皆在 UI 執行緒外（`Dispatchers.IO`）進行，期間顯示進行中指示。

支援副檔名（比對不分大小寫）：`jpg`、`jpeg`、`png`、`gif`、`heic`、`heif`，以及常見 RAW：`nef`、`cr2`、`cr3`、`arw`、`rw2`、`orf`、`raf`、`dng`、`sr2`、`pef`。掃描會略過點開頭檔案（如 macOS AppleDouble `._IMG.JPG`、`.DS_Store`）。

## 技術堆疊

| 項目 | 版本 |
| ---- | ---- |
| Kotlin | 2.1.0 |
| Compose Multiplatform（desktop / JVM target） | 1.7.3 |
| metadata-extractor（EXIF 解析） | 2.19.0 |
| JDK | 21 |
| Gradle（wrapper） | 8.13 |

> 僅開 desktop JVM target，不導入 `expect`/`actual` 跨平台抽象。

## 需求

- macOS
- JDK 21（`java -version` 應顯示 21）
- 不需另裝 Gradle，使用專案內附的 `./gradlew` wrapper 即可

## 建置與執行

```bash
# 編譯
./gradlew compileKotlin

# 啟動 app（開出桌面視窗）
./gradlew run
```

## 打包 `.dmg`

```bash
./gradlew packageDmg
```

產物位於 `build/compose/binaries/main/dmg/MacPhotoCollection-1.0.0.dmg`，雙擊即可掛載安裝。

## 測試

```bash
./gradlew test
```

核心邏輯（掃描、EXIF 解析、計畫、執行）以單元測試對照 spec 範例表格驗證，並含一個 Compose UI 測試。

## 專案結構

```
src/main/kotlin/org/photocollection/
├── Main.kt                 # 應用程式進入點（Compose application/Window）
├── core/                   # 純 Kotlin 邏輯，與 UI 分離、可單元測試
│   ├── Models.kt           # PhotoFile / CaptureDate / MovePlan / MoveItem
│   ├── PhotoScanner.kt     # 掃描來源資料夾、過濾副檔名（不遞迴）
│   ├── ExifReader.kt       # 讀取 EXIF 拍攝日期
│   ├── MovePlanner.kt      # plan()：計算搬移計畫，不寫磁碟
│   └── MoveExecutor.kt     # execute()：實際搬檔，逐項回報成功/失敗
└── ui/
    └── App.kt              # Compose 桌面介面
```

`plan()`（計算計畫）與 `execute()`（實際搬檔）在程式碼層面拆開：`plan()` 不觸碰檔案系統，只回傳計畫資料供 UI 預覽；`execute()` 才執行搬移。

## 行為細節

- **不做時區換算**：直接解析 EXIF 原始日期字串，避免午夜前後落到錯誤日期；零哨兵 `0000:00:00 00:00:00`、格式錯誤、缺漏皆視為「無日期」。
- **重複執行安全**：掃描不遞迴，已整理進年 / 月 / 日期子資料夾的檔案不會被再次掃描；已整理完的資料夾會得到空計畫、無操作。
- **衝突偵測**：搬移以不帶 replace-existing 選項的 `Files.move` 進行，靠攔截「目標已存在」失敗偵測衝突（避免 TOCTOU），同時涵蓋「執行前已存在同名」與「同批次內兩檔對應同一目標」兩種情況。

## 非目標 / 已知限制

- 不支援 Android / iOS。
- 不導入資料庫或任何持久化儲存層。
- 不提供 undo / 復原（預覽確認機制已足以避免誤搬）。
- 不提供同名衝突的進階處理（如自動改名）；僅「跳過並回報失敗」，由使用者手動處理。
- PNG / GIF 通常無 EXIF 拍攝日期，這類檔案會歸入錯誤清單，屬預期行為。
- 產物為 JVM 桌面 app，非原生觀感、體積較大（已知並接受）。

## 開發方式

本專案採 [Spectra](https://github.com/spectra-app/spectra) 進行規格驅動開發（SDD）。規格位於 `openspec/specs/`，已封存的變更提案位於 `openspec/changes/archive/`。
