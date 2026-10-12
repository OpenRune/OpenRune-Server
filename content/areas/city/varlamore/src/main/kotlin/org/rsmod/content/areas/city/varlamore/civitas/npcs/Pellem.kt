package org.rsmod.content.areas.city.varlamore.civitas.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Pellem
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:custom_fur_clothing] the Custom Fur Clothing interface (Fur-clothing op
//  and "Can you do much with animal furs?")
class Pellem @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(PELLEM) { startDialogue(it.npc) { pellem(it.npc) } }
        onOpNpc3(PELLEM) { trade(it.npc) }
    }

    private suspend fun ProtectedAccess.trade(npc: Npc) {
        if (!hasGuildHunterLevel()) {
            return startDialogue(npc) { refuseUnqualifiedHunter() }
        }
        openFurStore(npc)
    }

    private fun ProtectedAccess.openFurStore(npc: Npc) {
        shops.open(player, npc, "Pellem's Fur Store", "inv.fur_shop_guild")
    }

    private suspend fun Dialogue.pellem(npc: Npc) {
        if (!hasGuildHunterLevel()) {
            return refuseUnqualifiedHunter()
        }
        chatNpc(happy, "Hi there. How can I help you today?")
        while (true) {
            val option =
                choice5(
                    "What do you do here?",
                    1,
                    "Do you sell anything?",
                    2,
                    "Can you do much with animal furs?",
                    3,
                    "Can you make some armour for me?",
                    4,
                    "Never mind.",
                    5,
                )
            when (option) {
                1 -> whatDoYouDo()
                2 -> {
                    chatPlayer(quiz, "Do you sell anything?")
                    chatNpc(happy, "Why yes I do. Here's what I have available.")
                    access.openFurStore(npc)
                    return
                }
                3 -> {
                    chatPlayer(quiz, "Can you do much with animal furs?")
                    chatNpc(happy, "Of course! Why don't you take a look?")
                    return
                }
                4 -> return makeArmour()
                else -> {
                    chatPlayer(neutral, "Never mind.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.whatDoYouDo() {
        chatPlayer(quiz, "What do you do here?")
        chatNpc(happy, "I'm just a fur trader. I come across from the city most days.")
        chatPlayer(quiz, "Are you a hunter too?")
        chatNpc(laugh, "Oh no, that life isn't for me. I'm just a tailor.")
        chatNpc(quiz, "What's brought you out here anyway?")
        when (choice2("I'm just exploring.", 1, "I'm here to work with the guild.", 2)) {
            1 -> {
                chatPlayer(happy, "I'm just exploring.")
                chatNpc(happy, "Oh, enjoy yourself then!")
            }
            2 -> {
                chatPlayer(happy, "I'm here to work with the guild.")
                chatNpc(
                    happy,
                    "That's exciting! I expect I'll be turning some furs into clothes for you " +
                        "soon then?",
                )
                chatPlayer(neutral, "That depends on how much I manage to catch.")
            }
        }
    }

    private suspend fun Dialogue.makeArmour() {
        chatPlayer(quiz, "Can you make some armour for me?")
        chatNpc(happy, "I can indeed. What would you like?")
        val piece =
            choice5(
                "A mixed hide top.",
                TOP,
                "Some mixed hide legs.",
                LEGS,
                "Some mixed hide boots.",
                BOOTS,
                "A mixed hide cape.",
                CAPE,
                "Nothing.",
                null,
            )
        if (piece == null) {
            chatPlayer(neutral, "Nothing.")
            chatNpc(neutral, "Fair enough.")
            return
        }
        chatPlayer(happy, piece.request)
        if (!exchange(piece)) {
            chatNpc(
                neutral,
                "You don't have the required items for me to make ${piece.article} for you. " +
                    "You need ${piece.missing}",
            )
            return
        }
        chatNpc(happy, "Here you go. Enjoy it.")
        objbox(piece.obj, piece.traded)
    }

    private fun Dialogue.exchange(piece: MixedHide): Boolean {
        val inventory = player.inv
        val result =
            player.invTransaction(inventory) {
                val inv = select(inventory)
                delete {
                    from = inv
                    obj = COINS.asRSCM(RSCMType.OBJ)
                    strictCount = piece.coins
                }
                delete {
                    from = inv
                    obj = piece.fur.asRSCM(RSCMType.OBJ)
                    strictCount = piece.furCount
                }
                insert {
                    into = inv
                    obj = piece.obj.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        return result.success
    }

    private class MixedHide(
        val obj: String,
        val fur: String,
        val furCount: Int,
        val coins: Int,
        val request: String,
        val article: String,
        val missing: String,
        val traded: String,
    )

    private companion object {
        const val PELLEM = "npc.hg_fur_trader"
        const val COINS = "obj.coins"

        // https://oldschool.runescape.wiki/w/Mixed_hide_armour: Pellem's costs.
        val TOP =
            MixedHide(
                obj = "obj.hide_top",
                fur = "obj.hunting_antelopesun_fur",
                furCount = 2,
                coins = 20_000,
                request = "A mixed hide top.",
                article = "a mixed hide top",
                missing = "20,000 coins and two sunlight antelope fur.",
                traded =
                    "You trade 20,000 coins and two sunlight antelope fur for a mixed hide top.",
            )
        val LEGS =
            MixedHide(
                obj = "obj.hide_legs",
                fur = "obj.hunting_fennecfox_fur",
                furCount = 3,
                coins = 20_000,
                request = "Some mixed hide legs.",
                article = "some mixed hide legs",
                missing = "20,000 coins and three fox fur.",
                traded = "You trade 20,000 coins and three fox fur for some mixed hide legs.",
            )
        val BOOTS =
            MixedHide(
                obj = "obj.hide_boots",
                fur = "obj.hunting_antelopesun_fur",
                furCount = 1,
                coins = 17_500,
                request = "Some mixed hide boots.",
                article = "some mixed hide boots",
                missing = "17,500 coins and one sunlight antelope fur",
                traded =
                    "You trade 17,500 coins and one sunlight antelope fur for some mixed hide " +
                        "boots.",
            )
        val CAPE =
            MixedHide(
                obj = "obj.hide_cape",
                fur = "obj.varlamore_jaguar_fur",
                furCount = 1,
                coins = 20_000,
                request = "A mixed hide cape.",
                article = "a mixed hide cape",
                missing = "20,000 coins and one jaguar fur.",
                traded = "You trade 20,000 coins and one jaguar fur for a mixed hide cape.",
            )
    }
}
