package org.rsmod.content.skills.magic.arceuus

import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.npc.owner.isSpawnOwnedBy
import org.rsmod.api.player.stat.statAdvance

internal class ReanimationKillHook : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val head = ReanimatedHead.byNpcId[context.npc.id] ?: return
        if (!context.npc.isSpawnOwnedBy(context.hero)) return
        context.hero.statAdvance("stat.prayer", head.prayerXp)
    }
}
