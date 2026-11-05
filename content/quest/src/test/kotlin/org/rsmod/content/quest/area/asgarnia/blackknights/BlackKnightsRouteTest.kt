@file:OptIn(org.rsmod.annotations.InternalApi::class)

package org.rsmod.content.quest.area.asgarnia.blackknights

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.npc.aggression.AggressionTolerance
import org.rsmod.api.player.events.interact.LocUDefaultEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.route.*
import org.rsmod.content.areas.misc.blackknights.FortressGuard
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.*

@ResourceLock("ServerCacheManager")
class BlackKnightsRouteTest {
    @Test
    fun `native White Knights tower reaches Sir Amik and both reciprocal staircase landings`() {
        native { cache ->
            val f =
                BlackKnightsMapFixture(
                    cache,
                    CoordGrid(2960, 3340),
                    42,
                    npcRadius = 40,
                    extraCentres = listOf(CoordGrid(2960, 3336, 2)),
                )
            val street = CoordGrid(2945, 3371)
            fun step(symbol: String, at: CoordGrid) {
                val l = f.find(symbol, at)
                f.player.coords = f.approach(f.player.coords, l)
                f.operate(l, 1)
                f.tick(3)
                assertTrue(f.walkable(f.player.coords), "Tower landing ${f.player.coords}")
            }
            f.player.coords = street
            val door = f.find("loc.fai_falador_castledoubledoorr", CoordGrid(2965, 3339))
            f.player.coords = f.approach(street, door)
            f.operate(door, 1)
            f.tick(3)
            step("loc.fai_falador_castle_spiralstairs", CoordGrid(2954, 3338))
            step("loc.fai_falador_castle_spiralstairs", CoordGrid(2960, 3338, 1))
            val amik = f.npcs.single { it.first == "npc.sir_amik_varze".asRSCM() }.second
            assertEquals(2, amik.level)
            f.connected(f.player.coords, amik)
            step("loc.fai_falador_castle_spiralstairstop", CoordGrid(2960, 3339, 2))
            step("loc.fai_falador_castle_spiralstairstop", CoordGrid(2955, 3338, 1))
            f.connected(f.player.coords, street)
        }
    }

    @Test
    fun `complete native fortress route walks every ladder landing wall grill meeting and hole and returns without uniform`() {
        native { cache ->
            val f = BlackKnightsMapFixture(cache, CoordGrid(3020, 3514), 30)
            script(f, FortressGuard(f.repository, AggressionTolerance(f.clock)))
            script(f, BlackKnightsFortress())
            fun op(type: String, at: CoordGrid) {
                val l = f.find(type, at)
                f.player.coords = f.approach(f.player.coords, l)
                f.operate(l, 1)
                f.tick(3)
            }
            fun climb(type: String, x: Int, z: Int, level: Int) {
                op(type, CoordGrid(x, z, level))
                assertTrue(
                    f.walkable(f.player.coords),
                    "Blocked fortress landing ${f.player.coords}",
                )
            }
            val street = CoordGrid(3016, 3512)
            val entry = f.find("loc.bkfortressdoor1", FortressGuard.ENTRANCE)
            f.player.coords = f.approach(street, entry)
            f.operate(entry, 1)
            f.tick(3)
            assertNotNull(f.repository.findExact(entry.coords, entry.shape))
            f.player.worn[0] = InvObj("obj.bronze_med_helm")
            f.player.worn[4] = InvObj("obj.iron_chainbody")
            f.operate(entry, 1)
            f.tick(3)
            assertNull(f.repository.findExact(entry.coords, entry.shape))
            f.connected(f.player.coords, CoordGrid(3016, 3516))
            f.player.coords = CoordGrid(3016, 3516)
            f.player.worn.fillNulls()
            org.rsmod.api.player.vars.VarPlayerIntMapSetter.set(
                f.player,
                "varbit.black_knights_progress",
                1,
            )
            op("loc.bksecretdoor", CoordGrid(3016, 3517))
            climb("loc.dk_ladder", 3015, 3519, 0)
            climb("loc.dk_ladder", 3016, 3519, 1)
            climb("loc.dk_laddertop", 3017, 3516, 2)
            op("loc.wild_door", CoordGrid(3019, 3515, 1))
            climb("loc.dk_ladder", 3023, 3513, 1)
            climb("loc.dk_laddertop", 3025, 3513, 2)
            op("loc.bkfortressdoor3", CoordGrid(3025, 3511, 1))
            climb("loc.dk_laddertop", 3021, 3510, 1)
            op("loc.witchgrill", CoordGrid(3026, 3507))
            dialogue(f, 1)
            assertEquals(2, f.player.vars["varbit.black_knights_progress"])
            climb("loc.dk_ladder", 3021, 3510, 0)
            climb("loc.dk_ladder", 3025, 3513, 1)
            climb("loc.dk_laddertop", 3023, 3513, 2)
            climb("loc.dk_ladder", 3017, 3516, 1)
            climb("loc.dk_laddertop", 3016, 3519, 2)
            climb("loc.dk_laddertop", 3015, 3519, 1)
            val meeting = f.find("loc.bkfortressdoor2", FortressGuard.MEETING)
            f.player.coords = f.approach(f.player.coords, meeting)
            f.operate(meeting, 1)
            dialogue(f, 1)
            assertNotNull(f.repository.findExact(meeting.coords, meeting.shape))
            f.operate(meeting, 1)
            dialogue(f, 2)
            assertNull(f.repository.findExact(meeting.coords, meeting.shape))
            climb("loc.dk_meeting_ladder", 3022, 3518, 0)
            op("loc.bksecretdoor", CoordGrid(3030, 3510, 1))
            val hole = f.find("loc.blackknighthole", CoordGrid(3031, 3507, 1))
            f.player.coords = f.approach(f.player.coords, hole)
            f.player.inv[0] = InvObj("obj.cabbage")
            assertTrue(
                ProtectedAccessLauncher.withProtectedAccess(f.player, f.context) {
                    assertTrue(
                        f.events.publish(
                            this,
                            LocUDefaultEvents.OpType(
                                hole,
                                hole,
                                checkNotNull(ServerCacheManager.getObject(hole.id)),
                                checkNotNull(ServerCacheManager.getItem("obj.cabbage".asRSCM())),
                                0,
                            ),
                        )
                    )
                }
            )
            f.player.advanceActiveCoroutine()
            assertEquals(3, f.player.vars["varbit.black_knights_progress"])
            assertEquals(0, f.player.inv.count("obj.cabbage"))
            climb("loc.dk_meeting_laddertop", 3022, 3518, 1)
            f.connected(f.player.coords, CoordGrid(3016, 3515))
            f.player.coords = CoordGrid(3016, 3515)
            f.repository.add(
                org.rsmod.game.loc.LocInfo(entry.layer, entry.coords, entry.entity),
                500,
            )
            f.operate(entry, 1)
            f.tick(3)
            assertNull(f.repository.findExact(entry.coords, entry.shape))
            f.connected(f.player.coords, street)
            assertEquals(0, f.player.vars["varp.qp"])
            println(
                "Native full outward/return route connected; supplied stage1 and material is isolated route-component QA, not earned full client play."
            )
        }
    }

    @Test
    fun `native black knight hunters threaten sixty six combat and deny sixty seven outside wilderness`() {
        native { _ ->
            for (type in FortressGuard.KNIGHTS) for (level in listOf(66, 67)) {
                val clock = org.rsmod.game.MapClock(100)
                val hunt = mockk<org.rsmod.api.hunt.Hunt>()
                val random = mockk<org.rsmod.api.random.GameRandom>()
                val tolerance = AggressionTolerance(clock)
                tolerance.enroll(type)
                val player =
                    org.rsmod.game.entity.Player().apply {
                        coords = CoordGrid(3017, 3512)
                        slotId = 1
                        uuid = 1
                        observerUUID = 1
                        assignUid()
                        appearance.combatLevel = level
                    }
                val npc =
                    spyk(
                        org.rsmod.game.entity.Npc(type, player.coords).apply {
                            slotId = 1
                            assignUid()
                            huntClock = 100
                        }
                    )
                assertEquals(33, npc.type.combatLevel)
                assertNotNull(npc.huntMode)
                assertEquals(5, npc.huntRange)
                every { npc.isAnyoneNear() } returns true
                every { hunt.findPlayers(any(), any(), any()) } answers { sequenceOf(player) }
                every { random.of(minInclusive = any<Int>(), maxInclusive = any<Int>()) } answers
                    {
                        0
                    }
                org.rsmod.api.game.process.npc.hunt
                    .NpcPlayerHuntProcessor(random, clock, hunt, tolerance)
                    .process(npc)
                assertEquals(
                    if (level == 66) player.uid else org.rsmod.game.entity.player.PlayerUid.NULL,
                    npc.huntPlayer,
                    "$type vs $level",
                )
            }
        }
    }

    private fun dialogue(f: BlackKnightsMapFixture, choice: Int) {
        repeat(100) {
            val c = f.player.activeCoroutine
            if (c == null || c.isIdle) {
                f.player.ifClose(f.events)
                return
            }
            if (c.isAwaiting(ResumePauseButtonInput::class)) {
                val box =
                    listOf("chat_left", "chat_right", "messagebox", "chatmenu").first {
                        f.player.ui.containsModal("interface.$it")
                    }
                c.resumeWith(
                    ResumePauseButtonInput(
                        if (box == "chatmenu") "component.chatmenu:options"
                        else "component.$box:continue",
                        if (box == "chatmenu") choice else -1,
                    )
                )
            } else f.tick(1)
        }
        error("Dialogue did not finish")
    }

    private fun script(f: BlackKnightsMapFixture, s: PluginScript) {
        with(s) { ScriptContext(f.events, CheatCommandMap(), EngineQueueCache()).startup() }
    }

    private fun native(action: (dev.openrune.filesystem.Cache) -> Unit) {
        val cache = ServerCacheManager.init(241)
        try {
            action(cache)
        } finally {
            cache.close()
        }
    }

    companion object {
        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(241).close()
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field =
                    Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
