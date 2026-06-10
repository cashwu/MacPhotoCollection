## MODIFIED Requirements

### Requirement: Compute move plan grouped by capture date

The system SHALL compute a move plan from a list of image files without modifying the file system. Each file that has a capture date SHALL be assigned a target path under the source folder named after its capture date in `yyyy-MM-dd` form. Each move item SHALL carry the file's resolved capture date together with its date source, so the plan preview can display which fallback tier produced the date. The target path SHALL be constructed with the platform path API as an absolute path resolving the source folder against the `yyyy-MM-dd` date as a single path segment (i.e. `<sourceFolder>/<yyyy-MM-dd>/<fileName>`), not by raw string concatenation, so that planning and execution agree on the exact path. Each file that has no capture date SHALL be placed into an error list and SHALL NOT be assigned a target path. Computing the plan SHALL NOT create folders or move any file.

#### Scenario: Mixed files produce plan and error list

- **WHEN** the move plan is computed for files with and without capture dates
- **THEN** dated files appear as move items targeting their date folder, each move item carrying the file's resolved capture date and its source, and undated files appear in the error list, and no file on disk is changed

##### Example: plan grouping

- **GIVEN** source folder `/photos` with files: IMG1(2024-03-15, EXIF), IMG2(2024-03-15, FILENAME), IMG3(no date)
- **WHEN** the move plan is computed
- **THEN** the plan contains move items `IMG1 → /photos/2024-03-15` carrying (2024-03-15, EXIF) and `IMG2 → /photos/2024-03-15` carrying (2024-03-15, FILENAME), and the error list contains `IMG3`

---
### Requirement: Extract capture date from EXIF

The system SHALL resolve a capture date for each image file using a three-tier fallback chain, in this order: (1) EXIF metadata, (2) a date parsed from the file name, (3) the file system dates. The resolved date SHALL be exposed formatted as `yyyy-MM-dd` together with a date source marker identifying which tier produced it (`EXIF`, `FILENAME`, or `FILE_SYSTEM`).

**Tier 1 — EXIF.** The system SHALL use the EXIF `DateTimeOriginal` tag (0x9003) as the primary source and, when it is absent, SHALL fall back to `DateTimeDigitized` (0x9004); the date SHALL be taken as the local civil date contained in the EXIF value with no timezone conversion. To guarantee no timezone conversion, the system SHALL read the raw EXIF string value of the tag and parse its date portion directly, and SHALL NOT use a date accessor that returns an instant/epoch value. When the EXIF value is absent, malformed, or the zero sentinel `0000:00:00 00:00:00`, the system SHALL proceed to tier 2. Any exception raised while reading a file's EXIF metadata (for example a corrupt image) SHALL be treated the same as an absent EXIF value: the system SHALL proceed to tier 2.

**Tier 2 — file name.** The system SHALL search the file name (excluding the extension) for date patterns of the forms `yyyy-MM-dd`, `yyyy_MM_dd`, and `yyyyMMdd`. In the separated forms the two separators SHALL be identical (both `-` or both `_`); a mixed-separator string such as `2025-10_28` SHALL NOT be considered a candidate. The system SHALL consider candidates of all three forms together and take the candidate with the earliest start position in the file name that passes validation, regardless of which form it belongs to; ties cannot occur because two candidates cannot start at the same position. A pattern SHALL match only complete digit runs: a match whose immediately preceding or following character is another digit SHALL NOT be considered a candidate (so a digit run longer than 8 digits, such as a 12-digit timestamp, contains no `yyyyMMdd` candidate). A candidate is valid only when it denotes a real calendar date, its year is 1990 or later, and the date is not later than the current date; both bounds are inclusive, so 1990-01-01 and the current date itself are valid, while dates later than the current date SHALL be rejected. The current date SHALL be taken in the system default time zone and supplied by the caller (so the parser stays a pure function). When no candidate passes validation, the system SHALL proceed to tier 3.

**Tier 3 — file system dates.** The system SHALL read the file's creation time and last-modified time via the platform file attribute API, convert each to the local civil date using the system default time zone, and use the earlier of the two. A date earlier than 1990-01-01 SHALL be treated as a sentinel and ignored, whichever of the two dates it is (the creation date is a sentinel on file systems that do not record a birth time and report an epoch value; the last-modified date is a sentinel when tools such as zip extraction write a DOS-epoch 1980-01-01 value); when one date is a sentinel, the system SHALL use the other date alone. When both dates are earlier than 1990-01-01, the system SHALL treat tier 3 as failed and return the "no date" result. File system dates later than the current date SHALL be accepted as-is: tier 3 is the final tier, and an implausible folder name remains visible and correctable in the plan preview, whereas rejecting the file would leave it unorganizable.

When reading the file attributes in tier 3 fails (for example an I/O error), or when both file system dates are earlier than 1990-01-01, the system SHALL return a distinguishable "no date" result for that file. The system SHALL catch any exception raised while processing a single file and SHALL NOT let it abort processing of the remaining files.

#### Scenario: Image with valid EXIF date

- **WHEN** an image's EXIF `DateTimeOriginal` contains a capture date of 2024-03-15 at 23:50 local time
- **THEN** the system returns the capture date formatted as "2024-03-15" with source `EXIF` and no timezone adjustment

#### Scenario: Image without EXIF but with a date in the file name

- **WHEN** an image has no usable EXIF capture date and its file name contains a valid date pattern
- **THEN** the system returns the date parsed from the file name with source `FILENAME`

##### Example: filename date parsing

| File name | Result |
| --------- | ------ |
| SCR-20251028-jljd.png | 2025-10-28 (FILENAME) |
| Screenshot 2025-10-28 at 10.17.png | 2025-10-28 (FILENAME) |
| IMG_2024_03_15_home.png | 2024-03-15 (FILENAME) |
| pic_19900101.png | 1990-01-01 (FILENAME — lower bound inclusive) |
| shot_2026-06-10.png (when the current date is 2026-06-10) | 2026-06-10 (FILENAME — a candidate equal to the current date is valid) |
| 12345678.png | no filename date (1234-56-78 is not a real date) — falls to tier 3 |
| IMG_20991231.png | no filename date (later than current date) — falls to tier 3 |
| photo_19891231.png | no filename date (year before 1990) — falls to tier 3 |
| IMG_202510281017.png | no filename date (digit run longer than 8, no complete-run candidate) — falls to tier 3 |
| 920251028.png | no filename date (digit run longer than 8, no complete-run candidate) — falls to tier 3 |
| IMG_20991231_20251028.png | 2025-10-28 (FILENAME — first candidate 2099-12-31 rejected as later than current date, second candidate taken) |
| 2025_10_28_20240101.png | 2025-10-28 (FILENAME — the `yyyy_MM_dd` candidate at position 0 wins over the later `yyyyMMdd` candidate, by earliest start position across all forms) |
| 12025-10-28.png | no filename date (the `yyyy-MM-dd` match is immediately preceded by a digit, no complete-run candidate) — falls to tier 3 |
| shot_2025-10-281.png | no filename date (the `yyyy-MM-dd` match is immediately followed by a digit, no complete-run candidate) — falls to tier 3 |
| mix_2025-10_28.png | no filename date (mixed separators are not a candidate) — falls to tier 3 |
| random.png | no filename date — falls to tier 3 |

#### Scenario: Image without EXIF or filename date uses file system dates

- **WHEN** an image has no usable EXIF capture date and no valid date in its file name
- **THEN** the system returns the earlier of the file's creation date and last-modified date, as local civil dates, with source `FILE_SYSTEM`

##### Example: earlier of creation and modification

| Creation date | Modified date | Result |
| ------------- | ------------- | ------ |
| 2025-11-01 | 2025-10-30 | 2025-10-30 (FILE_SYSTEM) |
| 2025-10-28 | 2025-10-28 | 2025-10-28 (FILE_SYSTEM) |
| 2025-10-28 | 2025-12-01 | 2025-10-28 (FILE_SYSTEM) |
| 1970-01-01 | 2025-10-30 | 2025-10-30 (FILE_SYSTEM — creation sentinel before 1990 ignored) |
| 2025-11-01 | 1980-01-01 | 2025-11-01 (FILE_SYSTEM — modified sentinel before 1990 ignored) |
| 1970-01-01 | 1980-01-01 | no date (both dates before 1990) — error list |
| 2026-12-31 | 2026-12-31 | 2026-12-31 (FILE_SYSTEM — future file system dates accepted as-is) |

#### Scenario: File attributes cannot be read

- **WHEN** an image has no usable EXIF or filename date and reading its file attributes raises an I/O error
- **THEN** the system returns a "no date" result for that file and continues processing other files

##### Example: full tier resolution

| EXIF state | File name | Result |
| ---------- | --------- | ------ |
| DateTimeOriginal = `2024:03:15 23:50:00` | SCR-20251028-jljd.png | 2024-03-15 (EXIF — tier 1 wins over filename) |
| DateTimeOriginal absent, DateTimeDigitized = `2022:01:02 08:30:00` | random.png | 2022-01-02 (EXIF) |
| no EXIF block (typical PNG/GIF) | SCR-20251028-jljd.png | 2025-10-28 (FILENAME) |
| no EXIF block | random.png (created 2025-11-01, modified 2025-10-30) | 2025-10-30 (FILE_SYSTEM) |
| value = `0000:00:00 00:00:00` | 12345678.png | falls through tiers 1 and 2 to FILE_SYSTEM |
| metadata read throws (corrupt image bytes) | SCR-20251028-jljd.png | 2025-10-28 (FILENAME — exception treated as absent EXIF) |
| DateTimeOriginal malformed (`garbage`), DateTimeDigitized = `2022:01:02 08:30:00` | random.png (created 2025-11-01, modified 2025-10-30) | 2025-10-30 (FILE_SYSTEM — a malformed `DateTimeOriginal` does not fall back to `DateTimeDigitized`; existing tier 1 semantics kept) |
