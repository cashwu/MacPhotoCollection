## Context

目前日期判斷只有單一來源:`ExifReader` 讀取 EXIF `DateTimeOriginal`(主要)與 `DateTimeDigitized`(備援),回傳 `DateResult.Found` 或 `DateResult.NoDate`。無 EXIF 的圖(截圖、PNG/GIF)全部落入 `MovePlan.errors`,不被歸檔。

macOS(APFS)上每個檔案都有建立日期(birth time)與修改日期(mtime),Java NIO 的 `Files.readAttributes` 即可讀取,無需新依賴。另外 macOS 截圖等工具的檔名本身含日期(如 `SCR-20251028-jljd.png`),且檔名不會被拷貝、AirDrop、雲端同步破壞,可靠度高於檔案系統日期。

## Goals / Non-Goals

**Goals:**

- 無 EXIF 的圖片也能自動歸入日期資料夾
- 三層備援:EXIF → 檔名日期 → 檔案系統日期(建立/修改取較早)
- 日期來源可區分,UI 讓使用者檢查備援判定的檔案

**Non-Goals:**

- 不擴大掃描範圍(PDF、影片等其他檔案類型不在此變更)
- 不提供手動指定日期功能
- 不做時區轉換(「不轉時區」原則僅適用 tier 1 EXIF 的 raw string 解析;tier 3 的 `creationTime`/`lastModifiedTime` 是 instant,轉 civil date 必經系統預設時區(`ZoneId.systemDefault()`)換算,屬已知且接受的差異)
- 不提供「關閉備援」的設定選項(維持單一行為,簡化實作)
- 不在檔案清單標示日期來源(只在計畫預覽標示——使用者是在預覽階段做確認決策)
- 不修改 organizer-ui「Keep the UI responsive during long operations」的需求文字——其「EXIF batch reading」語意視為涵蓋擴充後的三層日期解析批次(解析仍在同一個背景批次路徑執行,行為不變)

## Decisions

### 三層優先序:EXIF 最先、檔名次之、檔案系統日期保底

EXIF 是拍攝裝置寫入的最權威來源,維持第一。檔名日期放第二,因為檔名不會被檔案傳輸破壞,可靠度高於系統日期;且若把系統日期放第二,系統日期永遠存在,檔名解析會成為永遠執行不到的死碼。檔案系統日期永遠有值,作為最後保底,確保所有檔案都能歸檔。

替代方案:原本考慮「EXIF → 系統日期 → 檔名」,因死碼問題否決;「只加系統日期、捨棄檔名解析」較簡單,但拷貝過的截圖會分錯資料夾,故不採用。

### 檔名日期解析規則:樣式比對加合法性與合理性驗證

不窮舉各工具的命名格式,以通用樣式比對:在檔名(不含副檔名)中尋找 `yyyy-MM-dd`、`yyyy_MM_dd`、`yyyyMMdd` 形式的數字串,依出現位置由左至右,取第一組通過驗證的日期。候選只比對完整數字段——緊鄰字元是數字的比對不算候選(例如 12 位 timestamp `202510281017` 不含合法的 `yyyyMMdd` 候選),避免從長流水號中切出假日期。驗證條件:必須是真實存在的日期,且年份介於 1990 到「今天」之間(含),晚於今天即不合理(避免把流水號誤判為未來日期)。「今天」由呼叫端以參數傳入(`parse(fileName, today)`),組合層在每次 `read`/批次開始時以 `LocalDate.now(ZoneId.systemDefault())` 取得並傳入,解析器維持純函式、測試可注入固定日期。三種樣式一併比對、依起始位置取最前的通過驗證者(不分樣式輪流掃描),分隔形式的兩個分隔符必須一致。全部候選都不通過時視為「檔名無日期」,落到下一層。

替代方案:窮舉已知格式(macOS 截圖、Android `IMG_yyyyMMdd` 等)清單比對——維護成本高且涵蓋不全,否決。

### 檔案系統日期取建立與修改日期較早者

拷貝會讓建立日期變成拷貝當下、編輯會讓修改日期變晚,但兩者很少同時被破壞;取較早者最接近檔案真實產生時間。以 `Files.readAttributes` 讀取 `BasicFileAttributes` 的 `creationTime` 與 `lastModifiedTime`,各以系統預設時區(`ZoneId.systemDefault()`)轉 civil date 後比較取較早者。防衛規則(對稱適用於兩個日期):任一日期早於 1990-01-01 即視為 sentinel 忽略之、改用另一個日期——建立日期的 sentinel 來自不支援 birth time 的卷回報 epoch 零值,修改日期的 sentinel 來自 zip 解壓等工具寫入 DOS epoch(1980-01-01)之類的值;兩者皆早於 1990-01-01 時視同 tier 3 失敗回傳 NoDate,避免檔案被歸進 `1970-01-01`/`1980-01-01` 資料夾。未來的檔案系統日期(時鐘錯誤)則照常接受——tier 3 是最後一層,拒絕會讓檔案無法歸檔,而錯誤的資料夾名在計畫預覽中可被使用者發現,比進 error list 更可回復。讀取失敗(IO 錯誤)或兩日期皆 sentinel 時回傳 NoDate,該檔案進入 error list——這是 error list 僅剩的兩種情境。

### CaptureDate 增加來源欄位,DateResult 結構不變

`CaptureDate` 增加 `source` 欄位(enum:`EXIF`、`FILENAME`、`FILE_SYSTEM`),`DateResult.Found` 包著帶來源的 `CaptureDate` 往上傳。為了讓計畫預覽拿得到來源,`MoveItem` 增加 `date: CaptureDate` 欄位,`MovePlanner.plan` 建立 `MoveItem` 時把 `DateResult.Found` 中的 `CaptureDate` 一併放入(仍是單純搬運,不依來源分支)。UI 從 plan 項目的 `date.source` 取得來源做顯示。

替代方案:(1) 擴充 `DateResult` 為多個 Found 子型別——會迫使 `MovePlanner` 處理多分支,且來源本質上是 `CaptureDate` 的屬性,否決;(2) `MoveItem` 不加欄位、UI 回頭以 `PhotoFile` 對照 entries 取來源——多一次 join 且兩份資料可能不同步,否決。

### 解析器拆為獨立物件,組合層負責優先序

檔名解析放 `FilenameDateParser`、檔案系統日期放 `FileSystemDateReader`,各自獨立可測;三層優先序的組合邏輯放在現有 `ExifReader.read` 內(維持現名),確保「優先序」這個行為只存在一處。

測試策略:repo 沒有帶 EXIF 的測試 fixture 圖檔,且 metadata-extractor 是唯讀函式庫無法寫入 EXIF,因此三層優先序的組合邏輯比照現有 `ExifReader.resolve()` 的慣例抽成 internal 函式(以「EXIF 解析結果」為參數,而非真實圖檔),含 EXIF 的優先序案例在這個 internal seam 上測試,不需要偽造影像位元組。同理,macOS 上 Java NIO 無法寫入 `creationTime`(`Files.setAttribute` 被靜默忽略,APFS 設早於 birth time 的 mtime 還會下拉 birth time),tier 3 的「取較早者 + 對稱 sentinel」邏輯抽成 internal seam `pick(creationDate: LocalDate?, modifiedDate: LocalDate?): LocalDate?`,spec「earlier of creation and modification」七列全部在 seam 上測試;真實暫存檔只測 happy-path smoke 與 IO 失敗案例,且「預期日期必須由寫入的 instant 經同一個 `ZoneId.systemDefault()` 換算推導、不得硬編 Example 日期」的防 flaky 規則適用於所有以真實檔案驗證 tier 3 結果的測試(含 ExifReaderTest 中結果為 FILE_SYSTEM 的列);這些 read() 層級的列僅斷言來源為 `FILE_SYSTEM` 且日期由實際寫入的 mtime 推導,「取較早者」與 sentinel 語意由 `pick` seam 獨佔驗證,Example 中 creation/modified 的具體組合視為 seam 層的說明性數值。組合層的 NoDate 路徑(tier 3 回傳 null 時 `read()` 回傳 `NoDate`、`readAll` 不中斷批次)同樣以可注入的 tier 3 結果在 internal seam 層級測試。其餘無 EXIF 的 FILENAME / FILE_SYSTEM 案例以真實暫存檔在 `read()` 層級測試。

## Implementation Contract

- **行為**:掃描後,每個圖片檔案依「EXIF → 檔名 → 檔案系統日期」順序解析出 `yyyy-MM-dd` 日期並歸入對應資料夾。只有檔案屬性讀取拋出 IO 錯誤、或兩個檔案系統日期皆早於 1990-01-01(雙 sentinel)的檔案才進入 error list。計畫預覽逐項標示日期來源,且計畫摘要顯示各來源的筆數。
- **介面/資料形狀**:
  - `CaptureDate(date: LocalDate, source: DateSource)`,`DateSource` 為 `EXIF` / `FILENAME` / `FILE_SYSTEM` 三值 enum;`folderName()` 行為不變。
  - `MoveItem(source: PhotoFile, target: Path, date: CaptureDate)`:增加 `date` 欄位攜帶日期與來源,供計畫預覽顯示;`MovePlanner.plan` 建立項目時放入,不依來源分支。
  - `FilenameDateParser`:`parse(fileName: String, today: LocalDate): LocalDate?`;純函式、無 IO,「今天」由呼叫端(組合層)傳入,測試可注入固定日期。
  - `FileSystemDateReader`:輸入 `PhotoFile`,輸出 `LocalDate?`(IO 失敗或兩日期皆早於 1990-01-01 時為 null);以系統預設時區轉 civil date,取 creationTime 與 lastModifiedTime 較早者,任一日期早於 1990-01-01 即忽略該日期、改用另一個;未來日期照常接受。內含 internal seam `pick(creationDate: LocalDate?, modifiedDate: LocalDate?): LocalDate?` 承載取較早者與對稱 sentinel 邏輯,可不經檔案系統獨立測試。
  - 組合層回傳的 `DateResult` 型別維持 `Found` / `NoDate` 兩態。
- **失敗模式**:EXIF 讀取拋出例外 → 視同無 EXIF,落到 tier 2;檔名無可驗證日期 → 靜默落到下一層;檔案屬性讀取 IO 錯誤、或建立與修改日期皆早於 1990-01-01 → `NoDate`,進 error list;任何單一檔案的例外不得中斷整批處理(沿用現行原則)。
- **驗收標準**:`./gradlew test` 全綠;`FilenameDateParserTest` 覆蓋 spec 的 Example 表(含合法、不合法、未來日期、上下界邊界、數字段邊界、無日期);`FileSystemDateReaderTest` 在 `pick` seam 上驗證取較早者、任一日期早於 1990 的對稱 sentinel 防衛、雙 sentinel 回 null、未來日期接受(七列全覆蓋),另以真實暫存檔測 happy-path smoke(預期值由寫入 instant 換算推導)與 IO 失敗;`ExifReaderTest` 中 `resolve()` 層級的 EXIF 案例不變,`read()` 層級的 unreadable-file 案例語意改變(corrupt 檔案改為 tier 3 `FILE_SYSTEM` 判定)需改寫斷言,三層優先序案例中含 EXIF 的列在 internal seam 上測試、無 EXIF 的列以真實暫存檔測試,並含 NoDate 路徑案例(tier 3 回傳 null 時 `read()` 回傳 `NoDate`、`readAll` 不中斷批次);`MovePlannerTest` 驗證 `MoveItem.date` 正確攜帶日期與來源,`MoveExecutorTest` 與 `PlanResultViewTest` 既有 `MoveItem` 建構同步更新為三參數;UI 測試驗證計畫預覽的來源標示與摘要的各來源筆數;error list 新語意以資料層測試為主,UI 文案以測試或 content review 確認。
- **範圍邊界**:in scope — core 日期解析、Models、UI 來源顯示與 error list 文案;out of scope — PhotoScanner 掃描規則、MoveExecutor 執行邏輯、新增任何外部依賴。

## Risks / Trade-offs

- [備援日期可能與真實拍攝日不符(拷貝過的檔案、編輯過的圖)] → UI 標示日期來源,使用者可在計畫預覽中檢查;檔名層優先於系統日期已降低大部分誤判
- [檔名中的流水號可能被誤判為日期(如 `12345678`)] → 合法性 + 年份範圍(1990~今天)雙重驗證;`1234-56-78` 非法日期、未來年份皆被拒
- [「今天」邊界使檔名驗證結果隨執行日期改變] → 此為刻意設計(拒絕未來日期);`parse` 以 `today` 參數注入,測試傳固定日期避免 flaky
- [檔案系統日期可能是 sentinel 值:不支援 birth time 的卷(NFS/部分 SMB)creationTime 回報 epoch 零值、zip 解壓工具寫入 DOS epoch(1980-01-01)mtime] → tier 3 對稱防衛:任一日期早於 1990-01-01 即忽略該日期、改用另一個;兩者皆是 sentinel 則進 error list
- [error list 幾乎清空,使用者可能誤以為所有日期都正確] → UI 上備援來源明確標示,計畫預覽仍需使用者確認後才執行(現行行為)
- [上千檔案時在計畫預覽逐項檢查備援判定不可行] → 計畫摘要顯示各日期來源的筆數,使用者可快速判斷備援判定的規模再決定是否細看;殘餘風險:單一檔案的誤判仍須逐項檢視才能發現,屬已知且接受
- [malformed `DateTimeOriginal` 且 `DateTimeDigitized` 完好時,不會退回 Digitized 而直接落入備援(沿襲現行 `resolve()` 行為)] → 已知且接受:維持手術式變更、不動 tier 1 內部語意;spec「full tier resolution」Example 固定此行為,且備援結果在預覽中帶來源標示可被檢查
