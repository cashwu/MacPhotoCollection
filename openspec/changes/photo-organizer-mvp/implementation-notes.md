<!-- apply-plus implementation notes | change: photo-organizer-mvp | initialized: 2026-06-08 21:12 | no entries below means no deviations or open questions were recorded -->

## 2026-06-08 21:30 — UI 互動式手動驗證待使用者執行
- 類別：open-question
- 任務：8, 9, 10, 11, 12
- 內容：tasks 5.1–5.5（folder 選擇＋掃描、檔案/錯誤清單與計數、照片預覽與「預覽不可用」佔位、計畫預覽、確認後執行與失敗逐項回報、`Dispatchers.IO` 背景化與進行中指示）的驗證目標皆為「手動執行 app」的 GUI 互動。實作已對照 `design.md` Implementation Contract 與 `organizer-ui` spec 各 SHALL 完成，`./gradlew compileKotlin` 通過、`./gradlew run` 能啟動且不崩潰。但選資料夾、點選預覽、觀察 count 5/2、刻意製造同名衝突等互動步驟無法於 headless 環境自動驅動。
- 原因：代理人無法驅動桌面 GUI 互動，僅能驗證編譯與啟動；逐情境的視覺驗收需使用者實機操作確認。建議使用者執行 `./gradlew run`，依各 5.x 任務「驗證」描述逐項手動確認。
