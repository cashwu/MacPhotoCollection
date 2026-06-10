# photo-organization Specification

## Purpose

TBD - created by archiving change 'photo-organizer-mvp'. Update Purpose after archive.

## Requirements

### Requirement: Scan source folder for image files

The system SHALL scan a given source folder and return the list of image files whose extension is supported (jpg, jpeg, png, gif, and HEIC / RAW formats supported by the EXIF library). Extension matching SHALL be case-insensitive, so files with uppercase extensions such as `.JPG` (commonly emitted by cameras) are included. The scan SHALL ignore dot-prefixed files (names beginning with `.`, such as macOS AppleDouble `._IMG_0001.JPG` sidecars), so they do not appear as organizable photos. The scan SHALL NOT recurse into subfolders. When the source folder does not exist, the system SHALL report a distinguishable error rather than returning an empty list. When the folder exists but contains no supported image files, the system SHALL return an empty list.

#### Scenario: Folder contains mixed files

- **WHEN** the source folder contains image files and non-image files
- **THEN** the system returns only the supported image files

##### Example: filtering by extension

| Folder contents | Returned |
| --------------- | -------- |
| a.jpg, b.PNG, IMG.JPG, ._IMG.JPG, .DS_Store, notes.txt, sub/ (dir) | a.jpg, b.PNG, IMG.JPG |
| report.pdf, data.csv | (empty list) |
| (folder does not exist) | error: folder not found |

#### Scenario: Folder does not exist

- **WHEN** the given source folder path does not exist
- **THEN** the system reports a folder-not-found error distinct from the empty result

#### Scenario: Already-organized folder

- **WHEN** the source folder's root contains no image files because they were all moved into date subfolders on a prior run
- **THEN** the system returns an empty list (the non-recursive scan ignores the date subfolders), which leads to an empty plan and a successful no-op execution rather than an error


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
### Requirement: Extract capture date from EXIF

The system SHALL read the EXIF capture date from each image file and classify it into one of three outcomes: a full capture date, a year-only date, or no date. The system SHALL use the EXIF `DateTimeOriginal` tag (0x9003) as the primary source and, when it is absent, SHALL fall back to `DateTimeDigitized` (0x9004); the date SHALL be taken as the local civil date contained in the EXIF value with no timezone conversion. To guarantee no timezone conversion, the system SHALL read the raw EXIF string value of the tag and parse its date portion directly, and SHALL NOT use a date accessor that returns an instant/epoch value (which would apply timezone or offset handling and risk shifting the date across midnight).

The system SHALL classify the parsed `yyyy:MM:dd` date portion as follows. When the year is in the range 1..9999 and the month and day are both greater than zero and the three form a valid calendar date, the system SHALL return a full capture date formatted as `yyyy-MM-dd`; the `1..9999` year bound applies symmetrically to the full-date and year-only outcomes so neither path can emit a year folder wider than four digits. When the year is a valid year in the range 1..9999 but the value cannot form a full valid calendar date — including the case where month or day is zero (such as `2008:00:00`), and the case where month or day is non-zero but out of range (such as `2008:13:45`) — the system SHALL return a year-only result carrying that year, because the year is reliable even though the month and day are not. When the year is zero (including the zero sentinel `0000:00:00 00:00:00`) or otherwise outside the range 1..9999, or the date portion is absent, has fewer than three colon-separated segments, or contains a non-numeric segment, the system SHALL return a "no date" result. The system SHALL catch any exception raised while reading a single file and SHALL NOT let it abort processing of the remaining files; a file that raises an exception SHALL be treated as "no date".

#### Scenario: Image with valid EXIF date

- **WHEN** an image's EXIF `DateTimeOriginal` contains a capture date of 2024-03-15 at 23:50 local time
- **THEN** the system returns the full capture date formatted as "2024-03-15" with no timezone adjustment

#### Scenario: Image with year-only EXIF date

- **WHEN** an image's EXIF date value is `2008:00:00 00:00:00`, or another value whose year is valid (1..9999) but whose month or day is zero or out of range
- **THEN** the system returns a year-only result carrying the year 2008 (respectively, that value's year)

#### Scenario: Image without usable EXIF date

- **WHEN** an image has no `DateTimeOriginal`/`DateTimeDigitized`, or its year is zero (such as `0000:00:00 00:00:00`), or its date portion is absent, has fewer than three segments, or has a non-numeric segment
- **THEN** the system returns a "no date" result for that file and continues processing other files

##### Example: date source resolution

| EXIF state | Result |
| ---------- | ------ |
| DateTimeOriginal = `2024:03:15 23:50:00` | full date 2024-03-15 |
| DateTimeOriginal absent, DateTimeDigitized = `2022:01:02 08:30:00` | full date 2022-01-02 |
| value = `2008:00:00 00:00:00` | year-only 2008 |
| value = `2008:13:45 00:00:00` | year-only 2008 |
| value = `0000:00:00 00:00:00` | no date |
| value = `12024:03:15 00:00:00` (year outside 1..9999) | no date |
| no EXIF block (typical PNG/GIF) | no date |
| unparseable/corrupt date bytes | no date (no exception escapes) |


<!-- @trace
source: nested-date-folders
updated: 2026-06-10
code:
  - src/main/kotlin/org/photocollection/core/MovePlanner.kt
  - .agents/skills/spectra-propose-plus/SKILL.md
  - src/main/kotlin/org/photocollection/ui/App.kt
  - src/main/kotlin/org/photocollection/core/Models.kt
  - .agents/skills/spectra-apply-plus/SKILL.md
  - src/main/kotlin/org/photocollection/core/MoveExecutor.kt
  - .agents/skills/spectra-commit/SKILL.md
  - src/main/kotlin/org/photocollection/core/ExifReader.kt
tests:
  - src/test/kotlin/org/photocollection/core/MovePlannerTest.kt
  - src/test/kotlin/org/photocollection/core/ModelsTest.kt
  - src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt
  - src/test/kotlin/org/photocollection/ui/OrganizerScanMappingTest.kt
  - src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt
  - src/test/kotlin/org/photocollection/core/ExifReaderTest.kt
-->

---
### Requirement: Compute move plan grouped by capture date

The system SHALL compute a move plan from a list of EXIF-classified image files without modifying the file system. Every target path SHALL be constructed with the platform path API as an absolute path by resolving the source folder against each path segment in turn (i.e. `Path.resolve` per segment), not by raw string concatenation, so that planning and execution agree on the exact path. Year folder segments SHALL be formatted as a four-digit zero-padded year (for example year 7 SHALL render as `0007`).

For each file the system SHALL assign a target as follows. A file with a full capture date SHALL target `<sourceFolder>/<yyyy>/<yyyy-MM-dd>/<fileName>`, a nested year folder containing the date folder. A file with a year-only result SHALL target `<sourceFolder>/<yyyy>/<yyyy-00-00>/<fileName>`, the same year folder containing a folder whose month and day are zero. A file with no date SHALL target `<sourceFolder>/no-exif/<fileName>`, a single shared folder for files with no usable EXIF date. Every scanned file therefore receives a move target. In addition, each no-date file SHALL also be recorded in the plan's error list so the user interface can display the count of files that have no usable EXIF date; a no-date file therefore appears both as a move item targeting `no-exif/` and in the error list, and the error list SHALL NOT be interpreted as "not moved". Computing the plan SHALL NOT create folders or move any file.

#### Scenario: Full, year-only, and undated files produce nested plan

- **WHEN** the move plan is computed for files with a full date, a year-only date, and no date
- **THEN** the full-date file targets its `<yyyy>/<yyyy-MM-dd>` folder, the year-only file targets its `<yyyy>/<yyyy-00-00>` folder, the undated file targets the shared `no-exif` folder and also appears in the error list, and no file on disk is changed

##### Example: plan grouping

- **GIVEN** source folder `/photos` with files: IMG1(full date 2024-03-15), IMG2(full date 2024-03-15), IMG3(year-only 2008), IMG4(no date)
- **WHEN** the move plan is computed
- **THEN** the plan contains move items `IMG1 → /photos/2024/2024-03-15`, `IMG2 → /photos/2024/2024-03-15`, `IMG3 → /photos/2008/2008-00-00`, and `IMG4 → /photos/no-exif`, and the error list contains `IMG4`


<!-- @trace
source: nested-date-folders
updated: 2026-06-10
code:
  - src/main/kotlin/org/photocollection/core/MovePlanner.kt
  - .agents/skills/spectra-propose-plus/SKILL.md
  - src/main/kotlin/org/photocollection/ui/App.kt
  - src/main/kotlin/org/photocollection/core/Models.kt
  - .agents/skills/spectra-apply-plus/SKILL.md
  - src/main/kotlin/org/photocollection/core/MoveExecutor.kt
  - .agents/skills/spectra-commit/SKILL.md
  - src/main/kotlin/org/photocollection/core/ExifReader.kt
tests:
  - src/test/kotlin/org/photocollection/core/MovePlannerTest.kt
  - src/test/kotlin/org/photocollection/core/ModelsTest.kt
  - src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt
  - src/test/kotlin/org/photocollection/ui/OrganizerScanMappingTest.kt
  - src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt
  - src/test/kotlin/org/photocollection/core/ExifReaderTest.kt
-->

---
### Requirement: Execute move plan

The system SHALL execute a move plan by creating the required nested subfolders under the source folder — a year folder containing a date folder for dated files, a year folder containing a `yyyy-00-00` folder for year-only files, or the shared `no-exif` folder for undated files — and moving each file into its target folder. The nested subfolders SHALL be created with an idempotent create-directories operation that creates every missing parent in the path, so repeated items for the same date do not fail and a missing year folder is created on demand; an existing target folder SHALL NOT be treated as an error; if a target subfolder cannot be created for any reason (for example its path is occupied by a non-directory, or the source folder no longer exists), the affected items SHALL be marked as failed rather than aborting the run. Each move SHALL be performed with a move operation invoked with no replace-existing option, so that it fails when the target path already exists; the implementation SHALL detect a conflict by catching that already-exists failure rather than doing a separate existence check followed by a move, so there is no time-of-check-to-time-of-use gap. (The conflict guarantee comes from omitting the replace-existing option, not from requesting an atomic-move option, whose fail-on-existing behavior is platform-dependent.) This single mechanism covers both conflict cases — a file that existed at the target before the run, and a target a same-run earlier item already moved a file into — and on a case-insensitive filesystem it also catches names that differ only in case. In every conflict the system SHALL skip the item, mark it as failed, and continue with the remaining items; it SHALL NOT overwrite the existing file. The move is expected to stay on one filesystem because every target is a subfolder of the source, but the implementation SHALL still treat any move that throws as a per-item failure. When any item fails for any reason, the system SHALL leave the source file unchanged (no partial copy or truncation). The system SHALL return a per-item result indicating success or failure.

#### Scenario: Successful execution

- **WHEN** a move plan with dated, year-only, and undated files is executed and no name conflicts exist
- **THEN** the system creates the nested year/date subfolders, the year/`yyyy-00-00` subfolders, and the `no-exif` folder as needed, moves each file into its target folder, and reports each item as successful

#### Scenario: Conflict with a pre-existing file at target

- **WHEN** a file's target folder already contains a file with the same name before the run
- **THEN** the system skips that file without overwriting, marks it as failed, leaves the source file unchanged, and continues moving the remaining files

#### Scenario: Two source files collide on the same target path within one run

- **WHEN** two items in the plan resolve to the same target path (same folder and same file name)
- **THEN** the first item is moved successfully and the second is reported as failed (skipped) without overwriting, and its source file is left unchanged

##### Example: execution with pre-existing and intra-run conflicts

- **GIVEN** plan items `A.jpg → /photos/2024/2024-03-15`, `B.jpg → /photos/2024/2024-03-15`, `C.jpg → /photos/2024/2024-03-15` where `/photos/2024/2024-03-15/A.jpg` already exists and `B.jpg` and `C.jpg` share the same file name
- **WHEN** the plan is executed
- **THEN** `A.jpg` is reported as failed (pre-existing target), the first of the same-named pair is moved successfully, the second is reported as failed (intra-run conflict), and every failed item's source file is left unchanged

<!-- @trace
source: nested-date-folders
updated: 2026-06-10
code:
  - src/main/kotlin/org/photocollection/core/MovePlanner.kt
  - .agents/skills/spectra-propose-plus/SKILL.md
  - src/main/kotlin/org/photocollection/ui/App.kt
  - src/main/kotlin/org/photocollection/core/Models.kt
  - .agents/skills/spectra-apply-plus/SKILL.md
  - src/main/kotlin/org/photocollection/core/MoveExecutor.kt
  - .agents/skills/spectra-commit/SKILL.md
  - src/main/kotlin/org/photocollection/core/ExifReader.kt
tests:
  - src/test/kotlin/org/photocollection/core/MovePlannerTest.kt
  - src/test/kotlin/org/photocollection/core/ModelsTest.kt
  - src/test/kotlin/org/photocollection/ui/PlanResultViewTest.kt
  - src/test/kotlin/org/photocollection/ui/OrganizerScanMappingTest.kt
  - src/test/kotlin/org/photocollection/core/MoveExecutorTest.kt
  - src/test/kotlin/org/photocollection/core/ExifReaderTest.kt
-->