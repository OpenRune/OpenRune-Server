package dev.openrune.tables.consumables.potion

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import java.io.File

internal data class PotionDefinition(
    val key: String,
    val row: String,
    val name: String,
    val items: List<String>,
    val empty: String,
    val effect: String,
    val category: String,
    val wildernessOnly: Boolean,
    val minigameOnly: String,
    val raidOnly: String,
    val mix: Boolean,
    val heal: Int,
    val drinkDelay: Int,
    val combatDelay: Int,
)

internal data class PotionEffectDefinition(
    val key: String,
    val row: String,
    val kind: String,
    val skills: List<String>,
    val base: Int,
    val percent: Int,
    val amount: Int,
    val effects: List<String>,
    val excludedSkills: List<String>,
    val restorePrayer: Boolean,
    val stamina: Boolean,
    val duration: Int,
    val poisonImmunity: Int,
    val venomImmunity: Int,
    val fullProtection: Boolean,
    val curesDisease: Boolean,
    val handler: String,
    val variant: String,
    val baseEffect: String?,
    val damage: Int,
)

internal data class PotionTomlData(
    val potions: List<PotionDefinition>,
    val effects: List<PotionEffectDefinition>,
)

internal object PotionTomlLoader {
    private const val POTIONS_FILE: String =
        "potions.toml"

    private const val OVERRIDES_FILE: String =
        "potion_overrides.toml"

    private val KEY_PATTERN =
        Regex("[a-z0-9]+(?:_[a-z0-9]+)*")

    private val POTION_ROOT_FIELDS: Set<String> =
        setOf("potion")

    private val OVERRIDE_ROOT_FIELDS: Set<String> =
        setOf(
            "potion_defaults",
            "potion_override",
            "potion_effect",
        )

    private val DEFAULT_FIELDS: Set<String> =
        setOf(
            "drinkDelay",
            "combatDelay",
        )

    private val POTION_FIELDS: Set<String> =
        setOf(
            "id",
            "row",
            "name",
            "objs",
            "empty",
            "effect",
            "category",
            "wildernessOnly",
            "minigameOnly",
            "raidOnly",
            "mix",
        )

    private val OVERRIDE_FIELDS: Set<String> =
        setOf(
            "id",
            "heal",
            "drinkDelay",
            "combatDelay",
        )

    private val EFFECT_FIELDS: Set<String> =
        setOf(
            "id",
            "row",
            "kind",
            "skills",
            "base",
            "percent",
            "amount",
            "effects",
            "excludedSkills",
            "restorePrayer",
            "stamina",
            "duration",
            "poisonImmunity",
            "venomImmunity",
            "fullProtection",
            "curesDisease",
            "handler",
            "variant",
            "baseEffect",
            "damage",
        )

    private val mapper =
        ObjectMapper(TomlFactory())
            .findAndRegisterModules()

    private val mapType =
        object : TypeReference<Map<String, Any?>>() {}

    private val loaded: PotionTomlData by lazy(::loadInternal)

    fun potions(): List<PotionDefinition> =
        loaded.potions

    fun effects(): List<PotionEffectDefinition> =
        loaded.effects

    private fun loadInternal(): PotionTomlData {
        val resources =
            findResourceDirectory()

        val potionRoot =
            readRoot(
                file = File(resources, POTIONS_FILE),
                allowedFields = POTION_ROOT_FIELDS,
            )

        val overrideRoot =
            readRoot(
                file = File(resources, OVERRIDES_FILE),
                allowedFields = OVERRIDE_ROOT_FIELDS,
            )

        val defaults =
            (overrideRoot["potion_defaults"] as? Map<*, *>)
                ?.toStringKeyMap(
                    "$OVERRIDES_FILE [potion_defaults]",
                )
                ?: emptyMap()

        defaults.requireOnlyFields(
            allowed = DEFAULT_FIELDS,
            context = "$OVERRIDES_FILE [potion_defaults]",
        )

        val defaultDrinkDelay =
            defaults.optionalInt(
                key = "drinkDelay",
                default = DEFAULT_DRINK_DELAY,
                context = "$OVERRIDES_FILE [potion_defaults]",
            )

        val defaultCombatDelay =
            defaults.optionalInt(
                key = "combatDelay",
                default = DEFAULT_COMBAT_DELAY,
                context = "$OVERRIDES_FILE [potion_defaults]",
            )

        val basePotions =
            potionRoot
                .requireBlocks(
                    key = "potion",
                    fileName = POTIONS_FILE,
                )
                .mapIndexed { index, values ->
                    values.toBasePotion(
                        context =
                            "$POTIONS_FILE [[potion]] #${index + 1}",
                    )
                }

        val overrides =
            overrideRoot
                .requireBlocks(
                    key = "potion_override",
                    fileName = OVERRIDES_FILE,
                )
                .mapIndexed { index, values ->
                    values.toPotionOverride(
                        context =
                            "$OVERRIDES_FILE [[potion_override]] #${index + 1}",
                    )
                }

        val effects =
            overrideRoot
                .requireBlocks(
                    key = "potion_effect",
                    fileName = OVERRIDES_FILE,
                )
                .mapIndexed { index, values ->
                    values.toPotionEffect(
                        context =
                            "$OVERRIDES_FILE [[potion_effect]] #${index + 1}",
                    )
                }

        require(basePotions.isNotEmpty()) {
            "$POTIONS_FILE does not contain any [[potion]] entries."
        }

        require(effects.isNotEmpty()) {
            "$OVERRIDES_FILE does not contain any [[potion_effect]] entries."
        }

        requireUnique(
            description = "potion ids",
            values = basePotions.map(BasePotion::key),
        )

        requireUnique(
            description = "potion rows",
            values = basePotions.map(BasePotion::row),
        )

        requireUnique(
            description = "potion override ids",
            values = overrides.map(PotionOverride::key),
        )

        requireUnique(
            description = "potion effect ids",
            values = effects.map(PotionEffectDefinition::key),
        )

        requireUnique(
            description = "potion effect rows",
            values = effects.map(PotionEffectDefinition::row),
        )

        val baseByKey =
            basePotions.associateBy(BasePotion::key)

        val unknownOverrides =
            overrides
                .map(PotionOverride::key)
                .filterNot(baseByKey::containsKey)

        require(unknownOverrides.isEmpty()) {
            buildString {
                appendLine(
                    "$OVERRIDES_FILE contains overrides for unknown potions:",
                )
                unknownOverrides
                    .sorted()
                    .forEach { appendLine(" - $it") }
            }
        }

        val overridesByKey =
            overrides.associateBy(PotionOverride::key)

        val mergedPotions =
            basePotions.map { base ->
                val override =
                    overridesByKey[base.key]

                PotionDefinition(
                    key = base.key,
                    row = base.row,
                    name = base.name,
                    items = base.items,
                    empty = base.empty,
                    effect = base.effect,
                    category = base.category,
                    wildernessOnly = base.wildernessOnly,
                    minigameOnly = base.minigameOnly,
                    raidOnly = base.raidOnly,
                    mix = base.mix,
                    heal = override?.heal ?: 0,
                    drinkDelay =
                        override?.drinkDelay
                            ?: defaultDrinkDelay,
                    combatDelay =
                        override?.combatDelay
                            ?: defaultCombatDelay,
                )
            }

        validatePotions(
            potions = mergedPotions,
            effects = effects,
        )

        validateEffects(effects)

        return PotionTomlData(
            potions = mergedPotions,
            effects = effects,
        )
    }

    private fun readRoot(
        file: File,
        allowedFields: Set<String>,
    ): Map<String, Any?> {
        require(file.isFile) {
            "Missing potion data file: ${file.canonicalPath}"
        }

        val root: Map<String, Any?> =
            mapper.readValue(file, mapType)

        root.requireOnlyFields(
            allowed = allowedFields,
            context = file.name,
        )

        return root
    }

    private fun Map<String, Any?>.requireBlocks(
        key: String,
        fileName: String,
    ): List<Map<String, Any?>> {
        val blocks =
            this[key] as? List<*>
                ?: error(
                    "$fileName does not contain any [[$key]] blocks.",
                )

        return blocks.mapIndexed { index, value ->
            (value as? Map<*, *>)
                ?.toStringKeyMap(
                    "$fileName [[$key]] #${index + 1}",
                )
                ?: error(
                    "$fileName [[$key]] #${index + 1} " +
                        "is not a TOML object.",
                )
        }
    }

    private fun Map<*, *>.toStringKeyMap(
        context: String,
    ): Map<String, Any?> =
        entries.associate { (rawKey, value) ->
            val key =
                rawKey as? String
                    ?: error(
                        "$context contains a non-string key.",
                    )

            key to value
        }

    private fun Map<String, Any?>.toBasePotion(
        context: String,
    ): BasePotion {
        requireOnlyFields(
            allowed = POTION_FIELDS,
            context = context,
        )

        return BasePotion(
            key = requireString("id", context),
            row = requireString("row", context),
            name = requireString("name", context),
            items = requireStringList("objs", context),
            empty = requireString("empty", context),
            effect = requireString("effect", context),
            category = requireString("category", context),
            wildernessOnly =
                optionalBoolean(
                    key = "wildernessOnly",
                    default = false,
                    context = context,
                ),
            minigameOnly =
                optionalString(
                    key = "minigameOnly",
                    default = "",
                    context = context,
                ),
            raidOnly =
                optionalString(
                    key = "raidOnly",
                    default = "",
                    context = context,
                ),
            mix =
                optionalBoolean(
                    key = "mix",
                    default = false,
                    context = context,
                ),
        )
    }

    private fun Map<String, Any?>.toPotionOverride(
        context: String,
    ): PotionOverride {
        requireOnlyFields(
            allowed = OVERRIDE_FIELDS,
            context = context,
        )

        return PotionOverride(
            key = requireString("id", context),
            heal = optionalIntOrNull("heal", context),
            drinkDelay =
                optionalIntOrNull(
                    key = "drinkDelay",
                    context = context,
                ),
            combatDelay =
                optionalIntOrNull(
                    key = "combatDelay",
                    context = context,
                ),
        )
    }

    private fun Map<String, Any?>.toPotionEffect(
        context: String,
    ): PotionEffectDefinition {
        requireOnlyFields(
            allowed = EFFECT_FIELDS,
            context = context,
        )

        val id =
            requireString("id", context)

        return PotionEffectDefinition(
            key = id,
            row =
                optionalString(
                    key = "row",
                    default = effectRow(id),
                    context = context,
                ),
            kind = requireString("kind", context),
            skills =
                optionalStringList("skills", context)
                    .map(::statReference),
            base = optionalInt("base", 0, context),
            percent = optionalInt("percent", 0, context),
            amount = optionalInt("amount", 0, context),
            effects =
                optionalStringList("effects", context)
                    .map(::effectRow),
            excludedSkills =
                optionalStringList(
                    "excludedSkills",
                    context,
                ).map(::statReference),
            restorePrayer =
                optionalBoolean(
                    "restorePrayer",
                    false,
                    context,
                ),
            stamina =
                optionalBoolean(
                    "stamina",
                    false,
                    context,
                ),
            duration =
                optionalDurationTicks(
                    "duration",
                    context,
                ),
            poisonImmunity =
                optionalDurationTicks(
                    "poisonImmunity",
                    context,
                ),
            venomImmunity =
                optionalDurationTicks(
                    "venomImmunity",
                    context,
                ),
            fullProtection =
                optionalBoolean(
                    "fullProtection",
                    false,
                    context,
                ),
            curesDisease =
                optionalBoolean(
                    "curesDisease",
                    false,
                    context,
                ),
            handler =
                optionalString(
                    "handler",
                    "",
                    context,
                ),
            variant =
                optionalString(
                    "variant",
                    "",
                    context,
                ),
            baseEffect =
                optionalStringOrNull(
                    "baseEffect",
                    context,
                )?.let(::effectRow),
            damage = optionalInt("damage", 0, context),
        )
    }

    private fun validatePotions(
        potions: List<PotionDefinition>,
        effects: List<PotionEffectDefinition>,
    ) {
        val effectRows =
            effects.mapTo(
                hashSetOf(),
                PotionEffectDefinition::row,
            )

        val allDoseItems =
            mutableSetOf<String>()

        potions.forEach { potion ->
            require(KEY_PATTERN.matches(potion.key)) {
                "${potion.name} has invalid id '${potion.key}'."
            }

            require(potion.row.startsWith("dbrow.")) {
                "${potion.name} has invalid row '${potion.row}'."
            }

            require(potion.category in POTION_CATEGORIES) {
                "${potion.name} has unsupported category " +
                    "'${potion.category}'."
            }

            require(potion.items.isNotEmpty()) {
                "${potion.name} must contain at least one dose object."
            }

            require(potion.items.all { it.startsWith("obj.") }) {
                "${potion.name} contains an invalid dose object: " +
                    potion.items.joinToString()
            }

            require(potion.empty.startsWith("obj.")) {
                "${potion.name} has invalid empty container " +
                    "'${potion.empty}'."
            }

            require(potion.effect in effectRows) {
                "${potion.name} references unknown effect row " +
                    "'${potion.effect}'."
            }

            require(potion.heal >= 0) {
                "${potion.name} has a negative heal amount."
            }

            require(
                potion.drinkDelay >= 0 &&
                    potion.combatDelay >= 0,
            ) {
                "${potion.name} contains a negative delay."
            }

            require(
                potion.minigameOnly.isBlank() ||
                    potion.raidOnly.isBlank(),
            ) {
                "${potion.name} cannot be both minigame-only " +
                    "and raid-only."
            }

            requireActivityKey(
                value = potion.minigameOnly,
                description = "minigame restriction",
                potionName = potion.name,
            )

            requireActivityKey(
                value = potion.raidOnly,
                description = "raid restriction",
                potionName = potion.name,
            )

            if (potion.mix) {
                require(potion.category == CATEGORY_BARBARIAN_MIX) {
                    "${potion.name} is marked as a mix but has " +
                        "category '${potion.category}'."
                }

                require(potion.items.size == MIX_DOSE_COUNT) {
                    "${potion.name} must contain exactly " +
                        "$MIX_DOSE_COUNT mix doses."
                }
            }

            potion.items.forEach { item ->
                require(allDoseItems.add(item)) {
                    "Potion dose object '$item' is assigned to " +
                        "more than one potion row."
                }
            }
        }
    }

    private fun validateEffects(
        effects: List<PotionEffectDefinition>,
    ) {
        val rows =
            effects.mapTo(
                hashSetOf(),
                PotionEffectDefinition::row,
            )

        val byRow =
            effects.associateBy(PotionEffectDefinition::row)

        effects.forEach { effect ->
            require(KEY_PATTERN.matches(effect.key)) {
                "Potion effect '${effect.key}' has an invalid id."
            }

            require(effect.row.isPotionEffectRow()) {
                "Potion effect '${effect.key}' has invalid row " +
                    "'${effect.row}'."
            }

            require(effect.kind in EFFECT_KINDS) {
                "Potion effect '${effect.key}' has unsupported kind " +
                    "'${effect.kind}'."
            }

            require(effect.skills.all { it.startsWith("stat.") }) {
                "Potion effect '${effect.key}' contains an invalid " +
                    "skill reference."
            }

            require(
                effect.excludedSkills.all {
                    it.startsWith("stat.")
                },
            ) {
                "Potion effect '${effect.key}' contains an invalid " +
                    "excluded skill reference."
            }

            require(effect.effects.all { it in rows }) {
                "Potion effect '${effect.key}' references an " +
                    "unknown compound effect."
            }

            require(
                effect.baseEffect == null ||
                    effect.baseEffect in rows,
            ) {
                "Potion effect '${effect.key}' references an " +
                    "unknown base effect."
            }

            require(
                listOf(
                    effect.base,
                    effect.percent,
                    effect.amount,
                    effect.duration,
                    effect.poisonImmunity,
                    effect.venomImmunity,
                    effect.damage,
                ).all { it >= 0 },
            ) {
                "Potion effect '${effect.key}' contains a negative " +
                    "numeric value."
            }

            require(effect.variant in EFFECT_VARIANTS) {
                "Potion effect '${effect.key}' has invalid variant " +
                    "'${effect.variant}'."
            }

            validateEffectShape(effect)
        }

        fun visit(
            row: String,
            visiting: MutableSet<String>,
            visited: MutableSet<String>,
        ) {
            if (row in visited) {
                return
            }

            require(visiting.add(row)) {
                "Potion effect cycle detected at '$row'."
            }

            val effect =
                byRow.getValue(row)

            effect.effects.forEach { nested ->
                visit(
                    row = nested,
                    visiting = visiting,
                    visited = visited,
                )
            }

            effect.baseEffect?.let { baseEffect ->
                visit(
                    row = baseEffect,
                    visiting = visiting,
                    visited = visited,
                )
            }

            visiting.remove(row)
            visited.add(row)
        }

        val visited =
            mutableSetOf<String>()

        rows.forEach { row ->
            visit(
                row = row,
                visiting = mutableSetOf(),
                visited = visited,
            )
        }
    }

    private fun validateEffectShape(
        effect: PotionEffectDefinition,
    ) {
        when (effect.kind) {
            KIND_STAT_BOOST -> {
                require(effect.skills.isNotEmpty()) {
                    "Stat boost '${effect.key}' must define skills."
                }
                require(effect.base > 0 || effect.percent > 0) {
                    "Stat boost '${effect.key}' must define a boost."
                }
            }

            KIND_FLAT_STAT_BOOST -> {
                require(effect.skills.isNotEmpty()) {
                    "Flat stat boost '${effect.key}' must define skills."
                }
                require(effect.amount > 0) {
                    "Flat stat boost '${effect.key}' must define amount."
                }
            }

            KIND_STAT_RESTORE,
            KIND_PRAYER_RESTORE,
                -> require(effect.base > 0 || effect.percent > 0) {
                "Restore effect '${effect.key}' must define a restore."
            }

            KIND_PRAYER_REGENERATION ->
                require(effect.duration > 0) {
                    "Prayer regeneration effect '${effect.key}' " +
                        "must have a positive duration."
                }

            KIND_RUN_ENERGY ->
                require(effect.amount > 0) {
                    "Run-energy effect '${effect.key}' must define amount."
                }

            KIND_POISON_CURE ->
                require(effect.poisonImmunity > 0) {
                    "Poison-cure effect '${effect.key}' must define " +
                        "poisonImmunity."
                }

            KIND_VENOM_CURE -> {
                require(effect.poisonImmunity > 0) {
                    "Venom-cure effect '${effect.key}' must define " +
                        "poisonImmunity."
                }
                require(effect.venomImmunity > 0) {
                    "Venom-cure effect '${effect.key}' must define " +
                        "venomImmunity."
                }
            }

            KIND_DRAGONFIRE_PROTECTION ->
                require(effect.duration > 0) {
                    "Dragonfire effect '${effect.key}' must have " +
                        "a positive duration."
                }

            KIND_COMPOUND -> {
                require(effect.effects.isNotEmpty()) {
                    "Compound effect '${effect.key}' must define effects."
                }
                if (effect.stamina) {
                    require(effect.duration > 0) {
                        "Stamina effect '${effect.key}' must have " +
                            "a positive duration."
                    }
                }
            }

            KIND_HANDLER ->
                require(effect.handler.isNotBlank()) {
                    "Handler effect '${effect.key}' is missing " +
                        "its handler name."
                }

            KIND_DIVINE -> {
                requireNotNull(effect.baseEffect) {
                    "Divine effect '${effect.key}' is missing baseEffect."
                }
                require(effect.duration > 0) {
                    "Divine effect '${effect.key}' must have " +
                        "a positive duration."
                }
                require(effect.damage > 0) {
                    "Divine effect '${effect.key}' must define damage."
                }
            }
        }
    }

    private fun requireActivityKey(
        value: String,
        description: String,
        potionName: String,
    ) {
        require(
            value.isBlank() ||
                KEY_PATTERN.matches(value),
        ) {
            "$potionName has invalid $description '$value'."
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

    private fun Map<String, Any?>.requireString(
        key: String,
        context: String,
    ): String {
        val value =
            this[key] as? String
                ?: error(
                    "$context is missing string field '$key'.",
                )

        require(value.isNotBlank()) {
            "$context field '$key' may not be blank."
        }

        return value
    }

    private fun Map<String, Any?>.optionalString(
        key: String,
        default: String,
        context: String,
    ): String {
        if (key !in this) {
            return default
        }

        return this[key] as? String
            ?: error(
                "$context field '$key' must be a string.",
            )
    }

    private fun Map<String, Any?>.optionalStringOrNull(
        key: String,
        context: String,
    ): String? {
        if (key !in this) {
            return null
        }

        return this[key] as? String
            ?: error(
                "$context field '$key' must be a string.",
            )
    }

    private fun Map<String, Any?>.requireStringList(
        key: String,
        context: String,
    ): List<String> {
        val values =
            this[key] as? List<*>
                ?: error(
                    "$context is missing list field '$key'.",
                )

        return values.toStringList(key, context)
    }

    private fun Map<String, Any?>.optionalStringList(
        key: String,
        context: String,
    ): List<String> {
        if (key !in this) {
            return emptyList()
        }

        val values =
            this[key] as? List<*>
                ?: error(
                    "$context field '$key' must be a list.",
                )

        return values.toStringList(key, context)
    }

    private fun List<*>.toStringList(
        key: String,
        context: String,
    ): List<String> =
        mapIndexed { index, value ->
            value as? String
                ?: error(
                    "$context field '$key' contains a non-string " +
                        "value at index $index.",
                )
        }

    private fun Map<String, Any?>.optionalBoolean(
        key: String,
        default: Boolean,
        context: String,
    ): Boolean {
        if (key !in this) {
            return default
        }

        return this[key] as? Boolean
            ?: error(
                "$context field '$key' must be a boolean.",
            )
    }

    private fun Map<String, Any?>.optionalInt(
        key: String,
        default: Int,
        context: String,
    ): Int {
        if (key !in this) {
            return default
        }

        return exactInt(
            value = this[key],
            context = "$context field '$key'",
        )
    }

    private fun Map<String, Any?>.optionalIntOrNull(
        key: String,
        context: String,
    ): Int? {
        if (key !in this) {
            return null
        }

        return exactInt(
            value = this[key],
            context = "$context field '$key'",
        )
    }

    private fun exactInt(
        value: Any?,
        context: String,
    ): Int {
        val longValue =
            when (value) {
                is Byte -> value.toLong()
                is Short -> value.toLong()
                is Int -> value.toLong()
                is Long -> value
                else ->
                    error(
                        "$context must be an integer.",
                    )
            }

        require(
            longValue >= Int.MIN_VALUE.toLong() &&
                longValue <= Int.MAX_VALUE.toLong(),
        ) {
            "$context is outside the supported integer range."
        }

        return longValue.toInt()
    }

    private fun Map<String, Any?>.optionalDurationTicks(
        key: String,
        context: String,
    ): Int {
        val raw =
            this[key]
                ?: return 0

        return when (raw) {
            is Byte,
            is Short,
            is Int,
            is Long,
                -> exactInt(
                value = raw,
                context = "$context field '$key'",
            )

            is String ->
                parseDurationTicks(
                    raw = raw,
                    context = "$context field '$key'",
                )

            else ->
                error(
                    "$context field '$key' must be an integer " +
                        "tick count or duration string.",
                )
        }
    }

    private fun parseDurationTicks(
        raw: String,
        context: String,
    ): Int {
        val match =
            DURATION_PATTERN.matchEntire(raw.trim())
                ?: error(
                    "$context has invalid duration '$raw'. " +
                        "Use values such as 90s, 2m, or 500t.",
                )

        val value =
            match.groupValues[1].toInt()

        return when (match.groupValues[2].lowercase()) {
            "t" -> value
            "s" -> secondsToTicks(value)
            "m" -> secondsToTicks(value * SECONDS_PER_MINUTE)
            else ->
                error(
                    "$context has unsupported duration '$raw'.",
                )
        }
    }

    private fun effectRow(value: String): String {
        if (
            value.substringBefore(
                delimiter = '.',
                missingDelimiterValue = "",
            ) == "dbrow"
        ) {
            return value
        }

        return listOf(
            "dbrow",
            "effect_$value",
        ).joinToString(".")
    }

    private fun statReference(value: String): String {
        if (
            value.substringBefore(
                delimiter = '.',
                missingDelimiterValue = "",
            ) == "stat"
        ) {
            return value
        }

        return listOf(
            "stat",
            value,
        ).joinToString(".")
    }

    private fun secondsToTicks(
        seconds: Int,
    ): Int =
        (
            seconds * GAME_TICKS_PER_THREE_SECONDS +
                SECONDS_PER_TICK_GROUP -
                1
            ) / SECONDS_PER_TICK_GROUP

    private fun requireUnique(
        description: String,
        values: List<String>,
    ) {
        val duplicates =
            values
                .groupingBy { it }
                .eachCount()
                .filterValues { count -> count > 1 }
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

    private fun String.isPotionEffectRow(): Boolean {
        val separator =
            indexOf('.')

        if (separator == -1) {
            return false
        }

        val type =
            substring(0, separator)

        val name =
            substring(separator + 1)

        return type == "dbrow" &&
            name.startsWith("effect_")
    }

    private fun findResourceDirectory(): File {
        val root =
            findProjectRoot()

        val resources =
            File(
                root,
                ".data/raw-cache/server/consumables",
            )

        require(resources.isDirectory) {
            "Consumables raw-cache directory does not exist: " +
                resources.canonicalPath
        }

        return resources
    }

    private fun findProjectRoot(): File {
        var directory =
            File(
                System.getProperty("user.dir"),
            ).canonicalFile

        while (true) {
            if (
                File(directory, "settings.gradle.kts").isFile &&
                File(directory, ".data/raw-cache/server").isDirectory
            ) {
                return directory
            }

            directory =
                directory.parentFile
                    ?: break
        }

        error(
            "Unable to locate the OpenRune project root from " +
                System.getProperty("user.dir"),
        )
    }

    private data class BasePotion(
        val key: String,
        val row: String,
        val name: String,
        val items: List<String>,
        val empty: String,
        val effect: String,
        val category: String,
        val wildernessOnly: Boolean,
        val minigameOnly: String,
        val raidOnly: String,
        val mix: Boolean,
    )

    private data class PotionOverride(
        val key: String,
        val heal: Int?,
        val drinkDelay: Int?,
        val combatDelay: Int?,
    )

    private val POTION_CATEGORIES: Set<String> =
        setOf(
            "potion",
            "barbarian_mix",
            "blighted_potion",
            "divine_potion",
            "tea",
            "nightmare_zone_potion",
            "cox_potion",
            "toa_supply",
        )

    private val EFFECT_KINDS: Set<String> =
        setOf(
            "stat_boost",
            "flat_stat_boost",
            "stat_restore",
            "prayer_restore",
            "prayer_regeneration",
            "run_energy",
            "poison_cure",
            "venom_cure",
            "dragonfire_protection",
            "compound",
            "handler",
            "divine",
        )

    private val EFFECT_VARIANTS: Set<String> =
        setOf(
            "",
            "weak",
            "normal",
            "strong",
        )

    private val DURATION_PATTERN =
        Regex(
            pattern = """^(\d+)([tsm])$""",
            option = RegexOption.IGNORE_CASE,
        )

    private const val KIND_STAT_BOOST: String =
        "stat_boost"

    private const val KIND_FLAT_STAT_BOOST: String =
        "flat_stat_boost"

    private const val KIND_STAT_RESTORE: String =
        "stat_restore"

    private const val KIND_PRAYER_RESTORE: String =
        "prayer_restore"

    private const val KIND_PRAYER_REGENERATION: String =
        "prayer_regeneration"

    private const val KIND_RUN_ENERGY: String =
        "run_energy"

    private const val KIND_POISON_CURE: String =
        "poison_cure"

    private const val KIND_VENOM_CURE: String =
        "venom_cure"

    private const val KIND_DRAGONFIRE_PROTECTION: String =
        "dragonfire_protection"

    private const val KIND_COMPOUND: String =
        "compound"

    private const val KIND_HANDLER: String =
        "handler"

    private const val KIND_DIVINE: String =
        "divine"

    private const val CATEGORY_BARBARIAN_MIX: String =
        "barbarian_mix"

    private const val MIX_DOSE_COUNT: Int = 2

    private const val DEFAULT_DRINK_DELAY: Int = 3
    private const val DEFAULT_COMBAT_DELAY: Int = 0

    private const val SECONDS_PER_MINUTE: Int = 60
    private const val GAME_TICKS_PER_THREE_SECONDS: Int = 5
    private const val SECONDS_PER_TICK_GROUP: Int = 3
}
