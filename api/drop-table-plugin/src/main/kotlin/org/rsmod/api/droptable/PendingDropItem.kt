package org.rsmod.api.droptable

import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

public fun DropRollItem.requiresRollableWrapper(): Boolean =
    killCondition != null || bonusDrops.isNotEmpty()

public class PendingDropItemConfig internal constructor(
    public val obj: String,
    public var count: IntRange,
) {
    public var countChoices: List<Int>? = null
    public var condition: (Player) -> Boolean = { true }
    public var transformObj: (Player) -> String? = { null }
    public var killCondition: ((Player, Npc, AreaChecker) -> Boolean)? = null

    public fun toItem(): DropRollItem =
        DropRollItem(
            obj = obj,
            count = count,
            countChoices = countChoices,
            condition = condition,
            killCondition = killCondition,
            transformObj = transformObj,
        )
}

public abstract class PendingDropItemHandle {
    protected abstract val config: PendingDropItemConfig
    private var committed = false

    public infix fun condition(predicate: (Player) -> Boolean) {
        config.condition = predicate
        commit()
    }

    public infix fun transformObj(transform: (Player) -> String?) {
        config.transformObj = transform
        commit()
    }

    public infix fun killCondition(predicate: (Player, Npc, AreaChecker) -> Boolean) {
        config.killCondition = predicate
        commit()
    }

    internal fun commitIfNeeded() {
        if (!committed) {
            commit()
        }
    }

    protected abstract fun onCommit(item: DropRollItem)

    internal fun commit() {
        if (committed) {
            return
        }
        committed = true
        onCommit(config.toItem())
    }
}
