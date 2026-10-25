package org.rsmod.content.skills.herblore

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
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
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.events.PlayerQueueEvents
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.front
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.herblore.HerbloreFinishedRow
import org.rsmod.api.table.herblore.HerbloreUnfinishedRow
import org.rsmod.content.skills.farming.FarmingBags
import org.rsmod.events.EventBus
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.game.client.Client
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ReagentPouchBrewingTest {
    @Test
    fun `full inventory brews 26 potions from pouch while retaining loose catalyst`() {
        val f = Fixture()
        val potion = prayerPotion()
        f.player.inv[0] = InvObj(FarmingBags.REAGENT_POUCH_OPEN)
        f.player.inv[1] = InvObj("obj.snape_grass")
        for (slot in 2..27) f.player.inv[slot] = InvObj("obj.ranarrvial")
        f.pouch[0] = InvObj("obj.snape_grass", 26)
        assertEquals(26, potion.maxProducible(f.player.inv, f.pouch))

        repeat(26) {
            assertTrue(potion.brew(f.player, f.pouch, potion.outputPotion.internalName, NO_SAVES))
        }
        assertEquals(26, f.player.inv.physicalCount(potion.outputPotion.internalName))
        assertEquals(0, f.player.inv.physicalCount("obj.ranarrvial"))
        assertEquals(1, f.player.inv.physicalCount("obj.snape_grass"))
        assertEquals(0, f.pouch.physicalCount("obj.snape_grass"))
        assertFalse(potion.brew(f.player, f.pouch, potion.outputPotion.internalName, NO_SAVES))
        assertEquals(26, f.player.inv.physicalCount(potion.outputPotion.internalName))
    }

    @Test
    fun `missing pouch secondary never consumes unfinished potion and saved secondary stays stored`() {
        val f = Fixture()
        val potion = prayerPotion()
        f.player.inv[0] = InvObj("obj.ranarrvial")
        f.pouch[0] = InvObj("obj.snape_grass")
        assertTrue(potion.hasRequiredMaterials(f.player.inv, f.pouch))
        f.pouch[0] = null
        assertFalse(potion.brew(f.player, f.pouch, potion.outputPotion.internalName, NO_SAVES))
        assertEquals(1, f.player.inv.physicalCount("obj.ranarrvial"))
        assertEquals(0, f.player.inv.physicalCount(potion.outputPotion.internalName))

        f.pouch[0] = InvObj("obj.snape_grass")
        val saved = HerbloreGoggles.SavedSecondaries(single = "obj.snape_grass" to 1)
        assertTrue(potion.brew(f.player, f.pouch, potion.outputPotion.internalName, saved))
        assertEquals(0, f.player.inv.physicalCount("obj.ranarrvial"))
        assertEquals(1, f.player.inv.physicalCount(potion.outputPotion.internalName))
        assertEquals(1, f.pouch.physicalCount("obj.snape_grass"))
    }

    @Test
    fun `extra dose preparation cannot consume last chemistry charge before successful brew`() {
        val f = Fixture()
        val potion = prayerPotion()
        f.player.front = InvObj("obj.amulet_of_chemistry")
        VarPlayerIntMapSetter.set(f.player, AmuletOfChemistry.CHARGES_VARBIT, 1)
        f.pouch[0] = InvObj("obj.snape_grass")
        val result = AmuletOfChemistry.rollBrewOutput(f.player, AlwaysProc, potion)
        assertTrue(result.extraDoseApplied)
        assertFalse(potion.brew(f.player, f.pouch, result.output, NO_SAVES))
        assertEquals(1, f.player.chemistryChargeCount())
        assertEquals(InvObj("obj.amulet_of_chemistry"), f.player.front)
        assertEquals(1, f.pouch.physicalCount("obj.snape_grass"))

        f.player.inv[0] = InvObj("obj.ranarrvial")
        assertTrue(potion.brew(f.player, f.pouch, result.output, NO_SAVES))
        AmuletOfChemistry.applyBrewOutput(f.player, result)
        assertEquals(0, f.player.chemistryChargeCount())
        assertNull(f.player.front)
        assertEquals(1, f.player.inv.physicalCount(result.output))
        assertEquals(0, f.pouch.physicalCount("obj.snape_grass"))
    }

    @Test
    fun `huasca cleaning replaces a full inventory slot and awards fractional XP once`() {
        val f = Fixture()
        for (slot in 0..27) f.player.inv[slot] = InvObj("obj.spade")
        f.player.inv[0] = InvObj("obj.unidentified_huasca")
        f.clean(0)
        f.clean(0)
        f.processQueue()
        f.processQueue()
        assertEquals(0, f.player.inv.physicalCount("obj.unidentified_huasca"))
        assertEquals(1, f.player.inv.physicalCount("obj.huasca"))
        assertEquals(27, f.player.inv.physicalCount("obj.spade"))
        assertEquals(118, f.xp())
    }

    @Test
    fun `vanished grimy herb cannot produce a clean herb or XP`() {
        val f = Fixture()
        f.player.inv[0] = InvObj("obj.unidentified_huasca")
        f.clean(0)
        f.player.inv[0] = null
        f.processQueue()
        assertEquals(0, f.player.inv.physicalCount("obj.huasca"))
        assertEquals(0, f.xp())
    }

    @Test
    fun `huasca unfinished and finished recipes conserve inputs with a full inventory`() {
        val f = Fixture()
        for (slot in 0..27) f.player.inv[slot] = InvObj("obj.spade")
        f.player.inv[0] = InvObj("obj.huasca")
        f.player.inv[1] = InvObj("obj.vial_water")
        val unfinished = HerbloreUnfinishedRow.getRow("dbrow.herblore_huasca_unfinished")
        assertTrue(unfinished.mix(f.player))
        assertFalse(unfinished.mix(f.player))
        assertEquals(0, f.player.inv.physicalCount("obj.huasca"))
        assertEquals(0, f.player.inv.physicalCount("obj.vial_water"))
        assertEquals(1, f.player.inv.physicalCount("obj.huascavial"))
        val free = f.player.inv.indices.first { f.player.inv[it] == null }
        f.player.inv[free] = InvObj("obj.aldarium", Int.MAX_VALUE)
        val potion = HerbloreFinishedRow.getRow("dbrow.herblore_prayer_regeneration")
        assertTrue(potion.brew(f.player, null, potion.outputPotion.internalName, NO_SAVES))
        assertFalse(potion.brew(f.player, null, potion.outputPotion.internalName, NO_SAVES))
        assertEquals(0, f.player.inv.physicalCount("obj.huascavial"))
        assertEquals(Int.MAX_VALUE - 1, f.player.inv.physicalCount("obj.aldarium"))
        assertEquals(1, f.player.inv.physicalCount("obj.3dose1prayer_regeneration"))
        assertEquals(26, f.player.inv.physicalCount("obj.spade"))
    }

    @Test
    fun `vanished huasca ingredients leave all other inputs untouched`() {
        val f = Fixture()
        val unfinished = HerbloreUnfinishedRow.getRow("dbrow.herblore_huasca_unfinished")
        f.player.inv[0] = InvObj("obj.vial_water")
        assertFalse(unfinished.mix(f.player))
        assertEquals(1, f.player.inv.physicalCount("obj.vial_water"))
        assertEquals(0, f.player.inv.physicalCount("obj.huascavial"))
        f.player.inv[0] = InvObj("obj.huasca")
        assertFalse(unfinished.mix(f.player))
        assertEquals(1, f.player.inv.physicalCount("obj.huasca"))
        assertEquals(0, f.player.inv.physicalCount("obj.huascavial"))
        f.player.inv[0] = InvObj("obj.huascavial")
        val potion = HerbloreFinishedRow.getRow("dbrow.herblore_prayer_regeneration")
        assertFalse(potion.brew(f.player, null, potion.outputPotion.internalName, NO_SAVES))
        assertEquals(1, f.player.inv.physicalCount("obj.huascavial"))
        assertEquals(0, f.player.inv.physicalCount(potion.outputPotion.internalName))
    }

    private fun prayerPotion() = HerbloreFinishedRow.getRow("dbrow.herblore_prayer_potion")

    private class Fixture {
        val player = Player().apply {
            client = SilentClient()
            val stat = "stat.herblore"
            statMap.setXP(stat, 224_463)
            statMap.setBaseLevel(stat, 58)
            statMap.setCurrentLevel(stat, 58)
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }
        val pouch = FarmingBags.reagentPouch.store(player)
        private val events = EventBus()
        private val coroutine = GameCoroutine("huasca-production-test")
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() },
        )
        private val initialXp = player.statMap.getFineXP("stat.herblore")

        init {
            with(CleaningHerbsEvents()) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        fun xp() = player.statMap.getFineXP("stat.herblore") - initialXp

        fun clean(slot: Int) = run {
            val obj = checkNotNull(player.inv[slot])
            val type = checkNotNull(ServerCacheManager.getItem(obj.id))
            assertTrue(events.publish(this, HeldObjEvents.Op1(slot, obj, type, player.inv)))
        }

        fun processQueue() {
            val iterator = checkNotNull(player.weakQueueList.iterator())
            val queue = iterator.next()
            iterator.remove()
            run { assertTrue(events.publish(this, PlayerQueueEvents.Protected(queue.args, queue.id))) }
        }

        private fun run(action: suspend ProtectedAccess.() -> Unit) {
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, context)
            var result: Result<Unit>? = null
            val body: suspend () -> Unit = { access.action() }
            body.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(value: Result<Unit>) { result = value }
            })
            checkNotNull(result).getOrThrow()
        }
    }

    private class SilentClient : Client<Any, Any> {
        override fun write(message: Any) {}
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    private object AlwaysProc : GameRandom {
        override fun of(maxExclusive: Int): Int = 0
        override fun of(minInclusive: Int, maxInclusive: Int): Int = minInclusive
        override fun randomDouble(): Double = 0.0
    }

    companion object {
        private val NO_SAVES = HerbloreGoggles.SavedSecondaries.NONE
        private val RESTORED = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic @BeforeAll fun cache() {
            val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision = Files.readAllLines(root.resolve("game.yml"))
                .first { it.startsWith("revision:") }.substringAfter(':').trim().substringBefore('.').toInt()
            ServerCacheManager.init(revision).close()
            for ((owner, name) in listOf(
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

        @JvmStatic @AfterAll fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
