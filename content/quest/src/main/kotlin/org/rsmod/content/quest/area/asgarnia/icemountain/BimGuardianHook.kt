package org.rsmod.content.quest.area.asgarnia.icemountain

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook

internal class BimGuardianHook
@Inject
constructor(
    private val quest: BelowIceMountain,
    private val guardian: BimGuardian,
) : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        guardian.onKill(context.npc, context.hero, quest)
    }
}
