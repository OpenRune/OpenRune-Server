package org.rsmod.content.other.sandstorm.npc

import dev.openrune.types.ItemServerType
import org.rsmod.api.config.Constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.content.other.sandstorm.MAX_STORED
import org.rsmod.content.other.sandstorm.SAND_PRICE
import org.rsmod.content.other.sandstorm.depositSandstone
import org.rsmod.content.other.sandstorm.heldBuckets
import org.rsmod.content.other.sandstorm.sandstoneDepositMessage
import org.rsmod.content.other.sandstorm.sandstoneObjs
import org.rsmod.content.other.sandstorm.sandstormBucketsEmpty
import org.rsmod.content.other.sandstorm.sandstormBucketsSand
import org.rsmod.content.other.sandstorm.storeBuckets
import org.rsmod.content.other.sandstorm.withdrawSand
import org.rsmod.content.other.sandstorm.withdrawableSand
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Drew : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.grinder_drew") { talkTo(it.npc) }
        onOpNpc3("npc.grinder_drew") { startDialogue(it.npc) { claimSand() } }
        onOpNpc4("npc.grinder_drew") { startDialogue(it.npc) { checkStorage() } }
        onOpNpc5("npc.grinder_drew") { startDialogue(it.npc) { depositBuckets() } }
        onOpNpcU("npc.grinder_drew") { useObjOnDrew(it.npc, it.objType) }
    }

    private suspend fun ProtectedAccess.talkTo(npc: Npc) {
        startDialogue(npc) {
            chatNpc(happy, "Hey there, what can I do for ya?")
            mainMenu()
        }
    }

    private suspend fun ProtectedAccess.useObjOnDrew(npc: Npc, objType: ItemServerType) {
        when {
            objType.internalName in sandstoneObjs -> depositSandstoneOn(npc)
            ocUncert(objType).internalName == "obj.bucket_empty" ->
                startDialogue(npc) { depositBuckets() }
            else -> mes(Constants.dm_default)
        }
    }

    private suspend fun ProtectedAccess.depositSandstoneOn(npc: Npc) {
        val deposit = depositSandstone()
        val message = sandstoneDepositMessage(deposit.overflowed, player.sandstormBucketsSand)
        startDialogue(npc) { chatNpc(neutral, message) }
    }

    private suspend fun Dialogue.mainMenu() {
        when (
            choice5(
                "Tell me about yourself.",
                1,
                "Deposit buckets.",
                2,
                "Withdraw buckets of sand.",
                3,
                "Check my buckets and sand.",
                4,
                "Nothing.",
                5,
            )
        ) {
            1 -> aboutYourself()
            2 -> depositBuckets()
            3 -> claimSand()
            4 -> checkStorage()
            5 -> nothing()
        }
    }

    private suspend fun Dialogue.aboutYourself() {
        chatNpc(
            happy,
            "My name is Drew and I man this here grinding machine. I call her Sandstorm!",
        )
        chatNpc(happy, "I can also look after any buckets ya want me to.")
        chatNpc(
            neutral,
            "Me and Sandstorm go way back, we started off back in the desert mining camp, I was " +
                "a slave and I had the honour of looking after Sandstorm.",
        )
        chatPlayer(quiz, "How did you and Sandstorm get out of the mining camp?")
        chatNpc(
            neutral,
            "Well, because I was in charge of looking after Sandstorm, I was able to look at the " +
                "inner workings.",
        )
        chatNpc(
            neutral,
            "While the guards weren't looking, I would take apart Sandstorm piece by piece and " +
                "place the pieces in boxes and barrels, when Sandstorms inners were all gutted, " +
                "I tricked the cart driver by telling him cart jokes.",
        )
        chatNpc(
            happy,
            "So, he didn't bother to check his cargo and I was able to board as he was " +
                "driving off.",
        )
        chatNpc(neutral, "Anyway, that's all in the past. Can I help ya with anything else?")
        mainMenu()
    }

    private suspend fun Dialogue.depositBuckets() {
        if (access.heldBuckets() <= 0) {
            chatNpc(neutral, "You haven't got any buckets for me to hold.")
            return
        }

        val requested = access.countDialog("How many buckets do you wish to deposit?")
        access.storeBuckets(requested)

        val stored = player.sandstormBucketsEmpty
        if (stored >= MAX_STORED) {
            chatNpc(
                neutral,
                "I am holding onto ${stored.formatAmount} buckets for ya. Why would ya want so " +
                    "many buckets?",
            )
            return
        }
        chatNpc(neutral, "I am holding onto ${stored.formatAmount} buckets for ya.")
    }

    private suspend fun Dialogue.claimSand() {
        if (access.withdrawableSand() <= 0) {
            chatNpc(
                neutral,
                "Ya don't have any buckets of sand to withdraw. Ya need to give me empty " +
                    "buckets, and grind sandstone in my grinder, then I'll sell ya buckets of " +
                    "sand.",
            )
            return
        }

        val requested =
            access.countDialog("How many buckets of sand do ya want? ($SAND_PRICE coins each)")
        if (access.withdrawSand(requested) <= 0) {
            return
        }
        chatNpc(neutral, "If ya need any more sand, please come back and use Sandstorm again.")
    }

    private suspend fun Dialogue.checkStorage() {
        chatNpc(
            neutral,
            "I have ${player.sandstormBucketsEmpty.formatAmount} of your buckets and you've " +
                "ground enough sandstone for " +
                "${player.sandstormBucketsSand.formatAmount} buckets of sand.",
        )
        chatNpc(quiz, "Would ya like to purchase some buckets of sand?")
        if (choice2("Yes.", true, "No.", false, title = "Select an Option")) {
            claimSand()
        }
    }

    private suspend fun Dialogue.nothing() {
        chatPlayer(neutral, "Nothing sorry. I have to be going now.")
        chatNpc(happy, "Oh, bye then.")
    }
}
