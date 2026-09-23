package org.rsmod.content.bosses.deserttreasure2.vardorvis

import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.api.instances.withInstanceLeaveTransition
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onPlayerInit
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class VardorvisInstance @Inject constructor(registry: BossInstanceRegistry) :
    InstanceScript(registry) {

    private var Player.stranglewoodProgress by intVarBit(STRANGLEWOOD_VARBIT)

    private val pendingAwakened = HashSet<Player>()

    override fun settingsRow(): String = "dbrow.instance_vardorvis"

    override fun area(): InstanceArea = INSTANCE

    override fun ScriptContext.configure() {
        onEnterPrelude { result, enter ->
            withInstanceEnterTransition(InstanceEnterTransition(message = ENTER_MESSAGE), enter)
            if (result is InstanceManager.Result.Created && pendingAwakened.remove(player)) {
                manager.npcsForInstance(result.session.id).firstOrNull()?.let(::markAwakened)
            }
        }

        onPlayerInit {
            if (player.stranglewoodProgress < STRANGLEWOOD_UNLOCKED) {
                player.stranglewoodProgress = STRANGLEWOOD_UNLOCKED
            }
        }

        onEnterObject { enterInstance() }
        onExitObject { defaultLeaveFlow() }

        ARENA_ESCAPE_LOCS.forEach { loc -> onOpLoc5(loc) { quickEscape() } }
    }

    private suspend fun ProtectedAccess.quickEscape() {
        withInstanceLeaveTransition { defaultLeaveFlow() }
    }

    private suspend fun ProtectedAccess.enterInstance() {
        val owned = player.uuid?.let { manager.sessionOwnedBy(key, it) }
        if (owned == null && tryConsumeAwakenersOrb()) {
            pendingAwakened += player
        }
        defaultInstanceEntry()
    }

    private suspend fun ProtectedAccess.tryConsumeAwakenersOrb(): Boolean {
        if (AWAKENERS_ORB !in inv) {
            return false
        }
        val useOrb =
            choice2(
                "Yes - consume an Awakener's orb.",
                true,
                "No - fight the normal encounter.",
                false,
                title = "Use an Awakener's orb to fight an Awakened Vardorvis?",
            )
        if (!useOrb) {
            return false
        }
        invDel(inv, AWAKENERS_ORB, 1)
        return true
    }

    private fun markAwakened(npc: Npc) {
        npc.vars["varn.awakened_state"] = 1
        npc.vars["varn.skip_killcount"] = 1
        npc.baseHitpointsLvl *= 2
        npc.hitpoints = npc.baseHitpointsLvl
    }

    private companion object {
        private const val STRANGLEWOOD_VARBIT = "varbit.dt2_stranglewood"

        private const val STRANGLEWOOD_UNLOCKED = 37

        private const val ARENA_REGION = 4405

        private const val AWAKENERS_ORB = "obj.dt2_awakeners_orb"

        private const val ENTER_MESSAGE = "You enter the ritual site."

        private val ARENA_ESCAPE_LOCS =
            arrayOf(
                "loc.vardorvis_escape_1",
                "loc.vardorvis_escape_2",
                "loc.vardorvis_escape_3",
            )

        private val INSTANCE =
            InstanceArea.copyRegions(
                regionIds = listOf(ARENA_REGION),
                npcSpawns = listOf(InstanceNpc("npc.vardorvis", CoordGrid(1128, 3417, 0))),
            )
    }
}
