package org.rsmod.content.other.treasuretrails

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.table.TrailTierRow
import org.rsmod.api.table.cluehelper.CluehelperClueCrypticRow
import org.rsmod.api.table.cluehelper.CluehelperClueSkillchallengeRow
import org.rsmod.content.other.treasuretrails.TrailFlow.advance
import org.rsmod.content.other.treasuretrails.TrailFlow.openBox
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class TreasureTrailsItemTest {
    @Test
    fun `opening a casket consumes it once and delivers every reward`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(f.tier.casketObj.symbol(), 1)

        f.openCasket()
        val shown = f.shownRewards()
        assertEquals(0, f.player.inv.count(f.tier.casketObj.symbol()))
        assertEquals(1, f.completed())
        assertTrue(shown.isNotEmpty())
        assertEquals(shown, f.held())

        f.openCasket()
        assertEquals(1, f.completed())
        assertEquals(shown, f.held())
    }

    @Test
    fun `a casket opened with a full inventory drops the overflow instead of losing it`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(f.tier.casketObj.symbol(), 1)
        for (slot in 1 until 28) f.player.inv[slot] = InvObj(FILLER, 1)

        f.openCasket()
        val delivered = f.held().toMutableMap()
        delivered.merge(FILLER.asRSCM(), -27, Int::plus)
        delivered.values.removeIf { it == 0 }
        for ((obj, count) in f.ground()) delivered.merge(obj, count, Int::plus)
        assertEquals(f.shownRewards(), delivered)
        assertEquals(27, f.player.inv.count(FILLER))
    }

    @Test
    fun `the final step swaps the clue for exactly one casket`() {
        val f = Fixture()
        f.player.inv[3] = InvObj(f.clue, 1)
        f.startTrail(step = HANS, length = 1)

        val given = f.access().advance(f.tier)
        assertEquals(f.tier.casketObj.id, given?.id)
        assertEquals(0, f.player.inv.count(f.clue))
        assertEquals(1, f.player.inv.count(f.tier.casketObj.symbol()))
        assertNull(TrailFlow.currentStep(f.player, f.tier))

        assertNull(f.access().advance(f.tier))
        assertEquals(1, f.player.inv.count(f.tier.casketObj.symbol()))
    }

    @Test
    fun `a clue that left the inventory cannot be advanced`() {
        val f = Fixture()
        f.bank[0] = InvObj(f.clue, 1)
        f.startTrail(step = HANS, length = 1)

        assertNull(f.access().advance(f.tier))
        assertEquals(0, TrailState(f.player, f.tier).progress)
        assertEquals(0, f.player.inv.count(f.tier.casketObj.symbol()))
        assertEquals(1, f.bank.count(f.clue))
    }

    @Test
    fun `an earlier step keeps the clue and moves the trail on`() {
        val f = Fixture()
        f.player.inv[3] = InvObj(f.clue, 1)
        f.startTrail(step = HANS, length = 3)

        val given = f.access().advance(f.tier)
        assertEquals(f.tier.clueObj?.id, given?.id)
        assertEquals(1, f.player.inv.count(f.clue))
        assertEquals(1, TrailState(f.player, f.tier).progress)
        assertTrue(TrailFlow.currentStep(f.player, f.tier) != null)
    }

    @Test
    fun `a scroll box becomes exactly one clue`() {
        val f = Fixture()
        f.player.inv[5] = InvObj(f.box, 1)

        f.run { openBox(f.tier) }
        assertEquals(0, f.player.inv.count(f.box))
        assertEquals(1, f.player.inv.count(f.clue))
    }

    @Test
    fun `a scroll box stays shut while a clue of the tier is held`() {
        val f = Fixture()
        f.player.inv[5] = InvObj(f.box, 1)
        f.bank[0] = InvObj(f.clue, 1)

        f.run { openBox(f.tier) }
        assertEquals(1, f.player.inv.count(f.box))
        assertEquals(0, f.player.inv.count(f.clue))
        assertEquals(1, f.bank.count(f.clue))
    }

    @Test
    fun `a scroll box stack is untouched when there is no room for the clue`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(f.box, 2)
        for (slot in 1 until 28) f.player.inv[slot] = InvObj(FILLER, 1)

        f.run { openBox(f.tier) }
        assertEquals(2, f.player.inv.count(f.box))
        assertEquals(0, f.player.inv.count(f.clue))
    }

    @Test
    fun `charlie takes the requested item exactly once`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(f.clue, 1)
        f.player.inv[1] = InvObj(TROUT, 1)
        f.player.inv[2] = InvObj(TROUT, 1)
        f.startTrail(step = CHARLIE_TROUT, length = 3)

        f.run { with(f.npcHook) { op(f.charlie(), InteractionOp.Op1) } }
        assertEquals(1, f.player.inv.count(TROUT))
        assertEquals(1, TrailState(f.player, f.tier).progress)
        assertEquals(1, f.player.inv.count(f.clue))
    }

    @Test
    fun `charlie takes nothing when the clue is not held`() {
        val f = Fixture()
        f.bank[0] = InvObj(f.clue, 1)
        f.player.inv[1] = InvObj(TROUT, 1)
        f.startTrail(step = CHARLIE_TROUT, length = 3)

        f.run { with(f.npcHook) { op(f.charlie(), InteractionOp.Op1) } }
        assertEquals(1, f.player.inv.count(TROUT))
        assertEquals(0, TrailState(f.player, f.tier).progress)
    }

    @Test
    fun `a fresh trail rolls a length within the tier range`() {
        val f = Fixture()
        repeat(50) {
            TrailFlow.resetTrail(f.player, f.tier)
            TrailFlow.startedStep(f.player, f.tier, f.random)
            val length = TrailState(f.player, f.tier).length
            assertTrue(length in f.tier.stepsMin..f.tier.stepsMax, "length=$length")
        }
    }

    private class Fixture {
        val tier: TrailTierRow = TrailTiers.tier(TrailTiers.BEGINNER)
        val clue = checkNotNull(tier.clueObj).symbol()
        val box = tier.boxObj.symbol()
        val random = DefaultGameRandom(seed = 7L)
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("treasure-trails-test")
        private var result: Result<Unit>? = null
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getRandom = { random },
                    getAlignment = { TextAlignment() },
                    getNpcInteractions = { NpcInteractions(events) },
                )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 792L
                observerUUID = 792L
                slotId = 1
                assignUid()
                coords = CoordGrid(3210, 3424, 0)
                currentMapClock = 100
                processedMapClock = 100
                inv = inventory("inv.inv", 28)
                worn = inventory("inv.worn", 14)
                invMap["inv.bank"] = inventory("inv.bank", 800)
            }

        val bank: Inventory = checkNotNull(player.invMap["inv.bank"])
        private val objRegistry = ObjRegistry(ZoneUpdateMap())
        private val objRepo = ObjRepository(MapClock(100), objRegistry)
        private val noPrices = object : MarketPrices {
            override fun get(type: dev.openrune.types.ItemServerType): Int? = null
        }
        private val rewards = TrailRewards(objRepo, noPrices)
        private val npcs = NpcList()
        val npcHook =
            ClueNpcHook(
                NpcRepository(MapClock(100), NpcRegistry(npcs, CollisionFlagMap(), events), npcs)
            )

        fun access() = ProtectedAccess(player, coroutine, context)

        fun startTrail(step: String, length: Int) {
            val state = TrailState(player, tier)
            state.length = length
            state.progress = 0
            state.step = stepId(step)
        }

        fun completed(): Int = player.vars[TrailTiers.varp(tier.completedVarp)]

        fun openCasket() = with(rewards) { access().openCasket(tier) }

        fun shownRewards(): Map<Int, Int> =
            totals(checkNotNull(player.invMap[TrailRewards.REWARD_INV]).objs.filterNotNull())

        fun held(): Map<Int, Int> = totals(player.inv.objs.filterNotNull())

        fun ground(): Map<Int, Int> =
            objRegistry.findAll(player.coords).groupBy({ baseObj(it.type) }, { it.count }).mapValues {
                it.value.sum()
            }

        fun charlie(): Npc {
            val target = ClueSteps.target(player, stepId(CHARLIE_TROUT)) as ClueTarget.TalkTo
            return Npc(target.row.npc, target.row.coord)
        }

        fun run(block: suspend ProtectedAccess.() -> Unit) {
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
            repeat(50) {
                if (coroutine.isIdle) return
                if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                    val parent =
                        listOf("objectbox", "chat_left", "chat_right", "messagebox").first {
                            player.ui.containsModal("interface.$it")
                        }
                    val component =
                        if (parent == "objectbox") "component.objectbox:universe"
                        else "component.$parent:continue"
                    coroutine.resumeWith(ResumePauseButtonInput(component, -1))
                } else {
                    player.currentMapClock++
                    player.processedMapClock = player.currentMapClock
                    coroutine.advance()
                }
                result?.getOrThrow()
            }
            fail<Unit>("Interaction did not finish")
        }

        private fun stepId(step: String): Int =
            when {
                step.contains("skillchallenge") -> CluehelperClueSkillchallengeRow.getRow(step).rowId
                else -> CluehelperClueCrypticRow.getRow(step).rowId
            }

        /** The reward screen's inv shows noted rewards unnoted, so compare by the base obj. */
        private fun totals(objs: List<InvObj>): Map<Int, Int> =
            objs.groupBy({ baseObj(it.id) }, { it.count }).mapValues { it.value.sum() }

        private fun baseObj(id: Int): Int {
            val type = checkNotNull(ServerCacheManager.getItem(id))
            return if (type.isCert) type.certlink else id
        }

        private fun inventory(type: String, size: Int) =
            Inventory(checkNotNull(ServerCacheManager.getInventory(type.asRSCM())), arrayOfNulls(size))
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
        private const val HANS = "dbrow.cluehelper_cryptic_beginner_hans"
        private const val CHARLIE_TROUT = "dbrow.cluehelper_skillchallenge_beginner_0"
        private const val TROUT = "obj.trout"
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
