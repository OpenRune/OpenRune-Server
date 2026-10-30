package org.rsmod.content.bosses.royaltitans

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import kotlin.math.max
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.combat.commons.magic.MagicSpellChecks
import org.rsmod.api.combat.manager.PlayerAttackManager
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcHit
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The three elementals a titan summons two ticks after its summon cast. Each one hunts the player
 * that was targeted until it is killed, destroyed by the opposing wall or blast, or self-destructs.
 */
@Singleton
class RoyalTitansElementals
@Inject
constructor(
    private val deps: BossDeps,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val npcList: NpcList,
    private val playerAttackManager: PlayerAttackManager,
) {
    private val active: MutableMap<Npc, TitanElement> = IdentityHashMap()

    internal fun isElemental(npc: Npc): Boolean = active.containsKey(npc)

    internal fun elementOf(npc: Npc): TitanElement? = active[npc]

    fun register(script: ScriptContext, spawnFor: (Npc) -> CoordGrid) {
        deps.extensionRegistry.register(CAST_HANDLER) { ctx ->
            cast(ctx.npc, ctx.target, ctx.params, spawnFor(ctx.npc))
        }
        script.onEvent<NpcStateEvents.Delete> { active.remove(npc) }
        for (element in TitanElement.entries) {
            registerKillReward(script, element)
        }
    }

    fun clearAll() {
        for (npc in active.keys.toList()) {
            delete(npc)
        }
        active.clear()
    }

    /**
     * The titans' defeat wipes the remaining elementals; each one plays its death animation and
     * spotanim before it is removed a couple of ticks later.
     */
    internal fun killAll() {
        for ((npc, element) in active.entries.toList()) {
            active.remove(npc)
            if (!npc.isSlotAssigned) continue
            npc.clearInteraction()
            npc.ignoreCombatInteractions = true
            npc.hideAllOps()
            npc.anim(element.minionDeath)
            npc.spotanim(element.minionDeathSpot)
            deps.worldQueues.add(MINION_DEATH_FADE_TICKS) {
                if (npc.isSlotAssigned) {
                    deps.npcRepo.del(npc, Int.MAX_VALUE)
                }
            }
        }
    }

    internal fun destroyAt(tile: CoordGrid, element: TitanElement) {
        val doomed =
            active.entries
                .filter { (npc, activeElement) -> activeElement == element && npc.coords == tile }
                .map { it.key }
        for (npc in doomed) {
            delete(npc)
        }
    }

    internal fun destroyIn(tiles: Collection<CoordGrid>, element: TitanElement) {
        val doomed =
            active.entries
                .filter { (npc, activeElement) ->
                    activeElement == element && npc.coords in tiles
                }
                .map { it.key }
        for (npc in doomed) {
            delete(npc)
        }
    }

    private fun delete(npc: Npc): Boolean {
        active.remove(npc)
        val slot = npc.slotId
        if (slot == INVALID_SLOT || npcList[slot] !== npc) return false
        deps.npcRepo.del(npc, Int.MAX_VALUE)
        return true
    }

    private fun registerKillReward(ctx: ScriptContext, element: TitanElement) {
        val type = ServerCacheManager.getNpc(element.minion.asRSCM(RSCMType.NPC)) ?: return
        ctx.onNpcHit(type) {
            if (npc.hitpoints > 0 || !hit.isFromPlayer) return@onNpcHit
            val player = hit.resolvePlayerSource(deps.playerList) ?: return@onNpcHit
            player.actionDelay = max(player.currentMapClock, player.actionDelay - 1)
        }
    }

    /**
     * Water spells cleave fire elementals and fire spells cleave ice elementals. Every elemental of
     * the primary target's element within the 3x3 around it is hit; the cleave hits reuse the
     * primary hit's impact delay so they land on the same tick.
     */
    internal fun cleave(
        player: Player,
        primary: Npc,
        spell: ItemServerType,
        damage: Int,
        hitDelay: Int,
    ) {
        val element = active[primary] ?: return
        if (!element.isCleavedBy(spell)) return
        val centre = primary.coords
        for ((other, otherElement) in active.entries.toList()) {
            if (other === primary || otherElement != element) continue
            if (!other.isSlotAssigned || other.hitpoints <= 0) continue
            if (other.coords.level != centre.level) continue
            if (other.coords.chebyshevDistance(centre) > 1) continue
            playerAttackManager.queueMagicHit(
                source = player,
                target = other,
                spell = null,
                damage = damage,
                clientDelay = 0,
                hitDelay = hitDelay,
            )
        }
    }

    private fun cast(caster: Npc, target: Player, params: Any?, spawn: CoordGrid) {
        val parsed = params as? ElementalParams
        val element = parsed?.element ?: params as? TitanElement ?: return
        val phaseTwo = parsed?.phaseTwo ?: false
        val tiles = RoyalTitansArena.elementalSummonTiles(spawn, element, phaseTwo) { deps.random.of(it) }

        deps.worldQueues.add(SUMMON_TICKS) {
            if (!caster.isSlotAssigned || caster.hitpoints <= 0) return@add
            if (!target.isValidTarget()) return@add
            for (tile in tiles) {
                spawn(element, tile, target)
            }
        }
    }

    private fun spawn(element: TitanElement, tile: CoordGrid, target: Player) {
        if (active.keys.any { it.isSlotAssigned && it.coords == tile }) return
        if (deps.npcRepo.findAll(tile).any { it.isSlotAssigned }) return
        val npc = Npc(element.minion, tile)
        deps.npcRepo.add(npc, MINION_LIFETIME)
        active[npc] = element
        npc.anim(element.minionSpawn)
        npc.spotanim(SPAWN_SPOTS.getValue(element))
        npc.apPlayer2(target, aiPlayerInteractions)
        deps.worldQueues.add(EXPLODE_MIN_TICKS + deps.random.of(EXPLODE_TICK_SPREAD)) {
            if (active[npc] == element && npc.isSlotAssigned && npc.hitpoints > 0) {
                explode(element, npc)
            }
        }
    }

    private fun explode(element: TitanElement, npc: Npc) {
        val centre = npc.coords
        npc.anim(element.minionExplode)
        val spot = EXPLODE_SPOTS.getValue(element)
        deps.worldRepo.spotanimMap(SpotanimType(spot.asRSCM(RSCMType.SPOTANIM)), centre)
        for (player in deps.playerList) {
            if (!player.isValidTarget() || player.coords.level != centre.level) continue
            if (player.coords.chebyshevDistance(centre) > EXPLODE_RADIUS) continue
            player.queueHit(
                npc,
                1,
                HitType.Typeless,
                EXPLODE_MIN_HIT + deps.random.of(EXPLODE_MAX_HIT - EXPLODE_MIN_HIT + 1),
                deps.playerHitModifier,
            )
        }
        active.remove(npc)
        deps.worldQueues.add(EXPLODE_FADE_TICKS) { delete(npc) }
    }

    companion object {
        const val CAST_HANDLER: String = "rt.elementals.cast"

        private const val SUMMON_TICKS = 2
        private const val MINION_LIFETIME = 200
        private const val EXPLODE_MIN_TICKS = 30
        private const val EXPLODE_TICK_SPREAD = 11
        private const val EXPLODE_FADE_TICKS = 2
        private const val MINION_DEATH_FADE_TICKS = 2
        private const val EXPLODE_RADIUS = 1
        private const val EXPLODE_MIN_HIT = 8
        private const val EXPLODE_MAX_HIT = 10

        private val SPAWN_SPOTS =
            mapOf(
                TitanElement.Fire to "spotanim.fire_minion_spawn_spotanim",
                TitanElement.Ice to "spotanim.ice_minion_spawn_spotanim",
            )

        private val EXPLODE_SPOTS =
            mapOf(
                TitanElement.Fire to "spotanim.fire_minion_explode_spotanim",
                TitanElement.Ice to "spotanim.ice_minion_explode_spotanim",
            )
    }
}

/** The opposing-element spell that cleaves this elemental and always deals its max hit. */
internal fun TitanElement.isCleavedBy(spell: ItemServerType): Boolean =
    when (this) {
        TitanElement.Fire -> MagicSpellChecks.isWaterSpell(spell)
        TitanElement.Ice -> MagicSpellChecks.isFireSpell(spell)
    }
