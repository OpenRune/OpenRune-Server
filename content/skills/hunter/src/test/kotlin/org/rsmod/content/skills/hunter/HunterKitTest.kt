package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.CastResult
import org.rsmod.api.enums.HunterEnums.hunter_kit_contents
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class HunterKitTest {
    private val script = HunterKitScript(mockk(relaxed = true), mockk(relaxed = true))
    private val contents by lazy { hunter_kit_contents.values.filterNotNull().map { it.internalName } }

    private fun caster(f: HunterFixture): HunterKitScript {
        val spell = mockk<MagicSpell>(relaxed = true)
        every { spell.component.packed } returns SPELL.asRSCM(RSCMType.COMPONENT)
        val spells = mockk<MagicSpellRegistry>()
        every { spells.allSpells() } returns listOf(spell)
        val runes = mockk<MagicRuneManager>(relaxed = true)
        every { runes.attemptCast(any(), any(), any()) } answers {
            val rune = f.player.inv[RUNE_SLOT]
            if (rune == null) {
                CastResult.Failure.NotEnoughInvObj
            } else {
                f.player.inv[RUNE_SLOT] = if (rune.count > 1) InvObj(RUNE, rune.count - 1) else null
                CastResult.Success.AllUnlimited
            }
        }
        return HunterKitScript(spells, runes)
    }

    @Test
    fun `casting twice makes one kit and takes the runes once`() {
        val f = HunterFixture()
        f.player.inv[RUNE_SLOT] = InvObj(RUNE, 2)
        val caster = caster(f)
        repeat(2) { f.run(caster, "cast") }
        assertEquals(1L, f.total(KIT))
        assertEquals(1L, f.total(RUNE))
    }

    @Test
    fun `casting with a full inventory takes no runes`() {
        val f = HunterFixture()
        f.player.inv[RUNE_SLOT] = InvObj(RUNE, 2)
        f.fill()
        f.run(caster(f), "cast")
        assertEquals(0L, f.total(KIT))
        assertEquals(2L, f.total(RUNE))
    }

    @Test
    fun `opening the kit twice gives its contents once`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(KIT, 1)
        repeat(2) { f.call(script, "open", 0) }
        assertEquals(0L, f.total(KIT))
        for (obj in contents) assertEquals(1L, f.total(obj), obj)
    }

    @Test
    fun `a full inventory keeps the kit`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(KIT, 1)
        f.fill()
        f.player.inv[1] = null
        f.call(script, "open", 0)
        assertEquals(1L, f.total(KIT))
        for (obj in contents) assertEquals(0L, f.total(obj), obj)
    }

    @Test
    fun `a kit gone from the slot gives nothing`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(KIT, 1)
        f.player.inv[0] = null
        f.call(script, "open", 0)
        for (obj in contents) assertEquals(0L, f.total(obj), obj)
    }

    companion object {
        private const val KIT = "obj.dream_hunter_box"
        private const val SPELL = "component.magic_spellbook:hunter_kit"
        private const val RUNE = "obj.earthrune"
        private const val RUNE_SLOT = 27

        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
