@file:OptIn(UncheckedType::class)

package org.rsmod.api.combat.manager

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.InventoryServerType
import dev.openrune.types.ItemServerType
import dev.openrune.types.util.UncheckedType
import dev.openrune.util.Wearpos
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.events.AttackKind
import org.rsmod.api.player.events.PlayerAttackEvents
import org.rsmod.api.player.events.PlayerSpellRuneEvents
import org.rsmod.api.player.events.ResourceKind
import org.rsmod.api.player.events.ResourceSpend
import org.rsmod.api.player.events.beginAttackRecord
import org.rsmod.api.player.events.finishAttackRecord
import org.rsmod.api.player.events.prepareAttackRecord
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.spells.runes.MagicRunes
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

/** Saving moves items, so every case accounts for what is left and what was reported. */
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ResourceSavingTest {
    @Test
    fun `no subscriber spends ammo as normal and never rolls a save`() {
        val f = Fixture()
        f.quiver(10)

        f.ammo.useQuiverAmmo(f.player, f.arrow, CoordGrid.ZERO, dropDelay = 1)

        assertEquals(9, f.quiverCount())
        verify(exactly = 0) { f.random.of(maxExclusive = 100) }
    }

    @Test
    fun `a saved arrow is not spent and cannot drop to the ground`() {
        val f = Fixture(saveRolls = listOf(0))
        f.quiver(10)
        f.onPrepare { saveChance = 50 }

        val spends = f.attack { f.ammo.useQuiverAmmo(f.player, f.arrow, CoordGrid.ZERO, 1) }

        assertEquals(10, f.quiverCount())
        assertEquals(listOf(ResourceSpend(ResourceKind.Ammo, f.arrow, 1, true)), spends)
        verify(exactly = 0) { f.random.randomBoolean(any()) }
        assertTrue(f.worldQueues.isEmpty)
    }

    @Test
    fun `an unsaved arrow is spent and reported`() {
        val f = Fixture(saveRolls = listOf(50))
        f.quiver(10)
        f.onPrepare { saveChance = 50 }

        val spends = f.attack { f.ammo.useQuiverAmmo(f.player, f.arrow, CoordGrid.ZERO, 1) }

        assertEquals(9, f.quiverCount())
        assertEquals(listOf(ResourceSpend(ResourceKind.Ammo, f.arrow, 1, false)), spends)
    }

    @Test
    fun `the last arrow stays when saved`() {
        val f = Fixture(saveRolls = listOf(0))
        f.quiver(1)
        f.onPrepare { saveChance = 100 }

        f.attack { f.ammo.useQuiverAmmo(f.player, f.arrow, CoordGrid.ZERO, 1) }

        assertEquals(1, f.quiverCount())
    }

    @Test
    fun `each shot of a two arrow attack rolls separately`() {
        val f = Fixture(saveRolls = listOf(0, 99))
        f.quiver(10)
        f.onPrepare { saveChance = 50 }

        val spends =
            f.attack {
                f.ammo.useQuiverAmmo(f.player, f.arrow, CoordGrid.ZERO, 1)
                f.ammo.useQuiverAmmo(f.player, f.arrow, CoordGrid.ZERO, 1)
            }

        assertEquals(9, f.quiverCount())
        assertEquals(listOf(true, false), spends.map { it.saved })
    }

    @Test
    fun `no subscriber spends every rune`() {
        val f = Fixture()
        f.inv(0, f.air, 5)
        f.inv(1, f.mind, 5)

        val result = f.cast(f.invRunes(0, f.air, 2), f.invRunes(1, f.mind, 1))

        assertEquals(MagicRuneManager.CastResult.Success.Consumed(false), result)
        assertEquals(3, f.invCount(f.air))
        assertEquals(4, f.invCount(f.mind))
    }

    @Test
    fun `a spell paid entirely from the rune pouch casts`() {
        val f = Fixture()
        f.pouch(10)
        val pouchOnly =
            MagicRunes.Validation.Valid.HasEnough(
                listOf(MagicRunes.Source.VarBitSource(POUCH, 3, "obj.airrune"))
            )

        val result = f.cast(pouchOnly)

        assertEquals(MagicRuneManager.CastResult.Success.Consumed(false), result)
        assertEquals(7, f.pouchCount())
    }

    @Test
    fun `saving every rune type still casts and spends nothing`() {
        val f = Fixture(saveRolls = listOf(0, 0))
        f.inv(0, f.air, 5)
        f.inv(1, f.mind, 5)
        f.onPrepare { saveChance = 100 }

        val result =
            f.attackReturning { f.cast(f.invRunes(0, f.air, 2), f.invRunes(1, f.mind, 1)) }

        assertEquals(MagicRuneManager.CastResult.Success.Consumed(false), result)
        assertEquals(5, f.invCount(f.air))
        assertEquals(5, f.invCount(f.mind))
        assertEquals(listOf(true, true), f.lastSpends.map { it.saved })
    }

    @Test
    fun `each rune type rolls separately and only the unsaved one is spent`() {
        val f = Fixture(saveRolls = listOf(0, 99))
        f.inv(0, f.air, 5)
        f.inv(1, f.mind, 5)
        f.onPrepare { saveChance = 50 }

        f.attackReturning { f.cast(f.invRunes(0, f.air, 2), f.invRunes(1, f.mind, 1)) }

        assertEquals(5, f.invCount(f.air))
        assertEquals(4, f.invCount(f.mind))
        val byRune = f.lastSpends.associate { it.item!!.id to (it.count to it.saved) }
        assertEquals(2 to true, byRune[f.air.id])
        assertEquals(1 to false, byRune[f.mind.id])
    }

    @Test
    fun `a rune split between inventory and pouch is saved or spent as one`() {
        val saved = Fixture(saveRolls = listOf(0))
        saved.inv(0, saved.air, 3)
        saved.pouch(10)
        saved.onPrepare { saveChance = 50 }

        saved.attackReturning { saved.cast(saved.splitRunes(0, 3, 2)) }

        assertEquals(3, saved.invCount(saved.air))
        assertEquals(10, saved.pouchCount())

        val spent = Fixture(saveRolls = listOf(99))
        spent.inv(0, spent.air, 3)
        spent.pouch(10)
        spent.onPrepare { saveChance = 50 }

        spent.attackReturning { spent.cast(spent.splitRunes(0, 3, 2)) }

        assertEquals(0, spent.invCount(spent.air))
        assertEquals(8, spent.pouchCount())
        assertEquals(listOf(false, false), spent.lastSpends.map { it.saved })
    }

    @Test
    fun `a saved sunfire rune still gives the spell its bonus`() {
        val f = Fixture(saveRolls = listOf(0))
        val sunfire = item("obj.sunfirerune")
        f.inv(0, sunfire, 2)
        f.onPrepare { saveChance = 100 }

        val result = f.attackReturning { f.cast(f.invRunes(0, sunfire, 1)) }

        assertEquals(MagicRuneManager.CastResult.Success.Consumed(true), result)
        assertEquals(2, f.invCount(sunfire))
    }

    @Test
    fun `a rune missing from the slot fails and reports nothing`() {
        val f = Fixture(saveRolls = listOf(99))
        f.inv(0, f.air, 1)
        f.onPrepare { saveChance = 50 }

        val result = f.attackReturning { f.cast(f.invRunes(0, f.air, 2)) }

        assertEquals(MagicRuneManager.CastResult.Failure.NotEnoughInvObj, result)
        assertEquals(1, f.invCount(f.air))
        assertTrue(f.lastSpends.isEmpty())
    }

    @Test
    fun `a failed rune type leaves the other type unspent`() {
        val f = Fixture(saveRolls = listOf(99, 99))
        f.inv(0, f.air, 5)
        f.inv(1, f.mind, 1)
        f.onPrepare { saveChance = 50 }

        val result =
            f.attackReturning { f.cast(f.invRunes(0, f.air, 2), f.invRunes(1, f.mind, 3)) }

        assertEquals(MagicRuneManager.CastResult.Failure.NotEnoughInvObj, result)
        assertEquals(5, f.invCount(f.air))
        assertEquals(1, f.invCount(f.mind))
    }

    @Test
    fun `a saved charge is not spent and cannot uncharge the weapon`() {
        val f = Fixture(saveRolls = listOf(0))
        every { f.objCharges.getCharges(any(), any()) } returns 1
        f.onPrepare { saveChance = 50 }

        var result: ObjChargeManager.Uncharge? = null
        val spends = f.attack { result = f.charges.attemptDetractWeapon(f.player, VAROBJ) }

        val success = result as ObjChargeManager.Uncharge.Success
        assertEquals(1, success.chargesLeft)
        assertFalse(success.fullyUncharged)
        assertTrue(spends.single().saved)
        verify(exactly = 0) { f.objCharges.reduceWornCharges(any(), any(), any(), any()) }
    }

    @Test
    fun `an empty weapon fails even with a guaranteed save`() {
        val f = Fixture(saveRolls = listOf(0))
        every { f.objCharges.getCharges(any(), any()) } returns 0
        every { f.objCharges.reduceWornCharges(any(), any(), any(), any()) } returns
            ObjChargeManager.Uncharge.Failure.NotEnoughCharges
        f.onPrepare { saveChance = 100 }

        var result: ObjChargeManager.Uncharge? = null
        val spends = f.attack { result = f.charges.attemptDetractWeapon(f.player, VAROBJ) }

        assertEquals(ObjChargeManager.Uncharge.Failure.NotEnoughCharges, result)
        assertTrue(spends.isEmpty())
    }

    @Test
    fun `an unsaved charge is spent as normal`() {
        val f = Fixture(saveRolls = listOf(99))
        every { f.objCharges.getCharges(any(), any()) } returns 5
        every { f.objCharges.reduceWornCharges(any(), any(), any(), any()) } returns
            ObjChargeManager.Uncharge.Success(4)
        f.onPrepare { saveChance = 50 }

        val spends = f.attack { f.charges.attemptDetractWeapon(f.player, VAROBJ) }

        assertFalse(spends.single().saved)
        verify(exactly = 1) {
            f.objCharges.reduceWornCharges(f.player, Wearpos.RightHand, VAROBJ, 1)
        }
    }

    @Test
    fun `a non-combat spell saves each rune type from its own event`() {
        val f = Fixture(saveRolls = listOf(0, 99))
        f.inv(0, f.air, 5)
        f.inv(1, f.mind, 5)
        f.onSpellPrepare { saveChance = 50 }

        val result = f.cast(f.invRunes(0, f.air, 2), f.invRunes(1, f.mind, 1))

        assertEquals(MagicRuneManager.CastResult.Success.Consumed(false), result)
        assertEquals(5, f.invCount(f.air))
        assertEquals(4, f.invCount(f.mind))
        val event = f.spellSpends.single()
        assertEquals(f.lastSpell, event.spell)
        val byRune = event.runes.associate { it.item!!.id to (it.count to it.saved) }
        assertEquals(2 to true, byRune[f.air.id])
        assertEquals(1 to false, byRune[f.mind.id])
    }

    @Test
    fun `a non-combat spell with every rune saved still casts`() {
        val f = Fixture(saveRolls = listOf(0))
        f.inv(0, f.air, 2)
        f.onSpellPrepare { saveChance = 100 }

        val result = f.cast(f.invRunes(0, f.air, 2))

        assertEquals(MagicRuneManager.CastResult.Success.Consumed(false), result)
        assertEquals(2, f.invCount(f.air))
        assertEquals(listOf(true), f.spellSpends.single().runes.map { it.saved })
    }

    @Test
    fun `a non-combat spell saves a rune split with the pouch as one`() {
        val f = Fixture(saveRolls = listOf(0))
        f.inv(0, f.air, 3)
        f.pouch(10)
        f.onSpellPrepare { saveChance = 50 }

        f.cast(f.splitRunes(0, 3, 2))

        assertEquals(3, f.invCount(f.air))
        assertEquals(10, f.pouchCount())
    }

    @Test
    fun `a non-rune cost is never saved and takes no roll`() {
        val f = Fixture(saveRolls = listOf(0))
        val banana = item("obj.banana")
        f.inv(0, f.air, 2)
        f.inv(1, banana, 3)
        f.onSpellPrepare { saveChance = 100 }

        f.cast(f.invRunes(0, f.air, 2), f.invRunes(1, banana, 1))

        assertEquals(2, f.invCount(f.air))
        assertEquals(2, f.invCount(banana))
        verify(exactly = 1) { f.random.of(maxExclusive = 100) }
    }

    @Test
    fun `an attack never saves a non-rune cost either`() {
        val f = Fixture(saveRolls = listOf(0))
        val banana = item("obj.banana")
        f.inv(0, banana, 3)
        f.onPrepare { saveChance = 100 }

        f.attackReturning { f.cast(f.invRunes(0, banana, 1)) }

        assertEquals(2, f.invCount(banana))
        assertEquals(listOf(false), f.lastSpends.map { it.saved })
    }

    @Test
    fun `a non-combat cast that fails spends nothing and reports nothing`() {
        val f = Fixture(saveRolls = listOf(99, 99))
        f.inv(0, f.air, 5)
        f.inv(1, f.mind, 1)
        f.onSpellPrepare { saveChance = 50 }

        val result = f.cast(f.invRunes(0, f.air, 2), f.invRunes(1, f.mind, 3))

        assertEquals(MagicRuneManager.CastResult.Failure.NotEnoughInvObj, result)
        assertEquals(5, f.invCount(f.air))
        assertEquals(1, f.invCount(f.mind))
        assertTrue(f.spellSpends.isEmpty())
    }

    @Test
    fun `a spell with nothing to pay publishes nothing`() {
        val f = Fixture()
        var prepared = false
        f.onSpellPrepare { prepared = true }

        val result = f.cast(MagicRunes.Validation.Valid.Unlimited)

        assertEquals(MagicRuneManager.CastResult.Success.AllUnlimited, result)
        assertFalse(prepared)
        assertTrue(f.spellSpends.isEmpty())
    }

    @Test
    fun `a spell inside an attack ignores the non-combat events`() {
        val f = Fixture(saveRolls = listOf(0))
        var prepared = false
        f.inv(0, f.air, 5)
        f.onPrepare {}
        f.onSpellPrepare {
            prepared = true
            saveChance = 100
        }

        f.attackReturning { f.cast(f.invRunes(0, f.air, 2)) }

        assertFalse(prepared)
        assertEquals(3, f.invCount(f.air))
        assertTrue(f.spellSpends.isEmpty())
    }

    @Test
    fun `a non-combat chance of zero never rolls`() {
        val f = Fixture()
        f.inv(0, f.air, 5)
        f.onSpellPrepare {}

        f.cast(f.invRunes(0, f.air, 2))

        assertEquals(3, f.invCount(f.air))
        verify(exactly = 0) { f.random.of(maxExclusive = 100) }
    }

    @Test
    fun `a chance of zero never rolls`() {
        val f = Fixture()
        f.quiver(5)
        f.onPrepare {}

        f.attack { f.ammo.useQuiverAmmo(f.player, f.arrow, CoordGrid.ZERO, 1) }

        assertEquals(4, f.quiverCount())
        verify(exactly = 0) { f.random.of(maxExclusive = 100) }
    }

    @OptIn(InternalApi::class)
    private class Fixture(saveRolls: List<Int> = emptyList()) {
        val events = EventBus()
        val player =
            Player().apply {
                inv = inventory(28)
                worn = inventory(14)
            }
        val npc = mockk<Npc>(relaxed = true)
        val arrow = item("obj.bronze_arrow")
        val air = item("obj.airrune")
        val mind = item("obj.mindrune")
        val worldQueues = WorldQueueList()
        var lastSpends: List<ResourceSpend> = emptyList()

        val random =
            mockk<GameRandom>(relaxed = true) {
                if (saveRolls.isNotEmpty()) {
                    every { of(maxExclusive = 100) } returnsMany saveRolls
                }
                every { randomBoolean(any()) } returns false
            }
        val ammo =
            RangedAmmoManager(
                random = random,
                eventBus = events,
                collision = mockk<CollisionFlagMap>(relaxed = true),
                worldQueues = worldQueues,
                objRepo = mockk<ObjRepository>(relaxed = true),
            )
        val objCharges = mockk<ObjChargeManager>()
        val charges = CombatChargeManager(objCharges, random)
        private val runes =
            spyk(
                MagicRuneManager(
                    mockk(relaxed = true),
                    mockk(relaxed = true),
                    mockk(relaxed = true),
                    mockk(relaxed = true),
                    mockk(relaxed = true),
                    mockk(relaxed = true),
                    emptySet(),
                    events,
                    random,
                )
            )

        val spellSpends = mutableListOf<PlayerSpellRuneEvents.Spent>()
        var lastSpell: ItemServerType? = null

        init {
            events.subscribeUnbound(PlayerAttackEvents.AttackResult::class.java) {
                lastSpends = resources
            }
            events.subscribeUnbound(PlayerSpellRuneEvents.Spent::class.java) {
                spellSpends += this
            }
        }

        fun onPrepare(action: PlayerAttackEvents.AttackPrepare.() -> Unit) {
            events.subscribeUnbound(PlayerAttackEvents.AttackPrepare::class.java, action)
        }

        fun onSpellPrepare(action: PlayerSpellRuneEvents.Prepare.() -> Unit) {
            events.subscribeUnbound(PlayerSpellRuneEvents.Prepare::class.java, action)
        }

        fun attack(block: () -> Unit): List<ResourceSpend> {
            lastSpends = emptyList()
            events.beginAttackRecord(player, npc, AttackKind.Ranged, null, null)
            events.prepareAttackRecord(player, baseCycles = 4)
            block()
            events.finishAttackRecord(player)
            return lastSpends
        }

        fun <T> attackReturning(block: () -> T): T {
            var result: T? = null
            attack { result = block() }
            @Suppress("UNCHECKED_CAST")
            return result as T
        }

        fun cast(vararg runeSets: MagicRunes.Validation.Valid): MagicRuneManager.CastResult {
            val spell = mockk<MagicSpell>(relaxed = true)
            lastSpell = spell.obj
            every { runes.validateSpell(player, spell) } returns runeSets.toList()
            return runes.delReqs(player, spell)
        }

        fun invRunes(slot: Int, rune: ItemServerType, count: Int) =
            MagicRunes.Validation.Valid.HasEnough(
                listOf(MagicRunes.Source.InvSource(symbol(rune), slot, count))
            )

        fun splitRunes(slot: Int, invCount: Int, pouchCount: Int) =
            MagicRunes.Validation.Valid.HasEnough(
                listOf(
                    MagicRunes.Source.InvSource(symbol(air), slot, invCount),
                    MagicRunes.Source.VarBitSource(POUCH, pouchCount, symbol(air)),
                )
            )

        fun quiver(count: Int) {
            player.worn[Wearpos.Quiver.slot] = InvObj(arrow.id, count)
        }

        fun quiverCount(): Int = player.worn[Wearpos.Quiver.slot]?.count ?: 0

        fun inv(slot: Int, rune: ItemServerType, count: Int) {
            player.inv[slot] = InvObj(rune.id, count)
        }

        fun invCount(rune: ItemServerType): Int =
            player.inv.filterNotNull().filter { it.id == rune.id }.sumOf { it.count }

        fun pouch(count: Int) {
            VarPlayerIntMapSetter.set(player, POUCH, count)
        }

        fun pouchCount(): Int = player.vars[POUCH]

        private fun inventory(size: Int) =
            Inventory(InventoryServerType(size = size), arrayOfNulls(size))

        private fun symbol(rune: ItemServerType): String =
            dev.openrune.rscm.RSCM.getReverseMapping(RSCMType.OBJ, rune.id)
    }

    companion object {
        private const val VAROBJ = "varobj.tumeken_charges"
        private const val POUCH = "varbit.rune_pouch_quantity_1"
        private val RESTORED = mutableListOf<() -> Unit>()

        private fun item(symbol: String): ItemServerType =
            ServerCacheManager.getItem(symbol.asRSCM(RSCMType.OBJ))!!

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
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORED += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            RESTORED += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
