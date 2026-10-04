package org.rsmod.content.bosses.barrows

import dtx.core.RollResult
import dtx.core.flatten
import jakarta.inject.Inject
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Player

/** Administrator chest rolls without changing a player's active crypt state or chest count. */
class BarrowsTestLoot @Inject constructor(
    private val objs: ObjRepository,
    private val random: GameRandom,
) {
    fun generate(player: Player, count: Int) {
        require(count in 1..1000)
        repeat(count) {
            val drops = when (val result = player.rollBarrowsChest(BarrowsBrother.entries, MAX_REWARD_POTENTIAL).flatten()) {
                is RollResult.Single -> listOf(result.result)
                is RollResult.ListOf -> result.results
                else -> emptyList()
            }
            for (drop in drops) {
                if (drop.isNothing || !drop.condition(player)) continue
                objs.add(drop.transformObj(player) ?: drop.obj, player.coords,
                    duration = 300, receiver = player, count = drop.rollCount(random))
            }
        }
    }
}
