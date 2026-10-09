package org.rsmod.content.quest.area.varrock.daddyshome

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.table.SawmillOperatorsRow
import org.rsmod.content.other.sawmill.SawmillHooks
import org.rsmod.content.other.sawmill.SawmillOperators
import org.rsmod.content.other.sawmill.SawmillTalkHook
import org.rsmod.content.other.sawmill.isSameOperator
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DaddysHomeSawmill
@Inject
constructor(private val daddysHome: DaddysHomeQuest, private val hooks: SawmillHooks) :
    PluginScript(), SawmillTalkHook {

    override fun ScriptContext.startup() {
        hooks.register(this@DaddysHomeSawmill)
    }

    override fun ScriptContext.shutdown() {
        hooks.unregister(this@DaddysHomeSawmill)
    }

    override fun option(player: Player, operator: SawmillOperatorsRow): String? {
        val stage = daddysHome.stage(player)
        return if (stage in DaddysHomeQuest.Building until DaddysHomeQuest.Complete) {
            "I need some waxwood planks for Old Man Yarlo."
        } else {
            null
        }
    }

    override suspend fun choose(dialogue: Dialogue, operator: SawmillOperatorsRow) {
        dialogue.waxwoodPlanks(lumberyard = operator.isSameOperator(SawmillOperators.lumberYard))
    }

    private suspend fun Dialogue.waxwoodPlanks(lumberyard: Boolean) {
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
}
