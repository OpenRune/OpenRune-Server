package org.rsmod.content.areas.city.rellekka.neitiznot.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Honour_guard
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class HonourGuards : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.frisb_n_brg_grd_inside") { startDialogue(it.npc) { guard { inTown() } } }
        onOpNpc1("npc.frisb_n_brg_grd_troll") { startDialogue(it.npc) { guard { onWalls() } } }
        onOpNpc1("npc.frisb_n_brg_grd_outside") {
            startDialogue(it.npc) { guard { outsideTown() } }
        }
    }

    private suspend fun Dialogue.guard(standard: suspend Dialogue.() -> Unit) {
        if (QuestRequirements.isOnQuest(player, FREMENNIK_ISLES)) {
            chatPlayer(happy, "Good day!")
            chatNpc(neutral, "Can't talk now, I'm on duty.")
            return
        }
        standard()
    }

    // TODO(content): [verify:in_game] whether the jester lines need all four costume pieces worn
    private fun Dialogue.wearingJesterCostume(): Boolean =
        "obj.frisd_jester_hat" in player.worn &&
            "obj.frisd_jester_top" in player.worn &&
            "obj.frisd_jester_legs" in player.worn &&
            "obj.frisd_jester_boots" in player.worn

    private suspend fun Dialogue.inTown() {
        if (wearingJesterCostume()) {
            jesterJoke("HaHa!")
            return
        }
        chatNpc(happy, "Hail, ${fremennikName()}!")
        chatPlayer(happy, "Good day! How are you doing?")
        chatNpc(happy, "Pretty good. It's nice to have a day off from troll- killing duty.")
        chatPlayer(quiz, "Have you killed many trolls?")
        chatNpc(neutral, "Of course. It's all part of growing up around here. You get used to it.")
        chatPlayer(happy, "See you later.")
    }

    private suspend fun Dialogue.onWalls() {
        if (wearingJesterCostume()) {
            chatNpc(
                worried,
                "Hey jester, this area is not a suitable place for civilians to wander about in. " +
                    "Trolls everywhere.",
            )
            chatPlayer(neutral, "Right you are. I'll be careful.")
            return
        }
        chatNpc(happy, "Hail, ${fremennikName()}!")
        chatPlayer(happy, "Good day!")
        chatNpc(quiz, "Are you here to help with the trolls?")
        chatPlayer(neutral, "I'm afraid not, I have other things to do.")
        chatNpc(
            worried,
            "Well, watch yourself. Those rocks that the trolls throw can take your arm off.",
        )
    }

    private suspend fun Dialogue.outsideTown() {
        if (wearingJesterCostume()) {
            jesterJoke("Haha!")
            return
        }
        chatNpc(happy, "Hail, ${fremennikName()}!")
        chatPlayer(happy, "Good day!")
        chatNpc(
            worried,
            "I hope you're not heading north. There are lots of trolls, very dangerous.",
        )
        chatPlayer(happy, "Thanks for the warning. I'll keep my eyes peeled.")
    }

    private suspend fun Dialogue.jesterJoke(reply: String) {
        chatNpc(happy, "Good day, jester! Got any new jokes?")
        chatPlayer(
            neutral,
            "No. I only use old, recycled jokes. It's better for the environment.",
        )
        chatNpc(laugh, reply)
    }

    private companion object {
        const val FREMENNIK_ISLES = "quest_fremennikisles"
    }
}
