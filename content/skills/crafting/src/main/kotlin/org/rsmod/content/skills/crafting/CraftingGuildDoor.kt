package org.rsmod.content.skills.crafting

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.generic.locs.doors.DoorTranslations
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.map.Direction
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CraftingGuildDoor @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(GUILD_DOOR) {
            val door = it.vis

            if (coords.z < door.coords.z) {
                walkThroughDoor(door)
                return@onOpLoc1
            }

            when {
                !player.hasGuildEntryOutfit() ->
                    denyEntry {
                        chatNpcSpecific(
                            title = MASTER_CRAFTER_TITLE,
                            type = MASTER_CRAFTER_NPC,
                            mesanim = neutral,
                            text = "Where's your brown apron? You can't come in here unless you're wearing one.",
                        )
                        chatPlayer(neutral, "Err... I haven't got one.")
                    }

                player.craftingLvl < GUILD_ENTRY_LEVEL ->
                    denyEntry {
                        chatNpcSpecific(
                            title = MASTER_CRAFTER_TITLE,
                            type = MASTER_CRAFTER_NPC,
                            mesanim = neutral,
                            text = "Sorry, only experienced crafters are allowed in here. You must be " +
                                "level $GUILD_ENTRY_LEVEL or above to enter.",
                        )
                    }

                else -> {
                    walkThroughDoor(door)
                    startDialogue {
                        chatNpcSpecific(
                            title = MASTER_CRAFTER_TITLE,
                            type = MASTER_CRAFTER_NPC,
                            mesanim = happy,
                            text = "Welcome to the Guild of Master Craftsmen.",
                        )
                    }
                }
            }
        }
    }

    private suspend fun ProtectedAccess.denyEntry(lines: suspend Dialogue.() -> Unit) {
        startDialogue { lines() }
    }

    private suspend fun ProtectedAccess.walkThroughDoor(door: BoundLocInfo) {
        val doorCoords = door.coords
        val leaving = coords.z < doorCoords.z
        val walkTo = if (leaving) doorCoords else doorCoords.translateZ(-1)

        val openAngle = door.turnAngle(rotations = 1)
        val openCoords = DoorTranslations.translateOpen(doorCoords, door.shape, door.angle)

        locRepo.del(door, 3)
        locRepo.add(openCoords, GUILD_DOOR_OPEN, 3, openAngle, door.shape)

        teleport(walkTo)
        faceDirection(if (leaving) Direction.North else Direction.South)

        delay(2)
    }

    private companion object {
        private const val GUILD_ENTRY_LEVEL = 40
        private const val MASTER_CRAFTER_TITLE = "Master Crafter"
        private const val MASTER_CRAFTER_NPC = "npc.master_crafter"

        private const val GUILD_DOOR = "loc.craftingguilddoor"
        private const val GUILD_DOOR_OPEN = "loc.inactivepoordoor"
    }
}
