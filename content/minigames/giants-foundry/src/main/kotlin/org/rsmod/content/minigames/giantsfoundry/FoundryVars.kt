package org.rsmod.content.minigames.giantsfoundry

import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.player.vars.typeCoordVarp
import org.rsmod.game.entity.Player

internal var Player.sleepingGiants by intVarBit("varbit.sleeping_giants")

internal var Player.sleepingGiantsTutorial by intVarBit("varbit.sleeping_giants_tutorial")

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

internal var Player.foundryReputation by intVarp("varp.giants_foundry_reward_shop_points")

// UNVERIFIED: that a logout is what loses the time; one sword fits it, and so does a timer that
// only starts on a commission given after login (plugin measurement).
internal var Player.commissionStart by intVarp("varp.giants_foundry_commission_start")
internal var Player.bestTime by intVarp("varp.giants_foundry_best_time")
internal var Player.bestQuality by intVarp("varp.giants_foundry_best_quality")
internal var Player.swordsMade by intVarp("varp.giants_foundry_swords_made")

internal var Player.sweetSpotsOffered by intVarBit("varbit.giants_foundry_sweetspots_offered")

/** The section of the last sweet spot offered, plus one; 0 none yet. */
internal var Player.sweetSpotsLastSection by
    intVarBit("varbit.giants_foundry_sweetspots_last_section")

internal var Player.workStation by intVarBit("varbit.giants_foundry_work_station")
internal var Player.workHeatTicks by intVarBit("varbit.giants_foundry_work_heat_ticks")
internal var Player.workSweetSpotArmed by boolVarBit("varbit.giants_foundry_work_sweetspot_armed")
internal var Player.workOverworked by boolVarBit("varbit.giants_foundry_work_overworked")

/** Armed by the click that started the work: the first action pays no bonus. */
internal var Player.workSweetSpotVoid by boolVarBit("varbit.giants_foundry_work_sweetspot_void")
internal var Player.workOrigin by typeCoordVarp("varp.giants_foundry_work_origin")

/** Tool action deadline; while heating, the re-click tick whose animation must be suppressed. */
internal var Player.workNextAction by intVarp("varp.giants_foundry_work_next_action")
internal var Player.sweetSpotUntil by intVarp("varp.giants_foundry_sweetspot_until")

internal var Player.skillmultiPreviousSelection by intVarp("varp.skillmulti_previousselection")

internal val METALS = listOf("bronze", "iron", "steel", "mithril", "adamant", "rune")

internal fun metalVarbit(metal: String): String = "varbit.giants_foundry_${metal}_count"

internal fun seedVarbit(section: Int): String = "varbit.giants_foundry_seed_$section"
