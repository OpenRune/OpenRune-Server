package org.rsmod.content.areas.misc.stronghold_of_security

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
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.random.GameRandom
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
import org.rsmod.api.repo.obj.ObjRepository
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
import org.rsmod.routefinder.loc.LocLayerConstants

private var Player.flapEmote by boolVarBit("varbit.sos_emote_flap")
private var Player.slapHeadEmote by boolVarBit("varbit.sos_emote_doh")
private var Player.ideaEmote by boolVarBit("varbit.sos_emote_idea")
private var Player.stampEmote by boolVarBit("varbit.sos_emote_stamp")
private var Player.metSolztunFlag by boolVarBit("varbit.sos_brother_found")

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class StrongholdInteractionTest {
    @Test
    fun `the question bank has thirty three valid questions`() {
        val questions = SecurityQuestions.all
        assertEquals(33, questions.size)
        for (q in questions) {
            assertEquals(1, q.answers.count { it.correct }, q.text)
            assertTrue(q.answers.all { it.text.isNotBlank() && it.response.isNotBlank() }, q.text)
            assertTrue(q.text.startsWith("") && q.answers.size in 2..3, q.text)
        }
        assertEquals(questions.size, questions.map { it.text }.toSet().size)
    }

    @Test
    fun `a correct answer moves the player through a door that stays shut`() {
        for (floor in StrongholdFloor.entries) {
            val f = Fixture(InsideDoors)
            f.doorPair(floor)
            f.ask(question = 0)
            f.op(f.find(Far, floor.face), options = listOf(3))
            assertEquals(CoordGrid(1858, 5235, 0), f.player.coords, floor.doorTitle)
            assertTrue(f.output().contains("To pass you must answer me this"), floor.doorTitle)
            assertTrue(f.output().contains("Correct!"), floor.doorTitle)
            assertTrue(f.has(Far, floor.face), floor.doorTitle)
            assertTrue(f.has(Far.translateX(1), floor.mirror), floor.doorTitle)
            assertTrue(f.locsAt(Far.translateZ(1)).isEmpty(), floor.doorTitle)
        }
    }

    @Test
    fun `a wrong answer refuses the door and explains why`() {
        for (floor in StrongholdFloor.entries) {
            val f = Fixture(InsideDoors)
            f.doorPair(floor)
            f.ask(question = 0)
            f.op(f.find(Far, floor.face), options = listOf(1))
            assertEquals(InsideDoors, f.player.coords, floor.doorTitle)
            assertTrue(f.output().contains("Wrong! Membership requires"), floor.doorTitle)
            assertTrue(f.has(Far, floor.face), floor.doorTitle)
        }
    }

    @Test
    fun `a merely decent answer does not open the door either`() {
        val f = Fixture(InsideDoors)
        f.doorPair(StrongholdFloor.War)
        f.ask(question = 0)
        f.op(f.find(Far, StrongholdFloor.War.face), options = listOf(2))
        assertEquals(InsideDoors, f.player.coords)
        assertTrue(f.output().contains("Quite good."))
    }

    @Test
    fun `either leaf of a doorway asks and lets the player through`() {
        val pestilence = StrongholdFloor.Pestilence
        val mirror = CoordGrid(1859, 5235, 0)
        val f = Fixture(CoordGrid(1859, 5236, 0))
        f.doorPair(pestilence)
        f.ask(question = 1)
        f.op(f.find(mirror, pestilence.mirror), options = listOf(1))
        assertEquals(mirror, f.player.coords)
        assertTrue(f.has(Far, pestilence.face))
        assertTrue(f.has(mirror, pestilence.mirror))
    }

    @Test
    fun `opening a door from a room of monsters is always immediate`() {
        for (floor in StrongholdFloor.entries) {
            val f = Fixture(CoordGrid(1858, 5234, 0))
            f.doorPair(floor)
            f.op(f.find(Far, floor.face))
            assertEquals(CoordGrid(1858, 5236, 0), f.player.coords, floor.doorTitle)
            assertFalse(
                f.output().contains("To pass you must answer me this"),
                floor.doorTitle,
            )
        }
    }

    @Test
    fun `leaving the space between doors the other way is questioned too`() {
        val f = Fixture(CoordGrid(1858, 5237, 0))
        f.doorPair(StrongholdFloor.War)
        f.ask(question = 0)
        f.op(f.find(Near, StrongholdFloor.War.face), options = listOf(3))
        assertEquals(CoordGrid(1858, 5239, 0), f.player.coords)
        assertTrue(f.output().contains("Correct!"))
    }

    @Test
    fun `players sometimes pass a door that is not asking`() {
        val f = Fixture(InsideDoors)
        f.doorPair(StrongholdFloor.War)
        f.skipQuestion()
        f.op(f.find(Far, StrongholdFloor.War.face))
        assertEquals(CoordGrid(1858, 5235, 0), f.player.coords)
        assertFalse(f.output().contains("To pass you must answer me this"))
    }

    @Test
    fun `claiming a floor stops its doors asking but not the others`() {
        val f = Fixture(InsideDoors)
        f.doorPair(StrongholdFloor.War)
        f.player.markClaimed(StrongholdFloor.War)
        f.op(f.find(Far, StrongholdFloor.War.face))
        assertEquals(CoordGrid(1858, 5235, 0), f.player.coords)
        assertFalse(f.output().contains("To pass you must answer me this"))

        val other = Fixture(InsideDoors)
        other.doorPair(StrongholdFloor.Famine)
        other.player.markClaimed(StrongholdFloor.War)
        other.ask(question = 0)
        other.op(other.find(Far, StrongholdFloor.Famine.face), options = listOf(1))
        assertEquals(InsideDoors, other.player.coords)
    }

    @Test
    fun `completing the stronghold ends all questioning`() {
        val f = Fixture(InsideDoors)
        f.doorPair(StrongholdFloor.Death)
        for (floor in StrongholdFloor.entries) f.player.markClaimed(floor)
        f.op(f.find(Far, StrongholdFloor.Death.face))
        assertEquals(CoordGrid(1858, 5235, 0), f.player.coords)
        assertFalse(f.output().contains("To pass you must answer me this"))
    }

    @Test
    fun `long questions are split to fit the chatbox`() {
        for (q in SecurityQuestions.all) {
            val text = "${SecurityQuestions.PREAMBLE} ${q.text}"
            with(StrongholdDoors) {
                val parts = text.splitForChatbox()
                assertTrue(parts.all { it.length <= 200 }, text)
                assertEquals(text, parts.joinToString(" "))
            }
        }
    }

    @Test
    fun `the gift of peace pays out once`() {
        val f = Fixture(CoordGrid(1907, 5223, 0))
        f.player.statMap.setBaseLevel("stat.hitpoints", 50)
        f.player.statMap.setCurrentLevel("stat.hitpoints", 10)
        f.player.statMap.setBaseLevel("stat.prayer", 40)
        f.player.statMap.setCurrentLevel("stat.prayer", 3)
        val chest = f.spawnLoc(StrongholdFloor.War.reward, CoordGrid(1907, 5222, 0))
        f.op(chest)
        assertEquals(2_000, f.player.inv.count("obj.coins"))
        assertTrue(f.player.hasClaimed(StrongholdFloor.War))
        assertTrue(f.player.flapEmote)
        assertFalse(f.player.slapHeadEmote)
        assertEquals(50, f.player.stat("stat.hitpoints"))
        assertEquals(40, f.player.stat("stat.prayer"))
        f.op(chest)
        assertEquals(2_000, f.player.inv.count("obj.coins"))
        assertTrue(f.output().contains("already claimed"))
    }

    @Test
    fun `the grain of plenty pays three thousand and teaches slap head`() {
        val f = Fixture(CoordGrid(2021, 5214, 0))
        val sack = f.spawnLoc(StrongholdFloor.Famine.reward, CoordGrid(2021, 5215, 0))
        f.op(sack)
        f.op(sack)
        assertEquals(3_000, f.player.inv.count("obj.coins"))
        assertTrue(f.player.slapHeadEmote)
        assertFalse(f.player.ideaEmote)
        assertTrue(f.player.hasClaimed(StrongholdFloor.Famine))
    }

    @Test
    fun `the box of health pays five thousand teaches idea and restores every stat`() {
        val f = Fixture(CoordGrid(2144, 5279, 0))
        f.player.statMap.setBaseLevel("stat.strength", 60)
        f.player.statMap.setCurrentLevel("stat.strength", 40)
        f.player.statMap.setBaseLevel("stat.attack", 55)
        f.player.statMap.setCurrentLevel("stat.attack", 70)
        val box = f.spawnLoc(StrongholdFloor.Pestilence.reward, CoordGrid(2144, 5280, 0))
        f.op(box)
        f.op(box)
        assertEquals(5_000, f.player.inv.count("obj.coins"))
        assertTrue(f.player.ideaEmote)
        assertFalse(f.player.flapEmote)
        assertEquals(60, f.player.stat("stat.strength"))
        assertEquals(70, f.player.stat("stat.attack"))
    }

    @Test
    fun `chests can be claimed in any order`() {
        val f = Fixture(CoordGrid(2144, 5279, 0))
        f.op(f.spawnLoc(StrongholdFloor.Pestilence.reward, CoordGrid(2144, 5280, 0)))
        assertTrue(f.player.hasClaimed(StrongholdFloor.Pestilence))
        assertFalse(f.player.hasClaimed(StrongholdFloor.War))
        assertFalse(f.player.hasClaimed(StrongholdFloor.Famine))
        assertFalse(f.player.completedStronghold())
    }

    @Test
    fun `the cradle of life teaches stamp once and offers boots every time`() {
        val f = Fixture(CoordGrid(2344, 5213, 0))
        val cradle = f.spawnLoc(StrongholdFloor.Death.reward, CoordGrid(2344, 5214, 0))
        f.op(cradle, options = listOf(3))
        assertTrue(f.player.stampEmote)
        assertTrue(f.player.hasClaimed(StrongholdFloor.Death))
        assertEquals(1, f.player.inv.count("obj.sos_boots3"))
        assertEquals(0, f.player.inv.count("obj.coins"))

        f.op(cradle, options = listOf(1))
        f.op(cradle, options = listOf(2))
        f.op(cradle, options = listOf(2))
        assertEquals(1, f.player.inv.count("obj.sos_boots3"))
        assertEquals(1, f.player.inv.count("obj.sos_boots"))
        assertEquals(2, f.player.inv.count("obj.sos_boots2"))
        assertEquals(1, f.output().split("You have unlocked the Stamp emote").size - 1)
    }

    @Test
    fun `completing all four floors completes the stronghold`() {
        val f = Fixture(CoordGrid(2344, 5213, 0))
        assertFalse(f.player.completedStronghold())
        for (floor in StrongholdFloor.entries) f.player.markClaimed(floor)
        assertTrue(f.player.completedStronghold())
    }

    @Test
    fun `portals need the reward or a high enough combat level`() {
        for ((floor, level) in
            listOf(
                StrongholdFloor.War to 26,
                StrongholdFloor.Famine to 51,
                StrongholdFloor.Pestilence to 76,
            )) {
            val low = Fixture(floor.start)
            low.player.appearance.combatLevel = level - 1
            low.op(low.spawnLoc(floor.portal, floor.start.translateZ(1)))
            assertEquals(floor.start, low.player.coords, floor.name)
            assertTrue(low.output().contains("combat level $level"), floor.name)

            val high = Fixture(floor.start)
            high.player.appearance.combatLevel = level
            high.op(high.spawnLoc(floor.portal, floor.start.translateZ(1)))
            assertEquals(floor.rewardRoom, high.player.coords, floor.name)

            val claimed = Fixture(floor.start)
            claimed.player.markClaimed(floor)
            claimed.op(claimed.spawnLoc(floor.portal, floor.start.translateZ(1)))
            assertEquals(floor.rewardRoom, claimed.player.coords, floor.name)
        }
    }

    @Test
    fun `the portal of death only opens once the cradle has been claimed`() {
        val floor = StrongholdFloor.Death
        val locked = Fixture(floor.start)
        locked.player.appearance.combatLevel = 126
        locked.op(locked.spawnLoc(floor.portal, floor.start.translateZ(1)))
        assertEquals(floor.start, locked.player.coords)
        assertTrue(locked.output().contains("claimed it"))

        val claimed = Fixture(floor.start)
        claimed.player.markClaimed(floor)
        claimed.op(claimed.spawnLoc(floor.portal, floor.start.translateZ(1)))
        assertEquals(floor.rewardRoom, claimed.player.coords)
    }

    @Test
    fun `ladders ropes and the entrance lead where the notes say`() {
        val routes =
            listOf(
                "loc.sos_dung_ent_open" to StrongholdTravel.WarArrival,
                "loc.sos_war_ladd_up" to StrongholdTravel.Surface,
                "loc.sos_war_ladd_down" to StrongholdTravel.FamineArrival,
                "loc.sos_fam_ladd_up" to StrongholdTravel.WarArrival,
                "loc.sos_fam_ladd_down" to StrongholdTravel.PestilenceArrival,
                "loc.sos_pest_ladd_up" to StrongholdTravel.FamineArrival,
                "loc.sos_pest_ladd_down" to StrongholdTravel.DeathArrival,
                "loc.sos_death_ladd_up" to StrongholdTravel.PestilenceArrival,
                "loc.sos_war_chainbottom" to StrongholdFloor.War.start,
                "loc.sos_fam_rope_up" to StrongholdFloor.Famine.start,
                "loc.sos_pest_rope_up" to StrongholdFloor.Pestilence.start,
                "loc.sos_death_rope_up" to StrongholdFloor.Death.start,
            )
        for ((loc, dest) in routes) {
            val f = Fixture(CoordGrid(1900, 5200, 0))
            f.op(f.spawnLoc(loc, CoordGrid(1900, 5201, 0)))
            assertEquals(dest, f.player.coords, loc)
        }
    }

    @Test
    fun `the bone chain in the cradle of life room leads out of the dungeon`() {
        val chain = StrongholdTravel.CradleRoomChain
        val f = Fixture(chain.translateZ(-1))
        f.op(f.spawnLoc("loc.sos_death_rope_up", chain))
        assertEquals(StrongholdTravel.Surface, f.player.coords)
    }

    @Test
    fun `searching the dead explorer gives the notes once`() {
        val f = Fixture(CoordGrid(1860, 5239, 0))
        val explorer = f.spawnLoc("loc.sos_skelly_bag", CoordGrid(1860, 5240, 0))
        f.op(explorer)
        assertEquals(1, f.player.inv.count("obj.sos_stronghold_book"))
        f.op(explorer)
        assertEquals(1, f.player.inv.count("obj.sos_stronghold_book"))
        assertTrue(f.output().contains("nothing else of use"))
    }

    @Test
    fun `the notes open as a book on the chapter list`() {
        val f = Fixture(CoordGrid(1860, 5239, 0))
        f.player.inv[0] = InvObj("obj.sos_stronghold_book", 1)
        f.startHeld("obj.sos_stronghold_book")
        assertTrue(f.player.ui.containsModal("interface.indexed_book"))
        val output = f.output()
        assertTrue(output.contains("Stronghold of Security - Notes"), output)
        for ((chapter, _) in StrongholdNotesBook.Chapters) {
            assertTrue(output.contains(chapter), chapter)
        }
        assertTrue(output.contains("there was treasure to be had,"), output)
    }

    @Test
    fun `the notes turn pages and jump to chapters`() {
        val f = Fixture(CoordGrid(1860, 5239, 0))
        f.player.inv[0] = InvObj("obj.sos_stronghold_book", 1)
        f.startHeld("obj.sos_stronghold_book")
        f.press(StrongholdNotesBook.PageRight)
        assertTrue(f.output().contains("wring a word from him about"))
        f.press(StrongholdNotesBook.FirstPage)
        f.press(StrongholdNotesBook.chapterLink(StrongholdNotesBook.FirstChapterLine + 5))
        assertTrue(f.output().contains("key to all the ladders and"))
        assertTrue(f.player.ui.containsModal("interface.indexed_book"))
    }

    @Test
    fun `Solztun chats and remembers the player`() {
        val f = Fixture(CoordGrid(2338, 5212, 0))
        f.talk("npc.sos_barb_spirit", options = listOf(1, 3))
        assertTrue(f.player.metSolztunFlag)
        assertTrue(f.output().contains("greatest barbarian explorer"))
        assertTrue(f.output().contains("Stay safe, adventurer."))
    }

    @Test
    fun `every scripted loc and npc has an op handler`() {
        val f = Fixture(CoordGrid(1900, 5200, 0))
        val locs =
            StrongholdFloor.entries.flatMap { listOf(it.face, it.mirror, it.portal, it.reward) } +
                listOf(
                    "loc.sos_dung_ent_open",
                    "loc.sos_war_ladd_up",
                    "loc.sos_war_ladd_down",
                    "loc.sos_fam_ladd_up",
                    "loc.sos_fam_ladd_down",
                    "loc.sos_pest_ladd_up",
                    "loc.sos_pest_ladd_down",
                    "loc.sos_death_ladd_up",
                    "loc.sos_war_chainbottom",
                    "loc.sos_fam_rope_up",
                    "loc.sos_pest_rope_up",
                    "loc.sos_death_rope_up",
                    "loc.sos_skelly_bag",
                )
        for (loc in locs) {
            assertTrue(f.events.contains(LocEvents.Op1::class.java, loc.asRSCM(RSCMType.LOC)), loc)
        }
        assertTrue(f.events.contains(NpcEvents.Op1::class.java, "npc.sos_barb_spirit".asRSCM()))
        assertTrue(
            f.events.contains(HeldObjEvents.Op1::class.java, "obj.sos_stronghold_book".asRSCM())
        )
    }

    private class ScriptedRandom : GameRandom {
        private val queue = ArrayDeque<Int>()

        fun enqueue(vararg values: Int) {
            queue.addAll(values.toList())
        }

        override fun of(maxExclusive: Int): Int = (queue.removeFirstOrNull() ?: 1) % maxExclusive

        override fun of(minInclusive: Int, maxInclusive: Int): Int = minInclusive

        override fun randomDouble(): Double = 0.5
    }

    private class Fixture(start: CoordGrid) {
        val events = EventBus()
        private val collision = CollisionFlagMap()
        private val clock = MapClock().apply { cycle = 100 }
        private val updates = ZoneUpdateMap()
        private val zones = LocZoneStorage()
        private val activity = ZonePlayerActivityBitSet()
        private val npcs = NpcRegistry(NpcList(), collision, events)
        private val players = PlayerRegistry(PlayerList(), collision, activity, events)
        private val normal = LocRegistryNormal(updates, collision, zones)
        private val regions =
            RegionRegistry(
                RegionListSmall(),
                RegionListLarge(),
                RegionListWorldEntity(),
                normal,
                collision,
                zones,
                npcs,
                ControllerRegistry(clock, ControllerList()),
                activity,
            )
        private val locRegistry =
            LocRegistry(zones, normal, LocRegistryRegion(updates, collision, zones, regions))
        private val locs = LocRepository(clock, locRegistry, regions)
        private val objRepo = ObjRepository(clock, ObjRegistry(updates))
        private val random = ScriptedRandom()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("stronghold-test")
        private var result: Result<Unit>? = null
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getAlignment = { TextAlignment() },
                    getCollision = { collision },
                    getRandom = { random },
                    getTeleportValidator = { PlayerTeleportValidator(emptySet()) },
                    getAreaChecker = { AreaChecker(regions, AreaIndex()) },
                    getNpcInteractions = { NpcInteractions(events) },
                )

        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 7240L
                slotId = 1
                assignUid()
                coords = start
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

        init {
            val anchors =
                listOf(start) +
                    StrongholdFloor.entries.flatMap { listOf(it.start, it.rewardRoom) } +
                    listOf(
                        StrongholdTravel.Surface,
                        StrongholdTravel.WarArrival,
                        StrongholdTravel.FamineArrival,
                        StrongholdTravel.PestilenceArrival,
                        StrongholdTravel.DeathArrival,
                        StrongholdTravel.CradleRoomChain,
                    )
            for (anchor in anchors) {
                for (dx in -16..16 step 8) for (dz in -16..16 step 8) {
                    collision.allocateIfAbsent(anchor.x + dx, anchor.z + dz, anchor.level)
                }
            }
            val script = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(StrongholdDoors(locs)) { script.startup() }
            with(StrongholdTravel()) { script.startup() }
            with(StrongholdRewards(objRepo)) { script.startup() }
            with(StrongholdNotes(objRepo)) { script.startup() }
        }

        fun ask(question: Int) = random.enqueue(1, question)

        fun skipQuestion() = random.enqueue(0)

        fun spawnLoc(
            loc: String,
            at: CoordGrid,
            angle: LocAngle = LocAngle.North,
            shape: LocShape = LocShape.CentrepieceStraight,
        ): LocInfo {
            val entity = LocEntity(loc.asRSCM(RSCMType.LOC), shape.id, angle.id)
            val info = LocInfo(LocLayerConstants.of(shape.id), at, entity)
            assertTrue(locs.add(info, Int.MAX_VALUE), "could not spawn $loc at $at")
            return info
        }

        fun doorPair(floor: StrongholdFloor) {
            for (z in listOf(5235, 5238)) {
                spawnLoc(floor.face, CoordGrid(1858, z, 0), LocAngle.North, Wall)
                spawnLoc(floor.mirror, CoordGrid(1859, z, 0), LocAngle.North, Wall)
            }
        }

        fun find(at: CoordGrid, loc: String): LocInfo =
            checkNotNull(locs.findAll(at).firstOrNull { it.id == loc.asRSCM(RSCMType.LOC) })

        fun has(at: CoordGrid, loc: String): Boolean =
            locs.findAll(at).any { it.id == loc.asRSCM(RSCMType.LOC) }

        fun locsAt(at: CoordGrid): List<LocInfo> = locs.findAll(at).toList()

        fun op(loc: LocInfo, options: List<Int> = emptyList()) {
            val baseType = checkNotNull(ServerCacheManager.getObject(loc.id))
            val bound = BoundLocInfo(loc, baseType)
            start { assertTrue(events.publish(this, LocEvents.Op1(bound, bound, baseType))) }
            finish(options)
        }

        fun startHeld(obj: String) {
            val slot = (0 until 28).first { player.inv[it]?.id == obj.asRSCM() }
            val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
            start {
                val event = HeldObjEvents.Op1(slot, checkNotNull(inv[slot]), type, inv)
                assertTrue(events.publish(this, event))
            }
            assertTrue(coroutine.isAwaiting(ResumePauseButtonInput::class))
        }

        fun press(component: String) {
            coroutine.resumeWith(ResumePauseButtonInput(component, -1))
            result?.getOrThrow()
            assertTrue(coroutine.isAwaiting(ResumePauseButtonInput::class))
        }

        fun talk(npc: String, options: List<Int> = emptyList()) {
            val target = Npc(npc, player.coords.translateZ(1))
            start { assertTrue(events.publish(this, NpcEvents.Op1(target))) }
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
            val body: suspend () -> Unit = { ProtectedAccess(player, coroutine, context).block() }
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

        private fun finish(options: List<Int>) {
            val selections = options.iterator()
            repeat(300) {
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

        fun output(): String = client.messages.joinToString("\n")
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
        private val InsideDoors = CoordGrid(1858, 5236, 0)
        private val Far = CoordGrid(1858, 5235, 0)
        private val Near = CoordGrid(1858, 5238, 0)
        private val Wall = LocShape.WallStraight

        private val restored = mutableListOf<() -> Unit>()

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
