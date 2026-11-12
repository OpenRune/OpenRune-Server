package org.rsmod.api.player.hook

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public enum class NeypotzliPassageKind {
    CityGate,
    CityExit,
    RuinsEntrance,
    RuinsExit,
    Cavern,
    MoonChamber,
    Vault,
}

public data class NeypotzliPassage(
    public val origin: CoordGrid,
    public val destination: CoordGrid,
    public val kind: NeypotzliPassageKind,
)

public enum class NeypotzliMoon {
    Blood,
    Blue,
    Eclipse,
}

/**
 * Lets the Perilous Moons quest steer Neypotzli without the Moons content depending on it. Every
 * method defaults to "carry on as normal".
 */
public interface NeypotzliHook {
    /** Shared-map coords for [coords], for a player the hook has placed in a private copy. */
    public fun normalCoords(player: Player, coords: CoordGrid): CoordGrid = coords

    /** `false` stops the player taking [passage]; the hook has told them why. */
    public suspend fun ProtectedAccess.mayPass(passage: NeypotzliPassage): Boolean = true

    /** `true` when the hook moved the player itself instead of the shared-map arrival. */
    public suspend fun ProtectedAccess.arrive(passage: NeypotzliPassage): Boolean = false

    /** `false` stops the player touching Neypotzli's resources; the hook has told them why. */
    public suspend fun ProtectedAccess.mayGather(): Boolean = true

    /** `false` stops the statue sending the player to [moon]; the hook has told them why. */
    public suspend fun ProtectedAccess.mayFight(moon: NeypotzliMoon): Boolean = true

    /** Runs after the entry warning and before the player is sent to [moon]. */
    public suspend fun ProtectedAccess.beforeFight(moon: NeypotzliMoon) {}
}
