package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingJaneScript @Inject constructor(private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(JANE) { startDialogue(it.npc) { welcome() } }
        if (FarmingContracts.assignmentAvailable) {
            onOpNpc3(JANE) { startDialogue(it.npc) { contract() } }
        }
        onEvent<FarmingCheckHealthEvent> { FarmingContracts.complete(player, patch, crop, true) }
        onEvent<FarmingHarvestEvent> {
            if (final) FarmingContracts.complete(player, patch, crop, false)
        }
        onOpHeld1(FarmingSeedPacks.PACK) { takePack(it.slot, all = false) }
        onOpHeld3(FarmingSeedPacks.PACK) { takePack(it.slot, all = true) }
        onOpHeld4(FarmingSeedPacks.PACK) {
            val pack = inv[it.slot] ?: return@onOpHeld4
            val state = SeedPackState.unpack(pack.vars)
            mes("The seed pack has ${state.remaining} rewards remaining.")
        }
    }

    private suspend fun Dialogue.welcome() {
        chatNpc(happy, "Welcome to the farming guild. How may I help you?")
        val option = if (FarmingContracts.assignmentAvailable) {
            choice5("Tell me about the guild.", 1, "Do you have any jobs for me?", 2,
                "How many farming contracts have I completed?", 3,
                "Can I trade in some spirit seeds?", 4, "I'm just passing by.", 5)
        } else {
            choice4("Tell me about the guild.", 1, "How many farming contracts have I completed?", 3,
                "Can I trade in some spirit seeds?", 4, "I'm just passing by.", 5)
        }
        when (option) {
            1 -> {
                chatNpc(neutral, "The guild is split into three tiers, with only advanced farmers being allowed access to the upper echelons.")
                chatNpc(neutral, "The beginner tier requires 45 Farming, the intermediate tier requires 65 Farming, and the advanced tier requires 85 Farming.")
            }
            2 -> contract()
            3 -> chatNpc(neutral, "You've completed ${vars[FarmingContracts.COUNT]} farming contracts.")
            4 -> exchange()
        }
    }

    private suspend fun Dialogue.contract() {
        if (vars[FarmingContracts.DISCUSSED] == 0) {
            chatNpc(neutral, "Well we always need to rotate the crops we're growing to stop the soil from tiring due to lack of nutrients. It would be great if you could help out with that.")
            chatPlayer(quiz, "What would I need to do?")
            chatNpc(neutral, "If you're interested, I can give you a farming contract which tells you what to plant within the guild. Complete the contract, and I'll give you some seeds as a thank you.")
            VarPlayerIntMapSetter.set(player, FarmingContracts.DISCUSSED, 1)
        }
        when {
            vars[FarmingContracts.COMPLETE] != 0 -> {
                val previous = FarmingContracts.claim(player, random)
                if (previous == null) {
                    chatNpc(neutral, "You'll be wanting a reward then. Come back when you have some room to take it.")
                    return
                }
                chatNpc(happy, "You'll be wanting a reward then. Here's a seed pack.")
                objbox(FarmingSeedPacks.PACK, "Guildmaster Jane has given you a seed pack.")
                when (choice3("Yes.", 1, "No.", 2, "Take another ${tierName(previous)} contract.", 3, title = "Take another contract?")) {
                    1 -> selectTier()
                    3 -> assign(previous)
                }
            }
            vars[FarmingContracts.TYPE] != 0 -> FarmingContracts.current(player)?.let { describe(it) }
            else -> selectTier()
        }
    }

    private suspend fun Dialogue.selectTier() {
        chatNpc(quiz, "What kind of contract would you like?")
        val tier = choice3(tierOption(player, 1), 1, tierOption(player, 2), 2, tierOption(player, 3), 3)
        assign(tier)
    }

    private suspend fun Dialogue.assign(tier: Int) {
        val assigned = FarmingContracts.assign(player, tier, random)
        if (assigned == null) {
            chatNpc(neutral, "You need a Farming level of at least ${FarmingContracts.requiredLevel(tier)} for that contract.")
            return
        }
        describe(assigned)
    }

    private suspend fun Dialogue.describe(contract: FarmingContract) {
        val crop = contract.crop
        val instruction = if (crop.checksHealth) "once you have checked its health" else "once they have been fully harvested"
        val article = if (crop.checksHealth) "a" else "some"
        chatNpc(neutral, "Please could you grow $article ${crop.name} for us? I'll reward you $instruction.")
        if (contract.tier <= 1) return
        if (choice2("Do you have anything easier?", 1, "Thank you.", 2) != 1) return
        chatNpc(neutral, "I suppose I could give you a different contract, if you're sure.")
        if (choice2("Yes please.", 1, "No thanks.", 2) != 1) return
        FarmingContracts.downgrade(player, random)?.let { describe(it) }
    }

    private suspend fun Dialogue.exchange() {
        chatNpc(neutral, "Of course, I will also take potted spirit seeds and saplings. What would you like to trade in?")
        val choice = choice5("Spirit seed", 0, "Spirit seedling", 1, "Spirit seedling(w)", 2, "Spirit sapling", 3, "All", 4)
        val chain = FarmingSaplings.chains.single { it.seed.internalName == "obj.spirit_tree_seed" }
        val items = listOf(chain.seed, chain.dry, chain.wet, chain.sapling)
        var exchanged = 0
        for (type in if (choice == 4) items else listOf(items[choice])) {
            while (player.inv.contains(type.internalName)) {
                if (!FarmingSeedPacks.exchange(player, type, random)) break
                exchanged++
                if (choice != 4) break
            }
        }
        if (exchanged == 0) {
            chatNpc(neutral, "I'm afraid you have nothing to trade in, or no room for a seed pack.")
        } else {
            doubleobjbox("obj.spirit_tree_seed", FarmingSeedPacks.PACK, "You trade in your spirit seed items for $exchanged seed pack${if (exchanged == 1) "" else "s"}.")
        }
    }

    private fun ProtectedAccess.takePack(slot: Int, all: Boolean) {
        do {
            val expected = inv[slot] ?: return
            if (!FarmingSeedPacks.take(player, slot, expected, random)) {
                mes("You don't have enough inventory space to take any more seeds.")
                return
            }
        } while (all && inv[slot]?.id == FarmingSeedPacks.PACK.asRSCM())
    }

    private fun tierOption(player: Player, tier: Int): String {
        val text = "${tierName(tier).replaceFirstChar(Char::uppercaseChar)} Contract (Requires ${FarmingContracts.requiredLevel(tier)} Farming)"
        return if (player.farmingLvl >= FarmingContracts.requiredLevel(tier)) text else "<str>$text</str>"
    }

    private fun tierName(tier: Int): String = when (tier) {
        1 -> "easy"
        2 -> "medium"
        else -> "hard"
    }

    private companion object {
        const val JANE = "npc.farming_guild_master"
    }
}
