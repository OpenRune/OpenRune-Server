package org.rsmod.content.bosses.doom

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MoveRestrict
import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import kotlin.math.abs
import kotlin.math.max
import org.rsmod.api.bosses.runtime.Angles
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.repeatTick
import org.rsmod.api.bosses.runtime.spawnOwnedNpc
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.heal
import org.rsmod.api.npc.hit.isStyleImmuneTo
import org.rsmod.api.npc.hit.modifier.StandardNpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

internal var Player.missedOrbs by intVarBit("varbit.dom_missed_orbs")

internal enum class LarvaPrayer {
    MeleeOrNone,
    RandomSingle,
    Coloured,
}

private enum class LarvaKind(val npc: String, val iconIndex: Int?, val immuneTo: List<String>) {
    Neutral("npc.dom_demonic_energy", null, emptyList()),
    PraysMelee("npc.dom_demonic_energy", 0, listOf(DoomVarns.IMMUNE_MELEE)),
    PraysMissiles("npc.dom_demonic_energy", 1, listOf(DoomVarns.IMMUNE_RANGED)),
    PraysMagic("npc.dom_demonic_energy", 2, listOf(DoomVarns.IMMUNE_MAGIC)),
    Melee("npc.dom_demonic_energy_melee", 6, listOf(DoomVarns.IMMUNE_RANGED, DoomVarns.IMMUNE_MAGIC)),
    Mage("npc.dom_demonic_energy_mage", 7, listOf(DoomVarns.IMMUNE_MELEE, DoomVarns.IMMUNE_RANGED)),
    Range("npc.dom_demonic_energy_range", 8, listOf(DoomVarns.IMMUNE_MELEE, DoomVarns.IMMUNE_MAGIC)),
}

@Singleton
internal class DoomLarvae
@Inject
constructor(
    private val deps: BossDeps,
    private val npcHitModifier: StandardNpcHitModifier,
    private val routeFactory: RouteFactory,
) {
    private val larvae: MutableMap<Npc, Larva> = IdentityHashMap()

    private val explosion by lazy { SpotanimType(EXPLOSION_SPOTANIM.asRSCM(RSCMType.SPOTANIM)) }

    private class Larva(val boss: Npc, val player: Player)

    fun register(script: ScriptContext) {
        deps.extensionRegistry.register(SPAWN_EXT) { ext -> spawn(ext.npc, ext.target, ext.params as DoomDelve) }
        for (name in LarvaKind.entries.map { it.npc }.distinct()) {
            val type = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))!!
            script.onNpcQueue(type, "queue.death") { onDeath(this) }
            script.onModifyNpcHit(type) { modifyHit(npc, hit) }
        }
        script.onEvent<NpcStateEvents.Delete> { larvae.remove(npc) }
    }

    private fun spawn(boss: Npc, player: Player, delve: DoomDelve) {
        val tile = spawnTile(boss, player) ?: return
        val neighbours =
            ringTiles(boss, SPAWN_RING).filter { it != tile && it.chebyshevDistance(tile) == 1 }.toMutableList()
        val tiles = mutableListOf(tile)
        repeat((delve.larvaSpawnCount - 1).coerceAtMost(neighbours.size)) {
            tiles += neighbours.removeAt(deps.random.of(neighbours.size))
        }
        tiles.forEach { spawn(boss, player, kindFor(delve), it) }
    }

    fun retarget(boss: Npc) {
        val centre = centreOf(boss)
        for ((larva, state) in larvae) {
            if (state.boss !== boss || !larva.isSlotAssigned || larva.hitpoints <= 0) continue
            larva.faceSquare(centre)
            larva.walk(routeTo(larva, centre)) { deps.worldQueues.add(1) { reach(larva, centre) } }
        }
    }

    fun startShieldStream(boss: Npc, player: Player, delve: DoomDelve) {
        val rate = delve.shieldLarvaRate ?: return
        val phaseStart = deps.encounter(boss).phaseEnteredTick
        var anchor: CoordGrid? = null
        var due = SHIELD_FIRST_SPAWN_DELAY
        deps.repeatTick(
            ticks = Int.MAX_VALUE,
            onTick = { _ ->
                if (!boss.isSlotAssigned || !player.isValidTarget()) return@repeatTick false
                val encounter = deps.encounter(boss)
                if (encounter.currentPhaseName != DoomShield.PHASE || encounter.phaseEnteredTick != phaseStart) {
                    return@repeatTick false
                }
                if (--due > 0) return@repeatTick true
                due = deps.random.of(rate)
                val side = ringTiles(boss, SHIELD_SPAWN_RING).filter { it.x <= centreOf(boss).x }
                var tile = anchor?.let { pickNear(side, it) }
                if (tile == null) {
                    anchor = deps.random.pickOrNull(side.filter { centreOf(boss).chebyshevDistance(it) in SHIELD_ANCHOR_RING })
                    tile = anchor?.let { pickNear(side, it) }
                }
                tile?.let { spawn(boss, player, shieldKindFor(delve), it) }
                true
            },
        )
    }

    private fun pickNear(tiles: List<CoordGrid>, anchor: CoordGrid): CoordGrid? =
        deps.random.pickOrNull(tiles.filter { it.chebyshevDistance(anchor) <= SHIELD_SPREAD })

    private fun spawn(boss: Npc, player: Player, kind: LarvaKind, tile: CoordGrid) {
        val larva = deps.spawnOwnedNpc(boss, kind.npc, tile) ?: return
        val centre = centreOf(boss)
        larva.mode = NpcMode.None
        larva.ignoreCombatInteractions = true
        larva.moveRestrict = MoveRestrict.PassThru
        larva.defaultMoveSpeed = MoveSpeed.Crawl
        kind.immuneTo.forEach { larva.vars[it] = 1 }
        kind.iconIndex?.let { larva.setHeadIcon(HEAD_ICON_SLOT, HEAD_ICON_GRAPHIC, it) }
        larva.anim(SPAWN_SEQ)
        larva.spotanim(SPAWN_SPOTANIM)
        larva.faceSquare(centre)
        larvae[larva] = Larva(boss, player)
        deps.worldQueues.add(1) {
            if (larva !in larvae) return@add
            larva.walk(routeTo(larva, centre)) { deps.worldQueues.add(1) { reach(larva, centre) } }
        }
        deps.worldQueues.add(STRANDED_DESPAWN_TICKS) { if (larva in larvae) remove(larva) }
    }

    /** Paths around rocks; the last stretch onto the centre tile goes straight through the boss. */
    private fun routeTo(larva: Npc, centre: CoordGrid): List<CoordGrid> {
        val route = routeFactory.create(larva.avatar, centre).map { CoordGrid(it.x, it.z, it.level) }
        return if (route.lastOrNull() == centre) route else route + centre
    }

    private fun kindFor(delve: DoomDelve): LarvaKind =
        when (delve.larvaPrayer) {
            LarvaPrayer.MeleeOrNone -> deps.random.pick(LarvaKind.Neutral, LarvaKind.PraysMelee)
            LarvaPrayer.RandomSingle ->
                deps.random.pick(LarvaKind.PraysMelee, LarvaKind.PraysMissiles, LarvaKind.PraysMagic)
            LarvaPrayer.Coloured -> deps.random.pick(LarvaKind.Mage, LarvaKind.Range)
        }

    private fun shieldKindFor(delve: DoomDelve): LarvaKind =
        when (delve.larvaPrayer) {
            LarvaPrayer.Coloured -> deps.random.pick(LarvaKind.Melee, LarvaKind.Mage, LarvaKind.Range)
            else -> kindFor(delve)
        }

    /**
     * Larvae drop on the far side of the player from the boss: on the ring [SPAWN_RING] tiles from
     * the boss's centre, within [SPAWN_ARC] of the player's bearing. The arc widens when rocks leave
     * no free tile there.
     */
    private fun spawnTile(boss: Npc, player: Player): CoordGrid? {
        val centre = centreOf(boss)
        val bearing = Angles.bearing(centre, player.coords)
        val ring = ringTiles(boss, SPAWN_RING)
        for (arc in SPAWN_ARCS) {
            val inArc = ring.filter { abs(Angles.delta(Angles.bearing(centre, it), bearing)) <= arc }
            deps.random.pickOrNull(inArc)?.let { return it }
        }
        return null
    }

    /** Free floor tiles whose Chebyshev distance from the boss's centre is in [ring]. */
    private fun ringTiles(boss: Npc, ring: IntRange): List<CoordGrid> {
        val centre = centreOf(boss)
        return ring.flatMap { distance ->
            (-distance..distance).flatMap { dx ->
                (-distance..distance).mapNotNull { dz ->
                    if (max(abs(dx), abs(dz)) != distance) return@mapNotNull null
                    centre.translate(dx, dz).takeIf {
                        DoomArena.onFloor(boss.spawnCoords, it) && !deps.collision.isWalkBlocked(it)
                    }
                }
            }
        }
    }

    private fun reach(larva: Npc, centre: CoordGrid) {
        val state = larvae[larva] ?: return
        if (larva.coords != centre || centre != centreOf(state.boss) || larva.hitpoints <= 0) return
        remove(larva)
        val boss = state.boss
        val player = state.player
        if (!boss.isSlotAssigned || boss.hitpoints <= 0 || !player.isValidTarget()) return
        val stacks = player.missedOrbs
        boss.heal(REACH_HEAL + stacks, showHitsplat = true)
        player.spotanim(PLAYER_IMPACT_SPOTANIM)
        player.queueHit(boss, 1, HitType.Typeless, REACH_DAMAGE + stacks, deps.playerHitModifier)
        player.missedOrbs = stacks + 1
    }

    private fun modifyHit(larva: Npc, hit: HitBuilder) {
        val attacker = hit.playerAttacker(deps.playerList) ?: return
        val demonbaneOrEye = hit.isDemonbaneOrEyeOfAyak()
        if (demonbaneOrEye) attacker.clearAttackDelay()
        if (larva.isStyleImmuneTo(hit.type)) {
            attacker.mes(RESIST_MESSAGE)
            return
        }
        hit.damage = if (demonbaneOrEye) DEMONBANE_DAMAGE else REGULAR_DAMAGE
    }

    private suspend fun onDeath(access: StandardNpcAccess) {
        val larva = access.npc
        val state = larvae.remove(larva)
        access.noneMode()
        access.hideAllOps()
        larva.anim(DEATH_SEQ)
        val blast = (-1..1).flatMap { dx -> (-1..1).map { dz -> larva.coords.translate(dx, dz) } }
        blast.forEach { deps.worldRepo.spotanimMap(explosion, it) }
        if (state != null) explode(state, blast)
        access.delay(1)
        deps.npcRepo.del(larva, Int.MAX_VALUE)
    }

    private fun explode(state: Larva, blast: List<CoordGrid>) {
        val boss = state.boss
        if (boss.isSlotAssigned && boss.hitpoints > 0 && blast.any { underBoss(boss, it) }) {
            val shielded = deps.encounter(boss).currentPhaseName == DoomShield.PHASE
            val damage = if (shielded) SHIELD_EXPLOSION_DAMAGE else deps.random.of(BOSS_EXPLOSION_DAMAGE)
            boss.queueHit(1, HitType.Typeless, damage, npcHitModifier)
            return
        }
        val player = state.player
        if (player.isValidTarget() && player.coords in blast) {
            player.queueHit(boss, 1, HitType.Typeless, deps.random.of(PLAYER_EXPLOSION_DAMAGE), deps.playerHitModifier)
        }
    }

    private fun remove(larva: Npc) {
        larvae.remove(larva)
        if (larva.isSlotAssigned) deps.npcRepo.del(larva, Int.MAX_VALUE)
    }

    private fun centreOf(boss: Npc): CoordGrid = boss.coords.translate(boss.size / 2, boss.size / 2)

    private fun underBoss(boss: Npc, tile: CoordGrid): Boolean =
        tile.x - boss.coords.x in 0 until boss.size && tile.z - boss.coords.z in 0 until boss.size

    companion object {
        const val SPAWN_EXT = "doom.spawn_larva"
        const val SPAWN_ONE_IN = 5

        private val SPAWN_RING = 8..12
        private const val SPAWN_ARC = 171
        private val SPAWN_ARCS = listOf(SPAWN_ARC, SPAWN_ARC * 2, Angles.FULL_TURN / 2)
        private const val STRANDED_DESPAWN_TICKS = 33

        private const val SHIELD_FIRST_SPAWN_DELAY = 5
        private val SHIELD_SPAWN_RING = 6..12
        private val SHIELD_ANCHOR_RING = 8..10
        private const val SHIELD_SPREAD = 2

        private const val HEAD_ICON_SLOT = 0
        private const val HEAD_ICON_GRAPHIC = 440

        private const val DEMONBANE_DAMAGE = 2
        private const val REGULAR_DAMAGE = 1

        private const val REACH_HEAL = 10
        private const val REACH_DAMAGE = 1
        private val BOSS_EXPLOSION_DAMAGE = 5..10
        private const val SHIELD_EXPLOSION_DAMAGE = 100
        private val PLAYER_EXPLOSION_DAMAGE = 0..21

        private const val SPAWN_SEQ = "seq.npc_demonic_grub_spawn"
        private const val DEATH_SEQ = "seq.npc_demonic_grub_death"
        private const val SPAWN_SPOTANIM = "spotanim.vfx_demonic_grub_spawn"
        private const val PLAYER_IMPACT_SPOTANIM = "spotanim.vfx_demonic_grub_player_impact01"
        private const val EXPLOSION_SPOTANIM = "spotanim.vfx_dom_burrowed_explosion_aoe"
        private const val RESIST_MESSAGE = "The demonic larva seems resistant to your attack."
    }
}
