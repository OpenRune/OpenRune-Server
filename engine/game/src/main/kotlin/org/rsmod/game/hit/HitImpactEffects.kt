package org.rsmod.game.hit

/** Shared by hit copies so impact modifiers keep callbacks and each hit completes only once. */
public class HitImpactEffects {
    private var actions: MutableList<(Int) -> Unit>? = null
    private var completed: Boolean = false

    public fun add(action: (actualDamage: Int) -> Unit) {
        check(!completed) { "Cannot add an effect after impact." }
        val list = actions ?: mutableListOf<(Int) -> Unit>().also { actions = it }
        list.add(action)
    }

    public fun complete(actualDamage: Int) {
        if (completed) return
        require(actualDamage >= 0)
        completed = true
        val pending = actions
        actions = null
        pending?.forEach { it(actualDamage) }
    }
}
