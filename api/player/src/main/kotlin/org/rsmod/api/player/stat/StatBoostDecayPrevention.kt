package org.rsmod.api.player.stat

import org.rsmod.api.attr.AttributeKey
import org.rsmod.game.entity.Player

/**
 * Prevents ordinary positive-stat decay without blocking explicit stat
 * changes such as brews, drains, restores, overload refreshes, or prayers.
 *
 * Multiple effects may protect the same stat at once. A stat resumes normal
 * decay only after every source protecting it has been removed.
 */
public object StatBoostDecayPrevention {
    private val sources =
        AttributeKey<
            MutableMap<String, MutableSet<String>>
            >(
            resetOnDeath = true,
            temp = true,
        )

    public fun add(
        player: Player,
        stat: String,
        source: String,
    ) {
        require(stat.isNotBlank()) {
            "`stat` must not be blank."
        }

        require(source.isNotBlank()) {
            "`source` must not be blank."
        }

        val byStat =
            player.attr.getOrPut(sources) {
                mutableMapOf()
            }

        byStat
            .getOrPut(stat) {
                mutableSetOf()
            }
            .add(source)
    }

    public fun add(
        player: Player,
        stats: Iterable<String>,
        source: String,
    ) {
        stats.forEach { stat ->
            add(
                player = player,
                stat = stat,
                source = source,
            )
        }
    }

    public fun remove(
        player: Player,
        stat: String,
        source: String,
    ) {
        val byStat =
            player.attr[sources]
                ?: return

        val statSources =
            byStat[stat]
                ?: return

        statSources.remove(source)

        if (statSources.isEmpty()) {
            byStat.remove(stat)
        }

        if (byStat.isEmpty()) {
            player.attr.remove(sources)
        }
    }

    public fun remove(
        player: Player,
        stats: Iterable<String>,
        source: String,
    ) {
        stats.forEach { stat ->
            remove(
                player = player,
                stat = stat,
                source = source,
            )
        }
    }

    public fun removeSource(
        player: Player,
        source: String,
    ) {
        val byStat =
            player.attr[sources]
                ?: return

        val iterator =
            byStat.iterator()

        while (iterator.hasNext()) {
            val entry =
                iterator.next()

            entry.value.remove(source)

            if (entry.value.isEmpty()) {
                iterator.remove()
            }
        }

        if (byStat.isEmpty()) {
            player.attr.remove(sources)
        }
    }

    public fun prevents(
        player: Player,
        stat: String,
    ): Boolean {
        return player
            .attr[sources]
            ?.get(stat)
            .orEmpty()
            .isNotEmpty()
    }
}

/**
 * Removes only a positive boost.
 *
 * A stat below its base level must remain drained.
 */
public fun Player.clearPositiveStatBoost(
    stat: String,
) {
    if (stat(stat) > statBase(stat)) {
        statRestore(stat)
    }
}

public fun Player.clearPositiveStatBoosts(
    stats: Iterable<String>,
) {
    stats.forEach { stat ->
        clearPositiveStatBoost(stat)
    }
}
