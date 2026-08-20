package org.rsmod.content.areas.godwars

import jakarta.inject.Inject
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.events.InstancePlayerJoinUnboundEvent
import org.rsmod.api.instances.events.InstancePlayerLeaveUnboundEvent
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onEvent
import org.rsmod.api.table.InstanceSettingsRow
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GodwarsOverlayScript
@Inject
constructor(private val eventBus: EventBus, private val instances: InstanceManager) :
    PluginScript() {
    /**
     * The overlay's client script hides the killcount layer unless the player stands in the
     * dungeon's map squares. Boss rooms are on dynamic map, so this flag stands in for that check.
     */
    private var Player.inBossInstance by boolVarBit("varbit.godwars_instance")

    override fun ScriptContext.startup() {
        val bossKeys =
            BOSS_INSTANCE_ROWS.mapTo(mutableSetOf()) { InstanceSettingsRow.getRow(it).key }

        onArea("area.godwars_dungeon") {
            player.ifOpenOverlay(
                "interface.godwars_overlay",
                "component.toplevel_osrs_stretch:overlay_hud",
                eventBus,
            )
            val sessionKey = instances.sessionForPlayer(player)?.key
            player.inBossInstance = sessionKey != null && sessionKey in bossKeys
        }

        onAreaExit("area.godwars_dungeon") {
            player.ifCloseOverlay("interface.godwars_overlay", eventBus)
        }

        onEvent<InstancePlayerJoinUnboundEvent> {
            if (key in bossKeys) {
                player.inBossInstance = true
            }
        }

        onEvent<InstancePlayerLeaveUnboundEvent> {
            if (key in bossKeys) {
                player.inBossInstance = false
            }
        }
    }

    private companion object {
        val BOSS_INSTANCE_ROWS =
            listOf(
                "dbrow.instance_graardor",
                "dbrow.instance_kreearra",
                "dbrow.instance_zilyana",
                "dbrow.instance_kril",
            )
    }
}
