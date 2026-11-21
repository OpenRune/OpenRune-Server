package org.rsmod.content.quest.area.varrock.dragonslayer

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.generic.locs.doors.DoorTranslations
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The Champions' Guild front door only lets in adventurers with 32 quest points. */
class ChampionsGuildDoor @Inject constructor(private val locRepo: LocRepository) :
    PluginScript() {
    private val Player.questPoints by intVarp("varp.qp")

    override fun ScriptContext.startup() {
        onOpLoc1(DOOR) { useDoor(it.loc) }
    }

    private suspend fun ProtectedAccess.useDoor(door: BoundLocInfo) {
        arriveDelay()
        val entering = coords.z >= door.coords.z
        if (entering && !meetsQuestPoints(player)) {
            startDialogue {
                chatNpcSpecific(
                    GUILDMASTER_NAME,
                    DragonSlayerQuest.GUILDMASTER,
                    neutral,
                    "You have not proved yourself worthy to enter here yet.",
                )
                mesbox("The door won't open - you need at least $REQUIRED_QUEST_POINTS Quest Points.")
            }
            return
        }
        val across = if (entering) door.coords.translateZ(-1) else door.coords
        soundSynth(DOOR_SOUND)
        val openedAt = DoorTranslations.translateOpen(door.coords, door.shape, door.angle)
        locRepo.change(door, INVISIBLE_WALL, DOOR_TICKS)
        locRepo.add(openedAt, OPEN_DOOR, DOOR_TICKS, door.turnAngle(1), door.shape)
        forcedWalk(across, 1)
    }

    /** Quest points only gate the guild when the server respects real quest progress. */
    private fun meetsQuestPoints(player: Player): Boolean =
        QuestRequirements.activePolicy().mode != QuestRequirementMode.RespectProgress ||
            player.questPoints >= REQUIRED_QUEST_POINTS

    private companion object {
        const val DOOR = "loc.championdoor"
        const val OPEN_DOOR = "loc.championdoor_open"
        const val INVISIBLE_WALL = "loc.inviswall"
        const val DOOR_SOUND = "synth.door_open"
        const val DOOR_TICKS = 3
        const val REQUIRED_QUEST_POINTS = 32
        const val GUILDMASTER_NAME = "Guildmaster"
    }
}
