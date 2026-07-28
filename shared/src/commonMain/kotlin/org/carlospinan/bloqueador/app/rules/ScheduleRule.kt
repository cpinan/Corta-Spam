package org.carlospinan.bloqueador.app.rules

/** Lightweight schedule (quiet hours) rule model for the resolver. */
data class ScheduleRule(
    val id: Long,
    val label: String?,
    val startMinute: Int,
    val endMinute: Int,
    val enabled: Boolean,
)
