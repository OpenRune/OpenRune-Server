package org.rsmod.content.generic.npcs.cow

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.script.onOpContentLoc2
import org.rsmod.api.script.onOpContentLocU
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DairyCow : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc1("content.dairy_cow") { attemptMilkingCow(it.loc) }
        onOpContentLoc2("content.dairy_cow") { stealCowbell(it.loc) }
        onOpContentLocU("content.dairy_cow") { mes("The cow doesn't want that.") }
        onOpContentLocU("content.dairy_cow", "obj.bucket_empty") { attemptMilkingCow(it.loc) }

        onPlayerQueue("queue.milk_cow") { milkCow() }
    }

    private suspend fun ProtectedAccess.attemptMilkingCow(loc: BoundLocInfo) {
        arriveDelay()
        faceSquare(loc.coords)
        if ("obj.bucket_empty" !in inv) {
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
        val replace = invReplace(inv, "obj.bucket_empty", 1, "obj.bucket_milk")
        if (replace.failure) {
            return
        }
        spam("You milk the cow.")
        anim("seq.milkit")
        soundSynth("synth.milk_cow")
        weakQueue("queue.milk_cow", 8)
    }

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
        // TODO(content): Should be different actions based on cold war quest progress.
        arriveDelay()
        faceSquare(loc.coords)
        mesbox("You need to have started the Cold War quest to attempt this.")
    }

    private fun CoordGrid.isNearGillie(): Boolean {
        return this == CoordGrid(0, 50, 51, 54, 8) || this == CoordGrid(0, 50, 51, 52, 11)
    }
}
