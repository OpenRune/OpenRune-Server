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
import org.rsmod.api.player.events.interact.LocContentEvents
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
import org.rsmod.content.generic.locs.bookcases.BookcasesScript
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
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.ROPE
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.RUNES_PLACED
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.STARTED
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.URN_EMPTY
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.URN_FULL
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs.Almera
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs.Gerald
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs.Golrie
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs.Hadley
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
import org.rsmod.game.entity.player.Appearance
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
        f.talk(ALMERA_NPC)
        f.finish(listOf(1))
        assertEquals(STARTED, f.stage())
        assertEquals(STARTED, f.player.vars["varp.waterfall_quest"])
        assertTrue(f.output().contains("You can use the small raft out back"), f.output())
        assertTrue(f.output().contains("recommended level of 25"), f.output())
        assertTrue(f.journal().contains("log raft"), f.journal())
    }

    @Test
    fun `declining leaves the quest unstarted and a started quest is not restarted`() {
        val declined = Fixture()
        declined.talk(ALMERA_NPC)
        declined.finish(listOf(2))
        assertEquals(0, declined.stage())
        assertTrue(declined.output().contains("Oh okay, never mind."), declined.output())

        val started = Fixture(STARTED)
        started.talk(ALMERA_NPC)
        started.finish()
        assertEquals(STARTED, started.stage())
        assertTrue(started.output().contains("have you seen my boy yet"), started.output())
    }

    @Test
    fun `Almera's talk changes with the stage and after the quest`() {
        val expected =
            mapOf(
                MET_HUDON to "tourist centre south of the waterfall",
                READ_BOOK to "wanted to dig up this whole area for a mine",
                ENTERED_TOMB to "how's your treasure hunt going",
                ENTERED_FALLS to "how's your treasure hunt going",
                COMPLETE to "please try not crash it this time",
            )
        for ((stage, text) in expected) {
            val f = Fixture(stage)
            f.talk(ALMERA_NPC)
            f.finish()
            assertTrue(f.output().contains(text), "stage $stage: ${f.output()}")
            assertEquals(stage, f.stage())
        }
    }

    @Test
    fun `the raft is refused before the quest and carries the player to the island after`() {
        val before = Fixture()
        before.loc(RAFT, RAFT_COORDS)
        before.finish()
        assertEquals(RAFT_BANK, before.player.coords)
        assertTrue(before.output().contains(RAFT_UNSAFE), before.output())

        val f = Fixture(STARTED)
        f.addNpc(HUDON_NPC, HUDON_COORDS)
        f.loc(RAFT, RAFT_COORDS)
        f.finish()
        assertEquals(WaterfallCoords.RAFT_CRASH, f.player.coords)
        assertTrue(f.output().contains("push off down stream"), f.output())
        assertTrue(f.output().contains("you crash into a small island"), f.output())
        assertTrue(f.output().contains("It looks like you need the help"))
        assertEquals(MET_HUDON, f.stage())
    }

    @Test
    fun `the raft keeps working after the quest`() {
        val f = Fixture(COMPLETE)
        f.loc(RAFT, RAFT_COORDS)
        f.finish()
        assertEquals(WaterfallCoords.RAFT_CRASH, f.player.coords)
        assertEquals(COMPLETE, f.stage())
    }

    @Test
    fun `Hudon only advances the quest once the whole conversation is heard`() {
        val early = Fixture(STARTED)
        early.player.coords = WaterfallCoords.RAFT_CRASH
        early.talk(HUDON_NPC)
        early.until { early.output().contains("I'm fine alone") }
        early.cancel()
        assertEquals(STARTED, early.stage())

        val full = Fixture(STARTED)
        full.player.coords = WaterfallCoords.RAFT_CRASH
        full.talk(HUDON_NPC)
        full.finish()
        assertEquals(MET_HUDON, full.stage())
        assertTrue(full.output().contains("Hmm... I wonder what this treasure is."))
        assertTrue(full.journal().contains("Hadley"), full.journal())
    }

    @Test
    fun `Hudon cannot be heard from the bank and says more as the quest goes on`() {
        val bank = Fixture(STARTED)
        bank.talk(HUDON_NPC)
        bank.finish()
        assertEquals(STARTED, bank.stage())
        assertTrue(bank.output().contains(NOISE_OF_THE_WATERFALL), bank.output())

        val expected =
            mapOf(
                MET_HUDON to "I'll find that treasure soon, just you wait and see.",
                READ_BOOK to "been washed downstream three times already.",
                ENTERED_TOMB to "Because I told you about the treasure.",
                ENTERED_FALLS to "No luck yet I'm afraid.",
                COMPLETE to "You stole my treasure. I saw you!",
            )
        for ((stage, text) in expected) {
            val f = Fixture(stage)
            f.player.coords = WaterfallCoords.RAFT_CRASH
            f.talk(HUDON_NPC)
            f.finish()
            assertTrue(f.output().contains(text), "stage $stage: ${f.output()}")
        }
    }

    @Test
    fun `swimming sweeps the player to Gerald who tells of the treasure once`() {
        val f = Fixture(MET_HUDON)
        f.addNpc(GERALD_NPC, GERALD_COORDS)
        f.player.coords = WaterfallCoords.RAFT_CRASH
        f.loc(RIVER, RIVER_COORDS)
        f.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, f.player.coords)
        assertTrue(f.player.heardOfTreasure)
        assertTrue(f.output().contains("Blimey! Where did you come from?"), f.output())
        assertTrue(f.output().contains("You could ask Hadley the tourist guide"), f.output())
        assertTrue(f.output().contains("You swim out into the water..."), f.output())
        assertTrue(f.output().contains("...but the current is too strong, washing you downstream."))

        f.player.coords = WaterfallCoords.RAFT_CRASH
        val before = f.output().split("Blimey!").size
        f.loc(RIVER, RIVER_COORDS)
        f.finish()
        assertEquals(before, f.output().split("Blimey!").size)

        val early = Fixture(STARTED)
        early.addNpc(GERALD_NPC, GERALD_COORDS)
        early.player.coords = WaterfallCoords.RAFT_CRASH
        early.loc(RIVER, RIVER_COORDS)
        early.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, early.player.coords)
        assertFalse(early.player.heardOfTreasure)
    }

    @Test
    fun `only the wash-out right after Hudon starts the treasure hunter talk with Gerald`() {
        val later = Fixture(READ_BOOK)
        later.addNpc(GERALD_NPC, GERALD_COORDS)
        later.player.coords = WaterfallCoords.RAFT_CRASH
        later.loc(ROCK, ROCK_COORDS)
        later.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, later.player.coords)
        assertFalse(later.output().contains("Blimey!"), later.output())

        val rock = Fixture(MET_HUDON)
        rock.addNpc(GERALD_NPC, GERALD_COORDS)
        rock.player.coords = WaterfallCoords.RAFT_CRASH
        rock.loc(ROCK, ROCK_COORDS)
        rock.finish()
        assertTrue(rock.output().contains("Blimey! Where did you come from?"), rock.output())
        assertTrue(rock.output().contains("Treasure hunters?"), rock.output())
        assertTrue(rock.player.heardOfTreasure)
    }

    @Test
    fun `Gerald points treasure hunters to Hadley and stays chatty outside the quest`() {
        val idle = Fixture()
        idle.talk(GERALD_NPC)
        idle.finish()
        assertTrue(idle.output().contains("The last one was this big!"), idle.output())

        val searching = Fixture(STARTED)
        searching.talk(GERALD_NPC)
        searching.finish()
        assertTrue(searching.output().contains("plenty of small fish though."), searching.output())

        val hunter = Fixture(MET_HUDON)
        hunter.talk(GERALD_NPC)
        hunter.finish()
        assertTrue(hunter.player.heardOfTreasure)
        assertTrue(hunter.output().contains("You could ask Hadley the tourist guide"), hunter.output())
        hunter.talk(GERALD_NPC)
        hunter.finish()
        assertTrue(hunter.output().contains("they never find anything though."), hunter.output())
    }

    @Test
    fun `Hadley tells the tourist story and treasure story`() {
        val tourist = Fixture()
        tourist.talk(HADLEY_NPC)
        tourist.finish(listOf(1, 4))
        assertTrue(tourist.output().contains("I guess he died a long long time ago"), tourist.output())
        assertTrue(tourist.output().contains("Enjoy your visit."), tourist.output())
        assertTrue(tourist.output().contains("Well hello, come in, come in"), tourist.output())
        assertTrue(tourist.output().contains("Surely pretty is an understatement, sir."))

        val hunter = Fixture(MET_HUDON)
        hunter.player.heardOfTreasure = true
        hunter.talk(HADLEY_NPC)
        hunter.finish(listOf(3, 2, 4))
        assertTrue(hunter.output().contains("Are you on holiday?"), hunter.output())
        assertTrue(hunter.output().contains("no one's been able to get to it"), hunter.output())
        assertTrue(hunter.output().contains("Who was Glarial"), hunter.output())

        val booked = Fixture(MET_HUDON)
        booked.player.heardOfTreasure = true
        booked.give(BOOK)
        booked.talk(HADLEY_NPC)
        booked.finish(listOf(4))
        assertTrue(booked.output().contains("Make sure you give it a read."), booked.output())
    }

    @Test
    fun `the book is only found once Hudon has been met and reading it moves the quest on`() {
        val early = Fixture(STARTED)
        early.loc(BOOKCASE, BOOKCASE_COORDS)
        early.finish()
        assertEquals(0, early.count(BOOK))

        val f = Fixture(MET_HUDON)
        f.loc(BOOKCASE, BOOKCASE_COORDS)
        f.finish()
        assertEquals(1, f.count(BOOK))
        assertTrue(f.output().contains("Book on Baxtorian"), f.output())
        f.loc(BOOKCASE, BOOKCASE_COORDS)
        f.finish()
        assertEquals(1, f.count(BOOK))

        f.read(BOOK)
        f.until { f.stage() == READ_BOOK }
        assertTrue(f.player.ui.containsModal("interface.book"))
        assertTrue(f.output().contains("<u>The Missing Relics</u>"), f.output())
        assertTrue(f.output().contains("dwarf miners recovered them"), f.output())
        assertTrue(f.output().contains("the Tree Gnome Village."), f.output())
        assertTrue(f.journal().contains("Glarial's pebble"), f.journal())
    }

    @Test
    fun `the other tourist centre bookcases hold nothing worth reading`() {
        for ((symbol, coords) in
            listOf("loc.bookcase2" to TOURIST_BOOKCASE_THIN, "loc.bookcase" to TOURIST_BOOKCASE_WIDE)) {
            val f = Fixture(MET_HUDON)
            if (symbol == "loc.bookcase") f.contentLoc(symbol, coords) else f.loc(symbol, coords)
            f.finish()
            val output = f.output()
            assertTrue(output.contains("You search the books..."), output)
            assertTrue(UNINTERESTING.count { output.contains(it) } == 1, output)
            assertFalse(output.contains("None of them look very interesting."), output)
            assertEquals(0, f.count(BOOK))
        }
        assertFalse(TouristCentreBookcases().claims(Fixture().player, bookcaseAt(OUTSIDE_BOOKCASE)))
    }

    @Test
    fun `Golrie's key turns up in the crate only after the book was read`() {
        val early = Fixture(MET_HUDON)
        early.loc(GOLRIE_CRATE, GOLRIE_CRATE_COORDS)
        early.finish()
        assertEquals(0, early.count(GOLRIE_KEY))

        val f = Fixture(READ_BOOK)
        f.loc(GOLRIE_CRATE, GOLRIE_CRATE_COORDS)
        f.finish()
        assertEquals(1, f.count(GOLRIE_KEY))
        f.loc(GOLRIE_CRATE, GOLRIE_CRATE_COORDS)
        f.finish()
        assertEquals(1, f.count(GOLRIE_KEY))
    }

    @Test
    fun `Golrie's gate talks without the key and unlocks with it`() {
        val none = Fixture(READ_BOOK)
        none.player.coords = GATE_SOUTH
        none.loc(GOLRIE_GATE, GOLRIE_GATE_COORDS, shape = LocShape.WallStraight, angle = LocAngle.North)
        none.finish()
        assertTrue(none.output().contains("Hello, are you okay?"), none.output())
        assertTrue(none.output().contains("but I've left the key somewhere."), none.output())
        assertTrue(none.output().contains("I'll have a look for a key."), none.output())

        val unstarted = Fixture()
        unstarted.player.coords = GATE_SOUTH
        unstarted.loc(GOLRIE_GATE, GOLRIE_GATE_COORDS, shape = LocShape.WallStraight, angle = LocAngle.North)
        unstarted.finish()
        assertTrue(unstarted.output().contains("Leave before you get yourself into trouble."))

        val done = Fixture(COMPLETE)
        done.player.coords = GATE_SOUTH
        done.loc(GOLRIE_GATE, GOLRIE_GATE_COORDS, shape = LocShape.WallStraight, angle = LocAngle.North)
        done.finish()
        assertTrue(done.output().contains("Golrie has locked himself in."), done.output())

        val keyed = Fixture(READ_BOOK)
        keyed.player.coords = GATE_SOUTH
        keyed.give(GOLRIE_KEY)
        keyed.loc(GOLRIE_GATE, GOLRIE_GATE_COORDS, shape = LocShape.WallStraight, angle = LocAngle.North)
        keyed.finish()
        assertTrue(keyed.output().contains("You use the key to unlock the gate."), keyed.output())
        assertEquals(1, keyed.count(GOLRIE_KEY))

        val inside = Fixture(READ_BOOK)
        inside.player.coords = GATE_SOUTH.translateZ(2)
        inside.loc(GOLRIE_GATE, GOLRIE_GATE_COORDS, shape = LocShape.WallStraight, angle = LocAngle.North)
        inside.finish()
        assertTrue(inside.output().contains("You open the gate and walk through."), inside.output())
    }

    @Test
    fun `Golrie takes the key back and hands over the pebble`() {
        val f = Fixture(READ_BOOK)
        f.give(GOLRIE_KEY)
        f.talk(GOLRIE_NPC)
        f.finish()
        assertEquals(1, f.count(PEBBLE))
        assertEquals(0, f.count(GOLRIE_KEY))
        assertTrue(f.player.metGolrie)
        assertTrue(f.output().contains("Could I take this old pebble?"), f.output())
        assertTrue(f.output().contains("You give Golrie the key."), f.output())
        assertTrue(f.output().contains("thanks a lot for the key, traveller."), f.output())

        f.talk(GOLRIE_NPC)
        f.finish()
        assertEquals(1, f.count(PEBBLE))
        assertTrue(f.output().contains("Any luck getting out?"), f.output())

        f.player.inv[f.player.inv.indexOfFirst { it?.id == PEBBLE.asRSCM() }] = null
        f.talk(GOLRIE_NPC)
        f.finish()
        assertEquals(1, f.count(PEBBLE))
        assertTrue(f.output().contains("have another look through this stuff?"), f.output())
    }

    @Test
    fun `Golrie does not give the pebble early and a full inventory does not lose it`() {
        val early = Fixture()
        early.talk(GOLRIE_NPC)
        early.finish()
        assertEquals(0, early.count(PEBBLE))
        assertFalse(early.player.metGolrie)
        assertTrue(early.output().contains("Leave before you get yourself into trouble."))

        val full = Fixture(READ_BOOK)
        full.fillInventory()
        full.talk(GOLRIE_NPC)
        full.finish()
        assertEquals(0, full.count(PEBBLE))
        assertTrue(full.output().contains("but you don't have enough room to take it."), full.output())
        assertFalse(full.player.metGolrie)
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
            assertTrue(f.output().contains(NOTHING_HAPPENS), "$carried ${f.output()}")
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
        assertTrue(peaceful.output().contains(SLAB_SLIDES), peaceful.output())
        assertEquals(WaterfallCoords.TOMB_ENTRY, peaceful.player.coords)
        assertEquals(ENTERED_TOMB, peaceful.stage())
        assertEquals(1, peaceful.count(PEBBLE))
    }

    @Test
    fun `the tombstone can be read and entering the tomb never starts the quest`() {
        val f = Fixture()
        f.loc(TOMBSTONE, TOMBSTONE_COORDS)
        f.finish()
        assertTrue(f.output().contains(TOMBSTONE_TEXT), f.output())

        f.give(PEBBLE)
        f.useOnLoc(TOMBSTONE, TOMBSTONE_COORDS, PEBBLE)
        f.finish()
        assertEquals(0, f.stage())
    }

    @Test
    fun `the tomb chest and coffin give the amulet and urn once each`() {
        val f = Fixture(ENTERED_TOMB)
        f.loc(CHEST_OPEN, CHEST_COORDS)
        f.finish()
        assertEquals(1, f.count(AMULET))
        f.loc(CHEST_OPEN, CHEST_COORDS)
        f.finish()
        assertEquals(1, f.count(AMULET))
        assertTrue(f.output().contains("find nothing"), f.output())

        f.loc(COFFIN, COFFIN_COORDS)
        f.finish()
        assertEquals(1, f.count(URN_FULL))
        f.loc(COFFIN, COFFIN_COORDS)
        f.finish()
        assertEquals(1, f.count(URN_FULL))
    }

    @Test
    fun `the chest gives a replacement amulet once the first one is gone`() {
        val f = Fixture(ENTERED_FALLS)
        f.give(AMULET)
        f.loc(CHEST_OPEN, CHEST_COORDS)
        f.finish()
        assertEquals(1, f.count(AMULET))

        f.player.inv[f.player.inv.indexOfFirst { it?.id == AMULET.asRSCM() }] = null
        f.loc(CHEST_OPEN, CHEST_COORDS)
        f.finish()
        assertEquals(1, f.count(AMULET))

        val worn = Fixture(ENTERED_FALLS)
        worn.player.worn[2] = InvObj(AMULET, 1)
        worn.loc(CHEST_OPEN, CHEST_COORDS)
        worn.finish()
        assertEquals(0, worn.count(AMULET))
    }

    @Test
    fun `climbing the tree without a rope drops the player in the river`() {
        val f = Fixture(ENTERED_TOMB)
        f.player.coords = WaterfallCoords.TREE_ISLAND
        f.loc(TREE, TREE_COORDS)
        f.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, f.player.coords)
        assertTrue(f.output().contains("You try to use the tree to climb down..."), f.output())
        assertTrue(f.output().contains("...but you slip and fall into the water."), f.output())
    }

    @Test
    fun `the rope on the rock and then the tree leads to the ledge`() {
        val f = Fixture(ENTERED_TOMB)
        f.player.coords = WaterfallCoords.RAFT_CRASH
        f.give(ROPE)
        f.useOnLoc(ROCK, ROCK_COORDS, ROPE)
        f.finish()
        assertEquals(WaterfallCoords.TREE_ISLAND, f.player.coords)
        assertEquals(1, f.count(ROPE))

        f.useOnLoc(TREE, TREE_COORDS, ROPE)
        f.finish()
        assertEquals(WaterfallCoords.LEDGE, f.player.coords)
        assertEquals(1, f.count(ROPE))
        assertTrue(f.output().contains("You tie the rope to the tree and climb down to the ledge below."))
    }

    @Test
    fun `the barrel brings the player back down the river`() {
        val f = Fixture(ENTERED_TOMB)
        f.player.coords = WaterfallCoords.LEDGE
        f.loc(BARREL, BARREL_COORDS)
        f.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, f.player.coords)
        assertTrue(f.output().contains("You climb into the barrel and push off the edge."))
        assertTrue(f.output().contains("You are carried down the river."))
    }

    @Test
    fun `the waterfall door needs the amulet worn`() {
        val none = Fixture(ENTERED_TOMB)
        none.player.coords = WaterfallCoords.LEDGE
        none.loc(LEDGE_DOOR, LEDGE_DOOR_COORDS, shape = LocShape.CentrepieceStraight)
        none.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, none.player.coords)
        assertEquals(ENTERED_TOMB, none.stage())
        assertTrue(none.output().contains(LEDGE_FLOODED), none.output())
        assertTrue(none.output().contains("...you are pushed over the waterfall and into the river."))

        val packed = Fixture(ENTERED_TOMB)
        packed.player.coords = WaterfallCoords.LEDGE
        packed.give(AMULET)
        packed.loc(LEDGE_DOOR, LEDGE_DOOR_COORDS, shape = LocShape.CentrepieceStraight)
        packed.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, packed.player.coords)
        assertEquals(ENTERED_TOMB, packed.stage())
        assertTrue(packed.output().contains(LEDGE_FLOODED), packed.output())

        val worn = Fixture(ENTERED_TOMB)
        worn.player.coords = WaterfallCoords.LEDGE
        worn.player.worn[2] = InvObj(AMULET, 1)
        worn.loc(LEDGE_DOOR, LEDGE_DOOR_COORDS, shape = LocShape.CentrepieceStraight)
        worn.finish()
        assertEquals(WaterfallCoords.FALLS_ENTRY, worn.player.coords)
        assertEquals(ENTERED_FALLS, worn.stage())
        assertTrue(worn.output().contains("You enter the waterfall."), worn.output())
    }

    @Test
    fun `after the quest the falls open without the amulet and the stage stays`() {
        val f = Fixture(COMPLETE)
        f.player.coords = WaterfallCoords.LEDGE
        f.loc(LEDGE_DOOR, LEDGE_DOOR_COORDS, shape = LocShape.CentrepieceStraight)
        f.finish()
        assertEquals(WaterfallCoords.FALLS_ENTRY, f.player.coords)
        assertEquals(COMPLETE, f.stage())
    }

    @Test
    fun `the exit door leads back to the ledge`() {
        val f = Fixture(ENTERED_FALLS)
        f.player.coords = WaterfallCoords.FALLS_ENTRY
        f.loc(EXIT_DOOR, EXIT_DOOR_COORDS, shape = LocShape.WallStraight, angle = LocAngle.South)
        f.finish()
        assertEquals(WaterfallCoords.LEDGE, f.player.coords)
        assertTrue(f.output().contains("You exit the dungeon."), f.output())
    }

    @Test
    fun `the dungeon crate holds one key and the locked doors need it from the south`() {
        val f = Fixture(ENTERED_FALLS)
        f.loc(CRATE, CRATE_COORDS, angle = LocAngle.North)
        f.finish()
        assertEquals(1, f.count(BAXTORIAN_KEY))
        f.loc(CRATE, CRATE_COORDS, angle = LocAngle.North)
        f.finish()
        assertEquals(1, f.count(BAXTORIAN_KEY))

        val locked = Fixture(ENTERED_FALLS)
        locked.player.coords = TOMB_DOOR_SOUTH
        locked.loc(TOMB_DOOR, TOMB_DOOR_COORDS, shape = LocShape.WallStraight, angle = LocAngle.North)
        locked.finish()
        assertTrue(locked.output().contains("The door is locked."), locked.output())

        val keyed = Fixture(ENTERED_FALLS)
        keyed.player.coords = TOMB_DOOR_SOUTH
        keyed.give(BAXTORIAN_KEY)
        keyed.loc(TOMB_DOOR, TOMB_DOOR_COORDS, shape = LocShape.WallStraight, angle = LocAngle.North)
        keyed.finish()
        assertTrue(keyed.output().contains("You use the key to unlock the door."), keyed.output())
        assertEquals(1, keyed.count(BAXTORIAN_KEY))

        val inside = Fixture(ENTERED_FALLS)
        inside.player.coords = TOMB_DOOR_SOUTH.translateZ(2)
        inside.loc(TOMB_DOOR, TOMB_DOOR_COORDS, shape = LocShape.WallStraight, angle = LocAngle.North)
        inside.finish()
        assertFalse(inside.output().contains("The door is locked."), inside.output())
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
        assertTrue(f.output().contains("You've already put that type of rune on this pillar."))
        assertTrue(f.output().contains("You place the rune on the pillar. It disappears in a puff of smoke."))

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
        assertTrue(f.quest.allRunesPlaced(f.player))
        assertTrue(f.journal().contains("place <red>Glarial's amulet"), f.journal())
    }

    @Test
    fun `other runes and other pillars are not accepted`() {
        val f = Fixture(ENTERED_FALLS)
        f.player.coords = ROOM_CENTER
        f.give("obj.firerune", 2)
        f.useOnLoc(PILLAR, PILLAR_COORDS[0], "obj.firerune", handled = false)
        f.finish()
        assertEquals(2, f.count("obj.firerune"))
        assertEquals(0, f.player.pillarRunes)
    }

    @Test
    fun `the amulet before the pillars are charged floods the room and is lost`() {
        val f = Fixture(ENTERED_FALLS)
        f.player.coords = ROOM_CENTER
        f.give(AMULET)
        f.useOnLoc(STATUE, STATUE_COORDS, AMULET)
        f.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, f.player.coords)
        assertEquals(0, f.count(AMULET))
        assertEquals(ENTERED_FALLS, f.stage())
        assertTrue(f.output().contains("However, water floods into the room as you do..."))
        assertTrue(f.output().contains("...you are washed out of the cave and down the river."))
    }

    @Test
    fun `the amulet on the statue raises the floor and only then can the urn be poured`() {
        val f = Fixture(RUNES_PLACED)
        f.player.pillarRunes = WaterfallQuest.ALL_PILLAR_RUNES
        f.player.coords = ROOM_CENTER
        f.give(AMULET)
        f.give(URN_FULL)
        f.useOnLoc(STATUE, STATUE_COORDS, AMULET)
        f.finish()
        assertTrue(f.output().contains("You hear a loud rumble from beneath as the floor rises."))
        assertEquals(ROOM_CENTER.translate(38, -1), f.player.coords)
        assertEquals(0, f.count(AMULET))
        assertEquals(FLOOR_RISEN, f.stage())
        assertTrue(f.journal().contains("pouring her ashes"), f.journal())

        f.give(AMULET)
        f.useOnLoc(STATUE, STATUE_COORDS.translate(38, -1), AMULET)
        f.finish()
        assertEquals(FLOOR_RISEN, f.stage())

        f.useOnLoc(CHALICE, CHALICE_COORDS, URN_FULL)
        f.finish()
        f.assertRewards()
        assertTrue(f.output().contains("You carefully pour the ashes into the chalice and remove"))
        assertEquals(1, f.count(URN_EMPTY))
        assertEquals(0, f.count(URN_FULL))
        assertTrue(f.player.ui.containsModal("interface.questscroll"))
    }

    @Test
    fun `the urn cannot be poured before the floor has risen`() {
        val f = Fixture(RUNES_PLACED)
        f.player.coords = ROOM_CENTER
        f.give(URN_FULL)
        f.useOnLoc(CHALICE, CHALICE_COORDS, URN_FULL)
        f.finish()
        assertEquals(RUNES_PLACED, f.stage())
        assertEquals(1, f.count(URN_FULL))
        assertTrue(f.output().contains("can't reach the chalice"), f.output())
    }

    @Test
    fun `taking the treasure without the ashes floods the room and keeps the urn`() {
        val f = Fixture(FLOOR_RISEN)
        f.player.coords = ROOM_CENTER.translate(38, -1)
        f.give(URN_FULL)
        f.loc(CHALICE, CHALICE_COORDS)
        f.finish()
        assertEquals(WaterfallCoords.DOWNSTREAM, f.player.coords)
        assertEquals(FLOOR_RISEN, f.stage())
        assertEquals(1, f.count(URN_FULL))
        assertEquals(0, f.player.vars["varp.qp"])
        assertTrue(f.output().contains("You go to take the treasure from the chalice."))
        assertTrue(f.output().contains("...you are washed out of the cave and down the river."))
    }

    @Test
    fun `a full inventory cannot pour the urn and loses nothing`() {
        val f = Fixture(FLOOR_RISEN)
        f.player.coords = ROOM_CENTER.translate(38, -1)
        f.give(URN_FULL)
        f.fillInventory()
        f.useOnLoc(CHALICE, CHALICE_COORDS, URN_FULL)
        f.finish()
        assertEquals(FLOOR_RISEN, f.stage())
        assertEquals(1, f.count(URN_FULL))
        assertTrue(f.output().contains("5 free inventory spaces"), f.output())
        assertEquals(0, f.player.vars["varp.qp"])
    }

    @Test
    fun `the rewards come exactly once and the chalice is empty afterwards`() {
        val f = Fixture(FLOOR_RISEN)
        f.player.coords = ROOM_CENTER.translate(38, -1)
        f.give(URN_FULL)
        f.useOnLoc(CHALICE, CHALICE_COORDS, URN_FULL)
        f.finish()
        f.assertRewards()

        f.give(URN_FULL)
        f.useOnLoc(CHALICE, CHALICE_COORDS, URN_FULL)
        f.finish()
        f.loc(CHALICE, CHALICE_COORDS)
        f.finish()
        f.assertRewards()
        assertEquals(1, f.count(URN_FULL))
        assertTrue(f.output().contains("The chalice only contains some old ashes."), f.output())
        assertTrue(f.quest.completedLog(f.access()).contains("chalice of eternity"))
    }

    @Test
    fun `leaving the raised room through its doors lands in the corridor of the real room`() {
        val f = Fixture(FLOOR_RISEN)
        f.player.coords = CoordGrid(2604, 9901, 0)
        f.loc(TOMB_DOOR, CoordGrid(2604, 9900, 0), shape = LocShape.WallStraight, angle = LocAngle.North)
        f.finish()
        assertEquals(CoordGrid(2566, 9901, 0), f.player.coords)
    }

    @Test
    fun `Hadley answers the tourist questions word for word`() {
        val f = Fixture()
        f.talk(HADLEY_NPC)
        f.finish(listOf(2, 3, 4))
        val wildlife =
            "Well, there's a wide variety of wildlife, although unfortunately most of it's " +
                "quite dangerous. Please don't feed the goblins."
        assertTrue(f.output().contains(wildlife), f.output())
        assertTrue(f.output().contains("There is a lovely spot for a picnic on the hill"))
        assertTrue(f.output().contains("That's just silly talk."), f.output())

        val lady = Fixture()
        lady.player.appearance.bodyType = Appearance.BODY_TYPE_B
        lady.talk(HADLEY_NPC)
        lady.finish(listOf(4))
        assertTrue(lady.output().contains("Surely pretty is an understatement, lady."))
    }

    @Test
    fun `Hudon cannot be heard before the quest even from the island`() {
        val f = Fixture()
        f.talk(HUDON_NPC)
        f.finish()
        assertTrue(f.output().contains(NOISE_OF_THE_WATERFALL), f.output())
    }

    @Test
    fun `the journal follows every stage`() {
        val f = Fixture()
        val expected =
            mapOf(
                STARTED to "log raft",
                MET_HUDON to "Hadley",
                READ_BOOK to "Glarial's pebble",
                ENTERED_TOMB to "amulet",
                ENTERED_FALLS to "A key somewhere",
                RUNES_PLACED to "The pillars are charged",
                FLOOR_RISEN to "pouring her ashes",
            )
        for ((stage, text) in expected) {
            f.stageTo(stage)
            assertTrue(f.journal().contains(text), "stage $stage: ${f.journal()}")
        }
        f.stageTo(COMPLETE)
        assertTrue(f.quest.completedLog(f.access()).contains("Almera asked me"))
    }

    @Test
    fun `the quest ends at the stage the cache expects`() {
        assertEquals(COMPLETE, Fixture().quest.quest.maxSteps)
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
            with(BookcasesScript(setOf(TouristCentreBookcases()))) { scripts.startup() }
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

        fun contentLoc(symbol: String, coords: CoordGrid) {
            val (loc, type) =
                boundLoc(symbol, coords, LocShape.CentrepieceStraight, LocAngle.West)
            run { assertTrue(events.publish(this, LocContentEvents.Op1(loc, loc, type, type.contentGroup))) }
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
        private val ALMERA_NPC = WaterfallQuest.ALMERA_NPC
        private val HUDON_NPC = WaterfallQuest.HUDON_NPC
        private val GERALD_NPC = WaterfallQuest.GERALD_NPC
        private val HADLEY_NPC = WaterfallQuest.HADLEY_NPC
        private val GOLRIE_NPC = WaterfallQuest.GOLRIE_NPC

        private const val RAFT_UNSAFE =
            "You're not sure if the raft is safe to use. Best to leave it alone."
        private const val NOISE_OF_THE_WATERFALL =
            "Hudon can't hear you because of the noise of the waterfall. Perhaps the acoustics " +
                "would be better from that island?"
        private const val NOTHING_HAPPENS =
            "You place the pebble in the gravestone's small indent but nothing happens."
        private const val SLAB_SLIDES =
            "You place the pebble in the gravestone's small indent. The stone slab slides back " +
                "revealing a ladder. You climb down it."
        private const val TOMBSTONE_TEXT =
            "Here lies Glarial, wife of Baxtorian, true friend of nature in life and death. " +
                "May she now rest knowing only visitors with peaceful intent can enter."
        private const val LEDGE_FLOODED =
            "You try to open the door, but the ledge is suddenly flooded with water..."

        private const val RAFT = "loc.lograft_waterfall_quest"
        private const val RIVER = "loc.waterfall_swim_point"
        private const val ROCK = "loc.crossing_rock_waterfall_quest"
        private const val TREE = "loc.overhanging_tree1_waterfall_quest"
        private const val LEDGE_DOOR = "loc.waterfall_ledge_door"
        private const val BARREL = "loc.barrel_waterfall_quest"
        private const val BOOKCASE = "loc.bookcase_waterfall_quest"
        private const val GOLRIE_CRATE = "loc.golrie_crate_waterfall_quest"
        private const val GOLRIE_GATE = "loc.golrie_gate_waterfall_quest"
        private const val TOMBSTONE = "loc.glarials_tombstone_waterfall_quest"
        private const val CHEST_OPEN = "loc.glarials_chest_open_waterfall_quest"
        private const val COFFIN = "loc.glarials_tomb_waterfall_quest"
        private const val EXIT_DOOR = "loc.baxtorian_door_waterfall_quest"
        private const val CRATE = "loc.baxtorian_crate_waterfall_quest"
        private const val TOMB_DOOR = "loc.baxtorian_door_2_waterfall_quest"
        private const val PILLAR = "loc.stonepillar_small_waterfall_quest_op"
        private const val STATUE = "loc.statue_queen_waterfall_quest"
        private const val CHALICE = "loc.baxtorian_chalice_waterfall_quest"

        private val GERALD_COORDS = CoordGrid(2528, 3414, 0)
        private val RAFT_BANK = CoordGrid(2510, 3492, 0)
        private val RAFT_COORDS = CoordGrid(2509, 3493, 0)
        private val HUDON_COORDS = CoordGrid(2511, 3484, 0)
        private val RIVER_COORDS = CoordGrid(2512, 3475, 0)
        private val ROCK_COORDS = CoordGrid(2512, 3468, 0)
        private val TREE_COORDS = CoordGrid(2512, 3465, 0)
        private val LEDGE_DOOR_COORDS = CoordGrid(2511, 3464, 0)
        private val BARREL_COORDS = CoordGrid(2512, 3463, 1)
        private val BOOKCASE_COORDS = CoordGrid(2520, 3426, 1)
        private val TOURIST_BOOKCASE_THIN = CoordGrid(2516, 3431, 1)
        private val TOURIST_BOOKCASE_WIDE = CoordGrid(2517, 3424, 1)
        private val OUTSIDE_BOOKCASE = CoordGrid(2518, 3493, 0)
        private val UNINTERESTING =
            listOf(
                "You don't find anything that you'd ever want to read.",
                "You find nothing to interest you.",
                "None of them look very interesting",
            )

        private fun bookcaseAt(coords: CoordGrid): BoundLocInfo {
            val type = checkNotNull(ServerCacheManager.getObject("loc.bookcase".asRSCM(RSCMType.LOC)))
            return BoundLocInfo(LocInfo(2, coords, LocEntity(type.id, 10, 0)), type)
        }
        private val GOLRIE_CRATE_COORDS = CoordGrid(2548, 9565, 0)
        private val GOLRIE_GATE_COORDS = CoordGrid(2515, 9575, 0)
        private val GATE_SOUTH = CoordGrid(2515, 9574, 0)
        private val TOMBSTONE_COORDS = CoordGrid(2558, 3444, 0)
        private val TOMBSTONE_BANK = CoordGrid(2558, 3443, 0)
        private val CHEST_COORDS = CoordGrid(2530, 9844, 0)
        private val COFFIN_COORDS = CoordGrid(2542, 9811, 0)
        private val EXIT_DOOR_COORDS = CoordGrid(2575, 9861, 0)
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
