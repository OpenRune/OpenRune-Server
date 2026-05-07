package org.rsmod.tools.mcp.wiki

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.DataOutputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.rsmod.tools.mcp.wiki.wiki.OsrsWikiClient

class WikiToolServiceTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `wiki_search returns formatted result list`() = runBlocking {
        val engine =
            MockEngine { request ->
                val action = request.url.parameters["action"]
                if (action == "query") {
                    respond(
                        content =
                            """
                            {
                              "query": {
                                "search": [
                                  {"title": "Lumbridge", "snippet": "Starter town <b>near</b> the River Lum."},
                                  {"title": "Hans", "snippet": "Castle guide in Lumbridge."}
                                ]
                              }
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers =
                            headersOf(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString(),
                            ),
                    )
                } else {
                    error("Unexpected action: $action")
                }
            }

        val client = HttpClient(engine)
        val service = WikiToolService(wikiProvider = { OsrsWikiClient(client, mapper, "https://example.test/api.php") })

        val output = service.wikiSearch("lumbridge", 2)

        assertContains(output, "Found 2 results for 'lumbridge'")
        assertContains(output, "Lumbridge")
        assertContains(output, "https://oldschool.runescape.wiki/w/Lumbridge")
        assertContains(output, "Starter town near the River Lum.")
        client.close()
    }

    @Test
    fun `wiki_page returns normalized page text`() = runBlocking {
        val engine =
            MockEngine { request ->
                val action = request.url.parameters["action"]
                if (action == "parse") {
                    respond(
                        content =
                            """
                            {
                              "parse": {
                                "title": "Lumbridge",
                                "text": "<p><b>Lumbridge</b> is a city.</p><p>It has a castle.</p>"
                              }
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers =
                            headersOf(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString(),
                            ),
                    )
                } else {
                    error("Unexpected action: $action")
                }
            }

        val client = HttpClient(engine)
        val service = WikiToolService(wikiProvider = { OsrsWikiClient(client, mapper, "https://example.test/api.php") })

        val output = service.wikiPage("Lumbridge", 1000)

        assertContains(output, "Title: Lumbridge")
        assertContains(output, "URL: https://oldschool.runescape.wiki/w/Lumbridge")
        assertContains(output, "Lumbridge is a city. It has a castle.")
        client.close()
    }

    @Test
    fun `wiki_page applies max char clipping`() = runBlocking {
        val engine =
            MockEngine { _ ->
                respond(
                    content =
                        """
                        {
                          "parse": {
                            "title": "LongPage",
                            "text": "<p>${"a".repeat(300)}</p>"
                          }
                        }
                        """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers =
                        headersOf(
                            HttpHeaders.ContentType,
                            ContentType.Application.Json.toString(),
                        ),
                )
            }

        val client = HttpClient(engine)
        val wiki = OsrsWikiClient(client, mapper, "https://example.test/api.php")
        val page = wiki.page("LongPage", 50)

        assertEquals(53, page.text.length)
        assertContains(page.text, "...")
        client.close()
    }

    @Test
    fun `wiki_npc_spawns parses coordinates from locline source`() = runBlocking {
        val engine =
            MockEngine { request ->
                val action = request.url.parameters["action"]
                if (action == "query") {
                    respond(
                        content =
                            """
                            {
                              "query": {
                                "pages": [
                                  {
                                    "title": "Cave kraken",
                                    "revisions": [
                                      {
                                        "slots": {
                                          "main": {
                                            "content": "==Locations==\n{{LocTableHead}}\n{{LocLine\n|name = Kraken\n|location = [[Kraken Cove]]\n|levels = 127\n|members = Yes\n|mapID = 19\n|plane = 0\n|x:2245,y:10026|x:2246,y:10013\n|mtype = pin\n}}\n{{LocTableBottom}}"
                                          }
                                        }
                                      }
                                    ]
                                  }
                                ]
                              }
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers =
                            headersOf(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString(),
                            ),
                    )
                } else {
                    error("Unexpected action: $action")
                }
            }

        val client = HttpClient(engine)
        val service = WikiToolService(wikiProvider = { OsrsWikiClient(client, mapper, "https://example.test/api.php") })

        val output = service.wikiNpcSpawns(title = "Cave kraken", npcName = "Kraken", location = "Kraken Cove")

        assertContains(output, "Found 1 spawn entries")
        assertContains(output, "Kraken")
        assertContains(output, "Location: Kraken Cove")
        assertContains(output, "Spawn count: 2")
        assertContains(output, "x:2245,y:10026|x:2246,y:10013")
        client.close()
    }

    @Test
    fun `gameval_search returns disambiguation guidance for multiple matches`() = runBlocking {
        val root = Files.createTempDirectory("wiki-tool-gamevals")
        val binaryDir = root.resolve(".data").resolve("gamevals-binary")
        Files.createDirectories(binaryDir)

        writeDat(
            binaryDir.resolve("gamevals.dat"),
            mapOf(
                "npc" to listOf("kraken=1234", "cave_kraken=5678", "kraken_boss=7777"),
            ),
        )
        writeDat(binaryDir.resolve("gamevals_columns.dat"), mapOf("dbcolumn" to emptyList()))

        val client = HttpClient(MockEngine { error("No wiki call expected") })
        val service =
            WikiToolService(
                wikiProvider = { OsrsWikiClient(client, mapper, "https://example.test/api.php") },
                gamevalIndexProvider = { GamevalIndex.load(root.toString()) },
            )

        val output = service.gamevalSearch(query = "kraken", table = "npc", id = null, limit = 2)

        assertContains(output, "Found 3 gameval matches; showing 2")
        assertContains(output, "npc.kraken = 1234")
        assertContains(output, "Results truncated. Increase 'limit' to see more.")
        assertContains(output, "rerun with an exact key")
        client.close()
    }

    private fun writeDat(path: java.nio.file.Path, tables: Map<String, List<String>>) {
        DataOutputStream(Files.newOutputStream(path)).use { output ->
            output.writeInt(tables.size)
            for ((table, lines) in tables) {
                writeSizedUtf(output, table)
                output.writeInt(lines.size)
                for (line in lines) {
                    writeSizedUtf(output, line)
                }
            }
        }
    }

    private fun writeSizedUtf(output: DataOutputStream, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        output.writeShort(bytes.size)
        output.write(bytes)
    }
}

