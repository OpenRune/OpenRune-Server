package org.rsmod.game.hit

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HitImpactEffectsTest {
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
