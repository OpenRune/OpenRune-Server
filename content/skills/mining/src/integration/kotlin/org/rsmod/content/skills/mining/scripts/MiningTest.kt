package org.rsmod.content.skills.mining.scripts

import dev.openrune.types.ObjectServerType
import org.junit.jupiter.api.Test
import org.rsmod.api.player.righthand
import org.rsmod.api.table.mining.MiningRocksRow
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.mining.configs.MiningRocks
import org.rsmod.content.skills.mining.configs.hasBonusRolls
import org.rsmod.content.skills.mining.configs.hasDepleteRange
import org.rsmod.content.skills.mining.configs.isInfinite
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class MiningTest {
    @Test
    fun GameTestState.`a successful roll mines one ore and depletes the rock`() =
        runGameTest(Mining::class) {
            // given
            val type = findDepletingRock()
            val data = rockData(type)
            val rock = placeMapLoc(ROCK, type)
            player.teleport(ROCK.translateX(-1))
            player.righthand = InvObj(PICKAXE)
            setMiningLevel(data.level)
            random.next = SUCCESS_ROLL

            // when
            player.opLoc1(rock)
            advance(ticks = TICKS_TO_FIRST_ROLL)

            // then
            assertEquals(1, player.inv.count(data.oreItem!!))
            assertDoesNotExist(rock)
            assertExists(ROCK, data.emptyRockObject!!)
        }

    @Test
    fun GameTestState.`a failed roll mines nothing and leaves the rock`() =
        runGameTest(Mining::class) {
            // given
            val type = findDepletingRock()
            val data = rockData(type)
            val rock = placeMapLoc(ROCK, type)
            player.teleport(ROCK.translateX(-1))
            player.righthand = InvObj(PICKAXE)
            setMiningLevel(data.level)
            random.next = FAIL_ROLL

            // when
            player.opLoc1(rock)
            advance(ticks = TICKS_TO_FIRST_ROLL)

            // then
            assertEquals(0, player.inv.count(data.oreItem!!))
            assertExists(rock)
        }

    @Test
    fun GameTestState.`mining below the rock's level is refused`() =
        runGameTest(Mining::class) {
            // given
            val type = findDepletingRock()
            val data = rockData(type)
            val rock = placeMapLoc(ROCK, type)
            player.teleport(ROCK.translateX(-1))
            player.righthand = InvObj(PICKAXE)
            setMiningLevel(data.level - 1)

            // when
            player.opLoc1(rock)
            advance()

            // then
            assertMessageSent("You need a Mining level of ${data.level} to mine this rock.")
            assertEquals(0, player.inv.count(data.oreItem!!))
        }

    @Test
    fun GameTestState.`mining with a full inventory is refused`() =
        runGameTest(Mining::class) {
            // given
            val type = findDepletingRock()
            val data = rockData(type)
            val rock = placeMapLoc(ROCK, type)
            player.teleport(ROCK.translateX(-1))
            player.righthand = InvObj(PICKAXE)
            player.fillInv()
            setMiningLevel(data.level)

            // when
            player.opLoc1(rock)
            advance()

            // then
            val ore = data.oreItem!!.name.lowercase()
            assertMessageSent("Your inventory is too full to hold any more $ore.")
            assertExists(rock)
        }

    private fun GameTestScope.findDepletingRock(): ObjectServerType =
        findLocType("content.rock") { type ->
            val data = MiningRocks.forLoc(type)
            data != null &&
                data.oreItem != null &&
                data.emptyRockObject != null &&
                !data.hasBonusRolls &&
                !data.isInfinite &&
                !data.hasDepleteRange
        }

    private fun rockData(type: ObjectServerType): MiningRocksRow =
        checkNotNull(MiningRocks.forLoc(type))

    private fun GameTestScope.setMiningLevel(level: Int) {
        player.setBaseLevel("stat.mining", level)
        player.setCurrentLevel("stat.mining", level)
    }

    private companion object {
        const val PICKAXE = "obj.bronze_pickaxe"
        const val SUCCESS_ROLL = 0
        const val FAIL_ROLL = 99
        const val TICKS_TO_FIRST_ROLL = 4
        val ROCK = CoordGrid(0, 50, 50, 34, 31)
    }
}
