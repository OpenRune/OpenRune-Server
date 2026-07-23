package org.rsmod.content.generic.killcount

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.classgraph.ClassGraph
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.content.generic.killcount.toml.TomlKillcountDef

/**
 * Loads npc-to-varbit killcount mappings from every `.toml` file under [TOML_RESOURCE_ROOT] on the
 * classpath. Content modules contribute mappings by shipping their own resource file there, e.g.
 * `src/main/resources/killcount/bosses.toml`.
 *
 * An npc can map to more than one [Entry] - e.g. a slayer monster's boss variant (Abyssal Sire)
 * increments both its own dedicated boss varp and the shared slayer-monster varp (Abyssal demon).
 */
@Singleton
public class KillcountRegistry @Inject constructor() {
    private val entriesByNpc: MutableMap<String, MutableList<Entry>> = hashMapOf()

    init {
        loadTomlEntries()
    }

    public fun entriesFor(npc: String): List<Entry> = entriesByNpc[npc] ?: emptyList()

    public data class Entry(val varbit: String, val notify: Boolean)

    private fun loadTomlEntries() {
        val mapper =
            ObjectMapper(TomlFactory())
                .registerKotlinModule()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        ClassGraph()
            .ignoreClassVisibility()
            .acceptPaths(TOML_RESOURCE_ROOT)
            .scan()
            .use { scan ->
                scan.allResources
                    .filter { resource -> resource.path.endsWith(".toml") }
                    .forEach { resource ->
                        val def = mapper.readValue<TomlKillcountDef>(resource.getContentAsString())
                        def.killcounts.forEach { entry ->
                            entry.npcs.forEach { npc ->
                                register(npc, entry.varbit, entry.notify, resource.path)
                            }
                        }
                    }
            }
    }

    private fun register(npc: String, varbit: String, notify: Boolean, sourcePath: String) {
        val entries = entriesByNpc.getOrPut(npc) { mutableListOf() }
        check(entries.none { it.varbit == varbit }) {
            "Duplicate killcount mapping for npc '$npc' with varbit '$varbit' (from $sourcePath)."
        }
        entries += Entry(varbit, notify)
    }

    private companion object {
        private const val TOML_RESOURCE_ROOT = "killcount"
    }
}
