package org.rsmod.content.events.leaguetasks.pacts

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.VarBitType
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.enums.enum
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.TalentTreeRow
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

/**
 * Demonic Pact points and the pact tree (`talent_tree`). A node's index in
 * `enum.talent_tree_nodes` is its unlock bit, kept 32 per varp in `varp.combat_mastery_perm_0..5`
 * as `[proc,talent_tree_tracking_get_set_unlocked]` reads them. Unlocking a node adds its effect
 * value to the varbit `enum.talent_effect_varbits` maps the effect id to.
 */
@Singleton
public class LeaguePacts @Inject constructor(private val eventBus: EventBus) {
    private val nodes: Map<Int, TalentTreeRow> =
        enum<Int, DBRowType>("talent_tree_nodes").filterValuesNotNull().backing.mapValues {
            TalentTreeRow.getRow(it.value.id)
        }
    private val indexByRow: Map<Int, Int> =
        nodes.entries.associate { (index, row) -> row.rowId to index }
    private val effectVarbits: Map<Int, VarBitType> =
        enum<Int, Int>("talent_effect_varbits")
            .filterValuesNotNull()
            .backing
            .mapNotNull { (effect, id) -> ServerCacheManager.getVarbit(id)?.let { effect to it } }
            .toMap()
    private val pactTasks: Set<Int> =
        enum<DBRowType, Boolean>("league_6_pact_tasks").keys.mapTo(HashSet()) { it.id }

    public fun awardsPact(task: Int): Boolean = task in pactTasks

    public fun earned(player: Player): Int = player.vars["varp.talent_points_earned"]

    public fun available(player: Player): Int =
        earned(player) - player.vars["varp.talent_points_spent"]

    public fun award(player: Player) {
        VarPlayerIntMapSetter.set(player, "varp.talent_points_earned", earned(player) + 1)
    }

    public fun isUnlocked(player: Player, node: Int): Boolean =
        player.vars[unlockVarp(node)] and (1 shl (node % NODES_PER_VARP)) != 0

    /**
     * Unlocks [requested] if [player] can afford them and each one links to a node already
     * unlocked or in the same request. The root (index 0) needs no link. Returns false and changes
     * nothing otherwise.
     */
    public fun spend(player: Player, requested: IntArray): Boolean {
        val batch = requested.toSet()
        if (batch.isEmpty() || batch.size != requested.size || batch.size > available(player)) {
            return false
        }
        if (batch.any { it !in nodes || isUnlocked(player, it) }) {
            return false
        }
        if (!isConnected(player, batch)) {
            return false
        }
        for (node in batch) {
            unlock(player, node)
        }
        val spent = player.vars["varp.talent_points_spent"] + batch.size
        VarPlayerIntMapSetter.set(player, "varp.talent_points_spent", spent)
        return true
    }

    public fun openTree(player: Player) {
        player.ifOpenOverlay("interface.talent_tree", eventBus)
        player.ifSetEvents("component.talent_tree:frame", CLOSE_SUB..CLOSE_SUB, IfEvent.Op1)
        player.ifSetEvents("component.talent_tree:data_layer", -1..-1, IfEvent.ScriptTrigger)
        player.ifSetEvents(
            "component.talent_tree:perm_floating_panel_content",
            -1..-1,
            IfEvent.Op1,
            IfEvent.ScriptTrigger,
        )
        player.runClientScript(
            "clientscript.[clientscript,talent_tree_init]".asRSCM(RSCMType.CLIENTSCRIPT),
            TREE_INIT_MODE,
        )
    }

    private fun isConnected(player: Player, batch: Set<Int>): Boolean {
        val reached = HashSet<Int>()
        var grew = true
        while (grew) {
            grew = false
            for (node in batch) {
                if (node in reached) {
                    continue
                }
                val linked =
                    node == ROOT ||
                        linkedNodes(node).any { isUnlocked(player, it) || it in reached }
                if (linked) {
                    reached += node
                    grew = true
                }
            }
        }
        return reached.size == batch.size
    }

    private fun linkedNodes(node: Int): List<Int> =
        nodes.getValue(node).linkedNodes.mapNotNull { indexByRow[it.rowId] }

    private fun unlock(player: Player, node: Int) {
        val varp = unlockVarp(node)
        val bit = 1 shl (node % NODES_PER_VARP)
        VarPlayerIntMapSetter.set(player, varp, player.vars[varp] or bit)
        val (effect, value) = nodes.getValue(node).effect
        val varbit = effectVarbits[effect] ?: return
        VarPlayerIntMapSetter.set(player, varbit, player.vars[varbit] + value)
    }

    private fun unlockVarp(node: Int): String = "varp.combat_mastery_perm_${node / NODES_PER_VARP}"

    private companion object {
        const val ROOT = 0
        const val NODES_PER_VARP = 32
        const val CLOSE_SUB = 11
        const val TREE_INIT_MODE = 1
    }
}
