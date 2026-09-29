package org.rsmod.content.quest.manager

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess

/**
 * Lets a quest lock a loc that another module handles, such as Doric's anvils before Doric's Quest.
 * A gate returns true to let the owning handler carry on; when it returns false it has already
 * told the player why.
 */
object QuestLocGates {
    private val gates = hashMapOf<Int, suspend ProtectedAccess.() -> Boolean>()

    fun register(loc: String, allows: suspend ProtectedAccess.() -> Boolean) {
        gates[loc.asRSCM(RSCMType.LOC)] = allows
    }

    suspend fun allows(access: ProtectedAccess, locId: Int): Boolean =
        gates[locId]?.invoke(access) ?: true
}
