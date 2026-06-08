# Apply Plus Review — Round 2

## Reviewer Findings

### Critical

（無）

### Warning

（無）

### Suggestion

- severity: Suggestion｜confidence: 50｜reviewer: B
  - location: `src/main/kotlin/org/photocollection/ui/App.kt`（`loadPreview`）
  - summary: `loadPreview` 以 `Files.readAllBytes` 讀入整個檔案且僅 `catch (e: Exception)`。選取極大的 RAW/HEIC 進行預覽時可能拋 `OutOfMemoryError`（屬 `Error` 而非 `Exception`），逃逸 catch 而崩潰，而非降級為「預覽不可用」佔位。Reviewer B 自評為「對一般 JPEG 屬可接受的 MVP 取捨」。
  - recommendation: 屬狹窄邊界；若需強化，可改 `catch (Throwable)` 或限制讀取大小。惟 spec 未指名此情境，且 `catch (Throwable)` 可能引入「攔截 Error」的反模式，故依 Simplicity-First 視為可接受取捨、不於本次更動。

- severity: Suggestion｜confidence: 50｜reviewer: B
  - location: `src/main/kotlin/org/photocollection/core/MoveExecutor.kt:24-26`
  - summary: 當日期子資料夾路徑被非資料夾佔用時，`Files.createDirectories` 拋 `FileAlreadyExistsException`，被與 move 衝突共用的分支以「目標已存在，跳過以避免覆蓋：<target>」回報，對此罕見情境的 reason 文字略不精確。行為完全正確（標記失敗、整批繼續、來源不變，符合 spec）。
  - recommendation: 可選擇性區分 createDirectories 失敗與 move 衝突以提供更精確的 reason；非正確性缺陷。依 Surgical 紀律不為罕見情境的文字微調擴增分支。

（已丟棄：Reviewer B 第三筆 produceState 預覽殘影，confidence 25 < 50，依 filter 規則不納入。）

## Rating

- quality_score: 9.2
- critical_gap: false
- rationale: 變更 spec-complete 且正確：13 項任務全完成、17 個單元測試零失敗、核心邏輯對照 spec 範例表格驗證；round 1 唯一實質缺陷（未攔截 IO 導致 UI 卡 busy）已以 try/catch/finally 妥善修正。Adherence review 零違規，confidence filter 後無 Critical/Warning 留存。剩餘兩項皆行為正確且狹窄：大檔預覽的 `OutOfMemoryError` 為已知 MVP 取捨（且攔截 `Error` 反而是 spec 未要求的防禦性複雜度），MoveExecutor 一項僅為罕見非資料夾碰撞情境下略不精確的 reason 字串、實際行為（失敗該項、繼續整批、來源不變）完全符合 spec。皆不影響正確性，僅屬可選的外觀打磨，故跨越高標準門檻。

## Fix Actions

None; pass condition met.

## Decision

passed
