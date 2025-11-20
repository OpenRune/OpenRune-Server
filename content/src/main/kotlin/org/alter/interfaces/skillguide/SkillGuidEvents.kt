package org.alter.interfaces.skillguide

import org.alter.api.ClientScript
import org.alter.api.CommonClientScripts
import org.alter.api.InterfaceDestination
import org.alter.api.ext.*
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ButtonClickEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.generated.tables.StatComponentsRow

class SkillGuidEvents : PluginEvent() {

    override fun init() {
        StatComponentsRow.all().forEach { row ->

            on<ButtonClickEvent> {
                where { component.combinedId == row.component }
                then {

                    if (!player.lock.canInterfaceInteract()) return@then

                    val optionSkillGuide = player.getVarbit("varbits.option_skill_guide")

                    if (optionSkillGuide == 0) {
                        player.setVarbit("varbits.skill_guide_subsection", 0)
                        player.setVarbit("varbits.skill_guide_skill", row.bit)
                        player.setInterfaceUnderlay(color = -1, transparency = -1)
                        player.setInterfaceEvents("components.skill_guide:categories", -1, -1,0)
                        player.openInterface("interfaces.skill_guide", InterfaceDestination.MAIN_SCREEN)
                    } else {
                        player.runClientScript(CommonClientScripts.MAIN_MODAL_OPEN, -1, -3)
                        player.openInterface("interfaces.skill_guide_v2", dest = InterfaceDestination.MAIN_SCREEN, isModal = true)
                        player.runClientScript(ClientScript("skill_guide_v2_init"), row.bit, 0)
                        player.setInterfaceEvents("components.skill_guide_v2:tabs", 0..200, InterfaceEvent.ClickOp1)
                    }
                }
            }
        }

        onButton("components.skill_guide_v2:close") {
            player.closeInterface(InterfaceDestination.MAIN_SCREEN)
        }

    }
}
