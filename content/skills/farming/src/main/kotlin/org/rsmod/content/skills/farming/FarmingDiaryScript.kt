package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.enums.FarmingDiaryEnums
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onEvent
import org.rsmod.api.shops.ShopOpenedEvent
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingDiaryScript @Inject constructor(private val siteAccess: FarmingSiteAccess) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<FarmingPlantEvent> { complete(player, patch, crop, Action.PLANT) }
        onEvent<FarmingCheckHealthEvent> { complete(player, patch, crop, Action.CHECK_HEALTH) }
        onEvent<FarmingHarvestEvent> {
            if (count > 0 && obj == crop.produce) complete(player, patch, crop, Action.HARVEST)
        }
        onEvent<FarmingScarecrowPlacedEvent> {
            if (patch.kind == PatchKind.FLOWER) complete(player, patch, Action.PLACE_SCARECROW)
        }
        onEvent<FarmingWhiteTreeHarvestEvent> {
            complete(player, "varrock_palace_white_tree", null, Action.PICK_WHITE_FRUIT)
        }
        onEvent<FarmingProtectionEvent> { complete(player, patch, crop, Action.PROTECT) }
        onEvent<FarmingGuildEntryEvent> {
            complete(player, "farming_guild_main_entrance", null, Action.ENTER_GUILD)
        }
        onEvent<FarmingTithePlantEvent> { complete(player, "tithe_farm", seed, Action.PLANT_TITHE) }
        onEvent<FarmingFertileSoilEvent> {
            if (patch.kind == PatchKind.HERB) complete(player, patch, Action.FERTILE_SOIL)
        }
        onEvent<ShopOpenedEvent> {
            for (rule in RULES) {
                if (rule.action == Action.BROWSE_SHOP && rule.shop == inventory.id) complete(player, rule)
            }
        }
    }

    private fun complete(player: Player, patch: PatchDef, crop: Crop, action: Action) {
        if (crop.kind != patch.kind || siteAccess.denial(player, patch) != null) return
        complete(player, patch.site, crop.seed, action)
    }

    private fun complete(player: Player, patch: PatchDef, action: Action) {
        if (siteAccess.denial(player, patch) == null) complete(player, patch.site, null, action)
    }

    private fun complete(player: Player, site: String, seed: String?, action: Action) {
        for (rule in RULES) {
            if (rule.action != action || rule.site != site ||
                (rule.seed != null && rule.seed != seed)
            ) continue
            complete(player, rule)
        }
    }

    private fun complete(player: Player, rule: Rule) {
        val previous = player.vars[rule.variable]
        val next = previous or rule.mask
        if (next != previous) VarPlayerIntMapSetter.set(player, rule.variable, next)
    }

    private enum class Action {
        PLANT, CHECK_HEALTH, HARVEST, PLACE_SCARECROW, PICK_WHITE_FRUIT, PROTECT,
        ENTER_GUILD, PLANT_TITHE, FERTILE_SOIL, BROWSE_SHOP,
    }

    private data class Rule(
        val action: Action,
        val site: String,
        val seed: String?,
        val variable: String,
        val mask: Int,
    ) {
        val shop = if (action == Action.BROWSE_SHOP) site.asRSCM(RSCMType.INV) else -1
    }

    private companion object {
        val RULES: List<Rule> by lazy {
            FarmingDiaryEnums.farming_diary_sites.filterValuesNotNull().map { (id, site) ->
                Rule(
                    action = Action.valueOf(checkNotNull(FarmingDiaryEnums.farming_diary_actions[id])),
                    site = site,
                    seed = FarmingDiaryEnums.farming_diary_seeds[id]?.internalName,
                    variable = checkNotNull(FarmingDiaryEnums.farming_diary_variables[id]),
                    mask = checkNotNull(FarmingDiaryEnums.farming_diary_masks[id]),
                )
            }
        }
    }
}
