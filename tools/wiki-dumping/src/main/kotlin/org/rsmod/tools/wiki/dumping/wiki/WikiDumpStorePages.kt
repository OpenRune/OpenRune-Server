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

    fun allMainNamespacePages(store: WikiDumpStore): Sequence<Pair<String, String>> =
        store.mainNamespacePages().map { page -> page.title to page.text }
}
