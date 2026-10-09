package org.rsmod.content.areas.city.barbarianvillage.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private val Player.workoutCount by intVarBit("varbit.bim_workout_counter")

class Atlas : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.bim_atlas") { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) = startDialogue(npc) { atlas() }

    private suspend fun Dialogue.atlas() {
        if (!QuestRequirements.hasCompleted(player, BELOW_ICE_MOUNTAIN)) {
            busy()
            return
        }
        when (player.workoutCount) {
            0 -> firstWorkout()
            1 -> secondWorkout()
            else -> subsequentWorkout()
        }
    }

    private suspend fun Dialogue.busy() {
        val title = if (access.isBodyTypeA()) "man" else "woman"
        chatNpc(neutral, "What do you want, little $title?")
        chatPlayer(neutral, "Nothing. Just though I'd try to strike up a conversation.")
        chatNpc(neutral, "Not now little one, I'm busy.")
        chatPlayer(neutral, "Okay then.")
    }

    private suspend fun Dialogue.firstWorkout() {
        chatNpc(happy, "Well done, little one. You've proven your worth.")
        chatPlayer(quiz, "Can I do it again?")
        chatNpc(confused, "What? Why?")
        chatPlayer(happy, "It was fun, I'd like to do it again.")
        chatNpc(
            neutral,
            "You're the first person to ask me that ${player.displayName}. I suppose I could " +
                "train you again, but I will have to charge you the full 25,000 coins this time.",
        )
        offerWorkout()
    }

    private suspend fun Dialogue.secondWorkout() {
        chatNpc(happy, "Back again I see, here for another workout?")
        chatPlayer(happy, "Yes please.")
        chatNpc(
            neutral,
            "You must take your training very seriously ${player.displayName}. This is the " +
                "second time you'll have done this workout and paid full price!",
        )
        offerWorkout()
    }

    private suspend fun Dialogue.subsequentWorkout() {
        chatNpc(happy, "Back again I see, here for another workout?")
        chatPlayer(happy, "Yes please.")
        chatNpc(
            neutral,
            "You take your training very seriously ${player.displayName}. You know you've done " +
                "this workout ${player.workoutCount} times, are you sure you want to do it again?",
        )
        offerWorkout()
    }

    private suspend fun Dialogue.offerWorkout() {
        val pay = choice2("Yes", true, "No", false, title = "Pay Atlas 25,000 coins to train again?")
        if (!pay) {
            return
        }
        chatPlayer(happy, "Let's do this, I'm ready.")
        if (access.inv.count("obj.coins") < WORKOUT_PRICE) {
            chatNpc(
                neutral,
                "Hold it there little one, you don't have enough money to pay for the lesson.",
            )
            chatPlayer(sad, "Oh, sorry I was too excited...")
            chatNpc(neutral, "No worries friend, come back when you can afford the session.")
            return
        }
        chatNpc(happy, "It's not going to be any easier my friend!")
    }

    private companion object {
        const val BELOW_ICE_MOUNTAIN = "quest_belowicemountain"
        const val WORKOUT_PRICE = 25_000
    }
}
