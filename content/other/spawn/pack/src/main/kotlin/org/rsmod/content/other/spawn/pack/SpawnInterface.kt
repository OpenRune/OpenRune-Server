package org.rsmod.content.other.spawn.pack

import dev.openrune.cache.tools.iftype.dsl.buildInterface
import dev.openrune.cache.tools.iftype.dsl.impl.FontType
import dev.openrune.cache.tools.iftype.dsl.impl.graphic
import dev.openrune.cache.tools.iftype.dsl.impl.layer
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.type.widget.IfEvent

/**
 * Admin item-spawner interface (`::spawn`): a search button, a persistent quantity-mode row, and a
 * grid of item slots. Items are pushed into the slots server-side with `ifSetObj` (see
 * `SpawnMenuScript`). Chrome (frame/title/close button) is drawn by the native `~stoneborder` CS2
 * proc - see [buildSpawnMenuInterface]'s doc comment for the full story on getting CS2 working.
 *
 * Structural rule #1, discovered by comparing against `origin/toolbelt`'s `buildToolbeltInterface()`
 * (the only other from-scratch interface anyone's gotten working in this codebase): every top-level
 * declaration there is a `layer(...)` block - never a bare `graphic`/`text`/`rectangle` at the
 * interface root. Deviating from that caused an instant client disconnect with zero server-side
 * signal earlier in this project.
 *
 * Structural rule #2, found the hard way *after* enabling CS2: toolbelt's `border` layer is chrome
 * ONLY - all real content lives in separate SIBLING layers (`search`, `content`, `scrollbar`,
 * `highlight`), never nested inside `border` itself. This file originally nested everything inside
 * `border`, which worked fine with a plain DIY rectangle background - but once `~stoneborder` starts
 * actively managing `$border`'s own children on load, it appears to clear/replace them: the frame
 * rendered perfectly (title, close button, stone texture) but every nested child (search button,
 * quantity row, the whole grid) vanished. Fixed by splitting `border` (empty, chrome-only) from a
 * sibling `content` layer holding everything else, matching toolbelt's actual shape.
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
private const val WIDTH = 488
private const val HEIGHT = 370

/** Toolbelt's own title-bar height, reused here since `~stoneborder` draws the same style header. */
private const val TITLE_H = 36

private const val COLS = 12
private const val ROWS = 6

/** Must stay in sync with `SpawnMenuScript`'s copy of this value. */
const val SLOT_COUNT = COLS * ROWS

const val QTY_BUTTON_COUNT = 4

private const val SLOT_SIZE = 36
private const val SLOT_PITCH = 40
private const val GRID_X = 4
private const val GRID_Y = 76

private const val BUTTON_SPRITE = 428 // sprites.button_brown
private const val BLANK_SPRITE = 3023 // sprites.blank

private const val COLOUR_TEXT = 0xffffff

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
 *    turning nothing needed it yet. Uncommented it, and its required companion task
 *    `UnpackDefaultCs2` (undocumented - only surfaced via a runtime error demanding it be present
 *    in the same task block).
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
        // file doc comment. "border" is childIndex 0 in this file, so its real key is 1.
        fun comp(childIndex: Int) = (iface shl 16) or (childIndex + 1)

        onLoadListener { arrayOf(initCs, comp(0)) }

        // Chrome only - see structural rule #2 above. No children nested here.
        layer("border") { // child 0
            position { 0 to 0 }
            size { WIDTH to HEIGHT }
            noClickThrough { true }
        }

        // Sibling of "border", not nested inside it - holds every real piece of content. Offset
        // down by TITLE_H so nothing sits underneath ~stoneborder's title bar.
        layer("content") { // child 1
            position { 0 to TITLE_H }
            size { WIDTH to (HEIGHT - TITLE_H) }

            graphic("searchbtn") {
                position { 372 to 4 }
                size { 108 to 24 }
                spriteId { BUTTON_SPRITE }
                addOption("Search")
                events = CLICK_EVENTS
            }

            // label only; carries no events so it never steals the button's click.
            text("searchlbl") {
                position { 372 to 4 }
                size { 108 to 24 }
                display { "Search" }
                font { FontType.FONT_REGULAR }
                color(COLOUR_TEXT)
                textShadowed { true }
                xAllignment { 1 }
                yAllignment { 1 }
            }

            for (i in 0 until QTY_BUTTON_COUNT) {
                graphic("qty$i") {
                    position { (8 + i * 64) to 48 }
                    size { 60 to 24 }
                    spriteId { BUTTON_SPRITE }
                    addOption("Select")
                    events = CLICK_EVENTS
                }
            }

            // text is replaced at runtime to highlight the active mode.
            for (i in 0 until QTY_BUTTON_COUNT) {
                text("qtylbl$i") {
                    position { (8 + i * 64) to 48 }
                    size { 60 to 24 }
                    display { QTY_LABELS[i] }
                    font { FontType.FONT_REGULAR }
                    color(COLOUR_TEXT)
                    textShadowed { true }
                    xAllignment { 1 }
                    yAllignment { 1 }
                }
            }

            text("status") {
                position { 272 to 50 }
                size { 208 to 20 }
                display { "" }
                font { FontType.FONT_SMALL }
                color(COLOUR_TEXT)
                textShadowed { true }
                xAllignment { 2 }
                yAllignment { 1 }
            }

            for (i in 0 until SLOT_COUNT) {
                val col = i % COLS
                val row = i / COLS
                graphic("slot$i") {
                    position { (GRID_X + col * SLOT_PITCH) to (GRID_Y + row * SLOT_PITCH) }
                    size { SLOT_SIZE to SLOT_SIZE }
                    spriteId { BLANK_SPRITE }
                    addOption("Spawn")
                    events = CLICK_EVENTS
                }
            }
        }
    }
