package org.rsmod.content.quest.area.rimmington

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class WitchsPotion :
    QuestScript(
        "quest_witchspotion",
        "varp.hetty",
        rewards { xp("stat.magic", 325.0) },
        ItemRewardDisplay(EYE_OF_NEWT),
    ) {

    override fun ScriptContext.init() {
        onOpNpc1("npc.hetty") { startDialogue(it.npc) { hetty() } }
        onOpLoc1("loc.hettycauldron") { drinkFromCauldron() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Hetty</col> in her house in <col=800000>Rimmington</col>."

    override fun questLog(player: ProtectedAccess): String =
        questJournal(player) {
            val stage = quest.getQuestStage(access.player)
            line(
                "I spoke to Hetty in her house at Rimmington. Hetty told me she could increase my " +
                    "magic power if I can bring her certain ingredients for a potion."
            )
            if (stage >= INGREDIENTS_GIVEN) {
                strike("Hetty needs me to bring her the following:")
                line("I brought Hetty the ingredients and she made a potion in her cauldron.")
                line("I should drink from the cauldron.")
                return@questJournal
            }
            line("Hetty needs me to bring her the following:")
            ingredientLine(access.player, ONION, "an onion", "An onion")
            ingredientLine(access.player, RATS_TAIL, "a rat's tail", "A rat's tail")
            ingredientLine(access.player, BURNT_MEAT, "a piece of burnt meat", "A piece of burnt meat")
            ingredientLine(access.player, EYE_OF_NEWT, "an eye of newt", "An eye of newt")
            if (hasAllIngredients(access.player)) {
                line("I should take these ingredients to Hetty.")
            }
        }

    private fun org.rsmod.content.quest.manager.QuestJournalBuilder.ingredientLine(
        player: Player,
        obj: String,
        held: String,
        needed: String,
    ) {
        if (player.inv.count(obj) > 0) {
            line("I have $held with me${if (obj == ONION) "" else "."}")
        } else {
            line(needed)
        }
    }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line("I brought Hetty an onion, a rat's tail, a piece of burnt meat and an eye of newt.")
            line("She made a potion in her cauldron, and drinking it imbued me with magical power.")
        }

    private fun hasAllIngredients(player: Player): Boolean =
        INGREDIENTS.all { player.inv.count(it) > 0 }

    private suspend fun Dialogue.hetty() {
        when {
            quest.isQuestCompleted(player) -> {
                chatNpc(happy, "How's your magic coming along?")
                chatPlayer(happy, "I'm practicing and slowly getting better.")
                chatNpc(happy, "Good, good.")
            }
            quest.getQuestStage(player) >= INGREDIENTS_GIVEN -> {
                chatNpc(neutral, "Well are you going to drink the potion or not?")
                chatPlayer(neutral, "Yes, I will.")
            }
            quest.isQuestInProgress(player) -> duringQuest()
            else -> beforeQuest()
        }
    }

    private suspend fun Dialogue.beforeQuest() {
        chatNpc(neutral, "What could you want with an old woman like me?")
        val quest = menu("I am in search of a quest." to true, "I've heard that you are a witch." to false)
        if (quest) {
            offerQuest()
            return
        }
        chatPlayer(neutral, "I've heard that you are a witch.")
        chatNpc(neutral, "Yes it does seem to be getting fairly common knowledge.")
        chatNpc(
            worried,
            "I fear I may be getting a visit from the witch hunters of Falador before long.",
        )
        val search = menu("I am in search of a quest." to true, "Goodbye." to false)
        if (search) {
            offerQuest()
        } else {
            chatPlayer(neutral, "Goodbye.")
        }
    }

    private suspend fun Dialogue.offerQuest() {
        chatPlayer(neutral, "I am in search of a quest.")
        chatNpc(neutral, "Hmmm... Maybe I can think of something for you.")
        chatNpc(happy, "Would you like to become more proficient in the dark arts?")
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "No, I have my principles and honour.")
            chatNpc(neutral, "Suit yourself, but you're missing out.")
            return
        }
        chatPlayer(happy, "Yes, help me become one with my darker side.")
        chatNpc(happy, "Okay, I'm going to make a potion to help bring out your darker self.")
        chatNpc(neutral, "You will need certain ingredients.")
        chatPlayer(neutral, "What do I need?")
        quest.advanceQuestStageTo(access, STARTED)
        chatNpc(
            neutral,
            "You need an eye of newt, a rat's tail, an onion... Oh and a piece of burnt meat.",
        )
        chatPlayer(happy, "Great, I'll go and get them.")
    }

    private suspend fun Dialogue.duringQuest() {
        chatPlayer(happy, "I've been looking for those ingredients.")
        chatNpc(happy, "So what have you found so far?")
        if (hasAllIngredients(player)) {
            chatPlayer(happy, "In fact I have everything!")
            chatNpc(happy, "Excellent, can I have them then?")
            for (ingredient in INGREDIENTS) {
                access.invDel(access.inv, ingredient)
            }
            mesbox(
                "You pass the ingredients to Hetty and she puts them all into her cauldron. Hetty " +
                    "closes her eyes and begins to chant. The cauldron bubbles mysteriously."
            )
            chatPlayer(neutral, "Well, is it ready?")
            quest.advanceQuestStageTo(access, INGREDIENTS_GIVEN)
            chatNpc(happy, "Ok, now drink from the cauldron.")
            return
        }
        if (INGREDIENTS.none { player.inv.count(it) > 0 }) {
            chatPlayer(sad, "I'm afraid I don't have any of them yet.")
            chatNpc(
                angry,
                "Well I can't make the potion without them! Remember... You need an eye of newt, " +
                    "a rat's tail, an onion, and a piece of burnt meat. Off you go dear!",
            )
            return
        }
        chatPlayer(neutral, partialIngredients(player))
        chatNpc(neutral, "Great, but I'll need the other ingredients as well.")
    }

    private fun partialIngredients(player: Player): String {
        fun has(obj: String) = player.inv.count(obj) > 0
        val tail = if (has(RATS_TAIL)) "I have the rat's tail (ewww)" else "I don't have a rat's tail"
        val meat = if (has(BURNT_MEAT)) "I have the burnt meat" else "I don't have any burnt meat"
        val onion = if (has(ONION)) "I have an onion" else "I don't have an onion"
        val newt = if (has(EYE_OF_NEWT)) "I have the eye of newt, yum!" else "I don't have an eye of newt."
        return "$tail, $meat, $onion, and $newt"
    }

    private suspend fun ProtectedAccess.drinkFromCauldron() {
        arriveDelay()
        if (quest.getQuestStage(player) != INGREDIENTS_GIVEN) {
            startDialogue {
                chatPlayer(
                    neutral,
                    "As nice as that looks, I think I'll give it a miss for now.",
                )
            }
            return
        }
        mesbox("You drink from the cauldron. It tastes horrible! You feel yourself imbued with power.")
        quest.advanceQuestStageTo(this, COMPLETE)
    }

    private companion object {
        const val STARTED = 1
        const val INGREDIENTS_GIVEN = 2
        const val COMPLETE = 3

        const val ONION = "obj.onion"
        const val RATS_TAIL = "obj.rats_tail"
        const val BURNT_MEAT = "obj.burnt_meat"
        const val EYE_OF_NEWT = "obj.eye_of_newt"
        val INGREDIENTS = listOf(EYE_OF_NEWT, RATS_TAIL, ONION, BURNT_MEAT)
    }
}
