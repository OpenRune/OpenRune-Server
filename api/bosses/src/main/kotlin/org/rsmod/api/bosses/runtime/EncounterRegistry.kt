package org.rsmod.api.bosses.runtime

import jakarta.inject.Singleton
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcUid

@Singleton
class EncounterRegistry {
    private val encounters = mutableMapOf<NpcUid, BossEncounter>()
    private val specs = mutableMapOf<Int, BossSpec>()

    fun register(npcTypeId: Int, spec: BossSpec) {
        specs[npcTypeId] = spec
    }

    fun of(npc: Npc): BossEncounter {
        return encounters.getOrPut(npc.uid) {
            val spec = specs[npc.type.id] ?: specs.values.first()
            BossEncounter(npc, spec)
        }
    }

    fun remove(npc: Npc) {
        encounters.remove(npc.uid)
    }
}
