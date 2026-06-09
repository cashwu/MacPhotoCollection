# Propose Plus Review — Round 1

## Reviewer Findings

### Warning

- **severity**: Warning | **confidence**: 80 | **reviewer**: A+B
  - **location**: design.md D5 / 範圍邊界；App.kt:195 `PlanResultView`
  - **summary**: 新增 `YearOnly` 與「no-date 同時進 moves 與 errors」後，`plan.moves` 不再只含完整日期檔（含 `yyyy-00-00`、`no-exif` 列），且 `moves.size` 與 `errors.size` 在 no-date 檔上重疊；既有標籤「N 筆將搬移，M 筆無日期」會重複計數，design D5 卻聲稱「不需改動」。
  - **recommendation**: 改寫 D5 註記其語意改變、把 `PlanResultView` 標籤改寫納入 In scope、新增 task 重寫標籤為不重疊語意並更新 `PlanResultViewTest`。

### Suggestion

- **severity**: Suggestion | **confidence**: 78 | **reviewer**: B
  - **location**: tasks.md 5.2
  - **summary**: 原 task 5.2 以「或新增 OrganizerModel 掃描映射測試」留下跳過更新既有 UI 測試的漏洞，且 `OrganizerModel` 為 private 無法直接測。
  - **recommendation**: 收緊 task 5.2，要求提升掃描映射可測性，禁止以「另開測試」當作不更新既有測試的藉口。

- **severity**: Suggestion | **confidence**: 65 | **reviewer**: B
  - **location**: specs/photo-organization/spec.md「Extract capture date from EXIF」；design D2
  - **summary**: year-only 分支限定年份 `1..9999`，但 full-date 分支無上界，導致 5 位數年份（如 `12024:03:15`）會被判為 Found 並產生 5 位數年份資料夾，兩分支不對稱。
  - **recommendation**: full-date 與 no-date 分支同步套用 `1..9999` 上界；補測試案例 `12024:03:15 → NoDate`。

- **severity**: Suggestion | **confidence**: 60 | **reviewer**: B
  - **location**: tasks.md 4.2；specs/photo-organization「Execute move plan」
  - **summary**: `no-exif/` 是本次新增的同名碰撞面（平鋪版本無日期檔根本不搬故不會碰撞），但測試未涵蓋 `no-exif/` 內同名碰撞。
  - **recommendation**: 在 MoveExecutorTest 新增 `no-exif/` 同名碰撞案例，驗證先到成功、後到失敗且來源不動。

- **severity**: Suggestion | **confidence**: 55 | **reviewer**: B
  - **location**: tasks.md 1.3
  - **summary**: year-only 四個子情況中，「月合法、日越界經 `LocalDate.of` 拋例外」路徑（如 `2008:02:30`）未被測試涵蓋。
  - **recommendation**: ExifReaderTest 新增 `2008:02:30 → YearOnly(2008)` 案例。

- **severity**: Suggestion | **confidence**: 50 | **reviewer**: A
  - **location**: design.md D7；tasks.md 5.x
  - **summary**: `selectedPhoto` 對 year-only 項目的預覽行為在 D7 有提及但無明確測試任務。
  - **recommendation**: 可選——擴充 UI 測試斷言 year-only 項目可被選取並預覽；非阻塞。

## Rating

- **quality_score**: 9.4
- **critical_gap**: false
- 理由：兩位 reviewer 獨立指出的最高信心發現（`PlanResultView` 標籤重複計數、design D5 失效聲明）已被完整修正——D5 註記改寫並附具體例子、範圍邊界將標籤改寫移入 In scope、新增 task 5.3 同時涵蓋重寫標籤與更新 `PlanResultViewTest`、out-of-scope 清乾淨。year-range 不對稱已在 spec/D2/tasks 三處於 full-date、year-only、no-date 三分支對稱套用 `1..9999`，並補上 example 表列與 `12024:03:15` 測試案例。task 5.2 漏洞已收緊並要求提升可測性；`no-exif/` 碰撞與 `2008:02:30` 子情況測試均已補入。Implementation Contract 驗收條件、D7 UI 映射與 organizer-ui delta 三者一致，未發現新增矛盾。唯一軟點是 task 5.2 在提升可見度前未指名測試檔，屬合理且非阻塞，未構成 critical gap。

## Fix Actions

本輪修正並記錄於以下檔案：

- `openspec/changes/nested-date-folders/proposal.md`：Modified Capabilities / Affected specs / Affected code 加入 `organizer-ui` 與 `App.kt`（擴大範圍修 UI 隱形問題）。
- `openspec/changes/nested-date-folders/design.md`：D5 改寫標籤語意註記；新增 D7（year-only 併入「檔案清單」）；D2 三分支對稱套用 `1..9999`；範圍邊界更新；Risks 補 UI 隱形與標籤重複計數條目；Implementation Contract 驗收條件補 UI 一行。
- `openspec/changes/nested-date-folders/tasks.md`：新增 §5（5.1 掃描映射、5.2 可測性、5.3 標籤重寫＋測試）；4.2 補 `no-exif/` 碰撞測試；1.2/1.3 補 `1..9999` 上界與 `2008:02:30`、`12024:03:15` 測試案例；驗證段改為 §6。
- `openspec/changes/nested-date-folders/specs/photo-organization/spec.md`：full-date 與 no-date 分支對稱套用 `1..9999`；example 表新增 `12024:03:15 → no date` 列。
- `openspec/changes/nested-date-folders/specs/organizer-ui/spec.md`：新建 delta，MODIFIED「Display file list and error list」為三態（year-only 併入可整理清單並顯示 `yyyy-00-00`，no-date 進無日期清單）。

修正後重跑 `spectra validate nested-date-folders` 通過。

## Decision

passed
