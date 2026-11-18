package org.rsmod.content.areas.misc.fossilunderwater

import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.shops.Shops
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Mairin, the mermaid behind the plant door: her talk, Mairin's Market for mermaid's tears, the
 * training she gives for glistening tears, and the pile of fishbowls she makes divers leave.
 */
class MairinScript @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(MAIRIN) { talk(it.npc) }
        onOpNpc3(MAIRIN) { openShop() }
        onOpNpc4(MAIRIN) { openTraining() }
        onIfModalButton(TRAIN_AGILITY) { train(Training.Agility, it.op) }
        onIfModalButton(TRAIN_THIEVING) { train(Training.Thieving, it.op) }
        onIfModalButton(TRAIN_BOTH) { train(Training.Both, it.op) }
        onIfClose(TRAINING) {
            TRAINING_VARPS.forEach { VarPlayerIntMapSetter.set(player, it, 0) }
        }
        onOpLoc1(PILE_EMPTY) { depositBowl() }
        // UNVERIFIED: live's answer to picking up a bowl the player never left.
        onOpLoc2(PILE_EMPTY) { mes("You don't have a bowl on the pile.") }
        onOpLoc3(PILE_HOLDING) { takeBowl() }
        // UNVERIFIED: live's answer to leaving a second bowl.
        onOpLoc4(PILE_HOLDING) { mes("You already have a bowl on the pile.") }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        var trade = false
        startDialogue(npc) { trade = conversation() }
        if (trade) {
            openShop()
        }
    }

    /** Talks to Mairin; returns `true` when the player asks to trade. */
    private suspend fun Dialogue.conversation(): Boolean {
        chatPlayer(happy, "Hello.")
        if (FISHBOWL in player.worn) {
            chatNpc(mesanim(SCARED), "Oh! Take that horrid thing off your head immediately!")
            return false
        }
        if (FISHBOWL in player.inv) {
            bowlWarning()
        }
        var last: Topic? = null
        while (true) {
            val topic = chooseTopic(Topic.entries.filter { it != last })
            when (topic) {
                Topic.Who -> whoAreYou()
                Topic.Breathe -> howToBreathe()
                Topic.Help -> offerHelp()
                Topic.Trade -> {
                    chatPlayer(quiz, "What have you got to trade?")
                    return true
                }
                Topic.Bye -> {
                    chatPlayer(bored, "Bye.")
                    return false
                }
            }
            last = topic
        }
    }

    private suspend fun Dialogue.chooseTopic(topics: List<Topic>): Topic =
        if (topics.size == Topic.entries.size) {
            choice5(
                topics[0].text,
                topics[0],
                topics[1].text,
                topics[1],
                topics[2].text,
                topics[2],
                topics[3].text,
                topics[3],
                topics[4].text,
                topics[4],
            )
        } else {
            choice4(
                topics[0].text,
                topics[0],
                topics[1].text,
                topics[1],
                topics[2].text,
                topics[2],
                topics[3].text,
                topics[3],
            )
        }

    private suspend fun Dialogue.bowlWarning() {
        chatNpc(sad, "You're not going into deeper water with that bowl.")
        chatPlayer(quiz, "Why don't you like my helmet?")
        chatNpc(
            sad,
            "My sister was once trapped in one of those for a long time by your kind. It was " +
                "awful and she's still terrified of them. We don't allow them beyond this point " +
                "anymore.",
        )
        chatPlayer(quiz, "What shall I do with it?")
        chatNpc(neutral, "Put it on the pile over there.")
        chatPlayer(quiz, "But how will I breathe?")
        chatNpc(laugh, "Breathing is for pesky humans.")
        chatPlayer(angry, "Yes. I am one.")
        chatNpc(neutral, "Oops. Nevermind. I'm sure you'll manage. Just look for bubbles.")
    }

    private suspend fun Dialogue.whoAreYou() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(neutral, "I'm Mairin, one of the many merfolk here.")
        chatPlayer(quiz, "Many? I don't see that many...")
        chatNpc(happy, "Just because you don't see us, doesn't mean we're not there.")
        chatPlayer(confused, "That's kind of creepy. What do you actually do?")
        chatNpc(happy, "Mostly stop people entering beyond this wreck with those horrid fishbowls.")
    }

    private suspend fun Dialogue.howToBreathe() {
        chatPlayer(quiz, "How do I breathe underwater without a helmet.")
        chatNpc(happy, "Just find some bubbles, or other sources of air.")
        chatPlayer(quiz, "Other sources?")
        chatNpc(happy, "Yes, some fish can be a good source of air too.")
    }

    private suspend fun Dialogue.offerHelp() {
        chatPlayer(quiz, "Is there anything I can do to help you?")
        chatNpc(happy, "Yes! You can collect the tears of my poor trapped sister.")
        chatPlayer(quiz, "What? Tears? But, they're water, surely underwater they would...")
        chatNpc(
            angry,
            "She's a mermaid, like me. Didn't you know our tears are the pearls of the sea? You " +
                "humans.",
        )
        chatPlayer(neutral, "Ok, keep your scales on. Where would I find these tears?")
        chatNpc(neutral, "They usually end up secreted in clams or other places.")
        chatPlayer(quiz, "What do I get out of it?")
        chatNpc(
            neutral,
            "Well, I have access to a lot of old stuff that's fallen down under the water, so " +
                "I'm sure I could reward you in some way.",
        )
        chatNpc(
            sad,
            "If you find any special glistening tears, I can teach you some useful Agility and " +
                "Thieving techniques too. So will you help me?",
        )
        if (choice2("Yes, I'll help.", true, "Nah. Not for me.", false)) {
            chatPlayer(happy, "Yes, I'll help.")
        } else {
            chatPlayer(sad, "Nah. Not for me.")
        }
    }

    private fun ProtectedAccess.openShop() {
        shops.open(
            player = player,
            title = SHOP_TITLE,
            shopInv = SHOP,
            buyPercentage = SHOP_PERCENTAGE,
            sellPercentage = SHOP_PERCENTAGE,
            changePercentage = 0.0,
            currency = MERMAID_TEARS,
        )
    }

    private fun ProtectedAccess.openTraining() {
        showRates()
        ifOpenMainModal(TRAINING)
    }

    private fun ProtectedAccess.showRates() {
        val agility = statBase(AGILITY)
        val thieving = statBase(THIEVING)
        vars[RATE_THIEVING_BOTH] = MairinTraining.thievingTenths(thieving, both = true) / TENTHS
        vars[RATE_AGILITY_BOTH] = MairinTraining.agilityTenths(agility, both = true) / TENTHS
        vars[RATE_THIEVING] = MairinTraining.thievingTenths(thieving, both = false) / TENTHS
        vars[RATE_AGILITY] = MairinTraining.agilityTenths(agility, both = false) / TENTHS
    }

    private suspend fun ProtectedAccess.train(training: Training, op: IfButtonOp) {
        if (invTotal(inv, GLISTENING_TEAR) == 0) {
            mes("You don't have any glistening tears to exchange.")
            return
        }
        val requested =
            when (op) {
                IfButtonOp.Op1 -> 1
                // UNVERIFIED: the second op is taken to be Exchange-5; live is not captured.
                IfButtonOp.Op2 -> EXCHANGE_FIVE
                IfButtonOp.Op3 -> {
                    val carried = invTotal(inv, GLISTENING_TEAR)
                    countDialog("How many glistening tears do you wish to exchange? ($carried)")
                }
                IfButtonOp.Op4 -> invTotal(inv, GLISTENING_TEAR)
                else -> return
            }
        exchange(training, requested)
    }

    private fun ProtectedAccess.exchange(training: Training, requested: Int) {
        val carried = invTotal(inv, GLISTENING_TEAR)
        val taken = MairinTraining.takenTears(requested, carried)
        val paid = MairinTraining.paidTears(requested, carried)
        if (taken <= 0 || invDel(inv, GLISTENING_TEAR, taken).failure) {
            return
        }
        // UNVERIFIED: that the real levels, not boosted ones, set the rates.
        val both = training == Training.Both
        val agility = if (training == Training.Thieving) 0 else agilityTenths(both) * paid
        val thieving = if (training == Training.Agility) 0 else thievingTenths(both) * paid
        if (agility > 0) {
            statAdvance(AGILITY, agility / XP_TENTHS)
        }
        if (thieving > 0) {
            statAdvance(THIEVING, thieving / XP_TENTHS)
        }
        val tears = if (paid == 1) "one glistening tear" else "$paid glistening tears"
        val agilityXp = "${(agility / TENTHS).formatAmount} Agility XP"
        val thievingXp = "${(thieving / TENTHS).formatAmount} Thieving XP"
        val trained =
            when (training) {
                Training.Agility -> "Agility, exchanging $tears for $agilityXp"
                Training.Thieving -> "Thieving, exchanging $tears for $thievingXp"
                Training.Both ->
                    "Agility and Thieving, exchanging $tears for $agilityXp and $thievingXp"
            }
        mes("Mairin trains you in $trained.")
        showRates()
    }

    private fun ProtectedAccess.agilityTenths(both: Boolean): Int =
        MairinTraining.agilityTenths(statBase(AGILITY), both)

    private fun ProtectedAccess.thievingTenths(both: Boolean): Int =
        MairinTraining.thievingTenths(statBase(THIEVING), both)

    private suspend fun ProtectedAccess.depositBowl() {
        arriveDelay()
        if (player.bowlOnPile) {
            return
        }
        if (FISHBOWL !in inv) {
            // UNVERIFIED: live's answer to a player without the bowl in their pack.
            mes("You don't have a bowl to put on the pile.")
            return
        }
        if (invDel(inv, FISHBOWL, 1).failure) {
            return
        }
        player.bowlOnPile = true
        mes("You put your bowl on the pile.")
    }

    private fun ProtectedAccess.takeBowl() {
        if (!player.bowlOnPile) {
            return
        }
        if (invAdd(inv, FISHBOWL, 1).failure) {
            // UNVERIFIED: live's answer to a full pack.
            mes("You don't have enough inventory space.")
            return
        }
        player.bowlOnPile = false
        mes("You take your bowl.")
    }

    private enum class Training {
        Agility,
        Thieving,
        Both,
    }

    private enum class Topic(val text: String) {
        Who("Who are you?"),
        Breathe("How do I breathe underwater without a helmet."),
        Help("Is there anything I can do to help you?"),
        Trade("What have you got to trade?"),
        Bye("Bye."),
    }

    private companion object {
        const val MAIRIN = "npc.fossil_mermaid_deepwater"
        const val PILE_EMPTY = "loc.fossil_underwater_bowlhelm_pile_place"
        const val PILE_HOLDING = "loc.fossil_underwater_bowlhelm_pile_take"
        const val FISHBOWL = "obj.hundred_pirate_diving_helmet"
        const val GLISTENING_TEAR = "obj.fossil_glistening_tear"
        const val SCARED = "mesanim.scared"
        const val SHOP = "inv.skill_guide_thieving_other"
        const val SHOP_TITLE = "Mairin's Market"
        const val SHOP_PERCENTAGE = 100.0
        const val MERMAID_TEARS = "currency.mermaid_tears"
        const val TRAINING = "interface.fossil_underwater_training"
        const val TRAIN_AGILITY = "component.fossil_underwater_training:agility"
        const val TRAIN_THIEVING = "component.fossil_underwater_training:thieving"
        const val TRAIN_BOTH = "component.fossil_underwater_training:both"
        const val RATE_AGILITY = "varp.if1"
        const val RATE_THIEVING = "varp.if2"
        const val RATE_AGILITY_BOTH = "varp.if3"
        const val RATE_THIEVING_BOTH = "varp.if4"
        const val AGILITY = "stat.agility"
        const val THIEVING = "stat.thieving"
        const val EXCHANGE_FIVE = 5
        const val TENTHS = 10
        const val XP_TENTHS = 10.0

        val TRAINING_VARPS =
            listOf(RATE_AGILITY, RATE_THIEVING, RATE_AGILITY_BOTH, RATE_THIEVING_BOTH)
    }
}
