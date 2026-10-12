package org.rsmod.content.areas.city.varrock.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class VarrockGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.philop") { startDialogue(it.npc) { busyChild() } }
        onOpNpc1("npc.kanel") { startDialogue(it.npc) { busyChild() } }
        onOpNpc1("npc.daddyshome_daddy") { startDialogue(it.npc) { oldManYarlo() } }
        onOpNpc1("npc.dr_harlow") { startDialogue(it.npc) { drHarlow() } }
        onOpNpc1("npc.zep_assist_varr") { startDialogue(it.npc) { assistantSerf() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Philop
    // https://oldschool.runescape.wiki/w/Transcript:Kanel
    private suspend fun Dialogue.busyChild() {
        chatPlayer(happy, "Hello youngster!")
        mesbox("The child is too busy playing to take any notice.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Old_Man_Yarlo
    // TODO(content): [miniquest:daddys_home] after-miniquest dialogue
    private suspend fun Dialogue.oldManYarlo() {
        val title = if (access.isBodyTypeA()) "man" else "lady"
        chatNpc(neutral, "What brings you here, young $title?")
        if (choice2("I'm just wandering around.", 1, "Nothing, really.", 2) == 1) {
            chatPlayer(neutral, "I'm just wandering around.")
            chatNpc(
                neutral,
                "Oh, that's nice. I used to wander around, back when my legs were better. So " +
                    "long ago.",
            )
            return
        }
        chatPlayer(neutral, "Nothing, really.")
        chatNpc(
            sad,
            "That's the trouble with young people these days. Not my boy, though. He's a good " +
                "boy, always working. That's why he never visits. Run along now, young $title.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dr_Harlow
    private suspend fun Dialogue.drHarlow() {
        chatNpc(drunk, "Buy me a drink pleassh...")
        chatPlayer(neutral, "I think you've had enough.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Assistant_Serf
    // TODO(content): [system:balloon_transport] open the balloon map on "Yes."
    private suspend fun Dialogue.assistantSerf() {
        chatNpc(
            neutral,
            "Do you want to use the balloon? Just so you know, some locations require special " +
                "logs and high Firemaking skills.",
        )
        if (choice2("Yes.", 1, "Who are you?", 2) == 1) {
            return
        }
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            happy,
            "I am a serf. Assistant Serf to you! Auguste freed me and gave me this job.",
        )
        chatNpc(neutral, "Do you want to use the balloon?")
        if (choice2("Yes.", 1, "No.", 2) == 2) {
            chatPlayer(neutral, "No.")
        }
    }
}
