package org.rsmod.content.areas.city.ardougne.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class EastArdougneGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ardougne_prison_guard") { startDialogue(it.npc) { prisonKnight() } }
        onOpNpc1("npc.vc_bones") { startDialogue(it.npc) { bones() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Knight_of_Ardougne_(NPC) (upstairs)
    // TODO(content): [verify:in_game] which Ardougne knight armour pieces count as the disguise
    private suspend fun Dialogue.prisonKnight() {
        chatPlayer(happy, "Good day.")
        if (KNIGHT_ARMOUR.all { player.worn.count(it) > 0 }) {
            chatNpc(
                happy,
                "Hello there fellow knight. Isn't today a wonderful day to be a Knight of " +
                    "Ardougne?",
            )
            chatPlayer(
                neutral,
                "I mean it's okay I suppose. The constant fear of some random adventurer " +
                    "murdering you and the fact that people are constantly picking your pockets " +
                    "isn't great. But hey ho, at least the benefits are good.",
            )
            chatNpc(happy, "That they are fellow knight, that they are.")
            chatPlayer(quiz, "Anyway, can I go down these stairs?")
            chatNpc(
                happy,
                "Of course you can! You're a Knight of Ardougne! There is no place where you " +
                    "can't go.",
            )
            chatPlayer(neutral, "Yeah... Thanks.")
            return
        }
        chatNpc(neutral, "Hello there citizen. What can I do for you?")
        chatPlayer(quiz, "What's down these stairs?")
        chatNpc(
            neutral,
            "These stairs lead to the Ardougne Prison. It's home to thieves, murderers and " +
                "everything in between.",
        )
        chatPlayer(quiz, "Sounds lovely. Can I go in?")
        chatNpc(
            neutral,
            "I'm afraid not citizen. Only the Knights of Ardougne are allowed into the prison " +
                "and I can see that you are no knight.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bones_(kitten)
    // TODO(content): [quest:dragon_slayer_ii] the cat also talks to players who finished the quest
    private suspend fun Dialogue.bones() {
        chatPlayer(happy, "Who's a cute little kitty?")
        val catspeak =
            CATSPEAK_AMULETS.any { player.inv.count(it) > 0 || player.worn.count(it) > 0 }
        if (catspeak) {
            chatNpc(neutral, "Jimmy doesn't like me talking to strangers.")
        } else {
            chatNpc(neutral, "Miaow!")
        }
    }

    private companion object {
        val KNIGHT_ARMOUR =
            listOf(
                "obj.ardougne_knight_helm",
                "obj.ardougne_knight_body",
                "obj.ardougne_knight_legs",
            )
        val CATSPEAK_AMULETS =
            listOf("obj.ics_little_amulet_of_catspeak", "obj.twocats_amuletofcatspeak")
    }
}
