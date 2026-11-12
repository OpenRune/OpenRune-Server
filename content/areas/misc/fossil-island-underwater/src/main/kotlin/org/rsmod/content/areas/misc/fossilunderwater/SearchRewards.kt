package org.rsmod.content.areas.misc.fossilunderwater

import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.FossilUnderwaterRewardsRow

/** The extra a successful search may find beside its pair of tears. */
internal class SearchRewards(private val roll: Int, private val rewards: List<Reward>) {
    class Reward(val obj: String, val min: Int, val max: Int, val weight: Int)

    class Found(val obj: String, val count: Int)

    fun at(value: Int): Reward? {
        var upTo = 0
        for (reward in rewards) {
            upTo += reward.weight
            if (value < upTo) {
                return reward
            }
        }
        return null
    }

    fun roll(random: GameRandom): Found? {
        val reward = at(random.of(roll)) ?: return null
        return Found(reward.obj, random.of(reward.min, reward.max))
    }

    companion object {
        private const val ROW = "dbrow.fossil_underwater_rewards"

        fun load(): SearchRewards {
            val row = FossilUnderwaterRewardsRow.getRow(ROW)
            val rewards =
                row.rewards.map { (obj, min, max, weight) ->
                    Reward(obj.internalName, min, max, weight)
                }
            return SearchRewards(row.roll, rewards)
        }
    }
}
