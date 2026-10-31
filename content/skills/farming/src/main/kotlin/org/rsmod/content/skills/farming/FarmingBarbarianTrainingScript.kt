package org.rsmod.content.skills.farming

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingBarbarianTrainingScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.brut_otto") {
            startDialogue(it.npc) { talk() }
        }
    }

    private suspend fun Dialogue.talk() {
        chatNpc(neutral, "Good day to you, hearty warrior! May the spirits guide you.")
        // shortcut: barehand trial odds are unknown; add its progression only when sourced.
        if (vars[PLANTING] != 3) {
            mesbox("Barbarian pot-smashing requires completed barehanded planting training. " +
                "Barehanded planting training is not available yet.")
            return
        }
        while (true) {
            if (vars[SMASHING] == 3) {
                val enabled = vars[SMASH_POTS] != 1
                val option = if (!enabled) {
                    "I'd like to be calmer and not smash my pots."
                } else {
                    "I'd like to smash my pots when planting trees again."
                }
                if (!choice2(option, true, LEAVE, false)) return
                chatPlayer(neutral, option)
                if (vars[SMASHING] != 3) return
                VarPlayerIntMapSetter.set(player, SMASH_POTS, if (enabled) 1 else 0)
                chatNpc(neutral, "Consider it done.")
            } else {
                if (!choice2("Let's talk about my training.", true, LEAVE, false)) return
                chatPlayer(neutral, "Let's talk about my training.")
                if (access.statBase("stat.farming") < 15) {
                    mesbox("You need a Farming level of 15 to learn barbarian pot-smashing.")
                    return
                }
                val option = if (vars[SMASHING] == 2) {
                    "I've smashed a pot whilst farming!"
                } else {
                    "Are there any other fist related farming activities I can learn?"
                }
                if (!choice2(option, true, LEAVE, false)) return
                when (vars[SMASHING]) {
                    0 -> instruct()
                    1 -> {
                        chatPlayer(quiz, option)
                        chatNpc(neutral, "You have not yet attempted to plant a tree. Why not?")
                        chatPlayer(neutral, "I've been busy doing other stuff.")
                        chatNpc(neutral, "You know your task. Come back when you have completed it.")
                    }
                    2 -> reportBack()
                }
            }
        }
    }

    private suspend fun Dialogue.instruct() {
        chatPlayer(quiz, "Are there any other fist related farming activities I can learn?")
        chatNpc(neutral, "This technique fully embraces your strength as a barbarian. " +
            "Nuance is not required here. It's about the fine art of smashing pots.")
        chatPlayer(quiz, "Smashing pots? That doesn't sound overly difficult.")
        chatNpc(neutral, "The secret is how to smash them in a way that doesn't leave debris " +
            "on the ground, for that would not be of benefit to the environment.")
        chatPlayer(neutral, "That makes sense. I wouldn't want to be littering shards of pot " +
            "everywhere. What's the trick?")
        chatNpc(neutral, "Planting a tree is strenuous work. You need to channel this energy " +
            "into pure anger. Release all that anger on the pot that contained it, " +
            "shattering it into dust!")
        chatPlayer(neutral, "I'm good at getting angry. I'll give it a try.")
        if (vars[PLANTING] != 3 || vars[SMASHING] != 0 || access.statBase("stat.farming") < 15) return
        VarPlayerIntMapSetter.set(player, SMASHING, 1)
        chatNpc(neutral, "May the spirits guide you into success.")
    }

    private suspend fun Dialogue.reportBack() {
        chatPlayer(happy, "I've smashed a pot whilst farming!")
        chatNpc(neutral, "I can see in your spirit that you have been successful in your latest endeavour.")
        chatPlayer(shocked, "I planted a tree and got really angry. The pot was gone before I even realised.")
        if (vars[PLANTING] != 3 || vars[SMASHING] != 2) return
        VarPlayerIntMapSetter.set(player, SMASHING, 3)
        chatNpc(quiz, "It will become more natural with practice.")
    }

    private companion object {
        const val PLANTING = "varbit.brut_farming_planting"
        const val SMASHING = "varbit.brut_farming_smashing"
        const val SMASH_POTS = "varbit.brut_smash_pots_automatically"
        const val LEAVE = "I'd better get going."
    }
}
