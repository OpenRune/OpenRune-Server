package org.rsmod.content.other.pets.cats

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.minutesAsText
import org.rsmod.content.other.pets.ticksToMinutes
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@Singleton
class CatDialogue @Inject constructor(private val care: CatCare) {
    fun catspeak(player: Player): Boolean =
        CATSPEAK_AMULET in player.worn ||
            QuestRequirements.isOnQuest(player, DRAGON_SLAYER_2) ||
            QuestRequirements.hasCompleted(player, DRAGON_SLAYER_2)

    suspend fun talk(access: ProtectedAccess, npc: Npc, cat: CatForm) {
        val player = access.player
        if (!catspeak(player)) {
            access.startDialogue(npc) {
                if (cat.stage == CatStage.Kitten) {
                    chatPlayer(happy, "Hey kitty. What's new?")
                    chatNpc(happy, "Meow!")
                } else {
                    chatPlayer(happy, "Hey puss! Any news?")
                    chatNpc(happy, "Purr!")
                }
            }
            return
        }
        if (CAT_EARS in player.worn) {
            access.startDialogue(npc) { catEars() }
            return
        }
        access.startDialogue(npc) {
            val option =
                choice4(
                    "How are you doing?",
                    1,
                    "How old are you now?",
                    2,
                    "Where do you want to go?",
                    3,
                    "What do you want to do now?",
                    4,
                    title = "Select an Option",
                )
            when (cat.stage) {
                CatStage.Kitten -> if (cat.isHell) hellKitten(option) else kitten(option)
                CatStage.Cat -> if (cat.isHell) hellCat(option) else cat(option)
                CatStage.Overgrown -> if (cat.isHell) overgrownHellCat(option) else overgrownCat(option)
                CatStage.Wily -> if (cat.isHell) wilyHellCat(option) else wilyCat(option)
                CatStage.Lazy -> if (cat.isHell) lazyHellCat(option) else lazyCat(option)
                else -> Unit
            }
        }
    }

    private suspend fun Dialogue.catEars() {
        chatNpc(quiz, "Sorry human, but what are you wearing?")
        chatPlayer(happy, "I got some cat ears made for me, don't they look meeeeeeeooooowvalous.")
        mesbox("The cat looks you dead in the eye, unblinking.")
        chatNpc(angry, "Am I a joke to you?")
        chatPlayer(confused, "UwU?")
        chatNpc(angry, "Why are you doing this?")
        chatPlayer(neutral, "I thought it'd be cute and funny.")
        chatNpc(angry, "It's not, take them off now human, before I get myself a pair of human ears to wear.")
    }

    private suspend fun Dialogue.kitten(option: Int) {
        when (option) {
            1 -> {
                chatPlayer(quiz, "How are you doing?")
                when {
                    care.isVeryHungry(player) -> chatNpc(sad, "I'm really hungry, do you have any fish, meeaow?")
                    care.isLonely(player) ->
                        chatNpc(sad, "You don't think I'm cute and cuddly any more, do you?")
                    else -> chatNpc(happy, "Meeow I'm happy.")
                }
            }
            2 -> {
                chatPlayer(quiz, "How old are you now?")
                chatNpc(happy, kittenAge())
                mesbox(ageBox("Age of your kitten"))
            }
            3 -> {
                chatPlayer(quiz, "Where do you want to go?")
                when (access.random.of(3)) {
                    0 -> chatNpc(happy, "Can we go visit Gertrude, I'd like to see her again, she's very nice.")
                    1 -> chatNpc(happy, "Let's go to the ratpits in Ardougne. I think I fancy a fight!")
                    else -> chatNpc(happy, "Lets go to the sewers of Varrock.")
                }
            }
            4 -> {
                chatPlayer(quiz, "What do you want to do now?")
                when (access.random.of(2)) {
                    0 -> chatNpc(happy, "I want to chase things.")
                    else -> chatNpc(bored, "I'm tired, I'd like to have a nap.")
                }
            }
        }
    }

    private suspend fun Dialogue.hellKitten(option: Int) {
        when (option) {
            1 -> {
                chatPlayer(quiz, "How are you doing?")
                when {
                    care.isVeryHungry(player) -> {
                        chatNpc(sad, "Hungry. I wound like....")
                        chatNpc(angry, "One meeeeeellion tuna!")
                        chatPlayer(laugh, "One meeeeeelion tuna? Don't be daft. No, I'll stick to pie thanks.")
                        chatNpc(quiz, "Rat pie?")
                        chatPlayer(shocked, "Yuck! I'd rather eat rock soup with the Trolls!")
                    }
                    care.isLonely(player) -> {
                        chatNpc(sad, "I've got this horrible itch behind my ears. Would you scratch it?")
                        chatPlayer(happy, "Ahhh, how sweet. Kitty want a cuddle?")
                        chatNpc(
                            angry,
                            "Just because I look big and tough doesn't mean I can't let my guard down " +
                                "once in a while? No fair!",
                        )
                        chatPlayer(laugh, "Ahhh, I new you were a big softie.")
                        chatNpc(angry, "Don't push your luck no-fur.")
                    }
                    else -> chatNpc(happy, "Great. Ready to take on another of those pesky rats!")
                }
            }
            2 -> {
                chatPlayer(quiz, "How old are you now?")
                chatNpc(happy, kittenAge())
                mesbox(ageBox("Age of your kitten"))
            }
            3 -> {
                chatPlayer(quiz, "Where do you want to go?")
                when (access.random.of(3)) {
                    0 -> chatNpc(shifty, "Let's go steal some things from stalls. I do enjoy watching shopkeepers panic!")
                    1 -> chatNpc(laugh, "Can we go tripping people up as they climb the stairs? It's my favorite.")
                    else -> chatNpc(happy, "Can we go and tear up some furniture? I love that!")
                }
            }
            4 -> {
                chatPlayer(quiz, "What do you want to do now?")
                when (access.random.of(2)) {
                    0 -> chatNpc(angry, "I need to crunch something - lets go ratting!")
                    else -> chatNpc(bored, "Yawn. I need sleep.")
                }
            }
        }
    }

    private suspend fun Dialogue.cat(option: Int) {
        when (option) {
            1 -> {
                chatPlayer(quiz, "How are you doing?")
                if (player.catRatsCaught >= MEDAL_RATS) {
                    chatNpc(happy, "I'm good. But could we go adventuring soon, I'm tired of talking, meeoow?")
                } else {
                    chatNpc(
                        happy,
                        "Good but could we go hunting again soon? One of my friends got a medal from Gertrude " +
                            "for catching lots of mice.",
                    )
                }
            }
            2 -> {
                chatPlayer(quiz, "How old are you now?")
                if (care.nearlyOvergrown(player)) {
                    chatNpc(
                        worried,
                        "I'm not as young as I used be, I think I'm beginning to get a bit fat too. Perhaps I " +
                            "should chase more mice to stay fit.",
                    )
                } else {
                    chatNpc(happy, "I'm not too old, and not too young. In fact I think I'm just right.")
                }
            }
            3 -> {
                chatPlayer(quiz, "Where do you want to go?")
                chatNpc(happy, "Can we go to Varrock sewers and chase some rats?")
            }
            4 -> {
                chatPlayer(quiz, "What do you want to do now?")
                chatNpc(happy, "I want to go chase things kill them and then eat them, purrr.")
            }
        }
    }

    private suspend fun Dialogue.hellCat(option: Int) {
        when (option) {
            1 -> {
                chatPlayer(quiz, "How are you doing?")
                chatNpc(angry, "Big, mean, and ready to rumble. Just the way I like it.")
            }
            2 -> {
                chatPlayer(quiz, "How old are you now?")
                if (care.nearlyOvergrown(player)) {
                    chatNpc(
                        bored,
                        "I'm feeling a bit like one of those dog things, all fat and slow. I'd love to do " +
                            "some rat catching.",
                    )
                } else {
                    chatNpc(happy, "Practically perfect in every way. Ready for world domination.")
                    chatPlayer(laugh, "Easy there tiger.")
                }
            }
            3 -> {
                chatPlayer(quiz, "Where do you want to go?")
                chatNpc(laugh, "I fancy pointing and laughing at some demons. They always make me cackle.")
                chatPlayer(quiz, "Why's that?")
                chatNpc(neutral, "Well, have you never noticed how well ghouls and demons get on?")
                chatPlayer(neutral, "No... Can't say I have.")
                chatNpc(laugh, "Yeah... Demons are a ghoul's best friend.")
                chatPlayer(confused, "Hmm. I'm not sure I understand that. Is it some kind of joke?")
                chatNpc(bored, "Sigh. I think you're the only joke round here, no-fur.")
            }
            4 -> {
                chatPlayer(quiz, "What do you want to do now?")
                chatNpc(angry, "Prey on the weak and injured. Let's try the wilderness!")
            }
        }
    }

    private suspend fun Dialogue.overgrownCat(option: Int) {
        when (option) {
            1 -> {
                chatPlayer(quiz, "How are you doing?")
                chatNpc(bored, "If you don't stop talking to me, I'll never squeeze in my slouching time for the day.")
            }
            2 -> {
                chatPlayer(quiz, "How old are you now?")
                chatNpc(
                    angry,
                    "I'm old and fat if that's what you're driving at. What, are you thinking of trading me " +
                        "in for a younger model?",
                )
                chatNpc(angry, "After all the adventuring we've done together? Is that it?")
                chatPlayer(worried, "No, nothing of the sort. I was just making conversation, that's all.")
                chatNpc(shifty, "Sure, I know your game. Going to trade me in for some death runes are you? Eh?")
                chatPlayer(shocked, "What? How do you know about that?")
                chatNpc(neutral, "We cats talk, you know.")
            }
            3 -> {
                chatPlayer(quiz, "Where do you want to go?")
                chatNpc(happy, "I'd just like to go somewhere nice and warm.")
            }
            4 -> {
                chatPlayer(quiz, "What do you want to do now?")
                chatNpc(bored, "I want to have a snooze, all this talking has made me sleepy.")
                chatPlayer(bored, "Gosh, you're a bit dull and boring.")
                chatNpc(neutral, "I suppose that's what you become once you pass your 10th birthday.")
            }
        }
    }

    private suspend fun Dialogue.overgrownHellCat(option: Int) {
        when (option) {
            1 -> {
                chatPlayer(quiz, "How are you doing?")
                chatNpc(neutral, "Frankly, I could do with it being a bit hotter round here, but I'll survive.")
                chatPlayer(quiz, "Why's that then?")
                chatNpc(
                    neutral,
                    "Those of us more in touch with our past tend to like it warmer. It's the legacy of the " +
                        "Felis Silvestris Lybica, or desert cat as you would say.",
                )
                chatPlayer(quiz, "Cats? In the desert?")
                chatNpc(
                    neutral,
                    "Oh yes. Many of us left due to the harsh conditions. Sand is the worst thing to lick " +
                        "yourself clean from.",
                )
                chatPlayer(happy, "I'll bet!")
            }
            2 -> {
                chatPlayer(quiz, "How old are you now?")
                chatNpc(neutral, "Age is irrelevant. Time is irrelevant.")
                chatPlayer(confused, "Erm, no, I mean, how are you doing?")
                chatNpc(shifty, "Look into my eyes...")
                chatPlayer(
                    neutral,
                    "...my belly is full from the crunching of bones you leave behind, and my paws ache.",
                )
                chatPlayer(shocked, "Huh? Who said that?")
                chatNpc(laugh, "I did, via you. Shall I do it again?")
            }
            3 -> {
                chatPlayer(quiz, "Where do you want to go?")
                chatNpc(
                    angry,
                    "Straight down the throat of destruction, grasping the tonsil of death as we swing " +
                        "into the belly of evil!",
                )
                chatPlayer(quiz, "I see. Is that located anywhere near Lumbridge?")
                chatNpc(neutral, "Only in your nightmares, no-fur.")
                chatPlayer(neutral, "I do have a name you know.")
                chatNpc(neutral, "And so do I. But you didn't stop to ask before naming me yourself, did you?")
                chatPlayer(neutral, "Touche my friend, touche.")
            }
            4 -> {
                chatPlayer(quiz, "What do you want to do now?")
                chatNpc(happy, "I'd love to crunch up some rats. Mmm... rats.")
                chatPlayer(quiz, "Hey, why do rats need oiling?")
                chatNpc(quiz, "Oiling? I have no idea.")
                chatPlayer(laugh, "Because they squeak. Get it? They squeak!")
                chatNpc(bored, "Wow. That's so funny I almost laughed. I guess I'll have to eat something else then.")
                chatPlayer(worried, "I don't like the way you're looking at me. We'll go get some rats soon, I promise.")
                chatNpc(neutral, "Good answer, no-fur.")
            }
        }
    }

    private suspend fun Dialogue.wilyCat(option: Int) {
        when (option) {
            1 -> {
                chatPlayer(quiz, "How are you doing?")
                chatNpc(happy, "Content. But a little more attention or rat hunting wouldn't go amiss.")
                chatPlayer(neutral, "I'll see what I can do.")
            }
            2 -> {
                chatPlayer(quiz, "How old are you now?")
                chatNpc(happy, "I'm getting on a bit, but I feel like there's plenty of life in me yet.")
            }
            3 -> {
                chatPlayer(quiz, "Where do you want to go?")
                chatNpc(happy, "Let's go hunting in Lumbridge.")
            }
            4 -> {
                chatPlayer(quiz, "What do you want to do now?")
                chatNpc(happy, "Hunting! Meow! Yes, hunting is good.")
                chatPlayer(quiz, "Is there anywhere in particular that you would like to go?")
                chatNpc(happy, "Let's go hunting in Lumbridge.")
            }
        }
    }

    private suspend fun Dialogue.wilyHellCat(option: Int) {
        when (option) {
            1 -> {
                chatPlayer(quiz, "How are you doing?")
                chatNpc(happy, "Happy as a demon in a lava pit.")
                chatPlayer(worried, "Good... I think.")
            }
            2 -> {
                chatPlayer(quiz, "How old are you now?")
                chatNpc(neutral, "I'm feeling a bit like a ghost in a cake shop.")
                chatPlayer(quiz, "A ghost in a cake shop? What do you mean?")
                chatNpc(laugh, "You know... In need of exorcise.")
                chatPlayer(bored, "Oh, now that ones really bad. Guthix save me!")
            }
            3 -> {
                chatPlayer(quiz, "Where do you want to go?")
                chatNpc(happy, "Let's go to the sewers of Varrock.")
            }
            4 -> {
                chatPlayer(quiz, "What do you want to do now?")
                chatNpc(angry, "Hunting!")
                chatPlayer(quiz, "Is there anywhere in particular that you would like to go?")
                chatNpc(happy, "Let's go to the sewers of Varrock.")
            }
        }
    }

    private suspend fun Dialogue.lazyCat(option: Int) {
        when (option) {
            1 -> {
                chatPlayer(quiz, "How are you doing?")
                chatNpc(
                    bored,
                    "I'm content. A little more action might be good, but then again you might ruffle up " +
                        "my fur and then I'd have to groom all over again.",
                )
                chatPlayer(bored, "Pah! You're the laziest old bag of fluff I've ever met.")
            }
            2 -> {
                chatPlayer(quiz, "How old are you now?")
                chatNpc(neutral, "A little older than I was before, but not as old as I will be in a moment.")
                chatPlayer(confused, "What kind of answer is that?")
                chatNpc(
                    neutral,
                    "Well, your idea of time and mine are too different for either of us to understand the other.",
                )
                chatPlayer(confused, "Um... I don't really get it.")
                chatNpc(
                    neutral,
                    "Well for one, I don't count the time I spend in your Bank - which is horribly boring - " +
                        "nor in your bag.",
                )
                chatPlayer(neutral, "Well that's not too complicated.")
                chatNpc(
                    neutral,
                    "I'm just getting started. Now there is also a difference in cat years and human years. " +
                        "To approximate time in cat years you take a value in years, subtract 1, multiply " +
                        "it by 4, and then add 16.",
                )
                chatPlayer(quiz, "So... if a cat is newly born it's already 12 cat years old?")
                chatNpc(neutral, "I said approximate not calculate. Anyway it only works once the cat is 1 year old.")
                chatNpc(neutral, "The last complication is that time doesn't actually seem to move, outside of cats ageing.")
                chatPlayer(quiz, "Anyway, back to the question. How old are you in cat years?")
                chatNpc(bored, "I don't know. I'm a cat, I can't do maths!")
                chatPlayer(angry, "Hmph!")
            }
            3 -> {
                chatPlayer(quiz, "Where do you want to go?")
                chatNpc(bored, "Somewhere nice and quiet. I'm a bit behind in my snoozing schedule.")
            }
            4 -> {
                chatPlayer(quiz, "What do you want to do now?")
                chatNpc(bored, "Sleep.")
                chatPlayer(quiz, "How am I going to get you back in shape if all you want to do is sleep?")
                chatNpc(neutral, "Surgery. That's the answer to all our problems.")
                chatPlayer(
                    quiz,
                    "Well smarty pants, where am I going to find a cosmetic surgeon who's willing to " +
                        "perform liposuction on a cat around here?",
                )
                chatNpc(neutral, "I don't know. To be honest I don't even know what those things are.")
                chatPlayer(confused, "Me neither, actually...")
            }
        }
    }

    private suspend fun Dialogue.lazyHellCat(option: Int) {
        when (option) {
            1 -> {
                chatPlayer(quiz, "How are you doing?")
                chatNpc(happy, "Curling up on a nice rug in front of a fire... or in the fire. Either way is good.")
                chatPlayer(confused, "You're a bit odd, really, aren't you?")
            }
            2 -> {
                chatPlayer(quiz, "How old are you now?")
                chatNpc(neutral, "Old, but not too old. I could still scratch that cheeky face of yours.")
                chatPlayer(laugh, "Awww, poor old kitty. I suppose all this were just fields when you were a nipper!")
                chatNpc(angry, "Most of it still is, you young whippersnapper!")
            }
            3 -> {
                chatPlayer(quiz, "Where do you want to go?")
                chatNpc(bored, "Somewhere brutal and nasty. I could do with a snooze, you see.")
            }
            4 -> {
                chatPlayer(quiz, "What do you want to do now?")
                chatNpc(laugh, "Let's go point and laugh at people getting killed by chickens.")
                chatPlayer(confused, "Nobody's ever died from chickens. What are you talking about?")
                chatNpc(happy, "Oh yes they have. Have you never met the evil chicken? He's my hero!")
                chatPlayer(worried, "Cripes! I forgot about that thing.")
                chatNpc(shocked, "Look, there he is now!")
                chatPlayer(shocked, "Argh! Where?")
                chatNpc(laugh, "Humans are too gullible these days. Heee heee.")
                chatPlayer(angry, "Grrr!! I'll turn you into violin strings yet, kitty!")
            }
        }
    }

    suspend fun stroke(access: ProtectedAccess, npc: Npc, cat: CatForm) {
        access.mes("You softly stroke your cat.")
        npc.say("Purr...purr...")
        val catspeak = catspeak(access.player)
        access.startDialogue(npc) {
            when (cat.stage) {
                CatStage.Kitten ->
                    when {
                        !catspeak -> chatPlayer(happy, "That cat sure loves to be stroked.")
                        cat.isHell -> {
                            chatPlayer(happy, "Who's a good little evil cat then?")
                            chatNpc(happy, "Me! Me! Find me some fish heads to crunch! Yum yum.")
                        }
                        else -> {
                            chatPlayer(happy, "Who's Gielinor's most ferocious kitten?")
                            chatNpc(happy, "Me! Me! <col=0000ff>~ Roar!</col>")
                            chatPlayer(happy, "Aww! So squeaky!")
                        }
                    }
                CatStage.Cat ->
                    when {
                        !catspeak -> Unit
                        cat.isHell -> {
                            chatPlayer(happy, "Who's an evil little kitty then?")
                            chatNpc(happy, "Hiss Hiss! Me! Do it again!")
                        }
                        else -> {
                            chatPlayer(happy, "Who's a good cat then?")
                            chatNpc(happy, "Me! Me! Scratch me behind the ears.")
                        }
                    }
                CatStage.Overgrown ->
                    when {
                        !catspeak -> Unit
                        cat.isHell -> {
                            chatPlayer(happy, "Hey there fat cat! Fancy a stroke?")
                            chatNpc(bored, "Only because I know it amuses you. I couldn't care less... mmmmmmmm.")
                        }
                        else -> {
                            chatPlayer(happy, "Hey there fatty! Fancy a tummy rub?")
                            chatNpc(happy, "Purr, nice master.")
                        }
                    }
                else -> Unit
            }
        }
    }

    suspend fun guessAge(access: ProtectedAccess, npc: Npc) {
        access.startDialogue(npc) {
            chatPlayer(quiz, "I wonder how old you are...")
            mesbox(ageBox("After taking a good look at your kitten you guess that its age is"))
        }
    }

    suspend fun shoo(access: ProtectedAccess, npc: Npc, cat: CatForm): Boolean {
        var confirm = false
        access.startDialogue(npc) {
            confirm = choice2("Yes I am.", true, "No I'm not.", false, title = "Are you sure you want to shoo away the cat?")
            if (!confirm || !catspeak(player)) {
                return@startDialogue
            }
            when (cat.stage) {
                CatStage.Kitten ->
                    if (cat.isHell) {
                        chatPlayer(angry, "I've had enough of your whiny attitude cat. Now get lost!")
                        chatNpc(
                            angry,
                            "Me? Whiny? Well, if that isn't the demon calling the imp red! I'll hunt you " +
                                "down when I'm bigger and you'll be sorry!",
                        )
                        chatPlayer(worried, "I'm sorry... all is forgiven!")
                        chatNpc(
                            angry,
                            "It's too late for you, no-fur. I hope you get your sword stuck and a dragon " +
                                "falls on your head. Now shoo!",
                        )
                    } else {
                        chatPlayer(
                            angry,
                            "I'm just fed up with you, you're so needy! Can't you just look after yourself for a bit?",
                        )
                        chatNpc(
                            sad,
                            "Meeeeow, don't leave me master, I'm sorry, I'll be a good kitty from now on meoooww.",
                        )
                        chatPlayer(angry, "No I've really had it with you, now scat!")
                    }
                CatStage.Cat ->
                    if (cat.isHell) {
                        chatPlayer(angry, "Go on, get lost.")
                        chatNpc(angry, "Thank Zamorak for that. Finally some freedom from your ugly face!")
                    } else {
                        chatPlayer(angry, "Go on, get lost.")
                        chatNpc(
                            angry,
                            "I was going anyway, hiss. You're probably the worst adventurer ever, I bet " +
                                "you'll be killed by a chicken before too long, hisss!",
                        )
                    }
                CatStage.Overgrown ->
                    if (cat.isHell) {
                        chatPlayer(angry, "I've had just about all I can take from you and your bad attitude. Now scat, cat!")
                        chatNpc(
                            bored,
                            "You bore me. I had my things packed weeks ago anyway, I was just hanging about " +
                                "to see how many little presents I could leave in your Bank.",
                        )
                        chatPlayer(angry, "If I find you've messed things up in there I'll...")
                        chatNpc(
                            laugh,
                            "You'll what? Come on now, let's be honest. You couldn't fight your way out of " +
                                "a cow field. I'm off!!",
                        )
                    } else {
                        chatPlayer(
                            angry,
                            "I've had enough of you and your whining, all you do is mope around all day. Just " +
                                "go away and don't come back.",
                        )
                        chatNpc(
                            angry,
                            "I'm getting tired of your company anyway! You must be the most boring adventurer " +
                                "ever. You couldn't find your way to Lumbridge without me.",
                        )
                    }
                CatStage.Wily ->
                    if (cat.isHell) {
                        chatPlayer(angry, "You're too old. I'm upgrading to a newer model!")
                        chatNpc(bored, "Yes, me too. Have fun with whatever rock you crawl under.")
                    } else {
                        chatPlayer(
                            angry,
                            "Hit the road cat. I've had enough of you. All you ever want to do is go hunting rats. Gah!",
                        )
                        chatNpc(angry, "Pah! You were no fun anyway.")
                    }
                CatStage.Lazy ->
                    if (!cat.isHell) {
                        chatPlayer(angry, "That's it, scram, go away and don't come back, you big sack of laziness.")
                        chatNpc(
                            angry,
                            "Hiss! I can't believe I wasted so much of my time with you. Pah! Next time I'll " +
                                "find a proper adventurer as a companion.",
                        )
                    }
                else -> Unit
            }
        }
        return confirm
    }

    suspend fun cryptRat(access: ProtectedAccess, npc: Npc, cat: CatForm) {
        val catspeak = catspeak(access.player)
        if (!catspeak) {
            access.player.say("Go on puss...kill that rat!")
            npc.say("Meeeeeoooowww!")
            access.mes("Your cat doesn't seem to like the look of these rats.")
            return
        }
        access.startDialogue(npc) {
            when (cat.stage) {
                CatStage.Kitten -> {
                    chatPlayer(quiz, "Do you fancy a bit of hunting?")
                    chatNpc(
                        worried,
                        "Meoowww? The nasty rat is too big for me, I might be able to take it when I grow up " +
                            "a little more.",
                    )
                }
                CatStage.Overgrown -> {
                    chatPlayer(quiz, "Are you up for some rodent catching?")
                    chatNpc(
                        bored,
                        "If you mean by rodent catching, getting myself killed by attacking that there rat,.... no.",
                    )
                    chatPlayer(happy, "Oh go on, I'm sure you'd take it.")
                    chatNpc(bored, "Nah, I've just groomed.")
                }
                CatStage.Lazy -> {
                    chatPlayer(quiz, "Are you up for some rodent catching?")
                    chatNpc(bored, "Nah, what's the point?")
                    chatPlayer(happy, "Come on lazy boots, crack on!")
                    chatNpc(
                        angry,
                        "No way! It's not that I'm lazy, no no I'll rephrase that, laziness aside I'm still " +
                            "not going to do that.",
                    )
                    chatPlayer(quiz, "Why not?")
                    chatNpc(
                        neutral,
                        "I'm not some crazy kamikazee cat, I want get old and live a quiet peacful life, " +
                            "close to a nice warm fire.",
                    )
                    chatPlayer(bored, "Pah you're just a big scardy cat.")
                }
                CatStage.Cat, CatStage.Wily -> {
                    chatPlayer(happy, "Go on get that nasty rodent.")
                    chatNpc(
                        angry,
                        "What? You're having a laugh I'm not going to get myself killed for you. Why don't " +
                            "you attack it?",
                    )
                    chatPlayer(laugh, "Chicken!")
                    chatNpc(angry, "No stupid, I'm a cat.")
                }
                else -> Unit
            }
        }
    }

    private fun Dialogue.kittenAge(): String {
        val minutes = care.ageTicks(player).ticksToMinutes()
        val untilAdult = care.ticksUntilAdult(player).ticksToMinutes()
        return when {
            minutes < 15 -> "I'm less than a month old."
            minutes < 30 -> "I'm just a month old."
            untilAdult < 15 -> "I'm almost fully grown!"
            else -> "I'm ${minutes / 15} months old."
        }
    }

    private fun Dialogue.ageBox(prefix: String): String {
        val age = care.ageTicks(player).ticksToMinutes()
        val left = care.ticksUntilAdult(player).ticksToMinutes()
        return "$prefix: ${age.minutesAsText()}. Approximate time until fully adult: " +
            "${left.minutesAsText()}, assuming you look after it."
    }

    private companion object {
        const val CATSPEAK_AMULET = "obj.ics_little_amulet_of_catspeak"
        const val CAT_EARS = "obj.osb7_cat_ears"
        const val DRAGON_SLAYER_2 = "quest_dragonslayer2"
        const val MEDAL_RATS = 100
    }
}
