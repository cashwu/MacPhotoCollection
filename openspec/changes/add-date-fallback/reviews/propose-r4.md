# Propose Plus Review — Round 4

## Reviewer Findings

### Critical

(無)

### Warning

- severity: Warning | confidence: 85 | 提出者: A
  - location: tasks.md 5.2 與 4.1(對照 design.md Implementation Contract 失敗模式、photo-organization delta spec Scenario「File attributes cannot be read」)
  - summary: task 5.2 宣稱「資料層以 ExifReaderTest 確認 NoDate 僅來自 IO 失敗與雙 sentinel 兩條路徑(任務 4.1)」,但 task 4.1 列舉的 ExifReaderTest 驗證項沒有任何案例產出 `NoDate`(full tier resolution 全為 Found);spec Scenario「File attributes cannot be read」沒有被任何 task 明文列為測試目標
  - recommendation: task 4.1 明文加入 NoDate 路徑案例(tier 3 回傳 null 時 `read()` 回傳 `NoDate`、`readAll` 不中斷批次,以可注入的 tier 3 結果測試),task 5.2 同步引用

### Suggestion

- severity: Suggestion | confidence: 75 | 提出者: B(原 Warning,信心過濾降級)
  - location: design.md「檔案系統日期取建立與修改日期較早者」/ Implementation Contract + tasks.md 3.1、4.1
  - summary: 工件要求 FileSystemDateReaderTest 以真實暫存檔覆蓋全部七列,但 macOS 上 Java NIO 設定 `creationTime` 被靜默忽略、APFS 會下拉 birth time,「建立日期晚於修改日期」的屬性組合造不出來;design 未保留 tier 3 internal seam,實作必然偏離驗收標準
  - recommendation: 比照 EXIF 慣例加入 tier 3 seam `pick(creationDate, modifiedDate)`,七列在 seam 上測,真實暫存檔只測 smoke 與 IO 失敗
- severity: Suggestion | confidence: 70 | 提出者: B
  - location: tasks.md 5.2 + src/main/kotlin/org/photocollection/ui/App.kt:195
  - summary: task 5.2 指向「清單標題文案」,但標題不含舊語意字樣;真正承載舊語意的是 PlanResultView 的「計畫:…筆無日期」(PlanResultViewTest 有字面斷言),照字面執行會是 no-op
  - recommendation: 文案目標改為 PlanResultView 摘要字串,同步更新 PlanResultViewTest 字面斷言
- severity: Suggestion | confidence: 60 | 提出者: B
  - location: design.md Risks(風險 1、5)+ organizer-ui delta「Indicate capture date source in plan preview」
  - summary: 風險緩解仰賴「使用者在計畫預覽中檢查」,但預覽是 160dp 捲動視窗,上千檔案時逐項檢查不可行;無來源筆數彙總,殘餘風險未言明
  - recommendation: 計畫摘要加入各來源筆數,並在 Risks 記錄殘餘風險
- severity: Suggestion | confidence: 55 | 提出者: B
  - location: design.md 測試策略 + tasks.md 3.1
  - summary: tier 3 的 `ZoneId.systemDefault()` 無注入點,測試硬編 Example 日期在不同時區 CI 會跨日 flaky,撰寫約束未提示
  - recommendation: 註明預期值由寫入 instant 經同一時區換算推導;採納 seam 後此問題大幅消除
- severity: Suggestion | confidence: 55 | 提出者: B
  - location: specs/photo-organization/spec.md「Tier 1 — EXIF」
  - summary: malformed `DateTimeOriginal` + 完好 `DateTimeDigitized` 不會退回 Digitized 而直接落入備援(沿襲現行 `resolve()` 行為),本變更使後果從「進 error list」變為「靜默套上備援日期」,語意惡化未記錄
  - recommendation: 維持現行為但在 design Risks 記為已知接受,並在 Example 表加一列固定此行為

(另有 1 筆 confidence 45 的 finding 依信心過濾規則濾除,未列入本檔。)

## Rating

- quality_score: 7.5
- critical_gap: false
- 評語:工件整體一致性經前三輪修正後已達高水準(兩個 MODIFIED 需求與 master spec 逐句一致、`MoveItem` 建構處數與 Example 列數相符、error list 條件與 sentinel 規則五份工件措辭一致),但本輪仍存在一個高信心(85)的測試覆蓋斷層:task 5.2 將 error list 語意的資料層驗證寄託於 task 4.1 的 ExifReaderTest,而 4.1 列舉的驗證項全為 Found 路徑,沒有任何案例實際產出 NoDate,且 spec 的「File attributes cannot be read」Scenario 無任何 task 明文列為測試目標——這使新 error list 語意的兩條 NoDate 路徑在交接文件上形同未被驗證。此外,FileSystemDateReaderTest 以真實暫存檔覆蓋七列的驗收標準在 macOS/APFS 上有具體列無法構造,而 design 未保留 tier 3 internal seam,實作者大概率被迫偏離設計;task 5.2 指向的「清單標題文案」實際不承載舊語意,照字面執行會是 no-op。其餘建議信心較低。無 confidence ≥ 80 的 Critical finding,critical_gap 為 false;但測試計畫斷層與不可達驗收標準使工件尚未達到可直接執行的完整度,未通過品質閘門。

## Fix Actions

- design.md:測試策略補 tier 3 internal seam `pick(creationDate, modifiedDate)`(macOS 無法寫 creationTime 的依據)、NoDate 路徑以可注入 tier 3 結果測試、smoke 測試預期值由 instant 換算推導不得硬編;Implementation Contract 的 `FileSystemDateReader` 補 seam;驗收標準改寫(七列在 seam 上測、ExifReaderTest 補 NoDate 路徑案例);Risks 補「上千檔案逐項檢查不可行 → 摘要顯示各來源筆數,殘餘風險已知接受」與「malformed Original 不退 Digitized 屬已知接受」兩條(對應 W1、S1、S3、S4、S5)
- specs/organizer-ui/spec.md:「Indicate capture date source in plan preview」補「The plan preview summary SHALL additionally show the number of files per date source」與 per-source counts Scenario(對應 S3)
- specs/photo-organization/spec.md:「full tier resolution」Example 補 malformed `DateTimeOriginal` + 完好 `DateTimeDigitized` 一列(表增至七列),固定既有 tier 1 語意(對應 S5)
- proposal.md:What Changes 的 UI 條目補「計畫摘要顯示各來源筆數」(對應 S3)
- tasks.md:3.1 改為在 `pick` seam 上覆蓋七列、暫存檔只測 smoke(instant 換算推導)與 IO 失敗;4.1 改七列並新增 NoDate 路徑案例(對應 spec Scenario「File attributes cannot be read」);5.1 補摘要各來源筆數驗證;5.2 文案目標改為 PlanResultView「X 筆無日期」摘要字串並更新 PlanResultViewTest 字面斷言、驗證引用 4.1 與 3.1 的 NoDate 案例(對應 W1、S1、S2、S3、S4)

## Decision

next_round

第 4 輪 quality_score 7.5 未達 > 9 門檻。1 項 Warning 與 5 項 Suggestion 已全數修正,進入第 5 輪複審。
