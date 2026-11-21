package org.rsmod.content.skills.agility

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class WyrmTermitesTest {
    private fun GameTestScope.scoopMultijump(
        termites: Int,
        shardRoll: Int,
        shards: Int = 0,
    ): List<String> {
        setAgility(99)
        startAt(START)
        advance()
        cross(LADDER)
        setVarbit(MARKER, MULTIJUMP)
        random.next = termites
        random.then = shardRoll
        if (shardRoll < SHARD_PERCENT) {
            random.then = shards
        }
        return cross(TIGHTROPE)
    }

    @Test
    fun GameTestState.`termites on the marker are scooped with bone shards`() = runAgilityTest {
        val messages = scoopMultijump(termites = 12, shardRoll = 79, shards = 30)

        assertEquals(12, player.inv.count(TERMITES))
        assertEquals(30, player.inv.count(BONE_SHARDS))
        assertEquals(0, player.vars[MARKER])
        assertTrue("You managed to scoop up <col=ff3045>12 termites</col>!" in messages)
    }

    @Test
    fun GameTestState.`a missed shard roll scoops termites alone`() = runAgilityTest {
        scoopMultijump(termites = 11, shardRoll = 80)

        assertEquals(11, player.inv.count(TERMITES))
        assertEquals(0, player.inv.count(BONE_SHARDS))
    }

    @Test
    fun GameTestState.`termites elsewhere are left for their own obstacle`() = runAgilityTest {
        setAgility(99)
        startAt(START)
        advance()
        cross(LADDER)
        setVarbit(MARKER, BASIC_JUMP)

        cross(TIGHTROPE)

        assertEquals(0, player.inv.count(TERMITES))
        assertEquals(BASIC_JUMP, player.vars[MARKER])
    }

    @Test
    fun GameTestState.`a full inventory drops the scoop at the player's feet`() =
        runAgilityTest { deps ->
            player.fillInv()
            val slots = player.inv.objs.toList()

            scoopMultijump(termites = 14, shardRoll = 0, shards = 22)

            assertEquals(slots, player.inv.objs.toList())
            assertEquals(14, groundCount(deps, TERMITES))
            assertEquals(22, groundCount(deps, BONE_SHARDS))
        }

    @Test
    fun GameTestState.`a scoop that would overflow the stack goes to the floor`() =
        runAgilityTest { deps ->
            player.inv[0] = InvObj(TERMITES, Int.MAX_VALUE - 5)

            scoopMultijump(termites = 12, shardRoll = 80)

            assertEquals(Int.MAX_VALUE - 5, player.inv.count(TERMITES))
            assertEquals(12, groundCount(deps, TERMITES))
        }

    @Test
    fun GameTestState.`termites are scooped once`() = runAgilityTest { deps ->
        scoopMultijump(termites = 13, shardRoll = 80)
        startAt(START)
        advance()
        cross(LADDER)
        cross(TIGHTROPE)

        assertEquals(13, player.inv.count(TERMITES) + groundCount(deps, TERMITES))
    }

    @Test
    fun GameTestState.`a lap can leave termites on one of its markers`() = runAgilityTest {
        setAgility(99)
        startAt(START)
        advance()
        crossAll(BASIC_LAP.dropLast(1))
        random.next = 34
        random.then = 3
        cross(BASIC_LAP.last())

        assertEquals(4, player.vars[MARKER])
    }

    @Test
    fun GameTestState.`a lap usually leaves no termites`() = runAgilityTest {
        setAgility(99)
        startAt(START)
        advance()
        crossAll(BASIC_LAP.dropLast(1))
        random.next = 35
        cross(BASIC_LAP.last())

        assertEquals(0, player.vars[MARKER])
    }

    private fun GameTestScope.groundCount(deps: AgilityTestDeps, obj: String): Int {
        val type = obj.asRSCM(RSCMType.OBJ)
        return deps.objRepo.findAll(player.coords).filter { it.type == type }.sumOf { it.count }
    }

    private companion object {
        const val MARKER = "varbit.varlamore_wyrm_agility_termite_obstacle"
        const val TERMITES = "obj.varlamore_wyrm_agility_termite"
        const val BONE_SHARDS = "obj.blessed_bone_shard"
        const val LADDER = "loc.varlamore_wyrm_agility_start_ladder_trigger"
        const val TIGHTROPE = "loc.varlamore_wyrm_agility_balance_1_trigger"
        const val SHARD_PERCENT = 80
        const val MULTIJUMP = 2
        const val BASIC_JUMP = 5
        val START = CoordGrid(1653, 2931, 0)
        val BASIC_LAP =
            listOf(
                LADDER,
                TIGHTROPE,
                "loc.varlamore_wyrm_agility_basic_balance_1_trigger",
                "loc.varlamore_wyrm_agility_basic_monkeybars_1_trigger",
                "loc.varlamore_wyrm_agility_basic_ladder_1_trigger",
                "loc.varlamore_wyrm_agility_end_zipline_trigger",
            )
    }
}
