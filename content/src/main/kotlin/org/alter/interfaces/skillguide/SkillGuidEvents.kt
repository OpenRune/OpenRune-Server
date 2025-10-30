package org.alter.interfaces.skillguide

import org.alter.api.ClientScript
import org.alter.api.CommonClientScripts
import org.alter.api.InterfaceDestination
import org.alter.api.ext.*
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ButtonClickEvent
import org.alter.game.util.DbHelper.Companion.table
import org.alter.game.util.column
import org.alter.game.util.vars.ComponentType
import org.alter.game.util.vars.IntType
import org.alter.rscm.RSCM.asRSCM

class SkillGuidEvents : PluginEvent() {

    override fun init() {
        table("tables.stat_components").forEach { row ->
            val componentID = row.column("columns.stat_components:component", ComponentType)
            val skillBit = row.column("columns.stat_components:bit", IntType)

            on<ButtonClickEvent> {
                where { component.combinedId == componentID }
                then {

                    if (!player.lock.canInterfaceInteract()) return@then

                    val optionSkillGuide = player.getVarbit("varbits.option_skill_guide")

                    if (optionSkillGuide == 0) {
                        player.setVarbit("varbits.skill_guide_subsection", 0)
                        player.setVarbit("varbits.skill_guide_skill", skillBit)
                        player.setInterfaceUnderlay(color = -1, transparency = -1)
                        player.setInterfaceEvents("components.skill_guide:categories", -1, -1,0)
                        player.openInterface("interfaces.skill_guide", InterfaceDestination.MAIN_SCREEN)
                    } else {
                        player.runClientScript(CommonClientScripts.MAIN_MODAL_OPEN, -1, -3)
                        player.openInterface("interfaces.skill_guide_v2", dest = InterfaceDestination.MAIN_SCREEN, isModal = true)
                        player.runClientScript(ClientScript("skill_guide_v2_init"), skillBit, 0)
                        player.setInterfaceEvents("components.skill_guide_v2:tabs", 0..200, InterfaceEvent.ClickOp1)
                    }
                }
            }
        }

        on<ButtonClickEvent> {
            where { component.combinedId == "components.skill_guide_v2:close".asRSCM() }
            then {
                player.closeInterface(InterfaceDestination.MAIN_SCREEN)
            }
        }

    }
}
