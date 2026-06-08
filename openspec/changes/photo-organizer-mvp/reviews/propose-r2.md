# Propose Plus Review — Round 2

## Reviewer Findings

過濾規則：丟棄 confidence < 50；confidence ∈ [50, 80) 降為 Suggestion；僅 confidence ≥ 80 可列為 Critical / Warning。本輪無 Critical。

### Warning

- **[B-F1] confidence 80** — `location`: specs/organizer-ui「Execute on user confirmation」+ specs/photo-organization「Execute move plan」。`summary`: 不同來源的同名檔（如兩台相機的 `IMG_0001.jpg`、同日）會永久卡在來源根目錄——第二筆每次重跑都因衝突再次失敗（自動改名為 Non-Goal）；而 UI 僅回報失敗「數量」，使用者無從定位或處理卡住的檔案。`recommendation`: UI 逐項列出失敗檔案與原因，並於 design / proposal 記載「同名衝突檔永久留在來源根目錄」為 MVP 可接受行為。（B 提；A-F? 無）

### Suggestion

- **[A-F1] confidence 75** — `location`: tasks.md 4.1 vs design / spec「Compute move plan」。`summary`: 「目標路徑須以平台 path API 建構、非字串相接」之 SHALL 無任何驗證 target。`recommendation`: 於 4.1 加入斷言目標路徑等於 `Path(sourceFolder).resolve(...)`。（A 提）
- **[B-F2] confidence 70** — `location`: specs/organizer-ui。`summary`: 失敗僅以數量呈現、無逐項細節（與 B-F1 同源）。`recommendation`: 同 B-F1，逐項列出失敗。（B 提）
- **[B-F3] confidence 60** — `location`: specs/photo-organization「date source resolution」表格。`summary`: `DateTimeDigitized = 2022-01-02` 為僅日期，與表內其他列的 `yyyy:MM:dd HH:mm:ss` 格式不一致，可能誤導測試作者餵入會被判為「無日期」的值。`recommendation`: 改為完整 EXIF 格式 `2022:01:02 08:30:00`。（B 提）
- **[A-F3] confidence 55** — `location`: tasks.md 3.2 vs proposal / spec。`summary`: HEIC / RAW 列為支援格式但無驗證樣本。`recommendation`: 加入樣本或於 design 註明倚賴函式庫、不另行逐格式驗證。（A 提）
- **[B-F4] confidence 55** — `location`: design / spec「Execute move plan」。`summary`: 「same-filesystem rename」寫成硬性假設，於 mount point / symlink / 外接碟可能不成立。`recommendation`: 改為「預期為 same-filesystem，但不得倚此略過錯誤處理」。（B 提）
- **[B-F5] confidence 50** — `location`: specs/photo-organization「Scan」。`summary`: 已整理（根目錄無影像）資料夾 → 空計畫 → no-op 成功未明述。`recommendation`: 加入 acceptance 說明此為成功 no-op 而非錯誤。（B 提）

被調查後撤回（不列入）：A 的路徑非遞迴已由 `sub/ (dir)` 表格列驗證、執行確認需求一致等。

## Rating

- `quality_score`: 7
- `critical_gap`: false
- 說明（rater 依過濾後 findings 獨立評分）：本輪無存活 Critical，`critical_gap` 為 false。主要問題為 B-F1/B-F2 群集（conf 80/70）——同名檔永久卡住且 UI 僅呈現失敗總數，使用者無法定位處理，屬可行動的 MVP 可用性缺口。其餘為次要建議：未驗證的 path-API SHALL（A-F1）、未驗證 HEIC/RAW（A-F3）、內部不一致的 EXIF 範例值（B-F3）、過度硬性的 same-filesystem 假設（B-F4）、未明述的空計畫 no-op（B-F5）。整體為穩固可實作的提案，但因一個明顯可用性缺口與數個小幅打磨項，給 7 分。因 pass 條件要求 `quality_score > 9`，本輪判定 `next_round`。

## Fix Actions

- specs/organizer-ui/spec.md：「Execute on user confirmation」需求加入「逐項列出失敗檔案與原因」，並新增「Some items fail」scenario（B-F1/B-F2）。
- specs/photo-organization/spec.md：修正「date source resolution」表格兩列為完整 EXIF 格式（B-F3）；新增「Already-organized folder」scenario 說明空計畫 no-op（B-F5）；「Execute move plan」將 same-filesystem 改為「預期、但不得倚此略過錯誤處理」（B-F4）；「Compute move plan」明述目標路徑以平台 path API 建絕對路徑（A-F1 對應 spec 面）。
- design.md：UI 行為加入逐項列出失敗（B-F1）；Risks 新增「同名檔永久卡住為 MVP 可接受行為」與「HEIC/RAW 倚賴函式庫、不另行逐格式驗證」（B-F1/A-F3）。
- tasks.md：4.1 加入目標路徑 `Path.resolve` 斷言（A-F1）；5.4 改為刻意製造同名衝突並驗證逐項失敗列出（B-F1/B-F2）；3.1 加入「Already-organized folder」情境驗證（B-F5）。

## Decision

next_round
