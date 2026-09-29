package org.rsmod.content.quest.area.lumbridge

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.InventoryServerType
import dev.openrune.util.Wearpos
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
import org.rsmod.api.game.process.player.PlayerFaceSquareProcessor
import org.rsmod.api.game.process.player.PlayerMovementProcessor
import org.rsmod.api.inv.LocUOpScript
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.PlayerTimerEvent
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.events.interact.OpEvent
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.LocInteractions
import org.rsmod.api.player.interact.LocTInteractions
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.loc.LocRegistryRegion
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.BoundValidator
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.StepFactory
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.Quest
import org.rsmod.content.quest.manager.QuestEvents
import org.rsmod.content.quest.manager.rewards
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
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.EntityFaceAngle
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.game.region.RegionListWorldEntity
import org.rsmod.game.seq.EntitySeq
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class TheRestlessGhostInteractionTest {
    @Test fun `accepting Aereck's offer starts the quest without completing it`() {
        val f = Fixture()
        f.talk("npc.father_aereck")
        f.finish(listOf(3, 1))
        assertEquals(TheRestlessGhostStage.Started, f.quest.getQuestStage(f.player))
        assertEquals(TheRestlessGhostStage.Started, f.player.vars["varp.prieststart"])
        assertEquals(0, f.player.vars["varp.qp"])
    }

    @Test fun `declining Aereck's offer leaves the quest unstarted`() {
        val f = Fixture()
        f.talk("npc.father_aereck")
        f.finish(listOf(3, 2))
        assertTrue(f.quest.isQuestNotStarted(f.player))
    }

    @Test fun `Urhney gives the amulet after the player explains Aereck's problem`() {
        val f = Fixture(TheRestlessGhostStage.Started)
        f.talk("npc.father_urhney")
        f.finish(listOf(2, 1))
        assertEquals(1, f.player.inv.count("obj.amulet_of_ghostspeak"), f.output())
        assertEquals(TheRestlessGhostStage.HasAmulet, f.quest.getQuestStage(f.player))
    }

    @Test fun `a lost amulet can be replaced without duplicating a carried one`() {
        val f = Fixture(TheRestlessGhostStage.HasAmulet)
        repeat(2) {
            f.talk("npc.father_urhney")
            f.finish(listOf(2))
            assertEquals(1, f.player.inv.count("obj.amulet_of_ghostspeak"), f.output())
        }
        assertTrue(f.output().contains("What are you talking about?"))
    }

    @Test fun `the ghost cannot explain the skull without a worn amulet`() {
        val f = Fixture(TheRestlessGhostStage.HasAmulet)
        f.player.inv[0] = InvObj("obj.amulet_of_ghostspeak", 1)
        f.talk("npc.ghostx")
        f.finish(listOf(1))
        assertEquals(TheRestlessGhostStage.HasAmulet, f.quest.getQuestStage(f.player))
        f.player.worn[Wearpos.Front.slot] = f.player.inv[0]
        f.player.inv[0] = null
        f.talk("npc.ghostx")
        f.finish(listOf(1, 1))
        assertEquals(TheRestlessGhostStage.GhostSpoken, f.quest.getQuestStage(f.player))
    }

    @Test fun `altar search retrieves the skull and wakes a skeleton that attacks the player`() {
        val f = Fixture(TheRestlessGhostStage.GhostSpoken)
        f.searchAltar()
        f.finish()
        assertEquals(1, f.player.inv.count("obj.ghostskull"))
        assertEquals(TheRestlessGhostStage.SkullFound, f.quest.getQuestStage(f.player))
        val skeleton = f.npcs.single { it.isType("npc.skull_skeleton") }
        f.timer("timer.restless_skeleton_attack")
        assertNotNull(skeleton.interaction)
        assertNotNull(skeleton.routeRequest)
    }

    @Test fun `altar search preserves unrelated ghost control bits`() {
        val f = Fixture(TheRestlessGhostStage.GhostSpoken)
        VarPlayerIntMapSetter.set(f.player, "varp.restless_ghost_control", 8)
        f.searchAltar()
        f.finish()
        assertEquals(8, f.player.vars["varp.restless_ghost_control"] and 8)
        assertEquals(1, f.player.vars["varbit.restless_ghost_altar_var"])
        assertEquals(1, f.player.vars["varbit.restless_ghost_skeleton_var"])
    }

    @Test fun `a full inventory cannot consume the altar skull or wake the skeleton`() {
        val f = Fixture(TheRestlessGhostStage.GhostSpoken)
        for (slot in f.player.inv.objs.indices) f.player.inv[slot] = InvObj("obj.bucket_empty", 1)
        f.searchAltar()
        f.finish()
        assertEquals(0, f.player.inv.count("obj.ghostskull"))
        assertEquals(0, f.player.vars["varbit.restless_ghost_altar_var"])
        assertEquals(0, f.npcs.count { it.isType("npc.skull_skeleton") })
        assertTrue(f.output().contains("enough inventory space"))
    }

    @Test fun `a lost skull can be retrieved again without waking another skeleton`() {
        val f = Fixture(TheRestlessGhostStage.GhostSpoken)
        f.searchAltar()
        f.finish()
        for (slot in f.player.inv.objs.indices) f.player.inv[slot] = null
        f.searchAltar()
        f.finish()
        assertEquals(1, f.player.inv.count("obj.ghostskull"))
        assertEquals(1, f.npcs.count { it.isType("npc.skull_skeleton") })
    }

    @Test fun `opening the coffin schedules one ghost and clears the rising tile`() {
        val f = Fixture(TheRestlessGhostStage.HasAmulet)
        f.coffin("loc.shutghostcoffin", InteractionOp.Op1)
        f.finish()
        assertNotEquals(0, f.player.restlessGhostRisingTile)
        f.timer("timer.restless_ghost_coffin_ghost")
        assertEquals(0, f.player.restlessGhostRisingTile)
        assertEquals(1, f.npcs.count { it.isType("npc.ghostx") })
        f.coffin("loc.shutghostcoffin", InteractionOp.Op1)
        f.finish()
        f.timer("timer.restless_ghost_coffin_ghost")
        assertEquals(1, f.npcs.count { it.isType("npc.ghostx") })
    }

    @Test fun `using the skull on a shut coffin asks the player to open it first`() {
        val f = Fixture(TheRestlessGhostStage.SkullFound)
        f.useSkull("loc.shutghostcoffin")
        f.finish()
        assertEquals(1, f.player.inv.count("obj.ghostskull"))
        assertEquals(0, f.player.vars["varbit.restless_ghost_coffin_var"])
        assertTrue(f.output().contains("Maybe I should open it first."))
    }

    @Test fun `returning the skull finishes the scene and grants the reward only once`() {
        val f = Fixture(TheRestlessGhostStage.SkullFound)
        f.useSkull("loc.openghostcoffin")
        f.finish()
        assertEquals(TheRestlessGhostStage.Complete, f.quest.getQuestStage(f.player))
        assertEquals(1, f.player.vars["varp.qp"])
        assertEquals(1125, f.player.statMap.getXP("stat.prayer"))
        assertEquals(0, f.player.inv.count("obj.ghostskull"))
        assertEquals(0, f.player.restlessGhostReturnTile)
        assertEquals(ReleaseScene.ReturnTile, f.player.coords)
        assertEquals(0, f.player.vars["varbit.cutscene_status"])
        assertTrue(f.player.ui.containsModal("interface.questscroll"))
        f.player.clearPendingAction(f.events)
        f.quest.completeQuest(f.access())
        assertEquals(1, f.player.vars["varp.qp"])
        assertEquals(1125, f.player.statMap.getXP("stat.prayer"))
    }

    @Test fun `completed quest coffin does not summon another ghost`() {
        val f = Fixture(TheRestlessGhostStage.Complete)
        f.coffin("loc.shutghostcoffin", InteractionOp.Op1)
        f.finish()
        assertEquals(0, f.player.restlessGhostRisingTile)
        f.timer("timer.restless_ghost_coffin_ghost")
        assertTrue(f.npcs.none { it.isType("npc.ghostx") })
    }

    @Test fun `login recovery returns to the graveyard and clears its saved exit`() {
        val f = Fixture()
        f.player.coords = CoordGrid(2000, 8000)
        f.player.restlessGhostReturnTile = ReleaseScene.ReturnTile.packed
        restoreRestlessGhostLogin(f.player)
        assertEquals(ReleaseScene.ReturnTile, f.player.coords)
        assertEquals(0, f.player.restlessGhostReturnTile)
        f.player.coords = CoordGrid(3222, 3218)
        restoreRestlessGhostLogin(f.player)
        assertEquals(CoordGrid(3222, 3218), f.player.coords)
    }

    @Test fun `scene coordinates remain inside the copied graveyard`() {
        assertRestlessGhostSceneCoords()
    }

    private class Fixture(stage: Int = TheRestlessGhostStage.NotStarted) {
        val quest = Quest(
            id = 120, key = "quest_restlessghost", rowID = 0, displayName = "The Restless Ghost",
            mapElement = null, startCoord = null, maxSteps = TheRestlessGhostStage.Complete,
            questPoints = 1, questVarp = "varp.prieststart", questVarbit = "varbit.restless_ghost_progress",
            rewards = rewards { xp("stat.prayer", 1125.0); extra("A Ghostspeak Amulet") },
            itemDisplay = ItemRewardDisplay("obj.ghostskull"),
        )
        private val client = RecordingClient()

        @OptIn(InternalApi::class)
        val player = Player().apply {
            this.client = this@Fixture.client
            uuid = 456L
            slotId = 1
            assignUid()
            coords = ReleaseScene.ReturnTile
            currentMapClock = 100
            processedMapClock = 100
            pendingSequence = EntitySeq.NULL
            pendingFaceAngle = EntityFaceAngle.NULL
            inv = Inventory(InventoryServerType(size = 28, flags = 0), arrayOfNulls(28))
            worn = Inventory(InventoryServerType(size = 14, flags = 0), arrayOfNulls(14))
        }
        val events = EventBus()
        val npcs = NpcList()
        private val collision = CollisionFlagMap()
        private lateinit var regions: RegionRegistry
        private lateinit var npcRepo: NpcRepository
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() },
            getCollision = { collision }, getNpcList = { npcs },
            getTeleportValidator = { PlayerTeleportValidator(emptySet()) },
            getAreaChecker = { AreaChecker(regions, AreaIndex()) },
        )
        private val movement = PlayerMovementProcessor(collision, RouteFactory(collision), StepFactory(collision), events)
        private val coroutine = GameCoroutine("restless-ghost-test")
        private var result: Result<Unit>? = null

        init {
            val clock = MapClock(100)
            val updates = ZoneUpdateMap()
            val storage = LocZoneStorage()
            val normal = LocRegistryNormal(updates, collision, storage)
            val npcRegistry = NpcRegistry(npcs, collision, events)
            regions = RegionRegistry(RegionListSmall(), RegionListLarge(), RegionListWorldEntity(),
                normal, collision, storage, npcRegistry,
                ControllerRegistry(clock, ControllerList()), ZonePlayerActivityBitSet())
            val locs = LocRepository(clock, LocRegistry(storage, normal,
                LocRegistryRegion(updates, collision, storage, regions)), regions)
            npcRepo = NpcRepository(clock, npcRegistry, npcs)
            val scene = TheRestlessGhostCutscene(quest, RegionRepository(regions), npcRepo, locs, WorldRepository(updates))
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(scene) { scripts.register() }
            with(TheRestlessGhostScenery(quest, locs, npcRepo, WorldRepository(updates), scene,
                AiPlayerInteractions(events, PlayerList()))) { scripts.register() }
            with(TheRestlessGhostDialogue(quest)) { scripts.register() }
            with(QuestEvents()) { scripts.startup() }
            val locU = LocUInteractions::class.java.getDeclaredConstructor(EventBus::class.java)
                .apply { isAccessible = true }.newInstance(events)
            val bridge = LocUOpScript::class.java.getDeclaredConstructor(LocUInteractions::class.java)
                .apply { isAccessible = true }.newInstance(locU)
            with(bridge) { scripts.startup() }
            quest.setQuestStage(access(), stage)
            for (level in 0..3) for (x in 3240..3263 step 8) for (z in 3176..3199 step 8) {
                collision.allocateIfAbsent(x, z, level)
            }
        }

        fun access() = ProtectedAccess(player, coroutine, context)

        fun talk(symbol: String) {
            val npc = Npc(symbol, player.coords.translateX(1))
            npcRepo.add(npc, 100)
            dispatch(NpcEvents.Op1(npc))
        }

        fun timer(symbol: String) {
            events.publish(PlayerTimerEvent.Soft(player, symbol.asRSCM()))
        }

        fun searchAltar() {
            player.coords = CoordGrid(3120, 9566)
            dispatch(checkNotNull(LocInteractions(BoundValidator(collision), events)
                .opTrigger(player, loc("loc.restless_ghost_altar", player.coords), InteractionOp.Op1)))
        }

        fun coffin(symbol: String, op: InteractionOp) {
            dispatch(checkNotNull(LocInteractions(BoundValidator(collision), events)
                .opTrigger(player, loc(symbol, CoordGrid(3248, 3190)), op)))
        }

        fun useSkull(symbol: String) {
            player.inv[0] = InvObj("obj.ghostskull", 1)
            val event = LocTInteractions(events).opTrigger(player, loc(symbol, CoordGrid(3248, 3190)),
                checkNotNull(ServerCacheManager.getItem("obj.ghostskull".asRSCM())),
                ServerCacheManager.fromComponent("component.inventory:items"), 0)
            dispatch(checkNotNull(event))
        }

        fun finish(options: List<Int> = emptyList()) {
            val selections = options.iterator()
            repeat(400) {
                if (coroutine.isIdle) return
                if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                    val parent = listOf("chat_left", "chat_right", "messagebox", "chatmenu", "objectbox")
                        .firstOrNull { player.ui.containsModal("interface.$it") }
                        ?: error("Unknown dialogue: ${output()}")
                    val input = when (parent) {
                        "chatmenu" -> ResumePauseButtonInput("component.chatmenu:options", if (selections.hasNext()) selections.next() else 1)
                        "objectbox" -> ResumePauseButtonInput("component.objectbox:universe", -1)
                        else -> ResumePauseButtonInput("component.$parent:continue", -1)
                    }
                    coroutine.resumeWith(input)
                } else {
                    player.currentMapClock++
                    player.processedMapClock = player.currentMapClock
                    player.pendingSequence = EntitySeq.NULL
                    player.pendingFaceAngle = EntityFaceAngle.NULL
                    movement.process(player)
                    coroutine.advance()
                    PlayerFaceSquareProcessor().process(player)
                }
                result?.getOrThrow()
            }
            fail<Unit>("Interaction did not finish: ${output()}")
        }

        private fun dispatch(event: OpEvent) {
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val block: suspend () -> Unit = { assertTrue(events.publish(access(), event)) }
            block.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { this@Fixture.result = result }
            })
            result?.getOrThrow()
            PlayerFaceSquareProcessor().process(player)
        }

        fun output() = client.messages.joinToString("\n")

        private fun loc(symbol: String, coords: CoordGrid): BoundLocInfo {
            val type = checkNotNull(ServerCacheManager.getObject(symbol.asRSCM()))
            return BoundLocInfo(LocInfo(2, coords, LocEntity(type.id, 10, 2)), type)
        }
    }

    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()
        override fun write(message: Any) { messages += message }
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic @BeforeAll fun cache() {
            ServerCacheManager.init(240).close()
            for ((owner, name) in listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic @AfterAll fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
