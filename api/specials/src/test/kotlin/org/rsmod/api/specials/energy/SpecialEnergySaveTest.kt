package org.rsmod.api.specials.energy

import dev.openrune.ServerCacheManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.events.AttackKind
import org.rsmod.api.player.events.PlayerAttackEvents
import org.rsmod.api.player.events.ResourceKind
import org.rsmod.api.player.events.beginAttackRecord
import org.rsmod.api.player.events.finishAttackRecord
import org.rsmod.api.player.events.prepareAttackRecord
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SpecialEnergySaveTest {
    private val events = EventBus()
    private val random = mockk<GameRandom>(relaxed = true)
    private val energy = SpecialAttackEnergy(events, random)
    private val player = Player()
    private val results = mutableListOf<PlayerAttackEvents.AttackResult>()

    init {
        VarPlayerIntMapSetter.set(player, ENERGY_VARP, 1000)
        events.subscribeUnbound(PlayerAttackEvents.AttackResult::class.java) { results += this }
    }

    private fun spend(cost: Int) {
        events.beginAttackRecord(player, mockk<Npc>(), AttackKind.Melee, null, null)
        events.prepareAttackRecord(player, baseCycles = 4)
        energy.takeSpecialEnergy(player, cost)
        events.finishAttackRecord(player)
    }

    private fun chance(percent: Int) {
        events.subscribeUnbound(PlayerAttackEvents.AttackPrepare::class.java) {
            specialSaveChance = percent
        }
    }

    @Test
    fun `no chance spends energy without rolling`() {
        spend(250)

        assertEquals(750, player.vars[ENERGY_VARP])
        verify(exactly = 0) { random.of(maxExclusive = 100) }
    }

    @Test
    fun `a saved special spends nothing but still counts as a special`() {
        every { random.of(maxExclusive = 100) } returns 0
        chance(20)

        spend(250)

        assertEquals(1000, player.vars[ENERGY_VARP])
        val result = results.single()
        assertTrue(result.special)
        val spend = result.resources.single()
        assertEquals(ResourceKind.SpecialEnergy, spend.kind)
        assertEquals(250, spend.count)
        assertTrue(spend.saved)
    }

    @Test
    fun `a failed save spends energy`() {
        every { random.of(maxExclusive = 100) } returns 20
        chance(20)

        spend(250)

        assertEquals(750, player.vars[ENERGY_VARP])
        assertFalse(results.single().resources.single().saved)
    }

    @Test
    fun `outside an attack energy is spent as normal`() {
        energy.takeSpecialEnergy(player, 100)

        assertEquals(900, player.vars[ENERGY_VARP])
    }

    @Test
    fun `not having the energy still fails even with a guaranteed save`() {
        every { random.of(maxExclusive = 100) } returns 0
        chance(100)
        VarPlayerIntMapSetter.set(player, ENERGY_VARP, 100)

        var threw = false
        try {
            spend(250)
        } catch (_: IllegalArgumentException) {
            threw = true
        }

        assertTrue(threw)
        assertEquals(100, player.vars[ENERGY_VARP])
    }

    private companion object {
        const val ENERGY_VARP = "varp.sa_energy"

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            val root =
                generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                    .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision =
                Files.readAllLines(root.resolve("game.yml"))
                    .first { it.startsWith("revision:") }
                    .substringAfter(':')
                    .trim()
                    .substringBefore('.')
                    .toInt()
            ServerCacheManager.init(revision).close()
        }
    }
}
