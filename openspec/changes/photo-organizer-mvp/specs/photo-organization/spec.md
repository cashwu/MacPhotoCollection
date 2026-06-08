## ADDED Requirements

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

### Requirement: Extract capture date from EXIF

The system SHALL read the EXIF capture date from each image file and expose it formatted as `yyyy-MM-dd`. The system SHALL use the EXIF `DateTimeOriginal` tag (0x9003) as the primary source and, when it is absent, SHALL fall back to `DateTimeDigitized` (0x9004); the date SHALL be taken as the local civil date contained in the EXIF value with no timezone conversion. To guarantee no timezone conversion, the system SHALL read the raw EXIF string value of the tag and parse its date portion directly, and SHALL NOT use a date accessor that returns an instant/epoch value (which would apply timezone or offset handling and risk shifting the date across midnight). When an image has no readable EXIF capture date — including the case where the value is absent, malformed, or the zero sentinel `0000:00:00 00:00:00` — the system SHALL return a distinguishable "no date" result for that file. The system SHALL catch any exception raised while reading a single file and SHALL NOT let it abort processing of the remaining files.

#### Scenario: Image with valid EXIF date

- **WHEN** an image's EXIF `DateTimeOriginal` contains a capture date of 2024-03-15 at 23:50 local time
- **THEN** the system returns the capture date formatted as "2024-03-15" with no timezone adjustment

#### Scenario: Image without usable EXIF date

- **WHEN** an image has no `DateTimeOriginal`/`DateTimeDigitized`, or its date value is malformed or the zero sentinel `0000:00:00 00:00:00`
- **THEN** the system returns a "no date" result for that file and continues processing other files

##### Example: date source resolution

| EXIF state | Result |
| ---------- | ------ |
| DateTimeOriginal = `2024:03:15 23:50:00` | 2024-03-15 |
| DateTimeOriginal absent, DateTimeDigitized = `2022:01:02 08:30:00` | 2022-01-02 |
| value = `0000:00:00 00:00:00` | no date |
| no EXIF block (typical PNG/GIF) | no date |
| unparseable/corrupt date bytes | no date (no exception escapes) |

### Requirement: Compute move plan grouped by capture date

The system SHALL compute a move plan from a list of image files without modifying the file system. Each file that has a capture date SHALL be assigned a target path under the source folder named after its capture date in `yyyy-MM-dd` form. The target path SHALL be constructed with the platform path API as an absolute path resolving the source folder against the `yyyy-MM-dd` date as a single path segment (i.e. `<sourceFolder>/<yyyy-MM-dd>/<fileName>`), not by raw string concatenation, so that planning and execution agree on the exact path. Each file that has no capture date SHALL be placed into an error list and SHALL NOT be assigned a target path. Computing the plan SHALL NOT create folders or move any file.

#### Scenario: Mixed files produce plan and error list

- **WHEN** the move plan is computed for files with and without capture dates
- **THEN** dated files appear as move items targeting their date folder and undated files appear in the error list, and no file on disk is changed

##### Example: plan grouping

- **GIVEN** source folder `/photos` with files: IMG1(2024-03-15), IMG2(2024-03-15), IMG3(no date)
- **WHEN** the move plan is computed
- **THEN** the plan contains move items `IMG1 → /photos/2024-03-15`, `IMG2 → /photos/2024-03-15`, and the error list contains `IMG3`

### Requirement: Execute move plan

The system SHALL execute a move plan by creating the required date subfolders under the source folder and moving each dated file into its target folder. The date subfolder SHALL be created with an idempotent create-directories operation so repeated items for the same date do not fail; an existing target folder SHALL NOT be treated as an error; if the date subfolder cannot be created for any reason (for example its path is occupied by a non-directory, or the source folder no longer exists), the affected items SHALL be marked as failed rather than aborting the run. Each move SHALL be performed with a move operation invoked with no replace-existing option, so that it fails when the target path already exists; the implementation SHALL detect a conflict by catching that already-exists failure rather than doing a separate existence check followed by a move, so there is no time-of-check-to-time-of-use gap. (The conflict guarantee comes from omitting the replace-existing option, not from requesting an atomic-move option, whose fail-on-existing behavior is platform-dependent.) This single mechanism covers both conflict cases — a file that existed at the target before the run, and a target a same-run earlier item already moved a file into — and on a case-insensitive filesystem it also catches names that differ only in case. In every conflict the system SHALL skip the item, mark it as failed, and continue with the remaining items; it SHALL NOT overwrite the existing file. The move is expected to stay on one filesystem because every target is a subfolder of the source, but the implementation SHALL still treat any move that throws as a per-item failure. When any item fails for any reason, the system SHALL leave the source file unchanged (no partial copy or truncation). The system SHALL return a per-item result indicating success or failure.

#### Scenario: Successful execution

- **WHEN** a move plan with dated files is executed and no name conflicts exist
- **THEN** the system creates the date subfolders, moves each file into its target folder, and reports each item as successful

#### Scenario: Conflict with a pre-existing file at target

- **WHEN** a file's target folder already contains a file with the same name before the run
- **THEN** the system skips that file without overwriting, marks it as failed, leaves the source file unchanged, and continues moving the remaining files

#### Scenario: Two source files collide on the same target path within one run

- **WHEN** two items in the plan resolve to the same target path (same date folder and same file name)
- **THEN** the first item is moved successfully and the second is reported as failed (skipped) without overwriting, and its source file is left unchanged

##### Example: execution with pre-existing and intra-run conflicts

- **GIVEN** plan items `A.jpg → /photos/2024-03-15`, `B.jpg → /photos/2024-03-15`, `C.jpg → /photos/2024-03-15` where `/photos/2024-03-15/A.jpg` already exists and `B.jpg` and `C.jpg` share the same file name
- **WHEN** the plan is executed
- **THEN** `A.jpg` is reported as failed (pre-existing target), the first of the same-named pair is moved successfully, the second is reported as failed (intra-run conflict), and every failed item's source file is left unchanged
