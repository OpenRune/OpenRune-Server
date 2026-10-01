package org.rsmod.content.interfaces.worldmap

import dev.openrune.ServerCacheManager
import dev.or2.central.account.Rights
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.WorldMapClick
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class WorldMapTeleportTest {
    @Test
    fun `teleport command opens the native map for an administrator`() {
        val fixture = Fixture(Rights.ADMINISTRATOR)

        assertTrue(fixture.commands.execute(fixture.player, "teleport", emptyList()))

        assertTrue(fixture.player.ui.contains("interface.worldmap"))
    }

    @Test
    fun `native map click teleports an administrator to its destination`() {
        val fixture = Fixture(Rights.ADMINISTRATOR)
        fixture.player.coords = CoordGrid(3204, 3204, 1)
        fixture.commands.execute(fixture.player, "teleport", emptyList())

        fixture.clickMap()

        assertEquals(fixture.destination, fixture.player.coords)
    }

    @Test
    fun `ordinary player cannot use teleport command or a forged map click`() {
        val fixture = Fixture(Rights.NONE)
        val original = fixture.player.coords
        fixture.commands.execute(fixture.player, "teleport", emptyList())
        fixture.clickMap()

        assertFalse(fixture.player.ui.contains("interface.worldmap"))
        assertEquals(original, fixture.player.coords)
    }

    private class Fixture(rights: Rights) {
        val events = EventBus()
        val commands = CheatCommandMap()
        val destination = CoordGrid(3210, 3210)
        val collision = CollisionFlagMap().apply {
            allocateIfAbsent(3200, 3200, 0)
            allocateIfAbsent(3200, 3200, 1)
            allocateIfAbsent(destination.x, destination.z, destination.level)
        }
        val player = Player().apply {
            modLevel = rights
            coords = CoordGrid(3204, 3204)
        }
        val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events },
            getCollision = { collision },
        )

        init {
            with(WorldMapScript(events)) {
                ScriptContext(events, commands, EngineQueueCache()).startup()
            }
        }

        fun clickMap() {
            assertTrue(ProtectedAccessLauncher.withProtectedAccess(player, context) {
                assertTrue(events.publish(this, WorldMapClick(player, destination)))
            })
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun loadCache() {
            ServerCacheManager.init(240).close()
        }
    }
}
