package org.rsmod.api.player.worn

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import org.rsmod.game.entity.Player

/** Combined capes keep their corresponding max variant's behavior; only regular forms inherit all skill perks. */
public fun Player.hasMaxCapePerks(): Boolean {
    val cape = worn[Wearpos.Back.slot] ?: return false
    return listOf(
        "obj.skillcape_max", "obj.skillcape_max_worn",
        "obj.nero_completionist_2011", "obj.nero_completionist_2011_trimmed",
    ).any { it.asRSCM() == cape.id }
}
