package org.rsmod.content.quest.area.varrock.daddyshome

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.menu
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DaddysHomeSawmill @Inject constructor(private val daddysHome: DaddysHomeQuest) :
    PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1(Lumberyard) { startDialogue(it.npc) { sawmillDialogue(lumberyard = true) } }
        for (operator in OtherOperators) {
            onOpNpc1(operator) { startDialogue(it.npc) { sawmillDialogue(lumberyard = false) } }
        }
    }

    private suspend fun Dialogue.sawmillDialogue(lumberyard: Boolean) {
        val stage = daddysHome.stage(player)
        val waxwood = stage in DaddysHomeQuest.Building until DaddysHomeQuest.Complete
        val greeting =
            "Do you want me to make some planks for you? I can make planks from wood, oak, teak " +
                "and mahogany logs. Or would you like to buy some other housing supplies?"
        chatNpc(neutral, if (lumberyard && waxwood) greeting else "Hello there. $greeting")
        val options = buildList {
            add("Yes, please make me some planks." to Option.Planks)
            add("Can I buy some housing supplies?" to Option.Supplies)
            if (waxwood) add("I need some waxwood planks for Old Man Yarlo." to Option.Waxwood)
            add("I'm good, thanks." to Option.Leave)
        }
        when (menu(options)) {
            Option.Planks -> {
                chatPlayer(happy, "Yes, please make me some planks.")
                chatNpc(neutral, "Use the Buy-plank option on me and I'll see what I can do.")
            }
            Option.Supplies -> {
                chatPlayer(quiz, "Can I buy some housing supplies?")
                chatNpc(happy, "Of course! Use the Trade option on me.")
            }
            Option.Waxwood -> waxwoodPlanks(lumberyard)
            Option.Leave -> {
                chatPlayer(neutral, "I'm good, thanks.")
                chatNpc(
                    happy,
                    "Well come back when you want some. You'll struggle to find quality planks " +
                        "anywhere but here!",
                )
            }
        }
    }

    private suspend fun Dialogue.waxwoodPlanks(lumberyard: Boolean) {
        chatPlayer(neutral, "I need some waxwood planks for Old Man Yarlo.")
        if (!lumberyard) {
            chatNpc(
                neutral,
                "I don't know any Old Man Yarlo, and I'd rather not work with that horrible " +
                    "material.",
            )
            chatPlayer(
                quiz,
                "But he said he knew the chap at the sawmill, and you'd make waxwood planks for " +
                    "him.",
            )
            chatNpc(neutral, "Perhaps he was thinking of some other sawmill operator.")
            return
        }
        chatNpc(
            neutral,
            "That old geezer? Oh, alright. I won't charge for this, since it's for him.",
        )
        val logs = access.inv.count(DaddysHomeQuest.WaxwoodLogs)
        if (logs == 0) {
            chatNpc(neutral, "I'll need some waxwood logs to work with first.")
            return
        }
        val converted =
            player.invTransaction(access.inv) {
                val from = select(access.inv)
                delete {
                    this.from = from
                    this.obj = DaddysHomeQuest.WaxwoodLogs.asRSCM(RSCMType.OBJ)
                    this.strictCount = logs
                }
                insert {
                    this.into = from
                    this.obj = DaddysHomeQuest.WaxwoodPlank.asRSCM(RSCMType.OBJ)
                    this.strictCount = logs
                }
            }
        if (converted.failure) return
        objbox(
            DaddysHomeQuest.WaxwoodPlank,
            "The sawmill operator turns your waxwood logs into planks.",
        )
    }

    private enum class Option {
        Planks,
        Supplies,
        Waxwood,
        Leave,
    }

    private companion object {
        const val Lumberyard = "npc.poh_sawmill_opp"
        val OtherOperators = listOf("npc.prif_sawmill_operator", "npc.auburn_sawmill_operator")
    }
}
