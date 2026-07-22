package org.rsmod.content.other.toolbelt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

@JsonIgnoreProperties(ignoreUnknown = true)
data class ToolbeltSettings(
    @JsonProperty("is_enabled") val isEnabled: Boolean = true,
    @JsonProperty("allow_higher_tier_unlock") val allowHigherTierUnlock: Boolean = true,
) {
    companion object {
        private const val RESOURCE = "toolbelt.toml"

        private val mapper: ObjectMapper = ObjectMapper(TomlFactory()).registerKotlinModule()

        @Volatile private var cached: ToolbeltSettings? = null

        fun load(): ToolbeltSettings {
            cached?.let {
                return it
            }
            val stream =
                ToolbeltSettings::class.java.classLoader.getResourceAsStream(RESOURCE)
                    ?: error("Missing plugin config resource: $RESOURCE")
            return stream.use { input ->
                mapper.readValue<ToolbeltSettings>(input).also { cached = it }
            }
        }
    }
}
