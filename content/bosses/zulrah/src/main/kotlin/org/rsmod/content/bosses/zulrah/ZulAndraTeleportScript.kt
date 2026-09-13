package org.rsmod.content.bosses.zulrah

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.content.quest.manager.QuestRequirementResolver
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class ZulAndraTeleportScript @Inject constructor(
    private val quests: QuestRequirementResolver,
    private val teleports: PlayerTeleportValidator,
    private val areas: AreaChecker,
    private val world: WorldRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(SCROLL) { readScroll(it.inventory, it.slot) }
    }

    private suspend fun ProtectedAccess.readScroll(inventory: Inventory, slot: Int) {
        if (actionDelay > mapClock || !canUseScroll()) return
        if (invDel(inventory, SCROLL, count = 1, slot = slot, ignoreVirtualStorage = true).failure) return
        anim("seq.teleport_scroll_open")
        spotanimMap(world, "spotanim.telescroll_teleport", coords)
        soundArea(world, coords, "synth.teleport_all", radius = 10)
        delay(3)
        if (!canUseScroll()) return
        telejump(ZulrahIsland.zulAndraTeleport, TeleportType.Standard)
        resetAnim()
    }

    private fun ProtectedAccess.canUseScroll(): Boolean {
        if (!quests.hasCompleted(player, "quest_regicide")) {
            mes(REGICIDE_MESSAGE)
            return false
        }
        val denial = teleports.validate(player, TeleportType.Standard, areas)
        if (denial != null) {
            mes(denial)
            return false
        }
        return true
    }

    companion object {
        const val SCROLL = "obj.teleportscroll_zulandra"
        const val REGICIDE_MESSAGE =
            "You need to complete the Regicide quest before you can teleport to Zul-Andra."
    }
}
