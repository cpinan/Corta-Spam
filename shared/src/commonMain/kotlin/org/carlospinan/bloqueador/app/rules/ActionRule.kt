package org.carlospinan.bloqueador.app.rules

/** Lightweight action-rule model for the resolver. */
data class ActionRule(
    val id: Long,
    val label: String?,
    val attempts: Int,
    val windowMinutes: Int,
    val enabled: Boolean,
)
