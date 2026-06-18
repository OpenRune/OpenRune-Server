package org.rsmod.content.areas.wilderness

import jakarta.inject.Inject
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.config.constants
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext


//TODO PJ Timer
//TODO Obelisks
//TODO Level 30 tele block
//MAKE ADMIN+ not be able to attack players or be attacked
class WildernessAreaScript @Inject constructor(
    private val eventBus: EventBus,
    private val areaChecker: AreaChecker
) : PluginScript() {

    private var Player.insideWilderness by boolVarBit("varbit.inside_wilderness")
    private var Player.specialSpecOrb by intVarBit("varbit.pvp_area_client")

    override fun ScriptContext.startup() {
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

        player.ifCloseOverlay(
            "interface.pvp_icons",
            eventBus,
        )
    }


    //Todo Hook this to proper combat maybe add some kind of hook ?
    fun canAttack(player: Player, target: Player): Boolean {
        if (areaChecker.inArea("area.ferox_enclave", target.coords)
            && target.vars["varbit.teleblock_cycles"] <= 0
            && player.vars["varbit.teleblock_cycles"] <= 0
        ) {
            player.mes("You cannot fight another player whilst next to the Enclave, please move further out.")
            return false
        }

        if (!player.attr.getOrDefault(IS_SKULLED, false) && player.vars["varbit.skull_prevent_enabled"] == 0 && canSkull(player, target)) {
            player.mes("You cannot attack this target as it would result in you getting skulled.")
            return false
        }

        if (player.canPvp() && !target.canPvp()) {
            player.mes("That player is not in the wilderness.")
            return false
        }

        val level = getWildernessLevel(player.coords)
        val otherLevel = getWildernessLevel(target.coords)
        val minimumLevel = minOf(level, otherLevel)

        if (minimumLevel >= 1) {
            if (abs(player.appearance.combatLevel - player.appearance.combatLevel) > minimumLevel) {
                player.mes("The difference between your Combat level and the Combat level of " + target.displayName + " is too great.")
                val pronouns = when(target.vars["varbit.settings_transmit_pronouns"]) {
                    0 -> "He"
                    1 -> "She"
                    2 -> "They"
                    else -> if (target.appearance.bodyType == constants.bodytype_a) "He" else "She"
                }
                player.mes("$pronouns needs to move deeper into the Wilderness before you can attack them.")
                return false
            }
        }


        return true
    }

    //Todo add a hook?
    fun onAttack(player: Player, target: Player) {
        if (canSkull(player, target)) {
            player.setSkull(true)
            //Set Target to who attacked last for skulling?
        }
    }

    //Todo add a hook?
    fun onPlayerDeath(player: Player, target: Player) {

    }

    //Todo add a hook? as dropping items in wildy has special effects
    fun onItemDrop(player: Player, target: Player) {

    }

    //Todo add Proper skulling https://oldschool.runescape.wiki/w/Skull_(status)
    fun canSkull(player: Player, target: Player) : Boolean {
        return true
    }

    fun Player.setSkull(skulled: Boolean) {
        attr[IS_SKULLED] = skulled
        appearance.clearRebuildFlag()
    }

    fun getWildernessLevel(tile: CoordGrid): Int {
        val x = tile.x
        val y = tile.z

        if (!areaChecker.inArea("area.wilderness", tile)) {
            return -1
        }

        return when (x) {
            in 2944..3392 if y in 3520..4351 -> ((y - 3520) shr 3) + 1
            in 3008..3071 if y in 10112..10175 -> ((y - 9920) shr 3) - 1
            in 2944..3455 if y in 9920..10879 -> ((y - 9920) shr 3) + 1
            in 1725..1919 if y in 11520..11583 -> 21
            in 1600..1663 if y in 11520..11583 -> 29
            else -> -1
        }
    }

    companion object {
        private val WILDERNESS_OPT_OUT = AttributeKey<Boolean>(persistenceKey = "wilderness_opt_out")
        private val IS_SKULLED = AttributeKey<Boolean>(persistenceKey = "is_skulled")

        private val CAN_PVP = AttributeKey<Boolean>()
        private val DUEL_FLAG = AttributeKey<Boolean>()

        fun Player.hasWildernessOptOut(): Boolean = attr.getOrDefault(WILDERNESS_OPT_OUT, false)

        fun Player.setWildernessOptOut(optOut: Boolean) {
            val current = hasWildernessOptOut()
            if (current == optOut) {
                return
            }

            attr[WILDERNESS_OPT_OUT] = optOut
            refreshPlayerOptions()
        }

        fun Player.canPvp(): Boolean = attr.getOrDefault(CAN_PVP, false) && !hasWildernessOptOut()

        fun Player.isDueling(): Boolean = attr.getOrDefault(DUEL_FLAG, false)

        fun Player.setCanPvp(canPvp: Boolean, duel: Boolean = false) {
            val currentPvp = attr.getOrDefault(CAN_PVP, false)
            val currentDuel = isDueling()

            if (currentPvp == canPvp && currentDuel == duel) {
                return
            }

            attr[CAN_PVP] = canPvp
            attr[DUEL_FLAG] = duel

            refreshPlayerOptions()
        }

        private fun Player.refreshPlayerOptions() {
            when {
                canPvp() -> MiscOutput.setPlayerOp(this, slot = 1, op = if (isDueling()) "Fight" else "Attack", priority = true)
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
