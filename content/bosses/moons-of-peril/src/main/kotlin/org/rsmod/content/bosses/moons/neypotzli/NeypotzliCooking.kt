package org.rsmod.content.bosses.moons.neypotzli

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.player.hook.NeypotzliHook
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class NeypotzliCooking
@Inject
constructor(private val hooks: Set<@JvmSuppressWildcards NeypotzliHook>) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(STOVE) { cook(FOODS) }
        onOpLoc2(STOVE) { makeCuppa() }
        for (food in FOODS) {
            onOpLocU(STOVE, food.raw) { cook(listOf(food)) }
        }
    }

    private suspend fun ProtectedAccess.cook(menu: List<Food>) {
        if (!neypotzliOpen(hooks)) return
        if (statBase("stat.cooking") < COOKING_LEVEL) {
            mes("You need a Cooking level of $COOKING_LEVEL to cook this.")
            return
        }
        if (menu.none { inv.count(it.raw) > 0 }) {
            mes("You haven't got anything to cook.")
            return
        }
        while (true) {
            val food = menu.firstOrNull { inv.count(it.raw) > 0 } ?: return
            anim("seq.human_cooking_walkmerge_norestart")
            soundSynth("synth.fry")
            delay(1)
            if (!cookOne(food)) return
        }
    }

    private fun ProtectedAccess.cookOne(food: Food): Boolean {
        if (invReplace(inv, food.raw, 1, food.cooked).failure) return false
        statAdvance("stat.cooking", food.xp)
        if (inv.count(food.raw) == 0) return true
        if (!statRandom("stat.cooking", EXTRA_LOW, EXTRA_HIGH, 0)) return true
        if (invReplace(inv, food.raw, 1, food.cooked).failure) return true
        mes(food.extraMessage)
        return true
    }

    private fun ProtectedAccess.makeCuppa() {
        anim("seq.human_eat")
        soundSynth("synth.liquid")
        player.runEnergy = constants.run_max_energy
        UpdateRun.energy(player, constants.run_max_energy)
        spam("You drink the tea and find yourself feeling energised!")
    }

    private class Food(
        val raw: String,
        val cooked: String,
        val xp: Double,
        val extraMessage: String,
    )

    private companion object {
        private const val STOVE = "loc.pmoon_range"
        private const val COOKING_LEVEL = 30

        // UNVERIFIED: the odds of a second item cooking alongside the first; only the fact that it
        // scales with level is known.
        private const val EXTRA_LOW = 16
        private const val EXTRA_HIGH = 256

        private val FOODS =
            listOf(
                Food(
                    raw = "obj.bream_fish_raw",
                    cooked = "obj.bream_fish_cooked",
                    xp = 45.0,
                    extraMessage = "Your cooking knowledge allows you to cook two fish at once!",
                ),
                Food(
                    raw = "obj.raw_lizard",
                    cooked = "obj.cooked_lizard",
                    xp = 60.0,
                    extraMessage =
                        "Your cooking knowledge allows you to cut extra meat from the lizard!",
                ),
            )
    }
}
