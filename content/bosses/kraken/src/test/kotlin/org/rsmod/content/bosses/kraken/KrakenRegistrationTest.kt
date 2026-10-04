package org.rsmod.content.bosses.kraken

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.bosses.runtime.*
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.events.interact.NpcUEvents
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
class KrakenRegistrationTest {
    @Test fun `disturb and explosive actions register against the actual cache options`() {
        ServerCacheManager.init(240).close()
        val registry = EncounterRegistry()
        val extensions = BossExtensionRegistry()
        val deps = mock(BossDeps::class.java) { call ->
            when (call.method.name) {
                "getEncounterRegistry" -> registry
                "getExtensionRegistry" -> extensions
                else -> RETURNS_DEFAULTS.answer(call)
            }
        }
        val bus = EventBus()
        val context = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
        with(KrakenScript(deps, mock(KrakenController::class.java), mock(NpcDeath::class.java))) { context.startup() }
        for (loc in listOf("loc.slayer_cave_kraken_boss_entrance", "loc.slayer_cave_kraken_boss_exit")) {
            assertTrue(bus.contains(LocEvents.Op1::class.java, loc.asRSCM()), loc)
        }
        for (kind in KrakenKind.entries) {
            assertTrue(bus.contains(NpcEvents.Op2::class.java, kind.poolId))
            assertTrue(bus.contains(NpcEvents.Ap2::class.java, kind.poolId))
            val key = EventBus.composeLongKey(kind.poolId, "obj.fishing_explosive".asRSCM())
            assertTrue(bus.contains(NpcUEvents.Op::class.java, key))
            assertTrue(bus.contains(NpcUEvents.Ap::class.java, key))
            assertNotNull(ServerCacheManager.getNpc(kind.activeId))
            for (animation in listOf(kind.spawn, kind.attack, kind.death)) {
                assertTrue(checkNotNull(ServerCacheManager.getAnim(animation.asRSCM())).tickDuration > 0, animation)
            }
        }
    }
}
