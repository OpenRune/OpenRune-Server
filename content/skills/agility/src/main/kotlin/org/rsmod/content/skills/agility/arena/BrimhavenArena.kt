package org.rsmod.content.skills.agility.arena

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.boolVarp
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.agility.AgilityArenaRow
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

internal val ARENA_START = CoordGrid(2805, 9591, 3)
internal val ARENA_EXIT = CoordGrid(2810, 3194, 0)
internal val ENTRANCE_HUT = CoordGrid(2809, 3193, 0)

internal const val ROTATION_TICKS = 98
internal const val REENTRY_TICKS = 100
internal const val HELPER_COURSE = 2
internal const val HINT_ARROW = 6
internal const val HINT_ARROW_HEIGHT = 60
internal const val ARENA_LEVEL = 3

internal fun CoordGrid.inArena(): Boolean = x in 2752..2815 && z in 9536..9599

internal val ARENA_ZONES = ZoneKey.covering(CoordGrid(2752, 9536, 0), CoordGrid(2815, 9599, 3))

internal var Player.ticketAvailable by boolVarBit("varbit.agilityarena_ticketavailable")
internal var Player.canEnterArena by boolVarBit("varbit.agilityarena_canenter")
internal var Player.teleportedOut by boolVarBit("varbit.agility_arena_teleported_out")
internal var Player.highlightedCourse by intVarBit("varbit.helper_agility_highlighted_course")
internal var Player.currentHintArrow by intVarBit("varbit.current_hint_arrow")
internal var Player.arenaTags by intVarp("varp.agility_laps_brimhaven_arena")
internal var Player.taggedRotation by intVarp("varp.agility_arena_tagged_rotation")
internal var Player.arenaLeftClock by intVarp("varp.agility_arena_left_clock")
internal var Player.lastTagSecond by intVarp("varp.agility_arena_last_tag")
internal var Player.tagLockedUntil by intVarp("varp.agility_arena_tag_lock")
internal var Player.taggedThisSession by boolVarp("varp.agility_arena_session_tagged")

@Singleton
class ArenaDispensers @Inject constructor(private val random: GameRandom) {
    val dispensers: List<CoordGrid> by lazy {
        AgilityArenaRow.getRow("dbrow.agility_arena").dispensers
    }

    var current: CoordGrid = CoordGrid.NULL
        private set

    var rotation: Int = 0
        private set

    private var nextRotation = 0
    private var tagged = false

    fun tick(clock: Int): Boolean {
        if (clock < nextRotation) {
            return false
        }
        // UNVERIFIED: a uniform pick of any other dispenser; captures never show one twice running.
        current = random.pick(dispensers.filter { it != current })
        rotation++
        tagged = false
        nextRotation = clock + ROTATION_TICKS
        return true
    }

    fun claimFirstTag(): Boolean {
        if (tagged) {
            return false
        }
        tagged = true
        return true
    }
}
