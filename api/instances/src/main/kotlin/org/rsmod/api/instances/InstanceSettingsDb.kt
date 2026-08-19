package org.rsmod.api.instances

import org.rsmod.api.table.InstanceSettingsRow
import org.rsmod.map.CoordGrid

internal fun InstanceSettingsRow.toInstanceSettings(): InstanceSettings =
    InstanceSettings(
        fee = fee,
        maxPlayers = maxPlayers,
        reclaimTicks = (graceMinutes.takeIf { it > 0 } ?: 10) * INSTANCE_TICKS_PER_MINUTE,
        graceTicks = graceMinutes * INSTANCE_TICKS_PER_MINUTE,
        timeLimitTicks = timeLimitMinutes.takeIf { it > 0 }?.times(INSTANCE_TICKS_PER_MINUTE),
        bossNpc = bossNpc,
        bossName = bossName,
        recommendedCombat = recommendedCombat.toCombatRange(),
        teamSize = teamSize,
        lootMultiplier = lootMultiplier,
        description = description,
    )

internal fun InstanceArea.withDbCoords(row: InstanceSettingsRow): InstanceArea {
    val enter =
        row.resolveEnterCoord(
            when (this) {
                is InstanceArea.Template -> enterCoord
                is InstanceArea.CopyRegions -> enterCoord
            }
        )

    val exit =
        row.resolveExitCoord(
            enter,
            when (this) {
                is InstanceArea.Template -> exitCoord
                is InstanceArea.CopyRegions -> exitCoord
            },
        )

    return when (this) {
        is InstanceArea.Template -> copy(enterCoord = enter, exitCoord = exit)
        is InstanceArea.CopyRegions -> copy(enterCoord = enter, exitCoord = exit)
    }
}

private fun List<Int>.toCombatRange(): IntRange {
    require(size == 2) { "recommendedCombat must contain exactly 2 values, found $size" }
    return first()..last()
}

private fun InstanceSettingsRow.resolveEnterCoord(fallback: RegionLocal): RegionLocal =
    if (enterCoord.isUnsetInstanceCoord()) {
        fallback
    } else {
        enterCoord.toRegionLocalEnter()
    }

private fun InstanceSettingsRow.resolveExitCoord(
    enter: RegionLocal,
    fallback: CoordGrid,
): CoordGrid =
    when {
        !exitCoord.isUnsetInstanceCoord() -> exitCoord
        !enterCoord.isUnsetInstanceCoord() -> enterCoord
        else -> fallback
    }

private fun CoordGrid.isUnsetInstanceCoord(): Boolean =
    this == CoordGrid.ZERO || this == CoordGrid.NULL || packed == 0

private fun CoordGrid.toRegionLocalEnter(): RegionLocal = RegionLocal(level, mx, mz, lx, lz)
