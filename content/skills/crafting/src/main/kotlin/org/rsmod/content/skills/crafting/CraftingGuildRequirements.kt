package org.rsmod.content.skills.crafting

import org.rsmod.api.player.stat.baseCraftingLvl
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.game.entity.Player

private val Player.faladorHardDiaryComplete by boolVarBit("varbit.falador_diary_hard_complete")
private val Player.faladorEliteDiaryComplete by boolVarBit("varbit.falador_diary_elite_complete")

private fun Player.wearingMaxCape(): Boolean = CraftingConstants.MAX_SKILLCAPES.any { it in worn }

internal fun Player.wearingCraftingSkillcape(): Boolean = CraftingConstants.CRAFTING_SKILLCAPES.any { it in worn }

internal fun Player.wearingCraftingApron(): Boolean = CraftingConstants.GUILD_APRONS.any { it in worn }

internal fun Player.ownsCraftingSkillcape(): Boolean = CraftingConstants.CRAFTING_SKILLCAPES.any { it in inv || it in worn }

internal fun Player.ownsCraftingHood(): Boolean = CraftingConstants.CRAFTING_HOOD in inv || CraftingConstants.CRAFTING_HOOD in worn

internal fun Player.hasGuildEntryOutfit(): Boolean = wearingCraftingApron() || wearingCraftingSkillcape() || wearingMaxCape()

internal fun Player.canUseGuildBank(): Boolean = baseCraftingLvl >= CraftingConstants.MAX_CRAFTING_LEVEL || hasFaladorHardDiary() || hasFaladorEliteDiary()

internal fun Player.hasFaladorHardDiary(): Boolean = faladorHardDiaryComplete
internal fun Player.hasFaladorEliteDiary(): Boolean = faladorEliteDiaryComplete
