# organizer-ui Specification

## Purpose

TBD - created by archiving change 'photo-organizer-mvp'. Update Purpose after archive.

## Requirements

### Requirement: Select source folder

The desktop application SHALL allow the user to select a source folder to organize. After a folder is selected, the application SHALL trigger a scan of that folder.

#### Scenario: User selects a folder

- **WHEN** the user picks a source folder
- **THEN** the application scans the folder and updates the displayed file lists


<!-- @trace
source: photo-organizer-mvp
updated: 2026-06-08
code:
  - gradle.properties
  - gradle/wrapper/gradle-wrapper.properties
  - gradlew
  - src/main/kotlin/org/photocollection/core/ExifReader.kt
  - src/main/kotlin/org/photocollection/core/Models.kt
  - src/main/kotlin/org/photocollection/core/MovePlanner.kt
  - gradlew.bat
  - src/main/resources/icons/app-icon.icns
  - build.gradle.kts
  - src/main/kotlin/org/photocollection/ui/App.kt
  - src/main/kotlin/org/photocollection/core/PhotoScanner.kt
  - gradle/wrapper/gradle-wrapper.jar
  - settings.gradle.kts
  - src/main/kotlin/org/photocollection/Main.kt
  - src/main/kotlin/org/photocollection/core/MoveExecutor.kt
  - src/main/resources/icons/app-icon.png
tests:
  - src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt
  - src/test/kotlin/org/photocollection/core/ExifReaderTest.kt
  - src/test/kotlin/org/photocollection/core/PhotoScannerTest.kt
  - src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt
  - src/test/kotlin/org/photocollection/core/MovePlannerTest.kt
  - src/test/kotlin/org/photocollection/core/ModelsTest.kt
-->

---
### Requirement: Display file list and error list

The application SHALL display a list of organizable image files and a list of files whose capture date could not be resolved by any tier of the date fallback chain (in practice, files whose file attributes could not be read due to an I/O error, or whose creation and last-modified dates are both earlier than 1990-01-01). An organizable image file is one the system will move into a date-based folder: a file with a full capture date (resolved from EXIF, the file name, or the file system) or a file with a year-only date. Each list SHALL show its item count.

For each organizable file the application SHALL show the target folder path relative to the selected source folder. A full capture date SHALL show a `yyyy/MM/yyyy-MM-dd` target folder path, and a year-only date SHALL show a `yyyy/00/yyyy-00-00` target folder path. A file whose date could not be resolved SHALL appear in the no-date list; such a file is still moved into the shared `no-exif` folder, but it is surfaced in the no-date list so the user can see the count of files whose date could not be determined. A year-only file SHALL NOT be omitted from both lists: because it is moved, it SHALL appear in the organizable list rather than silently disappearing from the display while still being moved.

#### Scenario: Lists populated after scan with full, year-only, and undated files

- **WHEN** a folder is scanned containing 5 images with a full capture date (from any tier), 1 image with a year-only date, and 2 images whose file attributes cannot be read
- **THEN** the organizable file list shows 6 items with count 6 — the 5 full-date files showing their `yyyy/MM/yyyy-MM-dd` target folder path and the year-only file showing its `yyyy/00/yyyy-00-00` target folder path — and the no-date list shows 2 items with count 2

##### Example: target folder display

| Date result | Displayed target folder |
| ----------- | ----------------------- |
| full date 2024-03-15 | `2024/03/2024-03-15` |
| year-only 2008 | `2008/00/2008-00-00` |
| no date | no-date list item, not an organizable target folder |

#### Scenario: Year-only file is visible, not silently moved

- **WHEN** a scanned image has a year-only EXIF date and therefore targets a `yyyy/00/yyyy-00-00` folder path
- **THEN** the application shows that file in the organizable file list before any move, so the user sees it rather than having it moved without ever appearing in a displayed list


<!-- @trace
source: add-month-folder-to-photo-layout
updated: 2026-06-11
code:
  - .agents/skills/spectra-commit/SKILL.md
  - src/main/kotlin/org/photocollection/core/MoveExecutor.kt
  - .agents/skills/spectra-apply-plus/SKILL.md
  - src/main/kotlin/org/photocollection/ui/App.kt
  - src/main/kotlin/org/photocollection/core/Models.kt
  - .agents/skills/spectra-propose-plus/SKILL.md
  - src/main/kotlin/org/photocollection/core/MovePlanner.kt
  - README.md
tests:
  - src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt
  - src/test/kotlin/org/photocollection/core/PhotoScannerTest.kt
  - src/test/kotlin/org/photocollection/core/ModelsTest.kt
  - src/test/kotlin/org/photocollection/core/MovePlannerTest.kt
  - src/test/kotlin/org/photocollection/ui/OrganizerScanMappingTest.kt
  - src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt
-->

---
### Requirement: Preview selected photo

The application SHALL display a preview of the image that the user selects from the file list. Because the desktop image decoder cannot render every supported format (notably HEIC and most RAW, which are still organizable), when a selected file cannot be decoded for preview the application SHALL show a "preview unavailable" placeholder instead of crashing or showing a blank area.

#### Scenario: User selects a decodable item

- **WHEN** the user selects an item the desktop image decoder can render (such as jpg, png, or gif)
- **THEN** the application displays that image in the preview area

#### Scenario: User selects an item that cannot be decoded for preview

- **WHEN** the user selects an organizable item whose format the desktop image decoder cannot render (such as HEIC or RAW)
- **THEN** the application shows a "preview unavailable" placeholder and remains usable


<!-- @trace
source: photo-organizer-mvp
updated: 2026-06-08
code:
  - gradle.properties
  - gradle/wrapper/gradle-wrapper.properties
  - gradlew
  - src/main/kotlin/org/photocollection/core/ExifReader.kt
  - src/main/kotlin/org/photocollection/core/Models.kt
  - src/main/kotlin/org/photocollection/core/MovePlanner.kt
  - gradlew.bat
  - src/main/resources/icons/app-icon.icns
  - build.gradle.kts
  - src/main/kotlin/org/photocollection/ui/App.kt
  - src/main/kotlin/org/photocollection/core/PhotoScanner.kt
  - gradle/wrapper/gradle-wrapper.jar
  - settings.gradle.kts
  - src/main/kotlin/org/photocollection/Main.kt
  - src/main/kotlin/org/photocollection/core/MoveExecutor.kt
  - src/main/resources/icons/app-icon.png
tests:
  - src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt
  - src/test/kotlin/org/photocollection/core/ExifReaderTest.kt
  - src/test/kotlin/org/photocollection/core/PhotoScannerTest.kt
  - src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt
  - src/test/kotlin/org/photocollection/core/MovePlannerTest.kt
  - src/test/kotlin/org/photocollection/core/ModelsTest.kt
-->

---
### Requirement: Preview move plan before execution

The application SHALL display the computed move plan, showing which files will be moved into which target folder path, before any file is moved. For dated files and year-only files, the preview SHALL include the year, month, and date-folder segments relative to the selected source folder so the user can verify the month grouping before execution. For no-date files, the preview SHALL show the shared `no-exif` folder. The application SHALL NOT move any file until the user confirms.

#### Scenario: Plan shown without moving files

- **WHEN** the user requests a move plan preview
- **THEN** the application shows each file target folder path and no file on disk is changed

##### Example: preview target folder display

| Planned target parent | Displayed target folder |
| --------------------- | ----------------------- |
| `/photos/2024/03/2024-03-15` | `2024/03/2024-03-15` |
| `/photos/2008/00/2008-00-00` | `2008/00/2008-00-00` |
| `/photos/no-exif` | `no-exif` |


<!-- @trace
source: add-month-folder-to-photo-layout
updated: 2026-06-11
code:
  - .agents/skills/spectra-commit/SKILL.md
  - src/main/kotlin/org/photocollection/core/MoveExecutor.kt
  - .agents/skills/spectra-apply-plus/SKILL.md
  - src/main/kotlin/org/photocollection/ui/App.kt
  - src/main/kotlin/org/photocollection/core/Models.kt
  - .agents/skills/spectra-propose-plus/SKILL.md
  - src/main/kotlin/org/photocollection/core/MovePlanner.kt
  - README.md
tests:
  - src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt
  - src/test/kotlin/org/photocollection/core/PhotoScannerTest.kt
  - src/test/kotlin/org/photocollection/core/ModelsTest.kt
  - src/test/kotlin/org/photocollection/core/MovePlannerTest.kt
  - src/test/kotlin/org/photocollection/ui/OrganizerScanMappingTest.kt
  - src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt
-->

---
### Requirement: Execute on user confirmation

The application SHALL execute the move plan only after the user confirms, and SHALL report the outcome as counts of successful and failed items. The application SHALL additionally surface each failed item individually, identifying the file and its failure reason (for example, a name conflict at the target), so the user can locate and manually resolve stuck files. After execution completes, the application SHALL refresh its displayed state (re-scan or clear the moved items) so the previously shown plan cannot be executed a second time against already-moved files; the failure report (file plus reason) SHALL remain visible after this refresh so the user retains the context needed to resolve stuck files.

#### Scenario: User confirms execution

- **WHEN** the user confirms the previewed move plan
- **THEN** the application executes the plan, displays the number of successful and failed moves, and refreshes the file list so the moved items no longer appear

#### Scenario: Some items fail

- **WHEN** execution finishes with one or more failed items
- **THEN** the application lists each failed file together with its failure reason, so the user can find and resolve it manually


<!-- @trace
source: photo-organizer-mvp
updated: 2026-06-08
code:
  - gradle.properties
  - gradle/wrapper/gradle-wrapper.properties
  - gradlew
  - src/main/kotlin/org/photocollection/core/ExifReader.kt
  - src/main/kotlin/org/photocollection/core/Models.kt
  - src/main/kotlin/org/photocollection/core/MovePlanner.kt
  - gradlew.bat
  - src/main/resources/icons/app-icon.icns
  - build.gradle.kts
  - src/main/kotlin/org/photocollection/ui/App.kt
  - src/main/kotlin/org/photocollection/core/PhotoScanner.kt
  - gradle/wrapper/gradle-wrapper.jar
  - settings.gradle.kts
  - src/main/kotlin/org/photocollection/Main.kt
  - src/main/kotlin/org/photocollection/core/MoveExecutor.kt
  - src/main/resources/icons/app-icon.png
tests:
  - src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt
  - src/test/kotlin/org/photocollection/core/ExifReaderTest.kt
  - src/test/kotlin/org/photocollection/core/PhotoScannerTest.kt
  - src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt
  - src/test/kotlin/org/photocollection/core/MovePlannerTest.kt
  - src/test/kotlin/org/photocollection/core/ModelsTest.kt
-->

---
### Requirement: Keep the UI responsive during long operations

The application SHALL run the scanning, EXIF batch reading, and move execution off the UI thread so that the interface remains responsive during these operations on large folders. The application SHALL surface progress or a busy indicator while a long operation is in progress.

#### Scenario: Scanning a large folder

- **WHEN** the user selects a folder containing thousands of images
- **THEN** the application performs the scan and EXIF reading without freezing the interface and shows that work is in progress

<!-- @trace
source: photo-organizer-mvp
updated: 2026-06-08
code:
  - gradle.properties
  - gradle/wrapper/gradle-wrapper.properties
  - gradlew
  - src/main/kotlin/org/photocollection/core/ExifReader.kt
  - src/main/kotlin/org/photocollection/core/Models.kt
  - src/main/kotlin/org/photocollection/core/MovePlanner.kt
  - gradlew.bat
  - src/main/resources/icons/app-icon.icns
  - build.gradle.kts
  - src/main/kotlin/org/photocollection/ui/App.kt
  - src/main/kotlin/org/photocollection/core/PhotoScanner.kt
  - gradle/wrapper/gradle-wrapper.jar
  - settings.gradle.kts
  - src/main/kotlin/org/photocollection/Main.kt
  - src/main/kotlin/org/photocollection/core/MoveExecutor.kt
  - src/main/resources/icons/app-icon.png
tests:
  - src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt
  - src/test/kotlin/org/photocollection/core/ExifReaderTest.kt
  - src/test/kotlin/org/photocollection/core/PhotoScannerTest.kt
  - src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt
  - src/test/kotlin/org/photocollection/core/MovePlannerTest.kt
  - src/test/kotlin/org/photocollection/core/ModelsTest.kt
-->

---
### Requirement: Indicate capture date source in plan preview

The move plan preview SHALL indicate, for each file, which tier of the date fallback chain produced its capture date (`EXIF`, `FILENAME`, or `FILE_SYSTEM`), so the user can identify fallback-dated files and verify their target folders before confirming execution. A year-only file SHALL be shown with source `EXIF`, since a year-only result is only produced by the EXIF tier. The plan preview summary SHALL additionally show the number of files per date source, so the user can gauge the scale of fallback-dated files without scrolling the full list. All three sources SHALL be shown even when a source has zero files, so the user can confirm the absence of fallback-dated files rather than having to infer it from a missing entry.

#### Scenario: Plan preview distinguishes date sources

- **WHEN** the user previews a move plan containing one EXIF-dated photo and one screenshot dated from its file name
- **THEN** the preview shows the photo marked with source `EXIF` and the screenshot marked with source `FILENAME`

#### Scenario: Plan preview summary shows per-source counts

- **WHEN** the user previews a move plan containing 3 EXIF-dated files, 2 filename-dated files, and 0 file-system-dated files
- **THEN** the plan preview summary shows the counts per source: `EXIF` 3, `FILENAME` 2, `FILE_SYSTEM` 0 (the zero-count source is still shown)

<!-- @trace
source: add-date-fallback
updated: 2026-06-10
code:
  - src/main/kotlin/org/photocollection/core/ExifReader.kt
  - src/main/kotlin/org/photocollection/core/FilenameDateParser.kt
  - src/main/kotlin/org/photocollection/ui/App.kt
  - src/main/kotlin/org/photocollection/core/MoveExecutor.kt
  - src/main/kotlin/org/photocollection/core/MovePlanner.kt
  - src/main/kotlin/org/photocollection/core/FileSystemDateReader.kt
  - src/main/kotlin/org/photocollection/core/Models.kt
tests:
  - src/test/kotlin/org/photocollection/core/MovePlannerTest.kt
  - src/test/kotlin/org/photocollection/core/FilenameDateParserTest.kt
  - src/test/kotlin/org/photocollection/core/FileSystemDateReaderTest.kt
  - src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt
  - src/test/kotlin/org/photocollection/ui/OrganizerScanMappingTest.kt
  - src/test/kotlin/org/photocollection/core/ModelsTest.kt
  - src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt
  - src/test/kotlin/org/photocollection/core/ExifReaderTest.kt
-->