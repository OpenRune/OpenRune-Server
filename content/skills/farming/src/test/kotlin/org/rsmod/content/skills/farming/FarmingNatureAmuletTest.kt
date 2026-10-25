package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import net.rsprot.protocol.game.outgoing.sound.MidiJingle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@ResourceLock("QuestRequirements")
class FarmingNatureAmuletTest {
    @Test
    fun `binding uses the native patch varp and rebinding leaves patch and unrelated vars untouched`() {
        val f = Fixture()
        val other = FarmingPatches.all.first { it.kind == PatchKind.HERB && it != f.patch }
        f.set(f.patch.varp, f.growing.pack())
        f.set(other.varp, f.growing.copy(stage = 2).pack())
        f.set("varp.farming_bounty_uses", 7)
        val patches = FarmingPatches.all.map { f.player.vars[it.varp] }
        f.bind()
        assertEquals(f.patch.varp.asRSCM(RSCMType.VARP), f.player.vars[BOUND_PATCH])
        assertEquals(f.growing.pack(), f.player.vars[LAST_STATUS])
        f.bind(other)
        assertEquals(other.varp.asRSCM(RSCMType.VARP), f.player.vars[BOUND_PATCH])
        assertEquals(f.player.vars[other.varp], f.player.vars[LAST_STATUS])
        assertEquals(patches, FarmingPatches.all.map { f.player.vars[it.varp] })
        assertEquals(7, f.player.vars["varp.farming_bounty_uses"])
        f.player.inv[0] = null
        f.bind()
        assertEquals(other.varp.asRSCM(RSCMType.VARP), f.player.vars[BOUND_PATCH])
        assertEquals(f.player.vars[other.varp], f.player.vars[LAST_STATUS])
    }

    @Test
    fun `held disease and worn death each alert once and unchanged death never repeats`() {
        val f = Fixture()
        f.seedAndBind(f.growing)
        f.replay(3999, 4000)
        assertEquals(Health.DISEASED, f.state().health)
        f.assertAlerts(1)
        f.player.worn[2] = f.player.inv[0]
        f.player.inv[0] = null
        f.replay(4000, 4040)
        assertEquals(Health.DEAD, f.state().health)
        f.assertAlerts(1)
        f.replay(4040, 4200)
        f.assertAlerts(0)
    }

    @Test
    fun `offline disease and death replay produces only the final alert after restoring the binding`() {
        val saved = Fixture()
        saved.seedAndBind(saved.growing)
        val restored = Fixture()
        for (variable in listOf(saved.patch.varp, BOUND_PATCH, LAST_STATUS)) {
            restored.set(variable, saved.player.vars[variable])
        }
        saved.assertAlerts(0)
        restored.assertAlerts(0)
        restored.replay(3999, 4080)
        assertEquals(Health.DEAD, restored.state().health)
        assertEquals(restored.state().pack(), restored.player.vars[LAST_STATUS])
        restored.assertAlerts(1)
        restored.replay(4080, 4200)
        restored.assertAlerts(0)
    }

    @Test
    fun `maturity alerts once but mature produce regrowth does not alert`() {
        val crop = checkNotNull(FarmingCrops.bySeed("obj.whiteberry_bush_seed"))
        val f = Fixture(crop)
        val maturing = f.growing.copy(stage = crop.stages - 1, paid = true)
        f.seedAndBind(maturing)
        val boundary = crop.cycleMinutes.toLong() * 100
        f.replay(boundary - 1, boundary)
        assertTrue(f.state().grown(crop))
        f.assertAlerts(1)
        f.replay(boundary, boundary + crop.cycleMinutes)
        f.assertAlerts(0)
        val checked = f.state().copy(phase = Phase.CHECKED, produce = 0)
        assertTrue(f.service.replace(f.player, f.patch, f.state(), checked))
        f.assertAlerts(0)
        val regrowth = crop.regrowMinutes.toLong() * 1000
        f.replay(regrowth - 1, regrowth)
        assertEquals(1, f.state().produce)
        f.assertAlerts(0)
    }

    @Test
    fun `clearing and replanting the same crop rearms an identical final status without losing binding`() {
        val f = Fixture()
        f.seedAndBind(f.growing)
        f.replay(3999, 4080)
        val dead = f.state()
        f.assertAlerts(1)
        assertTrue(f.service.clear(f.player, f.patch))
        assertEquals(f.growing.empty().pack(), f.player.vars[LAST_STATUS])
        assertTrue(f.service.replace(f.player, f.patch, f.state(), f.growing))
        assertEquals(f.growing.pack(), f.player.vars[LAST_STATUS])
        assertEquals(f.patch.varp.asRSCM(RSCMType.VARP), f.player.vars[BOUND_PATCH])
        f.assertAlerts(0)
        f.replay(3999, 4080)
        assertEquals(dead, f.state())
        f.assertAlerts(1)
    }

    @Test
    fun `curing a notified crop rearms disease notification for that same crop`() {
        val f = Fixture()
        f.seedAndBind(f.growing)
        f.replay(3999, 4000)
        f.assertAlerts(1)
        assertEquals(CureResult.CURED, f.service.cure(f.player, f.patch) { true })
        f.assertAlerts(0)
        f.replay(4039, 4040)
        assertEquals(Health.DISEASED, f.state().health)
        f.assertAlerts(1)
    }

    @Test
    fun `an absent amulet consumes the baseline without a backlog but the next transition still alerts`() {
        val f = Fixture()
        f.seedAndBind(f.growing)
        f.player.inv[0] = null
        f.replay(3999, 4000)
        assertEquals(Health.DISEASED, f.state().health)
        assertEquals(f.state().pack(), f.player.vars[LAST_STATUS])
        f.assertAlerts(0)
        f.player.worn[2] = InvObj(FarmingNatureAmulet.AMULET)
        f.replay(4000, 4001)
        f.assertAlerts(0)
        f.replay(4001, 4040)
        assertEquals(Health.DEAD, f.state().health)
        f.assertAlerts(1)
    }

    private class FixedRandom : GameRandom {
        override fun of(maxExclusive: Int): Int = 0
        override fun of(minInclusive: Int, maxInclusive: Int): Int = minInclusive
        override fun randomDouble(): Double = 0.0
    }

    private class Fixture(val crop: Crop = checkNotNull(FarmingCrops.bySeed("obj.guam_seed"))) {
        private val random = FixedRandom()
        private val events = EventBus()
        private val writes = mutableListOf<Any>()
        val service = FarmingService(
            XpModifiers(emptySet()), random, events,
            FarmingRewardTestSupport.rewards(random, events), FarmingEquipment(random), farmingSiteAccess(),
        )

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = object : Client<Any, Any> {
                override fun write(message: Any) { writes += message }
                override fun close() {}
                override fun read(player: Player) {}
                override fun flush() {}
                override fun flushHighPriority() {}
                override fun unregister(service: Any, player: Player) {}
            }
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            inv[0] = InvObj(FarmingNatureAmulet.AMULET)
        }
        val patch = FarmingPatches.all.first {
            it.kind == crop.kind && !FarmingPatches.diseaseFree(player, it)
        }
        val growing = PatchState(weeds = PatchState.CLEARED, cropIndex = crop.index, stage = 1)

        init {
            set("varp.farming_clock", Int.MAX_VALUE)
            set("varp.farming_growth_offset", 32)
            with(FarmingNatureAmulet(service)) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        fun set(variable: String, value: Int) = VarPlayerIntMapSetter.set(player, variable, value)

        fun state(): PatchState = service.stored(player, patch)

        fun seedAndBind(state: PatchState) {
            set(patch.varp, state.pack())
            bind()
            writes.clear()
        }

        fun bind(target: PatchDef = patch) {
            val type = checkNotNull(ServerCacheManager.getObject(target.locs.first().asRSCM(RSCMType.LOC)))
            val loc = BoundLocInfo(target.coords.first(), LocEntity(type.id, 10, 0), 2, 1, 1, 0)
            val amulet = checkNotNull(ServerCacheManager.getItem(FarmingNatureAmulet.AMULET.asRSCM()))
            val coroutine = GameCoroutine("farming-nature-amulet-test")
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(
                player, coroutine, ProtectedAccessContextFactory.empty().copy(getEventBus = { events }),
            )
            var outcome: Result<Unit>? = null
            val action: suspend () -> Unit = {
                assertTrue(events.publish(access, LocUEvents.Op(loc, loc, type, amulet, 0)))
            }
            action.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { outcome = result }
            })
            checkNotNull(outcome).getOrThrow()
        }

        fun replay(from: Long, to: Long) {
            set("varp.farming_clock", from.toInt())
            service.refresh(player, to * FarmingClock.MINUTE_MILLIS)
            set("varp.farming_clock", Int.MAX_VALUE)
        }

        fun assertAlerts(expected: Int) {
            assertEquals(expected, writes.filterIsInstance<MessageGame>().size)
            assertEquals(expected, writes.filterIsInstance<MidiJingle>().size)
            writes.clear()
        }
    }

    companion object {
        private const val BOUND_PATCH = "varp.farming_nature_patch"
        private const val LAST_STATUS = "varp.farming_nature_status"

        @OptIn(InternalApi::class)
        @JvmStatic @BeforeAll fun cache() {
            val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision = Files.readAllLines(root.resolve("game.yml"))
                .first { it.startsWith("revision:") }.substringAfter(':').trim().substringBefore('.').toInt()
            ServerCacheManager.init(revision).close()
        }
    }
}
