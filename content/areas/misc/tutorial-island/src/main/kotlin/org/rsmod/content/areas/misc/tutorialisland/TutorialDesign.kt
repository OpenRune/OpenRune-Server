package org.rsmod.content.areas.misc.tutorialisland

import org.rsmod.game.entity.Player

internal object TutorialDesign {
    private val BODY_SLOTS =
        mapOf(
            "head" to 0,
            "jaw" to 1,
            "torso" to 2,
            "arms" to 3,
            "hands" to 4,
            "legs" to 5,
            "feet" to 6,
        )

    private val COLOUR_SLOTS =
        mapOf(
            "hair" to 0,
            "torso_col" to 1,
            "legs_col" to 2,
            "feet_col" to 3,
            "skin" to 4,
        )

    private val COLOUR_MAX =
        mapOf(
            "hair" to 11,
            "torso_col" to 15,
            "legs_col" to 15,
            "feet_col" to 5,
            "skin" to 7,
        )

    private val MALE_DEFAULT = intArrayOf(9, 14, 109, 26, 33, 36, 42)
    private val FEMALE_DEFAULT = intArrayOf(45, 292, 56, 61, 67, 70, 79)

    private val MALE_STYLES =
        mapOf(
            "head" to (0..9).toList() + (129..134).toList(),
            "jaw" to (10..17).toList(),
            "torso" to (18..25).toList() + (105..110).toList() + (254..259).toList(),
            "arms" to (26..32).toList() + (84..88).toList() + (260..264).toList(),
            "hands" to listOf(33, 34),
            "legs" to (36..41).toList() + (281..291).toList(),
            "feet" to listOf(42, 43),
        )

    private val FEMALE_STYLES =
        mapOf(
            "head" to (45..55).toList() + (118..128).toList() + (141..143).toList(),
            "jaw" to (292..299).toList(),
            "torso" to (56..60).toList() + (89..94).toList() + (265..273).toList(),
            "arms" to (61..66).toList() + (95..99).toList() + (248..253).toList(),
            "hands" to listOf(67, 68),
            "legs" to (70..78).toList() + (135..140).toList() + (274..280).toList(),
            "feet" to listOf(79, 80),
        )

    fun setGender(player: Player, bodyType: Int, pronoun: Int) {
        player.playerDesignBodyType = bodyType
        player.transmitPronouns = pronoun
        player.appearance.bodyType = bodyType
        player.appearance.pronoun = pronoun
        val defaults = if (bodyType == 0) MALE_DEFAULT else FEMALE_DEFAULT
        for (slot in defaults.indices) {
            player.appearance.setIdentKit(slot, defaults[slot])
        }
        for ((slot, colour) in DEFAULT_COLOURS.withIndex()) {
            player.appearance.setColour(slot, colour)
        }
    }

    fun cycle(player: Player, part: String, delta: Int) {
        val bodySlot = BODY_SLOTS[part]
        if (bodySlot != null) {
            val styles = (if (player.appearance.bodyType == 0) MALE_STYLES else FEMALE_STYLES)[part]
                ?: return
            val current = player.appearance.identKitSnapshot()[bodySlot]
            val index = styles.indexOf(current.toInt())
            val next = Math.floorMod(if (index < 0) delta else index + delta, styles.size)
            player.appearance.setIdentKit(bodySlot, styles[next])
            return
        }
        val colourSlot = COLOUR_SLOTS[part] ?: return
        val max = COLOUR_MAX.getValue(part)
        val current = player.appearance.coloursSnapshot()[colourSlot].toInt() and 0xFF
        val next = Math.floorMod(current + delta, max + 1)
        player.appearance.setColour(colourSlot, next)
    }

    private val DEFAULT_COLOURS = intArrayOf(0, 3, 2, 0, 0)
}
