package org.rsmod.api.totp.laravel

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.logging.InlineLogger

public object LaravelSettingsPaths {
    public const val RESOURCE_NAME: String = "laravel-settings.yml"
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class LaravelSettingsYaml(
    @JsonProperty("app-key") val appKey: String = "",
)

public class LaravelSettingsLoader {
    private val yamlMapper: ObjectMapper =
        ObjectMapper(YAMLFactory())
            .registerKotlinModule()

    private val logger = InlineLogger()

    public fun loadAppKey(): String? {
        val stream =
            LaravelSettingsLoader::class.java.classLoader.getResourceAsStream(LaravelSettingsPaths.RESOURCE_NAME)
                ?: return null
        val settings =
            stream.use { input ->
                yamlMapper.readValue(input, LaravelSettingsYaml::class.java)
            }
        val appKey = settings.appKey.trim()
        if (appKey.isEmpty()) {
            return null
        }
        logger.info {
            "Loaded Laravel APP_KEY from classpath resource ${LaravelSettingsPaths.RESOURCE_NAME}"
        }
        return appKey
    }
}
