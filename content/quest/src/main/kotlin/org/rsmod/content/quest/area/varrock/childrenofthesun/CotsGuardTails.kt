package org.rsmod.content.quest.area.varrock.childrenofthesun

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal const val TailTimer = "timer.cots_tail"

internal interface CotsTails {
    fun start(access: ProtectedAccess)

    fun tick(player: Player): TailResult

    fun stop(player: Player)
}

internal class GuardTails
@Inject
constructor(private val npcRepo: NpcRepository, private val sight: RayCastValidator) : CotsTails {
    private class Session(val tail: GuardTail, val guard: Npc)

    private val sessions = HashMap<Long, Session>()

    override fun start(access: ProtectedAccess) {
        val player = access.player
        val uuid = player.uuid ?: return
        stop(player)
        val tail =
            GuardTail(CotsRoute.Tiles, CotsRoute.LookoutIndices) { guard, target ->
                sight.hasLineOfSight(guard, target)
            }
        val guard = Npc(GuardType, tail.guard)
        npcRepo.add(guard, SessionLifespan)
        guard.noneMode()
        sessions[uuid] = Session(tail, guard)
        player.timer(TailTimer, 1)
    }

    override fun tick(player: Player): TailResult {
        val uuid = player.uuid ?: return TailResult.TooFar
        val session = sessions[uuid] ?: return TailResult.TooFar
        if (!session.guard.isSlotAssigned) {
            return TailResult.TooFar
        }
        val before = session.tail.guard
        val result = session.tail.tick(player.coords)
        when (result) {
            TailResult.Continue -> advance(session, before)
            TailResult.Spotted -> {
                session.guard.facePlayer(player)
                session.guard.say(SpottedShout)
            }
            else -> Unit
        }
        return result
    }

    internal fun guardTile(player: Player): CoordGrid? =
        player.uuid?.let(sessions::get)?.tail?.guard

    override fun stop(player: Player) {
        player.clearTimer(TailTimer)
        val uuid = player.uuid ?: return
        val session = sessions.remove(uuid) ?: return
        if (session.guard.isSlotAssigned) {
            npcRepo.del(session.guard, Int.MAX_VALUE)
        }
    }

    private fun advance(session: Session, before: CoordGrid) {
        val tail = session.tail
        if (tail.guard != before) {
            session.guard.walk(tail.guard)
        }
        if (tail.isLooking) {
            session.guard.faceSquare(tail.lookingAt)
        }
    }

    private companion object {
        const val GuardType = "npc.vmq1_bag_guard"
        const val SpottedShout = "Hey! What are you doing?"
        const val SessionLifespan = 1500
    }
}
