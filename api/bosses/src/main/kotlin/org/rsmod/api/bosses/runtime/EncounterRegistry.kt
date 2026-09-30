package org.rsmod.api.bosses.runtime

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc

@Singleton
class EncounterRegistry @Inject constructor(private val mapClock: MapClock) {
    private class Registration(val specs: Collection<BossSpec>, val default: BossSpec?)

    private val encounters = mutableMapOf<Int, BossEncounter>()
    private val registrations = mutableMapOf<Int, Registration>()

    /**
     * [specs] are the specs an npc of [npcTypeId] may run. Without a [default], each npc must be
     * given one with [start] before its encounter is first used.
     */
    fun register(npcTypeId: Int, specs: Collection<BossSpec>, default: BossSpec?) {
        registrations[npcTypeId] = Registration(specs, default)
    }

    fun of(npc: Npc): BossEncounter {
        return encounters.getOrPut(npc.slotId) {
            val spec =
                registration(npc).default
                    ?: error("No spec assigned to npc type ${npc.type.id}; call startEncounter at spawn.")
            BossEncounter(npc, spec, mapClock)
        }
    }

    /** Starts [npc]'s encounter on [spec], one of the specs registered for its type. */
    fun start(npc: Npc, spec: BossSpec): BossEncounter {
        check(npc.slotId !in encounters) { "Npc type ${npc.type.id} already has an encounter." }
        require(registration(npc).specs.any { it === spec }) {
            "Spec is not one of those registered for npc type ${npc.type.id}."
        }
        return BossEncounter(npc, spec, mapClock).also { encounters[npc.slotId] = it }
    }

    fun remove(npc: Npc): BossEncounter? = encounters.remove(npc.slotId)

    fun isActive(encounter: BossEncounter): Boolean = encounters[encounter.npc.slotId] === encounter

    private fun registration(npc: Npc): Registration =
        registrations[npc.type.id] ?: error("No boss spec registered for npc type ${npc.type.id}.")
}
