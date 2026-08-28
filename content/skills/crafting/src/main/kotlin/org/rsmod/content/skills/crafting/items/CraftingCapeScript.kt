package org.rsmod.content.skills.crafting.items

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseCraftingLvl
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn3
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CraftingCapeScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (cape in CraftingConstants.CRAFTING_SKILLCAPES) {
            onOpHeld3(cape) { startTeleport() }
            onOpWorn3(cape) { startTeleport() }
            onOpWorn2(cape) { boostCrafting() }
        }
        onPlayerQueue(TELEPORT_QUEUE) { finishTeleport() }
    }

    private fun ProtectedAccess.boostCrafting() {
        if (!isMasterCrafter()) {
            return
        }
        statBoost(CraftingConstants.STAT_CRAFTING, constant = CAPE_BOOST, percent = 0)
    }

    private fun ProtectedAccess.startTeleport() {
        if (actionDelay > mapClock) {
            return
        }

        if (!isMasterCrafter()) {
            return
        }

        actionDelay = mapClock + TELEPORT_ACTION_DELAY
        anim(TELEPORT_START_ANIM)
        spotanim(TELEPORT_SPOTANIM, height = TELEPORT_SPOTANIM_HEIGHT)
        soundSynth(TELEPORT_SOUND)
        clearQueue(TELEPORT_QUEUE)
        queue(TELEPORT_QUEUE, TELEPORT_DELAY)
    }

    private fun ProtectedAccess.finishTeleport() {
        telejump(GUILD_TELEPORT, TeleportType.Standard)
        resetAnim()
    }

    private fun ProtectedAccess.isMasterCrafter(): Boolean {
        if (player.baseCraftingLvl >= CraftingConstants.MAX_CRAFTING_LEVEL) {
            return true
        }
        mes("You need to have a crafting level of ${CraftingConstants.MAX_CRAFTING_LEVEL}.")
        return false
    }

    private companion object {
        private val GUILD_TELEPORT = CoordGrid(2931, 3286, 0)

        private val TELEPORT_START_ANIM = RSCM.getReverseMapping(RSCMType.SEQ, 714)
        private val TELEPORT_SPOTANIM = RSCM.getReverseMapping(RSCMType.SPOTANIM, 111)

        private const val TELEPORT_SOUND = "synth.teleport_all"
        private const val TELEPORT_QUEUE = "queue.crafting_cape_teleport"
        private const val TELEPORT_SPOTANIM_HEIGHT = 92

        private const val CAPE_BOOST = 1

        private const val TELEPORT_DELAY = 4

        private const val TELEPORT_ACTION_DELAY = 5
    }
}
