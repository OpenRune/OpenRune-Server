package org.rsmod.content.areas.city.prifddinas.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Anwen
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [skill:thieving] "You're the one who stole something from me!" after stealing
class Anwen @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.prif_silk") { startDialogue(it.npc) { anwen() } }
    }

    private suspend fun Dialogue.anwen() {
        when {
            player.inv.physicalCount(SILK) > 0 -> haggle()
            player.inv.physicalCount(NOTED_SILK) > 0 -> notedSilk()
            else -> chatNpc(happy, "I buy silk. If you ever want to sell any silk, bring it here.")
        }
    }

    private suspend fun Dialogue.haggle() {
        chatPlayer(happy, "Hello. I have some fine silk from far away to sell to you.")
        chatNpc(
            neutral,
            "Ah I may be interested in that. What sort of price were you looking at per piece of " +
                "silk?",
        )
        when (choice4("20 coins.", 1, "80 coins.", 2, "120 coins.", 3, "200 coins.", 4)) {
            1 -> {
                chatPlayer(neutral, "20 coins.")
                chatNpc(happy, "Ok suits me.")
                sell(20)
            }
            2 -> {
                chatPlayer(neutral, "80 coins.")
                chatNpc(shocked, "80 coins? That's a bit steep! How about 40 coins?")
                val option =
                    choice3(
                        "Ok, 40 sounds good.",
                        1,
                        "50, and that's my final price.",
                        2,
                        "No, that is not enough.",
                        3,
                    )
                when (option) {
                    1 -> {
                        chatPlayer(happy, "Ok, 40 sounds good.")
                        sell(40)
                    }
                    2 -> {
                        chatPlayer(neutral, "50, and that's my final price.")
                        chatNpc(neutral, "Done.")
                        sell(50)
                    }
                    3 -> chatPlayer(angry, "No, that is not enough.")
                }
            }
            3 -> {
                chatPlayer(neutral, "120 coins.")
                chatNpc(
                    neutral,
                    "You'll never get that much for it. I'll be generous and give you 50 for it.",
                )
                val option =
                    choice3(
                        "Ok, I guess 50 will do.",
                        1,
                        "I'll give it to you for 60.",
                        2,
                        "No, that is not enough.",
                        3,
                    )
                when (option) {
                    1 -> {
                        chatPlayer(neutral, "Ok, I guess 50 will do.")
                        sell(50)
                    }
                    2 -> {
                        chatPlayer(neutral, "I'll give it to you for 60.")
                        chatNpc(
                            neutral,
                            "You drive a hard bargain, but I guess that will have to do.",
                        )
                        sell(60)
                    }
                    3 -> chatPlayer(angry, "No, that is not enough.")
                }
            }
            4 -> {
                chatPlayer(neutral, "200 coins.")
                chatNpc(
                    angry,
                    "Don't be ridiculous! That is far too much. You insult me with that price.",
                )
            }
        }
    }

    private suspend fun Dialogue.notedSilk() {
        val notes = player.inv.physicalCount(NOTED_SILK)
        chatNpc(happy, "I buy silk. If you ever want to sell any silk, bring it here.")
        chatPlayer(quiz, "I've got some silk here. Will you buy it?")
        chatNpc(confused, "Silk? You're not carrying any silk.")
        chatPlayer(confused, "Yes I am.")
        chatNpc(
            neutral,
            "No, you're carrying $notes bits of paper that say they can be exchanged for silk " +
                "at a bank or general store. I'm not buying those. Fetch me some real silk, " +
                "then we'll trade.",
        )
        chatPlayer(sad, "Oh, alright.")
    }

    private fun Dialogue.sell(price: Int) {
        if (player.invDel(player.inv, SILK, ignoreVirtualStorage = true).success) {
            player.invAddOrDrop(objRepo, COINS, price)
        }
    }

    private companion object {
        const val SILK = "obj.silk"
        const val NOTED_SILK = "obj.cert_silk"
        const val COINS = "obj.coins"
    }
}
