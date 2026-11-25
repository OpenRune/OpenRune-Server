package org.rsmod.content.quest.mudfoot

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Singleton
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Player

/** A set of starter weapons Captain Harlan hands out. */
internal enum class WeaponSet(
    val request: String,
    val claim: String,
    val items: List<Pair<String, Int>>,
    val description: String,
    val alreadyHave: String,
    val noSpace: String,
    val unlockStage: Int,
) {
    Melee(
        request = "Could I have a shortsword and a shield?",
        claim = "Claim a shortsword and a shield.",
        items = listOf("obj.bronze_sword" to 1, "obj.wooden_shield" to 1),
        description = "a bronze shortsword and a wooden shield",
        alreadyHave = "You already have a shortsword and a shield. Save some for the other adventurers.",
        noSpace =
            "Ah, you don't have enough space for me to give you a shortsword, or a shield to go " +
                "with it.",
        unlockStage = MudfootStage.WINDMILL,
    ),
    Ranged(
        request = "Could I have a shortbow and some arrows?",
        claim = "Claim a shortbow and some arrows.",
        items = listOf("obj.shortbow" to 1, "obj.bronze_arrow" to 40),
        description = "a shortbow and some bronze arrows",
        alreadyHave = "You already have a shortbow and some arrows. Save some for the other adventurers.",
        noSpace = "You don't have enough space for me to give you a shortbow, nor any arrows.",
        unlockStage = MudfootStage.RANGER_DEAD,
    ),
    Magic(
        request = "Could I have a staff and some runes?",
        claim = "Claim a staff and some runes.",
        items = listOf("obj.plainstaff" to 1, "obj.mindrune" to 40, "obj.airrune" to 40),
        description = "a staff and some runes",
        alreadyHave = "You already have a staff and some runes. Save some for the other adventurers.",
        noSpace = "You don't have enough space for me to give you a staff, nor any runes.",
        unlockStage = MudfootStage.MAGE_DEAD,
    ),
}

/** Adds every item in [items] to the inventory in one transaction, or none of them. */
internal fun Player.giveWeaponSet(items: List<Pair<String, Int>>): Boolean {
    val transaction =
        invTransaction(inv, autoCommit = false) {
            val inventory = select(inv)
            for ((obj, count) in items) {
                add(inventory, obj.asRSCM(RSCMType.OBJ), count)
            }
        }
    if (transaction.failure) {
        return false
    }
    transaction.commitAll()
    return true
}

@Singleton
class HarlanWeapons {
    internal lateinit var quest: Quest

    private fun available(player: Player): List<WeaponSet> {
        val stage = quest.getQuestStage(player)
        val done = quest.isQuestCompleted(player)
        return WeaponSet.entries.filter { done || stage >= it.unlockStage }
    }

    internal suspend fun Dialogue.weaponsOffer() {
        chatPlayer(quiz, "Do you have any weapons I could have?")
        val sets = available(player)
        val offer =
            when (sets.size) {
                1 -> "I can sort you out with a shortsword and a shield."
                2 -> "I can sort you out with a shortsword and a shield, or a shortbow with some arrows."
                else ->
                    "I can sort you out with a shortsword and a shield, a shortbow with some " +
                        "arrows, or a staff with some runes."
            }
        harlan(neutral, "Yes, I do have quite a few spare things. $offer")
        val options = sets.map { it.request } + "Actually, let's talk about something else."
        val picked = chatMenu(options)
        val set = sets.getOrNull(picked - 1)
        if (set == null) {
            chatPlayer(neutral, "Actually, let's talk about something else.")
            return
        }
        chatPlayer(quiz, set.request)
        handOut(set)
    }

    internal suspend fun Dialogue.claimMenu() {
        val sets = available(player)
        if (sets.isEmpty()) {
            return
        }
        val options = sets.map { it.claim } + "Cancel."
        val picked = chatMenu(options)
        val set = sets.getOrNull(picked - 1) ?: return
        handOut(set)
    }

    internal suspend fun Dialogue.stabbingWeapon() {
        chatPlayer(quiz, "Could I have a weapon for stabbing?")
        if (missing(player, WeaponSet.Melee).isEmpty()) {
            harlan(
                quiz,
                "You already have a shortsword, and a shield to go with it as well. Save some " +
                    "things for the other adventurers.",
            )
            return
        }
        harlan(happy, "Of course!")
        handOut(WeaponSet.Melee)
    }

    private suspend fun Dialogue.handOut(set: WeaponSet) {
        val missing = missing(player, set)
        if (missing.isEmpty()) {
            harlan(neutral, set.alreadyHave)
            return
        }
        val freeGiveaway = !bonusUsed(player, set)
        val minutesLeft = COOLDOWN_MINUTES - (nowMinute() - player.harlanWeaponClaimMinute)
        if (!freeGiveaway && minutesLeft > 0) {
            harlan(
                neutral,
                "I'm afraid I've already given you some items recently. I only have so many " +
                    "available, so I can't go overboard.",
            )
            val unit = if (minutesLeft == 1) "minute" else "minutes"
            harlan(neutral, "You'll have to come back in $minutesLeft $unit.")
            return
        }
        if (!player.giveWeaponSet(missing)) {
            harlan(neutral, set.noSpace)
            return
        }
        if (freeGiveaway) {
            markBonusUsed(player, set)
        } else {
            player.harlanWeaponClaimMinute = nowMinute()
        }
        val (first, second) = set.items[0].first to set.items[1].first
        doubleobjbox(first, second, "Captain Harlan gives you ${set.description}.")
    }

    private fun missing(player: Player, set: WeaponSet): List<Pair<String, Int>> =
        set.items.filter { (obj, _) -> obj !in player.inv && obj !in player.worn }

    private fun bonusUsed(player: Player, set: WeaponSet): Boolean =
        when (set) {
            WeaponSet.Melee -> player.harlanMeleeGiveaway
            WeaponSet.Ranged -> player.harlanRangedGiveaway
            WeaponSet.Magic -> player.harlanMagicGiveaway
        }

    private fun markBonusUsed(player: Player, set: WeaponSet) {
        when (set) {
            WeaponSet.Melee -> player.harlanMeleeGiveaway = true
            WeaponSet.Ranged -> player.harlanRangedGiveaway = true
            WeaponSet.Magic -> player.harlanMagicGiveaway = true
        }
    }

    private fun nowMinute(): Int = (System.currentTimeMillis() / MILLIS_PER_MINUTE).toInt()

    private companion object {
        const val COOLDOWN_MINUTES = 30
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
