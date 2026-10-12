package org.rsmod.content.areas.misc.guilds.heroes

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class HeroesGuild @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.achietties") { startDialogue(it.npc) { achietties() } }
        onOpNpc1("npc.helemos") { startDialogue(it.npc) { helemos(it.npc) } }
        onOpNpc3("npc.helemos") { player.openHemporium(it.npc) }
    }

    private fun Player.openHemporium(npc: Npc) {
        shops.open(this, npc, "Happy Heroes' H'emporium.", "inv.dragonaxeshop")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Achietties (before Heroes' Quest)
    // TODO(content): [quest:heroes_quest] quest start for eligible players, quest progress and
    //  the after-quest greeting
    private suspend fun Dialogue.achietties() {
        chatNpc(neutral, "Greetings. Welcome to the Heroes' Guild.")
        chatNpc(neutral, "Only the greatest heroes of this land may gain entrance to this guild.")
        val option =
            choice2(
                "I'm a hero, may I apply to join?",
                1,
                "Good for the foremost heroes of the land.",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "I'm a hero - may I apply to join?")
                chatNpc(
                    neutral,
                    "You're a hero? I've never heard of YOU. You are required to possess at " +
                        "least 55 quest points to file an application.",
                )
                chatNpc(
                    neutral,
                    "Additionally you must have completed the Shield of Arrav, Lost City, " +
                        "Merlin's Crystal and Dragon Slayer I quests.",
                )
            }
            2 -> {
                chatPlayer(neutral, "Good for the foremost heroes of the land.")
                chatNpc(neutral, "Yes. Yes it is.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Helemos
    private suspend fun Dialogue.helemos(npc: Npc) {
        chatNpc(happy, "Welcome to the Heroes' Guild!")
        val option =
            choice2("So do you sell anything here?", 1, "So what can I do here?", 2)
        when (option) {
            1 -> {
                chatPlayer(quiz, "So do you sell anything good here?")
                chatNpc(happy, "Why yes! We DO run an exclusive shop for our members!")
                player.openHemporium(npc)
            }
            2 -> {
                chatPlayer(quiz, "So what can I do here?")
                chatNpc(
                    happy,
                    "Look around... there are all sorts of things to keep our guild members " +
                        "entertained!",
                )
            }
        }
    }
}
