package org.rsmod.content.quest.mudfoot

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.baseDefenceLvl
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.game.entity.Player

internal var Player.defenceCapePurchased by boolVarBit("varbit.skillcape_defence_purchased")

internal const val DEFENCE_CAPE_PRICE = 99_000
internal const val DEFENCE_CAPE = "obj.skillcape_defence"
internal const val DEFENCE_CAPE_TRIMMED = "obj.skillcape_defence_trimmed"
internal const val DEFENCE_HOOD = "obj.skillcape_defence_hood"
private const val COINS = "obj.coins"
private const val MAX_LEVEL = 99
private const val CAPE_LABEL = "About that cape you're wearing..."

internal fun Player.canBuyDefenceCape(): Boolean = baseDefenceLvl >= MAX_LEVEL

/** Any 99 besides Defence makes the cape a trimmed one. */
internal fun Player.hasOtherNinetyNine(): Boolean =
    ServerCacheManager.getStats().values.count {
        statBase(RSCM.getReverseMapping(RSCMType.STAT, it.id)) >= MAX_LEVEL
    } > 1

/**
 * Swaps [DEFENCE_CAPE_PRICE] coins for a Defence cape (trimmed when [trimmed]) and its hood, in
 * one transaction: either all three moves happen or none do.
 */
internal fun Player.buyDefenceCape(trimmed: Boolean): Boolean {
    val cape = if (trimmed) DEFENCE_CAPE_TRIMMED else DEFENCE_CAPE
    val transaction =
        invTransaction(inv, autoCommit = false) {
            val inventory = select(inv)
            delete(inventory, COINS.asRSCM(RSCMType.OBJ), DEFENCE_CAPE_PRICE)
            add(inventory, cape.asRSCM(RSCMType.OBJ), 1)
            add(inventory, DEFENCE_HOOD.asRSCM(RSCMType.OBJ), 1)
        }
    if (transaction.failure) {
        return false
    }
    transaction.commitAll()
    return true
}

internal fun Player.giveDefenceHood(): Boolean {
    val transaction =
        invTransaction(inv, autoCommit = false) {
            add(select(inv), DEFENCE_HOOD.asRSCM(RSCMType.OBJ), 1)
        }
    if (transaction.failure) {
        return false
    }
    transaction.commitAll()
    return true
}

internal fun Dialogue.capeOption(): Pair<String, suspend Dialogue.() -> Unit>? =
    if (player.canBuyDefenceCape()) CAPE_LABEL to { harlanCape() } else null

private suspend fun Dialogue.harlanCape() {
    chatPlayer(quiz, CAPE_LABEL)
    harlan(neutral, "Ah, yes. What about it?")
    val options = ArrayList<String>()
    options += "Could I buy another one?"
    if (player.defenceCapePurchased) {
        options += "Could I have another hood for mine?"
    }
    options += "I'd better be off."
    when (options[chatMenu(options) - 1]) {
        "Could I buy another one?" -> buyCape()
        "Could I have another hood for mine?" -> anotherHood()
        else -> {
            chatPlayer(neutral, "I'd better be off.")
            harlan(neutral, "Until next time, adventurer.")
        }
    }
}

private suspend fun Dialogue.buyCape() {
    chatPlayer(quiz, "Could I buy another one?")
    harlan(
        happy,
        "Of course! However, there is a price for a cape like this. It will cost you 99,000 " +
            "coins.",
    )
    val buying =
        choice2("Sounds good to me.", true, "99,000 coins? That's too much for me.", false)
    if (!buying) {
        chatPlayer(neutral, "99,000 coins? That's too much for me.")
        return
    }
    chatPlayer(happy, "Sounds good to me.")
    if (player.inv.count(COINS) < DEFENCE_CAPE_PRICE) {
        chatPlayer(sad, "But, unfortunately, I don't have enough money with me.")
        harlan(neutral, "Well, come back and see me when you do.")
        return
    }
    if (!player.buyDefenceCape(trimmed = player.hasOtherNinetyNine())) {
        harlan(
            neutral,
            "Unfortunately all Skillcapes are only available with a free hood, it's part of a " +
                "skill promotion deal; buy one get one free, you know. So you'll need to free " +
                "up some inventory space before I can sell you one.",
        )
        return
    }
    player.defenceCapePurchased = true
    harlan(happy, "Excellent! Wear that cape with pride my friend.")
}

private suspend fun Dialogue.anotherHood() {
    chatPlayer(quiz, "Could I have another hood for mine?")
    if (!player.giveDefenceHood()) {
        harlan(neutral, "You'll need to free up some inventory space before I can give you one.")
        return
    }
    harlan(happy, "Of course! Here you go.")
}
