package org.rsmod.game.hit

/** Shared by hit copies so impact modifiers keep callbacks and each hit completes only once. */
public class HitImpactEffects {
    private var actions: MutableList<(Int) -> Unit>? = null
    private var completed: Boolean = false
    private var beforeImpact: MutableList<(Int) -> Int>? = null
    private var preparedDamage: Int? = null

    /** Resolve target-dependent damage at arrival, shared once across hit copies. */
    public fun beforeImpact(action: (damage: Int) -> Int) {
        check(preparedDamage == null && !completed)
        (beforeImpact ?: mutableListOf<(Int) -> Int>().also { beforeImpact = it }).add(action)
    }

    public fun prepare(damage: Int): Int {
        preparedDamage?.let { return it }
        var result = damage
        beforeImpact?.forEach { result = it(result).coerceAtLeast(0) }
        beforeImpact = null
        preparedDamage = result
        return result
    }

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
