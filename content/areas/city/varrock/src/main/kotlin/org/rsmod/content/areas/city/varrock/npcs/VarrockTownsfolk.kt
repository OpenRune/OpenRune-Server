package org.rsmod.content.areas.city.varrock.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class VarrockTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.jonny_the_beard") { startDialogue(it.npc) { jonnyTheBeard() } }
        onOpNpc1("npc.vc_hooknosed_jack") { startDialogue(it.npc) { hooknosedJack() } }
        onOpNpc1("npc.varrock_granny_1") { startDialogue(it.npc) { iffie() } }
        onOpNpc1("npc.artist2") { startDialogue(it.npc) { daVinci() } }
        onOpNpc1("npc.gambler2") { startDialogue(it.npc) { chancy() } }
        onOpNpc1("npc.drunk2") { startDialogue(it.npc) { hops() } }
        onOpNpc1("npc.aris") { startDialogue(it.npc) { aris() } }
        onOpNpc1("npc.juliet") { startDialogue(it.npc) { juliet() } }
        onOpNpc1("npc.draul_leptoc") { startDialogue(it.npc) { draulLeptoc() } }
        onOpNpc1("npc.phillipa") { startDialogue(it.npc) { phillipa() } }
        onOpNpc1("npc.tailor_boutique") { startDialogue(it.npc) { suzie() } }
        onOpNpc1("npc.25th_host") { startDialogue(it.npc) { wizardGary() } }
        onOpNpc1("npc.pmod_town_crier_varrock") { startDialogue(it.npc) { townCrier() } }
        onOpNpc1("npc.johnathon_fitzharmon") { startDialogue(it.npc) { johnathon() } }
        onOpNpc1("npc.con_contractor_varrock") { startDialogue(it.npc) { marlo() } }
        onOpNpc1("npc.con_contract_client_1") { startDialogue(it.npc) { mahoganyClient() } }
        onOpNpc1("npc.con_contract_client_2") { startDialogue(it.npc) { mahoganyClient() } }
        onOpNpc1("npc.con_contract_client_3") { startDialogue(it.npc) { mahoganyClient() } }
        onOpNpc1("npc.fai_varrock_jollyboar_chef") { startDialogue(it.npc) { jollyBoarCook() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jonny_the_Beard
    private suspend fun Dialogue.jonnyTheBeard() {
        chatNpc(angry, "Whatever you want, I'm not interested!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hooknosed_Jack (before Ratcatchers)
    // TODO(content): [quest:ratcatchers] after-quest dialogue and the rat pits manhole line
    private suspend fun Dialogue.hooknosedJack() {
        chatPlayer(happy, "Hello.")
        chatNpc(angry, "What do you want?")
        chatNpc(angry, "Actually, I've got no time for this. I don't want to talk to you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Iffie (2021 Halloween event not completed)
    // TODO(content): [event:halloween_2021] "Hello Iffie, remember our adventure?"
    private suspend fun Dialogue.iffie() {
        chatPlayer(happy, "Hello Iffie!")
        chatNpc(
            neutral,
            "Sorry, dearie, if I stop to chat I'll lose count. Talk to my sister instead; she " +
                "likes to chat. You'll find her upstairs in the church.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Da_Vinci
    // TODO(content): [quest:biohazard] only in the Dancing Donkey after Biohazard
    private suspend fun Dialogue.daVinci() {
        chatNpc(
            angry,
            "Bah! A great artist such as myself should not have to suffer the HUMILIATION of " +
                "spending time where the likes of you wander everywhere!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Chancy (Varrock, members world)
    // TODO(content): [quest:biohazard] only in the Dancing Donkey after Biohazard
    private suspend fun Dialogue.chancy() {
        chatPlayer(happy, "Good morning.")
        chatNpc(angry, "Leave me alone. I'm trying to find my gambling buddies!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hops_(Biohazard) (members world)
    // TODO(content): [quest:biohazard] only in the Dancing Donkey after Biohazard
    private suspend fun Dialogue.hops() {
        chatNpc(drunk, "Hops don't wanna talk now.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Demon_Slayer (starting Demon Slayer)
    // TODO(content): [quest:demon_slayer] fortune telling and start, in-progress and after-quest
    // dialogue (https://oldschool.runescape.wiki/w/Transcript:Aris)
    private suspend fun Dialogue.aris() {
        chatNpc(
            happy,
            "Hello young one. Cross my palm with silver and the future will be revealed to you.",
        )
        if ("obj.coins" !in player.inv) {
            chatPlayer(sad, "Oh dear. I don't have any money.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Juliet
    // TODO(content): [quest:romeo_and_juliet] quest and after-quest dialogue
    private suspend fun Dialogue.juliet() {
        chatNpc(sad, "Romeo, Romeo, wherefore art thou Romeo?")
        mesbox("She seems to be lost in thought.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Draul_Leptoc
    // TODO(content): [quest:romeo_and_juliet] quest and after-quest dialogue
    private suspend fun Dialogue.draulLeptoc() {
        chatNpc(
            angry,
            "What are you doing in my house...why the impertinence...the sheer cheek...how dare " +
                "you violate my personal lodgings....",
        )
        chatPlayer(worried, "I..I was just looking around....")
        chatNpc(
            angry,
            "Well get out! Get out....this is my house....and don't go near my daughter " +
                "Juliet...she's grounded in her room to keep her away from that good for nothing " +
                "Romeo.",
        )
        chatPlayer(worried, "Yes....sir....")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Phillipa
    // TODO(content): [quest:romeo_and_juliet] quest and after-quest dialogue
    private suspend fun Dialogue.phillipa() {
        chatPlayer(quiz, "Hello, who are you?")
        chatNpc(
            happy,
            "Hi, I'm Phillipa, Juliet's cousin. I like to keep an eye on her, make sure that " +
                "dashing young Romeo doesn't just steal her away under our plain old noses!",
        )
        chatNpc(
            happy,
            "He'd do it, you know - he's ever so dashing, and cavalier, in a wet blanket sort of " +
                "way.",
        )
        chatPlayer(quiz, "Romeo? Where would I find him then?")
        chatNpc(
            happy,
            "Well, that's a good question! Who knows where his head's at most of the time? In " +
                "the clouds, most likely!",
        )
        chatNpc(
            happy,
            "But he's probably chasing the ladies who frequent Varrock market. He does like a " +
                "bit of kiss chase, so I've heard!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Suzie
    private suspend fun Dialogue.suzie() {
        chatPlayer(happy, "Hello!")
        chatNpc(silent, "...")
        chatPlayer(quiz, "Is everything okay?")
        chatNpc(shifty, "*mumble mumble*")
        chatPlayer(confused, "I didn't catch that.")
        chatNpc(silent, "...")
        chatPlayer(neutral, "Right... Well, have a nice day.")
        chatNpc(shifty, "*mumble*")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Wizard_Gary
    // TODO(content): [event:25th_anniversary] dialogue after helping open the Realm of Memories
    private suspend fun Dialogue.wizardGary() {
        chatNpc(happy, "Hello. What brings you here?")
        while (true) {
            val option =
                choice3(
                    "Who are you?",
                    1,
                    "If you're a wizard, are you doing anything magical?",
                    2,
                    "Bye, Gary.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        happy,
                        "I'm Gary, the wizard. I've not actually been qualified for very long, " +
                            "so I'm living here near the rune shop so I can keep practicing the " +
                            "magical arts.",
                    )
                    chatNpc(
                        neutral,
                        "I seem to be raising a cat, too, ever since some local lady persuaded " +
                            "me to buy one off her.",
                    )
                    val next =
                        choice3(
                            "Is the cat supposed to help you get runes too?",
                            1,
                            "If you're a wizard, are you doing anything magical?",
                            2,
                            "Bye, Gary.",
                            3,
                        )
                    when (next) {
                        1 -> {
                            chatPlayer(quiz, "Is the cat supposed to help you get runes too?")
                            chatNpc(
                                angry,
                                "No! I know there are places where people think it's normal to " +
                                    "swap cats for runes, but that will NOT happen to Sookie!",
                            )
                        }
                        2 -> wizardGaryMagic()
                        3 -> {
                            chatPlayer(neutral, "Bye, Gary.")
                            return
                        }
                    }
                }
                2 -> wizardGaryMagic()
                3 -> {
                    chatPlayer(neutral, "Bye, Gary.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.wizardGaryMagic() {
        chatPlayer(quiz, "If you're a wizard, are you doing anything magical?")
        chatNpc(
            happy,
            "Always! But I'm not looking for any assistance from passing adventurers, if that's " +
                "what you were thinking. Maybe another time.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Town_Crier
    private suspend fun Dialogue.townCrier() {
        chatNpc(happy, "Hello citizen!")
        if (choice2("What do you do around here?", true, "See you later.", false)) {
            chatPlayer(quiz, "What do you do around here?")
            chatNpc(
                happy,
                "I'm a Town Crier. It's my job to let people know of any recent news. After all, " +
                    "there's all sorts of things happening around here.",
            )
            chatPlayer(neutral, "I see. See you later.")
        } else {
            chatPlayer(neutral, "See you later.")
        }
        chatNpc(happy, "Until next time.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Johnathon
    // TODO(content): [quest:family_crest] gauntlet changing after Family Crest
    // TODO(content): [quest:family_pest] chaos gauntlets after Family Pest
    private suspend fun Dialogue.johnathon() {
        chatNpc(sad, "I am so very tired... Leave me be... to rest...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Marlo
    // TODO(content): [system:mahogany_homes] contracts after joining the company
    private suspend fun Dialogue.marlo() {
        chatNpc(happy, "Hey, how can I help you?")
        chatPlayer(quiz, "What do you do here?")
        chatNpc(
            happy,
            "This is one of the branches for Mahogany Homes, Gielinor's first construction " +
                "company.",
        )
        chatPlayer(happy, "Oh wow, how can I get involved?")
        chatNpc(
            happy,
            "If you go see Amy in the Falador branch, she'll fill you in. Her office is just " +
                "east of the Estate Agent in Falador.",
        )
        chatPlayer(happy, "Nice, thank you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bob_(Mahogany_Homes)
    // https://oldschool.runescape.wiki/w/Transcript:Jeff_(Mahogany_Homes)
    // https://oldschool.runescape.wiki/w/Transcript:Sarah_(Mahogany_Homes)
    // TODO(content): [system:mahogany_homes] contract requests and the after-contract cup of tea
    private suspend fun Dialogue.mahoganyClient() {
        chatNpc(neutral, "Please excuse me, I'm rather busy.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cook_(Jolly_Boar_Inn)
    // TODO(content): [system:met_flags] "I distinctly remember..." greeting after asking to be cooked something
    private suspend fun Dialogue.jollyBoarCook() {
        chatNpc(angry, "What do you want? I'm busy!")
        val option =
            choice3(
                "Can you cook me something?",
                1,
                "Why do you work here?",
                2,
                "Can you tell me any good jokes?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Can you cook me something?")
                chatNpc(angry, "What? No! Go away.")
                chatPlayer(
                    confused,
                    "Well, you're meant to be a cook. Why can't you cook something for me?",
                )
                chatNpc(angry, "I don't want to, that's why! Now, leave me alone.")
                chatPlayer(sad, "You're not a very nice man.")
                chatNpc(angry, "Yet you keep talking to me!")
                chatPlayer(neutral, "Good point. I should stop that.")
                chatNpc(angry, "Yes, do us both a favour.")
            }
            2 -> {
                chatPlayer(quiz, "Why do you work here?")
                chatNpc(
                    angry,
                    "Why do you think I work in a disreputable inn filled with rough scoundrels " +
                        "on the very edge of the Wilderness? I work here because I need the job!",
                )
                chatPlayer(quiz, "Do you like it?")
                chatNpc(
                    sad,
                    "Like it? It's sucked the warmth and humanity out of me, and I've become a " +
                        "hollow shell holding nothing but anger, misery and gourmet recipes.",
                )
                chatPlayer(neutral, "Oh.")
                chatNpc(
                    angry,
                    "What did you expect to hear? I suppose you expected me to tell you an " +
                        "inspiring story about how working here can teach you to find hope in " +
                        "the darkest places, or how to see the best in everything?",
                )
                chatPlayer(neutral, "Maybe you should think about moving to a different inn.")
                chatNpc(
                    angry,
                    "Bah! I'm not letting it defeat me. Now, let me concentrate on my cooking.",
                )
                chatPlayer(neutral, "Okay.")
            }
            3 -> {
                chatPlayer(quiz, "Can you tell me any good jokes?")
                chatNpc(neutral, "Ohhh, if you insist...")
                chatNpc(happy, "What did the half-wit say to the cook?")
                chatPlayer(confused, "I don't know.")
                val pronoun = if (access.isBodyTypeA()) "He" else "She"
                chatNpc(laugh, "$pronoun said 'Can you tell me any good jokes?' Got ya! Hahahaha!")
                chatPlayer(bored, "Whatever.")
            }
        }
    }
}
