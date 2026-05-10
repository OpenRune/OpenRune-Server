package org.rsmod.content.skills.cooking

import dev.openrune.ServerCacheManager
import org.rsmod.api.player.back
import org.rsmod.api.player.hat
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

private val cookingOutfits = setOf(
    "obj.skillcape_cooking",
    "obj.skillcape_cooking_trimmed",
    "obj.skillcape_cooking_hood"
)

private val varrockDiaryBody = setOf(
    "obj.varrock_armour_hard",
    "obj.varrock_armour_elite",)

private fun InvObj?.hasItemContent(content: String): Boolean {
    val obj = this ?: return false
    val type = ServerCacheManager.getItem(obj.id) ?: return false
    return type.isContentType(content)
}

private fun Player.wearingMaxCapeOrHood(): Boolean =
    back.hasItemContent("content.max_cape") || hat.hasItemContent("content.max_hood")

internal fun Player.wearingChefsHat(): Boolean = "obj.chefs_hat" in worn || "obj.chefs_hat_gold" in worn

internal fun Player.hasGuildEntryOutfit(): Boolean {
    if (wearingChefsHat()) return true
    if (cookingOutfits.any { it in worn }) return true
    if (wearingMaxCapeOrHood()) return true
    if (varrockDiaryBody.any { it in worn }) return true
    return false
}

internal fun Player.ownsCookingSkillcape(): Boolean =
    "obj.skillcape_cooking" in inv ||
        "obj.skillcape_cooking_trimmed" in inv ||
        "obj.skillcape_cooking" in worn ||
        "obj.skillcape_cooking_trimmed" in worn
