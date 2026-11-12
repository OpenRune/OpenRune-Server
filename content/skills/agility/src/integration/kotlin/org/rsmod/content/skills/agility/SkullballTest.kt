package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.npc.owner.isSpawnOwnedBy
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.ring
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.agility.werewolf.SkullballScript
import org.rsmod.content.skills.agility.werewolf.WerewolfCourseScript
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class SkullballTest {
    @Test
    fun GameTestState.`the boss turns away a player without a ring of charos`() =
        runSkullballTest {
            val boss = spawnBoss()

            player.opNpc1(boss)
            advanceUntil({ player.ui.containsModal(NPC_CHAT) })

            assertEquals(null, ballOf())
        }

    @Test
    fun GameTestState.`the boss gives a player wearing the ring a ball from a spawnhole`() =
        runSkullballTest {
            player.ring = InvObj(RING, 1)

            val ball = startGame()

            assertTrue(ball.coords in SPAWNS, "Ball at ${ball.coords}")
            assertTrue(ball.isSpawnOwnedBy(player))
        }

    @Test
    fun GameTestState.`a goal out of order does not count`() = runSkullballTest {
        player.ring = InvObj(RING, 1)
        val ball = startGame()

        tapThrough(ball, GOALS[1])

        assertEquals(0, player.vars[GOALS_SCORED])
        assertEquals(0, player.vars[START_CLOCK])
    }

    @Test
    fun GameTestState.`every goal in order and the final shot pay the full xp`() =
        runSkullballTest {
            player.ring = InvObj(RING, 1)
            val ball = startGame()
            val before = agilityXp()

            for ((index, goal) in GOALS.withIndex()) {
                tapThrough(ball, goal)
                assertEquals(index + 1, player.vars[GOALS_SCORED])
            }
            tapThrough(ball, HOLE)

            assertEquals(before + fineXp(750.0), agilityXp())
            assertNull(ballOf())
            assertEquals(0, player.vars[GOALS_SCORED])
        }

    @Test
    fun GameTestState.`a slow game pays eight xp less for each three seconds over four minutes`() =
        runSkullballTest {
            player.ring = InvObj(RING, 1)
            val ball = startGame()
            val before = agilityXp()

            tapThrough(ball, GOALS.first())
            val start = player.vars[START_CLOCK]
            advance(ticks = 430)
            for (goal in GOALS.drop(1)) {
                tapThrough(ball, goal)
            }
            val finished = tapThrough(ball, HOLE)

            val elapsed = finished - start
            val expected = 750 - 8 * ((elapsed - 400) / 5)
            assertTrue(expected in 1 until 750, "Elapsed $elapsed")
            assertEquals(before + fineXp(expected.toDouble()), agilityXp())
        }

    @Test
    fun GameTestState.`a ball whose owner leaves the area is taken away`() = runSkullballTest {
        player.ring = InvObj(RING, 1)
        val ball = startGame()

        player.teleport(CoordGrid(3222, 3218, 0))
        advance(ticks = 2)

        assertTrue(!ball.isSlotAssigned)
        assertEquals(0, player.vars[GOALS_SCORED])
    }

    /**
     * Puts the ball a tile short of [goal] with the player behind it and taps it through; returns
     * the map clock of the tick the ball went through.
     */
    private fun GameTestScope.tapThrough(ball: Npc, goal: Goal): Int {
        ball.telejump(goal.centre.translate(-goal.dx, -goal.dz))
        startAt(goal.centre.translate(-2 * goal.dx, -2 * goal.dz))
        val xp = agilityXp()
        val scored = player.vars[GOALS_SCORED]
        player.opNpc1(ball)
        repeat(6) {
            advance()
            val moved = ball.coords == goal.centre || player.vars[GOALS_SCORED] != scored
            if (moved || agilityXp() != xp) {
                return mapClock.cycle - 1
            }
        }
        error("The ball never reached ${goal.centre} (at ${ball.coords}).")
    }

    private fun GameTestScope.startGame(): Npc {
        val boss = spawnBoss()
        player.opNpc1(boss)
        advanceUntil({ player.ui.containsModal(MENU) })
        choose(1)
        advanceUntil({ player.ui.containsModal(PLAYER_CHAT) })
        continueDialogue(PLAYER_CONTINUE)
        advance()
        return checkNotNull(ballOf()) { "No ball was spawned." }
    }

    private fun GameTestScope.spawnBoss(): Npc {
        startAt(BOSS_HOME.translateX(-1))
        val boss = checkNotNull(ServerCacheManager.getNpc(BOSS.asRSCM(RSCMType.NPC)))
        return spawnNpc(BOSS_HOME, boss)
    }

    private fun GameTestScope.ballOf(): Npc? =
        SPAWNS.plus(GOALS.map { it.centre })
            .flatMap { npcRepo.findAll(ZoneKey.from(it), 2).toList() }
            .distinct()
            .firstOrNull { it.id == BALL.asRSCM(RSCMType.NPC) && it.isSpawnOwnedBy(player) }

    private fun GameTestScope.choose(option: Int) {
        val input = ResumePauseButtonInput("component.chatmenu:options", option)
        player.activeCoroutine?.resumeWith(input)
        advance()
    }

    private fun GameTestScope.continueDialogue(component: String) {
        player.activeCoroutine?.resumeWith(ResumePauseButtonInput(component, -1))
        advance()
    }

    private fun GameTestState.runSkullballTest(body: GameTestScope.() -> Unit) =
        runAgilityTest(WerewolfCourseScript::class, SkullballScript::class) {
            setAgility(99)
            try {
                body()
            } finally {
                (SPAWNS + BOSS_HOME + GOALS.map { it.centre })
                    .flatMap { npcRepo.findAll(ZoneKey.from(it), 2).toList() }
                    .distinct()
                    .filter { it.isSlotAssigned }
                    .forEach { npcRepo.del(it, Int.MAX_VALUE) }
            }
        }

    private class Goal(val centre: CoordGrid, val dx: Int, val dz: Int)

    private companion object {
        const val BOSS = "npc.werewolf_skullballboss"
        const val BALL = "npc.waa_skullball"
        const val RING = "obj.ring_of_charos"
        const val GOALS_SCORED = "varbit.agility_skullball_goals"
        const val START_CLOCK = "varp.agility_skullball_start"
        const val MENU = "interface.chatmenu"
        const val NPC_CHAT = "interface.chat_left"
        const val PLAYER_CHAT = "interface.chat_right"
        const val PLAYER_CONTINUE = "component.chat_right:continue"

        val BOSS_HOME = CoordGrid(3549, 9867, 0)
        val SPAWNS =
            listOf(
                CoordGrid(3552, 9860, 0),
                CoordGrid(3554, 9861, 0),
                CoordGrid(3555, 9861, 0),
                CoordGrid(3557, 9860, 0),
            )
        val GOALS =
            listOf(
                Goal(CoordGrid(3555, 9870, 0), 0, 1),
                Goal(CoordGrid(3556, 9883, 0), 0, 1),
                Goal(CoordGrid(3558, 9891, 0), 0, 1),
                Goal(CoordGrid(3557, 9900, 0), 0, 1),
                Goal(CoordGrid(3558, 9906, 0), 0, 1),
                Goal(CoordGrid(3563, 9911, 0), 1, 0),
                Goal(CoordGrid(3575, 9905, 0), 0, -1),
                Goal(CoordGrid(3574, 9888, 0), 0, -1),
                Goal(CoordGrid(3575, 9878, 0), 0, -1),
                Goal(CoordGrid(3568, 9864, 0), -1, 0),
            )
        val HOLE = Goal(CoordGrid(3563, 9866, 0), 0, 1)
    }
}
