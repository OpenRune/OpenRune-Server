package org.rsmod.tools.wiki.dumping.wiki

object WikiTemplateParser {
    fun extractTemplates(wikitext: String, templateName: String): List<String> {
        val prefix = "{{$templateName|"
        val altPrefix = "{{$templateName}}"
        val results = mutableListOf<String>()
        var index = 0

        while (index < wikitext.length) {
            val start = wikitext.indexOf("{{", index)
            if (start < 0) {
                break
            }

            val nameEnd = wikitext.indexOfAny(charArrayOf('|', '}'), start + 2)
            if (nameEnd < 0) {
                break
            }

            val name = wikitext.substring(start + 2, nameEnd).trim()
            if (!name.equals(templateName, ignoreCase = true)) {
                index = start + 2
                continue
            }

            if (wikitext.startsWith(altPrefix, start) && !wikitext.startsWith(prefix, start)) {
                results += ""
                index = start + altPrefix.length
                continue
            }

            val contentStart = start + prefix.length
            val end = findTemplateEnd(wikitext, contentStart) ?: break
            results += wikitext.substring(contentStart, end - 2)
            index = end
        }

        return results
    }

    fun parseParams(content: String): Map<String, String> {
        if (content.isBlank()) {
            return emptyMap()
        }

        val params = linkedMapOf<String, String>()
        val parts = splitTopLevel(content, '|')
        var positional = 0

        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) {
                continue
            }

            val eq = trimmed.indexOf('=')
            if (eq <= 0) {
                params["_$positional"] = trimmed
                positional++
                continue
            }

            val key = trimmed.substring(0, eq).trim().lowercase()
            val value = trimmed.substring(eq + 1).trim()
            params[key] = value
        }

        return params
    }

    private fun findTemplateEnd(text: String, from: Int): Int? {
        var depth = 1
        var index = from
        while (index < text.length) {
            when {
                text.startsWith("{{", index) -> {
                    depth++
                    index += 2
                }
                text.startsWith("}}", index) -> {
                    depth--
                    index += 2
                    if (depth == 0) {
                        return index
                    }
                }
                else -> index++
            }
        }
        return null
    }

    private fun splitTopLevel(text: String, delimiter: Char): List<String> {
        val parts = mutableListOf<StringBuilder>()
        var current = StringBuilder()
        var depth = 0
        var index = 0

        while (index < text.length) {
            when {
                text.startsWith("{{", index) -> {
                    depth++
                    current.append("{{")
                    index += 2
                }
                text.startsWith("}}", index) -> {
                    depth = (depth - 1).coerceAtLeast(0)
                    current.append("}}")
                    index += 2
                }
                text[index] == delimiter && depth == 0 -> {
                    parts += current
                    current = StringBuilder()
                    index++
                }
                else -> {
                    current.append(text[index])
                    index++
                }
            }
        }
        parts += current
        return parts.map { it.toString() }
    }
}
