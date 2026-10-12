package org.rsmod.content.areas.city.catherby.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CatherbyTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.caleb_fitzharmon_1op") { startDialogue(it.npc) { caleb() } }
        onOpNpc1("npc.caleb_fitzharmon_2ops") { startDialogue(it.npc) { caleb() } }
        onOpNpc1("npc.current_affairs_councillor") { startDialogue(it.npc) { catherine() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Caleb
    private suspend fun Dialogue.caleb() {
        when {
            QuestRequirements.hasCompleted(player, FAMILY_PEST) -> calebAfterFamilyPest()
            QuestRequirements.hasCompleted(player, FAMILY_CREST) -> calebAfterFamilyCrest()
            else -> calebBeforeFamilyCrest()
        }
    }

    // TODO(content): [quest:family_crest] quest dialogue
    private suspend fun Dialogue.calebBeforeFamilyCrest() {
        chatNpc(confused, "Who are you? What are you after?")
        val cook =
            choice2(
                "Nothing, I will be on my way.",
                false,
                "I see you are a chef, will you cook me anything?",
                true,
            )
        if (!cook) {
            chatPlayer(neutral, "Nothing, I will be on my way.")
            return
        }
        chatPlayer(quiz, "I see you are a chef... Could you cook me something?")
        chatNpc(
            neutral,
            "I would, but I am very busy. I am trying to increase my reknown as one of the " +
                "worlds' leading chefs by preparing a special and unique fish salad.",
        )
    }

    private suspend fun Dialogue.calebAfterFamilyCrest() {
        if (choice2("Family Pest", false, "Gauntlets", true)) {
            gauntlets()
        }
        // TODO(content): [miniquest:family_pest] the "Family Pest" start dialogue
    }

    private suspend fun Dialogue.gauntlets() {
        chatNpc(
            happy,
            "I hear you have brought the completed crest to my father. I must say, that was " +
                "awfully impressive work.",
        )
        chatPlayer(happy, "It was easy really.")
        chatNpc(neutral, "Yes well... Is there anything I can help you with?")
        if (!choice2("Can you change my gauntlets for me?", true, "No thanks.", false)) {
            chatPlayer(neutral, "No thanks.")
            return
        }
        chatPlayer(quiz, "Can you change my gauntlets for me?")
        if (COOKING_GAUNTLETS in player.inv || COOKING_GAUNTLETS in player.worn) {
            chatNpc(neutral, "Your gauntlets have already been treated with my potions.")
            return
        }
        // TODO(content): [verify:in_game] gauntlets worn rather than carried; only carried ones
        //  are changed here
        val gauntlets = FAMILY_GAUNTLETS.firstOrNull { it in player.inv }
        if (gauntlets == null) {
            chatNpc(neutral, "You don't have any gauntlets I can imbue.")
            return
        }
        // Free for steel gauntlets: https://oldschool.runescape.wiki/w/Cooking_gauntlets
        // TODO(content): [verify:in_game] Caleb's lines for steel gauntlets (not in the transcript)
        if (gauntlets != STEEL_GAUNTLETS) {
            chatNpc(neutral, "Yes certainly, though it will cost you 25,000 coins.")
            if (player.inv.count(COINS) < CHANGE_FEE) {
                chatPlayer(neutral, "Oh, right.")
                return
            }
            if (!choice2("Great thanks!", true, "No that's okay thanks.", false)) {
                chatPlayer(neutral, "No that's okay thanks.")
                return
            }
            chatPlayer(happy, "Great thanks!")
        }
        val fee = if (gauntlets == STEEL_GAUNTLETS) 0 else CHANGE_FEE
        if (!changeGauntlets(gauntlets, fee)) {
            return
        }
        mesbox(
            "Caleb takes the gauntlets from you, pours some herbs and seasonings on them, bakes " +
                "them on his range for a short time, then hands them back to you.",
        )
    }

    private fun Dialogue.changeGauntlets(gauntlets: String, fee: Int): Boolean {
        val result =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete {
                    from = inv
                    obj = gauntlets.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                if (fee > 0) {
                    delete {
                        from = inv
                        obj = COINS.asRSCM(RSCMType.OBJ)
                        strictCount = fee
                    }
                }
                insert {
                    into = inv
                    obj = COOKING_GAUNTLETS.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        return result.success
    }

    // TODO(content): [miniquest:family_pest] "As thanks, I'll provide you with as many pairs of
    //  Cooking gauntlets as you wish." and the free pair on the first talk after the miniquest
    private suspend fun Dialogue.calebAfterFamilyPest() {
        chatNpc(
            happy,
            "I have received word from my father that you assisted us again! I get the feeling " +
                "you weren't going to stop trying until you did though.",
        )
        val more =
            choice2(
                "Could I have some more cooking gauntlets, please?",
                true,
                "You're welcome.",
                false,
            )
        if (!more) {
            chatPlayer(happy, "You're welcome.")
            return
        }
        chatPlayer(quiz, "Could I have some more cooking gauntlets, please?")
        val requested = access.countDialog("How many would you like to take?")
        val count = minOf(requested, player.inv.freeSpace())
        if (count <= 0) {
            return
        }
        if (!player.invAdd(player.inv, COOKING_GAUNTLETS, count).success) {
            return
        }
        mesbox("Caleb hands you some cooking gauntlets.")
        chatNpc(
            neutral,
            "You will find the rate you burn certain foods is much reduced whilst wearing these " +
                "gauntlets.",
        )
        chatPlayer(happy, "Thank you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Councillor_Catherine
    // TODO(content): [quest:current_affairs] quest dialogue
    private suspend fun Dialogue.catherine() {
        chatNpc(
            neutral,
            "Welcome to the Catherby Council Office. Your views are important to us.",
        )
        chatPlayer(neutral, "That's good to hear...")
        chatNpc(
            neutral,
            "Unfortunately, all of our operatives are too busy to help you today. Please try " +
                "again soon.",
        )
        chatPlayer(bored, "Delightful...")
    }

    private companion object {
        const val FAMILY_CREST = "quest_familycrest"
        const val FAMILY_PEST = "miniquest_familypest"
        const val COINS = "obj.coins"
        const val COOKING_GAUNTLETS = "obj.gauntlets_of_cooking"
        const val STEEL_GAUNTLETS = "obj.steel_gauntlets"
        const val CHANGE_FEE = 25_000
        val FAMILY_GAUNTLETS =
            listOf(STEEL_GAUNTLETS, "obj.gauntlets_of_goldsmithing", "obj.gauntlets_of_chaos")
    }
}
