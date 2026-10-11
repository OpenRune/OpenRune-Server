package org.rsmod.content.travel.shortcuts

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.*
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeAnim
import org.rsmod.game.type.getInvObj
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
        onOpLoc1("loc.archeuus_runestone_shortcut_grey_bottom") { mes("You cannot climb up this shortcut from below.") }
        onOpLoc1("loc.xbows_rock_not_grappled") { attachGrapple() }
        onOpLoc1("loc.viyeldi_shortcut_rock_ropeless") { mes("Attach a long rope to these rocks first.") }
        onOpLocU("loc.viyeldi_shortcut_rock_ropeless", "obj.long_rope") { attachViyeldi(it.invSlot) }
        onOpLoc1("loc.myq5_wall_cave_shortcut_6_child_blocked") { clearRock("varbit.myq5_lab_sc_unlocked", 78, null) }
        onOpLoc1("loc.wyrmscraig_cliff_shortcut_blockage_vis") { clearRock("varbit.wyrmscraig_cliff_shortcut", 1, "quest_fallenfromgrace") }
        onOpLoc1("loc.vampyrium_stepping_stone_nocleared") { clearRock("varbit.vampyrium_traversal_rock_mined", 1, "quest_bloodmoonrises") }
        for (item in dev.openrune.ServerCacheManager.getItemTypes().filter { it.isContentType("content.mining_pickaxe") }) {
            onOpLocU("loc.myq5_wall_cave_shortcut_6_child_blocked", item.internalName) { clearRock("varbit.myq5_lab_sc_unlocked", 78, null) }
            onOpLocU("loc.wyrmscraig_cliff_shortcut_blockage_vis", item.internalName) { clearRock("varbit.wyrmscraig_cliff_shortcut", 1, "quest_fallenfromgrace") }
            onOpLocU("loc.vampyrium_stepping_stone_nocleared", item.internalName) { clearRock("varbit.vampyrium_traversal_rock_mined", 1, "quest_bloodmoonrises") }
        }
    }
    private suspend fun ProtectedAccess.attachViyeldi(slot: Int) {
        if (vars["varbit.viyeldi_shortcut"] != 0 || !QuestRequirements.hasCompleted(player, "quest_legends"))return
        val rope = inv[slot] ?: return
        if (rope.id != "obj.long_rope".asRSCM())return
        val origin = coords; anim("seq.rope_tie")
        try { delay(2); if (!active(origin) || vars["varbit.viyeldi_shortcut"] != 0 || inv[slot] !== rope)return
            if (invDel(inv, "obj.long_rope", 1, slot = slot).success) { vars["varbit.viyeldi_shortcut"] = 1; mes("You secure the long rope to the rocks.") }
        } finally { resetAnim() }
    }
    private suspend fun ProtectedAccess.clearRock(gate: String, level: Int, quest: String?) {
        if (vars[gate] != 0)return
        if (player.miningLvl < level) { mes("You need Mining $level to clear this shortcut."); return }
        if (quest != null && !shortcutQuestCompleted(player, quest)) { mes("You haven't unlocked access to these rocks yet."); return }
        val pick = Mining.findPickaxe(player) ?: run { mes("You need a usable pickaxe to clear these rocks."); return }
        val origin = coords; anim(RSCM.getReverseMapping(RSCMType.SEQ, getInvObj(pick).pickaxeAnim.id))
        try {
            delay(3)
            if (!active(origin) || vars[gate] != 0 || player.miningLvl < level || Mining.findPickaxe(player) == null || quest != null && !shortcutQuestCompleted(player, quest))return
            vars[gate] = 1; mes("You clear the rocks and permanently unlock the shortcut.")
        } finally { resetAnim() }
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
