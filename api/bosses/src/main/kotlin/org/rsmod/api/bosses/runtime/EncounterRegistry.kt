package org.rsmod.api.bosses.runtime

import jakarta.inject.Singleton
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.game.entity.Npc

@Singleton
class EncounterRegistry {
    private val encounters = mutableMapOf<Int, BossEncounter>()
    private val specs = mutableMapOf<Int, BossSpec>()

    fun register(npcTypeId: Int, spec: BossSpec) {
        specs[npcTypeId] = spec
    }

    fun of(npc: Npc): BossEncounter {
        return encounters.getOrPut(npc.slotId) {
            val spec = specs[npc.type.id] ?: specs.values.first()
            BossEncounter(npc, spec)
        }
    }

    fun remove(npc: Npc) {
        encounters.remove(npc.slotId)
    }
}
