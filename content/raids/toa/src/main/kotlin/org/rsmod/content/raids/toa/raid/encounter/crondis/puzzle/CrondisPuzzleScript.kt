package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.combatDefaultRetaliate
import org.rsmod.api.config.Constants
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.vars.typePlayerUidVarn
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.script.onOpObj3
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.onRoomAiTimer
import org.rsmod.content.raids.toa.raid.encounter.onRoomNpcHit
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CrondisPuzzleScript
@Inject
constructor(
    private val playerList: PlayerList,
    private val interactions: AiPlayerInteractions,
) : PluginScript() {

    override fun ScriptContext.startup() {
        val containerType = ServerCacheManager.getItem(CrondisObjs.CONTAINER.asRSCM(RSCMType.OBJ))!!
        onOpObj3(containerType) { takeContainer() }

        onOpLoc1(CrondisLocs.WATER_SOURCE) { fill(it.loc) }
        onOpLocU(CrondisLocs.WATER_SOURCE, CrondisObjs.CONTAINER) { fill(it.loc) }
        onOpLocU(CrondisLocs.WATER_SOURCE_EMPTY, CrondisObjs.CONTAINER) { fillFromEmpty() }

        onOpHeld1(CrondisObjs.CONTAINER) { checkContainer(it.obj) }
        onOpHeld2(CrondisObjs.CONTAINER) { emptyContainer() }

        for (palm in CrondisNpcs.WATERABLE_PALMS) {
            val palmType = ServerCacheManager.getNpc(palm.asRSCM(RSCMType.NPC))!!
            onOpNpc1(palm) { waterPalm() }
            onOpNpcU(palmType, containerType) { waterPalm() }
        }

        onRoomAiTimer<CrondisPuzzleEncounter>(CrondisNpcs.CROCODILE) { crocodiles.ai(it) }

        val crocodileType = ServerCacheManager.getNpc(CrondisNpcs.CROCODILE.asRSCM(RSCMType.NPC))!!
        onRoomNpcHit<CrondisPuzzleEncounter>(CrondisNpcs.CROCODILE) { croc, hit ->
            if (hit.isFromPlayer) {
                val player = hit.resolvePlayerSource(playerList) ?: return@onRoomNpcHit
                crocodiles.hitBy(croc, player)
            }
        }
        onNpcQueue(crocodileType, "queue.com_retaliate_player") { crocodileRetaliate() }
    }

    private fun StandardNpcAccess.crocodileRetaliate() {
        val attacker = interactions.resolvePlayer(npc.aggressivePlayer) ?: return
        if (attacker.containerSlot() != null) return
        npc.combatDefaultRetaliate(interactions)
    }

    private val ProtectedAccess.room: CrondisPuzzleEncounter?
        get() = ToaRooms.of<CrondisPuzzleEncounter>(player)

    private suspend fun ProtectedAccess.takeContainer() {
        val room = room ?: return
        if (room.stage == ToaStage.COMPLETED) {
            mes("You don't need a container right now.")
            return
        }
        if (inv.contains(CrondisObjs.CONTAINER)) {
            mes("You already have a container.")
            return
        }
        if (inv.freeSpace() == 0) {
            mes("You do not have enough space to pick this up.")
            return
        }
        if (invAdd(inv, InvObj(CrondisObjs.CONTAINER, 1, vars = 0)).failure) return
        soundSynth(CrondisSynths.TAKE)
        anim(CrondisSeqs.PICKUP)
    }

    private fun ProtectedAccess.checkContainer(obj: InvObj) {
        mes("Your water container is at ${obj.vars}% capacity.")
    }

    private suspend fun ProtectedAccess.emptyContainer() {
        if (player.containerWater() <= 0) {
            mes("It's already empty.")
            return
        }
        val empty =
            choice2(
                "Yes, empty water container.",
                true,
                "No.",
                false,
                title = "Empty water container",
            )
        if (!empty) return
        val slot = player.containerSlot() ?: return
        player.setContainerWater(slot, 0)
        mes("You empty your water container.")
    }

    private suspend fun ProtectedAccess.fill(waterfall: BoundLocInfo) {
        arriveDelay()
        val room = room ?: return
        if (room.stage == ToaStage.COMPLETED) {
            mes("You don't need to do that right now.")
            return
        }
        val slot = player.containerSlot()
        if (slot == null) {
            mes("You don't have anything to fill.")
            return
        }
        if ((inv[slot]?.vars ?: 0) >= CONTAINER_FULL) {
            mes("Your container is full.")
            return
        }
        mes("You fill your container.")
        soundSynth(CrondisSynths.FILL)
        anim(CrondisSeqs.PICKUP)
        player.setContainerWater(slot, CONTAINER_FULL)
        room.drainWaterfall(waterfall)
        drink()
        delay(1)
    }

    private fun ProtectedAccess.drink() {
        if (player.runEnergy >= DRINK_BELOW_ENERGY) return
        val energy = player.runEnergy + FILL_RUN_ENERGY
        player.runEnergy = energy.coerceAtMost(Constants.run_max_energy)
        spam("While you're there, you take a drink of refreshing water!")
    }

    private suspend fun ProtectedAccess.fillFromEmpty() {
        arriveDelay()
        if (room == null) return
        mes("It's empty.")
        soundSynth(CrondisSynths.DECLINE)
    }

    private suspend fun ProtectedAccess.waterPalm() {
        val room = room ?: return
        val slot = player.containerSlot()
        val water = slot?.let { inv[it]?.vars } ?: 0
        if (slot == null || water < 1) {
            mes("You have nothing to water the palm with.")
            return
        }
        val message =
            when {
                water <= 25 -> "You empty a small amount of water onto the palm."
                water <= 50 -> "You empty a reasonable amount of water onto the palm."
                else -> "You empty a lot of water onto the palm!"
            }
        mes(message)
        anim(CrondisSeqs.PICKUP)
        soundSynth(CrondisSynths.WATER_PALM)
        player.setContainerWater(slot, 0)
        room.waterPalm(water)
        delay(1)
    }

    private companion object {
        const val FILL_RUN_ENERGY = 2_000

        const val DRINK_BELOW_ENERGY = 5_000
    }
}

private val Npc.aggressivePlayer by typePlayerUidVarn("varn.aggressive_player")
