package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.farming.FarmingGuildRow
import org.rsmod.api.table.toListOfTuple7
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

internal data class SeedPackState(
    val low: Int,
    val medium: Int,
    val high: Int,
    val tier: Int,
    val pending: Int = 0,
    val quantity: Int = 0,
) {
    val remaining: Int get() = low + medium + high
    val group: Int get() = if (low > 0) 0 else if (medium > 0) 1 else 2
    fun pack(): Int = low or (medium shl 4) or (high shl 7) or (tier shl 9) or (pending shl 12) or (quantity shl 19)
    fun taken(): SeedPackState = when (group) {
        0 -> copy(low = low - 1, pending = 0, quantity = 0)
        1 -> copy(medium = medium - 1, pending = 0, quantity = 0)
        else -> copy(high = high - 1, pending = 0, quantity = 0)
    }
    companion object {
        fun unpack(value: Int): SeedPackState = SeedPackState(
            value and 15, (value ushr 4) and 7, (value ushr 7) and 3,
            (value ushr 9) and 7, (value ushr 12) and 127, (value ushr 19) and 15,
        )
    }
}

object FarmingSeedPacks {
    const val PACK = "obj.seedbox"

    private val row by lazy { FarmingGuildRow.all().single() }
    private val rewards by lazy { row.rewards }
    private val packRolls by lazy {
        row.rolls.toListOfTuple7<Int, Int, Int, Int, Int, Int, Int>()
    }
    private val rewardsByGroup by lazy { rewards.withIndex().groupBy { it.value.t0 } }
    private val rewardWeights by lazy { rewardsByGroup.mapValues { (_, entries) -> entries.sumOf { it.value.t4 } } }

    fun create(tier: Int, random: GameRandom): InvObj {
        val rolls = packRolls.single { it.t0 == tier }
        val medium = random.of(rolls.t2, rolls.t3)
        val high = if (rolls.t6 == 1 || random.of(rolls.t6) == 0) random.of(rolls.t4, rolls.t5) else 0
        val state = SeedPackState(rolls.t1 - medium - high, medium, high, tier)
        return InvObj(PACK, vars = state.pack())
    }

    internal fun exchange(player: Player, source: ItemServerType, random: GameRandom): Boolean {
        val chain = FarmingSaplings.chains.single { it.seed.internalName == "obj.spirit_tree_seed" }
        if (source.id != chain.seed.id && source.id != chain.dry.id &&
            source.id != chain.wet.id && source.id != chain.sapling.id) return false
        if (!player.inv.contains(source.internalName)) return false
        val pack = create(5, random)
        return player.invTransaction(player.inv) {
            val inv = select(player.inv)
            delete(inv, source.id, 1)
            add(inv, pack.id, 1, vars = pack.vars)
        }.success
    }

    internal fun take(player: Player, slot: Int, expected: InvObj, random: GameRandom): Boolean {
        if (player.inv[slot] != expected || expected.id != PACK.asRSCM()) return false
        var state = SeedPackState.unpack(expected.vars)
        if (state.tier !in 1..5 || state.remaining <= 0) return false
        if (state.pending == 0) {
            val rewards = rewardsByGroup.getValue(state.group)
            var roll = random.of(rewardWeights.getValue(state.group))
            val reward = rewards.first { roll -= it.value.t4; roll < 0 }
            state = state.copy(pending = reward.index + 1, quantity = random.of(reward.value.t2, reward.value.t3))
            // Failed inventory awards retain this roll, including across logout and bank moves.
            player.inv[slot] = expected.copy(vars = state.pack())
        }
        val reward = rewards.getOrNull(state.pending - 1) ?: return false
        if (reward.t0 != state.group || state.quantity !in reward.t2..reward.t3) return false
        val next = state.taken()
        return player.invTransaction(player.inv) {
            val inv = select(player.inv)
            delete(inv, expected.id, 1, slot)
            if (next.remaining > 0) add(inv, expected.id, 1, vars = next.pack(), slot = slot)
            add(inv, reward.t1.id, state.quantity)
        }.success
    }
}
