package org.alter.interfaces.skillguide

import dev.openrune.definition.type.widget.IfEvent
import org.alter.api.CommonClientScripts
import org.alter.api.InterfaceDestination
import org.alter.api.ext.*
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ButtonClickEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.interfaces.ifCloseModal
import org.alter.interfaces.ifCloseOverlay
import org.alter.interfaces.ifOpenMainModal
import org.alter.interfaces.ifOpenOverlay
import org.alter.interfaces.ifSetEvents
import org.generated.tables.StatComponentsRow

class SkillGuidEvents : PluginEvent() {

    override fun init() {
        StatComponentsRow.all().forEach { row ->

            on<ButtonClickEvent> {
                where { component.combinedId == row.component }
                then {

                    if (!player.lock.canInterfaceInteract()) return@then

                    val optionSkillGuide = player.getVarbit("varbits.option_skill_guide")

                    player.setVarbit("varbits.skill_guide_skill", row.bit)

                    if (optionSkillGuide == 0) {
                        player.setVarbit("varbits.skill_guide_subsection", 0)
                        player.ifOpenMainModal("interfaces.skill_guide",-1,-1)
                    } else {
                        player.ifOpenOverlay("interfaces.skill_guide_v2")
                        player.ifSetEvents("components.skill_guide_v2:tabs", 0..200, IfEvent.Op1)
                    }
                }
            }
        }

        onButton("components.skill_guide_v2:close") {
            player.ifCloseOverlay("interfaces.skill_guide_v2")
        }

    }
}
