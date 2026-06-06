package org.rsmod.tools.wiki.dumping

import org.rsmod.tools.wiki.dumping.wiki.WikiDropParser

/** Formats wiki footnotes for Kotlin comments and TOML `#` notes. */
internal fun formatManualWikiNote(raw: String): String {
    var text = WikiDropParser.cleanWikiNotes(raw)
    text = text.replace(Regex("""\[\[([^\]]+)$""")) { it.groupValues[1].trim() }
    text = text.replace("[[", "").replace("]]", "").trim()

    if (text.contains("Frozen key", ignoreCase = true) && text.contains("Frozen Door", ignoreCase = true)) {
        return "Frozen key pieces are only dropped during The Frozen Door miniquest."
    }

    return text
}

internal fun formatKotlinComment(raw: String): String = formatManualWikiNote(raw).replace("*/", "* /")

internal fun buildTomlExportNotes(spec: GeneratedDropTableSpec): List<String> {
    val notes = mutableListOf<String>()
    for (entry in spec.unknownDropRates.distinctBy { it.itemName to it.rarity }) {
        val section = entry.section.lowercase()
        val subsection = entry.subsection.takeIf { it.isNotBlank() }?.let { "/$it" } ?: ""
        notes += "Unknown wiki drop rate: ${entry.itemName} [$section$subsection/${entry.rarity}]"
    }
    for (access in spec.subtableAccesses.filter { !it.isTomlExportableSharedAccess() }) {
        val label = access.wikiLabel.ifBlank { access.tableRef }
        when {
            access.needsHardcodedSharedTable ->
                notes += "Needs hardcoded shared table: $label (${access.numerator}/${access.denominator})"
            !access.herbRollVariants.isNullOrEmpty() ->
                notes += "Herb roll variants subtable: $label (${access.numerator}/${access.denominator})"
            else ->
                notes += "Unsupported shared table access: ${access.tableRef} (${access.numerator}/${access.denominator})"
        }
    }
    spec.poolPaddingWeight().takeIf { it > 0 }?.let { weight ->
        notes += "$POOL_PADDING_TOML_NOTE ($weight weight)"
    }
    for (entry in spec.allResolvedEntries()) {
        for (note in entry.wikiNotes.condition) {
            val formatted = formatManualWikiNote(note)
            if (formatted.isNotBlank()) {
                notes += "Drops need manual (${entry.obj.removePrefix("obj.")}): $formatted"
            }
        }
        for (note in entry.unhandledTransformRateNotes()) {
            val formatted = formatManualWikiNote(note)
            if (formatted.isNotBlank()) {
                notes += "Drops need manual rate (${entry.obj.removePrefix("obj.")}): $formatted"
            }
        }
    }
    return notes.distinct()
}
