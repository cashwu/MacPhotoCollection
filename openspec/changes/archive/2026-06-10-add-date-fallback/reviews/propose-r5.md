# Propose Plus Review — Round 5

## Reviewer Findings

### Critical

(無)

### Warning

- severity: Warning | confidence: 80 | 提出者: B
  - location: specs/photo-organization/spec.md「Tier 2」與「filename date parsing」Example 表 + tasks.md 2.1
  - summary: 核心規則「scanning candidates left to right and taking the first candidate that passes validation」沒有任何 Example 列覆蓋多候選情境(十一列全為單一候選或零候選),「第一候選被拒、改取第二候選」無測試約束;分隔形式的數字段邊界(如 `12025-10-28.png`)也無 Example
  - recommendation: Example 表補多候選列與分隔形式邊界列,同步更新 task 2.1 列數

### Suggestion

- severity: Suggestion | confidence: 75 | 提出者: A+B(原 Warning,信心過濾降級)
  - location: tasks.md 1.1 ↔ src/main/kotlin/org/photocollection/core/ExifReader.kt:44
  - summary: task 1.1 驗收要求「整個 test source set 編譯通過」,但 `CaptureDate` 加無預設值欄位後 ExifReader.kt 的 `CaptureDate(date)` 單參數建構必然編譯失敗,該檔修改列在 4.1,1.1 在自身範圍內驗收不可達成;MovePlannerTest/ModelsTest 的既有 `CaptureDate` 建構亦未點名
  - recommendation: task 1.1 補 ExifReader.kt `resolve()` 的最小過渡修改(補 `DateSource.EXIF`),不得以預設參數值解套;點名兩個測試的建構更新
- severity: Suggestion | confidence: 65 | 提出者: B(原 Warning,信心過濾降級)
  - location: tasks.md 4.1 + design.md 測試策略 ↔ spec「full tier resolution」第 4、7 列
  - summary: 第 4 列「created 2025-11-01, modified 2025-10-30」在 APFS 上無法以 NIO 建構(design 自己已指出),但 task 4.1 仍要求以真實暫存檔測試;derive-not-hardcode 防 flaky 規則未延伸到 ExifReaderTest 的 tier-3 列
  - recommendation: 明定 read() 層級 tier-3 列僅斷言來源與由 mtime instant 推導的日期,「取較早者」由 `pick` seam 獨佔驗證;防 flaky 規則延伸到所有真實檔案 tier-3 測試
- severity: Suggestion | confidence: 50 | 提出者: A
  - location: design.md Implementation Contract ↔ specs/organizer-ui/spec.md ADDED + tasks.md 5.1
  - summary: spec 與 tasks 都有 per-source counts,但 design contract 的「行為」與「驗收標準」未提及,三者未對齊
  - recommendation: contract 行為與驗收標準各補一句摘要各來源筆數
- severity: Suggestion | confidence: 50 | 提出者: B
  - location: tasks.md 5.1 ↔ src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt:38
  - summary: 既有逐項列精確斷言(`IMG1.jpg  →  2024-03-15`)在 task 5.1 加入來源標示後必然失敗,tasks 未點名改寫
  - recommendation: task 5.1 補一句同步更新該字面斷言

(另有 3 筆 confidence < 50 的 findings 依信心過濾規則濾除,未列入本檔。)

## Rating

- quality_score: 8.5
- critical_gap: false
- 評語:工件整體已相當成熟:三層備援的決策鏈、對稱 sentinel、internal seam 測試策略、`MoveItem` 資料形狀在 design、spec、tasks 三者間高度一致,前四輪修正的痕跡明顯。但本輪仍有實質缺口使其未達通過門檻。最重要的是 Warning(80):tier 2 的核心規則「由左至右取第一組通過驗證的候選」在十一列 Example 中完全沒有多候選情境覆蓋——「第一候選被拒、改取第二候選」這條明文行為無任何測試約束,分隔形式的數字段邊界亦無 Example,等於規格最具演算法性質的部分缺少可驗證的錨點。其次,task 1.1 的驗收標準「整個 test source set 編譯通過」經查證不可達成:`CaptureDate` 加入無預設值的 `source` 欄位後,ExifReader.kt:44 的單參數建構必然編譯失敗,而該檔修改列在 4.1,任務切分與驗收宣告自相矛盾。另外 full tier resolution 第 4 列要求以真實暫存檔建構 design 自己已指明 APFS 上無法建構的日期組合;PlanResultViewTest 的精確字面斷言在 5.1 後必然失敗但未點名;per-source counts 在 contract 缺席造成三件套未完全對齊。無 confidence ≥ 80 的 Critical,故 critical_gap 為 false,但核心規則的測試空窗與驗收不可達成的任務定義使其尚不足以無摩擦交付。

## Fix Actions

- specs/photo-organization/spec.md:「filename date parsing」Example 表補 `IMG_20991231_20251028.png`(多候選取第二組)與 `12025-10-28.png`(分隔形式前鄰數字)兩列,表增至十三列(對應 Warning)
- tasks.md:1.1 補 ExifReader.kt `resolve()` 最小過渡修改(`CaptureDate(date, DateSource.EXIF)`,明文禁止預設參數值解套)與 MovePlannerTest/ModelsTest 建構更新;2.1 改十三列;4.1 補 read() 層級 tier-3 列的斷言方式(來源 + mtime instant 推導,「取較早者」由 `pick` seam 獨佔驗證);5.1 補既有字面斷言同步更新(對應 S1、S2、S4、Warning)
- design.md:Implementation Contract「行為」補計畫摘要各來源筆數、「驗收標準」的 UI 測試句同步;測試策略將 derive-not-hardcode 規則延伸到所有以真實檔案驗證 tier 3 結果的測試並明定 read() 層級列的斷言降階(對應 S2、S3)

## Decision

next_round

第 5 輪 quality_score 8.5 未達 > 9 門檻。1 項 Warning 與 4 項 Suggestion 已全數修正,進入第 6 輪(最後一輪)複審。
