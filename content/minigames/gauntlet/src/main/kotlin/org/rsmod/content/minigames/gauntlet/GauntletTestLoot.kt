package org.rsmod.content.minigames.gauntlet

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import jakarta.inject.Inject
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj

/** Same chest tables and obtain logging; an active run, escrow and pending chest are preserved. */
class GauntletTestLoot @Inject constructor(
    private val registry: DropTableRegistry,
    private val random: GameRandom,
    private val objects: ObjRepository,
    private val collectionLog: CollectionLog,
) {
    fun generate(player: Player, count: Int, corrupted: Boolean) {
        require(count in 1..1000)
        val key = if (corrupted) GauntletRewardKeys.CORRUPTED else GauntletRewardKeys.NORMAL
        val source = if (corrupted) "Corrupted Gauntlet" else "Gauntlet"
        val table = checkNotNull(registry.forLoc(key)) { "Gauntlet reward table is not registered" }
        repeat(count) {
            val drops = when (val result = table.roll(player, ArgMap()).flatten()) {
                is RollResult.Single -> listOf(result.result)
                is RollResult.ListOf -> result.results
                else -> emptyList()
            }
            for (drop in drops) {
                if (drop.isNothing || !drop.condition(player)) continue
                val obj = drop.transformObj(player) ?: drop.obj
                val amount = drop.rollCount(random)
                check(objects.add(Obj.fromOwner(player, player.coords, obj, amount), duration = 300))
                collectionLog.grant(player, obj, amount, source = source)
            }
        }
    }
}
