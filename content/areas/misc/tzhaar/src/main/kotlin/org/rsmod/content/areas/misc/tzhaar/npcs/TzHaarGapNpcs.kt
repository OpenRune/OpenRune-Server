package org.rsmod.content.areas.misc.tzhaar.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.shops.Shops
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class TzHaarGapNpcs @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(KET_YIL) { startDialogue(it.npc) { ketYil() } }
        onOpNpc3(KET_YIL) { tryOpenBank() }
        onOpNpc4(KET_YIL) { openCollectionBox() }
        onOpNpc1(HUR_ZAL) { startDialogue(it.npc) { hurZal(it.npc) } }
        onOpNpc3(HUR_ZAL) { player.openHurZalShop(it.npc) }
        onOpNpc1("npc.tzhaar_fightpit_master") { startDialogue(it.npc) { mejKah() } }
        onOpNpc1("npc.tzhaar_elder1") { startDialogue(it.npc) { terminalChamberElder() } }
        onOpNpc1("npc.tzhaar_elder2") { startDialogue(it.npc) { terminalChamberElder() } }
    }

    private fun ProtectedAccess.openCollectionBox() {
        ifOpenMainModal("interface.ge_collect")
    }

    private fun Player.openHurZalShop(npc: Npc) {
        shops.open(this, npc, "TzHaar-Hur-Zal's Equipment Store", "inv.tzhaar_shop_cityequipment")
    }

    // https://oldschool.runescape.wiki/w/Transcript:TzHaar-Ket-Yil
    // TODO(content): [system:bank_space_shop] "I'd like to buy more bank slots."
    // TODO(content): [verify:in_game] the wiki greeting reads "JalYt-Hur-[caste]-[name]"
    private suspend fun Dialogue.ketYil() {
        chatNpc(quiz, "Can I help you ${player.tzhaarName()}?")
        val option =
            choice4(
                "I'd like to access my bank account please.",
                1,
                "What did you call me?",
                2,
                "I'd like to check my PIN settings.",
                3,
                "More options...",
                4,
            )
        when (option) {
            1 -> access.tryOpenBank()
            2 -> {
                val caste = TzHaarCaste.of(player)
                chatNpc(confused, "Are you not ${player.tzhaarName()}?")
                if (!yourCaste(caste)) {
                    return
                }
                val next = choice2("What are you then?", 1, "Thanks for explaining it.", 2)
                if (next == 2) {
                    chatPlayer(happy, "Thanks for explaining it.")
                    return
                }
                chatPlayer(quiz, "What are you then?")
                chatNpc(neutral, "Daft JalYt, I am TzHaar-Ket, one of the guardians of our city.")
                chatNpc(
                    neutral,
                    "There are the wise TzHaar-Mej who guide us, the swift TzHaar-Xil who hunt " +
                        "for our food, and the skilled TzHaar-Hur who craft our homes and tools.",
                )
            }
            3 -> {
                chatPlayer(neutral, "I'd like to check my PIN settings.")
                access.ifOpenMainModal("interface.bankpin_settings")
            }
            4 -> {
                val more =
                    choice3(
                        "I'd like to collect items.",
                        1,
                        "No, I'm fine thanks.",
                        2,
                        "Previous options...",
                        3,
                    )
                when (more) {
                    1 -> {
                        chatPlayer(neutral, "I'd like to collect items.")
                        access.openCollectionBox()
                    }
                    2 -> chatPlayer(neutral, "No, I'm fine thanks.")
                    3 -> ketYil()
                }
            }
        }
    }

    private suspend fun Dialogue.yourCaste(caste: TzHaarCaste): Boolean {
        val option = choice3("What's a '${caste.title}'?", 1, "I guess so...", 2, "No I'm not!", 3)
        when (option) {
            1 -> {
                chatPlayer(confused, "What's a '${caste.title}'?")
                chatNpc(neutral, caste.description)
                chatPlayer(neutral, "Well yes I suppose I am...")
                chatNpc(happy, "Then you ${caste.title}!")
                return true
            }
            2 -> {
                chatPlayer(neutral, "I guess so...")
                chatNpc(neutral, "Well then, no problems.")
            }
            3 -> {
                chatPlayer(angry, "No I'm not!")
                chatNpc(laugh, "What ever you say, crazy JalYt!")
            }
        }
        return false
    }

    // https://oldschool.runescape.wiki/w/Transcript:TzHaar-Hur-Zal
    private suspend fun Dialogue.hurZal(npc: Npc) {
        chatNpc(quiz, "Can I help you ${player.tzhaarName()}?")
        val option =
            choice3(
                "What do you have to trade?",
                1,
                "What did you call me?",
                2,
                "No I'm fine thanks.",
                3,
            )
        when (option) {
            1 -> player.openHurZalShop(npc)
            2 -> {
                val caste = TzHaarCaste.of(player)
                chatNpc(confused, "Are you not ${player.tzhaarName()}?")
                chatPlayer(confused, "What's a '${caste.title}'?")
                chatNpc(neutral, caste.description)
                chatPlayer(neutral, "Well yes I suppose I am...")
                chatNpc(happy, "Then you ${caste.title}!")
                val next =
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
                when (next) {
                    1 -> {
                        chatPlayer(quiz, "What are you then?")
                        chatNpc(
                            neutral,
                            "Silly JalYt, I am TzHaar-Hur, one of the crafters for this city.",
                        )
                        chatNpc(
                            neutral,
                            "There are the wise TzHaar-Mej who guide us, the mighty TzHaar-Ket " +
                                "who guard us, and the swift TzHaar-Xil who hunt for our food.",
                        )
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
            3 -> chatPlayer(neutral, "No I'm fine thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:TzHaar-Mej-Kah
    // TODO(content): [system:fight_pits] "Who's the current champion?" and the champion overhead
    // TODO(content): [system:fight_pits] "Could you stop me from gaining XP?" toggle
    private suspend fun Dialogue.mejKah() {
        chatNpc(quiz, "You want help ${player.tzhaarName()}?")
        val option =
            choice3("What is this place?", 1, "What did you call me?", 2, "No I'm fine thanks.", 3)
        when (option) {
            1 -> {
                chatPlayer(quiz, "What is this place?")
                chatNpc(
                    neutral,
                    "This is the fight pit, TzHaar-Xil made it for their sport but many JalYt " +
                        "come here to fight too. If you are wanting to fight then enter the " +
                        "cage, you will be summoned when next round is ready to begin.",
                )
                if (!choice2("Are there any rules?", true, "Ok thanks.", false)) {
                    chatPlayer(neutral, "Ok thanks.")
                    return
                }
                chatPlayer(quiz, "Are there any rules?")
                chatNpc(
                    neutral,
                    "No rules, you use whatever you want. Last person standing wins and is " +
                        "declared champion, they stay in the pit for next fight.",
                )
                if (!choice2("Do I win anything?", true, "Sounds good.", false)) {
                    chatPlayer(happy, "Sounds good.")
                    return
                }
                chatPlayer(quiz, "Do I win anything?")
                chatNpc(
                    angry,
                    "You ask a lot of questions. Champion gets TokKul as reward, more fights the " +
                        "more TokKul they get.",
                )
                chatPlayer(silent, "...")
                chatNpc(neutral, "Before you ask, TokKul is like your Coins.")
                chatNpc(
                    neutral,
                    "Gold is like you JalYt, soft and easily broken, we use hard rock forged in " +
                        "fire like TzHaar!",
                )
            }
            2 -> {
                val caste = TzHaarCaste.of(player)
                chatPlayer(quiz, "What did you call me?")
                chatNpc(confused, "Are you not ${player.tzhaarName()}?")
                if (!yourCaste(caste)) {
                    return
                }
                val next = choice2("What are you then?", 1, "Thanks for explaining it.", 2)
                if (next == 2) {
                    chatPlayer(happy, "Thanks for explaining it.")
                    return
                }
                chatPlayer(quiz, "What are you then?")
                chatNpc(neutral, "Foolish JalYt, I am TzHaar-Mej, one of the mystics of this city.")
                if (choice2("What other types are there?", true, "Ah ok then.", false)) {
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
            3 -> chatPlayer(neutral, "No I'm fine thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:TzHaar-Mej-Dir
    // https://oldschool.runescape.wiki/w/Transcript:TzHaar-Mej-Bal
    // TODO(content): [verify:in_game] Xo honorific in the greeting (both wiki greetings show it)
    private suspend fun Dialogue.terminalChamberElder() {
        val caste = TzHaarCaste.of(player)
        chatNpc(neutral, "Hello ${caste.title}-Xo-${player.displayName}.")
        chatPlayer(quiz, "Hello. What's happening here?")
        chatNpc(
            neutral,
            "This is TzHaar terminal chamber, TzHaar rest here to draw warmth from the volcano.",
        )
        chatPlayer(quiz, "Why is that?")
        chatNpc(
            sad,
            "As TzHaar grow older we lose heat and begin to solidify. Drawing heat from the " +
                "volcano allows us to prolong this process.",
        )
        chatPlayer(quiz, "What happens when TzHaar solidify?")
        chatNpc(
            sad,
            "Over time TzHaar will eventually harden and cease to be molten entirely before " +
                "turning to rock, known as Haar-Tok.",
        )
        chatPlayer(quiz, "Are Haar-Tok dead?")
        chatNpc(
            sad,
            "In essence yes, as Haar-Tok can not move or communicate but the former TzHaar " +
                "remains conscious.",
        )
        chatPlayer(neutral, "Intriguing, thank you.")
    }

    private companion object {
        const val KET_YIL = "npc.tzhaar_banker2"
        const val HUR_ZAL = "npc.tzhaar_merchant_cityequipment"
    }
}
