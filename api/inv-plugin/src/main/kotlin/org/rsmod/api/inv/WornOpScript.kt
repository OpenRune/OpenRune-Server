package org.rsmod.api.inv

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.enums.EquipmentEnums.equipment_tab_to_slots_map
import org.rsmod.api.player.events.interact.WornSubOpEvent
import org.rsmod.api.player.interact.WornInteractions
import org.rsmod.api.player.output.UpdateInventory.resendSlot
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfOverlayButton
import org.rsmod.api.player.ui.IfOverlaySubOpMenu
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onIfOverlaySubOpMenu
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class WornOpScript
@Inject
constructor(
    private val eventBus: EventBus,
    private val interactions: WornInteractions,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val mappedComponents = mappedComponents()
        for ((wearpos, component) in mappedComponents) {
            onIfOverlayButton(component) { opWornButton(it, wearpos.slot) }
            onIfOverlaySubOpMenu(component) { opWornSubOp(it, wearpos.slot) }
        }
    }

    private suspend fun ProtectedAccess.opWornSubOp(event: IfOverlaySubOpMenu, wornSlot: Int) {
        val obj = player.worn[wornSlot]
        val expected = event.obj
        if (obj == null || (expected != null && obj.id != expected.id)) {
            resendSlot(player.worn, 0)
            return
        }
        ifClose()
        val type = getInvObj(obj)
        eventBus.publish(this, WornSubOpEvent(wornSlot, obj, type, event.op.slot, event.subop))
    }

    private fun Player.opWorn(wornSlot: Int, op: IfButtonOp) {
        ifClose(eventBus)
        if (isAccessProtected) {
            resendSlot(worn, 0)
            return
        }
        protectedAccess.launch(this) { interactions.interact(this, worn, wornSlot, op) }
    }

    private fun ProtectedAccess.opWornButton(event: IfOverlayButton, wornSlot: Int) {
        if (event.op == IfButtonOp.Op10) {
            interactions.examine(player, player.worn, wornSlot)
            return
        }
        player.opWorn(wornSlot, event.op)
    }

    private fun mappedComponents(): Map<Wearpos, String> {
        val resolver = equipment_tab_to_slots_map
        check(resolver.isNotEmpty) { "Equipment component enum must not be empty: $resolver" }

        val invalidWearpos = resolver.keys.filter { Wearpos[it] == null }
        check(invalidWearpos.isEmpty()) { "Invalid wearpos for keys: $invalidWearpos" }

        val invalidComponent = resolver.values.filter { it == null }
        check(invalidComponent.isEmpty()) { "Equipment enum must not have null values: $resolver" }

        return resolver.associate { checkNotNull(Wearpos[it.key]) to checkNotNull(RSCM.getReverseMapping(RSCMType.COMPONENT, it.value!!.packed)) }
    }
}
