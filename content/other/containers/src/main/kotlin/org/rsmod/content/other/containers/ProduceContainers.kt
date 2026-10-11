package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM

/** Native filled forms carry their own quantity, so separate baskets never share account storage. */
internal data class ProduceContainer(val symbol: String, val empty: String, val item: String?, val quantity: Int, val capacity: Int, val prefix: String?) {
    val id = symbol.asRSCM()
    fun form(quantity: Int) = if (quantity == 0) empty else "$prefix$quantity"
}
internal object ProduceContainers {
    val all = buildList {
        add(ProduceContainer("obj.basket_empty", "obj.basket_empty", null, 0, 5, null))
        add(ProduceContainer("obj.sack_empty", "obj.sack_empty", null, 0, 10, null))
        for ((suffix, item) in listOf("apple" to "obj.cooking_apple", "orange" to "obj.orange", "strawberry" to "obj.strawberry", "banana" to "obj.banana", "tomato" to "obj.tomato"))
            for (n in 1..5) add(ProduceContainer("obj.basket_${suffix}_$n", "obj.basket_empty", item, n, 5, "obj.basket_${suffix}_"))
        for ((suffix, item) in listOf("cabbage" to "obj.cabbage", "onion" to "obj.onion", "potato" to "obj.potato"))
            for (n in 1..10) add(ProduceContainer("obj.sack_${suffix}_$n", "obj.sack_empty", item, n, 10, "obj.sack_${suffix}_"))
    }
    val byId = all.associateBy { it.id }
    fun filled(empty: String, item: Int) = all.firstOrNull { it.empty == empty && it.item?.asRSCM() == item }
    fun firstFill(empty: String) = all.filter { it.empty == empty && it.quantity == 1 }
}
