package org.rsmod.api.server.config

import com.fasterxml.jackson.annotation.JsonProperty

public data class ServerConfig(
    val name: String,
    @JsonProperty("game-port") val gamePort: Int,
    val revision: Int,
    val environment: String,
    val realm: String,
    val world: Int
)
