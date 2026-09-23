package org.rsmod.content.quest.area.draynor

import org.rsmod.api.invtx.invDelAll
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

internal class ErnestProgress(private val quest: Quest) {
    val foundErnest = quest.attribute(name = "FOUND_ERNEST", default = false)

    private var Player.partsReceived by boolVarBit("varbit.ernesthandin_complete")
    private var Player.hauntedStage by intVarp("varp.haunted")

    fun hasReceivedParts(player: Player): Boolean = player.partsReceived

    fun hasAllParts(player: Player): Boolean = Parts.all { player.inv.count(it) > 0 }

    fun receiveParts(access: ProtectedAccess): Boolean {
        val player = access.player
        if (!quest.isQuestInProgress(player) || !foundErnest.get(player)) {
            return false
        }
        if (player.partsReceived) {
            return true
        }
        if (player.invDelAll(access.inv, Parts.map { InvObj(it) }).failure) {
            return false
        }
        player.partsReceived = true
        return true
    }

    fun canReceiveReward(access: ProtectedAccess): Boolean =
        access.invAdd(
            access.inv,
            "obj.coins",
            CoinReward,
            autoCommit = false,
            ignoreVirtualStorage = true,
        ).success

    suspend fun restoreErnest(access: ProtectedAccess, scene: suspend () -> Unit) {
        if (quest.isQuestCompleted(access.player)) {
            return
        }
        check(access.player.partsReceived)
        try {
            access.player.hauntedStage = quest.maxSteps
            scene()
        } finally {
            quest.advanceQuestStage(access, quest.maxSteps - quest.getQuestStage(access.player))
        }
    }

    companion object {
        const val CoinReward = 300
        private val Parts = listOf("obj.rubber_tube", "obj.pressure_gauge", "obj.oil_can")
    }
}
