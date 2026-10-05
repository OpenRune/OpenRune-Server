package org.rsmod.content.bosses.doom

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.bosses.spec.ImpactRounding
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.api.bosses.spec.TargetExpr
import org.rsmod.api.bosses.spec.TileSet
import org.rsmod.api.random.GameRandom
import org.rsmod.map.CoordGrid

internal enum class DoomStyle(
    val type: HitType,
    val projectile: String,
    val impactSpotanim: String,
    val launchSynth: String,
    val impactSynth: String,
) {
    Magic(
        type = HitType.Magic,
        projectile = "spotanim.vfx_standard_projectile_magic",
        impactSpotanim = "spotanim.vfx_leviathan_01_projectile_impact_magic_01",
        launchSynth = "synth.dom_standard_launch_magic",
        impactSynth = "synth.dom_standard_impact_magic",
    ),
    Ranged(
        type = HitType.Ranged,
        projectile = "spotanim.vfx_standard_projectile_range",
        impactSpotanim = "spotanim.vfx_leviathan_01_projectile_impact_ranged_01",
        launchSynth = "synth.dom_standard_launch_range",
        impactSynth = "synth.leviathan_pathfinder_spawn",
    ),
    Melee(
        type = HitType.Melee,
        projectile = "spotanim.vfx_standard_projectile_melee",
        impactSpotanim = "spotanim.vfx_leviathan_01_projectile_impact_melee_01",
        launchSynth = "synth.dom_standard_launch_melee",
        impactSynth = "synth.dom_standard_impact_melee",
    );

    val alternate: DoomStyle
        get() = if (this == Magic) Ranged else Magic

    companion object {
        val DISTANCED = listOf(Magic, Ranged)
    }
}

internal object DoomAttacks {
    const val TONGUE_SEQ = "seq.dom_melee_attack"
    const val TONGUE_RATE = 4
    const val TONGUE_MAX_HIT = 40
    const val TONGUE_PRAYER_PENETRATION = 50

    private const val STANDARD_SEQ = "seq.dom_standard_range_attack"
    private const val IMPACT_HEIGHT = 100
    private const val LAUNCH_DELAY = 55
    private const val BURROW_SHOT_TRAVEL = 90

    fun standard(delve: DoomDelve): Effect {
        val styles = if (delve.basicProjMeleeStyle) DoomStyle.entries else DoomStyle.DISTANCED
        val config =
            ProjectileConfig.fixed(387, 100, delay = LAUNCH_DELAY, travel = delve.basicProjTravel, angle = 30, progress = 147)
        return sequence(
            anim(STANDARD_SEQ),
            oneOf(
                styles.map {
                    sequence(
                        styled(it, delve, config, from = null, rounding = ImpactRounding.Up),
                        soundTo(it.launchSynth, delay = LAUNCH_DELAY),
                    )
                }
            ),
        )
    }

    fun burrowShot(delve: DoomDelve): Effect {
        val styles = if (delve.basicProjMeleeStyle) DoomStyle.entries else DoomStyle.DISTANCED
        val config = ProjectileConfig.fixed(387, 100, delay = 0, travel = BURROW_SHOT_TRAVEL, angle = 30, progress = 147)
        return oneOf(
            styles.map {
                sequence(styled(it, delve, config, from = null, rounding = ImpactRounding.Up), soundTo(it.launchSynth))
            }
        )
    }

    fun styled(
        style: DoomStyle,
        delve: DoomDelve,
        config: ProjectileConfig,
        from: TargetExpr.Single?,
        rounding: ImpactRounding = ImpactRounding.Down,
    ): Effect =
        projectile(
            spotanim = style.projectile,
            config = config,
            from = from,
            resolveOnImpact = true,
            impactRounding = rounding,
            hit =
                hit {
                    damage(Accuracy(Roll(0..delve.standardMaxHit)))
                    type(style.type)
                    spotanim(style.impactSpotanim, height = IMPACT_HEIGHT, unlessPraying = true)
                    onHit(
                        whenever(!TargetPraying(style.type), soundTo(style.impactSynth)),
                        evenOnMiss = true,
                    )
                },
        )
}

internal object DoomRockThrow {
    const val PLACE_ROCK_EXT = "doom.place_rock"

    private const val SEQ = "seq.dom_rock_throw_attack"
    private const val SPLIT_TICKS = 7
    private const val ROCK_LAND_TICKS = 3
    private const val DEBRIS_HIT_TICKS = 3
    private const val SPLIT_HEIGHT = 244
    private const val DEBRIS_RADIUS = 4
    private const val CYCLES_PER_TICK = 30
    private const val SECOND_ROCK_DELAY = 3
    private const val SINGLE_THROW_SLOTS = 2
    private const val DOUBLE_THROW_SLOTS = 3

    private const val AIM = "aim"
    private const val PLAYER = "player"
    private const val DEBRIS = "debris"
    private const val SECOND_DEBRIS = "second_debris"
    private const val SOURCE = "source"

    private const val SPLIT_SYNTH = "synth.dom_rock_split"
    private const val DEBRIS_SHADOW = "spotanim.gargboss_debris_shadow_90_small"
    private const val DEBRIS_IMPACT = "spotanim.vfx_rock_projectile_impact"
    private const val PUSH_SEQ = "seq.agilityarena_player_spikedback"
    private val DEBRIS_DAMAGE = 0..21
    private val ROCK_DAMAGE = 10..21
    private val DEBRIS_PROJECTILES = (1..8).map { "spotanim.vfx_rock_projectile_projectile0$it" }

    private val DEBRIS_CONFIG =
        ProjectileConfig(
            startHeight = 500,
            endHeight = 0,
            startDelay = 0,
            travelTime = 60,
            angle = 0,
            progress = 0,
            stepMultiplier = 3,
        )

    private val midway =
        customTile { npc, target ->
            val centre = npc.coords.translate(npc.size / 2, npc.size / 2)
            CoordGrid((centre.x + target.coords.x) / 2, (centre.z + target.coords.z) / 2, centre.level)
        }

    private val debrisFall: Effect =
        sequence(
            mapSpotanim(DEBRIS_SHADOW, CurrentTile),
            oneOf(
                DEBRIS_PROJECTILES.map {
                    projectile(it, from = tile(AIM), target = CurrentTile, config = DEBRIS_CONFIG, impact = DEBRIS_IMPACT)
                }
            ),
            after(
                DEBRIS_HIT_TICKS,
                hit {
                    target = playersOn(CurrentTile)
                    delay = 1
                    damage(DEBRIS_DAMAGE).roll()
                    type(Typeless)
                    hazard()
                },
            ),
        )

    private val rockLanding: Effect =
        sequence(
            external(PLACE_ROCK_EXT, at = tile(PLAYER)),
            onEach(
                playersOn(tile(PLAYER)),
                sequence(
                    knockback(PUSH_SEQ, DoomArena.AREA),
                    hit {
                        delay = 1
                        damage(ROCK_DAMAGE).roll()
                        type(Typeless)
                        hazard()
                    },
                ),
            ),
        )

    fun rockThrow(delve: DoomDelve): Effect =
        sequence(anim(SEQ), oneOf(DoomStyle.DISTANCED.map { throwStyled(delve, it) }))

    private fun launchSpotanim(style: DoomStyle): String =
        if (style == DoomStyle.Magic) "spotanim.vfx_rock_projectile_launch_magic" else "spotanim.vfx_rock_projectile_launch_range"

    private fun splitSpotanim(style: DoomStyle): String =
        if (style == DoomStyle.Magic) "spotanim.vfx_rock_projectile_split_magic" else "spotanim.vfx_rock_projectile_split_range"

    fun attackSlots(delve: DoomDelve): Int = if (delve.doubleRockThrow) DOUBLE_THROW_SLOTS else SINGLE_THROW_SLOTS

    private fun throwStyled(delve: DoomDelve, style: DoomStyle): Effect {
        val travel = if (delve.hasBurrow) 120 else 150
        val splitTicks = if (delve.hasBurrow) SPLIT_TICKS - 1 else SPLIT_TICKS
        val launches =
            List(if (delve.doubleRockThrow) 2 else 1) { index ->
                projectile(
                    launchSpotanim(style),
                    target = tile(AIM),
                    config =
                        ProjectileConfig.fixed(
                            340,
                            500,
                            delay = 60,
                            travel = travel + index * SECOND_ROCK_DELAY * CYCLES_PER_TICK,
                            angle = 50,
                            progress = 124,
                        ),
                )
            }
        val second = if (delve.doubleRockThrow) split(delve, style, SECOND_DEBRIS, exclude = DEBRIS, next = null) else null
        return withTile(
            AIM,
            midway,
            sequence(
                Effect.Sequence(launches),
                after(splitTicks, split(delve, style, DEBRIS, exclude = null, next = second)),
            ),
        )
    }

    /** The second rock of a double throw splits inside the first's scope so its debris can skip the first's tiles. */
    private fun split(delve: DoomDelve, style: DoomStyle, debris: String, exclude: String?, next: Effect?): Effect {
        val tiles = debrisTiles(delve).let { if (exclude != null) it - bound(exclude) else it }
        return withTile(
            PLAYER,
            CurrentTargetTile,
            withTiles(
                debris,
                tiles,
                sequence(
                    mapSpotanim(splitSpotanim(style), tile(AIM), height = SPLIT_HEIGHT),
                    sound(SPLIT_SYNTH, at = tile(AIM)),
                    disablePrayers(overheadsOnly = true),
                    onTiles(bound(debris), debrisFall),
                    after(ROCK_LAND_TICKS, sequence(rockLanding, followUps(delve, style, debris))),
                    next?.let { after(SECOND_ROCK_DELAY, it) } ?: Effect.NoOp,
                ),
            ),
        )
    }

    private fun followUps(delve: DoomDelve, style: DoomStyle, debris: String): Effect =
        whenever(
            !tilesEmpty(debris),
            Effect.Sequence(
                (0 until delve.secondaryProjCount).map { index ->
                    val shotStyle = if (index % 2 == 0) style else style.alternate
                    val travel = delve.secondaryProjTravel + index * CYCLES_PER_TICK
                    val config = ProjectileConfig.fixed(100, 100, delay = 0, travel = travel, angle = 0)
                    withTile(SOURCE, randomOf(debris), DoomAttacks.styled(shotStyle, delve, config, from = tile(SOURCE)))
                }
            ),
        )

    private fun debrisTiles(delve: DoomDelve): TileSet =
        customTiles(DoomArena.AREA) { _, target, random ->
            val centre = target.coords
            val around =
                (-DEBRIS_RADIUS..DEBRIS_RADIUS).flatMap { dx ->
                    (-DEBRIS_RADIUS..DEBRIS_RADIUS).mapNotNull { dz ->
                        if (dx == 0 && dz == 0) null else centre.translate(dx, dz)
                    }
                }
            listOf(centre) + around.pickDistinct(random.of(delve.rockShrapnelQuantity) - 1, random)
        }

    private fun <T> List<T>.pickDistinct(count: Int, random: GameRandom): List<T> {
        val pool = toMutableList()
        return List(count.coerceIn(0, pool.size)) { pool.removeAt(random.of(pool.size)) }
    }
}
