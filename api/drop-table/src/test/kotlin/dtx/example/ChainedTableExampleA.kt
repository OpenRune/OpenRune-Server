package dtx.example

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.singleRollable
import dtx.impl.chain.ChainEnd
import dtx.impl.chain.ChainRollableImpl
import dtx.impl.chain.ChainRollableHooksBuilder
import dtx.impl.chain.ChainedTableImpl
import dtx.impl.chain.ChainedTableHooksBuilder
import kotlin.random.Random

// Example A: Loot chest with luck, pity, and anti-duplication
// Demonstrates usage of ALL hooks across ChainedTableHooks, ChainRollableHooks, and RollableHooks

data class PlayerContext(
    val luck: Int,
    var pityRare: Int,
    val owned: Set<String>,
    val dailyGuaranteed: Boolean
)

data class Drop(val itemId: String, val qty: Int)

// Build Table-level hooks (includes TableHooks + RollableHooks + ChainedTableHooks)
private val tableHooks = ChainedTableHooksBuilder<PlayerContext, Drop>()
    .apply {
        // TableHooks
        baseRollFor { target ->
            // not used by ChainedTable selection itself, but shown for completeness
            println("[table:baseRollFor] target=$target -> 0.0")
            0.0
        }
        modifyRollFor { target, base ->
            println("[table:modifyRoll] target=$target base=$base -> $base")
            base
        }
        // RollableHooks (as Table extends RollableHooks)
        shouldInclude { player, args ->
            println("[table:includeInRoll] -> true")
            true
        }
        vetoRoll { player, args ->
            println("[table:vetoRoll] -> false")
            false
        }
        onVeto { player ->
            println("[table:onRollVetoed]")
            RollResult.Nothing()
        }
        transform { player, res ->
            println("[table:transformResult] $res -> $res")
            res
        }
        onRollCompleted { player, args, res ->
            println("[table:onRollCompleted] result=$res")
        }
        // ChainedTableHooks
        onChainStart { player ->
            println("[chain:start] LootChest for $player")
        }
        onEachLink { player, link, rolled, threshold, passed ->
            println("[chain:link] base=${link.base} outOf=${link.rollChance} rolled=$rolled<th=$threshold -> passed=$passed")
        }
        onChainEnd { player, res ->
            println("[chain:end] result=$res")
        }
    }
    .build()

// Build link rollables
private val rareRollable: Rollable<PlayerContext, Drop> = singleRollable {
    result(Drop("rare_skin", 1))
}
private val uncommonRollable: Rollable<PlayerContext, Drop> = singleRollable {
    result(Drop("uncommon_pack", 1))
}
private val commonRollable: Rollable<PlayerContext, Drop> = singleRollable {
    result(Drop("common_pack", 1))
}

// Build ChainRollable hooks for each link
private val rareHooks = ChainRollableHooksBuilder<PlayerContext, Drop>().apply {
    // RollableHooks
    shouldInclude { player, args ->
        val include = "rare_skin" !in player.owned
        println("[rare:includeInRoll] -> $include")
        include
    }
    vetoRoll { player, args ->
        val veto = player.dailyGuaranteed && player.pityRare > 10
        println("[rare:vetoRoll] -> $veto")
        veto
    }
    onVeto { player ->
        println("[rare:onRollVetoed] granting rare immediately")
        RollResult.Single(Drop("rare_skin", 1))
    }
    transform { player, result ->
        val transformed = when (result) {
            is RollResult.Single -> result.copy(result = result.result.copy(qty = result.result.qty + if (player.luck >= 20) 1 else 0))
            else -> result
        }
        println("[rare:transformResult] $result -> $transformed")
        transformed
    }
    onRollCompleted { player, args, result ->
        println("[rare:onRollCompleted] $result")
    }
    // ChainRollableHooks
    adjustChance { player, base, outOf ->
        val pityBoost = (player.pityRare / 5) // +1 base per 5 pity
        val luckBoost = (player.luck / 10)    // +1 base per 10 luck
        val adjusted = (base + pityBoost + luckBoost) to outOf
        println("[rare:adjustChance] ($base/$outOf) -> (${adjusted.first}/${adjusted.second})")
        adjusted
    }
    skipLink { player ->
        val skip = false
        println("[rare:skipLink] -> $skip")
        skip
    }
    nextOverride { player, next ->
        println("[rare:nextOverride] -> next")
        next
    }
    onLinkEvaluated { player, rolled, threshold, passed ->
        println("[rare:onLinkEvaluated] rolled=$rolled<th=$threshold -> passed=$passed")
    }
}.build()

private val uncommonHooks = ChainRollableHooksBuilder<PlayerContext, Drop>().apply {
        shouldInclude { player, args -> println("[uncommon:includeInRoll] -> true")
            true
        }
        vetoRoll { player, args ->
            println("[uncommon:vetoRoll] -> false")
            false
        }
        onVeto { player ->
            println("[uncommon:onRollVetoed]")
            RollResult.Nothing()
        }
        transform { player, result ->
            println("[uncommon:transform] $result -> $result")
            result
        }
        onRollCompleted { player, args, result ->
            println("[uncommon:onRollCompleted] $result")
        }
        adjustChance { player, base, outOf ->
            val luckBoost = (player.luck / 25)
            val adjusted = (base + luckBoost) to outOf
            println("[uncommon:adjustChance] ($base/$outOf) -> (${adjusted.first}/${adjusted.second})")
            adjusted
        }
        skipLink { player ->
            println("[uncommon:skipLink] -> false")
            false
        }
        nextOverride { player, next ->
            println("[uncommon:nextOverride] -> next")
            next
        }
        onLinkEvaluated { player, r, th, p ->
            println("[uncommon:onLinkEvaluated] passed=$p")
        }
}.build()

private val commonHooks = ChainRollableHooksBuilder<PlayerContext, Drop>().apply {
    shouldInclude { player, args ->
        println("[common:includeInRoll] -> true")
        true
    }
    vetoRoll { player, args ->
        println("[common:vetoRoll] -> false")
        false
    }
    onVeto { player -> println("[common:onRollVetoed]")
        RollResult.Nothing()
    }
    transform { player, result ->
        val transformed = when (result) {
            is RollResult.Single -> result.copy(result = result.result.copy(qty = if (player.luck >= 30) 2 else 1))
            else -> result
        }
        println("[common:transformResult] $result -> $transformed")
        transformed
    }
    onRollCompleted { player, args, res ->
        println("[common:onRollCompleted] $res")
    }
    adjustChance { player, base, outOf ->
        println("[common:adjustChance] ($base/$outOf) -> ($base/$outOf)")
        base to outOf
    }
    skipLink { player ->
        println("[common:skipLink] -> false")
        false
    }
    nextOverride { player, next ->
        println("[common:nextOverride] -> next")
        next
    }
    onLinkEvaluated { player, r, th, p ->
        println("[common:onLinkEvaluated] passed=$p")
    }
}.build()

// Assemble chain bottom-up
private val commonLink = ChainRollableImpl(
    base = 100,
    rollChance = 100,
    next = ChainEnd(),
    rollable = commonRollable,
    hooks = commonHooks
)
private val uncommonLink = ChainRollableImpl(
    base = 25,
    rollChance = 100,
    next = commonLink,
    rollable = uncommonRollable,
    hooks = uncommonHooks
)
private val rareLink = ChainRollableImpl(
    base = 5,
    rollChance = 100,
    next = uncommonLink,
    rollable = rareRollable,
    hooks = rareHooks
)

val lootChest = ChainedTableImpl(
    tableIdentifier = "LootChest",
    head = rareLink,
    hooks = tableHooks
)

// Small demonstration that prints to console
fun runChainedTableExampleA() {
    val ctx = PlayerContext(
        luck = Random.nextInt(1, 100),
        pityRare = 7,
        owned = setOf("starter_skin"),
        dailyGuaranteed = false
    )

    println("=== Running LootChest Example A ===")
    repeat(3) { i ->
        println("-- Roll #$i --")
        val result = lootChest.roll(ctx, ArgMap.Empty)
        println("[demo] roll -> $result\n")
    }
}

fun main() {
    runChainedTableExampleA()
}