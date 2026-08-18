package org.rsmod.content.other.toolbelt.pack

import dev.openrune.cache.tools.iftype.dsl.InterfaceBuilder
import dev.openrune.cache.tools.iftype.dsl.buildInterface
import dev.openrune.cache.tools.iftype.dsl.impl.graphic
import dev.openrune.cache.tools.iftype.dsl.impl.layer
import dev.openrune.definition.constants.ConstantProvider

private const val BTN = 34
private const val ICON = 26
private const val BTN_Y = 212
private const val START_X = 4
private const val STEP = 37

const val WORN_WIDTH = 190

const val WORN_HEIGHT = 261

val WORN_FOOTER_VANILLA = listOf(
    "component.wornitems:equipment",
    "component.wornitems:equipment_icon",
    "component.wornitems:pricechecker",
    "component.wornitems:com_4",
    "component.wornitems:deathkeep",
    "component.wornitems:com_6",
    "component.wornitems:call_follower",
    "component.wornitems:com_8"
)

val WORN_FOOTER_TOOLBELT_ROW = listOf(
    "component.wornitems:tb_equipment",
    "component.wornitems:tb_equipment_icon",
    "component.wornitems:tb_pricechecker",
    "component.wornitems:tb_pricechecker_icon",
    "component.wornitems:tb_deathkeep",
    "component.wornitems:tb_deathkeep_icon",
    "component.wornitems:tb_call_follower",
    "component.wornitems:tb_call_follower_icon",
    "component.wornitems:tb_toolbelt",
    "component.wornitems:tb_toolbelt_icon"
)

fun buildWornItemsToolbelt() =
    buildInterface(internalName = "interface.wornitems", width = WORN_WIDTH, height = WORN_HEIGHT) {
        inherit("interface.wornitems")

        tbFooterButton("tb_equipment", "equipment", 0)
        tbFooterIcon("tb_equipment_icon", "equipment_icon", 0)

        tbFooterButton("tb_pricechecker", "pricechecker", 1)
        tbFooterIcon("tb_pricechecker_icon", "com_4", 1)

        tbFooterButton("tb_deathkeep", "deathkeep", 2)
        tbFooterIcon("tb_deathkeep_icon", "com_6", 2)

        tbFooterButton("tb_call_follower", "call_follower", 3)
        tbFooterIcon("tb_call_follower_icon", "com_8", 3)

        layer("tb_toolbelt") {
            from("equipment")
            hide { true }
            position { btnPos(4) }
            size { BTN to BTN }
            addOption("View toolbelt")
        }
        graphic("tb_toolbelt_icon") {
            from("com_8")
            hide { true }
            position { iconPos(4) }
            size { ICON to ICON }
            spriteId { ConstantProvider.getMapping("sprites.tool_belt") }
        }
    }

private fun InterfaceBuilder.tbFooterButton(
    name: String,
    donor: String,
    index: Int,
) {
    layer(name) {
        from(donor)
        hide { true }
        position { btnPos(index) }
        size { BTN to BTN }
    }
}

private fun InterfaceBuilder.tbFooterIcon(
    name: String,
    donor: String,
    index: Int,
) {
    graphic(name) {
        from(donor)
        hide { true }
        position { iconPos(index) }
        size { ICON to ICON }
    }
}

fun btnPos(index: Int): Pair<Int, Int> = (START_X + index * STEP) to BTN_Y

fun iconPos(index: Int): Pair<Int, Int> {
    val pad = (BTN - ICON) / 2
    return (START_X + index * STEP + pad) to (BTN_Y + pad)
}
