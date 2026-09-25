package org.rsmod.tools.wiki.dumping

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

enum class BoostedRollKind(val key: String) {
    Main("main"),
    Separate("separate"),
    PreRoll("pre_roll"),
    Tertiary("tertiary"),
}

data class BoostedDropRule(
    val objs: List<String>,
    val npcs: List<String> = emptyList(),
    val rolls: List<String> = emptyList(),
) {
    private val npcPatterns: List<Regex> = npcs.map { it.globToRegex() }
    private val rollKinds: Set<String> = rolls.toSet()

    fun matches(spec: GeneratedDropTableSpec, kind: BoostedRollKind, obj: String): Boolean =
        obj in objs &&
            (rollKinds.isEmpty() || kind.key in rollKinds) &&
            (npcPatterns.isEmpty() || spec.npcRscmKeys.any { key -> npcPatterns.any { it.matches(key) } })

    private fun String.globToRegex(): Regex =
        Regex(split("*").joinToString(".*") { Regex.escape(it) })
}

private data class BoostedDropRuleFile(val boosted: List<BoostedDropRule> = emptyList())

object BoostedDropAllowlist {
    private const val RESOURCE = "boosted-drops.toml"

    val rules: List<BoostedDropRule> by lazy { load() }

    fun apply(spec: GeneratedDropTableSpec, rules: List<BoostedDropRule> = this.rules): GeneratedDropTableSpec {
        if (rules.isEmpty()) {
            return spec
        }

        fun matches(kind: BoostedRollKind, obj: String): Boolean =
            rules.any { it.matches(spec, kind, obj) }

        fun List<ResolvedDropEntry>.flag(kind: BoostedRollKind) =
            map { entry -> entry.copy(boosted = !entry.isNothing && matches(kind, entry.obj)) }

        fun List<SeparateRollSpec>.flag(kind: BoostedRollKind) =
            map { roll ->
                roll.copy(boosted = roll.entries.any { !it.isNothing && matches(kind, it.obj) })
            }

        return spec.copy(
            main = spec.main.flag(BoostedRollKind.Main),
            separateRolls = spec.separateRolls.flag(BoostedRollKind.Separate),
            preRoll = spec.preRoll.flag(BoostedRollKind.PreRoll),
            preRollSeparateRolls = spec.preRollSeparateRolls.flag(BoostedRollKind.PreRoll),
            tertiary = spec.tertiary.flag(BoostedRollKind.Tertiary),
        )
    }

    fun parse(text: String): List<BoostedDropRule> {
        val mapper =
            ObjectMapper(TomlFactory())
                .registerKotlinModule()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        return mapper.readValue<BoostedDropRuleFile>(text).boosted
    }

    private fun load(): List<BoostedDropRule> {
        val stream = javaClass.classLoader.getResourceAsStream(RESOURCE) ?: return emptyList()
        return parse(stream.bufferedReader().use { it.readText() })
    }
}
