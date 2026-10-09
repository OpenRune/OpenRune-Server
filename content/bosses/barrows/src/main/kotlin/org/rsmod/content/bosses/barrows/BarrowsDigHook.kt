package org.rsmod.content.bosses.barrows

import kotlin.math.abs
import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player

internal const val MOUND_RADIUS = 3

private const val PRIEST_IN_PERIL = "quest_priestinperil"

internal class BarrowsDigHook : SpadeDigHook {
    override fun claims(player: Player): Boolean = player.mound() != null

    override suspend fun ProtectedAccess.beforeDig(): Boolean {
        if (!hfsInterruptsDig()) {
            return true
        }
        startDialogue {
            chatNpcSpecific(
                "Strange Old Man",
                "npc.barrows_oldman",
                happy,
                "You want to dig? Good, good! But we talk first. Talk then dig!",
            )
        }
        return false
    }

    override suspend fun ProtectedAccess.dig() {
        val mound = player.mound() ?: return
        spam("You've broken into a crypt!")
        telejump(mound.chamber)
    }

    private fun ProtectedAccess.hfsInterruptsDig(): Boolean =
        player.hfsStage == HFS_NOT_STARTED &&
            QuestRequirements.hasCompleted(player, PRIEST_IN_PERIL)

    private fun Player.mound(): BarrowsBrother? =
        BarrowsBrother.entries.firstOrNull { brother ->
            val centre = brother.moundCenter
            coords.level == 0 &&
                abs(coords.x - centre.x) <= MOUND_RADIUS &&
                abs(coords.z - centre.z) <= MOUND_RADIUS
        }
}
