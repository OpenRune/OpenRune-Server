package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.farming.FarmingGuildRow
import org.rsmod.game.entity.Player

private val CONTRACT_TYPE by lazy {
    checkNotNull(ServerCacheManager.getParam("param.farming_contract_type".asRSCM()))
}
private val VAULT_CATEGORY by lazy {
    checkNotNull(ServerCacheManager.getParam("param.seed_vault_category".asRSCM()))
}

internal fun ItemServerType.farmingContractType(): Int =
    paramOrNull<Int>(CONTRACT_TYPE) ?: 0

internal fun ItemServerType.seedVaultCategory(): Int =
    paramOrNull<Int>(VAULT_CATEGORY) ?: 0

internal data class FarmingContract(
    val tier: Int,
    val seed: ItemServerType,
    val unlock: Int,
    val rewardTier: Int,
) {
    val type: Int = seed.farmingContractType().also { check(it > 0) { "Missing native contract type: ${seed.internalName}" } }
    val crop: Crop by lazy {
        val input = FarmingSaplings.chains.firstOrNull { it.seed.id == seed.id }?.sapling?.internalName ?: seed.internalName
        FarmingCrops.all.single { it.seed == input }
    }
}

object FarmingContracts {
    internal val contracts: List<FarmingContract> by lazy {
        FarmingGuildRow.all().single().contracts.map { FarmingContract(it.t0, it.t1, it.t2, it.t3) }
    }

    // shortcut: Hold assignment until every native-pool crop ships; never reweight the pool.
    internal val assignmentAvailable: Boolean by lazy { contracts.all { it.crop.shipped } }

    internal fun current(player: Player): FarmingContract? = contracts.firstOrNull {
        it.tier == player.vars[LEVEL] && it.type == player.vars[TYPE]
    }

    internal fun assign(player: Player, tier: Int, random: GameRandom): FarmingContract? {
        if (tier !in 1..3 || player.farmingLvl < requiredLevel(tier)) return null
        if (player.vars[TYPE] != 0 || player.vars[COMPLETE] != 0) return null
        return choose(player, tier, random)
    }

    internal fun downgrade(player: Player, random: GameRandom): FarmingContract? {
        val active = current(player) ?: return null
        if (active.tier <= 1 || player.vars[COMPLETE] != 0) return null
        return choose(player, active.tier - 1, random)
    }

    private fun choose(player: Player, tier: Int, random: GameRandom): FarmingContract? {
        val pool = contracts.filter { it.tier == tier && it.unlock <= player.farmingLvl }
        if (pool.isEmpty()) return null
        val contract = pool[random.of(pool.size)]
        VarPlayerIntMapSetter.set(player, LEVEL, tier)
        VarPlayerIntMapSetter.set(player, TYPE, contract.type)
        VarPlayerIntMapSetter.set(player, COMPLETE, 0)
        return contract
    }

    fun complete(player: Player, patch: PatchDef, crop: Crop, checkedHealth: Boolean): Boolean {
        if (!patch.site.startsWith("farming_guild_") || crop.checksHealth != checkedHealth) return false
        if (player.vars[COMPLETE] != 0) return false
        val contract = current(player) ?: return false
        if (contract.crop.index != crop.index) return false
        VarPlayerIntMapSetter.set(player, COMPLETE, 1)
        player.mes("You've completed a Farming Guild Contract. You should return to Guildmaster Jane.")
        return true
    }

    internal fun claim(player: Player, random: GameRandom): Int? {
        val active = current(player) ?: return null
        if (player.vars[COMPLETE] == 0 || player.inv.isFull()) return null
        val pack = FarmingSeedPacks.create(active.rewardTier, random)
        if (!player.invAdd(player.inv, pack.id, 1, vars = pack.vars).success) return null
        VarPlayerIntMapSetter.set(player, TYPE, 0)
        VarPlayerIntMapSetter.set(player, COMPLETE, 0)
        VarPlayerIntMapSetter.set(player, COUNT, (player.vars[COUNT] + 1).coerceAtMost(0xffff))
        return active.tier
    }

    fun requiredLevel(tier: Int): Int = when (tier) {
        1 -> 45
        2 -> 65
        3 -> 85
        else -> error("Unknown Farming Guild tier: $tier")
    }

    internal const val LEVEL = "varbit.farmguild_contract_level"
    internal const val TYPE = "varbit.farmguild_contract_type"
    internal const val COMPLETE = "varbit.farmguild_contract_complete"
    internal const val COUNT = "varbit.farmguild_contract_count"
    internal const val DISCUSSED = "varbit.farmguild_contract_discussed"
}
