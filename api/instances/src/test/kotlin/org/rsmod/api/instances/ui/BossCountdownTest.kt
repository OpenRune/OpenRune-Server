package org.rsmod.api.instances.ui

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.events.EventBus
import org.rsmod.game.client.Client
import org.rsmod.game.client.NoopClient
import org.rsmod.game.entity.Player

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class BossCountdownTest {
    @Test
    fun `countdown coexists with the godwars killcount overlay`() {
        val events = EventBus()
        val player = Player()
        player.ifOpenOverlay(
            "interface.godwars_overlay",
            "component.toplevel_osrs_stretch:overlay_hud",
            events,
        )
        val countdown = BossCountdown(events)
        countdown.show(player, "boss", "General Graardor", 10)
        assertTrue(player.ui.containsOverlay(BossCountdown.INTERFACE))
        assertTrue(player.ui.containsOverlay("interface.godwars_overlay"))
        countdown.clear(player, "boss")
        assertFalse(player.ui.containsOverlay(BossCountdown.INTERFACE))
        assertTrue(player.ui.containsOverlay("interface.godwars_overlay"))
    }

    @Test
    fun `another timer cannot replace or close the active encounter countdown`() {
        val client = RecordingClient()
        val player = Player(client = client)
        val countdown = BossCountdown(EventBus())
        countdown.show(player, "zulrah", "Zulrah", 9, remainingSeconds = 5)
        val sent = client.messages.size
        countdown.show(player, "native-instance-respawn", "Other boss", 5)
        countdown.clear(player, "native-instance-respawn")
        assertEquals(sent, client.messages.size)
        assertTrue(player.ui.containsOverlay(BossCountdown.INTERFACE))
        countdown.clear(player, "zulrah")
        assertFalse(player.ui.containsOverlay(BossCountdown.INTERFACE))
        countdown.show(player, "native-instance-respawn", "Other boss", 5)
        assertTrue(player.ui.containsOverlay(BossCountdown.INTERFACE))
    }

    @Test
    fun `same displayed second sends no repeated widget updates and expiry closes overlay`() {
        val client = RecordingClient()
        val player = Player(client = client)
        val countdown = BossCountdown(EventBus())
        countdown.show(player, "boss", "Zulrah", 8)
        val sent = client.messages.size
        countdown.show(player, "boss", "Zulrah", 7)
        assertEquals(sent, client.messages.size)
        countdown.show(player, "boss", "Zulrah", 6)
        assertTrue(client.messages.size > sent)
        countdown.show(player, "boss", "Zulrah", 0)
        assertFalse(player.ui.containsOverlay(BossCountdown.INTERFACE))
    }

    @Test
    fun `nominal five second delay remains visible for the final rounded server tick`() {
        val client = RecordingClient()
        val player = Player(client = client)
        val countdown = BossCountdown(EventBus())
        countdown.show(player, "zulrah", "Zulrah", 9, remainingSeconds = 5)
        assertTrue(client.messages.joinToString().contains("5s"))
        assertFalse(client.messages.joinToString().contains("6s"))
        client.messages.clear()
        countdown.show(player, "zulrah", "Zulrah", 1, remainingSeconds = 0)
        assertTrue(player.ui.containsOverlay(BossCountdown.INTERFACE))
        assertTrue(client.messages.joinToString().contains("1s"))
        countdown.show(player, "zulrah", "Zulrah", 0, remainingSeconds = 0)
        assertFalse(player.ui.containsOverlay(BossCountdown.INTERFACE))
    }

    private class RecordingClient : Client<Any, Any> by NoopClient {
        val messages = mutableListOf<Any>()

        override fun write(message: Any) {
            messages += message
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
