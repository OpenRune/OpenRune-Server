package org.rsmod.content.areas.misc.tzhaar.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:TzHaar-Mej-Jal
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [boss:tztok_jad] "Bargain for TzRek-Jad." when the pet isn't owned
// TODO(content): [verify:in_game] fire cape sale confirmation menu (wiki marks it as unsure)
// and "Exchange fire cape" without a cape in the inventory
class TzHaarMejJal @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(MEJ_JAL) { startDialogue(it.npc) { mejJal() } }
        onOpNpc3(MEJ_JAL) {
            startDialogue(it.npc) { if (FIRE_CAPE in player.inv) exchangeFireCape() else mejJal() }
        }
    }

    private suspend fun Dialogue.mejJal() {
        chatNpc(quiz, "You want help ${player.tzhaarName()}?")
        val hasCape = FIRE_CAPE in player.inv
        val option =
            choice3(
                if (hasCape) "I have a fire cape here." else "What is this place?",
                1,
                "What did you call me?",
                2,
                "No I'm fine thanks.",
                3,
            )
        when (option) {
            1 -> if (hasCape) fireCape() else whatIsThisPlace()
            2 -> whatDidYouCallMe()
            3 -> chatPlayer(neutral, "No I'm fine thanks.")
        }
    }

    private suspend fun Dialogue.fireCape() {
        chatPlayer(happy, "I have a fire cape here.")
        exchangeFireCape()
    }

    private suspend fun Dialogue.exchangeFireCape() {
        chatNpc(
            neutral,
            "Indeed, JalYt. You want to sell it back? I swap it for 8,000 TokKul if you want.",
        )
        val option =
            choice2(
                "Yes, sell it for 8,000 TokKul.",
                true,
                "No, keep it.",
                false,
                title = "Sell your fire cape for 8,000 TokKul?",
            )
        if (!option) {
            chatNpc(neutral, "Wise choice, JalYt.")
            return
        }
        if (player.invDel(player.inv, FIRE_CAPE).failure) {
            return
        }
        player.invAddOrDrop(objRepo, TOKKUL, FIRE_CAPE_PRICE)
        mesbox("You exchange your fire cape for 8,000 TokKul.")
    }

    private suspend fun Dialogue.whatIsThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            neutral,
            "This is the fight cave, TzHaar-Xil made it for practice, but many JalYt come here " +
                "to fight too. Just enter the cave and make sure you're prepared.",
        )
        val option = choice2("Are there any rules?", 1, "Ok thanks.", 2)
        if (option == 2) {
            chatPlayer(neutral, "Ok thanks.")
            return
        }
        chatPlayer(quiz, "Are there any rules?")
        chatNpc(neutral, "Rules? Survival is the only rule in there.")
        val next = choice2("Do I win anything?", 1, "Sounds good.", 2)
        if (next == 2) {
            chatPlayer(happy, "Sounds good.")
            return
        }
        chatPlayer(quiz, "Do I win anything?")
        chatNpc(
            neutral,
            "You ask a lot of questions. Might give you TokKul if you last long enough.",
        )
        chatPlayer(confused, "...")
        chatNpc(neutral, "Before you ask, TokKul is like your Coins.")
        chatNpc(
            neutral,
            "Gold is like you JalYt, soft and easily broken, we use hard rock forged in fire " +
                "like TzHaar!",
        )
    }

    private suspend fun Dialogue.whatDidYouCallMe() {
        val caste = TzHaarCaste.of(player)
        chatPlayer(confused, "What did you call me?")
        chatNpc(confused, "Are you not ${player.tzhaarName()}?")
        chatPlayer(confused, "What's a '${caste.title}'?")
        chatNpc(neutral, caste.description)
        chatPlayer(neutral, "Well yes I suppose I am...")
        chatNpc(happy, "Then you ${caste.title}!")
        val option =
            choice4(
                "What are you then?",
                1,
                "Thanks for explaining it.",
                2,
                "I guess so...",
                3,
                "No I'm not!",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What are you then?")
                chatNpc(neutral, "Foolish JalYt, I am TzHaar-Mej, one of the mystics of this city.")
                val next = choice2("What other types are there?", 1, "Ah ok then.", 2)
                if (next == 1) {
                    chatPlayer(quiz, "What other types are there?")
                    chatNpc(
                        neutral,
                        "There are the mighty TzHaar-Ket who guard us, the swift TzHaar-Xil who " +
                            "hunt for our food, and the skilled TzHaar-Hur who craft our homes " +
                            "and tools.",
                    )
                } else {
                    chatPlayer(neutral, "Ah ok then.")
                }
            }
            2 -> chatPlayer(happy, "Thanks for explaining it.")
            3 -> {
                chatPlayer(neutral, "I guess so...")
                chatNpc(neutral, "Well then, no problems.")
            }
            4 -> {
                chatPlayer(angry, "No I'm not!")
                chatNpc(laugh, "What ever you say, crazy JalYt!")
            }
        }
    }

    internal companion object {
        const val MEJ_JAL = "npc.tzhaar_fightcave_master"
        const val FIRE_CAPE = "obj.tzhaar_cape_fire"
        const val TOKKUL = "obj.tzhaar_token"
        const val FIRE_CAPE_PRICE = 8000
    }
}
