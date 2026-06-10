# Propose Plus Review — Round 1

## Reviewer Findings

### Critical

(無)

### Warning

- severity: Warning | confidence: 90 | 提出者: A+B
  - location: proposal.md § Capabilities(Modified Capabilities)↔ specs/organizer-ui/spec.md ↔ tasks.md 5.1
  - summary: proposal 寫「檔案清單與計畫預覽需呈現日期來源」,但 delta spec 與 tasks 只覆蓋計畫預覽,範圍不一致,實作者無從判斷檔案清單是否要加來源標示
  - recommendation: 二擇一對齊——修正 proposal 措辭為僅計畫預覽,或在 delta spec 補檔案清單來源顯示的 SHALL 與 scenario
- severity: Warning | confidence: 90 | 提出者: B
  - location: design.md「CaptureDate 增加來源欄位」決策 + src/main/kotlin/org/photocollection/core/Models.kt / MovePlanner.kt
  - summary: design 聲稱「UI 從 plan 項目即可取得來源」,但 `MoveItem` 只有 source/target path,`MovePlanner.plan` 建項目時已丟棄 `CaptureDate`,計畫預覽拿不到 `DateSource`;工件未決定資料流走法
  - recommendation: 明確擇一——`MoveItem` 增加 `date: CaptureDate` 欄位並由 `MovePlanner` 傳遞(同步修正 design 敘述與 tasks 1.1),或 design 寫明 UI 回頭 join entries
- severity: Warning | confidence: 90 | 提出者: B
  - location: design.md 驗收標準 + src/test/kotlin/org/photocollection/core/ExifReaderTest.kt
  - summary: 「`ExifReaderTest` 既有案例不變」必然被打破——既有 unreadable-file 測試斷言 `NoDate`,三層備援後會落到 tier 3 回傳 Found(`FILE_SYSTEM`),必須改寫斷言
  - recommendation: 修正 design 驗收標準與 tasks 4.1,寫明 `resolve()` 層級案例不變、`read()` 層級 unreadable-file 案例語意改變需改寫
- severity: Warning | confidence: 80 | 提出者: A+B
  - location: design.md Implementation Contract(`FilenameDateParser`)↔ Risks / Trade-offs
  - summary: 契約自相矛盾——宣告「輸入檔名字串、輸出 `LocalDate?` 的純函式」,但驗證依賴「今天」且 Risks 要求固定 clock 注入,單一 String 參數無處注入
  - recommendation: 介面改為 `parse(fileName: String, today: LocalDate): LocalDate?`,由組合層傳入今天日期,測試注入固定值

### Suggestion

- severity: Suggestion | confidence: 75 | 提出者: B(原 Warning,信心過濾降級)
  - location: specs/photo-organization/spec.md「Tier 1 — EXIF」
  - summary: 未定義「讀取 EXIF metadata 拋例外」時進 tier 2 還是 NoDate;master spec 原「unparseable/corrupt date bytes → no date」Example 被移除卻無新行為定義
  - recommendation: 補「EXIF 讀取例外視同無 EXIF、進入 tier 2」的 SHALL 條款與 Example 列
- severity: Suggestion | confidence: 75 | 提出者: B(原 Warning,信心過濾降級)
  - location: specs/photo-organization/spec.md「Tier 2 — file name」+ Example: filename date parsing
  - summary: 檔名樣式比對的數字邊界規則未定義(長數字串中的 8 位視窗算不算候選,如 `IMG_202510281017.png`、`920251028.png`),Example 表未覆蓋
  - recommendation: 明定「只比對完整數字段(緊鄰字元為數字者不算候選)」並補兩列 Example 定錨
- severity: Suggestion | confidence: 60 | 提出者: B(原 Warning,信心過濾降級)
  - location: specs/photo-organization/spec.md「Tier 3 — file system dates」+ design.md Risks
  - summary: 未防衛 creationTime 在不支援 birth time 的卷上回傳 epoch 零值的情況,tier 3 也無 tier 2 的年份合理性驗證,風險清單未提
  - recommendation: 加防衛規則——建立日期早於 1990-01-01 即忽略、只用修改日期,並記入 Risks
- severity: Suggestion | confidence: 60 | 提出者: B
  - location: tasks.md 5.2 + src/main/kotlin/org/photocollection/ui/App.kt
  - summary: task 5.2 指定以 PlanResultViewTest 驗證清單計數文案,但該區塊位於 App() 非可獨立測試的 composable,且製造 readAttributes IO 失敗的測試手法未說明
  - recommendation: 寫明驗證層級——資料層以 ExifReaderTest 驗證 NoDate 路徑,UI 文案以 content review 或先抽出 data-only composable
- severity: Suggestion | confidence: 50 | 提出者: A
  - location: openspec/specs/organizer-ui/spec.md「Keep the UI responsive during long operations」(master,未列入 delta)
  - summary: master spec 以「EXIF batch reading」描述批次作業,本變更後該批次實際包含三層解析,屬於提到舊行為但未列入 delta 的需求
  - recommendation: 增列 MODIFIED 改為中性措辭,或在 design 記錄刻意不改

## Rating

- quality_score: 6
- critical_gap: false
- 評語:工件整體結構完整:三層備援的優先序決策有清楚理由與替代方案、Implementation Contract 含失敗模式與驗收標準、tasks 與 spec Example 表互相對應。但作為實作交接文件存在四處 confidence ≥ 80 的一致性缺口:(1) proposal 與 delta spec/tasks 的來源顯示範圍不一致;(2) `MoveItem` 不攜帶 `CaptureDate`,計畫預覽拿不到 `DateSource`,資料流未決定;(3) 驗收標準「ExifReaderTest 既有案例不變」與三層備援行為直接矛盾;(4) `FilenameDateParser` 契約的 clock 注入自相矛盾。另有數項中信心建議進一步削弱規格可實作性。無 confidence ≥ 80 的 Critical finding,故 critical_gap 為 false;但高信心矛盾足以讓實作者做出與提案意圖相左的猜測,未達交接品質門檻。

## Fix Actions

- proposal.md:Modified Capabilities 的 organizer-ui 條目改為「計畫預覽需呈現日期來源(檔案清單不標示來源)」;Impact 補列 src/main/kotlin/org/photocollection/core/MovePlanner.kt 與 src/test/kotlin/org/photocollection/core/MovePlannerTest.kt(對應 W1、W2)
- design.md:「CaptureDate 增加來源欄位」決策改為 `MoveItem` 增加 `date: CaptureDate` 欄位、`MovePlanner.plan` 放入(含替代方案否決理由);Implementation Contract 補 `MoveItem` 形狀、`FilenameDateParser` 介面改為 `parse(fileName, today)`、失敗模式補「EXIF 例外 → tier 2」、驗收標準改寫(resolve 層不變/read 層改寫/MovePlannerTest);Non-Goals 補「檔案清單不標示來源」與「Keep the UI responsive 需求文字刻意不改」;Risks 補 epoch sentinel 防衛、clock 注入改為 today 參數(對應 W2、W3、W4、S1、S3、S4)
- specs/photo-organization/spec.md:Tier 1 補 EXIF 例外條款、Tier 2 補完整數字段邊界規則、Tier 3 補 1990 前 sentinel 防衛;Example 表補 `IMG_202510281017.png`、`920251028.png`、`1970-01-01` sentinel、corrupt-image 共四列(對應 S2、S3、S4)
- tasks.md:1.1 補 `MoveItem.date` 與 MovePlannerTest;2.1 改為 `parse(fileName, today)` 介面與九列 Example;3.1 補 sentinel 防衛與四列 Example;4.1 補 EXIF 例外行為與 unreadable-file 案例改寫說明、六列 Example;5.2 改寫驗證層級(資料層測試 + UI 文案 content review)(對應 W2、W3、W4、S2、S3、S4、S5)

## Decision

next_round

第 1 輪 quality_score 6 未達 > 9 門檻。四項 Warning 與五項 Suggestion 已全數修正(含信心 50–75 的建議),進入第 2 輪複審。
