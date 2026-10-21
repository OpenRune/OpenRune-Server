package org.rsmod.content.other.treasuretrails

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.TrailTierRow
import org.rsmod.game.entity.Player

object ClueWatson {
    enum class Redeem {
        Missing,
        MasterHeld,
        NoSpace,
        Given,
    }

    val TIERS = 1..TrailTiers.ELITE

    fun tiers(): List<TrailTierRow> = TIERS.map(TrailTiers::tier)

    fun stored(player: Player, tier: TrailTierRow): Boolean = player.vars[varbit(tier)] != 0

    fun storedTiers(player: Player): List<TrailTierRow> = tiers().filter { stored(player, it) }

    fun complete(player: Player): Boolean = tiers().all { stored(player, it) }

    fun heldSlot(player: Player, tier: TrailTierRow): Int? {
        val challenges = TrailTiers.challengeObjs(tier.difficulty).map { it.id }.toSet()
        return (0 until player.inv.size).firstOrNull { slot ->
            val obj = player.inv[slot] ?: return@firstOrNull false
            TrailTiers.clueDifficulty(TrailTiers.type(obj.id)) == tier.difficulty ||
                obj.id in challenges
        }
    }

    fun ProtectedAccess.give(tier: TrailTierRow): Boolean {
        if (stored(player, tier)) return false
        val slot = heldSlot(player, tier) ?: return false
        val obj = player.inv[slot] ?: return false
        if (invDel(inv, TrailTiers.type(obj.id).symbol(), 1, slot).failure) return false
        VarPlayerIntMapSetter.set(player, varbit(tier), 1)
        if (!TrailFlow.holdsClue(player, tier)) TrailFlow.resetTrail(player, tier)
        return true
    }

    fun ProtectedAccess.redeem(): Redeem {
        if (!complete(player)) return Redeem.Missing
        val master = TrailTiers.tier(TrailTiers.MASTER)
        val clues = TrailTiers.ownedClues(player, master.difficulty)
        val boxes = ClueTakeHook.boxesUnlocked(player)
        val reward =
            if (boxes) {
                val held = clues + TrailTiers.ownedBoxes(player, master.difficulty)
                if (held >= TrailTiers.capacity(player, master)) return Redeem.MasterHeld
                master.boxObj
            } else {
                if (clues > 0) return Redeem.MasterHeld
                checkNotNull(master.clueObj)
            }
        if (!boxes) TrailFlow.resetTrail(player, master)
        if (invAdd(inv, reward.symbol(), 1).failure) return Redeem.NoSpace
        tiers().forEach { VarPlayerIntMapSetter.set(player, varbit(it), 0) }
        return Redeem.Given
    }

    private fun varbit(tier: TrailTierRow): String =
        TrailTiers.varbit(checkNotNull(tier.watsonVarbit) { "No Watson varbit for ${tier.name}." })
}
