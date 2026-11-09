package org.rsmod.content.areas.misc.warriorsguild.scripts

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.script.onEvent
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MeleeOnlyScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in MELEE_ONLY) {
            onEvent<NpcHitEvents.Modify>(npc.asRSCM(RSCMType.NPC)) {
                if (hit.type == HitType.Ranged || hit.type == HitType.Magic) hit.damage = 0
            }
        }
    }

    private companion object {
        val MELEE_ONLY =
            (1..6).flatMap { listOf("npc.warguild_cyclops$it", "npc.warguild_cyclops${it}_high") } +
                listOf("bronze", "iron", "steel", "black", "mithril", "adamant", "rune").map {
                    "npc.warguild_${it}_armour"
                }
    }
}
