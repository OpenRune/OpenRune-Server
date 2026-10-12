package org.rsmod.content.minigames.gauntlet

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal var Player.gauntletRewardAvailable by boolVarBit("varbit.gauntlet_reward_pending")
internal var Player.gauntletRewardVisible by boolVarBit("varbit.gauntlet_reward_available")
internal var Player.gauntletRewardTier by intVarp("varp.gauntlet_reward_tier")
internal var Player.gauntletPoints by intVarp("varp.gauntlet_points")
private var Player.gauntletCompletions by intVarp("varp.total_completed_gauntlet")
private var Player.gauntletCorruptedCompletions by intVarp("varp.total_completed_gauntlet_hm")

internal object GauntletPoints {
    const val WEAK_KILL = 2
    const val STRONG_KILL = 5
    const val DEMI_KILL = 10
    private val TIER = Regex("_t([123])(_hm)?$")

    fun forCraft(product: String): Int {
        if (product.contains("combo_food")) return 2
        return when (TIER.find(product)?.groupValues?.get(1)?.toInt()) {
            1 -> 2
            2 -> 5
            3 -> 10
            else -> 0
        }
    }
}

internal fun Player.addGauntletPoints(points: Int) {
    if (points > 0) gauntletPoints += points
}

@Singleton
class GauntletRewards
@Inject
constructor(
    private val objRepo: ObjRepository,
    private val pets: PetRewards,
    private val registry: DropTableRegistry,
    private val rewardRandom: GameRandom,
    private val collectionLog: CollectionLog,
) {
    fun complete(player: Player, run: GauntletRun) {
        if (run.completed) return
        run.completed = true
        val corrupted = run.mode.corrupted
        val count =
            if (corrupted) ++player.gauntletCorruptedCompletions else ++player.gauntletCompletions
        val name = if (corrupted) "Corrupted Gauntlet" else "Gauntlet"
        player.mes("Your $name completion count is: <col=ff0000>$count</col>.")
        store(player, if (corrupted) RewardTier.CORRUPTED else RewardTier.NORMAL)
    }

    fun isFirstNormalCompletion(player: Player, run: GauntletRun): Boolean =
        run.completed && !run.mode.corrupted && player.gauntletCompletions == 1

    fun settleByDamage(player: Player, run: GauntletRun) {
        if (run.completed) return
        val tier = GauntletRewardTables.tierForDamage(run.bossDamage)
        if (tier != RewardTier.NONE) store(player, tier)
    }

    suspend fun ProtectedAccess.claim() {
        if (!player.gauntletRewardAvailable) {
            mes("Looks pretty empty to me.")
            return
        }
        if (inv.isFull()) {
            mes("You need a free inventory space for whatever you may find in the chest.")
            return
        }
        mes("You open the chest.")
        anim("seq.human_openchest")
        soundSynth("synth.locked")
        delay(1)
        if (!player.gauntletRewardAvailable) return
        val tier = RewardTier.entries.getOrElse(player.gauntletRewardTier) { RewardTier.NONE }
        val table = tier.tableKey?.let(registry::forLoc) ?: return
        val drops = table.roll(player, ArgMap()).flatten()
        val items = when (drops) {
            is RollResult.Nothing -> emptyList()
            is RollResult.Single -> listOf(drops.result)
            is RollResult.ListOf -> drops.results
        }.filter { !it.isNothing && it.condition(player) }.map {
            (it.transformObj(player) ?: it.obj) to it.rollCount(rewardRandom)
        }
        player.gauntletRewardAvailable = false
        player.gauntletRewardVisible = false
        mes("You find some treasure in the chest!")
        for ((obj, count) in items) award(obj, count)
        player.gauntletRewardTier = 0
    }

    private fun ProtectedAccess.award(obj: String, count: Int) {
        if (pets.give(player, obj, source = rewardSource)) return
        give(obj, count)
    }

    private fun store(player: Player, tier: RewardTier) {
        player.gauntletRewardTier = tier.ordinal
        player.gauntletRewardAvailable = true
        player.gauntletRewardVisible = true
    }

    private fun ProtectedAccess.give(obj: String, count: Int) {
        if (obj == SHARD) {
            mes("<col=ef1020>Untradeable drop: $count x Crystal shard</col>")
        }
        invAddOrDrop(objRepo, obj, count)
        collectionLog.grant(player, obj, count, source = rewardSource)
    }

    private val Player.rewardSource: String
        get() = if (gauntletRewardTier == RewardTier.CORRUPTED.ordinal) "Corrupted Gauntlet" else "Gauntlet"

    private val ProtectedAccess.rewardSource: String get() = player.rewardSource

    private companion object {
        const val SHARD = "obj.prif_crystal_shard"
    }
}

class GauntletChest @Inject internal constructor(private val rewards: GauntletRewards) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (chest in CHESTS) {
            onOpLoc1(chest) { with(rewards) { claim() } }
        }
    }

    private companion object {
        val CHESTS =
            listOf(
                "loc.gauntlet_chest",
                "loc.gauntlet_chest_hm",
                "loc.gauntlet_chest_closed",
                "loc.gauntlet_chest_closed_hm",
            )
    }
}
