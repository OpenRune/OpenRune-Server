package org.rsmod.content.areas.city.taverley.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class TaverleyTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ballboy") { startDialogue(it.npc) { boy() } }
        onOpNpc1("npc.eadgar_druid_washing") { startDialogue(it.npc) { tegid() } }
        onOpNpc1("npc.ladyofthelake") { startDialogue(it.npc) { ladyOfTheLake() } }
        onOpNpc1("npc.grim_sylas") { startDialogue(it.npc) { sylas() } }
        onOpNpc1("npc.zep_assist_tav") { startDialogue(it.npc) { assistantStan() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Boy (before Witch's House)
    // TODO(content): [quest:witchs_house] "Start the Witch's House quest?" offer, start and
    //  after-quest line
    private suspend fun Dialogue.boy() {
        chatPlayer(happy, "Hello young man.")
        mesbox("The boy sobs.")
        val option =
            choice2(
                "What's the matter?",
                1,
                "Well if you're not going to answer, I'll go.",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What's the matter?")
                chatNpc(
                    sad,
                    "I've kicked my ball over that hedge, into that garden! The old lady who " +
                        "lives there is scary... She's locked the ball in her wooden shed! Can " +
                        "you get my ball back for me please?",
                )
            }
            2 -> {
                chatPlayer(neutral, "Well if you're not going to answer, I'll go.")
                mesbox("The boy sniffs slightly.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tegid
    private suspend fun Dialogue.tegid() {
        chatPlayer(happy, "So, you're doing laundry, eh?")
        chatNpc(angry, "Yes. What is it to you?")
        chatPlayer(happy, "Nice day for it.")
        chatNpc(neutral, "I suppose it is.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Lady_of_the_Lake
    // TODO(content): [quest:merlins_crystal] "I seek the sword Excalibur." for 500 coins
    // TODO(content): [system:treasure_trails] easy clue reward
    private suspend fun Dialogue.ladyOfTheLake() {
        val title = if (access.isBodyTypeA()) "sir" else "madam"
        chatNpc(neutral, "Good day to you $title.")
        when (choice2("Who are you?", 1, "Good day.", 2)) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(neutral, "I am the Lady of the Lake.")
            }
            2 -> chatPlayer(neutral, "Good day.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Grim_Tales (without the requirements)
    // TODO(content): [quest:grim_tales] quest start, in-progress and after-quest dialogue
    private suspend fun Dialogue.sylas() {
        chatPlayer(happy, "Hello there. My name's ${player.displayName}.")
        chatNpc(
            neutral,
            "Ahhh, good day young adventurer. I can't stop to talk right now I'm afraid.",
        )
        mesbox("You do not meet all of the requirements to start the Grim Tales quest.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Assistant_Stan
    // TODO(content): [system:balloon_transport] "Yes." opens the balloon map (log checks and
    //  the Entrana weapon search)
    private suspend fun Dialogue.assistantStan() {
        chatNpc(
            neutral,
            "Do you want to use the balloon? Just so you know, some locations require special " +
                "logs and high Firemaking skills.",
        )
        when (choice3("Yes.", 1, "No.", 2, "Who are you?", 3)) {
            1 -> Unit
            2 -> chatPlayer(neutral, "No.")
            3 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    neutral,
                    "I am Stan. Auguste hired me to look after this balloon. I make sure people " +
                        "are prepared to fly.",
                )
                chatNpc(neutral, "Do you want to use the balloon?")
                when (choice2("Yes.", 1, "No.", 2)) {
                    1 -> Unit
                    2 -> chatPlayer(neutral, "No.")
                }
            }
        }
    }
}
