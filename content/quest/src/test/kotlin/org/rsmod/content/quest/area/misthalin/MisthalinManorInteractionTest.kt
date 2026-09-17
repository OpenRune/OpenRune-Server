package org.rsmod.content.quest.area.misthalin

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.InventoryServerType
import dev.openrune.util.Wearpos
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
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
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.HeldEquipEvents
import org.rsmod.api.player.events.interact.OpEvent
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.LocInteractions
import org.rsmod.api.player.interact.LocTInteractions
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.loc.LocRegistryRegion
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.BoundValidator
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.StepFactory
import org.rsmod.content.quest.manager.Quest
import org.rsmod.content.quest.manager.rewards
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.area.AreaIndex
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.ControllerList
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
import org.rsmod.game.map.Direction
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.game.seq.EntitySeq
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import sun.misc.Unsafe

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class MisthalinManorInteractionTest {
    @Test fun `searching full rainwater barrel explains the water without consuming the bucket`() {
        val f = Fixture(MisthalinStage.SidDead)
        f.player.inv[1] = InvObj("obj.bucket_empty", 1)
        f.searchBarrel()
        assertTrue(f.output().replace("<br>", " ")
            .contains("The barrel is full of rainwater, I can't see what's in there."), f.output())
        assertFalse(f.output().contains("Nothing interesting happens."))
        assertTrue(f.player.ui.containsModal("interface.chat_right"))
        f.continueDialogue("component.chat_right:continue")
        assertEquals(MisthalinStage.SidDead, f.quest.getQuestStage(f.player))
        assertEquals(1, f.player.inv.count("obj.bucket_empty"))
        assertEquals(0, f.player.inv.count("obj.mistmyst_frontdoor_key"))
    }

    @Test fun `drained barrel search still gives the manor key`() {
        val f = Fixture(MisthalinStage.BucketFilled)
        f.searchBarrel()
        assertTrue(f.output().contains("You find a key at the bottom of the barrel."))
        assertEquals(1, f.player.inv.count("obj.mistmyst_frontdoor_key"))
        assertEquals(MisthalinStage.BucketFilled, f.quest.getQuestStage(f.player))
    }

    @Test fun `sapphire exit removes every carried murder weapon and refreshes equipped weapon`() {
        for (equipped in listOf(false, true)) {
            val f = Fixture(MisthalinStage.MandyWaiting)
            f.enterBlueRoom()
            f.player.inv[3] = InvObj(KillerKnife, 1)
            f.player.inv[16] = InvObj(KillerKnife, 1)
            if (equipped) f.player.worn[Wearpos.RightHand.slot] = InvObj(KillerKnife, 1)
            f.player.inv.clearModifiedSlots()
            f.player.worn.clearModifiedSlots()
            f.clearAppearanceRebuild()
            f.sapphireDoor(f.player.coords.translateX(1))
            assertEquals(CoordGrid(1628, 4829), f.player.coords)
            assertEquals(0, f.player.inv.count(KillerKnife))
            assertEquals(0, f.player.worn.count(KillerKnife))
            assertEquals(1, f.player.inv.count("obj.knife"))
            assertEquals(setOf(3, 16), f.player.inv.modifiedSlots.stream().toArray().toSet())
            assertEquals(equipped, f.player.worn.modifiedSlots[Wearpos.RightHand.slot])
            assertEquals(if (equipped) 1 else 0, f.wornChanges)
            assertEquals(if (equipped) 1 else 0, f.unequips)
            assertTrue(f.appearanceNeedsRebuild())
            assertEquals(1, f.output().split("You decide it's wise to leave the murder weapon behind.").size - 1)
            assertEquals(MisthalinStage.MandyWaiting, f.quest.getQuestStage(f.player))
            f.enterBlueRoom()
        }
    }

    @Test fun `abandoning finale removes inventory knife and rewinds room for another attempt`() {
        val f = Fixture(MisthalinStage.MandyWaiting)
        f.enterBlueRoom()
        VarPlayerIntMapSetter.set(f.player, "varbit.mistmyst_progress", MisthalinStage.HeweyDead)
        f.player.inv[1] = InvObj(KillerKnife, 1)
        f.sapphireDoor(f.player.coords.translateX(1))
        assertEquals(0, f.player.inv.count(KillerKnife))
        assertEquals(MisthalinStage.SapphireRoomOpen, f.quest.getQuestStage(f.player))
        assertEquals(CoordGrid(1628, 4829), f.player.coords)
    }

    @Test fun `sapphire exit without murder weapon preserves other equipment without a removal message`() {
        val f = Fixture(MisthalinStage.MandyWaiting)
        f.enterBlueRoom()
        f.player.worn[Wearpos.RightHand.slot] = InvObj("obj.bronze_sword", 1)
        f.player.worn.clearModifiedSlots()
        f.sapphireDoor(f.player.coords.translateX(1))
        assertEquals(1, f.player.worn.count("obj.bronze_sword"))
        assertFalse(f.player.worn.hasModifiedSlots())
        assertEquals(0, f.wornChanges)
        assertFalse(f.output().contains("leave the murder weapon behind"))
    }

    @Test fun `painting faces east for two ticks before slashing when stationary or arriving`() {
        for (arriving in listOf(false, true)) {
            val f = Fixture(MisthalinStage.Note1Read)
            if (arriving) f.player.lastMovement = f.player.currentMapClock
            f.slash()
            if (arriving) {
                assertEquals(EntityFaceAngle.NULL, f.player.pendingFaceAngle)
                f.assertUnslashed()
                f.tick()
            }
            assertEquals(Direction.East.angle, f.player.pendingFaceAngle.intValue)
            f.assertUnslashed()
            f.tick()
            f.assertUnslashed()
            f.tick()
            assertEquals("seq.human_knife_chop".asRSCM(), f.player.pendingSequence.id)
            assertEquals(MisthalinStage.PaintingSlashed, f.quest.getQuestStage(f.player))
            assertEquals(1, f.player.inv.count("obj.knife"))
        }
    }

    @Test fun `cancelled painting action does not slash or advance the quest`() {
        val f = Fixture(MisthalinStage.Note1Read)
        f.slash()
        f.tick()
        f.cancel()
        repeat(3) { f.tick() }
        f.assertUnslashed()
        assertEquals(1, f.player.inv.count("obj.knife"))
    }

    @Test fun `painting cannot be slashed before reading the clue or slashed again`() {
        for (stage in listOf(0, MisthalinStage.TaytenDead, MisthalinStage.PaintingSlashed, MisthalinStage.Complete)) {
            val f = Fixture(stage)
            f.slash()
            assertEquals(EntityFaceAngle.NULL, f.player.pendingFaceAngle)
            assertEquals(EntitySeq.NULL, f.player.pendingSequence)
            assertEquals(stage, f.quest.getQuestStage(f.player))
            assertTrue(f.output().contains("Nothing interesting happens."))
            assertTrue(f.coroutine.isIdle)
        }
    }

    @Test fun `native staircase op explains why it cannot be climbed without changing floors or quest state`() {
        for (stage in listOf(0, MisthalinStage.InsideManor, MisthalinStage.Complete)) {
            val f = Fixture(stage)
            f.player.coords = CoordGrid(1633, 4829)
            val start = f.player.coords
            f.stairs()
            assertTrue(f.output().contains("The staircase looks too rickety to climb."))
            assertEquals(start, f.player.coords)
            assertEquals(stage, f.quest.getQuestStage(f.player))
            assertEquals(EntitySeq.NULL, f.player.pendingSequence)
            assertTrue(f.coroutine.isIdle)
        }
    }

    @Test fun `native quest progress updates preserve the adjacent puzzle flags`() {
        val f = Fixture(MisthalinStage.CandlesLit)
        VarPlayerIntMapSetter.set(f.player, "varbit.mistmyst_candles", 15)
        VarPlayerIntMapSetter.set(f.player, "varbit.mistmyst_xpreward", 1)
        f.setStage(MisthalinStage.WallBlown)
        assertEquals(MisthalinStage.WallBlown, f.quest.getQuestStage(f.player))
        assertEquals(15, f.player.vars["varbit.mistmyst_candles"])
        assertEquals(1, f.player.vars["varbit.mistmyst_xpreward"])
        assertNull(f.player.attr[org.rsmod.content.quest.manager.QUEST_STAGE_MAP_ATTR])
    }

    @Test fun `reconnect restores a private fight exit and removes carried and equipped murder weapons`() {
        for (stage in listOf(MisthalinStage.BossArmed, MisthalinStage.BossBeaten, MisthalinStage.HeweyDead)) {
            val f = Fixture(stage)
            f.player.misthalinReturnTile = CoordGrid(1628, 4829).packed
            f.player.coords = CoordGrid(8000, 8000)
            f.player.inv[2] = InvObj(KillerKnife, 1)
            f.player.worn[Wearpos.RightHand.slot] = InvObj(KillerKnife, 1)
            restoreMisthalinLogin(f.player)
            assertEquals(CoordGrid(1628, 4829), f.player.coords)
            assertEquals(0, f.player.misthalinReturnTile)
        assertTrue(f.player.ui.modals.isEmpty())
            assertEquals(MisthalinStage.SapphireRoomOpen, f.quest.getQuestStage(f.player))
            assertEquals(0, f.player.inv.count(KillerKnife))
            assertEquals(0, f.player.worn.count(KillerKnife))
            assertEquals(1, f.player.inv.count("obj.knife"))
        }
    }

    @Test fun `reconnect after the final kill preserves progress and validates the saved exit`() {
        val f = Fixture(MisthalinStage.AbigaleKilled)
        f.player.misthalinReturnTile = CoordGrid(8000, 8000).packed
        restoreMisthalinLogin(f.player)
        assertEquals(MisthalinCoords.IslandLanding, f.player.coords)
        assertEquals(MisthalinStage.AbigaleKilled, f.quest.getQuestStage(f.player))
    }

    @Test fun `ordinary logins are not moved or rewound`() {
        val f = Fixture(MisthalinStage.BossArmed)
        val coords = f.player.coords
        restoreMisthalinLogin(f.player)
        assertEquals(coords, f.player.coords)
        assertEquals(MisthalinStage.BossArmed, f.quest.getQuestStage(f.player))
    }

    @Test fun `temporary puzzle fields do not overwrite one another or the persistent scene exit`() {
        val f = Fixture(MisthalinStage.PanelRevealed)
        f.player.misthalinReturnTile = MisthalinCoords.IslandLanding.packed
        f.player.misthalinReflections = 3
        f.player.misthalinGemRowSlot = 6
        assertEquals(3, f.player.misthalinReflections)
        assertEquals(6, f.player.misthalinGemRowSlot)
        assertEquals(MisthalinCoords.IslandLanding.packed, f.player.misthalinReturnTile)
        assertEquals(MisthalinStage.PanelRevealed, f.quest.getQuestStage(f.player))
    }

    @Test fun `selected gem models resolve to the original puzzle models`() {
        for ((name, model) in listOf("diamond" to 32333, "onyx" to 32342, "zenyte" to 32349,
            "ruby" to 2586, "sapphire" to 32345, "emerald" to 32334)) {
            assertEquals(model, "models.misthalin_gem_$name".asRSCM())
        }
    }

    @Test fun `completion awards quest points and completion interface only once`() {
        val f = Fixture(MisthalinStage.MandyWaiting)
        f.setStage(MisthalinStage.Complete)
        assertEquals(1, f.player.vars["varp.qp"])
        assertEquals(1, f.player.vars["varbit.quests_completed_count"])
        assertTrue(f.player.ui.containsModal("interface.questscroll"))
        val before = f.output()
        f.setStage(MisthalinStage.Complete)
        assertEquals(1, f.player.vars["varp.qp"])
        assertEquals(1, f.player.vars["varbit.quests_completed_count"])
        assertEquals(before, f.output())
    }

    @Test fun `item-use packet path empties the barrel after Sid dies`() {
        val f = Fixture(MisthalinStage.SidDead)
        f.useOn("loc.mistmyst_barrel", "obj.bucket_empty")
        assertEquals(MisthalinStage.BucketFilled, f.quest.getQuestStage(f.player))
        assertEquals(0, f.player.inv.count("obj.bucket_empty"))
        assertEquals(1, f.player.inv.count("obj.bucket_water"))
        assertFalse(f.output().contains("Nothing interesting happens."))
    }

    @Test fun `item-use packet path lights each distinct candle and dries the fuse`() {
        val f = Fixture(MisthalinStage.RubyRoomOpen)
        for (i in 1..4) {
            f.useOn("loc.mistmyst_candle$i", "obj.tinderbox")
            assertEquals(1, f.player.vars["varbit.mistmyst_candle$i"])
            f.continueDialogue("component.messagebox:continue")
        }
        assertEquals(MisthalinStage.CandlesLit, f.quest.getQuestStage(f.player))
        f.useOn("loc.mistmyst_explosive_barrel", "obj.tinderbox")
        assertEquals("seq.human_createfire".asRSCM(), f.player.pendingSequence.id)
        repeat(3) { f.tick() }
        f.continueDialogue("component.chat_right:continue")
        assertEquals(MisthalinStage.FuseLit, f.quest.getQuestStage(f.player))
    }

    @Test fun `quest list click permissions are restored when reopened after a scene`() {
        val (before, after) = Fixture(MisthalinStage.SidDead).reopenQuestList()
        assertNotEquals(0L, before)
        assertEquals(before, after)
    }

    @Test fun `Sid scene finishes and the next bucket interaction works`() {
        val f = Fixture(MisthalinStage.SeenShadyFigure)
        f.searchBarrel()
        f.finishScene()
        assertEquals(MisthalinStage.SidDead, f.quest.getQuestStage(f.player))
        assertEquals(0, f.player.vars["varbit.cutscene_status"])
        assertEquals(0, f.player.misthalinReturnTile)
        assertTrue(f.player.ui.modals.isEmpty())
        f.useOn("loc.mistmyst_barrel", "obj.bucket_empty")
        assertEquals(MisthalinStage.BucketFilled, f.quest.getQuestStage(f.player))
    }

    @Test fun `observing the tree after the wall explodes runs Lacey scene to completion`() {
        val f = Fixture(MisthalinStage.WallBlown)
        f.observeTree()
        f.finishScene()
        assertEquals(MisthalinStage.LaceyDead, f.quest.getQuestStage(f.player))
        assertEquals(0, f.player.vars["varbit.cutscene_status"])
        assertEquals(0, f.player.misthalinReturnTile)
        assertTrue(f.player.ui.modals.isEmpty())
    }

    @Test fun `using the bucket can trigger Sid scene and then fill it`() {
        val f = Fixture(MisthalinStage.SeenShadyFigure)
        f.useOn("loc.mistmyst_barrel", "obj.bucket_empty")
        f.finishScene()
        assertEquals(MisthalinStage.BucketFilled, f.quest.getQuestStage(f.player))
        assertEquals(1, f.player.inv.count("obj.bucket_water"))
    }

    private class Fixture(stage: Int) {
        val quest = Quest(
            id = 128, key = "quest_misthalinmystery", rowID = 0, displayName = "Misthalin Mystery",
            mapElement = null, startCoord = null, maxSteps = MisthalinStage.Complete, questPoints = 1,
            questVarp = "varp.mistmyst_main", questVarbit = "varbit.mistmyst_progress",
            rewards = rewards {}, itemDisplay = org.rsmod.content.quest.manager.ItemRewardDisplay("obj.mistmyst_cutscene_knife"),
        )
        private val client = RecordingClient()

        @OptIn(InternalApi::class)
        val player = Player().apply {
            this.client = this@Fixture.client
            uuid = 456L
            slotId = 1
            assignUid()
            coords = CoordGrid(1632, 4833)
            currentMapClock = 100
            processedMapClock = 100
            pendingSequence = EntitySeq.NULL
            pendingFaceAngle = EntityFaceAngle.NULL
            inv = Inventory(InventoryServerType(size = 28), arrayOfNulls(28))
            worn = Inventory(InventoryServerType(size = 14), arrayOfNulls(14))
            inv[0] = InvObj(checkNotNull(ServerCacheManager.getItem("obj.knife".asRSCM())), 1)
        }
        private val events = EventBus()
        private val collision = CollisionFlagMap()
        private lateinit var regions: RegionRegistry
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() },
            getCollision = { collision },
            getTeleportValidator = { PlayerTeleportValidator(emptySet()) },
            getAreaChecker = { AreaChecker(regions, AreaIndex()) },
        )
        private val movement = PlayerMovementProcessor(collision, RouteFactory(collision), StepFactory(collision), events)
        val coroutine = GameCoroutine("misthalin-manor-test")
        private var result: Result<Unit>? = null
        var wornChanges = 0
        var unequips = 0

        init {
            VarPlayerIntMapSetter.set(player, "varbit.mistmyst_progress", stage)
            val locU = LocUInteractions::class.java.getDeclaredConstructor(EventBus::class.java)
                .apply { isAccessible = true }.newInstance(events)
            val bridge = LocUOpScript::class.java.getDeclaredConstructor(LocUInteractions::class.java)
                .apply { isAccessible = true }.newInstance(locU)
            with(bridge) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
            with(org.rsmod.content.quest.manager.QuestEvents()) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
            val clock = MapClock(100)
            val updates = ZoneUpdateMap()
            val storage = LocZoneStorage()
            val normal = LocRegistryNormal(updates, collision, storage)
            val npcs = NpcList()
            val npcRegistry = NpcRegistry(npcs, collision, events)
            regions = RegionRegistry(RegionListSmall(), RegionListLarge(), normal, collision, storage,
                npcRegistry, ControllerRegistry(clock, ControllerList()), ZonePlayerActivityBitSet())
            val locs = LocRepository(clock, LocRegistry(storage, normal,
                LocRegistryRegion(updates, collision, storage, regions)), regions)
            val cutscenes = MisthalinCutscenes(quest, RegionRepository(regions), locs,
                NpcRepository(clock, npcRegistry, npcs), ObjRepository(clock, ObjRegistry(updates)), collision)
            MisthalinManor(quest, locs, cutscenes, MisthalinPuzzles(quest))
                .register(ScriptContext(events, CheatCommandMap(), EngineQueueCache()))
            MisthalinBossFight(quest, NpcRepository(clock, npcRegistry, npcs),
                ObjRepository(clock, ObjRegistry(updates)), locs, RegionRepository(regions), WorldRepository(updates),
                PlayerRepository(PlayerRegistry(PlayerList(), collision, ZonePlayerActivityBitSet(), events)),
                unused<ProtectedAccessLauncher>(), collision)
                .register(ScriptContext(events, CheatCommandMap(), EngineQueueCache()))
            events.subscribeUnbound(HeldEquipEvents.WearposChange::class.java) { wornChanges++ }
            val knife = checkNotNull(ServerCacheManager.getItem(KillerKnife.asRSCM()))
            events.subscribeKeyed(HeldEquipEvents.Unequip::class.java, knife.contentGroup.toLong()) { unequips++ }
            for (level in 0..3) for (x in 1600..1663 step 8) for (z in 4800..4863 step 8) {
                collision.allocateIfAbsent(x, z, level)
            }
        }

        fun setStage(stage: Int) {
            quest.setQuestStage(ProtectedAccess(player, coroutine, context), stage)
        }

        fun searchBarrel() {
            val loc = loc("loc.mistmyst_barrel", CoordGrid(1615, 4829), shape = 10)
            dispatch(checkNotNull(LocInteractions(BoundValidator(collision), events)
                .opTrigger(player, loc, InteractionOp.Op1)))
        }

        fun sapphireDoor(coords: CoordGrid) {
            val loc = loc("loc.mistmyst_door_sapphire", coords, shape = 0, angle = 0)
            dispatch(checkNotNull(LocInteractions(BoundValidator(collision), events)
                .opTrigger(player, loc, InteractionOp.Op1)))
        }

        fun enterBlueRoom() {
            player.coords = CoordGrid(1628, 4829)
            sapphireDoor(player.coords)
            repeat(2) { tick() }
            assertEquals(CoordGrid(1627, 4829), regions.normalizeCoords(player.coords))
            assertNotEquals(CoordGrid(1627, 4829), player.coords)
            assertTrue(coroutine.isIdle)
        }

        fun continueDialogue(component: String) {
            coroutine.resumeWith(ResumePauseButtonInput(component, -1))
            result?.getOrThrow()
            assertTrue(coroutine.isIdle)
        }

        fun slash() {
            val loc = loc("loc.mistmyst_painting", CoordGrid(1632, 4833), shape = 4)
            useOn(loc, "obj.knife")
        }

        fun useOn(symbol: String, item: String) {
            useOn(loc(symbol, player.coords, 10), item)
        }

        private fun useOn(target: BoundLocInfo, item: String) {
            player.inv[0] = InvObj(item, 1)
            val event = LocTInteractions(events).opTrigger(player, target,
                checkNotNull(ServerCacheManager.getItem(item.asRSCM())),
                ServerCacheManager.fromComponent("component.inventory:items"), 0)
            dispatch(checkNotNull(event))
        }

        fun reopenQuestList(): Pair<Long, Long> {
            val component = ServerCacheManager.fromComponent("component.questlist:list")
            player.ifOpenOverlay("interface.questlist", "component.side_journal:tab_container", events)
            val before = player.ui.events[component, 1]
            player.ifCloseOverlay("interface.questlist", events)
            assertEquals(0L, player.ui.events[component, 1])
            player.ifOpenOverlay("interface.questlist", "component.side_journal:tab_container", events)
            return before to player.ui.events[component, 1]
        }

        fun observeTree() {
            val target = loc("loc.mistmyst_tree", player.coords, 10)
            dispatch(checkNotNull(LocInteractions(BoundValidator(collision), events)
                .opTrigger(player, target, InteractionOp.Op1)))
        }

        fun finishScene() {
            repeat(800) {
                if (coroutine.isIdle) return
                if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                    val parent = listOf("chat_left", "chat_right", "messagebox", "chatmenu", "objectbox")
                        .firstOrNull { player.ui.containsModal("interface.$it") }
                        ?: error("Unknown scene dialogue")
                    val input = when (parent) {
                        "chatmenu" -> ResumePauseButtonInput("component.chatmenu:options", 1)
                        "objectbox" -> ResumePauseButtonInput("component.objectbox:universe", -1)
                        else -> ResumePauseButtonInput("component.$parent:continue", -1)
                    }
                    coroutine.resumeWith(input)
                    result?.getOrThrow()
                } else tick()
            }
            fail<Unit>("Scene did not finish")
        }

        fun stairs() {
            val loc = loc("loc.mistmyst_stairs_up", CoordGrid(1633, 4825), shape = 10)
            dispatch(checkNotNull(LocInteractions(BoundValidator(collision), events)
                .opTrigger(player, loc, InteractionOp.Op1)))
        }

        private fun dispatch(event: OpEvent) {
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, context)
            val block: suspend () -> Unit = { assertTrue(events.publish(access, event)) }
            block.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { this@Fixture.result = result }
            })
            result?.getOrThrow()
            PlayerFaceSquareProcessor().process(player)
        }

        fun assertUnslashed() {
            assertEquals(EntitySeq.NULL, player.pendingSequence)
            assertEquals(MisthalinStage.Note1Read, quest.getQuestStage(player))
        }

        fun tick() {
            player.currentMapClock++
            player.processedMapClock = player.currentMapClock
            player.pendingSequence = EntitySeq.NULL
            player.pendingFaceAngle = EntityFaceAngle.NULL
            movement.process(player)
            coroutine.advance()
            result?.getOrThrow()
            PlayerFaceSquareProcessor().process(player)
        }

        fun cancel() {
            coroutine.cancel()
            assertInstanceOf(CancellationException::class.java, result?.exceptionOrNull())
            result = null
            player.activeCoroutine = null
        }

        fun output(): String = client.messages.joinToString("\n")

        fun clearAppearanceRebuild() {
            player.appearance.javaClass.getDeclaredField("rebuild").apply { isAccessible = true }
                .setBoolean(player.appearance, false)
        }

        fun appearanceNeedsRebuild(): Boolean =
            player.appearance.javaClass.getDeclaredField("rebuild").apply { isAccessible = true }
                .getBoolean(player.appearance)

        private fun loc(symbol: String, coords: CoordGrid, shape: Int, angle: Int = 2): BoundLocInfo {
            val type = checkNotNull(ServerCacheManager.getObject(symbol.asRSCM()))
            val layer = when (shape) {
                in 0..3 -> 0
                in 4..8 -> 1
                else -> 2
            }
            return BoundLocInfo(LocInfo(layer, coords, LocEntity(type.id, shape, angle)), type)
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
        private const val KillerKnife = "obj.mistmyst_cutscene_knife"
        private val restored = mutableListOf<() -> Unit>()

        private inline fun <reified T> unused(): T {
            val field = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
            return (field.get(null) as Unsafe).allocateInstance(T::class.java) as T
        }

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
