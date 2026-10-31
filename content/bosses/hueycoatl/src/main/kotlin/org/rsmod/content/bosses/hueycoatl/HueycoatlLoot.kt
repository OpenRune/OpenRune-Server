package org.rsmod.content.bosses.hueycoatl

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import dtx.core.with
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.Locale
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.KillRollContext
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.output.mes
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.content.drops.tables.monsters.hueyMainLoot
import org.rsmod.content.drops.tables.monsters.hueyUniqueLoot
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.uncert

@Singleton
internal class HueycoatlLoot
@Inject
constructor(
    private val objRepo: ObjRepository,
    private val prices: MarketPrices,
    private val random: GameRandom,
) {
    fun award(fight: HueycoatlFight) {
        val head = fight.head ?: return
        val inArea = fight.participantsInParticipation()
        if (inArea.isEmpty()) return
        val eligible = inArea.filter { fight.qualifies(it.first, it.second) }
        val totalDamage = inArea.sumOf { it.second.totalDamage }.coerceAtLeast(1)
        val hero = fight.mvp() ?: eligible.firstOrNull()?.first ?: return
        val main = rollMain(hero, head)
        if (main != null && eligible.isNotEmpty()) {
            val lump = main.rollCount(random) * eligible.size
            val name = itemName(main.obj)
            fight.message("<col=ff289d>The Hueycoatl dropped: $lump x $name</col>")
            for ((player, participant) in eligible) {
                val share =
                    HueycoatlArena.lootShare(
                        lump = lump,
                        damage = participant.totalDamage,
                        totalDamage = totalDamage,
                        bodyDamage = participant.bodyDamage,
                    )
                if (share <= 0) continue
                if (participant.bodyDamage < 25) {
                    player.mes(
                        "<col=ff3045>As you didn't damage the Hueycoatl's body enough, " +
                            "you will receive fewer rewards.</col>",
                    )
                }
                spawnLoot(player, fight.lootCoord(player), main.obj, share)
                if (eligible.size > 1) {
                    player.mes("<col=ff289d>You received: $share x $name</col>")
                }
            }
        }
        for ((player, participant) in eligible) {
            rollUnique(fight, player, participant, totalDamage)
            rollTertiary(fight, player, participant, totalDamage)
        }
        for ((player, _) in inArea) {
            if (random.of(75) == 0) {
                spawnLoot(player, fight.lootCoord(player), "obj.varlamore_key_half_1", 1)
            }
        }
        spawnLoot(hero, fight.lootCoord(hero), "obj.big_bones", 1)
    }

    private fun rollMain(player: Player, head: Npc): DropRollItem? {
        val result = hueyMainLoot.roll(player, ArgMap(KillRollContext.npc with head)).flatten()
        val drops =
            when (result) {
                is RollResult.Single -> listOf(result.result)
                is RollResult.ListOf -> result.results
                else -> emptyList()
            }
        return drops.firstOrNull { !it.isNothing }
    }

    private fun rollUnique(
        fight: HueycoatlFight,
        player: Player,
        participant: HueyParticipant,
        totalDamage: Int,
    ) {
        val chance = participant.totalDamage.toDouble() / totalDamage / 18.0
        if (random.randomDouble() >= chance) return
        val result = hueyUniqueLoot.roll(player, ArgMap()).flatten()
        val drops =
            when (result) {
                is RollResult.Single -> listOf(result.result)
                is RollResult.ListOf -> result.results
                else -> emptyList()
            }
        val drop = drops.firstOrNull { !it.isNothing } ?: return
        val hide = drop.obj == "obj.huey_hide"
        if (hide) {
            val base = fight.lootCoord(player)
            repeat(3) { index ->
                spawnLoot(player, base.translate(index, 0), drop.obj, 1, valuable = false)
            }
            valuableDrop(player, drop.obj, 3)
        } else {
            spawnLoot(player, fight.lootCoord(player), drop.obj, 1)
        }
    }

    private fun rollTertiary(
        fight: HueycoatlFight,
        player: Player,
        participant: HueyParticipant,
        totalDamage: Int,
    ) {
        val petChance =
            (participant.totalDamage.toDouble() / totalDamage / 400.0) *
                if (participant.bodyDamage < 25) 0.05 else 1.0
        if (random.randomDouble() < petChance) {
            spawnLoot(player, fight.lootCoord(player), "obj.hueypet", 1)
        }
        if (random.of(50) == 0) {
            val scroll = player.clueScrollTransformObj("obj.trail_clue_hard_map001")
                ?: "obj.trail_clue_hard_map001"
            spawnLoot(player, fight.lootCoord(player), scroll, 1)
        }
    }

    private fun spawnLoot(
        player: Player,
        coords: org.rsmod.map.CoordGrid,
        obj: String,
        count: Int,
        valuable: Boolean = true,
    ) {
        if (count <= 0) return
        val type = ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)) ?: return
        objRepo.add(type, coords, LOOT_DURATION, player, count, reveal = LOOT_REVEAL)
        CollectionLog.grant(player, uncert(type).id, count)
        if (valuable) valuableDrop(player, obj, count)
    }

    private fun valuableDrop(player: Player, obj: String, count: Int) {
        val type = ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)) ?: return
        val value = (prices[uncert(type)] ?: 0).toLong() * count
        if (value < VALUABLE_THRESHOLD) return
        player.mes(
            "<col=ef1020>Valuable drop: $count x ${itemName(obj)} " +
                "(${String.format(Locale.UK, "%,d", value)} coins)</col>",
        )
    }

    private fun itemName(obj: String): String {
        val type = ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)) ?: return obj
        val name = uncert(type).name
        return name.ifBlank { obj }
    }

    private companion object {
        private const val LOOT_DURATION = 400
        private const val LOOT_REVEAL = 100
        private const val VALUABLE_THRESHOLD = 100_000L
    }
}
