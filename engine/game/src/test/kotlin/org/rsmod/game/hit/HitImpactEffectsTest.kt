package org.rsmod.game.hit

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HitImpactEffectsTest {
    @Test fun `arrival damage transforms run once across hit copies`() {
        val effects = HitImpactEffects()
        var calls = 0
        var shieldAlive = true
        effects.beforeImpact { damage -> calls++; if (shieldAlive) damage * 4 / 5 else damage }
        shieldAlive = false
        assertEquals(50, effects.prepare(50))
        shieldAlive = true
        assertEquals(50, effects.prepare(50))
        assertEquals(1, calls)
        assertThrows(IllegalStateException::class.java) { effects.beforeImpact { it } }
    }

    @Test fun `cancelled attack never executes arrival side effects`() {
        var calls = 0
        val effects = HitImpactEffects()
        effects.beforeImpact { calls++; it }
        assertEquals(0, calls)
    }

    @Test fun `modified hit copies apply actual damage exactly once`() {
        val pending = Hit(HitType.Magic, Hitmark(0).copy(damage = 50), null, null, null)
        val observed = mutableListOf<Int>()
        pending.impactEffects.add { observed.add(it) }
        assertTrue(observed.isEmpty())
        val capped = pending.copy(hitmark = pending.hitmark.copy(damage = 7))
        capped.impactEffects.complete(capped.damage)
        pending.impactEffects.complete(50)
        assertEquals(listOf(7), observed)
    }

    @Test fun `cancelled hits never complete and zero hits remain zero`() {
        val pending = HitImpactEffects()
        var total = 0
        pending.add { total += it }
        assertEquals(0, total)
        pending.complete(0)
        assertEquals(0, total)
        assertThrows(IllegalStateException::class.java) { pending.add { total += it } }
    }
}
