package org.rsmod.tools.mcp.wiki

data class WikiInfoboxNpcIdLine(
    val label: String,
    val npcIds: List<Int>,
)

object WikiInfoboxNpcIds {
    private val versionedIds =
        Regex("""\|\s*id(\d+)\s*=\s*([\d,\s]+)""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

    private val bareId =
        Regex("""\|\s*id\s*=\s*([\d,\s]+)""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

    fun parseMonsterIdLines(source: String): List<WikiInfoboxNpcIdLine> {
        val versioned =
            versionedIds
                .findAll(source)
                .mapNotNull { m ->
                    val n = m.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                    val ids = parseIdList(m.groupValues[2])
                    if (ids.isEmpty()) return@mapNotNull null
                    WikiInfoboxNpcIdLine(label = "id$n", npcIds = ids)
                }.sortedBy { line ->
                    line.label.removePrefix("id").toIntOrNull() ?: 0
                }.toList()

        if (versioned.isNotEmpty()) {
            return versioned
        }

        val bare = bareId.find(source) ?: return emptyList()
        val ids = parseIdList(bare.groupValues[1])
        return if (ids.isEmpty()) emptyList() else listOf(WikiInfoboxNpcIdLine(label = "id", npcIds = ids))
    }

    private fun parseIdList(raw: String): List<Int> =
        raw
            .split(',')
            .mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toIntOrNull() }
}
