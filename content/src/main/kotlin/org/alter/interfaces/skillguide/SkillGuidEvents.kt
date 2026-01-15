package org.alter.interfaces.skillguide

import dev.openrune.definition.type.widget.IfEvent
import org.alter.api.CommonClientScripts
import org.alter.api.InterfaceDestination
import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.model.entity.Player
import org.alter.game.model.priv.Privilege
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

                    val isAdmin = world.privileges.isEligible(player.privilege, Privilege.ADMIN_POWER)

                    if (!isAdmin) {
                        openGuide(player, row.bit)
                        return@then
                    }

                    player.queue {
                        when (options(player, "Guide", "Set Level")) {
                            1 -> openGuide(player, row.bit)
                            2 -> {
                                val skillName = Skills.getSkillName(row.stat)
                                val level = inputInt(player, "Enter a level for $skillName (1-99)").coerceIn(1, 99)

                                player.getSkills().apply {
                                    setBaseLevel(row.stat, level)
                                    setCurrentLevel(row.stat, level)
                                }
                            }
                        }
                    }
                }
            }
        }

        onButton("components.skill_guide:close") {
            player.ifCloseOverlay("interfaces.skill_guide")
        }

        onButton("components.skill_guide_v2:close") {
            player.ifCloseOverlay("interfaces.skill_guide_v2")
        }

    }

    fun openGuide(player: Player, bit : Int) {
        player.setVarbit("varbits.option_skill_guide",1)
        val optionSkillGuide = player.getVarbit("varbits.option_skill_guide")

        if (optionSkillGuide == 0) {
            player.setVarbit("varbits.skill_guide_skill", bit)
            player.setVarbit("varbits.skill_guide_subsection", 0)
            player.ifOpenOverlay("interfaces.skill_guide")
        } else {
            player.ifOpenOverlay("interfaces.skill_guide_v2")
            player.runClientScript(CommonClientScripts.SKILL_GUIDE,bit,0)
            player.ifSetEvents("components.skill_guide_v2:tabs", 0..200, IfEvent.Op1)
        }
    }

}
