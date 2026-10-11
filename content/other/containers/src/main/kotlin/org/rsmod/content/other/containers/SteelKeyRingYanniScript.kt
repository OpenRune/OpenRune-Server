package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.table.QuestRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class SteelKeyRingYanniScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() { onOpNpc1("npc.shiloantiques") { buyReplacement() } }
    private suspend fun ProtectedAccess.buyReplacement() {
        if (!active() || !eligible(player)) { mes("Use both One Small Favour reward lamps before buying a replacement key ring."); return }
        if (SteelKeyRing.backed(player)) { mes("You already have a steel key ring."); return }
        val origin = coords
        if (!choice2("Buy a steel key ring for 500 coins.", true, "Cancel", false, title = "Yanni's antiques")) return
        if (!active() || coords != origin || !eligible(player) || SteelKeyRing.backed(player)) return
        val tx = player.invTransaction(inv) { val i = select(inv); delete(i, "obj.coins".asRSCM(), 500); add(i, SteelKeyRing.ITEM.asRSCM(), 1) }
        if (tx.failure) { mes("You need 500 coins and room for the key ring."); return }
        player.attr.remove(SteelKeyRing.CONTENTS)
        mes("Yanni sells you a new steel key ring.")
    }
    private fun ProtectedAccess.active() = player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut
    companion object {
        private val endState by lazy { QuestRow.getRow("dbrow.quest_onesmallfavour".asRSCM()).endstate }

        // The native completed state is reached after using both lamps; no guessed lamp flags.
        fun eligible(player: Player) = QuestRequirements.hasCompleted(player, "quest_onesmallfavour") || player.vars["varp.onesmallfavour"] >= endState
    }
}
