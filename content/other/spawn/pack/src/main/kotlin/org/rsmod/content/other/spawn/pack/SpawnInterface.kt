package org.rsmod.content.other.spawn.pack

import dev.openrune.cache.tools.iftype.dsl.buildInterface
import dev.openrune.cache.tools.iftype.dsl.impl.FontType
import dev.openrune.cache.tools.iftype.dsl.impl.graphic
import dev.openrune.cache.tools.iftype.dsl.impl.layer
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.type.widget.IfEvent

/**
 * Admin item-spawner interface (`::spawn`): a search button, a persistent quantity-mode row, and a
 * scrollable grid of item slots. Items are pushed into the slots server-side with `ifSetObj` (see
 * `SpawnMenuScript`). Chrome (frame/title/close button) and the scrollbar are both driven by native
 * CS2 procs (`~stoneborder`, `~scrollbar_vertical`) - see [buildSpawnMenuInterface]'s doc comment
 * for the full story on getting CS2 working at all in this project.
 *
 * Structural rule #1, from `origin/toolbelt`'s `buildToolbeltInterface()` (the only other
 * from-scratch interface anyone's gotten working here): every top-level declaration is a
 * `layer(...)` block - never a bare `graphic`/`text`/`rectangle` at the interface root.
 *
 * Structural rule #2, found the hard way after enabling CS2: `border` must be chrome-only, empty of
 * real content - `~stoneborder` appears to manage/clear `$border`'s own children on load, so
 * anything nested inside it vanishes even though the frame itself renders. Real content lives in
 * SIBLING layers instead, matching toolbelt's own shape (`border`, `search`, `content`,
 * `scrollbar`, `highlight` as five separate top-level layers, not nested).
 *
 * Structural rule #3 (this pass): the scrollable grid needs its own dedicated layer, separate from
 * the non-scrolling search/quantity controls above it - `content` (controls) stays fixed, `grid`
 * (slots) scrolls independently via `~scrollbar_vertical` wiring a `scrollbar` sibling layer to it.
 *
 * Three hard-won addressing details this file depends on, all verified against the packed cache
 * rather than assumed (getting any wrong fails *silently* - the interface renders but does nothing):
 *
 * 1. Every component referenced by name from Kotlin needs an explicit `[gamevals.component]` entry
 *    in the content module's `gamevals.toml`, keyed `<interface>:<component>`.
 * 2. `buildInterface` auto-injects a hidden root component named `"universe"` at key `0` - every
 *    real component's packed id is `(interfaceId shl 16) or (childIndex + 1)`, not `childIndex`
 *    alone. **After any structural change here, regenerate `gamevals.toml` and verify the new
 *    values against a fresh `::spawndebug` dump** (`ServerCacheManager.getInterface(id).components`)
 *    - don't just recompute the formula by hand again, that's exactly how this went wrong twice.
 * 3. `events` must be built from the `Deprecated*` `IfEvent` variants, not the modern ones - see
 *    [CLICK_EVENTS].
 *
 * Also: after any cache rebuild, **fully close and reopen the client** before testing, not just
 * relog - it can keep a stale interface definition cached under the same numeric id, showing old
 * content even when the server-side data (confirmed via `::spawndebug`) is already correct.
 */
// WIDTH/HEIGHT match origin/toolbelt's buildToolbeltInterface() exactly (419x291, rounded to
// 420x291 here) - the only other custom interface confirmed to actually fit inside a mainmodal's
// real viewport in this codebase. The previous 488x370 was picked with no such reference and
// clipped at the bottom of the game window - don't just guess dimensions again.
private const val WIDTH = 420
private const val HEIGHT = 291

/** Toolbelt's own title-bar height, reused since `~stoneborder` draws the same style header. */
private const val TITLE_H = 36

/** Height of the non-scrolling search/quantity/status row, directly below the title bar. */
private const val CONTROLS_H = 64

private const val SCROLLBAR_W = 16

private const val COLS = 10
private const val SLOT_SIZE = 32
private const val SLOT_PITCH = 36
private const val GRID_X = 4
private const val GRID_Y = 4

/** Rows actually visible in the viewport at once - the grid layer itself is only this tall. */
private const val VISIBLE_ROWS = 5
private const val VIEWPORT_H = VISIBLE_ROWS * SLOT_PITCH

/** Total rows the scrollable grid holds - well beyond the old hard 72-item cap. */
private const val TOTAL_ROWS = 15

/** Must stay in sync with `SpawnMenuScript`'s copy of this value. */
const val SLOT_COUNT = COLS * TOTAL_ROWS

/** Passed to `if_setscrollsize` in the init cs2 script - the grid's full scrollable height. */
const val GRID_CONTENT_HEIGHT = TOTAL_ROWS * SLOT_PITCH

const val QTY_BUTTON_COUNT = 4

private const val BLANK_SPRITE = 3023 // sprites.blank

private const val COLOUR_TEXT = 0xffffff
private const val COLOUR_BOX = 0x2b2318
private const val COLOUR_ACTIVE = 0x7a1f1f

private val QTY_LABELS = listOf("1", "100", "1000", "X")

/**
 * `ComponentType.hasEvent()` maps `Op1` through `toV1Event()` to `DeprecatedOp1` (bitmask `2`) and
 * tests it against a **32-bit** `events` field. `IfEvent.Op1.bitmask` is `1L shl 32`, so the
 * otherwise-obvious `IfEvent.Op1.bitmask.toInt()` truncates to exactly `0` and the component ends
 * up with no events at all.
 *
 * This matters here specifically because these are *static* components. `If3ButtonHandler` routes
 * a click through `InterfaceEvents.isEnabled(...)`, which for `comsub == -1` (what a component with
 * no subcomponents sends) consults `component.hasEvent(...)` - the value baked into the cache by
 * this file - and ignores any runtime `ifSetEvents` call entirely.
 */
private val CLICK_EVENTS = IfEvent.DeprecatedOp1.bitmask.toInt()

/**
 * Getting CS2 working at all took three real fixes, in order:
 * 1. `PackCs2` is disabled by default in this repo's `or-cache` (`//Enable this for cs2`) - just
 *    nothing needed it yet. Uncommented it, and its required companion task `UnpackDefaultCs2`
 *    (undocumented - only surfaced via a runtime error demanding it be present in the same block).
 * 2. First real compile hit `Unable to add basic: type=COMPONENT, name=spawn_menu:universe` -
 *    reproduced with zero custom cs2 content, just from any `buildInterface` interface existing.
 *    Fixed per Mark_ (maintainer, Discord): pin `revision: 240.2` in `game.yml` (Neptune, the CS2
 *    compiler, doesn't fully support the plain `240` default yet) and do a genuinely fresh cache
 *    install (`:or-cache:freshCache`, not incremental `buildCache`) after clearing stale CS2
 *    install state (`:or-cache:cleanCs2`).
 * 3. Past that, `Unable to find id for ClientScriptSymbol(...)` at write time - `gamevals.toml`'s
 *    `[gamevals.clientscript]` entry only feeds the *server's* RSCM registry, not the CS2
 *    *compiler's* own symbol table. That needs its own `.sym` file
 *    (`pack/cs2/symbols/clientscript.sym`, format `<id>\t[clientscript,name]`, matching toolbelt's).
 */
fun buildSpawnMenuInterface() =
    buildInterface(internalName = "interface.spawn_menu", width = WIDTH, height = HEIGHT) {
        val iface = ConstantProvider.getMapping("interface.spawn_menu")
        val initCs = ConstantProvider.getMapping("clientscript.spawn_menu_init")
        // Real packed key = childIndex + 1 (the hidden "universe" root consumes index 0) - see the
        // file doc comment. childIndex is NOT simply "position among top-level layers" - each
        // layer's OWN children consume indices before the next sibling layer's index, so this has
        // to be computed by walking the full declaration order, not assumed (a Python one-liner
        // walking the same order this file declares components in - see gamevals.toml's comment -
        // not hand arithmetic). border=0, content=1 (+ 15 children: searchbg, searchlbl, qtybg0-3,
        // qtyhl0-3, qtylbl0-3, status = indices 2-16), grid=17 (+ SLOT_COUNT children =
        // 18..(18+SLOT_COUNT-1)), scrollbar=18+SLOT_COUNT.
        fun comp(childIndex: Int) = (iface shl 16) or (childIndex + 1)

        val gridChildIndex = 17
        val scrollbarChildIndex = gridChildIndex + 1 + SLOT_COUNT

        onLoadListener { arrayOf(initCs, comp(0), comp(gridChildIndex), comp(scrollbarChildIndex)) }

        // Chrome only - see structural rule #2 above. No children nested here.
        layer("border") { // child 0
            position { 0 to 0 }
            size { WIDTH to HEIGHT }
            noClickThrough { true }
        }

        // Non-scrolling controls: search button + quantity row + status text.
        //
        // Real bank quantity buttons (checked via a live screenshot, not just component names)
        // DO have a background box per button - a dark stone-ish pill, with the active one getting
        // a red-tinted highlight box - not bare floating text like the first attempt at this
        // assumed from `bank_filler_*` names alone. Each button here is now a stack of three:
        // a static dark box (bg), a hidden-unless-active red overlay (hl), then the clickable text
        // on top. `addOption`/`events` stay on the TEXT component (not the boxes) so clicking
        // anywhere in the box still hits the same target the box visually represents.
        layer("content") { // child 1
            position { 0 to TITLE_H }
            size { WIDTH to CONTROLS_H }

            rectangle("searchbg") {
                position { 312 to 4 }
                size { 100 to 20 }
                color(COLOUR_BOX)
                filled { true }
            }

            text("searchlbl") {
                position { 312 to 4 }
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
                rectangle("qtybg$i") {
                    position { (8 + i * 52) to 28 }
                    size { 48 to 20 }
                    color(COLOUR_BOX)
                    filled { true }
                }
            }

            // hidden by default; shown over whichever quantity button is currently active.
            for (i in 0 until QTY_BUTTON_COUNT) {
                rectangle("qtyhl$i") {
                    position { (8 + i * 52) to 28 }
                    size { 48 to 20 }
                    color(COLOUR_ACTIVE)
                    filled { true }
                    hide { true }
                }
            }

            // text is replaced at runtime to highlight the active mode.
            for (i in 0 until QTY_BUTTON_COUNT) {
                text("qtylbl$i") {
                    position { (8 + i * 52) to 28 }
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

            text("status") {
                position { 8 to 50 }
                size { (WIDTH - 16) to 12 }
                display { "" }
                font { FontType.FONT_SMALL }
                color(COLOUR_TEXT)
                textShadowed { true }
                xAllignment { 0 }
                yAllignment { 1 }
            }
        }

        // Scrollable grid - only VIEWPORT_H tall, but holds SLOT_COUNT slots across TOTAL_ROWS.
        // `if_setscrollsize`/`~scrollbar_vertical` (called from the init cs2 script against this
        // layer + the "scrollbar" sibling below) do the actual scrolling; nothing here needs a
        // baked scrollHeight DSL property.
        layer("grid") { // child 2
            position { 0 to (TITLE_H + CONTROLS_H) }
            size { (WIDTH - SCROLLBAR_W) to VIEWPORT_H }

            for (i in 0 until SLOT_COUNT) {
                val col = i % COLS
                val row = i / COLS
                graphic("slot$i") {
                    position { (GRID_X + col * SLOT_PITCH) to (GRID_Y + row * SLOT_PITCH) }
                    size { SLOT_SIZE to SLOT_SIZE }
                    spriteId { BLANK_SPRITE }
                    // addOption(_, true) got a hover box to appear at all (it didn't with the
                    // unset/false default), but it only shows the literal string "Spawn", not
                    // "Spawn <item name>" the way real OSRS action text works (confirmed via a
                    // live screenshot: "Use Armadyl page 3"). targetVerb alone (tried, then
                    // removed) made no difference either. opBase is the next real candidate -
                    // ComponentType (the final packed type, not just this DSL) has a distinct
                    // opBase field separate from addOption's menu-option strings, and its name
                    // matches "verb template the client appends the target's name to" far more
                    // closely than the two already-ruled-out properties.
                    addOption("Spawn", true)
                    opBase { "Spawn" }
                    events = CLICK_EVENTS
                }
            }
        }

        // Drag handle track - wired to "grid" by ~scrollbar_vertical in the init cs2 script.
        layer("scrollbar") { // child 3
            position { (WIDTH - SCROLLBAR_W) to (TITLE_H + CONTROLS_H) }
            size { SCROLLBAR_W to VIEWPORT_H }
        }
    }
