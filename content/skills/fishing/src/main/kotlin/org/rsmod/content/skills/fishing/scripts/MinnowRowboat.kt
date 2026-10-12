package org.rsmod.content.skills.fishing.scripts

import dev.openrune.util.Wearpos
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.game.inv.isType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MinnowRowboat : PluginScript() {

    override fun ScriptContext.startup() {
        onOpContentLoc1("content.minnow_rowboat_to_island") { toIsland() }
        onOpContentLoc1("content.minnow_rowboat_to_guild") { toGuild() }
    }

    private suspend fun ProtectedAccess.toIsland() {
        if (player.fishingLvl < 82) {
            startDialogue {
                chatNpcSpecific(
                    title = "Kylie Minnow",
                    type = "npc.minnow_fisherman_unlocked",
                    mesanim = neutral,
                    text =
                        "G'day, only the best fishers are allowed onto the fishing platform. You " +
                            "need a fishing level of 82.",
                )
            }
            return
        }
        // TODO(content): [quest:fishing_contest] Boarding needs varbit.minnow_access = 2 from Kylie,
        // with her "I don't think I have given you access" line; the angler check stands in for it.
        if (!hasFullAngler()) {
            mes("You must be wearing a full angler's outfit before Kylie will let you onto the island.")
            return
        }
        arriveDelay()
        telejump(ISLAND)
    }

    private suspend fun ProtectedAccess.toGuild() {
        arriveDelay()
        telejump(GUILD)
    }

    private companion object {
        private val ISLAND = CoordGrid(2614, 3411, 0)
        private val GUILD = CoordGrid(2599, 3424, 0)
    }
}

internal fun ProtectedAccess.hasFullAngler(): Boolean {
    val hat = player.worn[Wearpos.Hat.slot]
    val top = player.worn[Wearpos.Torso.slot]
    val legs = player.worn[Wearpos.Legs.slot]
    val boots = player.worn[Wearpos.Feet.slot]
    return (hat?.isType("obj.trawler_reward_hat") == true || hat?.isType("obj.spirit_angler_hat") == true) &&
        (top?.isType("obj.trawler_reward_top") == true || top?.isType("obj.spirit_angler_top") == true) &&
        (legs?.isType("obj.trawler_reward_legs") == true || legs?.isType("obj.spirit_angler_legs") == true) &&
        (boots?.isType("obj.trawler_reward_boots") == true || boots?.isType("obj.spirit_angler_boots") == true)
}

