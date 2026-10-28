package org.rsmod.content.bosses.royaltitans

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import java.util.IdentityHashMap
import org.rsmod.api.bosses.dsl.AbilityBuilder
import org.rsmod.api.bosses.dsl.AbilityRef
import org.rsmod.api.bosses.dsl.BossSpecBuilder
import org.rsmod.api.bosses.dsl.Melee
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.player.clearInteractionRoute
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RoyalTitans
@Inject
constructor(
    private val deps: BossDeps,
    private val walls: RoyalTitansWalls,
    private val elementals: RoyalTitansElementals,
    private val encounter: RoyalTitansEncounter,
) : PluginScript() {

    override fun ScriptContext.startup() {
        walls.register(this)
        elementals.register(this) { npc -> encounter.spawnFor(npc) }
        deps.extensionRegistry.register(STOMP_HANDLER) { ctx ->
            val params = ctx.params as? StompParams ?: return@register
            stompWave(deps, ctx.npc, params, encounter.spawnFor(ctx.npc))
            encounter.faceEachOther(ctx.npc)
        }
        deps.extensionRegistry.register(PHASE_TWO_HANDLER) { ctx -> encounter.phaseTwo(ctx.npc) }
        deps.extensionRegistry.register(CHARGE_HANDLER) { ctx ->
            encounter.charge(ctx.npc, ctx.params as? TitanElement ?: TitanElement.Fire)
        }
        deps.extensionRegistry.register(RELEASE_HANDLER) { ctx ->
            encounter.release(ctx.npc, ctx.params as? TitanElement ?: TitanElement.Fire)
        }
        onEvent<NpcStateEvents.Delete> { StompZones.remove(npc) }
        registerContribution()
        registerDeathQueue()
        registerLootOps()
        BossCombat.register(this, fireQueenSpec, deps)
        BossCombat.register(this, iceKingSpec, deps)
    }

    private fun ScriptContext.registerContribution() {
        for (element in TitanElement.entries) {
            val type = ServerCacheManager.getNpc(element.titanId) ?: continue
            onNpcHit(type) {
                if (!hit.isFromPlayer) return@onNpcHit
                val player = hit.resolvePlayerSource(deps.playerList) ?: return@onNpcHit
                encounter.recordDamage(player, npc, hit.damage)
            }
        }
    }

    private fun ScriptContext.registerDeathQueue() {
        for (element in TitanElement.entries) {
            val type = ServerCacheManager.getNpc(element.titanId) ?: continue
            onNpcQueue(type, "queue.death") {
                val deadType = ServerCacheManager.getNpc(element.inactiveId) ?: return@onNpcQueue
                noneMode()
                changeType(deadType, Int.MAX_VALUE)
                npc.abortRoute()
                npc.clearInteraction()
                npc.ignoreCombatInteractions = true
                npc.movementLocked = true
                npc.apRangeOverride = null
                // Keep the corpse a valid interaction target: the generic npc interaction
                // validator cancels any op when hitpoints <= 0, which also cancelled loot.
                npc.baseHitpointsLvl = 1
                npc.hitpoints = 1
                anim(element.inactiveSeq)
                encounter.onTitanDeath(npc)
            }
        }
    }

    private fun ScriptContext.registerLootOps() {
        for (element in TitanElement.entries) {
            for (type in listOf(element.titan, element.inactive)) {
                onOpNpc1(type) {
                    LOGGER.debug { "[royal-titans] loot op1 on ${it.npc.type.internalName}" }
                    lootTitan(encounter, it.npc, RoyalTitansLootMode.Loot)
                }
                onOpNpc3(type) {
                    LOGGER.debug { "[royal-titans] loot op3 on ${it.npc.type.internalName}" }
                    lootTitan(encounter, it.npc, RoyalTitansLootMode.Sacrifice)
                }
                onOpNpc4(type) {
                    LOGGER.debug { "[royal-titans] loot op4 on ${it.npc.type.internalName}" }
                    lootTitan(encounter, it.npc, RoyalTitansLootMode.Pages)
                }
            }
        }
    }

    private fun atFront(element: TitanElement): Condition =
        Condition.Custom { npc, _ ->
            val spawnX = npc.spawnCoords.x
            if (element == TitanElement.Fire) npc.coords.x >= spawnX else npc.coords.x <= spawnX
        }

    private fun backedOff(element: TitanElement): Condition = Condition.Not(atFront(element))

    private fun survivor(): Condition = Condition.Custom { npc, _ -> encounter.isSurvivor(npc) }

    private fun AbilityBuilder.stompAttack(
        element: TitanElement,
        phaseTwo: Boolean,
        survivor: Boolean,
    ) {
        include(external(STOMP_HANDLER, StompParams(element, phaseTwo)))
        wait(STOMP_HIT_DELAY)
        tileAoE(
            tiles = { npc, _ -> StompZones.all(npc) },
            damage = DamageExpr.Roll(0..if (survivor) SURVIVOR_MAX_HIT else STOMP_MAX_HIT),
            type = Melee,
            penetration = if (survivor) 100 else 0,
        )
    }

    private fun BossSpecBuilder.stompAbilities(element: TitanElement): Triple<AbilityRef, AbilityRef, AbilityRef> {
        val stomp =
            ability("stomp") {
                anim(element.meleeSeq)
                stompAttack(element, phaseTwo = false, survivor = false)
            }
        val stompTwo =
            ability("stomp_two") {
                anim(element.meleeSeq)
                stompAttack(element, phaseTwo = true, survivor = false)
            }
        val survivorStomp =
            ability("survivor_stomp") {
                anim(element.meleeSeq)
                stompAttack(element, phaseTwo = true, survivor = true)
            }
        return Triple(stomp, stompTwo, survivorStomp)
    }

    private fun BossSpecBuilder.wallAbility(element: TitanElement): AbilityRef =
        ability("wall") {
            anim(element.summonWallSeq)
            if (element == TitanElement.Fire) spotanim(WALL_SPOT)
            include(external(RoyalTitansWalls.CAST_HANDLER, element))
        }

    private fun BossSpecBuilder.elementalAbility(
        element: TitanElement,
        phaseTwo: Boolean,
    ): AbilityRef =
        ability("elementals") {
            anim(element.summonElementalsSeq)
            if (element == TitanElement.Fire) spotanim(ELEMENTALS_SPOT)
            include(external(RoyalTitansElementals.CAST_HANDLER, ElementalParams(element, phaseTwo)))
        }

    private fun BossSpecBuilder.pulseAbility(element: TitanElement): AbilityRef =
        ability(PULSE_ABILITY) {
            anim(element.aoeCharge)
            if (element == TitanElement.Fire) spotanim(AOE_CHARGE_SPOT)
            include(external(CHARGE_HANDLER, element))
            wait(CHARGE_TICKS)
            anim(element.aoeRelease)
            if (element == TitanElement.Fire) spotanim(AOE_RELEASE_SPOT)
            include(external(RELEASE_HANDLER, element))
        }

    private fun fireSpec(): BossSpec {
        val element = TitanElement.Fire
        return boss("npc.rt_fire_queen") {
            stats(attackRate = ATTACK_RATE)
            val (stomp, stompTwo, survivorStomp) = stompAbilities(element)
            val wall = wallAbility(element)
            val elementalCast = elementalAbility(element, phaseTwo = false)
            val elementalTwo = elementalAbility(element, phaseTwo = true)
            val pulse = pulseAbility(element)
            phase(PHASE_ONE) {
                weightedSelectorRandom {
                    +random(stomp, weight = STOMP_WEIGHT, requires = atFront(element))
                    +random(
                        wall,
                        weight = WALL_WEIGHT,
                        cooldown = WALL_COOLDOWN,
                        requires = backedOff(element),
                    )
                    +random(
                        elementalCast,
                        weight = ELEMENTALS_WEIGHT,
                        cooldown = ELEMENTALS_COOLDOWN,
                        requires = backedOff(element),
                    )
                }
            }
            phase(PHASE_TWO, entryHp = PHASE_TWO_HP, lockMovement = true) {
                entry = RoyalTitansEncounter.PHASE_TWO_ENTRY_ABILITY
                weightedSelectorRandom {
                    +random(stompTwo, weight = STOMP_WEIGHT, requires = Condition.Not(survivor()))
                    +random(survivorStomp, weight = STOMP_WEIGHT, requires = survivor())
                    +random(
                        elementalTwo,
                        weight = ELEMENTALS_WEIGHT,
                        cooldown = ELEMENTALS_COOLDOWN,
                    )
                }
            }
            ability(RoyalTitansEncounter.PHASE_TWO_ENTRY_ABILITY) { include(external(PHASE_TWO_HANDLER)) }
            pulse
        }
    }

    private fun iceSpec(): BossSpec {
        val element = TitanElement.Ice
        return boss("npc.rt_ice_king") {
            stats(attackRate = ATTACK_RATE)
            val (stomp, stompTwo, survivorStomp) = stompAbilities(element)
            val wall = wallAbility(element)
            val elementalCast = elementalAbility(element, phaseTwo = false)
            val elementalTwo = elementalAbility(element, phaseTwo = true)
            val pulse = pulseAbility(element)
            phase(PHASE_ONE) {
                weightedSelectorRandom {
                    +random(stomp, weight = STOMP_WEIGHT, requires = atFront(element))
                    +random(
                        wall,
                        weight = WALL_WEIGHT,
                        cooldown = WALL_COOLDOWN,
                        requires = backedOff(element),
                    )
                    +random(
                        elementalCast,
                        weight = ELEMENTALS_WEIGHT,
                        cooldown = ELEMENTALS_COOLDOWN,
                        requires = backedOff(element),
                    )
                }
            }
            phase(PHASE_TWO, entryHp = PHASE_TWO_HP, lockMovement = true) {
                entry = RoyalTitansEncounter.PHASE_TWO_ENTRY_ABILITY
                weightedSelectorRandom {
                    +random(stompTwo, weight = STOMP_WEIGHT, requires = Condition.Not(survivor()))
                    +random(survivorStomp, weight = STOMP_WEIGHT, requires = survivor())
                    +random(
                        elementalTwo,
                        weight = ELEMENTALS_WEIGHT,
                        cooldown = ELEMENTALS_COOLDOWN,
                    )
                }
            }
            ability(RoyalTitansEncounter.PHASE_TWO_ENTRY_ABILITY) { include(external(PHASE_TWO_HANDLER)) }
            pulse
        }
    }

    private val fireQueenSpec: BossSpec by lazy { fireSpec() }

    private val iceKingSpec: BossSpec by lazy { iceSpec() }

    private companion object {
        private const val ATTACK_RATE = 7
        private const val PHASE_ONE = "combat"
        private const val PHASE_TWO = "phase_two"
        private const val PHASE_TWO_HP = 0.35
        private const val PULSE_ABILITY = "pulse"
        private const val CHARGE_TICKS = 6

        private const val STOMP_MAX_HIT = 26
        private const val SURVIVOR_MAX_HIT = 10
        private const val STOMP_WEIGHT = 6
        private const val WALL_WEIGHT = 3
        private const val WALL_COOLDOWN = 14
        private const val ELEMENTALS_WEIGHT = 2
        private const val ELEMENTALS_COOLDOWN = 30

        private const val STOMP_HIT_DELAY = 1
        private const val WALL_SPOT = "spotanim.vfx_fire_giant_branda_queen_attack_summon01"
        private const val ELEMENTALS_SPOT = "spotanim.vfx_fire_giant_branda_queen_attack_summon02"
        private const val AOE_CHARGE_SPOT = "spotanim.vfx_fire_giant_branda_queen_attack_aoe01"
        private const val AOE_RELEASE_SPOT = "spotanim.vfx_fire_giant_branda_queen_attack_aoe02"

        private const val STOMP_HANDLER = "rt.stomp"
        private const val PHASE_TWO_HANDLER = "rt.phase_two"
        private const val CHARGE_HANDLER = "rt.pulse.charge"
        private const val RELEASE_HANDLER = "rt.pulse.release"
    }
}

internal data class StompParams(val element: TitanElement, val phaseTwo: Boolean)

internal data class ElementalParams(val element: TitanElement, val phaseTwo: Boolean)

private suspend fun ProtectedAccess.lootTitan(
    encounter: RoyalTitansEncounter,
    npc: Npc,
    mode: RoyalTitansLootMode,
) {
    val run = encounter.runOf(npc) ?: return
    if (!run.queenDead || !run.kingDead) return
    val element = TitanElement.of(npc) ?: return
    if (!encounter.canLoot(player, npc)) {
        // Already looted (or otherwise ineligible): only the refusal message plays.
        encounter.onLoot(player, npc, mode)
        return
    }
    // A pending combat sequence (weapon attack or spell) has priority over the pick-up
    // animation, which is why looting straight out of combat could silently skip it. OSRS plays
    // picking_low on the loot tick regardless, so clear the interaction and force the sequence
    // through at a priority no combat animation can outrank.
    player.clearInteractionRoute()
    player.faceNpc(npc)
    player.anim("seq.picking_low", priority = PICKUP_ANIM_PRIORITY)
    delay(2)
    encounter.onLoot(player, npc, mode)
    soundSynth(if (element == TitanElement.Fire) FIRE_LOOT_SOUND else ICE_LOOT_SOUND)
}

private const val FIRE_LOOT_SOUND = 10102
private const val ICE_LOOT_SOUND = 10108
private const val PICKUP_ANIM_PRIORITY = 100

private val TitanElement.meleeSeq: String
    get() =
        if (this == TitanElement.Fire) {
            "seq.fire_giant_branda_queen_attack_melee01"
        } else {
            "seq.ice_giant_eldric_king_attack_melee01"
        }

private val TitanElement.summonWallSeq: String
    get() =
        if (this == TitanElement.Fire) {
            "seq.fire_giant_branda_queen_attack_summon01"
        } else {
            "seq.ice_giant_eldric_king_attack_summon01"
        }

private val TitanElement.summonElementalsSeq: String
    get() =
        if (this == TitanElement.Fire) {
            "seq.fire_giant_branda_queen_attack_summon02"
        } else {
            "seq.ice_giant_eldric_king_attack_summon02"
        }

private val TitanElement.inactiveSeq: String
    get() =
        if (this == TitanElement.Fire) {
            "seq.fire_giant_branda_queen_inactive01"
        } else {
            "seq.ice_giant_eldric_king_inactive01"
        }

private object StompZones {
    private val zones = IdentityHashMap<Npc, List<List<CoordGrid>>>()

    fun put(npc: Npc, rows: List<List<CoordGrid>>) {
        zones[npc] = rows
    }

    fun all(npc: Npc): Collection<CoordGrid> = zones[npc]?.flatten().orEmpty()

    fun remove(npc: Npc) {
        zones.remove(npc)
    }
}

private fun stompWave(deps: BossDeps, npc: Npc, params: StompParams, spawn: CoordGrid) {
    val element = params.element
    val rows =
        if (params.phaseTwo) {
            RoyalTitansArena.phaseTwoStompRows(spawn, element)
        } else {
            stompRows(npc, element)
        }
    StompZones.put(npc, rows)
    val spot = SpotanimType(STOMP_SPOT.asRSCM(RSCMType.SPOTANIM))
    rows.forEachIndexed { depth, tiles ->
        for (tile in tiles) {
            deps.worldRepo.spotanimMap(spot, tile, delay = depth * STOMP_ROW_TICKS)
        }
    }
    LOGGER.debug {
        "[royal-titans] stomp ${npc.type.internalName} two=${params.phaseTwo} " +
            "at=(${npc.coords.x}, ${npc.coords.z}) rows=${rows.size}"
    }
}

/**
 * The five-deep, three-wide strip in front of the titan, grouped by depth where row 0 touches the
 * titan. The titans stand on either side of the arena and face across it, so the strip always runs
 * from the titan towards the middle of the arena rather than towards the player: the queen (on the
 * west side) slams east, the king (on the east side) slams west. The ground effect runs outwards
 * two ticks per row; the damage lands on the whole strip on the tick after the swipe.
 */
private fun stompRows(npc: Npc, element: TitanElement): List<List<CoordGrid>> {
    val level = npc.coords.level
    val size = npc.size
    val eastward = element == TitanElement.Fire
    val near = if (eastward) npc.coords.x + size else npc.coords.x - 1
    val step = if (eastward) 1 else -1
    val startZ = npc.coords.z + (size - STOMP_WIDTH) / 2
    return List(STOMP_DEPTH) { depth ->
        val x = near + depth * step
        List(STOMP_WIDTH) { dz -> CoordGrid(x, startZ + dz, level) }
    }
}

private const val STOMP_DEPTH = 5
private const val STOMP_WIDTH = 3
private const val STOMP_ROW_TICKS = 2
private const val STOMP_SPOT = "spotanim.vfx_ice_giant_eldric_king_melee01_proj01"

private val LOGGER = InlineLogger()
