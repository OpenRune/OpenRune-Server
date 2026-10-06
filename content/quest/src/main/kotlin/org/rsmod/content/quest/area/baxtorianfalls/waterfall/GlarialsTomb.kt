package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Amulet
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.EnteredTomb
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Pebble
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.UrnFull
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
        onOpLoc1(Tombstone) { readTombstone() }
        onOpLocU(Tombstone, Pebble) { placePebble() }
        onOpLoc1(ChestClosed) { openChest(it.loc) }
        onOpLoc1(ChestOpen) { searchChest() }
        onOpLoc2(ChestOpen) { shutChest(it.loc) }
        onOpLoc1(Coffin) { searchCoffin() }
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
        soundSynth(SlabSound)
        anim(ClimbSeq)
        delay(1)
        telejump(WaterfallCoords.TOMB_ENTRY, TeleportType.Exempt)
        waterfall.advanceTo(this, EnteredTomb)
    }

    private fun ProtectedAccess.openChest(chest: BoundLocInfo) {
        anim(ChestSeq)
        soundSynth(ChestOpenSound)
        locRepo.del(chest, ChestOpenTicks)
        locRepo.add(chest.coords, ChestOpen, ChestOpenTicks, chest.angle, chest.shape)
    }

    private fun ProtectedAccess.shutChest(chest: BoundLocInfo) {
        soundSynth(ChestCloseSound)
        locRepo.del(chest, ChestOpenTicks)
        locRepo.add(chest.coords, ChestClosed, ChestOpenTicks, chest.angle, chest.shape)
    }

    private suspend fun ProtectedAccess.searchChest() {
        if (player.hasAmulet()) {
            mes("You search the chest but find nothing.")
            return
        }
        invAddOrDrop(objRepo, Amulet)
        objbox(Amulet, "You find a small amulet in the chest.")
    }

    /** The search takes a while, and the tomb's guardians are free to attack meanwhile. */
    private suspend fun ProtectedAccess.searchCoffin() {
        anim(SearchSeq)
        delay(CoffinSearchTicks)
        if (UrnFull in player.inv) {
            mes("You search the tomb but find nothing.")
            return
        }
        invAddOrDrop(objRepo, UrnFull)
        objbox(UrnFull, "You find an urn full of ashes in the tomb.")
    }

    private companion object {
        const val Tombstone = "loc.glarials_tombstone_waterfall_quest"
        const val ChestClosed = "loc.glarials_chest_closed_waterfall_quest"
        const val ChestOpen = "loc.glarials_chest_open_waterfall_quest"
        const val Coffin = "loc.glarials_tomb_waterfall_quest"

        const val ClimbSeq = "seq.human_reachforladder"
        const val ChestSeq = "seq.human_openchest"
        const val SearchSeq = "seq.human_pickuptable"
        const val SlabSound = "synth.stone_door"
        const val ChestOpenSound = "synth.chest_open"
        const val ChestCloseSound = "synth.chest_close"

        const val ChestOpenTicks = 100
        const val CoffinSearchTicks = 3
    }
}
