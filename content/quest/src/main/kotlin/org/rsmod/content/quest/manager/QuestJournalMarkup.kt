package org.rsmod.content.quest.manager

import RsColor

internal fun journalLines(text: String): List<String> = text.lines().flatMap(::journalLine)

private class Glyph(
    val text: String,
    val colour: String?,
    val base: String?,
    val visible: Boolean = true,
)

private fun journalLine(source: String): List<String> {
    val struck = "<str>" in source
    val colours = ArrayList<String>()
    val segments = mutableListOf(mutableListOf<Glyph>())
    var base: String? = null
    var started = false
    var index = 0
    while (index < source.length) {
        val tag = TAG.matchAt(source, index)
        if (tag == null) {
            if (!started) {
                base = colours.firstOrNull()
                started = true
            }
            segments.last() += Glyph(source[index].toString(), colours.lastOrNull(), base)
            index++
            continue
        }
        val (closing, name, value) = tag.destructured
        val colour = if (name == "col") value.lowercase() else RsColor.lookup(name)
        when {
            name == "br" -> segments += mutableListOf<Glyph>()
            name == "str" -> Unit
            colour == null -> segments.last() += Glyph(tag.value, null, base, visible = false)
            closing.isNotEmpty() -> colours.removeLastOrNull()
            colours.size >= 2 && colours[colours.size - 2] == colour -> colours.removeLast()
            else -> colours += colour
        }
        index = tag.range.last + 1
    }
    return segments.flatMap(::wrap).map { render(it, struck) }
}

private fun wrap(glyphs: List<Glyph>): List<List<Glyph>> {
    val lines = mutableListOf<List<Glyph>>()
    var start = 0
    while (glyphs.subList(start, glyphs.size).count { it.visible } > JOURNAL_WIDTH) {
        var limit = start
        var seen = 0
        while (seen < JOURNAL_WIDTH || !glyphs[limit].visible) {
            if (glyphs[limit].visible) seen++
            limit++
        }
        val space = (limit downTo start + 1).firstOrNull { glyphs[it].text == " " }
        val end = space ?: limit
        lines += glyphs.subList(start, end)
        start = if (space != null) end + 1 else end
    }
    lines += glyphs.subList(start, glyphs.size)
    return lines
}

private fun render(glyphs: List<Glyph>, struck: Boolean): String {
    val visible = glyphs.filter { it.visible }
    val text = visible.joinToString("") { it.text }.trimEnd()
    if (text.isBlank()) {
        return ""
    }
    if (struck) {
        return "<str>$text"
    }
    val out = StringBuilder()
    var written: String? = null
    val base = visible.first().base
    if (base != null) {
        out.append("<col=").append(base).append('>')
        written = base
    }
    for (glyph in glyphs) {
        val colour = glyph.colour
        if (glyph.visible && colour != null && colour != written) {
            out.append("<col=").append(colour).append('>')
            written = colour
        }
        out.append(glyph.text)
    }
    return out.toString().trimEnd()
}

private const val JOURNAL_WIDTH = 64

private val TAG = Regex("<(/?)([a-z]+)(?:=([0-9A-Fa-f]{6}))?>")
