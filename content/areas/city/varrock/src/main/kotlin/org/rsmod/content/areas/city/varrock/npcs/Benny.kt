package org.rsmod.content.areas.city.varrock.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Benny
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:npc_overhead] random "Read all about it!" shouts
class Benny @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(BENNY) { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            val option =
                choice4(
                    "Can I have a newspaper, please?",
                    1,
                    "How much does a paper cost?",
                    2,
                    "Varrock Herald? Never heard of it.",
                    3,
                    "Anything interesting in there?",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Can I have a newspaper, please?")
                    chatNpc(happy, "Certainly, Guv. That'll be 50 coins, please.")
                }
                2 -> {
                    chatPlayer(quiz, "How much does a paper cost?")
                    chatNpc(happy, "Just 50 coins! An absolute bargain! Want one?")
                }
                3 -> {
                    chatPlayer(confused, "Varrock Herald? Never heard of it.")
                    chatNpc(
                        happy,
                        "For the illiterate amongst us, I shall elucidate. The Varrock Herald is " +
                            "a new newspaper. It is edited, printed and published by myself, " +
                            "Benny Gutenberg, and each edition promises to enthrall the reader " +
                            "with",
                    )
                    chatNpc(
                        happy,
                        "captivating material! Now, can I interest you in buying one for a mere " +
                            "50 coins?",
                    )
                }
                4 -> {
                    chatPlayer(quiz, "Anything interesting in there?")
                    chatNpc(
                        happy,
                        "Of course there is, mate. Packed full of thought provoking insights, " +
                            "contentious interviews and celebrity scandalmongering! An excellent " +
                            "read and all for just 50 coins! Want one?",
                    )
                }
            }
            offerPaper()
        }

    private suspend fun Dialogue.offerPaper() {
        if (access.invTotal(access.inv, COINS) < PRICE) {
            chatPlayer(sad, "Maybe when I've got more money on me.")
            chatNpc(neutral, "Well, no cash, no paper. Live in ignorance.")
            return
        }
        when (choice2("Sure, here you go...", 1, "No, thanks.", 2)) {
            1 -> {
                chatPlayer(happy, "Sure, here you go...")
                if (player.invTakeFee(PRICE)) {
                    player.invAddOrDrop(objRepo, NEWSPAPER)
                }
            }
            2 -> chatNpc(neutral, "Okay, suit yourself. Plenty more fish in the sea.")
        }
    }

    private companion object {
        const val BENNY = "npc.qip_soa_newspaperseller"
        const val COINS = "obj.coins"
        const val NEWSPAPER = "obj.qip_soa_newspaper2"
        const val PRICE = 50
    }
}
