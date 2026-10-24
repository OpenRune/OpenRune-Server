package org.rsmod.content.areas.misc.lobbies.pestcontrol

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.content.interfaces.collectionlog.collectionTransmit
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.stat.PlayerSkillXPTable
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class PestControlRewardsTest {
    @Test
    fun `every equipment reward debits its own price and grants exactly one logged item`() {
        val items = listOf(
            "pest_void_knight_mace", "pest_void_knight_top", "pest_void_knight_robes",
            "pest_void_knight_gloves", "game_pest_mage_helm", "game_pest_archer_helm",
            "game_pest_melee_helm", "pest_seal_8",
        )
        val prices = listOf(250, 250, 250, 150, 200, 200, 200, 10)
        for ((index, item) in items.withIndex()) {
            val f = Fixture(points = prices[index])
            f.open()
            f.click(24 + index)
            assertEquals(0, f.player.pestControlPoints, item)
            assertEquals(mapOf("obj.$item" to 1), f.totals(), item)
            assertEquals(1, f.player.collectionTransmit.count("obj.$item"), item)
            assertTrue(f.player.ui.containsModal("interface.messagebox"))
            f.continueReward()
            assertTrue(f.player.ui.containsModal("interface.pest_rewardshop"))
        }
    }

    @Test
    fun `full inventory never debits points or credits the collection log`() {
        val f = Fixture(points = 4000)
        f.fill()
        val before = f.totals()
        f.open()
        f.click(27)
        assertEquals(before, f.totals())
        assertEquals(4000, f.player.pestControlPoints)
        assertEquals(0, f.player.collectionTransmit.count("obj.pest_void_knight_gloves"))
        f.continueReward()
        assertTrue(f.player.ui.containsModal("interface.pest_rewardshop"))
    }

    @Test
    fun `insufficient persistent balance cannot be authorized by display scratch`() {
        val f = Fixture(points = 149)
        f.open()
        VarPlayerIntMapSetter.set(f.player, "varp.if1", 4000)
        f.click(27)
        assertEquals(149, f.player.pestControlPoints)
        assertEquals(emptyMap<String, Int>(), f.totals())
    }

    @Test
    fun `each equipment requirement uses base level and seal has no skill gate`() {
        for ((skill, required) in EQUIPMENT_REQUIREMENTS) {
            val f = Fixture(points = 1000)
            f.level(skill, required - 1, current = 99)
            f.open()
            f.click(24)
            assertEquals(1000, f.player.pestControlPoints, skill)
            assertEquals(emptyMap<String, Int>(), f.totals(), skill)
        }
        val f = Fixture(points = 10)
        for (skill in EQUIPMENT_REQUIREMENTS.keys) f.level(skill, 1)
        f.open()
        f.click(31)
        assertEquals(0, f.player.pestControlPoints)
        assertEquals(mapOf("obj.pest_seal_8" to 1), f.totals())
    }

    @Test
    fun `all skill denominations use divide before multiply and preserve fractional bonuses`() {
        val skills = listOf("attack", "strength", "defence", "ranged", "magic", "hitpoints", "prayer")
        val multipliers = listOf(35, 35, 35, 32, 32, 35, 18)
        for ((index, skill) in skills.withIndex()) {
            for (level in listOf(25, 34, 35, 42, 43, 97, 98, 99)) {
                for ((mode, fineMultiplier) in listOf(10, 101, 1100).withIndex()) {
                    val f = Fixture(points = 4000)
                    f.level(skill, level, current = 1)
                    val before = f.player.statMap.getFineXP("stat.$skill")
                    f.open()
                    f.click(index * 3 + mode)
                    val expected = (level * level / 600) * multipliers[index] * fineMultiplier
                    assertEquals(expected, f.player.statMap.getFineXP("stat.$skill") - before, "$skill/$level/$mode")
                    assertEquals(4000 - listOf(1, 10, 100)[mode], f.player.pestControlPoints)
                    assertEquals(emptyMap<String, Int>(), f.totals())
                }
            }
        }
    }

    @Test
    fun `boosting base24 cannot bypass the XP training requirement`() {
        val f = Fixture(points = 100)
        f.level("attack", 24, current = 99)
        val before = f.player.statMap.getFineXP("stat.attack")
        f.open()
        f.click(0)
        assertEquals(before, f.player.statMap.getFineXP("stat.attack"))
        assertEquals(100, f.player.pestControlPoints)
    }

    @Test
    fun `configured personal and global experience rates apply without changing point cost`() {
        val f = Fixture(points = 10)
        f.player.xpRate = 1.5
        f.player.globalXpRate = 2.0
        val before = f.player.statMap.getFineXP("stat.prayer")
        f.open()
        f.click(19)
        assertEquals(87264, f.player.statMap.getFineXP("stat.prayer") - before)
        assertEquals(0, f.player.pestControlPoints)
    }

    @Test
    fun `bulk reward crossing a level uses the starting base level once`() {
        val f = Fixture(points = 100)
        f.level("attack", 34)
        f.player.statMap.setXP("stat.attack", PlayerSkillXPTable.getXPFromLevel(35) - 1)
        val before = f.player.statMap.getFineXP("stat.attack")
        f.open()
        f.click(2)
        assertEquals(38500, f.player.statMap.getFineXP("stat.attack") - before)
        assertEquals(0, f.player.pestControlPoints)
        assertTrue(f.player.statMap.getBaseLevel("stat.attack") > 34)
    }

    @Test
    fun `experience near200m clamps without wrapping or creating negative points`() {
        val f = Fixture(points = 100)
        f.player.statMap.setFineXP("stat.attack", PlayerStatMap.MAX_FINE_XP - 5)
        f.open()
        f.click(2)
        assertEquals(PlayerStatMap.MAX_FINE_XP, f.player.statMap.getFineXP("stat.attack"))
        assertEquals(0, f.player.pestControlPoints)
    }

    @Test
    fun `mineral pack grants both noted stacks with exact conservation`() {
        val f = Fixture(points = 30)
        f.give("obj.cert_coal", 9)
        f.give("obj.cert_iron_ore", 4)
        f.fill()
        f.open()
        f.click(22)
        assertEquals(34, f.player.inv.count("obj.cert_coal"))
        assertEquals(22, f.player.inv.count("obj.cert_iron_ore"))
        assertEquals(26, f.player.inv.count("obj.bronze_dagger"))
        assertEquals(15, f.player.pestControlPoints)
    }

    @Test
    fun `mineral overflow rolls back earlier successful output too`() {
        val f = Fixture(points = 30)
        f.give("obj.cert_coal", 7)
        f.give("obj.cert_iron_ore", Int.MAX_VALUE - 17)
        val before = f.totals()
        f.open()
        f.click(22)
        assertEquals(before, f.totals())
        assertEquals(30, f.player.pestControlPoints)
    }

    @Test
    fun `herb packs grant all six noted grimy herbs at inclusive endpoints`() {
        val herbs = listOf("harralander", "ranarr", "toadflax", "irit", "avantoe", "kwuarm")
        for (maximum in listOf(false, true)) {
            val f = Fixture(points = 30, maximum = maximum)
            f.open()
            f.click(21)
            val quantities = if (maximum) listOf(4, 3, 4, 4, 4, 5) else listOf(2, 2, 1, 2, 1, 1)
            assertEquals(herbs.zip(quantities).associate { (name, count) -> "obj.cert_unidentified_$name" to count }, f.totals())
            assertEquals(0, f.player.pestControlPoints)
        }
    }

    @Test
    fun `partial herb pack cannot escape transaction when last output lacks space`() {
        val f = Fixture(points = 30)
        f.fill(23)
        val before = f.totals()
        f.open()
        f.click(21)
        assertEquals(before, f.totals())
        assertEquals(30, f.player.pestControlPoints)
    }

    @Test
    fun `pack skill checks current level rather than silently using equipment rules`() {
        for ((selection, skill) in listOf(21 to "herblore", 22 to "mining", 23 to "farming")) {
            val denied = Fixture(points = 100)
            denied.level(skill, 99, current = 24)
            denied.open()
            denied.click(selection)
            assertEquals(100, denied.player.pestControlPoints)
            assertEquals(emptyMap<String, Int>(), denied.totals())
            val boosted = Fixture(points = 100)
            boosted.level(skill, 24, current = 25)
            boosted.open()
            boosted.click(selection)
            assertEquals(100 - if (selection == 21) 30 else 15, boosted.player.pestControlPoints)
        }
    }

    @Test
    fun `seed pack requires three empty slots even when all rolls could merge`() {
        val f = Fixture(points = 15)
        f.give("obj.spirit_tree_seed", 3)
        f.fill(25)
        val before = f.totals()
        f.open()
        f.click(23)
        assertEquals(before, f.totals())
        assertEquals(15, f.player.pestControlPoints)
    }

    @Test
    fun `seed rolls with replacement merge and debit only once`() {
        val f = Fixture(points = 15)
        f.give("obj.spirit_tree_seed", 4)
        f.open()
        f.click(23)
        assertEquals(mapOf("obj.spirit_tree_seed" to 7), f.totals())
        assertEquals(0, f.player.pestControlPoints)
    }

    @Test
    fun `seed overflow never grants a partial pack or spends points`() {
        val f = Fixture(points = 15)
        f.give("obj.spirit_tree_seed", Int.MAX_VALUE - 1)
        val before = f.totals()
        f.open()
        f.click(23)
        assertEquals(before, f.totals())
        assertEquals(15, f.player.pestControlPoints)
    }

    @Test
    fun `malformed unopened and stale confirmations cannot buy anything`() {
        val f = Fixture(points = 1000)
        f.click(27)
        f.open()
        for (index in listOf(-1, 32, Int.MAX_VALUE)) f.click(index)
        f.click(27, IfButtonOp.Op2)
        assertEquals(1000, f.player.pestControlPoints)
        assertEquals(emptyMap<String, Int>(), f.totals())
        f.click(27)
        f.click(27)
        assertEquals(850, f.player.pestControlPoints)
        assertEquals(mapOf("obj.pest_void_knight_gloves" to 1), f.totals())
        assertEquals(1, f.player.collectionTransmit.count("obj.pest_void_knight_gloves"))
    }

    @Test
    fun `Continue enables a new intentional purchase but not free replay`() {
        val f = Fixture(points = 20)
        f.open()
        f.click(31)
        f.continueReward()
        f.click(31)
        assertEquals(0, f.player.pestControlPoints)
        assertEquals(mapOf("obj.pest_seal_8" to 2), f.totals())
        assertEquals(2, f.player.collectionTransmit.count("obj.pest_seal_8"))
    }

    @OptIn(InternalApi::class)
    private class Fixture(points: Int, maximum: Boolean = false) {
        val events = EventBus()
        val client = RecordingClient()
        val player = Player(client).apply {
            uuid = 795L
            observerUUID = 795L
            slotId = 1
            assignUid()
            coords = CoordGrid(2663, 2649, 0)
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            invMap["inv.inv"] = inv
            invMap["inv.worn"] = worn
        }
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() },
        )
        private var coroutine = GameCoroutine("pest-reward-test")
        private var result: Result<Unit>? = null

        init {
            player.pestControlPoints = points
            for (skill in EQUIPMENT_REQUIREMENTS.keys + listOf("herblore", "mining", "farming")) level(skill, 99)
            val random = object : GameRandom {
                override fun of(minInclusive: Int, maxInclusive: Int) = if (maximum) maxInclusive else minInclusive
                override fun of(maxExclusive: Int) = if (maximum) maxExclusive - 1 else 0
                override fun randomDouble() = 0.0
            }
            with(PestControlRewards(random)) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        fun level(skill: String, base: Int, current: Int = base) {
            player.statMap.setXP("stat.$skill", PlayerSkillXPTable.getXPFromLevel(base))
            player.statMap.setBaseLevel("stat.$skill", base.toByte())
            player.statMap.setCurrentLevel("stat.$skill", current.toByte())
        }

        fun open() = start { openPestControlRewards() }

        fun click(index: Int, op: IfButtonOp = IfButtonOp.Op1) = start {
            val component = ServerCacheManager.fromComponent("component.pest_rewardshop:confirm".asRSCM())
            assertTrue(events.publish(this, IfModalButton(component, index, null, op)))
        }

        private fun start(block: suspend ProtectedAccess.() -> Unit) {
            coroutine = GameCoroutine("pest-reward-test")
            player.activeCoroutine = coroutine
            result = null
            val action: suspend () -> Unit = { ProtectedAccess(player, coroutine, context).block() }
            action.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { this@Fixture.result = result }
            })
            result?.getOrThrow()
        }

        fun continueReward() {
            assertTrue(coroutine.isAwaiting(ResumePauseButtonInput::class))
            coroutine.resumeWith(ResumePauseButtonInput("component.messagebox:continue", -1))
            result?.getOrThrow()
            assertFalse(player.ui.containsModal("interface.messagebox"))
        }

        fun give(item: String, count: Int) {
            val slot = (0 until 28).first { player.inv[it] == null }
            player.inv[slot] = InvObj(item, count)
        }

        fun fill(count: Int = player.inv.freeSpace()) = repeat(count) { give("obj.bronze_dagger", 1) }

        fun totals(): Map<String, Int> = buildMap {
            for (slot in 0 until 28) {
                val obj = player.inv[slot] ?: continue
                val name = checkNotNull(ServerCacheManager.getItem(obj.id)).internalName
                put(name, (get(name) ?: 0) + obj.count)
            }
        }
    }

    private class RecordingClient : Client<Any, Any> {
        override fun write(message: Any) {}
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private val EQUIPMENT_REQUIREMENTS = mapOf(
            "attack" to 42, "strength" to 42, "defence" to 42, "ranged" to 42,
            "magic" to 42, "hitpoints" to 42, "prayer" to 22,
        )
        private val RESTORE = mutableListOf<() -> Unit>()

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(241).close()
            for ((owner, name) in listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORE += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            RESTORE += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            RESTORE.asReversed().forEach { it() }
            RESTORE.clear()
        }
    }
}
