package org.rsmod.content.areas.wilderness

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.game.inv.isType

public class ForinthrySurgeKillHook @Inject constructor() : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val npcName = RSCM.getReverseMapping(RSCMType.NPC, context.npc.visType.id) ?: return
        if (npcName != "npc.revenant_maledictus") {
            return
        }

        val player = context.hero
        if (player.worn[Wearpos.Front.slot]?.isType("obj.amulet_of_avarice") != true) {
            return
        }

        player.applyForinthrySurge()
    }
}
