package org.rsmod.content.quest.area.varrock.romeoandjuliet

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Romeo & Juliet. Progress is varp 144 (`varp.rjquest`) with the OSRS
 * stages Quest Helper reads: 0 talk to Romeo, 10 find Juliet, 20 take her message to Romeo, 30 see
 * Father Lawrence, 40 see the Apothecary, 50 bring him cadava berries and the potion to Juliet,
 * 60 tell Romeo, 100 complete.
 */
class RomeoAndJuliet @Inject constructor(private val objRepo: ObjRepository) :
    QuestScript(
        "quest_romeoandjuliet",
        "varp.rjquest",
        rewards {},
        ItemRewardDisplay(CADAVA_POTION, zoom = 200),
        questVarbit = "varbit.romeo_juliet_progress",
    ) {

    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE) {
            "Romeo & Juliet end state is ${quest.maxSteps} in the cache dbrow, but Romeo completes at $COMPLETE."
        }

        onOpNpc1("npc.romeo") { startDialogue(it.npc) { romeo() } }
        onOpNpc1("npc.juliet") { startDialogue(it.npc) { juliet() } }
        onOpNpc1("npc.juliet_multi_visible") { startDialogue(it.npc) { juliet() } }
        onOpNpc1("npc.father_lawrence") { startDialogue(it.npc) { fatherLawrence() } }
        onOpNpc1("npc.apothecary") { startDialogue(it.npc) { apothecary() } }
        onOpNpc1("npc.draul_leptoc") { startDialogue(it.npc) { draulLeptoc() } }
        onOpNpc1("npc.phillipa") { startDialogue(it.npc) { phillipa() } }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Romeo</col> in <col=800000>Varrock Square</col>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            val stage = stage(player.player)
            description("<red>Romeo</red> asked me to find his love <red>Juliet</red> and tell her that he longs to be with her.")
            objective("Juliet lives in the house west of <red>Varrock Square</red>.") {
                stageAtLeast(TALKED_TO_JULIET, "I found Juliet and she gave me a <red>message</red> for Romeo.").strike()
            }
            if (stage >= TALKED_TO_JULIET) {
                objective("I need to take Juliet's <red>message</red> to Romeo.") {
                    stageAtLeast(MESSAGE_DELIVERED, "Her father opposes their marriage; Romeo told me to see <red>Father Lawrence</red>.").strike()
                }
            }
            if (stage >= MESSAGE_DELIVERED) {
                objective("I need to talk to <red>Father Lawrence</red> at the church in north-east Varrock.") {
                    stageAtLeast(TALKED_TO_LAWRENCE, "He suggested a <red>Cadava potion</red> to make Juliet appear dead.").strike()
                }
            }
            if (stage >= TALKED_TO_LAWRENCE) {
                objective("The <red>Apothecary</red> in south-west Varrock can make the potion from <red>Cadava berries</red>.") {
                    hasItem("cadava", "I have the <red>Cadava potion</red>; I need to take it to Juliet.")
                    stageAtLeast(POTION_GIVEN, "Juliet drank the potion; I need to tell <red>Romeo</red>.").strike()
                }
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line("I passed messages between Romeo and Juliet, and Father Lawrence came up with a plan.")
            line("The Apothecary made a Cadava potion that made Juliet appear dead, so she could be taken to the crypt.")
            line("Romeo went to collect her from the crypt... and met her cousin Phillipa instead.")
        }

    private fun stage(player: Player): Int = quest.getQuestStage(player)

    private fun Dialogue.setStage(stage: Int) {
        quest.setQuestStage(access, stage)
    }

    private fun Dialogue.has(obj: String): Boolean = player.inv.count(obj) > 0

    private suspend fun Dialogue.romeo() {
        when (stage(player)) {
            NOT_STARTED -> romeoStart()
            STARTED -> romeoAgain()
            TALKED_TO_JULIET -> romeoMessage()
            MESSAGE_DELIVERED -> {
                chatPlayer(happy, "Hey again Romeo!")
                romeoLawrenceOptions()
            }
            TALKED_TO_LAWRENCE -> romeoAfterLawrence()
            TALKED_TO_APOTHECARY -> romeoPotion()
            POTION_GIVEN -> romeoFinish()
            else -> chatNpc(happy, "Thank you for all your help, my friend. I have never been happier!")
        }
    }

    private suspend fun Dialogue.romeoStart() {
        chatNpc(sad, "Juliet, Juliet, wherefore art thou Juliet? Have you seen my Juliet?")
        when (
            choice3(
                "Yes, I have seen her actually!", 1,
                "No sorry, I haven't seen her.", 2,
                "Perhaps I could help to find her for you?", 3,
            )
        ) {
            1 -> {
                chatPlayer(happy, "Yes, I have seen her actually!")
                chatPlayer(neutral, "At least, I think it was her... Blonde? A bit stressed?")
                chatNpc(happy, "Golly...yes, yes...you make her sound very interesting!")
                chatNpc(happy, "And I'll bet she's a bit of a fox!")
                chatPlayer(neutral, "Well, I guess she could be considered attractive...")
                chatNpc(laugh, "I'll bet she is!  Wooooooooo!")
                chatNpc(confused, "Sorry, all that jubilation has made me forget what we were talking about.")
                chatPlayer(neutral, "You were asking me about Juliet? You seemed to know her?")
                chatNpc(happy, "Oh yes, Juliet!")
                chatNpc(happy, "The fox...could you tell her that she is the love of my long and that I life to be with her?")
            }
            2 -> {
                chatPlayer(neutral, "No sorry, I haven't seen her.")
                chatNpc(sad, "Oh...well, that's a shame...I was rather hoping you had.")
                chatPlayer(quiz, "Why?  Is she a fugitive?  Does she owe you some money or something?")
                chatNpc(confused, "Hmmm, she might do?  Perhaps she does?  How do you know?")
                chatPlayer(angry, "I don't know? I was asking 'YOU' how 'YOU' know Juliet!")
                chatNpc(happy, "Ahh, yes Juliet, she's my one true love.  Well, one of my one true loves! If you see her, could you tell her that she is the love of my long and that I life to be with her?")
            }
            3 -> {
                chatPlayer(happy, "Perhaps I can help find her for you? What does she look like?")
                chatNpc(happy, "Oh would you? That would be great! She has this sort of hair...")
                chatPlayer(neutral, "Hair...check..")
                chatNpc(happy, "...and she these...great lips...")
                chatPlayer(neutral, "Lips...right.")
                chatNpc(happy, "Oh and she has these lovely shoulders as well..")
                chatPlayer(neutral, "Shoulders...right, so she has hair, lips and shoulders...that should cut it down a bit.")
                chatNpc(happy, "Oh yes, Juliet is very different...please tell her that she is the love of my long and that I life to be with her?")
            }
        }
        chatPlayer(quiz, "What? Surely you mean that she is the love of your life and that you long to be with her?")
        chatNpc(happy, "Oh yeah...what you said...tell her that, it sounds much better!  Oh you're so good at this!")
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "Sorry Romeo, I've got better things to do right now. Maybe later?")
            chatNpc(sad, "Oh, okay, well, I guess my Juliet and I can spend some time apart.  And as the old saying goes, 'Absinthe makes the heart glow longer'.")
            chatPlayer(quiz, "Don't you mean that, 'Absence makes the...")
            chatPlayer(neutral, "Actually forget it...")
            chatNpc(happy, "Okay!")
            return
        }
        chatPlayer(happy, "Yes, okay. I'll let her know.")
        setStage(STARTED)
        chatNpc(happy, "Oh great! And tell her that I want to kiss her a give.")
        chatPlayer(laugh, "You mean you want to give her a kiss!")
        chatNpc(happy, "Oh you're good...you are good!")
        chatNpc(happy, "I see I've picked a true professional...!")
        romeoJulietOptions()
    }

    private suspend fun Dialogue.romeoAgain() {
        chatPlayer(happy, "Hello again, remember me?")
        chatNpc(confused, "Of course, yes....how are.. you....ermmm...")
        chatPlayer(quiz, "You haven't got a clue who I am do you?")
        chatNpc(happy, "Not a clue my friend, but you seem to have a friendly face...a little blood stained, and perhaps in need of a wash, but friendly none the less.")
        chatPlayer(neutral, "You asked me to look for Juliet for you!")
        chatNpc(happy, "Ah yes, Juliet...my sweet darling...what news?")
        chatPlayer(neutral, "Nothing so far, but I need to ask a few questions?")
        romeoJulietOptions()
    }

    private suspend fun Dialogue.romeoJulietOptions() {
        when (
            choice3(
                "Where can I find Juliet?", 1,
                "Is there anything else you can tell me about Juliet?", 2,
                "Ok, thanks.", 3,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "Where can I find Juliet?")
                chatNpc(quiz, "Why do you ask?")
                chatPlayer(neutral, "So that I can try and find her for you!")
                chatNpc(happy, "Ah yes....quite right. Hmmm, let me think now.")
                chatNpc(neutral, "She may still be locked away at her Father's house on the sest vide of Warrock.")
                chatNpc(happy, "Oh, I remember how she loved it when I would sing up to her balcony! She would reward me with her own personal items...")
                chatPlayer(quiz, "What, she just gave you her stuff?")
                chatNpc(happy, "Well, not exactly give...more like 'throw with considerable force'...she's always a kidder that Juliet!")
            }
            2 -> {
                chatPlayer(quiz, "Is there anything else you can tell me about Juliet?")
                chatNpc(happy, "Oh, there is so much to tell...she is my true love, we intend to spend together forever...I can tell you so much about her..")
                chatPlayer(happy, "Great!")
                chatNpc(confused, "Ermmm.....")
                chatNpc(confused, "So much can I tell you...")
                chatPlayer(neutral, "Yes..")
                chatNpc(confused, "So much to tell...why, where do I start!")
                chatPlayer(neutral, "Yes..yes!  Please go on...don't let me interrupt...")
                chatNpc(confused, "Ermmm.....")
                chatNpc(confused, "...")
                chatPlayer(quiz, "You can't remember can you?")
                chatNpc(sad, "Not a thing sorry....")
            }
            3 -> chatPlayer(neutral, "Ok, thanks.")
        }
    }

    private suspend fun Dialogue.romeoMessage() {
        if (!has(MESSAGE)) {
            chatPlayer(happy, "Romeo...great news...I've been in touch with Juliet!")
            chatNpc(happy, "Oh great! That is great news! Well done...well done... what a total success!")
            chatPlayer(neutral, "Yes, and she gave me a message to give you...")
            chatNpc(happy, "Ohhh great! A message....wow!")
            chatPlayer(happy, "Yes!")
            chatNpc(happy, "A message...oh, I can't wait to read what my dear Juliet has to say....")
            chatPlayer(happy, "I know...it's exciting isn't it...?")
            chatNpc(happy, "Yes...yes...")
            chatNpc(neutral, "...")
            chatNpc(quiz, "You've lost the message haven't you?")
            chatPlayer(sad, "Yep, haven't got a clue where it is.")
            return
        }
        chatPlayer(happy, "Romeo...great news...I've been in touch with Juliet! She's written a message for you...")
        access.invDel(access.inv, MESSAGE)
        setStage(MESSAGE_DELIVERED)
        objbox(MESSAGE, "You hand over Juliet's message to Romeo.")
        chatNpc(happy, "Oh, a message! A message! I've never had a message before...")
        chatPlayer(quiz, "Really?")
        chatNpc(neutral, "No, no, not one!")
        chatNpc(neutral, "Oh, well, except for the occasional court summons.")
        chatNpc(happy, "But they're not really 'nice' messages. Not like this one! I'm sure that this message will be lovely.")
        chatPlayer(neutral, "Well are you going to open it or not?")
        chatNpc(happy, "Oh yes, yes, of course!  'Dearest Romeo, I am very pleased that you sent ${player.displayName} to look for me and to tell me that you still hold affliction...', Affliction! She thinks I'm diseased?")
        chatPlayer(neutral, "'Affection?'")
        chatNpc(happy, "Ahh yes...'still hold affection for me. I still feel great affection for you, but unfortunately my Father opposes our marriage.'")
        chatPlayer(worried, "Oh dear...that doesn't sound too good.")
        chatNpc(confused, "What? '...great affection for you. Father opposes our..")
        chatNpc(worried, "'...marriage and will...")
        chatNpc(shocked, "...will kill you if he sees you again!'")
        chatPlayer(worried, "I have to be honest, it's not getting any better...")
        chatNpc(neutral, "'Our only hope is that Father Lawrence, our long time confidant, can help us in some way.'")
        mesbox("Romeo folds the message away.")
        chatNpc(sad, "Well, that's it then...we haven't got a chance...")
        chatPlayer(quiz, "What about Father Lawrence?")
        chatNpc(sad, "...our love is over...the great romance, the life of my love...")
        chatPlayer(neutral, "...or you could speak to Father Lawrence!")
        chatNpc(sad, "Oh, my aching, breaking, heart...how useless the situation is now...we have no one to turn to...")
        chatPlayer(angry, "FATHER LAWRENCE!")
        chatNpc(confused, "Father Lawrence?")
        chatNpc(happy, "Oh yes, Father Lawrence...he's our long time confidant, he might have a solution! Yes, yes, you have to go and talk to Lather Fawrence for us and ask him if he's got any suggestions for our predicament?")
        romeoWhereIsLawrence()
        romeoLawrenceOptions()
    }

    private suspend fun Dialogue.romeoWhereIsLawrence() {
        chatPlayer(quiz, "Where can I find Father Lawrence?")
        chatNpc(happy, "Lather Fawrence! Oh he's...")
        chatNpc(neutral, "You know he's not my 'real' Father don't you?")
        chatPlayer(neutral, "I think I suspected that he wasn't.")
        chatNpc(neutral, "Well anyway...he tells these song, loring bermons...and keeps these here Carrockian vitizens snoring in his church to the East North.")
    }

    private suspend fun Dialogue.romeoLawrenceOptions() {
        when (
            choice4(
                "How are you?", 1,
                "Where can I find Father Lawrence?", 2,
                "Have you heard anything from Juliet?", 3,
                "Ok, thanks.", 4,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "How are you?")
                chatNpc(sad, "Not so good my friend...I miss Judi..., Junie..., Joopie...")
                chatPlayer(neutral, "Juliet?")
                chatNpc(sad, "Juliet! I miss Juliet, terribly!")
                chatPlayer(neutral, "Hmmm, so I see!")
            }
            2 -> romeoWhereIsLawrence()
            3 -> {
                chatPlayer(quiz, "Have you heard anything from Juliet?")
                chatNpc(sad, "Sadly not my friend! And what's worse, her Father has threatened to kill me if he sees me. I mean, that seems a bit harsh!")
                chatPlayer(neutral, "Well, I shouldn't worry too much...you can always run away if you see him...")
                chatNpc(worried, "I just wish I could remember what he looks like! I live in fear of every man I see!")
            }
            4 -> chatPlayer(neutral, "Ok, thanks.")
        }
    }

    private suspend fun Dialogue.romeoAfterLawrence() {
        chatPlayer(happy, "Hello again Romeo!")
        chatNpc(happy, "Ooh did you manage to survive one of Lather Fawrences sermons? I bet not, you were ages! I bet you snoozed on the welcome mat just as soon as you heard his voice!")
        chatPlayer(angry, "Did not!")
        chatNpc(quiz, "Oh, come on, come on, what did he say?")
        when (
            choice3(
                "He wants me to go to the Apothecary!", 1,
                "He seems keen for you to marry Juliet.", 2,
                "Ok, thanks.", 3,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "He wants me to go to the Apothecary!")
                chatNpc(quiz, "The Apothecary?")
                chatNpc(quiz, "Oh...is he the one who mixes up all them magical potion- ey things?")
                chatPlayer(neutral, "Yeah, I think so...but the word potion-ey doesn't exist.")
                chatNpc(happy, "Well, you just used it...so I guess it does exist!")
                chatPlayer(quiz, "It doesn't matter...do you know where the Apothecary is?")
                chatNpc(quiz, "Why should I tell you?")
                chatPlayer(angry, "Because I'm doing you a favour!")
                chatNpc(happy, "Right, yes...of course! Well, I think the potion-ey place is Wouth Sest of here and near a sword shop.")
            }
            2 -> {
                chatPlayer(neutral, "He seems very keen for you to marry Juliet.")
                chatNpc(happy, "Me too! I can't wait! Do you think it will be soon?")
                chatPlayer(neutral, "I don't know, but I'll do what I can. It just seems a bit odd that Father Lawrence is so keen for you to marry?")
                chatNpc(neutral, "I can't imagine why he's so keen? Though, he was our messenger before you. Of course, he had a lot more hair back then...and I think we may have gone to school together...")
                chatPlayer(quiz, "What, since you were kids?")
                chatNpc(laugh, "Yeah! Thinking about it now, we called him 'Diddy Dorrence' back then!")
                chatPlayer(neutral, "So, maybe, he just wants a bit of peace? And when you two are married, he won't have to traipse around delivering your messages anymore?")
                chatNpc(sad, "Yes, the years haven't been good to him.")
            }
            3 -> chatPlayer(neutral, "Ok, thanks.")
        }
    }

    private suspend fun Dialogue.romeoPotion() {
        if (!has(CADAVA_POTION)) {
            chatPlayer(happy, "Hi Romeo!")
            chatNpc(quiz, "Oh hello, have you seen Lather Fawrence?")
            chatPlayer(neutral, "Yes, he's given me details of a potion which should help resolve this situation. The Apothecary is helping me prepare it.")
            chatNpc(worried, "Ooooh, it all sounds horribly complicated?")
            chatNpc(happy, "All I can say is I'll be glad when Juliet's finally in that crypt!")
            chatPlayer(neutral, "Spoken like a true lover!")
            return
        }
        mesbox("Romeo spots the Cadava potion.")
        chatNpc(happy, "Ooohh, that's the potion is it? Rather you than me!")
        chatPlayer(angry, "I'm not drinking it! It's for Juliet!")
        chatNpc(shifty, "Well, I mean...it's probably delicious...and not dangerous at all. And you quite often see harmless drinks in a glowing sort of pink colour.")
        chatPlayer(quiz, "Hmm, I'm not convinced...maybe you ought to try some before I give it to Juliet?")
        chatNpc(shocked, "Urgh! No way!  Not in a million years!")
        chatPlayer(quiz, "Ok...but you know what to do when I've given this to Juliet?")
        chatNpc(happy, "Oh, yeah...sure Lather Fawrence explained all that to me...")
    }

    private suspend fun Dialogue.romeoFinish() {
        chatPlayer(happy, "Romeo, it's all set. Juliet has drunk the potion and has been taken down into the Crypt...now you just need to pop along and collect her.")
        chatNpc(happy, "Ah right, the potion! Great...")
        chatNpc(confused, "What potion would that be then?")
        chatPlayer(neutral, "The Cadava potion...you know, the one which will make her appear dead! She's in the crypt, pop along and claim your true love.")
        chatNpc(worried, "But I'm scared...will you come with me?")
        chatPlayer(neutral, "Oh, ok...come on! I think I saw the entrance when I visited there last...")
        chatNpc(worried, "This is pretty scary...")
        chatPlayer(neutral, "Oh, be quiet...")
        chatPlayer(neutral, "We're here. Look, Juliet is over there!")
        chatPlayer(neutral, "You go over to her...and I'll go and wait over here...")
        chatNpc(worried, "Ohhh, ok then...")
        chatNpc(neutral, "Hey...Juliet...")
        chatNpc(quiz, "Juliet....?")
        chatNpc(sad, "Oh dear...you seem to be dead.")
        chatNpcSpecific("Phillipa", "npc.phillipa", happy, "Hi Romeo...I'm Phillipa!")
        chatNpc(happy, "Wow! You're a fox!")
        chatNpcSpecific("Phillipa", "npc.phillipa", happy, "It's a shame about Juliet...but perhaps we can meet up later?")
        chatNpc(confused, "Who's Juliet?")
        quest.completeQuest(access)
    }

    private suspend fun Dialogue.juliet() {
        when (stage(player)) {
            NOT_STARTED -> mesbox("She seems to be lost in thought.")
            STARTED -> {
                chatPlayer(happy, "Juliet, I come from Romeo.  He begs me to tell you that he cares still.")
                chatNpc(happy, "Oh how my heart soars to hear this news! Please take this message to him with great haste.")
                chatPlayer(neutral, "Well, I hope it's good news...he was quite upset when I left him.")
                chatNpc(sad, "He's quite often upset...the poor sensitive soul. But I don't think he's going to take this news very well, however, all is not lost.")
                chatNpc(neutral, "Everything is explained in the letter, would you be so kind and deliver it to him please?")
                chatPlayer(happy, "Certainly, I'll do so straight away.")
                chatNpc(happy, "Many thanks! Oh, I'm so very grateful. You may be our only hope.")
                access.invAddOrDrop(objRepo, MESSAGE)
                setStage(TALKED_TO_JULIET)
                objbox(MESSAGE, "Juliet gives you a message.")
            }
            TALKED_TO_JULIET -> {
                chatPlayer(happy, "Hello Juliet!")
                chatNpc(quiz, "Hello there...have you delivered the message to Romeo yet? What news do you have from my loved one?")
                if (has(MESSAGE)) {
                    chatPlayer(sad, "Oh, sorry, I've not had chance to deliver it yet!")
                    chatNpc(sad, "Oh, that's a shame. I've been waiting so patiently to hear some word from him.")
                    return
                }
                chatPlayer(shifty, "Hmmm, that's the thing about messages...they're so easy to misplace...")
                chatNpc(angry, "How could you lose that message? It was incredibly important...and it took me an age to write! I used joined up writing and everything!")
                chatNpc(neutral, "Please, take this new message to him, and please don't lose it.")
                access.invAddOrDrop(objRepo, MESSAGE)
                objbox(MESSAGE, "Juliet gives you another message.")
            }
            MESSAGE_DELIVERED -> {
                chatPlayer(neutral, "Hi Juliet, I have passed your message on to Romeo..he's scared half out of his wits at the news that your father wants to kill him.")
                chatNpc(sad, "Yes, unfortunately my father is quite the hunter, you may have seen some the animal head trophies on the wall. And it would be so awful to see Romeo's head up there with them!")
                chatPlayer(neutral, "I know what you mean...")
                chatPlayer(laugh, "...his hair colour will clash terribly with the rest of the decoration.")
                chatNpc(angry, "That's not what I was suggesting at all...")
                chatPlayer(laugh, "I know, I know...I was just kidding.")
                chatPlayer(neutral, "Anyway, don't worry because I'm on the case. I'm going to get some help from Father Lawrence.")
                chatNpc(happy, "Oh yes, I'm sure that Father Lawrence will come up with a solution. I hope you find him soon.")
            }
            TALKED_TO_LAWRENCE -> {
                chatPlayer(happy, "Hi Juliet, I've found Father Lawrence and he has a cunning plan. But for it to work, I need to seek the Apothecary!")
                chatNpc(happy, "Oh good! I knew Father Lawrence would come up with something. However, I don't know where the apothecary is...I hope you find him soon. My father's temper gets no better.")
            }
            TALKED_TO_APOTHECARY -> julietPotion()
            else -> mesbox("She seems to be lost in thought.")
        }
    }

    private suspend fun Dialogue.julietPotion() {
        if (!has(CADAVA_POTION)) {
            chatPlayer(happy, "Hi Juliet!")
            chatNpc(quiz, "Hi ${player.displayName}, how close am I to being with my true love Romeo?")
            chatPlayer(neutral, "Sorry, I still have to get a special potion for you.")
            chatNpc(neutral, "Oh, I hope it isn't a love potion because you would be wasting your time. My love for Romeo grows stronger every minute...")
            chatPlayer(neutral, "That must be because you're not with him...")
            chatNpc(sad, "Oh no! I long to be close to my true love Romeo!")
            chatPlayer(neutral, "Well, ok then...I'll set about getting this potion as quickly as I can!")
            chatNpc(happy, "Fair luck to you, the end is close.")
            return
        }
        chatPlayer(happy, "Hi Juliet! I have an interesting proposition for you...suggested by Father Lawrence. It may be the only way you'll be able to escape from this house and be with Romeo.")
        chatNpc(quiz, "Go on....")
        chatPlayer(neutral, "I have a Cadava potion here, suggested by Father Lawrence. If you drink it, it will make you appear dead!")
        chatNpc(quiz, "Yes...")
        chatPlayer(neutral, "And when you appear dead...your still and lifeless corpse will be removed to the crypt!")
        chatNpc(worried, "Oooooh, a cold dark creepy crypt...")
        chatNpc(shifty, "...sounds just peachy!")
        chatPlayer(happy, "Then...Romeo can steal into the crypt and rescue you just as you wake up!")
        chatNpc(confused, "...and this is the great idea for getting me out of here?")
        chatPlayer(shifty, "To be fair, I can't take all the credit...in fact...it was all Father Lawrence's suggestion...")
        chatNpc(neutral, "Ok...if this is the best we can do...hand over the potion!")
        access.invDel(access.inv, CADAVA_POTION)
        setStage(POTION_GIVEN)
        objbox(CADAVA_POTION, "You pass the suspicious potion to Juliet.")
        chatNpc(happy, "Wonderful! I just hope Romeo can remember to get me from the crypt.")
        chatNpc(neutral, "Please go to Romeo and make sure he understands. Although I love his gormless, lovelorn soppy ways, he can be a bit dense sometimes and I don't want to wake up in that crypt on my own.")
    }

    private suspend fun Dialogue.fatherLawrence() {
        when (stage(player)) {
            MESSAGE_DELIVERED -> {
                chatNpc(neutral, "''...and let Saradomin light the way for you... '' Urgh!")
                chatNpc(angry, "Can't you see that I'm in the middle of a Sermon?!")
                chatPlayer(neutral, "But Romeo sent me!")
                chatNpc(angry, "But I'm busy delivering a sermon to my congregation!")
                chatPlayer(laugh, "Yes, well, it certainly seems like you have a captive audience!")
                chatNpc(neutral, "Ok, ok...what do you want so I can get rid of you and continue with my sermon?")
                chatPlayer(neutral, "Romeo sent me. He says you may be able to help.")
                chatNpc(neutral, "Ah Romeo, yes. A fine lad, but a little bit confused.")
                chatPlayer(neutral, "Yes, very confused....Anyway, Romeo wishes to be married to Juliet! She must be rescued from her father's control!")
                chatNpc(happy, "I agree, and I think I have an idea! A potion to make her appear dead...")
                chatPlayer(worried, "Dead! Sounds a bit creepy to me...but please, continue.")
                chatNpc(neutral, "The potion will only make Juliet 'appear' dead...then she'll be taken to the crypt...")
                chatPlayer(worried, "Crypt! Again...very creepy! You must have some strange hobbies.")
                chatNpc(neutral, "Then Romeo can collect her from the crypt! Go to the Apothecary, tell him I sent you and that you'll need a 'Cadava' potion.")
                setStage(TALKED_TO_LAWRENCE)
                chatPlayer(happy, "Apart from the strong overtones of death, this is turning out to be a real love story.")
            }
            TALKED_TO_LAWRENCE ->
                chatNpc(neutral, "Ah, have you found the Apothecary yet? Remember, Cadava potion, for Juliet.")
            TALKED_TO_APOTHECARY -> {
                chatNpc(quiz, "Did you find the Apothecary?")
                if (has(CADAVA_POTION)) {
                    chatPlayer(happy, "I've got the cadava potion.")
                    chatNpc(happy, "Good! Good work! Ok, take it to Juliet, she's expecting you. I'll talk to Romeo and make sure he knows what to do.")
                    return
                }
                chatPlayer(neutral, "Yes I did. He's told me I must find some Cadava berries.")
                chatNpc(neutral, "Well, good luck with that...they're quite tricky to find.")
                chatPlayer(quiz, "Any clues where I can start to look?")
                chatNpc(neutral, "I heard some kids saying they saw some the other day. They were visiting the mining place to the south east of Varrock.")
                chatPlayer(neutral, "Ok, that's as good a place to start looking as any.")
            }
            else -> chatNpc(happy, "Top of the morning to you.")
        }
    }

    private suspend fun Dialogue.apothecary() {
        chatNpc(neutral, "I am the Apothecary. I brew potions. Do you need anything specific?")
        when (choice2("Can you make potions for me?", 1, "Talk about something else.", 2)) {
            1 -> {
                chatPlayer(quiz, "Can you make potions for me?")
                chatNpc(neutral, "I'm afraid I'm too busy to brew anything for you right now.")
            }
            2 -> apothecaryOtherTopics()
        }
    }

    private suspend fun Dialogue.apothecaryOtherTopics() {
        val questTopic = stage(player) >= TALKED_TO_LAWRENCE
        val options = buildList {
            if (questTopic) add("Talk about Romeo & Juliet." to 1)
            add("Do you know a potion to make hair fall out?" to 2)
            add("Have you got any good potions to give away?" to 3)
            add("No thanks." to 4)
        }
        val choice =
            if (options.size == 4) {
                choice4(options[0].first, options[0].second, options[1].first, options[1].second,
                    options[2].first, options[2].second, options[3].first, options[3].second)
            } else {
                choice3(options[0].first, options[0].second, options[1].first, options[1].second,
                    options[2].first, options[2].second)
            }
        when (choice) {
            1 -> apothecaryRomeoAndJuliet()
            2 -> {
                chatPlayer(quiz, "Do you know a potion to make hair fall out?")
                chatNpc(neutral, "I do indeed. I gave it to my mother. That's why I now live alone.")
            }
            3 -> {
                chatPlayer(quiz, "Have you got any good potions to give away?")
                chatNpc(neutral, "Sorry, charity is not my strong point.")
            }
            4 -> chatPlayer(neutral, "No thanks.")
        }
    }

    private suspend fun Dialogue.apothecaryRomeoAndJuliet() {
        when (stage(player)) {
            TALKED_TO_LAWRENCE -> {
                chatPlayer(neutral, "Apothecary, Father Lawrence sent me. I need a Cadava potion to help Romeo and Juliet.")
                chatNpc(neutral, "Cadava potion. It's pretty nasty. And hard to make.")
                chatNpc(neutral, "Wing of rat, tail of frog. Ear of snake and horn of dog.")
                chatNpc(neutral, "I have all that, but I need some Cadava berries.")
                chatNpc(neutral, "You will have to find them while I get the rest ready. Bring them here when you have them. But be careful. They are nasty.")
                setStage(TALKED_TO_APOTHECARY)
                if (has(CADAVA_BERRIES)) {
                    chatPlayer(happy, "Conveniently, I have some here.")
                    makePotion()
                    return
                }
                berryQuestions()
            }
            TALKED_TO_APOTHECARY -> when {
                has(CADAVA_BERRIES) && !has(CADAVA_POTION) -> {
                    chatNpc(happy, "Well done. You have the berries.")
                    makePotion()
                }
                has(CADAVA_POTION) -> {
                    chatPlayer(happy, "Thank you for the Cadava potion.")
                    chatNpc(happy, "You're welcome. I hope it helps that young couple find happiness.")
                }
                else -> chatNpc(neutral, "Keep searching for those Cadavaberries. They're needed for the potion.")
            }
            else -> {
                chatPlayer(happy, "I gave Juliet the Cadava potion.")
                chatNpc(happy, "Good, good. I hope it helps that young couple find happiness.")
            }
        }
    }

    private suspend fun Dialogue.makePotion() {
        access.invDel(access.inv, CADAVA_BERRIES)
        mesbox("You hand over the berries, which the Apothecary shakes up in a vial of strange liquid.")
        chatNpc(happy, "Phew! Here is what you need.")
        access.invAddOrDrop(objRepo, CADAVA_POTION)
        objbox(CADAVA_POTION, "The Apothecary gives you a Cadava potion.")
    }

    private suspend fun Dialogue.berryQuestions() {
        when (
            choice4(
                "What do these berries look like?", 1,
                "Where can I get these berries?", 2,
                "How are these berries dangerous?", 3,
                "Ok, thanks.", 4,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "What do these berries look like?")
                chatNpc(neutral, "They're a nice bright pink colour, with green leaves, stalks are a pale brown colour...you can't miss them. They look pretty tasty, but I wouldn't advise eating them.")
                berryQuestions()
            }
            2 -> {
                chatPlayer(quiz, "Where can I get these berries?")
                chatNpc(neutral, "I think I saw some recently in the local vicinity. Oh yes, that's right! There was a little bit of commotion when some kids visited the south eastern Varrock mining pit. The trip was disrupted because some kids")
                chatNpc(neutral, "had found some cadavaberry bushes and they started picking them. They wrapped that trip up pretty quickly!")
                berryQuestions()
            }
            3 -> {
                chatPlayer(quiz, "How are these berries dangerous?")
                chatNpc(neutral, "Hmm, they're generally okay, but if handled improperly they can be a bit nasty. And I certainly wouldn't advise drinking the potion!")
                berryQuestions()
            }
            4 -> chatPlayer(neutral, "Ok, thanks.")
        }
    }

    private suspend fun Dialogue.draulLeptoc() {
        when (stage(player)) {
            TALKED_TO_JULIET -> {
                chatNpc(angry, "What are you doing in my house? Up to no good I shouldn't wonder!")
                chatPlayer(happy, "Just a small chore for Juliet, you do have a lovely daughter in her sir.")
                chatNpc(happy, "Oh...why, thank you...I've always tried to my best...")
                chatNpc(angry, "...Hang on! Enough of that smiley talk. I have a daughter and I know what she's like. Don't even think of carrying on anything behind my back, I have the eyes of a hawk, nothing gets past me!")
            }
            MESSAGE_DELIVERED ->
                chatNpc(angry, "Do you live here? If so, how's about a couple of hundred gold towards the rent eh? Pay your share I say...you don't want to be like that freeloading Romeo!")
            TALKED_TO_LAWRENCE, TALKED_TO_APOTHECARY -> {
                chatNpc(angry, "Hey , what are you doing here?")
                chatPlayer(shifty, "Nothing much sir...I promise...I'm just an innocent friend of Juliet's...doing her a few favours...as a friend.")
                chatNpc(angry, "Well, just make sure there's no funny business going on that's all I can say.")
            }
            POTION_GIVEN, COMPLETE ->
                chatNpc(sad, "My poor Juliet....she's dead...dead! I shouldn't have been so hard on her...she was my lovely daughter. Booo hoooo hooooo!")
            else -> {
                chatNpc(angry, "What are you doing here? Snooping around...")
                chatPlayer(neutral, "Oh...just looking around...")
                chatNpc(angry, "Just looking around! This is MY house!  You might have at least 'ASKED' to view my considerably well appointed abode...but no, you've just burst in with all the elegance of a Troll at a tea party.")
                chatPlayer(neutral, "I can see that you're busy ranting so I'll just nip off and investigate a bit.")
            }
        }
    }

    private suspend fun Dialogue.phillipa() {
        when (stage(player)) {
            in TALKED_TO_JULIET until POTION_GIVEN -> {
                chatNpc(happy, "Oh, hello. Juliet has told me what you're doing for her and Romeo, and I have to say I'm very grateful to you. Juliet deserves a bit of happiness in her life.")
                chatPlayer(happy, "Oh, thanks. I like to do my cupid bit.")
            }
            POTION_GIVEN -> {
                chatNpc(happy, "Oh, hello again! How was I - do you think I was convincing?")
                chatPlayer(happy, "Oh, yes, totally!")
                chatNpc(happy, "Ohhh good! I do hope Juliet will be pleased as well! That dashing young Romeo must be beside himself with excitement! Have you told him the good news yet?")
                chatPlayer(neutral, "Not yet, but soon!")
            }
            else -> {
                chatPlayer(happy, "Hello.")
                chatNpc(happy, "Hi, I'm Phillipa, Juliet's cousin. I like to keep an eye on her, make sure that dashing young Romeo doesn't just steal her away under our plain old noses!")
            }
        }
    }

    private companion object {
        const val NOT_STARTED = 0
        const val STARTED = 10
        const val TALKED_TO_JULIET = 20
        const val MESSAGE_DELIVERED = 30
        const val TALKED_TO_LAWRENCE = 40
        const val TALKED_TO_APOTHECARY = 50
        const val POTION_GIVEN = 60
        const val COMPLETE = 100

        const val MESSAGE = "obj.julietmessage"
        const val CADAVA_BERRIES = "obj.cadavaberries"
        const val CADAVA_POTION = "obj.cadava"
    }
}
