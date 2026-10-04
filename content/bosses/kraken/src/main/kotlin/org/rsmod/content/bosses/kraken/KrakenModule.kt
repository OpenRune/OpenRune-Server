package org.rsmod.content.bosses.kraken

import jakarta.inject.Inject
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

class KrakenModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcAttackValidateHook>(KrakenAttackHook::class.java)
    }
}

internal class KrakenAttackHook @Inject constructor(private val controller: KrakenController) : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (KrakenKind.entries.none { it.activeId == npc.type.id }) return NpcAttackValidateResult.Pass
        if (!controller.allowed(player)) return NpcAttackValidateResult.Deny("You need level 87 Slayer and a cave kraken task.")
        if (controller.owner(npc)?.let { it !== player } == true)
            return NpcAttackValidateResult.Deny("Someone else is already fighting this kraken.")
        return NpcAttackValidateResult.BypassSingleWayPvnRestriction
    }
}
