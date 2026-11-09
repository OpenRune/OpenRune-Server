package org.rsmod.content.bosses.brutus

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BrutusPen : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<NpcStateEvents.Spawn>(PEN_BULL.asRSCM(RSCMType.NPC).toLong()) { npc.hideAllOps() }
    }

    companion object {
        const val PEN_BULL = "npc.cowboss_mapped"
    }
}

class BrutusPenAttack : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult =
        if (npc.isType(BrutusPen.PEN_BULL)) {
            NpcAttackValidateResult.Deny("You cannot attack this NPC right now.")
        } else {
            NpcAttackValidateResult.Pass
        }
}
