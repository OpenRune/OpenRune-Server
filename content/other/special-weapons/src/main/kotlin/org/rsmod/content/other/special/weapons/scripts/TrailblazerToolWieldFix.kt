package org.rsmod.content.other.special.weapons.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.worn.HeldEquipResult
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onOpHeld1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The real cache puts every Trailblazer/Trailblazer Reloaded tool's "Wield" option at `iop1`
 * (confirmed via a live cache dump), not `iop2` like most other weapons. This engine's default
 * equip handling only fires on op2 ([HeldInteractions.opHeld2]/`onOpHeld2`'s own doc: "replaces the
 * default wield/wear op handling"). Nothing was registered for op1 on these items, so clicking
 * "Wield" routed to the generic op1 fallback, which does nothing - the item could never be worn.
 *
 * Fixed by manually delegating op1 to [HeldInteractions.equip], the same bypass entry point the
 * engine's own op2 handler uses internally.
 */
class TrailblazerToolWieldFix @Inject constructor(private val heldInteractions: HeldInteractions) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (item in WIELD_AT_OP1) {
            onOpHeld1(item) {
                val result = heldInteractions.equip(this, it.inventory, it.slot)
                if (result is HeldEquipResult.Fail) {
                    result.messages.forEach(::mes)
                }
            }
        }
    }

    private companion object {
        val WIELD_AT_OP1 =
            listOf(
                "obj.trailblazer_axe",
                "obj.trailblazer_axe_empty",
                "obj.trailblazer_axe_no_infernal",
                "obj.trailblazer_harpoon",
                "obj.trailblazer_harpoon_empty",
                "obj.trailblazer_harpoon_no_infernal",
                "obj.trailblazer_pickaxe",
                "obj.trailblazer_pickaxe_empty",
                "obj.trailblazer_pickaxe_no_infernal",
                "obj.trailblazer_reloaded_axe",
                "obj.trailblazer_reloaded_axe_empty",
                "obj.trailblazer_reloaded_axe_no_infernal",
                "obj.trailblazer_reloaded_harpoon",
                "obj.trailblazer_reloaded_harpoon_empty",
                "obj.trailblazer_reloaded_harpoon_no_infernal",
                "obj.trailblazer_reloaded_pickaxe",
                "obj.trailblazer_reloaded_pickaxe_empty",
                "obj.trailblazer_reloaded_pickaxe_no_infernal",
            )
    }
}
