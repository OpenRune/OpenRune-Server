package org.rsmod.content.quest.area.varrock.childrenofthesun

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
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
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.content.quest.manager.QuestProgressState
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ChildrenOfTheSunInteractionTest {
    @Test fun `accepting the quest starts it and remembers meeting Alina`() {
        val f = Fixture()
        f.talk(Alina)
        f.finish(listOf(1, 4))
        assertEquals(CotsStage.Started, f.stage())
        assertEquals(1, f.player.vars["varbit.vmq1_met_alina"])
        assertEquals(0, f.scenes.delegations)
        assertEquals(0, f.player.vars["varp.qp"])
    }

    @Test fun `Noah leads the same conversation as Alina`() {
        val f = Fixture()
        f.talk(Noah)
        f.finish(listOf(1, 4))
        assertEquals(CotsStage.Started, f.stage())
    }

    @Test fun `declining leaves the quest unstarted but Alina remembers the player`() {
        val f = Fixture()
        f.talk(Alina)
        f.finish(listOf(2))
        assertEquals(CotsStage.NotStarted, f.stage())
        assertEquals(1, f.player.vars["varbit.vmq1_met_alina"])
        assertTrue(f.output().contains("suit yourself"), f.output())
    }

    @Test fun `a returning player skips the introduction`() {
        val f = Fixture()
        VarPlayerIntMapSetter.set(f.player, "varbit.vmq1_met_alina", 1)
        f.talk(Alina)
        f.finish(listOf(2))
        assertFalse(f.output().contains("Are you here to see the delegation?"), f.output())
        assertTrue(f.output().contains("Bah! Boring!"), f.output())
        assertEquals(CotsStage.NotStarted, f.stage())
    }

    @Test fun `the first conversation includes the introduction`() {
        val f = Fixture()
        f.talk(Alina)
        f.finish(listOf(2))
        assertTrue(f.output().contains("Are you here to see the delegation?"), f.output())
    }

    @Test fun `the quest does not check membership to start`() {
        val f = Fixture()
        f.player.members = false
        f.talk(Alina)
        f.finish(listOf(1, 4))
        assertEquals(CotsStage.Started, f.stage())
        assertEquals(1, f.player.vars["varbit.vmq1_met_alina"])
        assertFalse(f.output().contains("members'"), f.output())
    }

    @Test fun `the quest does not check membership once started`() {
        val f = Fixture(CotsStage.Started)
        f.player.members = false
        f.talk(Alina)
        f.finish(listOf(3))
        assertEquals(CotsStage.Tailing, f.stage())
        assertEquals(1, f.scenes.delegations)
        f.talk(Noah)
        f.finish()
        assertEquals(2, f.tails.starts)
        assertFalse(f.output().contains("members'"), f.output())
    }

    @Test fun `the topics Alina offers loop until the player moves on`() {
        val f = Fixture()
        f.talk(Alina)
        f.finish(listOf(1, 1, 2, 4))
        val out = f.output()
        assertTrue(out.contains("Sun Queen"), out)
        assertTrue(out.contains("College of Bards"), out)
        assertEquals(CotsStage.Started, f.stage())
        assertEquals(0, f.scenes.delegations)
    }

    @Test fun `asking about the delegation plays the scene and starts the tail`() {
        val f = Fixture()
        f.talk(Alina)
        f.finish(listOf(1, 3))
        assertEquals(CotsStage.Tailing, f.stage())
        assertEquals(1, f.scenes.delegations)
        assertEquals(1, f.tails.starts)
    }

    @Test fun `talking to Alina again after starting offers the same topics`() {
        val f = Fixture(CotsStage.Started)
        f.talk(Alina)
        f.finish(listOf(3))
        assertEquals(CotsStage.Tailing, f.stage())
        assertEquals(1, f.scenes.delegations)
        assertTrue(f.output().contains("Hello again"), f.output())
    }

    @Test fun `Noah restarts the tail after it fails`() {
        val f = Fixture(CotsStage.Tailing)
        f.talk(Noah)
        f.finish()
        assertEquals(CotsStage.Tailing, f.stage())
        assertEquals(1, f.tails.starts)
        assertEquals(0, f.scenes.delegations)
        assertTrue(f.output().contains("large bag"), f.output())
    }

    @Test fun `being spotted lets the guard shout, then ends the tail without losing progress`() {
        val f = Fixture(CotsStage.Tailing)
        f.tails.results += TailResult.Spotted
        f.tick()
        assertEquals(0, f.tails.stops)
        assertFalse(f.output().contains("failed to stay hidden"), f.output())
        f.finish()
        assertEquals(CotsStage.Tailing, f.stage())
        assertEquals(1, f.tails.stops)
        assertTrue(f.output().contains("failed to stay hidden"), f.output())
    }

    @Test fun `falling behind ends the tail`() {
        val f = Fixture(CotsStage.Tailing)
        f.tails.results += TailResult.TooFar
        f.tick()
        f.finish()
        assertEquals(CotsStage.Tailing, f.stage())
        assertTrue(f.output().contains("failed to stay close enough"), f.output())
    }

    @Test fun `a tick that changes nothing keeps the tail running`() {
        val f = Fixture(CotsStage.Tailing)
        f.tick()
        f.finish()
        assertEquals(0, f.tails.stops)
        assertEquals(0, f.scenes.eavesdrops)
    }

    @Test fun `ticks outside the tailing stage clean up the tail`() {
        val f = Fixture(CotsStage.Marking)
        f.tick()
        f.finish()
        assertEquals(1, f.tails.stops)
        assertEquals(CotsStage.Marking, f.stage())
    }

    @Test fun `reaching the building plays the eavesdropping scene`() {
        val f = Fixture(CotsStage.Tailing)
        f.tails.results += TailResult.Arrived
        f.tick()
        f.finish()
        assertEquals(CotsStage.ReportToTobyn, f.stage())
        assertEquals(1, f.scenes.eavesdrops)
        assertEquals(CotsStage.Eavesdropping, f.scenes.stageDuringEavesdrop)
    }

    @Test fun `Tobyn waves the player on while they are tailing`() {
        val f = Fixture(CotsStage.Tailing)
        f.talk(Tobyn)
        f.finish()
        assertEquals(CotsStage.Tailing, f.stage())
        assertTrue(f.output().contains("Move along"), f.output())
    }

    @Test fun `reporting the bandits starts the marking phase`() {
        val f = Fixture(CotsStage.ReportToTobyn)
        f.talk(Tobyn)
        f.finish()
        assertEquals(CotsStage.Marking, f.stage())
        for (guard in 1..10) {
            assertEquals(1, f.player.vars["varbit.vmq1_guard_$guard"], "guard $guard")
        }
    }

    @Test fun `marking and unmarking guards flips their state`() {
        val f = Fixture(CotsStage.Marking)
        f.script.syncGuards(f.player)
        f.talk("npc.vmq1_guard_3")
        f.finish()
        assertEquals(2, f.player.vars["varbit.vmq1_guard_3"])
        assertEquals(1, f.script.markedCount(f.player))
        assertTrue(f.output().contains("You mark the guard."), f.output())
        f.talk("npc.vmq1_guard_3")
        f.finish()
        assertEquals(1, f.player.vars["varbit.vmq1_guard_3"])
        assertEquals(0, f.script.markedCount(f.player))
        assertTrue(f.output().contains("You unmark the guard."), f.output())
    }

    @Test fun `only four guards can be marked at once`() {
        val f = Fixture(CotsStage.Marking)
        f.script.syncGuards(f.player)
        for (guard in 1..4) f.mark(guard)
        f.mark(5)
        assertEquals(4, f.script.markedCount(f.player))
        assertEquals(1, f.player.vars["varbit.vmq1_guard_5"])
        assertTrue(f.output().contains("already marked enough guards"), f.output())
    }

    @Test fun `Tobyn repeats his instructions until four guards are marked`() {
        val f = Fixture(CotsStage.Marking)
        f.script.syncGuards(f.player)
        f.mark(1)
        f.talk(Tobyn)
        f.finish()
        assertEquals(CotsStage.Marking, f.stage())
        assertTrue(f.output().contains("point out the four bandits"), f.output())
        assertFalse(f.output().contains("pointed out all the bandits"), f.output())
    }

    @Test fun `marking an honest guard sends the player back to try again`() {
        val f = Fixture(CotsStage.Marking)
        f.script.syncGuards(f.player)
        for (guard in listOf(1, 2, 3, 5)) f.mark(guard)
        f.talk(Tobyn)
        f.finish()
        assertEquals(CotsStage.Marking, f.stage())
        assertEquals(0, f.scenes.roofTrips)
        assertTrue(f.output().contains("Are you sure?"), f.output())
    }

    @Test fun `marking all four bandits has them arrested and moves the player to the roof`() {
        val f = Fixture(CotsStage.Marking)
        f.script.syncGuards(f.player)
        for (guard in 1..4) f.mark(guard)
        f.talk(Tobyn)
        f.finish()
        assertEquals(CotsStage.OnRoof, f.stage())
        assertEquals(1, f.scenes.roofTrips)
        assertEquals(CotsStage.OnRoof, f.scenes.stageDuringRoofTrip)
        for (guard in 1..10) {
            assertEquals(0, f.player.vars["varbit.vmq1_guard_$guard"], "guard $guard")
        }
    }

    @Test fun `the interrogation runs once and completes the quest with its reward`() {
        val f = Fixture(CotsStage.OnRoof)
        f.talk(Itzla)
        f.finish()
        f.assertCompleted(interrogations = 1)
        assertTrue(f.output().contains("Itzla departs."), f.output())
    }

    @Test fun `Tobyn on the roof starts the interrogation too`() {
        val f = Fixture(CotsStage.OnRoof)
        f.talk(Tobyn)
        f.finish()
        f.assertCompleted(interrogations = 1)
    }

    @Test fun `an interrupted interrogation replays the scene`() {
        val f = Fixture(CotsStage.Interrogating)
        f.talk(Tobyn)
        f.finish()
        f.assertCompleted(interrogations = 1)
    }

    @Test fun `after the interrogation Itzla does not replay the scene`() {
        val f = Fixture(CotsStage.Interrogated)
        f.talk(Itzla)
        f.finish()
        f.assertCompleted(interrogations = 0)
    }

    @Test fun `Tobyn finishes the quest once Itzla has left`() {
        val f = Fixture(CotsStage.ItzlaLeft)
        f.talk(Tobyn)
        f.finish()
        f.assertCompleted(interrogations = 0)
        assertFalse(f.output().contains("Itzla departs."), f.output())
    }

    @Test fun `Itzla has nothing to say once he has left`() {
        val f = Fixture(CotsStage.ItzlaLeft)
        f.talk(Itzla)
        f.finish()
        assertEquals(CotsStage.ItzlaLeft, f.stage())
        assertEquals(0, f.player.vars["varp.qp"])
    }

    @Test fun `talking to Tobyn after the quest never rewards again`() {
        val f = Fixture(CotsStage.OnRoof)
        f.talk(Itzla)
        f.finish()
        f.talk(Tobyn)
        f.finish()
        assertEquals(CotsStage.Complete, f.stage())
        assertEquals(1, f.player.vars["varp.qp"])
        assertEquals(1, f.scenes.interrogations)
        assertTrue(f.output().contains("Good to see you"), f.output())
    }

    @Test fun `guards appear only while the player is marking`() {
        val f = Fixture(CotsStage.Marking)
        f.script.syncGuards(f.player)
        assertEquals(1, f.player.vars["varbit.vmq1_guard_1"])
        VarPlayerIntMapSetter.set(f.player, "varbit.vmq1_guard_2", 2)
        f.script.syncGuards(f.player)
        assertEquals(2, f.player.vars["varbit.vmq1_guard_2"])
        VarPlayerIntMapSetter.set(f.player, "varbit.vmq1", CotsStage.OnRoof)
        f.script.syncGuards(f.player)
        for (guard in 1..10) {
            assertEquals(0, f.player.vars["varbit.vmq1_guard_$guard"], "guard $guard")
        }
    }

    @Test fun `quest progress lives in the cache varbit over the quest varp`() {
        val f = Fixture(CotsStage.Marking)
        assertEquals(CotsStage.Marking, f.player.vars["varbit.vmq1"])
        assertEquals(24, f.script.quest.maxSteps)
        assertEquals(1, f.script.quest.questPoints)
        assertEquals(QuestProgressState.IN_PROGRESS, f.script.quest.questState(f.player))
    }

    @Test fun `the journal follows the quest from start to finish`() {
        val f = Fixture(CotsStage.Started)
        assertTrue(f.script.questLog(f.access()).contains("when the <red>delegation</red>"))
        VarPlayerIntMapSetter.set(f.player, "varbit.vmq1", CotsStage.Tailing)
        val tailing = f.script.questLog(f.access())
        assertTrue(tailing.contains("very large bag"), tailing)
        assertFalse(tailing.contains("when the <red>delegation</red>"), tailing)
        VarPlayerIntMapSetter.set(f.player, "varbit.vmq1", CotsStage.ReportToTobyn)
        assertTrue(f.script.questLog(f.access()).contains("planning to attack the delegation"))
        VarPlayerIntMapSetter.set(f.player, "varbit.vmq1", CotsStage.Marking)
        f.script.syncGuards(f.player)
        f.mark(1)
        val marking = f.script.questLog(f.access())
        assertTrue(marking.contains("<red>Mark</red>"), marking)
        assertTrue(marking.contains("marked <red>1</red> of the"), marking)
        VarPlayerIntMapSetter.set(f.player, "varbit.vmq1", CotsStage.Interrogated)
        assertTrue(f.script.questLog(f.access()).contains("palace roof"))
        VarPlayerIntMapSetter.set(f.player, "varbit.vmq1", CotsStage.ItzlaLeft)
        assertTrue(f.script.questLog(f.access()).contains("final word"))
        VarPlayerIntMapSetter.set(f.player, "varbit.vmq1", CotsStage.Complete)
        val done = f.script.completedLog(f.access())
        assertTrue(done.contains("QUEST COMPLETE"), done)
        assertTrue(done.contains("Servius"), done)
    }

    @Test fun `the scene scripts run to completion`() {
        val f = Fixture()
        f.dialogue {
            delegationArrives()
            guardLeaves()
            eavesdropOpening()
            banditsTalk()
            eavesdropClosing()
            interrogationInCell()
        }
        f.finish()
        val out = f.output()
        assertTrue(out.contains("Teokan"), out)
        assertTrue(out.contains("big bag") || out.contains("that bag"), out)
        assertTrue(out.contains("You're late."), out)
        assertTrue(out.contains("Kuaini!"), out)
        assertTrue(out.contains("What are you up to in there...?"), out)
        assertTrue(out.contains("a spot of bother."), out)
        assertTrue(out.contains("what are your thoughts on chicken?"), out)
    }

    @Test fun `every npc the scenes spawn exists in the cache`() {
        val names =
            listOf(
                "king_roald_cutscene",
                "aeonisig_raispher_cutscene",
                "itzla_cutscene",
                "servius_cutscene",
                "furia_cutscene",
                "ennius_cutscene",
                "guard_sergeant_cutscene",
                "bag_guard",
            ) + (1..6).map { "knight_$it" }
        for (name in names) {
            assertNotNull(ServerCacheManager.getNpc("npc.vmq1_$name".asRSCM()), name)
        }
        assertNotNull(ServerCacheManager.getNpc("npc.fai_varrock_guard02".asRSCM()))
    }

    @Test fun `the server can construct the quest through a single injectable constructor`() {
        val injectable =
            ChildrenOfTheSun::class.java.constructors.filter {
                it.isAnnotationPresent(Inject::class.java)
            }
        assertEquals(1, injectable.size)
        assertEquals(
            listOf(
                NpcRepository::class.java,
                LocRepository::class.java,
                CollisionFlagMap::class.java,
                RayCastValidator::class.java,
            ),
            injectable.single().parameterTypes.toList(),
        )
    }

    private class FakeScenes(private val fixture: () -> Fixture) : CotsScenes {
        var delegations = 0
        var eavesdrops = 0
        var roofTrips = 0
        var interrogations = 0
        var stageDuringEavesdrop = -1
        var stageDuringRoofTrip = -1

        override suspend fun delegation(access: ProtectedAccess) {
            delegations++
        }

        override suspend fun eavesdrop(access: ProtectedAccess) {
            eavesdrops++
            stageDuringEavesdrop = fixture().stage()
        }

        override suspend fun toRoof(access: ProtectedAccess) {
            roofTrips++
            stageDuringRoofTrip = fixture().stage()
        }

        override suspend fun interrogation(access: ProtectedAccess) {
            interrogations++
        }
    }

    private class FakeTails : CotsTails {
        var starts = 0
        var stops = 0
        val results = ArrayDeque<TailResult>()

        override fun start(access: ProtectedAccess) {
            starts++
        }

        override fun tick(player: Player): TailResult =
            results.removeFirstOrNull() ?: TailResult.Continue

        override fun stop(player: Player) {
            stops++
        }
    }

    private class Fixture(stage: Int = CotsStage.NotStarted) {
        val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("children-of-the-sun-test")
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
                slotId = 1
                assignUid()
                coords = CoordGrid(3225, 3427, 0)
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

        val scenes = FakeScenes { this }
        val tails = FakeTails()
        val script = ChildrenOfTheSun(scenes, tails)

        init {
            with(script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
            VarPlayerIntMapSetter.set(player, "varbit.vmq1", stage)
        }

        fun stage() = script.quest.getQuestStage(player)

        fun access() = ProtectedAccess(player, coroutine, context)

        fun talk(npc: String) {
            val target = Npc(npc, player.coords.translateZ(1))
            start { assertTrue(events.publish(access(), NpcEvents.Op1(target))) }
        }

        fun mark(guard: Int) {
            talk("npc.vmq1_guard_$guard")
            finish()
        }

        fun tick() {
            start { with(script) { access().tailTick() } }
        }

        fun dialogue(block: suspend org.rsmod.api.player.dialogue.Dialogue.() -> Unit) {
            start { access().startDialogue(block) }
        }

        private fun start(block: suspend () -> Unit) {
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            block.startCoroutine(
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

        fun assertCompleted(interrogations: Int) {
            assertEquals(CotsStage.Complete, stage())
            assertEquals(CotsStage.Complete, player.vars["varbit.vmq1"])
            assertEquals(1, player.vars["varp.qp"])
            assertEquals(interrogations, scenes.interrogations)
            assertEquals(QuestProgressState.FINISHED, script.quest.questState(player))
        }

        fun output() = client.messages.joinToString("\n")
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
        private const val Alina = "npc.vmq1_alina"
        private const val Noah = "npc.vmq1_noah"
        private const val Tobyn = "npc.vmq1_guard_sergeant"
        private const val Itzla = "npc.vmq1_itzla"
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
