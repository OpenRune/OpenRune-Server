// -----------------------
// RuneScape Tag Parser & Formatter with optional wrapping
// -----------------------
enum class RsColor(val tag: String, val hex: String) {
    RED("red", "800000"),
    GREEN("green", "00ff00"),
    BLUE("blue", "000080"),
    WHITE("white", "ffffff"),
    YELLOW("yellow", "ffff00"),
    CYAN("cyan", "00ffff"),
    MAGENTA("magenta", "ff00ff"),
    BLACK("black", "000000");

    companion object {
        fun lookup(nameOrHex: String): String? {
            val key = nameOrHex.lowercase()
            for (c in values()) if (c.tag == key) return c.hex
            return if (key.matches(Regex("^[0-9a-fA-F]{3,8}$"))) key else null
        }
    }
}

enum class RsStyle(val tag: String, val rsBase: String) {
    STRIKE("strike", "str"),
    UNDERLINE("underline", "u"),
    SHADOW("shad", "shad");

    companion object {
        fun fromTag(tag: String) = values().firstOrNull { it.tag.equals(tag, ignoreCase = true) }
    }
}

private data class ActiveTag(val type: String, val value: String?)

// -----------------------
// Main RS Conversion with optional wrap
// -----------------------
fun String.toRs(inheritPreviousTags: Boolean = true, wrapAt: Int? = null): String {
    val out = StringBuilder()
    val stack = ArrayDeque<ActiveTag>()
    var pendingRestore: ActiveTag? = null
    var i = 0

    while (i < length) {
        val ch = this[i]

        // Restore previous tag if inheritance enabled
        if (pendingRestore != null && inheritPreviousTags) {
            if (ch != '<' && !ch.isWhitespace()) {
                appendTag(out, pendingRestore!!)
                pendingRestore = null
            }
        }

        if (ch == '<') {
            val closeIndex = indexOf('>', i)
            if (closeIndex == -1) {
                out.append(substring(i))
                break
            }

            val rawTag = substring(i + 1, closeIndex)
            val tagContent = rawTag.lowercase().trim()
            i = closeIndex + 1

            when {
                // Closing tag
                tagContent.startsWith("/") -> {
                    val popped = stack.removeLastOrNull()
                    if (popped != null) out.append(closeTag(popped))
                    pendingRestore = if (inheritPreviousTags) stack.lastOrNull() else null
                }

                // Style with color <strike=red>
                Regex("^(strike|underline|shad)=([a-z0-9]+)$").matchEntire(tagContent) != null -> {
                    val match = Regex("^(strike|underline|shad)=([a-z0-9]+)$").matchEntire(tagContent)!!
                    val style = RsStyle.fromTag(match.groupValues[1])!!
                    val color = RsColor.lookup(match.groupValues[2]) ?: match.groupValues[2].lowercase()
                    out.append("<${style.rsBase}=$color>")
                    stack.addLast(ActiveTag(style.rsBase, color))
                }

                // Plain style <strike>
                RsStyle.fromTag(tagContent) != null -> {
                    val style = RsStyle.fromTag(tagContent)!!
                    out.append("<${style.rsBase}>")
                    stack.addLast(ActiveTag(style.rsBase, null))
                }

                // Color tag <red> or <ff00ff>
                RsColor.lookup(tagContent) != null -> {
                    val hex = RsColor.lookup(tagContent)!!
                    out.append("<col=$hex>")
                    stack.addLast(ActiveTag("col", hex))
                }

                // Unknown tag → literal
                else -> out.append("<$rawTag>")
            }
            continue
        }

        out.append(ch)
        i++
    }

    // Final pending restore
    if (pendingRestore != null && inheritPreviousTags) appendTag(out, pendingRestore)

    // Close remaining tags
    while (stack.isNotEmpty()) out.append(closeTag(stack.removeLast()))

    // Apply wrapping if wrapAt is defined
    return if (wrapAt != null) out.toString().wrapLines(wrapAt) else out.toString()
}

private fun appendTag(sb: StringBuilder, tag: ActiveTag) {
    val valuePart = tag.value?.let { "=$it" } ?: ""
    sb.append("<${tag.type}$valuePart>")
}

private fun closeTag(tag: ActiveTag) = when (tag.type) {
    "col"  -> "</col>"
    "str"  -> "</str>"
    "u"    -> "</u>"
    "shad" -> "</shad>"
    else   -> ""
}

// -----------------------
// Word-wrapping ignoring tags
// -----------------------
private fun String.wrapLines(maxLength: Int = 60): String {
    val out = StringBuilder()
    val activeTags = ArrayDeque<String>() // stack of currently open tags
    var visibleCount = 0
    var lastSpaceIndex = -1
    var lastSpaceVisibleCount = 0
    var i = 0

    while (i < length) {
        val ch = this[i]

        if (ch == '<') {
            val closeIndex = indexOf('>', i)
            if (closeIndex == -1) {
                out.append(substring(i))
                break
            }

            val tag = substring(i, closeIndex + 1)
            out.append(tag)

            if (!tag.startsWith("</")) {
                activeTags.addLast(tag)
            } else {
                val closeType = tag.drop(2).takeWhile { it != '=' && it != '>' }
                val idx = activeTags.indexOfLast { it.drop(1).takeWhile { c -> c != '=' && c != '>' } == closeType }
                if (idx != -1) activeTags.removeAt(idx)
            }

            i = closeIndex + 1
            continue
        }

        out.append(ch)
        visibleCount++

        if (ch.isWhitespace()) {
            lastSpaceIndex = out.length - 1
            lastSpaceVisibleCount = visibleCount
        }

        if (visibleCount >= maxLength) {
            val wrapPos = if (lastSpaceIndex != -1) lastSpaceIndex + 1 else out.length

            // Close tags
            val tagsToClose = activeTags.toList().reversed()
            val closeTagsStr = tagsToClose.joinToString("") { tag ->
                val type = tag.drop(1).takeWhile { it != '=' && it != '>' }
                "</$type>"
            }
            out.insert(wrapPos, "$closeTagsStr<br>")

            // Reopen tags safely, avoiding duplicates
            val reopenTagsStr = StringBuilder()
            for (tag in activeTags) {
                val checkLen = tag.length
                val endIndex = (wrapPos + closeTagsStr.length + 4).coerceAtMost(out.length)
                val alreadyHasTag = out.substring(endIndex - checkLen, endIndex).equals(tag, ignoreCase = true)
                if (!alreadyHasTag) reopenTagsStr.append(tag)
            }
            out.insert(wrapPos + closeTagsStr.length + 4, reopenTagsStr.toString())

            visibleCount = if (lastSpaceIndex != -1) visibleCount - lastSpaceVisibleCount else 0
            lastSpaceIndex = -1
            lastSpaceVisibleCount = 0
            i++
        } else {
            i++
        }
    }

    return out.toString()
}

// -----------------------
// Example
// -----------------------
fun main() {
    val input = "<blue>Hey this is a <red>test</red> of wrapping lines properly</blue>"

    println("RS conversion without wrapping:")
    println(input.toRs(false))

    println("\nRS conversion with wrapping at 15 chars:")
    println(input.toRs(false, wrapAt = 15))

    println("\nRS conversion with wrapping at 30 chars:")
    println(input.toRs(false, wrapAt = 30))
}