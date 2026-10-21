package org.rsmod.content.other.treasuretrails

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.other.treasuretrails.ClueWatson.give
import org.rsmod.content.other.treasuretrails.ClueWatson.redeem
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WatsonScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.trail_watson_pre_talk") { talk(it.npc) }
        onOpNpc1("npc.trail_watson_post_talk") { talk(it.npc) }
        onOpNpc3("npc.trail_watson_post_talk") { check(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        val first = player.vars[SPOKEN] == 0
        startDialogue(npc) {
            if (first) {
                chatNpc(neutral, "Hello there! I'm Watson, an expert in all things clue scrolls.")
                chatNpc(
                    neutral,
                    "Bring me one easy, medium, hard and elite clue scroll and I'll trade you " +
                        "a master clue scroll for them.",
                )
            } else {
                chatNpc(neutral, "Hello again, ${player.displayName}. Got any clues for me?")
            }
        }
        if (first) VarPlayerIntMapSetter.set(player, SPOKEN, 1)
        var choice = Choice.Leave
        startDialogue(npc) {
            choice =
                choice3(
                    "I have some clues for you.",
                    Choice.Give,
                    "Which clues have I given you?",
                    Choice.Check,
                    "Never mind.",
                    Choice.Leave,
                )
        }
        when (choice) {
            Choice.Give -> giveClues(npc)
            Choice.Check -> check(npc)
            else -> Unit
        }
    }

    private suspend fun ProtectedAccess.giveClues(npc: Npc) {
        val given = ClueWatson.tiers().count { give(it) }
        if (given == 0) {
            startDialogue(npc) {
                chatNpc(
                    neutral,
                    "You don't have any clue scrolls I need. Unopened scroll boxes don't count.",
                )
            }
            return
        }
        when (redeem()) {
            ClueWatson.Redeem.Given ->
                startDialogue(npc) {
                    chatNpc(neutral, "That's all four! Here is your master clue scroll.")
                    mesbox("Watson hands you a master clue scroll.")
                }
            ClueWatson.Redeem.MasterHeld ->
                startDialogue(npc) {
                    chatNpc(
                        neutral,
                        "I have all four, but you're already carrying a master clue. Finish it " +
                            "first and I'll hold on to these for you.",
                    )
                }
            ClueWatson.Redeem.NoSpace ->
                startDialogue(npc) {
                    chatNpc(neutral, "Make some room in your inventory and come back to me.")
                }
            ClueWatson.Redeem.Missing -> check(npc)
        }
    }

    private suspend fun ProtectedAccess.check(npc: Npc) {
        val missing = ClueWatson.tiers().filterNot { ClueWatson.stored(player, it) }
        startDialogue(npc) {
            if (missing.isEmpty()) {
                chatNpc(neutral, "I have a clue of every tier from you.")
            } else {
                val names = missing.joinToString(", ") { it.name.lowercase() }
                chatNpc(neutral, "I still need a clue scroll from you for: $names.")
            }
        }
    }

    private enum class Choice {
        Give,
        Check,
        Leave,
    }

    private companion object {
        const val SPOKEN = "varbit.trail_watson_spoken"
    }
}
