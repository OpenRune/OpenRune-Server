package org.rsmod.api.player.stat

public enum class XpSource {
    /** Earned in play: a level gained this way fires the level-up events. */
    Natural,

    /** Set by a tool such as an admin command: levels change without any level-up. */
    Manual,
}
