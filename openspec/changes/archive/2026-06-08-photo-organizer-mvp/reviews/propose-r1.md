# Propose Plus Review — Round 1

## Reviewer Findings

過濾規則：丟棄 confidence < 50；confidence ∈ [50, 80) 降為 Suggestion；僅 confidence ≥ 80 可列為 Critical / Warning。

### Critical

- **[B1] confidence 90** — `location`: specs/photo-organization/spec.md「Execute move plan」。`summary`: 同一批次內兩個來源檔對應到相同目標路徑的衝突未被涵蓋，原衝突規則只處理「執行前磁碟已存在同名檔」。`recommendation`: 將衝突定義擴及「批次內較早項目已搬入該路徑」，新增對應 scenario 與 example。

### Warning

- **[A1] confidence 100** — `location`: design.md 核心行為（掃描）。`summary`: design 漏掉 spec 已規範的「不遞迴子資料夾」。`recommendation`: 於 design 掃描行為補上不遞迴敘述。
- **[A2] confidence 100** — `location`: proposal.md Non-Goals。`summary`: 同名衝突 Non-Goal 句子雙重否定、語意自相矛盾。`recommendation`: 改寫為「不提供進階衝突策略（如自動改名）；僅採最小跳過並回報失敗策略」。
- **[B3] confidence 80** — `location`: specs/photo-organization「Execute move plan」+ design.md Implementation Contract。`summary`: 未規範「失敗時來源檔保持不變」與目標為同檔案系統（rename 而非 copy+delete）。`recommendation`: 於 spec 與 design 明確 same-filesystem rename 及失敗時來源不變。
- **[B4] confidence 85** — `location`: specs/photo-organization「Extract capture date from EXIF」。`summary`: 未指定 EXIF 日期標籤與時區處理，午夜前後照片可能落到錯誤日期資料夾。`recommendation`: 指定 `DateTimeOriginal`(0x9003) 為主、`DateTimeDigitized`(0x9004) 退回，取當地民用日期不做時區換算。
- **[B5] confidence 80** — `location`: specs/organizer-ui 全部 scenario + design.md UI 行為。`summary`: 無並行模型，掃描 / EXIF / 執行可能在 UI 執行緒造成大資料夾凍結。`recommendation`: 新增非功能需求：長時間操作於 UI 執行緒外（coroutine / `Dispatchers.IO`）並顯示進行中狀態。

### Suggestion

- **[B8] confidence 60** — 惡意 / 損壞 EXIF（含 `0000:00:00 00:00:00` 哨兵）應併入「無日期」並逐檔攔截例外。（A 提）
- **[B6] confidence 70** — PNG / GIF 通常無 EXIF 日期，會大量落入錯誤清單，應明示為預期行為。（B 提）
- **[B2] confidence 75** — 重複執行同一資料夾的 idempotency 應說明（不遞迴掃描已能避免重掃）。（B 提）
- **[B9] confidence 55** — 執行後清單變陳舊，應刷新顯示狀態避免重複執行。（B 提）
- **[B7] confidence 65** — 目標路徑正規化（絕對路徑、平台 path API、單一日期 segment）未完整界定。（B 提）
- **[A3] confidence 70** — organizer-ui 各 scenario 僅靠手動驗證；屬桌面 / Compose 性質，可接受。（A 提）
- **[A4] confidence 60** — proposal 摘要較 design / spec 精簡（容許，higher-level）。（A 提）

## Rating

- `quality_score`: 9
- `critical_gap`: false
- 說明（rater 對修正後 artifacts 獨立評分）：rater 逐項比對修正後 artifact 內文，所有 Critical 與 Warning 皆已被現有文字反駁——B1 的批次內衝突已於「Execute move plan」需求以專屬 scenario 與 example 處理、design 同步；A1「不遞迴子資料夾」、A2 Non-Goal 改寫、B3 same-filesystem rename 與失敗來源不變、B4 EXIF 標籤與不做時區換算、B5 off-UI-thread 皆已落實。無任何 confidence ≥ 80 的 Critical 存活，故 `critical_gap` 為 false。唯一殘留為 B7 路徑正規化細節未完整，屬 Suggestion 級，故分數保留於 9 未達 10。因 pass 條件要求 `quality_score > 9`，本輪判定 `next_round`。

## Fix Actions

- specs/photo-organization/spec.md：EXIF 需求改寫，指定 `DateTimeOriginal`/`DateTimeDigitized` 與不做時區換算、併入零哨兵 / 損壞值為「無日期」、逐檔攔截例外，新增「date source resolution」表格（B4/B8/B6）。Execute 需求改寫，衝突定義涵蓋執行前既有與批次內碰撞、same-filesystem rename、失敗時來源不變，新增「Two source files collide…」scenario 與合併 example（B1/B3）。
- specs/organizer-ui/spec.md：Execute 需求加入執行後刷新狀態（B9）；新增「Keep the UI responsive during long operations」需求與 scenario（B5）。
- proposal.md：改寫同名衝突 Non-Goal、新增 PNG/GIF 無 EXIF 屬預期行為之 Non-Goal（A2/B6）。
- design.md：掃描補「不遞迴子資料夾」（A1）；EXIF 行為補標籤 / 時區（B4）；execute 補 same-filesystem rename 與失敗來源不變、批次內衝突（B1/B3）；UI 行為補 off-UI-thread 與執行後刷新（B5/B9）；`MovePlan` 補目標路徑以平台 path API 建絕對路徑（B7 部分）；驗收標準同步更新。
- tasks.md：3.2 改為覆蓋「date source resolution」表格；4.2 改為覆蓋批次內 + 既有衝突與失敗來源不變；5.4 加入執行後刷新；新增 5.5（off-UI-thread）。

## Decision

next_round
