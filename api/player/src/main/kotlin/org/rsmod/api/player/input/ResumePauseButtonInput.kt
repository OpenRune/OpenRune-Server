package org.rsmod.api.player.input

public data class ResumePauseButtonInput(
    public val component: String,
    public val subcomponent: Int,
) {
    public fun isComponentType(type: String): Boolean = type == component

    public fun isSubcomponent(comsub: Int): Boolean = subcomponent == comsub
}
