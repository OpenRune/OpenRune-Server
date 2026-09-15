package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.map.MapSingletons.collision
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hunterLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.map.collision.firstStepDestination
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TrapHunting
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val xpMods: XpModifiers,
) : PluginScript() {
    private val setTraps = HashMap<Long, MutableSet<CoordGrid>>()

    override fun ScriptContext.startup() {
        for ((index, kind) in HunterTraps.kinds.withIndex()) {
            onOpHeld1(kind.item) { lay(kind, index) }
            registerTrapLoc(kind.activeLoc) { dismantle(kind, it) }
            registerTrapLoc(kind.brokenLoc) { dismantle(kind, it) }
            for (creature in kind.creatures) {
                registerTrapLoc(creature.fullLoc) { collect(kind, creature, it) }
            }
        }
        onPlayerQueueWithArgs<TrapTask>(QUEUE_TRAP) { trapTick(it.args) }
    }

    private fun ScriptContext.registerTrapLoc(
        loc: String,
        action: suspend ProtectedAccess.(BoundLocInfo) -> Unit,
    ) {
        val type = ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)) ?: return
        val slot = (1..5).firstOrNull { !type.actions.getOpOrNull(it - 1).isNullOrBlank() } ?: return
        when (slot) {
            1 -> onOpLoc1(loc) { action(it.loc) }
            2 -> onOpLoc2(loc) { action(it.loc) }
            3 -> onOpLoc3(loc) { action(it.loc) }
            4 -> onOpLoc4(loc) { action(it.loc) }
            else -> onOpLoc5(loc) { action(it.loc) }
        }
    }

    private suspend fun ProtectedAccess.lay(kind: TrapKind, index: Int) {
        if (player.hunterLvl < kind.level) {
            mes("You need a Hunter level of ${kind.level} to use this trap.")
            return
        }

        val owner = player.uuid ?: return
        val limit = HunterTraps.maxTraps(player.hunterLvl)
        val placed = setTraps.getOrPut(owner) { mutableSetOf() }
        if (placed.size >= limit) {
            mes("You can only manage $limit trap${if (limit == 1) "" else "s"} at a time.")
            return
        }

        val tile = coords
        if (locRepo.findExact(tile, LocShape.CentrepieceStraight) != null) {
            mes("You can't set a trap here.")
            return
        }

        anim(ANIM_LAY_TRAP)
        delay(2)

        if (invDel(inv, kind.item, 1).failure) {
            resetAnim()
            return
        }

        placed += tile
        locRepo.add(
            coords = tile,
            internal = kind.activeLoc,
            duration = TRAP_LIFETIME,
            angle = LocAngle.West,
            shape = LocShape.CentrepieceStraight,
            onDespawn = { release(owner, tile) },
        )

        resetAnim()
        spam("You set a trap.")
        stepOffTrap(tile)
        weakQueue(QUEUE_TRAP, FIRST_CHECK_TICKS, TrapTask(index, tile))
    }

    private fun ProtectedAccess.trapTick(task: TrapTask) {
        val kind = HunterTraps.kinds[task.kind]
        val loc = locRepo.findExact(task.coords, LocShape.CentrepieceStraight) ?: return
        if (loc.id != kind.activeLoc.asRSCM(RSCMType.LOC)) {
            return
        }

        val target = nearbyCreature(kind, task.coords)
        if (target == null) {
            weakQueue(QUEUE_TRAP, RECHECK_TICKS, task)
            return
        }

        val (npc, creature) = target
        val bonus = 1.0 + (player.hunterLvl - creature.level) * LEVEL_BONUS
        val chance = min(MAX_CATCH_CHANCE, creature.catchChance * bonus)
        val caught = random.of(1, ROLL_RANGE) <= (chance * ROLL_RANGE).toInt()

        val into = if (caught) creature.fullLoc else kind.brokenLoc
        val type = ServerCacheManager.getObject(into.asRSCM(RSCMType.LOC)) ?: return
        locRepo.change(loc, type, TRAP_LIFETIME)

        if (caught) {
            npcRepo.hide(npc, CATCH_HIDE_TICKS)
        }
    }

    private fun ProtectedAccess.nearbyCreature(
        kind: TrapKind,
        coords: CoordGrid,
    ): Pair<Npc, HunterCreature>? {
        val byNpc = kind.creatures.associateBy(HunterCreature::npc)
        val candidates =
            npcRepo
                .findAll(ZoneKey.from(coords), zoneRadius = 1)
                .mapNotNull { npc ->
                    val creature = byNpc[npc.visType.internalName] ?: return@mapNotNull null
                    if (player.hunterLvl < creature.level) null else npc to creature
                }
                .toList()
        if (candidates.isEmpty()) {
            return null
        }
        return candidates[random.of(0, candidates.size - 1)]
    }

    private suspend fun ProtectedAccess.collect(
        kind: TrapKind,
        creature: HunterCreature,
        loc: BoundLocInfo,
    ) {
        if (inv.freeSpace() < creature.loot.size + 1) {
            mes("You don't have enough inventory space to check this trap.")
            return
        }

        anim(kind.dismantleAnim)
        delay(2)

        removeTrap(loc)
        for (entry in creature.loot) {
            invAdd(inv, entry.obj, random.of(entry.amount.first, entry.amount.last))
        }
        invAdd(inv, kind.item, 1)

        statAdvance(STAT_HUNTER, creature.xp * xpMods.get(player, STAT_HUNTER))
        resetAnim()
        spam("You've caught a ${creature.name}.")
    }

    private suspend fun ProtectedAccess.dismantle(kind: TrapKind, loc: BoundLocInfo) {
        if (inv.freeSpace() < 1 && !inv.contains(kind.item)) {
            mes("You don't have enough inventory space to pick this trap up.")
            return
        }

        anim(kind.dismantleAnim)
        delay(2)

        removeTrap(loc)
        invAdd(inv, kind.item, 1)
        resetAnim()
        spam("You dismantle the trap.")
    }

    private fun ProtectedAccess.removeTrap(loc: BoundLocInfo) {
        locRepo.del(loc, TRAP_LIFETIME)
        val owner = player.uuid ?: return
        release(owner, loc.coords)
    }

    private fun release(owner: Long, tile: CoordGrid) {
        val placed = setTraps[owner] ?: return
        placed -= tile
        if (placed.isEmpty()) {
            setTraps -= owner
        }
    }

    private fun ProtectedAccess.stepOffTrap(tile: CoordGrid) {
        val dest = collision.firstStepDestination(tile, STEP_OFF_DIRECTIONS) ?: return
        walk(dest)
    }

    private data class TrapTask(val kind: Int, val coords: CoordGrid)

    private companion object {
        // ponytail: creatures do not walk into traps - a trap looks for whatever is standing near
        // it when it ticks. Wiring the npc AI to path onto the trap is the upgrade path.
        const val TRAP_LIFETIME = 1000
        const val FIRST_CHECK_TICKS = 10
        const val RECHECK_TICKS = 6
        const val CATCH_HIDE_TICKS = 50
        const val ROLL_RANGE = 1000
        const val LEVEL_BONUS = 0.02

        val STEP_OFF_DIRECTIONS =
            listOf(Direction.West, Direction.East, Direction.South, Direction.North)
    }
}
