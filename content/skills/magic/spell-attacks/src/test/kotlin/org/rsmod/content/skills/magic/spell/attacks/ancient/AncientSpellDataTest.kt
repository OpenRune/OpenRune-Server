package org.rsmod.content.skills.magic.spell.attacks.ancient

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.params
import org.rsmod.api.enums.SpellbookEnums

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AncientSpellDataTest {
    @BeforeAll
    fun loadCache() {
        TestCache.load()
    }

    private data class WikiSpell(
        val level: Int,
        val maxHit: Int,
        val xp: Int,
        val element: Element,
        val tier: Tier,
        val effect: Int,
    )

    private val wiki =
        mapOf(
            AncientSpell.SmokeRush to WikiSpell(50, 13, 30, Element.Smoke, Tier.Rush, 10),
            AncientSpell.ShadowRush to WikiSpell(52, 14, 31, Element.Shadow, Tier.Rush, 10),
            AncientSpell.BloodRush to WikiSpell(56, 15, 33, Element.Blood, Tier.Rush, 25),
            AncientSpell.IceRush to WikiSpell(58, 16, 34, Element.Ice, Tier.Rush, 8),
            AncientSpell.SmokeBurst to WikiSpell(62, 17, 36, Element.Smoke, Tier.Burst, 10),
            AncientSpell.ShadowBurst to WikiSpell(64, 18, 37, Element.Shadow, Tier.Burst, 10),
            AncientSpell.BloodBurst to WikiSpell(68, 21, 39, Element.Blood, Tier.Burst, 25),
            AncientSpell.IceBurst to WikiSpell(70, 22, 40, Element.Ice, Tier.Burst, 16),
            AncientSpell.SmokeBlitz to WikiSpell(74, 23, 42, Element.Smoke, Tier.Blitz, 20),
            AncientSpell.ShadowBlitz to WikiSpell(76, 24, 43, Element.Shadow, Tier.Blitz, 15),
            AncientSpell.BloodBlitz to WikiSpell(80, 25, 45, Element.Blood, Tier.Blitz, 25),
            AncientSpell.IceBlitz to WikiSpell(82, 26, 46, Element.Ice, Tier.Blitz, 24),
            AncientSpell.SmokeBarrage to WikiSpell(86, 27, 48, Element.Smoke, Tier.Barrage, 20),
            AncientSpell.ShadowBarrage to WikiSpell(88, 28, 48, Element.Shadow, Tier.Barrage, 15),
            AncientSpell.BloodBarrage to WikiSpell(92, 29, 51, Element.Blood, Tier.Barrage, 25),
            AncientSpell.IceBarrage to WikiSpell(94, 30, 52, Element.Ice, Tier.Barrage, 32),
        )

    @ParameterizedTest
    @EnumSource(AncientSpell::class)
    fun `spell matches wiki values`(spell: AncientSpell) {
        val expected = wiki.getValue(spell)
        assertEquals(expected.maxHit, spell.maxHit, "max hit")
        assertEquals(expected.element, spell.element, "element")
        assertEquals(expected.tier, spell.tier, "tier")
        assertEquals(expected.effect, spell.effectStrength, "effect strength")
    }

    @ParameterizedTest
    @EnumSource(AncientSpell::class)
    fun `cache params mark spell as an ancient combat spell`(spell: AncientSpell) {
        val expected = wiki.getValue(spell)
        val obj = checkNotNull(ServerCacheManager.getItem(spell.obj.asRSCM(RSCMType.OBJ)))
        assertEquals(Spellbook.Ancients, Spellbook[obj.param(params.spell_spellbook)])
        assertEquals(MagicSpellType.Combat, MagicSpellType[obj.param(params.spell_type)])
        assertEquals(expected.level, obj.param(params.spell_levelreq), "level")
        assertEquals(expected.xp * 10, obj.param(params.spell_castxp), "cast xp")
        val quest = checkNotNull(obj.paramOrNull(params.spell_questreq), { "quest requirement" })
        assertEquals("dbrow.quest_deserttreasure", RSCM.getReverseMapping(RSCMType.DBROW, quest.id))
    }

    @ParameterizedTest
    @EnumSource(AncientSpell::class)
    fun `all fx symbols resolve`(spell: AncientSpell) {
        assertResolves(spell.tier.anim, RSCMType.SEQ)
        assertResolves(spell.impact, RSCMType.SPOTANIM)
        assertResolves(spell.hitSound, RSCMType.SYNTH)
        assertResolves(spell.castSound, RSCMType.SYNTH)
        spell.launch?.let { assertResolves(it, RSCMType.SPOTANIM) }
        when (val travel = spell.travel) {
            Travel.None -> Unit
            is Travel.FromCaster -> assertResolves(travel.spotanim, RSCMType.SPOTANIM)
            is Travel.FromTarget -> assertResolves(travel.spotanim, RSCMType.SPOTANIM)
        }
    }

    @Test
    fun `only burst and barrage are multi-target`() {
        for (spell in AncientSpell.entries) {
            val expected = spell.tier == Tier.Burst || spell.tier == Tier.Barrage
            assertEquals(expected, spell.tier.multiTarget, spell.name)
        }
    }

    @Test
    fun `every ancient combat spell in the spellbook has an attack`() {
        val combatSpells =
            SpellbookEnums.ancient_spellbook
                .filterValuesNotNull()
                .values
                .filter { MagicSpellType[it.param(params.spell_type)] == MagicSpellType.Combat }
                .map { it.id }
                .toSet()
        val implemented = AncientSpell.entries.map { it.obj.asRSCM(RSCMType.OBJ) }.toSet()
        assertEquals(combatSpells, implemented)
    }

    private fun assertResolves(symbol: String, type: RSCMType) {
        assertNotNull(runCatching { symbol.asRSCM(type) }.getOrNull(), "unresolved: $symbol")
    }
}
