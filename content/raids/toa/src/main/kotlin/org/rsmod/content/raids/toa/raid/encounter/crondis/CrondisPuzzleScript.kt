package org.rsmod.content.raids.toa.raid.encounter.crondis

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
 * (fill), WaterContainerAction (check, empty) and CrondisPuzzleEncounter.handleWaterfall /
 * waterPalm. Every handler first finds the player's room, so nothing here works outside it.
 */
class CrondisPuzzleScript @Inject constructor(private val playerList: PlayerList) : PluginScript() {

    override fun ScriptContext.startup() {
        val containerType = ServerCacheManager.getItem(CrondisPuzzleEncounter.CONTAINER.asRSCM(RSCMType.OBJ))!!
        onOpObj3(containerType) { takeContainer() }

        // The waterfall's own op ("Fill") and using the container on it do the same.
        onOpLoc1(CrondisPuzzleEncounter.WATER_SOURCE) { fill(it.loc) }
        onOpLocU(CrondisPuzzleEncounter.WATER_SOURCE, CrondisPuzzleEncounter.CONTAINER) { fill(it.loc) }
        onOpLocU(CrondisPuzzleEncounter.WATER_SOURCE_EMPTY, CrondisPuzzleEncounter.CONTAINER) {
            fillFromEmpty()
        }

        onOpHeld1(CrondisPuzzleEncounter.CONTAINER) { checkContainer(it.obj) }
        onOpHeld2(CrondisPuzzleEncounter.CONTAINER) { emptyContainer() }

        for (palm in CrondisPuzzleEncounter.WATERABLE_PALMS) {
            onOpNpc1(palm) { waterPalm() }
        }

        // Every crocodile runs the room's AI once a tick (armed with aiTimer(1) at spawn).
        onAiTimer(CrondisPuzzleEncounter.CROCODILE) { CrondisPuzzleEncounter.crocodileTick(npc) }

        val crocodileType = ServerCacheManager.getNpc(CrondisPuzzleEncounter.CROCODILE.asRSCM(RSCMType.NPC))!!
        // Its own attack. Binding this for the type replaces the default npc combat for it.
        onAiOpPlayer2(crocodileType) { CrondisPuzzleEncounter.crocodileAttack(npc, it.target) }
        // Remember who hits it: players without a container who did are its third priority.
        onNpcHit(crocodileType) {
            if (hit.isFromPlayer) {
                hit.resolvePlayerSource(playerList)?.let { CrondisPuzzleEncounter.crocodileHitBy(npc, it) }
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
        if (inv.contains(CrondisPuzzleEncounter.CONTAINER)) {
            mes("You already have a container.")
            return
        }
        if (inv.freeSpace() == 0) {
            mes("You do not have enough space to pick this up.")
            return
        }
        // The String constructor: InvObj's Int one is @UncheckedType (opt-in only).
        if (invAdd(inv, InvObj(CrondisPuzzleEncounter.CONTAINER, 1, vars = 0)).failure) return
        soundSynth(SYNTH_TAKE)
        anim(SEQ_PICKUP)
    }

    private fun ProtectedAccess.checkContainer(obj: InvObj) {
        mes("Your water container is at ${obj.vars}% capacity.")
    }

    private suspend fun ProtectedAccess.emptyContainer() {
        val slot = CrondisPuzzleEncounter.containerSlot(player) ?: return
        if ((inv[slot]?.vars ?: 0) <= 0) {
            mes("It's already empty.")
            return
        }
        val empty =
            choice2("Yes, empty water container.", true, "No.", false, title = "Empty water container")
        if (!empty) return
        // Re-find the slot: the inventory may have changed while the dialog was open.
        val current = CrondisPuzzleEncounter.containerSlot(player) ?: return
        setCharges(current, 0)
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
        val slot = CrondisPuzzleEncounter.containerSlot(player)
        if (slot == null) {
            mes("You don't have anything to fill.")
            return
        }
        if ((inv[slot]?.vars ?: 0) >= CrondisPuzzleEncounter.CONTAINER_FULL) {
            mes("Your container is full.")
            return
        }
        mes("You fill your container.")
        soundSynth(SYNTH_FILL)
        anim(SEQ_PICKUP)
        setCharges(slot, CrondisPuzzleEncounter.CONTAINER_FULL)
        room.drainWaterfall(waterfall)
        // Offline_Scape: +20% run energy. OpenRune stores run energy as 0..10_000.
        player.runEnergy = (player.runEnergy + FILL_RUN_ENERGY).coerceAtMost(Constants.run_max_energy)
        delay(1) // Offline_Scape lock(1)
    }

    private suspend fun ProtectedAccess.fillFromEmpty() {
        arriveDelay()
        if (room == null) return
        mes("It's empty.")
        soundSynth(SYNTH_DECLINE)
    }

    // ---- The palm ----

    /** Offline_Scape waterPalm: all of the container goes onto the palm. */
    private suspend fun ProtectedAccess.waterPalm() {
        val room = room ?: return
        val slot = CrondisPuzzleEncounter.containerSlot(player)
        val charges = slot?.let { inv[it]?.vars } ?: 0
        if (slot == null || charges < 1) {
            mes("You have nothing to water the palm with.")
            return
        }
        val amount =
            when {
                charges <= 25 -> "small amount"
                charges <= 50 -> "reasonable amount"
                else -> "lot"
            }
        mes("You empty a $amount of water onto the palm.")
        anim(SEQ_PICKUP)
        soundSynth(SYNTH_WATER_PALM)
        setCharges(slot, 0)
        room.waterPalm(charges)
        delay(1) // Offline_Scape lock(1)
    }

    // ---- Helpers ----

    /**
     * The container's fill level lives in its obj vars (0..100). Rebuilt by name with the String
     * constructor, since the slot is known to hold a container.
     */
    private fun ProtectedAccess.setCharges(slot: Int, charges: Int) {
        val obj = inv[slot] ?: return
        inv[slot] = InvObj(CrondisPuzzleEncounter.CONTAINER, obj.count, vars = charges)
    }

    private companion object {
        const val SEQ_PICKUP = "seq.human_pickupfloor" // Offline_Scape anim 827

        /** 20% of Constants.run_max_energy. */
        const val FILL_RUN_ENERGY = 2_000

        /** Offline_Scape ITEM_TAKE_SOUND 2582, which does have a gameval. */
        const val SYNTH_TAKE = "synth.pick2"

        // The rest have no gameval name (the cache calls them synth_6522 etc.), so the Int
        // overload of soundSynth plays them by id. Named after the labels in the cache's
        // sound-list dbtable synth_pathofcrondis (osrs-dumps config/dump.dbrow).

        /** toa_crondis_fill_container */
        const val SYNTH_FILL = 6522

        /** toa_crondis_water_empty */
        const val SYNTH_DECLINE = 6524

        /** toa_crondis_water_tree_02 */
        const val SYNTH_WATER_PALM = 6534
    }
}
