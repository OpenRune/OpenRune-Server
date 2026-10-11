package org.rsmod.content.travel.shortcuts

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.*
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Permanent world shortcut setup uses native transforms and commits materials after interruption checks. */
internal class ShortcutSetupScript @Inject constructor(private val storage: PlayerItemStorage, private val xp: XpModifiers) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin {
            if (QuestRequirements.hasCompleted(player, "quest_fenkenstrain")) {
                for (gate in BRIDGE_GATES) if (player.vars[gate] == 0) VarPlayerIntMapSetter.set(player, gate, 1)
            }
        }
        onOpLoc1("loc.fenk_bridgebroken") { mes("The bridge needs to be repaired before you can cross.") }
        onOpLoc1("loc.fenk_bridgebroken_mirror") { mes("The bridge needs to be repaired before you can cross.") }
        for (loc in listOf("loc.fenk_bridgebroken_build", "loc.fenk_bridgebroken_mirror_build")) onOpLoc1(loc) {
            val gate = if (it.loc.coords.z >= 3561) BRIDGE_GATES[0] else BRIDGE_GATES[1]
            buildBridge(gate)
        }
        for ((loc, gate) in listOf("loc.darkm_outer_wall_2h_shortcut_no_rope" to "varbit.darkm_shortcut_inner", "loc.darkm_outer_wall_3h_shortcut_no_rope" to "varbit.darkm_shortcut_outer"))
            onOpLocU(loc, "obj.long_rope") { attachRope(gate, it.invSlot) }
        onOpLoc1("loc.xbows_rock_not_grappled") { attachGrapple() }
    }
    private suspend fun ProtectedAccess.attachRope(gate: String, slot: Int) {
        if (!QuestRequirements.hasCompleted(player, "quest_sinsofthefather") || vars[gate] != 0 || inv[slot]?.id != "obj.long_rope".asRSCM()) return
        val origin = coords; anim("seq.rope_tie")
        try {
            delay(2)
            if (!active(origin) || vars[gate] != 0 || inv[slot]?.id != "obj.long_rope".asRSCM()) return
            if (invDel(inv, "obj.long_rope", 1, slot = slot).success) { vars[gate] = 1; mes("You secure the long rope to the wall.") }
        } finally { resetAnim() }
    }
    private suspend fun ProtectedAccess.attachGrapple() {
        if (player.agilityLvl < 23 || player.rangedLvl < 24 || player.strengthLvl < 28) { mes("You need 23 Agility, 24 Ranged and 28 Strength."); return }
        if (!grappleEquipped()) { mes("Equip a crossbow and mith grapple first."); return }
        val origin = coords; anim("seq.xbows_human_hook_grapple_to_rock_part1")
        try {
            delay(2)
            if (!active(origin) || !grappleEquipped()) return
            vars["varbit.observatory_shortcut_rope"] = 1
            mes("You secure the grapple. You can now climb the rope.")
        } finally { resetAnim() }
    }
    private fun ProtectedAccess.grappleEquipped(): Boolean =
        player.worn[13]?.id == "obj.xbows_grapple_tip_bolt_mithril_rope".asRSCM() &&
        player.worn[3]?.id?.let { dev.openrune.ServerCacheManager.getItem(it)?.name?.contains("crossbow", true) } == true

    private suspend fun ProtectedAccess.buildBridge(gate: String) {
        if (vars[gate] != 1 || player.constructionLvl < 59 || !inv.contains("obj.hammer")) { mes("You need 59 Construction and a hammer to repair this bridge."); return }
        val origin = coords; anim("seq.fever_nail_plank")
        try {
            delay(2)
            if (!active(origin) || vars[gate] != 1 || player.constructionLvl < 59 || !inv.contains("obj.hammer")) return
            val plan = storage.planTransfer(player, inv, "obj.plank_teak", 6, incoming = false)
            val physicalPlanks = 6 - plan.amount
            var need = 28
            val nails = NAILS.mapNotNull { symbol ->
                val count = minOf(need, inv.physicalCount(symbol)); need -= count
                if (count == 0) null else symbol to count
            }
            if (inv.physicalCount("obj.plank_teak") < physicalPlanks || need > 0) { mes("You need 6 teak planks and 28 nails for this side."); return }
            val raw = player.invTransaction(inv, autoCommit = false) {
                val i = select(inv)
                if (physicalPlanks > 0) delete(i, "obj.plank_teak".asRSCM(), physicalPlanks)
                for ((symbol, count) in nails) delete(i, symbol.asRSCM(), count)
            }
            if (raw.failure || !plan.valid()) return
            val atomic = raw.copy(validateCommit = { plan.valid() }, afterCommit = { plan.commit() })
            atomic.commitAll()
            vars[gate] = 2
            vars["varbit.fenk_bridge_span"] = if (BRIDGE_GATES.all { vars[it] == 2 }) 2 else 0
            statAdvance("stat.construction", 550.0 * xp.get(player, "stat.construction"))
            mes("You reinforce this side of the bridge.")
        } finally { resetAnim() }
    }
    private fun ProtectedAccess.active(origin: org.rsmod.map.CoordGrid) = coords == origin && player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut
    private companion object {
        val BRIDGE_GATES = listOf("varbit.fenk_built_bridge_north", "varbit.fenk_built_bridge_south")
        val NAILS = listOf("obj.nails_bronze", "obj.nails_iron", "obj.nails", "obj.nails_black", "obj.nails_mithril", "obj.nails_adamant", "obj.nails_rune", "obj.nails_dragon")
    }
}
