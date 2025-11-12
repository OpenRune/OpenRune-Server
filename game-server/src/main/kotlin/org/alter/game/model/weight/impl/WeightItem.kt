package org.alter.game.model.weight.impl

import org.alter.game.model.item.Item
import org.alter.game.model.weight.WeightNode
import org.alter.rscm.RSCM.asRSCM
import java.util.*

/**
 * @author Tom <rspsmods@gmail.com>
 */
open class WeightItem(
    val item: Int,
    private val amount: Int = 1,
    private val maxAmount: Int = amount,
    weight: Int,
) : WeightNode<Item>(weight) {
    constructor(item: Int, amount: IntRange, weight: Int) : this(item, amount.start, amount.last, weight)

    constructor(item: String, weight: Int) : this(item.asRSCM(), 1, 1, weight)
    constructor(item: String, amount: Int, weight: Int) : this(item.asRSCM(), amount, amount, weight)
    constructor(item: String, amount: IntRange, weight: Int) : this(item.asRSCM(), amount.start, amount.last, weight)
    constructor(item: String, amount: Int, maxAmount: Int, weight: Int) : this(item.asRSCM(), amount, maxAmount, weight)

    constructor(item: Number, weight: Number) : this(item.toInt(), 1, 1, weight.toInt())
    constructor(item: Number, amount: Number, weight: Number) : this(item.toInt(), amount.toInt(), amount.toInt(), weight.toInt())
    constructor(item: Number, amount: IntRange, weight: Number) : this(item.toInt(), amount.start, amount.last, weight.toInt())
    constructor(item: Number, amount: Number, maxAmount: Number, weight: Number) : this(
        item.toInt(),
        amount.toInt(),
        maxAmount.toInt(),
        weight.toInt()
    )

    override fun convert(random: Random): Item =
        Item(item, amount + random.nextInt((maxAmount - amount) + 1))
}
