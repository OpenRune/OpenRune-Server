package org.rsmod.content.areas.misc.guilds.myths

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MythsGuildShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.myths_guild_herbalist") { startDialogue(it.npc) { primula(it.npc) } }
        onOpNpc3("npc.myths_guild_herbalist") { player.openHerbalist(it.npc) }
        onOpNpc1("npc.myths_guild_weapon_seller") { startDialogue(it.npc) { diana(it.npc) } }
        onOpNpc3("npc.myths_guild_weapon_seller") { player.openWeaponry(it.npc) }
        onOpNpc1("npc.myths_guild_smith") { startDialogue(it.npc) { erdan(it.npc) } }
        onOpNpc3("npc.myths_guild_smith") { player.openArmoury(it.npc) }
        onOpNpc1("npc.myths_guild_cape_seller") { startDialogue(it.npc) { jack(it.npc) } }
        onOpNpc3("npc.myths_guild_cape_seller") { player.openCapeStore(it.npc) }
    }

    private fun Player.openHerbalist(npc: Npc) {
        shops.open(this, npc, "Myths' Guild Herbalist", "inv.myths_guild_herbalist")
    }

    private fun Player.openWeaponry(npc: Npc) {
        shops.open(this, npc, "Myths' Guild Weaponry", "inv.myths_guild_weaponry")
    }

    private fun Player.openArmoury(npc: Npc) {
        shops.open(this, npc, "Myths' Guild Armoury", "inv.myths_guild_armoury")
    }

    private fun Player.openCapeStore(npc: Npc) {
        shops.open(this, npc, "Mythical Cape Store", "inv.myths_guild_capes")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Primula
    // TODO(content): [system:treasure_trails] master clue puzzle box
    // TODO(content): [skill:herblore] super antifire mixing does not check
    //  varbit.super_antifire_unlocked yet
    private suspend fun Dialogue.primula(npc: Npc) {
        chatPlayer(happy, "Hello.")
        if (player.superAntifireUnlocked) {
            chatNpc(happy, "Hello. Need anything?")
            when (choice2("Let's trade.", 1, "I'm good, thanks.", 2)) {
                1 -> {
                    chatPlayer(happy, "Let's trade.")
                    player.openHerbalist(npc)
                }
                2 -> chatPlayer(neutral, "I'm good, thanks.")
            }
            return
        }
        chatNpc(happy, "Hello. Interested in learning about some potions?")
        val option =
            choice3("Let's trade.", 1, "What kind of potions?", 2, "I'm good, thanks.", 3)
        when (option) {
            1 -> {
                chatPlayer(happy, "Let's trade.")
                player.openHerbalist(npc)
            }
            2 -> {
                chatPlayer(quiz, "What kind of potions?")
                chatNpc(
                    happy,
                    "Here's one I think you'll like. I found this recipe whilst exploring some " +
                        "old frozen ruins.",
                )
                player.superAntifireUnlocked = true
                mesbox("The herbalist teaches you how to make Super antifire potions.")
                chatPlayer(happy, "Thanks!")
                chatNpc(
                    happy,
                    "Don't mention it. Would you like to buy any herblore supplies?",
                )
                when (choice2("Yes please.", 1, "I'm good, thanks.", 2)) {
                    1 -> player.openHerbalist(npc)
                    2 -> chatPlayer(neutral, "I'm good, thanks.")
                }
            }
            3 -> chatPlayer(neutral, "I'm good, thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Diana
    private suspend fun Dialogue.diana(npc: Npc) {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Now then. Looking to buy some weaponry?")
        when (choice2("Let's trade.", 1, "No thanks", 2)) {
            1 -> {
                chatPlayer(happy, "Let's trade.")
                player.openWeaponry(npc)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "I'll be here if you change your mind.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Erdan
    private suspend fun Dialogue.erdan(npc: Npc) {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Good day. What can I help you with?")
        val option =
            choice3(
                "I would like to buy some equipment.",
                1,
                "Who are you?",
                2,
                "Just passing by.",
                3,
            )
        when (option) {
            1 -> buyEquipment(npc)
            2 -> {
                erdanHistory()
                when (choice2("I would like to buy some equipment.", 1, "I'll be off.", 2)) {
                    1 -> buyEquipment(npc)
                    2 -> chatPlayer(neutral, "I'll be off.")
                }
            }
            3 -> {
                chatPlayer(neutral, "Just passing by.")
                chatNpc(neutral, "As you wish.")
            }
        }
    }

    private suspend fun Dialogue.buyEquipment(npc: Npc) {
        chatPlayer(happy, "I would like to buy some equipment.")
        player.openArmoury(npc)
    }

    private suspend fun Dialogue.erdanHistory() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(neutral, "Well, you must be a new arrival. I am Erdan, a descendant of Uhld.")
        chatPlayer(
            quiz,
            "Uhld? I've seen that inscribed on that big old statue down below. What's that about?",
        )
        chatNpc(
            neutral,
            "It's also on the fountain in the dungeon below, it displays their emblem.",
        )
        chatPlayer(quiz, "Whose emblem?")
        chatNpc(
            neutral,
            "The Sect of Uhld, the founders of this here guild. I may not have gained entrance " +
                "to this guild in the usual fashion, but I'd expect that they'd at least inform " +
                "new members of this guild's history.",
        )
        chatPlayer(
            neutral,
            "Perhaps they expected you, a descendant of Uhld, to fill me in on the matter.",
        )
        chatNpc(neutral, "Aye, that may well be the case.")
        chatPlayer(neutral, "...")
        chatNpc(
            neutral,
            "Look, I'm awfully busy, what with the recent influx of members, so I'll give you " +
                "the short version for now.",
        )
        chatPlayer(neutral, "Fair enough.")
        chatNpc(
            neutral,
            "The story my father told me was of a war on magic. Of god fearing men who believed " +
                "nothing good would come of wielding a power that was never meant for them.",
        )
        chatNpc(
            neutral,
            "And so the rune temples that man had created were under threat. Before long, all " +
                "were destroyed and thus forgotten.",
        )
        chatNpc(
            neutral,
            "But the wrath that these Gunnites embraced rippled through the pocket dimensions. " +
                "Supposedly, a new altar of power was discovered.",
        )
        chatPlayer(quiz, "The wrath altar?")
        chatNpc(
            neutral,
            "Indeed. The temple was destroyed just as construction had finished, and a nearby " +
                "nomadic group called the Sect of Uhld were not pleased with the brutality they " +
                "witnessed.",
        )
        chatNpc(
            neutral,
            "They quickly established this guild with information preservation as their core " +
                "goal, for men are recorders of history and magic is key for this to last.",
        )
        chatPlayer(quiz, "Did the... er, Gunnites, return?")
        chatNpc(
            neutral,
            "Of course they did, but the Sect of Uhld had used the information gained from the " +
                "wrath altar, and with a spell of their own making, they destroyed the southern " +
                "runecrafting crusade with much ease.",
        )
        chatPlayer(happy, "Huh, that's fascinating.")
        chatNpc(
            neutral,
            "Indeed it is, but I best get back to work, so unless you wish to trade, that is all.",
        )
        chatPlayer(happy, "Thanks for the history lesson, Erdan.")
        chatNpc(happy, "No problem!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jack
    private suspend fun Dialogue.jack(npc: Npc) {
        chatNpc(happy, "Hi there. Would you like to buy a cape?")
        when (choice2("Yes please.", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(happy, "Yes please.")
                player.openCapeStore(npc)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "Suit yourself.")
            }
        }
    }
}

private var Player.superAntifireUnlocked by boolVarBit("varbit.super_antifire_unlocked")
