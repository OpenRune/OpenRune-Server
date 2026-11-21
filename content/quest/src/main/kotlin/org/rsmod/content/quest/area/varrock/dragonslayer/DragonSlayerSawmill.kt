package org.rsmod.content.quest.area.varrock.dragonslayer

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.table.SawmillOperatorsRow
import org.rsmod.content.other.sawmill.SawmillHooks
import org.rsmod.content.other.sawmill.SawmillOperators
import org.rsmod.content.other.sawmill.SawmillTalkHook
import org.rsmod.content.other.sawmill.isSameOperator
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.metSawmill
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.planksMade
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerShip.Companion.COINS
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerShip.Companion.LOGS
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerShip.Companion.MAX_PLANKS
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerShip.Companion.PLANK
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerShip.Companion.PLANK_PRICE
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The Varrock sawmill operator's nine off-the-clock planks for the Lady Lumbridge. */
class DragonSlayerSawmill
@Inject
constructor(private val ship: DragonSlayerShip, private val hooks: SawmillHooks) :
    PluginScript(), SawmillTalkHook {

    override fun ScriptContext.startup() {
        hooks.register(this@DragonSlayerSawmill)
    }

    override fun ScriptContext.shutdown() {
        hooks.unregister(this@DragonSlayerSawmill)
    }

    override fun option(player: Player, operator: SawmillOperatorsRow): String? =
        if (operator.isSameOperator(SawmillOperators.lumberYard) && ship.ownsShip(player)) {
            "As it so happens, I do need some wooden planks!"
        } else {
            null
        }

    override suspend fun choose(dialogue: Dialogue, operator: SawmillOperatorsRow) {
        dialogue.sawmillPlanks()
    }

    private suspend fun Dialogue.sawmillPlanks() {
        if (player.metSawmill == 0) {
            chatNpc(happy, "Fortunately for you, you've come to the right place!")
            chatNpc(sad, "Less fortunately for you, I'm on a legally mandated break right now.")
            chatPlayer(quiz, "Could you make an exception? I need the planks to go and fight a dragon.")
            chatNpc(confused, "A dragon? How are planks going to help you with that?")
            chatPlayer(neutral, "I'm repairing a boat to sail to Crandor so that I can slay Elvarg.")
            chatNpc(neutral, "Crandor? Elvarg? Well, far be it from me to stop you trying.")
            chatPlayer(quiz, "So you'll make some planks for me?")
            chatNpc(
                neutral,
                "I will, but I can't make you too many. It'll be just my luck if King Roald " +
                    "decides to audit this place and I've been making planks off the clock.",
            )
            player.metSawmill = 1
        }
        chatPlayer(quiz, "What do I need from you to make some planks?")
        val remaining = MAX_PLANKS - player.planksMade
        if (remaining <= 0) {
            chatNpc(
                sad,
                "Unfortunately I can't make any more planks for you without drawing attention to " +
                    "myself. You're going to have to source them elsewhere.",
            )
            return
        }
        chatNpc(
            neutral,
            "You'll have to provide the logs and 100 coins for each plank. Just basic logs will " +
                "do for what you need.",
        )
        chatNpc(
            neutral,
            "As a reminder, I can't make you very many without risking an audit, so I'll have to " +
                "stop at nine planks.",
        )
        val count = minOf(player.inv.count(LOGS), remaining)
        if (count == 0) {
            chatNpc(neutral, "You'll need to bring me some more logs.")
            return
        }
        val cost = count * PLANK_PRICE
        if (player.inv.count(COINS) < cost) {
            chatNpc(
                neutral,
                "Those planks cost 100 coins. You don't have enough money for all of them.",
            )
            return
        }
        if (!player.makeQuestPlanks(count)) {
            return
        }
        objbox(PLANK, "The sawmill operator turns your logs into planks.")
    }
}

/** Logs and 100 coins each become planks, counted against the nine the operator allows. */
internal fun Player.makeQuestPlanks(count: Int): Boolean {
    if (count <= 0 || planksMade + count > MAX_PLANKS) {
        return false
    }
    val made =
        exchange(
            take = listOf(LOGS to count, COINS to count * PLANK_PRICE),
            give = listOf(PLANK to count),
        )
    if (made) {
        planksMade += count
    }
    return made
}
