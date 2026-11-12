package org.rsmod.content.other.bots

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.rsmod.api.player.ironman.PlayerGamemode
import org.rsmod.api.random.DefaultGameRandom

class BotNamesTest {
    @Test
    fun `each account mode picks from its pool and skips taken usernames`() {
        val modes =
            mapOf(
                PlayerGamemode.NORMAL to "standard",
                PlayerGamemode.IRONMAN to "iron",
                PlayerGamemode.ULTIMATE_IRONMAN to "uim",
                PlayerGamemode.HARDCORE_IRONMAN to "hciron",
                PlayerGamemode.GROUP_IRONMAN to "iron",
                PlayerGamemode.HARDCORE_GROUP_IRONMAN to "hciron",
                PlayerGamemode.UNRANKED_GROUP_IRONMAN to "iron",
            )
        val names = checkNotNull(javaClass.getResourceAsStream("/bot-names.tsv"))
            .bufferedReader().useLines { lines ->
                lines.drop(1).map { it.split('\t', limit = 2) }.groupBy({ it[0] }, { it[1] })
            }
        val random = DefaultGameRandom(0)

        for ((mode, pool) in modes) {
            val poolNames = names.getValue(pool)
            val taken = poolNames.drop(1).mapTo(HashSet()) { it.lowercase() }
            assertEquals(poolNames.first(), BotNames.pick(random, mode, taken))
            taken += poolNames.first().lowercase()
            assertNull(BotNames.pick(random, mode, taken))
        }
    }
}
