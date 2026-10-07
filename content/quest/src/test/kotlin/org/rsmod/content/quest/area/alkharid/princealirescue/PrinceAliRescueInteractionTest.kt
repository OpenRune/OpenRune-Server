package org.rsmod.content.quest.area.alkharid.princealirescue

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.InventoryServerType
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
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.LocInteractions
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.interact.NpcUInteractions
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
import org.rsmod.api.route.BoundValidator
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.Ashes
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.BallOfWool
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.Beer
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.BlondWig
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.BronzeBar
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.BucketOfWater
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.Coins
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.JugOfWater
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.Key
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.KeyPrint
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NpcHassan
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NpcJoe
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NpcKeli
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NpcLeela
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NpcOsman
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NpcPrinceCell
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NpcPrincePalace
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.PinkSkirt
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.PotOfFlour
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.QuestKey
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.Redberries
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.Rope
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.SkinPaste
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.SoftClay
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.StageAliEscaped
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.StageBriefed
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.StageComplete
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.StageJoeDrunk
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.StageKeliTied
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.StagePrepared
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.StageStarted
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.Wig
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.YellowDye
import org.rsmod.content.quest.area.alkharid.princealirescue.npcs.Hassan
import org.rsmod.content.quest.area.alkharid.princealirescue.npcs.Joe
import org.rsmod.content.quest.area.alkharid.princealirescue.npcs.LadyKeli
import org.rsmod.content.quest.area.alkharid.princealirescue.npcs.Leela
import org.rsmod.content.quest.area.alkharid.princealirescue.npcs.Osman
import org.rsmod.content.quest.area.alkharid.princealirescue.npcs.PrinceAli
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirementPolicy
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.EntityFaceAngle
import org.rsmod.game.interact.InteractionOp
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
import org.rsmod.game.seq.EntitySeq
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.StepValidator
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * Drives the quest's real scripts through the event bus on the real collision map around the toll
 * gate and the jail: the whole rescue from Hassan to the reward, every refusal along the way, the
 * furnace key, the lost-key replacement, Joe's beers across visits, the cell door, and the toll
 * gate from both sides before and after the quest.
 */
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class PrinceAliRescueInteractionTest {

    @Test
    fun `the full rescue with Osman making the key completes the quest and pays out once`() =
        respectingProgress {
            val f = Fixture()
            assertFalse(QuestRequirements.hasCompleted(f.player, QuestKey))

            f.choose(1, 1)
            f.talk(NpcHassan)
            assertEquals(StageStarted, f.stage())
            assertTrue(f.journal().contains("Osman"))

            f.choose(OsmanLeave)
            f.talk(NpcOsman)
            assertEquals(StageBriefed, f.stage())
            assertTrue(f.said("abandoned jail just east of Draynor Village"))

            f.imprintKey()
            assertEquals(1, f.count(KeyPrint))
            assertEquals(0, f.count(SoftClay))
            assertTrue(f.journal().contains("imprint"))

            f.give(BronzeBar)
            f.choose(OsmanLeave)
            f.talk(NpcOsman)
            assertTrue(f.quest.keyOrdered(f.player))
            assertEquals(0, f.count(KeyPrint))
            assertEquals(0, f.count(BronzeBar))

            f.choose(LeelaLeave)
            f.talk(NpcLeela)
            assertTrue(f.quest.keyObtained(f.player))
            assertFalse(f.quest.keyOrdered(f.player))
            assertEquals(1, f.count(Key))
            assertEquals(StageBriefed, f.stage())

            f.makeDisguise()
            assertEquals(1, f.count(BlondWig))
            assertEquals(1, f.count(SkinPaste))

            f.talk(NpcLeela)
            assertEquals(StagePrepared, f.stage())
            assertTrue(f.said("deal with his personal guard"))

            f.give(Beer, 3)
            f.choose(1)
            f.talk(NpcJoe)
            assertEquals(StageJoeDrunk, f.stage())
            assertEquals(0, f.count(Beer))

            f.give(Rope)
            f.npcU(NpcKeli, Rope)
            assertEquals(StageKeliTied, f.stage())
            assertEquals(0, f.count(Rope))
            assertTrue(f.said("tie her up"))

            f.talk(NpcPrinceCell)
            assertEquals(StageAliEscaped, f.stage())
            assertEquals(0, f.count(BlondWig))
            assertEquals(0, f.count(SkinPaste))
            assertEquals(0, f.count(PinkSkirt))

            f.talk(NpcHassan)
            assertEquals(StageComplete, f.stage())
            assertEquals(3, f.player.vars["varp.qp"])
            assertEquals(700, f.count(Coins))
            assertTrue(f.player.ui.containsModal("interface.questscroll"))
            assertTrue(QuestRequirements.hasCompleted(f.player, QuestKey))

            f.talk(NpcHassan)
            assertEquals(700, f.count(Coins))
            assertEquals(3, f.player.vars["varp.qp"])
            f.talk(NpcPrincePalace)
            assertTrue(f.said("forever in your debt"))
        }

    @Test
    fun `the key can be made at a furnace from the imprint and a bronze bar`() {
        val f = Fixture(StageBriefed)
        f.give(KeyPrint)
        f.furnaceUse()
        assertEquals(1, f.count(KeyPrint))
        assertEquals(0, f.count(Key))
        assertTrue(f.said("You need a bronze bar"))

        f.give(BronzeBar)
        f.choose(2)
        f.furnaceUse()
        assertEquals(1, f.count(KeyPrint))
        assertEquals(1, f.count(BronzeBar))

        f.choose(1)
        f.furnaceUse()
        assertEquals(0, f.count(KeyPrint))
        assertEquals(0, f.count(BronzeBar))
        assertEquals(1, f.count(Key))
        assertTrue(f.quest.keyObtained(f.player))
        assertTrue(f.player.statMap.getXP("stat.crafting") > 0)
        assertTrue(f.journal().contains("copy of the cell key"))
    }

    @Test
    fun `the furnace route reaches Leela's go-ahead without Osman ever touching the key`() =
        respectingProgress {
            val f = Fixture(StageBriefed)
            f.imprintKey()
            assertEquals(1, f.count(KeyPrint))
            f.give(BronzeBar)
            f.choose(1)
            f.furnaceUse()
            assertEquals(1, f.count(Key))
            assertFalse(f.quest.keyOrdered(f.player))

            f.giveDisguise()
            f.choose(LeelaLeave)
            f.talk(NpcLeela)
            assertEquals(StageBriefed, f.stage())
            f.talk(NpcLeela)
            assertEquals(StagePrepared, f.stage())
            assertEquals(1, f.count(Key))
        }

    @Test
    fun `declining Hassan leaves the quest unstarted`() {
        val f = Fixture()
        f.choose(1, 2)
        f.talk(NpcHassan)
        assertEquals(0, f.stage())
        f.choose(4)
        f.talk(NpcHassan)
        assertEquals(0, f.stage())
    }

    @Test
    fun `Hassan hands out a jug of water when it is too hot`() {
        val f = Fixture()
        f.choose(2, 4)
        f.talk(NpcHassan)
        assertEquals(1, f.count(JugOfWater))
        assertEquals(0, f.stage())
    }

    @Test
    fun `the quest npcs keep to themselves before the briefing`() {
        val f = Fixture(StageStarted)
        f.give(SoftClay)
        f.talk(NpcKeli)
        assertTrue(f.said("Clear off then."))
        f.talk(NpcLeela)
        assertTrue(f.said("That is no concern of yours"))
        assertEquals(1, f.count(SoftClay))
        assertEquals(StageStarted, f.stage())
    }

    @Test
    fun `keli shows the key but no imprint is taken without soft clay`() = respectingProgress {
        val f = Fixture(StageBriefed)
        f.choose(1, 1, 2, 1)
        f.talk(NpcKeli)
        assertTrue(f.said("Keli shows you a small key"))
        assertEquals(0, f.count(KeyPrint))
        assertTrue(f.quest.keliAsked(f.player))
        f.give(SoftClay)
        f.choose(2, 1, 1)
        f.talk(NpcKeli)
        assertTrue(f.said("Hello again!"))
        assertEquals(1, f.count(KeyPrint))
        assertEquals(0, f.count(SoftClay))
    }

    @Test
    fun `osman keeps the imprint until a bronze bar comes with it`() {
        val f = Fixture(StageBriefed)
        f.give(KeyPrint)
        f.choose(OsmanLeave)
        f.talk(NpcOsman)
        assertTrue(f.said("Bring me a bronze bar"))
        assertEquals(1, f.count(KeyPrint))
        assertFalse(f.quest.keyOrdered(f.player))
    }

    @Test
    fun `leela waits for the whole disguise and a dyed wig`() {
        val f = Fixture(StageBriefed)
        f.quest.setMetLeela(f.player)
        f.give(Key)
        f.give(Wig)
        f.give(SkinPaste)
        f.give(PinkSkirt)
        f.choose(LeelaLeave)
        f.talk(NpcLeela)
        assertEquals(StageBriefed, f.stage())
        f.give(YellowDye)
        f.use(YellowDye, Wig)
        assertEquals(1, f.count(BlondWig))
        assertEquals(0, f.count(Wig))
        assertEquals(0, f.count(YellowDye))
        f.talk(NpcLeela)
        assertEquals(StagePrepared, f.stage())
    }

    @Test
    fun `leela still gives her briefing at the first meeting even with everything ready`() {
        val f = Fixture(StageBriefed)
        f.give(Key)
        f.give(BlondWig)
        f.give(SkinPaste)
        f.give(PinkSkirt)
        f.choose(LeelaLeave)
        f.talk(NpcLeela)
        assertTrue(f.said("I'd say that's a good summary."))
        assertEquals(StageBriefed, f.stage())
        f.talk(NpcLeela)
        assertEquals(StagePrepared, f.stage())
    }

    @Test
    fun `every quest npc is spawned on the base id the handlers bind`() {
        val f = Fixture()
        for (npc in listOf(NpcHassan, NpcOsman, NpcLeela, NpcKeli, NpcJoe, NpcPrinceCell, NpcPrincePalace)) {
            val id = npc.asRSCM(RSCMType.NPC)
            assertTrue(f.events.contains(NpcEvents.Op1::class.java, id), "$npc has no op1 handler")
        }
        for (visible in listOf("npc.lady_keli_vis", "npc.joe_vis", "npc.prince_ali_vis_blackeye", "npc.prince_ali_vis")) {
            assertFalse(f.events.contains(NpcEvents.Op1::class.java, visible.asRSCM(RSCMType.NPC)), visible)
        }
    }

    @Test
    fun `the prince stays put with the disguise but without the key`() {
        val f = Fixture(StageKeliTied)
        f.give(BlondWig)
        f.give(SkinPaste)
        f.give(PinkSkirt)
        f.talk(NpcPrinceCell)
        assertEquals(StageKeliTied, f.stage())
        assertEquals(1, f.count(BlondWig))
    }

    @Test
    fun `a lost key is replaced for fifteen coins`() {
        val f = Fixture(StagePrepared)
        f.quest.setMetLeela(f.player)
        f.quest.setKeyObtained(f.player)
        f.talk(NpcLeela)
        assertTrue(f.said("I haven't got that much."))
        assertEquals(0, f.count(Key))
        f.give(Coins, 20)
        f.talk(NpcLeela)
        assertEquals(1, f.count(Key))
        assertEquals(5, f.count(Coins))
        f.talk(NpcLeela)
        assertEquals(1, f.count(Key))
        assertEquals(5, f.count(Coins))
    }

    @Test
    fun `one beer is remembered and two more finish the job`() {
        val f = Fixture(StagePrepared)
        f.give(Beer, 2)
        f.choose(1)
        f.talk(NpcJoe)
        assertEquals(StagePrepared, f.stage())
        assertEquals(1, f.count(Beer))
        assertTrue(f.said("at least two more"))
        assertTrue(f.quest.joeHadBeer(f.player))

        f.give(Beer, 1)
        f.choose(1)
        f.talk(NpcJoe)
        assertEquals(StageJoeDrunk, f.stage())
        assertEquals(0, f.count(Beer))
    }

    @Test
    fun `joe does not talk before leela has sent the player`() {
        val f = Fixture(StageBriefed)
        f.give(Beer, 3)
        f.talk(NpcJoe)
        assertTrue(f.said("Can't say. It's all very secret."))
        assertEquals(3, f.count(Beer))
    }

    @Test
    fun `keli cannot be tied up before joe is drunk or without the disguise`() = respectingProgress {
        val f = Fixture(StagePrepared)
        f.give(Rope)
        f.npcU(NpcKeli, Rope)
        assertEquals(StagePrepared, f.stage())
        assertEquals(1, f.count(Rope))
        assertTrue(f.said("You cannot tie Keli up"))

        f.setStage(StageJoeDrunk)
        f.npcU(NpcKeli, Rope)
        assertEquals(StageJoeDrunk, f.stage())
        assertEquals(1, f.count(Rope))

        f.giveDisguiseAndKey()
        f.talk(NpcKeli)
        assertTrue(f.said("I'm here to tie you up!"))
        assertEquals(StageKeliTied, f.stage())
        assertEquals(0, f.count(Rope))
    }

    @Test
    fun `the prince stays put without his disguise`() {
        val f = Fixture(StageKeliTied)
        f.give(Key)
        f.give(BlondWig)
        f.talk(NpcPrinceCell)
        assertEquals(StageKeliTied, f.stage())
        assertEquals(1, f.count(BlondWig))
        assertTrue(f.said("I'll be back once I have it."))
    }

    @Test
    fun `hassan holds the payment back until there is room for the coins`() {
        val f = Fixture(StageAliEscaped)
        f.fill()
        f.talk(NpcHassan)
        assertEquals(StageAliEscaped, f.stage())
        assertTrue(f.said("your pack is full"))
        assertEquals(0, f.player.vars["varp.qp"])
        f.drop("obj.bronze_dagger")
        f.talk(NpcHassan)
        assertEquals(StageComplete, f.stage())
        assertEquals(700, f.count(Coins))
        assertEquals(3, f.player.vars["varp.qp"])
    }

    @Test
    fun `ned and aggie only make the disguise while the rescue is under way`() {
        val f = Fixture()
        assertFalse(f.makers.offers(f.player))
        f.setStage(StageStarted)
        assertTrue(f.makers.offers(f.player))
        f.setStage(StageAliEscaped)
        assertFalse(f.makers.offers(f.player))
    }

    @Test
    fun `ned makes a wig from three balls of wool and not from fewer`() {
        val f = Fixture(StageBriefed)
        f.give(BallOfWool, 2)
        f.choose(2)
        f.nedOtherThings()
        assertEquals(0, f.count(Wig))
        assertEquals(2, f.count(BallOfWool))

        f.give(BallOfWool, 1)
        f.choose(2, 1)
        f.nedOtherThings()
        assertEquals(1, f.count(Wig))
        assertEquals(0, f.count(BallOfWool))
    }

    @Test
    fun `aggie lists the paste ingredients until she has them all`() {
        val f = Fixture(StageBriefed)
        f.give(Ashes)
        f.give(PotOfFlour)
        f.aggieSkinPaste()
        assertTrue(f.said("ash, flour and water"))
        assertEquals(0, f.count(SkinPaste))
        assertEquals(1, f.count(Ashes))

        f.give(JugOfWater)
        f.give(Redberries)
        f.choose(1)
        f.aggieSkinPaste()
        assertEquals(1, f.count(SkinPaste))
        assertEquals(0, f.count(Ashes))
        assertEquals(0, f.count(PotOfFlour))
        assertEquals(0, f.count(JugOfWater))
        assertEquals(0, f.count(Redberries))
    }

    @Test
    fun `the cell door is locked without the key and only opens once keli is out of the way`() {
        val f = Fixture(StagePrepared)
        f.player.coords = CellOutside
        f.cellDoorOp()
        assertTrue(f.said("The gate is locked."))
        assertTrue(f.player.routeDestination.isEmpty())

        f.give(Key)
        f.cellDoorKey()
        assertTrue(f.said("deal with Lady Keli"))
        assertTrue(f.player.routeDestination.isEmpty())

        f.setStage(StageKeliTied)
        f.cellDoorKey()
        assertEquals(CellDoorTile, f.player.routeDestination.lastOrNull())
        assertTrue(f.canStep(CellOutside, 0, -1), "the door stays open while the player steps in ")

        f.player.coords = CellDoorTile
        f.player.routeDestination.clear()
        f.cellDoorOp()
        assertEquals(CellOutside, f.player.routeDestination.lastOrNull(), "let out without a key")
    }

    @Test
    fun `the toll gate charges ten coins on the way east and lets the player right through`() {
        val f = Fixture()
        f.player.coords = GateWest
        f.give(Coins, 25)
        assertFalse(f.canStep(GateWest, 1, 0), "the closed gate blocks the way")
        f.gate(InteractionOp.Op4)
        assertEquals(15, f.count(Coins))
        assertEquals(GateEast, f.player.routeDestination.lastOrNull(), "the far side, not the gate")
        assertTrue(f.canStep(GateWest, 1, 0), "the gate is open for the step")
        assertFalse(f.output().contains("Can I come through"), "no conversation on the pay option")
    }

    @Test
    fun `the toll gate stays open for the whole walk even from beside the gate`() {
        val f = Fixture()
        f.player.coords = GateWest.translateZ(-1)
        f.give(Coins, 10)
        f.gate(InteractionOp.Op4)
        assertEquals(GateEast, f.player.routeDestination.lastOrNull())
        assertTrue(TollGateOpenTicks >= 6, "open for $TollGateOpenTicks ticks")
    }

    @Test
    fun `the toll gate refuses a player who cannot pay`() {
        val f = Fixture()
        f.player.coords = GateWest
        f.give(Coins, 9)
        f.gate(InteractionOp.Op4)
        assertEquals(9, f.count(Coins))
        assertTrue(f.said("I don't actually seem to have enough money"))
        assertTrue(f.player.routeDestination.isEmpty())
        assertFalse(f.canStep(GateWest, 1, 0))
    }

    @Test
    fun `opening the toll gate asks for the toll with the three transcript options`() {
        val f = Fixture()
        f.player.coords = GateWest
        f.give(Coins, 10)
        f.choose(2)
        f.gate(InteractionOp.Op1)
        assertTrue(f.said("You must pay a toll of 10 gold coins to pass."))
        assertTrue(f.said("The money goes to the city of Al-Kharid."))
        assertEquals(10, f.count(Coins))
        assertTrue(f.player.routeDestination.isEmpty())

        f.choose(3)
        f.gate(InteractionOp.Op1)
        assertTrue(f.said("Ok suit yourself."))
        assertEquals(10, f.count(Coins))
        assertTrue(f.player.routeDestination.isEmpty())

        f.choose(1)
        f.gate(InteractionOp.Op1)
        assertEquals(0, f.count(Coins))
        assertEquals(GateEast, f.player.routeDestination.lastOrNull())
    }

    @Test
    fun `the border guard asks for the toll and a friend of Al Kharid passes for free`() =
        respectingProgress {
            val f = Fixture()
            f.player.coords = GateWest
            f.give(Coins, 10)
            f.choose(1)
            f.talk("npc.borderguard1", at = GateWest.translateZ(-1))
            assertEquals(0, f.count(Coins))
            assertEquals(GateEast, f.player.routeDestination.lastOrNull())

            val friend = Fixture(StageComplete)
            friend.player.coords = GateWest
            friend.give(Coins, 10)
            friend.talk("npc.borderguard1", at = GateWest.translateZ(-1))
            assertTrue(friend.said("You may pass for free, you are a friend of Al-Kharid."))
            assertEquals(10, friend.count(Coins))
            assertEquals(GateEast, friend.player.routeDestination.lastOrNull())
        }

    @Test
    fun `after the quest the gate just opens with no conversation`() = respectingProgress {
        val f = Fixture(StageComplete)
        f.player.coords = GateWest
        f.give(Coins, 10)
        f.gate(InteractionOp.Op1)
        assertEquals(GateEast, f.player.routeDestination.lastOrNull())
        assertEquals(10, f.count(Coins))
        assertFalse(f.output().contains("Can I come through"), "no dialogue after the quest")
        assertTrue(f.canStep(GateWest, 1, 0))
    }

    @Test
    fun `the gate is free as soon as the prince is out and before hassan pays`() = respectingProgress {
        val f = Fixture(StageAliEscaped)
        f.player.coords = GateWest
        f.give(Coins, 10)
        f.gate(InteractionOp.Op1)
        assertEquals(GateEast, f.player.routeDestination.lastOrNull())
        assertEquals(10, f.count(Coins))
        assertFalse(f.output().contains("Can I come through"), f.output())
    }

    @Test
    fun `leaving al kharid through the gate is free and silent for everyone`() {
        val f = Fixture()
        f.player.coords = GateEast
        f.give(Coins, 10)
        f.gate(InteractionOp.Op1)
        assertEquals(GateWest, f.player.routeDestination.lastOrNull())
        assertEquals(10, f.count(Coins))
        assertFalse(f.output().contains("toll"), f.output())
        assertTrue(f.canStep(GateEast, -1, 0))

        val guard = Fixture()
        guard.player.coords = GateEast
        guard.talk("npc.borderguard2", at = GateEast.translateZ(-1))
        assertEquals(GateWest, guard.player.routeDestination.lastOrNull())
        assertFalse(guard.output().contains("toll"), guard.output())
    }

    @Test
    fun `the gate resolves to the right visible op for the quest state`() {
        val f = Fixture()
        val left = checkNotNull(ServerCacheManager.getObject(GateLeft.asRSCM(RSCMType.LOC)))
        val before = checkNotNull(ServerCacheManager.getObject(left.multiLoc[0]))
        val after = checkNotNull(ServerCacheManager.getObject(left.multiLoc[StageAliEscaped]))
        assertEquals("Pay-toll(10gp)", before.actions.getOpOrNull(3))
        assertEquals(null, after.actions.getOpOrNull(3))
        assertEquals("Open", after.actions.getOpOrNull(0))
        assertTrue(f.events.contains(LocEvents.Op4::class.java, left.id))
    }

    @Test
    fun `quest progress and flags survive saving and loading`() {
        val f = Fixture(StageBriefed)
        f.quest.setMetLeela(f.player)
        f.quest.setKeyObtained(f.player)
        f.quest.setKeliAsked(f.player)
        val loaded = f.saveAndReload()
        assertEquals(StageBriefed, f.quest.stage(loaded))
        assertTrue(f.quest.metLeela(loaded))
        assertTrue(f.quest.keyObtained(loaded))
        assertTrue(f.quest.keliAsked(loaded))
        assertEquals(StageBriefed, loaded.vars["varp.princequest"])
    }

    private fun respectingProgress(block: () -> Unit) {
        val previous = QuestRequirements.activePolicy()
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
        try {
            block()
        } finally {
            QuestRequirements.install(previous)
        }
    }

    private class Fixture(stage: Int = 0) {
        val events = EventBus()
        private val client = RecordingClient()
        val collision = CollisionFlagMap()
        private val npcs = NpcList()
        private val coroutine = GameCoroutine("prince-ali-test")
        private var result: Result<Unit>? = null
        private val clock = MapClock(100)
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getAlignment = { TextAlignment() },
                    getCollision = { collision },
                    getNpcList = { npcs },
                    getNpcInteractions = { NpcInteractions(events) },
                )
        private val npcRepo: NpcRepository
        val locRepo: LocRepository
        private val picks = ArrayDeque<Int>()
        private val npcU =
            NpcUInteractions::class
                .java
                .getDeclaredConstructor(EventBus::class.java)
                .apply { isAccessible = true }
                .newInstance(events)
        private val locU =
            LocUInteractions::class
                .java
                .getDeclaredConstructor(EventBus::class.java)
                .apply { isAccessible = true }
                .newInstance(events)

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 7171L
                observerUUID = 7171L
                slotId = 1
                assignUid()
                coords = Start
                currentMapClock = 100
                processedMapClock = 100
                pendingSequence = EntitySeq.NULL
                pendingFaceAngle = EntityFaceAngle.NULL
                inv = Inventory(InventoryServerType(size = 28, flags = 0), arrayOfNulls(28))
                worn = Inventory(InventoryServerType(size = 14, flags = 0), arrayOfNulls(14))
            }

        val quest = PrinceAliRescueQuest()
        val makers = DisguiseMakers(quest)

        init {
            loadMap(collision)
            val updates = ZoneUpdateMap()
            val storage = LocZoneStorage()
            val normal = LocRegistryNormal(updates, collision, storage)
            val npcRegistry = NpcRegistry(npcs, collision, events)
            val regions =
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
            locRepo =
                LocRepository(
                    clock,
                    LocRegistry(storage, normal, LocRegistryRegion(updates, collision, storage, regions)),
                    regions,
                )
            npcRepo = NpcRepository(clock, npcRegistry, npcs)
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(quest) { scripts.startup() }
            with(Hassan(quest)) { scripts.startup() }
            with(Osman(quest)) { scripts.startup() }
            with(Leela(quest)) { scripts.startup() }
            with(LadyKeli(quest)) { scripts.startup() }
            with(Joe(quest)) { scripts.startup() }
            with(PrinceAli(quest)) { scripts.startup() }
            with(JailCellDoor(quest, locRepo)) { scripts.startup() }
            with(AlKharidTollGate(quest, locRepo)) { scripts.startup() }
            for (z in listOf(3227, 3228)) {
                val leaf = if (z == 3227) GateLeft else GateRight
                locRepo.add(CoordGrid(3268, z, 0), leaf, Int.MAX_VALUE, LocAngle.West, LocShape.WallStraight)
            }
            locRepo.add(CellDoorTile, CellDoor, Int.MAX_VALUE, LocAngle.North, LocShape.WallStraight)
            if (stage > 0) setStage(stage)
        }

        fun access() = ProtectedAccess(player, coroutine, context)

        fun stage(): Int = quest.stage(player)

        fun setStage(stage: Int) = VarPlayerIntMapSetter.set(player, "varp.princequest", stage)

        fun journal(): String = quest.questLog(access())

        fun choose(vararg options: Int) {
            picks += options.toList()
        }

        fun give(obj: String, count: Int = 1) {
            val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
            if (type.stackable) {
                val slot = player.inv.indexOfFirst { it?.id == obj.asRSCM() }
                if (slot >= 0) {
                    player.inv[slot] = InvObj(obj, checkNotNull(player.inv[slot]).count + count)
                    return
                }
                player.inv[player.inv.indexOfFirst { it == null }] = InvObj(obj, count)
                return
            }
            repeat(count) { player.inv[player.inv.indexOfFirst { it == null }] = InvObj(obj, 1) }
        }

        fun fill() {
            while (player.inv.freeSpace() > 0) give("obj.bronze_dagger")
        }

        fun drop(obj: String) {
            player.inv[player.inv.indexOfFirst { it?.id == obj.asRSCM() }] = null
        }

        fun count(obj: String): Int = player.inv.count(obj)

        fun said(text: String): Boolean = output().contains(text)

        fun giveDisguiseAndKey() {
            give(Key)
            giveDisguise()
        }

        fun giveDisguise() {
            give(BlondWig)
            give(SkinPaste)
            give(PinkSkirt)
        }

        fun imprintKey() {
            give(SoftClay)
            choose(1, 1, 2, 1, 1)
            talk(NpcKeli)
        }

        fun makeDisguise() {
            give(BallOfWool, 3)
            choose(2, 1)
            nedOtherThings()
            assertEquals(1, count(Wig))
            assertEquals(0, count(BallOfWool))
            give(YellowDye)
            use(YellowDye, Wig)
            give(Ashes)
            give(PotOfFlour)
            give(BucketOfWater)
            give(Redberries)
            choose(1)
            aggieSkinPaste()
            assertEquals(0, count(Ashes))
            assertEquals(0, count(PotOfFlour))
            assertEquals(0, count(BucketOfWater))
            assertEquals(0, count(Redberries))
            give(PinkSkirt)
        }

        fun nedOtherThings() {
            val ned = Npc("npc.ned", player.coords.translateX(1))
            npcRepo.add(ned, Int.MAX_VALUE)
            dispatch { startDialogue(ned) { with(makers) { nedOtherThings() } } }
            npcRepo.del(ned, Int.MAX_VALUE)
        }

        fun aggieSkinPaste() {
            val aggie = Npc("npc.aggie", player.coords.translateX(1))
            npcRepo.add(aggie, Int.MAX_VALUE)
            dispatch { startDialogue(aggie) { with(makers) { aggieSkinPaste() } } }
            npcRepo.del(aggie, Int.MAX_VALUE)
        }

        fun talk(type: String, at: CoordGrid = player.coords.translateX(1)) {
            val npc = Npc(type, at)
            npcRepo.add(npc, Int.MAX_VALUE)
            val trigger =
                checkNotNull(NpcInteractions(events).opTrigger(player, npc, InteractionOp.Op1)) {
                    "The engine finds no op1 handler for the spawned npc $type"
                }
            dispatch { assertTrue(events.publish(this, trigger)) }
            if (npc.isSlotAssigned) npcRepo.del(npc, Int.MAX_VALUE)
        }

        fun npcU(type: String, obj: String) {
            val npc = Npc(type, player.coords.translateX(1))
            npcRepo.add(npc, Int.MAX_VALUE)
            val slot = player.inv.indexOfFirst { it?.id == obj.asRSCM() }
            val objType = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
            val npcType = checkNotNull(ServerCacheManager.getNpc(type.asRSCM()))
            dispatch { npcU.interactOp(this, npc, player.inv, slot, npcType, objType) }
            if (npc.isSlotAssigned) npcRepo.del(npc, Int.MAX_VALUE)
        }

        fun use(first: String, second: String) {
            val a = player.inv.indexOfFirst { it?.id == first.asRSCM() }
            val b = player.inv.indexOfFirst { it?.id == second.asRSCM() }
            val typeA = checkNotNull(ServerCacheManager.getItem(first.asRSCM()))
            val typeB = checkNotNull(ServerCacheManager.getItem(second.asRSCM()))
            dispatch { assertTrue(events.publish(this, HeldUEvents.Type(typeA, a, typeB, b))) }
        }

        fun gate(op: InteractionOp) {
            val at = CoordGrid(3268, 3227, 0)
            val type = checkNotNull(ServerCacheManager.getObject(GateLeft.asRSCM(RSCMType.LOC)))
            val loc = BoundLocInfo(LocInfo(0, at, LocEntity(type.id, 0, 0)), type)
            val trigger =
                checkNotNull(LocInteractions(BoundValidator(collision), events).opTrigger(player, loc, op))
            dispatch { assertTrue(events.publish(this, trigger)) }
        }

        fun cellDoorOp() {
            val type = checkNotNull(ServerCacheManager.getObject(CellDoor.asRSCM(RSCMType.LOC)))
            val loc = BoundLocInfo(LocInfo(0, CellDoorTile, LocEntity(type.id, 0, 1)), type)
            val trigger =
                checkNotNull(
                    LocInteractions(BoundValidator(collision), events).opTrigger(player, loc, InteractionOp.Op1)
                )
            dispatch { assertTrue(events.publish(this, trigger)) }
        }

        fun cellDoorKey() = useOnLoc(CellDoor, CellDoorTile, Key, shape = 0)

        fun furnaceUse() {
            val furnace = "category.furnace".asRSCM(RSCMType.CATEGORY)
            val type =
                ServerCacheManager.getObjects().values.first { it.category == furnace && it.actions.getOpOrNull(0) != null }
            useOnLoc(locName(type.id), Start.translateX(1), KeyPrint)
        }

        private fun locName(id: Int): String = dev.openrune.rscm.RSCM.getReverseMapping(RSCMType.LOC, id)

        private fun useOnLoc(symbol: String, coords: CoordGrid, obj: String, shape: Int = 10) {
            val type = checkNotNull(ServerCacheManager.getObject(symbol.asRSCM(RSCMType.LOC)))
            val loc = BoundLocInfo(LocInfo(0, coords, LocEntity(type.id, shape, 1)), type)
            val objType = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
            val slot = player.inv.indexOfFirst { it?.id == obj.asRSCM() }
            val event = with(locU) { access().opTrigger(loc, loc, type, objType, slot) }
            val trigger = checkNotNull(event) { "No $obj handler for $symbol" }
            dispatch { assertTrue(events.publish(this, trigger)) }
        }

        fun canStep(from: CoordGrid, dx: Int, dz: Int): Boolean =
            StepValidator(collision).canTravel(from.level, from.x, from.z, dx, dz)

        @OptIn(InternalApi::class)
        fun saveAndReload(): Player {
            val loaded = Player()
            loaded.vars.backing.putAll(player.vars.backing)
            return loaded
        }

        private fun dispatch(block: suspend ProtectedAccess.() -> Unit) {
            player.clearPendingAction(events)
            player.routeDestination.clear()
            result = null
            player.activeCoroutine = coroutine
            val access = access()
            val start: suspend () -> Unit = { access.block() }
            start.startCoroutine(
                object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<Unit>) {
                        this@Fixture.result = result
                    }
                }
            )
            result?.getOrThrow()
            repeat(600) {
                if (coroutine.isIdle) {
                    picks.clear()
                    return
                }
                step()
                result?.getOrThrow()
            }
            fail<Unit>("Interaction did not finish: ${output()}")
        }

        private fun step() {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val parent =
                    listOf(
                            "chat_left",
                            "chat_right",
                            "messagebox",
                            "chatmenu",
                            "objectbox",
                            "objectbox_double",
                        )
                        .firstOrNull { player.ui.containsModal("interface.$it") }
                        ?: error("Unknown dialogue: ${output()}")
                val input =
                    when (parent) {
                        "chatmenu" ->
                            ResumePauseButtonInput(
                                "component.chatmenu:options",
                                picks.removeFirstOrNull() ?: 1,
                            )
                        "objectbox" -> ResumePauseButtonInput("component.objectbox:universe", -1)
                        "objectbox_double" ->
                            ResumePauseButtonInput("component.objectbox_double:pausebutton", -1)
                        else -> ResumePauseButtonInput("component.$parent:continue", -1)
                    }
                coroutine.resumeWith(input)
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                player.pendingSequence = EntitySeq.NULL
                player.pendingFaceAngle = EntityFaceAngle.NULL
                coroutine.advance()
            }
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
        val Start = CoordGrid(3120, 3250, 0)
        val GateWest = CoordGrid(3267, 3227, 0)
        val GateEast = CoordGrid(3268, 3227, 0)
        val CellDoorTile = CoordGrid(3123, 3243, 0)
        val CellOutside = CoordGrid(3123, 3244, 0)

        const val GateLeft = "loc.kharidmetalgateclosedl"
        const val GateRight = "loc.kharidmetalgateclosedr"
        const val CellDoor = "loc.alidoor"

        const val OsmanLeave = 3
        const val LeelaLeave = 3

        private val Squares = listOf(51 to 50, 48 to 50).map { (x, z) -> MapSquareKey(x, z) }
        private val restored = mutableListOf<() -> Unit>()
        private lateinit var cache: dev.openrune.filesystem.Cache

        fun loadMap(collision: CollisionFlagMap) {
            for (square in Squares) {
                val group = (square.x shl 8) or square.z
                val tiles =
                    MapTileDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, group, 0))))
                val spawns =
                    MapLocListDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, group, 1))))
                for (level in 0..3) for (x in square.x * 64 until square.x * 64 + 64 step 8) {
                    for (z in square.z * 64 until square.z * 64 + 64 step 8) {
                        collision.allocateIfAbsent(x, z, level)
                    }
                }
                GameMapDecoder.putMaps(collision, square, tiles)
                GameMapDecoder.putLocs(GameMapBuilder(), collision, square, tiles, spawns)
            }
        }

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            cache = ServerCacheManager.init(240)
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
            cache.close()
        }
    }
}
