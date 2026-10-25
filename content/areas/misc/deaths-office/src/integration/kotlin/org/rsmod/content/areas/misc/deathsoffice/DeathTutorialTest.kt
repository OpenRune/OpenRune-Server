package org.rsmod.content.areas.misc.deathsoffice

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import com.google.inject.multibindings.Multibinder
import dev.openrune.types.ItemServerType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerDeathStorageHook
import org.rsmod.api.death.PlayerGraveHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.plugin.PlayerDeathScript
import org.rsmod.api.death.plugin.StandardPvmDeathHook
import org.rsmod.api.music.MusicRepository
import org.rsmod.api.player.queueDeath
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.other.gravestones.GravestoneScript
import org.rsmod.content.other.gravestones.Gravestones
import org.rsmod.content.other.gravestones.graveCoords
import org.rsmod.content.other.gravestones.graveDuration
import org.rsmod.content.other.gravestones.inDeathTutorial
import org.rsmod.content.other.gravestones.metDeath
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

/* Obj transaction system is not thread-safe. */
@Execution(ExecutionMode.SAME_THREAD)
class DeathTutorialTest {
    @Test
    fun GameTestState.`a first lossy death sends the player to Death and holds the grave timer`() =
        runTutorialTest {
            player.metDeath = false
            dieWithLostItems()

            assertTrue(player.coords.x >= RegionRegistry.INSTANCE_MIN_X)
            assertEquals(OFFICE_ARRIVAL.packed, player.vars[OFFICE_COORD])
            assertTrue(player.metDeath)
            assertTrue(player.inDeathTutorial)
            assertNotNull(player.graveCoords)
            assertEquals(Gravestones.DURATION + 1, player.graveDuration)

            advance(ticks = 30)
            assertEquals(Gravestones.DURATION + 1, player.graveDuration)
        }

    @Test
    fun GameTestState.`a later death respawns as usual`() =
        runTutorialTest {
            player.metDeath = true
            dieWithLostItems()

            assertFalse(player.inDeathTutorial)
            assertTrue(player.coords.x < RegionRegistry.INSTANCE_MIN_X)
            assertEquals(0, player.vars[OFFICE_COORD])
            assertTrue(player.graveDuration < Gravestones.DURATION)
        }

    private fun GameTestScope.dieWithLostItems() {
        for (slot in 0 until 5) {
            player.inv[slot] = InvObj(item, 1)
        }
        player.queueDeath()
        advance(ticks = 12)
    }

    private val GameTestScope.item: ItemServerType
        get() = firstObjType { !it.isStackable && it.tradeable && it.cost in 100 until 1000 }

    private fun GameTestState.runTutorialTest(body: GameTestScope.() -> Unit) =
        runInjectedGameTest(
            MusicRepository::class,
            DeathHooks(),
            PlayerDeathScript::class,
            GravestoneScript::class,
            DeathsOfficeScript::class,
        ) { music ->
            music.load()
            player.placeAt(CoordGrid(0, 50, 50, 30, 30))
            player.clearAllInvs()
            player.observerUUID = 1L
            body()
        }

    private companion object {
        val OFFICE_ARRIVAL = CoordGrid(3173, 5726)
        const val OFFICE_COORD = "varp.death_office_coord"
    }

    private class DeathHooks : AbstractModule() {
        override fun configure() {
            Multibinder.newSetBinder(binder(), PlayerGraveHook::class.java)
                .addBinding()
                .to(Gravestones::class.java)
            Multibinder.newSetBinder(binder(), PlayerDeathHook::class.java)
                .addBinding()
                .to(StandardPvmDeathHook::class.java)
            Multibinder.newSetBinder(binder(), PlayerDeathCleanupHook::class.java)
                .addBinding()
                .to(DeathTutorialArrival::class.java)
            Multibinder.newSetBinder(binder(), PlayerRespawnHook::class.java)
            Multibinder.newSetBinder(binder(), PlayerDeathStorageHook::class.java)
            bind(MusicRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }
}
