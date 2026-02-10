package dev.openrune.types

import dev.openrune.definition.Definition

data class HealthBarServerType(
    override var id: Int = -1,
    var width: Int = 30,
) : Definition {

    fun barWidth(curHealth : Int, maxHealth : Int): Int {
        return ((curHealth.toDouble() / maxHealth) * width).toInt()
    }
}
