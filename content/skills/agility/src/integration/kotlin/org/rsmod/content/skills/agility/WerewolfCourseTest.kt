package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.agility.werewolf.WerewolfCourseScript
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NoopNpcInfo
import org.rsmod.game.entity.npc.NpcInfoProtocol
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class WerewolfCourseTest {
    @Test
    fun GameTestState.`the agility boss throws the stick as the first stone is jumped`() =
        runWerewolfTest { deps ->
            val boss = spawnRecorded(BOSS_HOME, BOSS)
            startAt(START)
            player.opLoc1(locNear(STONE, FIRST_STONE))

            var thrownAt = -1
            var landedAt = -1
            for (tick in 1..30) {
                advance()
                if (thrownAt < 0 && FETCH in boss.recorder.says) {
                    thrownAt = tick
                    assertEquals(FIRST_STONE, player.coords)
                }
                if (landedAt < 0 && sticksOnGround(deps) > 0) {
                    landedAt = tick
                }
            }

            assertTrue(thrownAt > 0, "The boss never shouted.")
            assertEquals(8, landedAt - thrownAt)
            assertEquals(listOf(THROW_SEQ.asRSCM(RSCMType.SEQ)), boss.recorder.seqs)
            assertEquals(1, sticksOnGround(deps))
            assertEquals(0, player.inv.count(STICK))
        }

    @Test
    fun GameTestState.`a player still holding a stick is asked for it and gets no other`() =
        runWerewolfTest { deps ->
            val boss = spawnRecorded(BOSS_HOME, BOSS)
            player.inv[0] = InvObj(STICK, 1)
            startAt(START)

            crossNear(STONE, FIRST_STONE)
            advance(ticks = 12)

            assertEquals(listOf("Why didn't you hand the stick over?!!?"), boss.recorder.says)
            assertTrue(boss.recorder.seqs.isEmpty())
            assertEquals(0, sticksOnGround(deps))
            assertEquals(1, player.inv.count(STICK))
        }

    @Test
    fun GameTestState.`leaving the area takes every stick and nothing else`() =
        runWerewolfTest {
            fillInventory()
            startAt(START)

            player.teleport(OUTSIDE)
            val messages = advanceCollecting(ticks = 2)

            assertEquals(listOf(STICK_TAKEN), messages)
            assertEquals(0, player.inv.count(STICK))
            assertEquals(COIN_COUNT, player.inv.count(COINS))
            assertEquals(1, player.inv.count(SHARK))
        }

    @Test
    fun GameTestState.`moving about inside the area keeps the stick`() = runWerewolfTest {
        fillInventory()
        startAt(START)

        player.teleport(SLIDE_TOP)
        val messages = advanceCollecting(ticks = 2)

        assertTrue(messages.isEmpty())
        assertEquals(2, player.inv.count(STICK))
    }

    @Test
    fun GameTestState.`the exit ladder takes the stick as the player climbs out`() =
        runWerewolfTest {
            placeMapLoc(LADDER, EXIT_LADDER, LocShape[10], LocAngle[0])
            fillInventory()
            startAt(LADDER.translateZ(1))

            player.opLoc1(ladder())
            val messages = advanceCollecting(ticks = 6)

            assertEquals(CoordGrid(3543, 3463, 0), player.coords)
            assertEquals(listOf(STICK_TAKEN), messages)
            assertEquals(0, player.inv.count(STICK))
            assertEquals(COIN_COUNT, player.inv.count(COINS))
            assertEquals(1, player.inv.count(SHARK))
        }

    @Test
    fun GameTestState.`logging out takes the stick and nothing else`() = runWerewolfTest {
        fillInventory()
        startAt(START)

        eventBus.publish(SessionStateEvent.Logout(player))
        eventBus.publish(SessionStateEvent.Logout(player))

        assertEquals(0, player.inv.count(STICK))
        assertEquals(COIN_COUNT, player.inv.count(COINS))
        assertEquals(1, player.inv.count(SHARK))
    }

    private fun GameTestState.runWerewolfTest(body: GameTestScope.(AgilityTestDeps) -> Unit) =
        runAgilityTest(WerewolfCourseScript::class) { deps ->
            setAgility(99)
            try {
                body(deps)
            } finally {
                npcRepo.findAll(ZoneKey.from(BOSS_HOME), 1).toList().forEach {
                    npcRepo.del(it, Int.MAX_VALUE)
                }
            }
        }

    private fun GameTestScope.fillInventory() {
        player.inv[0] = InvObj(STICK, 1)
        player.inv[1] = InvObj(COINS, COIN_COUNT)
        player.inv[2] = InvObj(STICK, 1)
        player.inv[3] = InvObj(SHARK, 1)
    }

    private fun GameTestScope.ladder(): BoundLocInfo {
        val type = checkNotNull(ServerCacheManager.getObject(EXIT_LADDER.asRSCM(RSCMType.LOC)))
        val info = findLocs(LADDER).first { it.id == type.id }
        return BoundLocInfo(info, type)
    }

    private fun GameTestScope.spawnRecorded(coords: CoordGrid, npc: String): RecordedNpc {
        val type = checkNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)))
        val spawned = spawnNpc(coords, type)
        val recorder = NpcRecorder()
        spawned.infoProtocol = recorder
        return RecordedNpc(spawned, recorder)
    }

    private fun GameTestScope.sticksOnGround(deps: AgilityTestDeps): Int {
        val stick = STICK.asRSCM(RSCMType.OBJ)
        return STICK_LANDINGS.sumOf { tile ->
            deps.objRepo.findAll(tile).count { it.type == stick }
        }
    }

    private class RecordedNpc(val npc: Npc, val recorder: NpcRecorder)

    private class NpcRecorder : NpcInfoProtocol by NoopNpcInfo {
        val says = mutableListOf<String>()
        val seqs = mutableListOf<Int>()

        override fun setSay(text: String) {
            says += text
        }

        override fun setSequence(seq: Int, delay: Int) {
            if (seq != -1) {
                seqs += seq
            }
        }
    }

    private companion object {
        const val STONE = "loc.werewolf_steping_stone"
        const val BOSS = "npc.werewolf_trainer_start"
        const val THROW_SEQ = "seq.werewolf_update_throw_stick"
        const val FETCH = "FETCH!!!!!"
        const val STICK = "obj.waa_stick"
        const val COINS = "obj.coins"
        const val SHARK = "obj.shark"
        const val COIN_COUNT = 500
        const val EXIT_LADDER = "loc.waa_ladder"
        const val STICK_TAKEN = "The werewolf trainer removes your stick as you leave."

        val START = CoordGrid(3538, 9873, 0)
        val FIRST_STONE = CoordGrid(3538, 9875, 0)
        val BOSS_HOME = CoordGrid(3540, 9873, 0)
        val SLIDE_TOP = CoordGrid(3528, 9909, 0)
        val LADDER = CoordGrid(3549, 9864, 0)
        val OUTSIDE = CoordGrid(3543, 3463, 0)
        val STICK_LANDINGS = listOf(CoordGrid(3538, 9912, 0), CoordGrid(3543, 9911, 0))
    }
}
