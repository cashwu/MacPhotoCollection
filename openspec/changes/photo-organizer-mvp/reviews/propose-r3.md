# Propose Plus Review — Round 3

## Reviewer Findings

過濾規則：丟棄 confidence < 50；confidence ∈ [50, 80) 降為 Suggestion；僅 confidence ≥ 80 可列為 Critical / Warning。本輪無 Critical。

### Warning

- **[A-F1] confidence 85** — `location`: specs/photo-organization「Execute move plan」vs design.md Implementation Contract vs tasks.md 4.2。`summary`: spec 寫「預期為 same-filesystem rename，但實作 SHALL NOT 倚此略過錯誤處理」，design / 4.2 卻寫成「以 same-filesystem rename 進行」，於 normative 子句上語氣相反。`recommendation`: 對齊三者措辭並指明 atomic move 機制。（A 提）

### Suggestion

- **[B-F1] confidence 70** — `location`: spec / design「Execute move plan」。`summary`: 衝突偵測應用「目標已存在即失敗」的 atomic move（`Files.move` 不加 `REPLACE_EXISTING`、攔截 `FileAlreadyExistsException`），而非 `exists()` 再搬，以消除 TOCTOU / 覆蓋風險。`recommendation`: 指明該機制。（B 提）
- **[B-F2] confidence 65** — `location`: spec / design「Scan」「Execute move plan」。`summary`: macOS 預設檔案系統不分大小寫；副檔名比對與同名衝突邏輯須不分大小寫（相機輸出 `.JPG`）。`recommendation`: 副檔名比對不分大小寫，並以檔案系統層級衝突偵測處理僅大小寫不同的碰撞。（B 提）
- **[B-F4] confidence 60** — `location`: specs/organizer-ui「Preview selected photo」。`summary`: HEIC / RAW 雖列為支援，但 Compose Desktop / Skia 無法解碼預覽，會失敗 / 留白。`recommendation`: 加入「預覽不可用」佔位 fallback。（B 提）
- **[B-F3] confidence 55** — `location`: spec / design「Execute move plan」。`summary`: 日期路徑被非資料夾佔用時 `createDirectories` 會拋例外。`recommendation`: 視為該項失敗、不中斷整批。（B 提）
- **[B-F5] confidence 55** — `location`: specs/organizer-ui「Execute on user confirmation」。`summary`: 執行後 re-scan 可能清掉逐項失敗報告。`recommendation`: 失敗報告（檔案 + 原因）於刷新後仍保留。（B 提）
- **[A-F2] confidence 70** — `location`: tasks.md 4.2。`summary`: 「Successful execution」無衝突路徑未於 4.2 單獨斷言。`recommendation`: 於 4.2 加入無衝突成功路徑斷言。（A 提）
- **[A-F4] confidence 55** — `location`: proposal Impact vs tasks。`summary`: proposal 列 Main.kt 進入點但無 task 提及建立。`recommendation`: 於 1.1 加入 Main.kt 進入點。（A 提）
- **[B-F6] confidence 50** — `location`: spec「Execute move plan」。`summary`: scan 與 execute 之間來源資料夾被外部刪除大致已被 catch-all 涵蓋，唯來源根被刪時 `createDirectories` 行為未明。`recommendation`: 併入 B-F3 的「建立失敗即該項失敗」即涵蓋。（B 提）

被調查後撤回（不列入）：UI 採手動驗證為刻意且一致（A）。

## Rating

- `quality_score`: 8
- `critical_gap`: false
- 說明（rater 依過濾後 findings 獨立評分）：本輪無存活 Critical，`critical_gap` 為 false。唯一 Warning（A-F1）與主要 Suggestion（B-F1～B-F5、A-F2、A-F4）皆已於本輪修正：atomic move 機制已指明並對齊 design / task 措辭、以 atomic `Files.move` 消除 TOCTOU、副檔名不分大小寫並以檔案系統層級偵測衝突、非資料夾佔用路徑視為該項失敗、加入「預覽不可用」佔位、失敗報告刷新後保留、4.2 加入成功路徑斷言、1.1 加入 Main.kt。殘留僅低信心（50–60）且範圍極小（B-F6 已大致由 catch-all 涵蓋），無 normative 載重缺口。給 8 分，因 pass 條件要求 `quality_score > 9`，本輪判定 `next_round`。

## Fix Actions

- specs/photo-organization/spec.md：「Execute move plan」改寫——指明不帶 `REPLACE_EXISTING` 的 atomic move、靠攔截已存在失敗偵測衝突、消除 TOCTOU、單一機制涵蓋既有 / 批次內 / 大小寫碰撞（A-F1/B-F1/B-F2）；日期子資料夾「無法建立（含非資料夾佔用、來源不存在）即該項失敗不中斷」（B-F3/B-F6）。「Scan」副檔名比對不分大小寫、表格加入 `b.PNG`、`IMG.JPG` 列（B-F2）。
- specs/organizer-ui/spec.md：「Preview selected photo」加入「預覽不可用」佔位需求與兩個 scenario（B-F4）；「Execute on user confirmation」加入失敗報告刷新後保留（B-F5）。
- design.md：掃描不分大小寫；execute 改為 atomic move + 不倚 rename 略過錯誤 + 非資料夾路徑失敗；UI 行為加入預覽 fallback（A-F1/B-F1/B-F2/B-F3/B-F4）。
- tasks.md：1.1 加入 Main.kt 進入點與 `./gradlew run` 驗證（A-F4）；3.1 副檔名不分大小寫（B-F2）；4.2 加入 atomic move 機制、無衝突成功路徑斷言、非資料夾路徑失敗斷言（A-F1/A-F2/B-F1/B-F3）；5.2 加入 HEIC「預覽不可用」驗證（B-F4）；5.4 加入刷新後失敗清單仍可見（B-F5）。

## Decision

next_round
