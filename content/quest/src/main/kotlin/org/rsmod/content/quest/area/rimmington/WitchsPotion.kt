package org.rsmod.content.quest.area.rimmington

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Witch's Potion. Progress is varp 67 (`varp.hetty`), as in OSRS and as
 * Quest Helper reads it: 0 not started, 1 started, 2 ingredients given, 3 complete (drunk from the
 * cauldron). Dialogue follows the wiki transcript; the journal text is ours. Rats drop a rat's tail
 * while the quest is in progress (drop table `rat.toml`).
 */
class WitchsPotion : QuestScript(
    "quest_witchspotion",
    "varp.hetty",
    rewards { xp("stat.magic", MAGIC_XP) },
    ItemRewardDisplay(EYE_OF_NEWT, zoom = 250),
    questVarbit = "varbit.witchs_potion_progress",
) {

    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE) {
            "Witch's Potion end state is ${quest.maxSteps} in the cache dbrow, but drinking " +
                "writes $COMPLETE."
        }
        onOpNpc1(HETTY) { startDialogue(it.npc) { hettyDialogue() } }
        onOpLoc1(CAULDRON) { drinkFromCauldron() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Hetty</col> in her house in <col=800000>Rimmington</col>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            description("<red>Hetty</red> is making a potion to bring out my darker side.")
            if (stage(player.player) >= INGREDIENTS_GIVEN) {
                objective(
                    "I gave Hetty the ingredients. I should drink from her <red>cauldron</red>."
                ) {}
                return@questJournal
            }
            objective(
                "She needs an <red>eye of newt</red>, a <red>rat's tail</red>, an " +
                    "<red>onion</red> and a piece of <red>burnt meat</red>."
            ) {
                hasItem("eye_of_newt", "I have an <red>eye of newt</red>.")
                    .preserveObjective(strikeObjective = false)
                hasItem("rats_tail", "I have a <red>rat's tail</red>.")
                    .preserveObjective(strikeObjective = false)
                hasItem("onion", "I have an <red>onion</red>.")
                    .preserveObjective(strikeObjective = false)
                hasItem("burnt_meat", "I have some <red>burnt meat</red>.")
                    .preserveObjective(strikeObjective = false)
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(
                "I brought Hetty an eye of newt, a rat's tail, an onion and some burnt meat. I " +
                    "drank the potion she brewed from them and felt imbued with power."
            )
        }

    fun stage(player: Player): Int = quest.getQuestStage(player)

    private fun has(player: Player, obj: String): Boolean = player.inv.count(obj) > 0

    private suspend fun Dialogue.hettyDialogue() {
        when (stage(player)) {
            0 -> beforeQuest()
            STARTED -> lookingForIngredients()
            INGREDIENTS_GIVEN -> {
                chatNpc(quiz, "Well are you going to drink the potion or not?")
                chatPlayer(neutral, "Yes, I will.")
            }
            else -> {
                chatNpc(happy, "How's your magic coming along?")
                chatPlayer(happy, "I'm practicing and slowly getting better.")
                chatNpc(happy, "Good, good.")
            }
        }
    }

    private suspend fun Dialogue.beforeQuest() {
        chatNpc(quiz, "What could you want with an old woman like me?")
        when (choice2("I am in search of a quest.", 1, "I've heard that you are a witch.", 2)) {
            1 -> offerQuest()
            2 -> {
                chatPlayer(quiz, "I've heard that you are a witch.")
                chatNpc(neutral, "Yes it does seem to be getting fairly common knowledge.")
                chatNpc(
                    worried,
                    "I fear I may be getting a visit from the witch hunters of Falador before " +
                        "long.",
                )
                when (choice2("I am in search of a quest.", 1, "Goodbye.", 2)) {
                    1 -> offerQuest()
                    2 -> chatPlayer(neutral, "Goodbye.")
                }
            }
        }
    }

    private suspend fun Dialogue.offerQuest() {
        chatPlayer(neutral, "I am in search of a quest.")
        chatNpc(confused, "Hmmm... Maybe I can think of something for you.")
        chatNpc(quiz, "Would you like to become more proficient in the dark arts?")
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "No, I have my principles and honour.")
            chatNpc(neutral, "Suit yourself, but you're missing out.")
            return
        }
        chatPlayer(happy, "Yes, help me become one with my darker side.")
        quest.setQuestStage(access, STARTED)
        chatNpc(happy, "Okay, I'm going to make a potion to help bring out your darker self.")
        chatNpc(neutral, "You will need certain ingredients.")
        chatPlayer(quiz, "What do I need?")
        chatNpc(
            neutral,
            "You need an eye of newt, a rat's tail, an onion... Oh and a piece of burnt meat.",
        )
        chatPlayer(happy, "Great, I'll go and get them.")
    }

    private suspend fun Dialogue.lookingForIngredients() {
        chatPlayer(neutral, "I've been looking for those ingredients.")
        chatNpc(quiz, "So what have you found so far?")
        val tail = has(player, RATS_TAIL)
        val meat = has(player, BURNT_MEAT)
        val onion = has(player, ONION)
        val newt = has(player, EYE_OF_NEWT)
        when {
            tail && meat && onion && newt -> handIn()
            !tail && !meat && !onion && !newt -> {
                chatPlayer(sad, "I'm afraid I don't have any of them yet.")
                chatNpc(
                    angry,
                    "Well I can't make the potion without them! Remember... You need an eye of " +
                        "newt, a rat's tail, an onion, and a piece of burnt meat. Off you go dear!",
                )
            }
            else -> {
                chatPlayer(neutral, ingredientsLine(tail, meat, onion, newt))
                chatNpc(neutral, "Great, but I'll need the other ingredients as well.")
            }
        }
    }

    private suspend fun Dialogue.handIn() {
        chatPlayer(happy, "In fact I have everything!")
        chatNpc(happy, "Excellent, can I have them then?")
        val taken =
            player.invTransaction(access.inv) {
                val from = select(access.inv)
                for (obj in INGREDIENTS) {
                    delete {
                        this.from = from
                        this.obj = obj.asRSCM(RSCMType.OBJ)
                        this.strictCount = 1
                    }
                }
            }
        if (taken.failure) {
            return
        }
        quest.setQuestStage(access, INGREDIENTS_GIVEN)
        mesbox("You pass the ingredients to Hetty and she puts them all into her cauldron.")
        mesbox("Hetty closes her eyes and begins to chant. The cauldron bubbles mysteriously.")
        chatPlayer(quiz, "Well, is it ready?")
        chatNpc(happy, "Ok, now drink from the cauldron.")
    }

    private suspend fun ProtectedAccess.drinkFromCauldron() {
        if (stage(player) != INGREDIENTS_GIVEN) {
            startDialogue {
                chatPlayer(neutral, "As nice as that looks, I think I'll give it a miss for now.")
            }
            return
        }
        mes("You drink from the cauldron. It tastes horrible! You feel yourself imbued with power.")
        quest.completeQuest(this)
    }

    companion object {
        const val STARTED = 1
        const val INGREDIENTS_GIVEN = 2
        const val COMPLETE = 3

        private const val HETTY = "npc.hetty"
        private const val CAULDRON = "loc.hettycauldron"
        private const val MAGIC_XP = 325.0

        const val RATS_TAIL = "obj.rats_tail"
        private const val BURNT_MEAT = "obj.burnt_meat"
        private const val ONION = "obj.onion"
        private const val EYE_OF_NEWT = "obj.eye_of_newt"
        private val INGREDIENTS = listOf(EYE_OF_NEWT, RATS_TAIL, ONION, BURNT_MEAT)

        fun ingredientsLine(tail: Boolean, meat: Boolean, onion: Boolean, newt: Boolean): String {
            val tailPart = if (tail) "I have the rat's tail (ewww)" else "I don't have a rat's tail"
            val meatPart = if (meat) "I have the burnt meat" else "I don't have any burnt meat"
            val onionPart = if (onion) "I have an onion" else "I don't have an onion"
            val newtPart =
                if (newt) "I have the eye of newt, yum!" else "I don't have an eye of newt."
            return "$tailPart, $meatPart, $onionPart, and $newtPart"
        }
    }
}
