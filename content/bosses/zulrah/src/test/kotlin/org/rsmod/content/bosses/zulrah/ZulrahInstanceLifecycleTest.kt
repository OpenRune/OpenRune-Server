package org.rsmod.content.bosses.zulrah

import com.google.inject.AbstractModule
import com.google.inject.Guice
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import java.nio.file.Files
import java.nio.file.Path
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossExtensionRegistry
import org.rsmod.api.bosses.runtime.EncounterRegistry
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.MaxHitFormulae
import org.rsmod.api.combat.formulas.accuracy.magic.NvPMagicAccuracy
import org.rsmod.api.combat.formulas.accuracy.melee.NvPMeleeAccuracy
import org.rsmod.api.combat.formulas.accuracy.ranged.NvPRangedAccuracy
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.game.process.npc.NpcQueueProcessor
import org.rsmod.api.game.process.world.WorldQueueListProcess
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceAttributes
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.InstanceSpec
import org.rsmod.api.instances.currentInstanceId
import org.rsmod.api.instances.events.InstanceEndedEvent
import org.rsmod.api.instances.events.instanceEventId
import org.rsmod.api.instances.region.InstanceAreaResolver
import org.rsmod.api.npc.access.StandardNpcAccessContextFactory
import org.rsmod.api.npc.access.StandardNpcAccessLauncher
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.events.interact.AiPlayerEvents
import org.rsmod.api.npc.hit.modifier.StandardNpcHitModifier
import org.rsmod.api.npc.hit.processor.NpcHitProcessor
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.queueDeath
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.cheat.adminGodMode
import org.rsmod.api.player.hit.modifier.StandardPlayerHitModifier
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.random.GameRandom
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
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.client.NoopClient
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.Direction
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class ZulrahInstanceLifecycleTest {
    private var Player.protectMagic by org.rsmod.api.player.vars.intVarBit("varbit.prayer_protectfrommagic")
    private var Player.protectMelee by org.rsmod.api.player.vars.intVarBit("varbit.prayer_protectfrommelee")
    private var Player.protectRanged by org.rsmod.api.player.vars.intVarBit("varbit.prayer_protectfrommissiles")

    @Test
    fun `each playable rotation completes two cycles in native DSL without resetting boss identity or hp`() {
        for (rotation in 1..4) {
            val world = World(openingEnabled = true, bossSpec = ZulrahSpec.rotating(listOf(rotation)))
            val player = world.player(1)
            player.adminGodMode = true
            val session = world.enter(player)
            world.cycle(4)
            val boss = world.manager.npcsForInstance(session.id).single()
            val encounter = world.deps.encounterRegistry.of(boss)
            boss.hitpoints = 321
            var tick = 0
            val expected = sortedMapOf<Int, String>()
            fun append(prefix: String, phases: List<ZulrahPhase>) {
                var relativeTick = 0
                for (phase in phases) {
                    for (event in phase.events) {
                        expected[tick + relativeTick + event.tick] = "${prefix}_${relativeTick + event.tick}"
                    }
                    relativeTick += phase.duration
                }
                tick += relativeTick
            }
            append("opening", listOf(ZulrahRotations.opening))
            repeat(2) {
                append("rotation_$rotation", ZulrahRotations.rotations[rotation - 1].drop(1))
                append("recurring", listOf(ZulrahRotations.recurringOpening))
            }
            for (elapsed in 0 until tick) {
                world.cycle(4 + elapsed)
                assertEquals(expected.getValue(expected.keys.last { it <= elapsed }), encounter.currentPhaseName,
                    "rotation=$rotation elapsed=$elapsed")
                assertSame(encounter, world.deps.encounterRegistry.of(boss))
                assertEquals(321, boss.hitpoints)
                assertEquals(ZulrahEncounterController.State.Fighting, world.opening.state(session.id))
            }
            world.cycle(4 + tick)
            assertEquals("rotation_${rotation}_0", encounter.currentPhaseName)
            assertTrue(world.opening.canAttack(player, boss))
        }
    }

    @Test
    fun `death during a reconstructed phase cancels rotations and delayed summons`() {
        val world = World(openingEnabled = true, bossSpec = ZulrahSpec.rotating(listOf(3)))
        val player = world.player(1)
        player.adminGodMode = true
        val session = world.enter(player)
        world.cycle(55)
        val boss = world.manager.npcsForInstance(session.id).first { it.type.name == "Zulrah" }
        boss.heroPoints(player, 500)
        boss.hitpoints = 0
        boss.queueDeath()
        world.cycle(500)
        assertEquals(ZulrahEncounterController.State.Finished, world.opening.state(session.id))
        assertTrue(world.manager.npcsForInstance(session.id).none { it.isSlotAssigned })
        assertEquals(1, world.kills.size)
    }

    @Test
    fun `boss DSL owns every phase transition without a global start cycle handler`() {
        val world = World(openingEnabled = true)
        assertNull(world.bus.unbound[GameLifecycle.StartCycle::class.java])
        val player = world.player(1)
        player.adminGodMode = true
        val session = world.enter(player)
        world.cycle(4)
        val boss = world.manager.npcsForInstance(session.id).single()
        val encounter = world.deps.encounterRegistry.of(boss)
        assertSame(world.bossSpec, encounter.spec)
        val steps = ZulrahRoutine.recorded.events.map { it.tick }.distinct()
        for (elapsed in 0..520) {
            world.cycle(4 + elapsed)
            val phase = if (elapsed == 520) "evidence_limit" else "step_${steps.last { it <= elapsed }}"
            assertSame(encounter, world.deps.encounterRegistry.of(boss))
            assertEquals(phase, encounter.currentPhaseName, "elapsed=$elapsed")
        }
    }

    @Test
    fun `tail windup fixes facing until the next attack and retargets on the second swing`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player)
        val tails = ZulrahRoutine.recorded.events.filter { it.kind == "tail" }.take(2)
        world.cycle(4 + tails.first().tick - 1)
        val boss = world.manager.npcsForInstance(session.id).single {
            it.visType.id == "npc.snakeboss_boss_melee".asRSCM(RSCMType.NPC)
        }
        val firstTile = player.coords
        world.cycle(4 + tails.first().tick)
        assertEquals(firstTile, boss.faceLockSquare)
        assertNull(boss.facingTarget(world.players))

        player.coords = firstTile.translate(2, 0)
        world.cycle(4 + tails.first().tick + 1)
        assertEquals(firstTile, boss.faceLockSquare)
        assertNull(boss.facingTarget(world.players))

        world.cycle(4 + tails.last().tick)
        assertEquals(player.coords, boss.faceLockSquare)
        val nextEmerge = ZulrahRoutine.recorded.events.first {
            it.kind == "emerge" && it.tick > tails.last().tick
        }
        world.cycle(4 + nextEmerge.tick)
        assertFalse(boss.isFacingLocked)
    }

    private val firstTailTick get() = 4 + ZulrahRoutine.recorded.events.first { it.kind == "tail" }.tick

    private fun World.prepareTail(): Pair<Player, Npc> {
        val player = player(1)
        val session = enter(player)
        cycle(firstTailTick - 1)
        val boss = manager.npcsForInstance(session.id).first()
        player.coords = boss.coords.translate(2, -4)
        return player to boss
    }

    private fun Player.captureMessages(): MutableList<MessageGame> {
        val messages = mutableListOf<MessageGame>()
        client = object : Client<Any, Any> by NoopClient {
            override fun write(message: Any) {
                if (message is MessageGame) messages += message
            }
        }
        return messages
    }

    @Test
    fun `successful stun sends the native body animation effect and exact spam message once`() {
        val world = World(openingEnabled = true)
        val (player, _) = world.prepareTail()
        val messages = player.captureMessages()
        player.pendingSpotanims.clear()
        world.cycle(firstTailTick + 3)
        assertTrue(messages.isEmpty())
        assertTrue(player.pendingSpotanims.isEmpty)
        world.cycle(firstTailTick + 4)
        assertEquals(848, player.pendingSequence.id)
        assertEquals(listOf(MessageGame(105, "<col=ff0000>You've been stunned!</col>")), messages)
        assertTrue(player.pendingSpotanims.contains(EntitySpotanim(80, 0, 100, 2).packed))
        world.cycle(firstTailTick + 4)
        assertEquals(1, messages.size)
    }

    @Test
    fun `stun expiry and instance departure clear only the stun graphic slot`() {
        for (leave in listOf(false, true)) {
            val world = World(openingEnabled = true)
            val (player, _) = world.prepareTail()
            world.cycle(firstTailTick + 4)
            player.pendingSpotanims.clear()
            val other = EntitySpotanim(1230, 0, 0, 1).packed
            player.pendingSpotanims.add(other)
            if (leave) {
                val session = requireNotNull(world.manager.sessionForPlayer(player))
                player.coords = world.manager.leave(player, session, firstTailTick + 4)
            } else {
                world.cycle(firstTailTick + 9)
            }
            assertTrue(player.pendingSpotanims.contains(other))
            assertTrue(player.pendingSpotanims.contains(EntitySpotanim(65535, 0, 0, 2).packed))
        }
    }

    @Test
    fun `dodged tail does not send a stun graphic or message`() {
        val world = World(openingEnabled = true)
        val (player, _) = world.prepareTail()
        val messages = player.captureMessages()
        player.pendingSpotanims.clear()
        world.cycle(firstTailTick)
        player.coords = player.coords.translate(1, 0)
        world.cycle(firstTailTick + 4)
        assertTrue(messages.isEmpty())
        assertTrue(player.pendingSpotanims.isEmpty)
    }

    @Test
    fun `tail hits after four ticks bypasses prayers and keeps NPC attribution without venom`() {
        val world = World(openingEnabled = true)
        val (player, boss) = world.prepareTail()
        val hits = mutableListOf<org.rsmod.game.hit.Hit>()
        world.bus.subscribeUnbound(org.rsmod.api.player.events.PlayerHitEvents.Impact::class.java) {
            hits += hit
        }
        player.protectMelee = 1
        player.protectRanged = 1
        player.protectMagic = 1
        world.cycle(firstTailTick + 3)
        assertEquals(99, player.hitpoints)
        assertTrue(hits.isEmpty())
        val venomBefore = player.vars["varp.venom_strikes"]
        world.cycle(firstTailTick + 4)
        val hit = hits.single()
        assertTrue(hit.damage in 20..30)
        assertEquals(99 - hit.damage, player.hitpoints)
        assertEquals(HitType.Typeless, hit.type)
        assertTrue(hit.isFromNpc)
        assertEquals(boss.slotId, hit.hitmark.sourceSlot)
        assertEquals(venomBefore, player.vars["varp.venom_strikes"])
        world.cycle(firstTailTick + 4)
        assertEquals(1, hits.size)
    }

    @Test
    fun `tail hits only the captured true tile and misses adjacent tiles and other planes`() {
        val offsets = (-2..2).flatMap { x -> (-2..2).map { z -> x to z } }
        for ((x, z) in offsets) {
            val world = World(openingEnabled = true)
            val (player, _) = world.prepareTail()
            val locked = player.coords
            world.cycle(firstTailTick)
            player.coords = locked.translate(x, z)
            world.cycle(firstTailTick + 4)
            val caught = x == 0 && z == 0
            assertEquals(caught, player.hitpoints < 99, "offset=$x,$z")
            assertEquals(caught, player.movementDelay > player.currentMapClock, "offset=$x,$z")
        }
        val world = World(openingEnabled = true)
        val (player, _) = world.prepareTail()
        world.cycle(firstTailTick)
        player.coords = player.coords.copy(level = 1)
        world.cycle(firstTailTick + 4)
        assertEquals(99, player.hitpoints)
        assertEquals(-1, player.movementDelay)
    }

    @Test
    fun `stun blocks native movement and attacks for five ticks without delaying incoming damage`() {
        val world = World(openingEnabled = true)
        val (player, _) = world.prepareTail()
        world.cycle(firstTailTick + 4)
        val impact = firstTailTick + 4
        val tile = player.coords
        // Isolate the movement lock from the island's walk-blocked scenery.
        world.collision[tile.x, tile.z, tile.level] = 0
        world.collision[tile.x + 1, tile.z, tile.level] = 0
        assertEquals(impact + 5, player.actionDelay)
        assertEquals(impact + 5, player.movementDelay)
        assertFalse(player.isDelayed)
        assertFalse(player.isFrozen)
        for (tick in impact until impact + 5) {
            world.cycle(tick)
            player.walk(tile.translate(1, 0))
            world.move(player)
            assertEquals(tile, player.coords, "tick=$tick")
            assertFalse(player.canProcessMovement)
            assertTrue(player.actionDelay > player.currentMapClock)
        }
        val hp = player.hitpoints
        world.combat.cloudDamage(player)
        assertTrue(player.hitpoints < hp)
        world.cycle(impact + 5)
        assertTrue(player.canProcessMovement)
        assertTrue(player.actionDelay <= player.currentMapClock)
        player.walk(tile.translate(1, 0))
        world.move(player)
        assertEquals(tile.translate(1, 0), player.coords)
    }

    @Test
    fun `first tail stun expires in time to dodge the second strike through native movement`() {
        val world = World(openingEnabled = true)
        val (player, boss) = world.prepareTail()
        val messages = player.captureMessages()
        val tile = player.coords
        world.collision[tile.x, tile.z, tile.level] = 0
        world.collision[tile.x + 1, tile.z, tile.level] = 0
        val second = 4 + ZulrahRoutine.recorded.events.filter { it.kind == "tail" }[1].tick
        world.cycle(firstTailTick + 4)
        val hpAfterFirstHit = player.hitpoints
        assertTrue(hpAfterFirstHit < 99)
        world.cycle(second)
        assertEquals(tile, boss.faceLockSquare)
        assertFalse(player.canProcessMovement)
        val recovery = firstTailTick + 4 + ZulrahSpec.tail.stunTicks
        assertEquals(2, second + 4 - recovery)
        world.cycle(recovery)
        assertTrue(player.canProcessMovement)
        player.walk(tile.translate(1, 0))
        world.move(player)
        assertEquals(tile.translate(1, 0), player.coords)
        world.cycle(second + 4)
        assertEquals(hpAfterFirstHit, player.hitpoints)
        assertEquals(1, messages.size)
        assertTrue(player.canProcessMovement)
    }

    @Test
    fun `second swing retargets after a dodge and can be dodged independently`() {
        val world = World(openingEnabled = true)
        val (player, boss) = world.prepareTail()
        val first = player.coords
        world.cycle(firstTailTick)
        player.coords = first.translate(2, 0)
        world.cycle(firstTailTick + 4)
        assertEquals(99, player.hitpoints)
        val second = 4 + ZulrahRoutine.recorded.events.filter { it.kind == "tail" }[1].tick
        world.cycle(second)
        assertEquals(player.coords, boss.faceLockSquare)
        player.coords = first
        world.cycle(second + 4)
        assertEquals(99, player.hitpoints)
        assertEquals(-1, player.movementDelay)
    }

    @Test
    fun `god mode prevents tail damage and stun and lethal hits queue native death without a lock`() {
        for (god in listOf(true, false)) {
            val world = World(openingEnabled = true)
            val (player, _) = world.prepareTail()
            player.adminGodMode = god
            val messages = player.captureMessages()
            player.pendingSpotanims.clear()
            player.statMap.setCurrentLevel("stat.hitpoints", 10)
            world.cycle(firstTailTick + 4)
            assertEquals(if (god) 10 else 0, player.hitpoints)
            assertEquals(-1, player.movementDelay)
            assertFalse(player.isDelayed)
            assertEquals(!god, "queue.death" in player.queueList)
            assertTrue(messages.isEmpty())
            assertTrue(player.pendingSpotanims.isEmpty)
        }
    }

    @Test
    fun `pending tail is cancelled on boss death dive departure logout and owner death`() {
        for (reason in listOf("boss", "dive", "leave", "logout", "death")) {
            val world = World(openingEnabled = true)
            val (player, boss) = world.prepareTail()
            val session = requireNotNull(world.manager.sessionForPlayer(player))
            world.cycle(firstTailTick)
            when (reason) {
                "boss" -> { boss.hitpoints = 0; world.opening.beginDeath(boss) }
                "dive" -> world.opening.dive(boss)
                "leave" -> player.coords = world.manager.leave(player, session, firstTailTick)
                "logout" -> world.manager.handleLogout(player, firstTailTick)
                "death" -> world.manager.handleDeath(player, firstTailTick)
            }
            world.cycle(firstTailTick + 4)
            assertEquals(99, player.hitpoints, reason)
            assertEquals(-1, player.movementDelay, reason)
        }
    }

    @Test
    fun `leaving during stun releases only this fights locks and preserves longer delays`() {
        for (longerDelay in listOf(false, true)) {
            val world = World(openingEnabled = true)
            val (player, _) = world.prepareTail()
            val session = requireNotNull(world.manager.sessionForPlayer(player))
            player.frozen = true
            player.freezeImmune = true
            if (longerDelay) {
                player.movementDelay = firstTailTick + 20
                player.actionDelay = firstTailTick + 30
            }
            world.cycle(firstTailTick + 4)
            assertTrue(player.hitpoints < 99)
            player.coords = world.manager.leave(player, session, firstTailTick + 4)
            assertEquals(if (longerDelay) firstTailTick + 20 else -1, player.movementDelay)
            assertEquals(if (longerDelay) firstTailTick + 30 else -1, player.actionDelay)
            assertTrue(player.frozen)
            assertTrue(player.freezeImmune)
            world.cycle(firstTailTick + 11)
            assertEquals(if (longerDelay) firstTailTick + 20 else -1, player.movementDelay)
        }
    }

    @Test
    fun `tail only hits its owner even when another player is on the targeted tile`() {
        val world = World(openingEnabled = true)
        val (player, _) = world.prepareTail()
        val other = world.player(2)
        other.coords = player.coords
        world.cycle(firstTailTick + 4)
        assertTrue(player.hitpoints < 99)
        assertEquals(99, other.hitpoints)
        assertEquals(-1, other.movementDelay)
    }

    @Test
    fun `stun expiry preserves delays extended by another action`() {
        val world = World(openingEnabled = true)
        val (player, _) = world.prepareTail()
        world.cycle(firstTailTick + 4)
        player.movementDelay = firstTailTick + 20
        player.actionDelay = firstTailTick + 30
        world.cycle(firstTailTick + 11)
        assertEquals(firstTailTick + 20, player.movementDelay)
        assertEquals(firstTailTick + 30, player.actionDelay)
    }

    @Test
    fun `remaining on the targeted tile allows a second damaging hit to apply a fresh five tick stun`() {
        val world = World(openingEnabled = true)
        val (player, _) = world.prepareTail()
        val messages = player.captureMessages()
        val secondImpact = 4 + ZulrahRoutine.recorded.events.filter { it.kind == "tail" }[1].tick + 4
        world.cycle(firstTailTick + 4)
        val hp = player.hitpoints
        world.cycle(secondImpact)
        assertEquals(List(2) { MessageGame(105, "<col=ff0000>You've been stunned!</col>") }, messages)
        assertEquals(EntitySpotanim(80, 0, 100, 2).packed, player.pendingSpotanims.last())
        assertTrue(player.hitpoints < hp)
        assertEquals(secondImpact + 5, player.movementDelay)
        assertEquals(secondImpact + 5, player.actionDelay)
        world.cycle(secondImpact + 4)
        assertFalse(player.canProcessMovement)
        world.cycle(secondImpact + 5)
        assertTrue(player.canProcessMovement)
        assertTrue(player.actionDelay <= player.currentMapClock)
    }

    @Test
    fun `replacement swing invalidates the earlier pending impact even on the same tile`() {
        val world = World(openingEnabled = true)
        val (player, boss) = world.prepareTail()
        world.cycle(firstTailTick)
        world.cycle(firstTailTick + 1)
        world.opening.tailWindup(boss, ZulrahSpec.tail)
        world.cycle(firstTailTick + 4)
        assertEquals(99, player.hitpoints)
        world.cycle(firstTailTick + 5)
        assertTrue(99 - player.hitpoints in 20..30)
        val hp = player.hitpoints
        world.cycle(firstTailTick + 6)
        assertEquals(hp, player.hitpoints)
    }

    @Test
    fun `projectile obstruction at windup or impact prevents tail damage`() {
        for (beforeWindup in listOf(true, false)) {
            val world = World(openingEnabled = true)
            val (player, _) = world.prepareTail()
            if (!beforeWindup) world.cycle(firstTailTick)
            val tile = player.coords
            world.collision.add(tile.x, tile.z + 1, tile.level,
                org.rsmod.routefinder.flag.CollisionFlag.LOC_PROJ_BLOCKER)
            world.cycle(firstTailTick + 4)
            assertEquals(99, player.hitpoints)
            assertEquals(-1, player.movementDelay)
        }
    }

    @Test
    fun `every cloud tile deals a venom hitsplat each tick without a venom status`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player)
        val hits = mutableListOf<org.rsmod.game.hit.Hit>()
        world.bus.subscribeUnbound(org.rsmod.api.player.events.PlayerHitEvents.Impact::class.java) {
            hits += hit
        }
        world.cycle(20)
        val cloud = requireNotNull(world.manager.resolveCoord(session, ZulrahIsland.openingSpawn)).translate(2, -5)
        val tiles = (0..2).flatMap { x -> (0..2).map { z -> cloud.translate(x, z) } }
        for ((index, tile) in tiles.withIndex()) {
            world.cycle(20 + index)
            player.coords = tile
            world.lateCycle(20 + index)
        }
        assertEquals(9, hits.size)
        assertTrue(hits.all { it.damage in 1..4 && it.type == HitType.Typeless })
        assertTrue(hits.all { it.hitmark.isNoSource && it.hitmark.delay == 0 })
        val venom = org.rsmod.api.config.refs.done.hitmark_groups.venom.lit.asRSCM(RSCMType.HITMARK)
        assertTrue(hits.all { it.hitmark.self == venom })
        assertEquals(99 - hits.sumOf { it.damage }, player.hitpoints)
        assertEquals(0, player.vars["varp.venom_strikes"])
    }

    @Test
    fun `turning god mode off restores cloud damage on the next occupied tick`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player)
        world.cycle(20)
        player.coords = requireNotNull(world.manager.resolveCoord(session, ZulrahIsland.openingSpawn)).translate(2, -5)
        player.adminGodMode = true
        world.lateCycle(20)
        assertEquals(99, player.hitpoints)
        player.adminGodMode = false
        world.cycle(21)
        world.lateCycle(21)
        assertTrue(99 - player.hitpoints in 1..4)
        assertEquals(0, player.vars["varp.venom_strikes"])
    }

    @Test
    fun `boat script registers no diagnostic commands`() {
        val world = World()
        val commands = CheatCommandMap()
        with(ZulrahBoatScript(world.manager, world.opening, world.bus)) {
            ScriptContext(world.bus, commands, EngineQueueCache()).startup()
        }
        assertTrue(commands.commands.isEmpty())
    }

    @Test
    fun `teleport scroll uses the held item DSL consumes one and arrives after three ticks`() {
        val world = World()
        val player = world.player(1)
        player.coords = CoordGrid(3200, 3200, 0)
        player.inv[0] = org.rsmod.game.inv.InvObj(ZulAndraTeleportScript.SCROLL, 2)
        world.installTeleport()
        world.readScroll(player)
        assertEquals(1, player.inv[0]?.count)
        assertEquals(3864, player.pendingSequence.id)
        world.advancePlayer(player, 3)
        assertEquals(CoordGrid(3200, 3200, 0), player.coords)
        world.advancePlayer(player, 4)
        assertEquals(ZulrahIsland.zulAndraTeleport, player.coords)
        assertEquals(0, player.pendingSequence.id)
        assertEquals(1, player.inv[0]?.count)
    }

    @Test
    fun `blocked scroll and unmet Regicide do not consume inventory or teleport`() {
        for (questBlocked in listOf(false, true)) {
            val world = World()
            val player = world.player(1)
            player.coords = CoordGrid(3200, 3200, 0)
            player.inv[0] = org.rsmod.game.inv.InvObj(ZulAndraTeleportScript.SCROLL, 2)
            world.installTeleport(questBlocked, teleportBlocked = !questBlocked)
            world.readScroll(player)
            world.advancePlayer(player, 10)
            assertEquals(2, player.inv[0]?.count)
            assertEquals(CoordGrid(3200, 3200, 0), player.coords)
        }
    }

    @Test
    fun `scroll leaving a live fight triggers existing instance cleanup`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player)
        world.cycle(4)
        val boss = world.manager.npcsForInstance(session.id).single()
        player.inv[0] = org.rsmod.game.inv.InvObj(ZulAndraTeleportScript.SCROLL, 1)
        world.installTeleport()
        world.readScroll(player)
        world.advancePlayer(player, 7)
        assertNull(player.inv[0])
        assertEquals(ZulrahIsland.zulAndraTeleport, player.coords)
        world.manager.tickReclaim(8)
        assertFalse(boss.isSlotAssigned)
        world.assertGone(session)
    }

    @Test
    fun `pending entries reserve distinct regions until cancelled`() {
        val world = World()
        val first = world.create(world.player(1))
        val second = world.create(world.player(2))
        assertNotEquals(first.enter, second.enter)
        assertNotEquals(first.session.regionIds, second.session.regionIds)
        assertSame(first.session, world.manager.sessionForRegion(first.session.regionIds.single()))
        world.registry.removeInactiveSmallRegions()
        assertNotNull(world.registry[first.enter])
        assertNotNull(world.registry[second.enter])
    }

    @Test
    fun `solo sessions reject other players and keep damage separate`() {
        val world = World()
        val firstPlayer = world.player(1)
        val secondPlayer = world.player(2)
        val first = world.enter(firstPlayer)
        val second = world.enter(secondPlayer)
        assertNotEquals(first.id, second.id)
        assertNotEquals(firstPlayer.coords, secondPlayer.coords)
        assertInstanceOf(InstanceManager.Result.Failed::class.java,
            world.manager.join(secondPlayer, first, 2))
        assertInstanceOf(InstanceManager.Result.Failed::class.java,
            world.manager.join(secondPlayer, first, 2, forceAccess = true))
        first.damageContributions.record(firstPlayer, 50)
        assertEquals(50, first.damageContributions.damageBy(firstPlayer))
        assertTrue(second.damageContributions.isEmpty)
        assertSame(second, world.manager.sessionForPlayer(secondPlayer))
    }

    @Test
    fun `leaving removes only that sessions NPCs and permits a fresh entry`() {
        val world = World()
        val player = world.player(1)
        val other = world.player(2)
        val first = world.enter(player)
        val second = world.enter(other)
        val npc = world.npc(first, player.coords)
        val otherNpc = world.npc(second, other.coords)
        val firstTile = player.coords
        val exit = world.manager.leave(player, first, 2)
        player.coords = exit
        assertEquals(ZulrahIsland.zulAndraTeleport, exit)
        assertFalse(npc.isSlotAssigned)
        assertNull(world.manager.instanceForNpc(npc))
        assertTrue(otherNpc.isSlotAssigned)
        assertEquals(second.id, world.manager.instanceForNpc(otherNpc))
        assertNull(player.currentInstanceId())
        world.assertGone(first)
        world.registry.removeInactiveSmallRegions()
        assertNull(world.registry[firstTile])
        assertNotNull(world.registry[other.coords])
        val fresh = world.enter(player)
        assertNotEquals(first.id, fresh.id)
        assertTrue(world.manager.npcsForInstance(fresh.id).isEmpty())
        assertTrue(fresh.damageContributions.isEmpty)
    }

    @Test
    fun `logout stores outside return tile and removes the private session`() {
        val world = World()
        val player = world.player(1)
        val session = world.enter(player)
        val npc = world.npc(session, player.coords)
        world.manager.handleLogout(player, 2)
        world.manager.handleLogout(player, 3)
        assertEquals(ZulrahIsland.zulAndraTeleport.packed,
            player.attr[InstanceAttributes.LOGIN_EXIT_COORD])
        assertNull(player.currentInstanceId())
        assertFalse(npc.isSlotAssigned)
        world.assertGone(session)
        assertEquals(listOf(session.id), world.ended)
    }

    @Test
    fun `death cleanup ends only the deceased players session`() {
        val world = World()
        val player = world.player(1)
        val other = world.player(2)
        val session = world.enter(player)
        val second = world.enter(other)
        val npc = world.npc(session, player.coords)
        world.manager.handleDeath(player, 2)
        assertFalse(npc.isSlotAssigned)
        assertNull(player.currentInstanceId())
        world.assertGone(session)
        assertSame(second, world.manager.sessionForPlayer(other))
        assertNull(player.attr[InstanceAttributes.LOGIN_EXIT_COORD])
    }

    @Test
    fun `teleporting outside is reconciled on the next tick`() {
        val world = World()
        val player = world.player(1)
        val session = world.enter(player)
        val npc = world.npc(session, player.coords)
        player.coords = ZulrahIsland.zulAndraTeleport
        world.manager.tickReclaim(2)
        assertFalse(npc.isSlotAssigned)
        assertNull(player.currentInstanceId())
        world.assertGone(session)
    }

    @Test
    fun `missing player is reclaimed even without the logout event`() {
        val world = World()
        val player = world.player(1)
        val session = world.enter(player)
        val npc = world.npc(session, player.coords)
        world.players.remove(1)
        world.manager.tickReclaim(2)
        world.assertGone(session)
        assertFalse(npc.isSlotAssigned)
        assertEquals(listOf(session.id), world.ended)
    }

    @Test
    fun `logout during pending entry releases ownership and allocation`() {
        val world = World()
        val player = world.player(1)
        val pending = world.create(player)
        world.manager.handleLogout(player, 2)
        world.assertGone(pending.session)
        world.registry.removeInactiveSmallRegions()
        assertNull(world.registry[pending.enter])
        val fresh = world.create(player)
        assertNotEquals(pending.session.id, fresh.session.id)
    }

    @Test
    fun `shared API preserves another bosses reclaim window until its deadline`() {
        val world = World()
        val player = world.player(1)
        val spec = ZulrahIsland.spec(player.coords).copy(
            destroyWhenEmpty = false, reclaimTicks = 10,
        )
        val pending = world.create(player, spec)
        player.coords = pending.enter
        world.manager.finalizeEntry(player, pending.session, 1)
        world.manager.handleLogout(player, 2)
        world.manager.tickReclaim(11)
        world.registry.removeInactiveSmallRegions()
        assertSame(pending.session, world.manager.sessionForId(pending.session.id))
        assertNotNull(world.registry[pending.enter])
        world.manager.tickReclaim(12)
        world.registry.removeInactiveSmallRegions()
        world.assertGone(pending.session)
        assertNull(world.registry[pending.enter])
    }

    @Test
    fun `shared API preserves server owned rooms when their last occupant vanishes`() {
        val world = World()
        val player = world.player(1)
        val spec = ZulrahIsland.spec(player.coords)
        val session = requireNotNull(world.manager.createServerOwned(KEY, spec, InstanceAccess.Friends, 1))
        val joined = world.manager.join(player, session, 1) as InstanceManager.Result.Joined
        player.coords = joined.enter
        world.manager.finalizeEntry(player, session, 1)
        world.players.remove(1)
        world.manager.tickReclaim(2)
        world.registry.removeInactiveSmallRegions()
        assertTrue(session.occupants.isEmpty())
        assertSame(session, world.manager.sessionForId(session.id))
        assertNotNull(world.registry[joined.enter])
        assertTrue(world.ended.isEmpty())
    }

    @Test
    fun `arrival waits indefinitely for the rowing prompt to be dismissed`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player, continueEntry = false)
        for (tick in 1..100) world.cycle(tick)
        assertEquals(ZulrahEncounterController.State.AwaitingContinue, world.opening.state(session.id))
        assertTrue(world.manager.npcsForInstance(session.id).isEmpty())
        assertTrue(world.opening.continueEntry(player, session.id, 100))
        world.cycle(100)
        assertTrue(world.manager.npcsForInstance(session.id).isEmpty())
        world.cycle(101)
        assertEquals(5071, world.manager.npcsForInstance(session.id).single().pendingSequence.id)
    }

    @Test
    fun `duplicate continue or join events cannot delay or duplicate the opening`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player, continueEntry = false)
        assertTrue(world.opening.continueEntry(player, session.id, 10))
        assertFalse(world.opening.continueEntry(player, session.id, 11))
        world.opening.enter(player, session.id)
        world.cycle(11)
        val npc = world.manager.npcsForInstance(session.id).single()
        assertFalse(world.opening.continueEntry(player, session.id, 12))
        world.cycle(12)
        assertEquals(listOf(npc), world.manager.npcsForInstance(session.id))
    }

    @Test
    fun `continue rejects other players and owners no longer on their island`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player, continueEntry = false)
        assertFalse(world.opening.continueEntry(world.player(2), session.id, 10))
        player.coords = ZulrahIsland.zulAndraTeleport
        assertFalse(world.opening.continueEntry(player, session.id, 10))
        world.cycle(11)
        assertTrue(world.manager.npcsForInstance(session.id).isEmpty())
    }

    @Test
    fun `logout while awaiting continue cannot start the old or next island`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player, continueEntry = false)
        world.manager.handleLogout(player, 10)
        assertFalse(world.opening.continueEntry(player, session.id, 11))
        val fresh = world.enter(player, continueEntry = false)
        world.cycle(100)
        assertTrue(world.manager.npcsForInstance(fresh.id).isEmpty())
        assertEquals(ZulrahEncounterController.State.AwaitingContinue, world.opening.state(fresh.id))
    }

    @Test
    fun `opening spawns once at the recorded north anchor inside its own island`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player)
        world.cycle(3)
        assertTrue(world.manager.npcsForInstance(session.id).isEmpty())
        world.cycle(4)
        val npc = world.manager.npcsForInstance(session.id).single()
        assertEquals(2042, npc.id)
        assertEquals(500, npc.hitpoints)
        assertEquals(5, npc.size)
        assertEquals(world.manager.resolveCoord(session, ZulrahIsland.openingSpawn), npc.coords)
        assertEquals(player.coords.translate(-2, 5), npc.coords)
        assertEquals(Direction.South, npc.respawnDir)
        assertEquals(5071, npc.pendingSequence.id)
        assertFalse(npc.respawns)
        assertTrue(npc.movementLocked)
        assertEquals(dev.openrune.types.NpcMode.ApPlayer2, npc.mode)
        assertEquals(session.id, world.manager.instanceForNpc(npc))
        world.opening.enter(player, session.id)
        world.cycle(5)
        assertEquals(listOf(npc), world.manager.npcsForInstance(session.id))
    }

    @Test
    fun `opening clocks and cleanup are independent between owners`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val other = world.player(2)
        val first = world.enter(player)
        world.cycle(4)
        val npc = world.manager.npcsForInstance(first.id).single()
        val pending = world.create(other)
        other.coords = pending.enter
        world.entryTick = 4
        world.manager.finalizeEntry(other, pending.session, 4)
        assertTrue(world.opening.continueEntry(other, pending.session.id, 6))
        world.cycle(6)
        assertTrue(world.manager.npcsForInstance(pending.session.id).isEmpty())
        world.cycle(7)
        val otherNpc = world.manager.npcsForInstance(pending.session.id).single()
        assertNotEquals(npc.coords, otherNpc.coords)
        world.manager.handleLogout(player, 8)
        world.cycle(9)
        assertFalse(npc.isSlotAssigned)
        assertFalse(world.opening.owns(npc))
        assertTrue(world.opening.owns(otherNpc))
        assertTrue(otherNpc.isSlotAssigned)
    }

    @Test
    fun `leaving before emergence cannot spawn into a reused island`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val first = world.enter(player)
        player.coords = world.manager.leave(player, first, 2)
        world.registry.removeInactiveSmallRegions()
        world.entryTick = 10
        val fresh = world.enter(player)
        world.opening.enter(player, first.id)
        world.cycle(12)
        assertTrue(world.manager.npcsForInstance(first.id).isEmpty())
        assertTrue(world.manager.npcsForInstance(fresh.id).isEmpty())
        world.cycle(13)
        val npc = world.manager.npcsForInstance(fresh.id).single()
        assertEquals(fresh.id, world.manager.instanceForNpc(npc))
        world.manager.handleDeath(player, 14)
        world.cycle(100)
        assertFalse(world.opening.owns(npc))
        assertFalse(npc.isSlotAssigned)
        world.assertGone(fresh)
    }

    @Test
    fun `missing or teleported owner cannot trigger a pending spawn`() {
        for (missing in listOf(false, true)) {
            val world = World(openingEnabled = true)
            val player = world.player(1)
            val session = world.enter(player)
            if (missing) world.players.remove(1) else player.coords = ZulrahIsland.zulAndraTeleport
            world.cycle(4)
            assertTrue(world.manager.npcsForInstance(session.id).isEmpty())
            world.manager.tickReclaim(4)
            world.cycle(100)
            world.assertGone(session)
        }
    }

    @Test
    fun `pending entry or non owner cannot start an opening`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val other = world.player(2)
        val pending = world.create(player)
        world.opening.enter(player, pending.session.id)
        world.opening.enter(other, pending.session.id)
        world.cycle(10)
        assertTrue(world.manager.npcsForInstance(pending.session.id).isEmpty())
    }

    @Test
    fun `attack guard permits owner and silently denies others and submerged boss`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player)
        world.cycle(4)
        val npc = world.manager.npcsForInstance(session.id).single()
        val guard = ZulrahAttackGuard(world.opening)
        assertEquals(NpcAttackValidateResult.Pass, guard.validate(player, npc))
        val result = guard.validate(world.player(2), npc)
        assertInstanceOf(NpcAttackValidateResult.Deny::class.java, result)
        assertNull((result as NpcAttackValidateResult.Deny).message)
        assertEquals(NpcAttackValidateResult.Pass, guard.validate(player, Npc(npc.type, npc.coords)))
        assertEquals(NpcAttackValidateResult.Pass, guard.validate(player, world.npc(session, player.coords)))
        world.cycle(30)
        assertInstanceOf(NpcAttackValidateResult.Deny::class.java, guard.validate(player, npc))
    }

    @Test
    fun `server tick event spawns and hit event caps only the owned boss`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player)
        world.cycle(4)
        val npc = world.manager.npcsForInstance(session.id).single()
        fun hit() = HitBuilder(
            type = HitType.Magic, damage = 100, sourceUid = null, sourceSlot = null,
            isFromNpc = false, isFromPlayer = false, clientDelay = 0,
            righthandType = null, secondaryType = null, targetHitmark = 0, sourceHitmark = 0,
            publicHitmark = null, zeroDamageHitmarkLit = null, zeroDamageHitmarkTint = null,
            maxDamageHitmarkLit = null, targetMaxDamageThreshold = Int.MAX_VALUE,
            sourceMaxDamageThreshold = Int.MAX_VALUE,
        )
        val previewHit = hit()
        world.bus.publish(NpcHitEvents.Modify(npc, previewHit))
        assertTrue(previewHit.damage in 45..50)
        val unrelatedHit = hit()
        world.bus.publish(NpcHitEvents.Modify(Npc(npc.type, npc.coords), unrelatedHit))
        assertEquals(100, unrelatedHit.damage)
        world.manager.handleLogout(player, 5)
        world.cycle(100)
        assertFalse(npc.isSlotAssigned)
        assertFalse(world.opening.owns(npc))
        world.assertGone(session)
    }

    @Test
    fun `boss retains identity and damage through form and position changes`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player)
        world.cycle(4)
        val boss = world.manager.npcsForInstance(session.id).single()
        val origin = boss.coords
        val uid = boss.uid
        boss.hitpoints = 321
        for (tick in 5..33) world.cycle(tick)
        assertEquals(2044, boss.visType.id)
        assertEquals(origin.translate(10, -2), boss.coords)
        assertEquals(321, boss.hitpoints)
        assertEquals(uid, boss.uid)
        assertTrue(world.opening.canAttack(player, boss))
    }

    @Test
    fun `clouds spawn in private map and expire after thirty recorded ticks`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player)
        world.cycle(4)
        val origin = world.manager.npcsForInstance(session.id).single().coords
        val tile = origin.translate(2, -5)
        for (tick in 5..19) world.cycle(tick)
        assertNull(world.locRegistry.findType(tile, 11700))
        world.cycle(20)
        assertNotNull(world.locRegistry.findType(tile, 11700))
        for (tick in 21..49) world.cycle(tick)
        assertNotNull(world.locRegistry.findType(tile, 11700))
        world.cycle(50)
        assertNull(world.locRegistry.findType(tile, 11700))
    }

    @Test
    fun `cloud footprint is exactly the cached three by three tiles on its plane`() {
        val origin = CoordGrid(2268, 3068, 0)
        for (x in -1..3) for (z in -1..3) {
            assertEquals(x in 0..2 && z in 0..2,
                ZulrahEncounterController.cloudContains(origin, origin.translate(x, z)))
        }
        assertFalse(ZulrahEncounterController.cloudContains(origin, origin.copy(level = 1)))
    }

    @Test
    fun `cloud damage runs after movement once per tick without venom or prayer protection`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player)
        for (tick in 1..20) world.cycle(tick)
        val cloud = requireNotNull(world.manager.resolveCoord(session, ZulrahIsland.openingSpawn)).translate(2, -5)
        player.coords = cloud.translate(1, 1)
        player.protectMagic = 1
        world.lateCycle(20)
        assertTrue(99 - player.hitpoints in 1..4)
        val hp = player.hitpoints
        world.lateCycle(20)
        assertEquals(hp, player.hitpoints)
        world.cycle(21)
        world.lateCycle(21)
        assertTrue(hp - player.hitpoints in 1..4)
        assertEquals(0, player.vars["varp.venom_strikes"])
        val afterDamage = player.hitpoints
        world.cycle(22)
        player.coords = cloud.translate(20, 20)
        world.lateCycle(22)
        assertEquals(afterDamage, player.hitpoints)
    }

    @Test
    fun `cloud damage stops on expiry boss death and leaving the private island`() {
        for (reason in listOf("expiry", "boss", "exit")) {
            val world = World(openingEnabled = true)
            val player = world.player(1)
            val session = world.enter(player)
            for (tick in 1..20) world.cycle(tick)
            val boss = world.manager.npcsForInstance(session.id).first()
            player.coords = boss.coords.translate(2, -5)
            when (reason) {
                "expiry" -> for (tick in 21..50) world.cycle(tick)
                "boss" -> boss.hitpoints = 0
                "exit" -> player.coords = world.manager.leave(player, session, 21)
            }
            world.lateCycle(if (reason == "expiry") 50 else 21)
            assertEquals(99, player.hitpoints, reason)
        }
    }

    @Test
    fun `clouds affect only their owner and respect admin god mode`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val other = world.player(2)
        val session = world.enter(player)
        for (tick in 1..20) world.cycle(tick)
        val cloud = requireNotNull(world.manager.resolveCoord(session, ZulrahIsland.openingSpawn)).translate(2, -5)
        player.coords = cloud
        other.coords = cloud
        world.lateCycle(20)
        assertTrue(player.hitpoints < 99)
        assertEquals(99, other.hitpoints)
        player.adminGodMode = true
        val hp = player.hitpoints
        world.lateCycle(21)
        assertEquals(hp, player.hitpoints)
    }

    @Test
    fun `death cancels pending summons and removes boss after six ticks with owned exit`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val messages = player.captureMessages()
        val session = world.enter(player)
        world.collision[player.x, player.z, player.level] = 0
        world.collision[player.x + 1, player.z, player.level] = 0
        for (tick in 1..41) world.cycle(tick)
        val boss = world.manager.npcsForInstance(session.id).first()
        boss.heroPoints(player, 500)
        val snake = world.manager.npcsForInstance(session.id).first { it !== boss }
        val cloudTile = requireNotNull(world.manager.resolveCoord(session, ZulrahIsland.openingSpawn)).translate(2, -5)
        boss.hitpoints = 0
        world.cycle(42)
        assertEquals(ZulrahEncounterController.State.Dying, world.opening.state(session.id))
        assertEquals(5804, boss.pendingSequence.id)
        assertTrue(snake.isSlotAssigned)
        assertEquals(0, snake.hitpoints)
        assertEquals(2408, snake.pendingSequence.id)
        assertNull(world.locRegistry.findType(cloudTile, 11700))
        for (tick in 43..47) world.cycle(tick)
        assertTrue(world.kills.isEmpty())
        assertFalse(snake.isSlotAssigned)
        assertTrue(boss.isSlotAssigned)
        world.cycle(48)
        val kill = world.kills.single()
        assertSame(player, kill.hero)
        assertEquals(player.coords, kill.dropCoords)
        assertEquals(18000, kill.dropDuration)
        assertEquals(listOf("obj.snakeboss_scale".asRSCM(RSCMType.OBJ)),
            world.objs.findAll(player.coords).map { it.type }.toList())
        assertTrue(world.objs.findAll(player.coords).all { it.ownerId == player.observerUUID })
        world.opening.finishDeath(boss)
        assertEquals(1, world.kills.size)
        assertFalse(boss.isSlotAssigned)
        assertEquals(ZulrahEncounterController.State.Finished, world.opening.state(session.id))
        assertNull(world.locRegistry.findType(player.coords, 11701))
        val exitTile = (-1..1).flatMap { x -> (-1..1).map { z -> player.coords.translate(x, z) } }
            .single { world.locRegistry.findType(it, 11701) != null }
        assertNotEquals(player.coords, exitTile)
        assertTrue(RayCastValidator(world.collision).hasLineOfWalk(player.coords, exitTile))
        assertTrue(world.opening.mayUseExit(player, exitTile))
        assertFalse(world.opening.mayUseExit(world.player(2), exitTile))
        assertFalse(world.opening.mayUseExit(player, player.coords))
        assertEquals(1, player.vars[ZulrahKillHook.KILLCOUNT])
        assertEquals(35, org.rsmod.content.generic.killcount.BossRecords.bestTicks(player, 1518))
        assertEquals(listOf(
            MessageGame(0, "Your Zulrah kill count is: <col=ff0000>1</col>."),
            MessageGame(0, "Fight duration: <col=ff0000>0:21</col> (new personal best)"),
        ), messages.takeLast(2))
        assertTrue(world.manager.npcsForInstance(session.id).none { it.isSlotAssigned })
        val lootTile = player.coords
        player.coords = world.manager.leave(player, session, 49)
        assertTrue(world.objs.findAll(lootTile).none())
        assertTrue(world.objs.findAll(exitTile).none())
        assertNull(world.locRegistry.findType(exitTile, 11701))
        assertNull(world.opening.state(session.id))
    }

    @Test
    fun `completion preserves faster and equal personal bests with precise OSRS messages`() {
        for (previous in listOf(34, 35, 36)) {
            val world = World(openingEnabled = true)
            val player = world.player(1)
            val messages = player.captureMessages()
            org.rsmod.content.generic.killcount.BossRecords.recordBest(player, 1518, previous)
            org.rsmod.api.player.vars.VarPlayerIntMapSetter.set(player, "varbit.option_precise_timing", 1)
            val session = world.enter(player)
            world.cycle(41)
            val boss = world.manager.npcsForInstance(session.id).first()
            boss.heroPoints(player, 500)
            assertNull(world.opening.claimCompletion(boss, player))
            boss.hitpoints = 0
            world.cycle(48)
            assertEquals(1, player.vars[ZulrahKillHook.KILLCOUNT])
            assertEquals(minOf(previous, 35), org.rsmod.content.generic.killcount.BossRecords.bestTicks(player, 1518))
            val text = when (previous) {
                34 -> "Fight duration: <col=ff0000>0:21.00</col>. Personal best: 0:20.40"
                35 -> "Fight duration: <col=ff0000>0:21.00</col>. Personal best: 0:21.00"
                else -> "Fight duration: <col=ff0000>0:21.00</col> (new personal best)"
            }
            assertEquals(MessageGame(0, text), messages.last())
            ZulrahKillHook(world.opening).onKill(world.kills.single())
            assertEquals(1, player.vars[ZulrahKillHook.KILLCOUNT])
        }
    }

    @Test
    fun `uncredited boss death does not award a count or personal best`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player)
        world.cycle(5)
        world.manager.npcsForInstance(session.id).single().hitpoints = 0
        world.cycle(12)
        assertTrue(world.kills.isEmpty())
        assertEquals(0, player.vars[ZulrahKillHook.KILLCOUNT])
        assertEquals(0, org.rsmod.content.generic.killcount.BossRecords.bestTicks(player, 1518))
    }

    @Test
    fun `leaving or dying during the death animation cancels loot`() {
        for (leave in listOf(true, false)) {
            val world = World(openingEnabled = true)
            val player = world.player(1)
            val session = world.enter(player)
            world.cycle(5)
            val boss = world.manager.npcsForInstance(session.id).single()
            boss.heroPoints(player, 500)
            boss.hitpoints = 0
            world.cycle(6)
            if (leave) player.coords = world.manager.leave(player, session, 6)
            else world.manager.handleDeath(player, 6)
            world.cycle(15)
            assertTrue(world.kills.isEmpty())
            assertEquals(0, player.vars[ZulrahKillHook.KILLCOUNT])
            assertEquals(0, org.rsmod.content.generic.killcount.BossRecords.bestTicks(player, 1518))
        }
    }

    @Test
    fun `loot follows the surviving owner at completion and preserves another islands loot`() {
        val world = World(openingEnabled = true)
        val first = world.player(1)
        val second = world.player(2)
        val a = world.enter(first)
        val b = world.enter(second)
        world.cycle(5)
        for ((player, session) in listOf(first to a, second to b)) {
            val boss = world.manager.npcsForInstance(session.id).single()
            boss.heroPoints(player, 500)
            boss.hitpoints = 0
        }
        world.cycle(6)
        val oldTile = first.coords
        first.coords = first.coords.translate(1, 0)
        world.cycle(12)
        assertEquals(2, world.kills.size)
        assertTrue(world.objs.findAll(oldTile).none())
        assertTrue(world.objs.findAll(first.coords).any())
        assertTrue(world.objs.findAll(second.coords).any())
        val lootTile = first.coords
        first.coords = world.manager.leave(first, a, 13)
        assertTrue(world.objs.findAll(lootTile).none())
        assertTrue(world.objs.findAll(second.coords).any())
    }

    @Test
    fun `ending one active fight clears only its hazards and pending callbacks`() {
        val world = World(openingEnabled = true)
        val first = world.player(1)
        val second = world.player(2)
        val a = world.enter(first)
        val b = world.enter(second)
        for (tick in 1..41) world.cycle(tick)
        val aNpcs = world.manager.npcsForInstance(a.id).toList()
        val bNpcs = world.manager.npcsForInstance(b.id).toList()
        val aCloud = requireNotNull(world.manager.resolveCoord(a, ZulrahIsland.openingSpawn)).translate(2, -5)
        val bCloud = requireNotNull(world.manager.resolveCoord(b, ZulrahIsland.openingSpawn)).translate(2, -5)
        world.manager.handleLogout(first, 42)
        world.cycle(42)
        assertTrue(aNpcs.none { it.isSlotAssigned })
        assertTrue(bNpcs.all { it.isSlotAssigned })
        assertNull(world.locRegistry.findType(aCloud, 11700))
        assertNotNull(world.locRegistry.findType(bCloud, 11700))
        for (tick in 43..47) world.cycle(tick)
        assertTrue(world.manager.npcsForInstance(a.id).isEmpty())
        assertTrue(world.manager.npcsForInstance(b.id).size > bNpcs.size)
    }

    @Test
    fun `full finite routine advances without spawning a guessed continuation`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        player.adminGodMode = true
        val session = world.enter(player)
        for (tick in 1..524) world.cycle(tick)
        val boss = world.manager.npcsForInstance(session.id).first()
        assertEquals(ZulrahEncounterController.State.EvidenceLimit, world.opening.state(session.id))
        assertFalse(world.opening.canAttack(player, boss))
        val survivors = world.manager.npcsForInstance(session.id).filter { it.isSlotAssigned }
        assertTrue(survivors.any { it !== boss && world.opening.canAttack(player, it) })
        val position = boss.coords
        for (tick in 525..1000) world.cycle(tick)
        assertEquals(listOf(boss), world.manager.npcsForInstance(session.id).filter { it.isSlotAssigned })
        assertEquals(position, boss.coords)
        assertEquals(ZulrahEncounterController.State.EvidenceLimit, world.opening.state(session.id))
    }

    @Test
    fun `snakelings begin dying at the first tick after forty seconds`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player)
        for (tick in 1..40) world.cycle(tick)
        val snake = world.manager.npcsForInstance(session.id).single { it.id == 2046 }
        for (tick in 41..106) world.cycle(tick)
        assertTrue(snake.isSlotAssigned)
        assertEquals(1, snake.hitpoints)
        world.cycle(107)
        assertEquals(2408, snake.pendingSequence.id)
        assertFalse(world.opening.canAttack(player, snake))
        world.cycle(108)
        assertTrue(snake.isSlotAssigned)
        world.cycle(109)
        assertFalse(snake.isSlotAssigned)
        assertEquals(ZulrahEncounterController.State.Fighting, world.opening.state(session.id))
    }

    @Test
    fun `protection is resolved at launch and boss death retains fired hits`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player)
        world.cycle(4)
        val boss = world.manager.npcsForInstance(session.id).single()
        fun queuedHits(): List<org.rsmod.game.hit.Hit> = buildList {
            val iterator = player.queueList.iterator() ?: return@buildList
            while (iterator.hasNext()) (iterator.next().args as? org.rsmod.game.hit.Hit)?.let(::add)
        }
        player.protectMagic = 1
        repeat(10) { world.combat.attack(boss, player, HitType.Magic, 41, 3) }
        val protectedHits = queuedHits()
        assertEquals(10, protectedHits.size)
        assertTrue(protectedHits.all { it.damage == 0 })
        player.protectMagic = 0
        repeat(10) { world.combat.attack(boss, player, HitType.Magic, 41, 3) }
        val firedHits = queuedHits()
        assertTrue(firedHits.drop(10).any { it.damage > 0 })
        player.protectMagic = 1
        assertEquals(firedHits, queuedHits())
        boss.hitpoints = 0
        world.cycle(5)
        assertEquals(firedHits, queuedHits())
    }

    @Test
    fun `minions preserve owner pursuit across arena and recover a lost interaction`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player)
        for (tick in 1..40) world.cycle(tick)
        val snake = world.manager.npcsForInstance(session.id).single { it.id == 2046 }
        assertFalse(snake.isNotDelayed)
        assertEquals(dev.openrune.types.NpcMode.None, snake.type.defaultMode)
        val cached = requireNotNull(ServerCacheManager.getNpc(snake.id))
        assertNotSame(cached, snake.type)
        assertSame(cached.paramMap, snake.type.paramMap)
        assertEquals(7, cached.maxRange)
        player.coords = snake.spawnCoords.translate(-14, 0)
        assertTrue(snake.spawnCoords.chebyshevDistance(player.coords) > cached.maxRange + cached.attackRange)
        assertTrue(snake.spawnCoords.chebyshevDistance(player.coords) <= snake.type.maxRange + snake.type.attackRange)
        snake.clearInteraction()
        snake.defaultMode()
        world.cycle(41)
        assertEquals(dev.openrune.types.NpcMode.ApPlayer2, snake.mode)
        assertSame(player, (snake.interaction as org.rsmod.game.interact.InteractionPlayer).target)
        world.cycle(43)
        snake.processedMapClock = 43
        assertTrue(snake.isNotDelayed)
    }

    @Test
    fun `recorded eggs spawn only once and do not multiply across cycles`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player)
        for (tick in 1..200) {
            world.cycle(tick)
            world.cycle(tick)
        }
        val snakes = world.manager.npcsForInstance(session.id).filter { it.id in 2045..2046 }
        assertEquals(14, snakes.size)
        assertTrue(snakes.any { !it.isSlotAssigned })
        assertTrue(snakes.any { it.isSlotAssigned })
    }

    @Test
    fun `ranged attack is emitted even when the boss footprint is walk blocked`() {
        val world = World(openingEnabled = true)
        val player = world.player(1)
        val session = world.enter(player)
        for (tick in 1..152) world.cycle(tick)
        val boss = world.manager.npcsForInstance(session.id).first()
        assertEquals(2042, boss.visType.id)
        for (x in 0 until boss.size) for (z in 0 until boss.size) {
            world.collision.add(boss.x + x, boss.z + z, boss.level,
                org.rsmod.routefinder.flag.CollisionFlag.LOC)
        }
        player.coords = boss.coords.translate(-3, 1)
        player.queueList.clear()
        for (tick in 153..155) world.cycle(tick)
        val iterator = requireNotNull(player.queueList.iterator())
        val hits = buildList {
            while (iterator.hasNext()) (iterator.next().args as? org.rsmod.game.hit.Hit)?.let(::add)
        }
        assertEquals(1, hits.size)
        assertEquals(HitType.Ranged, hits.single().type)
    }

    @Test
    fun `incoming hit cap leaves normal damage untouched`() {
        val world = World()
        for (damage in 0..50) assertEquals(damage, world.combat.capIncoming(damage))
        repeat(100) { assertTrue(world.combat.capIncoming(100) in 45..50) }
    }

    private class World(openingEnabled: Boolean = false,
        val bossSpec: org.rsmod.api.bosses.spec.BossSpec = ZulrahSpec.recorded(ZulrahRoutine.recorded)) {
        var entryTick: Int
            get() = clock.cycle
            set(value) { clock.cycle = value; lastCycle = maxOf(lastCycle, value) }
        val players = PlayerList()
        val ended = mutableListOf<org.rsmod.api.instances.InstanceId>()
        private val clock = MapClock(1)
        private var lastCycle = 1
        private val worldQueues = WorldQueueList()
        private val queueProcess = WorldQueueListProcess(worldQueues)
        val bus = EventBus()
        val collision = CollisionFlagMap()
        private val locZones = LocZoneStorage()
        private val npcList = NpcList()
        private val npcRegistry = NpcRegistry(npcList, collision, bus)
        private val npcRepo = NpcRepository(clock, npcRegistry, npcList)
        private val updates = ZoneUpdateMap()
        val objs = org.rsmod.api.repo.obj.ObjRepository(clock, org.rsmod.api.registry.obj.ObjRegistry(updates))
        val kills = mutableListOf<org.rsmod.api.death.NpcDeathKillContext>()
        private val normalLocs = LocRegistryNormal(updates, collision, locZones)
        val registry = RegionRegistry(
            RegionListSmall(), RegionListLarge(),
            normalLocs, collision, locZones,
            npcRegistry, ControllerRegistry(clock, ControllerList()), ZonePlayerActivityBitSet(),
        )
        val manager = InstanceManager(
            RegionRepository(registry), npcRepo, players, bus, InstanceAreaResolver(), clock, collision,
        )
        val locRegistry = LocRegistry(locZones, normalLocs,
            LocRegistryRegion(updates, collision, locZones, registry))
        private val styles = AttackStyles()
        private val bonuses = WornBonuses()
        val combat = ZulrahCombat(NvPMagicAccuracy(bonuses, styles),
            NvPRangedAccuracy(bonuses, styles), NvPMeleeAccuracy(bonuses, styles),
            DefaultGameRandom(1), StandardPlayerHitModifier(bus),
            org.rsmod.api.player.hit.processor.DamageOnlyPlayerHitProcessor(bus, npcList, players))
        private val injector = Guice.createInjector(object : AbstractModule() {
            override fun configure() {
                bind(GameRandom::class.java).toInstance(DefaultGameRandom(1))
                bind(GameRandom::class.java).annotatedWith(org.rsmod.api.random.CoreRandom::class.java)
                    .toInstance(DefaultGameRandom(1))
                bind(AttackStyles::class.java).toInstance(styles)
                bind(WornBonuses::class.java).toInstance(bonuses)
                bind(PlayerList::class.java).toInstance(players)
                bind(NpcList::class.java).toInstance(npcList)
                bind(EventBus::class.java).toInstance(bus)
                bind(CollisionFlagMap::class.java).toInstance(collision)
                bind(LocRegistry::class.java).toInstance(locRegistry)
            }
        })
        val deps = BossDeps(DefaultGameRandom(1), WorldRepository(updates), npcRepo, players,
            clock, worldQueues, collision, EncounterRegistry(), BossExtensionRegistry(),
            injector.getInstance(AccuracyFormulae::class.java),
            injector.getInstance(MaxHitFormulae::class.java), StandardPlayerHitModifier(bus))
        private val npcAccess = StandardNpcAccessLauncher(StandardNpcAccessContextFactory(
            DefaultGameRandom(1), StandardNpcHitModifier(bus), NpcHitProcessor { error("unused") }))
        private val npcQueues = NpcQueueProcessor(bus, npcAccess)
        private val npcModes = injector.getInstance(org.rsmod.api.game.process.npc.mode.NpcModeProcessor::class.java)
        val opening = ZulrahEncounterController(manager, npcRepo, players,
            LocRepository(clock, locRegistry, registry), WorldRepository(updates), collision,
            AiPlayerInteractions(bus, players), RayCastValidator(collision), combat, deps, objs)
        private val areas = org.rsmod.api.area.checker.AreaChecker(registry, org.rsmod.game.area.AreaIndex())
        private var teleportValidator = org.rsmod.api.player.hook.PlayerTeleportValidator(emptySet())

        fun installTeleport(questBlocked: Boolean = false, teleportBlocked: Boolean = false) {
            val config = org.rsmod.api.server.config.ServerConfig("test", 43594, 240, "test", 1,
                gameplay = org.rsmod.api.server.config.GameplayConfig(
                    org.rsmod.api.server.config.QuestRequirementsYaml(
                        mode = if (questBlocked) "respect-progress" else "assume-completed")))
            val quests = org.rsmod.content.quest.manager.QuestRequirementResolver(config)
            if (teleportBlocked) teleportValidator = org.rsmod.api.player.hook.PlayerTeleportValidator(
                setOf(object : org.rsmod.api.player.hook.PlayerTeleportValidateHook {
                    override fun validate(player: Player, type: org.rsmod.api.player.hook.TeleportType,
                        areaChecker: org.rsmod.api.area.checker.AreaChecker): String? =
                        "A teleport block has been cast on you."
                }))
            with(ZulAndraTeleportScript(quests, teleportValidator, areas, WorldRepository(updates))) {
                ScriptContext(bus, CheatCommandMap(), EngineQueueCache()).startup()
            }
            val dest = ZulrahIsland.zulAndraTeleport
            collision.allocateIfAbsent(dest.x, dest.z, dest.level)
        }

        fun readScroll(player: Player) {
            val context = org.rsmod.api.player.protect.ProtectedAccessContext(
                getRandom = { DefaultGameRandom(1) }, getEventBus = { bus },
                getNpcList = { npcList }, getPlayerList = { players }, getCollision = { collision },
                getAreaChecker = { areas }, getAlignment = { org.rsmod.api.player.dialogue.align.TextAlignment() },
                getLocInteractions = { error("unused") }, getNpcInteractions = { error("unused") },
                getPlayerInteractions = { error("unused") }, getHeldInteractions = { error("unused") },
                getWornInteractions = { error("unused") }, getMusicPlayer = { error("unused") },
                getMarketPrices = { error("unused") }, getInstantHitProcessor = { error("unused") },
                getTeleportValidator = { teleportValidator }, getHitModifier = { StandardPlayerHitModifier(bus) },
            )
            player.currentMapClock = clock.cycle
            player.processedMapClock = clock.cycle
            val obj = requireNotNull(player.inv[0])
            val type = requireNotNull(ServerCacheManager.getItem(obj.id))
            org.rsmod.api.player.protect.ProtectedAccessLauncher.withProtectedAccess(player, context) {
                assertTrue(bus.publish(this, org.rsmod.api.player.events.interact.HeldObjEvents.Op1(0, obj, type, player.inv)))
            }
        }

        fun advancePlayer(player: Player, tick: Int) {
            clock.cycle = tick
            player.currentMapClock = tick
            player.processedMapClock = tick
            player.advanceActiveCoroutine()
        }

        init {
            if (openingEnabled) {
                with(WeaponAttackStylesScript(styles)) {
                    ScriptContext(bus, CheatCommandMap(), EngineQueueCache()).startup()
                }
                with(ZulrahEncounterScript(opening, deps)) {
                    ScriptContext(bus, CheatCommandMap(), EngineQueueCache()).registerEncounter(bossSpec)
                }
                val death = org.rsmod.api.death.NpcDeath(npcRepo, players, objs,
                    emptySet(), setOf(org.rsmod.api.death.NpcDeathKillHook { context ->
                        kills += context
                        objs.add("obj.snakeboss_scale", context.dropCoords, context.dropDuration, context.hero, 100)
                    }, ZulrahKillHook(opening), org.rsmod.content.generic.killcount.KillcountNpcKillHook()))
                with(ZulrahDeathScript(opening, death)) {
                    ScriptContext(bus, CheatCommandMap(), EngineQueueCache()).startup()
                }
            } else {
                bus.subscribeKeyed(InstanceEndedEvent::class.java, instanceEventId(KEY)) {
                    ended += instanceId
                }
            }
        }

        fun cycle(tick: Int) {
            for (cycle in lastCycle + 1..tick) {
                clock.cycle = cycle
                for (player in players) {
                    player.currentMapClock = cycle
                    player.processedMapClock = cycle
                }
                for (npc in npcList.toList()) {
                    npc.currentMapClock = cycle
                    npc.processedMapClock = cycle
                }
                queueProcess.process()
                for (npc in npcList.toList()) {
                    npc.currentMapClock = cycle
                    npc.processedMapClock = cycle
                    npc.advanceActiveCoroutine()
                    if (!npc.isSlotAssigned || npc.isBusy) continue
                    if (npc.hitpoints <= 0 && "queue.death" !in npc.queueList) npc.queueDeath()
                    npcQueues.process(npc)
                    if (!npc.isSlotAssigned || npc.isBusy || npc.hitpoints <= 0) continue
                    npcModes.process(npc)
                    val target = (npc.interaction as? org.rsmod.game.interact.InteractionPlayer)?.target ?: continue
                    if (!npc.isWithinDistance(target, npc.attackRange)) continue
                    npcAccess.launch(npc) { bus.publish(this, AiPlayerEvents.Ap2(target, npc)) }
                }
                lastCycle = cycle
            }
        }

        fun lateCycle(tick: Int) {
            clock.cycle = tick + 1
            bus.publish(GameLifecycle.LateCycle)
        }

        fun move(player: Player) {
            injector.getInstance(org.rsmod.api.game.process.player.PlayerMovementProcessor::class.java).process(player)
        }

        @OptIn(org.rsmod.annotations.InternalApi::class)
        fun player(slot: Int): Player = Player().apply {
            uuid = slot.toLong()
            observerUUID = uuid
            slotId = slot
            assignUid()
            worn = org.rsmod.game.inv.Inventory.create("inv.worn")
            inv = org.rsmod.game.inv.Inventory.create("inv.inv")
            statMap.setBaseLevel("stat.hitpoints", 99)
            statMap.setCurrentLevel("stat.hitpoints", 99)
            coords = ZulrahIsland.zulAndraTeleport
            players[slot] = this
        }

        fun create(
            player: Player,
            spec: InstanceSpec = ZulrahIsland.spec(player.coords),
        ): InstanceManager.Result.Created =
            manager.create(player, KEY, spec, InstanceAccess.Private, 1)
                as InstanceManager.Result.Created

        fun enter(player: Player, continueEntry: Boolean = true): InstanceSession {
            val result = create(player)
            player.coords = result.enter
            manager.finalizeEntry(player, result.session, 1)
            if (continueEntry) opening.continueEntry(player, result.session.id, entryTick + 2)
            return result.session
        }

        fun npc(session: InstanceSession, coords: CoordGrid): Npc =
            Npc(NpcServerType(id = 1), coords).also {
                npcRepo.add(it, Int.MAX_VALUE)
                manager.attachNpc(session.id, it)
            }

        fun assertGone(session: InstanceSession) {
            assertNull(manager.sessionForId(session.id))
            assertNull(manager.sessionForOwner(session.owner))
            session.regionIds.forEach { assertNull(manager.sessionForRegion(it)) }
            assertTrue(manager.npcsForInstance(session.id).isEmpty())
        }
    }

    companion object {
        private const val KEY = "zulrah"

        @BeforeAll
        @JvmStatic
        fun loadCache() {
            assumeTrue(Files.isDirectory(Path.of(".data/cache/SERVER")), "Local cache is required")
            ServerCacheManager.init(240)
            with(org.rsmod.api.invtx.InvTransactionsScript(org.rsmod.api.inv.storage.PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }
    }
}
