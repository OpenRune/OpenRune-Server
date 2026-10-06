package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
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
import org.rsmod.content.quest.area.ardougne.QuestDoors
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Amulet
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.BaxtorianKey
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Book
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Complete
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.EnteredFalls
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.EnteredTomb
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.FloorRisen
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.GolrieKey
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.MetHudon
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Pebble
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.ReadBook
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Rope
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.RunesPlaced
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Started
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.UrnEmpty
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.UrnFull
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs.Almera
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs.Gerald
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs.Golrie
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs.Hadley
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs.Hudon
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
    fun `accepting Almera's request starts the quest`() {
        val f = Fixture()
        f.talk(AlmeraNpc)
        f.finish(listOf(1))
        assertEquals(Started, f.stage())
        assertEquals(Started, f.player.vars["varp.waterfall_quest"])
        assertTrue(f.output().contains("Take the little raft out the back"), f.output())
        assertTrue(f.output().contains("recommended level of 25"), f.output())
        assertTrue(f.journal().contains("log raft"), f.journal())
    }

    @Test
    fun `declining leaves the quest unstarted and a started quest is not restarted`() {
        val declined = Fixture()
        declined.talk(AlmeraNpc)
        declined.finish(listOf(2))
        assertEquals(0, declined.stage())
        assertTrue(declined.output().contains("Never mind then"), declined.output())

        val started = Fixture(Started)
        started.talk(AlmeraNpc)
        started.finish()
        assertEquals(Started, started.stage())
        assertTrue(started.output().contains("Any sign of my boy"), started.output())
    }

    @Test
    fun `Almera's talk changes with the stage and after the quest`() {
        val expected =
            mapOf(
                MetHudon to "tourist centre south of the waterfall",
                ReadBook to "turn this whole valley into a mine",
                EnteredTomb to "How is the treasure hunting going",
                EnteredFalls to "How is the treasure hunting going",
                Complete to "try not to wreck it again",
            )
        for ((stage, text) in expected) {
            val f = Fixture(stage)
            f.talk(AlmeraNpc)
            f.finish()
            assertTrue(f.output().contains(text), "stage $stage: ${f.output()}")
            assertEquals(stage, f.stage())
        }
    }

    @Test
    fun `the raft is refused before the quest and carries the player to the island after`() {
        val before = Fixture()
        before.loc(Raft, RaftCoords)
        before.finish()
        assertEquals(RaftBank, before.player.coords)
        assertTrue(before.output().contains("doesn't look very safe"), before.output())

        val f = Fixture(Started)
        f.addNpc(HudonNpc, HudonCoords)
        f.loc(Raft, RaftCoords)
        f.finish()
        assertEquals(WaterfallCoords.RAFT_CRASH, f.player.coords)
        assertTrue(f.output().contains("push off down stream"), f.output())
        assertTrue(f.output().contains("you crash into a small island"), f.output())
        assertTrue(f.output().contains("Looks to me like you're the one who needs a hand"))
        assertEquals(MetHudon, f.stage())
    }

    @Test
    fun `the raft keeps working after the quest`() {
        val f = Fixture(Complete)
        f.loc(Raft, RaftCoords)
        f.finish()
        assertEquals(WaterfallCoords.RAFT_CRASH, f.player.coords)
        assertEquals(Complete, f.stage())
    }

    @Test
    fun `Hudon only advances the quest once the whole conversation is heard`() {
        val early = Fixture(Started)
        early.player.coords = WaterfallCoords.RAFT_CRASH
        early.talk(HudonNpc)
        early.until { early.output().contains("I don't need any help") }
        early.cancel()
        assertEquals(Started, early.stage())

        val full = Fixture(Started)
        full.player.coords = WaterfallCoords.RAFT_CRASH
        full.talk(HudonNpc)
        full.finish()
        assertEquals(MetHudon, full.stage())
        assertTrue(full.output().contains("Hmm... I wonder what this treasure could be"))
        assertTrue(full.journal().contains("Hadley"), full.journal())
    }

    @Test
    fun `Hudon cannot be heard from the bank and says more as the quest goes on`() {
        val bank = Fixture(Started)
        bank.talk(HudonNpc)
        bank.finish()
        assertEquals(Started, bank.stage())
        assertTrue(bank.output().contains("roar of the waterfall"), bank.output())

        val expected =
            mapOf(
                0 to "Go away, I'm busy",
                MetHudon to "I'll find that treasure any day now",
                ReadBook to "swept me downstream three times",
                EnteredTomb to "Because I'm the one who told you about it",
                EnteredFalls to "Nothing yet",
                Complete to "You took my treasure",
            )
        for ((stage, text) in expected) {
            val f = Fixture(stage)
            f.player.coords = WaterfallCoords.RAFT_CRASH
            f.talk(HudonNpc)
            f.finish()
            assertTrue(f.output().contains(text), "stage $stage: ${f.output()}")
        }
    }

    @Test
    fun `swimming sweeps the player to Gerald who tells of the treasure once`() {
        val f = Fixture(MetHudon)
        f.player.coords = WaterfallCoords.RAFT_CRASH
        f.loc(River, RiverCoords)
        f.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, f.player.coords)
        assertTrue(f.player.heardOfTreasure)
        assertTrue(f.output().contains("Try Hadley"), f.output())

        f.player.coords = WaterfallCoords.RAFT_CRASH
        val before = f.output().split("Try Hadley").size
        f.loc(River, RiverCoords)
        f.finish()
        assertEquals(before, f.output().split("Try Hadley").size)

        val early = Fixture(Started)
        early.player.coords = WaterfallCoords.RAFT_CRASH
        early.loc(River, RiverCoords)
        early.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, early.player.coords)
        assertFalse(early.player.heardOfTreasure)
    }

    @Test
    fun `Gerald points treasure hunters to Hadley and stays chatty outside the quest`() {
        val idle = Fixture()
        idle.talk(GeraldNpc)
        idle.finish()
        assertTrue(idle.output().contains("THIS big"), idle.output())

        val searching = Fixture(Started)
        searching.talk(GeraldNpc)
        searching.finish()
        assertTrue(searching.output().contains("Plenty of young fish"), searching.output())

        val hunter = Fixture(MetHudon)
        hunter.talk(GeraldNpc)
        hunter.finish()
        assertTrue(hunter.player.heardOfTreasure)
        assertTrue(hunter.output().contains("Try Hadley"), hunter.output())
        hunter.talk(GeraldNpc)
        hunter.finish()
        assertTrue(hunter.output().contains("None of them ever find a thing"), hunter.output())
    }

    @Test
    fun `Hadley tells the tourist story and treasure story`() {
        val tourist = Fixture()
        tourist.talk(HadleyNpc)
        tourist.finish(listOf(1, 4))
        assertTrue(tourist.output().contains("He died a very long time ago"), tourist.output())
        assertTrue(tourist.output().contains("Enjoy your stay"), tourist.output())

        val hunter = Fixture(MetHudon)
        hunter.talk(HadleyNpc)
        hunter.finish(listOf(3, 2, 4))
        assertTrue(hunter.output().contains("nobody has ever reached it"), hunter.output())
        assertTrue(hunter.output().contains("Who was Glarial"), hunter.output())

        val booked = Fixture(MetHudon)
        booked.give(Book)
        booked.talk(HadleyNpc)
        booked.finish(listOf(4))
        assertTrue(booked.output().contains("do give it a read"), booked.output())
    }

    @Test
    fun `the book is only found once Hudon has been met and reading it moves the quest on`() {
        val early = Fixture(Started)
        early.loc(Bookcase, BookcaseCoords)
        early.finish()
        assertEquals(0, early.count(Book))

        val f = Fixture(MetHudon)
        f.loc(Bookcase, BookcaseCoords)
        f.finish()
        assertEquals(1, f.count(Book))
        assertTrue(f.output().contains("Book on Baxtorian"), f.output())
        f.loc(Bookcase, BookcaseCoords)
        f.finish()
        assertEquals(1, f.count(Book))

        f.read(Book)
        f.until { f.stage() == ReadBook }
        assertTrue(f.player.ui.containsModal("interface.book"))
        assertTrue(f.journal().contains("Glarial's pebble"), f.journal())
    }

    @Test
    fun `Golrie's key turns up in the crate only after the book was read`() {
        val early = Fixture(MetHudon)
        early.loc(GolrieCrate, GolrieCrateCoords)
        early.finish()
        assertEquals(0, early.count(GolrieKey))

        val f = Fixture(ReadBook)
        f.loc(GolrieCrate, GolrieCrateCoords)
        f.finish()
        assertEquals(1, f.count(GolrieKey))
        f.loc(GolrieCrate, GolrieCrateCoords)
        f.finish()
        assertEquals(1, f.count(GolrieKey))
    }

    @Test
    fun `Golrie's gate talks without the key and unlocks with it`() {
        val none = Fixture(ReadBook)
        none.player.coords = GateSouth
        none.loc(GolrieGate, GolrieGateCoords, shape = LocShape.WallStraight, angle = LocAngle.North)
        none.finish()
        assertTrue(none.output().contains("lost the key somewhere"), none.output())

        val unread = Fixture(MetHudon)
        unread.player.coords = GateSouth
        unread.loc(GolrieGate, GolrieGateCoords, shape = LocShape.WallStraight, angle = LocAngle.North)
        unread.finish()
        assertTrue(unread.output().contains("Leave before you land yourself in trouble"))

        val keyed = Fixture(ReadBook)
        keyed.player.coords = GateSouth
        keyed.give(GolrieKey)
        keyed.loc(GolrieGate, GolrieGateCoords, shape = LocShape.WallStraight, angle = LocAngle.North)
        keyed.finish()
        assertTrue(keyed.output().contains("You unlock the gate with the key."), keyed.output())
        assertEquals(1, keyed.count(GolrieKey))

        val inside = Fixture(ReadBook)
        inside.player.coords = GateSouth.translateZ(2)
        inside.loc(GolrieGate, GolrieGateCoords, shape = LocShape.WallStraight, angle = LocAngle.North)
        inside.finish()
        assertFalse(inside.output().contains("lost the key"), inside.output())
    }

    @Test
    fun `Golrie takes the key back and hands over the pebble`() {
        val f = Fixture(ReadBook)
        f.give(GolrieKey)
        f.talk(GolrieNpc)
        f.finish()
        assertEquals(1, f.count(Pebble))
        assertEquals(0, f.count(GolrieKey))
        assertTrue(f.player.metGolrie)
        assertTrue(f.output().contains("Could I have this old pebble"), f.output())

        f.talk(GolrieNpc)
        f.finish()
        assertEquals(1, f.count(Pebble))
        assertTrue(f.output().contains("Had any luck getting out"), f.output())

        f.player.inv[f.player.inv.indexOfFirst { it?.id == Pebble.asRSCM() }] = null
        f.talk(GolrieNpc)
        f.finish()
        assertEquals(1, f.count(Pebble))
        assertTrue(f.output().contains("another rummage"), f.output())
    }

    @Test
    fun `Golrie does not give the pebble early and a full inventory does not lose it`() {
        val early = Fixture(MetHudon)
        early.talk(GolrieNpc)
        early.finish()
        assertEquals(0, early.count(Pebble))
        assertFalse(early.player.metGolrie)
        assertTrue(early.output().contains("Leave before you land yourself in trouble"))

        val full = Fixture(ReadBook)
        full.fillInventory()
        full.talk(GolrieNpc)
        full.finish()
        assertEquals(0, full.count(Pebble))
        assertTrue(full.output().contains("you don't have room"), full.output())
        assertFalse(full.player.metGolrie)
    }

    @Test
    fun `the tombstone stays shut for the armed and opens for the peaceful`() {
        for (carried in listOf("obj.bronze_sword", "obj.airrune", "obj.logs", "obj.bronze_arrow")) {
            val f = Fixture(ReadBook)
            f.player.coords = TombstoneBank
            f.give(Pebble)
            f.give(carried)
            f.useOnLoc(Tombstone, TombstoneCoords, Pebble)
            f.finish()
            assertEquals(TombstoneBank, f.player.coords, carried)
            assertEquals(ReadBook, f.stage(), carried)
            assertTrue(f.output().contains("nothing happens"), "$carried ${f.output()}")
        }

        val worn = Fixture(ReadBook)
        worn.player.coords = TombstoneBank
        worn.give(Pebble)
        worn.player.worn[3] = InvObj("obj.bronze_sword", 1)
        worn.useOnLoc(Tombstone, TombstoneCoords, Pebble)
        worn.finish()
        assertEquals(TombstoneBank, worn.player.coords)

        val peaceful = Fixture(ReadBook)
        peaceful.player.coords = TombstoneBank
        peaceful.give(Pebble)
        peaceful.give("obj.swordfish")
        peaceful.give("obj.coins", 100)
        peaceful.useOnLoc(Tombstone, TombstoneCoords, Pebble)
        peaceful.finish()
        assertEquals(WaterfallCoords.TOMB_ENTRY, peaceful.player.coords)
        assertEquals(EnteredTomb, peaceful.stage())
        assertEquals(1, peaceful.count(Pebble))
    }

    @Test
    fun `the tombstone can be read and entering the tomb never starts the quest`() {
        val f = Fixture()
        f.loc(Tombstone, TombstoneCoords)
        f.finish()
        assertTrue(f.output().contains("Only those who come in peace"), f.output())

        f.give(Pebble)
        f.useOnLoc(Tombstone, TombstoneCoords, Pebble)
        f.finish()
        assertEquals(0, f.stage())
    }

    @Test
    fun `the tomb chest and coffin give the amulet and urn once each`() {
        val f = Fixture(EnteredTomb)
        f.loc(ChestOpen, ChestCoords)
        f.finish()
        assertEquals(1, f.count(Amulet))
        f.loc(ChestOpen, ChestCoords)
        f.finish()
        assertEquals(1, f.count(Amulet))
        assertTrue(f.output().contains("find nothing"), f.output())

        f.loc(Coffin, CoffinCoords)
        f.finish()
        assertEquals(1, f.count(UrnFull))
        f.loc(Coffin, CoffinCoords)
        f.finish()
        assertEquals(1, f.count(UrnFull))
    }

    @Test
    fun `the chest gives a replacement amulet once the first one is gone`() {
        val f = Fixture(EnteredFalls)
        f.give(Amulet)
        f.loc(ChestOpen, ChestCoords)
        f.finish()
        assertEquals(1, f.count(Amulet))

        f.player.inv[f.player.inv.indexOfFirst { it?.id == Amulet.asRSCM() }] = null
        f.loc(ChestOpen, ChestCoords)
        f.finish()
        assertEquals(1, f.count(Amulet))

        val worn = Fixture(EnteredFalls)
        worn.player.worn[2] = InvObj(Amulet, 1)
        worn.loc(ChestOpen, ChestCoords)
        worn.finish()
        assertEquals(0, worn.count(Amulet))
    }

    @Test
    fun `climbing the tree without a rope drops the player in the river`() {
        val f = Fixture(EnteredTomb)
        f.player.coords = WaterfallCoords.TREE_ISLAND
        f.loc(Tree, TreeCoords)
        f.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, f.player.coords)
        assertTrue(f.output().contains("lose your grip"), f.output())
    }

    @Test
    fun `the rope on the rock and then the tree leads to the ledge`() {
        val f = Fixture(EnteredTomb)
        f.player.coords = WaterfallCoords.RAFT_CRASH
        f.give(Rope)
        f.useOnLoc(Rock, RockCoords, Rope)
        f.finish()
        assertEquals(WaterfallCoords.TREE_ISLAND, f.player.coords)
        assertEquals(1, f.count(Rope))

        f.useOnLoc(Tree, TreeCoords, Rope)
        f.finish()
        assertEquals(WaterfallCoords.LEDGE, f.player.coords)
        assertEquals(1, f.count(Rope))
    }

    @Test
    fun `the barrel brings the player back down the river`() {
        val f = Fixture(EnteredTomb)
        f.player.coords = WaterfallCoords.LEDGE
        f.loc(Barrel, BarrelCoords)
        f.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, f.player.coords)
    }

    @Test
    fun `the waterfall door needs the amulet in the pack or on the neck`() {
        val none = Fixture(EnteredTomb)
        none.player.coords = WaterfallCoords.LEDGE
        none.loc(LedgeDoor, LedgeDoorCoords, shape = LocShape.CentrepieceStraight)
        none.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, none.player.coords)
        assertEquals(EnteredTomb, none.stage())
        assertTrue(none.output().contains("pushes you over the waterfall"), none.output())

        val packed = Fixture(EnteredTomb)
        packed.player.coords = WaterfallCoords.LEDGE
        packed.give(Amulet)
        packed.loc(LedgeDoor, LedgeDoorCoords, shape = LocShape.CentrepieceStraight)
        packed.finish()
        assertEquals(WaterfallCoords.FALLS_ENTRY, packed.player.coords)
        assertEquals(EnteredFalls, packed.stage())

        val worn = Fixture(EnteredTomb)
        worn.player.coords = WaterfallCoords.LEDGE
        worn.player.worn[2] = InvObj(Amulet, 1)
        worn.loc(LedgeDoor, LedgeDoorCoords, shape = LocShape.CentrepieceStraight)
        worn.finish()
        assertEquals(WaterfallCoords.FALLS_ENTRY, worn.player.coords)
        assertEquals(EnteredFalls, worn.stage())
    }

    @Test
    fun `after the quest the falls open without the amulet and the stage stays`() {
        val f = Fixture(Complete)
        f.player.coords = WaterfallCoords.LEDGE
        f.loc(LedgeDoor, LedgeDoorCoords, shape = LocShape.CentrepieceStraight)
        f.finish()
        assertEquals(WaterfallCoords.FALLS_ENTRY, f.player.coords)
        assertEquals(Complete, f.stage())
    }

    @Test
    fun `the exit door leads back to the ledge`() {
        val f = Fixture(EnteredFalls)
        f.player.coords = WaterfallCoords.FALLS_ENTRY
        f.loc(ExitDoor, ExitDoorCoords, shape = LocShape.WallStraight, angle = LocAngle.South)
        f.finish()
        assertEquals(WaterfallCoords.LEDGE, f.player.coords)
    }

    @Test
    fun `the dungeon crate holds one key and the locked doors need it from the south`() {
        val f = Fixture(EnteredFalls)
        f.loc(Crate, CrateCoords, angle = LocAngle.North)
        f.finish()
        assertEquals(1, f.count(BaxtorianKey))
        f.loc(Crate, CrateCoords, angle = LocAngle.North)
        f.finish()
        assertEquals(1, f.count(BaxtorianKey))

        val locked = Fixture(EnteredFalls)
        locked.player.coords = TombDoorSouth
        locked.loc(TombDoor, TombDoorCoords, shape = LocShape.WallStraight, angle = LocAngle.North)
        locked.finish()
        assertTrue(locked.output().contains("The door is locked."), locked.output())

        val keyed = Fixture(EnteredFalls)
        keyed.player.coords = TombDoorSouth
        keyed.give(BaxtorianKey)
        keyed.loc(TombDoor, TombDoorCoords, shape = LocShape.WallStraight, angle = LocAngle.North)
        keyed.finish()
        assertTrue(keyed.output().contains("You use the key to unlock the door."), keyed.output())
        assertEquals(1, keyed.count(BaxtorianKey))

        val inside = Fixture(EnteredFalls)
        inside.player.coords = TombDoorSouth.translateZ(2)
        inside.loc(TombDoor, TombDoorCoords, shape = LocShape.WallStraight, angle = LocAngle.North)
        inside.finish()
        assertFalse(inside.output().contains("The door is locked."), inside.output())
    }

    @Test
    fun `each pillar takes one rune of each kind and the sixth charge moves the quest on`() {
        val f = Fixture(EnteredFalls)
        f.player.coords = RoomCenter
        f.give("obj.airrune", 6)
        f.give("obj.waterrune", 6)
        f.give("obj.earthrune", 6)

        f.useOnLoc(Pillar, PillarCoords[0], "obj.airrune")
        f.finish()
        assertEquals(5, f.count("obj.airrune"))
        assertEquals(1, f.player.pillarRunes)

        f.useOnLoc(Pillar, PillarCoords[0], "obj.airrune")
        f.finish()
        assertEquals(5, f.count("obj.airrune"))
        assertEquals(1, f.player.pillarRunes)
        assertTrue(f.output().contains("already placed"), f.output())

        for ((index, pillar) in PillarCoords.withIndex()) {
            for (rune in listOf("obj.airrune", "obj.waterrune", "obj.earthrune")) {
                if (index == 0 && rune == "obj.airrune") continue
                f.useOnLoc(Pillar, pillar, rune)
                f.finish()
            }
        }
        assertEquals(WaterfallQuest.AllPillarRunes, f.player.pillarRunes)
        assertEquals(0, f.count("obj.airrune") + f.count("obj.waterrune") + f.count("obj.earthrune"))
        assertEquals(RunesPlaced, f.stage())
        assertTrue(f.quest.allRunesPlaced(f.player))
        assertTrue(f.journal().contains("place <red>Glarial's amulet"), f.journal())
    }

    @Test
    fun `other runes and other pillars are not accepted`() {
        val f = Fixture(EnteredFalls)
        f.player.coords = RoomCenter
        f.give("obj.firerune", 2)
        f.useOnLoc(Pillar, PillarCoords[0], "obj.firerune", handled = false)
        f.finish()
        assertEquals(2, f.count("obj.firerune"))
        assertEquals(0, f.player.pillarRunes)
    }

    @Test
    fun `the amulet before the pillars are charged floods the room and is lost`() {
        val f = Fixture(EnteredFalls)
        f.player.coords = RoomCenter
        f.give(Amulet)
        f.useOnLoc(Statue, StatueCoords, Amulet)
        f.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, f.player.coords)
        assertEquals(0, f.count(Amulet))
        assertEquals(EnteredFalls, f.stage())
    }

    @Test
    fun `the amulet on the statue raises the floor and only then can the urn be poured`() {
        val f = Fixture(RunesPlaced)
        f.player.pillarRunes = WaterfallQuest.AllPillarRunes
        f.player.coords = RoomCenter
        f.give(Amulet)
        f.give(UrnFull)
        f.useOnLoc(Statue, StatueCoords, Amulet)
        f.finish()
        assertEquals(RoomCenter.translate(38, -1), f.player.coords)
        assertEquals(0, f.count(Amulet))
        assertEquals(FloorRisen, f.stage())
        assertTrue(f.journal().contains("pouring her ashes"), f.journal())

        f.give(Amulet)
        f.useOnLoc(Statue, StatueCoords.translate(38, -1), Amulet)
        f.finish()
        assertEquals(FloorRisen, f.stage())

        f.useOnLoc(Chalice, ChaliceCoords, UrnFull)
        f.finish()
        f.assertRewards()
        assertEquals(1, f.count(UrnEmpty))
        assertEquals(0, f.count(UrnFull))
        assertTrue(f.player.ui.containsModal("interface.questscroll"))
    }

    @Test
    fun `the urn cannot be poured before the floor has risen`() {
        val f = Fixture(RunesPlaced)
        f.player.coords = RoomCenter
        f.give(UrnFull)
        f.useOnLoc(Chalice, ChaliceCoords, UrnFull)
        f.finish()
        assertEquals(RunesPlaced, f.stage())
        assertEquals(1, f.count(UrnFull))
        assertTrue(f.output().contains("can't reach the chalice"), f.output())
    }

    @Test
    fun `taking the treasure without the ashes floods the room and keeps the urn`() {
        val f = Fixture(FloorRisen)
        f.player.coords = RoomCenter.translate(38, -1)
        f.give(UrnFull)
        f.loc(Chalice, ChaliceCoords)
        f.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, f.player.coords)
        assertEquals(FloorRisen, f.stage())
        assertEquals(1, f.count(UrnFull))
        assertEquals(0, f.player.vars["varp.qp"])
    }

    @Test
    fun `a full inventory cannot pour the urn and loses nothing`() {
        val f = Fixture(FloorRisen)
        f.player.coords = RoomCenter.translate(38, -1)
        f.give(UrnFull)
        f.fillInventory()
        f.useOnLoc(Chalice, ChaliceCoords, UrnFull)
        f.finish()
        assertEquals(FloorRisen, f.stage())
        assertEquals(1, f.count(UrnFull))
        assertTrue(f.output().contains("5 free inventory spaces"), f.output())
        assertEquals(0, f.player.vars["varp.qp"])
    }

    @Test
    fun `the rewards come exactly once and the chalice is empty afterwards`() {
        val f = Fixture(FloorRisen)
        f.player.coords = RoomCenter.translate(38, -1)
        f.give(UrnFull)
        f.useOnLoc(Chalice, ChaliceCoords, UrnFull)
        f.finish()
        f.assertRewards()

        f.give(UrnFull)
        f.useOnLoc(Chalice, ChaliceCoords, UrnFull)
        f.finish()
        f.loc(Chalice, ChaliceCoords)
        f.finish()
        f.assertRewards()
        assertEquals(1, f.count(UrnFull))
        assertTrue(f.output().contains("only contains some old ashes"), f.output())
        assertTrue(f.quest.completedLog(f.access()).contains("chalice of eternity"))
    }

    @Test
    fun `leaving the raised room through its doors lands in the corridor of the real room`() {
        val f = Fixture(FloorRisen)
        f.player.coords = CoordGrid(2604, 9901, 0)
        f.loc(TombDoor, CoordGrid(2604, 9900, 0), shape = LocShape.WallStraight, angle = LocAngle.North)
        f.finish()
        assertEquals(CoordGrid(2566, 9901, 0), f.player.coords)
    }

    @Test
    fun `the journal follows every stage`() {
        val f = Fixture()
        val expected =
            mapOf(
                Started to "log raft",
                MetHudon to "Hadley",
                ReadBook to "Glarial's pebble",
                EnteredTomb to "amulet",
                EnteredFalls to "A key somewhere",
                RunesPlaced to "The pillars are charged",
                FloorRisen to "pouring her ashes",
            )
        for ((stage, text) in expected) {
            f.stageTo(stage)
            assertTrue(f.journal().contains(text), "stage $stage: ${f.journal()}")
        }
        f.stageTo(Complete)
        assertTrue(f.quest.completedLog(f.access()).contains("Almera asked me"))
    }

    @Test
    fun `the quest ends at the stage the cache expects`() {
        assertEquals(Complete, Fixture().quest.quest.maxSteps)
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
                )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 1465L
                observerUUID = 1465L
                slotId = 1
                assignUid()
                coords = RaftBank
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
            with(Almera(quest)) { scripts.startup() }
            with(Hudon(quest)) { scripts.startup() }
            with(Gerald(quest)) { scripts.startup() }
            with(Hadley(quest)) { scripts.startup() }
            with(Golrie(quest, objs)) { scripts.startup() }
            with(BookOnBaxtorian(quest, objs)) { scripts.startup() }
            with(GnomeVillageDungeon(quest, objs, doors)) { scripts.startup() }
            with(GlarialsTomb(quest, locs, objs)) { scripts.startup() }
            with(WaterfallDungeon(quest, objs, world, doors)) { scripts.startup() }
            with(BaxtorianFalls(quest, search)) { scripts.startup() }
            stageTo(stage)
        }

        fun stageTo(value: Int) {
            VarPlayerIntMapSetter.set(player, "varbit.waterfall_progress", value)
        }

        fun stage() = quest.stage(player)

        fun access() = ProtectedAccess(player, coroutine, context)

        fun journal() = quest.questLog(access())

        fun count(obj: String) = player.inv.count(obj)

        fun addNpc(symbol: String, coords: CoordGrid) {
            npcs.add(Npc(symbol, coords), 100)
        }

        fun give(obj: String, count: Int = 1) {
            val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)))
            if (type.stackable) {
                player.inv[freeSlot()] = InvObj(obj, count)
            } else {
                repeat(count) { player.inv[freeSlot()] = InvObj(obj, 1) }
            }
        }

        fun fillInventory() {
            for (slot in 0 until 28) {
                if (player.inv[slot] == null) {
                    player.inv[slot] = InvObj("obj.logs", 1)
                }
            }
        }

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

        fun useOnLoc(symbol: String, coords: CoordGrid, obj: String, handled: Boolean = true) {
            val (loc, type) = boundLoc(symbol, coords, LocShape.CentrepieceStraight, LocAngle.West)
            val item = checkNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)))
            val slot = (0 until 28).first { player.inv[it]?.id == item.id }
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

        fun cancel() {
            coroutine.cancel()
            assertInstanceOf(CancellationException::class.java, result?.exceptionOrNull())
            result = null
            player.activeCoroutine = null
        }

        fun assertRewards() {
            assertEquals(Complete, stage())
            assertEquals(Complete, player.vars["varp.waterfall_quest"])
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
        private val AlmeraNpc = WaterfallQuest.AlmeraNpc
        private val HudonNpc = WaterfallQuest.HudonNpc
        private val GeraldNpc = WaterfallQuest.GeraldNpc
        private val HadleyNpc = WaterfallQuest.HadleyNpc
        private val GolrieNpc = WaterfallQuest.GolrieNpc

        private const val Raft = "loc.lograft_waterfall_quest"
        private const val River = "loc.waterfall_swim_point"
        private const val Rock = "loc.crossing_rock_waterfall_quest"
        private const val Tree = "loc.overhanging_tree1_waterfall_quest"
        private const val LedgeDoor = "loc.waterfall_ledge_door"
        private const val Barrel = "loc.barrel_waterfall_quest"
        private const val Bookcase = "loc.bookcase_waterfall_quest"
        private const val GolrieCrate = "loc.golrie_crate_waterfall_quest"
        private const val GolrieGate = "loc.golrie_gate_waterfall_quest"
        private const val Tombstone = "loc.glarials_tombstone_waterfall_quest"
        private const val ChestOpen = "loc.glarials_chest_open_waterfall_quest"
        private const val Coffin = "loc.glarials_tomb_waterfall_quest"
        private const val ExitDoor = "loc.baxtorian_door_waterfall_quest"
        private const val Crate = "loc.baxtorian_crate_waterfall_quest"
        private const val TombDoor = "loc.baxtorian_door_2_waterfall_quest"
        private const val Pillar = "loc.stonepillar_small_waterfall_quest_op"
        private const val Statue = "loc.statue_queen_waterfall_quest"
        private const val Chalice = "loc.baxtorian_chalice_waterfall_quest"

        private val RaftBank = CoordGrid(2510, 3492, 0)
        private val RaftCoords = CoordGrid(2509, 3493, 0)
        private val HudonCoords = CoordGrid(2511, 3484, 0)
        private val RiverCoords = CoordGrid(2512, 3475, 0)
        private val RockCoords = CoordGrid(2512, 3468, 0)
        private val TreeCoords = CoordGrid(2512, 3465, 0)
        private val LedgeDoorCoords = CoordGrid(2511, 3464, 0)
        private val BarrelCoords = CoordGrid(2512, 3463, 1)
        private val BookcaseCoords = CoordGrid(2520, 3426, 1)
        private val GolrieCrateCoords = CoordGrid(2548, 9565, 0)
        private val GolrieGateCoords = CoordGrid(2515, 9575, 0)
        private val GateSouth = CoordGrid(2515, 9574, 0)
        private val TombstoneCoords = CoordGrid(2558, 3444, 0)
        private val TombstoneBank = CoordGrid(2558, 3443, 0)
        private val ChestCoords = CoordGrid(2530, 9844, 0)
        private val CoffinCoords = CoordGrid(2542, 9811, 0)
        private val ExitDoorCoords = CoordGrid(2575, 9861, 0)
        private val CrateCoords = CoordGrid(2589, 9888, 0)
        private val TombDoorCoords = CoordGrid(2568, 9893, 0)
        private val TombDoorSouth = CoordGrid(2568, 9892, 0)
        private val RoomCenter = CoordGrid(2565, 9908, 0)
        private val StatueCoords = CoordGrid(2565, 9916, 0)
        private val ChaliceCoords = CoordGrid(2603, 9910, 0)
        private val PillarCoords =
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
