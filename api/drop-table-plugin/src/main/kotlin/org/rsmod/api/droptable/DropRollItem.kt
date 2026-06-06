package org.rsmod.api.droptable

import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

public data class DropRollItem(
    public val obj: String,
    public val count: IntRange,
    public val condition: (Player) -> Boolean = { true },
    public val killCondition: ((Player, Npc, AreaChecker) -> Boolean)? = null,
    public val transformObj: (Player) -> String? = { null },
    public val bonusDrops: List<DropRollItem> = emptyList(),
    public val isNothing: Boolean = false,
) {
    public constructor(
        obj: String,
        count: Int,
        condition: (Player) -> Boolean = { true },
        killCondition: ((Player, Npc, AreaChecker) -> Boolean)? = null,
        transformObj: (Player) -> String? = { null },
        bonusDrops: List<DropRollItem> = emptyList(),
        isNothing: Boolean = false,
    ) : this(
        obj = obj,
        count = count..count,
        condition = condition,
        killCondition = killCondition,
        transformObj = transformObj,
        bonusDrops = bonusDrops,
        isNothing = isNothing,
    )

    public constructor(
        obj: String,
        count: Int,
        countMax: Int?,
        condition: (Player) -> Boolean = { true },
        killCondition: ((Player, Npc, AreaChecker) -> Boolean)? = null,
        transformObj: (Player) -> String? = { null },
        bonusDrops: List<DropRollItem> = emptyList(),
        isNothing: Boolean = false,
    ) : this(
        obj = obj,
        count = if (countMax != null && countMax > count) count..countMax else count..count,
        condition = condition,
        killCondition = killCondition,
        transformObj = transformObj,
        bonusDrops = bonusDrops,
        isNothing = isNothing,
    )
}

public fun DropRollItem.rollCount(random: GameRandom): Int {
    if (count.first == count.last) {
        return count.first
    }
    return random.of(count.first, count.last)
}

public fun nothingDrop(
    includeWhen: (Player) -> Boolean = { player -> !player.wearingRingOfWealth() },
): DropRollItem =
    DropRollItem(
        obj = "",
        count = 1,
        isNothing = true,
        condition = includeWhen,
    )
