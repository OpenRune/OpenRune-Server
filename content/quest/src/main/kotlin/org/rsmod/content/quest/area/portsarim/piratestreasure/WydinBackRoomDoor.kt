package org.rsmod.content.quest.area.portsarim.piratestreasure

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.generic.locs.doors.DoorTranslations
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureSmuggling.Companion.APRON
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureSmuggling.Companion.WYDIN
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WydinBackRoomDoor
@Inject
constructor(
    private val smuggling: PiratesTreasureSmuggling,
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(DOOR) { useDoor(it.loc) }
    }

    private suspend fun ProtectedAccess.useDoor(door: BoundLocInfo) {
        arriveDelay()
        if (player.coords.x < door.coords.x) {
            passDoor(door)
            return
        }
        if (!smuggling.isHiredByWydin(player)) {
            val wydin = findWydin()
            if (wydin == null) {
                startDialogue { chatNpcSpecific(WYDIN_NAME, WYDIN, angry, BACK_ROOM_WARNING) }
                return
            }
            startDialogue(wydin) {
                chatNpc(angry, BACK_ROOM_WARNING)
                val askForJob =
                    smuggling.canAskWydinForJob(player) &&
                        choice2("Well, can I get a job here?", true, "Sorry, I didn't realise.", false)
                if (askForJob) {
                    with(smuggling) { askWydinForJob() }
                } else {
                    chatPlayer(sad, "Sorry, I didn't realise.")
                }
            }
            return
        }
        if (!player.worn.contains(APRON)) {
            startDialogue {
                chatNpcSpecific(
                    WYDIN_NAME,
                    WYDIN,
                    neutral,
                    "Can you put your white apron on before going in there, please?",
                )
            }
            return
        }
        passDoor(door)
    }

    private fun ProtectedAccess.findWydin(): Npc? =
        npcRepo.findAll(ZoneKey.from(player.coords), zoneRadius = 1).firstOrNull {
            it.isType(WYDIN) && it.coords.level == player.coords.level
        }

    private suspend fun ProtectedAccess.passDoor(door: BoundLocInfo) {
        val across =
            if (player.coords.x < door.coords.x) {
                door.coords
            } else {
                CoordGrid(door.coords.x - 1, door.coords.z, door.coords.level)
            }
        soundSynth(DOOR_OPEN_SOUND)
        val openedAt = DoorTranslations.translateOpen(door.coords, door.shape, door.angle)
        locRepo.change(door, INVISIBLE_WALL, DOOR_TICKS)
        locRepo.add(openedAt, OPEN_DOOR, DOOR_TICKS, door.turnAngle(1), door.shape)
        forcedWalk(across, 1)
    }

    private companion object {
        const val DOOR = "loc.wydindoor"
        const val OPEN_DOOR = "loc.wydindooropen"
        const val WYDIN_NAME = "Wydin"
        const val BACK_ROOM_WARNING =
            "Hey, you can't go in there. Only employees of the grocery store can go in."
        const val DOOR_OPEN_SOUND = "synth.door_open"
        const val INVISIBLE_WALL = "loc.inviswall"
        const val DOOR_TICKS = 3
    }
}
