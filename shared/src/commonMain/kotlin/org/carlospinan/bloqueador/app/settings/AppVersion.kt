package org.carlospinan.bloqueador.app.settings

/**
 * The build actually running on this device.
 *
 * Read from the platform's own package metadata and supplied through DI, rather than declared as
 * a constant in `commonMain`. A constant would be a second version number to remember to bump,
 * and the whole point of showing it is to answer "is this the build I think it is?" — an answer
 * that is worthless if it can disagree with the artifact it is printed on.
 *
 * [code] is what a store increments per upload and is the value that settles which of two builds
 * is newer; [name] is what a human quotes in a bug report. A version row shows both because a
 * tester who says "I'm on 1.4.0" has not said whether they are on the build that fixed anything.
 */
data class AppVersion(
    val name: String,
    val code: Long,
) {
    companion object {
        /**
         * What a platform that cannot answer returns. Rendered as-is rather than hidden: a blank
         * version row is indistinguishable from a build that forgot to add one, and "unknown" at
         * least tells the person reading a screenshot that the lookup failed.
         */
        val Unknown = AppVersion(name = "?", code = 0L)
    }
}
