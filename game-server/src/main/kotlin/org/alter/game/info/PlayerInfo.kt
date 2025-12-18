package org.alter.game.info

import dev.openrune.ServerCacheManager
import dev.openrune.server.impl.item.ItemRenderDataManager
import org.alter.game.model.AnimationSet
import org.alter.game.model.ForcedMovement
import org.alter.game.model.Tile
import org.alter.game.model.appearance.Gender
import org.alter.game.model.entity.Player
import org.alter.game.model.move.MovementType

class PlayerInfo(var player: Player) {
    val info = player.playerInfo.avatar.extendedInfo

    val DEFAULT_ANIM_SET = AnimationSet(readyAnim = 808, turnAnim = 823, walkAnim = 819, walkAnimBack = 820, walkAnimLeft = 821, walkAnimRight = 822, runAnim = 824)
    var animSequance = DEFAULT_ANIM_SET
    val walkAnimOverride = player.walkAnimOverride;

    fun syncAppearance() {
        if (player.getTransmogId() == -1) {
            for (i in 0 until 7) {
                info.setIdentKit(i, player.appearance.getLook(i))
            }
            for (i in 0 until 12) {
                val obj = player.equipment[i]
                if (obj == null) {
                    info.setWornObj(i, -1, -1, -1)
                } else {
                    val config = ServerCacheManager.getItem(obj.id)
                    if (config == null) {
                        info.setWornObj(i, -1, -1, -1)
                        continue
                    }

                    info.setWornObj(i, obj.id, config.appearanceOverride1, config.appearanceOverride2)
                }
            }
            val weapon = player.equipment[3]
            if (weapon != null) {
                val renderAnimationSet = ItemRenderDataManager.getItemRenderAnimationByItem(weapon.id)
                if (renderAnimationSet != null) {
                    animSequance = AnimationSet(
                        readyAnim = renderAnimationSet.readyAnim,
                        turnAnim = renderAnimationSet.turnAnim,
                        walkAnim = renderAnimationSet.walkAnim,
                        walkAnimBack = renderAnimationSet.walkAnimBack,
                        walkAnimLeft = renderAnimationSet.walkAnimLeft,
                        walkAnimRight = renderAnimationSet.walkAnimRight,
                        runAnim = renderAnimationSet.runAnim
                    )
                }
            }
            syncAnimationSet()
            for (i in 0 until 5) {
                info.setColour(i, player.appearance.colors[i])
            }
        } else {
            val def = ServerCacheManager.getNpc(player.getTransmogId())?: return
            animSequance = AnimationSet(
                readyAnim = def.standAnim,
                walkAnim = def.walkAnim,
                walkAnimBack = def.walkAnim,
                walkAnimLeft = def.rotateLeftAnim,
                walkAnimRight = def.rotateRightAnim,
                turnAnim = def.rotateBackAnim,
                runAnim = def.walkAnim
            )
            syncAnimationSet()
        }
        info.setName(player.username)
        info.setCombatLevel(player.combatLevel)
        info.setMale(player.appearance.gender == Gender.MALE)
        info.setOverheadIcon(player.prayerIcon)
        info.setSkullIcon(player.skullIcon)
        info.transformToNpc(player.getTransmogId())
        info.setHidden(player.invisible)
    }

    fun syncAnimationSet() {
        if(walkAnimOverride != null) {
            player.writeMessage("Overriding walk with anim $walkAnimOverride")
            val plusOne = walkAnimOverride + 1
            info.setBaseAnimationSet(
                readyAnim = plusOne,
                turnAnim = plusOne,
                walkAnim = walkAnimOverride,
                walkAnimBack = plusOne,
                walkAnimLeft = plusOne,
                walkAnimRight = plusOne,
                runAnim = -1,
            )
        } else {
            info.setBaseAnimationSet(
                readyAnim = animSequance.readyAnim,
                turnAnim = animSequance.turnAnim,
                walkAnim = animSequance.walkAnim,
                walkAnimBack = animSequance.walkAnimBack,
                walkAnimLeft = animSequance.walkAnimLeft,
                walkAnimRight = animSequance.walkAnimRight,
                runAnim = animSequance.runAnim,
            )
        }
    }

    fun setSequence(id: Int, delay: Int) {
        info.setSequence(id, delay)
    }

    fun tinting(hue: Int = 0, saturation: Int = 0, luminance: Int = 0, opacity: Int = 0, delay: Int = 0, duration: Int = 0) {
        info.tinting(
            startTime = delay,
            endTime = duration,
            hue = hue,
            saturation = saturation,
            lightness = luminance,
            weight = opacity,
        )
    }

    fun setSay(message:String) {
        info.setSay(message)
    }

    fun setFaceCoord(
        face: Tile,
        width: Int = 1,
        length: Int = 1,
    ) {
        val srcX = player.tile.x * 64
        val srcZ = player.tile.z * 64
        val dstX = face.x * 64
        val dstZ = face.z * 64
        var degreesX = (srcX - dstX).toDouble()
        var degreesZ = (srcZ - dstZ).toDouble()
        degreesX += (Math.floor(width / 2.0)) * 32
        degreesZ += (Math.floor(length / 2.0)) * 32
        info.setFaceAngle((Math.atan2(degreesX, degreesZ) * 325.949).toInt() and 0x7ff)
    }

    fun facePawn(index: Int) {
        info.setFacePathingEntity(index)
    }

    fun forceMove(movement: ForcedMovement) {
        info.setExactMove(
            deltaX1 = movement.diffX1,
            deltaZ1 = movement.diffZ1,
            delay1 = movement.clientDuration1,
            deltaX2 = movement.diffX2,
            deltaZ2 = movement.diffZ2,
            delay2 = movement.clientDuration2,
            angle = movement.directionAngle,
        )
    }

    fun setMoveSpeed(movementType: MovementType) {
        info.setMoveSpeed(movementType.value)
    }
}