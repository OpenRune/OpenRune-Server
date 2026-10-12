package org.rsmod.content.areas.city.desert.nardah.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Ali_the_Carter
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:loc_interactions] items used on Ali's water cart and barrel
class AliTheCarter @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ali_the_carter") { startDialogue(it.npc) { carter() } }
    }

    private suspend fun Dialogue.carter() {
        if (QuestRequirements.hasCompleted(player, SPIRITS_OF_THE_ELID)) {
            glumCarter()
            return
        }
        chatNpc(
            happy,
            "Water, water get your fresh water here, 1000 coins a waterskin, a bargain in these " +
                "difficult times.",
        )
        val option =
            choice3(
                "That's extortionate! Why so much?",
                1,
                "Ok, I'll buy some!",
                2,
                "I think I'll give it a miss.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(angry, "That's extortionate! Why so much?")
                chatNpc(shifty, "Well it's the cheapest water you'll find in this town.")
                if (QuestRequirements.isOnQuest(player, SPIRITS_OF_THE_ELID)) {
                    curseQuestion()
                }
            }
            2 -> buyWater()
            3 -> chatPlayer(neutral, "I think I'll give it a miss.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Spirits_of_the_Elid
    private suspend fun Dialogue.curseQuestion() {
        chatPlayer(quiz, "Would you happen to know anything about the curse?")
        chatNpc(
            happy,
            "Yes the people round here can't seem to get the water themselves. It's an ideal " +
                "business opportunity for one such as myself.",
        )
        chatPlayer(
            neutral,
            "It does seem to be slightly exploiting their unfortunate situation.",
        )
        chatNpc(
            shifty,
            "I don't see it like that. Without me they'd be in even more of a mess. I'm doing " +
                "them a favour and in true Pollnivneachian spirit making a profit along the way.",
        )
    }

    private suspend fun Dialogue.glumCarter() {
        chatPlayer(quiz, "You look glum.")
        chatNpc(
            sad,
            "Business here really seems to have dried up. I had a good thing going here and you " +
                "spoiled it.",
        )
        chatPlayer(
            neutral,
            "Well it serves you right really. Taking advantage of people like that.",
        )
        chatNpc(neutral, "So want to buy some water?")
        chatPlayer(quiz, "How much?")
        chatNpc(neutral, "To you 1000 coins for a waterskin!")
        if (choice2("Ok, I feel sorry for you.", true, "I think I'll give it a miss.", false)) {
            buyWater()
        } else {
            chatPlayer(neutral, "I think I'll give it a miss.")
        }
    }

    private suspend fun Dialogue.buyWater() {
        if (!player.invTakeFee(WATER_PRICE)) {
            mesbox("You don't have 1000 coins to buy the water with.")
            return
        }
        player.invAddOrDrop(objRepo, WATERSKIN)
        mesbox("You buy a full waterskin for 1000 coins!")
    }

    private companion object {
        const val WATERSKIN = "obj.water_skin4"
        const val WATER_PRICE = 1000
        const val SPIRITS_OF_THE_ELID = "quest_spiritsoftheelid"
    }
}
