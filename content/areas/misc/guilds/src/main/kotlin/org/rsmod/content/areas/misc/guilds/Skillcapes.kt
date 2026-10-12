package org.rsmod.content.areas.misc.guilds

import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository

internal const val SKILLCAPE_LEVEL = 99
internal const val SKILLCAPE_PRICE = 99_000

// TODO(content): [system:skillcapes] trimmed cape when the player has more than one level 99
internal suspend fun Dialogue.sellSkillcape(
    objRepo: ObjRepository,
    cape: String,
    hood: String,
): Boolean {
    if (access.inv.count("obj.coins") < SKILLCAPE_PRICE) {
        chatPlayer(sad, "But, unfortunately, I don't have enough money with me.")
        chatNpc(neutral, "Well, come back and see me when you do.")
        return false
    }
    if (access.inv.freeSpace() < 2) {
        chatNpc(
            neutral,
            "Unfortunately all Skillcapes are only available with a free hood, it's part of a " +
                "skill promotion deal; buy one get one free, you know. So you'll need to free up " +
                "some inventory space before I can sell you one.",
        )
        return false
    }
    if (!player.invTakeFee(SKILLCAPE_PRICE)) {
        return false
    }
    player.invAddOrDrop(objRepo, cape)
    player.invAddOrDrop(objRepo, hood)
    return true
}
