package org.rsmod.content.skills.agility

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.enums.AgilityPyramidEnums.agility_pyramid_artefact_prices
import org.rsmod.api.enums.AgilityPyramidEnums.agility_pyramid_sceptre_offers
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Simon Templeton (https://oldschool.runescape.wiki/w/Simon_Templeton) buys pyramid tops for 10,000
 * coins each and Pyramid Plunder artefacts at his own prices: the lot through his chat, noted ones
 * too once the easy Desert diary is done, or one at a time when used on him. He calls a pharaoh's
 * sceptre a fake worth 100 coins, which the player always turns down.
 */
class SimonTempletonScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(SIMON) { startDialogue(it.npc) { talk() } }
        onOpNpcU(SIMON) {
            val obj = it.objType
            when {
                obj.id == GOLD_PYRAMID.asRSCM(RSCMType.OBJ) -> startDialogue(it.npc) { handOver() }
                obj.id in SCEPTRE_OFFERS ->
                    startDialogue(it.npc) { sceptre(obj, sellOthers = false) }
                obj.id in PRICES -> startDialogue(it.npc) { offerArtefact(obj) }
                // UNVERIFIED: the wiki says only that noted artefacts used on him do not work.
                else -> mes("Nothing interesting happens.")
            }
        }
    }

    private suspend fun Dialogue.talk() {
        when {
            vars[JOB] == 0 -> introduce()
            access.artefactLots().isNotEmpty() -> offerArtefacts()
            vars[JOB_DONE] == 0 -> {
                chatPlayer(happy, "Hello, Simon.")
                chatNpc(happy, "G'day, ${player.displayName}.")
                chatPlayer(quiz, "How's it going?")
                chatNpc(happy, "Not bad, mate, not bad at all. Got any artefacts for me?")
                offerTops {
                    chatPlayer(sad, "No, I haven't.")
                    chatNpc(
                        neutral,
                        "Try harder, mate! It won't be long till some other critter turns up " +
                            "and snatches them out from under your eyes.",
                    )
                    chatPlayer(neutral, "I will, goodbye.")
                    chatNpc(happy, "Bye, cobber.")
                }
            }
            else -> {
                chatNpc(happy, "G'day, mate. Got any new pyramid artefacts for me?")
                offerTops {
                    chatPlayer(sad, "No, I haven't.")
                    chatNpc(neutral, "Well, keep my offer in mind if you do find one.")
                    chatPlayer(neutral, "I will. Goodbye, Simon.")
                    chatNpc(happy, "Bye, mate.")
                }
            }
        }
    }

    private suspend fun Dialogue.introduce() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "G'day, mate. I'm Simon - Simon Templeton. Who are you?")
        vars[NAMED] = 1
        chatPlayer(happy, "I'm ${player.displayName}. What are you doing all the way out here?")
        chatNpc(
            neutral,
            "I've been contracted to retrieve artefacts from the top of this magnificent pyramid.",
        )
        chatPlayer(quiz, "So, why are you down here and not up there then?")
        chatNpc(
            sad,
            "Well, it's me back - an old injury I picked up in Sophanem has come back to haunt " +
                "me. I was working for the Museum of Varrock, and the gold I had been given " +
                "wasn't enough to pay for the carpet rides, let alone any decent equipment. But " +
                "that was a long time ago... I can't even lift a chisel right now. There's no " +
                "way I can climb all the way up there.",
        )
        chatPlayer(quiz, "How on earth did you get down the cliff face, then?")
        chatNpc(
            sad,
            "That's what set off the injury. I was fine before I scrabbled down that pesky " +
                "slope. I think it will be a while before I can climb back up again; I must be " +
                "getting old.",
        )
        chatNpc(
            neutral,
            "Hang on a second... You look like you're pretty agile and up for a challenge. How " +
                "about you retrieve it for me?",
        )
        vars[JOB] = 1
        chatNpc(
            happy,
            "I'll tell you what, I'll give you 10000 coins for every artefact you get for me.",
        )
        chatPlayer(happy, "I'll see what I can do.")
        chatNpc(happy, "Good on ya, mate!")
    }

    // UNVERIFIED: the mesanims from here on.
    private suspend fun Dialogue.offerTops(none: suspend Dialogue.() -> Unit) {
        val tops = player.inv.count(GOLD_PYRAMID)
        if (tops == 0) {
            none()
            return
        }
        val sell =
            if (tops == 1) {
                choice2("Sell it.", true, "Keep it.", false)
            } else {
                choice2("Sell them.", true, "Keep them.", false)
            }
        if (sell) {
            sellTops()
            return
        }
        if (tops == 1) {
            chatPlayer(neutral, "I have one, but I want to keep it for now.")
        } else {
            chatPlayer(neutral, "I have some, but I want to keep them for now.")
        }
        chatNpc(neutral, "Careful, mate, people might come looking for a thing like that!")
        chatPlayer(neutral, "Thanks for the advice, but I'll hang onto it for now.")
        chatNpc(happy, "Bye, cobber.")
    }

    private suspend fun Dialogue.handOver() {
        if (vars[JOB] == 0) {
            askForChat()
            return
        }
        sellTops()
    }

    private suspend fun Dialogue.sellTops() {
        val paid = access.sellTops() ?: return
        objbox(GOLD_PYRAMID, "You hand over the artefact(s) and Simon hands you $paid coins.")
        chatNpc(
            happy,
            "Ripper! Thanks a bundle, mate! Thanks to you I can fulfill me contract. You're a " +
                "true blue! The boss will be pleased.",
        )
        chatPlayer(
            quiz,
            "Glad I was able to help... but who is your boss? I thought you worked for the " +
                "museum?",
        )
        chatNpc(neutral, "Mind your own bizzo, mate. But if you get any more, you know where I am!")
    }

    // UNVERIFIED: that artefacts come before tops when the player carries both.
    private suspend fun Dialogue.offerArtefacts() {
        chatPlayer(neutral, "I have some interesting artefacts I'd like you to look at.")
        chatNpc(happy, "Bonzer! Let's have a Butcher's mate.")
        chatNpc(quiz, "Do you want to flog the lot of 'em?")
        if (!choice2("Yes, show me the money.", true, "No, let me think about it.", false)) {
            chatPlayer(neutral, "No, let me think about it.")
            chatNpc(
                neutral,
                "No probs mate. Just give me the items you want to sell and we can do them one " +
                    "by one.",
            )
            return
        }
        chatPlayer(happy, "Yes, show me the money.")
        val sceptre = SCEPTRES.firstOrNull { player.inv.count(it.internalName) > 0 }
        if (sceptre != null && !sceptre(sceptre, sellOthers = true)) {
            return
        }
        if (access.sellArtefacts()) {
            chatNpc(happy, "Fair Dinkum mate, Bonzer doing business with you mate.")
        }
    }

    private suspend fun Dialogue.sceptre(sceptre: ItemServerType, sellOthers: Boolean): Boolean {
        val offer = SCEPTRE_OFFERS.getValue(sceptre.id)
        if (!sellOthers) {
            chatPlayer(neutral, "I have some interesting artefacts I'd like you to look at.")
        }
        chatNpc(
            neutral,
            "You sellin' me this gold coloured stick thing. Looks fake to me. I'll give you " +
                "$offer gold for it.",
        )
        chatPlayer(
            angry,
            "What! This is a genuine pharaoh's sceptre - made out of solid gold and finely " +
                "jewelled with precious gems by the finest craftsmen in the area.",
        )
        chatNpc(
            laugh,
            "Strewth! I can tell a pile of croc when I hear it! You've got the patter mate, but " +
                "I'm no mug. That's a fake.",
        )
        chatPlayer(angry, "It has magical powers!")
        chatNpc(
            neutral,
            "Oh, magical powers ... yeah, yeah, yeah. Heard it all before mate. I'll give you a " +
                "$offer gold, or some 'magic beans'.",
        )
        chatNpc(neutral, "Take it or leave it.")
        chatPlayer(angry, "I don't think so! I'll find someone who'll give me what it's worth.")
        if (!sellOthers) {
            chatNpc(neutral, "Suit yerself..")
            return false
        }
        chatNpc(quiz, "Suit yerself.. You wanna sell the other artefacts you have?")
        val sell =
            choice2(
                "Yes, as long as I get a decent price for them.",
                true,
                "Nah, I've changed my mind.",
                false,
            )
        if (sell) {
            chatPlayer(neutral, "Yes, as long as I get a decent price for them.")
        } else {
            chatPlayer(neutral, "Nah, I've changed my mind.")
        }
        return sell
    }

    private suspend fun Dialogue.offerArtefact(artefact: ItemServerType) {
        if (vars[JOB] == 0) {
            askForChat()
            return
        }
        chatNpc(quiz, "You wanna sell that mate?")
        if (!choice2("Yes, show me the money.", true, "No, I think I'll keep it.", false)) {
            chatPlayer(neutral, "No, I think I'll keep it.")
            return
        }
        chatPlayer(happy, "Yes, show me the money.")
        if (access.sellArtefact(artefact)) {
            chatNpc(happy, "Cheers mate!")
        }
    }

    private suspend fun Dialogue.askForChat() {
        chatNpc(
            happy,
            "That's a nice looking artefact you got there mate. Gimme a chat and lemme explain " +
                "what these are all about first!",
        )
    }

    private class Lot(val obj: ItemServerType, val count: Int, val price: Int)

    private fun ProtectedAccess.artefactLots(): List<Lot> {
        val noted = vars[EASY_DESERT_DIARY] != 0
        return PRICED.flatMap { (artefact, price) ->
            val forms = listOfNotNull(artefact, if (noted) ocCert(artefact.internalName) else null)
            forms.distinctBy { it.id }.mapNotNull { form ->
                val count = inv.count(form.internalName)
                if (count > 0) Lot(form, count, price) else null
            }
        }
    }

    private fun ProtectedAccess.sellArtefacts(): Boolean = sell(artefactLots())

    private fun ProtectedAccess.sellArtefact(artefact: ItemServerType): Boolean =
        sell(listOf(Lot(artefact, 1, PRICES.getValue(artefact.id))))

    private fun ProtectedAccess.sell(lots: List<Lot>): Boolean {
        if (lots.isEmpty() || lots.any { inv.count(it.obj.internalName) < it.count }) {
            return false
        }
        val paid = lots.sumOf { it.count.toLong() * it.price }
        val sold =
            paid <= Int.MAX_VALUE &&
                player
                    .invTransaction(inv) {
                        val target = select(inv)
                        for (lot in lots) {
                            delete(target, lot.obj.id, lot.count)
                        }
                        add(target, COINS.asRSCM(RSCMType.OBJ), paid.toInt())
                    }
                    .success
        if (!sold) {
            // UNVERIFIED: the wording when the coins don't fit.
            mes("You don't have enough inventory space.")
        }
        return sold
    }

    private fun ProtectedAccess.sellTops(): Int? {
        val tops = inv.count(GOLD_PYRAMID)
        if (tops == 0) {
            return null
        }
        val paid = tops * PRICE
        val sold =
            player
                .invTransaction(inv) {
                    val target = select(inv)
                    delete(target, GOLD_PYRAMID.asRSCM(RSCMType.OBJ), tops)
                    add(target, COINS.asRSCM(RSCMType.OBJ), paid)
                }
                .success
        if (!sold) {
            // UNVERIFIED: the wording when the coins don't fit.
            mes("You don't have enough inventory space.")
            return null
        }
        vars[JOB_DONE] = 1
        return paid
    }

    private companion object {
        const val SIMON = "npc.agility_pyramid_simon"
        const val GOLD_PYRAMID = "obj.agility_pyramid_gold_pyramid"
        const val COINS = "obj.coins"
        const val PRICE = 10_000
        const val NAMED = "varbit.agility_pyramid_simon_named"
        const val JOB = "varbit.agility_pyramid_simon_job"
        const val JOB_DONE = "varbit.agility_pyramid_simon_job_done"
        const val EASY_DESERT_DIARY = "varbit.desert_diary_easy_complete"

        val PRICED: List<Pair<ItemServerType, Int>> by lazy {
            agility_pyramid_artefact_prices.backing.map { (obj, price) ->
                obj to checkNotNull(price)
            }
        }
        val PRICES: Map<Int, Int> by lazy { PRICED.associate { (obj, price) -> obj.id to price } }
        val SCEPTRES: List<ItemServerType> by lazy { agility_pyramid_sceptre_offers.keys.toList() }
        val SCEPTRE_OFFERS: Map<Int, Int> by lazy {
            agility_pyramid_sceptre_offers.backing.entries.associate { (obj, offer) ->
                obj.id to checkNotNull(offer)
            }
        }
    }
}
