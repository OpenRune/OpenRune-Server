package org.rsmod.content.minigames.giantsfoundry

import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

internal var Player.sleepingGiants by intVarBit("varbit.sleeping_giants")

/** Sleeping Giants' sword-making steps; see [SleepingGiants]. */
internal var Player.sleepingGiantsTutorial by intVarBit("varbit.sleeping_giants_tutorial")

/** 0-1 broken, 2 repaired (the `giants_foundry_*_quest_multi` locs). */
internal var Player.repairedHammer by intVarBit("varbit.sleeping_giants_repair_hammer")
internal var Player.repairedGrindstone by intVarBit("varbit.sleeping_giants_repair_grind")
internal var Player.repairedPolishingWheel by intVarBit("varbit.sleeping_giants_repair_polish")

internal var Player.commissionWord1 by intVarBit("varbit.giants_foundry_commission_word_1")
internal var Player.commissionWord2 by intVarBit("varbit.giants_foundry_commission_word_2")

internal var Player.mouldTab by intVarBit("varbit.giants_foundry_mould_current_tab")
internal var Player.mouldForte by intVarBit("varbit.giants_foundry_mould_selected_ricasso")
internal var Player.mouldBlade by intVarBit("varbit.giants_foundry_mould_selected_blade")
internal var Player.mouldTip by intVarBit("varbit.giants_foundry_mould_selected_tip")
internal var Player.mouldAlloyDifficulty by intVarBit("varbit.giants_foundry_mould_alloy_dificulty")

/** 0 empty, 1 moulds set, 2 metal poured (hot), 3 cooled. */
internal var Player.mouldState by intVarBit("varbit.giants_foundry_mould_state")

/** 0 empty, 1 partially full, 2 full. */
internal var Player.crucibleState by intVarBit("varbit.giants_foundry_crucible_state")

internal var Player.preformDifficulty by intVarBit("varbit.giants_foundry_preform_dificulty")
internal var Player.preformQuality by intVarBit("varbit.giants_foundry_preform_quality")
internal var Player.preformStartQuality by intVarBit("varbit.giants_foundry_preform_start_quality")
internal var Player.preformTemperature by intVarBit("varbit.giants_foundry_preform_temperature")
internal var Player.preformCompletion by intVarBit("varbit.giants_foundry_preform_completion")
internal var Player.preformStored by boolVarBit("varbit.giants_foundry_preform_stored")

/** Foundry Reputation, shown by the reward shop. */
internal var Player.foundryReputation by intVarp("varp.giants_foundry_reward_shop_points")

internal var Player.commissionStart by intVarp("varp.giants_foundry_commission_start")
internal var Player.bestQuality by intVarp("varp.giants_foundry_best_quality")
internal var Player.bestTime by intVarp("varp.giants_foundry_best_time")
internal var Player.swordsMade by intVarp("varp.giants_foundry_swords_made")

/** Low byte: sweet spots offered this sword; next byte: section of the next one (+1, 0 none). */
internal var Player.sweetSpots by intVarp("varp.giants_foundry_sweetspots")

internal val METALS = listOf("bronze", "iron", "steel", "mithril", "adamant", "rune")

internal fun metalVarbit(metal: String): String = "varbit.giants_foundry_${metal}_count"

internal fun seedVarbit(section: Int): String = "varbit.giants_foundry_seed_$section"
