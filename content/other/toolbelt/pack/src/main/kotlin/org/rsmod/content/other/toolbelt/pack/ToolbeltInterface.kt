package org.rsmod.content.other.toolbelt.pack

import dev.openrune.cache.tools.iftype.dsl.buildInterface
import dev.openrune.cache.tools.iftype.dsl.impl.layer
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.type.widget.IfEvent

private const val WIDTH = 419
private const val HEIGHT = 291
private const val INSET = 10
private const val TITLE_H = 36
private const val SEARCH_H = 24
private const val SEARCH_GAP = 4
private const val SCROLL_W = 16
private const val CONTENT_TOP = TITLE_H + SEARCH_H + SEARCH_GAP

fun buildToolbeltInterface() =
    buildInterface(internalName = "interface.toolbelt", width = WIDTH, height = HEIGHT) {
        val iface = ConstantProvider.getMapping("interface.toolbelt")
        val initCs = ConstantProvider.getMapping("clientscript.toolbelt_init")
        fun comp(child: Int) = (iface shl 16) or child

        onLoadListener { arrayOf(initCs, comp(1), comp(2), comp(3), comp(4)) }

        layer("border") {
            position { 0 to 0 }
            size { WIDTH to HEIGHT }
            noClickThrough { true }
        }

        layer("search") {
            position { INSET to TITLE_H }
            size { WIDTH - INSET * 2 to SEARCH_H }
        }

        layer("content") {
            position { INSET to CONTENT_TOP }
            size { WIDTH - INSET * 2 - SCROLL_W - 2 to HEIGHT - CONTENT_TOP - INSET }
            scrollHeight { 0 }
            events = (IfEvent.Op1.bitmask or IfEvent.Op10.bitmask).toInt()
        }

        layer("scrollbar") {
            position { WIDTH - INSET - SCROLL_W to CONTENT_TOP }
            size { SCROLL_W to HEIGHT - CONTENT_TOP - INSET }
        }

        layer("highlight") {
            position { 0 to 0 }
            size { WIDTH to HEIGHT }
        }
    }
