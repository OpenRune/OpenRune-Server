package org.rsmod.tools

import dev.openrune.gamevals.GameValProvider
import dev.openrune.rscm.RSCM
import java.io.File
import java.net.URL

/* ---------------- SOURCES ---------------- */

private const val TOGGLE_SETTINGS_URL =
    "https://raw.githubusercontent.com/Joshua-F/osrs-dumps/refs/heads/master/script/%5Bproc%2Csettings_get_toggle%5D.cs2"

private const val DROPDOWN_SETTINGS_URL =
    "https://raw.githubusercontent.com/Joshua-F/osrs-dumps/refs/heads/master/script/%5Bproc%2Csettings_get_dropdown%5D.cs2"

private const val NUMBER_INPUT_SETTINGS_URL =
    "https://raw.githubusercontent.com/Joshua-F/osrs-dumps/refs/heads/master/script/%5Bproc%2Csettings_get_number_input%5D.cs2"

private const val COLOUR_SETTINGS_URL =
    "https://raw.githubusercontent.com/Joshua-F/osrs-dumps/refs/heads/master/script/%5Bproc%2Csettings_get_colour%5D.cs2"

private const val KEYBIND_SETTINGS_URL =
    "https://raw.githubusercontent.com/Joshua-F/osrs-dumps/refs/heads/master/script/%5Bproc%2Csettings_get_keybind%5D.cs2"


/* ---------------- MODEL ---------------- */

private data class SettingMapping(
    val id: Int,
    val name: String,
    val dropdownEnum: Int? = null,

    val min: Int? = null,
    val max: Int? = null,

    val prompt: String? = null,
    val enable_toggle_setting_id: String? = null
)

data class SettingOverride(
    val id: Int,
    val min: Int? = null,
    val max: Int? = null,
    val prompt: String? = null,
    val enable_toggle_setting_id: String? = null
)

/* ---------------- REGEX ---------------- */

private val TOGGLE_REGEX =
    Regex("""case\s+(\d+)\s*:\s*.*?%([a-zA-Z0-9_]+)""", RegexOption.DOT_MATCHES_ALL)

private val CASE_REGEX =
    Regex("""case\s+([\d,\s]+)\s*:\s*(.*?)(?=case\s+[\d,\s]+:|})""", RegexOption.DOT_MATCHES_ALL)

private val VAR_REGEX = Regex("""%([a-zA-Z0-9_]+)""")

private val ENUM_REGEX =
    Regex("""enum\([^)]*?,\s*enum_(\d+),\s*%([a-zA-Z0-9_]+)\)""")

private val SCRIPT_REGEX =
    Regex("""~[a-zA-Z0-9_]+(?:\([^)]*\))?""")

private fun parseKeybindMappings(text: String) =
    CASE_REGEX.findAll(text).flatMap { match ->
        val ids = match.groupValues[1]
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }

        val body = match.groupValues[2]

        val enum = ENUM_REGEX.find(body)
        val name = enum?.groupValues?.get(2)
            ?: VAR_REGEX.find(body)?.groupValues?.get(1)
            ?: return@flatMap emptySequence()

        val enumId = enum?.groupValues?.get(1)?.toInt()

        ids.asSequence().map {
            SettingMapping(
                id = it,
                name = name,
                dropdownEnum = enumId
            )
        }
    }.toList()

/* number input */
private val MIN_REGEX = Regex("""min[^%~]*?(\d+)""", RegexOption.IGNORE_CASE)
private val MAX_REGEX = Regex("""max[^%~]*?(\d+)""", RegexOption.IGNORE_CASE)

/* enable toggle */
private val ENABLE_REGEX = Regex("""enable[^%]*%([a-zA-Z0-9_]+)""", RegexOption.IGNORE_CASE)

/* prompt */
private val PROMPT_REGEX = Regex("""prompt[^%~]*%([a-zA-Z0-9_]+)""", RegexOption.IGNORE_CASE)

private val COLOUR_REGEX =
    Regex(
        """case\s+(\d+)\s*:\s*return\(calc\(%([a-zA-Z0-9_]+)\s*-\s*1\)\);""",
        RegexOption.DOT_MATCHES_ALL
    )

/* ---------------- MAIN ---------------- */

fun loadOverrides(file: File): Map<Int, SettingOverride> {
    if (!file.exists()) return emptyMap()

    val result = mutableMapOf<Int, SettingOverride>()

    var currentId: Int? = null
    val buffer = mutableMapOf<String, String>()

    fun flush() {
        val id = currentId ?: return
        result[id] = SettingOverride(
            id = id,
            min = buffer["min"]?.toIntOrNull(),
            max = buffer["max"]?.toIntOrNull(),
            prompt = buffer["prompt"],
            enable_toggle_setting_id = buffer["enable_toggle_setting_id"]
        )
        buffer.clear()
    }

    file.forEachLine { raw ->
        val line = raw.trim()

        when {
            line.isBlank() || line.startsWith("#") -> return@forEachLine

            line.startsWith("[") && line.endsWith("]") -> {
                flush()
                val key = line.removeSurrounding("[", "]")
                currentId = key.substringAfterLast(".").toIntOrNull()
            }

            "=" in line -> {
                val (k, v) = line.split("=", limit = 2)
                buffer[k.trim()] = v.trim().trim('"')
            }
        }
    }

    flush()
    return result
}

private fun parseColourMappings(text: String) =
    COLOUR_REGEX.findAll(text).map {
        SettingMapping(
            id = it.groupValues[1].toInt(),
            name = it.groupValues[2]
        )
    }.toList()

fun main() {
    GameValProvider.load(autoAssignIds = true)

    val root = findRepoRoot()

    val gameValsFile = root.resolve(
        "content/interfaces/settings/src/main/resources/gamevals.toml"
    )

    val overridesFile = root.resolve(
        "content/interfaces/settings/src/main/resources/settings_overrides.toml"
    )

    val tableFile = root.resolve(
        "or-cache/src/main/kotlin/dev/openrune/tables/SettingConfigs.kt"
    )

    val overrides = loadOverrides(overridesFile)

    val toggleMappings =
        URL(TOGGLE_SETTINGS_URL).readText().let(::parseToggleMappings)

    val dropdownMappings =
        URL(DROPDOWN_SETTINGS_URL).readText().let(::parseDropdownMappings)

    val numberMappings =
        URL(NUMBER_INPUT_SETTINGS_URL).readText().let(::parseNumberInputMappings)

    val colourMappings =
        URL(COLOUR_SETTINGS_URL).readText().let(::parseColourMappings)

    val keybindMappings =
        URL(KEYBIND_SETTINGS_URL).readText().let(::parseKeybindMappings)


    val mappings = (toggleMappings + dropdownMappings + numberMappings + colourMappings + keybindMappings)
        .groupBy { it.id }
        .map { (_, list) ->
            val base = list.reduce { a, b ->
                a.copy(
                    dropdownEnum = a.dropdownEnum ?: b.dropdownEnum,
                    min = a.min ?: b.min,
                    max = a.max ?: b.max,
                    prompt = a.prompt ?: b.prompt,
                    enable_toggle_setting_id = a.enable_toggle_setting_id ?: b.enable_toggle_setting_id
                )
            }

            val o = overrides[base.id]

            base.copy(
                min = o?.min ?: base.min,
                max = o?.max ?: base.max,
                prompt = o?.prompt ?: base.prompt,
                enable_toggle_setting_id = o?.enable_toggle_setting_id ?: base.enable_toggle_setting_id
            )
        }
        .sortedBy { it.id }

    updateGameVals(gameValsFile, mappings)

    val out = buildString {
        appendLine("package dev.openrune.tables")
        appendLine()
        appendLine("import dev.openrune.definition.dbtables.dbTable")
        appendLine("import dev.openrune.definition.util.VarType")
        appendLine()
        appendLine("object SettingConfigs {")
        appendLine()

        appendLine("    const val SETTING_ID = 0")
        appendLine("    const val VARP = 1")
        appendLine("    const val VARBIT_ID = 2")
        appendLine("    const val DEFAULT = 3")
        appendLine("    const val DROPDOWN_ENUM = 4")
        appendLine("    const val MIN = 5")
        appendLine("    const val MAX = 6")
        appendLine("    const val PROMPT = 7")
        appendLine("    const val ENABLE_TOGGLE = 8")

        appendLine()

        appendLine("""    fun settings() = dbTable("dbtable.settings_configs", serverOnly = true) {""")

        appendLine(
            """        column("setting_id", SETTING_ID, VarType.INT)"""
        )
        appendLine(
            """        column("varp", VARP, VarType.VARP)"""
        )
        appendLine(
            """        column("varbit", VARBIT_ID, VarType.INT)"""
        )
        appendLine(
            """        column("default", DEFAULT, VarType.INT)"""
        )
        appendLine("""        column("dropdown_enum", DROPDOWN_ENUM, VarType.INT)""")
        appendLine("""        column("min", MIN, VarType.INT)""")
        appendLine("""        column("max", MAX, VarType.INT)""")
        appendLine("""        column("prompt", PROMPT, VarType.STRING)""")
        appendLine("""        column("enable_toggle", ENABLE_TOGGLE, VarType.DBROW)""")

        appendLine()

        mappings.forEach { m ->
            val hasVarbit = runCatching { RSCM.getRSCM("varbit.${m.name}") }.isSuccess
            val hasVarp = runCatching { RSCM.getRSCM("varp.${m.name}") }.isSuccess

            appendLine("""        row("dbrow.setting_${m.id}") {""")
            appendLine("""            column(SETTING_ID, ${m.id})""")

            if (hasVarbit) {
                appendLine("""            columnRSCM(VARBIT_ID, "varbit.${m.name}")""")
            }

            if (hasVarp) {
                appendLine("""            columnRSCM(VARP, "varp.${m.name}")""")
            }

            m.dropdownEnum?.let {
                appendLine("""            column(DROPDOWN_ENUM, $it)""")
            }

            m.min?.let {
                appendLine("""            column(MIN, $it)""")
            }

            m.max?.let {
                appendLine("""            column(MAX, $it)""")
            }

            m.prompt?.let {
                appendLine("""            column(PROMPT, "$it")""")
            }

            m.enable_toggle_setting_id?.let {
                appendLine("""            columnRSCM(ENABLE_TOGGLE, "$it")""")
            }

            appendLine("        }")
            appendLine()
        }

        appendLine("    }")
        appendLine("}")
    }

    tableFile.parentFile.mkdirs()
    tableFile.writeText(out)

    println("Wrote ${tableFile.absolutePath}")
    println("Updated ${gameValsFile.absolutePath}")
}



/* ---------------- GAMEVALS ---------------- */

private fun updateGameVals(file: File, mappings: List<SettingMapping>) {
    val lines = file.readLines().toMutableList()

    val existing = mutableSetOf<Int>()
    var inDb = false

    for (line in lines) {
        when {
            line.startsWith("[gamevals.dbrow]") -> inDb = true
            line.startsWith("[") && inDb -> break
        }

        if (inDb && line.startsWith("setting_")) {
            line.substringAfter("setting_")
                .substringBefore("=")
                .toIntOrNull()
                ?.let { existing += it }
        }
    }

    val additions = mappings
        .map { it.id }
        .filterNot(existing::contains)
        .map { "setting_$it=-1" }

    if (additions.isEmpty()) return

    val idx = lines.indexOf("[gamevals.dbrow]")
    require(idx != -1)

    var insert = idx + 1
    while (insert < lines.size && !lines[insert].startsWith("[")) insert++

    lines.addAll(insert, additions)
    file.writeText(lines.joinToString(System.lineSeparator()))
}

/* ---------------- PARSERS ---------------- */

private fun parseToggleMappings(text: String) =
    TOGGLE_REGEX.findAll(text).map {
        SettingMapping(
            id = it.groupValues[1].toInt(),
            name = it.groupValues[2]
        )
    }.toList()

private fun parseDropdownMappings(text: String) =
    CASE_REGEX.findAll(text).flatMap { match ->
        val ids = match.groupValues[1]
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }

        val body = match.groupValues[2]

        val enum = ENUM_REGEX.find(body)
        val name = enum?.groupValues?.get(2)
            ?: VAR_REGEX.find(body)?.groupValues?.get(1)

        val enumId = enum?.groupValues?.get(1)?.toInt()

        ids.asSequence().map {
            SettingMapping(it, name ?: return@map null, enumId)
        }.filterNotNull()
    }.toList()

private fun parseNumberInputMappings(text: String) =
    CASE_REGEX.findAll(text).flatMap { match ->
        val ids = match.groupValues[1]
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }

        val body = match.groupValues[2]

        val name = VAR_REGEX.find(body)?.groupValues?.get(1)
            ?: return@flatMap emptySequence()

        val min = MIN_REGEX.find(body)?.groupValues?.get(1)?.toInt()
        val max = MAX_REGEX.find(body)?.groupValues?.get(1)?.toInt()
        val prompt = PROMPT_REGEX.find(body)?.groupValues?.get(1)

        val enable = ENABLE_REGEX.find(body)?.groupValues?.get(1)

        ids.asSequence().map {
            SettingMapping(
                id = it,
                name = name,
                min = min,
                max = max,
                prompt = prompt,
                enable_toggle_setting_id = enable
            )
        }
    }.toList()

/* ---------------- UTIL ---------------- */

private fun findRepoRoot(): File {
    var dir = File(System.getProperty("user.dir")).absoluteFile
    while (true) {
        if (File(dir, "content").isDirectory) return dir
        dir = dir.parentFile ?: error("Repo root not found")
    }
}
