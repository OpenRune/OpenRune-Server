package org.rsmod.content.bosses.dagannothkings

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WaterbirthPetRock
@Inject
constructor(
    private val interactions: HeldInteractions,
    private val npcs: NpcRepository,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpHeld5(PET_ROCK) { dropPetRock(it) }
        onOpNpc1(PET_ROCK_NPC) { pickUp(it.npc) }
    }

    private suspend fun ProtectedAccess.dropPetRock(event: HeldObjEvents.Op5) {
        if (player.coords !in PAD_TILES) {
            interactions.drop(this, event.inventory, event.slot)
            return
        }
        if (npcs.findAll(player.coords).any()) {
            mes("There is already something on this pressure pad.")
            return
        }
        if (!invDel(inv, PET_ROCK, count = 1, slot = event.slot).success) {
            return
        }
        val type = checkNotNull(ServerCacheManager.getNpc(PET_ROCK_NPC_TYPE.asRSCM(RSCMType.NPC)))
        npcs.add(Npc(type, player.coords), Int.MAX_VALUE)
        mes("You place the pet rock on the pressure pad.")
    }

    private fun ProtectedAccess.pickUp(npc: Npc) {
        if (inv.isFull()) {
            mes("You don't have enough inventory space to pick that up.")
            return
        }
        npcs.del(npc, 0)
        invAdd(inv, PET_ROCK, 1)
        mes("You pick up the pet rock.")
    }

    private companion object {
        const val PET_ROCK = "obj.vt_useless_rock"
        const val PET_ROCK_NPC = "npc.dagannoth_dungeon_pressure_pet_rock"
        const val PET_ROCK_NPC_TYPE = PET_ROCK_NPC

        val PAD_TILES =
            setOf(
                CoordGrid(2490, 10130, 0),
                CoordGrid(2490, 10132, 0),
                CoordGrid(2490, 10146, 0),
                CoordGrid(2490, 10148, 0),
                CoordGrid(2490, 10162, 0),
                CoordGrid(2490, 10164, 0),
            )
    }
}
