package org.rsmod.content.areas.city.desert.nardah.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Rokuh
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class RoksChocsBox @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(ROKUH) { startDialogue(it.npc) { rokuh(it.npc) } }
        onOpNpc3(ROKUH) { player.openStore(it.npc) }
    }

    private fun Player.openStore(npc: Npc) {
        shops.open(this, npc, "Rok's Chocs Box.", "inv.chociceshopnardah")
    }

    private suspend fun Dialogue.rokuh(npc: Npc) {
        chatNpc(
            happy,
            "Come one, come all, buy my amazing choc ice invention here! Chocolate on the " +
                "outside. Iced cream on the inside. Oh I also have some chocolate left over from " +
                "making choc ices which I'm selling too.",
        )
        val option =
            when {
                QuestRequirements.hasCompleted(player, SPIRITS_OF_THE_ELID) ->
                    choice4(
                        "Cool I'd like to buy some.",
                        1,
                        "No thanks, I'm not interested.",
                        2,
                        "How do you stop your icy snacks melting?",
                        3,
                        "How's business, now that the curse has gone?",
                        4,
                    )
                // https://oldschool.runescape.wiki/w/Transcript:Spirits_of_the_Elid
                QuestRequirements.isOnQuest(player, SPIRITS_OF_THE_ELID) ->
                    choice4(
                        "Cool I'd like to buy some.",
                        1,
                        "No thanks, I'm not interested.",
                        2,
                        "How do you stop your icy snacks melting?",
                        3,
                        "I am trying to find out the cause of this town's curse.",
                        5,
                    )
                else ->
                    choice3(
                        "Cool I'd like to buy some.",
                        1,
                        "No thanks, I'm not interested.",
                        2,
                        "How do you stop your icy snacks melting?",
                        3,
                    )
            }
        when (option) {
            4 -> {
                chatPlayer(quiz, "How's business, now that the curse has gone?")
                chatNpc(
                    happy,
                    "Ohh as good as ever. The people of Nardah seem to have taken well to the " +
                        "taste of chocolate and iced cream combined and, with the curse gone, all " +
                        "my friends are much happier too, so it's all good.",
                )
            }
            5 -> {
                chatPlayer(quiz, "I am trying to find out the cause of this town's curse.")
                chatNpc(
                    sad,
                    "I really have no idea. It's most regrettable. Fortunately my innovation with " +
                        "my choc ice invention is helping people stay at least slightly cool.",
                )
            }
            1 -> player.openStore(npc)
            2 -> chatPlayer(neutral, "No thanks, I'm not interested.")
            3 -> {
                chatPlayer(
                    quiz,
                    "How do you stop your icy snacks melting? The middle of the desert is the " +
                        "last place I'd expect to see ice.",
                )
                chatNpc(
                    happy,
                    "It's quite a surprise, isn't it, my friend. I actually have this special " +
                        "magic box of ice, which I bought for a princely sum, from a strange man " +
                        "while adventuring in lands far away. He said it is imbued with some sort " +
                        "of",
                )
                chatNpc(
                    happy,
                    "powerful ice magic which keeps it cold all the time. I had never seen any " +
                        "ice magic before, it's clearly very rare and powerful, so I felt I had " +
                        "to buy it.",
                )
            }
        }
    }

    private companion object {
        const val ROKUH = "npc.elid_choc_ice_seller"
        const val SPIRITS_OF_THE_ELID = "quest_spiritsoftheelid"
    }
}
