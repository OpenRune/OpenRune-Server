package org.alter.skills.agility.shortcuts

import org.alter.api.Skills
import org.alter.api.ext.filterableMessage
import org.alter.api.ext.getInteractingGameObj
import org.alter.game.model.Direction
import org.alter.game.model.ForcedMovement
import org.alter.game.model.Tile
import org.alter.game.model.entity.Player
import org.alter.game.model.move.moveTo
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onObjectOption
import org.alter.rscm.RSCM
import kotlin.random.Random

class GeneralShortcuts : PluginEvent() {

    enum class Axis {
        HORIZONTAL, VERTICAL
    }

    data class Shortcut(
        val obj: String,
        val option: String,
        val axis: Axis,
        val distance: Int,
        val anim: String,
        val xp: Double,
        val levelReq: Int,
        val duration1: Int = 5,
        val duration2: Int = 250,
        val msgStart: String? = null,
        val msgEnd: String? = null,
        val failAnim: String? = null,
        val failMsg: String? = null,
        val minSuccess: Double = 0.20,
        val maxSuccess: Double = 0.99
    )

    private val shortcuts = listOf(
        Shortcut(
            obj = "objects.swamp_cave_steppingstone_a",
            option = "Jump-across",
            axis = Axis.HORIZONTAL,      // links <-> rechts
            distance = 2,                // aantal tiles overbruggen
            anim = "sequences.human_steppingstonejump",
            xp = 3.0,
            levelReq = 1,
            msgStart = "You jump to the next stone...",
            msgEnd = "You land safely.",
            failAnim = "sequences.human_wobbleandfall_l",
            failMsg = "You slip and fall!",
            minSuccess = 0.20,
            maxSuccess = 0.99
        )
    )

    override fun init() {
        shortcuts.forEach { shortcut ->
            registerShortcut(shortcut)
        }
    }

    private fun registerShortcut(data: Shortcut) {
        onObjectOption(data.obj, data.option) {
            val p = player

            if (!p.hasLevel(data.levelReq)) {
                p.filterableMessage(
                    "You need at least level <col=ff0000>${data.levelReq}</col> Agility to use this shortcut."
                )
                return@onObjectOption
            }

            val obj = p.getInteractingGameObj() ?: return@onObjectOption

            val (dest, angle) = computeDestination(p.tile, obj.tile, data)

            val chance = getSuccessChance(p, data)
            val fail = Random.nextDouble() > chance

            if (fail) {
                failShortcut(p, obj.tile, data)
                return@onObjectOption
            }

            successShortcut(p, dest, angle, data)
        }
    }

    private fun computeDestination(
        playerTile: Tile,
        objTile: Tile,
        data: Shortcut
    ): Pair<Tile, Int> {
        return when (data.axis) {
            Axis.HORIZONTAL -> {
                val isWestSide = playerTile.x < objTile.x
                if (isWestSide) {
                    objTile.step(Direction.EAST, data.distance) to Direction.EAST.angle
                } else {
                    objTile.step(Direction.WEST, data.distance) to Direction.WEST.angle
                }
            }
            Axis.VERTICAL -> {
                val isSouthSide = playerTile.z < objTile.z
                if (isSouthSide) {
                    objTile.step(Direction.NORTH, data.distance) to Direction.NORTH.angle
                } else {
                    objTile.step(Direction.SOUTH, data.distance) to Direction.SOUTH.angle
                }
            }
        }
    }

    private fun successShortcut(player: Player, dest: Tile, angle: Int, data: Shortcut) {
        player.queue {
            data.msgStart?.let { player.filterableMessage(it) }
            player.animate(data.anim)

            val mv = ForcedMovement.of(
                src = player.tile,
                dst = dest,
                clientDuration1 = data.duration1,
                clientDuration2 = data.duration2,
                directionAngle = angle
            )

            player.forceMove(this, mv)
            wait(1)

            player.animate(RSCM.NONE)

            if (data.xp > 0.0) {
                player.addXp(Skills.AGILITY, data.xp)
            }

            data.msgEnd?.let { player.filterableMessage(it) }
        }
    }

    private fun failShortcut(player: Player, origin: Tile, data: Shortcut) {
        player.queue {
            data.failMsg?.let { player.filterableMessage(it) }
            data.failAnim?.let { player.animate(it) }

            wait(2)
            player.moveTo(origin)
            wait(1)
            player.animate(RSCM.NONE)
        }
    }

    private fun getSuccessChance(player: Player, data: Shortcut): Double {
        val lvl = player.getSkills().getBaseLevel(Skills.AGILITY)
            .coerceAtLeast(data.levelReq)

        val minC = data.minSuccess
        val maxC = data.maxSuccess

        val curve = 1 - Math.pow(0.90, (lvl - data.levelReq).toDouble() / 6.0)
        return (minC + curve * (maxC - minC)).coerceIn(minC, maxC)
    }

    private fun Player.hasLevel(level: Int): Boolean {
        return getSkills().getBaseLevel(Skills.AGILITY) >= level
    }
}
