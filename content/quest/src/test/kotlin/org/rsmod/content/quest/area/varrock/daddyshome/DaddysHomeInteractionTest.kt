package org.rsmod.content.quest.area.varrock.daddyshome

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
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class DaddysHomeInteractionTest {

    @Test
    fun `Marlo asks for the favour and sends the player to Yarlo`() {
        val f = Fixture()
        f.talkMarlo()
        f.finish(listOf(3, 3, 3))
        assertEquals(DaddysHomeQuest.Started, f.stage())
        assertEquals(DaddysHomeQuest.Started, f.player.vars["varbit.daddyshome_status"])
        assertTrue(f.output().contains("south-east of Varrock"), f.output())
    }

    @Test
    fun `leaving the conversation at any point keeps the miniquest unstarted`() {
        for (options in listOf(listOf(4), listOf(1, 2, 4), listOf(3, 4), listOf(3, 3, 4), listOf(2, 4))) {
            val f = Fixture()
            f.talkMarlo()
            f.finish(options)
            assertEquals(0, f.stage(), "options $options")
        }
    }

    @Test
    fun `players who have not joined Mahogany Homes can ask about the company`() {
        val f = Fixture()
        f.talkMarlo()
        f.finish(listOf(1, 4))
        assertTrue(f.output().contains("redecorate people's houses"), f.output())
        assertEquals(0, f.stage())
    }

    @Test
    fun `Yarlo is only a chatty old man until Marlo has sent the player`() {
        val f = Fixture()
        f.talkYarlo()
        f.finish(listOf(2))
        assertEquals(0, f.stage())
        assertTrue(f.output().contains("not my boy, though") || f.output().contains("Not my boy"), f.output())
    }

    @Test
    fun `Yarlo asks for the old furniture to be removed and makes it demolishable`() {
        val f = Fixture(DaddysHomeQuest.Started)
        f.talkYarlo()
        f.finish()
        assertEquals(DaddysHomeQuest.Removing, f.stage())
        for (furniture in Furniture.entries) {
            assertEquals(Furniture.Broken, f.state(furniture), furniture.name)
        }
    }

    @Test
    fun `furniture cannot be demolished before Yarlo has asked`() {
        val f = Fixture(DaddysHomeQuest.Started)
        f.use(Furniture.Chair)
        assertEquals(Furniture.Untouched, f.state(Furniture.Chair))
        assertEquals(DaddysHomeQuest.Started, f.stage())
    }

    @Test
    fun `every piece is demolished one at a time and the last one moves the stage on`() {
        val f = Fixture(DaddysHomeQuest.Removing)
        f.setAll(Furniture.Broken)
        for ((index, furniture) in Furniture.entries.withIndex()) {
            assertEquals(DaddysHomeQuest.Removing, f.stage())
            f.use(furniture)
            assertEquals(Furniture.Cleared, f.state(furniture), furniture.name)
            if (index < Furniture.entries.size - 1) assertEquals(DaddysHomeQuest.Removing, f.stage())
        }
        assertEquals(DaddysHomeQuest.Removed, f.stage())
    }

    @Test
    fun `Yarlo lists what is still standing`() {
        val f = Fixture(DaddysHomeQuest.Removing)
        f.setAll(Furniture.Broken)
        f.set(Furniture.KitchenStool, Furniture.Cleared)
        f.set(Furniture.Carpet, Furniture.Cleared)
        f.talkYarlo()
        f.finish()
        val output = f.output().replace("<br>", " ")
        assertTrue(output.contains("A broken stool in the bedroom"), output)
        assertTrue(output.contains("The old campbed"), output)
        assertFalse(output.contains("A broken stool in the kitchen"), output)
        assertFalse(output.contains("The rotten carpet"), output)
        assertEquals(DaddysHomeQuest.Removing, f.stage())
    }

    @Test
    fun `Yarlo repeats the whole task when nothing has been removed`() {
        val f = Fixture(DaddysHomeQuest.Removing)
        f.setAll(Furniture.Broken)
        f.talkYarlo()
        f.finish()
        assertTrue(f.output().contains("What am I supposed to be doing again?"), f.output())
    }

    @Test
    fun `skipping the lecture still gives the building instructions`() {
        val f = Fixture(DaddysHomeQuest.Removed)
        f.setAll(Furniture.Cleared)
        f.talkYarlo()
        f.finish(listOf(2))
        assertEquals(DaddysHomeQuest.Building, f.stage())
        assertTrue(f.output().contains("optimise the fun out of life"), f.output())
        assertTrue(f.output().contains("two new stools, two new tables"), f.output())
        assertFalse(f.output().contains("Teleport Nexus"), f.output())
    }

    @Test
    fun `listening to the lecture plays it before the instructions`() {
        val f = Fixture(DaddysHomeQuest.Removed)
        f.setAll(Furniture.Cleared)
        f.talkYarlo()
        f.finish(listOf(1))
        assertEquals(DaddysHomeQuest.Building, f.stage())
        assertTrue(f.output().contains("Teleport Nexus"), f.output())
        assertTrue(f.output().contains("two new stools, two new tables"), f.output())
    }

    @Test
    fun `players who own a house point that out before the lecture`() {
        val f = Fixture(DaddysHomeQuest.Removed)
        f.setAll(Furniture.Cleared)
        VarPlayerIntMapSetter.set(f.player, "varbit.poh_house_location", 2)
        f.talkYarlo()
        f.finish(listOf(2))
        assertTrue(f.output().contains("house of my own, in Taverley"), f.output())
    }

    @Test
    fun `an interrupted lecture is offered again`() {
        val f = Fixture(DaddysHomeQuest.Lecture)
        f.setAll(Furniture.Cleared)
        f.talkYarlo()
        f.finish(listOf(1))
        assertEquals(DaddysHomeQuest.Building, f.stage())
    }

    @Test
    fun `Yarlo lends a hammer and a saw to anyone without them`() {
        val f = Fixture(DaddysHomeQuest.Building)
        f.talkYarlo()
        f.finish(listOf(3, 1, 1, 5))
        assertEquals(1, f.player.inv.count("obj.hammer"))
        assertEquals(1, f.player.inv.count("obj.poh_saw"))
    }

    @Test
    fun `Yarlo does not hand out tools that are already carried`() {
        val f = Fixture(DaddysHomeQuest.Building)
        f.give("obj.hammer" to 1, "obj.poh_saw" to 1)
        f.talkYarlo()
        f.finish(listOf(3, 5))
        assertEquals(1, f.player.inv.count("obj.hammer"))
        assertEquals(1, f.player.inv.count("obj.poh_saw"))
        assertTrue(f.output().contains("You've got a hammer and a saw"), f.output())
    }

    @Test
    fun `declining Yarlo's tools gives nothing`() {
        val f = Fixture(DaddysHomeQuest.Building)
        f.talkYarlo()
        f.finish(listOf(3, 2, 2, 5))
        assertEquals(0, f.player.inv.count("obj.hammer"))
        assertEquals(0, f.player.inv.count("obj.poh_saw"))
    }

    @Test
    fun `building needs Yarlo's instructions first`() {
        val f = Fixture(DaddysHomeQuest.Removed)
        f.setAll(Furniture.Cleared)
        f.give(*Tools, "obj.woodplank" to 2, "obj.nails" to 2)
        f.use(Furniture.Chair)
        assertEquals(Furniture.Cleared, f.state(Furniture.Chair))
        assertEquals(2, f.player.inv.count("obj.woodplank"))
        assertTrue(f.output().contains("talk to Old Man Yarlo before you start building"), f.output())
    }

    @Test
    fun `building without both tools consumes nothing`() {
        val f = Fixture(DaddysHomeQuest.Building)
        f.setAll(Furniture.Cleared)
        f.give("obj.hammer" to 1, "obj.woodplank" to 2, "obj.nails" to 2)
        f.use(Furniture.Chair)
        assertEquals(Furniture.Cleared, f.state(Furniture.Chair))
        assertEquals(2, f.player.inv.count("obj.woodplank"))
        assertEquals(2, f.player.inv.count("obj.nails"))
        assertTrue(f.output().contains("You need a saw"), f.output())
    }

    @Test
    fun `building without enough materials consumes nothing`() {
        val f = Fixture(DaddysHomeQuest.Building)
        f.setAll(Furniture.Cleared)
        f.give(*Tools, "obj.woodplank" to 1, "obj.nails" to 2)
        f.use(Furniture.Chair)
        assertEquals(Furniture.Cleared, f.state(Furniture.Chair))
        assertEquals(1, f.player.inv.count("obj.woodplank"))
        assertTrue(f.output().contains("enough planks"), f.output())
        assertEquals(0, f.xp())
    }

    @Test
    fun `too few nails stops a table`() {
        val f = Fixture(DaddysHomeQuest.Building)
        f.setAll(Furniture.Cleared)
        f.give(*Tools, "obj.woodplank" to 3, "obj.nails" to 3)
        f.use(Furniture.KitchenTable)
        assertEquals(Furniture.Cleared, f.state(Furniture.KitchenTable))
        assertEquals(3, f.player.inv.count("obj.nails"))
        assertTrue(f.output().contains("enough nails"), f.output())
    }

    @Test
    fun `each piece takes its own materials and pays its own experience`() {
        val expected =
            mapOf(
                Furniture.KitchenStool to Triple(29, 1, 2),
                Furniture.BedroomStool to Triple(29, 1, 2),
                Furniture.Chair to Triple(58, 2, 2),
                Furniture.KitchenTable to Triple(87, 3, 4),
                Furniture.BedroomTable to Triple(87, 3, 4),
            )
        for ((furniture, cost) in expected) {
            val f = Fixture(DaddysHomeQuest.Building)
            f.setAll(Furniture.Cleared)
            f.give(*Tools, "obj.woodplank" to 10, "obj.nails" to 10)
            f.use(furniture)
            assertEquals(Furniture.Built, f.state(furniture), furniture.name)
            assertEquals(cost.first, f.xp(), furniture.name)
            assertEquals(10 - cost.second, f.player.inv.count("obj.woodplank"), furniture.name)
            assertEquals(10 - cost.third, f.player.inv.count("obj.nails"), furniture.name)
            assertEquals(1, f.player.inv.count("obj.hammer"))
            assertEquals(1, f.player.inv.count("obj.poh_saw"))
        }
    }

    @Test
    fun `the carpet takes three bolts of cloth through its remove and build op`() {
        val f = Fixture(DaddysHomeQuest.Removing)
        f.setAll(Furniture.Broken)
        f.use(Furniture.Carpet)
        assertEquals(Furniture.Cleared, f.state(Furniture.Carpet))
        f.setStage(DaddysHomeQuest.Building)
        f.give(*Tools, "obj.cloth" to 4)
        f.use(Furniture.Carpet)
        assertEquals(Furniture.Built, f.state(Furniture.Carpet))
        assertEquals(1, f.player.inv.count("obj.cloth"))
        assertEquals(45, f.xp())
    }

    @Test
    fun `the waxwood bed takes three waxwood planks and two bolts of cloth`() {
        val f = Fixture(DaddysHomeQuest.Building)
        f.setAll(Furniture.Cleared)
        f.give(*Tools, "obj.woodplank" to 3, "obj.cloth" to 2, "obj.daddyshome_waxwood_plank" to 2)
        f.use(Furniture.Bed)
        assertEquals(Furniture.Cleared, f.state(Furniture.Bed))
        f.give("obj.daddyshome_waxwood_plank" to 1)
        f.use(Furniture.Bed)
        assertEquals(Furniture.Built, f.state(Furniture.Bed))
        assertEquals(0, f.player.inv.count("obj.daddyshome_waxwood_plank"))
        assertEquals(0, f.player.inv.count("obj.cloth"))
        assertEquals(3, f.player.inv.count("obj.woodplank"))
        assertEquals(207, f.xp())
    }

    @Test
    fun `the best nails are used first and mixed nails make up the difference`() {
        val f = Fixture(DaddysHomeQuest.Building)
        f.setAll(Furniture.Cleared)
        f.give(*Tools, "obj.woodplank" to 3, "obj.nails_bronze" to 3, "obj.nails_mithril" to 2)
        f.use(Furniture.KitchenTable)
        assertEquals(Furniture.Built, f.state(Furniture.KitchenTable))
        assertEquals(0, f.player.inv.count("obj.nails_mithril"))
        assertEquals(1, f.player.inv.count("obj.nails_bronze"))
    }

    @Test
    fun `a built piece and an untouched piece do nothing when used again`() {
        val f = Fixture(DaddysHomeQuest.Building)
        f.setAll(Furniture.Built)
        f.give(*Tools, "obj.woodplank" to 3, "obj.nails" to 4)
        f.use(Furniture.KitchenTable)
        assertEquals(3, f.player.inv.count("obj.woodplank"))
        assertEquals(0, f.xp())
    }

    @Test
    fun `the crates give three waxwood logs while the bed is unbuilt`() {
        val f = Fixture(DaddysHomeQuest.Building)
        f.setAll(Furniture.Cleared)
        f.crates()
        assertEquals(3, f.player.inv.count("obj.daddyshome_waxwood_logs"))
        assertTrue(f.output().contains("water-repellant waxwood"), f.output())
    }

    @Test
    fun `the crates hold nothing before the building stage or once the bed is built`() {
        val early = Fixture(DaddysHomeQuest.Removing)
        early.crates()
        assertEquals(0, early.player.inv.count("obj.daddyshome_waxwood_logs"))
        val late = Fixture(DaddysHomeQuest.Building)
        late.setAll(Furniture.Built)
        late.crates()
        assertEquals(0, late.player.inv.count("obj.daddyshome_waxwood_logs"))
    }

    @Test
    fun `a full inventory does not lose the logs`() {
        val f = Fixture(DaddysHomeQuest.Building)
        f.setAll(Furniture.Cleared)
        for (slot in 0 until 28) f.player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        f.crates()
        assertEquals(0, f.player.inv.count("obj.daddyshome_waxwood_logs"))
        assertTrue(f.output().contains("enough inventory space"), f.output())
    }

    @Test
    fun `the Lumber Yard operator turns the logs into planks for free`() {
        val f = Fixture(DaddysHomeQuest.Building)
        f.give("obj.daddyshome_waxwood_logs" to 3)
        f.talkSawmill("npc.poh_sawmill_opp")
        f.finish(listOf(3))
        assertEquals(0, f.player.inv.count("obj.daddyshome_waxwood_logs"))
        assertEquals(3, f.player.inv.count("obj.daddyshome_waxwood_plank"))
        assertTrue(f.output().contains("I won't charge for this"), f.output())
    }

    @Test
    fun `the Lumber Yard operator wants logs to work with`() {
        val f = Fixture(DaddysHomeQuest.Building)
        f.talkSawmill("npc.poh_sawmill_opp")
        f.finish(listOf(3))
        assertEquals(0, f.player.inv.count("obj.daddyshome_waxwood_plank"))
        assertTrue(f.output().contains("waxwood logs to work with"), f.output())
    }

    @Test
    fun `other sawmill operators refuse to touch the waxwood`() {
        for (operator in listOf("npc.prif_sawmill_operator", "npc.auburn_sawmill_operator")) {
            val f = Fixture(DaddysHomeQuest.Building)
            f.give("obj.daddyshome_waxwood_logs" to 3)
            f.talkSawmill(operator)
            f.finish(listOf(3))
            assertEquals(3, f.player.inv.count("obj.daddyshome_waxwood_logs"), operator)
            assertEquals(0, f.player.inv.count("obj.daddyshome_waxwood_plank"), operator)
            assertTrue(f.output().contains("some other sawmill operator"), f.output())
        }
    }

    @Test
    fun `the waxwood option is only offered while building`() {
        val f = Fixture(DaddysHomeQuest.Removing)
        f.talkSawmill("npc.poh_sawmill_opp")
        f.finish(listOf(3))
        assertFalse(f.output().contains("Old Man Yarlo"), f.output())
    }

    @Test
    fun `Yarlo thanks the player once everything is built and points them to Marlo`() {
        val f = Fixture(DaddysHomeQuest.Building)
        f.setAll(Furniture.Built)
        f.talkYarlo()
        f.finish()
        assertEquals(DaddysHomeQuest.Built, f.stage())
        assertTrue(f.output().contains("Trot off back to my boy Marlo"), f.output())
    }

    @Test
    fun `Yarlo does not move on while a piece is still missing`() {
        val f = Fixture(DaddysHomeQuest.Building)
        f.setAll(Furniture.Built)
        f.set(Furniture.Bed, Furniture.Cleared)
        f.talkYarlo()
        f.finish(listOf(5))
        assertEquals(DaddysHomeQuest.Building, f.stage())
    }

    @Test
    fun `Yarlo asks whether Marlo has paid yet`() {
        val f = Fixture(DaddysHomeQuest.Built)
        f.setAll(Furniture.Built)
        f.talkYarlo()
        f.finish()
        assertTrue(f.output().contains("Has my boy Marlo rewarded you yet?"), f.output())
        assertEquals(DaddysHomeQuest.Built, f.stage())
    }

    @Test
    fun `Marlo reminds a player in the middle of the job where Yarlo lives`() {
        val f = Fixture(DaddysHomeQuest.Building)
        f.talkMarlo()
        f.finish(listOf(1))
        assertTrue(f.output().contains("He's sure to be at home"), f.output())
        assertEquals(DaddysHomeQuest.Building, f.stage())
    }

    @Test
    fun `claiming the reward without a house grants one, the crate, experience and the scroll once`() {
        val f = Fixture(DaddysHomeQuest.Built)
        f.setAll(Furniture.Built)
        f.talkMarlo()
        f.finish(listOf(1))
        assertEquals(DaddysHomeQuest.Complete, f.stage())
        assertEquals(400, f.xp())
        assertEquals(1, f.player.inv.count(DaddysHomeQuest.Crate))
        assertEquals(1, f.player.vars["varbit.poh_house_location"])
        assertEquals(0, f.player.inv.count("obj.coins"))
        assertEquals(0, f.player.vars["varp.qp"])
        assertEquals(1, f.player.vars["varbit.miniquests_completed_count"])
        assertEquals(0, f.player.vars["varbit.quests_completed_count"])
        assertTrue(f.player.ui.containsModal("interface.questscroll"))
        assertTrue(f.output().contains("persuaded the Estate Agent"), f.output())
        f.talkMarlo()
        f.finish()
        assertEquals(400, f.xp())
        assertEquals(1, f.player.inv.count(DaddysHomeQuest.Crate))
        assertEquals(1, f.player.vars["varbit.miniquests_completed_count"])
    }

    @Test
    fun `players who already own a house are paid a thousand coins instead`() {
        val f = Fixture(DaddysHomeQuest.Built)
        f.setAll(Furniture.Built)
        VarPlayerIntMapSetter.set(f.player, "varbit.poh_house_location", 4)
        f.talkMarlo()
        f.finish(listOf(1))
        assertEquals(DaddysHomeQuest.Complete, f.stage())
        assertEquals(1_000, f.player.inv.count("obj.coins"))
        assertEquals(4, f.player.vars["varbit.poh_house_location"])
        assertEquals(400, f.xp())
    }

    @Test
    fun `a full inventory drops the rewards instead of losing them`() {
        val f = Fixture(DaddysHomeQuest.Built)
        f.setAll(Furniture.Built)
        VarPlayerIntMapSetter.set(f.player, "varbit.poh_house_location", 1)
        for (slot in 0 until 28) f.player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        f.talkMarlo()
        f.finish(listOf(1))
        assertEquals(DaddysHomeQuest.Complete, f.stage())
        assertEquals(400, f.xp())
        assertEquals(0, f.player.inv.count(DaddysHomeQuest.Crate))
        assertEquals(1, f.objsOnFloor(DaddysHomeQuest.Crate))
        assertEquals(1_000, f.objsOnFloor("obj.coins"))
    }

    @Test
    fun `putting off the reward is remembered and Marlo asks again`() {
        val f = Fixture(DaddysHomeQuest.Built)
        f.setAll(Furniture.Built)
        f.talkMarlo()
        f.finish(listOf(3))
        assertEquals(DaddysHomeQuest.RewardPending, f.stage())
        assertEquals(0, f.xp())
        assertTrue(f.output().contains("not qualified to do contracts"), f.output())
        f.talkMarlo()
        f.finish(listOf(1))
        assertTrue(f.output().contains("I haven't forgotten"), f.output())
        assertEquals(DaddysHomeQuest.Complete, f.stage())
        assertEquals(400, f.xp())
    }

    @Test
    fun `asking about the contract does not claim the reward`() {
        val f = Fixture(DaddysHomeQuest.Built)
        f.setAll(Furniture.Built)
        f.talkMarlo()
        f.finish(listOf(2))
        assertEquals(DaddysHomeQuest.RewardPending, f.stage())
        assertEquals(0, f.xp())
    }

    @Test
    fun `after the miniquest Marlo talks about the company and Yarlo about the house`() {
        val f = Fixture(DaddysHomeQuest.Complete)
        f.give(DaddysHomeQuest.Crate to 1)
        f.talkMarlo()
        f.finish()
        assertTrue(f.output().contains("Gielinor's first construction company"), f.output())
        assertEquals(1, f.player.inv.count(DaddysHomeQuest.Crate))
        val g = Fixture(DaddysHomeQuest.Complete)
        g.talkYarlo()
        g.finish(listOf(1))
        assertTrue(g.output().contains("wonders for my little home"), g.output())
    }

    @Test
    fun `Yarlo replays the lecture after the miniquest`() {
        val f = Fixture(DaddysHomeQuest.Complete)
        f.talkYarlo()
        f.finish(listOf(2))
        assertTrue(f.output().contains("Teleport Nexus"), f.output())
        assertEquals(DaddysHomeQuest.Complete, f.stage())
    }

    @Test
    fun `a lost crate is replaced once and never after it has been opened`() {
        val lost = Fixture(DaddysHomeQuest.Complete)
        lost.talkMarlo()
        lost.finish()
        assertEquals(1, lost.player.inv.count(DaddysHomeQuest.Crate))
        val opened = Fixture(DaddysHomeQuest.Complete)
        opened.give(DaddysHomeQuest.Crate to 1)
        opened.openCrate(0)
        assertEquals(0, opened.player.inv.count(DaddysHomeQuest.Crate))
        opened.talkMarlo()
        opened.finish()
        assertEquals(0, opened.player.inv.count(DaddysHomeQuest.Crate))
    }

    @Test
    fun `opening the crate hands over the supplies once`() {
        val f = Fixture(DaddysHomeQuest.Complete)
        f.give(DaddysHomeQuest.Crate to 1)
        f.openCrate(0)
        assertEquals(0, f.player.inv.count(DaddysHomeQuest.Crate))
        assertEquals(25, f.player.inv.count("obj.cert_woodplank"))
        assertEquals(50, f.player.inv.count("obj.nails_mithril"))
        assertEquals(5, f.player.inv.count("obj.cert_steel_bar"))
        assertEquals(10, f.player.inv.count("obj.cert_plank_oak"))
        assertEquals(8, f.player.inv.count("obj.cert_cloth"))
        assertEquals(5, f.player.inv.count("obj.poh_tablet_teleporttohouse"))
        assertEquals(1, f.player.inv.count("obj.poh_tablet_faladorteleport"))
        assertEquals(1, f.player.vars["varbit.daddyshome_crate_opened"])
    }

    @Test
    fun `a crate is kept when there is no room to open it`() {
        val f = Fixture(DaddysHomeQuest.Complete)
        for (slot in 0 until 28) f.player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        f.player.inv[0] = InvObj(DaddysHomeQuest.Crate, 1)
        f.openCrate(0)
        assertEquals(1, f.player.inv.count(DaddysHomeQuest.Crate))
        assertEquals(0, f.player.inv.count("obj.cert_woodplank"))
        assertEquals(0, f.player.vars["varbit.daddyshome_crate_opened"])
    }

    @Test
    fun `the journal follows the miniquest`() {
        val expected =
            mapOf(
                DaddysHomeQuest.Started to "speak to <red>Old Man Yarlo</red>",
                DaddysHomeQuest.Removing to "remove all of the old furniture",
                DaddysHomeQuest.Removed to "talk to <red>Old Man Yarlo</red> again",
                DaddysHomeQuest.Building to "build new furniture",
                DaddysHomeQuest.Built to "return to <red>Marlo</red>",
            )
        for ((stage, text) in expected) {
            val f = Fixture(stage)
            val log = f.quest.questLog(f.access())
            assertTrue(log.contains(text, ignoreCase = true), "stage $stage: $log")
        }
        val done = Fixture(DaddysHomeQuest.Complete)
        assertTrue(done.quest.completedLog(done.access()).contains("QUEST COMPLETE"))
    }

    @Test
    fun `the bed objective drops out once the bed is built`() {
        val f = Fixture(DaddysHomeQuest.Building)
        assertTrue(f.quest.questLog(f.access()).contains("waxwood planks"))
        f.set(Furniture.Bed, Furniture.Built)
        assertFalse(f.quest.questLog(f.access()).contains("waxwood planks"))
    }

    private class Fixture(stage: Int = 0) {
        val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("daddys-home-test")
        private var result: Result<Unit>? = null
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getAlignment = { TextAlignment() },
                    getNpcInteractions = { NpcInteractions(events) },
                )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 793L
                observerUUID = 793L
                slotId = 1
                assignUid()
                coords = CoordGrid(3240, 3395, 0)
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

        private val objRepo = ObjRepository(MapClock(100), ObjRegistry(ZoneUpdateMap()))
        val quest = DaddysHomeQuest(objRepo)

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(quest) { scripts.startup() }
            with(DaddysHomeFurniture(quest)) { scripts.startup() }
            with(DaddysHomeSawmill(quest)) { scripts.startup() }
            setStage(stage)
        }

        fun setStage(stage: Int) {
            VarPlayerIntMapSetter.set(player, "varbit.daddyshome_status", stage)
        }

        fun stage() = quest.quest.getQuestStage(player)

        fun state(furniture: Furniture): Int = player.vars[furniture.varbit]

        fun set(furniture: Furniture, state: Int) {
            VarPlayerIntMapSetter.set(player, furniture.varbit, state)
        }

        fun setAll(state: Int) {
            for (furniture in Furniture.entries) set(furniture, state)
        }

        fun xp(): Int = player.statMap.getXP("stat.construction")

        fun access() = ProtectedAccess(player, coroutine, context)

        fun give(vararg objs: Pair<String, Int>) {
            for ((obj, count) in objs) {
                val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)))
                val stacks = if (type.stackable) listOf(count) else List(count) { 1 }
                for (amount in stacks) {
                    val slot = (0 until 28).first { player.inv[it] == null }
                    player.inv[slot] = InvObj(obj, amount)
                }
            }
        }

        fun objsOnFloor(obj: String): Int =
            objRepo.findAll(player.coords).filter { it.type == obj.asRSCM(RSCMType.OBJ) }.sumOf { it.count }

        fun talkMarlo() = talk("npc.con_contractor_varrock")

        fun talkYarlo() = talk("npc.daddyshome_daddy")

        fun talkSawmill(operator: String) = talk(operator)

        private fun talk(npc: String) = start {
            assertTrue(events.publish(this, NpcEvents.Op1(Npc(npc, coords.translateZ(1)))))
        }

        fun use(furniture: Furniture) {
            val type = checkNotNull(ServerCacheManager.getObject(furniture.loc.asRSCM(RSCMType.LOC)))
            val loc = BoundLocInfo(LocInfo(0, CoordGrid(3240, 3394, 0), LocEntity(type.id, 10, 0)), type)
            start {
                val event =
                    if (furniture == Furniture.Carpet) {
                        LocEvents.Op5(loc, loc, type)
                    } else {
                        LocEvents.Op1(loc, loc, type)
                    }
                assertTrue(events.publish(this, event))
            }
            finish()
        }

        fun crates() {
            val type = checkNotNull(ServerCacheManager.getObject(DaddysHomeQuest.Crates.asRSCM(RSCMType.LOC)))
            val loc = BoundLocInfo(LocInfo(0, CoordGrid(3243, 3398, 0), LocEntity(type.id, 10, 0)), type)
            start { assertTrue(events.publish(this, LocEvents.Op1(loc, loc, type))) }
            finish()
        }

        fun openCrate(slot: Int) {
            val type = checkNotNull(ServerCacheManager.getItem(DaddysHomeQuest.Crate.asRSCM(RSCMType.OBJ)))
            start {
                val event = HeldObjEvents.Op1(slot, checkNotNull(inv[slot]), type, inv)
                assertTrue(events.publish(this, event))
            }
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
        private val Tools = arrayOf("obj.hammer" to 1, "obj.poh_saw" to 1)
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
