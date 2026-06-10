## 1. 資料模型擴充

- [ ] 1.1 依設計決策「CaptureDate 增加來源欄位,DateResult 結構不變」擴充 src/main/kotlin/org/photocollection/core/Models.kt 與 src/main/kotlin/org/photocollection/core/MovePlanner.kt,滿足修改後的 spec 需求「Compute move plan grouped by capture date」(plan 項目攜帶日期與來源):新增 `DateSource` enum(`EXIF` / `FILENAME` / `FILE_SYSTEM`),`CaptureDate` 增加 `source` 欄位且 `folderName()` 行為不變,`MoveItem` 增加 `date: CaptureDate` 欄位並由 `MovePlanner.plan` 在建立項目時放入(不依來源分支),同步更新既有的兩參數 `MoveItem` 建構為三參數(src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt 7 處、src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt 2 處,共 9 處),並對 src/main/kotlin/org/photocollection/core/ExifReader.kt 的 `resolve()` 做最小過渡修改(`CaptureDate(date)` 改為 `CaptureDate(date, DateSource.EXIF)`,不在本任務引入備援邏輯;不得以 `source` 預設參數值解套,避免後續任務遺漏來源標注時靜默通過編譯),MovePlannerTest 與 ModelsTest 既有 `CaptureDate` 建構同步補上 `source` 引數;以 ModelsTest 驗證 `folderName()` 既有案例不變、來源欄位可正確攜帶三種值,以 MovePlannerTest 驗證 `MoveItem.date` 正確攜帶日期與來源(對應 spec「plan grouping」Example 的 EXIF/FILENAME 來源標注),整個 test source set 編譯通過且 MoveExecutorTest、PlanResultViewTest 全綠

## 2. 檔名日期解析

- [ ] 2.1 依設計決策「檔名日期解析規則:樣式比對加合法性與合理性驗證」新增 src/main/kotlin/org/photocollection/core/FilenameDateParser.kt:`parse(fileName: String, today: LocalDate): LocalDate?` 純函式(「今天」由呼叫端注入,測試傳固定日期),支援 `yyyy-MM-dd`、`yyyy_MM_dd`、`yyyyMMdd` 樣式且只比對完整數字段(緊鄰字元為數字者不算候選),由左至右取第一組通過「真實日期、年份 ≥ 1990、不晚於今天(上下界皆含)」驗證的候選;以 FilenameDateParserTest 覆蓋 spec「filename date parsing」Example 表全部十六列(含不合法日期、未來日期、1990 前年份、上下界邊界、超長數字段、多候選取第二組、跨樣式依起始位置取最前者、分隔形式前/後鄰數字邊界、混合分隔符、無日期)

## 3. 檔案系統日期讀取

- [ ] 3.1 依設計決策「檔案系統日期取建立與修改日期較早者」新增 src/main/kotlin/org/photocollection/core/FileSystemDateReader.kt:以 `Files.readAttributes` 讀取 `creationTime` 與 `lastModifiedTime`,各以系統預設時區轉 civil date 後回傳較早者,任一日期早於 1990-01-01(sentinel:epoch 零值或 DOS epoch)即忽略該日期、改用另一個,兩日期皆早於 1990-01-01 或 IO 失敗時回傳 null,未來日期照常接受,取較早者與對稱 sentinel 邏輯放在 internal seam `pick(creationDate, modifiedDate)` 中;以 FileSystemDateReaderTest 在 `pick` seam 上覆蓋 spec「earlier of creation and modification」Example 表全部七列(含建立/修改兩側的對稱 sentinel 防衛、雙 sentinel 失敗、未來日期接受),另以真實暫存檔測 happy-path smoke(預期值由寫入 instant 經 `ZoneId.systemDefault()` 換算推導,不得硬編)與讀取失敗案例

## 4. 三層備援組合

- [ ] 4.1 依設計決策「三層優先序:EXIF 最先、檔名次之、檔案系統日期保底」與「解析器拆為獨立物件,組合層負責優先序」修改 src/main/kotlin/org/photocollection/core/ExifReader.kt:`read` 依序嘗試 EXIF、FilenameDateParser、FileSystemDateReader,EXIF 讀取拋例外時視同無 EXIF 落到 tier 2,回傳帶對應 `DateSource` 的 `DateResult.Found`,僅當第三層回傳 null(IO 失敗或兩個檔案系統日期皆早於 1990-01-01)時回傳 `NoDate`,單檔例外不中斷整批,滿足修改後的 spec 需求「Extract capture date from EXIF」;以 ExifReaderTest 驗證:`resolve()` 層級的既有 EXIF 案例不變,`read()` 層級的 unreadable-file 既有案例改寫斷言(corrupt 檔案語意改為 tier 3 `FILE_SYSTEM` 判定),並新增「full tier resolution」Example 表全部七列的優先序案例(含 malformed `DateTimeOriginal` 不退回 `DateTimeDigitized` 的既有語意固定列)——依 design 測試策略,含 EXIF 值的列在 internal seam(以 EXIF 解析結果為參數的組合函式)上測試;無 EXIF 的列以真實暫存檔在 `read()` 層級測試,其中結果為 `FILE_SYSTEM` 的列僅斷言來源正確且日期由實際寫入的 mtime instant 經 `ZoneId.systemDefault()` 推導(不得硬編 Example 日期;「取較早者」語意由任務 3.1 的 `pick` seam 獨佔驗證);「metadata read throws → FILENAME」列(seam 無法表達讀取拋例外)以寫入垃圾位元組、檔名為 `SCR-20251028-jljd.png` 的真實暫存檔在 `read()` 層級測試,驗證例外被視同無 EXIF 並落到 FILENAME;另新增 NoDate 路徑案例,對應 spec Scenario「File attributes cannot be read」:以可注入的 tier 3 null 結果驗證 `read()` 回傳 `NoDate` 且 `readAll` 不中斷批次

## 5. UI 呈現

- [ ] 5.1 實作 spec 新需求「Indicate capture date source in plan preview」:計畫預覽中每個檔案標示其日期來源(`EXIF` / `FILENAME` / `FILE_SYSTEM`),且計畫摘要顯示各來源筆數,修改 src/main/kotlin/org/photocollection/ui/App.kt;以 PlanResultViewTest 驗證 EXIF 判定與檔名判定的項目顯示不同來源標示,並驗證摘要的各來源筆數(對應 spec Scenario「Plan preview summary shows per-source counts」);逐項列文字格式變更時同步更新 PlanResultViewTest 既有的精確字面斷言(如「IMG1.jpg  →  2024-03-15」該列)
- [ ] 5.2 依修改後的 spec 需求「Display file list and error list」更新 error list 語意與文案:承載舊語意的字串是 PlanResultView 計畫摘要的「X 筆無日期」(src/main/kotlin/org/photocollection/ui/App.kt),改為「X 筆無法判斷日期」之類涵蓋「檔案屬性讀取失敗與雙 sentinel」新語意的措辭,清單計數行為不變;驗證方式:資料層由任務 4.1 的 ExifReaderTest NoDate 路徑案例與任務 3.1 的 `pick` seam null 案例共同確認 `NoDate` 僅來自 IO 失敗與雙 sentinel 兩條路徑,UI 文案更新 PlanResultViewTest 既有的「計畫:…筆無日期」字面斷言為新措辭,其餘清單標題若未抽成可獨立測試的 composable 則以 content review 確認

## 6. 整體驗證

- [ ] 6.1 全套測試通過且工件一致:執行 ./gradlew test 確認全綠,並執行 spectra validate add-date-fallback 確認工件無錯誤
