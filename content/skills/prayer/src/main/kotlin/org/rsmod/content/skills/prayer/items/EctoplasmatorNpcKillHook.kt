package org.rsmod.content.skills.prayer.items

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.params
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase

@Singleton
public class EctoplasmatorNpcKillHook @Inject constructor() : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val player = context.hero
        if (player.statBase("stat.prayer") < 40) {
            return
        }
        if (!player.inv.contains("obj.soul_wars_ectoplasmator")) {
            return
        }
        val npc = context.npc
        if (npc.visType.paramOrNull(params.spectral) != true) {
            return
        }

        val maxHp = npc.baseHitpointsLvl
        val xp = (maxHp * 20) / 100
        if (xp <= 0) {
            return
        }
        player.statAdvance("stat.prayer", xp.toDouble())
    }
}
