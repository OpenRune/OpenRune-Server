package org.rsmod.content.skills.farming

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn3
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingEquipmentScript @Inject constructor(private val equipment: FarmingEquipment) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld3(FarmingEquipment.BOUNTY) {
            mes("Your amulet of bounty has ${equipment.bountyCharges(player)} charges remaining.")
        }
        onOpWorn2(FarmingEquipment.BOUNTY) {
            mes("Your amulet of bounty has ${equipment.bountyCharges(player)} charges remaining.")
        }
        onOpHeld4(FarmingEquipment.BOUNTY) {
            if (invDel(inv, FarmingEquipment.BOUNTY, 1, slot = it.slot, strict = true).success) {
                VarPlayerIntMapSetter.set(player, FarmingEquipment.BOUNTY_USES, 0)
                mes("You break the amulet of bounty.")
            }
        }
    }
}

class FarmingCapeScript @Inject constructor(
    private val teleportValidator: PlayerTeleportValidator,
    private val areaChecker: AreaChecker,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (cape in CAPES) {
            onOpHeld3(cape) { teleport(cape, inv, it.slot) }
            onOpWorn3(cape) { teleport(cape, player.worn, Wearpos.Back.slot) }
        }
    }

    private suspend fun ProtectedAccess.teleport(cape: String, container: Inventory, slot: Int) {
        val selected = container[slot] ?: return
        if (!selected.isType(cape) || actionDelay > mapClock) return
        if (vars[VISITED_KOUREND] == 0) {
            mes("You must travel to Great Kourend before you can use this teleport.")
            return
        }
        val denial = teleportValidator.validate(player, TeleportType.Standard, areaChecker)
        if (denial != null) {
            player.clearMapFlag()
            player.mes(denial, ChatType.Engine)
            return
        }
        actionDelay = mapClock + 5
        player.clearMapFlag()
        anim("seq.human_castteleport")
        spotanim("spotanim.teleport_casting", height = 92)
        soundSynth("synth.teleport_all")
        delay(4)
        if (container[slot] == selected && vars[VISITED_KOUREND] != 0) {
            telejump(FarmingGuild.CENTRAL_ARRIVAL, TeleportType.Standard)
        }
        resetAnim()
    }

    private companion object {
        private val CAPES = listOf("obj.skillcape_farming", "obj.skillcape_farming_trimmed")
        private const val VISITED_KOUREND = "varbit.zeah_playerhasvisited"
    }
}
