package org.rsmod.content.other.treasuretrails

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import dtx.rs.RSWeightedTable
import jakarta.inject.Inject
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.invtx.invClear
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.table.TrailTierRow
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.game.entity.Player

class TrailRewards
@Inject
constructor(private val objRepo: ObjRepository, private val marketPrices: MarketPrices) {
    /**
     * Rewards go straight into the inventory (overflow drops to the floor) when the casket opens;
     * the reward screen only shows a copy, so closing or disconnecting can never lose or repeat
     * them.
     */
    fun ProtectedAccess.openCasket(tier: TrailTierRow) {
        val table = CasketTables.forDifficulty(tier.difficulty) ?: return
        if (invDel(inv, tier.casketObj.symbol(), 1).failure) {
            return
        }
        val completedVarp = TrailTiers.varp(tier.completedVarp)
        val completed = player.vars[completedVarp] + 1
        VarPlayerIntMapSetter.set(player, completedVarp, completed)

        val rewards = rollRewards(tier, table) + tertiaryRewards(player, tier, completed)
        val masterClue = masterClueReward(tier)
        for (reward in rewards + listOfNotNull(masterClue)) {
            invAddOrDrop(objRepo, reward.obj, reward.count)
        }
        mes("Well done, you've completed the Treasure Trail!")
        mes("<col=ef1020>You have completed $completed ${tier.name} Treasure Trails.</col>")
        for (reward in rewards) {
            CollectionLog.grant(player, reward.obj, reward.count)
        }
        val value = rewards.sumOf { it.value() }
        mes("<col=ef1020>Your treasure is worth around ${"%,d".format(value)} coins!</col>")
        showRewards(rewards + listOfNotNull(masterClue))
    }

    private fun ProtectedAccess.masterClueReward(tier: TrailTierRow): CasketReward? {
        val chance = tier.masterClueChance ?: return null
        if (!random.randomBoolean(chance)) return null
        val master = TrailTiers.tier(TrailTiers.MASTER)
        val clues = TrailTiers.ownedClues(player, master.difficulty)
        if (!ClueTakeHook.boxesUnlocked(player)) {
            if (clues > 0) return null
            TrailFlow.resetTrail(player, master)
            return CasketReward(checkNotNull(master.clueObj).symbol(), 1)
        }
        val held = clues + TrailTiers.ownedBoxes(player, master.difficulty)
        if (held >= TrailTiers.capacity(player, master)) return null
        return CasketReward(master.boxObj.symbol(), 1)
    }

    fun closeRewards(player: Player) {
        val display = player.invMap[REWARD_INV] ?: return
        player.invClear(display)
        player.stopInvTransmit(display)
    }

    private fun ProtectedAccess.rollRewards(
        tier: TrailTierRow,
        table: RSWeightedTable<Player, DropRollItem>,
    ): List<CasketReward> {
        val rolls = random.of(tier.rewardRollsMin, tier.rewardRollsMax)
        return (0 until rolls)
            .flatMap {
                when (val result = table.roll(player, ArgMap()).flatten()) {
                    is RollResult.Single -> listOfNotNull(reward(result.result))
                    is RollResult.ListOf -> result.results.mapNotNull { reward(it) }
                    else -> emptyList()
                }
            }
            .flatMap { reward -> listOf(reward) + COMPANIONS[reward.obj].orEmpty() }
    }

    /**
     * Scroll cases and milestone objs are given on the threshold completion, or on any later one
     * when the player has lost the obj (and, for a case, has not opened one).
     */
    private fun tertiaryRewards(
        player: Player,
        tier: TrailTierRow,
        completed: Int,
    ): List<CasketReward> {
        fun missingCase(obj: ItemServerType, at: Int, varbit: Int): ItemServerType? =
            obj.takeIf {
                completed >= at &&
                    player.vars[TrailTiers.varbit(varbit)] == 0 &&
                    !TrailTiers.owns(player, it)
            }
        val milestone =
            tier.milestoneObj?.takeIf { obj ->
                tier.milestoneAt?.let { completed >= it } == true && !TrailTiers.owns(player, obj)
            }
        return listOfNotNull(
                missingCase(tier.minorCaseObj, tier.minorCaseAt, tier.minorCaseVarbit),
                missingCase(tier.majorCaseObj, tier.majorCaseAt, tier.majorCaseVarbit),
                milestone,
            )
            .map { CasketReward(it.symbol(), 1) }
    }

    private fun ProtectedAccess.reward(drop: DropRollItem): CasketReward? {
        if (drop.isNothing || !drop.condition(player)) return null
        return CasketReward(drop.transformObj(player) ?: drop.obj, drop.rollCount(random))
    }

    private fun ProtectedAccess.showRewards(rewards: List<CasketReward>) {
        val display = player.invMap.getOrPut(REWARD_INV)
        invClear(display)
        for (reward in rewards) {
            invAdd(display, reward.obj, reward.count)
        }
        invTransmit(display)
        midiJingle(TREASURE_JINGLE)
        ifOpenMainModal(REWARD_INTERFACE)
        ifSetEvents("component.trail_rewardscreen:items", 0 until display.size, IfEvent.Op10)
    }

    private fun CasketReward.value(): Long {
        val type = ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)) ?: return 0
        return (marketPrices[type] ?: 0).toLong() * count
    }

    private data class CasketReward(val obj: String, val count: Int)

    internal companion object {
        const val REWARD_INV = "inv.trail_rewardinv"
        const val REWARD_INTERFACE = "interface.trail_rewardscreen"
        const val TREASURE_JINGLE = "jingle.yo_ho_ho"

        private val COMPANIONS =
            mapOf(
                "obj.cert_4dose2attack" to
                    listOf(
                        CasketReward("obj.cert_4dose2strength", 5),
                        CasketReward("obj.cert_4dose2defense", 5),
                    )
            )
    }
}
