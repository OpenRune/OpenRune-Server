package org.rsmod.content.minigames.gauntlet

import jakarta.inject.Inject
import kotlin.random.Random
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GauntletGathering
@Inject
constructor(
    private val runs: GauntletRuns,
    private val locRepo: LocRepository,
    private val clock: MapClock,
) : PluginScript() {
    private enum class Node(
        val loc: String,
        val stat: String,
        val xp: Double,
        val tool: String?,
        val seq: String,
        val product: String,
        val start: String,
        val yield: String?,
        val last: String,
        val full: String,
    ) {
        LINUM(
            "fibre",
            "stat.farming",
            1.0,
            null,
            "seq.picking_low",
            "fibre",
            "You pick some fibre from the plant.",
            null,
            "You pick some fibre from the plant.",
            "You can't carry any more fibre.",
        ),
        GRYM(
            "herb",
            "stat.farming",
            1.0,
            null,
            "seq.picking_low",
            "herb",
            "You pick a herb from the roots.",
            null,
            "You pick a herb from the roots.",
            "You can't carry any more herbs.",
        ),
        FISHING(
            "pond",
            "stat.fishing",
            10.0,
            "harpoon",
            "seq.human_harpoon_crystal",
            "raw_food",
            "You start harpooning fish.",
            "You manage to catch a fish.",
            "You catch the last of the fish.",
            "You can't carry any more fish.",
        ),
    }

    override fun ScriptContext.startup() {
        for (node in Node.entries) {
            for (suffix in listOf("", "_hm")) {
                onOpLoc1("loc.gauntlet_${node.loc}$suffix") { gather(node, it.loc) }
            }
        }
        onPlayerQueueWithArgs<GatherTask>(GATHER_QUEUE) { collect(it.args) }
        onEvent<SkillingActionCompleteEvent> {
            val product = context as? SkillingActionContext.Product ?: return@onEvent
            if (product.isBonus || product.item !in SKILLED_PRODUCTS) return@onEvent
            rollShards(player, player.gauntletCorrupted)
        }
    }

    private suspend fun ProtectedAccess.gather(node: Node, loc: BoundLocInfo) {
        val run = runs.runFor(player) ?: return
        val corrupted = run.mode.corrupted
        if (node.tool != null) {
            val tool = gauntletObj(node.tool, corrupted)
            if (tool !in inv && tool !in player.worn) {
                mesbox("You need a ${node.tool} to do that.")
                return
            }
        }
        spam(node.start)
        anim(animFor(node, corrupted))
        val wait = ((run.nextGather[player] ?: 0) - clock.cycle).coerceAtLeast(1)
        clearWeakQueue(GATHER_QUEUE)
        weakQueue(GATHER_QUEUE, wait, GatherTask(node, loc))
    }

    private suspend fun ProtectedAccess.collect(task: GatherTask) {
        val (node, loc) = task
        val run = runs.runFor(player) ?: return
        val corrupted = run.mode.corrupted
        val remaining = run.charges[loc.coords] ?: return
        if (!invAdd(inv, gauntletObj(node.product, corrupted), 1).success) {
            mesbox(node.full)
            return
        }
        statAdvance(node.stat, node.xp)
        rollShards(player, corrupted)
        run.charges[loc.coords] = remaining - 1
        run.nextGather[player] = clock + CYCLE
        if (remaining - 1 <= 0) {
            spam(node.last)
            resetAnim()
            deplete(node, loc, corrupted)
            return
        }
        node.yield?.let(::spam)
        anim(animFor(node, corrupted))
        weakQueue(GATHER_QUEUE, CYCLE, task)
    }

    private fun animFor(node: Node, corrupted: Boolean) =
        if (node == Node.FISHING && corrupted) HARPOON_CORRUPTED else node.seq

    private data class GatherTask(val node: Node, val loc: BoundLocInfo)

    private fun rollShards(player: Player, corrupted: Boolean) {
        if (Random.nextInt(SHARD_ODDS) != 0) return
        val amount = Random.nextInt(SHARD_MIN, SHARD_MAX + 1)
        if (!player.invAdd(player.inv, gauntletObj("crystal_shard", corrupted), amount).success) {
            return
        }
        player.mes("You find $amount ${if (corrupted) "corrupted" else "crystal"} shards.")
    }

    private fun deplete(node: Node, loc: BoundLocInfo, corrupted: Boolean) {
        val suffix = if (corrupted) "_hm" else ""
        locRepo.change(loc, "loc.gauntlet_${node.loc}_depleted$suffix", Int.MAX_VALUE)
    }

    private companion object {
        val SKILLED_PRODUCTS =
            setOf("obj.gauntlet_ore", "obj.gauntlet_ore_hm", "obj.gauntlet_bark", "obj.gauntlet_bark_hm")
        const val GATHER_QUEUE = "queue.generic_queue9"
        const val CYCLE = 3
        const val SHARD_ODDS = 3
        const val SHARD_MIN = 10
        const val SHARD_MAX = 30
        const val HARPOON_CORRUPTED = "seq.human_harpoon_gauntlet_hm"
    }
}
