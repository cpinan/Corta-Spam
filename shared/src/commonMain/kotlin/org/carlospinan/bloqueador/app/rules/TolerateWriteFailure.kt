package org.carlospinan.bloqueador.app.rules

import kotlinx.coroutines.CancellationException

/**
 * Runs [write], swallowing any failure so an optional side effect (a history row) can never undo
 * a decision the caller has already applied.
 *
 * The bug this exists to fix: `PassthroughInCallService.onCallAdded` used to log a blocked call
 * and set `CallState.blockedByRules` in the same unguarded sequence. A [CallLogRepository.logCall]
 * exception thrown between them left the full-screen gate believing the call was blocked (it had
 * already been told so) while `blockedByRules` itself was never set — so nothing else (the
 * ringer, the notification, the auto-responder) ever treated the call as blocked either, and it
 * rang until the caller gave up. Extracted here, rather than left as an inline `try`/`catch`,
 * because the service that needed this cannot be constructed in a unit test, and this is the
 * difference between a blocked call staying blocked and one silently ringing forever.
 *
 * `CancellationException` is rethrown rather than swallowed — catching it here would mark a
 * cancelled coroutine as merely "failed", which is the coroutines anti-pattern this project has
 * hand-written a guard against at every other suspend call site that can throw.
 */
suspend fun <T> tolerateWriteFailure(
    onFailure: (Exception) -> Unit,
    write: suspend () -> T,
): T? =
    try {
        write()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onFailure(e)
        null
    }
