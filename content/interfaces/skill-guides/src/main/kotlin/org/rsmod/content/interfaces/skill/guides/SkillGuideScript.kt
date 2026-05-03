package org.rsmod.content.interfaces.skill.guides

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.enums.SkillGuideEnums.skill_guide_button_vars
import org.rsmod.api.enums.SkillGuideEnums.skill_guide_section_vars
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SkillGuideScript
@Inject
constructor(private val eventBus: EventBus, private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    override fun ScriptContext.startup() {
        val mappedTabButtons = skill_guide_button_vars.filterValuesNotNull().map { RSCM.getReverseMapping(RSCMType.COMPONENT,it.key.packed) to it.value }
        for ((button, varbit) in mappedTabButtons) {
            onIfOverlayButton(button) { player.selectGuide(varbit) }
        }

        val mappedSubsections = skill_guide_section_vars.filterValuesNotNull().map { RSCM.getReverseMapping(RSCMType.COMPONENT,it.key.packed) to it.value }
        for ((button, varbit) in mappedSubsections) {
            onIfOverlayButton(button) { player.changeSubsection(varbit) }
        }

        onIfOverlayButton("component.skill_guide:close") { player.closeGuide() }
    }

    private fun Player.selectGuide(guideVarBit: Int) {
        ifClose(eventBus)
        protectedAccess.launch(this) { openGuide(guideVarBit, sectionVar = 0) }
    }

    private fun Player.openGuide(skillVar: Int, sectionVar: Int) {
        selectedSkill = skillVar
        selectedSubsection = sectionVar
        ifOpenOverlay("interface.skill_guide", eventBus)
        // Note: This is for the "Check _" left-click options on subsection entries. As of the
        // moment of writing this, only construction handles this server-side. (Magic handles it
        // entirely through cs2) We do not currently have the construction data in order to send
        // the corresponding message for these ops, so we stick to never enabling them.
        ifSetEvents("component.skill_guide:icons", 0..99)
    }

    private fun Player.changeSubsection(sectionVar: Int) {
        openGuide(selectedSkill, sectionVar)
    }

    private fun Player.closeGuide() {
        ifCloseSub("interface.skill_guide", eventBus)
    }
}

private var Player.selectedSkill by intVarBit("varbit.skill_guide_skill")
private var Player.selectedSubsection by intVarBit("varbit.skill_guide_subsection")
