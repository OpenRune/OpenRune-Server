package org.rsmod.api.combat.commons

import dev.openrune.util.Wearpos
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.worn.DragonfireShields
import org.rsmod.game.entity.Player

public object DragonfireProtection {

    public fun absorb(player: Player) {
        val slot = Wearpos.LeftHand.slot
        val shield = player.worn[slot] ?: return
        val kind = DragonfireShields.kind(shield) ?: return
        if (kind == DragonfireShields.Kind.WYVERN) return
        val charges = DragonfireShields.charges(shield)
        if (charges >= DragonfireShields.MAX_CHARGES) return
        player.worn[slot] = DragonfireShields.withCharges(shield, charges + 1)
        player.mes("Your shield absorbs the breath and gains a charge.")
    }

    public fun blocksShieldBlast(player: Player): Boolean =
        hasAntifireShield(player) || hasAntifire(player) || hasSuperAntifire(player) ||
            isProtectingFromMagic(player)

    public enum class DragonfireType { Chromatic, Metal, WyvernIce }

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

        if (hasSuperAntifire(player)) return 0

        val shield = hasAntifireShield(player)
        val protect = type == DragonfireType.Chromatic && isProtectingFromMagic(player)
        val antifire = hasAntifire(player)

        if (antifire && (shield || protect)) return 0

        var cap = when {
            shield -> 5
            protect -> 10
            else -> baseMax
        }
        if (antifire) cap = (cap - 15).coerceAtLeast(0)
        return cap
    }

    private fun hasAntifireShield(player: Player): Boolean =
        antifireShields.any { it in player.worn }

    private fun hasSuperAntifire(player: Player): Boolean =
        player.vars["varbit.super_antifire_potion"] > 0

    private fun hasAntifire(player: Player): Boolean =
        player.vars["varbit.antifire_potion"] > 0

    private fun isProtectingFromMagic(player: Player): Boolean =
        player.vars["varbit.prayer_protectfrommagic"] > 0
}
