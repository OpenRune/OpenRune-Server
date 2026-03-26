package org.alter.api

import org.alter.game.model.attr.AttributeKey

object CombatAttributes {
    val CASTING_SPELL = AttributeKey<Any>()
    val DAMAGE_DEAL_MULTIPLIER = AttributeKey<Double>()
    val DAMAGE_TAKE_MULTIPLIER = AttributeKey<Double>()
    /** Set to true during the current attack if it's a special attack. */
    val SPECIAL_ATTACK_ACTIVE = AttributeKey<Boolean>()
}
