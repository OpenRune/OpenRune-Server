package org.rsmod.api.bosses.runtime

import dev.openrune.types.NpcServerType
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.rsmod.api.bosses.dsl.resetAnim
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.BossStats
import org.rsmod.api.bosses.spec.PhaseSpec
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

class EncounterRegistryTest {
    private val type = NpcServerType(id = 1, name = "Boss", size = 1, hitpoints = 100)
    private val easy = spec("easy")
    private val hard = spec("hard")

    @Test
    fun `a single-spec boss gets its default spec lazily`() {
        val registry = registry(listOf(easy), default = easy)
        val npc = npc(slot = 1)
        assertSame(easy, registry.of(npc).spec)
        assertSame(registry.of(npc), registry.of(npc))
    }

    @Test
    fun `each npc runs the spec it was started on`() {
        val registry = registry(listOf(easy, hard), default = null)
        val first = npc(slot = 1)
        val second = npc(slot = 2)
        registry.start(first, easy)
        registry.start(second, hard)
        assertSame(easy, registry.of(first).spec)
        assertSame(hard, registry.of(second).spec)
    }

    @Test
    fun `a boss without a default must be started first`() {
        val registry = registry(listOf(easy, hard), default = null)
        assertThrows<IllegalStateException> { registry.of(npc(slot = 1)) }
    }

    @Test
    fun `only registered specs can be started, once per encounter`() {
        val registry = registry(listOf(easy), default = null)
        val npc = npc(slot = 1)
        assertThrows<IllegalArgumentException> { registry.start(npc, spec("easy")) }
        registry.start(npc, easy)
        assertThrows<IllegalStateException> { registry.start(npc, easy) }
        registry.remove(npc)
        registry.start(npc, easy)
    }

    @Test
    fun `an unregistered npc type has no encounter`() {
        assertThrows<IllegalStateException> { EncounterRegistry(MapClock()).of(npc(slot = 1)) }
    }

    private fun registry(specs: List<BossSpec>, default: BossSpec?): EncounterRegistry =
        EncounterRegistry(MapClock()).apply { register(type.id, specs, default) }

    private fun npc(slot: Int): Npc = Npc(type, CoordGrid(0, 1, 1, 0, 0)).apply { slotId = slot }

    private fun spec(phase: String): BossSpec =
        BossSpec(
            npcTypes = listOf("npc.boss"),
            stats = BossStats(),
            abilities = mapOf("a" to resetAnim()),
            phases = mapOf(phase to PhaseSpec(phase)),
            triggers = emptyList(),
        )
}
