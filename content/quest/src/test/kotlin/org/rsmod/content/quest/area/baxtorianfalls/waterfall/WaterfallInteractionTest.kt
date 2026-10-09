package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.hunt.Hunt
import org.rsmod.api.hunt.NpcSearch
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.DefaultGameRandom
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
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.AMULET
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.BAXTORIAN_KEY
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.BOOK
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.COMPLETE
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.ENTERED_FALLS
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.ENTERED_TOMB
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.FLOOR_RISEN
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.GOLRIE_KEY
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.MET_HUDON
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.PEBBLE
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.READ_BOOK
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.RUNES_PLACED
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.STARTED
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.URN_EMPTY
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.URN_FULL
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs.Golrie
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs.Hudon
import org.rsmod.content.quest.util.QuestDoors
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
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
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
class WaterfallInteractionTest {

    @Test
    fun `the book is found once and only after Hudon was met`() {
        val early = Fixture(STARTED)
        early.loc(BOOKCASE, BOOKCASE_COORDS)
        early.finish()
        assertEquals(0, early.total(BOOK))

        val f = Fixture(MET_HUDON)
        f.loc(BOOKCASE, BOOKCASE_COORDS)
        f.finish()
        assertEquals(1, f.total(BOOK))
        f.loc(BOOKCASE, BOOKCASE_COORDS)
        f.finish()
        assertEquals(1, f.total(BOOK))
    }

    @Test
    fun `Golrie's key turns up in the crate once, and only after the book was read`() {
        val early = Fixture(MET_HUDON)
        early.loc(GOLRIE_CRATE, GOLRIE_CRATE_COORDS)
        early.finish()
        assertEquals(0, early.total(GOLRIE_KEY))

        val f = Fixture(READ_BOOK)
        f.loc(GOLRIE_CRATE, GOLRIE_CRATE_COORDS)
        f.finish()
        assertEquals(1, f.total(GOLRIE_KEY))
        f.loc(GOLRIE_CRATE, GOLRIE_CRATE_COORDS)
        f.finish()
        assertEquals(1, f.total(GOLRIE_KEY))
    }

    @Test
    fun `Golrie swaps the key for the pebble and hands out only one pebble`() {
        val f = Fixture(READ_BOOK)
        f.give(GOLRIE_KEY)
        f.talk(GOLRIE_NPC)
        f.finish()
        assertEquals(1, f.total(PEBBLE))
        assertEquals(0, f.total(GOLRIE_KEY))
        assertTrue(f.player.metGolrie)

        f.talk(GOLRIE_NPC)
        f.finish()
        assertEquals(1, f.total(PEBBLE))

        f.player.inv[f.player.inv.indexOfFirst { it?.id == PEBBLE.asRSCM() }] = null
        f.talk(GOLRIE_NPC)
        f.finish()
        assertEquals(1, f.total(PEBBLE))
    }

    @Test
    fun `a full inventory gets no pebble, and the key is swapped for it when there is one`() {
        val full = Fixture(READ_BOOK)
        full.fillInventory()
        full.talk(GOLRIE_NPC)
        full.finish()
        assertEquals(0, full.total(PEBBLE))
        assertFalse(full.player.metGolrie)

        val keyed = Fixture(READ_BOOK)
        keyed.give(GOLRIE_KEY)
        keyed.fillInventory()
        keyed.talk(GOLRIE_NPC)
        keyed.finish()
        assertEquals(0, keyed.total(GOLRIE_KEY))
        assertEquals(1, keyed.total(PEBBLE))
        assertEquals(0, keyed.ground(PEBBLE))
    }

    @Test
    fun `the dungeon crate holds one key and the door keeps it when it opens`() {
        val f = Fixture(ENTERED_FALLS)
        f.loc(CRATE, CRATE_COORDS, angle = LocAngle.North)
        f.finish()
        assertEquals(1, f.total(BAXTORIAN_KEY))
        f.loc(CRATE, CRATE_COORDS, angle = LocAngle.North)
        f.finish()
        assertEquals(1, f.total(BAXTORIAN_KEY))

        f.player.coords = TOMB_DOOR_SOUTH
        f.loc(TOMB_DOOR, TOMB_DOOR_COORDS, shape = LocShape.WallStraight, angle = LocAngle.North)
        f.finish()
        assertEquals(1, f.total(BAXTORIAN_KEY))
    }

    @Test
    fun `the tomb chest and coffin give the amulet and urn once each`() {
        val f = Fixture(ENTERED_TOMB)
        f.loc(CHEST_OPEN, CHEST_COORDS)
        f.finish()
        f.loc(CHEST_OPEN, CHEST_COORDS)
        f.finish()
        assertEquals(1, f.total(AMULET))

        f.loc(COFFIN, COFFIN_COORDS)
        f.finish()
        f.loc(COFFIN, COFFIN_COORDS)
        f.finish()
        assertEquals(1, f.total(URN_FULL))

        f.player.inv[f.player.inv.indexOfFirst { it?.id == AMULET.asRSCM() }] = null
        f.loc(CHEST_OPEN, CHEST_COORDS)
        f.finish()
        assertEquals(1, f.total(AMULET))

        val worn = Fixture(ENTERED_FALLS)
        worn.player.worn[2] = InvObj(AMULET, 1)
        worn.loc(CHEST_OPEN, CHEST_COORDS)
        worn.finish()
        assertEquals(0, worn.total(AMULET))
    }

    @Test
    fun `quest items found with a full inventory land on the floor instead of vanishing`() {
        val finds =
            listOf(
                Triple(MET_HUDON, BOOK) { f: Fixture -> f.loc(BOOKCASE, BOOKCASE_COORDS) },
                Triple(READ_BOOK, GOLRIE_KEY) { f: Fixture ->
                    f.loc(GOLRIE_CRATE, GOLRIE_CRATE_COORDS)
                },
                Triple(ENTERED_TOMB, AMULET) { f: Fixture -> f.loc(CHEST_OPEN, CHEST_COORDS) },
                Triple(ENTERED_TOMB, URN_FULL) { f: Fixture -> f.loc(COFFIN, COFFIN_COORDS) },
                Triple(ENTERED_FALLS, BAXTORIAN_KEY) { f: Fixture ->
                    f.loc(CRATE, CRATE_COORDS, angle = LocAngle.North)
                },
            )
        for ((stage, obj, search) in finds) {
            val f = Fixture(stage)
            f.fillInventory()
            search(f)
            f.finish()
            assertEquals(0, f.count(obj), obj)
            assertEquals(1, f.ground(obj), obj)
        }
    }

    @Test
    fun `the tombstone stays shut for the armed and opens for the peaceful`() {
        val refused =
            listOf(
                "obj.bronze_sword",
                "obj.airrune",
                "obj.logs",
                "obj.bronze_arrow",
                "obj.skillcape_attack",
                "obj.mythical_cape",
                "obj.ardy_cape_easy",
            )
        for (carried in refused) {
            val f = Fixture(READ_BOOK)
            f.player.coords = TOMBSTONE_BANK
            f.give(PEBBLE)
            f.give(carried)
            f.useOnLoc(TOMBSTONE, TOMBSTONE_COORDS, PEBBLE)
            f.finish()
            assertEquals(TOMBSTONE_BANK, f.player.coords, carried)
            assertEquals(READ_BOOK, f.stage(), carried)
        }

        val worn = Fixture(READ_BOOK)
        worn.player.coords = TOMBSTONE_BANK
        worn.give(PEBBLE)
        worn.player.worn[3] = InvObj("obj.bronze_sword", 1)
        worn.useOnLoc(TOMBSTONE, TOMBSTONE_COORDS, PEBBLE)
        worn.finish()
        assertEquals(TOMBSTONE_BANK, worn.player.coords)

        val peaceful = Fixture(READ_BOOK)
        peaceful.player.coords = TOMBSTONE_BANK
        peaceful.give(PEBBLE)
        peaceful.give("obj.swordfish")
        peaceful.give("obj.coins", 100)
        peaceful.give("obj.tome_of_fire")
        peaceful.player.worn[1] = InvObj("obj.graceful_cape", 1)
        peaceful.useOnLoc(TOMBSTONE, TOMBSTONE_COORDS, PEBBLE)
        peaceful.finish()
        assertEquals(WaterfallCoords.TOMB_ENTRY, peaceful.player.coords)
        assertEquals(ENTERED_TOMB, peaceful.stage())
        assertEquals(1, peaceful.total(PEBBLE))
    }

    @Test
    fun `each pillar takes one rune of each kind and the sixth charge moves the quest on`() {
        val f = Fixture(ENTERED_FALLS)
        f.player.coords = ROOM_CENTER
        f.give("obj.airrune", 6)
        f.give("obj.waterrune", 6)
        f.give("obj.earthrune", 6)

        f.useOnLoc(PILLAR, PILLAR_COORDS[0], "obj.airrune")
        f.finish()
        assertEquals(5, f.count("obj.airrune"))
        assertEquals(1, f.player.pillarRunes)

        f.useOnLoc(PILLAR, PILLAR_COORDS[0], "obj.airrune")
        f.finish()
        assertEquals(5, f.count("obj.airrune"))
        assertEquals(1, f.player.pillarRunes)

        for ((index, pillar) in PILLAR_COORDS.withIndex()) {
            for (rune in listOf("obj.airrune", "obj.waterrune", "obj.earthrune")) {
                if (index == 0 && rune == "obj.airrune") continue
                f.useOnLoc(PILLAR, pillar, rune)
                f.finish()
            }
        }
        assertEquals(WaterfallQuest.ALL_PILLAR_RUNES, f.player.pillarRunes)
        assertEquals(0, f.count("obj.airrune") + f.count("obj.waterrune") + f.count("obj.earthrune"))
        assertEquals(RUNES_PLACED, f.stage())
    }

    @Test
    fun `other runes are refused and a rune that is already gone is not charged`() {
        val f = Fixture(ENTERED_FALLS)
        f.player.coords = ROOM_CENTER
        f.give("obj.firerune", 2)
        f.useOnLoc(PILLAR, PILLAR_COORDS[0], "obj.firerune", handled = false)
        f.finish()
        assertEquals(2, f.count("obj.firerune"))
        assertEquals(0, f.player.pillarRunes)

        f.useOnLoc(PILLAR, PILLAR_COORDS[0], "obj.airrune", absent = true)
        f.finish()
        assertEquals(0, f.player.pillarRunes)
    }

    @Test
    fun `a maxed rune stack loses exactly one rune per pillar`() {
        val f = Fixture(ENTERED_FALLS)
        f.player.coords = ROOM_CENTER
        f.give("obj.airrune", Int.MAX_VALUE)
        f.useOnLoc(PILLAR, PILLAR_COORDS[0], "obj.airrune")
        f.finish()
        assertEquals(Int.MAX_VALUE - 1, f.count("obj.airrune"))
    }

    @Test
    fun `the amulet floods the room and is lost before the pillars are charged`() {
        val f = Fixture(ENTERED_FALLS)
        f.player.coords = ROOM_CENTER
        f.give(AMULET)
        f.useOnLoc(STATUE, STATUE_COORDS, AMULET)
        f.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, f.player.coords)
        assertEquals(0, f.total(AMULET))
        assertEquals(ENTERED_FALLS, f.stage())

        val gone = Fixture(ENTERED_FALLS)
        gone.player.coords = ROOM_CENTER
        gone.useOnLoc(STATUE, STATUE_COORDS, AMULET, absent = true)
        gone.finish()
        assertEquals(ROOM_CENTER, gone.player.coords)
    }

    @Test
    fun `the amulet on the statue raises the floor and is taken once`() {
        val f = Fixture(RUNES_PLACED)
        f.player.pillarRunes = WaterfallQuest.ALL_PILLAR_RUNES
        f.player.coords = ROOM_CENTER
        f.give(AMULET)
        f.useOnLoc(STATUE, STATUE_COORDS, AMULET)
        f.finish()
        assertEquals(ROOM_CENTER.translate(38, -1), f.player.coords)
        assertEquals(0, f.total(AMULET))
        assertEquals(FLOOR_RISEN, f.stage())

        f.give(AMULET)
        f.useOnLoc(STATUE, STATUE_COORDS.translate(38, -1), AMULET)
        f.finish()
        assertEquals(1, f.total(AMULET))
        assertEquals(FLOOR_RISEN, f.stage())

        val gone = Fixture(RUNES_PLACED)
        gone.player.pillarRunes = WaterfallQuest.ALL_PILLAR_RUNES
        gone.player.coords = ROOM_CENTER
        gone.useOnLoc(STATUE, STATUE_COORDS, AMULET, absent = true)
        gone.finish()
        assertEquals(RUNES_PLACED, gone.stage())
        assertEquals(ROOM_CENTER, gone.player.coords)
    }

    @Test
    fun `the urn cannot be poured before the floor has risen`() {
        val f = Fixture(RUNES_PLACED)
        f.player.coords = ROOM_CENTER
        f.give(URN_FULL)
        f.useOnLoc(CHALICE, CHALICE_COORDS, URN_FULL)
        f.finish()
        assertEquals(RUNES_PLACED, f.stage())
        assertEquals(1, f.total(URN_FULL))
    }

    @Test
    fun `taking the treasure without the ashes keeps the urn`() {
        val f = Fixture(FLOOR_RISEN)
        f.player.coords = ROOM_CENTER.translate(38, -1)
        f.give(URN_FULL)
        f.loc(CHALICE, CHALICE_COORDS)
        f.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, f.player.coords)
        assertEquals(FLOOR_RISEN, f.stage())
        assertEquals(1, f.total(URN_FULL))
        assertEquals(0, f.total("obj.diamond"))
    }

    @Test
    fun `a full inventory or a maxed seed stack cannot pour the urn and loses nothing`() {
        val full = Fixture(FLOOR_RISEN)
        full.player.coords = ROOM_CENTER.translate(38, -1)
        full.give(URN_FULL)
        full.fillInventory()
        full.useOnLoc(CHALICE, CHALICE_COORDS, URN_FULL)
        full.finish()
        assertEquals(FLOOR_RISEN, full.stage())
        assertEquals(1, full.total(URN_FULL))
        assertEquals(0, full.player.vars["varp.qp"])

        val maxed = Fixture(FLOOR_RISEN)
        maxed.player.coords = ROOM_CENTER.translate(38, -1)
        maxed.give(URN_FULL)
        maxed.give("obj.mithril_seed", Int.MAX_VALUE)
        maxed.useOnLoc(CHALICE, CHALICE_COORDS, URN_FULL)
        maxed.finish()
        assertEquals(FLOOR_RISEN, maxed.stage())
        assertEquals(1, maxed.total(URN_FULL))
        assertEquals(Int.MAX_VALUE, maxed.total("obj.mithril_seed"))
        assertEquals(0, maxed.total("obj.diamond"))
    }

    @Test
    fun `the rewards fit in exactly five free slots and come exactly once`() {
        val f = Fixture(FLOOR_RISEN)
        f.player.coords = ROOM_CENTER.translate(38, -1)
        f.give(URN_FULL)
        f.fillInventory(free = 5)
        f.useOnLoc(CHALICE, CHALICE_COORDS, URN_FULL)
        f.finish()
        f.assertRewards()
        assertEquals(1, f.total(URN_EMPTY))
        assertEquals(0, f.total(URN_FULL))
        assertEquals(0, f.ground("obj.diamond") + f.ground("obj.gold_bar") + f.ground("obj.mithril_seed"))

        f.player.inv[f.player.inv.indexOfFirst { it?.id == "obj.logs".asRSCM() }] = null
        f.give(URN_FULL)
        f.useOnLoc(CHALICE, CHALICE_COORDS, URN_FULL)
        f.finish()
        f.loc(CHALICE, CHALICE_COORDS)
        f.finish()
        f.assertRewards()
        assertEquals(1, f.total(URN_FULL))
    }

    @Test
    fun `an urn that is already gone gives no reward`() {
        val f = Fixture(FLOOR_RISEN)
        f.player.coords = ROOM_CENTER.translate(38, -1)
        f.useOnLoc(CHALICE, CHALICE_COORDS, URN_FULL, absent = true)
        f.finish()
        assertEquals(FLOOR_RISEN, f.stage())
        assertEquals(0, f.total("obj.diamond"))
        assertEquals(0, f.total(URN_EMPTY))
    }

    private class Fixture(stage: Int = 0) {
        val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("waterfall-test")
        private var result: Result<Unit>? = null
        private val collision = CollisionFlagMap()
        private val clock = MapClock(100)
        private val updates = ZoneUpdateMap()
        private val storage = LocZoneStorage()
        private val activity = ZonePlayerActivityBitSet()
        private val npcList = NpcList()
        private val playerList = PlayerList()
        private val npcRegistry = NpcRegistry(npcList, collision, events)
        private val playerRegistry = PlayerRegistry(playerList, collision, activity, events)
        private val objRegistry = ObjRegistry(updates)
        private val normal = LocRegistryNormal(updates, collision, storage)
        private val regions =
            RegionRegistry(
                RegionListSmall(),
                RegionListLarge(),
                RegionListWorldEntity(),
                normal,
                collision,
                storage,
                npcRegistry,
                ControllerRegistry(clock, ControllerList()),
                activity,
            )
        private val locRegistry =
            LocRegistry(storage, normal, LocRegistryRegion(updates, collision, storage, regions))
        private val locs = LocRepository(clock, locRegistry, regions)
        private val npcs = NpcRepository(clock, npcRegistry, npcList)
        private val objs = ObjRepository(clock, objRegistry)
        private val world = WorldRepository(updates)
        private val search =
            NpcSearch(
                Hunt(RayCastValidator(collision), playerRegistry, npcRegistry, objRegistry, locRegistry)
            )
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getAlignment = { TextAlignment() },
                    getNpcInteractions = { NpcInteractions(events) },
                    getCollision = { collision },
                    getNpcList = { npcList },
                    getTeleportValidator = { PlayerTeleportValidator(emptySet()) },
                    getAreaChecker = { AreaChecker(regions, AreaIndex()) },
                    getRandom = { DefaultGameRandom(1) },
                )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 1465L
                observerUUID = 1465L
                slotId = 1
                assignUid()
                coords = RAFT_BANK
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

        val quest = WaterfallQuest()

        private fun freeSlot(): Int = (0 until 28).first { player.inv[it] == null }

        init {
            for (level in 0..1) {
                for (x in 2496..2624 step 8) {
                    for (z in 3392..3520 step 8) collision.allocateIfAbsent(x, z, level)
                    for (z in 9536..9600 step 8) collision.allocateIfAbsent(x, z, level)
                    for (z in 9792..9936 step 8) collision.allocateIfAbsent(x, z, level)
                }
            }
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            val doors = QuestDoors(locs)
            with(quest) { scripts.startup() }
            with(Golrie(quest, objs)) { scripts.startup() }
            with(BookOnBaxtorian(quest, objs)) { scripts.startup() }
            with(GnomeVillageDungeon(quest, objs, doors)) { scripts.startup() }
            with(GlarialsTomb(quest, locs, objs)) { scripts.startup() }
            with(WaterfallDungeon(quest, objs, world, doors)) { scripts.startup() }
            stageTo(stage)
        }

        fun stageTo(value: Int) {
            VarPlayerIntMapSetter.set(player, "varbit.waterfall_progress", value)
        }

        fun stage() = quest.stage(player)

        fun access() = ProtectedAccess(player, coroutine, context)

        fun count(obj: String) = player.inv.count(obj)

        fun give(obj: String, count: Int = 1) {
            val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)))
            if (type.stackable) {
                player.inv[freeSlot()] = InvObj(obj, count)
            } else {
                repeat(count) { player.inv[freeSlot()] = InvObj(obj, 1) }
            }
        }

        fun fillInventory(free: Int = 0) {
            var left = free
            for (slot in 0 until 28) {
                if (player.inv[slot] == null) {
                    if (left > 0) {
                        left--
                    } else {
                        player.inv[slot] = InvObj("obj.logs", 1)
                    }
                }
            }
        }

        fun ground(obj: String): Int =
            objRegistry.findAll(player.coords).filter { it.type == obj.asRSCM() }.sumOf { it.count }

        fun total(obj: String): Int = count(obj) + ground(obj)

        fun talk(symbol: String) {
            val npc = Npc(symbol, player.coords.translateZ(1))
            run { assertTrue(events.publish(this, NpcEvents.Op1(npc))) }
        }

        private fun boundLoc(
            symbol: String,
            coords: CoordGrid,
            shape: LocShape,
            angle: LocAngle,
        ): Pair<BoundLocInfo, dev.openrune.types.ObjectServerType> {
            val type = checkNotNull(ServerCacheManager.getObject(symbol.asRSCM(RSCMType.LOC)))
            val loc =
                BoundLocInfo(
                    LocInfo(if (shape == LocShape.WallStraight) 0 else 2, coords, LocEntity(type.id, shape.id, angle.id)),
                    type,
                )
            return loc to type
        }

        fun loc(
            symbol: String,
            coords: CoordGrid,
            slot: Int = 1,
            shape: LocShape = LocShape.CentrepieceStraight,
            angle: LocAngle = LocAngle.West,
        ) {
            val (loc, type) = boundLoc(symbol, coords, shape, angle)
            run {
                val event =
                    if (slot == 2) LocEvents.Op2(loc, loc, type) else LocEvents.Op1(loc, loc, type)
                assertTrue(events.publish(this, event))
            }
        }

        fun useOnLoc(
            symbol: String,
            coords: CoordGrid,
            obj: String,
            handled: Boolean = true,
            absent: Boolean = false,
        ) {
            val (loc, type) = boundLoc(symbol, coords, LocShape.CentrepieceStraight, LocAngle.West)
            val item = checkNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)))
            val slot = if (absent) 0 else (0 until 28).first { player.inv[it]?.id == item.id }
            run {
                val event = LocUEvents.Op(loc, loc, type, item, slot)
                assertEquals(handled, events.publish(this, event), "$obj on $symbol")
            }
        }

        fun read(obj: String) {
            val slot = (0 until 28).first { player.inv[it]?.id == obj.asRSCM() }
            val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
            run {
                val event = HeldObjEvents.Op1(slot, checkNotNull(player.inv[slot]), type, player.inv)
                assertTrue(events.publish(this, event))
            }
        }

        fun run(block: suspend ProtectedAccess.() -> Unit) {
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

        fun until(predicate: () -> Boolean) {
            repeat(400) {
                if (predicate()) return
                advance(emptyList<Int>().iterator())
            }
            fail<Unit>("Condition was not reached: ${output()}")
        }

        fun finish(options: List<Int> = emptyList()) {
            val selections = options.iterator()
            repeat(400) {
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

        fun assertRewards() {
            assertEquals(COMPLETE, stage())
            assertEquals(COMPLETE, player.vars["varp.waterfall_quest"])
            assertEquals(1, player.vars["varp.qp"])
            assertEquals(13750, player.statMap.getXP("stat.attack"))
            assertEquals(13750, player.statMap.getXP("stat.strength"))
            assertEquals(2, count("obj.diamond"))
            assertEquals(2, count("obj.gold_bar"))
            assertEquals(40, count("obj.mithril_seed"))
        }

        fun output() = client.messages.joinToString("\n").replace("<br>", " ")
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
        private val GOLRIE_NPC = WaterfallQuest.GOLRIE_NPC


        private const val BOOKCASE = "loc.bookcase_waterfall_quest"
        private const val GOLRIE_CRATE = "loc.golrie_crate_waterfall_quest"
        private const val TOMBSTONE = "loc.glarials_tombstone_waterfall_quest"
        private const val CHEST_OPEN = "loc.glarials_chest_open_waterfall_quest"
        private const val COFFIN = "loc.glarials_tomb_waterfall_quest"
        private const val CRATE = "loc.baxtorian_crate_waterfall_quest"
        private const val TOMB_DOOR = "loc.baxtorian_door_2_waterfall_quest"
        private const val PILLAR = "loc.stonepillar_small_waterfall_quest_op"
        private const val STATUE = "loc.statue_queen_waterfall_quest"
        private const val CHALICE = "loc.baxtorian_chalice_waterfall_quest"

        private val RAFT_BANK = CoordGrid(2510, 3492, 0)
        private val BOOKCASE_COORDS = CoordGrid(2520, 3426, 1)

        private val GOLRIE_CRATE_COORDS = CoordGrid(2548, 9565, 0)
        private val TOMBSTONE_COORDS = CoordGrid(2558, 3444, 0)
        private val TOMBSTONE_BANK = CoordGrid(2558, 3443, 0)
        private val CHEST_COORDS = CoordGrid(2530, 9844, 0)
        private val COFFIN_COORDS = CoordGrid(2542, 9811, 0)
        private val CRATE_COORDS = CoordGrid(2589, 9888, 0)
        private val TOMB_DOOR_COORDS = CoordGrid(2568, 9893, 0)
        private val TOMB_DOOR_SOUTH = CoordGrid(2568, 9892, 0)
        private val ROOM_CENTER = CoordGrid(2565, 9908, 0)
        private val STATUE_COORDS = CoordGrid(2565, 9916, 0)
        private val CHALICE_COORDS = CoordGrid(2603, 9910, 0)
        private val PILLAR_COORDS =
            listOf(2562, 2569).flatMap { x -> listOf(9910, 9912, 9914).map { z -> CoordGrid(x, z, 0) } }

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
