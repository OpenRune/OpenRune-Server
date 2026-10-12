package org.rsmod.content.generic.npcs.cow

import jakarta.inject.Inject
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.obj.charges.ObjChargeManager.Companion.isFailure
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.script.onOpContentLoc2
import org.rsmod.api.script.onOpContentLocU
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DairyCow @Inject constructor(private val charges: ObjChargeManager) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc1("content.dairy_cow") { attemptMilkingCow(it.loc) }
        onOpContentLoc2("content.dairy_cow") { stealCowbell(it.loc) }
        onOpContentLocU("content.dairy_cow") { mes("The cow doesn't want that.") }
        onOpContentLocU("content.dairy_cow", "obj.bucket_empty") { attemptMilkingCow(it.loc) }

        // The Zanaris dairy cow puts Milk second and Steal-cowbell fourth
        // (https://oldschool.runescape.wiki/w/Dairy_cow).
        // TODO(content): its Talk-to option.
        onOpLoc2(ZANARIS_COW) { attemptMilkingCow(it.loc) }
        onOpLoc4(ZANARIS_COW) { stealCowbell(it.loc) }
        onOpLocU(ZANARIS_COW) { mes("The cow doesn't want that.") }
        onOpLocU(ZANARIS_COW, "obj.bucket_empty") { attemptMilkingCow(it.loc) }

        onPlayerQueue("queue.milk_cow") { milkCow() }
    }

    private suspend fun ProtectedAccess.attemptMilkingCow(loc: BoundLocInfo) {
        arriveDelay()
        faceSquare(loc.coords)
        val bottomless = bottomlessSlots()
        if (bottomless.isNotEmpty() && bottomless.none { it in roomySlots() } && "obj.bucket_empty" !in inv) {
            mes(BOTTOMLESS_FULL)
            return
        }
        if ("obj.bucket_empty" !in inv && bottomless.isEmpty()) {
            if (loc.coords.isNearGillie()) {
                startDialogue { noBucket() }
            } else {
                mes("You'll need an empty bucket to collect the milk.")
            }
            return
        }
        weakQueue("queue.milk_cow", 2)
    }

    private fun ProtectedAccess.milkCow() {
        // Empty buckets fill first, then a bottomless milk bucket, all from the one click (live
        // capture, rev 241, 20261012T233700-df49f48).
        val bottomless = if ("obj.bucket_empty" in inv) null else roomySlots().firstOrNull()
        if (bottomless != null) {
            if (charges.addCharges(inv, bottomless, 1, BOTTOMLESS_USES, BOTTOMLESS_MAX).isFailure()) {
                return
            }
        } else if (invReplace(inv, "obj.bucket_empty", 1, "obj.bucket_milk").failure) {
            return
        }
        spam("You milk the cow.")
        anim("seq.milkit")
        soundSynth("synth.milk_cow")
        if (bottomless != null && bottomless !in roomySlots()) {
            mes(BOTTOMLESS_FULL)
            return
        }
        weakQueue("queue.milk_cow", if (wearsCowbellAmulet()) COWBELL_MILK_DELAY else MILK_DELAY)
    }

    private fun ProtectedAccess.bottomlessSlots(): List<Int> =
        inv.indices.filter { inv[it]?.let { obj -> obj.isType(BOTTOMLESS_EMPTY) || obj.isType(BOTTOMLESS_FILLED) } == true }

    /** Bottomless milk buckets with room for more regular milk. */
    private fun ProtectedAccess.roomySlots(): List<Int> =
        bottomlessSlots().filter { charges.getCharges(inv[it], BOTTOMLESS_USES) < BOTTOMLESS_MAX }

    // https://oldschool.runescape.wiki/w/Cowbell_amulet: one tick faster while worn.
    private fun ProtectedAccess.wearsCowbellAmulet(): Boolean =
        "obj.cowbell_amulet" in player.worn || "obj.cowbell_amulet_empty" in player.worn

    // https://oldschool.runescape.wiki/w/Transcript:Gillie_Groats
    private suspend fun Dialogue.noBucket() {
        chatNpcSpecific(
            "Gillie Groats the Milkmaid",
            "npc.gillie_the_milkmaid",
            laugh,
            "Tee hee! You've never milked a cow before, have you?",
        )
        chatPlayer(quiz, "Erm... No. How could you tell?")
        chatNpcSpecific(
            "Gillie Groats the Milkmaid",
            "npc.gillie_the_milkmaid",
            laugh,
            "Because you're spilling milk all over the floor. What a " +
                "waste! You need something to hold the milk.",
        )
        chatPlayer(neutral, "Ah yes, I really should have guessed that one, shouldn't I?")
        chatNpcSpecific(
            "Gillie Groats the Milkmaid",
            "npc.gillie_the_milkmaid",
            laugh,
            "You're from the city, aren't you... Try it again with an empty bucket.",
        )
        chatPlayer(neutral, "Right, I'll do that.")
    }

    private suspend fun ProtectedAccess.stealCowbell(loc: BoundLocInfo) {
        // TODO(content): [quest:cold_war] Stealing the cowbell depends on Cold War progress.
        arriveDelay()
        faceSquare(loc.coords)
        mesbox("You need to have started the Cold War quest to attempt this.")
    }

    private fun CoordGrid.isNearGillie(): Boolean {
        return this == CoordGrid(0, 50, 51, 54, 8) || this == CoordGrid(0, 50, 51, 52, 11)
    }

    private companion object {
        const val ZANARIS_COW = "loc.fairy_fat_cow"

        // A milking every 7 ticks (live capture, rev 241, 20261012T222638-df49f48@1720-1778), 6
        // with the cowbell amulet worn (https://oldschool.runescape.wiki/w/Cowbell_amulet). The
        // queue runs a tick before its delay is up.
        const val MILK_DELAY = 8
        const val COWBELL_MILK_DELAY = 7

        const val BOTTOMLESS_EMPTY = BottomlessMilkBucket.EMPTY
        const val BOTTOMLESS_FILLED = BottomlessMilkBucket.FILLED
        const val BOTTOMLESS_USES = BottomlessMilkBucket.USES
        const val BOTTOMLESS_MAX = BottomlessMilkBucket.MAX_USES

        // https://oldschool.runescape.wiki/w/Bottomless_milk_bucket#Trivia
        const val BOTTOMLESS_FULL = "Your bottomless milk bucket, surprisingly, is full."
    }
}
