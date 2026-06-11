## MODIFIED Requirements

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
