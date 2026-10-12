package org.rsmod.content.areas.misc.islands.apeatoll.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.shops.Shops
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:monkey_disguise] Monkey language for a player without an M'speak amulet
class MarimGapNpcs @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hundred_ilm_iwazaru") { startDialogue(it.npc) { threeMonkeys() } }
        onOpNpc1("npc.hundred_ilm_kikazaru") { startDialogue(it.npc) { threeMonkeys() } }
        onOpNpc1("npc.hundred_ilm_mizaru") { startDialogue(it.npc) { threeMonkeys() } }
        onOpNpc1(OOBAPOHK) { startDialogue(it.npc) { oobapohk(it.npc) } }
        onOpNpc3(OOBAPOHK) { oobapohkTrade(it.npc) }
        onOpNpc1(JUMAANE) { startDialogue(it.npc) { jumaane() } }
        onOpNpc3(JUMAANE) { tryOpenBank() }
        onOpNpc4(JUMAANE) { openCollectionBox() }
        onOpNpc1("npc.mm_padulah") { startDialogue(it.npc) { padulah() } }
    }

    private fun ProtectedAccess.openCollectionBox() {
        ifOpenMainModal("interface.ge_collect")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Iwazaru
    // https://oldschool.runescape.wiki/w/Transcript:Kikazaru
    // https://oldschool.runescape.wiki/w/Transcript:Mizaru
    private suspend fun Dialogue.threeMonkeys() {
        chatNpcSpecific("Iwazaru", "npc.hundred_ilm_iwazaru", angry, "MMMGHGHGGMH!!!")
        chatNpcSpecific("Mizaru", "npc.hundred_ilm_mizaru", shocked, "What's going on ARRGH!")
        if (access.random.of(2) == 0) {
            chatNpcSpecific(
                "Mizaru",
                "npc.hundred_ilm_mizaru",
                shocked,
                "Arrgh! Human! Scary! Go away!",
            )
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Oobapohk
    private suspend fun Dialogue.oobapohk(npc: Npc) {
        if (!QuestRequirements.hasCompleted(player, MONKEY_MADNESS_II)) {
            oobapohkOutOfStock()
            return
        }
        chatNpc(happy, "Would you like to buy or sell any javelins?")
        val option =
            choice3(
                "Yes, please.",
                1,
                "No, thanks.",
                2,
                "Could you sell me some dragon javelins?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Yes, please.")
                shops.open(player, npc, JAVELIN_SHOP, JAVELIN_INV)
            }
            2 -> chatPlayer(neutral, "No, thanks.")
            3 -> {
                chatPlayer(quiz, "Could you sell me some dragon javelins?")
                chatNpc(
                    laugh,
                    "Dragon javelins? Haha, you'll be a chief advisor to the King before you " +
                        "ever hold a dragon javelin!",
                )
            }
        }
    }

    // TODO(content): [verify:in_game] the Trade option before Monkey Madness II; the transcript
    // only gives the Talk-to lines
    private suspend fun ProtectedAccess.oobapohkTrade(npc: Npc) {
        if (!QuestRequirements.hasCompleted(player, MONKEY_MADNESS_II)) {
            startDialogue(npc) { oobapohkOutOfStock() }
            return
        }
        shops.open(player, npc, JAVELIN_SHOP, JAVELIN_INV)
    }

    private suspend fun Dialogue.oobapohkOutOfStock() {
        chatPlayer(quiz, "May I browse your store?")
        chatNpc(sad, "Sorry, I'm currently out of javelins.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jumaane
    // TODO(content): [system:bank_space_shop] "I'd like to buy more bank slots."
    private suspend fun Dialogue.jumaane() {
        chatNpc(neutral, "Good day, how may I help you?")
        while (true) {
            val option =
                choice5(
                    "What is this place?",
                    1,
                    "I'd like to access my bank account, please.",
                    2,
                    "I'd like to check my PIN settings.",
                    3,
                    "I'd like to collect items.",
                    4,
                    "I'm fine, thanks.",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        neutral,
                        "This is the Marim branch of the Bank of Gielinor. Now that our people " +
                            "have more contact with the lands overseas, we have new business " +
                            "opportunities. Can I help you?",
                    )
                }
                2 -> {
                    access.tryOpenBank()
                    return
                }
                3 -> {
                    access.ifOpenMainModal("interface.bankpin_settings")
                    return
                }
                4 -> {
                    access.openCollectionBox()
                    return
                }
                else -> {
                    chatPlayer(neutral, "I'm fine, thanks.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Padulah
    // TODO(content): [skill:thieving] dialogue around stealing from the statue
    private suspend fun Dialogue.padulah() {
        chatNpc(angry, "What do you want?")
        chatPlayer(neutral, "Oh, nothing in particular really.")
        chatNpc(
            angry,
            "Well stop distracting me then. I'm meant to be guarding this sacred statue from the " +
                "temple of Marimbo.",
        )
        val option =
            choice3(
                "Where is the temple of Marimbo?",
                1,
                "What is the statue of?",
                2,
                "I'll be back later.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Where is the temple of Marimbo?")
                chatNpc(neutral, "You're not from around here are you?")
                chatPlayer(neutral, "Actually, I'm not. I'm a visitor from foreign lands.")
                chatNpc(neutral, "Very well. The temple is to the east of the village.")
            }
            2 -> {
                chatPlayer(quiz, "What is the statue of?")
                chatNpc(angry, "It's of Marimbo, you cretin!")
                chatPlayer(shifty, "Ah yes. How stupid of me not to see the likeness.")
            }
            3 -> {
                chatPlayer(neutral, "I'll be back later.")
                chatNpc(shifty, "I wouldn't count on it.")
                chatPlayer(shocked, "What!?")
                chatNpc(shifty, "Oh, nothing.")
            }
        }
    }

    private companion object {
        const val JUMAANE = "npc.mm2_monkey_banker_model"
        const val OOBAPOHK = "npc.mm2_javelinstoreowner"
        const val JAVELIN_SHOP = "Oobapohk's Javelin Store"
        const val JAVELIN_INV = "inv.mm2_javelin_store"
        const val MONKEY_MADNESS_II = "quest_monkeymadness2"
    }
}
