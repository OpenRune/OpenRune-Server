package org.rsmod.api.player.hook

public enum class WildernessTeleportType {
    /** Standard spell, tablet, or similar teleport - works up to level 20 Wilderness. */
    Standard,

    /** Member jewellery and items that work up to level 30 Wilderness. */
    MemberLevel30,

    /** Chronicle - cannot be used anywhere in the Wilderness. */
    Chronicle,

    /** Minigame teleports - cannot be used anywhere in the Wilderness. */
    Minigame,

    /** Bypasses wilderness teleport restrictions (e.g. admin commands). */
    Exempt,
}
