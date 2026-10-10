package org.rsmod.content.areas.wilderness

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.game.inv.isType

public class ForinthrySurgeKillHook @Inject constructor() : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        if (context.npc.type.id != "npc.wild_cave_superior".asRSCM() || context.npc.damageContributions.isEmpty) {
            return
        }

        val player = context.hero
        if (player.worn[Wearpos.Front.slot]?.isType("obj.wild_cave_amulet") != true) {
            return
        }

        player.applyForinthrySurge()
    }
}
