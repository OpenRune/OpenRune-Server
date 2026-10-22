package org.rsmod.content.raids.toa.party

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.table.ToaInvocationsRow

enum class ToaInvocationCategory {
    ATTEMPTS,
    TIME_LIMIT,
    HELPFUL_SPIRIT,
    PATH_LEVEL,
    PRAYER,
    RESTORATION,
    PATHS,
    AKKHA,
    KEPHRI,
    ZEBAK,
    BA_BA,
    THE_WARDENS,
    BLAZING_TOMBS,
}

class ToaInvocation private constructor(private val row: ToaInvocationsRow) {

    val name: String = row.name
    val index: Int = row.index
    val category: ToaInvocationCategory = categoryOf(row.category)
    val levelModifier: Int = row.levelModifier

    val prerequisite: ToaInvocation? by lazy { row.prerequisite?.let { byRowId.getValue(it.rowId) } }

    val dependents: List<ToaInvocation> by lazy { ALL.filter { it.prerequisite === this } }

    val eventOnly: Boolean
        get() = category == ToaInvocationCategory.BLAZING_TOMBS

    companion object {
        private const val CATEGORY_OFFSET = 3

        val ALL: List<ToaInvocation> by lazy { ToaInvocationsRow.all().map(::ToaInvocation) }

        private val byRowId: Map<Int, ToaInvocation> by lazy {
            ALL.associateBy { it.row.rowId }
        }

        fun of(symbol: String): ToaInvocation {
            RSCM.requireRSCM(RSCMType.DBROW, symbol)
            return byRowId.getValue(symbol.asRSCM() and 0xFFFF)
        }

        private fun categoryOf(cacheValue: Int): ToaInvocationCategory =
            ToaInvocationCategory.entries.getOrNull(cacheValue - CATEGORY_OFFSET)
                ?: error("Unknown invocation category $cacheValue")
    }
}
