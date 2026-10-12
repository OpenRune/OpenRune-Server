package org.rsmod.content.areas.city.ardougne.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Silk_merchant
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [skill:thieving] overhead lines and refusing silk for 10 minutes after a theft
// TODO(content): [diary:ardougne] easy task for selling silk for 60 coins
class SilkMerchant @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.silk_merchant_ardougne") { startDialogue(it.npc) { silkMerchant() } }
    }

    private suspend fun Dialogue.silkMerchant() {
        if (count(SILK) > 0) {
            haggle()
            return
        }
        chatNpc(neutral, "I buy silk. If you ever want to sell any silk, bring it here.")
        val noted = count(NOTED_SILK)
        if (noted == 0) {
            return
        }
        chatPlayer(quiz, "I've got some silk here. Will you buy it?")
        chatNpc(confused, "Silk? You're not carrying any silk.")
        chatPlayer(neutral, "Yes I am.")
        chatNpc(
            neutral,
            "No, you're carrying $noted bits of paper that say they can be exchanged for silk at " +
                "a bank or general store. I'm not buying those. Fetch me some real silk, then " +
                "we'll trade.",
        )
        chatPlayer(sad, "Oh, alright.")
    }

    private suspend fun Dialogue.haggle() {
        chatPlayer(happy, "Hello. I have some fine silk from far away to sell to you.")
        chatNpc(
            neutral,
            "Ah I may be interested in that. What sort of price were you looking at per piece " +
                "of silk?",
        )
        when (choice4("20 coins.", 20, "80 coins.", 80, "120 coins.", 120, "200 coins.", 200)) {
            20 -> {
                chatPlayer(neutral, "20 coins.")
                chatNpc(happy, "Ok suits me.")
                sellSilk(20)
            }
            80 -> {
                chatPlayer(neutral, "80 coins.")
                chatNpc(shocked, "80 coins? That's a bit steep! How about 40 coins?")
                val price =
                    choice3(
                        "Ok, 40 sounds good.",
                        40,
                        "50, and that's my final price.",
                        50,
                        "No, that is not enough.",
                        0,
                    )
                when (price) {
                    40 -> {
                        chatPlayer(happy, "Ok, 40 sounds good.")
                        sellSilk(40)
                    }
                    50 -> {
                        chatPlayer(neutral, "50, and that's my final price.")
                        chatNpc(neutral, "Done.")
                        sellSilk(50)
                    }
                    else -> chatPlayer(angry, "No, that is not enough.")
                }
            }
            120 -> {
                chatPlayer(neutral, "120 coins.")
                chatNpc(
                    neutral,
                    "You'll never get that much for it. I'll be generous and give you 50 for it.",
                )
                val price =
                    choice3(
                        "Ok, I guess 50 will do.",
                        50,
                        "I'll give it to you for 60.",
                        60,
                        "No, that is not enough.",
                        0,
                    )
                when (price) {
                    50 -> {
                        chatPlayer(neutral, "Ok, I guess 50 will do.")
                        sellSilk(50)
                    }
                    60 -> {
                        chatPlayer(neutral, "I'll give it to you for 60.")
                        chatNpc(
                            neutral,
                            "You drive a hard bargain, but I guess that will have to do.",
                        )
                        sellSilk(60)
                    }
                    else -> chatPlayer(angry, "No, that is not enough.")
                }
            }
            200 -> {
                chatPlayer(neutral, "200 coins.")
                chatNpc(
                    angry,
                    "Don't be ridiculous! That is far too much. You insult me with that price.",
                )
            }
        }
    }

    private fun Dialogue.sellSilk(price: Int) {
        val silk = count(SILK)
        if (silk == 0) {
            return
        }
        val taken = player.invDel(player.inv, SILK, count = silk, ignoreVirtualStorage = true)
        if (!taken.success) {
            return
        }
        player.invAddOrDrop(objRepo, COINS, silk * price)
    }

    private fun Dialogue.count(obj: String): Int = player.inv.physicalCount(obj)

    private companion object {
        const val SILK = "obj.silk"
        const val NOTED_SILK = "obj.cert_silk"
        const val COINS = "obj.coins"
    }
}
