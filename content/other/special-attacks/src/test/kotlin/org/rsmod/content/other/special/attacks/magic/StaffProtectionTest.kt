package org.rsmod.content.other.special.attacks.magic

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import kotlin.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.manager.PlayerAttackManager
import org.rsmod.api.config.constants
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hit.modifier.StandardPlayerHitModifier
import org.rsmod.api.player.hit.processor.DamageOnlyPlayerHitProcessor
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.specials.*
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.content.other.special.attacks.magic.StaffProtectionSpecialAttacks.Companion.protectionEnd
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.*
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.hit.*
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@OptIn(InternalApi::class)
class StaffProtectionTest {
    private var Player.protectMelee by intVarBit("varbit.prayer_protectfrommelee")

    @Test fun `all eight staff variants have their own player animation and consume full special energy`() {
        val weapons = SpecialAttackWeapons()
        SpecialAttackWeapons::class.java.declaredMethods.single { it.name.startsWith("startup") }
            .also { it.isAccessible = true }.invoke(weapons)
        val registry = SpecialAttackRegistry(weapons)
        with(StaffProtectionSpecialAttacks()) { SpecialAttackRepository(registry).register(mock(SpecialAttackManager::class.java)) }
        assertEquals(8, StaffProtectionSpecialAttacks.EFFECTS.size)
        for ((weapon, effects) in StaffProtectionSpecialAttacks.EFFECTS) {
            val player = player().apply { worn[Wearpos.RightHand.slot] = InvObj(weapon) }
            val access = mock(ProtectedAccess::class.java)
            `when`(access.player).thenReturn(player)
            val special = registry[InvObj(weapon)] as SpecialAttack.Instant
            assertEquals(1000, special.energyInHundreds)
            complete { with(special.special) { access.activate() } }
            assertEquals(200, player.protectionEnd)
            verify(access).anim(effects.first, 0)
            verify(access).spotanim(effects.second, 0, 0, constants.spotanim_slot_combat)
            assertTrue(effects.first.asRSCM() >= 0)
            assertTrue(effects.second.asRSCM() >= 0)
        }
    }

    @Test fun `protection lasts exactly one hundred ticks never stacks and only reduces melee`() {
        val player = player()
        StaffProtectionSpecialAttacks.activate(player)
        repeat(2) { assertEquals(6, StaffProtectionSpecialAttacks.modify(player, hit(13)).damage) }
        StaffProtectionSpecialAttacks.activate(player)
        assertEquals(6, StaffProtectionSpecialAttacks.modify(player, hit(13)).damage)
        for (type in listOf(HitType.Ranged, HitType.Magic, HitType.Typeless)) {
            val hit = hit(13, type)
            assertSame(hit, StaffProtectionSpecialAttacks.modify(player, hit))
        }
        player.currentMapClock = 199
        assertEquals(6, StaffProtectionSpecialAttacks.modify(player, hit(13)).damage)
        player.currentMapClock = 200
        assertEquals(13, StaffProtectionSpecialAttacks.modify(player, hit(13)).damage)
        assertEquals(0, player.protectionEnd)
    }

    @Test fun `weapon switching only cancels on positive damage while the staff is absent`() {
        val player = player()
        StaffProtectionSpecialAttacks.activate(player)
        player.worn[Wearpos.RightHand.slot] = InvObj("obj.dragon_dagger")
        StaffProtectionSpecialAttacks.modify(player, hit(0))
        assertEquals(200, player.protectionEnd)
        player.worn[Wearpos.RightHand.slot] = InvObj("obj.sotd")
        assertEquals(6, StaffProtectionSpecialAttacks.modify(player, hit(13)).damage)
        player.worn[Wearpos.RightHand.slot] = null
        assertEquals(4, StaffProtectionSpecialAttacks.modify(player, hit(4, HitType.Typeless)).damage)
        assertEquals(0, player.protectionEnd)
        player.worn[Wearpos.RightHand.slot] = InvObj("obj.sotd")
        assertEquals(13, StaffProtectionSpecialAttacks.modify(player, hit(13)).damage)
    }

    @Test fun `native impact uses reduced damage for health and callbacks and preserves inactive hits`() {
        val bus = scriptBus()
        val processor = DamageOnlyPlayerHitProcessor(bus, mock(NpcList::class.java), mock(PlayerList::class.java))
        val player = player()
        val unprotected = hit(13)
        with(processor) { player.process(unprotected) }
        assertEquals(86, player.hitpoints)
        StaffProtectionSpecialAttacks.activate(player)
        val protected = hit(13)
        var applied = -1
        protected.impactEffects.add { applied = it }
        with(processor) { player.process(protected) }
        assertEquals(80, player.hitpoints)
        assertEquals(6, applied)
    }

    @Test fun `native prayer reduction precedes the half damage rounding`() {
        val bus = scriptBus()
        val modifier = StandardPlayerHitModifier(bus)
        val constructor = PlayerAttackManager::class.java.constructors.single()
        val manager = constructor.newInstance(*constructor.parameterTypes.map {
            when (it) { PlayerHitModifier::class.java -> modifier; Set::class.java -> emptySet<Any>(); else -> mock(it) }
        }.toTypedArray()) as PlayerAttackManager
        val source = player()
        val target = player().apply { slotId = 2; uuid = 2; assignUid(); protectMelee = 1 }
        StaffProtectionSpecialAttacks.activate(target)
        val queued = manager.queueMeleeHit(source, target, 7, 1)
        assertEquals(4, queued.damage)
        assertEquals(2, StaffProtectionSpecialAttacks.modify(target, queued).damage)
    }

    @Test fun `session initialization clears temporary protection`() {
        val player = player()
        StaffProtectionSpecialAttacks.activate(player)
        val bus = scriptBus()
        bus.publish(SessionStateEvent.Initialize(player))
        assertEquals(0, player.protectionEnd)
        val definition = ServerCacheManager.getVarp(StaffProtectionSpecialAttacks.END_CLOCK.asRSCM())
        Assertions.assertNotNull(definition)
    }

    private fun scriptBus(): EventBus = EventBus().also { bus ->
        with(StaffProtectionScript()) { ScriptContext(bus, CheatCommandMap(), EngineQueueCache()).startup() }
    }
    private fun player() = Player().apply {
        slotId = 1; uuid = 1; assignUid(); currentMapClock = 100
        worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        worn[Wearpos.RightHand.slot] = InvObj("obj.sotd")
        statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99)
    }
    private fun hit(amount: Int, type: HitType = HitType.Melee) = Hit(type, Hitmark.fromNoSource(self = 0, source = 0, public = 0, damage = amount, delay = 0), null, null, null)
    private fun complete(block: suspend () -> Boolean) {
        var done = false
        block.startCoroutine(object : Continuation<Boolean> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Boolean>) { assertTrue(result.getOrThrow()); done = true }
        })
        assertTrue(done)
    }
    companion object { @JvmStatic @BeforeAll fun setup() { ServerCacheManager.init(240).close() } }
}
