package org.carlospinan.bloqueador.app.rules

import org.carlospinan.bloqueador.app.backup.BackupActionRule
import org.carlospinan.bloqueador.app.backup.BackupAllowlistedNumber
import org.carlospinan.bloqueador.app.backup.BackupBlockedNumber
import org.carlospinan.bloqueador.app.backup.BackupCountryRule
import org.carlospinan.bloqueador.app.backup.BackupPatternRule
import org.carlospinan.bloqueador.app.backup.BackupScheduleRule

/**
 * Gate for entries coming back in through backup restore.
 *
 * A backup file is plain JSON the user can hand-edit or receive from someone else, so it is the
 * one path into the rule tables that does not go through the app's own input validation. Values
 * the UI cannot produce reach the resolver and behave badly there: an action rule with
 * `attempts = 0` satisfies `count >= attempts` for every caller and blocks the entire phone,
 * and a quiet-hours window outside 0..1439 can never be entered *or* left. Rejecting at the
 * door keeps the resolver free of defensive checks on data it should be able to trust.
 */
object BackupEntryValidator {
    /** Minutes since local midnight; 1440 would be the next day's 00:00, hence the exclusive end. */
    private val MINUTE_OF_DAY = 0..1439

    fun isValid(entry: BackupBlockedNumber): Boolean = hasDigits(entry.number)

    fun isValid(entry: BackupAllowlistedNumber): Boolean = hasDigits(entry.number)

    fun isValid(entry: BackupPatternRule): Boolean = RulePrecedenceResolver.isUsablePattern(entry.pattern)

    fun isValid(entry: BackupCountryRule): Boolean = entry.countryCode.isNotBlank() && entry.countryName.isNotBlank()

    fun isValid(entry: BackupActionRule): Boolean = entry.attempts >= 1 && entry.windowMinutes >= 1

    fun isValid(entry: BackupScheduleRule): Boolean = entry.startMinute in MINUTE_OF_DAY && entry.endMinute in MINUTE_OF_DAY

    /**
     * `created_at` is stored in whole seconds (the schema default is `strftime('%s','now')`) and
     * drives list order. A backup missing it, or carrying a nonsense value, gets stamped with
     * [nowSeconds] so the row still sorts sanely instead of landing at the epoch.
     */
    fun createdAtOrNow(
        createdAt: Long,
        nowSeconds: Long,
    ): Long = if (createdAt > 0) createdAt else nowSeconds

    private fun hasDigits(number: String): Boolean = PhoneNumberParser.normalizeForComparison(number).isNotEmpty()
}
