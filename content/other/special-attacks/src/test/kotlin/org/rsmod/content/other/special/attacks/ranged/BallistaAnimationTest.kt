package org.rsmod.content.other.special.attacks.ranged

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BallistaAnimationTest {
    @Test
    fun `light or heavy ballista against a player`() {
        assertEquals(
            "seq.ballista_special_attack",
            BallistaAnimation.resolve(ornamented = false, targetIsNpc = false),
        )
    }

    @Test
    fun `light or heavy ballista against an npc`() {
        assertEquals(
            "seq.ballista_special_attack_pvn",
            BallistaAnimation.resolve(ornamented = false, targetIsNpc = true),
        )
    }

    @Test
    fun `ornamented heavy ballista against a player`() {
        assertEquals(
            "seq.ballista02_special_attack",
            BallistaAnimation.resolve(ornamented = true, targetIsNpc = false),
        )
    }

    @Test
    fun `ornamented heavy ballista against an npc`() {
        assertEquals(
            "seq.ballista02_special_attack_pvn",
            BallistaAnimation.resolve(ornamented = true, targetIsNpc = true),
        )
    }
}
