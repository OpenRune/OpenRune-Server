package org.rsmod.tools.wiki.dumping

import org.rsmod.tools.wiki.dumping.wiki.WikiClient
import org.rsmod.tools.wiki.dumping.wiki.WikiTemplateParser

/** Resolves wiki item display names to cache item ids from item page infoboxes. */
class ItemWikiLookup(
    private val wiki: WikiClient,
    private val log: DropDumpLog = DropDumpLog(quiet = true),
) {
    private val nameToId = mutableMapOf<String, Int>()
    private val idToName = mutableMapOf<Int, String>()

    suspend fun prewarm(displayNames: Collection<String>) {
        val pending =
            displayNames
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .filter { normalizeName(it) !in nameToId }
        if (pending.isEmpty()) {
            return
        }
        log.verbose("prewarming ${pending.size} item(s) from wiki")
        for (name in pending) {
            resolveItemId(name)
        }
    }

    suspend fun resolveItemId(displayName: String): Int? {
        val normalized = normalizeName(displayName)
        nameToId[normalized]?.let {
            log.onItemLookup(displayName, cached = true)
            return it
        }

        for (title in wikiPageTitles(displayName)) {
            val id = resolveItemIdFromPage(title, displayName) ?: continue
            nameToId[normalized] = id
            idToName[id] = displayName
            log.onItemLookup(displayName, cached = false)
            return id
        }

        log.onItemLookup(displayName, cached = false)
        return null
    }

    fun cachedName(itemId: Int): String? = idToName[itemId]

    fun cachedItemId(displayName: String): Int? = nameToId[normalizeName(displayName)]

    private suspend fun resolveItemIdFromPage(title: String, displayName: String): Int? {
        val (source, redirectAnchor) = fetchPageSourceFollowingRedirects(title) ?: return null
        val parsed = parseDisplayName(displayName)
        val anchor = parsed.hashAnchor ?: redirectAnchor
        val lookupName =
            if (anchor != null) {
                "${parsed.wikiName}#$anchor"
            } else {
                displayName
            }
        return parseInfoboxItemId(source, lookupName)
    }

    private suspend fun fetchPageSourceFollowingRedirects(startTitle: String): Pair<String, String?>? {
        var pageTitle = startTitle
        var inheritedAnchor: String? = null
        repeat(6) {
            val source = runCatching { wiki.rawPageSource(pageTitle) }.getOrNull() ?: return null
            val redirect = parseRedirectTarget(source) ?: return source to inheritedAnchor
            if (inheritedAnchor == null) {
                inheritedAnchor = redirect.anchor
            }
            pageTitle = redirect.title
        }
        return null
    }

    private fun normalizeName(name: String): String = name.trim().lowercase()

    private data class InfoboxVariant(
        val index: Int,
        val name: String,
        val id: Int,
        val version: String? = null,
    )

    private data class ParsedDisplayName(
        val wikiName: String,
        val hashAnchor: String? = null,
    )

    private data class RedirectTarget(
        val title: String,
        val anchor: String? = null,
    )

    private companion object {
        private val simpleItemIdPattern = Regex("""(?im)\|id\s*=\s*(\d+)\s*$""")
        private val simpleItemNamePattern = Regex("""(?im)\|name\s*=\s*(.+)$""")
        private val numberedNamePattern = Regex("""(?im)\|name(\d+)\s*=\s*(.+)$""")
        private val numberedIdPattern = Regex("""(?im)\|id(\d+)\s*=\s*(.+)$""")
        private val versionPattern = Regex("""(?im)\|version(\d+)\s*=\s*(.+)$""")
        private val redirectTargetPattern = Regex("""(?im)^#REDIRECT\s*\[\[([^\]]+)]]""")

        private fun parseDisplayName(displayName: String): ParsedDisplayName {
            val trimmed = displayName.trim()
            val hashIndex = trimmed.indexOf('#')
            if (hashIndex < 0) {
                return ParsedDisplayName(trimmed)
            }
            return ParsedDisplayName(
                wikiName = trimmed.substring(0, hashIndex).trim(),
                hashAnchor = trimmed.substring(hashIndex + 1).trim().takeIf { it.isNotBlank() },
            )
        }

        private fun wikiPageTitles(displayName: String): List<String> {
            val trimmed = parseDisplayName(displayName).wikiName
            val underscored = trimmed.replace(' ', '_')
            val baseName = parentheticalBase(trimmed)
            val doseQualifier = parentheticalDoseQualifier(trimmed)
            return buildList {
                add(trimmed)
                add(underscored)
                if (baseName != null) {
                    add(baseName)
                    add(baseName.replace(' ', '_'))
                    if (doseQualifier != null) {
                        add("$baseName$doseQualifier")
                        add("${baseName.replace(' ', '_')}$doseQualifier")
                    }
                }
            }.distinct()
        }

        private fun parseRedirectTarget(source: String): RedirectTarget? {
            val inner =
                redirectTargetPattern
                    .find(source.trim())
                    ?.groupValues
                    ?.get(1)
                    ?.trim()
                    ?: return null
            val linkTarget = inner.substringBefore('|').trim().replace('_', ' ')
            val anchor =
                linkTarget
                    .substringAfter('#', "")
                    .trim()
                    .takeIf { '#' in linkTarget && it.isNotBlank() }
            return RedirectTarget(title = linkTarget.substringBefore('#').trim(), anchor = anchor)
        }

        private fun parsePrimaryItemId(raw: String): Int? =
            raw.trim().substringBefore(',').trim().toIntOrNull()

        private fun parseInfoboxItemId(wikitext: String, displayName: String): Int? {
            val parsed = parseDisplayName(displayName)
            parseNestedInfoboxItemId(wikitext, displayName)?.let { return it }

            val variants = parseInfoboxVariants(wikitext)
            if (variants.isNotEmpty()) {
                selectVariantId(variants, displayName)?.let { return it }
                if (parsed.hashAnchor != null) {
                    return null
                }
            }

            val simpleId = simpleItemIdPattern.find(wikitext)?.groupValues?.get(1)?.toIntOrNull()
            if (simpleId != null) {
                val simpleName =
                    simpleItemNamePattern.find(wikitext)?.groupValues?.get(1)?.trim().orEmpty()
                if (simpleName.isBlank() || namesMatch(simpleName, displayName)) {
                    return simpleId
                }
            }

            if (parsed.hashAnchor != null) {
                return null
            }

            return variants.firstOrNull { it.index == 1 }?.id ?: variants.firstOrNull()?.id
        }

        private fun parseNestedInfoboxItemId(wikitext: String, displayName: String): Int? {
            for (content in WikiTemplateParser.extractTemplates(wikitext, "Infobox Item")) {
                val wrapped = "{{Infobox Item\n$content}}"
                val variants = parseInfoboxVariants(wrapped)
                if (variants.isNotEmpty()) {
                    selectVariantId(variants, displayName)?.let { return it }
                    continue
                }

                val name = simpleItemNamePattern.find(content)?.groupValues?.get(1)?.trim().orEmpty()
                val id = simpleItemIdPattern.find(content)?.groupValues?.get(1)?.toIntOrNull() ?: continue
                if (name.isBlank() || namesMatch(name, displayName)) {
                    return id
                }
            }
            return null
        }

        private fun parseInfoboxVariants(wikitext: String): List<InfoboxVariant> {
            val baseName = infoboxBaseName(wikitext)
            val names =
                numberedNamePattern
                    .findAll(wikitext)
                    .associate { match -> match.groupValues[1].toInt() to match.groupValues[2].trim() }
            val ids =
                numberedIdPattern
                    .findAll(wikitext)
                    .mapNotNull { match ->
                        val index = match.groupValues[1].toInt()
                        val id = parsePrimaryItemId(match.groupValues[2]) ?: return@mapNotNull null
                        index to id
                    }.toMap()
            val versions =
                versionPattern
                    .findAll(wikitext)
                    .associate { match -> match.groupValues[1].toInt() to match.groupValues[2].trim() }

            val fromNumberedNames =
                names.mapNotNull { (index, name) ->
                    val id = ids[index] ?: return@mapNotNull null
                    InfoboxVariant(index = index, name = name, id = id, version = versions[index])
                }

            val fromSharedName =
                if (baseName.isNullOrBlank()) {
                    emptyList()
                } else {
                    ids
                        .filterKeys { it !in names }
                        .map { (index, id) ->
                            InfoboxVariant(
                                index = index,
                                name = baseName,
                                id = id,
                                version = versions[index],
                            )
                        }
                }

            return (fromNumberedNames + fromSharedName).sortedBy { it.index }
        }

        private fun infoboxBaseName(wikitext: String): String? =
            simpleItemNamePattern
                .find(wikitext)
                ?.groupValues
                ?.get(1)
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        private fun selectVariantId(variants: List<InfoboxVariant>, displayName: String): Int? {
            val parsed = parseDisplayName(displayName)
            val wikiName = parsed.wikiName

            parsed.hashAnchor?.let { anchor ->
                variants
                    .firstOrNull { it.version?.equals(anchor, ignoreCase = true) == true }
                    ?.let { return it.id }
            }

            val normalized = wikiName.lowercase()
            val baseDisplay = parentheticalBase(wikiName)?.lowercase()

            variants
                .filter { it.name.trim().equals(wikiName, ignoreCase = true) }
                .let { exactMatches ->
                    if (exactMatches.isNotEmpty()) {
                        return preferItemVersion(exactMatches)?.id ?: exactMatches.first().id
                    }
                }

            parentheticalQualifier(wikiName)?.let { qualifier ->
                variants
                    .firstOrNull { it.version?.equals(qualifier, ignoreCase = true) == true }
                    ?.let { return it.id }
            }

            if (baseDisplay != null) {
                val baseMatches = variants.filter { it.name.trim().lowercase() == baseDisplay }
                if (baseMatches.isNotEmpty()) {
                    return preferItemVersion(baseMatches)?.id ?: baseMatches.first().id
                }
            }

            val infoboxBase = variants.firstOrNull()?.name?.trim()?.lowercase()
            if (infoboxBase != null && (normalized == infoboxBase || baseDisplay == infoboxBase)) {
                val sharedNameMatches = variants.filter { it.name.trim().lowercase() == infoboxBase }
                if (sharedNameMatches.isNotEmpty()) {
                    return preferItemVersion(sharedNameMatches)?.id ?: sharedNameMatches.first().id
                }
            }

            return null
        }

        private fun parentheticalQualifier(name: String): String? =
            Regex("""\((.+)\)""")
                .find(parseDisplayName(name).wikiName)
                ?.groupValues
                ?.get(1)
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        private fun parentheticalDoseQualifier(name: String): String? =
            parentheticalQualifier(name)?.takeIf { it.all(Char::isDigit) }

        private fun preferItemVersion(variants: List<InfoboxVariant>): InfoboxVariant? =
            variants.firstOrNull { it.version?.equals("Item", ignoreCase = true) == true }

        private fun namesMatch(infoboxName: String, displayName: String): Boolean {
            val wikiName = parseDisplayName(displayName).wikiName
            if (infoboxName.equals(wikiName, ignoreCase = true)) {
                return true
            }
            val base = parentheticalBase(wikiName) ?: return false
            return infoboxName.equals(base, ignoreCase = true)
        }

        private fun parentheticalBase(name: String): String? =
            Regex("""^(.+?)\s*\(.+\)$""")
                .find(parseDisplayName(name).wikiName)
                ?.groupValues
                ?.get(1)
                ?.trim()
    }
}
