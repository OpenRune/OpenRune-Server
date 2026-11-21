package org.rsmod.api.combat.commons

import org.rsmod.game.entity.Player

public object DragonfireProtection {

    public enum class DragonfireType { Chromatic, Metal, WyvernIce, Elvarg }

    private val antifireShields = setOf(
        "obj.antidragonbreathshield",
        "obj.dragonfire_shield",
        "obj.dragonfire_shield_uncharged",
        "obj.dragonfire_ward",
        "obj.dragonfire_ward_uncharged",
        "obj.wyvern_shield",
        "obj.wyvern_shield_uncharged",
    )

    private val wyvernIceShields = setOf(
        "obj.elemental_shield",
        "obj.elemental_mind_shield",
        "obj.dragonfire_shield",
        "obj.dragonfire_shield_uncharged",
        "obj.dragonfire_ward",
        "obj.dragonfire_ward_uncharged",
        "obj.wyvern_shield",
        "obj.wyvern_shield_uncharged",
    )

    public fun resolveMaxHit(player: Player, type: DragonfireType, baseMax: Int): Int {
        if (type == DragonfireType.WyvernIce) {
            return when {
                wyvernIceShields.any { it in player.worn } -> 10
                isProtectingFromMagic(player) -> 20
                else -> baseMax
            }
        }

        val started = hasStartedDragonSlayer(player)
        if (started && hasSuperAntifire(player)) return 0

        val shield = started && hasAntifireShield(player)
        val antifire = started && hasAntifire(player)

        if (type == DragonfireType.Elvarg) {
            return elvargMaxHit(shield, isProtectingFromMagic(player), antifire, baseMax)
        }

        val protect = type == DragonfireType.Chromatic && isProtectingFromMagic(player)

        if (antifire && (shield || protect)) return 0

        var cap = when {
            shield -> 5
            protect -> 10
            else -> baseMax
        }
        if (antifire) cap = (cap - 15).coerceAtLeast(0)
        return cap
    }

    /** Elvarg's breath reaches past melee range, so her caps differ from other chromatic dragons. */
    internal fun elvargMaxHit(shield: Boolean, protect: Boolean, antifire: Boolean, baseMax: Int): Int =
        when {
            shield && protect && antifire -> 4
            shield && (protect || antifire) -> 7
            shield -> 10
            protect && antifire -> 40
            protect || antifire -> 55
            else -> baseMax
        }

    /** Dragonfire protection only works once Dragon Slayer I has been started. */
    private fun hasStartedDragonSlayer(player: Player): Boolean =
        player.vars["varp.dragonquest"] > 0

    private fun hasAntifireShield(player: Player): Boolean =
        antifireShields.any { it in player.worn }

    private fun hasSuperAntifire(player: Player): Boolean =
        player.vars["varbit.super_antifire_potion"] > 0

    private fun hasAntifire(player: Player): Boolean =
        player.vars["varbit.antifire_potion"] > 0

    private fun isProtectingFromMagic(player: Player): Boolean =
        player.vars["varbit.prayer_protectfrommagic"] > 0
}
