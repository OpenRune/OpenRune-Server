package org.rsmod.content.interfaces.skill.guides

import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.PlayerSkillXP
import org.rsmod.api.player.ui.PlayerInterfaceUpdates
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.table.StatComponentsRow
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.stat.PlayerSkillXPTable
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private sealed interface StatGuideMenuChoice {
    data object Guide : StatGuideMenuChoice
    data object SetLevel : StatGuideMenuChoice
}

class SkillGuideEvents @Inject constructor(
    private val eventBus: EventBus,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {

    @OptIn(InternalApi::class)
    override fun ScriptContext.startup() {
        StatComponentsRow.all().forEach { row ->
            onIfOverlayButton(row.component) {
                ifClose()

                protectedAccess.launch(player) {
                    if (!player.modLevel.hasAccessTo("modlevel.admin")) {
                        openStatGuide(row.bit)
                        return@launch
                    }

                    when (choice2("Guide", StatGuideMenuChoice.Guide, "Set Level", StatGuideMenuChoice.SetLevel)) {
                        StatGuideMenuChoice.Guide -> openStatGuide(row.bit)
                        StatGuideMenuChoice.SetLevel -> setLevel(row)
                    }
                }
            }
        }


        onIfOverlayButton("component.skill_guide:close") {
            player.ifCloseOverlay("interface.skill_guide", eventBus)
        }

        onIfOverlayButton("component.skill_guide_v2:close") {
            player.ifCloseOverlay("interface.skill_guide_v2", eventBus)
        }
    }

    @OptIn(InternalApi::class)
    private suspend fun ProtectedAccess.setLevel(row: StatComponentsRow) {
        val stat = row.stat.internalName
        val targetLevel = countDialog("Enter a level for ${row.stat.displayName} (1-99)").coerceIn(1, 99)

        val currentLevel = stat(stat)
        val levelDelta = targetLevel - currentLevel

        player.statMap.setCurrentLevel(stat, player.statMap.getBaseLevel(stat))
        player.statMap.setXP(stat, PlayerSkillXPTable.getXPFromLevel(targetLevel))
        player.statMap.setBaseLevel(stat, targetLevel.toByte())

        when {
            levelDelta > 0 -> statAdd(stat, constant = levelDelta,percent = 0)
            levelDelta < 0 -> statSub(stat, constant = -levelDelta,percent = 0)
        }

        player.appearance.combatLevel = PlayerSkillXP.calculateCombatLevel(player)
        PlayerInterfaceUpdates.updateCombatLevel(player)
    }

    private fun ProtectedAccess.openStatGuide(skillGuideBit: Int) {
        player.openSkillGuide(skillGuideBit, player.vars["varp.skill_guide_v2"] != 0)
    }

    private fun Player.openSkillGuide(
        skillGuideBit: Int,
        useV2: Boolean,
        sectionVar: Int = 0,
    ) = if (useV2) {
        openSkillGuideV2(skillGuideBit, sectionVar)
    } else {
        openSkillGuideV1(skillGuideBit)
    }

    private fun Player.openSkillGuideV1(skillGuideBit: Int) {
        ifOpenOverlay("interface.skill_guide", eventBus)
        ifSetEvents("component.skill_guide:icons", 0..99)
        runClientScript(9340, skillGuideBit, 0, 0, 0)
    }

    private fun Player.openSkillGuideV2(
        skillGuideBit: Int,
        sectionVar: Int,
    ) {
        ifOpenOverlay("interface.skill_guide_v2", eventBus)
        ifSetEvents("component.skill_guide_v2:tabs", 0..200, IfEvent.Op1)
        runClientScript(1902, skillGuideBit, sectionVar)
    }
}
