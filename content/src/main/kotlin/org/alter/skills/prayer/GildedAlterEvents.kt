package org.alter.skills.prayer

import org.alter.api.ChatMessageType
import org.alter.api.Skills
import org.alter.api.ext.message
import org.alter.api.ext.playSound
import org.alter.api.ext.random
import org.alter.game.model.Area
import org.alter.game.model.Tile
import org.alter.game.model.TileGraphic
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemOnObject
import org.alter.rscm.RSCM.asRSCM

class GildedAlterEvents : PluginEvent() {

    companion object {
        val CHAOS_ALTAR_AREA = Area(2946, 3825, 2957, 3816, true)
        val GILDED_ALTERS = listOf(
            "objects.poh_altar_saradomin_7".asRSCM(),
            "objects.poh_altar_zamorak_7".asRSCM(),
            "objects.poh_altar_gnomechild_7".asRSCM()
        )
    }

    override fun init() {
        Bones.bones.forEach { bone ->
            if (!bone.isAshes) {
                on<ItemOnObject> {
                    where { item.id == bone.id && gameObject.id == "objects.chaosaltar".asRSCM() }
                    then { startAlter(player, bone.id, bone.xp, true, gameObject) }
                }

                on<ItemOnObject> {
                    where { item.id == bone.id && GILDED_ALTERS.contains(gameObject.id) }
                    then { startAlter(player, bone.id, bone.xp, false, gameObject) }
                }
            }
        }
    }

    private fun startAlter(player: Player, bone: Int, xp: Int, isChaosAltar: Boolean, gameObject: GameObject) {
        player.queue {
            repeatUntil(delay = 3, immediate = true, predicate = { canSacrifice(player, bone) }) {
                val removeBone = random(0..2) == 1

                player.animate("sequences.human_bone_sacrifice".asRSCM())
                player.playSound(1628)
                player.world.spawn(TileGraphic(tile = gameObject.tile, id = "spotanims.poh_bone_sacrifice".asRSCM()))
                player.addXp(Skills.PRAYER, (xp * 3.5).toInt())

                if (isChaosAltar) {
                    if (!removeBone) {
                        player.message("The Dark Lord spares your sacrifice, but rewards you for your efforts.", ChatMessageType.FILTERED)
                    } else {
                        player.inventory.remove(bone)
                    }
                } else {
                    player.inventory.remove(bone)
                }
            }
        }
    }

    private fun canSacrifice(player: Player, bone: Int): Boolean {
        if (!player.inventory.contains(bone)) return false
        if (!player.tile.isWithinRadius(Tile(2947, 3821), 1)) return false
        if (CHAOS_ALTAR_AREA.contains(player.tile)) return false
        return true
    }
}
