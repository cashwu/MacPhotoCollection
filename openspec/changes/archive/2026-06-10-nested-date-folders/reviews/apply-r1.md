# Apply Plus Review — Round 1

## Reviewer Findings

兩位 reviewer 平行獨立執行。Reviewer A（Adherence）無 finding；Reviewer B（Quality）提出兩筆 Warning，經主代理套用 confidence filter 與「常見誤報」規則後，兩筆皆未通過 Critical/Warning 門檻，僅以 Suggestion 列出供參考。

### Critical

（無）

### Warning

（無存活；兩筆原始 Warning 皆被過濾，理由見下方 Suggestion）

### Suggestion

- severity: Suggestion（原 Reviewer B 評為 Warning，重評後過濾）
  - confidence: 25（重評）
  - location: `src/main/kotlin/org/photocollection/core/ExifReader.kt` line 56（`parseExifDate`）
  - summary: `substringBefore(' ')` 不處理 tab/`T` 分隔的 EXIF 日期時間字串，`2024:03:15T...` 會落入 `NoDate` 而非 `YearOnly`。
  - recommendation: 不需修正。spec 明文「contains a non-numeric segment → NoDate」，`15T10` 為非數字段，現行行為符合規格；EXIF `DateTimeOriginal` 標準以空格分隔日期與時間，`T` 為 ISO-8601 而非 EXIF 慣例。判定為對 spec 的誤報。
  - 提出者: B

- severity: Suggestion（原 Reviewer B 評為 Warning，過濾為非阻擋並轉記 open-question）
  - confidence: 50（重評；真實但 out-of-scope）
  - location: `src/main/kotlin/org/photocollection/ui/App.kt` lines 345–349（`OrganizerModel.execute()` 的 rescan `catch`）
  - summary: 搬移後重掃若拋例外，`catch` 只更新 `statusMessage`，未像 `scan()` 錯誤路徑那樣清空 `entries`/`datedPhotos`/`undatedPhotos`，理論上使用者再按「預覽搬移計畫」會對已搬走的舊清單二次計畫，執行時全部以 `FileAlreadyExistsException` 失敗。
  - recommendation: 真實 latent issue，但該 `catch` block 不在本次 `nested-date-folders` diff 修改範圍，本次變更未使其惡化，屬 pre-existing。已轉記為 `implementation-notes.md` 的 open-question，需取得使用者確認是否於本次 change 內修正，或另開 change 處理。
  - 提出者: B

## Rating

- quality_score: 9
- critical_gap: false
- 理由：Reviewer A 對 Adherence 無 finding，逐項對照 design.md Implementation Contract、spec 的 SHALL、tasks.md 均相符，唯一 deviation（6.2 以自動化測試替代 headless GUI 冒煙）有合理記錄。Reviewer B 兩筆 Warning 經過濾後皆不阻擋：第一筆為對 spec 的誤報（spec 已規定非數字段 → NoDate），第二筆為真實但 pre-existing/out-of-scope 的狀態洩漏風險，已以 open-question 承載。過濾後無 severity==Critical 且 confidence≥80 的 finding 存活，`critical_gap` 為 false。Rater 保留 1 分，因第二筆雖正當排除於範圍外，仍是與本次搬移語意相鄰、值得追蹤的真實品質負債。本輪 quality_score 9 未達 `> 9` 門檻，且存在一筆需使用者確認的 open-question，故本輪不通過。

## Fix Actions

- 取得使用者對 App.kt `execute()` rescan `catch` 狀態洩漏（Finding 2）的決定：於本次 change 內以鏡像 `scan()` 錯誤路徑的方式修正（在 `catch` 內清空 `entries`/`datedPhotos`/`undatedPhotos`），或維持 open-question 另開 change 處理。使用者決定後，於 `implementation-notes.md` 追加 resolution 條目，再進入 Round 2 重新審查與評分。

## Decision

next_round
