package org.alter.api

import org.alter.game.model.attr.AttributeKey

object CombatAttributes {
    val CASTING_SPELL = AttributeKey<Any>()
    val DAMAGE_DEAL_MULTIPLIER = AttributeKey<Double>()
    val DAMAGE_TAKE_MULTIPLIER = AttributeKey<Double>()
}
