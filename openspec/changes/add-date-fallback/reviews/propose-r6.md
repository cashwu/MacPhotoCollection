# Propose Plus Review — Round 6

## Reviewer Findings

### Critical

(無)

### Warning

(無 — 本輪所有 finding 信心皆 < 80,經信心過濾後無任何 Critical 或 Warning)

### Suggestion

- severity: Suggestion | confidence: 70 | 提出者: B
  - location: specs/photo-organization/spec.md「Tier 2」與「filename date parsing」Example 表 + tasks.md 2.1
  - summary: 「由左至右取第一組通過驗證者」缺跨樣式多候選的 Example;三種樣式輪流掃描的實作會通過全部列卻違反 SHALL(如 `2025_10_28_20240101.png` 可能錯取 2024-01-01)
  - recommendation: Example 表加跨樣式多候選列,spec 明文「依起始位置取最前者,不分樣式」
- severity: Suggestion | confidence: 70 | 提出者: A
  - location: proposal.md Capabilities「Modified Capabilities」photo-organization 條目
  - summary: delta spec 修改了兩條 photo-organization 需求,但 proposal 只列出「Extract capture date from EXIF」,漏「Compute move plan grouped by capture date」
  - recommendation: 補上後者的修改說明(move item 攜帶 capture date 與 source)
- severity: Suggestion | confidence: 60 | 提出者: A
  - location: tasks.md 4.1 ↔ spec「full tier resolution」第六列
  - summary: 「含 EXIF → seam、無 EXIF → 真實檔案」二分法無法套用到「metadata read throws → FILENAME」列(seam 以 EXIF 解析結果為參數,無法表達讀取拋例外)
  - recommendation: 該列明定以真實 corrupt 暫存檔在 read() 層級測試
- severity: Suggestion | confidence: 55 | 提出者: A
  - location: design.md Decisions「解析器拆為獨立物件,組合層負責優先序」
  - summary: 「ExifReader.read 的呼叫端或改名後(實作時決定)」與已定案的 contract/tasks(組合邏輯放在未改名的 ExifReader.read)矛盾,屬設計內部陳舊描述
  - recommendation: 改為「放在現有 ExifReader.read 內(維持現名)」
- severity: Suggestion | confidence: 50 | 提出者: B
  - location: specs/photo-organization/spec.md「filename date parsing」Example 表
  - summary: 完整數字段的後鄰數字邊界只有 compact 形式有 Example;分隔形式的後鄰數字案例(如 `2025-10-281.png`)無測項
  - recommendation: 補分隔形式後鄰數字案例

(另有 3 筆 confidence < 50 的 findings 依信心過濾規則濾除:tier 2 today 時區未言明、混合分隔符未規範、per-source counts 0 筆顯示與否。主代理人一併處理為合理澄清。)

## Rating

- quality_score: 9.5
- critical_gap: false
- 評語:第 6 輪五項 Suggestion 全部確實修正且未引入矛盾:Tier 2 跨樣式取最前候選已在 spec 規則句與 Example(`2025_10_28_20240101.png`)雙重落實;proposal Modified Capabilities 補上 move plan 條目;tasks 4.1 明定 corrupt→FILENAME 列以真實垃圾位元組暫存檔在 read() 層測試,避開 seam 無法表達例外的問題;design 改名矛盾改為「維持現名 ExifReader.read」與 contract/tasks 完全對齊;分隔形式前後鄰數字邊界形成對稱 Example。三項過濾澄清(today 系統時區每批取一次、混合分隔符不接受、0 筆來源仍顯示)亦各有對應文字。跨工件交叉核對 enum 值、seam 命名(pick/read)、1990-01-01 sentinel 邊界、MoveItem.date/CaptureDate.source 資料形狀、16 列 Example 與 tasks「十六列」數量皆一致,設計決策均附替代方案與否決理由,作為實作交接文件完整且可執行。未達滿分僅因 tier 3 真實檔案測試對 birth time 不可寫入的平台限制仍留有 smoke-only 的固有殘餘風險(已在 design 明確標註並接受),非工件缺陷。無 confidence ≥ 80 的 Critical finding。

## Fix Actions

- specs/photo-organization/spec.md:Tier 2 明文「三種樣式一併比對、依起始位置取最前的通過驗證者,不分樣式輪流掃描」、分隔符必須一致、current date 以系統預設時區取得;Example 表新增跨樣式多候選(`2025_10_28_20240101.png`)、分隔形式後鄰數字(`shot_2025-10-281.png`)、混合分隔符(`mix_2025-10_28.png`)三列,表增至十六列
- specs/organizer-ui/spec.md:per-source counts 補「零筆來源仍顯示」規則,Scenario 改為含 0 筆來源案例
- proposal.md:Modified Capabilities 的 photo-organization 條目補「Compute move plan grouped by capture date」需求修改
- design.md:決策句改為「放在現有 ExifReader.read 內(維持現名)」;檔名解析規則補 today 取得時機(每批一次、`LocalDate.now(ZoneId.systemDefault())`)、跨樣式起始位置優先序、分隔符一致
- tasks.md:2.1 改為十六列並列舉新邊界;4.1 明定 corrupt→FILENAME 列以真實暫存檔在 read() 層級測試

## Decision

passed

第 6 輪 quality_score 9.5 > 9 且 critical_gap == false,達成通過條件。品質閘門通過,結束 review 迴圈。
