package org.carlospinan.bloqueador.app.telecom

import org.carlospinan.bloqueador.app.R

/** The canned lines, in [StallPolicy.nextLine]'s 1-based order. Read with `getString`, so they follow the app locale. */
object StallLines {
    val IDS =
        intArrayOf(
            R.string.stall_line_1,
            R.string.stall_line_2,
            R.string.stall_line_3,
            R.string.stall_line_4,
            R.string.stall_line_5,
            R.string.stall_line_6,
            R.string.stall_line_7,
            R.string.stall_line_8,
        )
}
