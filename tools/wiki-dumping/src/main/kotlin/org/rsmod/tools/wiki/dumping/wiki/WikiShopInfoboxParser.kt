package org.rsmod.tools.wiki.dumping.wiki



data class ParsedWikiShopInfobox(

    val pageTitle: String,

    val rsName: String?,

    val infoboxName: String,

) {

    val displayName: String

        get() = rsName?.takeIf { it.isNotBlank() } ?: infoboxName



    fun slugFromTitle(): String = WikiShopInfoboxParser.wikiSlugFromTitle(pageTitle)
}



object WikiShopInfoboxParser {

    private val shopInfoboxStart =

        Regex("""\{\{Infobox\s+Shop\b""", RegexOption.IGNORE_CASE)



    fun pageHasShopInfobox(wikitext: String): Boolean = shopInfoboxStart.containsMatchIn(wikitext)



    fun parseShopInfobox(pageTitle: String, wikitext: String): ParsedWikiShopInfobox? {

        val block =

            shopInfoboxStart

                .find(wikitext)

                ?.range

                ?.first

                ?.let { start -> WikiText.extractBalancedTemplate(wikitext, start) }

                ?: return null



        val params = WikiTemplateParser.parseParams(stripInfoboxHeader(block))

        val infoboxName =

            params["name"]

                ?.let(::sanitizeWikiMarkup)

                ?.takeIf { it.isNotBlank() }

                ?: pageTitle



        return ParsedWikiShopInfobox(

            pageTitle = pageTitle,

            rsName = parseExternalRsName(wikitext),

            infoboxName = infoboxName,

        )

    }



    fun parseExternalRsName(wikitext: String): String? =

        WikiTemplateParser

            .extractTemplates(wikitext, "External")

            .asSequence()

            .map { content -> WikiTemplateParser.parseParams(content)["rs"] }

            .mapNotNull { value -> value?.let(::sanitizeWikiMarkup)?.takeIf { it.isNotBlank() } }

            .firstOrNull()



    private fun stripInfoboxHeader(block: String): String {

        val headerEnd = block.indexOf('\n').takeIf { it >= 0 } ?: block.length

        return block.substring(headerEnd).removeSuffix("}}").trim()

    }



    private fun sanitizeWikiMarkup(input: String): String =

        input

            .replace(Regex("""\[\[([^|\]]+)\|([^\]]+)]]"""), "$2")

            .replace(Regex("""\[\[([^\]]+)]]"""), "$1")

            .replace("'''", "")

            .replace("''", "")

            .trim()



    fun wikiSlugFromTitle(title: String): String =

        title

            .lowercase()

            .replace(Regex("[''`]"), "")

            .replace(Regex("[^a-z0-9]+"), "_")

            .trim('_')

}

