package org.rsmod.content.other.bots

import org.rsmod.api.player.ironman.PlayerGamemode
import org.rsmod.api.random.GameRandom

internal object BotNames {
    private val poolsByMode =
        mapOf(
            PlayerGamemode.NORMAL to "standard",
            PlayerGamemode.IRONMAN to "iron",
            PlayerGamemode.ULTIMATE_IRONMAN to "uim",
            PlayerGamemode.HARDCORE_IRONMAN to "hciron",
            PlayerGamemode.GROUP_IRONMAN to "iron",
            PlayerGamemode.HARDCORE_GROUP_IRONMAN to "hciron",
            PlayerGamemode.UNRANKED_GROUP_IRONMAN to "iron",
        )

    private val pools by lazy {
        val resource = checkNotNull(BotNames::class.java.getResourceAsStream("/bot-names.tsv"))
        val names = resource.bufferedReader().useLines { lines ->
            val rows = lines.iterator()
            check(rows.hasNext() && rows.next() == "mode\tname") { "Invalid bot name TSV header." }
            rows.asSequence().map { row ->
                val fields = row.split('\t', limit = 2)
                check(fields.size == 2 && fields[1].length in 1..12) { "Invalid bot name row: $row" }
                fields[0] to fields[1]
            }.groupBy({ it.first }, { it.second })
        }
        check(names.keys == poolsByMode.values.toSet()) { "Bot name TSV must contain all generator modes." }
        names
    }

    fun randomMode(random: GameRandom): Int = random.pick(poolsByMode.keys)

    /**
     * Selects an available display name from [mode]'s pool.
     *
     * Group modes use the generator's matching solo ironman style. [taken] contains lowercase
     * usernames for the currently active players and bots.
     */
    fun pick(random: GameRandom, mode: Int, taken: Set<String>): String? {
        val pool = pools.getValue(poolsByMode.getValue(mode))
        return random.pickOrNull(pool.filterNot { it.lowercase() in taken })
    }
}
