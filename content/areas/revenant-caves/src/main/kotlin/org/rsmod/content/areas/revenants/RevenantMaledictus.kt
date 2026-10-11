package org.rsmod.content.areas.revenants

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.*
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.npc.heal
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.game.damage.DamageContributor
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.flag.CollisionFlag

@Singleton
internal class RevenantMaledictusController @Inject constructor(private val deps: BossDeps, private val areas: AreaChecker, private val objects: ObjRepository) : NpcDeathKillHook {
    internal var active: Npc? = null
        private set
    internal var enabled = false
    private var cooldownUntil = 0
    private var spawnWeight = 0
    private var lastAttack = 0
    private val hinted = mutableSetOf<PlayerUid>()
    override fun onKill(context: NpcDeathKillContext) {
        if (!enabled || context.npc.damageContributions.isEmpty) return // Test-loot never changes the live encounter.
        if (context.npc.type.id == RevenantSpecies.MALEDICTUS.asRSCM()) {
            for (contribution in context.npc.damageContributions.entries().filterIsInstance<DamageContributor.ByPlayer>()) {
                val player = contribution.resolve(deps.playerList) ?: continue
                if (player === context.hero) continue
                val food = if (deps.random.of(2) == 0) RevenantLootTable.lookup("Blighted manta ray") else RevenantLootTable.lookup("Blighted karambwan")
                objects.add(food, player.coords, 1000, player, 2)
                objects.add(RevenantLootTable.lookup("Blighted super restore(4)"), player.coords, 1000, player, 1)
            }
            finish(); return
        }
        if (RevenantSpecies.of(context.npc) == null || !areas.inArea("area.revenant_caves", context.npc.coords) || active != null || deps.mapClock.cycle < cooldownUntil) return
        spawnWeight = minOf(SPAWN_DENOMINATOR, spawnWeight + context.npc.type.combatLevel)
        if (deps.random.of(SPAWN_DENOMINATOR) < spawnWeight) spawn(context.npc.coords)
    }
    internal fun spawn(coords: CoordGrid): Npc? {
        if (!enabled || active != null || !areas.inArea("area.revenant_caves", coords) || !deps.collision.isZoneValid(coords)) return null
        val type = checkNotNull(ServerCacheManager.getNpc(RevenantSpecies.MALEDICTUS.asRSCM()))
        val destination = (0..8).asSequence().flatMap { radius ->
            (-radius..radius).asSequence().flatMap { x -> (-radius..radius).asSequence().filter { z -> maxOf(kotlin.math.abs(x), kotlin.math.abs(z)) == radius }.map { z -> coords.translate(x, z) } }
        }.firstOrNull { tile -> fits(tile, type.size) } ?: return null
        val npc = Npc(type, destination)
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        active = npc; spawnWeight = 0; lastAttack = deps.mapClock.cycle; cooldownUntil = deps.mapClock.cycle + COOLDOWN
        npc.anim("seq.npc_revenant_superior_spawn")
        val section = when { destination.z < 10112 -> "southern"; destination.z < 10176 -> "middle"; else -> "northern" }
        for (player in deps.playerList) if (areas.inArea("area.revenant_caves", player.coords)) player.mes("A revenant maledictus has spawned in the $section caves!")
        updateHints(npc)
        return npc
    }
    internal fun attacking(npc: Npc) { if (active === npc) lastAttack = deps.mapClock.cycle }
    private fun fits(tile: CoordGrid, size: Int): Boolean {
        val mask = CollisionFlag.LOC or CollisionFlag.GROUND_DECOR or CollisionFlag.BLOCK_WALK or CollisionFlag.BLOCK_NPCS
        for (x in 0 until size) for (z in 0 until size) {
            val cell = tile.translate(x, z)
            if (!areas.inArea("area.revenant_caves", cell) || !deps.collision.isZoneValid(cell) || deps.collision.get(cell.x, cell.z, cell.level) and mask != 0) return false
        }
        return true
    }
    private fun updateHints(npc: Npc) {
        if (!enabled || active !== npc || !npc.isSlotAssigned) return
        if (npc.hitpoints == 0) { finish(); return }
        if (deps.mapClock.cycle - lastAttack >= IDLE_EXPIRY) {
            deps.npcRepo.del(npc, Int.MAX_VALUE); finish(); return
        }
        val nearby = deps.playerList.filter { !it.pendingLogout && it.hitpoints > 0 && it.coords.level == npc.coords.level && it.coords.chebyshevDistance(npc.coords) <= 16 }
        val wanted = nearby.map { it.uid }.toSet()
        for (uid in hinted - wanted) uid.resolve(deps.playerList)?.let { if (!it.pendingLogout) MiscOutput.hintArrowReset(it) }
        for (player in nearby) if (player.uid !in hinted) MiscOutput.hintArrowNpc(player, npc.slotId)
        hinted.clear(); hinted.addAll(wanted)
        deps.worldQueues.add(5) { updateHints(npc) }
    }
    internal fun finish() {
        for (uid in hinted) uid.resolve(deps.playerList)?.let { if (!it.pendingLogout) MiscOutput.hintArrowReset(it) }
        hinted.clear(); active = null
    }
    internal fun stop() { enabled = false; active?.let { if (it.isSlotAssigned) deps.npcRepo.del(it, Int.MAX_VALUE) }; finish() }
    internal companion object {
        // OSRS does not publish the flat spawn denominator. This is explicit fork tuning.
        const val SPAWN_DENOMINATOR = 1_000_000
        const val COOLDOWN = 4_500
        const val IDLE_EXPIRY = 1_000
    }
}

internal class RevenantMaledictus @Inject constructor(private val deps: BossDeps, private val controller: RevenantMaledictusController) : PluginScript() {
    private var running = false
    private val attacks = java.util.IdentityHashMap<Npc, Int>()
    private val spec = boss(RevenantSpecies.MALEDICTUS) {
        stats(attackRate = 5, aggressionRadius = 8)
        val attack = ability("attack") { include(external("revenants.maledictus")) }
        phase("combat") { weightedSelectorRandom { +random(attack, weight = 1) } }
    }
    override fun ScriptContext.startup() {
        running = true; controller.enabled = true
        deps.extensionRegistry.register("revenants.maledictus") { _, npc, player, _ -> attack(npc, player) }
        BossCombat.register(this, spec, deps, onModifyHit = { if (hit.isFromPlayer) controller.attacking(npc) })
        onEvent<NpcStateEvents.Create> { if (npc.type.id == RevenantSpecies.MALEDICTUS.asRSCM()) npc.apRangeOverride = 8 }
        onEvent<NpcStateEvents.Delete> { attacks.remove(npc); if (controller.active === npc) controller.finish() }
        onCommand("testmaledictus") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Spawn one Maledictus near you inside Revenant Caves"
            cheat { player.mes(if (controller.spawn(player.coords) != null) "Maledictus spawned." else "Enter the caves and find open space; only one Maledictus may be active.") }
        }
    }
    override fun ScriptContext.shutdown() { running = false; attacks.clear(); controller.stop(); deps.extensionRegistry.unregister("revenants.maledictus") }
    private fun attack(npc: Npc, target: Player) {
        if (!running) return
        controller.attacking(npc)
        val count = attacks.getOrDefault(npc, 0); attacks[npc] = count + 1
        val special = if (count % 4 == 3) (count / 4) % 3 + 1 else 0
        val animation = when (special) { 1 -> "stun"; 2 -> "heal"; 3 -> "dot"; else -> "melee" }
        npc.anim("seq.npc_revenant_superior_attack_$animation")
        val encounter = deps.encounter(npc)
        val targets = deps.playerList.filter { live(npc, it) && it.coords.chebyshevDistance(npc.coords) <= npc.size + 8 }
        val snapshots = (if (special == 3) listOf(target) else targets).map { it.uid to it.coords }
        for ((uid, tile) in snapshots) {
            deps.bossProjectile("spotanim.revenant_superior_standard_attack".asRSCM(), npc.coords.translate(2, 2), tile, 90, 20, 30, 60, 15, homing = if (special == 0) uid.resolve(deps.playerList) else null)
        }
        deps.worldQueues.add(3) {
            if (!running || !npc.isSlotAssigned || npc.hitpoints == 0 || !deps.encounterRegistry.isActive(encounter)) return@add
            for ((uid, tile) in snapshots) {
                if (special == 3) {
                    for (player in deps.playerList.filter { live(npc, it) && it.coords.chebyshevDistance(tile) <= 2 }) strike(npc, player, special, 25 - player.coords.chebyshevDistance(tile) * 8)
                } else {
                    val player = uid.resolve(deps.playerList) ?: continue
                    if (live(npc, player) && (special == 0 || player.coords == tile)) strike(npc, player, special, deps.random.of(0, 30))
                }
            }
        }
    }
    private fun live(npc: Npc, player: Player) = player.hitpoints > 0 && !player.pendingLogout && player.coords.level == npc.coords.level && player.coords.chebyshevDistance(npc.coords) <= 16
    private fun strike(npc: Npc, player: Player, special: Int, rawDamage: Int) {
        val type = if (special == 0) HitType.Ranged else if (special == 3) HitType.Typeless else HitType.Magic
        if (special == 1) { player.spotanim("spotanim.revenant_superior_barrage_impact"); CombatEffects.freeze(player, 5); player.actionDelay = maxOf(player.actionDelay, deps.mapClock.cycle + 5) }
        if (special == 2) player.spotanim("spotanim.revenant_superior_special_2_impact")
        val hit = player.finishNpcHit(npc, 1, type, rawDamage, deps.playerHitModifier, penetration = 50)
        if (special == 2) {
            val encounter = deps.encounter(npc)
            hit.impactEffects.add { actualDamage -> if (running && npc.isSlotAssigned && npc.hitpoints > 0 && deps.encounterRegistry.isActive(encounter)) npc.heal(actualDamage / 2, showHitsplat = true) }
        }
    }
}
