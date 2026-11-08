package org.rsmod.api.player.events.skilling

import dev.openrune.types.ItemServerType
import org.rsmod.api.table.ComboruneRecipeRow
import org.rsmod.api.table.cooking.CookingAlesRow
import org.rsmod.api.table.cooking.CookingFoodsRow
import org.rsmod.api.table.herblore.HerbloreBarbarianMixesRow
import org.rsmod.api.table.herblore.HerbloreCleaningRow
import org.rsmod.api.table.herblore.HerbloreCrushingRow
import org.rsmod.api.table.herblore.HerbloreFinishedRow
import org.rsmod.api.table.herblore.HerbloreSwampTarRow
import org.rsmod.api.table.mining.MiningRocksRow
import org.rsmod.api.table.runecrafting.RunecraftingRunesRow
import org.rsmod.api.table.smithing.SmithingBarsRow
import org.rsmod.api.table.smithing.SmithingCannonBallsRow
import org.rsmod.api.table.smithing.SmithingCrystalSingingRow
import org.rsmod.api.table.smithing.SmithingDragonForgeRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.BoundLocInfo

public sealed class SkillingProductSource {
    /** The axe, pickaxe or other tool the action was done with, if it needs one. */
    public open val tool: ItemServerType?
        get() = null

    public data class Mining(
        public val rock: BoundLocInfo,
        public val rockData: MiningRocksRow,
        override val tool: ItemServerType,
    ) : SkillingProductSource()

    public data class Woodcutting(
        public val tree: BoundLocInfo,
        public val productType: ItemServerType,
        override val tool: ItemServerType,
    ) : SkillingProductSource()

    public data class Fishing(
        public val productType: ItemServerType,
    ) : SkillingProductSource()

    public data class Cooking(
        public val food: CookingFoodsRow,
        public val burnt: Boolean,
    ) : SkillingProductSource()

    /** [mature] is true when the barrel held the rare mature batch. */
    public data class Brewing(
        public val ale: CookingAlesRow,
        public val mature: Boolean,
    ) : SkillingProductSource()

    public data class HerbCleaning(public val herb: HerbloreCleaningRow) : SkillingProductSource()

    public data class HerbCrushing(public val recipe: HerbloreCrushingRow) : SkillingProductSource()

    public data class BarbarianMix(public val mix: HerbloreBarbarianMixesRow) :
        SkillingProductSource()

    /** [extraDose] is true when the amulet of chemistry added a dose. */
    public data class FinishedPotion(
        public val potion: HerbloreFinishedRow,
        public val extraDose: Boolean,
    ) : SkillingProductSource()

    public data class SwampTar(public val recipe: HerbloreSwampTarRow) : SkillingProductSource()

    public data class Anvil(
        public val bar: SmithingBarsRow,
        public val product: ItemServerType,
        public val barCount: Int,
    ) : SkillingProductSource()

    public data class Smelting(public val bar: SmithingBarsRow) : SkillingProductSource()

    public data class CannonballSmelting(public val ball: SmithingCannonBallsRow) :
        SkillingProductSource()

    public data class DragonForge(public val recipe: SmithingDragonForgeRow) :
        SkillingProductSource()

    public data class CrystalSinging(public val item: SmithingCrystalSingingRow) :
        SkillingProductSource()

    /** [ourania] is true at the altar that rolls the rune type per essence. */
    public data class Runecrafting(
        public val rune: RunecraftingRunesRow,
        public val ourania: Boolean = false,
    ) : SkillingProductSource()

    public data class CombinationRunecrafting(public val recipe: ComboruneRecipeRow) :
        SkillingProductSource()

    public data class StallThieving(public val stall: BoundLocInfo) : SkillingProductSource()

    public data class Pickpocketing(public val npc: Npc) : SkillingProductSource()

    public data class CoinPouchOpening(public val pouch: String) : SkillingProductSource()

    public data object TiaraBinding : SkillingProductSource()

    /** [section] is the recipe group, such as "Spinning". [failed] marks a ruined craft. */
    public data class Crafting(
        public val section: String,
        public val failed: Boolean = false,
    ) : SkillingProductSource()

    public data object GodswordBlade : SkillingProductSource()

    public data object GodswordHilt : SkillingProductSource()

    public data object GraniteCannonballs : SkillingProductSource()
}
