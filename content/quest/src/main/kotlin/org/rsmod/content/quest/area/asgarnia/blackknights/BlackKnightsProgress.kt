package org.rsmod.content.quest.area.asgarnia.blackknights

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.InvScope
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

internal object BlackKnightsProgress {
    const val QUEST = "quest_blackknightsfortress"
    const val STAGE = "varbit.black_knights_progress"
    const val DOSSIER = "obj.bk_dossier"
    val quest
        get() = checkNotNull(Quest.get(QUEST))

    fun stage(p: Player) = quest.getQuestStage(p)

    fun owns(p: Player, item: String) =
        (listOf(p.inv, p.worn) + p.invMap.values)
            .distinct()
            .filter {
                it.type.scope == InvScope.Perm && it.type.id != "inv.gravestone_layout".asRSCM()
            }
            .any { item in it }

    fun dossier(a: ProtectedAccess): Boolean {
        val s = stage(a.player)
        if (s !in 0..2 || s == 0 && a.vars["varp.qp"] < 12 || owns(a.player, DOSSIER)) return false
        if (!exchange(a, emptyList(), listOf(InvObj(DOSSIER)))) return false
        if (s == 0) quest.setQuestStage(a, 1)
        return true
    }

    fun read(a: ProtectedAccess): Boolean =
        stage(a.player) in 1..4 && exchange(a, listOf(InvObj(DOSSIER)), emptyList())

    fun hear(a: ProtectedAccess) =
        stage(a.player) == 1 && near(a.coords, CoordGrid(3026, 3507)) && quest.setQuestStage(a, 2)

    fun sabotage(a: ProtectedAccess, item: String): Boolean {
        if (
            stage(a.player) != 2 ||
                item != "obj.cabbage" ||
                !near(a.coords, CoordGrid(3031, 3507, 1))
        )
            return false
        if (!exchange(a, listOf(InvObj("obj.cabbage")), emptyList())) return false
        return quest.setQuestStage(a, 3)
    }

    fun finish(a: ProtectedAccess) = stage(a.player) == 3 && quest.completeQuest(a)

    private fun near(a: CoordGrid, b: CoordGrid) = a.level == b.level && a.chebyshevDistance(b) <= 1

    private fun exchange(a: ProtectedAccess, costs: List<InvObj>, gains: List<InvObj>): Boolean {
        val tx =
            a.player.invTransaction(a.inv, autoCommit = false) {
                val i = select(a.inv)
                costs.forEach { delete(i, it.id, it.count) }
                gains.forEach { add(i, it.id, it.count) }
            }
        if (tx.failure) return false
        tx.commitAll()
        return true
    }
}
