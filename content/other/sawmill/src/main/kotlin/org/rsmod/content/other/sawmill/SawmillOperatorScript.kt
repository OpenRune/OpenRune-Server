package org.rsmod.content.other.sawmill

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onApNpc3
import org.rsmod.api.script.onApNpc4
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.shops.Shops
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.SkillingActionType
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SawmillOperatorScript
@Inject
constructor(private val shops: Shops, private val hooks: SawmillHooks) : PluginScript() {

    override fun ScriptContext.startup() {
        for (operator in SawmillOperator.entries) {
            onOpNpc1(operator.npc) { talkTo(it.npc, operator) }
            onOpNpc3(operator.npc) { buyPlanks(it.npc) }
            onOpNpc4(operator.npc) { openSupplies(player) }
            onApNpc1(operator.npc) { approach(it.npc) { talkTo(it.npc, operator) } }
            onApNpc3(operator.npc) { approach(it.npc) { buyPlanks(it.npc) } }
            onApNpc4(operator.npc) { approach(it.npc) { openSupplies(player) } }
        }
    }

    private suspend fun ProtectedAccess.approach(npc: Npc, action: suspend () -> Unit) {
        if (isWithinApRange(npc, APPROACH_RANGE)) action()
    }

    private suspend fun ProtectedAccess.talkTo(npc: Npc, operator: SawmillOperator) =
        startDialogue(npc) { talk(operator) }

    private suspend fun ProtectedAccess.buyPlanks(npc: Npc) = startDialogue(npc) { makePlanks() }

    private suspend fun Dialogue.talk(operator: SawmillOperator) {
        chatNpc(neutral, operator.greeting)
        val options = buildList {
            add("Yes, please make me some planks." to Choice.Planks)
            if (operator.explainsPlanks) {
                add("What kind of planks can you make?" to Choice.Kinds)
            }
            add("Can I buy some housing supplies?" to Choice.Supplies)
            for ((label, hook) in hooks.offered(player, operator)) {
                add(label to Choice.Extension(hook))
            }
            add(operator.declineOption to Choice.Decline)
        }
        val choice = pick(options)
        val label = options.first { it.second == choice }.first
        chatPlayer(if (choice == Choice.Supplies) quiz else neutral, label)
        when (choice) {
            Choice.Planks -> makePlanks()
            Choice.Kinds -> {
                chatNpc(
                    neutral,
                    "I can make planks from wood, oak, teak and mahogany. I don't make planks " +
                        "from other woods as they're no good for making furniture.",
                )
                chatNpc(
                    neutral,
                    "Wood and oak are all over the place, but teak and mahogany can only be " +
                        "found in a few places like Karamja and Etceteria.",
                )
            }
            Choice.Supplies -> {
                if (operator.acknowledgesSupplies) chatNpc(happy, "Of course!")
                openSupplies(player)
            }
            is Choice.Extension -> choice.hook.choose(this, operator)
            Choice.Decline -> chatNpc(happy, operator.farewell)
        }
    }

    private suspend fun Dialogue.pick(options: List<Pair<String, Choice>>): Choice {
        val o = options
        return when (o.size) {
            3 -> choice3(o[0].first, o[0].second, o[1].first, o[1].second, o[2].first, o[2].second)
            4 ->
                choice4(
                    o[0].first,
                    o[0].second,
                    o[1].first,
                    o[1].second,
                    o[2].first,
                    o[2].second,
                    o[3].first,
                    o[3].second,
                )
            else ->
                choice5(
                    o[0].first,
                    o[0].second,
                    o[1].first,
                    o[1].second,
                    o[2].first,
                    o[2].second,
                    o[3].first,
                    o[3].second,
                    o[4].first,
                    o[4].second,
                )
        }
    }

    private suspend fun Dialogue.makePlanks() {
        val config =
            SkillMultiConfig(
                actionType = SkillingActionType.BUY,
                verb = "buy",
                entries = Plank.entries.map { SkillMultiEntry(it.plank) },
                maxCountProvider = { inv, entry ->
                    maxOf(1, inv.count(Plank.forPlank(entry.internal).logs))
                },
            )
        access.openSkillMulti(config) { selection ->
            buy(Plank.forPlank(selection.entry.internal), selection.amount)
        }
    }

    private suspend fun Dialogue.buy(plank: Plank, requested: Int) {
        val held = access.inv.count(plank.logs)
        if (held == 0) {
            chatNpc(neutral, "You'll need to bring me some more logs.")
            return
        }
        val amount = minOf(requested, held)
        val cost = amount * plank.price
        if (access.inv.count(Coins) < cost) {
            chatNpc(
                neutral,
                "Those planks cost ${"%,d".format(cost)} coins. You don't have enough money for " +
                    "all of them.",
            )
            return
        }
        player.invTransaction(access.inv) {
            val from = select(access.inv)
            delete {
                this.from = from
                this.obj = Coins.asRSCM(RSCMType.OBJ)
                this.strictCount = cost
            }
            delete {
                this.from = from
                this.obj = plank.logs.asRSCM(RSCMType.OBJ)
                this.strictCount = amount
            }
            insert {
                this.into = from
                this.obj = plank.plank.asRSCM(RSCMType.OBJ)
                this.strictCount = amount
            }
        }
    }

    private fun openSupplies(player: Player) {
        shops.open(
            player = player,
            title = "Construction supplies",
            shopInv = SuppliesInv,
            buyPercentage = 50.0,
            sellPercentage = 130.0,
            changePercentage = 0.0,
        )
    }

    private sealed interface Choice {
        data object Planks : Choice

        data object Kinds : Choice

        data object Supplies : Choice

        data object Decline : Choice

        data class Extension(val hook: SawmillTalkHook) : Choice
    }

    private companion object {
        const val APPROACH_RANGE = 2
        const val Coins = "obj.coins"
        const val SuppliesInv = "inv.poh_sawmill_shop"
    }
}
