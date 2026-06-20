package org.rsmod.content.areas.wilderness

import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.isInPvpCombat
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WildernessAreaScript
@Inject
constructor(
    private val eventBus: EventBus,
) : PluginScript() {

    private var Player.insideWilderness by boolVarBit("varbit.inside_wilderness")
    private var Player.specialSpecOrb by intVarBit("varbit.pvp_area_client")

    override fun ScriptContext.startup() {
        onPlayerLogin {
            if (player.isSkullEquipLocked()) {
                player.refreshSkullIcon()
                return@onPlayerLogin
            }
            if (player.isSkulled()) {
                player.expireForinthrySurgeIfNeeded()
                val source = player.attr.getOrDefault(SKULL_SOURCE, SkullSource.COMBAT)
                if (source.timerTicks != Int.MAX_VALUE) {
                    player.softTimer("timer.skull_timer", source.timerTicks)
                }
                player.refreshSkullIcon()
            }
        }

        onPlayerSoftTimer("timer.skull_timer") {
            if (player.isSkullEquipLocked()) {
                return@onPlayerSoftTimer
            }
            if (player.isInPvpCombat()) {
                player.softTimer("timer.skull_timer", 10)
                return@onPlayerSoftTimer
            }
            player.expireForinthrySurgeIfNeeded()
            if (player.isSkulled()) {
                player.clearSkull()
            }
        }

        onArea("area.wilderness") {
            player.insideWilderness = true
            if (!player.hasWildernessOptOut()) {
                setAttackable(player)
            }
        }

        onAreaExit("area.wilderness") {
            player.insideWilderness = false
            setUnAttackable(player)
        }
    }

    private fun setAttackable(player: Player) {
        player.setCanPvp(true)
        player.specialSpecOrb = 1
        player.ifOpenOverlay(
            "interface.pvp_icons",
            "component.toplevel_osrs_stretch:pvp_icons",
            eventBus,
        )
    }

    private fun setUnAttackable(player: Player) {
        player.setCanPvp(false)
        player.specialSpecOrb = 0
        player.ifCloseOverlay("interface.pvp_icons", eventBus)
    }

    companion object {
        private val WILDERNESS_OPT_OUT = AttributeKey<Boolean>(persistenceKey = "wilderness_opt_out")
        private val CAN_PVP = AttributeKey<Boolean>()
        private val DUEL_FLAG = AttributeKey<Boolean>()

        fun Player.hasWildernessOptOut(): Boolean = attr.getOrDefault(WILDERNESS_OPT_OUT, false)

        fun Player.setWildernessOptOut(optOut: Boolean) {
            if (hasWildernessOptOut() == optOut) return
            attr[WILDERNESS_OPT_OUT] = optOut
            refreshPlayerOptions()
        }

        fun Player.canPvp(): Boolean = attr.getOrDefault(CAN_PVP, false) && !hasWildernessOptOut()

        fun Player.isDueling(): Boolean = attr.getOrDefault(DUEL_FLAG, false)

        fun Player.setCanPvp(canPvp: Boolean, duel: Boolean = false) {
            val currentPvp = attr.getOrDefault(CAN_PVP, false)
            val currentDuel = isDueling()
            if (currentPvp == canPvp && currentDuel == duel) return
            attr[CAN_PVP] = canPvp
            attr[DUEL_FLAG] = duel
            refreshPlayerOptions()
        }

        private fun Player.refreshPlayerOptions() {
            when {
                canPvp() -> MiscOutput.setPlayerOp(
                    this,
                    slot = 1,
                    op = if (isDueling()) "Fight" else "Attack",
                    priority = true,
                )
                isDueling() -> MiscOutput.setPlayerOp(this, slot = 1, op = "Challenge", priority = false)
                else -> {
                    MiscOutput.clearPlayerOp(this, 1, "Attack")
                    MiscOutput.clearPlayerOp(this, 1, "Fight")
                    MiscOutput.clearPlayerOp(this, 1, "Challenge")
                }
            }
        }
    }
}
