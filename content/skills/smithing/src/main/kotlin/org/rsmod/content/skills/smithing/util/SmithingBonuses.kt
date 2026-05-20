package org.rsmod.content.skills.smithing.util

import org.rsmod.api.table.smithing.SmithingBarsRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid
import kotlin.math.abs
import kotlin.random.Random

object SmithingBonuses {
    const val SMITHING_CATALYST = "obj.smithing_catalyst"
    private const val COAL = "obj.coal"
    private const val ICE_SMITHING_GLOVES = "obj.smithing_uniform_gloves_ice"

    private val VARROCK_ARMOUR_BY_TIER = listOf(
        "obj.varrock_armour_easy", "obj.varrock_armour_medium",
        "obj.varrock_armour_hard", "obj.varrock_armour_elite"
    )

    private val EDGEVILLE_FURNACE = CoordGrid(0, 48, 54, 33, 40)

    fun isEdgevilleFurnace(locInternal: String, coords: CoordGrid): Boolean {
        if (locInternal.contains("edgeville", ignoreCase = true)) {
            return true
        }
        if (coords.level != EDGEVILLE_FURNACE.level) {
            return false
        }
        return abs(coords.x - EDGEVILLE_FURNACE.x) <= 3 && abs(coords.z - EDGEVILLE_FURNACE.z) <= 3
    }

    fun Player.varrockArmourTier(): Int? {
        for ((index, armour) in VARROCK_ARMOUR_BY_TIER.withIndex()) {
            if (armour in worn) {
                return index + 1
            }
        }
        return null
    }

    fun maxBarLevelForVarrockTier(tier: Int): Int = when (tier) {
        1 -> 30 // steel
        2 -> 50 // mithril
        3 -> 70 // adamantite
        else -> Int.MAX_VALUE
    }

    fun isBarEligibleForVarrockArmour(bar: SmithingBarsRow, tier: Int): Boolean =
        bar.level <= maxBarLevelForVarrockTier(tier)

    fun rollVarrockDoubleBar(): Boolean = Random.nextInt(100) < 10

    fun Player.wearingIceSmithingGloves(): Boolean = ICE_SMITHING_GLOVES in worn

    fun Inventory.hasSmithingCatalyst(): Boolean = contains(SMITHING_CATALYST)

    fun requiresCoal(bar: SmithingBarsRow): Boolean =
        bar.inputSecondary?.internalName == COAL && (bar.inputSecondaryAmt ?: 0) > 0

    /**
     * Half coal and double XP at regular furnaces when wearing Smith's gloves (i) or when a
     * [SMITHING_CATALYST] is in the inventory.
     */
    fun hasCoalSmeltBoost(
        player: Player,
        inventory: Inventory,
        bar: SmithingBarsRow,
        regularFurnace: Boolean,
    ): Boolean = regularFurnace &&
        requiresCoal(bar) && (player.wearingIceSmithingGloves() || inventory.hasSmithingCatalyst())

    /** Consumable catalyst is used only when gloves are not providing the same effect. */
    fun shouldConsumeSmithingCatalyst(
        player: Player,
        inventory: Inventory,
        bar: SmithingBarsRow,
        regularFurnace: Boolean,
    ): Boolean = regularFurnace && requiresCoal(bar) &&
        inventory.hasSmithingCatalyst() && !player.wearingIceSmithingGloves()

    fun effectiveCoalAmount(
        player: Player,
        inventory: Inventory,
        bar: SmithingBarsRow,
        regularFurnace: Boolean = true,
    ): Int {
        val secondaryAmt = bar.inputSecondaryAmt ?: 0
        if (!hasCoalSmeltBoost(player, inventory, bar, regularFurnace)) {
            return secondaryAmt
        }
        return (secondaryAmt / 2).coerceAtLeast(1)
    }
}
