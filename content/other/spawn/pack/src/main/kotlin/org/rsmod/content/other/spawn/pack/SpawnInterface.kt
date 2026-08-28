package org.rsmod.content.other.spawn.pack

import dev.openrune.cache.tools.iftype.dsl.buildInterface
import dev.openrune.cache.tools.iftype.dsl.impl.FontType
import dev.openrune.cache.tools.iftype.dsl.impl.layer
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.type.widget.IfEvent

private const val WIDTH = 512
private const val HEIGHT = 334

private const val TITLE_H = 36

private const val ROW_Y = 4
private const val ROW_H = 20
private const val CONTROLS_H = ROW_Y + ROW_H + 4

private const val SCROLLBAR_W = 16
private const val SCROLLBAR_GAP = 2
private const val INSET = 10

private const val CONTENT_W = WIDTH - INSET * 2
private const val GRID_W = WIDTH - INSET * 2 - SCROLLBAR_W - SCROLLBAR_GAP

const val QTY_BUTTON_COUNT = 4
private const val BUTTON_W = 48
private const val BUTTON_PITCH = 52
private const val NOTE_X = 8 + QTY_BUTTON_COUNT * BUTTON_PITCH
private const val BANK_X = NOTE_X + BUTTON_PITCH

private const val SEARCH_X = BANK_X + BUTTON_W + 8
private const val SEARCH_W = CONTENT_W - 8 - SEARCH_X

private const val COLOUR_GOLD = 0xff981f

private const val VIEWPORT_H = HEIGHT - TITLE_H - CONTROLS_H - INSET

private val CLICK_EVENTS = IfEvent.DeprecatedOp1.bitmask.toInt()

private const val BORDER_CHILD = 0
private const val SEARCHBAR_CHILD = 1
private const val CONTENT_CHILD = 2
private const val CONTENT_CHILD_COUNT = 1 + QTY_BUTTON_COUNT + 2
private const val SEARCHTEXT_CHILD = CONTENT_CHILD + 1
private const val GRID_CHILD = CONTENT_CHILD + 1 + CONTENT_CHILD_COUNT
private const val MESSAGE_CHILD = GRID_CHILD + 1
private const val MESSAGE_TEXT_CHILD = MESSAGE_CHILD + 1
private const val SCROLLBAR_CHILD = MESSAGE_TEXT_CHILD + 1

fun buildSpawnMenuInterface() =
    buildInterface(internalName = "interface.spawn_menu", width = WIDTH, height = HEIGHT) {
        val iface = ConstantProvider.getMapping("interface.spawn_menu")
        val initCs = ConstantProvider.getMapping("clientscript.spawn_menu_init")
        fun comp(childIndex: Int) = (iface shl 16) or (childIndex + 1)

        onLoadListener {
            arrayOf(
                initCs,
                comp(BORDER_CHILD),
                comp(SEARCHBAR_CHILD),
                comp(SEARCHTEXT_CHILD),
                comp(GRID_CHILD),
                comp(SCROLLBAR_CHILD),
                comp(MESSAGE_TEXT_CHILD),
            )
        }

        layer("border") {
            position { 0 to 0 }
            size { WIDTH to HEIGHT }
            noClickThrough { true }
        }

        layer("searchbar") {
            position { (INSET + SEARCH_X) to (TITLE_H + ROW_Y) }
            size { SEARCH_W to ROW_H }
        }

        layer("content") {
            position { INSET to TITLE_H }
            size { CONTENT_W to CONTROLS_H }

            text("searchtext") {
                position { SEARCH_X to ROW_Y }
                size { SEARCH_W to ROW_H }
                display { "*" }
                font { FontType.FONT_REGULAR }
                color(COLOUR_GOLD)
                textShadowed { true }
                xAllignment { 1 }
                yAllignment { 1 }
                addOption("Search")
                events = CLICK_EVENTS
            }

            for (i in 0 until QTY_BUTTON_COUNT) {
                layer("qtybtn$i") {
                    position { (8 + i * BUTTON_PITCH) to ROW_Y }
                    size { BUTTON_W to ROW_H }
                    addOption("Select")
                    events = CLICK_EVENTS
                }
            }

            layer("notebtn") {
                position { NOTE_X to ROW_Y }
                size { BUTTON_W to ROW_H }
                addOption("Toggle")
                events = CLICK_EVENTS
            }

            layer("bankbtn") {
                position { BANK_X to ROW_Y }
                size { BUTTON_W to ROW_H }
                addOption("Toggle")
                events = CLICK_EVENTS
            }
        }

        layer("grid") {
            position { INSET to (TITLE_H + CONTROLS_H) }
            size { GRID_W to VIEWPORT_H }
        }

        layer("message") {
            position { INSET to (TITLE_H + CONTROLS_H) }
            size { GRID_W to VIEWPORT_H }

            text("status") {
                position { 0 to 0 }
                size { GRID_W to VIEWPORT_H }
                display { "Start typing to find items" }
                font { FontType.FONT_BOLD }
                color(COLOUR_GOLD)
                textShadowed { true }
                xAllignment { 1 }
                yAllignment { 1 }
            }
        }

        layer("scrollbar") {
            position { (WIDTH - INSET - SCROLLBAR_W) to (TITLE_H + CONTROLS_H) }
            size { SCROLLBAR_W to VIEWPORT_H }
        }
    }
