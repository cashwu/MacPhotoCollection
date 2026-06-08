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

The application SHALL display the list of organizable image files and the list of files that have no readable EXIF capture date. Each list SHALL show its item count.

#### Scenario: Lists populated after scan

- **WHEN** a folder is scanned containing 5 dated images and 2 undated images
- **THEN** the file list shows 5 items with count 5 and the error list shows 2 items with count 2


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

The application SHALL display the computed move plan, showing which files will be moved into which date folder, before any file is moved. The application SHALL NOT move any file until the user confirms.

#### Scenario: Plan shown without moving files

- **WHEN** the user requests a move plan preview
- **THEN** the application shows each file's target date folder and no file on disk is changed


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