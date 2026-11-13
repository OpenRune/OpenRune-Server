package org.rsmod.content.areas.misc.fossilunderwater

import jakarta.inject.Inject
import kotlin.math.sign
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.table.FossilUnderwaterCurrentsRow
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Life on the sea floor (https://oldschool.runescape.wiki/w/Underwater_Agility_and_Thieving): the
 * water overlay, the breath that drains every three ticks and fills in air pockets or with a
 * fishbowl helmet and diving apparatus on, the bubbles a player breathes out, the currents that
 * carry a player a tile every three ticks, and drowning when the breath runs out.
 */
class UnderwaterScript
@Inject
constructor(
    private val players: PlayerRegistry,
    private val mapClock: MapClock,
    private val locRepo: LocRepository,
    private val eventBus: EventBus,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    private lateinit var currents: Map<CoordGrid, CoordGrid>
    private val swimmers = LinkedHashSet<Player>()

    override fun ScriptContext.startup() {
        currents =
            FossilUnderwaterCurrentsRow.getRow(CURRENTS).pushes.chunked(2).associate { (from, to) ->
                from to to
            }
        onEvent<GameLifecycle.LateCycle> { tickSwimmers() }
        onPlayerLogin { if (player.inCurrent) swimmers += player }
        onPlayerQueue(DROWN) { drown() }
    }

    private fun tickSwimmers() {
        swimmers += players.findAll(Underwater.ZONES)
        for (player in swimmers.toList()) {
            if (player.slotId == INVALID_SLOT) {
                swimmers -= player
                continue
            }
            tick(player)
            if (!Underwater.contains(player.coords)) {
                swimmers -= player
            }
        }
    }

    private fun tick(player: Player) {
        if (!Underwater.contains(player.coords)) {
            surfaced(player)
            return
        }
        if (!player.drowning && !player.ui.containsOverlay(Underwater.OVERLAY)) {
            Underwater.openOverlay(player, eventBus)
        }
        val cycle = mapClock.cycle
        if (cycle % EXHALE_CYCLE == 0) {
            player.spotanim(BUBBLES, height = BUBBLES_HEIGHT)
        }
        if (cycle % Breath.CYCLE != 0 || player.drowning) {
            return
        }
        breathe(player)
        val carriedTo = currents[player.coords]
        if (carriedTo != null) {
            carry(player, carriedTo)
        } else if (player.inCurrent) {
            player.inCurrent = false
        }
    }

    private fun surfaced(player: Player) {
        if (player.ui.containsOverlay(Underwater.OVERLAY)) {
            player.ifCloseOverlay(Underwater.OVERLAY, eventBus)
            MiscOutput.hintArrowReset(player)
        }
        if (player.inCurrent) {
            player.inCurrent = false
        }
    }

    private fun breathe(player: Player) {
        val before = player.oxygen
        val after =
            if (player.wearsDivingGear()) {
                Breath.FULL
            } else {
                Breath.breathe(before, inAirPocket(player.coords))
            }
        if (after != before) {
            player.oxygen = after
        }
        if (after == 0) {
            player.drowning = true
            player.strongQueue(DROWN, DROWN_DELAY)
        }
    }

    private fun Player.wearsDivingGear(): Boolean =
        worn.count(FISHBOWL) > 0 && worn.count(DIVING_APPARATUS) > 0

    private fun inAirPocket(coords: CoordGrid): Boolean =
        AIR_POCKET_CORNERS.any { (dx, dz) -> locRepo.findLoc(coords.translate(dx, dz), AIR_POCKET) }

    private fun carry(player: Player, to: CoordGrid) {
        protectedAccess.launch(player) {
            if (!player.inCurrent) {
                player.inCurrent = true
                ifClose()
                mes(CAUGHT)
            }
            val from = coords
            // UNVERIFIED: live keeps the player's facing; the stream's direction is used here.
            exactMove(
                start = from,
                end = to,
                delay1 = 0,
                delay2 = PUSH_CYCLES,
                dir = angleOf(from, to),
                teleportType = TeleportType.Exempt,
            )
        }
    }

    private suspend fun ProtectedAccess.drown() {
        if (!Underwater.contains(coords)) {
            player.drowning = false
            return
        }
        anim(DEATH)
        soundSynth(DEATH_SYNTH)
        delay(1)
        fadeOverlay(0, Underwater.TRANSPARENT, 0, Underwater.SOLID, Underwater.FADE_CYCLES)
        delay(3)
        telejump(Underwater.SMALL_ISLAND, TeleportType.Exempt)
        resetAnim()
        fadeOverlay(0, Underwater.SOLID, 0, Underwater.TRANSPARENT, Underwater.FADE_CYCLES)
        MiscOutput.hintArrowReset(player)
        delay(1)
        mes("You wake up on the small island.")
        payMermaid()
        player.drowning = false
        closeFadeOverlay()
    }

    private fun ProtectedAccess.payMermaid() {
        val taken = Breath.tearsTaken(invTotal(inv, MERMAID_TEAR), random.of(Breath.TEARS_TAKEN))
        // UNVERIFIED: what live says when the player carries no tears to pay with.
        if (taken == 0 || invDel(inv, MERMAID_TEAR, taken).failure) {
            return
        }
        mes(
            "A mermaid pulls you to the surface, taking $taken of your total mermaid's tears " +
                "as payment."
        )
    }

    private fun angleOf(from: CoordGrid, to: CoordGrid): Int {
        val dx = (to.x - from.x).sign
        val dz = (to.z - from.z).sign
        return when {
            dz < 0 -> FACE_SOUTH
            dx < 0 -> FACE_WEST
            dz > 0 -> FACE_NORTH
            else -> FACE_EAST
        }
    }

    private companion object {
        const val CURRENTS = "dbrow.fossil_underwater_currents"
        const val DROWN = "queue.fossil_underwater_drown"
        const val AIR_POCKET = "loc.fossil_air_space"
        const val FISHBOWL = "obj.hundred_pirate_diving_helmet"
        const val DIVING_APPARATUS = "obj.hundred_pirate_diving_backpack"
        const val MERMAID_TEAR = "obj.fossil_mermaid_tear"
        const val BUBBLES = "spotanim.100_pirate_bubbles_spotanim"
        const val DEATH = "seq.human_real_swimming_death"
        const val DEATH_SYNTH = "synth.human_underwater_death"
        const val CAUGHT = "You are caught in a current."
        const val BUBBLES_HEIGHT = 900
        const val EXHALE_CYCLE = 30
        const val DROWN_DELAY = 3
        const val PUSH_CYCLES = 30
        const val FACE_SOUTH = 0
        const val FACE_WEST = 512
        const val FACE_NORTH = 1024
        const val FACE_EAST = 1536

        /** The south-west corners of the 2x2 air pockets that could cover a tile. */
        val AIR_POCKET_CORNERS = listOf(0 to 0, -1 to 0, 0 to -1, -1 to -1)
    }
}
