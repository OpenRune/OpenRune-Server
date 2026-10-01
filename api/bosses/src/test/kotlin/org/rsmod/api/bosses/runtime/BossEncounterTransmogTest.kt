package org.rsmod.api.bosses.runtime

import dev.openrune.types.NpcServerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.dsl.resetAnim
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.BossStats
import org.rsmod.api.bosses.spec.PhaseSpec
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.map.CoordGrid

class BossEncounterTransmogTest {
    private val base = NpcServerType(id = 1, name = "Boss", size = 1, hitpoints = 50)
    private val shielded = NpcServerType(id = 2, name = "Boss", size = 1, hitpoints = 50)
    private val types = mapOf("npc.boss" to base, "npc.boss_shielded" to shielded)

    @Test
    fun `transition into a phase with a transmog transmogs the npc`() {
        val encounter = encounter()
        encounter.transitionTo("shielded", tick = 1)
        assertEquals(shielded, encounter.npc.visType)
        assertEquals(NpcUid(SLOT, shielded.id), encounter.npc.uid)
    }

    @Test
    fun `transition into a phase without a transmog keeps the current type`() {
        val encounter = encounter()
        encounter.transitionTo("shielded", tick = 1)
        encounter.transitionTo("plain", tick = 2)
        assertEquals(shielded, encounter.npc.visType)
    }

    @Test
    fun `transition into a phase declaring the base type reverts the transmog`() {
        val encounter = encounter()
        encounter.transitionTo("shielded", tick = 1)
        encounter.transitionTo("fight", tick = 2)
        assertEquals(base, encounter.npc.visType)
    }

    private fun encounter(): BossEncounter {
        val npc = Npc(base, CoordGrid(0, 1, 1, 0, 0)).apply { slotId = SLOT }
        val spec =
            BossSpec(
                npcTypes = types.keys.toList(),
                stats = BossStats(),
                abilities = mapOf("a" to resetAnim()),
                phases =
                    mapOf(
                        "fight" to PhaseSpec("fight", transmog = "npc.boss"),
                        "shielded" to PhaseSpec("shielded", transmog = "npc.boss_shielded"),
                        "plain" to PhaseSpec("plain"),
                    ),
                triggers = emptyList(),
            )
        return BossEncounter(npc, spec, MapClock(), types::get)
    }

    private companion object {
        const val SLOT = 1
    }
}
