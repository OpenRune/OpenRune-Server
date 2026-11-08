package org.rsmod.content.raids.toa.raid.loot

import org.rsmod.api.random.GameRandom

internal class LootItem(val obj: String, val count: Int)

internal class UniqueRoll<K>(val winner: K, val obj: String?) {
    val consolation: Boolean
        get() = obj == null
}

internal class ToaLootRoll<K>(
    val uniques: Map<K, UniqueRoll<K>>,
    val commons: Map<K, List<LootItem>>,
    val dung: Set<K>,
)

internal class ToaLootRoller<K>(
    private val random: GameRandom,
    private val pickCommon: (K) -> CommonDrop,
    private val pickUnique: (K, Int) -> String,
    private val uniqueBoost: (K) -> Double = { 1.0 },
) {
    fun rollRaid(points: Map<K, Int>, raidLevel: Int, inLevel: (String) -> Boolean): ToaLootRoll<K> {
        val dung = points.filterValues { it < ToaLootMath.DUNG_BELOW_POINTS }.keys
        val uniques = LinkedHashMap<K, UniqueRoll<K>>()
        val commons = HashMap<K, List<LootItem>>()
        for ((player, playerPoints) in points) {
            if (player in dung) continue
            val unique = rollUnique(player, playerPoints, raidLevel, inLevel)
            if (unique != null) uniques[player] = unique
            if (unique?.consolation == false) continue
            commons[player] = rollCommons(player, playerPoints, raidLevel)
        }
        return ToaLootRoll(uniques, commons, dung)
    }

    fun rollUnique(
        player: K,
        points: Int,
        raidLevel: Int,
        inLevel: (String) -> Boolean,
    ): UniqueRoll<K>? {
        if (points <= 0) return null
        val chance = ToaLootMath.uniqueChance(points, raidLevel, uniqueBoost(player))
        if (random.randomDouble() >= chance) return null
        val obj = pickUnique(player, raidLevel)
        if (inLevel(obj) || random.of(ToaLootMath.OUT_OF_LEVEL_ODDS) == 0) {
            return UniqueRoll(player, obj)
        }
        return UniqueRoll(player, null)
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
}
