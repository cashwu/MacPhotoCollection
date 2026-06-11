## MODIFIED Requirements

### Requirement: Compute move plan grouped by capture date

The system SHALL compute a move plan from a list of date-classified image files without modifying the file system. Every target path SHALL be constructed with the platform path API as an absolute path by resolving the source folder against each path segment in turn (i.e. `Path.resolve` per segment), not by raw string concatenation, so that planning and execution agree on the exact path. Year folder segments SHALL be formatted as a four-digit zero-padded year (for example year 7 SHALL render as `0007`). Month folder segments SHALL be formatted as a two-digit zero-padded month (`01` through `12`) for files with a full capture date. A year-only result SHALL use `00` as the month folder segment because the month is unknown.

For each file the system SHALL assign a target as follows. A file with a full capture date SHALL target `<sourceFolder>/<yyyy>/<MM>/<yyyy-MM-dd>/<fileName>`, a nested year folder containing a month folder containing the date folder. A file with a year-only result SHALL target `<sourceFolder>/<yyyy>/00/<yyyy-00-00>/<fileName>`, the same three-level layout with `00` as the month folder and a date folder whose month and day are zero. A file with no date SHALL target `<sourceFolder>/no-exif/<fileName>`, a single shared folder for files whose date could not be resolved by any tier. Every scanned file therefore receives a move target. In addition, each no-date file SHALL also be recorded in the plan error list so the user interface can display its count; a no-date file therefore appears both as a move item targeting `no-exif/` and in the error list, and the error list SHALL NOT be interpreted as "not moved". Computing the plan SHALL NOT create folders or move any file.

Each move item SHALL additionally carry the resolved date result together with its date source, so the plan preview can display which tier of the fallback chain produced the date. The move item SHALL carry this date result for every file regardless of source (full date, year-only, or no date); the planner SHALL store it without branching on the source.

#### Scenario: Full, year-only, and undated files produce month-nested plan carrying date sources

- **WHEN** the move plan is computed for files with a full date, a year-only date, and no date
- **THEN** the full-date file targets its `<yyyy>/<MM>/<yyyy-MM-dd>` folder, the year-only file targets its `<yyyy>/00/<yyyy-00-00>` folder, the undated file targets the shared `no-exif` folder and also appears in the error list, each move item carries that resolved date result and source, and no file on disk is changed

##### Example: plan grouping

- **GIVEN** source folder `/photos` with files: IMG1(full date 2024-03-15, EXIF), IMG2(full date 2024-03-15, FILENAME), IMG3(year-only 2008, EXIF), IMG4(no date)
- **WHEN** the move plan is computed
- **THEN** the plan contains move items `IMG1 → /photos/2024/03/2024-03-15` carrying (2024-03-15, EXIF), `IMG2 → /photos/2024/03/2024-03-15` carrying (2024-03-15, FILENAME), `IMG3 → /photos/2008/00/2008-00-00` carrying (year-only 2008, EXIF), and `IMG4 → /photos/no-exif` carrying the no-date result, and the error list contains `IMG4`

### Requirement: Execute move plan

The system SHALL execute a move plan by creating the required nested subfolders under the source folder — a year folder containing a month folder containing a date folder for dated files, a year folder containing a `00` month folder containing a `yyyy-00-00` folder for year-only files, or the shared `no-exif` folder for undated files — and moving each file into its target folder. The nested subfolders SHALL be created with an idempotent create-directories operation that creates every missing parent in the path, so repeated items for the same date do not fail and missing year or month folders are created on demand; an existing target folder SHALL NOT be treated as an error; if a target subfolder cannot be created for any reason (for example its path is occupied by a non-directory, or the source folder no longer exists), the affected items SHALL be marked as failed rather than aborting the run. Each move SHALL be performed with a move operation invoked with no replace-existing option, so that it fails when the target path already exists; the implementation SHALL detect a conflict by catching that already-exists failure rather than doing a separate existence check followed by a move, so there is no time-of-check-to-time-of-use gap. (The conflict guarantee comes from omitting the replace-existing option, not from requesting an atomic-move option, whose fail-on-existing behavior is platform-dependent.) This single mechanism covers both conflict cases — a file that existed at the target before the run, and a target a same-run earlier item already moved a file into — and on a case-insensitive filesystem it also catches names that differ only in case. In every conflict the system SHALL skip the item, mark it as failed, and continue with the remaining items; it SHALL NOT overwrite the existing file. The move is expected to stay on one filesystem because every target is a subfolder of the source, but the implementation SHALL still treat any move that throws as a per-item failure. When any item fails for any reason, the system SHALL leave the source file unchanged (no partial copy or truncation). The system SHALL return a per-item result indicating success or failure.

#### Scenario: Successful execution

- **WHEN** a move plan with dated, year-only, and undated files is executed and no name conflicts exist
- **THEN** the system creates the nested year/month/date subfolders, the year/`00`/`yyyy-00-00` subfolders, and the `no-exif` folder as needed, moves each file into its target folder, and reports each item as successful

#### Scenario: Conflict with a pre-existing file at target

- **WHEN** a file target folder already contains a file with the same name before the run
- **THEN** the system skips that file without overwriting, marks it as failed, leaves the source file unchanged, and continues moving the remaining files

#### Scenario: Two source files collide on the same target path within one run

- **WHEN** two items in the plan resolve to the same target path (same folder and same file name)
- **THEN** the first item is moved successfully and the second is reported as failed (skipped) without overwriting, and its source file is left unchanged

##### Example: execution with pre-existing and intra-run conflicts

- **GIVEN** plan items `A.jpg → /photos/2024/03/2024-03-15`, `B.jpg → /photos/2024/03/2024-03-15`, `C.jpg → /photos/2024/03/2024-03-15` where `/photos/2024/03/2024-03-15/A.jpg` already exists and `B.jpg` and `C.jpg` share the same file name
- **WHEN** the plan is executed
- **THEN** `A.jpg` is reported as failed (pre-existing target), the first of the same-named pair is moved successfully, the second is reported as failed (intra-run conflict), and every failed item source file is left unchanged
