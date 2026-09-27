package org.rsmod.content.bosses.leviathan

import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.withInstanceLeaveTransition
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.plugin.scripts.ScriptContext

class LeviathanInstance
@Inject
internal constructor(registry: BossInstanceRegistry, private val encounters: LeviathanEncounters) :
    InstanceScript(registry) {

    override fun settingsRow(): String = "dbrow.instance_leviathan"

    override fun area(): InstanceArea = INSTANCE

    override fun ScriptContext.configure() {
        onEnterTransition(InstanceEnterTransition(message = ROW_MESSAGE))
        onExitObject { withInstanceLeaveTransition(InstanceEnterTransition(message = ROW_BACK_MESSAGE)) { defaultLeaveFlow() } }
        onOpLoc2(LeviathanEncounters.BOAT_ESCAPE_LOC) { defaultLeaveFlow() }
        onOpLoc2(LeviathanEncounters.BOAT_LEAVE_LOC) {
            withInstanceLeaveTransition(InstanceEnterTransition(message = ROW_BACK_MESSAGE)) { defaultLeaveFlow() }
        }

        onInstancePlayerJoin {
            val session = manager.sessionForId(instanceId) ?: return@onInstancePlayerJoin
            encounters.placeArenaLocs(session)
            encounters.ensureTails(session)
        }

        onOpLoc1(LeviathanEncounters.HANDHOLDS_ENTER_LOC) { climb(it.loc.coords.x) }
        onOpLoc1(LeviathanEncounters.HANDHOLDS_EXIT_LOC) { climb(it.loc.coords.x) }
    }

    private suspend fun ProtectedAccess.climb(handholdsX: Int) {
        val session = manager.sessionForPlayer(player) ?: return
        val climbingIn = player.coords.x < handholdsX
        val awakened = climbingIn && !encounters.hasBoss(session) && tryConsumeAwakenersOrb()

        anim(CLIMB_SEQ)
        delay(1)
        telejump(player.coords.translate(if (climbingIn) CLIMB_DISTANCE else -CLIMB_DISTANCE, 0))

        if (!climbingIn) {
            encounters.setHandholds(session, LeviathanEncounters.HANDHOLDS_ENTER_LOC)
            return
        }
        mes(HANDHOLDS_MESSAGE, ChatType.Spam)
        if (!encounters.hasBoss(session)) {
            encounters.beginEncounter(session, player, awakened, SPAWN_DELAY)
        }
    }

    private suspend fun ProtectedAccess.tryConsumeAwakenersOrb(): Boolean {
        if (AWAKENERS_ORB !in inv) return false
        val useOrb =
            choice2(
                "Yes - consume an Awakener's orb.",
                true,
                "No - fight the normal encounter.",
                false,
                title = "Use an Awakener's orb to fight an Awakened Leviathan?",
            )
        if (!useOrb) return false
        invDel(inv, AWAKENERS_ORB, 1)
        return true
    }

    private companion object {
        private const val ARENA_REGION = 8291
        private const val AWAKENERS_ORB = "obj.dt2_awakeners_orb"
        private const val CLIMB_SEQ = "seq.human_reachforladder"
        private const val CLIMB_DISTANCE = 2
        private const val SPAWN_DELAY = 5
        private const val ROW_MESSAGE = "You row out to the island..."
        private const val ROW_BACK_MESSAGE = "You row back to the camp..."
        private const val HANDHOLDS_MESSAGE = "The brain holds fall away as you use them."

        private val INSTANCE = InstanceArea.copyRegions(regionIds = listOf(ARENA_REGION))
    }
}
