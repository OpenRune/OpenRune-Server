package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.cookingLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.generic.locs.doors.DoorTranslations
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CookingGuildDoor @Inject constructor(
    private val locRepo: LocRepository,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1("loc.chefdoor") {
            when {
                !player.hasGuildEntryOutfit() && player.cookingLvl >= 32 ->
                    denyEntry {
                        chatNpcSpecific(
                            title = "Head chef",
                            type = "npc.cook",
                            mesanim = neutral,
                            text = "You can't come in here unless you're wearing a chef's hat, or something like that.",
                        )
                    }

                !player.hasGuildEntryOutfit() && player.cookingLvl < 32 ->
                    denyEntry {
                        chatNpcSpecific(
                            title = "Head chef",
                            type = "npc.cook",
                            mesanim = neutral,
                            text =
                                "Sorry. Only the finest chefs are allowed in here. Get your cooking level up to 32 " +
                                    "and come back wearing a chef's hat.",
                        )
                    }

                player.hasGuildEntryOutfit() && player.cookingLvl < 32 ->
                    denyEntry {
                        chatNpcSpecific(
                            title = "Head chef",
                            type = "npc.cook",
                            mesanim = neutral,
                            text =
                                "Sorry. Only the finest chefs are allowed in here. Get your cooking level up to 32.",
                        )
                    }

                else -> walkThroughDoor(it.vis)
            }
        }
    }

    private suspend fun ProtectedAccess.denyEntry(lines: suspend Dialogue.() -> Unit) {
        startDialogue { lines() }
    }

    private suspend fun ProtectedAccess.walkThroughDoor(door: BoundLocInfo) {
        val doorCoords = door.coords
        val south = coords.z <= doorCoords.z
        val walkTo = if (south) doorCoords.translateZ(1) else doorCoords.translateZ(-1)

        val openAngle = door.turnAngle(rotations = 1)
        val openCoords = DoorTranslations.translateOpen(doorCoords, door.shape, door.angle)

        locRepo.del(door, 3)
        locRepo.add(openCoords, "loc.chefdoor_open", 3, openAngle, door.shape)

        teleport(walkTo)
    }
}
