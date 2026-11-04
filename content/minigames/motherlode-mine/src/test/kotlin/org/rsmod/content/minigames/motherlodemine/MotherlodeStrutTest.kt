package org.rsmod.content.minigames.motherlodemine

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.random.GameRandom
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.content.minigames.motherlodemine.MotherlodeMiningTest.Fixture
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.scripts.MotherlodeStrutScript
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocZoneKey
import org.rsmod.game.seq.EntitySeq
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey

@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class MotherlodeStrutTest {
    @Test fun `successful first repair waits for a hammer swing before restoring the wheel`() {
        val f = RepairFixture()
        val coroutine = f.start()
        assertTrue(coroutine.isSuspended)
        assertTrue(f.machine.isStrutBroken(f.strut.coords))
        assertEquals(MotherlodeConstants.STRUT_REPAIR_ANIM.asRSCM(), f.player.pendingSequence.id)
        assertEquals(listOf("synth.hammer_wood".asRSCM()), f.sounds())
        assertEquals(0, f.player.statMap.getFineXP("stat.smithing"))

        f.advance()
        coroutine.advance()
        assertTrue(coroutine.isIdle)
        assertFalse(f.machine.isStrutBroken(f.strut.coords))
        assertTrue(f.player.statMap.getFineXP("stat.smithing") > 0)
        assertEquals(EntitySeq.ZERO, f.player.pendingSequence)
        assertEquals(1, f.sounds().size)
    }

    @Test fun `failed attempts sound once per full swing and cancellation stops the animation`() {
        val f = RepairFixture(succeeds = false)
        val coroutine = f.start()
        repeat(4) { elapsed ->
            assertEquals(1 + elapsed / 2, f.sounds().size)
            f.advance()
            coroutine.advance()
        }
        assertEquals(3, f.sounds().size)
        assertTrue(f.machine.isStrutBroken(f.strut.coords))
        f.player.cancelActiveCoroutine()
        assertEquals(EntitySeq.ZERO, f.player.pendingSequence)
        assertEquals(0, f.player.statMap.getFineXP("stat.smithing"))
    }

    @Test fun `another player repairing during the swing does not award a duplicate repair`() {
        val f = RepairFixture()
        val coroutine = f.start()
        assertTrue(f.machine.repairStrut(f.strut.coords))
        f.advance()
        coroutine.advance()
        assertTrue(coroutine.isIdle)
        assertEquals(0, f.player.statMap.getFineXP("stat.smithing"))
        assertEquals(EntitySeq.ZERO, f.player.pendingSequence)
        assertEquals(1, f.sounds().size)
    }

    private class RepairFixture(succeeds: Boolean = true) {
        private val f = Fixture()
        private val messages = mutableListOf<Any>()
        val player = Player().apply {
            coords = CoordGrid(3742, 5670)
            currentMapClock = f.clock.cycle
            processedMapClock = currentMapClock
            pendingSequence = EntitySeq.NULL
            inv = invMap.getOrPut("inv.inv")
            worn = invMap.getOrPut("inv.worn")
            inv[0] = InvObj("obj.hammer")
            client = object : Client<Any, Any> {
                override fun write(message: Any) { messages += message }
                override fun close() {}
                override fun read(player: Player) {}
                override fun flush() {}
                override fun flushHighPriority() {}
                override fun unregister(service: Any, player: Player) {}
            }
        }
        private val random = object : GameRandom {
            override fun of(maxExclusive: Int) = if (succeeds) 0 else maxExclusive - 1
            override fun of(minInclusive: Int, maxInclusive: Int) = if (succeeds) minInclusive else maxInclusive
            override fun randomDouble() = if (succeeds) 0.0 else 0.99
        }
        val machine = MotherlodeMachine(f.locs, f.npcs, random, f.collision)
        val strut: BoundLocInfo
        private val script = MotherlodeStrutScript(machine, InvisibleLevels(emptySet()))

        init {
            for (x in 3740..3746) for (z in 5666..5671) f.collision.allocateIfAbsent(x, z, 0)
            val wheel = LocInfo(2, CoordGrid(3743, 5668),
                LocEntity(MotherlodeConstants.WHEEL_BROKEN_LOC.asRSCM(), 10, 2))
            f.storage.mapLocs[ZoneKey.from(wheel.coords), LocZoneKey(ZoneGrid.from(wheel.coords), wheel.layer)] = wheel.entity
            machine.tick()
            val loc = f.locs.findAll(ZoneKey.from(CoordGrid(3742, 5669))).single {
                it.id == MotherlodeConstants.STRUT_BROKEN_LOC.asRSCM()
            }
            strut = BoundLocInfo(loc, checkNotNull(ServerCacheManager.getObject(loc.id)))
        }

        fun start() = player.launch {
            val context = ProtectedAccessContextFactory.empty().copy(getRandom = { random })
            with(script) { ProtectedAccess(player, this@launch, context).repair(strut) }
        }

        fun advance() {
            f.clock.tick()
            player.currentMapClock = f.clock.cycle
            player.processedMapClock = f.clock.cycle
        }

        fun sounds() = messages.filterIsInstance<SynthSound>().map { it.id }
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(241).close() }
    }
}
