package org.rsmod.tools.wiki.dumping.wiki

import dev.openrune.wiki.WikiDumpStore
import java.io.Closeable
import java.io.File
import kotlin.io.path.Path

/** Reads page wikitext from a local OSRS wiki XML dump. */
class WikiClient private constructor(
    private val store: WikiDumpStore,
    private val onPageFetch: ((String) -> Unit)? = null,
) : Closeable {
    fun wikiDumpStore(): WikiDumpStore = store
    private val pageCache = mutableMapOf<String, String>()

    val loadedPages: Int get() = store.pageCount

    val infoboxMonsterPages: Int get() = store.infoboxMonsterCount

    val itemSpawnPageCount: Int get() = cachedItemSpawnTitles().size

    val cachedPages: Int get() = pageCache.size

    private var itemSpawnTitlesCache: List<String>? = null

    override fun close() = Unit

    suspend fun rawPageSource(title: String): String {
        pageCache[title]?.let { return it }
        onPageFetch?.invoke(title)
        val content = store.rawPageSource(title)
        pageCache[title] = content
        return content
    }

    suspend fun fetchItemSpawnPageBatch(
        batchSize: Int,
        continueToken: String?,
    ): MonsterPageBatch {
        val all = cachedItemSpawnTitles()
        val offset = continueToken?.toIntOrNull() ?: 0
        val titles = all.drop(offset).take(batchSize.coerceIn(1, 500))
        val nextOffset = offset + titles.size
        val hasMore = nextOffset < all.size
        return MonsterPageBatch(
            titles = titles,
            continueToken = if (hasMore) nextOffset.toString() else null,
        )
    }

    suspend fun fetchMonsterPageBatch(
        batchSize: Int,
        continueToken: String?,
    ): MonsterPageBatch {
        val offset = continueToken?.toIntOrNull() ?: 0
        val titles =
            store.listInfoboxMonsterTitles(
                offset = offset,
                limit = batchSize.coerceIn(1, 500),
            )
        val nextOffset = offset + titles.size
        val hasMore = nextOffset < store.infoboxMonsterCount
        return MonsterPageBatch(
            titles = titles,
            continueToken = if (hasMore) nextOffset.toString() else null,
        )
    }

    data class MonsterPageBatch(
        val titles: List<String>,
        val continueToken: String?,
    )

    private fun cachedItemSpawnTitles(): List<String> {
        itemSpawnTitlesCache?.let { return it }
        val titles = WikiDumpStorePages.listItemSpawnLineTitles(store)
        itemSpawnTitlesCache = titles
        return titles
    }

    companion object {
        private const val DEFAULT_DUMP_DIR = "D:/OpenRune/OpenRune-FileStore-Server/dumps/wiki"

        fun resolveDumpDirectory(flagValue: String?): File {
            val configured =
                flagValue?.trim()?.takeIf { it.isNotEmpty() }
                    ?: System.getenv("WIKI_DUMP_DIR")?.trim()?.takeIf { it.isNotEmpty() }
                    ?: DEFAULT_DUMP_DIR
            return Path(configured).toFile()
        }

        fun open(
            wikiDumpDir: String? = null,
            onPageFetch: ((String) -> Unit)? = null,
        ): WikiClient {
            val store = WikiDumpStore.load(resolveDumpDirectory(wikiDumpDir))
            return WikiClient(store, onPageFetch)
        }
    }
}
