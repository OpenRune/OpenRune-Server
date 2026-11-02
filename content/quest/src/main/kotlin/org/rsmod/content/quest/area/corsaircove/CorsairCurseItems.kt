package org.rsmod.content.quest.area.corsaircove

import org.rsmod.api.config.constants
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.table.QuestRow
import org.rsmod.game.entity.Player

internal fun ProtectedAccess.takeRelicFromTock(): Boolean {
    if (invAdd(inv, CorsairCurseRefs.Relic, 1).failure) {
        mes(constants.dm_take_invspace)
        return false
    }
    corscursThief = CorsairCurseCounts.ArsenRelicTaken
    return true
}

internal fun ProtectedAccess.returnRelicToTess(): Boolean {
    if (invTotal(inv, CorsairCurseRefs.Relic) <= 0) {
        return false
    }
    if (invDel(inv, CorsairCurseRefs.Relic, 1).failure) {
        return false
    }
    corscursThief = CorsairCurseCounts.ArsenRelicReturned
    return true
}

private val BIG_CHOMPY_END_STATE by lazy {
    QuestRow.getRow("dbrow.quest_bigchompybirdhunting").endstate
}

internal fun Player.hasCompletedBigChompy(): Boolean =
    vars["varp.chompybird"] >= BIG_CHOMPY_END_STATE
