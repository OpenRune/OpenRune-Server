package org.rsmod.content.areas.misc.zanaris.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ZanarisTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.handsand_blaec") { startDialogue(it.npc) { blaec() } }
        onOpNpc1("npc.fairy_coordinator") { startDialogue(it.npc) { coordinator() } }
        onOpNpc1("npc.fairy") { startDialogue(it.npc) { fairy() } }
        onOpNpc1("npc.fairy_chef") { startDialogue(it.npc) { fairyChef() } }
        onOpNpc1("npc.fairy2_repair") { startDialogue(it.npc) { fairyFixit() } }
        onOpNpc1("npc.fairy_henchman1") { startDialogue(it.npc) { slimLouie() } }
        onOpNpc1("npc.fairy_henchman2") { startDialogue(it.npc) { fatRocco() } }
        onOpNpc1("npc.fairy_mindslayer") { startDialogue(it.npc) { gatekeeper() } }
        onOpNpc1("npc.fairy_queen") { startDialogue(it.npc) { fairyQueen() } }
        onOpNpc1("npc.fairy_nuff") { startDialogue(it.npc) { fairyNuff() } }
        onOpNpc1("npc.ii_lost_impling") { startDialogue(it.npc) { wanderingImpling() } }
        onOpNpc1("npc.fairy_ladder_attendant") { startDialogue(it.npc) { marketExitFairy() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Blaec
    private suspend fun Dialogue.blaec() {
        when (access.random.of(3)) {
            0 -> chatNpc(happy, "Greetin's ${player.displayName}, fine day today!")
            1 -> chatNpc(neutral, "Please leave me alone, I'm busy trapping the pygmy shrews.")
            else -> chatNpc(happy, "Wunnerful weather we're having today!")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Co-ordinator
    private suspend fun Dialogue.coordinator() {
        chatPlayer(quiz, "Hello, what are you doing?")
        val line =
            when (access.random.of(5)) {
                0 ->
                    "Sorry, I don't have time for idle chit-chat, I need to send a fairy to get " +
                        "little Freddies tooth!"
                1 ->
                    "Sorry, I don't have time to talk, I need to send a Tooth Fairy to visit " +
                        "Sarah-Jane!"
                2 ->
                    "Sorry, I don't have time for idle chit-chat, I need to find a Winter Fairy " +
                        "to send to Trollheim!"
                3 ->
                    "Sorry, I don't have time to stop, I need to send a Weather Fairy off to " +
                        "Etceteria!"
                else ->
                    "Sorry, I don't have time for idle chit-chat, I need to send an Autumn Fairy " +
                        "off to Burthorpe!"
            }
        chatNpc(neutral, line)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fairy
    // TODO(content): [quest:fairytale_ii_cure_a_queen] the Fairy Resistance Hideout lines
    private suspend fun Dialogue.fairy() {
        when (access.random.of(7)) {
            0 ->
                chatNpc(
                    confused,
                    "Have you got any idea where Burthorpe is? The co- ordinator says I need to " +
                        "go there and paint all the leaves yellow, but I've never heard of that " +
                        "place!",
                )
            1 ->
                chatNpc(
                    worried,
                    "Be careful when you pick mushrooms near the strange ruins to the south. " +
                        "Some of them will try to eat you!",
                )
            2 -> {
                chatNpc(
                    confused,
                    "Err, hello. Do you have any idea what a Winter Fairy actually does?",
                )
                chatPlayer(neutral, "No, no idea at all.")
                chatNpc(sad, "Oh, pity. Neither do I, but I'm supposed to be one this week.")
            }
            3 -> chatNpc(worried, "Sorry, I can't talk I'm looking for Sarah-Jane!")
            4 -> {
                chatNpc(quiz, "Is your name Freddie, and are you loosing your teeth?")
                chatPlayer(neutral, "Err, no. I'm afraid not.")
                chatNpc(sad, "Oh, right.")
                chatNpc(quiz, "You don't have any idea where I can find him do you?")
                chatPlayer(neutral, "No, I don't know that either.")
                chatNpc(
                    neutral,
                    "Oh, ok then. If you do find out please let me know. The co-ordinator has " +
                        "sent me to find him.",
                )
            }
            5 ->
                chatNpc(
                    neutral,
                    "I'm afraid I can't stop to chat. I've just been told to get ready to go to " +
                        "Etceteria for some reason or other!",
                )
            else ->
                chatNpc(
                    worried,
                    "Picking mushrooms near the forge will probably end quite badly for you.",
                )
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fairy_chef
    // TODO(content): [quest:fairytale_ii_cure_a_queen] showing Nuff's certificate
    private suspend fun Dialogue.fairyChef() {
        chatNpc(
            happy,
            "'Ello, sugar. I'm afraid I can't gossip right now, I've got a cake in the oven.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fairy_Fixit
    private suspend fun Dialogue.fairyFixit() {
        if (QuestRequirements.hasCompleted(player, FAIRYTALE_II)) {
            fairyFixitAfterQuest()
            return
        }
        chatNpc(neutral, "What is it, human? Busy busy busy!")
        when (choice2("Why are you carrying that toolbox?", 1, "I'm okay, thanks.", 2)) {
            1 -> fizgog()
            2 -> chatPlayer(neutral, "I'm okay, thanks.")
        }
    }

    // TODO(content): [system:fairy_enchantment_shop] Fairy Fixit's Fairy Enchantment after "What
    //  have you got for me?"; the cache has no shop inv for it
    private suspend fun Dialogue.fairyFixitAfterQuest() {
        chatNpc(shifty, "Pssst! Human! I've got something for you.")
        val option =
            choice3(
                "What have you got for me?",
                1,
                "Why are you carrying that toolbox?",
                2,
                "Not interested, thanks.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What have you got for me?")
                chatNpc(
                    happy,
                    "They said you'd helped cure our Queen. I haven't got a lot of rewards to " +
                        "offer, but my enchantment scrolls might help if you're working with " +
                        "fairy rings in your home.",
                )
            }
            2 -> fizgog()
            3 -> chatPlayer(neutral, "Not interested, thanks.")
        }
    }

    private suspend fun Dialogue.fizgog() {
        chatPlayer(quiz, "Why are you carrying that toolbox?")
        chatNpc(angry, "It's the fizgog! It's picking up cable again!")
        chatPlayer(confused, "Uh, right. So is it safe to use the fairy rings then?")
        chatNpc(
            neutral,
            "Sure, as long as you have been given permission to use them. You should " +
                "just be aware that using the fairy rings sometimes has strange results, " +
                "the locations that you have been to may affect the locations you are",
        )
        chatNpc(
            neutral,
            "trying to reach. I could fix it by replacing the fizgog and the " +
                "whosprangit, I've put in a request for some new parts but they're " +
                "pretty hard to get hold of, it seems.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Slim_Louie
    private suspend fun Dialogue.slimLouie() {
        chatNpc(angry, "Ah don' talk to humans! Yous wanna talk, talk to da boss!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fat_Rocco
    private suspend fun Dialogue.fatRocco() {
        chatNpc(angry, "Don' say nuffin' to me! Say it to da boss!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gatekeeper
    // TODO(content): [system:zanaris_market_door] the trading tax of one diamond at the magic door
    // TODO(content): [quest:fairytale_ii_cure_a_queen] showing Nuff's certificate
    private suspend fun Dialogue.gatekeeper() {
        chatPlayer(quiz, "What happened to the old man who used to be the doorman?")
        chatNpc(
            neutral,
            "You mean my father? He went into retirement, I've taken over the family business " +
                "instead.",
        )
        chatPlayer(shocked, "Your father! But you don't look anything like him!")
        chatNpc(happy, "No, fortunately for me I inherited my good looks from my mother.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fairy_Queen
    // TODO(content): [quest:fairytale_i_growing_pains] quest stages, and the Queen's state between
    //  the two quests
    // TODO(content): [system:treasure_trails] hard clue puzzle box
    private suspend fun Dialogue.fairyQueen() {
        if (QuestRequirements.hasCompleted(player, FAIRYTALE_II)) {
            chatPlayer(quiz, "Have you managed to work out a plan yet?")
            chatNpc(
                neutral,
                "Not yet, ${player.displayName}, but it looks like we'll need to ask you for " +
                    "your help again, I'm afraid. I'll be able to tell you more once we have " +
                    "finished making our battle plans.",
            )
            return
        }
        val option =
            choice2(
                "How do crops and such survive down here?",
                1,
                "What's so good about this place?",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(
                    quiz,
                    "How do crops and such survive down here? Surely they need a bit of sunlight?",
                )
                chatNpc(
                    neutral,
                    "Clearly you come from a plane dependent on sunlight. Down here, the plants " +
                        "grow in the aura of faerie.",
                )
            }
            2 -> {
                chatPlayer(quiz, "What's so good about this place?")
                chatNpc(
                    happy,
                    "Zanaris is a meeting point of cultures. Those from many worlds converge " +
                        "here to exchange knowledge and goods.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fairy_Nuff
    // TODO(content): [system:met_flags] the later-visit greeting and paying gems to be healed
    private suspend fun Dialogue.fairyNuff() {
        if (QuestRequirements.hasCompleted(player, FAIRYTALE_II)) {
            chatPlayer(
                quiz,
                "Why aren't you in the war room with the Fairy Queen and her advisors?",
            )
            chatNpc(
                angry,
                "I don't like war! I don't believe that violence is ever justified, that's why I " +
                    "became a healer.",
            )
            return
        }
        if (QuestRequirements.hasCompleted(player, FAIRYTALE_I)) {
            chatPlayer(quiz, "How is the Fairy Queen doing?")
            chatNpc(sad, "Not too well; the Godfather hasn't given me those secateurs yet.")
            return
        }
        chatPlayer(quiz, "What do you do around here?")
        chatNpc(
            happy,
            "Oh, I'm the resident healer. My name is Nuff, Fairy Nuff. Who are you?",
        )
        chatPlayer(neutral, "My name is ${player.displayName}.")
        chatNpc(
            happy,
            "Pleased to meet you, ${player.displayName}. As I was saying, I'm the healer in " +
                "Zanaris. I graduated over 200 human years ago. Look, there's my Advanced " +
                "Healing Certificate on the wall. I never go anywhere without it.",
        )
        chatNpc(
            neutral,
            "It's my job to look after the sick, wounded or dying fairies around here. Of " +
                "course, fairies being magical by nature, not many of them get hurt, so I don't " +
                "normally have much to do.",
        )
        chatPlayer(
            quiz,
            "What about healing humans? Lots of adventurers must get hurt fighting the " +
                "Otherworldly beings.",
        )
        chatNpc(
            neutral,
            "Yes they do. Silly isn't it? I do use my healing magic on humans sometimes, but I " +
                "don't like them attacking our other guests, so I charge them for the service.",
        )
        chatPlayer(quiz, "How much do you charge to heal someone, then?")
        chatNpc(
            neutral,
            "Well, the gold that you humans are so fond of has no real value in Zanaris, so I " +
                "ask my patients to pay me in gems.",
        )
        chatNpc(
            neutral,
            "I will heal 4 hitpoints for an opal, 5 hitpoints for either a sapphire or a piece " +
                "of jade, 7 hitpoints for an emerald or red topaz, 9 for a ruby and 11 for a " +
                "diamond.",
        )
        chatPlayer(shocked, "Only 11 hitpoints for a diamond? Doesn't that seem a bit steep?")
        chatNpc(
            neutral,
            "Maybe to you, but you'd be surprised how many adventurer's are willing to pay " +
                "those prices.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Wandering_impling (no crop circle available)
    // TODO(content): [system:impling_crop_circles] the cutscene showing the travelling crop
    //  circle's current location
    private suspend fun Dialogue.wanderingImpling() {
        chatNpc(confused, "Is it here? Is it there? Is it elsewhere?")
        chatPlayer(confused, "Is what where?")
        chatNpc(neutral, "This gate's sister...")
        chatPlayer(confused, "I didn't know gates had relations.")
        chatNpc(
            neutral,
            "Oh yes. This one here - she is calm and boring. She stays here all the time. She " +
                "doesn't like to move, but her sister is fidgety and refuses to stay in the same " +
                "place for more than half an hour.",
        )
        chatPlayer(quiz, "So where is the, errr, fidgety sister at the moment?")
        chatNpc(
            neutral,
            "Oh she's asleep at the moment. I'm sure she'll go somewhere in the next half an " +
                "hour though.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fairy_(Zanaris_market)
    // TODO(content): [system:fairy_rings] walk into the market fairy ring and arrive slightly west
    //  of Al Kharid bank
    private suspend fun Dialogue.marketExitFairy() {
        chatNpc(
            neutral,
            "This fairy ring will take you out of Zanaris. It leads to the place known as Al " +
                "Kharid in your realm. Once passed you can not return this way.",
        )
        chatNpc(
            happy,
            "Before leaving make sure that you have fully sampled the delights of our " +
                "marketplace.",
        )
        val option =
            choice2(
                "I think I'll stay down here a bit longer.",
                1,
                "Yes, I'm ready to leave.",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(neutral, "I think I'll stay down here a bit longer.")
                chatNpc(neutral, "As you wish.")
            }
            2 -> chatPlayer(happy, "Yes, I'm ready to leave.")
        }
    }

    private companion object {
        const val FAIRYTALE_I = "quest_fairytale1"
        const val FAIRYTALE_II = "quest_fairytale2"
    }
}
