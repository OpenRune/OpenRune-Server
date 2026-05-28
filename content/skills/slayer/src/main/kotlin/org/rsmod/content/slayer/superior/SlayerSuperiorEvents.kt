package org.rsmod.content.slayer.superior

import jakarta.inject.Inject
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SlayerSuperiorEvents @Inject constructor(
    private val superiors: SlayerSuperiorManager,
    private val npcRepo: NpcRepository,
) : PluginScript(), PlayerPostTickHook {
    override fun ScriptContext.startup() {
        onPlayerLogout { superiors.onPlayerLogout(player, npcRepo) }
    }

    override fun onPostTick(player: Player) {
        superiors.tickOwnedSuperiors(player, npcRepo)
    }
}
