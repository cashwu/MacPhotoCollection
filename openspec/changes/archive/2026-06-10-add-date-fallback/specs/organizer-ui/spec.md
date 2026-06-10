## MODIFIED Requirements

### Requirement: Display file list and error list

The application SHALL display a list of organizable image files and a list of files whose capture date could not be resolved by any tier of the date fallback chain (in practice, files whose file attributes could not be read due to an I/O error, or whose creation and last-modified dates are both earlier than 1990-01-01). An organizable image file is one the system will move into a date-based folder: a file with a full capture date (resolved from EXIF, the file name, or the file system) or a file with a year-only date. Each list SHALL show its item count.

For each organizable file the application SHALL show the name of the target folder the file will be moved into: a `yyyy-MM-dd` folder for a full capture date, and a `yyyy-00-00` folder for a year-only date. A file whose date could not be resolved SHALL appear in the no-date list; such a file is still moved into the shared `no-exif` folder, but it is surfaced in the no-date list so the user can see the count of files whose date could not be determined. A year-only file SHALL NOT be omitted from both lists: because it is moved, it SHALL appear in the organizable list rather than silently disappearing from the display while still being moved.

#### Scenario: Lists populated after scan with full, year-only, and undated files

- **WHEN** a folder is scanned containing 5 images with a full capture date (from any tier), 1 image with a year-only date, and 2 images whose file attributes cannot be read
- **THEN** the organizable file list shows 6 items with count 6 — the 5 full-date files showing their `yyyy-MM-dd` target folder and the year-only file showing its `yyyy-00-00` target folder — and the no-date list shows 2 items with count 2

#### Scenario: Year-only file is visible, not silently moved

- **WHEN** a scanned image has a year-only EXIF date and therefore targets a `yyyy-00-00` folder
- **THEN** the application shows that file in the organizable file list before any move, so the user sees it rather than having it moved without ever appearing in a displayed list

## ADDED Requirements

### Requirement: Indicate capture date source in plan preview

The move plan preview SHALL indicate, for each file, which tier of the date fallback chain produced its capture date (`EXIF`, `FILENAME`, or `FILE_SYSTEM`), so the user can identify fallback-dated files and verify their target folders before confirming execution. A year-only file SHALL be shown with source `EXIF`, since a year-only result is only produced by the EXIF tier. The plan preview summary SHALL additionally show the number of files per date source, so the user can gauge the scale of fallback-dated files without scrolling the full list. All three sources SHALL be shown even when a source has zero files, so the user can confirm the absence of fallback-dated files rather than having to infer it from a missing entry.

#### Scenario: Plan preview distinguishes date sources

- **WHEN** the user previews a move plan containing one EXIF-dated photo and one screenshot dated from its file name
- **THEN** the preview shows the photo marked with source `EXIF` and the screenshot marked with source `FILENAME`

#### Scenario: Plan preview summary shows per-source counts

- **WHEN** the user previews a move plan containing 3 EXIF-dated files, 2 filename-dated files, and 0 file-system-dated files
- **THEN** the plan preview summary shows the counts per source: `EXIF` 3, `FILENAME` 2, `FILE_SYSTEM` 0 (the zero-count source is still shown)
