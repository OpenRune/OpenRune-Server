package org.rsmod.content.areas.city.yanille.npcs

import jakarta.inject.Inject
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.area.lumbridge.RuneMysteriesQuest
import org.rsmod.content.skills.runecrafting.essence.teleportToRuneEssenceMine
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class YanilleTownsfolk
@Inject
constructor(
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val runeMysteries: RuneMysteriesQuest,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.handsand_bert") { startDialogue(it.npc) { bert() } }
        onOpNpc1("npc.colonel_radick") { startDialogue(it.npc) { colonelRadick(it.npc) } }
        onOpNpc1("npc.sigbert_the_adventurer") { startDialogue(it.npc) { sigbert() } }
        onOpNpc1("npc.tower_guard") { startDialogue(it.npc) { towerGuard() } }
        onOpNpc1("npc.watchtower_wizard") { startDialogue(it.npc) { watchtowerWizard() } }
        for (wizard in WATCHTOWER_WIZARDS) {
            onOpNpc1(wizard) { startDialogue(it.npc) { watchtowerWizards() } }
        }
        onOpNpc1("npc.zogre_human_zavistic_rarve") { startDialogue(it.npc) { zavisticRarve() } }
        onOpNpc1("npc.aluft_gnome_mage_2") { startDialogue(it.npc) { imblewyn() } }
        onOpNpc1("npc.wizard_frumscone") { startDialogue(it.npc) { frumscone() } }
        onOpNpc1(DISTENTOR) { startDialogue(it.npc) { distentor() } }
        onOpNpc3(DISTENTOR) { teleportToRuneEssenceMine(it.npc) }
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Hand_in_the_Sand (talking to Bert)
    // TODO(content): [quest:the_hand_in_the_sand] start offer, quest stages, after-quest lines
    // TODO(content): [quest:the_hand_in_the_sand] daily sand deliveries
    // TODO(content): [diary:ardougne] elite sand delivery notification options
    private suspend fun Dialogue.bert() {
        chatNpc(worried, "Eeee, wha' shall I do! I'll mos' certainly lose tha job...")
        chatPlayer(quiz, "Lose your job? What's wrong, why?")
        chatNpc(
            worried,
            "I w-w-work... over yon sand pit... and weeell... I found... this... hand! T'were " +
                "buried in't Sand!",
        )
        val authorities =
            choice2(
                "Oh, you found a hand in the sand - that's nice for you.",
                false,
                "Eww a hand, in the sand! Why haven't you told the authorities?",
                true,
            )
        if (!authorities) {
            chatPlayer(laugh, "Oh, you found a hand in the sand - that's nice for you.")
            return
        }
        chatPlayer(shocked, "Eww a hand, in the sand! Why haven't you told the authorities?")
        chatNpc(
            sad,
            "They's no' wha' they once was. Tha cap'ain o'the Guard spends near all o'the time " +
                "drunk in yon pub.",
        )
        chatPlayer(
            quiz,
            "Oh? The Guard Captain is drunk in the pub you say? That's not good, what will you do?",
        )
        chatNpc(worried, "Weeellll... do yer think yer could 'elp me?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Colonel_Radick
    private suspend fun Dialogue.colonelRadick(npc: Npc) {
        chatNpc(quiz, "Who goes there? Friend or foe?")
        val option =
            choice3(
                "Friend",
                1,
                "Foe!",
                2,
                "Why is this town so heavily defended?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Friend.")
                chatNpc(happy, "Ok good to hear it.")
            }
            2 -> {
                chatPlayer(angry, "Foe!")
                chatNpc(angry, "Oh righty...")
                npc.opPlayer2(player, aiPlayerInteractions)
            }
            3 -> {
                chatPlayer(quiz, "Why is this town so heavily defended?")
                chatNpc(
                    neutral,
                    "Yanille is on the southwest border of Kandarin, Beyond here you go into the " +
                        "Feldip hills which is major Ogre territory. Our job is to defend " +
                        "Yanille from the ogres.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sigbert_the_Adventurer
    private suspend fun Dialogue.sigbert() {
        chatNpc(worried, "I'd be very careful going down there friend.")
        val why = choice2("Why what's down there?", true, "Fear not, I am very strong", false)
        if (!why) {
            chatPlayer(happy, "Fear not, I am very strong")
            chatNpc(neutral, "You might find you are not so strong shortly...")
            return
        }
        chatPlayer(quiz, "Why what's down there ?")
        chatNpc(
            worried,
            "Salarin the twisted. One of Kandarin's most dangerous chaos druids. I tried to take " +
                "him on, then suddenly felt immensely weak.",
        )
        chatNpc(
            confused,
            "I hear he's susceptible to attacks from the mind. However I have no idea what that " +
                "means, so it's not much help to me.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tower_guard
    // TODO(content): [quest:watchtower] stopping players at the ladder before the quest
    private suspend fun Dialogue.towerGuard() {
        chatPlayer(quiz, "Hello. What are you doing here?")
        chatNpc(angry, "We are the tower guards - our business is our own!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Watchtower_Wizard (before Watchtower)
    // TODO(content): [quest:watchtower] start offer, quest stages and after-quest dialogue
    private suspend fun Dialogue.watchtowerWizard() {
        chatNpc(quiz, "Who are you? Are you one of the new guards?")
        chatPlayer(neutral, "No, I'm an adventurer.")
        chatNpc(quiz, "Well what are you doing here?")
        chatPlayer(happy, "Looking for adventures - what else?")
        chatNpc(sad, "Oh my, oh my! What does it all matter in the end, anyway?")
        val option =
            choice3(
                "What's the matter?",
                1,
                "I'm not interested in your rantings.",
                2,
                "You wizards are always complaining.",
                3,
            )
        when (option) {
            1 -> watchtowerTrouble()
            2 -> {
                chatPlayer(angry, "I'm not interested in your rantings.")
                chatNpc(angry, "Hmph! Suit yourself.")
            }
            3 -> {
                chatPlayer(angry, "You wizards are always complaining.")
                chatNpc(angry, "Complaining? Complaining!")
                chatNpc(
                    angry,
                    "What folks these days don't realise is that if it weren't for us wizards, " +
                        "this entire world would be overrun with every creature you could " +
                        "possibly imagine. And some you couldn't even conceive of!",
                )
            }
        }
    }

    private suspend fun Dialogue.watchtowerTrouble() {
        chatPlayer(quiz, "What's the matter?")
        chatNpc(sad, "Oh dear, oh dear. Darn and drat!")
        chatNpc(sad, "We try hard to keep this town protected.")
        chatNpc(sad, "But how can we do that when the Watchtower isn't working?")
        chatPlayer(quiz, "What do you mean it isn't working?")
        chatNpc(
            neutral,
            "The Watchtower over here works by the power of magic. An ancient spell designed to " +
                "ward off ogres that has been in place here for many moons.",
        )
        chatNpc(
            neutral,
            "The exact knowledge of the spell is lost to us now, but the essence of the spell " +
                "has been infused into four powering crystals that keep the tower protected " +
                "from the hordes in the Feldips.",
        )
        chatPlayer(quiz, "So how come the spell doesn't work?")
        chatNpc(shocked, "The crystals! The crystals have been taken!")
        chatPlayer(confused, "Taken?")
        chatNpc(shocked, "Stolen!")
        chatPlayer(confused, "Stolen?")
        chatNpc(angry, "Yes, yes! Do I have to repeat myself?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Wizard_(Watchtower) (before Watchtower)
    // TODO(content): [quest:watchtower] quest stages and after-quest dialogue
    private suspend fun Dialogue.watchtowerWizards() {
        chatPlayer(quiz, "What's going on here?")
        chatNpc(neutral, "You'll have to speak with the Watchtower Wizard about that.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Zavistic_Rarve
    // TODO(content): [quest:the_hand_in_the_sand] quest stages and after-quest greeting
    // TODO(content): [quest:zogre_flesh_eaters] quest stages and selling the black prism
    // TODO(content): [system:wizards_guild_bell] Zavistic answering the bell outside the guild
    private suspend fun Dialogue.zavisticRarve() {
        chatNpc(
            angry,
            "What are you doing bothering me? Don't you think some of us have work to do?",
        )
        chatPlayer(confused, "I thought you were here to help?")
        chatNpc(
            neutral,
            "Well... I am, I suppose, anyway... we're very busy here, hurry up, what do you want?",
        )
        while (true) {
            val option =
                choice4(
                    "What is there to do in the Wizards' Guild?",
                    1,
                    "What are the requirements to get in the Wizards' Guild?",
                    2,
                    "What do you do in the Guild?",
                    3,
                    "Ok, thanks.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What is there to do in the Wizards' Guild?")
                    chatNpc(
                        neutral,
                        "This is the finest wizards' establishment in the land. We have magic " +
                            "portals to the other towers of wizardry around Gielinor. We have a " +
                            "particularly wide collection of runes in our rune shop. We sell " +
                            "some of the finest",
                    )
                    chatNpc(
                        neutral,
                        "mage robes in the land and we have a training area full of zombies for " +
                            "you to practice your magic on.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What are the requirements to get in the Wizards' Guild?")
                    chatNpc(
                        neutral,
                        "You need a magic level of 66, the high magic energy level is too " +
                            "dangerous for anyone below that level.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "What do you do in the Guild?")
                    chatNpc(
                        neutral,
                        "I'm the Grand Secretary for the Wizards' Guild, I have lots of " +
                            "correspondence to keep up with, as well as attending to the " +
                            "discipline of the more problematic guild members.",
                    )
                }
                4 -> {
                    chatPlayer(neutral, "Ok, thanks.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Professor_Imblewyn
    // TODO(content): [quest:a_tail_of_two_cats] "Hey! Some of my best friends are cats!"
    // TODO(content): [system:gnome_restaurant] food deliveries
    private suspend fun Dialogue.imblewyn() {
        val title = if (access.isBodyTypeA()) "lad" else "lass"
        chatPlayer(quiz, "I didn't realise gnomes were interested in magic.")
        chatNpc(happy, "Gnomes are interested in everything, $title.")
        chatPlayer(neutral, "Of course.")
        chatNpc(
            happy,
            "We have a saying, 'Curiosity killed the cat'. But cats aren't clever enough to " +
                "understand what they find. Gnomes are.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Wizard_Frumscone
    // TODO(content): [quest:swan_song] after-quest dialogue about the necromancer
    private suspend fun Dialogue.frumscone() {
        chatNpc(
            happy,
            "Do you like my magic Zombies? Feel free to kill them, there's plenty more where " +
                "these came from!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Wizard_Distentor
    // TODO(content): [verify:in_game] the essence mine option and op before Rune Mysteries
    private suspend fun Dialogue.distentor() {
        chatNpc(happy, "Welcome to the Magicians' Guild!")
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "What can I do for you?")
        val teleport =
            runeMysteries.quest.isQuestCompleted(player) &&
                choice2(
                    "Nothing thanks, I'm just looking around.",
                    false,
                    "Can you teleport me to the Rune Essence?",
                    true,
                )
        if (teleport) {
            chatPlayer(quiz, "Can you teleport me to the Rune Essence?")
            teleportToRuneEssenceMine()
            return
        }
        chatPlayer(neutral, "Nothing thanks, I'm just looking around.")
        chatNpc(happy, "That's fine with me.")
    }

    private companion object {
        const val DISTENTOR = "npc.guild_wizard"
        val WATCHTOWER_WIZARDS =
            listOf(
                "npc.qip_watchtower_wizard_02",
                "npc.qip_watchtower_wizard_03",
                "npc.qip_watchtower_wizard_04",
            )
    }
}
