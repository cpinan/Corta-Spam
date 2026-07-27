package org.carlospinan.bloqueador.app.rules

/**
 * Lightweight data model for a pattern rule, decoupled from SQLDelight-generated code.
 * Used by the resolver and repository layer.
 */
data class PatternRule(
    val id: Long,
    val pattern: String,
    val label: String?,
    val enabled: Boolean,
)
