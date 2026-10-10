package org.rsmod.content.areas.revenants

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import dtx.core.*
import dtx.rs.RSDropTable
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.death.*
import org.rsmod.api.droptable.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.npc.hit.modifier.StandardNpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.protect.*
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.drops.NpcDropTableKillHook
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.*
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
@OptIn(org.rsmod.annotations.InternalApi::class)
internal class RevenantRewardsTest {
    @Test fun `all eleven species and Maledictus expose real registered tables and no placeholder loot`() {
        val fields = RevenantDropTables::class.java.declaredFields.filter { it.isAnnotationPresent(RegisterDropTable::class.java) }
        assertEquals(12, fields.size); assertEquals(198, RevenantLootTable.common.sumOf { it.weight }); assertEquals(40, RevenantLootTable.rare.sumOf { it.weight }); assertEquals(22, RevenantLootTable.rareSkulled.sumOf { it.weight })
        for (field in fields) {
            val table = field.get(null) as RSDropTable<Player, DropRollItem>; assertTrue(table.rollBeforeProgression)
            val results = table.roll(player()).flatten(); assertFalse(results is RollResult.Nothing)
            assertTrue(results.toItems().any { it.obj == RevenantCharges.ETHER })
            assertTrue(results.toItems().filterNot { it.isNothing }.all { ServerCacheManager.getItem(it.obj.asRSCM()) != null })
        }
    }

    @Test fun `unique skull denominator includes Forinthry and fivefold Slayer boost uses kill time inputs`() {
        val p = player(); val npc = npc(); val rng = random(); val table = RevenantLootTable(135, rng)
        val args = ArgMap(KillRollContext.npc with npc)
        assertFalse(table.selectResult(p, args).toItems().any { it.obj in uniqueNames })
        p.skullIcon = constants.skullicon_forinthry_surge
        table.selectResult(p, args); verify(rng).of(table.a * 44)
        setTask(p, npc)
        assertTrue(table.selectResult(p, args).toItems().any { it.obj in uniqueNames })
        ProtectedAccessLauncher.withProtectedAccess(p, ProtectedAccessContextFactory.empty()) { vars["varp.slayer_target"] = npc.type.param(params.slayer_task_id) + 1 }
        assertFalse(table.selectResult(p, args).toItems().any { it.obj in uniqueNames })
    }

    @Test fun `native kill hook snapshots last Slayer kill then grants collection loot and absorbs only matching ether`() {
        val p = player(); val npc = npc(); setTask(p, npc)
        val table = RSDropTable<Player, DropRollItem>("snapshot", mainTable = RevenantLootTable(135, random()), rollBeforeProgression = true)
        val registry = mock(DropTableRegistry::class.java); val areas = mock(AreaChecker::class.java)
        `when`(registry.forNpc(npc, areas)).thenReturn(table)
        val log = mock(CollectionLog::class.java); val objects = mock(ObjRepository::class.java)
        val hook = NpcDropTableKillHook(log, registry, areas, objects, random(), emptySet(), setOf(RevenantEtherHook()))
        p.worn[Wearpos.Hands.slot] = RevenantCharges.toggle(RevenantCharges.withCount(InvObj(RevenantCharges.EMPTY_BRACELET), 15999))
        val finish = hook.prepare(NpcDeathKillContext(p, npc, 0))
        ProtectedAccessLauncher.withProtectedAccess(p, ProtectedAccessContextFactory.empty()) { vars["varp.slayer_count"] = 0; vars["varp.slayer_target"] = 0 }
        finish()
        assertTrue(mockingDetails(log).invocations.any { it.arguments.getOrNull(1) in uniqueNames })
        verify(log).grant(p, RevenantCharges.ETHER, 2, npc.name)
        assertEquals(16000, RevenantCharges.count(p.worn[Wearpos.Hands.slot]!!))
        assertTrue(mockingDetails(objects).invocations.any { it.arguments.firstOrNull() == RevenantCharges.ETHER && it.arguments.getOrNull(4) == 1 })
    }

    @Test fun `actual hit queues consume one ether per attack including misses multihits and all Wilderness targets`() {
        for (kind in RevenantCharges.weapons) {
            val p = player(); p.slotId = 1; p.uuid = 1; p.assignUid(); val players = PlayerList(); players[1] = p
            val bus = EventBus(); val script = RevenantChargeConsumption(players); with(script) { ScriptContext(bus, CheatCommandMap(), EngineQueueCache()).startup() }
            val target = npc(); p.worn[Wearpos.RightHand.slot] = RevenantCharges.withCount(InvObj(kind.empty), 1004)
            val modifier = StandardNpcHitModifier(bus)
            repeat(4) { target.queueHit(p, 1, HitType.Magic, 0, modifier) }
            assertEquals(1003, RevenantCharges.count(p.worn[Wearpos.RightHand.slot]!!))
            p.currentMapClock++; target.queueHit(p, 1, HitType.Magic, 5, modifier)
            assertEquals(1002, RevenantCharges.count(p.worn[Wearpos.RightHand.slot]!!))
            target.coords = CoordGrid(3200, 3200); p.currentMapClock++; target.queueHit(p, 1, HitType.Magic, 0, modifier)
            val shoots = kind.charged.contains("bow_") || kind.charged.contains("webweaver_") || ((kind.charged.contains("sceptre_") || kind.charged.contains("accursed_")) && !kind.charged.endsWith("_recol"))
            assertEquals(if (shoots)1001 else 1002, RevenantCharges.count(p.worn[Wearpos.RightHand.slot]!!))
        }
    }

    @Test fun `all three weapon upgrades preserve charged item vars and dismantle into original components atomically`() {
        for (upgrade in RevenantCrafting.upgrades) {
            val p = player(); p.statMap.setBaseLevel(upgrade.skill, 85); p.statMap.setCurrentLevel(upgrade.skill, 85)
            val original = RevenantCharges.withCount(InvObj("obj.wild_cave_${upgrade.base}_uncharged"), 1042); p.inv[0] = original; p.inv[1] = InvObj(RevenantLootTable.lookup(upgrade.component))
            assertTrue(RevenantCrafting.combine(p, 0, original, upgrade)); val upgraded = p.inv[0]!!; assertEquals(1042, RevenantCharges.count(upgraded))
            assertFalse(RevenantCrafting.dismantle(p, p.inv, 0, upgraded, RevenantCharges.kind(upgraded)!!))
            p.inv[0] = RevenantCharges.withCount(upgraded, 0); val item = p.inv[0]!!
            assertTrue(RevenantCrafting.dismantle(p, p.inv, 0, item, RevenantCharges.kind(item)!!)); assertEquals(1, p.inv.physicalCount("obj.wild_cave_${upgrade.base}_uncharged")); assertEquals(1, p.inv.physicalCount(RevenantLootTable.lookup(upgrade.component)))
            assertFalse(RevenantCrafting.dismantle(p, p.inv, 0, item, RevenantCharges.kind(item)!!))
        }
    }

    @Test fun `upgraded dismantle with full inventory rolls back instead of destroying a part`() {
        val p = player(); p.inv[0] = InvObj("obj.wild_cave_webweaver_uncharged"); val item = p.inv[0]!!
        for (slot in 1..27)p.inv[slot] = InvObj("obj.dragon_dagger")
        assertFalse(RevenantCrafting.dismantle(p, p.inv, 0, item, RevenantCharges.kind(item)!!)); assertSame(item, p.inv[0])
    }
    private val uniqueNames get() = RevenantLootTable.uniques.map { RevenantLootTable.lookup(it.name) }
    private fun random(): GameRandom = mock(GameRandom::class.java) { invocation ->
        when { invocation.method.name == "of" && invocation.arguments.size == 1 && invocation.arguments[0] is Int -> (invocation.arguments[0] as Int).let { if (it > 1000)10 else it - 1 }
            invocation.method.name == "of" && invocation.arguments.size == 2 -> invocation.arguments[0]
            else -> org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation) }
    }
    private fun setTask(p: Player, npc: Npc) {
        val master = SlayerTaskManager.tasks.keys.single { SlayerTaskManager.isWildernessMaster(it) }
        ProtectedAccessLauncher.withProtectedAccess(p, ProtectedAccessContextFactory.empty()) { vars["varbit.slayer_master"] = master.masterId; vars["varp.slayer_target"] = npc.type.param(params.slayer_task_id); vars["varp.slayer_count"] = 1 }
    }
    private fun npc() = Npc(ServerCacheManager.getNpc(RevenantSpecies.Dragon.npcType.asRSCM())!!, CoordGrid(3190, 10072))
    private fun player() = Player().apply { currentMapClock = 100; processedMapClock = 100; inv = Inventory(ServerCacheManager.getInventory("inv.inv".asRSCM())!!, arrayOfNulls(28)); worn = Inventory(ServerCacheManager.getInventory("inv.worn".asRSCM())!!, arrayOfNulls(14)) }
    private fun RollResult<DropRollItem>.toItems(): List<DropRollItem> = when (this) { is RollResult.Single -> listOf(result); is RollResult.ListOf -> results; else -> emptyList() }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close(); with(InvTransactionsScript(mock(PlayerItemStorage::class.java))) { ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup() } } }
}
