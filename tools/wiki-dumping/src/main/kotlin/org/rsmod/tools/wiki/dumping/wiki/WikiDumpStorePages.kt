package org.rsmod.tools.wiki.dumping.wiki

import dev.openrune.wiki.WikiDumpStore

internal object WikiDumpStorePages {
    private val itemSpawnLineTag =
        Regex("""\{\{ItemSpawnLine\b""", RegexOption.IGNORE_CASE)

    fun listShopInfoboxTitles(store: WikiDumpStore): List<String> =
        store
            .mainNamespacePages()
            .filter { page -> WikiShopInfoboxParser.pageHasShopInfobox(page.text) }
            .map { page -> page.title }
            .sorted()
            .toList()

    fun listShopInfoboxPages(store: WikiDumpStore): List<ParsedWikiShopInfobox> =
        store
            .mainNamespacePages()
            .mapNotNull { page -> WikiShopInfoboxParser.parseShopInfobox(page.title, page.text) }
            .toList()

    fun listItemSpawnLineTitles(store: WikiDumpStore): List<String> =
        store
            .mainNamespacePages()
            .filter { page -> itemSpawnLineTag.containsMatchIn(page.text) }
            .map { page -> page.title }
            .sorted()
            .toList()

    private val locLineTag =
        Regex("""\{\{LocLine\b""", RegexOption.IGNORE_CASE)

    private val infoboxNpcTag =
        Regex("""\{\{Infobox NPC\b""", RegexOption.IGNORE_CASE)

    private val infoboxMapTag =
        Regex("""\|\s*map\s*=\s*\{\{Map\b""", RegexOption.IGNORE_CASE)

    fun listLocLineTitles(store: WikiDumpStore): List<String> =
        store
            .mainNamespacePages()
            .filter { page -> locLineTag.containsMatchIn(page.text) }
            .map { page -> page.title }
            .sorted()
            .toList()

    fun listInfoboxNpcMapTitles(store: WikiDumpStore): List<String> =
        store
            .mainNamespacePages()
            .filter { page ->
                infoboxNpcTag.containsMatchIn(page.text) && infoboxMapTag.containsMatchIn(page.text)
            }
            .map { page -> page.title }
            .sorted()
            .toList()

    fun allMainNamespacePages(store: WikiDumpStore): Sequence<Pair<String, String>> =
        store.mainNamespacePages().map { page -> page.title to page.text }
}
