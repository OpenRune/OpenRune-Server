package org.rsmod.api.player.skilling

import org.rsmod.api.attr.AttributeKey
import org.rsmod.game.entity.Player

public object SkillingProductCounts {
    public val PRODUCTS: AttributeKey<MutableMap<String, Int>> =
        AttributeKey(persistenceKey = "skilling_product_counts")

    public val BY_SKILL: AttributeKey<MutableMap<String, Int>> =
        AttributeKey(persistenceKey = "skilling_skill_product_counts")
}

public fun Player.skillingProductCount(item: String): Int =
    attr[SkillingProductCounts.PRODUCTS]?.get(item) ?: 0

public fun Player.skillingSkillProductCount(skill: String): Int =
    attr[SkillingProductCounts.BY_SKILL]?.get(skill) ?: 0

public fun Player.recordSkillingProduct(skill: String, item: String, count: Int) {
    if (count <= 0) {
        return
    }
    incrementSkillingCount(SkillingProductCounts.PRODUCTS, item, count)
    incrementSkillingCount(SkillingProductCounts.BY_SKILL, skill, count)
}

private fun Player.incrementSkillingCount(
    key: AttributeKey<MutableMap<String, Int>>,
    entry: String,
    amount: Int,
) {
    val map = attr.getOrPut(key) { mutableMapOf() }
    map[entry] = (map[entry] ?: 0) + amount
    attr[key] = map
}
