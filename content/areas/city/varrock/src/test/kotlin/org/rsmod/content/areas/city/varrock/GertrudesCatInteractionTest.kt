package org.rsmod.content.areas.city.varrock

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
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
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.events.interact.NpcUDefaultEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.content.areas.city.varrock.npcs.Gertrude
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.content.other.pets.cats.CatCare
import org.rsmod.content.other.pets.cats.CatStage
import org.rsmod.content.other.pets.cats.Cats
import org.rsmod.content.quest.area.varrock.gertrudescat.DoogleSardines
import org.rsmod.content.quest.area.varrock.gertrudescat.GertrudesCatQuest
import org.rsmod.content.quest.area.varrock.gertrudescat.LumberYardCrates
import org.rsmod.content.quest.area.varrock.gertrudescat.fluffsKittenCrate
import org.rsmod.content.quest.area.varrock.gertrudescat.metFluffs
import org.rsmod.content.quest.area.varrock.gertrudescat.npcs.Fluffs
import org.rsmod.content.quest.area.varrock.gertrudescat.npcs.GertrudesKids
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
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
class GertrudesCatInteractionTest {
    @Test fun `accepting Gertrude's request starts the quest in the native varp`() {
        val f = Fixture()
        f.talkToGertrude()
        f.finish(listOf(1))
        assertEquals(1, f.stage())
        assertEquals(1, f.player.vars["varp.fluffs"])
    }

    @Test fun `declining leaves the quest unstarted`() {
        val f = Fixture()
        f.talkToGertrude()
        f.finish(listOf(2))
        assertEquals(0, f.stage())
        assertEquals(0, f.player.vars["varp.fluffs"])
    }

    @Test fun `the boys take the fee and advance the quest in one step`() {
        val f = Fixture(1)
        f.player.inv[0] = InvObj(Coins, 150)
        f.talkToKid("npc.shilop")
        f.stepUntil { f.player.inv.count(Coins) != 150 }
        assertEquals(2, f.stage())
        assertEquals(50, f.player.inv.count(Coins))
        f.finish(listOf(2, 2))
        assertEquals(2, f.stage())
        assertEquals(50, f.player.inv.count(Coins))
    }

    @Test fun `short of coins the quest does not advance`() {
        val f = Fixture(1)
        f.player.inv[0] = InvObj(Coins, 99)
        f.talkToKid("npc.wilough")
        f.finish(listOf(2, 2))
        assertEquals(1, f.stage())
        assertEquals(99, f.player.inv.count(Coins))
    }

    @Test fun `refusing to pay or threatening the boy costs nothing`() {
        for (options in listOf(listOf(2, 1), listOf(1), listOf(3))) {
            val f = Fixture(1)
            f.player.inv[0] = InvObj(Coins, 100)
            f.talkToKid("npc.shilop")
            f.finish(options)
            assertEquals(1, f.stage(), "options $options")
            assertEquals(100, f.player.inv.count(Coins), "options $options")
        }
    }

    @Test fun `the boys only talk tough after the fee has been paid and Fluffs fed`() {
        val f = Fixture(3)
        f.talkToKid("npc.wilough")
        f.finish()
        assertTrue(f.output().contains("You think you're tough, do you?"), f.output())
        assertEquals(3, f.stage())
    }

    @Test fun `milk then sardine then the kitten each need the right stage`() {
        val f = Fixture(2)
        f.give(SeasonedSardine)
        f.useOnFluffs(SeasonedSardine)
        assertEquals(2, f.stage())
        assertEquals(1, f.player.inv.count(SeasonedSardine))

        f.give(BucketOfMilk, slot = 1)
        f.useOnFluffs(BucketOfMilk)
        assertEquals(3, f.stage())
        assertEquals(0, f.player.inv.count(BucketOfMilk))
        assertEquals(1, f.player.inv.count(BucketEmpty))
        assertTrue(f.player.metFluffs)

        f.useOnFluffs(SeasonedSardine)
        assertEquals(4, f.stage())
        assertEquals(0, f.player.inv.count(SeasonedSardine))
        assertTrue(f.player.fluffsKittenCrate in LumberYardCrates.CRATES.indices)

        f.give(FluffsKitten, slot = 2)
        f.useOnFluffs(FluffsKitten)
        assertEquals(5, f.stage())
        assertEquals(0, f.player.inv.count(FluffsKitten))
    }

    @Test fun `milk is refused before the quest and the bucket is kept`() {
        val f = Fixture(0)
        f.give(BucketOfMilk)
        f.useOnFluffs(BucketOfMilk)
        assertEquals(0, f.stage())
        assertEquals(1, f.player.inv.count(BucketOfMilk))
    }

    @Test fun `the sardine is refused before the milk`() {
        val f = Fixture(2)
        f.give(SeasonedSardine)
        f.useOnFluffs(SeasonedSardine)
        assertEquals(2, f.stage())
        assertEquals(1, f.player.inv.count(SeasonedSardine))
    }

    @Test fun `the kitten is refused before the sardine`() {
        val f = Fixture(3)
        f.give(FluffsKitten)
        f.useOnFluffs(FluffsKitten)
        assertEquals(3, f.stage())
        assertEquals(1, f.player.inv.count(FluffsKitten))
    }

    @Test fun `doogle leaves season a raw sardine`() {
        val f = Fixture(0)
        f.player.inv[1] = InvObj(DoogleLeaves, 1)
        f.player.inv[0] = InvObj(RawSardine, 1)
        f.useItemOnItem(RawSardine, DoogleLeaves)
        assertEquals(0, f.player.inv.count(RawSardine))
        assertEquals(0, f.player.inv.count(DoogleLeaves))
        assertEquals(1, f.player.inv.count(SeasonedSardine))
    }

    @Test fun `only the chosen crate holds the kitten and it can be fetched again`() {
        val f = Fixture(4)
        f.player.fluffsKittenCrate = 3
        for (index in LumberYardCrates.CRATES.indices) {
            f.searchCrate(index)
            val found = f.player.inv.count(FluffsKitten)
            assertEquals(if (index == 3) 1 else 0, found, "crate $index")
            if (index == 3) {
                f.player.inv[f.slotOf(FluffsKitten)] = null
            }
        }
        f.searchCrate(3)
        assertEquals(1, f.player.inv.count(FluffsKitten))
        f.searchCrate(3)
        assertEquals(1, f.player.inv.count(FluffsKitten))
    }

    @Test fun `crates are empty outside the sardine stage`() {
        for (stage in listOf(0, 1, 3, 5)) {
            val f = Fixture(stage)
            f.player.fluffsKittenCrate = 0
            f.searchCrate(0)
            assertEquals(0, f.player.inv.count(FluffsKitten), "stage $stage")
        }
    }

    @Test fun `a full pack does not eat the kitten`() {
        val f = Fixture(4)
        f.player.fluffsKittenCrate = 0
        f.fillInventory()
        f.searchCrate(0)
        assertEquals(0, f.player.inv.count(FluffsKitten))
        f.player.inv[27] = null
        f.searchCrate(0)
        assertEquals(1, f.player.inv.count(FluffsKitten))
    }

    @Test fun `Gertrude advises on sardines once Fluffs is found`() {
        val f = Fixture(3)
        f.talkToGertrude()
        f.finish()
        assertTrue(f.output().contains("doogle sardines"), f.output())
        assertEquals(3, f.stage())
    }

    @Test fun `the journal follows the stage`() {
        val f = Fixture(2)
        f.player.metFluffs = true
        val log = f.journal()
        assertTrue(log.contains("bucket of milk"), log)
        VarPlayerIntMapSetter.set(f.player, "varbit.gertrudes_cat_progress", 4)
        assertTrue(f.journal().contains("kittens"), f.journal())
    }

    @Test fun `a full pack delays the reward and nothing is lost`() {
        val f = Fixture(5)
        f.fillInventory()
        f.talkToGertrude()
        f.finish()
        assertEquals(5, f.stage())
        assertEquals(0, f.player.vars["varp.qp"])
        assertEquals(0, f.player.statMap.getXP("stat.cooking"))
        assertEquals(0, f.player.inv.count(Cake))
    }

    @Test fun `finishing grants rewards exactly once and Gertrude still offers kittens`() {
        val f = Fixture(5)
        f.talkToGertrude()
        f.finish()
        assertEquals(6, f.stage())
        assertEquals(6, f.player.vars["varp.fluffs"])
        assertEquals(1, f.player.vars["varp.qp"])
        assertEquals(1525, f.player.statMap.getXP("stat.cooking"))
        assertEquals(1, f.player.inv.count(Cake))
        assertEquals(1, f.player.inv.count(Stew))
        assertEquals(1, f.kittens())

        f.talkToGertrude()
        f.finish()
        assertEquals(1525, f.player.statMap.getXP("stat.cooking"))
        assertEquals(1, f.player.inv.count(Cake))
        assertEquals(1, f.kittens())

        f.removeKittens()
        f.player.inv[27] = InvObj(Coins, 100)
        f.talkToGertrude()
        f.finish(listOf(1))
        assertEquals(1, f.kittens())
        assertEquals(0, f.player.inv.count(Coins))
        assertEquals(1525, f.player.statMap.getXP("stat.cooking"))
        assertEquals(1, f.player.vars["varp.qp"])
    }

    @Test fun `the Kitten option sells one kitten after the quest only`() {
        val f = Fixture(6)
        f.player.inv[0] = InvObj(Coins, 100)
        f.quickBuyKitten()
        f.finish()
        assertEquals(1, f.kittens())
        assertEquals(0, f.player.inv.count(Coins))
        f.player.inv[1] = InvObj(Coins, 100)
        f.quickBuyKitten()
        f.finish()
        assertEquals(1, f.kittens())
        assertEquals(100, f.player.inv.count(Coins))

        val early = Fixture(3)
        early.player.inv[0] = InvObj(Coins, 100)
        early.quickBuyKitten()
        early.finish()
        assertEquals(0, early.kittens())
        assertEquals(100, early.player.inv.count(Coins))
    }

    @Test fun `owning a kitten blocks buying another one after the quest`() {
        val f = Fixture(6)
        f.player.inv[0] = InvObj(Cats.objsOf(CatStage.Kitten).first(), 1)
        f.player.inv[1] = InvObj(Coins, 100)
        f.talkToGertrude()
        f.finish(listOf(1))
        assertEquals(1, f.kittens())
        assertEquals(100, f.player.inv.count(Coins))
    }

    @Test fun `the full quest plays through from start to finish`() {
        val f = Fixture(0)
        f.player.inv[0] = InvObj(Coins, 100)
        f.player.inv[1] = InvObj(BucketOfMilk, 1)
        f.player.inv[2] = InvObj(RawSardine, 1)
        f.player.inv[3] = InvObj(DoogleLeaves, 1)

        f.talkToGertrude()
        f.finish(listOf(1))
        assertEquals(1, f.stage())

        f.talkToKid("npc.wilough")
        f.finish(listOf(2, 2))
        assertEquals(2, f.stage())
        assertEquals(0, f.player.inv.count(Coins))

        f.useOnFluffs(BucketOfMilk)
        assertEquals(3, f.stage())

        f.talkToGertrude()
        f.finish()

        f.useItemOnItem(RawSardine, DoogleLeaves)
        assertEquals(1, f.player.inv.count(SeasonedSardine))
        f.useOnFluffs(SeasonedSardine)
        assertEquals(4, f.stage())

        for (index in LumberYardCrates.CRATES.indices) {
            f.searchCrate(index)
        }
        assertEquals(1, f.player.inv.count(FluffsKitten))

        f.useOnFluffs(FluffsKitten)
        assertEquals(5, f.stage())

        f.talkToGertrude()
        f.finish()
        assertEquals(6, f.stage())
        assertEquals(1, f.player.vars["varp.qp"])
        assertEquals(1525, f.player.statMap.getXP("stat.cooking"))
        assertEquals(1, f.player.inv.count(Cake))
        assertEquals(1, f.player.inv.count(Stew))
        assertEquals(1, f.kittens())
    }

    private class Fixture(stage: Int = 0) {
        val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("gertrudes-cat-test")
        private var result: Result<Unit>? = null
        private val random = DefaultGameRandom(7L)
        private val collision = CollisionFlagMap()
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getAlignment = { TextAlignment() },
                    getNpcInteractions = { NpcInteractions(events) },
                    getRandom = { random },
                    getCollision = { collision },
                )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 793L
                slotId = 1
                assignUid()
                coords = CoordGrid(3306, 3511, 1)
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

        val quest = GertrudesCatQuest()
        private val fluffs = Npc("npc.gertrudescat", CoordGrid(3306, 3512, 1))

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            val clock = MapClock(100)
            val updates = ZoneUpdateMap()
            val storage = LocZoneStorage()
            val normal = LocRegistryNormal(updates, collision, storage)
            val npcs = NpcList()
            val npcRegistry = NpcRegistry(npcs, collision, events)
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
            val npcRepo = NpcRepository(clock, npcRegistry, npcs)
            val worldRepo = WorldRepository(updates)
            val care = CatCare(PetFollowers(npcRepo, npcs, clock, collision))
            with(quest) { scripts.startup() }
            with(GertrudesKids(quest)) { scripts.startup() }
            with(Fluffs(quest, npcRepo, random)) { scripts.startup() }
            with(LumberYardCrates(quest, random, worldRepo)) { scripts.startup() }
            with(DoogleSardines()) { scripts.startup() }
            with(Gertrude(care, quest)) { scripts.startup() }
            npcRepo.add(fluffs, Int.MAX_VALUE)
            VarPlayerIntMapSetter.set(player, "varbit.gertrudes_cat_progress", stage)
        }

        fun stage() = quest.quest.getQuestStage(player)

        fun access() = ProtectedAccess(player, coroutine, context)

        fun journal(): String = quest.questLog(access())

        fun give(obj: String, slot: Int = 0) {
            player.inv[slot] = InvObj(obj, 1)
        }

        fun fillInventory() {
            for (slot in 0 until 28) player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        }

        fun slotOf(obj: String): Int =
            (0 until 28).firstOrNull { player.inv[it]?.id == obj.asRSCM() } ?: 0

        fun kittens(): Int = Cats.objsOf(CatStage.Kitten).sumOf { player.inv.count(it) }

        fun removeKittens() {
            for (slot in 0 until 28) {
                val obj = player.inv[slot] ?: continue
                if (Cats.forObj(obj.id)?.stage === CatStage.Kitten) player.inv[slot] = null
            }
        }

        fun talkToGertrude() = start {
            assertTrue(events.publish(this, NpcEvents.Op1(Npc("npc.gertrude", coords.translateZ(1)))))
        }

        fun quickBuyKitten() = start {
            assertTrue(events.publish(this, NpcEvents.Op3(Npc("npc.gertrude", coords.translateZ(1)))))
        }

        fun talkToKid(kid: String) = start {
            assertTrue(events.publish(this, NpcEvents.Op1(Npc(kid, coords.translateZ(1)))))
        }

        fun useOnFluffs(obj: String) {
            val slot = slotOf(obj)
            val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
            val npcType = checkNotNull(ServerCacheManager.getNpc("npc.gertrudescat".asRSCM()))
            start {
                assertTrue(events.publish(this, NpcUDefaultEvents.OpType(fluffs, slot, type, npcType)))
            }
            finish()
        }

        fun useItemOnItem(first: String, second: String) {
            val firstSlot = slotOf(first)
            val secondSlot = slotOf(second)
            val firstType = checkNotNull(ServerCacheManager.getItem(first.asRSCM()))
            val secondType = checkNotNull(ServerCacheManager.getItem(second.asRSCM()))
            start {
                val event = HeldUEvents.Type(firstType, firstSlot, secondType, secondSlot)
                assertTrue(events.publish(this, event))
            }
            finish()
        }

        fun searchCrate(index: Int) {
            val crate = Npc("npc.kittens_mew", LumberYardCrates.CRATES[index])
            start { assertTrue(events.publish(this, NpcEvents.Op1(crate))) }
            finish()
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

        fun finish(options: List<Int> = emptyList()) {
            val selections = options.iterator()
            repeat(300) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${output()}")
        }

        fun stepUntil(condition: () -> Boolean) {
            val selections = listOf(2, 2).iterator()
            repeat(300) {
                if (condition()) return
                advance(selections)
            }
            fail<Unit>("Condition never held: ${output()}")
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
        private const val Coins = "obj.coins"
        private const val BucketOfMilk = "obj.bucket_milk"
        private const val BucketEmpty = "obj.bucket_empty"
        private const val RawSardine = "obj.raw_sardine"
        private const val DoogleLeaves = "obj.doogleleaves"
        private const val SeasonedSardine = "obj.seasoned_sardine"
        private const val FluffsKitten = "obj.gertrudekittens"
        private const val Cake = "obj.chocolate_cake"
        private const val Stew = "obj.stew"
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
