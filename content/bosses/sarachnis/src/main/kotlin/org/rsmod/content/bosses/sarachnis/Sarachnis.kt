package org.rsmod.content.bosses.sarachnis

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.spawnOwnedNpc
import org.rsmod.api.bosses.spec.Area
import org.rsmod.api.bosses.spec.HitType as BossHitType
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.clearInteractionRoute
import org.rsmod.api.npc.hit.modifier.StandardNpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.feet
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.walkTo
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Bounds
import org.rsmod.plugin.scripts.ScriptContext

class Sarachnis
@Inject
constructor(
    deps: BossDeps,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val routeFactory: RouteFactory,
    private val npcHitModifier: StandardNpcHitModifier,
) : BossPluginScript(deps) {

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onLethal = { npc -> scheduleMinionDeath(npc) })
        registerExtensions()
    }

    override val spec = run {
        val rangedHit =
            hit {
                damage(Accuracy(Roll(0..MAX_HIT)))
                type(Ranged)
                onHit(healEffect(Ranged, RANGED_HEAL))
            }
        val rangedAttack =
            onEach(
                playersIn(ARENA),
                projectile(
                    spotanim = RANGED_PROJECTILE,
                    travel = "projanim.sarachnis_ranged",
                    target = CurrentTarget,
                    hit = rangedHit,
                    resolveOnImpact = true,
                ),
            )

        val webAttack =
            onEach(
                playersIn(ARENA),
                projectile(
                    spotanim = WEB_PROJECTILE,
                    travel = "projanim.sarachnis_web",
                    target = CurrentTarget,
                ),
            )

        val webBind =
            onEach(
                playersIn(ARENA),
                after(1, external(WEB_BIND_EXT)),
            )

        boss("npc.sarachnis") {
            stats(attackRate = ATTACK_RATE)

            val melee =
                ability("melee") {
                    anim(MELEE_ATTACK_SEQ)
                    soundTo(MELEE_SOUND)
                    hit {
                        damage(
                            Accuracy(
                                Roll(0..MAX_HIT),
                                meleeAttackType = MeleeAttackType.Crush,
                            )
                        )
                        type(Melee)
                        onHit(healEffect(Melee, MELEE_HEAL))
                    }
                }

            val ranged =
                ability("ranged") {
                    anim(RANGED_ATTACK_SEQ)
                    soundTo(RANGED_SOUND)
                    include(rangedAttack)
                }

            val web =
                ability("web") {
                    anim(RANGED_ATTACK_SEQ)
                    say("Hsss!")
                    soundTo(MELEE_SOUND)
                    include(webAttack)
                    include(webBind)
                    include(external(WEB_REPOSITION_EXT))
                }

            phase("combat") {
                forceEveryAttacks(WEB_ATTACK_INTERVAL, WEB_ATTACK_INTERVAL, web)
                weightedSelectorRandom {
                    +random(melee, weight = 1, requires = WithinMeleeRange)
                    +random(ranged, weight = 1, requires = !WithinMeleeRange)
                }
            }

            triggers {
                on(HpBelow(FIRST_SPAWN_HP, inclusive = true)) runs external(SPAWN_MINIONS_EXT)
                on(HpBelow(SECOND_SPAWN_HP, inclusive = true)) runs external(SPAWN_MINIONS_EXT)
            }
        }
    }

    private fun healEffect(hitType: BossHitType, amount: Int) =
        whenever(
            !TargetPraying(hitType),
            parallel(
                spotanim(HEAL_SPOTANIM, height = HEAL_SPOTANIM_HEIGHT),
                healSelf(amount),
            ),
        )

    private fun registerExtensions() {
        deps.extensionRegistry.register(WEB_BIND_EXT) { _, _, target, _ ->
            if (target.feet.isType(ARANEA_BOOTS)) {
                target.mes(ARANEA_BOOTS_MESSAGE)
            } else {
                deps.locRepo.add(
                    target.coords,
                    WEB_BIND_LOC,
                    WEB_BIND_WORLD_TICKS,
                    LocAngle.West,
                    LocShape.CentrepieceStraight,
                )
                CombatEffects.freeze(target, WEB_BIND_WORLD_TICKS)
            }
        }

        deps.extensionRegistry.register(WEB_REPOSITION_EXT) { _, npc, target, _ ->
            repositionAfterWeb(npc, target)
        }

        deps.extensionRegistry.register(SPAWN_MINIONS_EXT) { _, npc, _, _ ->
            spawnMinions(npc)
        }
    }

    private fun repositionAfterWeb(npc: Npc, target: Player) {
        val destination = chooseWebCorner(npc)
        npc.abortRoute()
        npc.clearInteraction()
        npc.resetFaceEntity()
        npc.clearFacingLock()
        npc.ignoreCombatInteractions = true
        npc.walkTo(routeFactory, destination, speed = MoveSpeed.Run) {
            if (!npc.isSlotAssigned || npc.hitpoints <= 0) return@walkTo
            npc.ignoreCombatInteractions = false
            val next = target.takeIf { it.isValidTarget() } ?: pickArenaTarget(npc)
            if (next != null) {
                npc.apPlayer2(next, aiPlayerInteractions)
            }
        }
    }

    private fun pickArenaTarget(npc: Npc): Player? {
        val candidates =
            deps.playerList.filter {
                it.isValidTarget() &&
                    it.coords.level == npc.coords.level &&
                    it.coords.chebyshevDistance(npc.spawnCoords) <= npc.type.maxRange
            }
        if (candidates.isEmpty()) return null
        val inAttackRange =
            candidates.filter { it.coords.chebyshevDistance(npc.coords) <= npc.attackRange }
        val pool = inAttackRange.ifEmpty { candidates }
        return pool[deps.random.of(pool.size)]
    }

    private fun chooseWebCorner(npc: Npc): CoordGrid {
        val current = npc.coords
        val corners = WEB_CORNERS.map { npc.spawnCoords.translate(it.first, it.second) }
        val candidates = corners.filter { it != current }
        val pool = candidates.ifEmpty { corners }
        return deps.random.pick(pool)
    }

    private fun spawnMinions(npc: Npc) {
        val southWest = npc.spawnCoords.translate(ARENA_MIN_OFFSET, ARENA_MIN_OFFSET)
        val northEast = npc.spawnCoords.translate(ARENA_MAX_OFFSET, ARENA_MAX_OFFSET)
        val players =
            deps.playerList.filter {
                it.isValidTarget() &&
                    it.coords.level == npc.coords.level &&
                    it.isWithinArea(southWest, northEast)
            }
        val occupied = HashSet<CoordGrid>()
        addFootprint(occupied, npc.coords, npc.size)
        for (player in players) {
            occupied += player.coords
            findMinionTile(npc, MELEE_SPAWN_REGION, occupied)?.let { tile ->
                deps.spawnOwnedNpc(npc, MELEE_SPAWN, tile)?.let { minion ->
                    minion.respawns = false
                    minion.opPlayer2(player, aiPlayerInteractions)
                }
            }

            findMinionTile(npc, MAGE_SPAWN_REGION, occupied)?.let { tile ->
                deps.spawnOwnedNpc(npc, MAGE_SPAWN, tile)?.let { minion ->
                    minion.respawns = false
                    minion.apPlayer2(player, aiPlayerInteractions)
                }
            }
        }
    }

    private fun findMinionTile(
        npc: Npc,
        region: SpawnRegion,
        occupied: MutableSet<CoordGrid>,
    ): CoordGrid? {
        repeat(MINION_SPAWN_ATTEMPTS) {
            val x = deps.random.of(region.minDx..region.maxDx)
            val z = deps.random.of(region.minDz..region.maxDz)
            val tile = npc.spawnCoords.translate(x, z)
            if (isSpawnable(tile, occupied)) {
                addFootprint(occupied, tile, MINION_SIZE)
                return tile
            }
        }
        for (x in region.minDx..region.maxDx) {
            for (z in region.minDz..region.maxDz) {
                val tile = npc.spawnCoords.translate(x, z)
                if (isSpawnable(tile, occupied)) {
                    addFootprint(occupied, tile, MINION_SIZE)
                    return tile
                }
            }
        }
        return null
    }

    private fun addFootprint(target: MutableSet<CoordGrid>, tile: CoordGrid, size: Int) {
        for (coord in Bounds(tile, size)) {
            target += coord
        }
    }

    private fun isSpawnable(tile: CoordGrid, occupied: Set<CoordGrid>): Boolean =
        canStand(tile, MINION_SIZE) &&
            Bounds(tile, MINION_SIZE).asSequence().none { it in occupied }

    private fun canStand(tile: CoordGrid, size: Int): Boolean =
        Bounds(tile, size).asSequence().none { deps.collision.isWalkBlocked(it) }

    private fun scheduleMinionDeath(npc: Npc) {
        // Keep ownership until the encounter is disposed so late-spawned minions are still removed.
        val minions = deps.encounter(npc).ownedNpcsSnapshot()
        for (minion in minions) {
            if (minion.hitpoints <= 0) continue
            minion.clearInteractionRoute()
            minion.ignoreCombatInteractions = true
            if (!minion.isSlotAssigned) continue
            minion.queueHit(
                MINION_DEATH_DELAY,
                HitType.Typeless,
                minion.hitpoints,
                npcHitModifier,
            )
        }
    }

    private data class SpawnRegion(
        val minDx: Int,
        val maxDx: Int,
        val minDz: Int,
        val maxDz: Int,
    )

    private companion object {
        private const val MELEE_SPAWN = "npc.sarachnis_melee_spawn"
        private const val MAGE_SPAWN = "npc.sarachnis_mage_spawn"

        private const val MELEE_ATTACK_SEQ = "seq.hosdun_spider_boss_attack_melee"
        private const val RANGED_ATTACK_SEQ = "seq.hosdun_spider_boss_attack_ranged"

        private const val RANGED_PROJECTILE = "spotanim.sarachnis_rangeproj"
        private const val WEB_PROJECTILE = "spotanim.sarachnis_web_proj"
        private const val WEB_BIND_LOC = "loc.sarachnis_web"
        private const val HEAL_SPOTANIM = "spotanim.sanguinesti_staff_heal"
        private const val HEAL_SPOTANIM_HEIGHT = 92
        private const val ARANEA_BOOTS = "obj.aranea_boots"
        private const val ARANEA_BOOTS_MESSAGE =
            "Your Aranea boots let you avoid the spider-based attack."

        private const val MELEE_SOUND = "synth.kilrag_attack"
        private const val RANGED_SOUND = "synth.kalphite_queen_spines"
        private const val WEB_BIND_TICKS = 6

        // The bind runs from the pre-tick world queue, before the map clock advances.
        private const val WEB_BIND_WORLD_TICKS = WEB_BIND_TICKS + 1

        private const val ATTACK_RATE = 4
        private const val WEB_ATTACK_INTERVAL = 4
        private const val MAX_HIT = 31
        private const val MELEE_HEAL = 5
        private const val RANGED_HEAL = 10

        private const val FIRST_SPAWN_HP = 0.66
        private const val SECOND_SPAWN_HP = 0.33

        private const val ARENA_MIN_OFFSET = -10
        private const val ARENA_MAX_OFFSET = 11

        private const val WEB_BIND_EXT = "sarachnis.web_bind"
        private const val WEB_REPOSITION_EXT = "sarachnis.web_reposition"
        private const val SPAWN_MINIONS_EXT = "sarachnis.spawn_minions"

        private const val MINION_SIZE = 2
        private const val MINION_SPAWN_ATTEMPTS = 40
        private const val MINION_DEATH_DELAY = 1

        private val WEB_CORNERS =
            listOf(
                -5 to -5,
                4 to -5,
                -5 to 4,
                4 to 4,
            )
        private val MELEE_SPAWN_REGION = SpawnRegion(minDx = -7, maxDx = 1, minDz = -4, maxDz = 6)
        private val MAGE_SPAWN_REGION = SpawnRegion(minDx = 1, maxDx = 9, minDz = -4, maxDz = 6)

        private val ARENA: Area =
            area(
                offset(spawnTile(), ARENA_MIN_OFFSET, ARENA_MIN_OFFSET),
                offset(spawnTile(), ARENA_MAX_OFFSET, ARENA_MAX_OFFSET),
            )
    }
}
