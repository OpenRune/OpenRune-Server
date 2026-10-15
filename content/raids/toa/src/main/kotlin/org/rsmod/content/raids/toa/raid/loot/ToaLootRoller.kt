package org.rsmod.content.raids.toa.raid.loot

import org.rsmod.api.random.GameRandom

internal class LootItem(val obj: String, val count: Int)

internal class UniqueRoll<K>(val winner: K, val obj: String?) {
    val consolation: Boolean
        get() = obj == null
}

internal class ToaLootRoll<K>(
    val unique: UniqueRoll<K>?,
    val commons: Map<K, List<LootItem>>,
    val dung: Set<K>,
)

internal class ToaLootRoller<K>(
    private val random: GameRandom,
    private val pickCommon: (K) -> CommonDrop,
    private val pickUnique: (K, Int) -> String,
) {
    fun rollRaid(points: Map<K, Int>, raidLevel: Int, inLevel: (String) -> Boolean): ToaLootRoll<K> {
        val unique = rollUnique(points, raidLevel, inLevel)
        val dung = points.filterValues { it < ToaLootMath.DUNG_BELOW_POINTS }.keys
        val commons = HashMap<K, List<LootItem>>()
        for ((player, playerPoints) in points) {
            if (player == unique?.winner || player in dung) continue
            commons[player] = rollCommons(player, playerPoints, raidLevel)
        }
        return ToaLootRoll(unique, commons, dung - setOfNotNull(unique?.winner))
    }

    fun rollUnique(points: Map<K, Int>, raidLevel: Int, inLevel: (String) -> Boolean): UniqueRoll<K>? {
        val total = points.values.sum()
        if (total <= 0) return null
        if (random.randomDouble() >= ToaLootMath.uniqueChance(total, raidLevel)) return null
        val winner = pickWinner(points, total)
        val obj = pickUnique(winner, raidLevel)
        if (inLevel(obj) || random.of(ToaLootMath.OUT_OF_LEVEL_ODDS) == 0) {
            return UniqueRoll(winner, obj)
        }
        return UniqueRoll(winner, null)
    }

    fun rollCommons(player: K, points: Int, raidLevel: Int): List<LootItem> =
        List(ToaLootMath.COMMON_ROLLS) {
            val drop = pickCommon(player)
            LootItem(drop.obj, ToaLootMath.commonQuantity(points, drop.pointsPerUnit, raidLevel))
        }

    fun rollThread(killCount: Int, owned: Boolean): Boolean =
        random.randomDouble() < ToaLootMath.threadChance(killCount, owned)

    fun rollJewel(killCount: Int, jewels: List<String>, owned: Set<String>): String? {
        if (random.randomDouble() >= ToaLootMath.jewelChance(killCount)) return null
        val missing = jewels.filter { it !in owned }
        return random.pick(missing.ifEmpty { jewels })
    }

    fun rollPet(points: Int, raidLevel: Int): Boolean =
        random.randomDouble() < ToaLootMath.petChance(points, raidLevel)

    fun rollEliteClue(personalPoints: Int, combatAchievementBonus: Boolean): Boolean =
        random.randomDouble() < ToaLootMath.eliteClueChance(personalPoints, combatAchievementBonus)

    private fun pickWinner(points: Map<K, Int>, total: Int): K {
        var roll = random.of(total)
        for ((player, playerPoints) in points) {
            if (roll < playerPoints) return player
            roll -= playerPoints
        }
        return points.keys.last()
    }
}
