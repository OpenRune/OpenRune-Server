package org.alter.interfaces.appearance

import org.alter.api.ext.*
import org.alter.game.model.appearance.*
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ButtonClickEvent
import org.alter.game.pluginnew.event.impl.CommandEvent
import org.alter.game.pluginnew.event.impl.InterfaceOpenEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.game.pluginnew.event.impl.onInterfaceOpen
import org.alter.rscm.RSCM.asRSCM

/**
 * Appearance customization plugin using the new PluginEvent system.
 * Handles commands to open the appearance interface and all interface interactions.
 */
class AppearanceEvents : PluginEvent() {

    companion object {
        private const val INTERFACE_ID = 679

        // Component IDs from RSCM
        private val HEAD_LEFT = "components.player_design:head_left".asRSCM()
        private val HEAD_RIGHT = "components.player_design:head_right".asRSCM()
        private val JAW_LEFT = "components.player_design:jaw_left".asRSCM()
        private val JAW_RIGHT = "components.player_design:jaw_right".asRSCM()
        private val TORSO_LEFT = "components.player_design:torso_left".asRSCM()
        private val TORSO_RIGHT = "components.player_design:torso_right".asRSCM()
        private val ARMS_LEFT = "components.player_design:arms_left".asRSCM()
        private val ARMS_RIGHT = "components.player_design:arms_right".asRSCM()
        private val HANDS_LEFT = "components.player_design:hands_left".asRSCM()
        private val HANDS_RIGHT = "components.player_design:hands_right".asRSCM()
        private val LEGS_LEFT = "components.player_design:legs_left".asRSCM()
        private val LEGS_RIGHT = "components.player_design:legs_right".asRSCM()
        private val FEET_LEFT = "components.player_design:feet_left".asRSCM()
        private val FEET_RIGHT = "components.player_design:feet_right".asRSCM()

        // Color buttons
        private val HAIR_COL_LEFT = "components.player_design:hair_left".asRSCM()
        private val HAIR_COL_RIGHT = "components.player_design:hair_right".asRSCM()
        private val TORSO_COL_LEFT = "components.player_design:torso_col_left".asRSCM()
        private val TORSO_COL_RIGHT = "components.player_design:torso_col_right".asRSCM()
        private val LEGS_COL_LEFT = "components.player_design:legs_col_left".asRSCM()
        private val LEGS_COL_RIGHT = "components.player_design:legs_col_right".asRSCM()
        private val FEET_COL_LEFT = "components.player_design:feet_col_left".asRSCM()
        private val FEET_COL_RIGHT = "components.player_design:feet_col_right".asRSCM()
        private val SKIN_LEFT = "components.player_design:skin_left".asRSCM()
        private val SKIN_RIGHT = "components.player_design:skin_right".asRSCM()

        // Gender buttons
        private val GENDER_MALE = "components.player_design:gender_male".asRSCM()
        private val GENDER_FEMALE = "components.player_design:gender_female".asRSCM()

        // Confirm button
        private val CONFIRM = "components.player_design:confirm".asRSCM()
    }

    override fun init() {
        // Handle commands to open appearance interface
        on<CommandEvent> {
            where {
                command.equals("appearance", ignoreCase = true) ||
                command.equals("looks", ignoreCase = true) ||
                command.equals("char", ignoreCase = true)
            }
            then {
                player.queue {
                    selectAppearance(player)
                }
            }
        }

        // Initialize interface when opened
        onInterfaceOpen(INTERFACE_ID) {
            // Sync the gender selection varbit to match current gender
            val bodyTypeVarbit = when (player.appearance.gender) {
                Gender.MALE -> 0
                Gender.FEMALE -> 1
            }
            player.setVarbit("varbits.player_design_bodytype", bodyTypeVarbit)
            // Sync appearance when interface opens
            org.alter.game.info.PlayerInfo(player).syncAppearance()
        }

        // Handle look changes (head, jaw, torso, arms, hands, legs, feet)
        onButton("components.player_design:head_left") {
            changeLook(player, 0, -1) // Head, decrease
        }

        onButton("components.player_design:head_right") {
            changeLook(player, 0, 1) // Head, increase
        }

        onButton("components.player_design:jaw_left") {
            if (player.appearance.gender == Gender.MALE) {
                changeLook(player, 1, -1) // Jaw, decrease (males only)
            }
        }

        onButton("components.player_design:jaw_right") {
            if (player.appearance.gender == Gender.MALE) {
                changeLook(player, 1, 1) // Jaw, increase (males only)
            }
        }

        onButton("components.player_design:torso_left") {
            changeLook(player, 2, -1) // Torso, decrease
        }

        onButton("components.player_design:torso_right") {
            changeLook(player, 2, 1) // Torso, increase
        }

        onButton("components.player_design:arms_left") {
            changeLook(player, 3, -1) // Arms, decrease
        }

        onButton("components.player_design:arms_right") {
            changeLook(player, 3, 1) // Arms, increase
        }

        onButton("components.player_design:hands_left") {
            changeLook(player, 4, -1) // Hands, decrease
        }

        onButton("components.player_design:hands_right") {
            changeLook(player, 4, 1) // Hands, increase
        }

        onButton("components.player_design:legs_left") {
            changeLook(player, 5, -1) // Legs, decrease
        }

        onButton("components.player_design:legs_right") {
            changeLook(player, 5, 1) // Legs, increase
        }

        onButton("components.player_design:feet_left") {
            changeLook(player, 6, -1) // Feet, decrease
        }

        onButton("components.player_design:feet_right") {
            changeLook(player, 6, 1) // Feet, increase
        }

        // Handle color changes
        onButton("components.player_design:hair_left") {
            changeColor(player, 0, -1) // Hair color, decrease
        }

        onButton("components.player_design:hair_right") {
            changeColor(player, 0, 1) // Hair color, increase
        }

        onButton("components.player_design:torso_col_left") {
            changeColor(player, 1, -1) // Torso color, decrease
        }

        onButton("components.player_design:torso_col_right") {
            changeColor(player, 1, 1) // Torso color, increase
        }

        onButton("components.player_design:legs_col_left") {
            changeColor(player, 2, -1) // Legs color, decrease
        }

        onButton("components.player_design:legs_col_right") {
            changeColor(player, 2, 1) // Legs color, increase
        }

        onButton("components.player_design:feet_col_left") {
            changeColor(player, 3, -1) // Feet color, decrease
        }

        onButton("components.player_design:feet_col_right") {
            changeColor(player, 3, 1) // Feet color, increase
        }

        onButton("components.player_design:skin_left") {
            changeColor(player, 4, -1) // Skin color, decrease
        }

        onButton("components.player_design:skin_right") {
            changeColor(player, 4, 1) // Skin color, increase
        }

        // Handle gender changes
        onButton("components.player_design:gender_male") {
            changeGender(player, Gender.MALE)
        }

        onButton("components.player_design:gender_female") {
            changeGender(player, Gender.FEMALE)
        }

        // Handle confirm button
        onButton("components.player_design:confirm") {
            player.closeInterface(INTERFACE_ID)
            org.alter.game.info.PlayerInfo(player).syncAppearance()
            player.message("Your appearance has been updated!")
        }
    }

    /**
     * Changes a look (head, jaw, torso, arms, hands, legs, feet) by the specified direction.
     * @param lookIndex The index in the looks array (0=head, 1=jaw, 2=torso, 3=arms, 4=hands, 5=legs, 6=feet)
     * @param direction -1 for left (decrease), 1 for right (increase)
     */
    private fun changeLook(player: org.alter.game.model.entity.Player, lookIndex: Int, direction: Int) {
        val appearance = player.appearance
        val gender = appearance.gender

        // Get the appropriate looks array based on gender and look type
        val looksArray = when (lookIndex) {
            0 -> Looks.getHeads(gender) // Head
            1 -> Looks.getJaws(gender) // Jaw (males only)
            2 -> Looks.getTorsos(gender) // Torso
            3 -> Looks.getArms(gender) // Arms
            4 -> Looks.getHands(gender) // Hands
            5 -> Looks.getLegs(gender) // Legs
            6 -> Looks.getFeets(gender) // Feet
            else -> return
        }

        // Skip jaw for females
        if (lookIndex == 1 && gender == Gender.FEMALE) {
            return
        }

        // Get current look index in the looks array
        // appearance.looks contains indices into the looks arrays, not model IDs
        val currentArrayIndex = when (gender) {
            Gender.MALE -> {
                when (lookIndex) {
                    0 -> appearance.looks[0] // Head
                    1 -> appearance.looks[1] // Jaw
                    2 -> appearance.looks[2] // Torso
                    3 -> appearance.looks[3] // Arms
                    4 -> appearance.looks[4] // Hands
                    5 -> appearance.looks[5] // Legs
                    6 -> appearance.looks[6] // Feet
                    else -> return
                }
            }
            Gender.FEMALE -> {
                when (lookIndex) {
                    0 -> appearance.looks[0] // Head
                    2 -> appearance.looks[1] // Torso (index 1 in female array)
                    3 -> appearance.looks[2] // Arms (index 2 in female array)
                    4 -> appearance.looks[3] // Hands (index 3 in female array)
                    5 -> appearance.looks[4] // Legs (index 4 in female array)
                    6 -> appearance.looks[5] // Feet (index 5 in female array)
                    else -> return
                }
            }
        }

        // Ensure current index is valid
        val safeCurrentIndex = currentArrayIndex.coerceIn(0, looksArray.size - 1)

        // Calculate new index with wrap-around
        var newArrayIndex = safeCurrentIndex + direction
        if (newArrayIndex < 0) {
            newArrayIndex = looksArray.size - 1
        } else if (newArrayIndex >= looksArray.size) {
            newArrayIndex = 0
        }

        // Update the appearance
        val newLooks = appearance.looks.copyOf()
        when (gender) {
            Gender.MALE -> {
                when (lookIndex) {
                    0 -> newLooks[0] = newArrayIndex
                    1 -> newLooks[1] = newArrayIndex
                    2 -> newLooks[2] = newArrayIndex
                    3 -> newLooks[3] = newArrayIndex
                    4 -> newLooks[4] = newArrayIndex
                    5 -> newLooks[5] = newArrayIndex
                    6 -> newLooks[6] = newArrayIndex
                }
            }
            Gender.FEMALE -> {
                when (lookIndex) {
                    0 -> newLooks[0] = newArrayIndex
                    2 -> newLooks[1] = newArrayIndex // Torso
                    3 -> newLooks[2] = newArrayIndex // Arms
                    4 -> newLooks[3] = newArrayIndex // Hands
                    5 -> newLooks[4] = newArrayIndex // Legs
                    6 -> newLooks[5] = newArrayIndex // Feet
                }
            }
        }

        player.appearance = Appearance(newLooks, appearance.colors, appearance.gender)
        org.alter.game.info.PlayerInfo(player).syncAppearance()
    }

    /**
     * Changes a color (hair, torso, legs, feet, skin) by the specified direction.
     * @param colorIndex The index in the colors array (0=hair, 1=torso, 2=legs, 3=feet, 4=skin)
     * @param direction -1 for left (decrease), 1 for right (increase)
     */
    private fun changeColor(player: org.alter.game.model.entity.Player, colorIndex: Int, direction: Int) {
        val appearance = player.appearance

        // Get the appropriate color array
        val colorArray = when (colorIndex) {
            0 -> Colours.HAIR_COLOURS // Hair
            1 -> Colours.TORSO_COLOURS // Torso
            2 -> Colours.LEG_COLOURS // Legs
            3 -> Colours.FEET_COLOURS // Feet
            4 -> Colours.SKIN_COLOURS // Skin
            else -> return
        }

        val currentColor = appearance.colors[colorIndex]
        var newColorIndex = currentColor + direction

        // Wrap around
        if (newColorIndex < 0) {
            newColorIndex = colorArray.size - 1
        } else if (newColorIndex >= colorArray.size) {
            newColorIndex = 0
        }

        val newColors = appearance.colors.copyOf()
        newColors[colorIndex] = newColorIndex

        player.appearance = Appearance(appearance.looks, newColors, appearance.gender)
        org.alter.game.info.PlayerInfo(player).syncAppearance()
    }

    /**
     * Changes the player's gender (body type).
     */
    private fun changeGender(player: org.alter.game.model.entity.Player, newGender: Gender) {
        if (player.appearance.gender == newGender) {
            return // Already this gender
        }

        // When changing gender, we need to reset to default looks for the new gender
        val defaultAppearance = when (newGender) {
            Gender.MALE -> Appearance.DEFAULT_MALE
            Gender.FEMALE -> Appearance.DEFAULT_FEMALE
        }

        // Preserve colors if possible (they use the same indices)
        player.appearance = Appearance(
            defaultAppearance.looks,
            player.appearance.colors.copyOf(),
            newGender
        )

        // Update the varbit that controls the interface's selected gender option
        // 0 = Male (option A), 1 = Female (option B)
        val bodyTypeVarbit = when (newGender) {
            Gender.MALE -> 0
            Gender.FEMALE -> 1
        }
        player.setVarbit("varbits.player_design_bodytype", bodyTypeVarbit)

        org.alter.game.info.PlayerInfo(player).syncAppearance()
    }
}

