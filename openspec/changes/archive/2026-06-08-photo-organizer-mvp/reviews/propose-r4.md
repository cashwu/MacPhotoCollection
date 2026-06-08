# Propose Plus Review — Round 4

## Reviewer Findings

過濾規則：丟棄 confidence < 50；confidence ∈ [50, 80) 降為 Suggestion；僅 confidence ≥ 80 可列為 Critical / Warning。本輪無 Critical、無 Warning。Reviewer A 回報 artifact 集內部一致、無 ≥ 50 findings、建議 pass；Reviewer B 最高兩項為 confidence 70 / 65，依過濾降為 Suggestion。

### Suggestion

- **[B-F1] confidence 70** — `location`: specs/photo-organization「Execute move plan」+ design.md。`summary`: 「atomic move」措辭把耐久性與 fail-on-existing 混為一談；`ATOMIC_MOVE` 選項不保證目標已存在時拋例外。`recommendation`: 指明以「不帶 replace-existing 選項的 `Files.move`」（會拋 `FileAlreadyExistsException`）作為衝突偵測基礎。（B 提）
- **[B-F2] confidence 65** — `location`: specs/photo-organization「Extract capture date from EXIF」+ design.md。`summary`: 要落實「不做時區換算」，須讀原始 EXIF 字串值解析日期部分，不可用回傳 instant 的 `getDate()`（會套時區 / offset、跨午夜位移）。`recommendation`: 明述讀原始字串、禁用 instant 存取器。（B 提）
- **[B-F3] confidence 55** — `location`: specs/photo-organization「Scan source folder」。`summary`: macOS AppleDouble `._IMG.JPG` 會以不分大小寫比對命中 `.jpg`、污染錯誤清單。`recommendation`: 略過點開頭檔案。（B 提）
- **[B-F4] confidence 50** — `location`: design.md Risks。`summary`: 衝突失敗檔於後續掃描會再次出現並再次失敗，屬已接受的 MVP 行為（design risk 已載）。`recommendation`: 無需動作。（B 提）
- **[B-F5] confidence 50** — `location`: specs/photo-organization「Execute move plan」+ design.md。`summary`: 同日多筆應以 idempotent `Files.createDirectories` 建資料夾，避免第二筆失敗。`recommendation`: 指明 `createDirectories`（複數、idempotent）。（B 提）

Reviewer A 唯一觀察（來源資料夾消失之驗證）confidence 45、低於門檻，未列入；其於 spec 已以「日期子資料夾無法建立即該項失敗」之共用 scenario 涵蓋。

## Rating

- `quality_score`: 9.5
- `critical_gap`: false
- 說明（rater 依過濾後 findings 獨立評分）：本輪修正後，四項可行動 findings（B-F1 以不帶 replace-existing 的 `Files.move` 偵測衝突、B-F2 讀原始 EXIF 字串避免時區位移、B-F3 略過點開頭 AppleDouble sidecar、B-F5 idempotent `createDirectories`）皆已於 spec / design 解決，B-F4 為已載於 design risk 的接受行為。Reviewer A 認定 artifact 集內部一致且無 finding，Reviewer B 殘留項皆 confidence < 80（Suggestion）且已處理。無 Critical / Warning 存活，`critical_gap` 為 false；殘留面向符合刻意最小化的個人工具 MVP。分數略高於門檻而非滿分，反映此 spec 歷經數輪才補齊數個非顯而易見的耐久性 / 正確性細節，但最終狀態穩固。

## Fix Actions

- specs/photo-organization/spec.md：「Execute move plan」改為「不帶 replace-existing 選項的 `Files.move`」並註明 `ATOMIC_MOVE` 非衝突保證基礎、以 idempotent create-directories 建資料夾（B-F1/B-F5）；「Extract capture date from EXIF」加入「讀原始 EXIF 字串值直接解析、禁用回傳 instant 之存取器」（B-F2）；「Scan source folder」加入「略過點開頭檔案」並於表格加入 `._IMG.JPG`、`.DS_Store` 列（B-F3）。
- design.md：掃描略過點開頭檔案；EXIF 讀原始字串；execute 用 idempotent `createDirectories` 與不帶 replace-existing 選項的 `Files.move`（含 `ATOMIC_MOVE` 說明）（B-F1/B-F2/B-F3/B-F5）。
- tasks.md：3.1 加入略過 `._*`/`.DS_Store`；3.2 加入讀原始 EXIF 字串解析；4.2 改為 idempotent `createDirectories` 與不帶 replace-existing 選項的 `Files.move`。
- B-F4 無需動作（已為 design risk 載明之接受行為）。

## Decision

passed
