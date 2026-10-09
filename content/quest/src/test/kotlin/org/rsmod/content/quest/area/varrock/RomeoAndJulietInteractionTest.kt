package org.rsmod.content.quest.area.varrock

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.table.ApothecaryPotionsRow
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.area.AreaIndex
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.game.region.RegionListWorldEntity
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class RomeoAndJulietInteractionTest {
    @Test fun `Juliet hands out one message, replaces a lost one and never duplicates it`() {
        val f = Fixture(10)
        f.talk(JULIET)
        f.finish()
        assertEquals(20, f.stage())
        assertEquals(1, f.total(MESSAGE))
        f.talk(JULIET)
        f.finish()
        assertEquals(1, f.total(MESSAGE))
        f.player.inv[0] = null
        f.talk(JULIET)
        f.finish()
        assertEquals(1, f.total(MESSAGE))
        assertEquals(20, f.stage())
    }

    @Test fun `a full inventory drops Juliet's message on the floor instead of losing it`() {
        val f = Fixture(10)
        f.fillInventory()
        f.talk(JULIET)
        f.finish()
        assertEquals(20, f.stage())
        assertEquals(0, f.player.inv.count(MESSAGE))
        assertEquals(1, f.ground(MESSAGE))
        assertEquals(28, f.player.inv.count(FILLER))
    }

    @Test fun `Romeo takes exactly one message and only when the player carries it`() {
        val lost = Fixture(20)
        lost.talk(ROMEO)
        lost.finish()
        assertEquals(20, lost.stage())

        val f = Fixture(20)
        f.player.inv[0] = InvObj(MESSAGE, 1)
        f.player.inv[1] = InvObj(MESSAGE, 1)
        f.talk(ROMEO)
        f.finish(listOf(4))
        assertEquals(30, f.stage())
        assertEquals(1, f.total(MESSAGE))
    }

    @Test fun `a message that vanishes during the hand-in does not advance the quest`() {
        val f = Fixture(20)
        f.player.inv[0] = InvObj(MESSAGE, 1)
        f.talk(ROMEO)
        f.finishUntil { f.player.ui.containsModal("interface.objectbox") }
        f.player.inv[0] = null
        f.finish(listOf(4))
        assertEquals(20, f.stage())
        assertEquals(0, f.total(MESSAGE))
    }

    @Test fun `berries become exactly one potion even with a full inventory`() {
        val f = Fixture(50)
        f.fillInventory()
        f.player.inv[0] = InvObj(BERRIES, 1)
        f.player.inv[1] = InvObj(BERRIES, 1)
        f.talk(APOTHECARY)
        f.finish(listOf(2, 1))
        assertEquals(50, f.stage())
        assertEquals(1, f.total(BERRIES))
        assertEquals(1, f.total(POTION))
    }

    @Test fun `berries are kept when the Apothecary has not asked for them yet`() {
        val f = Fixture(30)
        f.player.inv[0] = InvObj(BERRIES, 1)
        f.talk(APOTHECARY)
        f.finish(listOf(2, 3))
        assertEquals(30, f.stage())
        assertEquals(1, f.total(BERRIES))
        assertEquals(0, f.total(POTION))
    }

    @Test fun `Juliet takes exactly one potion`() {
        val f = Fixture(50)
        f.player.inv[0] = InvObj(POTION, 1)
        f.player.inv[1] = InvObj(POTION, 1)
        f.talk(JULIET)
        f.finish()
        assertEquals(60, f.stage())
        assertEquals(1, f.total(POTION))
        f.talk(JULIET)
        f.finish()
        assertEquals(1, f.total(POTION))
    }

    @Test fun `a potion that vanishes during the hand-in does not advance the quest`() {
        val f = Fixture(50)
        f.player.inv[0] = InvObj(POTION, 1)
        f.talk(JULIET)
        f.finishUntil { f.player.ui.containsModal("interface.chat_left") }
        f.player.inv[0] = null
        f.finish()
        assertEquals(50, f.stage())
        assertEquals(0, f.total(POTION))
    }

    @Test fun `cancelling the potion cutscene keeps the hand-in`() {
        val f = Fixture(50)
        f.player.inv[0] = InvObj(POTION, 1)
        f.talk(JULIET)
        f.finishUntil { f.player.vars["varbit.cutscene_status"] == 1 }
        f.cancel()
        assertEquals(60, f.stage())
        assertEquals(0, f.total(POTION))
    }

    @Test fun `drinking the potion swaps it for one empty vial and is refused outside the shop`() {
        val f = Fixture(50)
        f.fillInventory()
        f.player.inv[0] = InvObj(POTION, 1)
        f.held(POTION, op = 2)
        assertEquals(1, f.total(POTION))
        assertEquals(0, f.total(VIAL))

        f.player.coords = CoordGrid(3195, 3404, 0)
        f.held(POTION, op = 2, options = listOf(1))
        assertEquals(0, f.total(POTION))
        assertEquals(1, f.total(VIAL))
        assertEquals(50, f.stage())
    }

    @Test fun `the Apothecary takes exactly the ingredients and coins of each potion`() {
        for (potion in ApothecaryPotionsRow.all()) {
            val f = Fixture(0)
            f.stock(potion, coins = potion.coins + 100)
            val before = f.snapshot(potion)
            f.button(potion)
            assertEquals(1, f.total(potion.product.internalName), potion.key)
            for ((index, ingredient) in potion.ingredients.withIndex()) {
                assertEquals(
                    before.getValue(ingredient.internalName) - potion.amounts[index],
                    f.total(ingredient.internalName),
                    potion.key,
                )
            }
            assertEquals(100, f.total(COINS), potion.key)
        }
    }

    @Test fun `the Apothecary refuses a potion without consuming anything when an item is missing`() {
        for (potion in ApothecaryPotionsRow.all()) {
            val missing = potion.ingredients.size + if (potion.coins > 0) 1 else 0
            for (skip in 0 until missing) {
                val f = Fixture(0)
                f.stock(potion, coins = potion.coins, skip = skip)
                val before = f.snapshot(potion)
                f.button(potion)
                assertEquals(before, f.snapshot(potion), "${potion.key} without item $skip")
                assertEquals(0, f.total(potion.product.internalName), potion.key)
            }
        }
    }

    @Test fun `a full inventory still brews exactly one potion`() {
        for (potion in ApothecaryPotionsRow.all()) {
            val f = Fixture(0)
            f.fillInventory()
            f.stock(potion, coins = potion.coins)
            f.button(potion)
            assertEquals(1, f.total(potion.product.internalName), potion.key)
            for (ingredient in potion.ingredients) {
                assertEquals(0, f.total(ingredient.internalName), potion.key)
            }
        }
    }

    @Test fun `clicking twice with the items for one potion brews one`() {
        for (potion in ApothecaryPotionsRow.all()) {
            val f = Fixture(0)
            f.stock(potion, coins = potion.coins)
            f.button(potion)
            f.button(potion)
            assertEquals(1, f.total(potion.product.internalName), potion.key)
        }
    }

    @Test fun `paying from a maxed coin stack takes only the price`() {
        val potion = ApothecaryPotionsRow.all().first { it.coins > 0 }
        val f = Fixture(0)
        f.stock(potion, coins = Int.MAX_VALUE)
        f.button(potion)
        assertEquals(Int.MAX_VALUE - potion.coins, f.total(COINS))
        assertEquals(1, f.total(potion.product.internalName))
    }

    @Test fun `the quest hands out and takes back each item exactly once`() {
        val f = Fixture()
        f.talk(ROMEO)
        f.finish(listOf(3, 1, 3))
        f.talk(JULIET)
        f.finish()
        assertEquals(1, f.total(MESSAGE))
        f.talk(ROMEO)
        f.finish(listOf(4))
        assertEquals(0, f.total(MESSAGE))
        f.talk(LAWRENCE)
        f.finish()
        f.talk(APOTHECARY)
        f.finish(listOf(2, 1, 4))
        f.player.inv[1] = InvObj(BERRIES, 1)
        f.talk(APOTHECARY)
        f.finish(listOf(2, 1))
        assertEquals(0, f.total(BERRIES))
        assertEquals(1, f.total(POTION))
        f.talk(JULIET)
        f.finish()
        assertEquals(60, f.stage())
        assertEquals(0, f.total(POTION))
        f.talk(ROMEO)
        f.finish()
        f.assertComplete()
        assertEquals(0, f.total(MESSAGE) + f.total(POTION) + f.total(BERRIES))
    }

    private class Fixture(stage: Int = 0) {
        val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("romeo-and-juliet-test")
        private var result: Result<Unit>? = null
        val npcs = NpcList()
        private lateinit var objs: ObjRegistry
        private val collision = CollisionFlagMap()
        private lateinit var regions: RegionRegistry
        private lateinit var npcRepo: NpcRepository
        private val random =
            object : GameRandom by DefaultGameRandom(1) {
                override fun of(minInclusive: Int, maxInclusive: Int) = minInclusive
            }
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getAlignment = { TextAlignment() },
                    getNpcInteractions = { NpcInteractions(events) },
                    getCollision = { collision },
                    getNpcList = { npcs },
                    getTeleportValidator = { PlayerTeleportValidator(emptySet()) },
                    getAreaChecker = { AreaChecker(regions, AreaIndex()) },
                    getRandom = { random },
                )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 793L
                observerUUID = 793L
                slotId = 1
                assignUid()
                coords = CoordGrid(3211, 3424, 0)
                currentMapClock = 100
                processedMapClock = 100
                inv =
                    Inventory(
                        checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                        arrayOfNulls(28),
                    )
                worn =
                    Inventory(
                        checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())),
                        arrayOfNulls(14),
                    )
            }

        lateinit var script: RomeoAndJuliet

        init {
            val clock = MapClock(100)
            val updates = ZoneUpdateMap()
            val storage = LocZoneStorage()
            val normal = LocRegistryNormal(updates, collision, storage)
            val npcRegistry = NpcRegistry(npcs, collision, events)
            regions =
                RegionRegistry(
                    RegionListSmall(),
                    RegionListLarge(),
                    RegionListWorldEntity(),
                    normal,
                    collision,
                    storage,
                    npcRegistry,
                    ControllerRegistry(clock, ControllerList()),
                    ZonePlayerActivityBitSet(),
                )
            npcRepo = NpcRepository(clock, npcRegistry, npcs)
            objs = ObjRegistry(updates)
            script = RomeoAndJuliet(npcRepo, ObjRepository(clock, objs))
            with(script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
            setStage(stage)
            val zones =
                listOf(
                    CoordGrid(3157, 3425, 1),
                    CoordGrid(2333, 4646, 0),
                    CoordGrid(3215, 3418, 0),
                    CoordGrid(3195, 3404, 0),
                )
            for (zone in zones) collision.allocateIfAbsent(zone.x, zone.z, zone.level)
            for (x in 2320..2340 step 8) for (z in 4630..4660 step 8) {
                collision.allocateIfAbsent(x, z, 0)
            }
        }

        fun setStage(stage: Int) {
            VarPlayerIntMapSetter.set(player, "varbit.romeo_juliet_progress", stage)
        }

        fun stage() = script.quest.getQuestStage(player)

        fun access() = ProtectedAccess(player, coroutine, context)

        fun fillInventory() {
            for (slot in 0 until 28) player.inv[slot] = InvObj(FILLER, 1)
        }

        fun ground(obj: String): Int =
            objs.findAll(player.coords).filter { it.type == obj.asRSCM() }.sumOf { it.count }

        fun total(obj: String): Int = player.inv.count(obj) + ground(obj)

        fun stock(potion: ApothecaryPotionsRow, coins: Int, skip: Int? = null) {
            var slot = 0
            fun give(obj: String, count: Int) {
                val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
                if (type.isStackable) {
                    player.inv[slot++] = InvObj(obj, count)
                } else {
                    repeat(count) { player.inv[slot++] = InvObj(obj, 1) }
                }
            }
            potion.ingredients.forEachIndexed { index, ingredient ->
                if (index != skip) give(ingredient.internalName, potion.amounts[index])
            }
            if (coins > 0 && skip != potion.ingredients.size) give(COINS, coins)
        }

        fun snapshot(potion: ApothecaryPotionsRow): Map<String, Int> =
            (potion.ingredients.map { it.internalName } + COINS + potion.product.internalName)
                .associateWith { total(it) }

        fun button(potion: ApothecaryPotionsRow) {
            val component =
                ServerCacheManager.fromComponent("component.apothecary_potions:${potion.key}")
            start {
                assertTrue(
                    events.publish(this, IfModalButton(component, -1, null, IfButtonOp.Op1))
                )
            }
            finish()
        }

        fun talk(npc: String, op: Int = 1) {
            val target = Npc(npc, player.coords.translateZ(1))
            start {
                val event = if (op == 3) NpcEvents.Op3(target) else NpcEvents.Op1(target)
                assertTrue(events.publish(this, event))
            }
        }

        fun held(obj: String, op: Int, options: List<Int> = emptyList()) {
            val slot = (0 until 28).first { player.inv[it]?.id == obj.asRSCM() }
            val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
            start {
                val event =
                    if (op == 2) HeldObjEvents.Op2(slot, checkNotNull(inv[slot]), type, inv)
                    else HeldObjEvents.Op1(slot, checkNotNull(inv[slot]), type, inv)
                assertTrue(events.publish(this, event))
            }
            finish(options)
        }

        private fun start(block: suspend ProtectedAccess.() -> Unit) {
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val body: suspend () -> Unit = { access().block() }
            body.startCoroutine(
                object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<Unit>) {
                        this@Fixture.result = result
                    }
                }
            )
            result?.getOrThrow()
        }

        fun finishUntil(condition: () -> Boolean) {
            repeat(1000) {
                if (condition()) return
                advance(emptyList<Int>().iterator())
            }
            fail<Unit>("Condition never reached: ${output()}")
        }

        fun cancel() {
            coroutine.cancel()
            assertInstanceOf(
                kotlin.coroutines.cancellation.CancellationException::class.java,
                result?.exceptionOrNull(),
            )
            result = null
            player.activeCoroutine = null
        }

        fun finish(options: List<Int> = emptyList()) {
            val selections = options.iterator()
            repeat(1000) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${output()}")
        }

        private fun advance(options: Iterator<Int>) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val input =
                    when {
                        player.ui.containsModal("interface.chatmenu") ->
                            ResumePauseButtonInput(
                                "component.chatmenu:options",
                                if (options.hasNext()) options.next() else 1,
                            )
                        player.ui.containsModal("interface.objectbox") ->
                            ResumePauseButtonInput("component.objectbox:universe", -1)
                        else -> {
                            val parent =
                                listOf("chat_left", "chat_right", "messagebox").firstOrNull {
                                    player.ui.containsModal("interface.$it")
                                } ?: error("Unknown dialogue: ${output()}")
                            ResumePauseButtonInput("component.$parent:continue", -1)
                        }
                    }
                coroutine.resumeWith(input)
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
            }
            result?.getOrThrow()
        }

        fun assertComplete() {
            assertEquals(100, script.quest.getQuestStage(player))
            assertEquals(100, player.vars["varp.rjquest"])
            assertEquals(5, player.vars["varp.qp"])
            assertEquals(0, player.vars["varbit.cutscene_status"])
        }

        fun output() = client.messages.joinToString("\n")
    }

    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()

        override fun write(message: Any) {
            messages += message
        }

        override fun close() {}

        override fun read(player: Player) {}

        override fun flush() {}

        override fun flushHighPriority() {}

        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val ROMEO = "npc.romeo"
        private const val JULIET = "npc.juliet"
        private const val LAWRENCE = "npc.father_lawrence"
        private const val APOTHECARY = "npc.apothecary"
        private const val MESSAGE = "obj.julietmessage"
        private const val POTION = "obj.cadava"
        private const val BERRIES = "obj.cadavaberries"
        private const val COINS = "obj.coins"
        private const val VIAL = "obj.vial_empty"
        private const val FILLER = "obj.bronze_dagger"
        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field =
                    Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
