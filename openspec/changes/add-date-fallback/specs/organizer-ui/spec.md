## MODIFIED Requirements

### Requirement: Display file list and error list

The application SHALL display the list of organizable image files and the list of files whose capture date could not be resolved by any tier of the date fallback chain (in practice, files whose file attributes could not be read due to an I/O error, or whose creation and last-modified dates are both earlier than 1990-01-01). Each list SHALL show its item count.

#### Scenario: Lists populated after scan

- **WHEN** a folder is scanned containing 5 images with resolvable dates and 2 images whose file attributes cannot be read
- **THEN** the file list shows 5 items with count 5 and the error list shows 2 items with count 2

## ADDED Requirements

### Requirement: Indicate capture date source in plan preview

The move plan preview SHALL indicate, for each file, which tier of the date fallback chain produced its capture date (`EXIF`, `FILENAME`, or `FILE_SYSTEM`), so the user can identify fallback-dated files and verify their target folders before confirming execution. The plan preview summary SHALL additionally show the number of files per date source, so the user can gauge the scale of fallback-dated files without scrolling the full list. All three sources SHALL be shown even when a source has zero files, so the user can confirm the absence of fallback-dated files rather than having to infer it from a missing entry.

#### Scenario: Plan preview distinguishes date sources

- **WHEN** the user previews a move plan containing one EXIF-dated photo and one screenshot dated from its file name
- **THEN** the preview shows the photo marked with source `EXIF` and the screenshot marked with source `FILENAME`

#### Scenario: Plan preview summary shows per-source counts

- **WHEN** the user previews a move plan containing 3 EXIF-dated files, 2 filename-dated files, and 0 file-system-dated files
- **THEN** the plan preview summary shows the counts per source: `EXIF` 3, `FILENAME` 2, `FILE_SYSTEM` 0 (the zero-count source is still shown)
