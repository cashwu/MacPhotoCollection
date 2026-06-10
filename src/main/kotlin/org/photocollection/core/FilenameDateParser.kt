package org.photocollection.core

import java.time.DateTimeException
import java.time.LocalDate

/**
 * Tier 2 of the date fallback chain: parse a capture date from a file name. Pure and side-effect
 * free — the caller supplies [today] so the "not in the future" bound is testable with a fixed date.
 */
object FilenameDateParser {

    /** Earliest plausible year; values before it are rejected as serial numbers, not dates. */
    private const val MIN_YEAR = 1990

    /**
     * The `yyyyMMdd`, `yyyy-MM-dd`, and `yyyy_MM_dd` forms. The `(?<!\d)`/`(?!\d)` guards keep a
     * match to a complete digit run, so an 8-digit window inside a longer run (such as a 12-digit
     * timestamp) is not a candidate, and a separated form flanked by a digit is rejected.
     */
    private val PATTERNS: List<Regex> = listOf(
        Regex("""(?<!\d)(\d{4})(\d{2})(\d{2})(?!\d)"""),
        Regex("""(?<!\d)(\d{4})-(\d{2})-(\d{2})(?!\d)"""),
        Regex("""(?<!\d)(\d{4})_(\d{2})_(\d{2})(?!\d)"""),
    )

    /**
     * The first valid date among all pattern candidates, taken by earliest start position in the
     * file name (the extension excluded) regardless of which form it belongs to, or null when no
     * candidate is a real date in `[1990-01-01, today]`.
     */
    fun parse(fileName: String, today: LocalDate): LocalDate? {
        val stem = fileName.substringBeforeLast('.')
        return PATTERNS
            .flatMap { it.findAll(stem) }
            .sortedBy { it.range.first }
            .firstNotNullOfOrNull { validate(it, today) }
    }

    private fun validate(match: MatchResult, today: LocalDate): LocalDate? {
        val (year, month, day) = match.destructured
        val date = try {
            LocalDate.of(year.toInt(), month.toInt(), day.toInt())
        } catch (e: DateTimeException) {
            return null
        }
        if (date.year < MIN_YEAR || date.isAfter(today)) return null
        return date
    }
}
