## MODIFIED Requirements

### Requirement: Display file list and error list

The application SHALL display a list of organizable image files and a list of files that have no readable EXIF capture date. An organizable image file is one the system will move into a date-based folder: a file with a full capture date or a file with a year-only date. Each list SHALL show its item count.

For each organizable file the application SHALL show the name of the target folder the file will be moved into: a `yyyy-MM-dd` folder for a full capture date, and a `yyyy-00-00` folder for a year-only date. A file with no readable EXIF capture date SHALL appear in the no-date list; such a file is still moved into the shared `no-exif` folder, but it is surfaced in the no-date list so the user can see the count of files without a usable EXIF date. A year-only file SHALL NOT be omitted from both lists: because it is moved, it SHALL appear in the organizable list rather than silently disappearing from the display while still being moved.

#### Scenario: Lists populated after scan with full, year-only, and undated files

- **WHEN** a folder is scanned containing 5 images with a full capture date, 1 image with a year-only date, and 2 images with no usable EXIF date
- **THEN** the organizable file list shows 6 items with count 6 — the 5 full-date files showing their `yyyy-MM-dd` target folder and the year-only file showing its `yyyy-00-00` target folder — and the no-date list shows 2 items with count 2

#### Scenario: Year-only file is visible, not silently moved

- **WHEN** a scanned image has a year-only EXIF date and therefore targets a `yyyy-00-00` folder
- **THEN** the application shows that file in the organizable file list before any move, so the user sees it rather than having it moved without ever appearing in a displayed list
