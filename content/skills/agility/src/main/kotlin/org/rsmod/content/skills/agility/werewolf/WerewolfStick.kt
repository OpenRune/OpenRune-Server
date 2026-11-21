package org.rsmod.content.skills.agility.werewolf

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.hunt.HuntVis
import jakarta.inject.Inject
import org.rsmod.api.hunt.NpcSearch
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.content.skills.agility.AgilityCourseHook
import org.rsmod.content.skills.agility.AgilityObstacle
import org.rsmod.content.skills.agility.LandSpawn
import org.rsmod.game.entity.Player
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid

internal class WerewolfStickThrow @Inject constructor(private val stick: WerewolfStick) :
    AgilityCourseHook {
    override fun landed(access: ProtectedAccess, obstacle: AgilityObstacle) {
        val spawn = obstacle.landSpawn ?: return
        stick.thrown(access.player, spawn)
    }
}

class WerewolfStick
@Inject
constructor(
    private val search: NpcSearch,
    private val worldRepo: WorldRepository,
    private val objRepo: ObjRepository,
    private val random: GameRandom,
) {
    internal fun thrown(player: Player, spawn: LandSpawn) {
        val boss = search.find(player.coords, BOSS, BOSS_RANGE, HuntVis.Off)
        if (player.inv.count(spawn.obj) > 0) {
            // UNVERIFIED: the line, which the wiki only describes.
            boss?.say("Why didn't you hand the stick over?!!?")
            return
        }
        if (boss != null) {
            boss.faceSquare(boss.coords.translateZ(1))
            boss.say("FETCH!!!!!")
            boss.anim(THROW)
            worldRepo.projAnim(flight(boss.coords))
        }
        player.softQueue(LANDING_QUEUE, LANDING_TICKS, spawn)
    }

    internal fun land(player: Player, spawn: LandSpawn) {
        if (player.inv.count(spawn.obj) > 0) {
            return
        }
        objRepo.add(spawn.obj, random.pick(spawn.tiles), STICK_TICKS, player, reveal = NEVER_REVEAL)
    }

    private fun flight(from: CoordGrid): ProjAnim =
        ProjAnim(
            spotanim = STICK_TRAVEL.asRSCM(RSCMType.SPOTANIM),
            startHeight = 40,
            endHeight = 36,
            startTime = 30,
            endTime = 180,
            angle = 10,
            progress = 32,
            sourceIndex = 0,
            targetIndex = 0,
            startCoord = from,
            endCoord = FLIGHT_END,
        )

    internal companion object {
        const val STICK = "obj.waa_stick"
        const val LANDING_QUEUE = "queue.agility_werewolf_stick"
        const val LANDING_TICKS = 8
        const val LEFT_MESSAGE = "The werewolf trainer removes your stick as you leave."

        private const val BOSS = "npc.werewolf_trainer_start"
        private const val BOSS_RANGE = 10
        private const val THROW = "seq.werewolf_update_throw_stick"
        private const val STICK_TRAVEL = "spotanim.waa_stick_travel"
        private const val STICK_TICKS = 150
        private const val NEVER_REVEAL = STICK_TICKS + 1
        private val FLIGHT_END = CoordGrid(3540, 9903, 0)

        fun takeSticks(player: Player): Int {
            val sticks = player.inv.count(STICK)
            if (sticks == 0 || !player.invDel(player.inv, STICK, sticks).success) {
                return 0
            }
            return sticks
        }

        fun takeSticksOnLeaving(player: Player) {
            if (takeSticks(player) > 0) {
                player.mes(LEFT_MESSAGE)
            }
        }
    }
}
