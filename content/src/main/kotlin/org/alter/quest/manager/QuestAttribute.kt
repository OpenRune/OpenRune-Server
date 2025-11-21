package org.alter.quest.manager

import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player

class QuestAttribute<T> internal constructor(
    val name: String,
    val attributeKey: AttributeKey<T>,
    private val defaultProvider: () -> T
) {
    fun default(): T = defaultProvider()

    fun get(player: Player) : T  {
        return player.attr.getOrPut(attributeKey) { defaultProvider() }
    }

    fun getOrNull(player: Player): T? = player.attr[attributeKey]

    fun set(player: Player, value: T) {
        player.attr[attributeKey] = value
    }

    fun reset(player: Player): T {
        val value = defaultProvider()
        player.attr[attributeKey] = value
        return value
    }

    fun clear(player: Player) {
        player.attr.remove(attributeKey)
    }

    fun exists(player: Player): Boolean = player.attr.has(attributeKey)
}