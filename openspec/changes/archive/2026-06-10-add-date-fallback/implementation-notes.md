<!-- apply-plus implementation notes | change: add-date-fallback | initialized: 2026-06-10 14:55 | no entries below means no deviations or open questions were recorded -->

## 2026-06-10 14:55 — 工件與現行程式碼存在 drift(nested-date-folders 已先 archive)

- 類別：open-question
- 任務：n/a(進入 task loop 前發現,尚未實作任何任務)
- 內容：`add-date-fallback` 的 spec delta / design / tasks 是針對 `nested-date-folders` archive **之前**的舊模型撰寫,與目前程式碼有四處實質衝突:(1) design contract 要求 `MoveItem(source, target, date: CaptureDate)` 的 `date` 非空,但現行 `MovePlanner.plan` 也會為 `YearOnly`、`NoDate` 建立 `MoveItem`,二者無 `CaptureDate`;(2) spec delta「Compute move plan」寫扁平 `<sourceFolder>/<yyyy-MM-dd>/`,但現行程式碼與 master spec 是巢狀 `<sourceFolder>/<yyyy>/<yyyy-MM-dd>/`;(3) spec delta 把日期結果簡化為 `Found`/`NoDate` 兩態並無 `YearOnly`,但 design 要求 `resolve()` 既有案例不變(仍回傳 `YearOnly`),且 `App.kt`、`MovePlannerTest`、`PlanResultViewTest` 仍使用 `YearOnly`,`read()` 如何銜接 `YearOnly` 未定義;(4) spec delta 說無日期檔案只進 error list、不給 target,但現行程式碼給 `no-exif/` target 且 UI 文案為「…筆無 EXIF 日期搬往 no-exif/」。
- 原因：`nested-date-folders`(commit `ddb2d7a`,2026-06-10)在本 change 建立前剛 archive,引入年份巢狀結構、`YearOnly`、`no-exif` 三態。本 change 工件未納入這些,直接實作會破壞剛上線行為並造成大量返工。需使用者裁示:是先以 `/spectra-ingest add-date-fallback` 重新對齊工件,還是確認要以「移除巢狀/year-only/no-exif、改採扁平兩態」作為本 change 的刻意目標。

## 2026-06-10 14:55 — 上述 drift 已由 /spectra-ingest 解決

- 類別：open-question
- 任務：n/a
- 內容：已執行 `/spectra-ingest add-date-fallback`,將 proposal / design / specs(photo-organization、organizer-ui)/ tasks 重新對齊目前程式碼模型:保留巢狀年份/日期、`YearOnly`、`no-exif` 三態與結構;三層備援僅在 EXIF 回傳 `NoDate` 時觸發,EXIF `Found`/`YearOnly` 維持現行歸檔;`MoveItem` 改攜帶 `dateResult: DateResult`(非 `date: CaptureDate`)以涵蓋三態;扁平 `<yyyy-MM-dd>` 與「無日期不給 target」的舊模型描述已移除。`spectra analyze` 僅剩 1 個 Suggestion(`SHALL consider` 規範用語誤判),`spectra validate` 通過。
- 原因：使用者選擇「先 ingest 重新對齊」。四處衝突(MoveItem.date 非空 vs YearOnly/NoDate、扁平 vs 巢狀資料夾、YearOnly 三態、no-exif 去向)皆已在工件層解決,可安全進入 apply。
