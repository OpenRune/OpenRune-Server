package org.rsmod.api.npc.aggression

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/**
 * OSRS aggression tolerance: after a player spends [TOLERANCE_TICKS] (10 minutes) in the vicinity
 * of aggressive monsters, those monsters stop being aggressive towards that player until the player
 * leaves the area and comes back.
 *
 * Tolerance only applies to npc types that have been [enroll]ed, so unenrolled npcs keep their
 * existing aggression untouched. Per-player state lives in temporary varps; a player's timer
 * restarts whenever they are first seen after moving more than [LEAVE_DISTANCE] tiles from where it
 * started, or after going [ABSENT_TICKS] without being seen by an enrolled npc.
 */
@Singleton
public class AggressionTolerance @Inject constructor(private val mapClock: MapClock) {
    private val enrolled: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    public fun enroll(type: NpcServerType) {
        enrolled += type.id
    }

    public fun enroll(internal: String) {
        enrolled += internal.asRSCM(RSCMType.NPC)
    }

    public fun appliesTo(npc: Npc): Boolean = npc.type.id in enrolled

    /**
     * Returns `true` if [npc] should ignore [player] due to tolerance. Always `false` when the
     * npc's type is not enrolled. Evaluating this also counts as the player being seen in the area,
     * so it must be called for every player an enrolled npc considers.
     */
    public fun isTolerant(npc: Npc, player: Player): Boolean {
        if (!appliesTo(npc)) {
            return false
        }
        return isTolerant(player)
    }

    public fun isTolerant(player: Player): Boolean {
        val now = mapClock.cycle
        val coords = player.coords
        val anchor = player.toleranceAnchor
        val absent = now - player.toleranceSeen > ABSENT_TICKS
        val moved = anchor == 0 || coords.chebyshevDistance(CoordGrid(anchor)) > LEAVE_DISTANCE
        if (player.toleranceStart == 0 || absent || moved) {
            player.toleranceStart = now + 1
            player.toleranceAnchor = coords.packed
            player.toleranceSeen = now
        }
        if (now - player.toleranceSeen >= SEEN_REFRESH_TICKS) {
            player.toleranceSeen = now
        }
        return now - (player.toleranceStart - 1) >= TOLERANCE_TICKS
    }

    public fun reset(player: Player) {
        player.toleranceStart = 0
        player.toleranceAnchor = 0
        player.toleranceSeen = 0
    }

    public companion object {
        public const val TOLERANCE_TICKS: Int = 1000
        public const val LEAVE_DISTANCE: Int = 38
        public const val ABSENT_TICKS: Int = 150

        /** Varp writes ask the persistence layer for a save, so `seen` is only refreshed this often. */
        private const val SEEN_REFRESH_TICKS: Int = 50
    }
}

private var Player.toleranceStart: Int by intVarp("varp.aggression_tolerance_start")
private var Player.toleranceAnchor: Int by intVarp("varp.aggression_tolerance_anchor")
private var Player.toleranceSeen: Int by intVarp("varp.aggression_tolerance_seen")
