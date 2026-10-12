package org.rsmod.content.areas.city.desert.sophanem.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SophanemBankGuards @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.contact_sophanem_guard_female_01") { startDialogue(it.npc) { bankGuard() } }
        onOpNpc1("npc.contact_sophanem_guard_male_01") { startDialogue(it.npc) { bankGuard() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sophanem_Guard
    private suspend fun Dialogue.bankGuard() {
        chatNpc(neutral, "How can I help you?")
        while (true) {
            val option =
                choice5(
                    "What are you doing down here?",
                    1,
                    "Do you have a tinderbox?",
                    2,
                    "Do you have a torch?",
                    3,
                    "How come you're not spotty?",
                    4,
                    "You can't. I'd better go.",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What are you doing down here?")
                    chatNpc(
                        neutral,
                        "We're here to guard the bank and make sure no one tries anything.",
                    )
                }
                2 -> {
                    tinderbox()
                    return
                }
                3 -> {
                    torch()
                    return
                }
                4 -> spotty()
                else -> {
                    chatPlayer(neutral, "You can't. I'd better go.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.tinderbox() {
        chatPlayer(quiz, "Do you have a tinderbox?")
        if (player.inv.count(TINDERBOX) > 0) {
            chatNpc(neutral, "I do, but I'd like to hold on to it for now.")
            chatPlayer(neutral, "That's okay; I've one with me anyway.")
            return
        }
        chatNpc(neutral, "I have one I could sell you for 50 coins.")
        val buy = choice2("I'll take it.", true, "Sorry, that's too expensive.", false)
        if (!buy) {
            tooExpensive()
            return
        }
        chatPlayer(happy, "I'll take it.")
        if (!player.invTakeFee(TINDERBOX_PRICE)) {
            chatPlayer(sad, "Actually, I don't seem to have enough money on me.")
            return
        }
        player.invAddOrDrop(objRepo, TINDERBOX)
        doubleobjbox(
            TINDERBOX,
            COINS_250,
            "The guard hands you a tinderbox in exchange for 50 coins.",
        )
    }

    private suspend fun Dialogue.torch() {
        chatPlayer(quiz, "Do you have a torch?")
        chatNpc(neutral, "I'll sell you an unlit one for 200 coins.")
        val buy = choice2("I'll take it.", true, "Sorry, that's too expensive.", false)
        if (!buy) {
            tooExpensive()
            return
        }
        chatPlayer(happy, "I'll take it.")
        if (!player.invTakeFee(TORCH_PRICE)) {
            chatPlayer(sad, "Actually, I don't seem to have enough money on me.")
            return
        }
        player.invAddOrDrop(objRepo, UNLIT_TORCH)
        doubleobjbox(
            UNLIT_TORCH,
            COINS_250,
            "The guard hands you an unlit torch in exchange for 200 coins.",
        )
    }

    private suspend fun Dialogue.tooExpensive() {
        chatPlayer(neutral, "Sorry, that's too expensive.")
        chatNpc(neutral, "No problem. Come back if you change your mind.")
    }

    private suspend fun Dialogue.spotty() {
        chatPlayer(quiz, "How come you're not spotty?")
        chatNpc(
            neutral,
            "Ah, the pox. We weren't here when the outbreak took place. We were out of town " +
                "trying to catch an antiques thief called Simon Templeton. We've only just got " +
                "back.",
        )
        chatPlayer(worried, "Aren't you afraid you'll fall ill as well?")
        chatNpc(
            neutral,
            "The only reason we've been let back into the city is because the High Priest " +
                "believes the pox isn't contagious.",
        )
        chatNpc(
            neutral,
            "Besides, Sophanem is my home. It will take a lot more than the threat of a few " +
                "spots to keep me away.",
        )
    }

    private companion object {
        const val TINDERBOX = "obj.tinderbox"
        const val UNLIT_TORCH = "obj.torch_unlit"
        const val COINS_250 = "obj.coins_250"
        const val TINDERBOX_PRICE = 50
        const val TORCH_PRICE = 200
    }
}
