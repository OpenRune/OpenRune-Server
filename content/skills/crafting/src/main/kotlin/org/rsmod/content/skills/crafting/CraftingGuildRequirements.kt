package org.rsmod.content.skills.crafting

import dev.openrune.ServerCacheManager
import org.rsmod.api.player.back
import org.rsmod.api.player.stat.baseCraftingLvl
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

private val Player.faladorHardDiaryComplete by boolVarBit("varbit.falador_diary_hard_complete")
private val Player.faladorEliteDiaryComplete by boolVarBit("varbit.falador_diary_elite_complete")

private fun InvObj?.hasItemContent(content: String): Boolean {
    val obj = this ?: return false
    val type = ServerCacheManager.getItem(obj.id) ?: return false
    return type.isContentType(content)
}

private fun Player.wearingMaxCape(): Boolean = back.hasItemContent(CraftingConstants.CONTENT_MAX_CAPE)

internal fun Player.wearingCraftingSkillcape(): Boolean = CraftingConstants.CRAFTING_SKILLCAPES.any { it in worn }

internal fun Player.wearingCraftingApron(): Boolean = CraftingConstants.GUILD_APRONS.any { it in worn }

internal fun Player.ownsCraftingSkillcape(): Boolean = CraftingConstants.CRAFTING_SKILLCAPES.any { it in inv || it in worn }

internal fun Player.ownsCraftingHood(): Boolean = CraftingConstants.CRAFTING_HOOD in inv || CraftingConstants.CRAFTING_HOOD in worn

internal fun Player.hasGuildEntryOutfit(): Boolean = wearingCraftingApron() || wearingCraftingSkillcape() || wearingMaxCape()

internal fun Player.canUseGuildBank(): Boolean = baseCraftingLvl >= CraftingConstants.MAX_CRAFTING_LEVEL || hasFaladorHardDiary() || hasFaladorEliteDiary()

internal fun Player.hasFaladorHardDiary(): Boolean = faladorHardDiaryComplete

internal fun Player.hasFaladorEliteDiary(): Boolean = faladorEliteDiaryComplete
