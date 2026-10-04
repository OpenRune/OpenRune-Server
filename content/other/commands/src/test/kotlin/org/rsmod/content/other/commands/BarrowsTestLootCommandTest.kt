package org.rsmod.content.other.commands

import com.google.inject.Injector
import dev.openrune.ServerCacheManager
import dev.or2.central.account.Rights
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.content.bosses.barrows.BarrowsTestLoot
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
class BarrowsTestLootCommandTest {
    @Test fun `barrows alias defaults to 100 and accepts an explicit chest count`() {
        val fixture = Fixture()
        fixture.run("barrows")
        verify(fixture.loot).generate(fixture.player, 100)
        fixture.run("BARROWS", "5")
        verify(fixture.loot).generate(fixture.player, 5)
    }

    @Test fun `invalid counts and non administrators cannot generate loot`() {
        val fixture = Fixture()
        for (count in listOf("0", "-1", "1001", "invalid", "999999999999")) fixture.run("barrows", count)
        fixture.run("barrows", "1", "extra")
        verifyNoInteractions(fixture.loot)
        assertEquals(Rights.ADMINISTRATOR, fixture.commands.commands.getValue("testloot").requiredRights)
        fixture.player.modLevel = Rights.NONE
        fixture.commands.execute(fixture.player, "testloot", listOf("barrows"))
        verifyNoInteractions(fixture.loot)
    }

    private class Fixture {
        val loot = mock(BarrowsTestLoot::class.java)
        val injector = mock(Injector::class.java)
        val commands = CheatCommandMap()
        val player = Player(RecordingClient()).apply { modLevel = Rights.ADMINISTRATOR }

        init {
            `when`(injector.getInstance(BarrowsTestLoot::class.java)).thenReturn(loot)
            val constructor = AdminCommands::class.java.constructors.single()
            val dependencies = constructor.parameterTypes.map {
                when (it) {
                    Injector::class.java -> injector
                    Set::class.java -> emptySet<Any>()
                    else -> mock(it)
                }
            }.toTypedArray()
            val script = constructor.newInstance(*dependencies) as AdminCommands
            val context = ScriptContext(EventBus(), commands, EngineQueueCache())
            with(script) { context.startup() }
        }

        fun run(vararg args: String) {
            assertTrue(commands.execute(player, "testloot", args.toList()))
        }
    }

    private class RecordingClient : Client<Any, Any> {
        override fun write(message: Any) = Unit
        override fun close() = Unit
        override fun read(player: Player) = Unit
        override fun flush() = Unit
        override fun flushHighPriority() = Unit
        override fun unregister(service: Any, player: Player) = Unit
    }

    companion object {
        @JvmStatic @BeforeAll fun initCache() { ServerCacheManager.init(240).close() }
    }
}
