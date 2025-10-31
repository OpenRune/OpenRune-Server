package org.alter.plugins.content.interfaces.gameframe.orbs.xpdrops

import dev.openrune.ServerCacheManager.getEnumOrDefault
import kotlin.math.roundToInt
import org.alter.api.ClientScript
import org.alter.api.cfg.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.skill.SkillSet
import org.alter.game.plugin.*
import org.alter.plugins.content.interfaces.xpdrops.XpSettings

/**
 * @author CloudS3c
 */
class XpSettingsComponentsPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        val POSITION_DROPDOWN_ID = 51
        val SIZE_DROPDOWN_ID = 52
        val SPEED_DROPDOWN_ID = 58
        val DURATION_DROPDOWN_ID = 53
        val COUNTER_DROPDOWN_ID = 54
        val PROGRESS_BAR_DROPDOWN_ID = 55
        val COLOUR_BAR_DROPDOWN_ID = 56
        val GROUP_BAR_DROPDOWN_ID = 57
        val FAKE_DROPS_DROPDOWN_ID = 59

        val CONFIGURE_SKILL = 17 // Skills selected by Slot
        val RETURN_TO_SETTINGS = 45

        val NO_TRACKER_OR_GOAL_COMPONENT_ID = 21
        val TRACKER_COMPONENT_ID = 25
        val GOAL_COMPONENT_ID = 33
        val TRACKER_SET = 30

        val skillEnum = getEnumOrDefault(681)

        listOf(
            Pair(POSITION_DROPDOWN_ID, "varbits.xpdrops_position"),
            Pair(SIZE_DROPDOWN_ID, "varbits.xpdrops_size"),
            Pair(SPEED_DROPDOWN_ID, "varbits.xpdrops_speed"),
            Pair(DURATION_DROPDOWN_ID, "varbits.xpdrops_duration"),
            Pair(COUNTER_DROPDOWN_ID, "varbits.xpdrops_counter_type"),
            Pair(PROGRESS_BAR_DROPDOWN_ID, "varbits.xpdrops_progress_type"),
            Pair(COLOUR_BAR_DROPDOWN_ID, "varbits.xpdrops_colour"),
            Pair(GROUP_BAR_DROPDOWN_ID, "varbits.xpdrops_groupskills"),
            Pair(FAKE_DROPS_DROPDOWN_ID, "varbits.xpdrops_fakesoff"),
        ).forEach { (button, varbit) ->
            on_xp_button(button) {
                player.setVarbit(varbit, player.getInteractingSlot() - 1)
                player.runClientScript(ClientScript("xpdrops_fake"), 3, 20000001) // @TODO
            }
        }

// Skill Selection
        on_xp_button(CONFIGURE_SKILL) {
            player.setVarbit("varbits.xpdrops_setup_skill", player.getInteractingSlot() + 1)
            player.setVarbit("varbits.xpdrops_setup_skill", 0)
            player.setVarbit("varbits.xpdrops_setup_skill", player.getInteractingSlot() + 1)
        }

// Reset when we Discard.
        on_xp_button(RETURN_TO_SETTINGS) {
            player.setVarbit("varbits.xpdrops_setup_skill", 0)
            if (player.getVarbit("varbits.xpdrops_setup_type") > 0) {
                player.setVarbit("varbits.xpdrops_setup_type", 0)
            }
            player.setVarbit("varbits.xpdrops_setup_skill", 1) // Actl on osrs they set this one to wut it was before but i aint wasting memory on that shit.
            player.setVarbit("varbits.xpdrops_setup_skill", 0)
            player.setVarp("varp.if1", -1)
            player.setVarp("varp.if2", -1)
        }
// No tracker or Goal
        on_xp_button(NO_TRACKER_OR_GOAL_COMPONENT_ID) {
            player.setVarbit("varbits.xpdrops_setup_type", 0)
        }

// Tracker
        on_xp_button(TRACKER_COMPONENT_ID) {
            player.setVarp("varp.if2", -1) // Tf Makes 61512 ?
            player.setVarp("varp.if1", -1) // Tf Makes 61512 ?
            player.setVarbit("varbits.xpdrops_setup_type", 1)
            val slot = player.getVarbit("varbits.xpdrops_setup_skill")
            when {
                slot in 0..23 -> {
                    val skillEnum = getEnumOrDefault(681)
                    player.setVarp("varp.if1", player.getSkills()[skillEnum.getInt(player.getVarbit("varbits.xpdrops_setup_skill"))].xp.roundToInt(),
                    )
                }
                slot == 24 -> {
                    player.setVarp("varp.if1", player.getSkills().calculateTotalXp.roundToInt())
                }
                else -> {
                    println("Unknown Slot $slot by Player: ${player.username}") // @TODO No logger yet..
                    return@on_xp_button
                }
            }
        }

// Goal
        /**
         * Goal will have : Starting point 0 // If ofc player does not have anything assigned.
         * Goal will be assigned by: 1k -> 10k -> 100k -> 1m -> 10m -> 100m -> 1b -> 2147m
         */
        on_xp_button(GOAL_COMPONENT_ID) {
            player.playSound(Sound.INTERFACE_SELECT1)
            player.setVarp("varp.if1", -1)
            player.setVarbit("varbits.xpdrops_setup_type", 2)
            val slot = player.getVarbit("varbits.xpdrops_setup_skill")
            when {
                slot in 0..23 -> {

                    val skill = player.getSkills()[skillEnum.getInt(player.getVarbit("varbits.xpdrops_setup_skill"))]
                    player.setVarp("varp.if1", skill.xp.roundToInt())
                    player.setVarp("varp.if2", SkillSet.getXpForLevel(skill.currentLevel + 1).roundToInt())
                }
                slot == 24 -> {
                    player.setVarp("varp.if1", 0)
                    player.setVarp("varp.if2", getClosestNumber(player.getSkills().calculateTotalXp.roundToInt()))
                }
                else -> {
                    println("Unknown Slot $slot by Player: ${player.username}") // @TODO No logger yet..
                    return@on_xp_button
                }
            }
        }

        on_xp_button(TRACKER_SET) {
            val xp = 0
            val level = 0
            val inter = 0
            when (player.getInteractingOption()) {
                5 -> {
                    player.queue {
                        inputInt(player, "Set tracker start point: (skill level)") // If more than 99 it's not valid. "Input is not a valid skill level.
                        // Set varp 261 to xp : so if u input level 80 , it needs to convert into XP
                    }
                } // Set level
                6 -> {
                    player.queue {
                        inputInt(player, "Set tracker start point: (XP value)")
                    }
                }
                9 ->
                    player.setVarp("varp.if1", player.getSkills()[skillEnum.getInt(player.getVarbit("varbits.xpdrops_setup_skill"))].xp.roundToInt(),
                    )
                else -> "Something went wrong! method: on_xp_button(TRACKER_SET) Interaction slot: ${player.getInteractingOption()}"
            }
        }
    }





    fun getClosestNumber(xp: Int): Int {
        return when {
            xp < 10 -> 10
            xp < 100 -> 100
            xp < 1000 -> 1000
            xp < 10000 -> 10000
            xp < 100000 -> 100000
            xp < 1000000 -> 1000000
            xp < 10000000 -> 10000000
            xp < 100000000 -> 100000000
            xp < 1000000000 -> 1000000000
            xp < Int.MAX_VALUE -> Int.MAX_VALUE
            else -> 0
        }
    }

    fun on_xp_button(
        component: Int,
        plugin: Plugin.() -> Unit,
    ) {
        onButton(XpSettings.SETUP_INTERFACE_ID, component) {
            player.playSound(Sound.INTERFACE_SELECT1)
            plugin()
        }
    }
}