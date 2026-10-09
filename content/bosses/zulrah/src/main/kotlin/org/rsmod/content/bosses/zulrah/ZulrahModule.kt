package org.rsmod.content.bosses.zulrah

import jakarta.inject.Inject
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

class ZulrahModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcAttackValidateHook>(ZulrahAttackGuard::class.java)
    }
}

internal class ZulrahAttackGuard @Inject constructor(
    private val controller: ZulrahEncounterController,
) : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult =
        if (controller.canAttack(player, npc)) {
            NpcAttackValidateResult.Pass
        } else {
            NpcAttackValidateResult.Deny("You can't attack this npc.")
        }
}
