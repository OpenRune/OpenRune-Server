package org.rsmod.content.skills.runecrafting.magic

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.skills.runecrafting.magicImbueActive
import org.rsmod.game.entity.Player

object MagicImbue {
    const val DURATION_CYCLES = 20
    const val EXPIRE_QUEUE = "queue.runecraft_magic_imbue_expire"

    fun Player.isActive(): Boolean = magicImbueActive == 1

    fun ProtectedAccess.activate() {
        player.magicImbueActive = 1
        clearQueue(EXPIRE_QUEUE)
        weakQueue(EXPIRE_QUEUE, DURATION_CYCLES)
        mes("You are imbued with the ability to bind combination runes without a talisman.")
        anim("seq.human_casting")
        spotanim("spotanim.quest_lunar_spellbook_magic_embue_spot_anim", height = 92)
    }

    fun ProtectedAccess.deactivate() {
        player.magicImbueActive = 0
        clearQueue(EXPIRE_QUEUE)
    }
}
