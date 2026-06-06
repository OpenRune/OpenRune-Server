package org.rsmod.tools.wiki.dumping

import kotlin.system.measureTimeMillis

/** Structured console output for the drop-table dumper. */
class DropDumpLog(
    private val quiet: Boolean = false,
    private val verbose: Boolean = false,
) {
    var wikiFetches: Int = 0
        private set
    var itemLookups: Int = 0
        private set
    var itemCacheHits: Int = 0
        private set

    fun info(message: String) {
        if (!quiet) {
            println("[drop-dump] $message")
        }
    }

    fun verbose(message: String) {
        if (verbose && !quiet) {
            println("[drop-dump]   $message")
        }
    }

    fun warn(message: String) {
        System.err.println("[drop-dump] WARN $message")
    }

    fun error(message: String) {
        System.err.println("[drop-dump] ERROR $message")
    }

    fun phase(label: String, block: () -> Unit) {
        val elapsed = measureTimeMillis(block)
        info("$label (${elapsed}ms)")
    }

    suspend fun phaseAsync(label: String, block: suspend () -> Unit) {
        val elapsed = measureTimeMillis { block() }
        info("$label (${elapsed}ms)")
    }

    fun onWikiFetch(title: String) {
        wikiFetches++
        verbose("wiki fetch: $title")
    }

    fun onItemLookup(name: String, cached: Boolean) {
        itemLookups++
        if (cached) {
            itemCacheHits++
        } else {
            verbose("item lookup: $name")
        }
    }

    fun summary(pages: Int, elapsedMs: Long) {
        if (quiet) {
            return
        }
        info(
            "done — $pages page(s) in ${elapsedMs}ms " +
                "(wiki=$wikiFetches, items=$itemLookups, item-cache=$itemCacheHits)",
        )
    }
}
