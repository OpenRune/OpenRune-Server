package org.rsmod.content.quest.manager

import RsColor
import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.FontType
import java.nio.charset.Charset

internal fun journalLines(text: String): List<String> = text.lines().flatMap(::journalLine)

private class Glyph(
    val text: String,
    val colour: String?,
    val base: String?,
    val visible: Boolean = true,
    val measured: Char = text[0],
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
        val escaped = tag?.let { ESCAPES[it.value] }
        if (tag == null || escaped != null) {
            if (!started) {
                base = colours.firstOrNull()
                started = true
            }
            val text = if (tag == null) source[index].toString() else tag.value
            segments.last() += Glyph(text, colours.lastOrNull(), base, measured = escaped ?: text[0])
            index += text.length
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

// Same break rules as the client's text wrapper: last space (or hyphen) before the width is passed.
private fun wrap(glyphs: List<Glyph>): List<List<Glyph>> {
    val font = journalFont
    val lines = mutableListOf<List<Glyph>>()
    var start = 0
    var lineWidth = 0
    var breakEnd = -1
    var breakWidth = 0
    var breakGap = 0
    var previous = NONE
    for ((index, glyph) in glyphs.withIndex()) {
        if (!glyph.visible) continue
        val char = glyph.measured
        lineWidth += font.advance(char) + font.kerning(previous, char)
        previous = char
        if (char == ' ') {
            breakEnd = index + 1
            breakWidth = lineWidth
            breakGap = 1
        }
        if (lineWidth > JOURNAL_LINE_WIDTH && breakEnd >= 0) {
            lines += glyphs.subList(start, breakEnd - breakGap)
            start = breakEnd
            lineWidth -= breakWidth
            breakEnd = -1
            previous = NONE
        }
        if (char == '-') {
            breakEnd = index + 1
            breakWidth = lineWidth
            breakGap = 0
        }
    }
    lines += glyphs.subList(start, glyphs.size)
    return lines
}

private fun glyphIndex(char: Char): Int =
    when {
        char == NBSP -> ' '.code
        char.code < 0x80 || char.code in 0xA0..0xFF -> char.code
        else -> CP1252_EXTRA[char] ?: '?'.code
    }

private fun FontType.advance(char: Char): Int = glyphAdvances[glyphIndex(char)]

private fun FontType.kerning(previous: Char, char: Char): Int {
    if (kerning.isEmpty() || previous == NONE) return 0
    return kerning[(glyphIndex(previous) shl 8) or glyphIndex(char)].toInt()
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

private const val JOURNAL_LINE_WIDTH = 350

private const val NONE = 0.toChar()
private const val NBSP = 160.toChar()

private val ESCAPES = mapOf("<lt>" to '<', "<gt>" to '>')

private val CP1252_EXTRA: Map<Char, Int> =
    String(ByteArray(32) { (it + 0x80).toByte() }, Charset.forName("windows-1252"))
        .withIndex()
        .filter { it.value != '\uFFFD' }
        .associate { it.value to it.index + 0x80 }

private val journalFont: FontType by lazy {
    val font = ServerCacheManager.fromComponent("component.questjournal:qj1").textFont
    checkNotNull(ServerCacheManager.getFont(font)) { "Quest journal font $font is missing from the cache." }
}

private val TAG = Regex("<(/?)([a-z]+)(?:=([0-9A-Fa-f]{6}))?>")
