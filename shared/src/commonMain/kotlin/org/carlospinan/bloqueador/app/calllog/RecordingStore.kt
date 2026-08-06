package org.carlospinan.bloqueador.app.calllog

/**
 * Deletes auto-responder recordings from the filesystem.
 *
 * The audio lives as a file and only its path lives in `CallLogEntry.recording_path`, so
 * dropping a log row leaks the file unless something deletes it. That "something" has to reach
 * the filesystem, which `commonMain` cannot, hence expect/actual rather than a plain function.
 *
 * Deliberately delete-only. Recording is Android-only (iOS has no equivalent capability -- see
 * `docs/SPEC.md` §1), so the iOS actual exists to satisfy the compiler and to make "clear log"
 * behave identically on both platforms, not because iOS will ever write one of these files.
 */
expect object RecordingStore {
    /**
     * Deletes [path], returning true when the file is gone afterwards.
     *
     * A missing file counts as success: the row and the file can already disagree if the user
     * cleared app storage, and failing the delete of a file that is already absent would leave
     * the database row pointing at nothing forever.
     */
    fun delete(path: String): Boolean

    fun exists(path: String): Boolean
}
