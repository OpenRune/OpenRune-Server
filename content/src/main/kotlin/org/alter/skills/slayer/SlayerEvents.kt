package org.alter.skills.slayer

import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.LoginEvent
import org.alter.game.pluginnew.event.impl.NpcClickEvent
import org.alter.interfaces.ifOpenMainModal
import org.alter.rscm.RSCM.asRSCM
import org.alter.skills.slayer.dialogue.GenericDialogue
import org.alter.skills.slayer.dialogue.TuradelDialogue
import org.generated.tables.slayer.SlayerUnlockRow

class SlayerEvents : PluginEvent() {



    override fun init() {

        spawnNpc("npcs.slayer_master_1_tureal",2931,3536)

        on<NpcClickEvent> {
            where { SlayerTaskManager.slayerMasterNpcs.contains(npc.id) }
            then {
                when(op) {
                    MenuOption.OP1 -> player.queue {
                        when(npc.id) {
                            "npcs.slayer_master_1_tureal".asRSCM() -> TuradelDialogue.start(player)
                            else -> GenericDialogue.slayerGenericDialogue(player,this,npc.id)
                        }
                    }
                    MenuOption.OP3 -> player.queue { GenericDialogue.slayerNeedAnotherAssignment(player,this,npc.id) }
                    MenuOption.OP4 -> SlayerInterfaces.openSlayerEquipment(player)
                    MenuOption.OP5 -> SlayerInterfaces.openSlayerRewards(player)
                    else -> {}
                }
            }
        }
    }


}
