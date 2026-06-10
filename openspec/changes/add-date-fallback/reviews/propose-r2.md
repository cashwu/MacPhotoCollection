# Propose Plus Review — Round 2

## Reviewer Findings

### Critical

(無)

### Warning

- severity: Warning | confidence: 85 | 提出者: B
  - location: proposal.md Impact / tasks.md 1.1 ↔ src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt
  - summary: design 契約把 `MoveItem` 改為三參數(無預設值),但 MoveExecutorTest.kt 有 7 處 `MoveItem(source, target)` 兩參數建構必須跟著改,proposal Impact 與 tasks 均未列該檔案,屬影響範圍盤點遺漏
  - recommendation: proposal Impact 的 Modified 清單加入 MoveExecutorTest.kt,tasks 1.1 註明同步更新該測試的 `MoveItem` 建構

### Suggestion

- severity: Suggestion | confidence: 70 | 提出者: A(原 Warning,信心過濾降級)
  - location: tasks.md 5.2 ↔ design.md Implementation Contract 驗收標準
  - summary: design 驗收標準明定「UI 測試驗證來源標示與 error list 新語意」,但 task 5.2 允許 UI 文案降級為 content review,兩工件對同一驗收條件強度不一致
  - recommendation: 對齊兩者——design 驗收標準改寫為「error list 新語意以資料層測試為主,UI 文案以測試或 content review 確認」
- severity: Suggestion | confidence: 70 | 提出者: B(原 Warning,信心過濾降級)
  - location: specs/photo-organization/spec.md「Tier 3 — file system dates」+ design.md
  - summary: sentinel 防衛未定義「修改日期也早於 1990(兩者皆 sentinel)」的行為;tier 2 拒絕未來日期但 tier 3 對未來檔案系統時間無檢查,政策不一致未言明;Example 未覆蓋
  - recommendation: 補規則與 Example——雙 sentinel 視同 tier 3 失敗進 error list;未來日期照常接受並明文記錄理由
- severity: Suggestion | confidence: 60 | 提出者: B
  - location: specs/photo-organization/spec.md「Tier 3」+ design.md Non-Goals
  - summary: tier 3 將 instant 轉 civil date 必經時區換算,spec 未明文使用系統預設時區;與 Non-Goals「不做時區轉換」原則的關係未說明(該原則實際僅適用 tier 1)
  - recommendation: spec 明文「using the system default time zone」,design Non-Goals 註明差異為已知且接受
- severity: Suggestion | confidence: 55 | 提出者: A
  - location: specs/photo-organization/spec.md(缺漏)↔ design.md「CaptureDate 增加來源欄位」/ tasks.md 1.1
  - summary: `MoveItem` 攜帶日期與來源是可觀察行為,但 master spec「Compute move plan grouped by capture date」未在 delta 中 MODIFIED 記載
  - recommendation: delta spec 補一條 MODIFIED,加入「each move item SHALL carry the resolved capture date and its source」

(另有 4 筆 confidence < 50 的 findings 依信心過濾規則濾除,未列入本檔。)

## Rating

- quality_score: 8.5
- critical_gap: false
- 評語:工件整體品質高:三層備援的決策理由完整、Implementation Contract 介面形狀明確、spec Example 表覆蓋充分,且第 1 輪四項高信心矛盾均已妥善修正。但作為實作交接文件仍有實質缺口:(1) 已驗證 MoveExecutorTest.kt 有 7 處兩參數 `MoveItem` 建構,而契約將 `MoveItem` 改為三參數無預設值,該檔案卻未列入 proposal Impact 與 tasks,實作者執行任務 1.1 時必然遇到清單外的編譯失敗;(2) design 驗收標準與 task 5.2 對 error list 驗證強度描述不一致,交接時會造成驗收歧義。其餘建議級問題屬規格嚴謹度的次要缺漏,單獨不足以扣到門檻以下,但合計使工件未達「實作者無需回頭追問」的完整度。無 confidence ≥ 80 的 Critical finding,故 critical_gap 為 false。

## Fix Actions

- proposal.md:Impact 的 Modified 清單加入 src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt(對應 Warning)
- design.md:Non-Goals 的「不做時區轉換」改寫,明文 tier 3 經 `ZoneId.systemDefault()` 換算屬已知且接受;「檔案系統日期取建立與修改日期較早者」決策補雙 sentinel 失敗與未來日期接受的規則與理由;Implementation Contract 的 `FileSystemDateReader` 介面、失敗模式、驗收標準同步更新(驗收標準改為「error list 新語意以資料層測試為主,UI 文案以測試或 content review 確認」,並補 MoveExecutorTest 三參數更新)(對應 S1、S2、S3)
- specs/photo-organization/spec.md:Tier 3 條文補系統預設時區、雙 sentinel 失敗、未來日期接受;「earlier of creation and modification」Example 表補雙 sentinel 與未來日期兩列;新增 MODIFIED「Compute move plan grouped by capture date」,加入 move item 攜帶日期與來源的 SHALL 條款(對應 S2、S3、S4)
- tasks.md:1.1 補 MoveExecutorTest.kt 同步更新與「Compute move plan grouped by capture date」需求對應;3.1 補時區、雙 sentinel、未來日期案例與六列 Example(對應 Warning、S2、S4)

## Decision

next_round

第 2 輪 quality_score 8.5 未達 > 9 門檻。1 項 Warning 與 4 項 Suggestion 已全數修正,進入第 3 輪複審。
