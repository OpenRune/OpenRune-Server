package org.rsmod.content.bosses.barrows

import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.npc.owner.isSpawnOwnedBy
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal class BarrowsKillHook : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        context.hero.creditBarrowsKill(context.npc)
    }
}

internal fun Player.creditBarrowsKill(npc: Npc) {
    if (barrowsLooted) return
    val brother = npc.brother()
    if (brother != null) {
        if (!npc.isSpawnOwnedBy(this) || isSlain(brother)) return
        clearHintArrow()
        setSlain(brother, true)
        addPotential(brother.combatLevel)
        return
    }
    if (npc.isCryptMonster() && npc.coords.inBarrowsCrypt()) {
        addPotential(npc.type.combatLevel)
    }
}
