package org.rsmod.content.areas.city.keldagrim.dorgeshkaan.npcs

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
class DorgeshuunMines @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.dttd_bone_dealer") { startDialogue(it.npc) { nardok(it.npc) } }
        onOpNpc3("npc.dttd_bone_dealer") { player.openBoneWeapons(it.npc) }
        onOpNpc1("npc.dttd_maze_guide_there") { startDialogue(it.npc) { dartog() } }
    }

    private fun Player.openBoneWeapons(npc: Npc) {
        shops.open(this, npc, "Nardok's Bone Weapons", "inv.dorgeshuun_weapon_shop")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nardok (after The Lost Tribe)
    // TODO(content): [quest:the_lost_tribe] "You ain't getting nuthin' off me" before the quest
    private suspend fun Dialogue.nardok(npc: Npc) {
        chatNpc(shifty, "Psst... wanna buy some weapons?")
        val choice =
            choice3("What have you got?", 1, "No thanks.", 2, "Why are you whispering?", 3)
        when (choice) {
            1 -> {
                chatPlayer(quiz, "What have you got?")
                chatNpc(
                    shifty,
                    "Well, first up there's the normal bone club and bone spear. Good, solid " +
                        "frog-bone, and the spear has an iron tip. Can withstand a lot of " +
                        "punishment, if you know what I mean!",
                )
                chatNpc(
                    shifty,
                    "But if you're after something a bit fancier, a bit more sophisticated, " +
                        "there's the bone crossbow and dagger.",
                )
                chatNpc(
                    shifty,
                    "These are specially good for getting your prey unawares, but you're not " +
                        "going to get the best out of them unless you've been taught the special " +
                        "technique.",
                )
                chatNpc(shifty, "So, you interested?")
                interested(npc)
            }
            2 -> noThanks()
            else -> {
                chatPlayer(quiz, "Why are you whispering?")
                chatNpc(
                    shifty,
                    "Well, y'know, it's not the most reputable profession, is it? Selling " +
                        "weapons?",
                )
                chatPlayer(quiz, "Why not?")
                chatNpc(
                    shifty,
                    "Well, they're used to hurt people, aren't they? That's not considered very, " +
                        "y'know, respectable. Even though the guards use them, and the hunters, " +
                        "they're still considered a bit, y'know, dirty.",
                )
                chatNpc(quiz, "Isn't it like that where you come from?")
                chatPlayer(
                    neutral,
                    "No, on the surface weapon-making is considered a noble profession.",
                )
                chatNpc(confused, "I don't know, you surface people are strange!")
            }
        }
    }

    private suspend fun Dialogue.interested(npc: Npc) {
        val choice =
            choice3("I might be.", 1, "No thanks.", 2, "What is this special technique?", 3)
        when (choice) {
            1 -> {
                chatPlayer(neutral, "I might be.")
                player.openBoneWeapons(npc)
            }
            2 -> noThanks()
            else -> {
                chatPlayer(quiz, "What is this special technique?")
                chatNpc(
                    shifty,
                    "Special aiming technique. If you hit an enemy with it their defence will " +
                        "get weaker so you can hit them again!",
                )
                chatNpc(
                    shifty,
                    "Plus, if you use this technique on an enemy who isn't expecting it, you'll " +
                        "always hit them! Pretty good, eh?",
                )
                chatNpc(
                    shifty,
                    "'Course, there's only a few people who know it, so unless you can get " +
                        "someone to show you you won't be able to do it.",
                )
                chatPlayer(happy, "Zanik showed me how to do it!")
                chatNpc(happy, "Good for you! So, are you interested?")
                if (choice2("I might be.", 1, "No thanks.", 2) == 1) {
                    chatPlayer(neutral, "I might be.")
                    player.openBoneWeapons(npc)
                } else {
                    noThanks()
                }
            }
        }
    }

    private suspend fun Dialogue.noThanks() {
        chatPlayer(neutral, "No thanks.")
        chatNpc(neutral, "Yeah, you stay out of trouble.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dartog
    // TODO(content): [system:dorgeshuun_tunnel_guide] move the player through the tunnels
    private suspend fun Dialogue.dartog() {
        chatNpc(happy, "Hello, surface-dweller. Can I help you?")
        val choice =
            choice4(
                "Who are you?",
                1,
                "Can you show me the way to the mines?",
                2,
                "Can you show me the way to Lumbridge Castle cellar?",
                3,
                "I'm fine, thanks.",
                4,
            )
        when (choice) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    happy,
                    "The council posted me here to guard this new tunnel. I can also give you " +
                        "directions through the tunnels. A hero like you is always welcome in " +
                        "our mines!",
                )
            }
            2 -> {
                chatPlayer(quiz, "Can you show me the way to the mines?")
                chatNpc(happy, "Of course! You're always welcome in our mines!")
            }
            3 -> {
                chatPlayer(quiz, "Can you show me the way to Lumbridge Castle cellar?")
                chatNpc(happy, "Of course!")
            }
            else -> chatPlayer(neutral, "I'm fine, thanks.")
        }
    }
}
