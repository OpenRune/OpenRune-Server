package org.alter.skills.prayer

import org.alter.api.ChatMessageType
import org.alter.api.Skills
import org.alter.api.ext.message
import org.alter.api.ext.options
import org.alter.api.ext.playSound
import org.alter.game.model.LockState
import org.alter.game.model.entity.Player
import org.alter.game.model.move.stopMovement
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemClickEvent
import org.alter.rscm.RSCM.asRSCM
import org.alter.skills.prayer.GildedAlterEvents.Companion.CHAOS_ALTAR_AREA

class PrayerBuryEvents : PluginEvent() {

    override fun init() {
        Bones.bones.forEach { bone ->
            on<ItemClickEvent> {
                where { item == bone.id && !player.isLocked() }
                then {
                    if (CHAOS_ALTAR_AREA.contains(player.tile) && !bone.isAshes) {
                        player.queue {
                            when (options(player, "Bury the Bone", "Cancel", title = "Are you sure you want to do that?")) {
                                1 -> buryBone(player, false, item, slot, bone.xp)
                            }
                        }
                    } else {
                        buryBone(player, bone.isAshes, item, slot, bone.xp)
                    }
                }
            }
        }
    }

    private fun buryBone(player: Player, isAshes: Boolean, item: Int, slot: Int, xp: Int) {
        player.stopMovement()
        player.lock = LockState.DELAY_ACTIONS

        if (!isAshes) {
            player.message("You dig a hole in the ground...", ChatMessageType.FILTERED)
        }

        val animation = if (isAshes) "sequences.farming_ingredient_sprinkle" else "sequences.human_openchest"

        player.animate(animation.asRSCM())
        player.playSound(2738)

        player.queue {
            wait(2)
            if (player.inventory.remove(item = item, beginSlot = slot).hasSucceeded()) {
                val message = if (isAshes) "You scatter the ashes." else "You bury the bones."
                player.message(message, ChatMessageType.FILTERED)
                player.addXp(Skills.PRAYER, xp)
            }
            player.unlock()
        }
    }

}

