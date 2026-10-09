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
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.ASHES
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.BALL_OF_WOOL
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.BEER
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.BLOND_WIG
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.BRONZE_BAR
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.BUCKET_OF_WATER
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.COINS
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.JUG_OF_WATER
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.KEY
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.KEY_PRINT
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NPC_HASSAN
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NPC_JOE
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NPC_KELI
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NPC_LEELA
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NPC_OSMAN
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NPC_PRINCE_CELL
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NPC_PRINCE_PALACE
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.PINK_SKIRT
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.POT_OF_FLOUR
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.QUEST_KEY
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.REDBERRIES
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.ROPE
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.SKIN_PASTE
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.SOFT_CLAY
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_ALI_ESCAPED
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_BRIEFED
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_COMPLETE
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_JOE_DRUNK
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_KELI_TIED
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_PREPARED
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_STARTED
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.WIG
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.YELLOW_DYE
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
            assertFalse(QuestRequirements.hasCompleted(f.player, QUEST_KEY))

            f.choose(1, 1)
            f.talk(NPC_HASSAN)
            assertEquals(STAGE_STARTED, f.stage())
            assertTrue(f.journal().contains("Osman"))

            f.choose(OSMAN_LEAVE)
            f.talk(NPC_OSMAN)
            assertEquals(STAGE_BRIEFED, f.stage())
            assertTrue(f.said("abandoned jail just east of Draynor Village"))

            f.imprintKey()
            assertEquals(1, f.count(KEY_PRINT))
            assertEquals(0, f.count(SOFT_CLAY))
            assertTrue(f.journal().contains("imprint"))

            f.give(BRONZE_BAR)
            f.choose(OSMAN_LEAVE)
            f.talk(NPC_OSMAN)
            assertTrue(f.quest.keyOrdered(f.player))
            assertEquals(0, f.count(KEY_PRINT))
            assertEquals(0, f.count(BRONZE_BAR))

            f.choose(LEELA_LEAVE)
            f.talk(NPC_LEELA)
            assertTrue(f.quest.keyObtained(f.player))
            assertFalse(f.quest.keyOrdered(f.player))
            assertEquals(1, f.count(KEY))
            assertEquals(STAGE_BRIEFED, f.stage())

            f.makeDisguise()
            assertEquals(1, f.count(BLOND_WIG))
            assertEquals(1, f.count(SKIN_PASTE))

            f.talk(NPC_LEELA)
            assertEquals(STAGE_PREPARED, f.stage())
            assertTrue(f.said("deal with his personal guard"))

            f.give(BEER, 3)
            f.choose(1)
            f.talk(NPC_JOE)
            assertEquals(STAGE_JOE_DRUNK, f.stage())
            assertEquals(0, f.count(BEER))

            f.give(ROPE)
            f.npcU(NPC_KELI, ROPE)
            assertEquals(STAGE_KELI_TIED, f.stage())
            assertEquals(0, f.count(ROPE))
            assertTrue(f.said("tie her up"))

            f.talk(NPC_PRINCE_CELL)
            assertEquals(STAGE_ALI_ESCAPED, f.stage())
            assertEquals(0, f.count(BLOND_WIG))
            assertEquals(0, f.count(SKIN_PASTE))
            assertEquals(0, f.count(PINK_SKIRT))

            f.talk(NPC_HASSAN)
            assertEquals(STAGE_COMPLETE, f.stage())
            assertEquals(3, f.player.vars["varp.qp"])
            assertEquals(700, f.count(COINS))
            assertTrue(f.player.ui.containsModal("interface.questscroll"))
            assertTrue(QuestRequirements.hasCompleted(f.player, QUEST_KEY))

            f.talk(NPC_HASSAN)
            assertEquals(700, f.count(COINS))
            assertEquals(3, f.player.vars["varp.qp"])
            f.talk(NPC_PRINCE_PALACE)
            assertTrue(f.said("forever in your debt"))
        }

    @Test
    fun `the key can be made at a furnace from the imprint and a bronze bar`() {
        val f = Fixture(STAGE_BRIEFED)
        f.give(KEY_PRINT)
        f.furnaceUse()
        assertEquals(1, f.count(KEY_PRINT))
        assertEquals(0, f.count(KEY))
        assertTrue(f.said("You need a bronze bar"))

        f.give(BRONZE_BAR)
        f.choose(2)
        f.furnaceUse()
        assertEquals(1, f.count(KEY_PRINT))
        assertEquals(1, f.count(BRONZE_BAR))

        f.choose(1)
        f.furnaceUse()
        assertEquals(0, f.count(KEY_PRINT))
        assertEquals(0, f.count(BRONZE_BAR))
        assertEquals(1, f.count(KEY))
        assertTrue(f.quest.keyObtained(f.player))
        assertTrue(f.player.statMap.getXP("stat.crafting") > 0)
        assertTrue(f.journal().contains("copy of the cell key"))
    }

    @Test
    fun `the furnace route reaches Leela's go-ahead without Osman ever touching the key`() =
        respectingProgress {
            val f = Fixture(STAGE_BRIEFED)
            f.imprintKey()
            assertEquals(1, f.count(KEY_PRINT))
            f.give(BRONZE_BAR)
            f.choose(1)
            f.furnaceUse()
            assertEquals(1, f.count(KEY))
            assertFalse(f.quest.keyOrdered(f.player))

            f.giveDisguise()
            f.choose(LEELA_LEAVE)
            f.talk(NPC_LEELA)
            assertEquals(STAGE_BRIEFED, f.stage())
            f.talk(NPC_LEELA)
            assertEquals(STAGE_PREPARED, f.stage())
            assertEquals(1, f.count(KEY))
        }

    @Test
    fun `declining Hassan leaves the quest unstarted`() {
        val f = Fixture()
        f.choose(1, 2)
        f.talk(NPC_HASSAN)
        assertEquals(0, f.stage())
        f.choose(4)
        f.talk(NPC_HASSAN)
        assertEquals(0, f.stage())
    }

    @Test
    fun `Hassan hands out a jug of water when it is too hot`() {
        val f = Fixture()
        f.choose(2, 4)
        f.talk(NPC_HASSAN)
        assertEquals(1, f.count(JUG_OF_WATER))
        assertEquals(0, f.stage())
    }

    @Test
    fun `the quest npcs keep to themselves before the briefing`() {
        val f = Fixture(STAGE_STARTED)
        f.give(SOFT_CLAY)
        f.talk(NPC_KELI)
        assertTrue(f.said("Clear off then."))
        f.talk(NPC_LEELA)
        assertTrue(f.said("That is no concern of yours"))
        assertEquals(1, f.count(SOFT_CLAY))
        assertEquals(STAGE_STARTED, f.stage())
    }

    @Test
    fun `keli shows the key but no imprint is taken without soft clay`() = respectingProgress {
        val f = Fixture(STAGE_BRIEFED)
        f.choose(1, 1, 2, 1)
        f.talk(NPC_KELI)
        assertTrue(f.said("Keli shows you a small key"))
        assertEquals(0, f.count(KEY_PRINT))
        assertTrue(f.quest.keliAsked(f.player))
        f.give(SOFT_CLAY)
        f.choose(2, 1, 1)
        f.talk(NPC_KELI)
        assertTrue(f.said("Hello again!"))
        assertEquals(1, f.count(KEY_PRINT))
        assertEquals(0, f.count(SOFT_CLAY))
    }

    @Test
    fun `osman keeps the imprint until a bronze bar comes with it`() {
        val f = Fixture(STAGE_BRIEFED)
        f.give(KEY_PRINT)
        f.choose(OSMAN_LEAVE)
        f.talk(NPC_OSMAN)
        assertTrue(f.said("Bring me a bronze bar"))
        assertEquals(1, f.count(KEY_PRINT))
        assertFalse(f.quest.keyOrdered(f.player))
    }

    @Test
    fun `leela waits for the whole disguise and a dyed wig`() {
        val f = Fixture(STAGE_BRIEFED)
        f.quest.setMetLeela(f.player)
        f.give(KEY)
        f.give(WIG)
        f.give(SKIN_PASTE)
        f.give(PINK_SKIRT)
        f.choose(LEELA_LEAVE)
        f.talk(NPC_LEELA)
        assertEquals(STAGE_BRIEFED, f.stage())
        f.give(YELLOW_DYE)
        f.use(YELLOW_DYE, WIG)
        assertEquals(1, f.count(BLOND_WIG))
        assertEquals(0, f.count(WIG))
        assertEquals(0, f.count(YELLOW_DYE))
        f.talk(NPC_LEELA)
        assertEquals(STAGE_PREPARED, f.stage())
    }

    @Test
    fun `leela still gives her briefing at the first meeting even with everything ready`() {
        val f = Fixture(STAGE_BRIEFED)
        f.give(KEY)
        f.give(BLOND_WIG)
        f.give(SKIN_PASTE)
        f.give(PINK_SKIRT)
        f.choose(LEELA_LEAVE)
        f.talk(NPC_LEELA)
        assertTrue(f.said("I'd say that's a good summary."))
        assertEquals(STAGE_BRIEFED, f.stage())
        f.talk(NPC_LEELA)
        assertEquals(STAGE_PREPARED, f.stage())
    }

    @Test
    fun `every quest npc is spawned on the base id the handlers bind`() {
        val f = Fixture()
        for (npc in listOf(NPC_HASSAN, NPC_OSMAN, NPC_LEELA, NPC_KELI, NPC_JOE, NPC_PRINCE_CELL, NPC_PRINCE_PALACE)) {
            val id = npc.asRSCM(RSCMType.NPC)
            assertTrue(f.events.contains(NpcEvents.Op1::class.java, id), "$npc has no op1 handler")
        }
        for (visible in listOf("npc.lady_keli_vis", "npc.joe_vis", "npc.prince_ali_vis_blackeye", "npc.prince_ali_vis")) {
            assertFalse(f.events.contains(NpcEvents.Op1::class.java, visible.asRSCM(RSCMType.NPC)), visible)
        }
    }

    @Test
    fun `the prince stays put with the disguise but without the key`() {
        val f = Fixture(STAGE_KELI_TIED)
        f.give(BLOND_WIG)
        f.give(SKIN_PASTE)
        f.give(PINK_SKIRT)
        f.talk(NPC_PRINCE_CELL)
        assertEquals(STAGE_KELI_TIED, f.stage())
        assertEquals(1, f.count(BLOND_WIG))
    }

    @Test
    fun `a lost key is replaced for fifteen coins`() {
        val f = Fixture(STAGE_PREPARED)
        f.quest.setMetLeela(f.player)
        f.quest.setKeyObtained(f.player)
        f.talk(NPC_LEELA)
        assertTrue(f.said("I haven't got that much."))
        assertEquals(0, f.count(KEY))
        f.give(COINS, 20)
        f.talk(NPC_LEELA)
        assertEquals(1, f.count(KEY))
        assertEquals(5, f.count(COINS))
        f.talk(NPC_LEELA)
        assertEquals(1, f.count(KEY))
        assertEquals(5, f.count(COINS))
    }

    @Test
    fun `one beer is remembered and two more finish the job`() {
        val f = Fixture(STAGE_PREPARED)
        f.give(BEER, 2)
        f.choose(1)
        f.talk(NPC_JOE)
        assertEquals(STAGE_PREPARED, f.stage())
        assertEquals(1, f.count(BEER))
        assertTrue(f.said("at least two more"))
        assertTrue(f.quest.joeHadBeer(f.player))

        f.give(BEER, 1)
        f.choose(1)
        f.talk(NPC_JOE)
        assertEquals(STAGE_JOE_DRUNK, f.stage())
        assertEquals(0, f.count(BEER))
    }

    @Test
    fun `joe does not talk before leela has sent the player`() {
        val f = Fixture(STAGE_BRIEFED)
        f.give(BEER, 3)
        f.talk(NPC_JOE)
        assertTrue(f.said("Can't say. It's all very secret."))
        assertEquals(3, f.count(BEER))
    }

    @Test
    fun `keli cannot be tied up before joe is drunk or without the disguise`() = respectingProgress {
        val f = Fixture(STAGE_PREPARED)
        f.give(ROPE)
        f.npcU(NPC_KELI, ROPE)
        assertEquals(STAGE_PREPARED, f.stage())
        assertEquals(1, f.count(ROPE))
        assertTrue(f.said("You cannot tie Keli up"))

        f.setStage(STAGE_JOE_DRUNK)
        f.npcU(NPC_KELI, ROPE)
        assertEquals(STAGE_JOE_DRUNK, f.stage())
        assertEquals(1, f.count(ROPE))

        f.giveDisguiseAndKey()
        f.talk(NPC_KELI)
        assertTrue(f.said("I'm here to tie you up!"))
        assertEquals(STAGE_KELI_TIED, f.stage())
        assertEquals(0, f.count(ROPE))
    }

    @Test
    fun `the prince stays put without his disguise`() {
        val f = Fixture(STAGE_KELI_TIED)
        f.give(KEY)
        f.give(BLOND_WIG)
        f.talk(NPC_PRINCE_CELL)
        assertEquals(STAGE_KELI_TIED, f.stage())
        assertEquals(1, f.count(BLOND_WIG))
        assertTrue(f.said("I'll be back once I have it."))
    }

    @Test
    fun `hassan holds the payment back until there is room for the coins`() {
        val f = Fixture(STAGE_ALI_ESCAPED)
        f.fill()
        f.talk(NPC_HASSAN)
        assertEquals(STAGE_ALI_ESCAPED, f.stage())
        assertTrue(f.said("your pack is full"))
        assertEquals(0, f.player.vars["varp.qp"])
        f.drop("obj.bronze_dagger")
        f.talk(NPC_HASSAN)
        assertEquals(STAGE_COMPLETE, f.stage())
        assertEquals(700, f.count(COINS))
        assertEquals(3, f.player.vars["varp.qp"])
    }

    @Test
    fun `ned and aggie only make the disguise while the rescue is under way`() {
        val f = Fixture()
        assertFalse(f.makers.offers(f.player))
        f.setStage(STAGE_STARTED)
        assertTrue(f.makers.offers(f.player))
        f.setStage(STAGE_ALI_ESCAPED)
        assertFalse(f.makers.offers(f.player))
    }

    @Test
    fun `ned makes a wig from three balls of wool and not from fewer`() {
        val f = Fixture(STAGE_BRIEFED)
        f.give(BALL_OF_WOOL, 2)
        f.choose(2)
        f.nedOtherThings()
        assertEquals(0, f.count(WIG))
        assertEquals(2, f.count(BALL_OF_WOOL))

        f.give(BALL_OF_WOOL, 1)
        f.choose(2, 1)
        f.nedOtherThings()
        assertEquals(1, f.count(WIG))
        assertEquals(0, f.count(BALL_OF_WOOL))
    }

    @Test
    fun `aggie lists the paste ingredients until she has them all`() {
        val f = Fixture(STAGE_BRIEFED)
        f.give(ASHES)
        f.give(POT_OF_FLOUR)
        f.aggieSkinPaste()
        assertTrue(f.said("ash, flour and water"))
        assertEquals(0, f.count(SKIN_PASTE))
        assertEquals(1, f.count(ASHES))

        f.give(JUG_OF_WATER)
        f.give(REDBERRIES)
        f.choose(1)
        f.aggieSkinPaste()
        assertEquals(1, f.count(SKIN_PASTE))
        assertEquals(0, f.count(ASHES))
        assertEquals(0, f.count(POT_OF_FLOUR))
        assertEquals(0, f.count(JUG_OF_WATER))
        assertEquals(0, f.count(REDBERRIES))
    }

    @Test
    fun `the cell door is locked without the key and only opens once keli is out of the way`() {
        val f = Fixture(STAGE_PREPARED)
        f.player.coords = CELL_OUTSIDE
        f.cellDoorOp()
        assertTrue(f.said("The gate is locked."))
        assertTrue(f.player.routeDestination.isEmpty())

        f.give(KEY)
        f.cellDoorKey()
        assertTrue(f.said("deal with Lady Keli"))
        assertTrue(f.player.routeDestination.isEmpty())

        f.setStage(STAGE_KELI_TIED)
        f.cellDoorKey()
        assertEquals(CELL_DOOR_TILE, f.player.routeDestination.lastOrNull())
        assertTrue(f.canStep(CELL_OUTSIDE, 0, -1), "the door stays open while the player steps in ")

        f.player.coords = CELL_DOOR_TILE
        f.player.routeDestination.clear()
        f.cellDoorOp()
        assertEquals(CELL_OUTSIDE, f.player.routeDestination.lastOrNull(), "let out without a key")
    }

    @Test
    fun `the toll gate charges ten coins on the way east and lets the player right through`() {
        val f = Fixture()
        f.player.coords = GATE_WEST
        f.give(COINS, 25)
        assertFalse(f.canStep(GATE_WEST, 1, 0), "the closed gate blocks the way")
        f.gate(InteractionOp.Op4)
        assertEquals(15, f.count(COINS))
        assertEquals(GATE_EAST, f.player.routeDestination.lastOrNull(), "the far side, not the gate")
        assertTrue(f.canStep(GATE_WEST, 1, 0), "the gate is open for the step")
        assertFalse(f.output().contains("Can I come through"), "no conversation on the pay option")
    }

    @Test
    fun `the toll gate stays open for the whole walk even from beside the gate`() {
        val f = Fixture()
        f.player.coords = GATE_WEST.translateZ(-1)
        f.give(COINS, 10)
        f.gate(InteractionOp.Op4)
        assertEquals(GATE_EAST, f.player.routeDestination.lastOrNull())
        assertTrue(TOLL_GATE_OPEN_TICKS >= 6, "open for $TOLL_GATE_OPEN_TICKS ticks")
    }

    @Test
    fun `the toll gate refuses a player who cannot pay`() {
        val f = Fixture()
        f.player.coords = GATE_WEST
        f.give(COINS, 9)
        f.gate(InteractionOp.Op4)
        assertEquals(9, f.count(COINS))
        assertTrue(f.said("I don't actually seem to have enough money"))
        assertTrue(f.player.routeDestination.isEmpty())
        assertFalse(f.canStep(GATE_WEST, 1, 0))
    }

    @Test
    fun `opening the toll gate asks for the toll with the three transcript options`() {
        val f = Fixture()
        f.player.coords = GATE_WEST
        f.give(COINS, 10)
        f.choose(2)
        f.gate(InteractionOp.Op1)
        assertTrue(f.said("You must pay a toll of 10 gold coins to pass."))
        assertTrue(f.said("The money goes to the city of Al-Kharid."))
        assertEquals(10, f.count(COINS))
        assertTrue(f.player.routeDestination.isEmpty())

        f.choose(3)
        f.gate(InteractionOp.Op1)
        assertTrue(f.said("Ok suit yourself."))
        assertEquals(10, f.count(COINS))
        assertTrue(f.player.routeDestination.isEmpty())

        f.choose(1)
        f.gate(InteractionOp.Op1)
        assertEquals(0, f.count(COINS))
        assertEquals(GATE_EAST, f.player.routeDestination.lastOrNull())
    }

    @Test
    fun `the border guard asks for the toll and a friend of Al Kharid passes for free`() =
        respectingProgress {
            val f = Fixture()
            f.player.coords = GATE_WEST
            f.give(COINS, 10)
            f.choose(1)
            f.talk("npc.borderguard1", at = GATE_WEST.translateZ(-1))
            assertEquals(0, f.count(COINS))
            assertEquals(GATE_EAST, f.player.routeDestination.lastOrNull())

            val friend = Fixture(STAGE_COMPLETE)
            friend.player.coords = GATE_WEST
            friend.give(COINS, 10)
            friend.talk("npc.borderguard1", at = GATE_WEST.translateZ(-1))
            assertTrue(friend.said("You may pass for free, you are a friend of Al-Kharid."))
            assertEquals(10, friend.count(COINS))
            assertEquals(GATE_EAST, friend.player.routeDestination.lastOrNull())
        }

    @Test
    fun `after the quest the gate just opens with no conversation`() = respectingProgress {
        val f = Fixture(STAGE_COMPLETE)
        f.player.coords = GATE_WEST
        f.give(COINS, 10)
        f.gate(InteractionOp.Op1)
        assertEquals(GATE_EAST, f.player.routeDestination.lastOrNull())
        assertEquals(10, f.count(COINS))
        assertFalse(f.output().contains("Can I come through"), "no dialogue after the quest")
        assertTrue(f.canStep(GATE_WEST, 1, 0))
    }

    @Test
    fun `the gate is free as soon as the prince is out and before hassan pays`() = respectingProgress {
        val f = Fixture(STAGE_ALI_ESCAPED)
        f.player.coords = GATE_WEST
        f.give(COINS, 10)
        f.gate(InteractionOp.Op1)
        assertEquals(GATE_EAST, f.player.routeDestination.lastOrNull())
        assertEquals(10, f.count(COINS))
        assertFalse(f.output().contains("Can I come through"), f.output())
    }

    @Test
    fun `leaving al kharid through the gate is free and silent for everyone`() {
        val f = Fixture()
        f.player.coords = GATE_EAST
        f.give(COINS, 10)
        f.gate(InteractionOp.Op1)
        assertEquals(GATE_WEST, f.player.routeDestination.lastOrNull())
        assertEquals(10, f.count(COINS))
        assertFalse(f.output().contains("toll"), f.output())
        assertTrue(f.canStep(GATE_EAST, -1, 0))

        val guard = Fixture()
        guard.player.coords = GATE_EAST
        guard.talk("npc.borderguard2", at = GATE_EAST.translateZ(-1))
        assertEquals(GATE_WEST, guard.player.routeDestination.lastOrNull())
        assertFalse(guard.output().contains("toll"), guard.output())
    }

    @Test
    fun `the gate resolves to the right visible op for the quest state`() {
        val f = Fixture()
        val left = checkNotNull(ServerCacheManager.getObject(GATE_LEFT.asRSCM(RSCMType.LOC)))
        val before = checkNotNull(ServerCacheManager.getObject(left.multiLoc[0]))
        val after = checkNotNull(ServerCacheManager.getObject(left.multiLoc[STAGE_ALI_ESCAPED]))
        assertEquals("Pay-toll(10gp)", before.actions.getOpOrNull(3))
        assertEquals(null, after.actions.getOpOrNull(3))
        assertEquals("Open", after.actions.getOpOrNull(0))
        assertTrue(f.events.contains(LocEvents.Op4::class.java, left.id))
    }

    @Test
    fun `quest progress and flags survive saving and loading`() {
        val f = Fixture(STAGE_BRIEFED)
        f.quest.setMetLeela(f.player)
        f.quest.setKeyObtained(f.player)
        f.quest.setKeliAsked(f.player)
        val loaded = f.saveAndReload()
        assertEquals(STAGE_BRIEFED, f.quest.stage(loaded))
        assertTrue(f.quest.metLeela(loaded))
        assertTrue(f.quest.keyObtained(loaded))
        assertTrue(f.quest.keliAsked(loaded))
        assertEquals(STAGE_BRIEFED, loaded.vars["varp.princequest"])
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
                coords = START
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
                val leaf = if (z == 3227) GATE_LEFT else GATE_RIGHT
                locRepo.add(CoordGrid(3268, z, 0), leaf, Int.MAX_VALUE, LocAngle.West, LocShape.WallStraight)
            }
            locRepo.add(CELL_DOOR_TILE, CELL_DOOR, Int.MAX_VALUE, LocAngle.North, LocShape.WallStraight)
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
            give(KEY)
            giveDisguise()
        }

        fun giveDisguise() {
            give(BLOND_WIG)
            give(SKIN_PASTE)
            give(PINK_SKIRT)
        }

        fun imprintKey() {
            give(SOFT_CLAY)
            choose(1, 1, 2, 1, 1)
            talk(NPC_KELI)
        }

        fun makeDisguise() {
            give(BALL_OF_WOOL, 3)
            choose(2, 1)
            nedOtherThings()
            assertEquals(1, count(WIG))
            assertEquals(0, count(BALL_OF_WOOL))
            give(YELLOW_DYE)
            use(YELLOW_DYE, WIG)
            give(ASHES)
            give(POT_OF_FLOUR)
            give(BUCKET_OF_WATER)
            give(REDBERRIES)
            choose(1)
            aggieSkinPaste()
            assertEquals(0, count(ASHES))
            assertEquals(0, count(POT_OF_FLOUR))
            assertEquals(0, count(BUCKET_OF_WATER))
            assertEquals(0, count(REDBERRIES))
            give(PINK_SKIRT)
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
            val type = checkNotNull(ServerCacheManager.getObject(GATE_LEFT.asRSCM(RSCMType.LOC)))
            val loc = BoundLocInfo(LocInfo(0, at, LocEntity(type.id, 0, 0)), type)
            val trigger =
                checkNotNull(LocInteractions(BoundValidator(collision), events).opTrigger(player, loc, op))
            dispatch { assertTrue(events.publish(this, trigger)) }
        }

        fun cellDoorOp() {
            val type = checkNotNull(ServerCacheManager.getObject(CELL_DOOR.asRSCM(RSCMType.LOC)))
            val loc = BoundLocInfo(LocInfo(0, CELL_DOOR_TILE, LocEntity(type.id, 0, 1)), type)
            val trigger =
                checkNotNull(
                    LocInteractions(BoundValidator(collision), events).opTrigger(player, loc, InteractionOp.Op1)
                )
            dispatch { assertTrue(events.publish(this, trigger)) }
        }

        fun cellDoorKey() = useOnLoc(CELL_DOOR, CELL_DOOR_TILE, KEY, shape = 0)

        fun furnaceUse() {
            val furnace = "category.furnace".asRSCM(RSCMType.CATEGORY)
            val type =
                ServerCacheManager.getObjects().values.first { it.category == furnace && it.actions.getOpOrNull(0) != null }
            useOnLoc(locName(type.id), START.translateX(1), KEY_PRINT)
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
        val START = CoordGrid(3120, 3250, 0)
        val GATE_WEST = CoordGrid(3267, 3227, 0)
        val GATE_EAST = CoordGrid(3268, 3227, 0)
        val CELL_DOOR_TILE = CoordGrid(3123, 3243, 0)
        val CELL_OUTSIDE = CoordGrid(3123, 3244, 0)

        const val GATE_LEFT = "loc.kharidmetalgateclosedl"
        const val GATE_RIGHT = "loc.kharidmetalgateclosedr"
        const val CELL_DOOR = "loc.alidoor"

        const val OSMAN_LEAVE = 3
        const val LEELA_LEAVE = 3

        private val SQUARES = listOf(51 to 50, 48 to 50).map { (x, z) -> MapSquareKey(x, z) }
        private val restored = mutableListOf<() -> Unit>()
        private lateinit var cache: dev.openrune.filesystem.Cache

        fun loadMap(collision: CollisionFlagMap) {
            for (square in SQUARES) {
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
