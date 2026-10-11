package org.rsmod.content.other.treasure.trails

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.mock
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.music.MusicRepository
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.events.skilling.PickpocketSuccessEvent
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.interact.ContextualInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onEvent
import org.rsmod.content.skills.thieving.ThievingScript
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
class TrailElfPickpocketTest {
    @Test fun `native theft followed by Sherlock talk advances or awards the final casket exactly once`() {
        for (last in listOf(false, true)) {
            val f = Fixture()
            if (last) f.player.inv[0] = f.player.inv[0]!!.copy(vars = TrailState(TrailSkillChallenges.elfTask, 3, 2, 9).encode())
            val targets = TrailTargets(f.progress)
            val contextual = ContextualInteractions()
            val interaction = TrailInteractionScript(f.progress, targets, TrailRequirements(f.progress.catalog),
                mock(TrailPuzzleScript::class.java), mock(TrailGuards::class.java), contextual, mock(MusicRepository::class.java))
            with(interaction) { f.scripts.startup() }
            val clue = f.progress.catalog.clues.getValue(TrailSkillChallenges.elfTask)
            val fields = f.progress.catalog.fields(clue.targets.single())
            val sherlock = Npc(ServerCacheManager.getNpc(fields.int("npc"))!!, CoordGrid(fields.int("coord")))
            val event = contextual.npc(f.player, sherlock, InteractionOp.Op1)!!
            f.run { f.bus.publish(f.access, event) }
            assertEquals(9, f.state().phase)
            f.pick()
            assertEquals(10, f.state().phase)
            f.run { f.bus.publish(f.access, event) }
            if (last) assertEquals(1, f.player.inv.objs.filterNotNull().filter { it.id == TrailTier.MASTER.casket }.sumOf { it.count })
            else assertEquals(3, f.state().completed)
            val snapshot = f.player.inv.objs.toList()
            f.run { f.bus.publish(f.access, event) }
            assertEquals(snapshot, f.player.inv.objs.toList())
            with(interaction) { f.scripts.shutdown() }
        }
    }

    @Test fun `native NPC pickpocket commits loot and completes only the assigned elf task once`() {
        for (symbol in listOf("npc.mourning_town_elf_1", "npc.prif_citizen_miriel", "npc.prif_citizen_aranwe")) {
            val f = Fixture()
            f.pick(symbol)
            assertEquals(1, f.player.inv.count("obj.pickpocket_coin_pouch_elf"))
            assertEquals(353, f.player.statMap.getXP("stat.thieving"))
            assertEquals(10, f.state().phase)
            assertEquals(2, f.state().completed)
            f.player.inv[0] = f.player.inv[0]!!.copy()
            f.pick(symbol)
            assertEquals(10, f.state().phase)
            assertEquals(2, f.state().completed)
            assertEquals(2, f.player.inv.count("obj.pickpocket_coin_pouch_elf"))
            assertEquals(2, f.successes.size)
        }
    }

    @Test fun `wrong NPC unassigned clue and spawned or opened pouches give no task credit`() {
        val f = Fixture()
        f.pick("npc.man")
        assertEquals(9, f.state().phase)
        f.player.inv[1] = InvObj("obj.pickpocket_coin_pouch_elf", 2)
        val item = f.player.inv[1]!!
        f.run { f.bus.publish(f.access, HeldObjEvents.Op1(1, item, ServerCacheManager.getItem(item.id)!!, f.player.inv)) }
        assertEquals(9, f.state().phase)
        assertEquals(0, f.player.inv.count("obj.pickpocket_coin_pouch_elf"))
        f.clue(phase = 0)
        f.pick()
        assertEquals(0, f.state().phase)
        f.clue(row = "dbrow.cluehelper_skillchallenge_master_12", phase = 9)
        f.pick()
        assertEquals(9, f.state().phase)
    }

    @Test fun `native failed chance stun prevents reward and clue progress`() {
        val f = Fixture(success = false)
        f.pick()
        assertEquals(9, f.state().phase)
        assertEquals(0, f.player.statMap.getXP("stat.thieving"))
        assertEquals(0, f.player.inv.count("obj.pickpocket_coin_pouch_elf"))
        assertTrue(f.player.isFrozen)
        assertTrue(f.successes.isEmpty())
    }

    @Test fun `level inventory cap and interruption reject native pickpocket completion`() {
        val f = Fixture()
        f.player.statMap.setCurrentLevel("stat.thieving", 84)
        f.pick()
        f.player.statMap.setCurrentLevel("stat.thieving", 99)
        for (slot in 1..27) f.player.inv[slot] = InvObj("obj.abyssal_whip")
        f.pick()
        f.player.inv[1] = InvObj("obj.pickpocket_coin_pouch_elf", 28)
        f.pick()
        f.player.inv[1] = null
        var finished: Result<Unit>? = null
        suspend { f.bus.publish(f.access, NpcEvents.Op3(Npc("npc.mourning_town_elf_1", f.player.coords))); Unit }
            .startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { finished = result }
            })
        assertNull(finished)
        assertTrue(f.coroutine.isSuspended)
        f.coroutine.cancel()
        assertTrue(finished!!.isFailure)
        assertEquals(9, f.state().phase)
        assertEquals(0, f.player.statMap.getXP("stat.thieving"))
        assertTrue(f.successes.isEmpty())
    }

    @Test fun `inventory becoming full during the native attempt awards no XP or clue credit`() {
        val f = Fixture()
        var outcome: Result<Unit>? = null
        suspend { f.bus.publish(f.access, NpcEvents.Op3(Npc("npc.mourning_town_elf_1", f.player.coords))); Unit }
            .startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { outcome = result }
            })
        assertNull(outcome)
        for (slot in 1..27) f.player.inv[slot] = InvObj("obj.abyssal_whip")
        f.player.currentMapClock++
        f.player.processedMapClock = f.player.currentMapClock
        f.coroutine.advance()
        outcome!!.getOrThrow()
        assertEquals(9, f.state().phase)
        assertEquals(0, f.player.statMap.getXP("stat.thieving"))
        assertTrue(f.successes.isEmpty())
    }

    private class Fixture(success: Boolean = true) {
        val bus = EventBus()
        val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
        val progress = TrailProgress(TrailCatalog(), DefaultGameRandom(42))
        val coroutine = GameCoroutine("native-elf-test")
        val successes = mutableListOf<PickpocketSuccessEvent>()
        val player = Player().apply {
            currentMapClock = 100; processedMapClock = 100; activeCoroutine = coroutine; coords = CoordGrid(2332, 3171, 0)
            inv = Inventory(ServerCacheManager.getInventory("inv.inv".asRSCM())!!, arrayOfNulls(28))
            worn = Inventory(ServerCacheManager.getInventory("inv.worn".asRSCM())!!, arrayOfNulls(14))
            statMap.setBaseLevel("stat.thieving", 99.toByte())
            statMap.setCurrentLevel("stat.thieving", 99)
            statMap.setBaseLevel("stat.hitpoints", 99.toByte())
            statMap.setCurrentLevel("stat.hitpoints", 99)
        }
        val random = object : GameRandom {
            override fun of(maxExclusive: Int) = if (maxExclusive in setOf(1024, 35)) 1 else 0
            override fun of(minInclusive: Int, maxInclusive: Int) = minInclusive + of(maxInclusive - minInclusive + 1)
            override fun randomDouble() = if (success) 0.0 else 1.0
        }
        val access = ProtectedAccess(player, coroutine, ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getRandom = { random }, getHitModifier = { NoopPlayerHitModifier }))
        init {
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }
            with(ThievingScript(mock(LocRepository::class.java), mock(NpcRepository::class.java), mock(AiPlayerInteractions::class.java))) { scripts.startup() }
            scripts.onEvent<PickpocketSuccessEvent> {
                assertTrue(player.statMap.getXP("stat.thieving") > 0)
                successes += this
            }
            with(TrailSkillChallenges(progress, TrailTargets(progress), TrailRequirements(progress.catalog))) { scripts.startup() }
            clue()
        }
        fun clue(row: String = "dbrow.cluehelper_skillchallenge_master_13", phase: Int = 9) {
            val clue = progress.catalog.clues.getValue(row.asRSCM())
            player.inv[0] = InvObj(ServerCacheManager.getItem(progress.catalog.item(clue))!!, 1, TrailState(clue.row, 6, 2, phase).encode())
        }
        fun state() = checkNotNull(progress.state(player.inv[0]!!))
        fun pick(symbol: String = "npc.mourning_town_elf_1") = run { bus.publish(access, NpcEvents.Op3(Npc(symbol, player.coords))) }
        fun run(block: suspend () -> Boolean) {
            var outcome: Result<Boolean>? = null
            block.startCoroutine(object : Continuation<Boolean> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Boolean>) { outcome = result }
            })
            repeat(10) {
                if (outcome == null) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; coroutine.advance() }
            }
            assertTrue(checkNotNull(outcome).getOrThrow(), "Native handler registered")
        }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
