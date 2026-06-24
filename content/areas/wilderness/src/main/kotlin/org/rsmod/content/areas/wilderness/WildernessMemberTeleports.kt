package org.rsmod.content.areas.wilderness

import org.rsmod.api.player.hook.TeleportType

/**
 * Member jewellery and items that may teleport up to level 30 Wilderness.
 *
 * Charge variants (e.g. glory_4) and imbued versions are matched by base name prefix.
 */
public object WildernessMemberTeleports {
    private val LEVEL_30_BASE_NAMES =
        setOf(
            "amulet_of_glory",
            "amulet_of_eternal_glory",
            "combat_bracelet",
            "skills_necklace",
            "ring_of_wealth",
            "pharaohs_sceptre",
            "grand_seed_pod",
            "royal_seed_pod",
            "mm2_royal_seed_pod",
            "slayer_ring",
            "ring_of_life",
            "escape_crystal",
            "defence_cape",
            "max_cape",
            "max_hood",
        )

    public fun isMemberLevel30Teleport(obj: String): Boolean {
        val base = obj.removePrefix("obj.")
        return LEVEL_30_BASE_NAMES.any { name ->
            base == name || base.startsWith("${name}_")
        }
    }

    public fun teleportTypeFor(obj: String): TeleportType =
        if (isMemberLevel30Teleport(obj)) {
            TeleportType.MemberLevel30
        } else {
            TeleportType.Standard
        }
}
