package org.rsmod.content.areas.misc.motherlode.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseMiningLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.content.areas.misc.motherlode.MotherlodeMine
import org.rsmod.content.areas.misc.motherlode.hasLargerSack
import org.rsmod.content.areas.misc.motherlode.hasUpperHopper
import org.rsmod.content.areas.misc.motherlode.hasUpperLevel
import org.rsmod.content.areas.misc.motherlode.syncMotherlodeVars
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ProspectorPercyScript
@Inject
constructor(private val shops: Shops, private val shopOps: ShopOperationMap) : PluginScript() {
    override fun ScriptContext.startup() {
        shopOps.costOf(CURRENCY) { it.cost.coerceAtLeast(1) }
        onOpNpc1(PERCY) { talk(it.npc) }
        onOpNpc3(PERCY) { player.openNuggetShop() }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) = startDialogue(npc) { greeting() }

    // https://oldschool.runescape.wiki/w/Transcript:Prospector_Percy
    private suspend fun Dialogue.greeting() {
        chatNpc(
            angry,
            "Git back ter work, ye young varmint! There's treasure in them walls, and it's not " +
                "gonna mine itself while ye stand here yappin'.",
        )
        mainOptions()
    }

    private suspend fun Dialogue.mainOptions() {
        val choice =
            choice5(
                "How do I mine here?",
                Topic.HowToMine,
                "Would you like to trade?",
                Topic.Trade,
                "Tell me about yourself.",
                Topic.About,
                "Is there anything else I can unlock here?",
                Topic.Unlocks,
                "I'll leave you alone.",
                Topic.Leave,
                title = "What would you like to say?",
            )
        when (choice) {
            Topic.HowToMine -> howToMine()
            Topic.Trade -> trade()
            Topic.About -> about()
            Topic.Unlocks -> {
                chatPlayer(quiz, "Is there anything else I can unlock here?")
                unlocks()
            }
            Topic.Leave -> {
                chatPlayer(neutral, "I'll leave you alone.")
                chatNpc(angry, "Dern straight ye will.")
            }
        }
    }

    private suspend fun Dialogue.howToMine() {
        chatPlayer(quiz, "How do I mine here?")
        chatNpc(
            neutral,
            "Git ahold of yer pickaxe, find a vein of ore, and set to work. If ye got a bit of " +
                "skill, ye'll have a pocket o' pay- dirt in no time.",
        )
        chatNpc(
            neutral,
            "I've built me a contraption to wash the pay-dirt. Just drop yer pay-dirt in the " +
                "hopper, an' wait fer it at the other end.",
        )
        chatNpc(
            neutral,
            "I won't charge ye fer usin' my contraption, but ye'd better fix it yerself when it " +
                "breaks. A good whack with a hammer usually settles it.",
        )
        chatNpc(
            angry,
            "Now will ye be gettin' to work now, or are ye gonna keep yappin' like a doggone " +
                "galoot?",
        )
        mainOptions()
    }

    private suspend fun Dialogue.trade() {
        chatPlayer(quiz, "Would you like to trade?")
        chatNpc(
            neutral,
            "If ye've found yerself some golden nuggets in this 'ere mine, I'll do you a swap, " +
                "yeah.",
        )
        player.openNuggetShop()
    }

    private suspend fun Dialogue.about() {
        chatPlayer(quiz, "Tell me about yourself.")
        chatNpc(
            happy,
            "Why, I'm Percy. Prospector Percy, the roughest, toughest, gruffest miner in the " +
                "land. I've been pannin' fer gold since I were a young-un, and here's where " +
                "I've struck it lucky.",
        )
        val language =
            choice2(
                "Excuse me, but what language are you speaking?",
                true,
                "You discovered this mine?",
                false,
                title = "What would you like to say?",
            )
        if (language) {
            chatPlayer(confused, "Excuse me, but what language are you speaking?")
            chatNpc(
                angry,
                "Don't ye give me any of yer lip, ye dern varmint! Young'uns these days got no " +
                    "respect.",
            )
            chatPlayer(neutral, "Do go on.")
        } else {
            chatPlayer(quiz, "You discovered this mine?")
        }
        chatNpc(
            neutral,
            "This here's the richest seam of ore I've found in all my days. After I built a " +
                "contraption for washing the pay- dirt, the dwarves let me run things down here.",
        )
        chatNpc(
            angry,
            "Now, have ye any more idjit questions, or are ye ready to do some real work?",
        )
        // TODO(content): [verify:in_game] "Shows other options" here is read as the main options.
        mainOptions()
    }

    private suspend fun Dialogue.unlocks() {
        val missing = Upgrade.entries.filterNot { it.unlocked(player) }
        if (missing.isEmpty()) {
            chatNpc(neutral, ALL_UNLOCKED)
            mainOptions()
            return
        }
        chatNpc(neutral, "Let me have a think...")
        if (!player.hasUpperLevel) {
            chatNpc(
                neutral,
                "If ye've got level ${MotherlodeMine.UPPER_LEVEL_MINING_LEVEL} Mining, ye could " +
                    "pay to use my restricted mine, up the ladder. $UPPER_LEVEL_COST nuggets " +
                    "gives unlimited access.",
            )
        }
        if (!player.hasLargerSack) {
            chatNpc(
                neutral,
                "How would ye like it if the sack were bigger? I'll let ye have more ore in " +
                    "there if ye pays me $LARGER_SACK_COST nuggets.",
            )
        }
        if (!player.hasUpperHopper) {
            val requirement =
                if (player.hasUpperLevel) {
                    "it connects to the hopper down here."
                } else {
                    "but you'll need access to the restricted mine first."
                }
            chatNpc(
                neutral,
                "I can give ye access to a special hopper in the restricted mine for " +
                    "$UPPER_HOPPER_COST nuggets, $requirement",
            )
        }
        val choice = unlockOptions(missing)
        if (choice == null) {
            mainOptions()
            return
        }
        purchase(choice)
    }

    private suspend fun Dialogue.unlockOptions(missing: List<Upgrade>): Upgrade? {
        val title = "Select an option"
        return when (missing.size) {
            1 -> choice2(missing[0].option, missing[0], "Cancel", null, title = title)
            2 ->
                choice3(
                    missing[0].option,
                    missing[0],
                    missing[1].option,
                    missing[1],
                    "Cancel",
                    null,
                    title = title,
                )
            else ->
                choice4(
                    missing[0].option,
                    missing[0],
                    missing[1].option,
                    missing[1],
                    missing[2].option,
                    missing[2],
                    "Cancel",
                    null,
                    title = title,
                )
        }
    }

    private suspend fun Dialogue.purchase(upgrade: Upgrade) {
        upgrade.request?.let { chatPlayer(neutral, it) }
        val level = MotherlodeMine.UPPER_LEVEL_MINING_LEVEL
        if (upgrade == Upgrade.UpperLevel && player.baseMiningLvl < level) {
            chatNpc(
                angry,
                "Ye'll need level $level Mining first. An' " +
                    "don't think ye can fool me with yer potions and fancy stat-boosts. Get yer " +
                    "level up for real.",
            )
            askForMore()
            return
        }
        if (player.inv.count(MotherlodeMine.NUGGET) < upgrade.cost) {
            chatNpc(
                neutral,
                "That'll be ${upgrade.cost} nuggets. If ye ain't got enough, ye'd better do some " +
                    "more mining - that's how ye gets stuff round here!",
            )
            askForMore()
            return
        }
        if (access.invDel(player.inv, MotherlodeMine.NUGGET, upgrade.cost).failure) {
            return
        }
        upgrade.unlock(player)
        player.syncMotherlodeVars()
        mesbox("You pay Percy ${upgrade.cost} nuggets.")
        chatNpc(happy, upgrade.thanks)
        if (Upgrade.entries.all { it.unlocked(player) }) {
            chatNpc(neutral, ALL_UNLOCKED)
            return
        }
        askForMore()
    }

    private suspend fun Dialogue.askForMore() {
        chatPlayer(quiz, "Is there anything else I can unlock here?")
        unlocks()
    }

    private fun Player.openNuggetShop() {
        shops.open(
            player = this,
            title = "Prospector Percy's Nugget Shop",
            shopInv = SHOP_INV,
            buyPercentage = 80.0,
            sellPercentage = 100.0,
            changePercentage = 0.0,
            currency = CURRENCY,
        )
    }

    private enum class Topic {
        HowToMine,
        Trade,
        About,
        Unlocks,
        Leave,
    }

    private enum class Upgrade(
        val cost: Int,
        val option: String,
        val request: String?,
        val thanks: String,
        val unlocked: (Player) -> Boolean,
        val unlock: (Player) -> Unit,
    ) {
        UpperLevel(
            UPPER_LEVEL_COST,
            "Restricted mine access: $UPPER_LEVEL_COST nuggets",
            "I'd like to buy access to the restricted mine, please.",
            "Right, ye can go up there whenever ye likes. Just behave yerself, or my Ma will tan " +
                "yer hide.",
            { it.hasUpperLevel },
            { it.hasUpperLevel = true },
        ),
        LargerSack(
            LARGER_SACK_COST,
            "Bigger sack: $LARGER_SACK_COST nuggets",
            "I'd like to get the bigger sack, please.",
            // TODO(content): [verify:in_game] The wiki transcript is missing this reply.
            "Right, I'll let ye have more ore in that sack o' yours.",
            { it.hasLargerSack },
            { it.hasLargerSack = true },
        ),
        UpperHopper(
            UPPER_HOPPER_COST,
            "Restricted hopper: $UPPER_HOPPER_COST nuggets",
            null,
            "Ye can now use that fancy new hopper in the restricted mine.",
            { it.hasUpperHopper },
            { it.hasUpperHopper = true },
        ),
    }

    private companion object {
        const val PERCY = "npc.motherlode_percy"
        const val SHOP_INV = "inv.motherlode_nugget_shop"
        const val CURRENCY = "currency.golden_nugget"

        const val UPPER_LEVEL_COST = 100
        const val UPPER_HOPPER_COST = 50
        const val LARGER_SACK_COST = 200

        const val ALL_UNLOCKED =
            "Well, now, I think ye've got everything already. Ye can already go climb the ladder " +
                "to the restricted mine, ye've got the bigger sack capacity too and ye have " +
                "access to that restricted hopper."
    }
}
