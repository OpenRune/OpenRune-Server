package org.rsmod.content.bosses.archaeologists

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

internal data class BookRainConfig(val directMaxHit: Int, val splashMaxHit: Int)

internal object BookRain {
    const val EXTENSION = "archaeologists.book_rain"

    private const val LAND_TICKS = 4
    private const val SPLIT_TICKS = 4
    private const val PRIMARY_BOOKS = 3
    private const val SECONDARY_BOOKS = 2
    private const val NORMAL_RADIUS = 2
    private const val RARE_RADIUS = 4
    private const val RARE_RADIUS_ONE_IN = 10

    private const val EXPLOSION_HEIGHT = 75
    private const val EXPLOSION_DELAY = 2
    private const val EXPLOSION_SOUND_RADIUS = 5
    private const val PROJECTILE_STAGGER = 2
    private const val THROW_SOUND_RADIUS = 10

    private const val PRIMARY_START_HEIGHT = 40
    private const val PRIMARY_END_HEIGHT = 2
    private const val PRIMARY_START_DELAY = 41
    private const val PRIMARY_TRAVEL_TIME = 79
    private const val SECONDARY_START_HEIGHT = 2
    private const val SECONDARY_END_HEIGHT = 2
    private const val SECONDARY_START_DELAY = 0
    private const val SECONDARY_TRAVEL_TIME = 120
    private const val BOOK_CURVE = 40
    private const val BOOK_PROGRESS = 11

    private const val BOOK_PROJECTILE = "spotanim.crazy_archaeologist_book_special"
    private const val EXPLOSION_SPOTANIM = "spotanim.firewave_impact"
    private const val THROW_SYNTH = "synth.archaeologist_book_throw"
    private const val IMPACT_SYNTH = "synth.archaeologist_book_impact"

    fun launch(deps: BossDeps, npc: Npc, target: Player, params: Any?) {
        val config = params as? BookRainConfig ?: return
        if (!target.isValidTarget()) return

        val centre = target.coords
        val primary =
            listOf(centre) +
                chooseBookTiles(
                    centre = centre,
                    count = PRIMARY_BOOKS - 1,
                    radius = rollSpreadRadius(deps),
                    exclude = emptySet(),
                    random = deps.random,
                )
        throwBooks(
            deps = deps,
            source = npc.coords,
            tiles = primary,
            startHeight = PRIMARY_START_HEIGHT,
            endHeight = PRIMARY_END_HEIGHT,
            startDelay = PRIMARY_START_DELAY,
            travelTime = PRIMARY_TRAVEL_TIME,
            delayStep = PROJECTILE_STAGGER,
        )

        deps.worldQueues.add(LAND_TICKS) {
            explode(deps, npc, primary, config)
            launchSecondary(deps, npc, centre, primary, config)
        }
    }

    private fun launchSecondary(
        deps: BossDeps,
        npc: Npc,
        centre: CoordGrid,
        primary: List<CoordGrid>,
        config: BookRainConfig,
    ) {
        val secondary =
            chooseBookTiles(
                centre = centre,
                count = SECONDARY_BOOKS,
                radius = rollSpreadRadius(deps),
                exclude = primary.toSet(),
                random = deps.random,
            )
        if (secondary.isEmpty()) return

        throwBooks(
            deps = deps,
            source = centre,
            tiles = secondary,
            startHeight = SECONDARY_START_HEIGHT,
            endHeight = SECONDARY_END_HEIGHT,
            startDelay = SECONDARY_START_DELAY,
            travelTime = SECONDARY_TRAVEL_TIME,
            travelStep = PROJECTILE_STAGGER,
        )
        deps.worldQueues.add(SPLIT_TICKS) { explode(deps, npc, secondary, config) }
    }

    private fun throwBooks(
        deps: BossDeps,
        source: CoordGrid,
        tiles: List<CoordGrid>,
        startHeight: Int,
        endHeight: Int,
        startDelay: Int,
        travelTime: Int,
        delayStep: Int = 0,
        travelStep: Int = 0,
    ) {
        deps.worldRepo.soundArea(
            source,
            THROW_SYNTH,
            loops = 3,
            radius = THROW_SOUND_RADIUS,
        )
        for ((index, tile) in tiles.withIndex()) {
            deps.bossProjectile(
                spotanim = BOOK_PROJECTILE.asRSCM(RSCMType.SPOTANIM),
                src = source,
                target = tile,
                startHeight = startHeight,
                endHeight = endHeight,
                delay = startDelay + index * delayStep,
                travel = travelTime + index * travelStep,
                curve = BOOK_CURVE,
                progress = BOOK_PROGRESS,
            )
        }
    }

    private fun explode(
        deps: BossDeps,
        npc: Npc,
        tiles: List<CoordGrid>,
        config: BookRainConfig,
    ) {
        for (tile in tiles) {
            deps.worldRepo.spotanimMap(
                SpotanimType(EXPLOSION_SPOTANIM.asRSCM(RSCMType.SPOTANIM)),
                tile,
                height = EXPLOSION_HEIGHT,
                delay = EXPLOSION_DELAY,
            )
            deps.worldRepo.soundArea(
                tile,
                IMPACT_SYNTH,
                delay = EXPLOSION_DELAY,
                radius = EXPLOSION_SOUND_RADIUS,
            )
            for (player in deps.playerList) {
                if (!player.isValidTarget()) continue
                if (player.coords.level != tile.level) continue
                val maxHit = explosionMaxHit(player.coords.chebyshevDistance(tile), config) ?: continue
                val damage = deps.random.of(0, maxHit)
                player.queueHit(
                    source = npc,
                    delay = 1,
                    type = HitType.Typeless,
                    damage = damage,
                    modifier = deps.playerHitModifier,
                )
            }
        }
    }

    private fun rollSpreadRadius(deps: BossDeps): Int =
        if (deps.random.of(RARE_RADIUS_ONE_IN) == 0) RARE_RADIUS else NORMAL_RADIUS
}

internal fun chooseBookTiles(
    centre: CoordGrid,
    count: Int,
    radius: Int,
    exclude: Set<CoordGrid>,
    random: GameRandom,
): List<CoordGrid> {
    val candidates =
        buildList {
            for (dx in -radius..radius) {
                for (dz in -radius..radius) {
                    if (dx == 0 && dz == 0) continue
                    val tile = centre.translate(dx, dz)
                    if (tile !in exclude) add(tile)
                }
            }
        }.toMutableList()

    return List(minOf(count, candidates.size)) {
        candidates.removeAt(random.of(candidates.size))
    }
}

internal fun explosionMaxHit(distance: Int, config: BookRainConfig): Int? =
    when (distance) {
        0 -> config.directMaxHit
        1 -> config.splashMaxHit
        else -> null
    }
