package org.alter.game.logs

import dev.openrune.central.logging.Loggable
import dev.openrune.central.logging.PlayerRights
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PlayerLogin")
data class PlayerLogin(override var player: String = "", ) : Loggable()

