package org.rsmod.api.player.prayer

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

/**
 * Tracks a temporary block on activating protection prayers (Dragon scimitar's Sever, in PvP).
 * The three protection-prayer varbits, shared with [org.rsmod.api.player.hit.modifier
 * .StandardPlayerHitModifier]'s own protection-prayer check.
 */
public object ProtectionPrayerLockout {
    // Wiki: "prevents the targeted player from using said prayers for 8 ticks (4.8 seconds)".
    private const val LOCKOUT_CYCLES: Int = 8

    public val PROTECTION_PRAYER_VARBITS: Set<String> =
        setOf(
            "varbit.prayer_protectfrommelee",
            "varbit.prayer_protectfrommissiles",
            "varbit.prayer_protectfrommagic",
        )

    private val lockedUntil = AttributeKey<Int>(resetOnDeath = true, temp = true)

    public fun activate(player: Player) {
        player.attr[lockedUntil] = player.currentMapClock + LOCKOUT_CYCLES
    }

    public fun isLocked(player: Player): Boolean {
        val until = player.attr[lockedUntil] ?: return false
        return player.currentMapClock < until
    }
}

/**
 * Immediately turns off any active protection prayer and blocks re-activating one for 8 ticks.
 * PvP-only per the wiki - callers are expected to only invoke this against [Player] targets.
 */
public fun Player.disableProtectionPrayers() {
    for (varbit in ProtectionPrayerLockout.PROTECTION_PRAYER_VARBITS) {
        if (vars[varbit] != 0) {
            VarPlayerIntMapSetter.set(this, varbit, 0)
        }
    }
    ProtectionPrayerLockout.activate(this)
}
