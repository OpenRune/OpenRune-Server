package org.rsmod.content.other.maxcape

import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

internal var Player.capeResetDay by intVarp("varp.max_cape_reset_day")
internal var Player.capeSpellbookUses by intVarBit("varbit.sb_swap_check")
internal var Player.capeSearchUses by intVarBit("varbit.free_grapple")
internal var Player.capeHunterUses by intVarBit("varbit.chinchompa_teleports")
internal var Player.capeStaminaUsed by boolVarBit("varbit.max_cape_stamina_used")
internal var Player.capeJunkDisabled by boolVarBit("varbit.max_cape_junk_disabled")
internal var Player.capeLifeDisabled by boolVarBit("varbit.rol_toggle")
internal var Player.capeNextCollection by intVarp("varp.max_cape_next_collection")
internal var Player.capeEscapePending by intVarp("varp.max_cape_escape_pending")

internal fun Player.resetCapeDailyUses(day: Int = LocalDate.now(ZoneOffset.UTC).toEpochDay().toInt()) {
    if (capeResetDay == day) return
    capeResetDay = day
    capeSpellbookUses = 0
    capeSearchUses = 0
    capeHunterUses = 0
    capeStaminaUsed = false
}
