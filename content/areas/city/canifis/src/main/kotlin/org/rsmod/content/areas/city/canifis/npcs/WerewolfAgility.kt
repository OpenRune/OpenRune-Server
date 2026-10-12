package org.rsmod.content.areas.city.canifis.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [skill:agility] Werewolf Agility Course, Give-Stick and overheads
// TODO(content): [skill:agility] Werewolf Skullball (the ball, the goals and the overheads)
class WerewolfAgility : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.werewolf_trainer_start") { startDialogue(it.npc) { agilityBoss() } }
        onOpNpc1("npc.werewolf_trainer_2") { startDialogue(it.npc) { agilityTrainer() } }
        onOpNpc1("npc.werewolf_skullballboss") { startDialogue(it.npc) { skullballBoss() } }
        onOpNpc1("npc.werewolf_trainer_1") { startDialogue(it.npc) { skullballTrainer() } }
    }

    private suspend fun Dialogue.notAWerewolf(): Boolean {
        if (player.wearsRingOfCharos()) {
            return false
        }
        chatNpc(angry, "Grrr - you don't belong in here, human!")
        return true
    }

    // https://oldschool.runescape.wiki/w/Transcript:Agility_Boss
    // TODO(content): [verify:in_game] the wiki is missing the below 60 Agility dialogue
    private suspend fun Dialogue.agilityBoss() {
        if (notAWerewolf()) {
            return
        }
        chatPlayer(quiz, "How do I use the agility course?")
        chatNpc(
            neutral,
            "I'll throw you a stick, which you need to fetch as quickly as possible, from the " +
                "area beyond the pipes.",
        )
        chatNpc(
            neutral,
            "Be wary of the deathslide - you must hang by your teeth, and if your strength is " +
                "not up to the job you will fall into a pit of spikes. Also, I would advise not " +
                "carrying too much extra weight.",
        )
        chatNpc(
            neutral,
            "Bring the stick back to the werewolf waiting at the end of the death slide to get " +
                "your agility bonus.",
        )
        chatNpc(neutral, "I will throw your stick as soon as you jump onto the first stone.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Agility_Trainer
    private suspend fun Dialogue.agilityTrainer() {
        if (notAWerewolf()) {
            return
        }
        chatNpc(quiz, "Have you brought the stick yet?")
        chatPlayer(confused, "What stick?")
        chatNpc(angry, "Come on, get round that course - I need something to chew!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Skullball_Boss
    // TODO(content): [verify:in_game] the wiki is missing the below 25 Agility dialogue
    private suspend fun Dialogue.skullballBoss() {
        if (notAWerewolf()) {
            return
        }
        val option =
            choice2(
                "I would like to do the skullball course.",
                1,
                "What are the instructions for the skullball course?",
                2,
            )
        when (option) {
            1 -> chatPlayer(happy, "I think I'll give the Skullball Course a go.")
            2 -> {
                chatPlayer(quiz, "What are the instructions for using the skullball course?")
                chatNpc(
                    neutral,
                    "The skullball comes out of one of these four spawnholes. Just kick the ball " +
                        "through the middle of each goal, through the skeleton's feet.",
                )
                chatNpc(
                    neutral,
                    "There are 10 goals, which you must complete in order, and one final goal.",
                )
                chatNpc(
                    neutral,
                    "An arrow will point to your ball, just in case lots of people are using the " +
                        "course at the same time as yourself.",
                )
                chatNpc(
                    neutral,
                    "The better your time, the more agillity XP you will be awarded. The timer " +
                        "starts when you score your first goal.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Skullball_Trainer
    private suspend fun Dialogue.skullballTrainer() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(neutral, "This is the Skullball Course.")
        chatNpc(
            neutral,
            "Go and talk to the boss at the beginning of the course if you'd like a go.",
        )
    }
}
