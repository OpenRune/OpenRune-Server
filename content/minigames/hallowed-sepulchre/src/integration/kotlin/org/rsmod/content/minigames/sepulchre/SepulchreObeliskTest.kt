package org.rsmod.content.minigames.sepulchre

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.back
import org.rsmod.api.player.front
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SepulchreObeliskTest {
    private fun GameTestState.runObeliskTest(body: GameTestScope.() -> Unit) =
        runGameTest(SepulchreScript::class, PlayerHitScript::class) {
            advance()
            setLevel(HITPOINTS, 99)
            body()
        }

    private fun GameTestScope.pray(): List<String> {
        val obelisk = place(OBELISK, CoordGrid(2398, 5860, 1), angle = 0)
        startAt(CoordGrid(2399, 5861, 1))
        player.runEnergy = HALF_ENERGY
        player.opLoc1(obelisk)
        return advanceCollecting(5)
    }

    @Test
    fun GameTestState.`a saradomin item restores the player's energy`() = runObeliskTest {
        player.front = InvObj("obj.hallowed_symbol", 1)

        val messages = pray()

        assertTrue(SARADOMIN in messages)
        assertEquals(FULL_ENERGY, player.runEnergy)
        assertEquals(99, player.hitpoints)
    }

    @Test
    fun GameTestState.`another god's item costs energy and a hitpoint`() = runObeliskTest {
        player.back = InvObj("obj.zamorak_cape", 1)

        val messages = pray()

        assertTrue(ANOTHER_GOD in messages)
        assertTrue(player.runEnergy < HALF_ENERGY)
        assertEquals(98, player.hitpoints)
    }

    @Test
    fun GameTestState.`another god's item outweighs a saradomin one`() = runObeliskTest {
        player.front = InvObj("obj.hallowed_symbol", 1)
        player.back = InvObj("obj.zamorak_cape", 1)

        val messages = pray()

        assertTrue(ANOTHER_GOD in messages)
        assertTrue(SARADOMIN !in messages)
    }

    @Test
    fun GameTestState.`the obelisk ignores a player without a god's item`() = runObeliskTest {
        val messages = pray()

        assertTrue(NO_FAITH in messages)
        assertTrue(player.runEnergy in HALF_ENERGY until FULL_ENERGY)
        assertEquals(99, player.hitpoints)
    }

    @Test
    fun GameTestState.`a crystal shard is used up and reaches the lobby three ticks later`() =
        runObeliskTest {
            startAt(CoordGrid(3222, 3218, 0))
            player.inv[0] = InvObj(SHARD, 2)
            allocZoneCollision(SHARD_ARRIVAL)

            player.withProtectedAccess { opHeld1(0) }
            assertEquals(1, player.inv.count(SHARD))
            advance(2)
            assertEquals(CoordGrid(3222, 3218, 0), player.coords)
            advance()

            assertEquals(SHARD_ARRIVAL, player.coords)
            assertEquals(1, player.inv.count(SHARD))
        }

    @Test
    fun GameTestState.`a shard used on a floor ends the run`() = runObeliskTest {
        onFloor(2, CoordGrid(2532, 5984, 2), remaining = 200)
        player.inv[0] = InvObj(SHARD, 1)
        allocZoneCollision(SHARD_ARRIVAL)

        player.withProtectedAccess { opHeld1(0) }
        advance(3)
        assertEquals(SHARD_ARRIVAL, player.coords)
        assertEquals(0, player.vars[FLOOR])
        assertEquals(0, player.vars[REMAINING])

        val messages = advanceCollecting(1)
        assertTrue(DOOR_CLOSES in messages)
        assertEquals(0, player.inv.count(SHARD))
    }

    private companion object {
        const val HITPOINTS = "stat.hitpoints"
        const val OBELISK = "loc.hallowed_exit"
        const val SHARD = "obj.hallowed_teleport"
        const val HALF_ENERGY = 5000
        const val FULL_ENERGY = 10_000
        const val SARADOMIN =
            "<col=229628>The obelisk senses your loyalty to Saradomin and fully restores your " +
                "energy."
        const val ANOTHER_GOD =
            "<col=e00a19>The obelisk senses your loyalty to another god and steals some of your " +
                "energy from you."
        const val NO_FAITH = "The obelisk doesn't respond to your lack of faith."
        const val DOOR_CLOSES =
            "<col=ef1020>You hear a loud rumbling noise as the door to the next floor closes."
        val SHARD_ARRIVAL = CoordGrid(2401, 5981, 0)
    }
}
