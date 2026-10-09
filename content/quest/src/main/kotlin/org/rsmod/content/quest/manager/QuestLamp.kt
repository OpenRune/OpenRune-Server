package org.rsmod.content.quest.manager

import dev.openrune.definition.type.widget.IfEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter

private val LampStats =
    listOf(
        "stat.attack",
        "stat.strength",
        "stat.ranged",
        "stat.magic",
        "stat.defence",
        "stat.hitpoints",
        "stat.prayer",
        "stat.agility",
        "stat.herblore",
        "stat.thieving",
        "stat.crafting",
        "stat.runecrafting",
        "stat.mining",
        "stat.smithing",
        "stat.fishing",
        "stat.cooking",
        "stat.firemaking",
        "stat.woodcutting",
        "stat.fletching",
        "stat.slayer",
        "stat.farming",
        "stat.construction",
        "stat.hunter",
        "stat.sailing",
    )

private val AllLampSkillsMask = LampStats.indices.fold(0) { mask, index -> mask or (1 shl (index + 1)) }

suspend fun ProtectedAccess.chooseLampSkill(title: String, minLevel: Int = 0): String? {
    VarPlayerIntMapSetter.set(player, "varp.if1", minLevel)
    VarPlayerIntMapSetter.set(player, "varp.if2", AllLampSkillsMask)
    ifOpenMainModal("interface.xpreward")
    ifSetText("component.xpreward:title", title)
    ifSetEvents("component.xpreward:universe", LampStats.indices, IfEvent.PauseButton)
    val input = pauseButton()
    ifClose()
    if (!input.isComponentType("component.xpreward:universe")) {
        return null
    }
    return LampStats.getOrNull(input.subcomponent)
}
