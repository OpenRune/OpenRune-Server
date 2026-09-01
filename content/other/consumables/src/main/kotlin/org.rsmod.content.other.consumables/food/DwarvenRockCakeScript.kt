package org.rsmod.content.other.consumables.food

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Dwarven rock cake is never consumed and never destroyed - it's a self-damage item, not
 * actual food. Wiki: eating depletes 1 HP when above 2 HP (does nothing at 2 HP or below);
 * guzzling depletes 10% of current HP (rounded down) plus one, which can bring the player down to
 * exactly 1 HP but never lower (0 damage once already at 1 HP). Neither action can kill.
 */
class DwarvenRockCakeScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        for (item in ROCK_CAKE_ITEMS) {
            onOpHeld1(item) { eatRockCake() }
            onOpHeld3(item) { guzzleRockCake() }
        }
    }

    private fun ProtectedAccess.eatRockCake() {
        val damage = DwarvenRockCakeDamage.eatDamage(player.hitpoints)
        if (damage <= 0) {
            mes("The cake resists all attempts to eat it.")
            return
        }
        anim(EAT_ANIM)
        mes("Ow! You nearly broke a tooth!")
        takeInstantHit(type = HitType.Typeless, damage = damage)
    }

    private fun ProtectedAccess.guzzleRockCake() {
        val damage = DwarvenRockCakeDamage.guzzleDamage(player.hitpoints)
        anim(EAT_ANIM)
        mes("You guzzle down some of the cake.")
        if (damage > 0) {
            takeInstantHit(type = HitType.Typeless, damage = damage)
        }
    }

    private companion object {
        val ROCK_CAKE_ITEMS =
            listOf("obj.hundred_dwarf_hot_rockcake", "obj.hundred_dwarf_cool_rockcake")
        const val EAT_ANIM = "seq.human_eat"
    }
}

/** Pure damage math, kept separate from [ProtectedAccess] so it can be unit tested. */
internal object DwarvenRockCakeDamage {
    fun eatDamage(currentHitpoints: Int): Int = if (currentHitpoints > 2) 1 else 0

    fun guzzleDamage(currentHitpoints: Int): Int {
        if (currentHitpoints <= 1) {
            return 0
        }
        val raw = currentHitpoints / 10 + 1
        return raw.coerceAtMost(currentHitpoints - 1)
    }
}
