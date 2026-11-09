package org.rsmod.content.areas.misc.warriorsguild.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseAttackLvl
import org.rsmod.api.player.stat.baseStrengthLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.areas.misc.warriorsguild.GuildDoors
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EntranceScript @Inject constructor(private val doors: GuildDoors) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.warguild_door_front") { frontDoor(it.loc) }
        onOpLoc1("loc.warguild_door_dummy") {
            doors.pass(this, it.loc, "loc.warguild_door_inactive_dummy")
        }
        onOpLoc1("loc.warguild_door_catapult") {
            doors.pass(this, it.loc, "loc.warguild_door_inactive_catapult")
        }
    }

    private suspend fun ProtectedAccess.frontDoor(door: BoundLocInfo) {
        val entering = doors.destination(this, door).x < door.coords.x
        if (entering && !player.meetsGuildEntry()) {
            denyEntry()
            return
        }
        doors.pass(this, door, "loc.warguild_door_inactive_front", GuildDoors.FRONT_DOOR_SOUND)
    }

    private suspend fun ProtectedAccess.denyEntry() {
        startDialogue {
            chatNpcSpecific(
                "Ghommal",
                GHOMMAL,
                neutral,
                "You may not enter the Warrior's Guild as you are right now.",
            )
            mesbox(
                "You need a combined Attack and Strength level of at least $COMBINED_LEVEL, or " +
                    "level $MAX_LEVEL in either, to enter the Warriors' Guild.",
            )
        }
    }

    companion object {
        private const val GHOMMAL = "npc.warguild_ghommal_npc"
        private const val COMBINED_LEVEL = 130
        private const val MAX_LEVEL = 99

        fun Player.meetsGuildEntry(): Boolean =
            baseAttackLvl >= MAX_LEVEL ||
                baseStrengthLvl >= MAX_LEVEL ||
                baseAttackLvl + baseStrengthLvl >= COMBINED_LEVEL
    }
}
