package org.rsmod.content.areas.misc.villages.barbarianoutpost.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class BarbarianOutpostTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.gunnjorn") { startDialogue(it.npc) { gunnjorn() } }
        onOpNpc1("npc.barbguard1") { startDialogue(it.npc) { barbarianGuard() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gunnjorn
    // TODO(content): [system:treasure_trails] hard clue puzzle box
    private suspend fun Dialogue.gunnjorn() {
        chatNpc(
            laugh,
            "Haha welcome to my obstacle course. Have fun, but remember this isn't a child's " +
                "playground. People have died here.",
        )
        chatNpc(
            neutral,
            "The best way to train, is to go round the course in a clockwise direction.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Barbarian_guard (before the Barcrawl)
    // TODO(content): [miniquest:alfred_grimhands_barcrawl] barcrawl card, progress checks, entry
    //  and vial smashing
    private suspend fun Dialogue.barbarianGuard() {
        chatNpc(angry, "Oi, whaddya want?")
        when (choice2("I want to come through this gate.", 1, "I want some money.", 2)) {
            1 -> {
                chatPlayer(neutral, "I want to come through this gate.")
                chatNpc(
                    neutral,
                    "Barbarians only. Are you a barbarian? You don't look like one.",
                )
                val option =
                    choice2(
                        "Hmm, yep you've got me there.",
                        1,
                        "Looks can be deceiving, I am in fact a barbarian.",
                        2,
                    )
                if (option == 1) {
                    chatPlayer(neutral, "Hmm, yep you've got me there.")
                    return
                }
                chatPlayer(happy, "Looks can be deceiving, I am in fact a barbarian.")
                chatNpc(
                    neutral,
                    "If you're a barbarian you need to be able to drink like one. We barbarians " +
                        "like a good drink.",
                )
                chatNpc(
                    happy,
                    "I have the perfect challenge for you... The Alfred Grimhand Barcrawl! First " +
                        "completed by Alfred Grimhand.",
                )
            }
            2 -> {
                chatPlayer(neutral, "I want some money.")
                chatNpc(angry, "Do I look like a bank to you?")
            }
        }
    }
}
