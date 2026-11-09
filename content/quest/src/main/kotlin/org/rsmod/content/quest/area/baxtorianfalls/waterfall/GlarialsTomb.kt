package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
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

class GlarialsTomb
@Inject
constructor(
    private val waterfall: WaterfallQuest,
    private val locRepo: LocRepository,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(TOMBSTONE) { readTombstone() }
        onOpLocU(TOMBSTONE, PEBBLE) { placePebble() }
        onOpLoc1(CHEST_CLOSED) { openChest(it.loc) }
        onOpLoc1(CHEST_OPEN) { searchChest() }
        onOpLoc2(CHEST_OPEN) { shutChest(it.loc) }
        onOpLoc1(COFFIN) { searchCoffin() }
    }

    private fun ProtectedAccess.readTombstone() {
        ifOpenMainModal(SCROLL)
        for (line in 1..SCROLL_LINES) {
            val text = TOMBSTONE_TEXT.getOrElse(line - TOMBSTONE_FIRST_LINE) { "" }
            ifSetText("$SCROLL_LINE$line", text)
        }
    }

    private suspend fun ProtectedAccess.placePebble() {
        arriveDelay()
        if (player.carriesUnpeacefulItem()) {
            mesbox("You place the pebble in the gravestone's small indent but nothing happens.")
            return
        }
        mesboxNp(TOMB_OPENS)
        fadeOut()
        delay(CLIMB_TICKS)
        telejump(WaterfallCoords.TOMB_ENTRY, TeleportType.Exempt)
        waterfall.advanceTo(this, ENTERED_TOMB)
        delay(1)
        fadeIn()
        mesbox(TOMB_OPENS)
        closeFadeOverlayNow()
    }

    private suspend fun ProtectedAccess.openChest(chest: BoundLocInfo) {
        arriveDelay()
        anim(CHEST_SEQ)
        soundSynth(CHEST_OPEN_SOUND)
        delay(1)
        locRepo.change(chest, CHEST_OPEN, CHEST_OPEN_TICKS)
        searchChest()
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
        if (invAdd(inv, AMULET).failure) {
            objbox(
                AMULET,
                "You find a small amulet in the chest but you don't have enough room to take it.",
            )
            return
        }
        objbox(AMULET, "You find a small amulet in the chest.")
    }

    private suspend fun ProtectedAccess.searchCoffin() {
        if (URN_FULL in player.inv) {
            mes("You search the tomb but find nothing.")
            return
        }
        if (invAdd(inv, URN_FULL).failure) {
            objbox(
                URN_FULL,
                "You find an urn full of ashes in the tomb but you don't have enough room to " +
                    "take it.",
            )
            return
        }
        objbox(URN_FULL, "You find an urn full of ashes in the tomb.")
    }

    private companion object {
        const val TOMBSTONE = "loc.glarials_tombstone_waterfall_quest"
        const val CHEST_CLOSED = "loc.glarials_chest_closed_waterfall_quest"
        const val CHEST_OPEN = "loc.glarials_chest_open_waterfall_quest"
        const val COFFIN = "loc.glarials_tomb_waterfall_quest"

        const val SCROLL = "interface.messagescroll2"
        const val SCROLL_LINE = "component.messagescroll2:ms"
        const val SCROLL_LINES = 12
        const val TOMBSTONE_FIRST_LINE = 5
        val TOMBSTONE_TEXT =
            listOf(
                "Here lies Glarial, wife of Baxtorian, true",
                "friend of nature in life and death. May she",
                "now rest knowing only visitors with peaceful",
                "intent can enter.",
            )
        const val TOMB_OPENS =
            "You place the pebble in the gravestone's small indent. The stone slab slides back " +
                "revealing a ladder. You climb down it."
        const val CLIMB_TICKS = 2
        const val CHEST_SEQ = "seq.human_openchest"
        const val CHEST_OPEN_SOUND = "synth.chest_open"
        const val CHEST_CLOSE_SOUND = "synth.chest_close"

        const val CHEST_OPEN_TICKS = 500
    }
}
