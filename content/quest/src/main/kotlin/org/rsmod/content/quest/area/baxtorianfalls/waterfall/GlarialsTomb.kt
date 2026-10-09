package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.AMULET
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.ENTERED_TOMB
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.PEBBLE
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.URN_FULL
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Glarial's tombstone on the hill north-west of the Fishing Guild and the tomb beneath it. The
 * way out is the ladder at 2556,9844, which the generic passage script already climbs.
 */
class GlarialsTomb
@Inject
constructor(
    private val waterfall: WaterfallQuest,
    private val locRepo: LocRepository,
    private val objRepo: ObjRepository,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(TOMBSTONE) { readTombstone() }
        onOpLocU(TOMBSTONE, PEBBLE) { placePebble() }
        onOpLoc1(CHEST_CLOSED) { openChest(it.loc) }
        onOpLoc1(CHEST_OPEN) { searchChest() }
        onOpLoc2(CHEST_OPEN) { shutChest(it.loc) }
        onOpLoc1(COFFIN) { searchCoffin() }
    }

    private suspend fun ProtectedAccess.readTombstone() {
        mesbox(
            "Here lies Glarial, wife of Baxtorian, true<br>friend of nature in life and death. " +
                "May she<br>now rest knowing only visitors with peaceful<br>intent can enter."
        )
    }

    private suspend fun ProtectedAccess.placePebble() {
        if (player.carriesUnpeacefulItem()) {
            mesbox("You place the pebble in the gravestone's small indent but nothing happens.")
            return
        }
        mesbox(
            "You place the pebble in the gravestone's small indent. The stone slab slides back " +
                "revealing a ladder. You climb down it."
        )
        soundSynth(SLAB_SOUND)
        anim(CLIMB_SEQ)
        delay(1)
        telejump(WaterfallCoords.TOMB_ENTRY, TeleportType.Exempt)
        waterfall.advanceTo(this, ENTERED_TOMB)
    }

    private fun ProtectedAccess.openChest(chest: BoundLocInfo) {
        anim(CHEST_SEQ)
        soundSynth(CHEST_OPEN_SOUND)
        locRepo.del(chest, CHEST_OPEN_TICKS)
        locRepo.add(chest.coords, CHEST_OPEN, CHEST_OPEN_TICKS, chest.angle, chest.shape)
    }

    private fun ProtectedAccess.shutChest(chest: BoundLocInfo) {
        soundSynth(CHEST_CLOSE_SOUND)
        locRepo.del(chest, CHEST_OPEN_TICKS)
        locRepo.add(chest.coords, CHEST_CLOSED, CHEST_OPEN_TICKS, chest.angle, chest.shape)
    }

    private suspend fun ProtectedAccess.searchChest() {
        if (player.hasAmulet()) {
            mes("You search the chest but find nothing.")
            return
        }
        invAddOrDrop(objRepo, AMULET)
        objbox(AMULET, "You find a small amulet in the chest.")
    }

    /** The search takes a while, and the tomb's guardians are free to attack meanwhile. */
    private suspend fun ProtectedAccess.searchCoffin() {
        anim(SEARCH_SEQ)
        delay(COFFIN_SEARCH_TICKS)
        if (URN_FULL in player.inv) {
            mes("You search the tomb but find nothing.")
            return
        }
        invAddOrDrop(objRepo, URN_FULL)
        objbox(URN_FULL, "You find an urn full of ashes in the tomb.")
    }

    private companion object {
        const val TOMBSTONE = "loc.glarials_tombstone_waterfall_quest"
        const val CHEST_CLOSED = "loc.glarials_chest_closed_waterfall_quest"
        const val CHEST_OPEN = "loc.glarials_chest_open_waterfall_quest"
        const val COFFIN = "loc.glarials_tomb_waterfall_quest"

        const val CLIMB_SEQ = "seq.human_reachforladder"
        const val CHEST_SEQ = "seq.human_openchest"
        const val SEARCH_SEQ = "seq.human_pickuptable"
        const val SLAB_SOUND = "synth.stone_door"
        const val CHEST_OPEN_SOUND = "synth.chest_open"
        const val CHEST_CLOSE_SOUND = "synth.chest_close"

        const val CHEST_OPEN_TICKS = 100
        const val COFFIN_SEARCH_TICKS = 3
    }
}
