package org.rsmod.content.bosses.leviathan

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.map.CoordGrid

internal object LeviathanRockfall {
    const val SEQ = "seq.npc_leviathan_01_stun_01"
    const val SYNTH = "synth.leviathan_rockfall"
    const val RECOVERY = 5
    private const val RUBBLE_LOC = "loc.leviathan_rubble"
    private const val DEBRIS_IMPACT_SYNTH = "synth.leviathan_debris_impact"
    private const val RUBBLE_LAND_SYNTH = "synth.leviathan_rubble_land"
    private const val KNOCKBACK_SEQ = "seq.agilityarena_player_spikedback"
    private val BOULDER_DAMAGE = 10..30

    private const val RUMBLE_SYNTH = "synth.leviathan_rockfall_rumble"
    private val SCATTER_COUNT = 12..30
    private val CHIP_DAMAGE = 5..10
    private val SHAKE = 5..8
    private const val HINT_DELAY = 50
    private const val HINT_SEARCH_RADIUS = 4
    private val BREAK_DELAYS = listOf(20, 50, 80)
    private const val BOULDER_LAND_BASE = 6
    private val BREAK_SPOTANIMS =
        listOf("spotanim.spotanim_brain_01_falling_break_01", "spotanim.spotanim_brain_01_falling_break_02")
    private val STAY_SPOTANIMS =
        listOf(
            "spotanim.spotanim_brain_01_falling_stay_01",
            "spotanim.spotanim_brain_01_falling_stay_02",
            "spotanim.spotanim_brain_01_falling_stay_03",
            "spotanim.spotanim_brain_01_falling_stay_04",
        )

    val ARENA =
        area(
            LeviathanArena.relative(CoordGrid(LeviathanArena.HANDHOLDS_INSIDE.x, LeviathanArena.SEARCH_SW.z, 0)),
            LeviathanArena.relative(LeviathanArena.SEARCH_NE),
        )

    private val arenaPlayers = playersIn(ARENA)

    private val staying: Effect =
        oneOf(STAY_SPOTANIMS.mapIndexed { angle, spot -> boulder(spot, HINT_DELAY, land(stays = true, angle)) })

    val rubbleLanding: Effect = oneOf((0 until STAY_SPOTANIMS.size).map { land(stays = true, angle = it) })

    private val breaking: Effect =
        oneOf(BREAK_DELAYS.flatMap { delay -> BREAK_SPOTANIMS.map { boulder(it, delay, land(stays = false)) } })

    fun rockfall(animated: Boolean, withHints: Boolean): Effect =
        Effect.Sequence(
            listOfNotNull(
                if (animated) anim(SEQ) else null,
                if (withHints) hints() else null,
                onTiles(randomFreeTiles(ARENA, SCATTER_COUNT), breaking),
                onTiles(tilesUnderPlayers(ARENA), staying),
                if (animated) aftershock() else null,
            )
        )

    private fun hints(): Effect =
        whenever(
            varnIs(LeviathanVarns.NEXT_SPECIAL, Special.NONE),
            oneOf(
                Special.entries.map { special ->
                    val tiles = special.hints.map(LeviathanArena::relative)
                    sequence(
                        setVarn(LeviathanVarns.NEXT_SPECIAL, special.varnValue),
                        onTiles(nearestFreeTiles(tiles, ARENA, HINT_SEARCH_RADIUS), staying),
                    )
                }
            ),
        )

    private fun aftershock(): Effect =
        sequence(
            soundTo(SYNTH, arenaPlayers, delay = 5),
            soundTo(RUMBLE_SYNTH, arenaPlayers, loops = 5),
            camShake(CamShakeAxis.LEFT_RIGHT, SHAKE, arenaPlayers),
            camShake(CamShakeAxis.UP_DOWN, SHAKE, arenaPlayers),
            camShake(CamShakeAxis.FORWARDS_BACKWARDS, SHAKE, arenaPlayers),
            hit {
                target = arenaPlayers
                delay = 2
                damage(CHIP_DAMAGE).roll()
                type(Typeless)
                hazard()
            },
            onEach(arenaPlayers, after(RECOVERY, camReset(), requireAlive = false)),
        )

    private fun boulder(spot: String, delay: Int, landing: Effect): Effect =
        sequence(
            mapSpotanim(spot, CurrentTile, delay = delay),
            after(BOULDER_LAND_BASE + (delay - BREAK_DELAYS.first()) / 30, landing),
        )

    private fun land(stays: Boolean, angle: Int = 0): Effect =
        Effect.Sequence(
            listOfNotNull(
                sound(DEBRIS_IMPACT_SYNTH, radius = 5, at = CurrentTile),
                sound(RUBBLE_LAND_SYNTH, radius = 5, at = CurrentTile),
                if (stays) spawnLoc(RUBBLE_LOC, CurrentTile, angle, blockPlayersOnly = true) else null,
                hit {
                    target = playersOn(CurrentTile)
                    delay = 1
                    damage(BOULDER_DAMAGE).roll()
                    type(Typeless)
                    hazard()
                },
                if (stays) onEach(playersOn(CurrentTile), knockback(KNOCKBACK_SEQ, ARENA)) else null,
            )
        )
}
