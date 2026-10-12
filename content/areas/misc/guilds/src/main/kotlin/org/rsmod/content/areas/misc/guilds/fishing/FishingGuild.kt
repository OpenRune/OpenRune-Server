package org.rsmod.content.areas.misc.guilds.fishing

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.baseFishingLvl
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.misc.guilds.SKILLCAPE_LEVEL
import org.rsmod.content.areas.misc.guilds.sellSkillcape
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class FishingGuild
@Inject
constructor(private val objRepo: ObjRepository, private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.master_fisher") { startDialogue(it.npc) { masterFisher() } }
        onOpNpc1("npc.fishguildshop") { startDialogue(it.npc) { roachey(it.npc) } }
        onOpNpc3("npc.fishguildshop") { player.openFishingGuildShop(it.npc) }
    }

    private fun Player.openFishingGuildShop(npc: Npc) {
        shops.open(this, npc, "Fishing Guild Shop.", "inv.fishingguild")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Master_fisher
    // TODO(content): [verify:in_game] whether the 68 Fishing check counts boosts (the guild door
    //  does)
    private suspend fun Dialogue.masterFisher() {
        if (player.fishingLvl < GUILD_LEVEL) {
            chatNpc(
                neutral,
                "Hello, I'm afraid only the top fishers are allowed to use our premier fishing " +
                    "facilities.",
            )
            return
        }
        chatNpc(
            happy,
            "Hello, welcome to the fishing guild. Please feel free to make use of any of our " +
                "facilities.",
        )
        val ask =
            choice2(
                "Yes",
                true,
                "No",
                false,
                title = "Would you like to ask about his Skillcape of Fishing?",
            )
        if (!ask) {
            return
        }
        chatPlayer(quiz, "Can you tell me about that skillcape you're wearing?")
        if (player.baseFishingLvl < SKILLCAPE_LEVEL) {
            chatNpc(
                happy,
                "I'm happy to, my friend. This beautiful cape was presented to me in recognition " +
                    "of my skills and experience as a fisherman and I was asked to be the head " +
                    "of this guild at the same time.",
            )
            chatNpc(
                neutral,
                "As the best fisherman in the guild it is my duty to control who has access to " +
                    "the guild and to say who can buy similar skillcapes and the cape can even " +
                    "teleport you right inside the guild.",
            )
            return
        }
        chatNpc(
            happy,
            "I'm happy to say that being such a fine fisher yourself, I'm happy to offer you the " +
                "opportunity to purchase a fine skillcape just like this one, it can even " +
                "teleport you right inside the guild. Are you interested? It would cost",
        )
        chatNpc(neutral, "99000 coins.")
        if (!choice2("Yes.", true, "No.", false)) {
            chatPlayer(neutral, "No, thanks.")
            chatNpc(neutral, "Okay, suit yourself.")
            return
        }
        chatPlayer(happy, "Yes, please.")
        if (sellSkillcape(objRepo, FISHING_CAPE, FISHING_HOOD)) {
            chatNpc(happy, "Excellent! Wear that cape with pride my friend.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Roachey
    private suspend fun Dialogue.roachey(npc: Npc) {
        chatNpc(happy, "Would you like to buy some fishing equipment, or sell some fish?")
        when (choice2("Yes please.", 1, "No thank you.", 2)) {
            1 -> player.openFishingGuildShop(npc)
            2 -> chatPlayer(neutral, "No thank you.")
        }
    }

    private companion object {
        const val GUILD_LEVEL = 68
        const val FISHING_CAPE = "obj.skillcape_fishing"
        const val FISHING_HOOD = "obj.skillcape_fishing_hood"
    }
}
