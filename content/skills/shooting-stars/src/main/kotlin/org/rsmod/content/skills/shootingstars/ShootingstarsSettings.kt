package org.rsmod.content.skills.shootingstars

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlin.random.Random

@JsonIgnoreProperties(ignoreUnknown = true)
data class ShootingstarsSettings(
    @JsonProperty("is_enabled") val isEnabled: Boolean = true,
    @JsonProperty("spawn_interval_minutes") val spawnIntervalMinutes: Int = 90,
    @JsonProperty("spawn_variation_minutes") val spawnVariationMinutes: Int = 15,
    @JsonProperty("boot_spawn_min_minutes") val bootSpawnMinMinutes: Int = 20,
    @JsonProperty("boot_spawn_max_minutes") val bootSpawnMaxMinutes: Int = 50,
    @JsonProperty("star_teleport_enabled") val starTeleportEnabled: Boolean = true,
) {
    fun spawnIntervalCycles(): Int = spawnIntervalMinutes * CYCLES_PER_MINUTE

    fun spawnVariationCycles(): Int = spawnVariationMinutes * CYCLES_PER_MINUTE

    fun rollBootSpawnDelayCycles(random: Random = Random.Default): Int {
        val min = bootSpawnMinMinutes.coerceAtLeast(0)
        val max = bootSpawnMaxMinutes.coerceAtLeast(min)
        return random.nextInt(min, max + 1) * CYCLES_PER_MINUTE
    }

    companion object {
        private const val CYCLES_PER_MINUTE = 100
        private const val RESOURCE = "shootingstars.toml"

        private val mapper: ObjectMapper =
            ObjectMapper(TomlFactory()).registerKotlinModule()

        @Volatile private var cached: ShootingstarsSettings? = null

        fun load(): ShootingstarsSettings {
            cached?.let {
                return it
            }
            val stream =
                ShootingstarsSettings::class.java.classLoader.getResourceAsStream(RESOURCE)
                    ?: error("Missing plugin config resource: $RESOURCE")
            return stream.use { input ->
                mapper.readValue<ShootingstarsSettings>(input).also { cached = it }
            }
        }
    }
}
