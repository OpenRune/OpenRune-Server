package org.rsmod.content.skills.smithing.smelting

import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.smithing.SmithingBarsRow
import org.rsmod.content.skills.smithing.util.SmithingBonuses.hasCoalSmeltBoost
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

object SmithingSmeltXp {
    private const val GOLD_BAR = "obj.gold_bar"
    private const val BLURITE_BAR = "obj.blurite_bar"

    fun resolve(
        player: Player,
        inventory: Inventory,
        bar: SmithingBarsRow,
        isSuperHeat: Boolean,
        xpMods: XpModifiers,
        regularFurnace: Boolean = true,
    ): Double {
        val alternate = bar.smithxpalternate
        val baseXp = when {
            alternate == null -> bar.smeltxp
            bar.output.internalName == BLURITE_BAR && isSuperHeat -> alternate
            bar.output.internalName == GOLD_BAR && player.hasGoldsmithSmeltBonus() -> alternate
            else -> bar.smeltxp
        }

        var xp = baseXp.toDouble() * xpMods.get(player, "stat.smithing")

        if (hasCoalSmeltBoost(player, inventory, bar, regularFurnace)) {
            xp *= 2
        }

        return xp
    }

    private fun Player.hasGoldsmithSmeltBonus(): Boolean =
        "obj.gauntlets_of_goldsmithing" in worn ||
            "obj.skillcape_smithing" in worn ||
            "obj.skillcape_smithing_trimmed" in worn ||
            "obj.skillcape_max" in worn
}
