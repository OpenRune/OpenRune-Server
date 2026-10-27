package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.game.process.player.PlayerMovementProcessor
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.hook.PlayerStepValidateHook
import org.rsmod.api.player.ironman.PlayerGamemode
import org.rsmod.api.player.stat.baseFarmingLvl
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.player.ui.ifCloseModal
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.loc.LocRegistryRegion
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.registry.player.PlayerRegistryResult
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.StepFactory
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.game.region.RegionListWorldEntity
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FarmingGuildTest {
    @Test
    fun `tier entry uses current Farming rather than base level in both directions`() {
        val f = Fixture()
        val hook = FarmingGuildStepHook(EventBus())
        val entrances = listOf(
            Triple(45, CoordGrid(1248, 3722), CoordGrid(1248, 3723)),
            Triple(65, CoordGrid(1244, 3729), CoordGrid(1243, 3729)),
            Triple(85, CoordGrid(1248, 3744), CoordGrid(1248, 3745)),
        )
        for ((required, outside, inside) in entrances) {
            f.player.statMap.setBaseLevel("stat.farming", (required - 1).toByte())
            f.player.statMap.setCurrentLevel("stat.farming", required.toByte())
            assertTrue(hook.canStep(f.player, outside, inside))
            f.player.statMap.setBaseLevel("stat.farming", required.toByte())
            f.player.statMap.setCurrentLevel("stat.farming", (required - 1).toByte())
            assertFalse(hook.canStep(f.player, outside, inside))
            assertTrue(hook.canStep(f.player, inside, outside))
        }
    }

    @Test
    fun `actual boosted main entrance walk emits only after movement commit`() {
        val f = MovementFixture()
        val outside = CoordGrid(1248, 3722)
        val inside = CoordGrid(1248, 3723)
        f.player.statMap.setBaseLevel("stat.farming", 44)
        f.player.statMap.setCurrentLevel("stat.farming", 45)
        f.events.subscribeUnbound(FarmingGuildEntryEvent::class.java) {
            assertEquals(f.player, player)
            assertEquals(inside, player.coords)
            assertEquals(1, player.pendingStepCount)
        }
        assertTrue(f.guild.canStep(f.player, outside, inside))
        assertEquals(0, f.entries)
        f.move(outside, inside)
        assertEquals(inside, f.player.coords)
        assertEquals(1, f.entries)
        f.movement.process(f.player)
        assertEquals(1, f.entries)
    }

    @Test
    fun `denied collision and later-validator rejected entrances emit nothing`() {
        val outside = CoordGrid(1248, 3722)
        val inside = CoordGrid(1248, 3723)
        val denied = MovementFixture()
        denied.player.statMap.setCurrentLevel("stat.farming", 44)
        denied.move(outside, inside)
        assertEquals(outside, denied.player.coords)
        assertEquals(0, denied.entries)
        val blocked = MovementFixture()
        blocked.collision[inside.x, inside.z, inside.level] = CollisionFlag.LOC
        blocked.move(outside, inside)
        assertEquals(outside, blocked.player.coords)
        assertEquals(0, blocked.entries)
        val rejected = MovementFixture(PlayerStepValidateHook { _, _, _ -> false })
        rejected.move(outside, inside)
        assertEquals(outside, rejected.player.coords)
        assertEquals(0, rejected.entries)
    }

    @Test
    fun `run entry is detected on either committed step and first-only rejection retains entry`() {
        val first = MovementFixture()
        first.move(CoordGrid(1248, 3722), CoordGrid(1248, 3724), MoveSpeed.Run)
        assertEquals(CoordGrid(1248, 3724), first.player.coords)
        assertEquals(2, first.player.pendingStepCount)
        assertEquals(1, first.entries)
        val second = MovementFixture()
        second.move(CoordGrid(1248, 3721), CoordGrid(1248, 3723), MoveSpeed.Run)
        assertEquals(CoordGrid(1248, 3723), second.player.coords)
        assertEquals(2, second.player.pendingStepCount)
        assertEquals(1, second.entries)
        val partial = MovementFixture(PlayerStepValidateHook { _, _, to -> to.z < 3724 })
        partial.move(CoordGrid(1248, 3722), CoordGrid(1248, 3724), MoveSpeed.Run)
        assertEquals(CoordGrid(1248, 3723), partial.player.coords)
        assertEquals(1, partial.player.pendingStepCount)
        assertEquals(1, partial.entries)
        val rejectedEntry = MovementFixture(PlayerStepValidateHook { _, _, to -> to.z < 3723 })
        rejectedEntry.move(CoordGrid(1248, 3721), CoordGrid(1248, 3723), MoveSpeed.Run)
        assertEquals(CoordGrid(1248, 3722), rejectedEntry.player.coords)
        assertEquals(1, rejectedEntry.player.pendingStepCount)
        assertEquals(0, rejectedEntry.entries)
    }

    @Test
    fun `west and north wing walks allow expired boost exit but reject reentry at both gate tiles`() {
        val entrances = listOf(
            Triple(65, CoordGrid(1244, 3729), CoordGrid(1243, 3729)),
            Triple(65, CoordGrid(1244, 3730), CoordGrid(1243, 3730)),
            Triple(85, CoordGrid(1248, 3744), CoordGrid(1248, 3745)),
            Triple(85, CoordGrid(1249, 3744), CoordGrid(1249, 3745)),
        )
        for ((required, outside, inside) in entrances) {
            val f = MovementFixture()
            val deeper = inside.translate(inside.x - outside.x, inside.z - outside.z)
            f.player.statMap.setBaseLevel("stat.farming", (required - 1).toByte())
            f.player.statMap.setCurrentLevel("stat.farming", (required - 1).toByte())
            f.move(outside, inside)
            assertEquals(outside, f.player.coords)
            assertEquals(0, f.player.pendingStepCount)

            f.player.statMap.setCurrentLevel("stat.farming", required.toByte())
            f.move(outside, inside)
            assertEquals(inside, f.player.coords)
            assertEquals(1, f.player.pendingStepCount)
            f.player.statMap.setCurrentLevel("stat.farming", (required - 1).toByte())
            f.move(inside, deeper)
            assertEquals(deeper, f.player.coords)
            f.move(deeper, inside)
            assertEquals(inside, f.player.coords)
            f.move(inside, outside)
            assertEquals(outside, f.player.coords)
            f.move(outside, inside)
            assertEquals(outside, f.player.coords)
            assertEquals(0, f.player.pendingStepCount)
            assertEquals(0, f.entries)
        }
    }

    @Test
    fun `outside internal wing and exit movement never emits guild entry`() {
        val f = MovementFixture()
        val moves = listOf(
            CoordGrid(1248, 3720) to CoordGrid(1248, 3721),
            CoordGrid(1248, 3723) to CoordGrid(1248, 3724),
            CoordGrid(1248, 3723) to CoordGrid(1248, 3722),
            CoordGrid(1244, 3729) to CoordGrid(1243, 3729),
            CoordGrid(1248, 3744) to CoordGrid(1248, 3745),
        )
        for ((from, to) in moves) {
            f.move(from, to)
            assertEquals(to, f.player.coords)
            assertEquals(0, f.entries)
        }
    }

    @Test
    fun `login placement and actual telejump across entrance never emits guild entry`() {
        val f = MovementFixture()
        f.player.coords = CoordGrid(1248, 3723)
        f.movement.process(f.player)
        assertEquals(0, f.entries)
        f.player.coords = CoordGrid(1248, 3722)
        PathingEntityCommon.telejump(f.player, f.collision, CoordGrid(1248, 3723))
        f.movement.process(f.player)
        assertEquals(CoordGrid(1248, 3723), f.player.coords)
        assertEquals(0, f.entries)
    }

    @Test
    fun `logout dispatcher relocates advanced ground and upper platforms without changing saved state`() {
        val collision = CollisionFlagMap()
        for (x in 1240..1255) {
            for (z in 3744..3751) collision[x, z, 0] = CollisionFlag.LOC
        }
        for (x in 1248..1250) {
            for (z in 3745..3747) collision[x, z, 0] = 0
        }
        collision[1249, 3746, 0] = CollisionFlag.LOC
        collision[1247, 3747, 0] = 0
        for (z in 3742..3744) collision[1248, z, 0] = 0
        collision.allocateIfAbsent(FarmingGuild.CENTRAL_ARRIVAL.x, FarmingGuild.CENTRAL_ARRIVAL.z, 0)
        FarmingGuild.initializeWing(collision)
        val f = Fixture()
        val events = EventBus()
        val registry = PlayerRegistry(PlayerList(), collision, ZonePlayerActivityBitSet(), events)
        guildLogoutEvents(collision, events, registry)
        f.player.slotId = 1
        f.player.uuid = 1L
        assertTrue(registry.add(f.player) is PlayerRegistryResult.Add.Success)
        fun place(to: CoordGrid) {
            val from = f.player.coords
            if (from != to) registry.change(f.player, ZoneKey.from(from), ZoneKey.from(to))
            f.player.coords = to
        }
        f.player.statMap.setBaseLevel("stat.farming", 85)
        f.player.statMap.setCurrentLevel("stat.farming", 64)
        val contract = mapOf(
            FarmingContracts.LEVEL to 3,
            FarmingContracts.TYPE to 32,
            FarmingContracts.COMPLETE to 1,
            FarmingContracts.COUNT to 7,
            FarmingContracts.DISCUSSED to 1,
        )
        contract.forEach { (key, value) -> f.set(key, value) }
        f.player.inv[0] = InvObj("obj.potato_seed", 11)
        f.player.worn[0] = InvObj("obj.rune_scimitar")
        f.vault[0] = InvObj("obj.potato_seed", 3)
        for (outside in listOf(CoordGrid(1247, 3746), CoordGrid(1248, 3744), CoordGrid(1300, 3746))) {
            place(outside)
            assertTrue(events.publish(SessionStateEvent.Logout(f.player)))
            assertEquals(outside, f.player.coords)
        }
        for (level in 0..1) for (inside in listOf(CoordGrid(1249, 3746, level), CoordGrid(1247, 3747, level))) {
            place(inside)
            assertTrue(events.publish(SessionStateEvent.Logout(f.player)))
            assertEquals(FarmingGuild.CENTRAL_ARRIVAL, f.player.coords)
            assertEquals(85, f.player.baseFarmingLvl)
            assertEquals(64, f.player.farmingLvl)
            contract.forEach { (key, value) -> assertEquals(value, f.player.vars[key]) }
            assertEquals(11, f.count(f.player.inv, "obj.potato_seed"))
            assertEquals(1, f.count(f.player.worn, "obj.rune_scimitar"))
            assertEquals(3, f.count(f.vault, "obj.potato_seed"))
            assertTrue(events.publish(SessionStateEvent.Logout(f.player)))
            assertEquals(FarmingGuild.CENTRAL_ARRIVAL, f.player.coords)
            contract.forEach { (key, value) -> assertEquals(value, f.player.vars[key]) }
            assertEquals(11, f.count(f.player.inv, "obj.potato_seed"))
            assertEquals(1, f.count(f.player.worn, "obj.rune_scimitar"))
            assertEquals(3, f.count(f.vault, "obj.potato_seed"))
        }
    }

    @Test
    fun `advanced wing logout then registry removal leaves no stale collision or zone entry at departure`() {
        val collision = CollisionFlagMap()
        for (x in 1240..1255) for (z in 3744..3751) collision[x, z, 0] = CollisionFlag.LOC
        for (x in 1248..1250) for (z in 3745..3747) collision[x, z, 0] = 0
        collision[1249, 3746, 0] = CollisionFlag.LOC
        collision[1247, 3747, 0] = 0
        collision.allocateIfAbsent(FarmingGuild.CENTRAL_ARRIVAL.x, FarmingGuild.CENTRAL_ARRIVAL.z, 0)
        FarmingGuild.initializeWing(collision)
        val events = EventBus()
        val registry = PlayerRegistry(PlayerList(), collision, ZonePlayerActivityBitSet(), events)
        guildLogoutEvents(collision, events, registry)
        val central = FarmingGuild.CENTRAL_ARRIVAL
        for (departure in listOf(
            CoordGrid(1249, 3746, 0), CoordGrid(1249, 3746, 1), CoordGrid(1247, 3747, 0), CoordGrid(1247, 3747, 1),
        )) {
            val f = Fixture()
            f.player.slotId = 1
            f.player.uuid = 1L
            f.player.statMap.setBaseLevel("stat.farming", 85)
            f.player.statMap.setCurrentLevel("stat.farming", 64)
            f.set(FarmingContracts.COUNT, 7)
            f.player.inv[0] = InvObj("obj.potato_seed", 11)
            f.vault[0] = InvObj("obj.potato_seed", 3)
            f.player.coords = departure
            f.player.addBlockWalkCollision(collision, departure)
            assertTrue(registry.add(f.player) is PlayerRegistryResult.Add.Success)
            registry.change(f.player, ZoneKey.NULL, ZoneKey.from(departure))
            assertTrue(collision[departure.x, departure.z, departure.level] and CollisionFlag.BLOCK_NPCS != 0)

            // Same order as production: logout event (before the account save), then registry deletion.
            assertTrue(events.publish(SessionStateEvent.Logout(f.player)))
            assertEquals(central, f.player.coords)
            assertTrue(registry.del(f.player) is PlayerRegistryResult.Delete.Success)

            assertEquals(0, collision[departure.x, departure.z, departure.level] and CollisionFlag.BLOCK_NPCS)
            assertEquals(0, collision[central.x, central.z, central.level] and CollisionFlag.BLOCK_NPCS)
            assertTrue(registry.findAll(ZoneKey.from(departure)).none())
            assertTrue(registry.findAll(ZoneKey.from(central)).none())
            assertNull(registry.playerList[1])
            assertEquals(85, f.player.baseFarmingLvl)
            assertEquals(7, f.player.vars[FarmingContracts.COUNT])
            assertEquals(11, f.count(f.player.inv, "obj.potato_seed"))
            assertEquals(3, f.count(f.vault, "obj.potato_seed"))
        }
        assertEquals(CollisionFlag.LOC, collision[1249, 3746, 0])
        assertEquals(0, collision[1247, 3747, 0])
    }

    @Test
    fun `native contract completes only matching guild harvest once and full claim stays pending`() {
        val f = Fixture()
        val crop = FarmingCrops.all.single { it.seed == "obj.dwarf_weed_seed" }
        val wrong = FarmingCrops.all.single { it.seed == "obj.guam_seed" }
        val guild = FarmingPatches.all.single { it.site == "farming_guild_herb" }
        val outside = FarmingPatches.all.first { it.kind == PatchKind.HERB && !it.site.startsWith("farming_guild_") }
        f.set(FarmingContracts.LEVEL, 3)
        f.set(FarmingContracts.TYPE, 32)
        assertFalse(FarmingContracts.complete(f.player, guild, wrong, false))
        assertFalse(FarmingContracts.complete(f.player, outside, crop, false))
        assertFalse(FarmingContracts.complete(f.player, guild, crop, true))
        assertTrue(FarmingContracts.complete(f.player, guild, crop, false))
        assertFalse(FarmingContracts.complete(f.player, guild, crop, false))
        f.fill()
        assertNull(FarmingContracts.claim(f.player, LOW_RANDOM))
        assertEquals(1, f.player.vars[FarmingContracts.COMPLETE])
        assertEquals(0, f.player.vars[FarmingContracts.COUNT])
        f.player.inv[27] = null
        assertEquals(3, FarmingContracts.claim(f.player, LOW_RANDOM))
        assertNotNull(f.player.inv[27])
        assertEquals(1, f.player.vars[FarmingContracts.COUNT])
        assertNull(FarmingContracts.claim(f.player, LOW_RANDOM))
        assertEquals(1, f.player.vars[FarmingContracts.COUNT])
    }

    @Test
    fun `failed pack award preserves pending roll and retry cannot reroll or duplicate`() {
        val f = Fixture()
        val pack = FarmingSeedPacks.create(1, LOW_RANDOM)
        f.player.inv[0] = pack
        f.fill()
        assertFalse(FarmingSeedPacks.take(f.player, 0, pack, LOW_RANDOM))
        val pendingPack = checkNotNull(f.player.inv[0])
        val pending = SeedPackState.unpack(pendingPack.vars)
        assertTrue(pending.pending > 0)
        assertEquals(6, pending.remaining)
        f.player.inv[1] = null
        assertTrue(FarmingSeedPacks.take(f.player, 0, pendingPack, NO_RANDOM))
        assertEquals(5, SeedPackState.unpack(checkNotNull(f.player.inv[0]).vars).remaining)
        assertEquals(8, f.count(f.player.inv, "obj.potato_seed"))
        assertFalse(FarmingSeedPacks.take(f.player, 0, pendingPack, NO_RANDOM))
        assertEquals(8, f.count(f.player.inv, "obj.potato_seed"))
    }

    @Test
    fun `vault preserves items and favourites when either required modal is closed`() {
        for (closed in listOf(FarmingSeedVault.MAIN, FarmingSeedVault.SIDE)) {
            val f = Fixture()
            val held = InvObj("obj.potato_seed", 5)
            val stored = InvObj("obj.potato_seed", 3)
            f.player.inv[0] = held
            f.vault[0] = stored
            f.player.ifCloseModal(closed, EventBus())

            assertEquals(0, FarmingSeedVault.deposit(f.player, 0, held, 5))
            assertEquals(0, FarmingSeedVault.withdraw(f.player, 0, stored, 3, false))
            assertFalse(FarmingSeedVault.favourite(f.player, 0))
            assertEquals(5, f.count(f.player.inv, "obj.potato_seed"))
            assertEquals(3, f.count(f.vault, "obj.potato_seed"))
            assertEquals(255, f.player.vars["varbit.seed_vault_fave1"])
        }
    }

    @Test
    fun `vault rejects stale ineligible and UIM deposits and caps real stacks without loss`() {
        val f = Fixture()
        val seed = InvObj("obj.potato_seed", 10)
        f.player.inv[0] = seed
        f.vault[0] = InvObj("obj.potato_seed", Int.MAX_VALUE - 2)
        assertEquals(2, FarmingSeedVault.deposit(f.player, 0, seed, 10))
        assertEquals(Int.MAX_VALUE, f.count(f.vault, "obj.potato_seed"))
        assertEquals(8, f.count(f.player.inv, "obj.potato_seed"))
        assertEquals(0, FarmingSeedVault.deposit(f.player, 0, seed, 10))
        val seedling = InvObj("obj.plantpot_acorn")
        f.player.inv[1] = seedling
        assertEquals(0, FarmingSeedVault.deposit(f.player, 1, seedling, 1))
        f.player.gamemode = PlayerGamemode.ULTIMATE_IRONMAN
        val current = checkNotNull(f.player.inv[0])
        assertEquals(0, FarmingSeedVault.deposit(f.player, 0, current, 1))
        assertEquals(8, f.count(f.player.inv, "obj.potato_seed"))
    }

    @Test
    fun `vault withdrawal fits partially and favourite survives placeholder cleanup and reorder`() {
        val f = Fixture()
        val oak = InvObj("obj.plantpot_oak_sapling", 3)
        f.vault[0] = oak
        assertTrue(FarmingSeedVault.favourite(f.player, 0))
        f.fill()
        assertEquals(0, FarmingSeedVault.withdraw(f.player, 0, oak, 3, false))
        f.player.inv[0] = null
        assertEquals(1, FarmingSeedVault.withdraw(f.player, 0, oak, 3, false))
        assertEquals(2, f.count(f.vault, "obj.plantpot_oak_sapling"))
        assertEquals(1, f.count(f.player.inv, "obj.plantpot_oak_sapling"))
        assertEquals(0, FarmingSeedVault.withdraw(f.player, 0, oak, 3, false))
        assertEquals(2, f.count(f.vault, "obj.plantpot_oak_sapling"))
        assertEquals(1, f.count(f.player.inv, "obj.plantpot_oak_sapling"))
        val remaining = checkNotNull(f.vault[0])
        f.player.inv[1] = null
        assertEquals(2, FarmingSeedVault.withdraw(f.player, 0, remaining, 2, true))
        assertEquals(0, f.count(f.vault, "obj.plantpot_oak_sapling"))
        val oakType = checkNotNull(ServerCacheManager.getItem("obj.plantpot_oak_sapling".asRSCM()))
        assertEquals(2, f.player.inv.countOf(oakType.certlink))
        FarmingSeedVault.removePlaceholders(f.player)
        assertNotNull(f.vault[0])
        assertEquals(0, f.player.vars["varbit.seed_vault_fave1"])
        f.vault[1] = InvObj("obj.plantpot_willow_sapling", 1)
        assertTrue(FarmingSeedVault.reorder(f.player, 0, 1))
        assertEquals(1, f.player.vars["varbit.seed_vault_fave1"])
        assertEquals(1, f.count(f.vault, "obj.plantpot_willow_sapling"))
    }

    @Test
    fun `spirit exchange rolls back when pack cannot fit and accounts for the final seed`() {
        val f = Fixture()
        val source = checkNotNull(ServerCacheManager.getItem("obj.spirit_tree_seed".asRSCM()))
        f.player.inv[0] = InvObj(source, 2)
        f.fill()
        assertFalse(FarmingSeedPacks.exchange(f.player, source, LOW_RANDOM))
        assertEquals(2, f.count(f.player.inv, source.internalName))
        assertEquals(0, f.count(f.player.inv, FarmingSeedPacks.PACK))
        f.player.inv[1] = null
        assertTrue(FarmingSeedPacks.exchange(f.player, source, LOW_RANDOM))
        assertTrue(FarmingSeedPacks.exchange(f.player, source, LOW_RANDOM))
        assertFalse(FarmingSeedPacks.exchange(f.player, source, LOW_RANDOM))
        assertEquals(0, f.count(f.player.inv, source.internalName))
        assertEquals(2, f.count(f.player.inv, FarmingSeedPacks.PACK))
    }

    @Test
    fun `vault honors locks on noted deposits and returns exact unnoted quantity`() {
        val f = Fixture()
        val oakType = checkNotNull(ServerCacheManager.getItem("obj.plantpot_oak_sapling".asRSCM()))
        val noted = InvObj(checkNotNull(ServerCacheManager.getItem(oakType.certlink)), 7)
        f.player.inv[0] = noted
        f.set(FarmingSeedVault.LOCKS, 1)
        assertEquals(0, FarmingSeedVault.deposit(f.player, 0, noted, 7))
        assertEquals(7, f.player.inv.countOf(noted.id))
        f.set(FarmingSeedVault.IGNORE_LOCKS, 1)
        assertEquals(7, FarmingSeedVault.deposit(f.player, 0, noted, 7))
        assertEquals(0, f.player.inv.countOf(noted.id))
        val slot = f.vault.indices.first { f.vault[it]?.id == oakType.id }
        val stored = checkNotNull(f.vault[slot])
        assertEquals(7, FarmingSeedVault.withdraw(f.player, slot, stored, 7, false))
        assertEquals(7, f.player.inv.countOf(oakType.id))
        assertEquals(0, f.vault.countOf(oakType.id))
    }

    private fun guildLogoutEvents(collision: CollisionFlagMap, events: EventBus, players: PlayerRegistry): EventBus {
        val clock = MapClock()
        val updates = ZoneUpdateMap()
        val zones = LocZoneStorage()
        val normal = LocRegistryNormal(updates, collision, zones)
        val regions = RegionRegistry(
            RegionListSmall(), RegionListLarge(), RegionListWorldEntity(), normal, collision, zones,
            NpcRegistry(NpcList(), collision, events), ControllerRegistry(clock, ControllerList()),
            ZonePlayerActivityBitSet(),
        )
        val locs = LocRepository(clock,
            LocRegistry(zones, normal, LocRegistryRegion(updates, collision, zones, regions)), regions)
        with(FarmingGuildGateScript(locs, collision, players)) {
            ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
        }
        return events
    }

    private class MovementFixture(laterValidator: PlayerStepValidateHook? = null) {
        val events = EventBus()
        val collision = CollisionFlagMap()
        val player = Player().apply {
            client = SilentClient()
            currentMapClock = 0
            processedMapClock = 0
            statMap.setCurrentLevel("stat.farming", 99)
        }
        val guild = FarmingGuildStepHook(events)
        private val validators = linkedSetOf<PlayerStepValidateHook>(guild).apply {
            if (laterValidator != null) add(laterValidator)
        }
        val movement = PlayerMovementProcessor(collision, RouteFactory(collision),
            StepFactory(collision), events, validators)
        var entries = 0
            private set

        init {
            for (x in 1232..1256 step 8) for (z in 3712..3752 step 8) {
                collision.allocateIfAbsent(x, z, 0)
            }
            events.subscribeUnbound(FarmingGuildEntryEvent::class.java) { entries++ }
        }

        fun move(from: CoordGrid, to: CoordGrid, speed: MoveSpeed = MoveSpeed.Walk) {
            player.coords = from
            player.pendingStepCount = 0
            player.moveSpeed = speed
            player.routeDestination.clear()
            player.routeDestination.add(to)
            movement.process(player)
        }
    }

    @OptIn(InternalApi::class)
    private class Fixture {
        val player = Player().apply {
            client = SilentClient()
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            ui.modals.backing.put("component.toplevel_osrs_stretch:mainmodal".asRSCM(), FarmingSeedVault.MAIN.asRSCM())
            ui.modals.backing.put("component.toplevel_osrs_stretch:sidemodal".asRSCM(), FarmingSeedVault.SIDE.asRSCM())
        }
        val vault = FarmingSeedVault.store(player)
        init { FarmingSeedVault.initialize(player) }
        fun set(key: String, value: Int) = VarPlayerIntMapSetter.set(player, key, value)
        fun fill() { for (slot in player.inv.indices) if (player.inv[slot] == null) player.inv[slot] = InvObj("obj.spade") }
        fun count(inv: Inventory, type: String): Int = inv.countOf(type.asRSCM())
    }

    private class SilentClient : Client<Any, Any> {
        override fun write(message: Any) {}
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private val LOW_RANDOM = object : GameRandom {
            override fun of(maxExclusive: Int): Int = 0
            override fun of(minInclusive: Int, maxInclusive: Int): Int = minInclusive
            override fun randomDouble(): Double = 0.0
        }
        private val NO_RANDOM = object : GameRandom {
            override fun of(maxExclusive: Int): Int = error("Pending pack must not reroll")
            override fun of(minInclusive: Int, maxInclusive: Int): Int = error("Pending pack must not reroll")
            override fun randomDouble(): Double = error("Pending pack must not reroll")
        }
        private val RESTORE = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic @BeforeAll fun cache() {
            val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision = Files.readAllLines(root.resolve("game.yml"))
                .first { it.startsWith("revision:") }.substringAfter(':').trim().substringBefore('.').toInt()
            ServerCacheManager.init(revision).close()
            for ((owner, name) in listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORE += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            RESTORE += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic @AfterAll fun restore() {
            RESTORE.asReversed().forEach { it() }
            RESTORE.clear()
        }
    }
}
