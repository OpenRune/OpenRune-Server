package org.rsmod.tools.wiki.dumping.dropfill

/**
 * Applies fills to a drop-table file's text, preserving every other byte.
 * - TOML: emit `[[main.separate_rolls]]` blocks (the dumper's own form) directly after the last
 *   existing main-path block; delete each filled anchor comment.
 * - Kotlin: emit `N outOf D separate "obj.x" count n` lines at the end of the `mainTable =
 *   rsPlayerWeightedTable(…) { … }` lambda; delete each filled anchor comment. Brace matching skips
 *   string/char literals and both comment forms, since a naive count lands in the wrong place.
 */
object DropRatePatcher {
    private fun renderTomlBlock(fill: Fill): String {
        val count =
            if (fill.qtyLow == fill.qtyHigh) "${fill.qtyLow}"
            else "\"${fill.qtyLow}..${fill.qtyHigh}\""
        return listOf(
                "[[main.separate_rolls]]",
                "numerator = ${fill.rate.numerator}",
                "denominator = ${fill.rate.denominator}",
                "",
                "[[main.separate_rolls.entries]]",
                "weight = 1",
                "obj = \"${fill.objSymbol}\"",
                "count = $count",
            )
            .joinToString("\n")
    }

    fun patchToml(text: String, fills: List<Fill>): String {
        if (fills.isEmpty()) {
            return text
        }
        val lines = text.split("\n")
        var lastMainHeader = -1
        for (i in lines.indices) {
            if (lines[i].startsWith("[main]") || lines[i].startsWith("[[main.")) {
                lastMainHeader = i
            }
        }
        check(lastMainHeader != -1) { "no [main] section — refusing to patch" }
        var end = lines.size
        for (i in lastMainHeader + 1 until lines.size) {
            if (lines[i].startsWith("[") || lines[i].startsWith("# Unknown wiki drop rate:")) {
                end = i
                break
            }
        }
        while (end > 0 && lines[end - 1].isBlank()) {
            end-- // insert before the blank gap that precedes the next section
        }
        val remove = fills.map { it.anchorLine - 1 }.toSet()
        val insertion = fills.joinToString("\n") { "\n" + renderTomlBlock(it) }.split("\n")
        val out = mutableListOf<String>()
        for (i in 0..lines.size) {
            if (i == end) {
                out += insertion
            }
            if (i < lines.size && i !in remove) {
                out += lines[i]
            }
        }
        return out.joinToString("\n")
    }

    private fun renderKotlinLine(fill: Fill): String {
        val count =
            if (fill.qtyLow == fill.qtyHigh) "${fill.qtyLow}" else "${fill.qtyLow}..${fill.qtyHigh}"
        return "        ${fill.rate.numerator} outOf ${fill.rate.denominator} separate \"${fill.objSymbol}\" count $count"
    }

    private data class Span(val open: Int, val close: Int)

    private fun mainTableSpan(text: String): Span? {
        val match =
            Regex("""mainTable = rsPlayerWeightedTable\([^)]*\)\s*\{""").find(text) ?: return null
        val open = match.range.last // index of the '{'
        var depth = 0
        var i = open
        while (i < text.length) {
            when {
                text[i] == '"' -> {
                    // string literal (handles escapes; triple-quoted strings do not occur here)
                    i++
                    while (i < text.length && text[i] != '"') {
                        i += if (text[i] == '\\') 2 else 1
                    }
                    i++
                }
                text[i] == '\'' -> {
                    i++
                    while (i < text.length && text[i] != '\'') {
                        i += if (text[i] == '\\') 2 else 1
                    }
                    i++
                }
                text[i] == '/' && text.getOrNull(i + 1) == '/' -> {
                    while (i < text.length && text[i] != '\n') i++
                }
                text[i] == '/' && text.getOrNull(i + 1) == '*' -> {
                    i += 2
                    while (i < text.length && !(text[i] == '*' && text.getOrNull(i + 1) == '/')) i++
                    i += 2
                }
                else -> {
                    if (text[i] == '{') {
                        depth++
                    } else if (text[i] == '}') {
                        depth--
                        if (depth == 0) {
                            return Span(open, i)
                        }
                    }
                    i++
                }
            }
        }
        return null
    }

    fun patchKotlin(text: String, fills: List<Fill>): String {
        if (fills.isEmpty()) {
            return text
        }
        val span =
            checkNotNull(mainTableSpan(text)) {
                "no mainTable = rsPlayerWeightedTable(...) { ... } span — refusing to patch"
            }
        val lines = text.split("\n")
        // Line index holding the main table's closing brace; the block is
        // inserted immediately before it. Working in line-index space (rather
        // than deleting by line content) means two byte-identical anchor
        // comments cannot both be removed when only one was targeted.
        val closeLine = text.substring(0, span.close).count { it == '\n' }
        check(closeLine != 0) { "main table close brace on the first line — refusing to patch" }
        val remove = fills.map { it.anchorLine - 1 }.toSet()
        for (index in remove) {
            check(index in lines.indices && lines[index].startsWith("//   - ")) {
                "anchorLine ${index + 1} is not an anchor comment — refusing to patch"
            }
        }
        val emitted = fills.map { renderKotlinLine(it) }
        val out = mutableListOf<String>()
        for (i in lines.indices) {
            if (i == closeLine) {
                out += emitted
            }
            if (i !in remove) {
                out += lines[i]
            }
        }
        val result = out.joinToString("\n")
        check(out.size == lines.size) {
            "line accounting off: ${lines.size} -> ${out.size} with ${fills.size} fills"
        }
        val patched =
            checkNotNull(mainTableSpan(result)) { "patched file lost its main table span" }
        for (fill in fills) {
            val at = result.indexOf(renderKotlinLine(fill))
            check(at in patched.open..patched.close) {
                "inserted line for ${fill.objSymbol} fell outside the main table span"
            }
        }
        return result
    }
}
