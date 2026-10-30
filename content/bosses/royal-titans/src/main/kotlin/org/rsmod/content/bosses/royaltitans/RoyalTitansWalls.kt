package org.rsmod.content.bosses.royaltitans

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import kotlin.math.abs
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.magic.MagicSpellChecks
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.player.events.interact.NpcTEvents
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackRegistry
import org.rsmod.api.spells.attack.SpellCastFx
import org.rsmod.api.spells.attack.playCast
import org.rsmod.api.spells.attack.playImpact
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.getOrNull
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.loc.LocLayerConstants

enum class TitanElement(
    val titan: String,
    val inactive: String,
    val displayName: String,
    val hitbox: String,
    val visual: String,
    val spawnSpot: String,
    val despawnSpot: String,
    val minion: String,
    val minionSpawn: String,
    val minionExplode: String,
    val minionDeath: String,
    val minionDeathSpot: String,
    val wallColumnOffsets: IntRange,
    val aoeCharge: String,
    val aoeRelease: String,
    val aoeWave: String,
    val phaseTwoBodyModel: Int,
) {
    Fire(
        titan = "npc.rt_fire_queen",
        inactive = "npc.rt_fire_queen_inactive",
        displayName = "Branda the Fire Queen",
        hitbox = "npc.rs_loc_attack_fire_hitbox",
        visual = "loc.vfx_branda_queen_loc01_idle01",
        spawnSpot = "spotanim.vfx_branda_queen_loc01_spawn01",
        despawnSpot = "spotanim.vfx_branda_queen_loc01_despawn01",
        minion = "npc.rt_summon_elemental_fire",
        minionSpawn = "seq.fire_minion_spawn01",
        minionExplode = "seq.fire_minion_explode01",
        minionDeath = "seq.fire_minion_death01",
        minionDeathSpot = "spotanim.fire_minion_death_spotanim",
        wallColumnOffsets = 3..7,
        aoeCharge = "seq.fire_giant_branda_queen_attack_aoe01",
        aoeRelease = "seq.fire_giant_branda_queen_attack_aoe02",
        aoeWave = "spotanim.vfx_fire_giant_branda_queen_attack_aoe_proj01",
        phaseTwoBodyModel = 55952,
    ),
    Ice(
        titan = "npc.rt_ice_king",
        inactive = "npc.rt_ice_king_inactive",
        displayName = "Eldric the Ice King",
        hitbox = "npc.rs_loc_attack_ice_hitbox",
        visual = "loc.vfx_eldric_king_loc01_idle01",
        spawnSpot = "spotanim.vfx_eldric_king_loc01_spawn01",
        despawnSpot = "spotanim.vfx_eldric_king_loc01_despawn01",
        minion = "npc.rt_summon_elemental_ice",
        minionSpawn = "seq.ice_minion_spawn01",
        minionExplode = "seq.ice_minion_explode01",
        minionDeath = "seq.ice_minion_death01",
        minionDeathSpot = "spotanim.ice_minion_death_spotanim",
        wallColumnOffsets = -5..-1,
        aoeCharge = "seq.ice_giant_edric_king_attack_aoe01",
        aoeRelease = "seq.ice_giant_edric_king_attack_aoe02",
        aoeWave = "spotanim.vfx_ice_giant_eldric_king_attack_aoe_proj01",
        phaseTwoBodyModel = 55940,
    ),
    ;

    val opposite: TitanElement
        get() = if (this == Fire) Ice else Fire

    val hitboxId: Int by lazy { hitbox.asRSCM(RSCMType.NPC) }

    val visualId: Int by lazy { visual.asRSCM(RSCMType.LOC) }

    val titanId: Int by lazy { titan.asRSCM(RSCMType.NPC) }

    val inactiveId: Int by lazy { inactive.asRSCM(RSCMType.NPC) }

    val undousedMessage: String
        get() =
            when (this) {
                Fire ->
                    "You'll need to use a water or ice spell that is strong enough to put out " +
                        "the fire."
                Ice ->
                    "You'll need to use a fire spell that is strong enough to melt the icicles."
            }

    fun isDousedBy(spell: ItemServerType): Boolean =
        if (this == Fire) {
            MagicSpellChecks.isWaterBlastOrGreater(spell) || MagicSpellChecks.isIceSpell(spell)
        } else {
            MagicSpellChecks.isFireBlastOrGreater(spell)
        }

    companion object {
        fun of(npc: Npc): TitanElement? =
            entries.firstOrNull { npc.isType(it.titan) || npc.isType(it.inactive) }
    }
}

/**
 * Fire and icicle walls. A cast telegraphs five tiles, and four ticks later each unoccupied tile
 * turns into a blocking hitbox npc with a decorative loc. A tile a player is standing on is not
 * formed: it deals up to twelve damage instead, exactly as in the real fight.
 *
 * A wall is removed either when its lifetime runs out or when its opposing spell hits one tile; the
 * spell extinguishes the target tile and the one on either side of it.
 */
@Singleton
class RoyalTitansWalls
@Inject
constructor(
    private val deps: BossDeps,
    private val elementals: RoyalTitansElementals,
    private val npcList: NpcList,
    private val spellRegistry: MagicSpellRegistry,
    private val spellAttacks: SpellAttackManager,
    private val spellAttackRegistry: SpellAttackRegistry,
) {
    private val logger = InlineLogger()
    private val walls: MutableMap<Npc, Wall> = IdentityHashMap()

    /** Rows with a wall still in its four-tick telegraph, before any hitbox npc exists. */
    private val pendingRows: MutableSet<Int> = HashSet()

    /** Invalidates telegraphs that were queued before a phase transition or reset. */
    private var castEpoch: Int = 0

    private val ProtectedAccess.autocastEnabled by boolVarBit("varbit.autocast_set")
    private val ProtectedAccess.autocastSpellId by intVarBit("varbit.autocast_spell")

    fun register(script: ScriptContext) {
        for (element in TitanElement.entries) {
            script.onApNpc1(element.hitbox) {
                val spell = autocastDousingSpell(element)
                if (spell == null) {
                    player.mes(element.undousedMessage)
                } else {
                    castAtWall(it.npc, spell)
                }
            }
        }
        val registered = HashSet<Long>()
        for (spell in spellRegistry.allSpells()) {
            for (element in TitanElement.entries) {
                if (!element.isDousedBy(spell.obj)) continue
                val id = EventBus.composeLongKey(element.hitboxId, spell.component.packed)
                if (!registered.add(id)) continue
                script.onProtectedEvent<NpcTEvents.Ap>(id) { castAtWall(it.npc, spell) }
            }
        }
        script.onEvent<NpcStateEvents.Delete> { remove(npc) }
        deps.extensionRegistry.register(CAST_HANDLER) { ctx -> cast(ctx.npc, ctx.params) }
    }

    /** Removes every wall, e.g. when the fight enters phase two or resets. */
    fun clearAll() {
        castEpoch++
        pendingRows.clear()
        for (wall in walls.keys.toList()) {
            despawn(wall)
        }
    }

    /**
     * A wall is only ever removed by the spell that hits it: the cast animation, launch spotanim
     * and projectile are played exactly as a combat cast, and the row is torn down when the
     * projectile lands. Casting at a wall is not combat, so no hit is queued.
     */
    private suspend fun ProtectedAccess.castAtWall(npc: Npc, spell: MagicSpell) {
        val wall = walls[npc] ?: return
        val fx = spellAttackRegistry[spell] as? SpellCastFx ?: return
        val attack = CombatAttack.Spell(player.righthand, spell, defensive = false)
        if (!spellAttacks.attemptCast(this, attack).isFailure()) {
            val proj = fx.playCast(this, npc, getOrNull(attack.weapon))
            val (serverDelay, clientDelay) = proj.durations
            fx.playImpact(this, npc, clientDelay)
            deps.worldQueues.add(serverDelay) { douse(npc) }
        }
    }

    /** The wall's click op douses with whatever spell the player is autocasting, if it matches. */
    private fun ProtectedAccess.autocastDousingSpell(element: TitanElement): MagicSpell? {
        if (!autocastEnabled) return null
        val spell = spellRegistry.getAutocastSpell(autocastSpellId) ?: return null
        return spell.takeIf { element.isDousedBy(it.obj) }
    }

    private fun cast(npc: Npc, params: Any?) {
        val element = params as? TitanElement ?: return
        val origin = npc.spawnCoords
        val level = origin.level
        val epoch = castEpoch
        val occupiedRows =
            walls.values.mapTo(HashSet()) { it.tile.z }.apply { addAll(pendingRows) }
        val freeRows =
            (origin.z + RoyalTitansArena.WALL_ROW_MIN..origin.z + RoyalTitansArena.WALL_ROW_MAX)
                .filter { it !in occupiedRows }
        if (freeRows.isEmpty()) {
            logger.debug { "[royal-titans] ${element.name.lowercase()} wall skipped: no free row" }
            return
        }
        val row = freeRows[deps.random.of(freeRows.size)]
        pendingRows += row
        val tiles = element.wallColumnOffsets.map { CoordGrid(origin.x + it, row, level) }
        clearRow(tiles)
        val spot = SpotanimType(element.spawnSpot.asRSCM(RSCMType.SPOTANIM))
        for (tile in tiles) {
            deps.worldRepo.spotanimMap(spot, tile)
            playCastSounds(element, tile)
        }
        deps.worldQueues.add(FORMATION_TICKS) {
            // A transition/reset wipes pending telegraphs; don't let a stale cast form a wall.
            if (epoch != castEpoch) return@add
            pendingRows -= row
            if (!npc.isSlotAssigned || npc.hitpoints <= 0) return@add
            val formed = tiles.count { form(element, npc, it) }
        }
    }

    private fun playCastSounds(element: TitanElement, tile: CoordGrid) {
        val (first, second) = if (element == TitanElement.Fire) FIRE_CAST_SOUNDS else ICE_CAST_SOUNDS
        deps.worldRepo.soundArea(tile, first, delay = 5, radius = 1)
        deps.worldRepo.soundArea(tile, second, delay = if (element == TitanElement.Fire) 120 else 122, radius = 1)
    }

    private fun form(element: TitanElement, npc: Npc, tile: CoordGrid): Boolean {
        // Two telegraphs resolved in the same tick could otherwise both add a hitbox/loc here.
        if (walls.values.any { it.tile == tile }) return false
        val occupied = deps.playerList.any { it.isValidTarget() && it.coords == tile }
        if (occupied) {
            for (player in deps.playerList) {
                if (!player.isValidTarget() || player.coords != tile) continue
                player.queueHit(
                    npc,
                    HIT_DELAY,
                    HitType.Typeless,
                    deps.random.of(FORMATION_MAX_HIT + 1),
                    deps.playerHitModifier,
                )
            }
            return false
        }
        val wall = Npc(element.hitbox, tile)
        wall.mode = NpcMode.None
        deps.npcRepo.add(wall, WALL_LIFETIME + 2)
        walls[wall] = Wall(element, tile)
        clearVisuals(tile)
        val loc =
            LocInfo(
                LocLayerConstants.of(SHAPE.id),
                tile,
                LocEntity(element.visualId, SHAPE.id, LocAngle.North.id),
            )
        val added = deps.locRepo.add(loc, Int.MAX_VALUE)
        if (!added) {
            logger.warn { "[royal-titans] wall visual rejected at (${tile.x}, ${tile.z})" }
        }
        elementals.destroyAt(tile, element.opposite)
        scheduleDespawn(wall, WALL_LIFETIME)
        return added
    }

    private fun scheduleDespawn(wall: Npc, delay: Int) {
        deps.worldQueues.add(delay) {
            val owner = walls[wall] ?: return@add
            val spot = SpotanimType(owner.element.despawnSpot.asRSCM(RSCMType.SPOTANIM))
            deps.worldRepo.spotanimMap(spot, owner.tile)
            deps.worldQueues.add(1) {
                unlessCleared(owner) { removeVisual(owner.tile, owner.element) }
                deps.worldQueues.add(1) {
                    if (walls.remove(wall) != null) {
                        deleteNpc(wall)
                    }
                }
            }
        }
    }

    private inline fun unlessCleared(wall: Wall, action: () -> Unit) {
        if (walls.values.any { it === wall }) action()
    }

    private fun clearRow(tiles: List<CoordGrid>) {
        if (tiles.isEmpty()) return
        val row = tiles.first()
        val occupied =
            walls.entries
                .filter { (_, wall) ->
                    wall.tile.level == row.level &&
                        wall.tile.z == row.z &&
                        wall.tile.x in tiles.first().x..tiles.last().x
                }
                .map { it.key }
        for (wall in occupied) {
            despawn(wall)
        }
        for (tile in tiles) {
            clearVisuals(tile)
        }
    }

    private fun douse(npc: Npc) {
        val wall = walls[npc] ?: return
        val targets =
            walls.entries
                .filter { (_, other) ->
                    other.element == wall.element &&
                        other.tile.level == wall.tile.level &&
                        other.tile.z == wall.tile.z &&
                        abs(other.tile.x - wall.tile.x) <= 1
                }
                .map { it.key }
        for (target in targets) {
            despawn(target)
        }
    }

    /** The npc is already out of the npc list when this event fires, so only the visual is left. */
    private fun remove(npc: Npc) {
        val wall = walls.remove(npc) ?: return
        removeVisual(wall.tile, wall.element)
    }

    private fun despawn(npc: Npc) {
        val wall = walls.remove(npc) ?: return
        val spot = SpotanimType(wall.element.despawnSpot.asRSCM(RSCMType.SPOTANIM))
        deps.worldRepo.spotanimMap(spot, wall.tile)
        removeVisual(wall.tile, wall.element)
        deleteNpc(npc)
    }

    private fun deleteNpc(npc: Npc): Boolean {
        val slot = npc.slotId
        if (slot == INVALID_SLOT || npcList[slot] !== npc) return false
        deps.npcRepo.del(npc, Int.MAX_VALUE)
        return true
    }

    private fun removeVisual(tile: CoordGrid, element: TitanElement) {
        val visuals = deps.locRepo.findAll(tile).filter { it.id == element.visualId }.toList()
        for (loc in visuals) {
            deps.locRepo.del(loc, Int.MAX_VALUE)
        }
    }

    private fun clearVisuals(tile: CoordGrid) {
        val visuals =
            deps.locRepo
                .findAll(tile)
                .filter { it.id == TitanElement.Fire.visualId || it.id == TitanElement.Ice.visualId }
                .toList()
        for (loc in visuals) {
            deps.locRepo.del(loc, Int.MAX_VALUE)
        }
    }

    private class Wall(val element: TitanElement, val tile: CoordGrid)

    companion object {
        const val CAST_HANDLER: String = "rt.wall.cast"

        private val SHAPE = LocShape.CentrepieceStraight
        private const val FORMATION_TICKS = 4
        private const val WALL_LIFETIME = 38
        private const val FORMATION_MAX_HIT = 12
        private const val HIT_DELAY = 1

        private val FIRE_CAST_SOUNDS = 10147 to 10148
        private val ICE_CAST_SOUNDS = 10116 to 10118
    }
}
