package org.rsmod.content.bosses.araxxor

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossExtensionRegistry
import org.rsmod.api.bosses.runtime.EncounterRegistry
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
class AraxxorRegistrationTest {
    @Test fun `scripts register against native NPC loc and queue namespaces`() {
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
        val context = ScriptContext(mock(EventBus::class.java), mock(CheatCommandMap::class.java),
            mock(EngineQueueCache::class.java))
        val controller = mock(AraxxorController::class.java)
        with(AraxxorScript(deps, controller, mock(NpcDeath::class.java))) { context.startup() }
        with(AraxxorEntryScript(mock(InstanceManager::class.java),
            mock(ProtectedAccessLauncher::class.java), controller)) { context.startup() }
    }
}
