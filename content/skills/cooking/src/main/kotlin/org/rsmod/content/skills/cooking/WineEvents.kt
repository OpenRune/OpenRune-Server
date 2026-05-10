package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.cookingLvl
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.content.skills.skillMulti
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import skillSuccess


private enum class WineKind {
    Normal,
    Zamorak;

    companion object {
        fun fromUnfermentedProduct(internal: String): WineKind? =
            when (internal) {
                "obj.jug_unfermented_wine", "obj.jug_wine" -> Normal
                "obj.jug_unfermented_zamorak_wine", "obj.wine_of_zamorak" -> Zamorak
                else -> null
            }
    }
}

private data class WineMixRecipe(
    val minCooking: Int,
    val lowLevelMes: String,
    val primary: String,
    val water: String,
    val unfermented: String,
    val squeezeMes: String,
)

private fun WineKind.toRecipe(): WineMixRecipe =
    when (this) {
        WineKind.Normal -> WineMixRecipe(
            minCooking = 35,
            lowLevelMes = "You need a Cooking level of 35 to make wine.",
            primary = "obj.grapes",
            water = "obj.jug_water",
            unfermented = "obj.jug_unfermented_wine",
            squeezeMes = "You squeeze the grapes into the jug of water.",)
        WineKind.Zamorak -> WineMixRecipe(
            minCooking = 65,
            lowLevelMes = "You need a Cooking level of 65 to make wine of Zamorak.",
            primary = "obj.zamorak_grapes",
            water = "obj.jug_water",
            unfermented = "obj.jug_unfermented_zamorak_wine",
            squeezeMes = "You squeeze the grapes of Zamorak into the jug of water."
        )
    }

private fun ProtectedAccess.canOfferWineRecipe(kind: WineKind): Boolean {
    val r = kind.toRecipe()
    if (player.cookingLvl < r.minCooking) {
        return false
    }
    return inv.count(r.primary) > 0 && inv.count(r.water) > 0
}

private data class WineMakeTask(
    val kind: WineKind,
    val target: Int,
    val finished: Int,
)

class WineEvents @Inject constructor() : PluginScript() {

    override fun ScriptContext.startup() {
        onOpHeldU("obj.grapes", "obj.jug_water") { promptWineMaking() }
        onOpHeldU("obj.zamorak_grapes", "obj.jug_water") { promptWineMaking() }

        onPlayerQueueWithArgs("queue.wine_mix") { processWineMixTick(it.args) }
        onPlayerSoftTimer("timer.cooking_wine_ferment") { player.onWineFermentTimer() }
        onPlayerSoftTimer("timer.cooking_zamorak_wine_ferment") { player.onZamorakWineFermentTimer() }
    }

    private suspend fun ProtectedAccess.promptWineMaking() {
        val offered = WineKind.entries.filter { canOfferWineRecipe(it) }
        if (offered.isEmpty()) {
            when {
                !inv.contains("obj.jug_water") -> mes("You need a jug of water to make wine.")
                inv.contains("obj.grapes") && player.cookingLvl < 35 -> mes("You need a Cooking level of 35 to make wine.")
                inv.contains("obj.zamorak_grapes") && player.cookingLvl < 65 -> mes("You need a Cooking level of 65 to make wine of Zamorak.")
                else -> mes("You don't have what you need to make this wine.")
            }
            return
        }

        val config = skillMulti {
            verb("make")

            for (kind in offered) {
                when (kind) {
                    WineKind.Normal -> entry("obj.jug_unfermented_wine") {
                        material("obj.grapes")
                        material("obj.jug_water")
                    }
                    WineKind.Zamorak -> entry("obj.jug_unfermented_zamorak_wine") {
                        material("obj.zamorak_grapes")
                        material("obj.jug_water")
                    }
                }
            }
        }

        openSkillMulti(config) { selection ->
            val kind = WineKind.fromUnfermentedProduct(selection.entry.internal) ?: return@openSkillMulti
            val r = kind.toRecipe()
            val maxMake = minOf(inv.count(r.primary), inv.count(r.water))
            startWineMixChain(kind, selection.amount.coerceAtMost(maxMake))
        }
    }

    private fun ProtectedAccess.startWineMixChain(kind: WineKind, amount: Int) {
        if (amount <= 0) {
            return
        }
        clearQueue("queue.wine_mix")
        queue("queue.wine_mix", 2, WineMakeTask(kind, amount, 0))
    }

    private fun ProtectedAccess.processWineMixTick(task: WineMakeTask) {
        val r = task.kind.toRecipe()

        if (player.cookingLvl < r.minCooking) {
            mes(r.lowLevelMes)
            return
        }

        if (!inv.contains(r.primary) || !inv.contains(r.water)) {
            return
        }

        anim("seq.human_make_wine")
        spotanim("spotanim.cooking_make_wine_spotanim")
        invDel(inv, r.primary, 1)
        invDel(inv, r.water, 1)
        invAdd(inv, r.unfermented, 1)
        mes(r.squeezeMes)

        scheduleWineFermentTimer(task.kind)

        val made = task.finished + 1
        if (made < task.target && inv.contains(r.primary) && inv.contains(r.water)) {
            queue("queue.wine_mix", 2, WineMakeTask(task.kind, task.target, made))
        }
    }

    private fun ProtectedAccess.scheduleWineFermentTimer(kind: WineKind) {
        when (kind) {
            WineKind.Normal -> {
                player.clearSoftTimer("timer.cooking_wine_ferment")
                player.softTimer("timer.cooking_wine_ferment", 20)
            }
            WineKind.Zamorak -> {
                player.clearSoftTimer("timer.cooking_zamorak_wine_ferment")
                player.softTimer("timer.cooking_zamorak_wine_ferment", 20)
            }
        }
    }

    /**
     * Soft timers are not cleared by [org.rsmod.api.player.ui.ifClose], unlike weak queues, so
     * fermentation still completes while interfaces open or other actions run.
     */
    private fun Player.onWineFermentTimer() {
        clearSoftTimer("timer.cooking_wine_ferment")
        fermentAllUnfermented(
            unfermented = "obj.jug_unfermented_wine",
            product = "obj.jug_wine",
            noFailLevel = 68,
        )
    }

    private fun Player.onZamorakWineFermentTimer() {
        clearSoftTimer("timer.cooking_zamorak_wine_ferment")
        fermentAllUnfermented(
            unfermented = "obj.jug_unfermented_zamorak_wine",
            product = "obj.wine_of_zamorak",
            noFailLevel = 75,
        )
    }

    /**
     * When fermentation completes, every matching unfermented jug in the inventory and bank is
     * resolved. Success rolls use the player's Cooking level at completion time.
     */
    private fun Player.fermentAllUnfermented(unfermented: String, product: String, noFailLevel: Int) {
        val packInv = inv
        val bankInv = invMap.getOrPut("inv.bank")

        if (packInv.count(unfermented) == 0 && bankInv.count(unfermented) == 0) {
            return
        }

        val level = cookingLvl
        var successes = 0
        var failures = 0

        fun rollSuccess(): Boolean = level >= noFailLevel || skillSuccess(50, 256, level)

        fun processContainer(container: Inventory) {
            val eligible = container.count(unfermented)
            repeat(eligible) {
                if (invDel(container, unfermented, 1).failure) {
                    return@repeat
                }
                if (rollSuccess()) {
                    invAdd(container, product, 1)
                    successes++
                } else {
                    invAdd(container, "obj.jug_bad_wine", 1)
                    failures++
                }
            }
        }

        processContainer(packInv)
        processContainer(bankInv)

        if (successes > 0) {
            statAdvance("stat.cooking", 200.0 * successes)
        }
        if (failures > 0) {
            mes("The wine has turned bad.")
        }
    }
}
