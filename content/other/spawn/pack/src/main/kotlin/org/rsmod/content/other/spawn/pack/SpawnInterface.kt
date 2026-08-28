package org.rsmod.content.other.spawn.pack

import dev.openrune.cache.tools.iftype.dsl.buildInterface
import dev.openrune.cache.tools.iftype.dsl.impl.FontType
import dev.openrune.cache.tools.iftype.dsl.impl.graphic
import dev.openrune.cache.tools.iftype.dsl.impl.layer
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.type.widget.IfEvent

/**
 * Admin item-spawner interface (`::spawn`): search button, quantity/note row, scrollable result
 * grid. Items are pushed into slots server-side via `ifSetObj` (see `SpawnMenuScript`). Chrome and
 * scrollbar are native CS2 procs (`~stoneborder`, `~scrollbar_vertical`).
 *
 * Structural rules:
 * - Every top-level declaration must be a `layer(...)` block, never a bare component.
 * - `border` must stay empty - `~stoneborder` clears its children on load.
 * - `content` (controls) and `grid` (results) are separate layers so the grid can scroll
 *   independently.
 *
 * Addressing: every component referenced by name needs an entry in `gamevals.toml`, keyed
 * `<interface>:<component>`. `buildInterface` injects a hidden `"universe"` root at key 0, so a
 * real packed id is `(interfaceId shl 16) or (childIndex + 1)`. After any structural change here,
 * regenerate `gamevals.toml` and verify with `::spawndebug`.
 *
 * Close and reopen the client after any cache rebuild - it can hold a stale interface definition.
 */
private const val WIDTH = 512
private const val HEIGHT = 334

private const val TITLE_H = 36

private const val ROW_Y = 4
private const val STATUS_Y = ROW_Y + 20 + 4
private const val CONTROLS_H = STATUS_Y + 12 + 4

private const val SCROLLBAR_W = 16
private const val SCROLLBAR_GAP = 2
private const val INSET = 10

private const val CONTENT_W = WIDTH - INSET * 2
private const val SEARCH_X = CONTENT_W - 100 - 8
private const val GRID_W = WIDTH - INSET * 2 - SCROLLBAR_W - SCROLLBAR_GAP

private const val CARD_COLS = 2
private const val CARD_GAP = 6
private val CARD_W = (GRID_W - CARD_GAP) / CARD_COLS
private const val CARD_H = 42
private const val CARD_PITCH_Y = CARD_H + CARD_GAP

private const val SLOT_SIZE = 32
private const val ICON_MARGIN = 6

private const val COLOUR_CARD_BORDER = 0x5a4a2f
private const val COLOUR_CARD_FILL = 0x2b2318

private const val VIEWPORT_H = HEIGHT - TITLE_H - CONTROLS_H - INSET

/** 50 rows * 2 cols = 100 max results. */
private const val TOTAL_ROWS = 50

/** Must stay in sync with `SpawnMenuScript`. */
const val SLOT_COUNT = CARD_COLS * TOTAL_ROWS

const val GRID_CONTENT_HEIGHT = TOTAL_ROWS * CARD_PITCH_Y

const val QTY_BUTTON_COUNT = 4
private const val NOTE_X = 8 + QTY_BUTTON_COUNT * 52

private const val BLANK_SPRITE = 3023 // sprites.blank
private const val COLOUR_TEXT = 0xffffff

private val QTY_LABELS = listOf("1", "100", "1000", "X")

/**
 * `hasEvent()` checks `DeprecatedOp1` (bitmask 2) against a 32-bit `events` field. The modern
 * `IfEvent.Op1.bitmask` is `1L shl 32` and truncates to 0 when cast to Int, so it must be the
 * deprecated variant here.
 */
private val CLICK_EVENTS = IfEvent.DeprecatedOp1.bitmask.toInt()

fun buildSpawnMenuInterface() =
    buildInterface(internalName = "interface.spawn_menu", width = WIDTH, height = HEIGHT) {
        val iface = ConstantProvider.getMapping("interface.spawn_menu")
        val initCs = ConstantProvider.getMapping("clientscript.spawn_menu_init")
        // Packed key = childIndex + 1. border=0, content=1 (+19 children, indices 2-20),
        // grid=21 (+SLOT_COUNT*4 children), scrollbar=21+1+SLOT_COUNT*4.
        fun comp(childIndex: Int) = (iface shl 16) or (childIndex + 1)

        val gridChildIndex = 21
        val scrollbarChildIndex = gridChildIndex + 1 + SLOT_COUNT * 4

        onLoadListener { arrayOf(initCs, comp(0), comp(gridChildIndex), comp(scrollbarChildIndex)) }

        layer("border") { // child 0
            position { 0 to 0 }
            size { WIDTH to HEIGHT }
            noClickThrough { true }
        }

        layer("content") { // child 1
            position { INSET to TITLE_H }
            size { CONTENT_W to CONTROLS_H }

            rectangle("searchborder") {
                position { SEARCH_X to ROW_Y }
                size { 100 to 20 }
                color(COLOUR_CARD_BORDER)
                filled { true }
            }

            rectangle("searchbg") {
                position { (SEARCH_X + 1) to (ROW_Y + 1) }
                size { 98 to 18 }
                color(COLOUR_CARD_FILL)
                filled { true }
            }

            text("searchlbl") {
                position { SEARCH_X to ROW_Y }
                size { 100 to 20 }
                display { "Search" }
                font { FontType.FONT_REGULAR }
                color(COLOUR_TEXT)
                textShadowed { true }
                xAllignment { 1 }
                yAllignment { 1 }
                addOption("Search")
                events = CLICK_EVENTS
            }

            for (i in 0 until QTY_BUTTON_COUNT) {
                rectangle("qtyborder$i") {
                    position { (8 + i * 52) to ROW_Y }
                    size { 48 to 20 }
                    color(COLOUR_CARD_BORDER)
                    filled { true }
                }
            }

            for (i in 0 until QTY_BUTTON_COUNT) {
                rectangle("qtybg$i") {
                    position { (9 + i * 52) to (ROW_Y + 1) }
                    size { 46 to 18 }
                    color(COLOUR_CARD_FILL)
                    filled { true }
                }
            }

            for (i in 0 until QTY_BUTTON_COUNT) {
                text("qtylbl$i") {
                    position { (8 + i * 52) to ROW_Y }
                    size { 48 to 20 }
                    display { QTY_LABELS[i] }
                    font { FontType.FONT_REGULAR }
                    color(COLOUR_TEXT)
                    textShadowed { true }
                    xAllignment { 1 }
                    yAllignment { 1 }
                    addOption("Select")
                    events = CLICK_EVENTS
                }
            }

            rectangle("noteborder") {
                position { NOTE_X to ROW_Y }
                size { 48 to 20 }
                color(COLOUR_CARD_BORDER)
                filled { true }
            }

            rectangle("notebg") {
                position { (NOTE_X + 1) to (ROW_Y + 1) }
                size { 46 to 18 }
                color(COLOUR_CARD_FILL)
                filled { true }
            }

            text("notelbl") {
                position { NOTE_X to ROW_Y }
                size { 48 to 20 }
                display { "Note" }
                font { FontType.FONT_REGULAR }
                color(COLOUR_TEXT)
                textShadowed { true }
                xAllignment { 1 }
                yAllignment { 1 }
                addOption("Toggle")
                events = CLICK_EVENTS
            }

            text("status") {
                position { 8 to STATUS_Y }
                size { (CONTENT_W - 16) to 12 }
                display { "" }
                font { FontType.FONT_SMALL }
                color(COLOUR_TEXT)
                textShadowed { true }
                xAllignment { 0 }
                yAllignment { 1 }
            }
        }

        layer("grid") { // child 2
            position { INSET to (TITLE_H + CONTROLS_H) }
            size { GRID_W to VIEWPORT_H }

            for (i in 0 until SLOT_COUNT) {
                val col = i % CARD_COLS
                val row = i / CARD_COLS
                val cellX = col * (CARD_W + CARD_GAP)
                val cellY = row * CARD_PITCH_Y

                rectangle("cardborder$i") {
                    position { cellX to cellY }
                    size { CARD_W to CARD_H }
                    color(COLOUR_CARD_BORDER)
                    filled { true }
                }

                rectangle("cardbg$i") {
                    position { (cellX + 1) to (cellY + 1) }
                    size { (CARD_W - 2) to (CARD_H - 2) }
                    color(COLOUR_CARD_FILL)
                    filled { true }
                }

                graphic("slot$i") {
                    position { (cellX + ICON_MARGIN) to (cellY + (CARD_H - SLOT_SIZE) / 2) }
                    size { SLOT_SIZE to SLOT_SIZE }
                    spriteId { BLANK_SPRITE }
                    shadowColor(0x333333)
                    addOption("Spawn", true)
                    events = CLICK_EVENTS
                }

                // Name is set at runtime (SpawnMenuScript.renderSlots); shares slot$i's op so
                // clicking the name spawns the item too.
                text("slotname$i") {
                    position { (cellX + ICON_MARGIN + SLOT_SIZE + 8) to cellY }
                    size { (CARD_W - ICON_MARGIN - SLOT_SIZE - 8 - 6) to CARD_H }
                    display { "" }
                    font { FontType.FONT_SMALL }
                    color(COLOUR_TEXT)
                    textShadowed { true }
                    xAllignment { 0 }
                    yAllignment { 1 }
                    addOption("Spawn", true)
                    events = CLICK_EVENTS
                }
            }
        }

        layer("scrollbar") { // child 3
            position { (WIDTH - INSET - SCROLLBAR_W) to (TITLE_H + CONTROLS_H) }
            size { SCROLLBAR_W to VIEWPORT_H }
        }
    }
