@file:OptIn(org.rsmod.annotations.InternalApi::class)

package org.rsmod.content.quest.area.asgarnia.blackknights

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.rsprot.protocol.game.outgoing.camera.CamLookAtV3
import net.rsprot.protocol.game.outgoing.camera.CamMoveToV3
import net.rsprot.protocol.game.outgoing.camera.CamReset
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.player.events.interact.LocUDefaultEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
class BlackKnightsSceneTest {
    @Test
    fun `native cabbage use frames a private scene and completes only after restoring the player`() {
        native { f ->
            assertTrue(f.operate())
            assertEquals(1, f.map.player.inv.count("obj.cabbage"))
            assertEquals(2, f.stage)
            f.map.tick(12)
            assertEquals(2, f.stage)
            assertEquals(CamMoveToV3(7035, 7499, 1500, 100, 100, false),
                f.messages.map { it.second }.filterIsInstance<CamMoveToV3>().single())
            assertEquals(CamLookAtV3(7032, 7505, 300, 100, 100, false),
                f.messages.map { it.second }.filterIsInstance<CamLookAtV3>().single())
            val cameraTick = f.messages.single { it.second is CamMoveToV3 }.first
            assertEquals(f.throwTick + 9, cameraTick)
            assertEquals(listOf(130, 0, 96, 196, 55, 60), f.flights.first().let {
                listOf(it.startHeight, it.endHeight, it.startTime, it.endTime, it.angle, it.progress)
            })
            f.drain()
            assertEquals(f.returnTo, f.map.player.coords)
            assertFalse(f.active)
            assertEquals(3, f.stage)
            assertEquals(12, f.map.player.vars["varp.qp"])
            assertEquals(0, f.map.player.inv.count("obj.coins"))
            assertTrue(f.messages.any { it.second == CamReset })
            assertEquals(listOf(500, 50, 0, 100, 192, 128), f.flights.last().let {
                listOf(it.startHeight, it.endHeight, it.startTime, it.endTime, it.angle, it.progress)
            })
            assertTrue(f.operate())
            assertEquals(1, f.map.player.inv.count("obj.cabbage"))
            verify(exactly = 1) { f.instances.create(any(), any(), any(), any(), any()) }
        }
    }

    @Test
    fun `interrupting the camera scene restores position and permits a fresh cabbage attempt`() {
        native { f ->
            assertTrue(f.operate())
            f.map.tick(12)
            assertFalse(f.operate())
            assertEquals(1, f.map.player.inv.count("obj.cabbage"))
            f.map.player.cancelActiveCoroutine()
            assertEquals(f.returnTo, f.map.player.coords)
            assertFalse(f.active)
            assertEquals(2, f.stage)
            assertTrue(f.messages.any { it.second == CamReset })
            assertTrue(f.operate())
            f.drain()
            assertEquals(3, f.stage)
            assertEquals(0, f.map.player.inv.count("obj.cabbage"))
            assertEquals(f.returnTo, f.map.player.coords)
        }
    }

    private fun native(body: (Fixture) -> Unit) {
        val cache = ServerCacheManager.init(242)
        try { body(Fixture(BlackKnightsMapFixture(cache, CoordGrid(3031, 3507, 1), 12))) } finally { cache.close() }
    }

    internal class Fixture(val map: BlackKnightsMapFixture, initializePlayer: Boolean = true) {
        var returnTo = CoordGrid(3031, 3506, 1)
        val messages = mutableListOf<Pair<Int, Any>>()
        val flights = mutableListOf<ProjAnim>()
        val instances = mockk<InstanceManager>(relaxed = true)
        var active = false
        var throwTick = 0
        private val session = mockk<InstanceSession>(relaxed = true)
        private val world = mockk<WorldRepository>(relaxed = true)
        private val scene = BlackKnightsScene(instances, world, map.clock)

        init {
            val client = mockk<Client<Any, Any>>(relaxed = true)
            every { client.write(any()) } answers { messages += map.clock.cycle to firstArg<Any>() }
            map.player.client = client
            map.player.uuid = 104L
            map.player.slotId = 1
            map.player.assignUid()
            if (initializePlayer) {
                map.player.coords = returnTo
                map.player.inv[0] = InvObj("obj.cabbage")
                map.player.inv[1] = InvObj("obj.cabbage")
                VarPlayerIntMapSetter.set(map.player, BlackKnightsProgress.STAGE, 2)
                VarPlayerIntMapSetter.set(map.player, "varp.qp", 12)
            }
            every { session.id } returns InstanceId(1L)
            every { instances.create(any(), any(), any(), any(), any()) } answers {
                returnTo = map.player.coords
                InstanceManager.Result.Created(session, CoordGrid(7017, 7497))
            }
            every { instances.resolveCoord(session, any()) } answers {
                val c = CoordGrid(secondArg<Int>())
                CoordGrid(c.x + 4000, c.z + 4000, c.level)
            }
            val actors = listOf("npc.fortwitch", "npc.grillknight", "npc.bkf_cat").mapIndexed { i, type ->
                Npc(type, CoordGrid(7030 + i, 7509)).apply { slotId = i + 2; assignUid() }
            }
            every { instances.npcsForInstance(any()) } returns actors
            every { instances.finalizeEntry(any(), session, any()) } answers { active = true }
            every { instances.sessionForPlayer(map.player) } answers { if (active) session else null }
            every { instances.leave(map.player, session, any()) } answers { active = false; returnTo }
            every { world.projAnim(capture(flights)) } answers { Unit }
            with(BlackKnightsFortress(scene)) {
                ScriptContext(map.events, CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        val stage get() = BlackKnightsProgress.stage(map.player)

        fun operate(): Boolean {
            throwTick = map.clock.cycle
            val loc = map.find("loc.blackknighthole", CoordGrid(3031, 3507, 1))
            val type = checkNotNull(ServerCacheManager.getObject(loc.id))
            val cabbage = checkNotNull(ServerCacheManager.getItem("obj.cabbage".asRSCM()))
            return ProtectedAccessLauncher.withProtectedAccess(map.player, map.context) {
                assertTrue(map.events.publish(this, LocUDefaultEvents.OpType(loc, loc, type, cabbage, 0)))
            }
        }

        fun drain() {
            repeat(200) {
                val c = map.player.activeCoroutine
                if (c == null || c.isIdle) { map.player.ifClose(map.events); return }
                if (c.isAwaiting(ResumePauseButtonInput::class)) {
                    val box = listOf("chat_left", "chat_right", "messagebox").first {
                        map.player.ui.containsModal("interface.$it")
                    }
                    c.resumeWith(ResumePauseButtonInput("component.$box:continue", -1))
                } else map.tick(1)
            }
            error("Scene did not finish")
        }
    }

    companion object {
        @JvmStatic @BeforeAll fun setup() = BlackKnightsFortressTest.cache()

        @JvmStatic @AfterAll fun cleanup() = BlackKnightsFortressTest.restore()
    }
}
