package org.rsmod.content.quest.area.varrock

import dev.openrune.types.MesAnimType
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.camera.CamShake
import org.rsmod.api.player.cinematic.Cinematic
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

private var Player.julietVisible by intVarBit("varbit.romjul_juliet_visible")
private var Player.tailoredBush by intVarBit("varbit.cadavabush")

class RomeoAndJuliet @Inject constructor(private val npcRepo: NpcRepository) :
    QuestScript("quest_romeoandjuliet", "varp.rjquest", rewards {}, ItemRewardDisplay(POTION)) {

    override fun ScriptContext.init() {
        onOpNpc1("npc.romeo") { startDialogue(it.npc) { romeo() } }
        onOpNpcU("npc.romeo") {
            if (it.objType.internalName == MESSAGE && stage(player) > MESSAGE_GIVEN) {
                startDialogue(it.npc) {
                    chatNpc(
                        confused,
                        "Why are you waving that around at me as if it's something important?",
                    )
                }
            } else {
                mes("Nothing interesting happens.")
            }
        }
        onOpNpc1("npc.juliet_multi_visible") { startDialogue(it.npc) { juliet() } }
        onOpNpc1("npc.juliet") { startDialogue(it.npc) { juliet() } }
        onOpNpc1("npc.phillipa") { startDialogue(it.npc) { phillipa() } }
        onOpNpc1("npc.draul_leptoc") { startDialogue(it.npc) { draul() } }
        onOpNpc1("npc.father_lawrence") { startDialogue(it.npc) { lawrence() } }
        onOpNpc1("npc.apothecary") { startDialogue(it.npc) { apothecary() } }
        onOpNpc3("npc.apothecary") { openPotions() }
        onOpNpcU("npc.apothecary") {
            if (it.objType.internalName == BERRIES && quest.isQuestCompleted(player)) {
                startDialogue(it.npc) { chatNpc(neutral, "I don't need those any more.") }
            } else {
                mes("Nothing interesting happens.")
            }
        }
        onIfModalButton("component.apothecary_potions:strength") { brew(STRENGTH) }
        onIfModalButton("component.apothecary_potions:energy") { brew(ENERGY) }
        onIfModalButton("component.apothecary_potions:antipoison") { brew(ANTIPOISON) }
        onOpHeld1(POTION) {
            objbox(
                POTION,
                "This looks very colourful, but you remember what Father Lawrence told you about " +
                    "it, probably best not to drink it.",
            )
        }
        onOpHeld2(POTION) { drinkPotion() }
        onOpLoc1("loc.fai_varrock_cadavabush_tailored") { pickTailoredBush() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Romeo</col> in <col=800000>Varrock Square</col>."

    override fun questLog(player: ProtectedAccess): String =
        questJournal(player) {
            val stage = stage(access.player)
            if (stage < MESSAGE_GIVEN) {
                line("I have agreed to find <red>Juliet</red> for <red>Romeo</red> and tell her how he feels. For some reason he can't just do this himself.")
                line("I should go and speak to <red>Juliet</red>. I can find her <red>west of Varrock</red>.")
                return@questJournal
            }
            strike("I have agreed to find Juliet for Romeo and tell her how he feels. For some reason he can't just do this himself.")
            if (stage < MESSAGE_DELIVERED) {
                line("I found <red>Juliet</red> on the Western edge of Varrock, and told her about Romeo. She gave me a <red>message</red> to take back.")
                line("I should take the message to <red>Romeo</red> in <red>Varrock Square</red>.")
                return@questJournal
            }
            strike("I found Juliet on the Western edge of Varrock, and told her about Romeo. She gave me a message to take back.")
            if (stage < SPOKEN_TO_LAWRENCE) {
                line("I delivered the message to Romeo, and he was sad to hear that Juliet's father opposed their marriage. However, he said that <red>Father Lawrence</red> might be able to overcome this.")
                line("I should find <red>Father Lawrence</red> and see how we can help. I can find him in his church in the <red>north-east of Varrock</red>.")
                return@questJournal
            }
            strike("I delivered the message to Romeo, and he was sad to hear that Juliet's father opposed their marriage. However, he said that Father Lawrence might be able to overcome this.")
            if (stage < SPOKEN_TO_APOTHECARY) {
                line("I found Father Lawrence and he suggested the use of a potion to fool Juliet's father that she is dead so that Romeo and Juliet can be together in peace.")
                line("I should ask the <red>Apothecary</red> in <red>south-west Varrock</red> to make a <red>cadava potion</red>.")
                return@questJournal
            }
            strike("I found Father Lawrence and he suggested the use of a potion to fool Juliet's father that she is dead so that Romeo and Juliet can be together in peace.")
            if (stage < POTION_GIVEN) {
                line("I went to the Apothecary regarding making this cadava potion, and he told me to bring him some <red>cadava berries</red>.")
                if (access.inv.count(POTION) > 0) {
                    line("I should take this <red>cadava potion</red> to <red>Juliet</red>.")
                } else {
                    line("I should bring the <red>Apothecary</red> some <red>cadava berries</red>. They grow near the mine <red>south-east of Varrock</red>.")
                }
                return@questJournal
            }
            strike("I went to the Apothecary regarding making this cadava potion, and he told me to bring him some cadava berries.")
            line("Juliet drank the cadava potion and has been taken to the crypt.")
            line("I should tell <red>Romeo</red> to go and collect her.")
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line("I helped Romeo and Juliet fool Juliet's father with a cadava potion, but when Romeo reached the crypt he fell for her cousin Phillipa instead.")
        }

    private fun stage(player: Player): Int = quest.getQuestStage(player)

    private val Dialogue.scared
        get() = mesanim("mesanim.scared")

    private suspend fun Dialogue.romeo() {
        when (stage(player)) {
            NOT_STARTED -> romeoStart()
            STARTED -> {
                chatPlayer(happy, "Hello again, remember me?")
                chatNpc(confused, "Of course, yes....how are.. you....ermmm...")
                chatPlayer(quiz, "You haven't got a clue who I am do you?")
                chatNpc(
                    happy,
                    "Not a clue my friend, but you seem to have a friendly face...a little blood " +
                        "stained, and perhaps in need of a wash, but friendly none the less.",
                )
                chatPlayer(neutral, "You asked me to look for Juliet for you!")
                chatNpc(happy, "Ah yes, Juliet...my sweet darling...what news?")
                chatPlayer(neutral, "Nothing so far, but I need to ask a few questions?")
                julietQuestions()
            }
            MESSAGE_GIVEN -> deliverMessage()
            MESSAGE_DELIVERED -> {
                chatPlayer(happy, "Hey again Romeo!")
                lawrenceQuestions()
            }
            SPOKEN_TO_LAWRENCE -> romeoAfterLawrence()
            SPOKEN_TO_APOTHECARY -> romeoPotion()
            POTION_GIVEN -> romeoCrypt()
            else -> {
                chatNpc(sad, "I heard Juliet had died. Terrible business.")
                chatNpc(happy, "Her cousin and I are getting on well though. Thanks for your help.")
            }
        }
    }

    private suspend fun Dialogue.romeoStart() {
        when (access.random.of(0, 5)) {
            0 -> chatNpc(sad, "Blub! Blub...where is my Juliet? Have you seen her?")
            1 ->
                chatNpc(
                    quiz,
                    "Looking for a blonde girl, goes by the name of Juliet..quite pretty...haven't " +
                        "seen her have you?",
                )
            2 -> chatNpc(sad, "Juliet, Juliet, wherefore art thou Juliet? Have you seen my Juliet?")
            3 ->
                chatNpc(
                    sad,
                    "Oh woe is me that I cannot find my Juliet! You haven't seen Juliet have you?",
                )
            4 ->
                chatNpc(
                    sad,
                    "Sadness surrounds me now that Juliet's father forbids us to meet. Have you " +
                        "seen my Juliet?",
                )
            else ->
                chatNpc(
                    sad,
                    "What is to become of me and my darling Juliet, I cannot find her anywhere, " +
                        "have you seen her?",
                )
        }
        when (
            menu(
                "Yes, I have seen her actually!" to 1,
                "No sorry, I haven't seen her." to 2,
                "Perhaps I could help to find her for you?" to 3,
            )
        ) {
            1 -> {
                chatPlayer(happy, "Yes, I have seen her actually!")
                chatPlayer(shifty, "At least, I think it was her... Blonde? A bit stressed?")
                chatNpc(laugh, "Golly...yes, yes...you make her sound very interesting!")
                chatNpc(shifty, "And I'll bet she's a bit of a fox!")
                chatPlayer(shifty, "Well, I guess she could be considered attractive...")
                chatNpc(laugh, "I'll bet she is!  Wooooooooo!")
                chatNpc(
                    quiz,
                    "Sorry, all that jubilation has made me forget what we were talking about.",
                )
                chatPlayer(bored, "You were asking me about Juliet? You seemed to know her?")
                chatNpc(laugh, "Oh yes, Juliet!")
                chatNpc(
                    happy,
                    "The fox...could you tell her that she is the love of my long and that I " +
                        "life to be with her?",
                )
                chatPlayer(
                    quiz,
                    "What?  Surely you mean that she is the love of your life and that you long " +
                        "to be with her?",
                )
            }
            2 -> {
                chatPlayer(neutral, "No sorry, I haven't seen her.")
                chatNpc(sad, "Oh...well, that's a shame...I was rather hoping you had.")
                chatPlayer(
                    quiz,
                    "Why?  Is she a fugitive?  Does she owe you some money or something?",
                )
                chatNpc(confused, "Hmmm, she might do?  Perhaps she does?  How do you know?")
                chatPlayer(angry, "I don't know? I was asking 'YOU' how 'YOU' know Juliet!")
                chatNpc(
                    happy,
                    "Ahh, yes Juliet, she's my one true love.  Well, one of my one true loves! If " +
                        "you see her, could you tell her that she is the love of my long and that " +
                        "I life to be with her?",
                )
                chatPlayer(
                    quiz,
                    "What?  Surely you mean that 'she is the love of your life and that you long " +
                        "to be with her?'",
                )
            }
            else -> {
                chatPlayer(quiz, "Perhaps I can help find her for you? What does she look like?")
                chatNpc(happy, "Oh would you? That would be great! She has this sort of hair...")
                chatPlayer(neutral, "Hair...check..")
                chatNpc(happy, "...and she these...great lips...")
                chatPlayer(neutral, "Lips...right.")
                chatNpc(happy, "Oh and she has these lovely shoulders as well..")
                chatPlayer(
                    shifty,
                    "Shoulders...right, so she has hair, lips and shoulders...that should cut it " +
                        "down a bit.",
                )
                chatNpc(
                    happy,
                    "Oh yes, Juliet is very different...please tell her that she is the love of my " +
                        "long and that I life to be with her?",
                )
                chatPlayer(
                    quiz,
                    "What?  Surely you mean that 'she is the love of your life and that you long " +
                        "to be with her?'",
                )
            }
        }
        chatNpc(
            happy,
            "Oh yeah...what you said...tell her that, it sounds much better!  Oh you're so good " +
                "at this!",
        )
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "Sorry Romeo, I've got better things to do right now. Maybe later?")
            chatNpc(
                happy,
                "Oh, okay, well, I guess my Juliet and I can spend some time apart.  And as the old " +
                    "saying goes, 'Absinthe makes the heart glow longer'.",
            )
            chatPlayer(quiz, "Don't you mean that, 'Absence makes the...")
            chatPlayer(bored, "Actually forget it...")
            chatNpc(happy, "Okay!")
            return
        }
        chatPlayer(bored, "Yes, okay. I'll let her know.")
        chatNpc(happy, "Oh great! And tell her that I want to kiss her a give.")
        quest.advanceQuestStageTo(access, STARTED)
        chatPlayer(neutral, "You mean you want to give her a kiss!")
        chatNpc(quiz, "Oh you're good...you are good!")
        chatNpc(happy, "I see I've picked a true professional...!")
        julietQuestions()
    }

    private suspend fun Dialogue.julietQuestions() {
        while (true) {
            when (
                menu(
                    "Where can I find Juliet?" to 1,
                    "Is there anything else you can tell me about Juliet?" to 2,
                    "Ok, thanks." to 3,
                )
            ) {
                1 -> {
                    chatPlayer(quiz, "Where can I find Juliet?")
                    chatNpc(quiz, "Why do you ask?")
                    chatPlayer(neutral, "So that I can try and find her for you!")
                    chatNpc(confused, "Ah yes....quite right. Hmmm, let me think now.")
                    chatNpc(
                        neutral,
                        "She may still be locked away at her Father's house on the sest vide of " +
                            "Warrock.",
                    )
                    chatNpc(
                        happy,
                        "Oh, I remember how she loved it when I would sing up to her balcony! She " +
                            "would reward me with her own personal items...",
                    )
                    chatPlayer(quiz, "What, she just gave you her stuff?")
                    chatNpc(
                        laugh,
                        "Well, not exactly give...more like 'throw with considerable force'...she's " +
                            "always a kidder that Juliet!",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Is there anything else you can tell me about Juliet?")
                    chatNpc(
                        happy,
                        "Oh, there is so much to tell...she is my true love, we intend to spend " +
                            "together forever...I can tell you so much about her..",
                    )
                    chatPlayer(happy, "Great!")
                    chatNpc(confused, "Ermmm.....")
                    chatNpc(confused, "So much can I tell you...")
                    chatPlayer(neutral, "Yes..")
                    chatNpc(confused, "So much to tell...why, where do I start!")
                    chatPlayer(happy, "Yes..yes!  Please go on...don't let me interrupt...")
                    chatNpc(confused, "Ermmm.....")
                    chatNpc(confused, "...")
                    chatPlayer(quiz, "You can't remember can you?")
                    chatNpc(sad, "Not a thing sorry....")
                }
                else -> {
                    chatPlayer(bored, "Ok, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.deliverMessage() {
        if (access.inv.count(MESSAGE) == 0) {
            chatPlayer(happy, "Romeo...great news...I've been in touch with Juliet!")
            chatNpc(
                happy,
                "Oh great! That is great news! Well done...well done... what a total success!",
            )
            chatPlayer(happy, "Yes, and she gave me a message to give you...")
            chatNpc(happy, "Ohhh great! A message....wow!")
            chatPlayer(happy, "Yes!")
            chatNpc(happy, "A message...oh, I can't wait to read what my dear Juliet has to say....")
            chatPlayer(happy, "I know...it's exciting isn't it...?")
            chatNpc(happy, "Yes...yes...")
            chatNpc(neutral, "...")
            chatNpc(quiz, "You've lost the message haven't you?")
            chatPlayer(shifty, "Yep, haven't got a clue where it is.")
            return
        }
        chatPlayer(
            happy,
            "Romeo...great news...I've been in touch with Juliet! She's written a message for you...",
        )
        objbox(MESSAGE, "You hand over Juliet's message to Romeo.")
        chatNpc(happy, "Oh, a message! A message! I've never had a message before...")
        chatPlayer(quiz, "Really?")
        chatNpc(happy, "No, no, not one!")
        chatNpc(quiz, "Oh, well, except for the occasional court summons.")
        chatNpc(
            happy,
            "But they're not really 'nice' messages. Not like this one! I'm sure that this message " +
                "will be lovely.",
        )
        chatPlayer(quiz, "Well are you going to open it or not?")
        chatNpc(
            happy,
            "Oh yes, yes, of course!  'Dearest Romeo, I am very pleased that you sent " +
                "${player.displayName} to look for me and to tell me that you still hold " +
                "affliction...', Affliction! She thinks I'm diseased?",
        )
        chatPlayer(bored, "'Affection?'")
        chatNpc(
            happy,
            "Ahh yes...'still hold affection for me. I still feel great affection for you, but " +
                "unfortunately my Father opposes our marriage.'",
        )
        chatPlayer(sad, "Oh dear...that doesn't sound too good.")
        chatNpc(happy, "What? '...great affection for you. Father opposes our..")
        chatNpc(sad, "'...marriage and will...")
        chatNpc(shocked, "...will kill you if he sees you again!'")
        chatPlayer(shifty, "I have to be honest, it's not getting any better...")
        chatNpc(
            sad,
            "'Our only hope is that Father Lawrence, our long time confidant, can help us in some " +
                "way.'",
        )
        objbox(MESSAGE, "Romeo folds the message away.")
        chatNpc(sad, "Well, that's it then...we haven't got a chance...")
        chatPlayer(quiz, "What about Father Lawrence?")
        chatNpc(sad, "...our love is over...the great romance, the life of my love...")
        chatPlayer(quiz, "...or you could speak to Father Lawrence!")
        chatNpc(
            sad,
            "Oh, my aching, breaking, heart...how useless the situation is now...we have no one " +
                "to turn to...",
        )
        chatPlayer(angry, "FATHER LAWRENCE!")
        access.invDel(access.inv, MESSAGE)
        quest.advanceQuestStageTo(access, MESSAGE_DELIVERED)
        chatNpc(shocked, "Father Lawrence?")
        chatNpc(
            happy,
            "Oh yes, Father Lawrence...he's our long time confidant, he might have a solution! " +
                "Yes, yes, you have to go and talk to Lather Fawrence for us and ask him if he's " +
                "got any suggestions for our predicament?",
        )
        whereIsLawrence()
        lawrenceQuestions()
    }

    private suspend fun Dialogue.whereIsLawrence() {
        chatPlayer(quiz, "Where can I find Father Lawrence?")
        chatNpc(happy, "Lather Fawrence! Oh he's...")
        chatNpc(shifty, "You know he's not my 'real' Father don't you?")
        chatPlayer(bored, "I think I suspected that he wasn't.")
        chatNpc(
            happy,
            "Well anyway...he tells these song, loring bermons...and keeps these here Carrockian " +
                "vitizens snoring in his church to the East North.",
        )
    }

    private suspend fun Dialogue.lawrenceQuestions() {
        while (true) {
            when (
                menu(
                    "How are you?" to 1,
                    "Where can I find Father Lawrence?" to 2,
                    "Have you heard anything from Juliet?" to 3,
                    "Ok, thanks." to 4,
                )
            ) {
                1 -> {
                    chatPlayer(quiz, "How are you?")
                    chatNpc(sad, "Not so good my friend...I miss Judi..., Junie..., Joopie...")
                    chatPlayer(quiz, "Juliet?")
                    chatNpc(sad, "Juliet! I miss Juliet, terribly!")
                    chatPlayer(neutral, "Hmmm, so I see!")
                }
                2 -> whereIsLawrence()
                3 -> {
                    chatPlayer(quiz, "Have you heard anything from Juliet?")
                    chatNpc(
                        sad,
                        "Sadly not my friend! And what's worse, her Father has threatened to kill " +
                            "me if he sees me. I mean, that seems a bit harsh!",
                    )
                    chatPlayer(
                        neutral,
                        "Well, I shouldn't worry too much...you can always run away if you see " +
                            "him...",
                    )
                    chatNpc(
                        scared,
                        "I just wish I could remember what he looks like! I live in fear of every " +
                            "man I see!",
                    )
                }
                else -> {
                    chatPlayer(neutral, "Ok, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.romeoAfterLawrence() {
        chatPlayer(happy, "Hello again Romeo!")
        chatNpc(
            laugh,
            "Ooh did you manage to survive one of Lather Fawrences sermons? I bet not, you were " +
                "ages! I bet you snoozed on the welcome mat just as soon as you heard his voice!",
        )
        chatPlayer(angry, "Did not!")
        chatNpc(happy, "Oh, come on, come on, what did he say?")
        while (true) {
            when (
                menu(
                    "He wants me to go to the Apothecary!" to 1,
                    "He seems keen for you to marry Juliet." to 2,
                    "Ok, thanks." to 3,
                )
            ) {
                1 -> {
                    chatPlayer(happy, "He wants me to go to the Apothecary!")
                    chatNpc(quiz, "The Apothecary?")
                    chatNpc(
                        quiz,
                        "Oh...is he the one who mixes up all them magical potion- ey things?",
                    )
                    chatPlayer(
                        neutral,
                        "Yeah, I think so...but the word potion-ey doesn't exist.",
                    )
                    chatNpc(happy, "Well, you just used it...so I guess it does exist!")
                    chatPlayer(bored, "It doesn't matter...do you know where the Apothecary is?")
                    chatNpc(quiz, "Why should I tell you?")
                    chatPlayer(angry, "Because I'm doing you a favour!")
                    chatNpc(
                        happy,
                        "Right, yes...of course! Well, I think the potion-ey place is Wouth Sest of " +
                            "here and near a sword shop.",
                    )
                }
                2 -> {
                    chatPlayer(neutral, "He seems very keen for you to marry Juliet.")
                    chatNpc(happy, "Me too! I can't wait! Do you think it will be soon?")
                    chatPlayer(
                        neutral,
                        "I don't know, but I'll do what I can. It just seems a bit odd that Father " +
                            "Lawrence is so keen for you to marry?",
                    )
                    chatNpc(
                        confused,
                        "I can't imagine why he's so keen? Though, he was our messenger before " +
                            "you. Of course, he had a lot more hair back then...and I think we may " +
                            "have gone to school together...",
                    )
                    chatPlayer(quiz, "What, since you were kids?")
                    chatNpc(laugh, "Yeah! Thinking about it now, we called him 'Diddy Dorrence' back then!")
                    chatPlayer(
                        shifty,
                        "So, maybe, he just wants a bit of peace? And when you two are married, he " +
                            "won't have to traipse around delivering your messages anymore?",
                    )
                    chatNpc(neutral, "Yes, the years haven't been good to him.")
                }
                else -> {
                    chatPlayer(neutral, "Ok, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.romeoPotion() {
        if (access.inv.count(POTION) == 0) {
            chatPlayer(happy, "Hi Romeo!")
            chatNpc(happy, "Oh hello, have you seen Lather Fawrence?")
            chatPlayer(
                neutral,
                "Yes, he's given me details of a potion which should help resolve this situation. " +
                    "The Apothecary is helping me prepare it.",
            )
            chatNpc(confused, "Ooooh, it all sounds horribly complicated?")
            chatNpc(happy, "All I can say is I'll be glad when Juliet's finally in that crypt!")
            chatPlayer(shifty, "Spoken like a true lover!")
            return
        }
        objbox(POTION, "Romeo spots the Cadava potion.")
        chatNpc(laugh, "Ooohh, that's the potion is it? Rather you than me!")
        chatPlayer(angry, "I'm not drinking it! It's for Juliet!")
        chatNpc(
            shifty,
            "Well, I mean...it's probably delicious...and not dangerous at all. And you quite often " +
                "see harmless drinks in a glowing sort of pink colour.",
        )
        chatPlayer(
            shifty,
            "Hmm, I'm not convinced...maybe you ought to try some before I give it to Juliet?",
        )
        chatNpc(scared, "Urgh! No way!  Not in a million years!")
        chatPlayer(quiz, "Ok...but you know what to do when I've given this to Juliet?")
        chatNpc(happy, "Oh, yeah...sure Lather Fawrence explained all that to me...")
    }

    private suspend fun Dialogue.juliet() {
        when (stage(player)) {
            NOT_STARTED -> {
                chatNpc(sad, "Romeo, Romeo, wherefore art thou Romeo?")
                mesbox("She seems to be lost in thought.")
            }
            STARTED -> {
                chatPlayer(
                    neutral,
                    "Juliet, I come from Romeo.  He begs me to tell you that he cares still.",
                )
                chatNpc(
                    happy,
                    "Oh how my heart soars to hear this news! Please take this message to him with " +
                        "great haste.",
                )
                chatPlayer(neutral, "Well, I hope it's good news...he was quite upset when I left him.")
                chatNpc(
                    quiz,
                    "He's quite often upset...the poor sensitive soul. But I don't think he's going " +
                        "to take this news very well, however, all is not lost.",
                )
                chatNpc(
                    happy,
                    "Everything is explained in the letter, would you be so kind and deliver it to " +
                        "him please?",
                )
                chatPlayer(happy, "Certainly, I'll do so straight away.")
                chatNpc(happy, "Many thanks! Oh, I'm so very grateful. You may be our only hope.")
                quest.advanceQuestStageTo(access, MESSAGE_GIVEN)
                giveMessage("Juliet gives you a message.")
            }
            MESSAGE_GIVEN -> {
                chatPlayer(happy, "Hello Juliet!")
                chatNpc(
                    quiz,
                    "Hello there...have you delivered the message to Romeo yet? What news do you " +
                        "have from my loved one?",
                )
                if (access.inv.count(MESSAGE) > 0) {
                    chatPlayer(worried, "Oh, sorry, I've not had chance to deliver it yet!")
                    chatNpc(
                        sad,
                        "Oh, that's a shame. I've been waiting so patiently to hear some word from " +
                            "him.",
                    )
                    return
                }
                chatPlayer(
                    shifty,
                    "Hmmm, that's the thing about messages...they're so easy to misplace...",
                )
                chatNpc(
                    angry,
                    "How could you lose that message? It was incredibly important...and it took me " +
                        "an age to write! I used joined up writing and everything!",
                )
                chatNpc(angry, "Please, take this new message to him, and please don't lose it.")
                giveMessage("Juliet gives you another message.")
            }
            MESSAGE_DELIVERED -> {
                chatPlayer(
                    neutral,
                    "Hi Juliet, I have passed your message on to Romeo..he's scared half out of his " +
                        "wits at the news that your father wants to kill him.",
                )
                chatNpc(
                    sad,
                    "Yes, unfortunately my father is quite the hunter, you may have seen some the " +
                        "animal head trophies on the wall. And it would be so awful to see Romeo's " +
                        "head up there with them!",
                )
                chatPlayer(neutral, "I know what you mean...")
                chatPlayer(shifty, "...his hair colour will clash terribly with the rest of the decoration.")
                chatNpc(angry, "That's not what I was suggesting at all...")
                chatPlayer(laugh, "I know, I know...I was just kidding.")
                chatPlayer(
                    neutral,
                    "Anyway, don't worry because I'm on the case. I'm going to get some help from " +
                        "Father Lawrence.",
                )
                chatNpc(
                    happy,
                    "Oh yes, I'm sure that Father Lawrence will come up with a solution. I hope you " +
                        "find him soon.",
                )
            }
            SPOKEN_TO_LAWRENCE -> {
                chatPlayer(
                    happy,
                    "Hi Juliet, I've found Father Lawrence and he has a cunning plan. But for it to " +
                        "work, I need to seek the Apothecary!",
                )
                chatNpc(
                    happy,
                    "Oh good! I knew Father Lawrence would come up with something. However, I don't " +
                        "know where the apothecary is...I hope you find him soon. My father's " +
                        "temper gets no better.",
                )
            }
            SPOKEN_TO_APOTHECARY -> julietPotion()
            else -> {
                chatNpc(angry, "I sat in that cold crypt for ages waiting for Romeo.")
                chatNpc(angry, "That useless fool never showed up.")
                chatNpc(angry, "And all I got was indigestion. I am done with men like him.")
                chatNpc(angry, "Now go away before I call my father!")
            }
        }
    }

    private suspend fun Dialogue.giveMessage(text: String) {
        access.invAdd(access.inv, MESSAGE)
        objbox(MESSAGE, text)
    }

    private suspend fun Dialogue.julietPotion() {
        if (access.inv.count(POTION) == 0) {
            chatPlayer(happy, "Hi Juliet!")
            chatNpc(
                happy,
                "Hi ${player.displayName}, how close am I to being with my true love Romeo?",
            )
            chatPlayer(neutral, "Sorry, I still have to get a special potion for you.")
            chatNpc(
                happy,
                "Oh, I hope it isn't a love potion because you would be wasting your time. My love " +
                    "for Romeo grows stronger every minute...",
            )
            chatPlayer(shifty, "That must be because you're not with him...")
            chatNpc(sad, "Oh no! I long to be close to my true love Romeo!")
            chatPlayer(
                neutral,
                "Well, ok then...I'll set about getting this potion as quickly as I can!",
            )
            chatNpc(happy, "Fair luck to you, the end is close.")
            return
        }
        chatPlayer(
            happy,
            "Hi Juliet! I have an interesting proposition for you...suggested by Father Lawrence. " +
                "It may be the only way you'll be able to escape from this house and be with Romeo.",
        )
        chatNpc(shifty, "Go on....")
        chatPlayer(
            quiz,
            "I have a Cadava potion here, suggested by Father Lawrence. If you drink it, it will " +
                "make you appear dead!",
        )
        chatNpc(angry, "Yes...")
        chatPlayer(
            quiz,
            "And when you appear dead...your still and lifeless corpse will be removed to the crypt!",
        )
        chatNpc(happy, "Oooooh, a cold dark creepy crypt...")
        chatNpc(angry, "...sounds just peachy!")
        chatPlayer(quiz, "Then...Romeo can steal into the crypt and rescue you just as you wake up!")
        chatNpc(angry, "...and this is the great idea for getting me out of here?")
        chatPlayer(
            quiz,
            "To be fair, I can't take all the credit...in fact...it was all Father Lawrence's " +
                "suggestion...",
        )
        chatNpc(angry, "Ok...if this is the best we can do...hand over the potion!")
        access.invDel(access.inv, POTION)
        objbox(POTION, "You pass the suspicious potion to Juliet.")
        chatNpc(angry, "Wonderful! I just hope Romeo can remember to get me from the crypt.")
        chatNpc(
            confused,
            "Please go to Romeo and make sure he understands. Although I love his gormless, " +
                "lovelorn soppy ways, he can be a bit dense sometimes and I don't want to wake up " +
                "in that crypt on my own.",
        )
        player.julietVisible = 1
        quest.advanceQuestStageTo(access, POTION_GIVEN)
        potionCutscene()
    }

    private suspend fun Dialogue.potionCutscene() {
        Cinematic.setHideToplevel(player, true)
        access.telejump(JULIET_ROOM)
        player.midiJingle(POTION_JINGLE)
        val juliet = spawn("npc.juliet", CUTSCENE_JULIET, JULIET_ROOM)
        val phillipa = spawn("npc.phillipa", CUTSCENE_PHILLIPA, JULIET_ROOM)
        access.delay(1)
        access.faceSquare(CUTSCENE_JULIET)
        access.camMoveTo(JULIET_CAMERA, height = 375, rate = 100, rate2 = 100)
        access.camLookAt(JULIET_ROOM, height = 175, rate = 100, rate2 = 100)
        chatJuliet(happy, "Oh, here's Phillipa, my cousin...she's in on the plot too!")
        chatJuliet(shifty, "She's going to make it seem even more convincing!")
        chatPhillipa(happy, "Yes, I'm quite the actress! Good luck dear cousin!")
        chatJuliet(happy, "Right...bottoms up!")
        juliet.spotanim("spotanim.human_drink_from_vial_cadava_spotanim")
        juliet.anim("seq.human_drink_from_vial_cadava")
        access.soundSynth(DRINK_SYNTH)
        access.delay(3)
        chatJuliet(shocked, "Urk!")
        juliet.anim("seq.human_death")
        access.soundSynth(COLLAPSE_SYNTH)
        access.delay(3)
        chatPhillipa(happy, "Oh no...Juliet has...died!")
        chatPlayer(neutral, "You might be more believable if you're not smiling when you say it...")
        chatPhillipa(shocked, "Oh yeah...you might be right...ok, let's try again.")
        chatPhillipa(confused, "Oh no...Juliet has...died?")
        chatPlayer(quiz, "Perhaps a bit louder, like you're upset...that your cousin has died!")
        chatPhillipa(
            shifty,
            "Right...yes...Ok, ok, I think I'm getting my motivation now. Ok, let's try this again!",
        )
        chatPhillipa(
            sad,
            "OH NO...JULIET HAS...DIED?.... Oooooohhhhhh....(sob), (sob).Juliet...my poor dead " +
                "cousin!",
        )
        val draul = spawn("npc.draul_leptoc", CUTSCENE_DRAUL, CUTSCENE_PHILLIPA)
        access.camMoveTo(JULIET_CAMERA_CLOSE, height = 425, rate = 5, rate2 = 5)
        access.camLookAt(CUTSCENE_JULIET, height = 125, rate = 5, rate2 = 5)
        chatDraul(angry, "What's all that screaming?")
        chatDraul(sad, "Oh no! My poor daughter...what has become of you?")
        chatPhillipa(sad, "Poor Juliet...make preparations for her body to be placed in the Crypt...")
        despawn(juliet, phillipa, draul)
        access.telejump(JULIET_ROOM)
        Cinematic.setHideToplevel(player, false)
        access.camReset()
    }

    private suspend fun Dialogue.romeoCrypt() {
        chatPlayer(
            happy,
            "Romeo, it's all set. Juliet has drunk the potion and has been taken down into the " +
                "Crypt...now you just need to pop along and collect her.",
        )
        chatNpc(happy, "Ah right, the potion! Great...")
        chatNpc(confused, "What potion would that be then?")
        chatPlayer(
            neutral,
            "The Cadava potion...you know, the one which will make her appear dead! She's in the " +
                "crypt, pop along and claim your true love.",
        )
        chatNpc(scared, "But I'm scared...will you come with me?")
        chatPlayer(angry, "Oh, ok...come on! I think I saw the entrance when I visited there last...")
        cryptCutscene()
    }

    private suspend fun Dialogue.cryptCutscene() {
        Cinematic.setHideToplevel(player, true)
        access.soundSynth(CRYPT_ENTER_SYNTH)
        access.telejump(CRYPT_PLAYER)
        player.midiJingle(CRYPT_JINGLE)
        val romeo = spawn("npc.romeo", CRYPT_ROMEO, CRYPT_ROMEO.translateZ(1))
        chatRomeo(scared, "This is pretty scary...")
        chatPlayer(angry, "Oh, be quiet...")
        access.camMoveTo(CRYPT_CAMERA, height = 400, rate = 100, rate2 = 100)
        access.camLookAt(CRYPT_LOOK, height = 25, rate = 100, rate2 = 100)
        access.soundSynth(CRYPT_DOOR_SYNTH)
        chatPlayer(happy, "We're here. Look, Juliet is over there!")
        access.camMoveTo(CoordGrid(2334, 4647, 0), height = 375, rate = 10, rate2 = 10)
        access.camLookAt(CoordGrid(2322, 4639, 0), height = 250, rate = 10, rate2 = 10)
        access.delay(5)
        access.camMoveTo(CoordGrid(2330, 4647, 0), height = 375, rate = 5, rate2 = 5)
        access.camLookAt(CRYPT_JULIET, height = 250, rate = 5, rate2 = 5)
        access.delay(3)
        access.camMoveTo(CoordGrid(2324, 4644, 0), height = 250, rate = 5, rate2 = 5)
        access.camLookAt(CRYPT_JULIET, height = 200, rate = 5, rate2 = 5)
        access.delay(7)
        access.camMoveTo(CRYPT_CAMERA, height = 400, rate = 100, rate2 = 100)
        access.camLookAt(CRYPT_LOOK, height = 25, rate = 100, rate2 = 100)
        chatPlayer(happy, "You go over to her...and I'll go and wait over here...")
        chatRomeo(scared, "Ohhh, ok then...")
        access.camMoveTo(CoordGrid(2322, 4639, 0), height = 300, rate = 100, rate2 = 100)
        access.camLookAt(CoordGrid(2328, 4654, 0), height = 25, rate = 100, rate2 = 100)
        romeo.walk(ROMEO_PATH)
        access.delay(9)
        val phillipa = spawn("npc.phillipa", CRYPT_PHILLIPA, CRYPT_PHILLIPA.translateZ(-1))
        phillipa.walk(PHILLIPA_PATH)
        access.delay(4)
        chatRomeo(scared, "Hey...Juliet...")
        chatRomeo(neutral, "Juliet....?")
        chatRomeo(sad, "Oh dear...you seem to be dead.")
        chatPhillipa(happy, "Hi Romeo...I'm Phillipa!")
        chatRomeo(happy, "Wow! You're a fox!")
        chatPhillipa(happy, "It's a shame about Juliet...but perhaps we can meet up later?")
        chatRomeo(happy, "Who's Juliet?")
        despawn(romeo, phillipa)
        access.telejump(CRYPT_RETURN)
        Cinematic.setHideToplevel(player, false)
        access.camReset()
        player.julietVisible = 2
        quest.advanceQuestStageTo(access, COMPLETE)
    }

    private fun spawn(type: String, coords: CoordGrid, facing: CoordGrid): Npc {
        val npc = Npc(type, coords)
        npcRepo.add(npc, CUTSCENE_NPC_TICKS)
        npc.faceSquare(facing)
        return npc
    }

    private fun despawn(vararg npcs: Npc) {
        for (npc in npcs) {
            if (npc.isSlotAssigned) {
                npcRepo.del(npc, Int.MAX_VALUE)
            }
        }
    }

    private suspend fun Dialogue.chatJuliet(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Juliet", "npc.juliet", mesanim, text)

    private suspend fun Dialogue.chatPhillipa(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Phillipa", "npc.phillipa", mesanim, text)

    private suspend fun Dialogue.chatDraul(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Draul Leptoc", "npc.draul_leptoc", mesanim, text)

    private suspend fun Dialogue.chatRomeo(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Romeo", "npc.romeo", mesanim, text)

    private suspend fun Dialogue.phillipa() {
        when (stage(player)) {
            NOT_STARTED -> {
                chatPlayer(quiz, "Hello, who are you?")
                phillipaIntro()
                chatPlayer(quiz, "Romeo? Where would I find him then?")
                chatNpc(
                    laugh,
                    "Well, that's a good question! Who knows where his head's at most of the time? " +
                        "In the clouds, most likely!",
                )
                chatNpc(
                    happy,
                    "But he's probably chasing the ladies who frequent Varrock market. He does like " +
                        "a bit of kiss chase, so I've heard!",
                )
            }
            STARTED -> {
                chatPlayer(happy, "Hello.")
                phillipaIntro()
            }
            in MESSAGE_GIVEN until POTION_GIVEN -> {
                chatNpc(
                    happy,
                    "Oh, hello. Juliet has told me what you're doing for her and Romeo, and I have to " +
                        "say I'm very grateful to you. Juliet deserves a bit of happiness in her life.",
                )
                chatNpc(
                    happy,
                    "And I'm sure Romeo is just the sort of jester to make her laugh out loud - " +
                        "hysterically you might say.",
                )
                chatNpc(
                    laugh,
                    "He always brings a tear to my eyes - tears of happiness at his foolish antics!",
                )
                chatPlayer(happy, "Oh, thanks. I like to do my cupid bit.")
            }
            POTION_GIVEN -> {
                chatNpc(happy, "Oh, hello again! How was I - do you think I was convincing?")
                chatPlayer(happy, "Oh, yes, totally!")
                chatNpc(
                    happy,
                    "Ohhh good! I do hope Juliet will be pleased as well! That dashing young Romeo " +
                        "must be beside himself with excitement! Have you told him the good news yet?",
                )
                chatPlayer(neutral, "Not yet, but soon!")
                chatNpc(
                    happy,
                    "Good, good - you lucky thing, I can't wait to see the look on his face!",
                )
            }
            else ->
                chatNpc(
                    happy,
                    "Hello, I seem to be getting on really well with that dashing lovely dovely, " +
                        "Romeo. Pity about poor Juliet - she missed out there I reckon!",
                )
        }
    }

    private suspend fun Dialogue.phillipaIntro() {
        chatNpc(
            happy,
            "Hi, I'm Phillipa, Juliet's cousin. I like to keep an eye on her, make sure that dashing " +
                "young Romeo doesn't just steal her away under our plain old noses!",
        )
        chatNpc(
            happy,
            "He'd do it, you know - he's ever so dashing, and cavalier, in a wet blanket sort of way.",
        )
    }

    private suspend fun Dialogue.draul() {
        when (stage(player)) {
            NOT_STARTED -> {
                chatNpc(
                    angry,
                    "What are you doing in my house...why the impertinence...the sheer cheek...how " +
                        "dare you violate my personal lodgings....",
                )
                chatPlayer(worried, "I..I was just looking around....")
                chatNpc(
                    angry,
                    "Well get out! Get out....this is my house....and don't go near my daughter " +
                        "Juliet...she's grounded in her room to keep her away from that good for " +
                        "nothing Romeo.",
                )
                chatPlayer(worried, "Yes....sir....")
            }
            STARTED -> draulSnooping()
            MESSAGE_GIVEN -> draulMessage()
            MESSAGE_DELIVERED,
            SPOKEN_TO_LAWRENCE ->
                chatNpc(
                    angry,
                    "Do you live here? If so, how's about a couple of hundred gold towards the rent " +
                        "eh? Pay your share I say...you don't want to be like that freeloading Romeo!",
                )
            SPOKEN_TO_APOTHECARY -> draulPotion()
            POTION_GIVEN ->
                chatNpc(
                    sad,
                    "My poor Juliet....she's dead...dead! I shouldn't have been so hard on her...she " +
                        "was my lovely daughter. Booo hoooo hooooo!",
                )
            else -> chatNpc(angry, "I suppose you're quite pleased with yourself now...")
        }
    }

    private suspend fun Dialogue.draulSnooping() {
        chatNpc(angry, "What are you doing here? Snooping around...")
        when (
            menu(
                "I've come to see Juliet on Romeo's behalf." to 1,
                "I've just come to have a chat with Juliet." to 2,
                "Oh...just looking around..." to 3,
                "Ok, thanks." to 4,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "I've come to see Juliet on Romeo's behalf.")
                chatNpc(
                    angry,
                    "What...what...Romeo! Why that good for nothing swine...he's always trying to " +
                        "get the affections of my daughter..that soppy, half brained nincompoop " +
                        "won't ever have the heart of my daughter.",
                )
                chatNpc(angry, "She deserves someone of character, wit and repose.")
                chatPlayer(quiz, "What's so wrong about Romeo?")
                chatNpc(
                    angry,
                    "Wrong! What's wrong with him...have you actually talked to him? He's nothing but " +
                        "a dim witted upperclass twit, totally useless.",
                )
                chatNpc(
                    angry,
                    "If he threw a stone at the ground, he'd probably miss! He's totally invisible " +
                        "when it's raining because he's so wet!",
                )
                chatNpc(
                    angry,
                    "If you started with what's right with him, you'd have much less to consider!",
                )
                chatPlayer(
                    shifty,
                    "Well, I admit, he's probably not the sharpest knife in the cutlery draw...",
                )
                chatNpc(
                    angry,
                    "Sharp? I've seen keener wit in root vegetables.  Anyway, stop changing the " +
                        "subject. Get out of here and don't think you can sneak up those stairs to " +
                        "see Juliet, because I'll catch you and then you'll be for it!",
                )
                chatPlayer(worried, "That seems a bit harsh....")
                chatNpc(angry, "Harsh but fair I think you'll find...now get OUT!")
            }
            2 -> {
                chatPlayer(neutral, "I've just come to have a chat with Juliet.")
                chatNpc(
                    angry,
                    "What on earth about?  I hope you're not in cahoots with that good for nothing " +
                        "Romeo!",
                )
                chatPlayer(worried, "Err..no of course not....why would I be?")
                chatNpc(
                    angry,
                    "He's been trying to wooo my daughter for an age. Up until now she's had the " +
                        "good sense to just ignore him. I just don't know what's gotten into her " +
                        "recently so that she would give him the time of day.",
                )
                chatPlayer(
                    laugh,
                    "Well, love is mysterious!  Perhaps one day someone may even learn to love you!",
                )
                chatNpc(
                    angry,
                    "What!  Someone may fall in love with me...what are you trying to insinuate?",
                )
                chatPlayer(worried, "Err...Nothing....I guess I'd better be going now...")
            }
            3 -> {
                chatPlayer(neutral, "Oh...just looking around...")
                chatNpc(
                    angry,
                    "Just looking around! This is MY house!  You might have at least 'ASKED' to view " +
                        "my considerably well appointed abode...but no, you've just burst in with " +
                        "all the elegance of a Troll at a tea party.",
                )
                chatPlayer(
                    shifty,
                    "I can see that you're busy ranting so I'll just nip off and investigate a bit.",
                )
            }
            else -> chatPlayer(neutral, "Ok, thanks.")
        }
    }

    private suspend fun Dialogue.draulMessage() {
        chatNpc(angry, "What are you doing in my house? Up to no good I shouldn't wonder!")
        chatPlayer(
            happy,
            "Just a small chore for Juliet, you do have a lovely daughter in her sir.",
        )
        chatNpc(happy, "Oh...why, thank you...I've always tried to my best...")
        chatNpc(
            angry,
            "...Hang on! Enough of that smiley talk. I have a daughter and I know what she's like. " +
                "Don't even think of carrying on anything behind my back, I have the eyes of a " +
                "hawk, nothing gets past me!",
        )
        if (access.inv.count(MESSAGE) == 0) return
        objbox(MESSAGE, "Sir Draul notices the message!")
        chatNpc(
            angry,
            "Hey! What's that in your hands...looks like a message to me...with Juliet's barely " +
                "legible scrawl on it...",
        )
        chatPlayer(worried, "Yes, yes, that's probably why I can't read it!")
        chatPlayer(
            worried,
            "Sorry, I mean, that's right sir. I'm just popping to the shops to get some groceries " +
                "for Juliet.",
        )
        chatPlayer(neutral, "Right, have to be off now...thanks...")
        chatNpc(angry, "Groceries!")
        chatNpc(
            angry,
            "Groceries!...at a time like this, does that girl know what she's putting me through!",
        )
    }

    private suspend fun Dialogue.draulPotion() {
        chatNpc(angry, "Hey , what are you doing here?")
        chatPlayer(
            worried,
            "Nothing much sir...I promise...I'm just an innocent friend of Juliet's...doing her a " +
                "few favours...as a friend.",
        )
        chatNpc(
            angry,
            "Well, just make sure there's no funny business going on that's all I can say. Do you " +
                "know why?",
        )
        chatPlayer(neutral, "I think I can guess sir...")
        chatNpc(
            angry,
            "There's no need to guess! You can see it in my hawk like eyes, in my cat like ears and " +
                "my dog like nose...",
        )
        chatPlayer(quiz, "Are you saying that you look like an animal sir?")
        chatNpc(
            angry,
            "NO! Not look like an animal! I have the keen 'SENSES' of an animal and I don't miss a " +
                "thing. Don't even think about trying anything!",
        )
        if (access.inv.count(POTION) == 0) return
        objbox(POTION, "Draul notices the potion!")
        chatNpc(angry, "Hey!  What's that in your hands? Looks like some sort of potion to me!")
        chatPlayer(
            worried,
            "Err...no! Nope! Not a potion!  Some medicine...I have a terrible cough  " +
                "...cough...cough....see?",
        )
        chatPlayer(
            worried,
            "Except, I can't drink too much or else it makes me feel 'really' tired. One sip of this " +
                "and the lights go out...I mean, I'm asleep in a minute.",
        )
        chatNpc(angry, "Sleep!")
        chatNpc(
            angry,
            "Sleep...! How can you even think of such a thing at a time like this? And I hope you " +
                "have no intentions of sleeping in my house!",
        )
        chatNpc(angry, "You'll get a bill for rent if I catch you dozing off around here!")
    }

    private suspend fun Dialogue.lawrence() {
        val stage = stage(player)
        when {
            stage < MESSAGE_DELIVERED -> lawrenceAdvice()
            stage == MESSAGE_DELIVERED -> sermon()
            stage == SPOKEN_TO_LAWRENCE ->
                chatNpc(
                    neutral,
                    "Ah, have you found the Apothecary yet? Remember, Cadava potion, for Juliet.",
                )
            stage == SPOKEN_TO_APOTHECARY && access.inv.count(POTION) > 0 -> {
                chatNpc(quiz, "Did you find the Apothecary?")
                chatPlayer(happy, "I've got the cadava potion.")
                chatNpc(
                    happy,
                    "Good! Good work! Ok, take it to Juliet, she's expecting you. I'll talk to Romeo " +
                        "and make sure he knows what to do.",
                )
            }
            stage == SPOKEN_TO_APOTHECARY -> {
                chatNpc(quiz, "Did you find the Apothecary?")
                chatPlayer(neutral, "Yes I did. He's told me I must find some Cadava berries.")
                chatNpc(neutral, "Well, good luck with that...they're quite tricky to find.")
                chatPlayer(quiz, "Any clues where I can start to look?")
                chatNpc(
                    neutral,
                    "I heard some kids saying they saw some the other day. They were visiting the " +
                        "mining place to the south east of Varrock.",
                )
                chatPlayer(neutral, "Ok, that's as good a place to start looking as any.")
            }
            else -> {
                chatNpc(drunk, "Oh to be a father in the times of whiskey.")
                chatNpc(drunk, "I sing and I drink and I wake up in gutters.")
                if (access.random.of(0, 1) == 0) {
                    chatNpc(drunk, "Top of the morning to you.")
                } else {
                    chatNpc(drunk, "To err is human, to forgive, quite difficult.")
                    chatNpc(drunk, "I need a think I drink.")
                }
            }
        }
    }

    private suspend fun Dialogue.lawrenceAdvice() {
        chatNpc(neutral, "Hello adventurer, do you seek a quest?")
        while (true) {
            when (
                menu(
                    "I am always looking for a quest." to 1,
                    "No, I prefer just to kill things." to 2,
                    "Can you recommend a good bar?" to 3,
                    "Ok, thanks" to 4,
                )
            ) {
                1 -> {
                    chatPlayer(happy, "I am always looking for a quest.")
                    chatNpc(
                        neutral,
                        "Well, I see poor Romeo wandering around the square. I think he may need " +
                            "help.",
                    )
                    chatNpc(
                        neutral,
                        "I was helping him and Juliet to meet, but it became impossible.",
                    )
                    chatNpc(neutral, "I am sure he can use some help.")
                }
                2 -> {
                    chatPlayer(neutral, "No, I prefer just to kill things.")
                    chatNpc(
                        neutral,
                        "That's a fine career in these lands. There is more that needs killing " +
                            "every day.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Can you recommend a good bar?")
                    chatNpc(angry, "Drinking will be the death of you.")
                    chatNpc(neutral, "But the Blue Moon in the city is cheap enough.")
                    chatNpc(
                        neutral,
                        "And providing you buy one drink an hour they let you stay all night.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "Ok, thanks")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.sermon() {
        chatNpc(happy, "''...and let Saradomin light the way for you... '' Urgh!")
        chatNpc(angry, "Can't you see that I'm in the middle of a sermon?!")
        chatPlayer(angry, "But Romeo sent me!")
        chatNpc(angry, "But I'm busy delivering a sermon to my congregation!")
        access.camMoveTo(SERMON_CAMERA, height = 325, rate = 100, rate2 = 100)
        access.camLookAt(SERMON_LOOK, height = 25, rate = 100, rate2 = 100)
        chatNpcSpecific(
            "Congregation",
            "npc.romeo_juliet_pew_sleeping_man",
            mesanim("mesanim.sleep"),
            "Zzzzzzzzz",
        )
        access.say("Yes, well, it certainly seems")
        access.delay(2)
        access.say("like you have a captive")
        access.delay(2)
        access.say("audience!")
        chatPlayer(bored, "Yes, well, it certainly seems like you have a captive audience!")
        access.camReset()
        chatNpc(
            confused,
            "Ok, okay... what do you want so I can get rid of you and continue with my sermon?",
        )
        chatPlayer(neutral, "Romeo sent me. He says you may be able to help.")
        chatNpc(confused, "Ah Romeo, yes. A fine lad, but a little bit confused.")
        chatPlayer(
            neutral,
            "Yes, very confused.... Anyway, Romeo wishes to be married to Juliet! She must be " +
                "rescued from her father's control!",
        )
        chatNpc(happy, "I agree, and I think I have an idea! A potion to make her appear dead...")
        chatPlayer(shifty, "Dead! Sounds a bit creepy to me... but please, continue.")
        chatNpc(
            shifty,
            "The potion will only make Juliet 'appear' dead... then she'll be taken to the crypt...",
        )
        chatPlayer(shifty, "Crypt! Again... very creepy! You must have some strange hobbies.")
        quest.advanceQuestStageTo(access, SPOKEN_TO_LAWRENCE)
        chatNpc(
            shifty,
            "Then Romeo can collect her from the crypt! Go to the Apothecary, tell him I sent you " +
                "and that you'll need a 'Cadava' potion.",
        )
        chatPlayer(
            shifty,
            "Apart from the strong overtones of death, this is turning out to be a real love story.",
        )
    }

    private suspend fun Dialogue.apothecary() {
        chatNpc(neutral, "I am the Apothecary. I brew potions. Do you need anything specific?")
        while (true) {
            val potions =
                menu("Can you make potions for me?" to true, "Talk about something else." to false)
            if (potions) {
                chatPlayer(quiz, "Can you make potions for me?")
                access.openPotions()
                return
            }
            val stage = stage(player)
            val gossip = stage == NOT_STARTED || quest.isQuestCompleted(player)
            val options = buildList {
                if (stage >= SPOKEN_TO_LAWRENCE) add("Talk about Romeo & Juliet." to 1)
                if (gossip) add("Have you got any decent gossip to share?" to 2)
                add("Do you know a potion to make hair fall out?" to 3)
                add("Have you got any good potions to give away?" to 4)
                add("No thanks." to 5)
            }
            when (menu(options)) {
                1 -> return apothecaryQuest()
                2 -> {
                    chatPlayer(quiz, "Have you got any decent gossip to share?")
                    if (stage == NOT_STARTED) {
                        chatNpc(
                            neutral,
                            "Well I hear young Romeo's having a little woman trouble but other than " +
                                "that all's quiet on the eastern front. Can I do something for you?",
                        )
                    } else {
                        chatNpc(sad, "Sad about that affair with young Romeo and Juliet...")
                        chatNpc(
                            neutral,
                            "I hear every time Romeo sees Juliet now he runs away screaming " +
                                "something about ghosts and Juliet's cousin?",
                        )
                        chatNpc(neutral, "Always did think he was a bit of a strange one...")
                        chatNpc(
                            happy,
                            "Anyway! Life goes on and so does business! Can I do something for you?",
                        )
                    }
                }
                3 -> {
                    chatPlayer(happy, "Do you know a potion to make hair fall out?")
                    chatNpc(
                        happy,
                        "I do indeed. I gave it to my mother. That's why I now live alone.",
                    )
                    chatNpc(neutral, "But can I do something for you?")
                }
                4 -> {
                    chatPlayer(happy, "Have you got any good potions to give away?")
                    chatNpc(sad, "Sorry, charity is not my strong point. Do you need anything else?")
                }
                else -> {
                    chatPlayer(neutral, "No thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.apothecaryQuest() {
        val stage = stage(player)
        when {
            stage == SPOKEN_TO_LAWRENCE -> {
                chatPlayer(
                    neutral,
                    "Apothecary, Father Lawrence sent me. I need a Cadava potion to help Romeo and " +
                        "Juliet.",
                )
                chatNpc(neutral, "Cadava potion. It's pretty nasty. And hard to make.")
                chatNpc(neutral, "Wing of rat, tail of frog. Ear of snake and horn of dog.")
                player.tailoredBush = 0
                quest.advanceQuestStageTo(access, SPOKEN_TO_APOTHECARY)
                chatNpc(neutral, "I have all that, but I need some Cadava berries.")
                chatNpc(
                    neutral,
                    "You will have to find them while I get the rest ready. Bring them here when you " +
                        "have them. But be careful. They are nasty.",
                )
                if (access.inv.count(BERRIES) > 0) {
                    chatPlayer(happy, "Conveniently, I have some here.")
                    brewCadava()
                } else {
                    berryQuestions()
                }
            }
            stage == SPOKEN_TO_APOTHECARY && access.inv.count(POTION) > 0 -> {
                chatPlayer(neutral, "Thank you for the Cadava potion.")
                chatNpc(
                    neutral,
                    "You're welcome. I hope it helps that young couple find happiness.",
                )
            }
            stage == SPOKEN_TO_APOTHECARY && access.inv.count(BERRIES) > 0 -> {
                chatNpc(happy, "Well done. You have the berries.")
                brewCadava()
            }
            stage == SPOKEN_TO_APOTHECARY ->
                chatNpc(
                    neutral,
                    "Keep searching for those Cadavaberries. They're needed for the potion.",
                )
            else -> {
                chatPlayer(neutral, "I gave Juliet the Cadava potion.")
                chatNpc(neutral, "Good, good. I hope it helps that young couple find happiness.")
            }
        }
    }

    private suspend fun Dialogue.berryQuestions() {
        while (true) {
            when (
                menu(
                    "What do these berries look like?" to 1,
                    "Where can I get these berries?" to 2,
                    "How are these berries dangerous?" to 3,
                    "Ok, thanks." to 4,
                )
            ) {
                1 -> {
                    chatPlayer(quiz, "What do these berries look like?")
                    chatNpc(
                        neutral,
                        "They're a nice bright pink colour, with green leaves, stalks are a pale " +
                            "brown colour...you can't miss them. They look pretty tasty, but I " +
                            "wouldn't advise eating them.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Where can I get these berries?")
                    chatNpc(
                        neutral,
                        "I think I saw some recently in the local vicinity. Oh yes, that's right! " +
                            "There was a little bit of commotion when some kids visited the south " +
                            "eastern Varrock mining pit. The trip was disrupted because some kids",
                    )
                    chatNpc(
                        neutral,
                        "had found some cadavaberry bushes and they started picking them. They " +
                            "wrapped that trip up pretty quickly!",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "How are these berries dangerous?")
                    chatNpc(
                        neutral,
                        "Hmm, they're generally okay, but if handled improperly they can be a bit " +
                            "nasty. And I certainly wouldn't advise drinking the potion!",
                    )
                }
                else -> {
                    chatPlayer(neutral, "Ok, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.brewCadava() {
        access.invDel(access.inv, BERRIES)
        val handOver =
            "You hand over the berries, which the Apothecary shakes up in a vial of strange liquid."
        objbox(BERRIES, handOver)
        chatNpc(happy, "Phew! Here is what you need.")
        access.invAdd(access.inv, POTION)
        objbox(POTION, "The Apothecary gives you a Cadava potion.")
    }

    private fun ProtectedAccess.openPotions() {
        ifOpenMainModal("interface.apothecary_potions")
        for (potion in listOf(STRENGTH, ENERGY, ANTIPOISON)) {
            ifSetText("component.apothecary_potions:${potion.key}_title", potion.title)
            ifSetText("component.apothecary_potions:${potion.key}_contents", potion.contents(this))
            ifSetObj("component.apothecary_potions:${potion.key}_pic", potion.product, zoom = 380)
        }
    }

    private fun ProtectedAccess.brew(potion: ApothecaryPotion) {
        if (!potion.hasIngredients(this)) {
            mes("You haven't got the required items.")
            return
        }
        for ((obj, count) in potion.ingredients) {
            invDel(inv, obj, count)
        }
        if (potion.coins > 0) {
            invDel(inv, COINS, potion.coins)
        }
        invAdd(inv, potion.product)
        mes("The Apothecary brews you ${potion.article} ${potion.title.lowercase()}.")
        openPotions()
    }

    private suspend fun ProtectedAccess.drinkPotion() {
        val coords = player.coords
        val inShop = coords.level == 0 && coords.x in 3189..3198 && coords.z in 3400..3407
        if (!inShop) {
            objbox(
                POTION,
                "You dare not drink this outside of the apothecary's establishment. At least there " +
                    "the apothecary may be able to help you if you need assistance.",
            )
            return
        }
        startDialogue {
            val drink =
                choice2(
                    "Yes, I'll drink it all down.",
                    true,
                    "No, I'm having second thoughts now.",
                    false,
                    title = "Are you sure you wish to drink this Cadava Potion?",
                )
            if (!drink) {
                access.mes("You decide against drinking the potion.")
                return@startDialogue
            }
            val defiance =
                "In defiance of Father Lawrence's warnings, you drink the entire contents of the " +
                    "potion."
            objbox(POTION, defiance)
            access.anim("seq.human_eat")
            access.invReplace(access.inv, POTION, 1, "obj.vial_empty")
            access.delay(2)
            player.client.write(CamShake(axis = 3, random = 0, amplitude = 20, rate = 2))
            access.delay(3)
            access.anim("seq.human_death")
            access.say("Urk!")
            chatPlayer(shocked, "Urk!")
            access.mes("Oh dear...you are nearly dead.")
            access.delay(3)
            access.resetAnim()
            mesbox("Some time later.....")
            access.camReset()
            chatNpcSpecific(
                "Apothecary",
                "npc.apothecary",
                shocked,
                "Blimey... you've been out for hours! Are you ok? People were tripping over you as " +
                    "they walked into the shop.",
            )
        }
    }

    private suspend fun ProtectedAccess.pickTailoredBush() {
        arriveDelay()
        if (player.tailoredBush >= 2) {
            mes("There are no berries on this bush. Maybe you should try another bush.")
            return
        }
        if (inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }
        anim("seq.picking_low")
        delay(1)
        player.tailoredBush++
        invAdd(inv, BERRIES)
    }

    private class ApothecaryPotion(
        val key: String,
        val title: String,
        val article: String,
        val product: String,
        val ingredients: List<Pair<String, Int>>,
        val coins: Int,
        val labels: List<String>,
    ) {
        fun hasIngredients(access: ProtectedAccess): Boolean =
            ingredients.all { (obj, count) -> access.inv.count(obj) >= count } &&
                access.inv.count(COINS) >= coins

        fun contents(access: ProtectedAccess): String {
            val lines =
                ingredients.mapIndexed { index, (obj, count) ->
                    strikeUnless(access.inv.count(obj) >= count, labels[index])
                }
            val coinLine =
                if (coins > 0) listOf(strikeUnless(access.inv.count(COINS) >= coins, "$coins coins"))
                else emptyList()
            return (lines + coinLine).joinToString("<br>")
        }

        private fun strikeUnless(has: Boolean, text: String): String =
            if (has) text else "<str>$text</str>"
    }

    private companion object {
        const val NOT_STARTED = 0
        const val STARTED = 10
        const val MESSAGE_GIVEN = 20
        const val MESSAGE_DELIVERED = 30
        const val SPOKEN_TO_LAWRENCE = 40
        const val SPOKEN_TO_APOTHECARY = 50
        const val POTION_GIVEN = 60
        const val COMPLETE = 100

        const val MESSAGE = "obj.julietmessage"
        const val POTION = "obj.cadava"
        const val BERRIES = "obj.cadavaberries"
        const val COINS = "obj.coins"

        const val POTION_JINGLE = "jingle.soul_wars"
        const val CRYPT_JINGLE = "jingle.camdozaal_ruins_2021"
        const val DRINK_SYNTH = "synth.romeo_juliet_drink"
        const val COLLAPSE_SYNTH = "synth.romeo_juliet_collapse"
        const val CRYPT_ENTER_SYNTH = "synth.romeo_juliet_crypt_enter"
        const val CRYPT_DOOR_SYNTH = "synth.romeo_juliet_crypt_door"
        const val CUTSCENE_NPC_TICKS = 200

        val SERMON_CAMERA = CoordGrid(3254, 3486, 0)
        val SERMON_LOOK = CoordGrid(3256, 3476, 0)

        val JULIET_ROOM = CoordGrid(3157, 3425, 1)
        val JULIET_CAMERA = CoordGrid(3153, 3422, 1)
        val JULIET_CAMERA_CLOSE = CoordGrid(3156, 3421, 1)
        val CUTSCENE_JULIET = CoordGrid(3157, 3426, 1)
        val CUTSCENE_PHILLIPA = CoordGrid(3156, 3426, 1)
        val CUTSCENE_DRAUL = CoordGrid(3158, 3426, 1)

        val CRYPT_PLAYER = CoordGrid(2333, 4646, 0)
        val CRYPT_ROMEO = CoordGrid(2333, 4645, 0)
        val CRYPT_PHILLIPA = CoordGrid(2331, 4645, 0)
        val CRYPT_CAMERA = CoordGrid(2330, 4641, 0)
        val CRYPT_LOOK = CoordGrid(2342, 4655, 0)
        val CRYPT_JULIET = CoordGrid(2320, 4639, 0)
        val CRYPT_RETURN = CoordGrid(3215, 3418, 0)
        val ROMEO_PATH =
            listOf(
                CoordGrid(2330, 4645, 0),
                CoordGrid(2327, 4645, 0),
                CoordGrid(2326, 4644, 0),
                CoordGrid(2325, 4644, 0),
                CoordGrid(2323, 4643, 0),
            )
        val PHILLIPA_PATH =
            listOf(
                CoordGrid(2327, 4645, 0),
                CoordGrid(2326, 4644, 0),
                CoordGrid(2325, 4644, 0),
            )

        val STRENGTH =
            ApothecaryPotion(
                key = "strength",
                title = "Strength potion",
                article = "a",
                product = "obj.strength4",
                ingredients = listOf("obj.red_spiders_eggs" to 1, "obj.limpwurt_root" to 1),
                coins = 5,
                labels = listOf("Red spiders' eggs", "Limpwurt root"),
            )
        val ENERGY =
            ApothecaryPotion(
                key = "energy",
                title = "Energy potion",
                article = "an",
                product = "obj.4dose1energy",
                ingredients = listOf("obj.chocolate_dust" to 1, "obj.limpwurt_root" to 2),
                coins = 0,
                labels = listOf("Chocolate dust", "2 limpwurt roots"),
            )
        val ANTIPOISON =
            ApothecaryPotion(
                key = "antipoison",
                title = "Antipoison potion",
                article = "an",
                product = "obj.4doseantipoison",
                ingredients = listOf("obj.cadavaberries" to 1, "obj.limpwurt_root" to 1),
                coins = 5,
                labels = listOf("Cadava berry", "Limpwurt root"),
            )
    }
}
