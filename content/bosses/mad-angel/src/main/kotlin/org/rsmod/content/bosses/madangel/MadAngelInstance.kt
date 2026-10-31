package org.rsmod.content.bosses.madangel

import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.table.QuestRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The cathedral copy the angel fights in, entered by climbing onto the broken pew.
 *
 * The angel is a spawn of the copy itself, so every entry (and every rejoin of an emptied copy)
 * gets a fresh, dormant one; the pew it sits beside is swapped for the exit variant, so the same
 * tile gets the player in and out.
 */
class MadAngelInstance
@Inject
constructor(
    registry: BossInstanceRegistry,
    private val locRepo: LocRepository,
) : InstanceScript(registry) {

    override fun settingsRow(): String = "dbrow.instance_mad_angel"

    override fun area(): InstanceArea = INSTANCE

    override fun runsPreludeOnFreshRun(): Boolean = true

    override fun ScriptContext.configure() {
        onEnterPrelude { result, enter ->
            val session =
                when (result) {
                    is InstanceManager.Result.Created -> result.session
                    is InstanceManager.Result.Joined -> result.session
                    else -> return@onEnterPrelude
                }
            assumeFallenFromGraceComplete()
            mesboxNp(CLIMB_MESSAGE)
            climbAnimation()
            fadeOverlay(0, 255, 0, 0, FADE_CLIENT_DURATION)
            delay(ENTRY_FADE_CYCLES)
            minimapHideMap()
            enter()
            faceDirection(Direction.West)
            delay(ENTRY_REVEAL_CYCLES)
            minimapReset()
            fadeOverlay(0, 0, 0, 255, FADE_CLIENT_DURATION)
            if (result is InstanceManager.Result.Created) placeExitPew(session)
            mesbox(CLIMB_MESSAGE)
            closeFadeOverlay()
        }
        onEnterObject { defaultInstanceEntry() }
        onExitObject {
            climbAnimation()
            fadeOverlay(0, 255, 0, 0, FADE_CLIENT_DURATION)
            delay(HIDE_DELAY_CYCLES)
            minimapHideMap()
            defaultLeaveFlow()
            delay(REVEAL_DELAY_CYCLES)
            minimapReset()
            faceDirection(Direction.East)
            fadeOverlay(0, 0, 0, 255, FADE_CLIENT_DURATION)
            closeFadeOverlay()
        }
        onInstancePlayerLeave {
            player.nextHitBuff = MadAngelFight.NEXT_HIT_NONE
            for (angel in manager.npcsForInstance(instanceId)) {
                angel.vars[MadAngelFight.GUARANTEED_HIT] = 0
            }
        }
    }

    private fun ProtectedAccess.climbAnimation() {
        anim(CLIMB_ANIM, delay = CLIMB_ANIM_DELAY)
    }

    /**
     * The dormant angel only offers `Wake` once `varbit.ffg` reaches the quest-complete value; the
     * client hides the op while the varbit is in the quest range. The repeatable encounter is
     * post-quest content, so entering it treats Fallen From Grace as completed when the active quest
     * policy does. With real quest progress the varbit is left to the quest.
     */
    private fun ProtectedAccess.assumeFallenFromGraceComplete() {
        if (!QuestRequirements.hasCompleted(player, QUEST)) return
        if (vars[VARBIT_FFG] < FFG_QUEST_COMPLETE) vars[VARBIT_FFG] = FFG_QUEST_COMPLETE
    }

    private fun placeExitPew(session: InstanceSession) {
        val coords = manager.resolveCoord(session, PEW_TEMPLATE_COORD) ?: return
        locRepo.add(coords, EXIT_PEW, Int.MAX_VALUE, LocAngle[PEW_ROTATION], PEW_SHAPE)
    }

    private companion object {
        private val NPC = "npc.mad_angel"
        private const val QUEST = "quest_fallenfromgrace"

        private const val VARBIT_FFG = "varbit.ffg"
        private val FFG_QUEST_COMPLETE: Int by lazy { QuestRow.getRow("dbrow.$QUEST").endstate }

        private const val CLIMB_MESSAGE = "You climb over the broken pew..."
        private const val CLIMB_ANIM = "seq.human_pickuptable"
        private const val CLIMB_ANIM_DELAY = 30
        private const val FADE_CLIENT_DURATION = 50
        private const val ENTRY_FADE_CYCLES = 2
        private const val ENTRY_REVEAL_CYCLES = 1
        private const val HIDE_DELAY_CYCLES = 1
        private const val REVEAL_DELAY_CYCLES = 2

        private const val EXIT_PEW = "loc.wyrmscraig_cathedral_pew_broken_exit"
        private const val PEW_ROTATION = 1
        private val PEW_SHAPE = LocShape.CentrepieceStraight

        private val SPAWN_TEMPLATE_COORD = CoordGrid(2532, 2215)
        private val PEW_TEMPLATE_COORD = CoordGrid(2538, 2215)

        private val INSTANCE =
            InstanceArea.copyRegions(
                centerRegionId = 10018,
                npcSpawns = listOf(InstanceNpc(NPC, SPAWN_TEMPLATE_COORD)),
            )
    }
}
