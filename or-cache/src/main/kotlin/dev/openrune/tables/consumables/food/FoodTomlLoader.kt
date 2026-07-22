package dev.openrune.tables.consumables.food

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import java.io.File

internal data class FoodDefinition(
    val row: String,
    val name: String,
    val items: List<String>,
    val heal: Int,
    val combo: Boolean,
    val effect: String,
    val overheal: Boolean,
    val eatDelay: List<Int>,
    val combatDelay: List<Int>,
)

internal object FoodTomlLoader {
    private const val FOOD_FILE = "food.toml"
    private const val OVERRIDES_FILE = "food_overrides.toml"

    private val EFFECT_KEY_PATTERN =
        Regex("[a-z0-9]+(?:_[a-z0-9]+)*")

    private val BASE_FIELDS: Set<String> =
        setOf(
            "row",
            "name",
            "objs",
            "heal",
        )

    private val OVERRIDE_FIELDS: Set<String> =
        setOf(
            "row",
            "combo",
            "effect",
            "overheal",
            "eatDelay",
            "combatDelay",
        )

    private val mapper =
        ObjectMapper(TomlFactory())
            .findAndRegisterModules()

    private val mapType =
        object : TypeReference<Map<String, Any?>>() {}

    fun load(): List<FoodDefinition> {
        val resources = findResourceDirectory()

        val foodFile = File(resources, FOOD_FILE)
        val overridesFile = File(resources, OVERRIDES_FILE)

        val baseFoods =
            readBlocks(
                file = foodFile,
                key = "food",
            ).mapIndexed { index, values ->
                values.toBaseFood(
                    context = "$FOOD_FILE [[food]] #${index + 1}",
                )
            }

        val overrides =
            readBlocks(
                file = overridesFile,
                key = "food_override",
            ).mapIndexed { index, values ->
                values.toFoodOverride(
                    context =
                        "$OVERRIDES_FILE [[food_override]] #${index + 1}",
                )
            }

        require(baseFoods.isNotEmpty()) {
            "$FOOD_FILE does not contain any [[food]] entries."
        }

        requireUnique(
            description = "food rows",
            values = baseFoods.map { it.row },
        )

        requireUnique(
            description = "food override rows",
            values = overrides.map { it.row },
        )

        val baseRows = baseFoods.mapTo(hashSetOf()) { it.row }
        val unknownOverrides =
            overrides
                .map { it.row }
                .filterNot { it in baseRows }

        require(unknownOverrides.isEmpty()) {
            buildString {
                appendLine(
                    "$OVERRIDES_FILE contains rows that do not exist in $FOOD_FILE:",
                )
                unknownOverrides
                    .sorted()
                    .forEach { appendLine(" - $it") }
            }
        }

        val overridesByRow =
            overrides.associateBy { it.row }

        return baseFoods
            .map { base ->
                val override = overridesByRow[base.row]

                FoodDefinition(
                    row = base.row,
                    name = base.name,
                    items = base.items,
                    heal = base.heal,
                    combo = override?.combo ?: false,
                    effect = override?.effect.orEmpty(),
                    overheal = override?.overheal ?: false,
                    eatDelay = override?.eatDelay ?: emptyList(),
                    combatDelay =
                        override?.combatDelay ?: emptyList(),
                )
            }
            .also(::validate)
    }

    private fun readBlocks(
        file: File,
        key: String,
    ): List<Map<String, Any?>> {
        require(file.isFile) {
            "Missing food data file: ${file.canonicalPath}"
        }

        val root: Map<String, Any?> =
            mapper.readValue(file, mapType)

        val unknownRootKeys =
            root.keys - key

        require(unknownRootKeys.isEmpty()) {
            "${file.name} contains unknown top-level fields: " +
                unknownRootKeys.sorted().joinToString()
        }

        val blocks =
            root[key] as? List<*>
                ?: error(
                    "${file.name} does not contain any [[$key]] blocks.",
                )

        return blocks.mapIndexed { index, value ->
            val block =
                value as? Map<*, *>
                    ?: error(
                        "${file.name} [[$key]] #${index + 1} " +
                            "is not a TOML object.",
                    )

            block.entries.associate { (rawKey, rawValue) ->
                val stringKey =
                    rawKey as? String
                        ?: error(
                            "${file.name} [[$key]] #${index + 1} " +
                                "contains a non-string key.",
                        )

                stringKey to rawValue
            }
        }
    }

    private fun Map<String, Any?>.toBaseFood(
        context: String,
    ): BaseFood {
        requireOnlyFields(
            allowed = BASE_FIELDS,
            context = context,
        )

        return BaseFood(
            row = requireString("row", context),
            name = requireString("name", context),
            items = requireStringList("objs", context),
            heal = requireInt("heal", context),
        )
    }

    private fun Map<String, Any?>.toFoodOverride(
        context: String,
    ): FoodOverride {
        requireOnlyFields(
            allowed = OVERRIDE_FIELDS,
            context = context,
        )

        val row = requireString(
            key = "row",
            context = context,
        )

        return FoodOverride(
            row = row,
            combo = optionalBoolean(
                key = "combo",
                context = context,
            ),
            effect = optionalEffect(
                key = "effect",
                row = row,
                context = context,
            ),
            overheal = optionalBoolean(
                key = "overheal",
                context = context,
            ),
            eatDelay = optionalIntList(
                key = "eatDelay",
                context = context,
            ),
            combatDelay = optionalIntList(
                key = "combatDelay",
                context = context,
            ),
        )
    }

    private fun Map<String, Any?>.requireString(
        key: String,
        context: String,
    ): String {
        val value =
            this[key] as? String
                ?: error("$context is missing string field '$key'.")

        require(value.isNotBlank()) {
            "$context field '$key' may not be blank."
        }

        return value
    }

    private fun Map<String, Any?>.optionalEffect(
        key: String,
        row: String,
        context: String,
    ): String? {
        val value = this[key] ?: return null

        return when (value) {
            true ->
                effectKeyFromRow(
                    row = row,
                    context = context,
                )

            false ->
                null

            is String -> {
                require(value.isNotBlank()) {
                    "$context field '$key' may not be blank."
                }
                value
            }

            else ->
                error(
                    "$context field '$key' must be a boolean " +
                        "or string.",
                )
        }
    }

    private fun effectKeyFromRow(
        row: String,
        context: String,
    ): String {
        val separator = row.indexOf('.')

        require(
            separator > 0 &&
                separator < row.lastIndex,
        ) {
            "$context cannot derive an effect key from '$row'."
        }

        return row.substring(separator + 1)
    }

    private fun Map<String, Any?>.requireInt(
        key: String,
        context: String,
    ): Int {
        val value =
            this[key]
                ?: error(
                    "$context is missing integer field '$key'.",
                )

        return value.toExactInt(
            context = "$context field '$key'",
        )
    }

    private fun Map<String, Any?>.requireStringList(
        key: String,
        context: String,
    ): List<String> {
        val values =
            this[key] as? List<*>
                ?: error("$context is missing list field '$key'.")

        require(values.isNotEmpty()) {
            "$context field '$key' may not be empty."
        }

        return values.mapIndexed { index, value ->
            val stringValue =
                value as? String
                    ?: error(
                        "$context field '$key' contains a non-string " +
                            "value at index $index.",
                    )

            require(stringValue.isNotBlank()) {
                "$context field '$key' contains a blank value " +
                    "at index $index."
            }

            stringValue
        }
    }

    private fun Map<String, Any?>.optionalBoolean(
        key: String,
        context: String,
    ): Boolean? {
        if (key !in this) {
            return null
        }

        return this[key] as? Boolean
            ?: error("$context field '$key' must be a boolean.")
    }

    private fun Map<String, Any?>.optionalIntList(
        key: String,
        context: String,
    ): List<Int>? {
        if (key !in this) {
            return null
        }

        val values =
            this[key] as? List<*>
                ?: error("$context field '$key' must be a list.")

        return values.mapIndexed { index, value ->
            value.toExactInt(
                context =
                    "$context field '$key' value at index $index",
            )
        }
    }

    private fun Map<String, Any?>.requireOnlyFields(
        allowed: Set<String>,
        context: String,
    ) {
        val unknown =
            keys - allowed

        require(unknown.isEmpty()) {
            "$context contains unknown fields: " +
                unknown.sorted().joinToString()
        }
    }

    private fun Any?.toExactInt(
        context: String,
    ): Int {
        val number =
            this as? Number
                ?: error("$context must be an integer.")

        val doubleValue =
            number.toDouble()

        require(doubleValue.isFinite()) {
            "$context must be a finite integer."
        }

        val longValue =
            number.toLong()

        require(doubleValue == longValue.toDouble()) {
            "$context must be an exact integer, but was $number."
        }

        require(
            longValue in
                Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong(),
        ) {
            "$context is outside the 32-bit integer range: $number."
        }

        return longValue.toInt()
    }

    private fun validate(
        definitions: List<FoodDefinition>,
    ) {
        definitions.forEach { food ->
            require(food.row.startsWith("dbrow.")) {
                "${food.name} has invalid row '${food.row}'."
            }

            require(food.items.isNotEmpty()) {
                "${food.name} must contain at least one object."
            }

            require(food.items.all { it.startsWith("obj.") }) {
                "${food.name} contains an invalid object reference: " +
                    food.items.joinToString()
            }

            require(food.heal >= -1) {
                "${food.name} has invalid heal amount ${food.heal}."
            }

            require(food.eatDelay.all { it >= 0 }) {
                "${food.name} contains a negative eat delay."
            }

            require(food.combatDelay.all { it >= 0 }) {
                "${food.name} contains a negative combat delay."
            }

            require(food.eatDelay.size <= food.items.size) {
                "${food.name} has more eat delays than item stages."
            }

            require(food.combatDelay.size <= food.items.size) {
                "${food.name} has more combat delays than item stages."
            }

            require(
                food.effect.isBlank() ||
                    EFFECT_KEY_PATTERN.matches(food.effect),
            ) {
                "${food.name} has invalid effect key " +
                    "'${food.effect}'."
            }
        }
    }

    private fun requireUnique(
        description: String,
        values: List<String>,
    ) {
        val duplicates =
            values
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }
                .keys

        require(duplicates.isEmpty()) {
            buildString {
                appendLine("Duplicate $description found:")
                duplicates
                    .sorted()
                    .forEach { appendLine(" - $it") }
            }
        }
    }

    private fun findResourceDirectory(): File {
        val root = findProjectRoot()

        val resources =
            File(
                root,
                ".data/raw-cache/server/consumables",
            )

        require(resources.isDirectory) {
            "Consumables raw-cache directory does not exist: " +
                resources.canonicalPath
        }

        require(File(resources, FOOD_FILE).isFile) {
            "Missing food data file: " +
                File(resources, FOOD_FILE).canonicalPath
        }

        require(File(resources, OVERRIDES_FILE).isFile) {
            "Missing food overrides file: " +
                File(resources, OVERRIDES_FILE).canonicalPath
        }

        return resources
    }

    private fun findProjectRoot(): File {
        var directory =
            File(System.getProperty("user.dir")).canonicalFile

        while (true) {
            val hasSettings =
                File(directory, "settings.gradle.kts").isFile

            val hasContent =
                File(directory, "content").isDirectory

            if (hasSettings && hasContent) {
                return directory
            }

            directory = directory.parentFile ?: break
        }

        error(
            "Unable to locate the OpenRune project root from " +
                System.getProperty("user.dir"),
        )
    }

    private data class BaseFood(
        val row: String,
        val name: String,
        val items: List<String>,
        val heal: Int,
    )

    private data class FoodOverride(
        val row: String,
        val combo: Boolean?,
        val effect: String?,
        val overheal: Boolean?,
        val eatDelay: List<Int>?,
        val combatDelay: List<Int>?,
    )
}
