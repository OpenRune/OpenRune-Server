package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc2
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ZulrahRecoveryScript
@Inject
constructor(private val recovery: ZulrahDeathRecovery) : PluginScript() {
    override fun ScriptContext.startup() {
        for (priest in PRIESTS) {
            onOpNpc1(priest) { talk(it.npc) }
            val type = checkNotNull(ServerCacheManager.getNpc(priest.asRSCM(RSCMType.NPC)))
            if (type.actions.getOpOrNull(1).equals("Collect", ignoreCase = true)) {
                onOpNpc2(priest) { recovery.reclaim(player) }
            }
        }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) = startDialogue(npc) {
        if (!recovery.hasItems(player)) {
            chatNpc(neutral, "If Zulrah defeats you, I will hold your lost items here.")
            chatNpc(neutral, "Reclaim them before another unsafe death, or they will be lost.")
        } else {
            chatNpc(neutral, "I have your lost items. You can reclaim them for free.")
            if (choice2("Reclaim my items.", true, "Not now.", false)) {
                recovery.reclaim(player)
            }
        }
    }

    internal companion object {
        val PRIESTS = listOf(
            "npc.snakeboss_priest",
            "npc.snakeboss_priest_1op",
            "npc.snakeboss_priest_2ops",
        )
    }
}
