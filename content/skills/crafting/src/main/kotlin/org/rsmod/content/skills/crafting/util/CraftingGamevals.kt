package org.rsmod.content.skills.crafting.util

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType

/** Defensive resolution for crafting gameval strings, where a raw RSCM lookup would throw. */
object CraftingGamevals {
    private val logger = InlineLogger()
    private val resolvable = HashMap<String, Boolean>()
    private val warned = HashSet<String>()

    /** The RSCM type a gameval prefix names. */
    private fun typeOf(gameval: String): RSCMType? =
        when (gameval.substringBefore('.', "")) {
            "seq" -> RSCMType.SEQ
            "synth" -> RSCMType.SYNTH
            "spotanim" -> RSCMType.SPOTANIM
            "loc" -> RSCMType.LOC
            "npc" -> RSCMType.NPC
            "obj" -> RSCMType.OBJ
            "varbit" -> RSCMType.VARBIT
            "interface" -> RSCMType.INTERFACE
            "component" -> RSCMType.COMPONENT
            else -> null
        }

    /** True if [gameval] resolves against the cache. Never throws. */
    fun exists(gameval: String): Boolean =
        resolvable.getOrPut(gameval) {
            val type = typeOf(gameval) ?: return@getOrPut false
            try {
                gameval.asRSCM(type)
                true
            } catch (_: Exception) {
                false
            }
        }

    /** Returns [gameval] if it resolves, else null, logging the miss once. Use for anim and sound. */
    fun optional(gameval: String?): String? {
        if (gameval == null) return null
        if (exists(gameval)) return gameval
        warnOnce(gameval)
        return null
    }

    /** Keeps only resolvable entries, logging any misses once. Use for loc and npc trigger lists. */
    fun filterResolvable(gamevals: List<String>): List<String> =
        gamevals.filter { entry ->
            exists(entry).also { if (!it) warnOnce(entry) }
        }

    /** Resolves an obj gameval to its numeric id, or null when it does not resolve. */
    fun objOrNull(gameval: String): Int? =
        if (exists(gameval)) {
            try {
                gameval.asRSCM(RSCMType.OBJ)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

    /** Logs a missing gameval the first time it is seen. */
    private fun warnOnce(gameval: String) {
        if (warned.add(gameval)) {
            logger.warn { "Crafting: gameval '$gameval' did not resolve; skipping it." }
        }
    }
}
