# Propose Plus Review — Round 3

## Reviewer Findings

### Critical

(無)

### Warning

- severity: Warning | confidence: 90 | 提出者: A+B
  - location: design.md「Implementation Contract — 行為」、proposal.md What Changes / Modified Capabilities、tasks.md 4.1 / 5.2、specs/organizer-ui/spec.md「Display file list and error list」
  - summary: error list 條件在多處工件互相矛盾——photo-organization delta spec 與 design「失敗模式」明定雙 sentinel(兩日期皆早於 1990-01-01)也回 `NoDate` 進 error list,但 design「行為」、proposal「第三層永遠有值」「僅剩 IO 錯誤」、task 4.1「僅當第三層 IO 失敗」、task 5.2 文案、organizer-ui delta 的 in-practice 括註均只寫 IO 錯誤
  - recommendation: 統一各處為「IO 錯誤,或兩個檔案系統日期皆早於 1990-01-01(雙 sentinel)」
- severity: Warning | confidence: 80 | 提出者: B
  - location: specs/photo-organization/spec.md「Tier 3 — file system dates」+ design.md「檔案系統日期取建立與修改日期較早者」
  - summary: 1990 sentinel 防衛只保護 creation date,不對稱地漏掉「creation 正常、mtime 早於 1990」(zip 解壓寫入 DOS epoch 1980-01-01 的實際情境),依字面「use the earlier of the two」檔案會被歸進 `1980-01-01` 資料夾,違背防衛目的;Example 未覆蓋
  - recommendation: 防衛規則改為對稱——任一日期早於 1990-01-01 即忽略該日期、改用另一個;兩者皆早於 1990 才回 NoDate;補 Example 列並同步 design 與 task 3.1

### Suggestion

- severity: Suggestion | confidence: 70 | 提出者: A+B(原 Warning,信心過濾降級)
  - location: tasks.md 1.1 ↔ src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt:25-26
  - summary: task 1.1 只列 MoveExecutorTest 7 處兩參數 `MoveItem` 建構,漏列 PlanResultViewTest.kt 的 2 處;`MoveItem` 加無預設值欄位後整個 test source set 無法編譯,task 1.1 的驗收門檻無法達成
  - recommendation: 將 PlanResultViewTest.kt 的 2 處納入 task 1.1(共 9 處),驗證門檻改為整個 test source set 編譯通過
- severity: Suggestion | confidence: 70 | 提出者: B(原 Warning,信心過濾降級)
  - location: tasks.md 4.1、design.md 驗收標準(ExifReaderTest 三層優先序案例)
  - summary: 「full tier resolution」有三列需要真實 EXIF 圖檔,但 repo 無測試 fixture 且 metadata-extractor 為唯讀函式庫,工件未說明測試檔案如何產生或是否留 internal seam
  - recommendation: design 明定測試策略——含 EXIF 的列在 internal seam(以 EXIF 解析結果為參數的組合函式)上測試,無 EXIF 的列以真實暫存檔測試;更新 task 4.1
- severity: Suggestion | confidence: 55 | 提出者: B
  - location: specs/photo-organization/spec.md「Tier 2 — file name」+ Example 表
  - summary: 條文「whose year is between 1990 and the current date inclusive」年份與日期型別混淆,「等於 today」與「1990-01-01」兩個邊界無 Example 列
  - recommendation: 改寫為「年份 ≥ 1990 且日期不晚於今天(上下界皆含)」並補兩列邊界 Example
- severity: Suggestion | confidence: 55 | 提出者: B
  - location: specs/photo-organization/spec.md MODIFIED「Compute move plan grouped by capture date」
  - summary: 新增的 SHALL(move item 攜帶日期與來源)沒有對應的 Scenario/Example 驗證情境,「plan grouping」Example 沿用舊版未標注來源
  - recommendation: Scenario THEN 與「plan grouping」Example 標注 EXIF/FILENAME 來源,使 MovePlannerTest 有可追溯依據

## Rating

- quality_score: 6.5
- critical_gap: false
- 評語:工件整體結構完整、設計取捨交代清楚且 Example 覆蓋面廣,但作為實作交接文件存在兩個 confidence ≥ 80 的實質缺陷:其一,error list 的成立條件在五處工件位置互相矛盾——photo-organization delta spec 與 design 失敗模式明定雙 sentinel 也回 NoDate 進 error list,但 design 行為段、proposal、task 4.1、task 5.2 與 organizer-ui delta 均寫成「僅 IO 錯誤」,實作者依任一說法都會與另一半工件不符,這是核心行為層級的規範性矛盾;其二,tier 3 的 1990 sentinel 防衛不對稱,規格字面上「creation 正常、mtime 為 1980 DOS epoch」的常見情境(zip 解壓)會依「取較早者」把檔案歸進 1980-01-01 資料夾,直接違背防衛規則的設計目的,且 Example 表未覆蓋此列。此外 task 1.1 漏列 PlanResultViewTest 兩處兩參數 MoveItem 建構,依該任務驗收門檻將無法編譯通過;EXIF fixture 來源、tier 2 年份邊界 Example、MoveItem 新 SHALL 缺驗證情境等缺口也降低可驗證性。無 Critical 級 finding,故無 critical gap,但上述矛盾與規格漏洞必須先修正才能交付實作,未達通過門檻。

## Fix Actions

- proposal.md:「第三層永遠有值…僅剩 IO 錯誤」改為「第三層幾乎永遠有值…IO 錯誤、或兩個檔案系統日期皆早於 1990-01-01(雙 sentinel)」;Modified Capabilities 的 organizer-ui 括註同步(對應 W1)
- design.md:「行為」句補雙 sentinel;「檔案系統日期取建立與修改日期較早者」決策改為對稱防衛(含 DOS epoch 情境與理由);Implementation Contract 的 `FileSystemDateReader` 介面改對稱;「解析器拆為獨立物件」決策補測試策略(internal seam);驗收標準補 PlanResultViewTest 三參數更新、對稱 sentinel、上下界邊界、internal seam 測試方式;Risks 的 sentinel 條目改對稱(對應 W1、W2、S1、S2)
- specs/photo-organization/spec.md:Tier 3 條文改對稱 sentinel 防衛並補「modified sentinel」「雙 sentinel(1970/1980)」Example 列(表增至七列);Tier 2 條文改寫為「年份 ≥ 1990 且不晚於今天,上下界皆含」並補 `pic_19900101.png`、`shot_2026-06-10.png` 兩列邊界 Example(表增至十一列);MODIFIED「Compute move plan grouped by capture date」的 Scenario THEN 與「plan grouping」Example 標注 (date, source)(對應 W2、S3、S4)
- specs/organizer-ui/spec.md:「Display file list and error list」的 in-practice 括註補雙 sentinel(對應 W1)
- tasks.md:1.1 改為共 9 處 `MoveItem` 建構(MoveExecutorTest 7 處 + PlanResultViewTest 2 處)且門檻改為整個 test source set 編譯通過;2.1 改十一列與上下界邊界;3.1 改對稱防衛七列;4.1 補「第三層回傳 null(IO 失敗或雙 sentinel)」與 internal seam 測試方式;5.2 文案與驗證涵蓋兩條 NoDate 路徑(對應 W1、W2、S1、S2、S3)

## Decision

next_round

第 3 輪 quality_score 6.5 未達 > 9 門檻。2 項 Warning 與 4 項 Suggestion 已全數修正,進入第 4 輪複審。
