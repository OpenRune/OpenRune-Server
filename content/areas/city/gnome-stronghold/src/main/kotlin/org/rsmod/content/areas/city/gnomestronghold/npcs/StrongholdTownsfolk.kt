package org.rsmod.content.areas.city.gnomestronghold.npcs

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:gnome_restaurant] delivery hand-in dialogue for every customer here
// TODO(content): [system:treasure_trails] Ermin, Guard Vemmeldo and gnome trainer clue steps
class StrongholdTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.aluft_gnomeknight") { startDialogue(it.npc) { burkor() } }
        onOpNpc1("npc.aluft_gnome_cheerleader") { startDialogue(it.npc) { damwin() } }
        onOpNpc1("npc.aluft_customer_spinner") { startDialogue(it.npc) { ermin() } }
        onOpNpc1(DALILA) { startDialogue(it.npc) { anniversaryDinner() } }
        onOpNpc1(PERRDUR) { startDialogue(it.npc) { anniversaryDinner() } }
        onOpNpc1("npc.aluft_gnome_delivery_three") { startDialogue(it.npc) { spang() } }
        onOpNpc1("npc.aluft_gnome_delivery_two") { startDialogue(it.npc) { tamble() } }
        onOpNpc1("npc.aluft_gnome_delivery_one") { startDialogue(it.npc) { timble() } }
        onOpNpc1("npc.aluft_gnome_terrorbird_trainer") { startDialogue(it.npc) { meegle() } }
        onOpNpc1("npc.aluft_toad_handler") { startDialogue(it.npc) { sarble() } }
        onOpNpc1("npc.aluft_gnomeball_fan") { startDialogue(it.npc) { wurbel() } }
        onOpNpc1("npc.gnome_bank_guard") { startDialogue(it.npc) { guardVemmeldo() } }
        onOpNpc1("npc.gnome_tortoise_trainer") { startDialogue(it.npc) { nacklepen() } }
        onOpNpc1("npc.gnometrainer") { startDialogue(it.npc) { gnomeTrainer() } }
        onOpNpc1("npc.aluft_customer_male") { startDialogue(it.npc) { eebel() } }
        onOpNpc1("npc.aluft_gnome_child") { startDialogue(it.npc) { froono() } }
        onOpNpc1("npc.grandtree_femi") { startDialogue(it.npc) { femi() } }
        onOpNpc1("npc.grandtree_glough") { startDialogue(it.npc) { glough() } }
        onOpNpc1("npc.grandtree_anita") { startDialogue(it.npc) { anita() } }
        onOpNpc1("npc.mm_daero") { startDialogue(it.npc) { daero() } }
        onOpNpc1("npc.zep_assist_gno") { startDialogue(it.npc) { assistantLeSmith() } }
        onOpNpc1("npc.zep_assist_gno_replacement_vis") {
            startDialogue(it.npc) { assistantLori() }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Burkor
    private suspend fun Dialogue.burkor() {
        chatPlayer(happy, "Hello.")
        chatNpc(neutral, "Be vigilant, citizen.")
        chatPlayer(confused, "Err, right.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Damwin
    private suspend fun Dialogue.damwin() {
        chatNpc(happy, "Hiya! Do you want to buy me a drink?")
        chatPlayer(neutral, "I think I'll pass on that.")
        chatNpc(angry, "Fine, I'm sure somebody else round here will know how to treat a woman...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ermin
    private suspend fun Dialogue.ermin() {
        chatNpc(confused, "Ooh, I'm all dizzy...")
        chatPlayer(quiz, "Why's that?")
        chatNpc(confused, "Well, watching that wheel go round and round and round...")
        chatNpc(confused, "and round and round and round...")
        chatPlayer(neutral, "Looks like you need a bit of a rest.")
        chatNpc(confused, "and round and round and round again...")
        chatPlayer(laugh, "See you around!")
        chatNpc(angry, "Very funny.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dalila
    // https://oldschool.runescape.wiki/w/Transcript:Perrdur
    private suspend fun Dialogue.anniversaryDinner() {
        dalila(happy, "Ooh, lovely view, dear.")
        perrdur(neutral, "Yes dear.")
        dalila(neutral, "I hope the food's better here than it was last time.")
        perrdur(neutral, "I'm sure it will be, dear.")
        chatPlayer(quiz, "Sorry to disturb you, but what are you two doing here?")
        dalila(happy, "Perrdur and I are having a nice romantic dinner, aren't we dear?")
        dalila(happy, "We are celebrating the anniversary of our marriage, aren't we dear?")
        perrdur(neutral, "Yes dear.")
        dalila(happy, "And we are having a very enjoyable time, aren't we dear?")
        perrdur(neutral, "Yes dear.")
        chatPlayer(laugh, "Clearly. I'll let you two lovebirds carry on, then.")
    }

    private suspend fun Dialogue.dalila(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Dalila", DALILA, mesanim, text)

    private suspend fun Dialogue.perrdur(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Perrdur", PERRDUR, mesanim, text)

    // https://oldschool.runescape.wiki/w/Transcript:Spang
    private suspend fun Dialogue.spang() {
        chatNpc(
            sad,
            "I hate this job. All the other staff are so happy all the time. It depresses me.",
        )
        chatPlayer(quiz, "Why don't you get a new job?")
        chatNpc(
            sad,
            "I can't find any other work. I wanted to be a Gnomeball player but I wasn't big " +
                "enough.",
        )
        chatPlayer(neutral, "That's a shame. I'd better let you get on with your work.")
        chatNpc(bored, "OK, whatever.")
        chatPlayer(quiz, "Aren't you going to wish me a nice day?")
        chatNpc(bored, "Have a day. It can be nice if you can be bothered. I know I can't.")
        chatPlayer(confused, "... nice.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tamble
    private suspend fun Dialogue.tamble() {
        chatNpc(happy, "Busy Busy Busy.")
        chatPlayer(quiz, "Why are you so busy?")
        chatNpc(
            happy,
            "Aluft Aloft is a bustling new business, here in the Gnome restaurant. We're " +
                "getting food orders all the time.",
        )
        chatPlayer(quiz, "Do you have any jobs then?")
        chatNpc(happy, "If you want to get a job, talk to Aluft Gianne jnr. I'm sure he has plenty.")
        chatNpc(happy, "Have a nice day.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Timble
    private suspend fun Dialogue.timble() {
        chatNpc(happy, "Good Day, how can I help you?")
        chatPlayer(quiz, "I was just wondering what you were up to.")
        chatNpc(
            happy,
            "I work for Aluft Aloft Gnome Food Delivery Service. It's a growing business you " +
                "know. We get more customers every day!",
        )
        chatPlayer(quiz, "So do you have any spare jobs then?")
        chatNpc(happy, "Aluft Gianne jnr. is in charge - he'll be able to help you.")
        chatPlayer(happy, "Thanks.")
        chatNpc(happy, "Have a nice day.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Meegle
    private suspend fun Dialogue.meegle() {
        chatPlayer(quiz, "So what do you do?")
        chatNpc(happy, "I breed Terrorbirds.")
        chatPlayer(quiz, "Why are they called Terrorbirds?")
        chatNpc(
            happy,
            "Well, they're supposed to look terrifying but I think they are rather cute.",
        )
        chatPlayer(worried, "I'm sure they are cute, unless they are biting your arm off.")
        chatNpc(laugh, "Oh they'd never do that - unless I tell them to, of course.")
        chatPlayer(worried, "I'd better stay on your good side then!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sarble
    private suspend fun Dialogue.sarble() {
        mesbox("This gnome lady appears to be singing ... to her toads.")
        chatNpc(happy, "La La La, toadies in their holeys! .. Oh hello, I didn't notice you.")
        chatPlayer(quiz, "Hello, What do you do here?")
        chatNpc(
            happy,
            "I look after the toads and worms. They are very useful in gnome cooking, don't " +
                "you know.",
        )
        chatPlayer(quiz, "Does singing to the toads help them grow?")
        chatNpc(laugh, "Don't be silly, I just sing to pass the time while looking after them!")
        chatPlayer(confused, "Of course...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Wurbel
    private suspend fun Dialogue.wurbel() {
        chatNpc(
            angry,
            "Gah! You play like a donkey, don't just sit there, get up and get the ball back!",
        )
        chatPlayer(quiz, "Hello?")
        chatNpc(happy, "That's more like it, get in there!")
        chatPlayer(quiz, "Excuse me?")
        chatNpc(shocked, "Oof. That's got to hurt.")
        mesbox(
            "You think this guy is far too engrossed in the sport to pay any attention to " +
                "anything else."
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_Vemmeldo
    private suspend fun Dialogue.guardVemmeldo() {
        chatPlayer(quiz, "Good day. What are you up to?")
        chatNpc(
            neutral,
            "My job is to ensure that everyone can use the bank safely. Shifty types always " +
                "hang around banks, you know.",
        )
        chatNpc(shifty, "You're not shifty, are you?")
        chatPlayer(neutral, "No, of course not.")
        chatNpc(happy, "Jolly good. Carry on then.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Trainer_Nacklepen
    private suspend fun Dialogue.nacklepen() {
        chatPlayer(quiz, "Who are you then?")
        chatNpc(neutral, "Hurumph.")
        chatNpc(neutral, "Nacklepen, tortoise trainer extraordinaire.")
        chatPlayer(quiz, "What's extraordinary about you?")
        chatNpc(neutral, "Do you know any other tortoise trainers?")
        chatPlayer(neutral, "Well, no.")
        chatNpc(neutral, "Well there you go then.")
        var option =
            choice4(
                "So what are you training these tortoises to do?",
                1,
                "Why tortoises?",
                2,
                "Where did these tortoises come from?",
                3,
                "I've heard enough.",
                4,
            )
        while (true) {
            when (option) {
                1 -> nacklepenTraining()
                2 -> nacklepenWhyTortoises()
                3 -> nacklepenWhereFrom()
                else -> return
            }
            option = nacklepenOptions(exclude = option)
        }
    }

    private suspend fun Dialogue.nacklepenOptions(exclude: Int): Int {
        val options =
            listOf(
                "So what are you training these tortoises to do?" to 1,
                "Why tortoises?" to 2,
                "Where did these tortoises come from?" to 3,
                "I've heard enough." to 4,
            )
        val remaining = options.filter { it.second != exclude }
        return choice3(
            remaining[0].first,
            remaining[0].second,
            remaining[1].first,
            remaining[1].second,
            remaining[2].first,
            remaining[2].second,
        )
    }

    private suspend fun Dialogue.nacklepenTraining() {
        chatNpc(
            neutral,
            "Tortoises are naturally pacifistic animals that eat nothing but cabbages. I am " +
                "required to beat those peaceful tendencies out of the softies and turn them " +
                "into hardened battle-trained killers.",
        )
        chatNpc(
            neutral,
            "Firstly I acclimatise them to excessive aural stimulation and simple physical " +
                "confrontation.",
        )
        chatPlayer(shocked, "You shout at them and beat them with sticks?")
        chatNpc(
            neutral,
            "Of course. Then I re-educate them to become comfortable with riders and military " +
                "combat situations.",
        )
        chatPlayer(confused, "You sit on their backs and prod them with sharp pointy things.")
        chatNpc(neutral, "If you like.")
        chatPlayer(sad, "Doesn't seem very fair on the tortoises.")
        chatNpc(angry, "This is War! Fairness is a luxury of the weak.")
        chatPlayer(confused, "Hmmm, profound but disturbing.")
    }

    private suspend fun Dialogue.nacklepenWhyTortoises() {
        chatPlayer(quiz, "So why tortoises?")
        chatNpc(
            neutral,
            "Isn't it obvious? Surely even to your untrained eye that giant tortoises are the " +
                "perfect mobile armoured field unit?",
        )
        chatNpc(
            neutral,
            "They are sturdy animals, and well protected by their shell. They can even swim! " +
                "All we need to do is put a bit of fire in their bellies.",
        )
        chatPlayer(neutral, "I suppose so....")
    }

    private suspend fun Dialogue.nacklepenWhereFrom() {
        chatPlayer(quiz, "Where do these tortoises come from?")
        chatNpc(
            neutral,
            "We breed them. The originals were found by gnome explorers back in King Healthorg " +
                "the Great's rule. On their discovery King Healthorg authorised a small party " +
                "to go there and transport a breeding pair back here. He",
        )
        chatNpc(neutral, "kept one for himself to ride into battle.")
        chatNpc(
            neutral,
            "Trouble is they take a lot of skill and time to breed, so we're only just getting " +
                "them battle ready.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gnome_trainer
    // TODO(content): [system:npc_overhead] agility course shouts
    private suspend fun Dialogue.gnomeTrainer() {
        when (access.random.of(4)) {
            0 -> {
                chatPlayer(happy, "Hello how are you?")
                chatNpc(
                    angry,
                    "I'm amazed by how much humans chat. The sign over there says training " +
                        "area, not pointless conversation area.",
                )
            }
            1 -> {
                chatPlayer(quiz, "Hello, what is this place?")
                chatNpc(
                    neutral,
                    "This, my friend, is where we train. Here we improve our agility. It's an " +
                        "essential skill.",
                )
                chatPlayer(neutral, "It looks easy enough.")
                chatNpc(
                    neutral,
                    "If you complete the course in order from the slippery log to the end, " +
                        "your agility will increase much faster than by repeating just one " +
                        "obstacle.",
                )
            }
            2 -> {
                chatPlayer(happy, "This is fun!")
                chatNpc(angry, "This is training soldier. If you want fun go make some cocktails.")
            }
            else -> {
                chatPlayer(happy, "Hello there.")
                chatNpc(
                    angry,
                    "This isn't a grannies' tea party, let's see some sweat human. Go! Go! Go!",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Eebel
    private suspend fun Dialogue.eebel() {
        chatPlayer(quiz, "Hello?")
        chatNpc(happy, "Hello there. Can I do anything for you?")
        chatPlayer(neutral, "No I'm fine, I'm just wandering around.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Froono
    private suspend fun Dialogue.froono() {
        chatNpc(happy, "Pretty Trees!")
        chatPlayer(quiz, "Having fun?")
        chatNpc(happy, "I'm just looking at the magic trees. I think they're cool.")
        chatPlayer(neutral, "Well, they are magical. Valuable as well...")
        chatNpc(
            happy,
            "When I grow up I want to chop down trees and make magic bows. Then I'd be rich.",
        )
        chatPlayer(neutral, "Well, that does seem to be a common career around here.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Femi
    // TODO(content): [quest:the_grand_tree] the box-lifting dialogue at the gate on first entry
    // TODO(content): [verify:in_game] Talk-to before the player has ever entered the Stronghold
    private suspend fun Dialogue.femi() {
        mesbox("The little gnome is too busy to talk.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Glough
    // TODO(content): [quest:the_grand_tree] quest stages and after-quest dialogue
    private suspend fun Dialogue.glough() {
        chatPlayer(happy, "Hello there!")
        chatNpc(angry, "You shouldn't be here human!")
        chatPlayer(quiz, "What do you mean?")
        chatNpc(angry, "The Gnome Stronghold is for gnomes alone!")
        chatPlayer(shocked, "Surely not!")
        chatNpc(angry, "We don't need your sort round here!")
        mesbox("He doesn't seem very nice!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Anita
    // TODO(content): [quest:the_grand_tree] Glough's key during the quest
    // TODO(content): [quest:monkey_madness_ii] after-quest dialogue about Glough
    private suspend fun Dialogue.anita() {
        mesbox("Anita is busy cleaning.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Daero
    // TODO(content): [quest:monkey_madness_i] quest stages, training programme and Crash Island
    private suspend fun Dialogue.daero() {
        mesbox("Daero seems like he is enjoying himself too much to talk.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Assistant_Le_Smith
    // TODO(content): [system:balloon_transport] "Yes." opens the balloon map
    private suspend fun Dialogue.assistantLeSmith() {
        chatNpc(
            neutral,
            "Do you want to use the balloon? Just so you know, some locations require special " +
                "logs and high Firemaking skills.",
        )
        when (choice3("Yes.", 1, "No.", 2, "Who are you?", 3)) {
            1 -> Unit
            2 -> chatPlayer(neutral, "No.")
            3 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    neutral,
                    "I am Assistant Le Smith. I used to work as a glider pilot, but they kicked " +
                        "me off.",
                )
                chatPlayer(quiz, "Why?")
                chatNpc(sad, "They said I was too full of hot air.")
                chatNpc(neutral, "Do you want to use the balloon?")
                when (choice2("Yes.", 1, "No.", 2)) {
                    1 -> Unit
                    2 -> chatPlayer(neutral, "No.")
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Assistant_Lori
    // TODO(content): [system:balloon_transport] "Yes." opens the balloon map
    private suspend fun Dialogue.assistantLori() {
        chatNpc(
            neutral,
            "Do you want to use the balloon? Just so you know, some locations require special " +
                "logs and high Firemaking skills.",
        )
        when (choice2("Yes.", 1, "No.", 2)) {
            1 -> Unit
            2 -> chatPlayer(neutral, "No.")
        }
    }

    private companion object {
        const val DALILA = "npc.aluft_gnome_customer_two"
        const val PERRDUR = "npc.aluft_gnome_customer_one"
    }
}
