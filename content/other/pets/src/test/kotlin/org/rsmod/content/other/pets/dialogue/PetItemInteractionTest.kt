package org.rsmod.content.other.pets.dialogue

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.events.interact.NpcUDefaultEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.content.other.pets.PetForm
import org.rsmod.content.other.pets.Pets
import org.rsmod.content.other.pets.petOpIndex
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.events.SuspendEvent
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
class PetItemInteractionTest {
    @Test fun `accepting the unlock eats 3000 flakes and turns the heron blue`() {
        val f = Fixture()
        f.spawn(HERON)
        f.player.inv[0] = InvObj(FLAKES, 3005)
        f.useOnPet(FLAKES)
        f.finish(ACCEPT)
        assertEquals(5, f.player.inv.count(FLAKES))
        assertEquals(GREAT_BLUE.obj, f.followers.followerForm(f.player)?.obj)
    }

    @Test fun `declining the unlock keeps the flakes and the heron white`() {
        val f = Fixture()
        f.spawn(HERON)
        f.player.inv[0] = InvObj(FLAKES, 3000)
        f.useOnPet(FLAKES)
        f.finish(DECLINE)
        assertEquals(3000, f.player.inv.count(FLAKES))
        assertEquals(HERON.obj, f.followers.followerForm(f.player)?.obj)
    }

    @Test fun `too few flakes are kept`() {
        val f = Fixture()
        f.spawn(HERON)
        f.player.inv[0] = InvObj(FLAKES, 2999)
        f.useOnPet(FLAKES)
        f.finish(ACCEPT)
        assertEquals(2999, f.player.inv.count(FLAKES))
        assertEquals(HERON.obj, f.followers.followerForm(f.player)?.obj)
    }

    @Test fun `metamorphosis on the great blue heron eats one raw fish`() {
        val f = Fixture()
        f.spawn(GREAT_BLUE)
        f.player.inv[0] = InvObj(RAW_SHRIMPS, 2)
        f.metamorphose()
        f.finish(ACCEPT)
        assertEquals(1, f.player.inv.count(RAW_SHRIMPS))
        assertEquals(HERON.obj, f.followers.followerForm(f.player)?.obj)
    }

    @Test fun `soup eats the hot water and the soup item`() {
        val f = Fixture()
        f.spawn(SOUP)
        f.player.inv[0] = InvObj(HOT_WATER, 1)
        f.player.inv[1] = InvObj(RAW_SHRIMPS, 1)
        f.player.inv[2] = InvObj(PLAIN_WATER, 1)
        f.useOnPet(RAW_SHRIMPS)
        f.finish(ACCEPT)
        assertEquals(0, f.player.inv.count(HOT_WATER))
        assertEquals(0, f.player.inv.count(RAW_SHRIMPS))
        assertEquals(1, f.player.inv.count(PLAIN_WATER))
    }

    @Test fun `a plain bowl of water is not hot water`() {
        val f = Fixture()
        f.spawn(SOUP)
        f.player.inv[0] = InvObj(PLAIN_WATER, 1)
        f.player.inv[1] = InvObj(RAW_SHRIMPS, 1)
        f.useOnPet(RAW_SHRIMPS)
        f.finish(ACCEPT)
        assertEquals(1, f.player.inv.count(PLAIN_WATER))
        assertEquals(1, f.player.inv.count(RAW_SHRIMPS))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("pet-item-test")
        private var result: Result<Unit>? = null
        private val clock = MapClock(100)
        private val collision = CollisionFlagMap()
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getAlignment = { TextAlignment() },
                    getNpcInteractions = { NpcInteractions(events) },
                    getCollision = { collision },
                )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 795L
                observerUUID = 795L
                slotId = 1
                assignUid()
                coords = CoordGrid(3650, 3215, 0)
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
                invMap["inv.inv"] = inv
                invMap["inv.worn"] = worn
            }

        val followers: PetFollowers

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            val updates = ZoneUpdateMap()
            val storage = LocZoneStorage()
            val npcs = NpcList()
            val npcRegistry = NpcRegistry(npcs, collision, events)
            RegionRegistry(
                RegionListSmall(),
                RegionListLarge(),
                RegionListWorldEntity(),
                LocRegistryNormal(updates, collision, storage),
                collision,
                storage,
                npcRegistry,
                ControllerRegistry(clock, ControllerList()),
                ZonePlayerActivityBitSet(),
            )
            val npcRepo = NpcRepository(clock, npcRegistry, npcs)
            followers = PetFollowers(npcRepo, npcs, clock, collision)
            with(HeronDialogue(followers)) { scripts.startup() }
            with(SoupDialogue()) { scripts.startup() }
        }

        fun spawn(form: PetForm) = followers.spawn(player, form)

        fun useOnPet(obj: String) = interact { pet ->
            val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
            NpcUDefaultEvents.OpType(pet, 0, type, pet.visType)
        }

        fun metamorphose() = interact { pet ->
            when (petOpIndex(pet.visType.internalName, "Metamorphosis")) {
                1 -> NpcEvents.Op1(pet)
                2 -> NpcEvents.Op2(pet)
                3 -> NpcEvents.Op3(pet)
                4 -> NpcEvents.Op4(pet)
                else -> NpcEvents.Op5(pet)
            }
        }

        private fun interact(event: (Npc) -> SuspendEvent<ProtectedAccess>) {
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val pet = checkNotNull(followers.follower(player))
            val body: suspend () -> Unit = {
                val access = ProtectedAccess(player, coroutine, context)
                assertTrue(events.publish(access, event(pet)))
            }
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

        fun finish(options: List<Int>) {
            val selections = options.iterator()
            repeat(100) {
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
        private const val FLAKES = "obj.spirit_flakes"
        private const val RAW_SHRIMPS = "obj.raw_shrimp"
        private const val HOT_WATER = "obj.bowl_hot_water"
        private const val PLAIN_WATER = "obj.bowl_water"
        private val SOUP by lazy { checkNotNull(Pets.forNpc("npc.skillpet_sailing".asRSCM())).second }
        private val ACCEPT = listOf(1)
        private val DECLINE = listOf(2)
        private val HERON by lazy { checkNotNull(Pets.forObj("obj.skillpetfish")).second }
        private val GREAT_BLUE by lazy {
            checkNotNull(Pets.forObj("obj.skillpetfish_tempoross")).second
        }
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
