package org.rsmod.content.areas.misc.ver_sinhaza

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
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
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.content.other.pets.dogs.DogCare
import org.rsmod.content.other.pets.dogs.Dogs
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
class MysteriousStrangerInteractionTest {
    @Test fun `a grown dog in the pack is exchanged for 100 blood runes`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(DOG.obj, 1)
        f.talkToStranger()
        f.finish(ACCEPT)
        assertEquals(0, f.player.inv.count(DOG.obj))
        assertEquals(100, f.player.inv.count(BLOOD_RUNE))
        assertEquals(0, f.groundCount(BLOOD_RUNE))
    }

    @Test fun `a following dog with a full pack drops the runes instead of losing them`() {
        val f = Fixture()
        f.fillInventory()
        f.followers.spawn(f.player, DOG.form)
        assertSame(DOG, f.care.following(f.player))
        f.talkToStranger()
        f.finish(ACCEPT)
        assertNull(f.care.following(f.player))
        assertNull(f.followers.follower(f.player))
        assertEquals(0, f.player.inv.count(BLOOD_RUNE))
        assertEquals(100, f.groundCount(BLOOD_RUNE))
    }

    @Test fun `a following dog with free space puts the runes in the pack`() {
        val f = Fixture()
        f.followers.spawn(f.player, DOG.form)
        f.talkToStranger()
        f.finish(ACCEPT)
        assertNull(f.care.following(f.player))
        assertEquals(100, f.player.inv.count(BLOOD_RUNE))
        assertEquals(0, f.groundCount(BLOOD_RUNE))
    }

    @Test fun `declining keeps the dog`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(DOG.obj, 1)
        f.talkToStranger()
        f.finish(DECLINE)
        assertEquals(1, f.player.inv.count(DOG.obj))

        f.followers.spawn(f.player, DOG.form)
        f.player.inv[0] = null
        f.talkToStranger()
        f.finish(DECLINE)
        assertSame(DOG, f.care.following(f.player))
        assertNotNull(f.followers.follower(f.player))
        assertEquals(0, f.player.inv.count(BLOOD_RUNE))
        assertEquals(0, f.groundCount(BLOOD_RUNE))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("mysterious-stranger-test")
        private var result: Result<Unit>? = null
        private val clock = MapClock(100)
        private val collision = CollisionFlagMap()
        private val objRegistry = ObjRegistry(ZoneUpdateMap())
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
        val care: DogCare

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
            care = DogCare(followers)
            val objRepo = ObjRepository(clock, objRegistry)
            with(MysteriousStranger(care, followers, objRepo)) { scripts.startup() }
        }

        fun fillInventory() {
            for (slot in 0 until 28) player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        }

        fun groundCount(obj: String): Int =
            objRegistry.findAll(player.coords).filter { it.type == obj.asRSCM() }.sumOf { it.count }

        fun talkToStranger() {
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val stranger = Npc(STRANGER, player.coords.translateZ(1))
            val body: suspend () -> Unit = {
                val access = ProtectedAccess(player, coroutine, context)
                assertTrue(events.publish(access, NpcEvents.Op1(stranger)))
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
        private const val STRANGER = "npc.tob_dog_stranger"
        private const val BLOOD_RUNE = "obj.bloodrune"
        private val ACCEPT = listOf(2)
        private val DECLINE = listOf(1)
        private val DOG by lazy { Dogs.all.first { !it.puppy } }
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
