package org.rsmod.api.combat.player

import dev.openrune.ServerCacheManager
import dev.openrune.filesystem.Cache
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.*
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.styles.AttackStyle
import org.rsmod.api.combat.commons.types.AttackType
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapDelegate
import org.rsmod.api.specials.SpecialAttack
import org.rsmod.api.specials.SpecialAttackRegistry
import org.rsmod.api.specials.SpecialAttackType
import org.rsmod.api.specials.combat.MagicSpecialAttack
import org.rsmod.api.specials.combat.ShieldSpecialAttack
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.spells.autocast.AutocastWeapons
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MagicSpecialDispatchTest {
    private lateinit var cache: Cache

    @BeforeAll fun loadCache() { cache = ServerCacheManager.init(240) }

    @AfterAll fun closeCache() { if (::cache.isInitialized) cache.close() }

    @Test fun `nightmare special overrides staff melee style and uses ten tile range`() {
        val fixture = fixture(SpecialAttackType.Weapon)
        assertTrue(fixture.access.hasMagicSpecialSelected(fixture.registry))
        assertEquals(10, fixture.access.attackRange(AttackStyle.AccurateMelee, fixture.registry))
        val resolved = fixture.access.resolveCombatAttack(fixture.item, AttackType.Crush, AttackStyle.AccurateMelee, null, fixture.access.hasMagicSpecialSelected(fixture.registry))
        assertInstanceOf(CombatAttack.Staff::class.java, resolved)
    }

    @Test fun `selected staff special skips autocast rune validation without resetting autocast`() {
        val fixture = fixture(SpecialAttackType.Weapon)
        val vars = vars(fixture.player)
        vars["varbit.autocast_set"] = 1
        vars["varbit.autocast_spell"] = 1
        val spells = mock(MagicSpellRegistry::class.java)
        val runes = mock(MagicRuneManager::class.java)
        val autocast = mock(AutocastWeapons::class.java)
        Assertions.assertNull(fixture.access.resolveAutocastSpell(spells, runes, autocast, fixture.registry))
        verifyNoInteractions(spells, runes, autocast)
        assertEquals(1, fixture.player.vars["varbit.autocast_set"])
    }

    @Test fun `manual spell remains a spell and idle weapon retains normal melee attack`() {
        val fixture = fixture(SpecialAttackType.None)
        assertFalse(fixture.access.hasMagicSpecialSelected(fixture.registry))
        val spell = mock(MagicSpell::class.java)
        assertInstanceOf(CombatAttack.Spell::class.java, fixture.access.resolveCombatAttack(fixture.item, null, null, spell))
        assertInstanceOf(CombatAttack.Melee::class.java, fixture.access.resolveCombatAttack(fixture.item, AttackType.Crush, AttackStyle.AccurateMelee, null))
    }

    @Test fun `shield special range does not depend on the equipped weapon range`() {
        val fixture = fixture(SpecialAttackType.Shield)
        assertFalse(fixture.access.hasMagicSpecialSelected(fixture.registry))
        assertEquals(10, fixture.access.attackRange(AttackStyle.AccurateMelee, fixture.registry))
    }

    @Test fun `selected shield bypasses missing spell runes without clearing autocast`() {
        val fixture = fixture(SpecialAttackType.Shield)
        val shield = InvObj("obj.dragonfire_shield")
        fixture.player.worn[Wearpos.LeftHand.slot] = shield
        `when`(fixture.registry[shield]).thenReturn(SpecialAttack.Shield(mock(ShieldSpecialAttack::class.java)))
        val vars = vars(fixture.player)
        vars["varbit.autocast_set"] = 1
        vars["varbit.autocast_spell"] = 1
        val spells = mock(MagicSpellRegistry::class.java)
        val runes = mock(MagicRuneManager::class.java)
        val autocast = mock(AutocastWeapons::class.java)
        assertTrue(fixture.access.hasShieldSpecialSelected(fixture.registry))
        Assertions.assertNull(fixture.access.resolveAutocastSpell(spells, runes, autocast, fixture.registry))
        verifyNoInteractions(spells, runes, autocast)
        assertEquals(1, fixture.player.vars["varbit.autocast_set"])
    }

    private fun fixture(type: SpecialAttackType): Fixture {
        val player = Player().apply {
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }
        val item = InvObj("obj.nightmare_staff_volatile")
        player.worn[Wearpos.RightHand.slot] = item
        vars(player)["varp.sa_attack"] = type.varValue
        val access = mock(ProtectedAccess::class.java)
        `when`(access.player).thenReturn(player)
        val registry = mock(SpecialAttackRegistry::class.java)
        `when`(registry[item]).thenReturn(SpecialAttack.Magic(550, mock(MagicSpecialAttack::class.java)))
        return Fixture(player, item, access, registry)
    }
    private data class Fixture(val player: Player, val item: InvObj, val access: ProtectedAccess, val registry: SpecialAttackRegistry)
    private fun vars(player: Player): VarPlayerIntMapDelegate = VarPlayerIntMapDelegate(player.client, player.vars, false, player)
}
