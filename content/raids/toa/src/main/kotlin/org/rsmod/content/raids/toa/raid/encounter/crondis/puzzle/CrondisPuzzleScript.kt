package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.script.onOpObj3
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Crondis puzzle's ops. Ports Offline_Scape ContainerFloorItem (take), ContainerOnWaterfall
 * (fill), WaterContainerAction (check, empty), PalmTreeAction (water) and
 * CrondisPuzzleEncounter.handleWaterfall / waterPalm. Every handler first finds the player's
 * room, so nothing here works outside it.
 */
class CrondisPuzzleScript @Inject constructor(private val playerList: PlayerList) : PluginScript() {

    override fun ScriptContext.startup() {
        val containerType = ServerCacheManager.getItem(CrondisObjs.CONTAINER.asRSCM(RSCMType.OBJ))!!
        onOpObj3(containerType) { takeContainer() }

        // The waterfall's own op ("Fill") and using the container on it do the same.
        onOpLoc1(CrondisLocs.WATER_SOURCE) { fill(it.loc) }
        onOpLocU(CrondisLocs.WATER_SOURCE, CrondisObjs.CONTAINER) { fill(it.loc) }
        onOpLocU(CrondisLocs.WATER_SOURCE_EMPTY, CrondisObjs.CONTAINER) { fillFromEmpty() }

        onOpHeld1(CrondisObjs.CONTAINER) { checkContainer(it.obj) }
        onOpHeld2(CrondisObjs.CONTAINER) { emptyContainer() }

        // Op1 "Water" and using the container on the palm do the same.
        for (palm in CrondisNpcs.WATERABLE_PALMS) {
            val palmType = ServerCacheManager.getNpc(palm.asRSCM(RSCMType.NPC))!!
            onOpNpc1(palm) { waterPalm() }
            onOpNpcU(palmType, containerType) { waterPalm() }
        }

        // Every crocodile runs the room's AI once a tick (timer = 1 in toa_crondis.toml).
        onAiTimer(CrondisNpcs.CROCODILE) { CrondisPuzzleEncounter.onCrocodileTick(npc) }

        val crocodileType = ServerCacheManager.getNpc(CrondisNpcs.CROCODILE.asRSCM(RSCMType.NPC))!!
        // Its own attack. Binding this for the type replaces the default npc combat for it.
        onAiOpPlayer2(crocodileType) { CrondisPuzzleEncounter.onCrocodileAttack(npc, it.target) }
        // Remember who hits it: players without a container who did are its third priority.
        onNpcHit(crocodileType) {
            if (hit.isFromPlayer) {
                val player = hit.resolvePlayerSource(playerList) ?: return@onNpcHit
                CrondisPuzzleEncounter.onCrocodileHit(npc, player)
            }
        }
    }

    private val ProtectedAccess.room: CrondisPuzzleEncounter?
        get() = player.currentRaid?.encounterOf(player) as? CrondisPuzzleEncounter

    // ---- Containers ----

    /**
     * Offline_Scape ContainerFloorItem: the floor container stays where it is (overrideTake); you
     * get your own, empty, one.
     */
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
        // The String constructor: InvObj's Int one is @UncheckedType (opt-in only).
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
            choice2("Yes, empty water container.", true, "No.", false, title = "Empty water container")
        if (!empty) return
        // Re-find the slot: the inventory may have changed while the dialog was open.
        val slot = player.containerSlot() ?: return
        player.setContainerWater(slot, 0)
        // Offline_Scape said "Your empty your water container."
        mes("You empty your water container.")
    }

    // ---- Waterfalls ----

    /** Offline_Scape handleWaterfall, for a waterfall that still has water. */
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
        // Offline_Scape: +20% run energy. OpenRune stores run energy as 0..10_000.
        player.runEnergy = (player.runEnergy + FILL_RUN_ENERGY).coerceAtMost(Constants.run_max_energy)
        delay(1) // Offline_Scape lock(1)
    }

    private suspend fun ProtectedAccess.fillFromEmpty() {
        arriveDelay()
        if (room == null) return
        mes("It's empty.")
        soundSynth(CrondisSynths.DECLINE)
    }

    // ---- The palm ----

    /** Offline_Scape waterPalm: all of the container goes onto the palm. */
    private suspend fun ProtectedAccess.waterPalm() {
        val room = room ?: return
        val slot = player.containerSlot()
        val water = slot?.let { inv[it]?.vars } ?: 0
        if (slot == null || water < 1) {
            mes("You have nothing to water the palm with.")
            return
        }
        val amount =
            when {
                water <= 25 -> "small amount"
                water <= 50 -> "reasonable amount"
                else -> "lot"
            }
        mes("You empty a $amount of water onto the palm.")
        anim(CrondisSeqs.PICKUP)
        soundSynth(CrondisSynths.WATER_PALM)
        player.setContainerWater(slot, 0)
        room.waterPalm(water)
        delay(1) // Offline_Scape lock(1)
    }

    private companion object {
        /** 20% of Constants.run_max_energy. */
        const val FILL_RUN_ENERGY = 2_000
    }
}
