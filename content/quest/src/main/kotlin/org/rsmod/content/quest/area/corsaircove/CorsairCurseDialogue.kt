package org.rsmod.content.quest.area.corsaircove

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.ScriptContext

internal class CorsairCurseDialogue(
    private val quest: Quest,
    private val cutscene: CorsairCurseCutscene,
) {

    fun ScriptContext.register() {
        onOpNpc1(CorsairCurseRefs.CaptainCrossroads) { startDialogue(it.npc) { crossroadsTock() } }

        onOpNpc1(CorsairCurseRefs.NavigatorSick) { startDialogue(it.npc) { ithoi() } }
        onOpNpc1(CorsairCurseRefs.Navigator) { startDialogue(it.npc) { ithoi() } }
        val whip = checkNotNull(ServerCacheManager.getItem(WhipObj.asRSCM(RSCMType.OBJ)))
        val feather = checkNotNull(ServerCacheManager.getItem(FeatherObj.asRSCM(RSCMType.OBJ)))
        for (npc in listOf(CorsairCurseRefs.NavigatorSick, CorsairCurseRefs.Navigator)) {
            val type = checkNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)))
            onOpNpcU(type, whip) { startDialogue(it.npc) { ithoiItemTease() } }
            onOpNpcU(type, feather) { startDialogue(it.npc) { ithoiItemTease() } }
        }

        onOpNpc1(CorsairCurseRefs.CabinBoySick) { startDialogue(it.npc) { colin() } }
        onOpNpc1(CorsairCurseRefs.CabinBoy) { startDialogue(it.npc) { colin() } }

        onOpNpc1(CorsairCurseRefs.CookSick) { startDialogue(it.npc) { gnocci() } }
        onOpNpc1(CorsairCurseRefs.Cook) { startDialogue(it.npc) { gnocci() } }

        onOpNpc1(CorsairCurseRefs.ThiefSick) { talkToArsen(it.npc) }
        onOpNpc1(CorsairCurseRefs.Thief) { talkToArsen(it.npc) }
        onOpNpc1(CorsairCurseRefs.ThiefPostquest) { talkToArsen(it.npc) }

        onOpNpc1(CorsairCurseRefs.Tess) { startDialogue(it.npc) { tess() } }

        onOpNpc1(CorsairCurseRefs.CaptainThrone) { startDialogue(it.npc) { throneTock() } }
        onOpNpc1(CorsairCurseRefs.CaptainThroneChild) { startDialogue(it.npc) { throneTock() } }
    }

    internal suspend fun ProtectedAccess.talkToCrossroadsTock(npc: Npc) {
        startDialogue(npc) { crossroadsTock() }
    }

    private suspend fun ProtectedAccess.talkToArsen(npc: Npc) {
        startDialogue(npc) { arsen() }
    }

    private fun ProtectedAccess.closeChat() {
        ifCloseSub("interface.chat_left")
        ifCloseSub("interface.chat_right")
    }

    private suspend fun Dialogue.tooSickToChat() {
        chatNpc(sad, "Urgh...")
        mesbox("He looks too sick to chat.")
    }

    private suspend fun Dialogue.throneTock() {
        if (stage < CorsairCurseStage.Complete) {
            chatNpc(neutral, "Arrr.")
            return
        }
        while (true) {
            chatNpc(
                happy,
                "Arrr, 'tis the young $ladOrLass who helped save my crew from that scoundrel, " +
                    "Ithoi! How be ye this fine day?",
            )
            when (
                choice3(
                    "I'm fine, thanks.",
                    1,
                    "What is there to do here?",
                    2,
                    "Eh, goodbye.",
                    3,
                )
            ) {
                1 -> {
                    chatPlayer(happy, "I'm fine, thanks.")
                    chatNpc(
                        happy,
                        "Arrr, ye looks fine. Stiff in the spine, firm in the mind an' long in the " +
                            "leg. 'Tis good to see ye again.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What is there to do here?")
                    chatNpc(
                        happy,
                        "Well, ye could maybe explore the ogre caves. Arsen says the ogres be " +
                            "fearsome, though, so watch yer step. An' there be places for ye to " +
                            "train yer skills, over to the west.",
                    )
                    chatPlayer(neutral, "Thanks.")
                    return
                }
                else -> {
                    chatPlayer(neutral, "Eh, goodbye.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.crossroadsTock() {
        when (stage) {
            CorsairCurseStage.NotStarted -> tockOpener()
            CorsairCurseStage.Started -> tockSetsOff()
            else -> chatNpc(neutral, TockShipLine)
        }
    }

    private suspend fun Dialogue.tockOpener() {
        chatNpc(sad, "Ahoy, young $ladOrLass.")
        chatNpc(
            sad,
            "Ahoy, young $ladOrLass. Might ye be able to help me, perhaps? I be in a spot of " +
                "bother.",
        )

        while (true) {
            when (choice3("Who are you?", 1, "What kind of help do you need?", 2, TockRefuse, 3)) {
                1 -> tockWhoAreYou()
                2 -> {
                    tockExplainsTheCurse()
                    return
                }
                else -> {
                    chatPlayer(neutral, TockRefuse)
                    chatNpc(neutral, "Arrr. Some other time, perhaps...")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.tockWhoAreYou() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            happy,
            "I be the Corsair Captain! Captain of the Corsairs of Corsair Cove! My gallant band " +
                "of corsairs are famed in song and story among all seafarers.",
        )
        when (choice2("I've never heard of you.", 1, "That's very interesting; do tell me more.", 2)) {
            1 -> {
                chatPlayer(neutral, "I've never heard of you.")
                chatNpc(
                    neutral,
                    "Arrr... well, ye can boast of yer ignorance if ye likes, but it don't make ye " +
                        "sound smarter.",
                )
                chatPlayer(neutral, "...")
                chatNpc(
                    neutral,
                    "Well, as I were saying, young $ladOrLass, I be in a spot of bother, and I " +
                        "thought ye might be able to help.",
                )
            }
            else -> {
                chatPlayer(happy, "That's very interesting; do tell me more.")
                chatNpc(
                    neutral,
                    "Well, as I were saying, young $ladOrLass, I be in a spot of bother, and I " +
                        "thought ye might be able to help.",
                )
            }
        }
    }

    private suspend fun Dialogue.tockExplainsTheCurse() {
        chatPlayer(quiz, "What kind of help do you need?")
        chatNpc(
            sad,
            "It be about my crew. They be a lazy bunch at times, but they be stout in the heart " +
                "and strong in the arm, and they don't deserve what's happened to them...",
        )
        chatNpc(
            shifty,
            "It be a curse! A curse has been laid upon my crew! That's the only thing " +
                "it can be.",
        )
        chatNpc(
            sad,
            "So I need a young $ladOrLass such as yerself to find a way to lift the curse. " +
                "Will ye help me?",
        )

        if (player.combatLevel < CorsairCurseRefs.RecommendedCombatLevel) {
            mesbox(
                "Before starting this quest, be aware that your combat level is lower than the " +
                    "recommended level of ${CorsairCurseRefs.RecommendedCombatLevel}."
            )
        }

        val accept = choice2("Yes.", true, "No.", false, title = "Start The Corsair Curse quest?")
        if (!accept) {
            chatNpc(neutral, "Arrr. Some other time, perhaps...")
            return
        }

        quest.setQuestStage(access, CorsairCurseStage.Started)

        chatPlayer(happy, "Sure, I'll try to help with your curse.")
        chatNpc(
            happy,
            "Arrr! Thanks, young $ladOrLass. And a worthy $ladOrLass ye must surely be, brave in " +
                "the heart and stiff in the spine.",
        )

        chatNpc(neutral, TockShipLine)
        tockSetsOff()
    }

    private suspend fun Dialogue.tockSetsOff() {
        access.closeChat()
        cutscene.rimmingtonFlyby(access)

        chatNpc(neutral, TockShipLine)
        chatPlayer(
            happy,
            "West of Rimmington, you say? Okay, I'll meet you on board your ship there.",
        )

        quest.setQuestStage(access, CorsairCurseStage.TockSailedAhead)
        access.soundSynth(WalkOffSound)
        mesbox("Captain Tock swaggers off towards Rimmington.")
    }

    private suspend fun Dialogue.ithoi() {
        if (stage < CorsairCurseStage.AtTheCove) {
            tooSickToChat()
            return
        }
        when {
            stage >= CorsairCurseStage.IthoiExposed -> ithoiAfterExposed()
            stage >= CorsairCurseStage.IthoiConfessed -> ithoiRefusesToGetUp()
            stage >= CorsairCurseStage.ArsenNamedMotive -> ithoiAccused()
            stage >= CorsairCurseStage.TockToldOfDinner -> ithoiDinnerQuestion()
            access.corscursNavigator >= CorsairCurseCounts.IthoiInterviewed -> ithoiInvestigation()
            else -> ithoiInterview()
        }
    }

    private suspend fun Dialogue.ithoiItemTease() {
        chatNpc(
            angry,
            "Arrr, Old School RuneScape isn't that sort of fantasy game, in case ye hadn't been " +
                "told that a-fore today.",
        )
    }

    private suspend fun Dialogue.ithoiInterview() {
        chatNpc(sad, "Arrr, we be cursed, we be CURSED! It be a curse upon us!")

        val ask = choice2(CursedOpener, true, "I think I'll leave you to get on with it.", false)
        if (!ask) {
            chatPlayer(neutral, "I think I'll leave you to get on with it.")
            chatNpc(sad, "... arrr.")
            return
        }

        chatPlayer(happy, CursedOpener)
        chatNpc(angry, "Aye? D'ye think so? Really? Am I not a picture of health an' beauty?")
        chatNpc(angry, "YES - WE'VE BEEN CURSED!")
        chatNpc(
            sad,
            "I've sailed the seas all my life. I sailed the seas as a man, like I " +
                "sailed 'em as a boy.",
        )
        chatNpc(
            shifty,
            "I even sailed the seas during that tricky phase in between, when ye'r not quite a " +
                "man yet, but ye'r not really a boy anymore. But I don't want to talk about that " +
                "bit.",
        )
        chatPlayer(confused, "Um...")
        chatNpc(
            angry,
            "The point I'm makin' is that I know the seas like I know the palm of my hand. I've " +
                "been to strange lands an' seen things that'll make yer juices turn in yer veins.",
        )
        chatNpc(
            shifty,
            "I've seen dancing fish-women, monkey-kings, huge men in turbans from the far West " +
                "selling treasure maps - you name it, an' I've seen it.",
        )
        chatNpc(
            laugh,
            "There be more things in the world than ye dreams of in yer fancy book-learnin'! An' " +
                "most of 'em be hostile. Could be any number of things brought this curse upon us. " +
                "Demons, evil spirits, anything!",
        )

        access.corscursNavigator = CorsairCurseCounts.IthoiInterviewed

        chatPlayer(neutral, "Okay, it doesn't sound like you know what's behind the curse. Thanks.")
        chatNpc(shifty, "Arrr!")

        access.checkTheoriesRuledOut()
    }

    private suspend fun Dialogue.ithoiInvestigation() {
        chatNpc(
            quiz,
            "Ye be back? Tell me, do those scurvy Corsairs take the curse seriously?",
        )
        chatPlayer(quiz, "I've been hearing about what might have caused it...")
        chatPlayer(
            neutral,
            "Colin thought he upset a mermaid, and she might have cursed you all in revenge.",
        )
        chatNpc(
            sad,
            "Arrr! That'll teach him to meddle with such folk. Colin should stick to what he's " +
                "good at.",
        )
        chatPlayer(
            neutral,
            "Gnocci fished a doll out of the water, and he thought it might have been possessed by " +
                "an evil spirit.",
        )
        chatNpc(
            laugh,
            "Arrr chuckie chuckie chuckie! That'll teach him to play with dolls at his age. Dolls " +
                "can be frightening.",
        )
        chatPlayer(
            neutral,
            "Arsen stole what he thought was a sacred relic from the ogre cave, and he said their " +
                "shamans might have cursed you in revenge.",
        )
        chatNpc(
            shifty,
            "Arrr! Ogre women be powerful, an' Arsen be a fool.",
        )
        chatPlayer(neutral, "I'll carry on trying to solve the curse.")
        chatNpc(neutral, "Arrr! Off ye goes...")
    }

    private suspend fun Dialogue.ithoiDinnerQuestion() {
        chatNpc(quiz, "Ye be back? Have ye been investigatin' the curse?")
        val ask = choice2("I hear it happened straight after dinner.", true, StillLooking, false)
        if (!ask) {
            chatPlayer(neutral, StillLooking)
            return
        }
        chatPlayer(quiz, "I hear it happened straight after dinner.")
        chatNpc(
            angry,
            "Arrr! That be wrong! I were away for a parlay with Captain Tock, an' I didn't eat " +
                "that dinner at all. The Captain can tell ye so himself.",
        )
        chatNpc(angry, "So it must be a curse. A CURSE!")
        chatPlayer(neutral, "Okay, thanks.")
    }

    private suspend fun Dialogue.ithoiAccused() {
        chatNpc(quiz, "Ye be back? Have ye been investigatin' the curse?")

        val accuse =
            choice2(
                "I hear you cooked the meal they ate before getting sick.",
                true,
                StillLooking,
                false,
            )
        if (!accuse) {
            chatPlayer(neutral, StillLooking)
            return
        }

        chatPlayer(angry, "I hear you cooked the meal they ate before getting sick.")
        chatNpc(
            angry,
            "Arrr? And what if I did? Can't an old sailor help his crew without interfering young " +
                "land-lubbers making accusations?",
        )
        chatNpc(
            shifty,
            "Besides, if ye be sayin' I poisoned my crew, why would I do such a thing? Why would " +
                "I poison 'em an' then pretend to be sick too?",
        )

        val motive =
            choice3(
                "Maybe because the Captain's thinking of firing you.",
                1,
                "Maybe you're just a nasty person.",
                2,
                "I don't know your reason.",
                3,
            )

        if (motive != 1) {
            chatPlayer(
                neutral,
                if (motive == 2) "Maybe you're just a nasty person."
                else "I don't know your reason.",
            )
            chatNpc(
                angry,
                "Bah! Ye can't even think of a reason why I'd want to poison the crew, so don't " +
                    "bother me with yer accusations.",
            )
            return
        }

        chatPlayer(
            quiz,
            "Maybe because the Captain's thinking of firing you - Arsen heard it from his " +
                "brother. The Corsairs don't need a navigator anymore.",
        )

        quest.setQuestStage(access, CorsairCurseStage.IthoiConfessed)

        chatNpc(
            shocked,
            "Arrr! Too clever ye be... I heard the Captain might kick me off the crew, so I " +
                "poisoned the dinner, an' acted sick too. Now they think they're cursed, so " +
                "they'll want me around, wi' my knowledge o' such things!",
        )
        chatNpc(
            laugh,
            "But they all think I'm sick, so they'll never believe I'm behind it. Hahaha!",
        )
        chatPlayer(angry, "We'll see what they have to say about that!")
    }

    private suspend fun Dialogue.ithoiRefusesToGetUp() {
        chatNpc(angry, "What d'ye want now?")

        val accuse =
            choice2("I know you've faked the curse.", true, "Um... nothing, really.", false)
        if (!accuse) {
            chatPlayer(neutral, "Um... nothing, really.")
            return
        }

        chatPlayer(angry, "I know you've faked the curse.")

        if (stage < CorsairCurseStage.IthoiChallenged) {
            quest.setQuestStage(access, CorsairCurseStage.IthoiChallenged)
        }

        chatNpc(
            laugh,
            "The Corsairs all think I'm sick too, even though I never ate the food. An' ye'll " +
                "never make me prove that I'm healthy - I ain't gettin' up! So they'll keep " +
                "believing in the curse.",
        )

        val bet =
            choice2("I bet I can prove you're well enough to get up.", true, "I'll be back.", false)
        if (!bet) {
            chatPlayer(angry, "I'll be back.")
            return
        }

        chatPlayer(angry, "I bet I can prove you're well enough to get up.")
        chatNpc(
            angry,
            "Ye'll never make me get up! I ain't afraid of cold, nor of wet, nor of anythin' ye " +
                "can do in here.",
        )
    }

    private suspend fun Dialogue.ithoiAfterExposed() {
        chatNpc(angry, "What d'ye want now?")
        when (
            choice2(
                "I saw you running around! You're not sick!",
                true,
                "Um... nothing, really.",
                false,
            )
        ) {
            true -> {
                chatPlayer(angry, "I saw you running around! You're not sick!")
                chatNpc(
                    shifty,
                    "So long as that old fool, Tock, believes it's a curse, I've nothing to fear " +
                        "from ye flapping yer mouth.",
                )
                chatPlayer(angry, "We'll see what the Captain has to say about that.")
            }
            false -> chatPlayer(neutral, "Um... nothing, really.")
        }
    }

    private suspend fun Dialogue.colin() {
        if (stage < CorsairCurseStage.AtTheCove) {
            tooSickToChat()
            return
        }
        when {
            stage >= CorsairCurseStage.IthoiSlain -> colinIthoiKilled()
            stage >= CorsairCurseStage.IthoiExposed -> colinIthoiExposed()
            stage >= CorsairCurseStage.IthoiConfessed -> colinIthoiConfessed()
            stage >= CorsairCurseStage.GnocciNamedIthoi -> colinFiringQuestion()
            stage >= CorsairCurseStage.TockToldOfDinner -> colinDinnerQuestion()
            access.corscursCabinBoy >= CorsairCurseCounts.ColinReassured -> colinAfterwards()
            access.corscursCabinBoy == CorsairCurseCounts.ColinTelescopeSeen -> colinReassured()
            access.corscursCabinBoy == CorsairCurseCounts.ColinInterviewed -> colinWaiting()
            access.corscursNavigator == 0 -> colinPreIthoi()
            else -> colinInterview()
        }
    }

    private suspend fun Dialogue.colinPreIthoi() {
        chatNpc(worried, "I'm sorry! Tell her I'm sorry! Then maybe she'll make the pain stop...")
        val ask =
            choice2(CursedOpener, true, "Perhaps you can tell me about it another time.", false)
        if (!ask) {
            chatPlayer(neutral, "Perhaps you can tell me about it another time.")
            chatNpc(neutral, "I'll be here. I'm not going anywhere while I feel so sick.")
            return
        }
        chatPlayer(happy, CursedOpener)
        chatNpc(
            sad,
            "Yes, that's what it must be. Ithoi, the navigator, always told us to beware of " +
                "curses. You should talk to him - he really knows about these things.",
        )
        chatNpc(
            neutral,
            "Ithoi lives in the hut to the south-west, nearest to the sea.",
        )
    }

    private suspend fun Dialogue.colinInterview() {
        chatNpc(worried, "I'm sorry! Tell her I'm sorry! Then maybe she'll make the pain stop...")

        val ask =
            choice2(CursedOpener, true, "Perhaps you can tell me about it another time.", false)
        if (!ask) {
            chatPlayer(neutral, "Perhaps you can tell me about it another time.")
            chatNpc(neutral, "I'll be here. I'm not going anywhere while I feel so sick.")
            return
        }

        chatPlayer(happy, CursedOpener)
        chatNpc(
            sad,
            "Yes, that's what it must be. Ithoi, the navigator, always told us to beware " +
                "of curses.",
        )
        chatPlayer(neutral, MetIthoi)
        chatNpc(
            worried,
            "This is all my fault. But I didn't mean to upset her. I just thought we could be " +
                "friends. Now I must have upset her, and she's cursed us all!",
        )
        chatNpc(
            sad,
            "Ithoi always told us that bad things happen to sailors who get involved with " +
                "mermaids, but I didn't believe him. I should have listened! But I'll tell you " +
                "what happened...",
        )

        chatNpc(neutral, "I was using Ithoi's telescope...")

        cutscene.colinFlashback(access)

        access.corscursCabinBoy = CorsairCurseCounts.ColinInterviewed

        chatNpc(
            sad,
            "Perhaps you can find the mermaid and tell her I'm very sorry I was rude to her. Then " +
                "maybe she'll lift the curse.",
        )
    }

    private suspend fun Dialogue.colinWaiting() {
        chatNpc(worried, ColinGreeting)
        when (
            choice2(
                "Where did you find this mermaid?",
                1,
                "Nope, I've not seen any mermaids round here.",
                2,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "Where did you find this mermaid?")
                chatNpc(
                    neutral,
                    "I was using Ithoi's telescope to look out at the sea, imagining the strange " +
                        "lands and fantastic creatures Ithoi's always telling us he's seen, and I " +
                        "saw her.",
                )
            }
            else -> {
                chatPlayer(neutral, "Nope, I've not seen any mermaids round here.")
                chatNpc(worried, "Oh dear, oh dear. Maybe we'll be cursed forever!")
            }
        }
    }

    private suspend fun Dialogue.colinDinnerQuestion() {
        chatNpc(worried, "Have you found out how we all got cursed?")
        val ask = choice2("I hear it happened straight after dinner.", true, StillLooking, false)
        if (!ask) {
            chatPlayer(neutral, StillLooking)
            return
        }
        chatPlayer(quiz, "I hear it happened straight after dinner.")
        chatNpc(
            neutral,
            "That's right. Gnocci had cooked us some swordfish. It didn't taste as good as his " +
                "usual cooking, but I didn't complain. The Captain doesn't like complainers.",
        )
        chatNpc(
            worried,
            "Then we all started feeling sick and clammy. That was when the Captain and Ithoi came " +
                "back from their parlay, and Ithoi looked sick too.",
        )
        chatPlayer(neutral, "Okay, thanks.")
    }

    private suspend fun Dialogue.colinFiringQuestion() {
        chatNpc(worried, "Have you found out how we all got cursed?")
        val ask =
            choice2("I hear the Captain's thinking of firing Ithoi.", true, StillLooking, false)
        if (!ask) {
            chatPlayer(neutral, StillLooking)
            return
        }
        chatPlayer(quiz, "I hear the Captain's thinking of firing Ithoi.")
        chatNpc(
            neutral,
            "Well, I suppose we don't need a navigator these days, and he's not very good at " +
                "anything else. But I don't see why that's got anything to do with the curse.",
        )
        chatPlayer(neutral, "Okay, thanks.")
    }

    private suspend fun Dialogue.colinIthoiConfessed() {
        chatNpc(worried, "Have you solved the curse yet?")
        val ask =
            choice2(
                "Ithoi admits poisoning you all and faking the curse.",
                true,
                StillLooking,
                false,
            )
        if (!ask) {
            chatPlayer(neutral, StillLooking)
            return
        }
        chatPlayer(quiz, "Ithoi admits poisoning you all and faking the curse.")
        chatNpc(
            confused,
            "That can't be right - he looks as sick as the rest of us. Maybe he's delerious and " +
                "doesn't know what he's saying.",
        )
    }

    private suspend fun Dialogue.colinIthoiExposed() {
        chatNpc(worried, "So what's going on now?")
        val ask =
            choice2(
                "Ithoi's faked the curse. I've seen him running around, healthy.",
                true,
                StillLooking,
                false,
            )
        if (!ask) {
            chatPlayer(neutral, StillLooking)
            return
        }
        chatPlayer(quiz, "Ithoi's faked the curse. I've seen him running around, healthy.")
        chatNpc(
            happy,
            "It sounds like there isn't a curse after all! Maybe I'll recover soon from whatever " +
                "he did to me.",
        )
    }

    private suspend fun Dialogue.colinIthoiKilled() {
        chatNpc(worried, "So what's going on now?")
        when (
            choice2(
                "Ithoi faked the curse, and I've killed him.",
                true,
                "Not much.",
                false,
            )
        ) {
            true -> {
                chatPlayer(quiz, "Ithoi faked the curse, and I've killed him.")
                chatNpc(
                    happy,
                    "Who'd have thought Ithoi would do such a thing? I guess if there isn't really " +
                        "a curse, I'll get better soon. In fact I feel better already.",
                )
            }
            false -> chatPlayer(neutral, "Not much.")
        }
    }

    private suspend fun Dialogue.colinReassured() {
        chatNpc(worried, ColinGreeting)
        chatPlayer(
            laugh,
            "I've looked through that telescope myself. That wasn't a mermaid. That was a wet " +
                "ogre.",
        )
        chatNpc(shocked, "What?")
        if (player.hasCompletedBigChompy()) {
            chatPlayer(
                quiz,
                "I've met that ogre before. He lives with his dad in the Feldip Hills. He's " +
                    "definitely not a mermaid.",
            )
        } else {
            chatPlayer(happy, "He's definitely not a mermaid.")
        }

        access.corscursCabinBoy = CorsairCurseCounts.ColinReassured

        chatNpc(
            confused,
            "Oh. Oh, I see. Okay... I guess that means I didn't upset a mermaid after all. So the " +
                "curse isn't my fault! Thank you!",
        )
        access.checkTheoriesRuledOut()

        colinAfterwards()
    }

    private suspend fun Dialogue.colinAfterwards() {
        chatPlayer(quiz, "How are you feeling?")
        chatNpc(
            sad,
            "I still feel awful. But I'm glad you've proven it wasn't my fault that we got " +
                "cursed. I hope you can work out what brought it on us.",
        )
        chatPlayer(neutral, "I'll be on the case.")
    }

    private suspend fun Dialogue.gnocci() {
        if (stage < CorsairCurseStage.AtTheCove) {
            tooSickToChat()
            return
        }
        if (stage >= CorsairCurseStage.Complete) {
            chatNpc(
                happy,
                "I'm really grateful to you for clearing up what Ithoi did to us. The rest of the " +
                    "Corsairs will appreciate my cooking even more now!",
            )
            chatPlayer(happy, "That's good for you!")
            return
        }
        when {
            stage >= CorsairCurseStage.IthoiSlain -> gnocciIthoiKilled()
            stage >= CorsairCurseStage.IthoiExposed -> gnocciIthoiExposed()
            stage >= CorsairCurseStage.IthoiConfessed -> gnocciIthoiConfessed()
            stage >= CorsairCurseStage.GnocciNamedIthoi -> gnocciNamingQuestion()
            stage >= CorsairCurseStage.TockToldOfDinner -> gnocciDinnerQuestion()
            access.corscursCook >= CorsairCurseCounts.GnocciReassured -> gnocciAfterwards()
            access.corscursCook == CorsairCurseCounts.GnocciDollDug -> gnocciReassured()
            access.corscursCook == CorsairCurseCounts.GnocciInterviewed -> gnocciWaiting()
            access.corscursNavigator == 0 -> gnocciPreIthoi()
            else -> gnocciInterview()
        }
    }

    private suspend fun Dialogue.gnocciPreIthoi() {
        chatNpc(sad, "My stomach!")
        val ask = choice2(CursedOpener, true, "My lower intestine!", false)
        if (!ask) {
            chatPlayer(neutral, "My lower intestine!")
            chatNpc(
                sad,
                "... look, I'm in a lot of pain here. We're not playing Name Your Favourite " +
                    "Body Part.",
            )
            chatPlayer(neutral, "Yes, I hear you've been cursed.")
        } else {
            chatPlayer(happy, CursedOpener)
        }
        chatNpc(
            worried,
            "Yes, I think She did this to us. I should have left Her alone. Ithoi told us to " +
                "beware of this kind of thing.",
        )
        chatNpc(
            neutral,
            "Ithoi's been cursed too. You can probably find him in his hut, south-west from here.",
        )
    }

    private suspend fun Dialogue.gnocciInterview() {
        chatNpc(sad, "My stomach!")

        val ask = choice2(CursedOpener, true, "My lower intestine!", false)
        if (!ask) {
            chatPlayer(neutral, "My lower intestine!")
            chatNpc(
                sad,
                "... look, I'm in a lot of pain here. We're not playing Name Your Favourite " +
                    "Body Part.",
            )
            chatPlayer(neutral, "Yes, I hear you've been cursed.")
        } else {
            chatPlayer(happy, CursedOpener)
        }

        chatNpc(
            worried,
            "Yes, I think She did this to us. I should have left Her alone. Ithoi told us to " +
                "beware of this kind of thing.",
        )
        chatPlayer(neutral, MetIthoi)
        chatNpc(
            worried,
            "It's all my fault, I know it is. I've awoken Her, and now She's done " + "this to us!",
        )
        chatPlayer(quiz, "Who are you talking about?")
        chatNpc(
            worried,
            "I'll explain... See, I'm the cook, and I have to find the food as well " +
                "as cooking it...",
        )

        chatNpc(neutral, "I was fishing...")

        cutscene.gnocciFlashback(access)

        access.corscursCook = CorsairCurseCounts.GnocciInterviewed

        chatNpc(sad, GnocciBuriedIt)
    }

    private suspend fun Dialogue.gnocciWaiting() {
        chatNpc(sad, GnocciGreeting)
        when (
            choice2(
                "Where did you say this happened?",
                1,
                "No, I've not.",
                2,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "Where did you say this happened?")
                chatNpc(
                    worried,
                    "On the beach, to the west of the cove. Near a big tree. I buried Her in the " +
                        "sand, but I can still feel her eyes staring into my soul. Please find " +
                        "Her, and kill the demon inside!",
                )
            }
            else -> {
                chatPlayer(neutral, "No, I've not.")
                chatNpc(sad, "Oh dear, I feel terrible. Please help us soon.")
            }
        }
    }

    private suspend fun Dialogue.gnocciDinnerQuestion() {
        chatNpc(sad, "Have you solved the curse yet?")
        val options =
            if (access.corscursThief >= CorsairCurseCounts.ArsenInterviewed) {
                choice3(
                    "I hear it happened straight after dinner.",
                    1,
                    "I hear the Captain's thinking of firing Ithoi.",
                    2,
                    StillLooking,
                    3,
                )
            } else {
                choice2("I hear it happened straight after dinner.", 1, StillLooking, 3)
            }
        when (options) {
            1 -> {
                chatPlayer(quiz, "I hear it happened straight after dinner.")
                chatNpc(
                    angry,
                    "Hey, don't go blaming the cook just because the curse began after dinner. I " +
                        "didn't even cook that meal. I'd been too scared of the possessed doll...",
                )
                chatPlayer(laugh, "You mean the clockwork doll?")
                chatNpc(angry, "It's an easy mistake to make!")

                if (stage == CorsairCurseStage.TockToldOfDinner) {
                    quest.setQuestStage(access, CorsairCurseStage.GnocciNamedIthoi)
                }

                chatNpc(
                    sad,
                    "Anyway I didn't dare try to fish after that, and Ithoi volunteered to do all " +
                        "the fishing and cooking instead, before he went off for his parlay with " +
                        "Captain Tock.",
                )
                chatNpc(
                    confused,
                    "Anyway, Ithoi got sick without even eating it, so it can't be the food.",
                )
            }
            2 -> {
                chatPlayer(quiz, "I hear the Captain's thinking of firing Ithoi.")
                chatNpc(
                    neutral,
                    "I don't see why that's got anything to do with the curse.",
                )
                chatPlayer(neutral, "Perhaps not.")
            }
            else -> chatPlayer(neutral, StillLooking)
        }
    }

    private suspend fun Dialogue.gnocciNamingQuestion() {
        chatNpc(sad, "Have you solved the curse yet?")
        val ask =
            choice2("So you say Ithoi prepared that meal?", true, StillLooking, false)
        if (!ask) {
            chatPlayer(neutral, StillLooking)
            return
        }
        chatPlayer(quiz, "So you say Ithoi prepared that meal?")
        chatNpc(
            neutral,
            "Yes, he volunteered to do all the fishing and cooking for me, even though he knew " +
                "he'd miss dinner for his parlay with Captain Tock. Ithoi's a good soul.",
        )
        chatPlayer(neutral, "Okay, thanks.")
    }

    private suspend fun Dialogue.gnocciIthoiConfessed() {
        chatNpc(sad, "Have you solved the curse yet?")
        val ask =
            choice2(
                "Ithoi admits poisoning you all and faking the curse.",
                true,
                StillLooking,
                false,
            )
        if (!ask) {
            chatPlayer(neutral, StillLooking)
            return
        }
        chatPlayer(quiz, "Ithoi admits poisoning you all and faking the curse.")
        chatNpc(
            confused,
            "If Ithoi's been afflicted by the curse as badly as I have, he might be half out of " +
                "his mind with pain. I'm sure he doesn't know what he's saying.",
        )
    }

    private suspend fun Dialogue.gnocciIthoiExposed() {
        chatNpc(sad, "Can you tell me what's going on now?")
        val ask =
            choice2(
                "Ithoi's faked the curse. I've seen him running around, healthy.",
                true,
                StillLooking,
                false,
            )
        if (!ask) {
            chatPlayer(neutral, StillLooking)
            return
        }
        chatPlayer(quiz, "Ithoi's faked the curse. I've seen him running around, healthy.")
        chatNpc(angry, "Make sure he gets what's coming to him. No-one poisons my crew!")
    }

    private suspend fun Dialogue.gnocciIthoiKilled() {
        chatNpc(sad, "Can you tell me what's going on now?")
        when (
            choice2(
                "Ithoi faked the curse, and I've killed him.",
                true,
                "Not much.",
                false,
            )
        ) {
            true -> {
                chatPlayer(quiz, "Ithoi faked the curse, and I've killed him.")
                chatNpc(
                    sad,
                    "You think you know a guy, and then he does a thing like this! Oh well, I hope " +
                        "I recover soon from whatever he did to me.",
                )
            }
            false -> chatPlayer(neutral, "Not much.")
        }
    }

    private suspend fun Dialogue.gnocciReassured() {
        chatNpc(sad, GnocciGreeting)
        chatPlayer(
            laugh,
            "Well, sort of. That doll wasn't possessed at all. It was a clockwork toy. They're " +
                "supposed to move.",
        )

        access.corscursCook = CorsairCurseCounts.GnocciReassured

        chatNpc(
            confused,
            "Clockwork? Oh, that would explain how it moved. Ahem. Well, I suppose the curse " +
                "didn't come from Her.",
        )
        access.checkTheoriesRuledOut()

        gnocciAfterwards()
    }

    private suspend fun Dialogue.gnocciAfterwards() {
        chatNpc(sad, "I hope you can find the real cause soon.")
        chatPlayer(sad, "Still trying.")
    }

    private suspend fun Dialogue.arsen() {
        if (stage < CorsairCurseStage.AtTheCove) {
            tooSickToChat()
            return
        }
        when {
            stage >= CorsairCurseStage.Complete -> arsenAfterwards()
            stage >= CorsairCurseStage.IthoiSlain -> arsenIthoiKilled()
            stage >= CorsairCurseStage.IthoiExposed -> arsenIthoiExposed()
            stage >= CorsairCurseStage.IthoiConfessed -> arsenIthoiConfessed()
            stage >= CorsairCurseStage.GnocciNamedIthoi -> arsenMotive()
            access.corscursThief >= CorsairCurseCounts.ArsenReassured -> arsenAfterwards()
            access.corscursThief == CorsairCurseCounts.ArsenRelicReturned -> arsenReassured()
            access.corscursThief == CorsairCurseCounts.ArsenRelicTaken -> arsenWaitingForTess()
            access.corscursThief == CorsairCurseCounts.ArsenInterviewed -> arsenWaitingForTock()
            access.corscursNavigator == 0 -> arsenPreIthoi()
            else -> arsenInterview()
        }
    }

    private suspend fun Dialogue.arsenPreIthoi() {
        chatNpc(sad, "Urgh!")
        val ask = choice2(CursedOpener, true, "Urgh to you too, with candy sprinkles on.", false)
        if (!ask) {
            chatPlayer(neutral, "Urgh to you too, with candy sprinkles on.")
            chatNpc(neutral, "A sympathetic soul you are. I'm in real pain here.")
            chatPlayer(neutral, "Yes, I hear you've been cursed.")
        } else {
            chatPlayer(happy, CursedOpener)
        }
        chatNpc(
            sad,
            "I think so. That navigator, Ithoi, always warned us that this kind of thing could " +
                "happen, but I didn't think it could possibly be real. But now this has happened, " +
                "and I think it's my fault.",
        )
        chatNpc(
            neutral,
            "You should maybe see what Ithoi thinks. He's been cursed too, so I guess he'll be in " +
                "his hut, off to the south-west, near the shore.",
        )
    }

    private suspend fun Dialogue.arsenInterview() {
        chatNpc(sad, "Urgh!")

        val ask = choice2(CursedOpener, true, "Urgh to you too, with candy sprinkles on.", false)
        if (!ask) {
            chatPlayer(neutral, "Urgh to you too, with candy sprinkles on.")
            chatNpc(neutral, "A sympathetic soul you are. I'm in real pain here.")
            chatPlayer(neutral, "Yes, I hear you've been cursed.")
        } else {
            chatPlayer(happy, CursedOpener)
        }

        chatNpc(
            sad,
            "I think so. That navigator, Ithoi, always warned us that this kind of thing " +
                "could happen.",
        )
        chatPlayer(neutral, MetIthoi)
        chatNpc(sad, "I'm afraid it may be all my fault.")
        chatPlayer(quiz, "Oh?")
        chatNpc(
            sad,
            "Captain Tock had me exploring some caves, west of the cove. I'm a thief, see, like " +
                "my brother Francois, and the Captain thought I could sneak past the monsters " +
                "without being noticed.",
        )
        chatNpc(
            worried,
            "Well, I climbed down the vines, and found a big cave filled with " +
                "terrifying ogres!",
        )

        access.corscursThief = CorsairCurseCounts.ArsenTellingStory

        chatNpc(
            sad,
            "I'd hoped I could use my amazing Thieving skills to avoid them seeing me, but I was " +
                "out of luck. By the time I'd finished clambering down the ladder, they'd spotted " +
                "me.",
        )
        chatPlayer(bored, "So you were trying to explore that ogre cave, and the ogres saw you?")
        chatNpc(
            worried,
            "Yes. I quickly grabbed some loot, so I wouldn't have to face the Captain " +
                "empty-handed, and fled up the ladder before the ogres could kill me!",
        )
        chatNpc(
            sad,
            "But a couple of days later we all fell ill. I think what I stole must have been some " +
                "kind of sacred relic, and the shamans in the cave have cursed us for stealing it.",
        )

        access.corscursThief = CorsairCurseCounts.ArsenInterviewed

        chatNpc(
            quiz,
            "Maybe you could get the relic back from the Captain, and return it to the ogres? And " +
                "tell them I'm very sorry. Then perhaps they'll lift the curse.",
        )
        chatPlayer(neutral, "I see.")
    }

    private suspend fun Dialogue.arsenWaitingForTock() {
        chatNpc(
            quiz,
            "Have you got that relic back from Captain Tock? Maybe if you return it to the ogres, " +
                "they'll lift the curse.",
        )
        chatPlayer(neutral, "Nope, I've not done that.")
        chatNpc(sad, "Oh dear.")
    }

    private suspend fun Dialogue.arsenWaitingForTess() {
        chatNpc(
            quiz,
            "Have you got that relic back from Captain Tock? Maybe if you return it to the ogres, " +
                "they'll lift the curse.",
        )
        chatNpc(sad, "Please take it back to the ogres for me, and tell them I'm sorry.")
    }

    private suspend fun Dialogue.arsenReassured() {
        chatNpc(quiz, ArsenGreeting)

        access.corscursThief = CorsairCurseCounts.ArsenToldOfTess

        chatPlayer(
            laugh,
            "Their chief, Tess, says that wasn't a sacred relic at all. It was her " + "tooth-pick.",
        )
        chatNpc(
            confused,
            "Are you telling me the ogres cursed us all because I stole their " +
                "chief's tooth-pick?",
        )

        access.corscursThief = CorsairCurseCounts.ArsenReassured

        chatPlayer(
            laugh,
            "She didn't care - she's got lots of tooth-picks, and she just thought you were some " +
                "kind of weirdo when you stole one. They never cursed you at all.",
        )
        chatNpc(
            confused,
            "Oh. That's a relief, I guess. Um... thank you for your help. But I do wonder where " +
                "the curse really came from.",
        )
        access.checkTheoriesRuledOut()

        chatPlayer(sad, NotDoneInvestigating)
    }

    private suspend fun Dialogue.arsenAfterwards() {
        if (stage >= CorsairCurseStage.Complete) {
            chatNpc(
                happy,
                "Thanks again for clearing up that 'curse'. Ever since you proved it wasn't " +
                    "supernatural, I've been feeling a lot better.",
            )
            chatPlayer(neutral, "You're welcome.")
            return
        }
        if (stage >= CorsairCurseStage.TockToldOfDinner) {
            chatNpc(sad, "Do you know how we all got cursed?")
            chatPlayer(sad, NotDoneInvestigating)
            return
        }
        chatNpc(quiz, ArsenGreeting)
        chatPlayer(sad, NotDoneInvestigating)
    }

    private suspend fun Dialogue.arsenMotive() {
        chatNpc(sad, "Do you know how we all got cursed?")

        val ask =
            choice2("I hear Ithoi cooked the meal you ate that night.", true, StillLooking, false)
        if (!ask) {
            chatPlayer(neutral, StillLooking)
            return
        }

        chatPlayer(shifty, "I hear Ithoi cooked the meal you ate that night.")
        chatNpc(
            confused,
            "I suppose Ithoi's trying to be extra helpful at the moment. Don't tell anyone, but " +
                "Ithoi might not be in our crew much longer.",
        )

        if (stage == CorsairCurseStage.GnocciNamedIthoi) {
            quest.setQuestStage(access, CorsairCurseStage.ArsenNamedMotive)
        }

        chatNpc(
            shifty,
            "My brother, Francois, says the Captain will make him second-in-command if he " +
                "completes his mission. That's Ithoi's rank, but we don't need a navigator now " +
                "we've settled here, and he can't do much else.",
        )

        val askMission =
            choice2("What is the mission Francois is doing?", true, "Okay, thanks.", false)
        if (askMission) {
            chatPlayer(quiz, "What is the mission Francois is doing?")
            chatNpc(shifty, "That's a secret.")
        }
        chatPlayer(neutral, "Okay, thanks.")
    }

    private suspend fun Dialogue.arsenIthoiConfessed() {
        chatNpc(sad, "Do you know how we all got cursed?")
        val ask =
            choice2(
                "Ithoi admits poisoning you all and faking the curse.",
                true,
                StillLooking,
                false,
            )
        if (!ask) {
            chatPlayer(neutral, StillLooking)
            return
        }
        chatPlayer(quiz, "Ithoi admits poisoning you all and faking the curse.")
        chatNpc(
            confused,
            "He's had a lot to worry about, with losing his place on the crew, and now he's sick " +
                "from the curse too. He must have gone mad and started babbling.",
        )
    }

    private suspend fun Dialogue.arsenIthoiExposed() {
        chatNpc(sad, "What's happening now?")
        val ask =
            choice2(
                "Ithoi's faked the curse. I've seen him running around, healthy.",
                true,
                StillLooking,
                false,
            )
        if (!ask) {
            chatPlayer(neutral, StillLooking)
            return
        }
        chatPlayer(quiz, "Ithoi's faked the curse. I've seen him running around, healthy.")
        chatNpc(angry, "Give him what he deserves!")
    }

    private suspend fun Dialogue.arsenIthoiKilled() {
        chatNpc(sad, "What's happening now?")
        when (
            choice2(
                "Ithoi faked the curse, and I've killed him.",
                true,
                "Not much.",
                false,
            )
        ) {
            true -> {
                chatPlayer(quiz, "Ithoi faked the curse, and I've killed him.")
                chatNpc(
                    neutral,
                    "I really didn't think he was like that. Still, if there isn't really a curse, " +
                        "I suppose we'll just get better from whatever he did to us. I don't feel " +
                        "so bad now.",
                )
            }
            false -> chatPlayer(neutral, "Not much.")
        }
    }

    private suspend fun Dialogue.tess() {
        chatNpc(quiz, "What you want, skinny creature?")

        val returnOption = access.invTotal(access.inv, CorsairCurseRefs.Relic) > 0
        val arsenOption =
            !returnOption && access.corscursThief >= CorsairCurseCounts.ArsenInterviewed

        when {
            returnOption -> {
                when (
                    choice3(
                        "I've come to return what Arsen stole.",
                        1,
                        "What is this place?",
                        2,
                        "I'll leave you alone, sorry.",
                        3,
                    )
                ) {
                    1 -> tessReturnRelic()
                    2 -> tessWhatIsThisPlace()
                    else -> chatPlayer(neutral, "I'll leave you alone, sorry.")
                }
            }
            arsenOption -> {
                when (
                    choice3(
                        "A man called Arsen told me he'd visited your cave.",
                        1,
                        "What is this place?",
                        2,
                        "I'll leave you alone, sorry.",
                        3,
                    )
                ) {
                    1 -> tessArsenVisited()
                    2 -> tessWhatIsThisPlace()
                    else -> chatPlayer(neutral, "I'll leave you alone, sorry.")
                }
            }
            else -> {
                when (choice2("What is this place?", 1, "I'll leave you alone, sorry.", 2)) {
                    1 -> tessWhatIsThisPlace()
                    else -> chatPlayer(neutral, "I'll leave you alone, sorry.")
                }
            }
        }
    }

    private suspend fun Dialogue.tessArsenVisited() {
        chatPlayer(quiz, "A man called Arsen told me he'd visited your cave.")
        chatNpc(
            laugh,
            "Yah, skinny man climb down vine the other day. Little man, big idiot! Hahahaha...",
        )
        chatPlayer(neutral, "He says he's very sorry...")
        chatNpc(neutral, "He very silly!")
        chatPlayer(neutral, "Perhaps I should come back another time.")
    }

    private suspend fun Dialogue.tessWhatIsThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            neutral,
            "Dis our home. It comfy, though we thinking of maybe getting more space up on land, " +
                "above. But silly Corsair men live up there an' think dey own whole place.",
        )
        chatNpc(shifty, "Maybe one day dat gonna change. Tess got plans.")
        chatPlayer(neutral, "Thanks.")
    }

    private suspend fun Dialogue.tessReturnRelic() {
        chatPlayer(shifty, "I've come to return what Arsen stole.")

        if (access.invTotal(access.inv, CorsairCurseRefs.Relic) <= 0) {
            chatNpc(quiz, "What you want, skinny creature?")
            return
        }

        chatNpc(
            laugh,
            "He skinny man who climb down vine the other day, yah? He big fool. You see " +
                "what he stole?",
        )
        chatPlayer(happy, "Arsen says he's very sorry for stealing from you. Here it is...")

        if (!access.returnRelicToTess()) {
            return
        }

        mesbox("You present Tess with Arsen's loot.")

        chatNpc(
            laugh,
            "Yah, dat little man is big idiot. Climbed down vine, grabbed tooth-pick off Tess and " +
                "ran away like scared kebbit.",
        )
        chatPlayer(
            confused,
            "You're saying this is a tooth-pick? Not a sacred relic of the ogres? Did your " +
                "shamans curse the Corsairs for stealing it?",
        )

        chatNpc(
            confused,
            "Dis is tooth-pick. Tess got plenty more. If skinny idiot man want Tess' old " +
                "tooth-pick, maybe he sick in his head. But Tess not having shamans curse anyone.",
        )
        chatPlayer(
            happy,
            "Oh. Well, I guess Arsen will be pleased to hear he didn't bring the " +
                "curse upon the Corsairs.",
        )
    }

    private val Dialogue.stage: Int
        get() = quest.getQuestStage(player)

    private companion object {
        private const val WhipObj = "obj.abyssal_whip"

        private const val FeatherObj = "obj.feather"

        private const val TockShipLine =
            "Now, I've a ship moored west of Rimmington. Come and meet me on board, and we'll set " +
                "off to Corsair Cove."

        private const val TockRefuse = "No, I'm not available."

        private const val CursedOpener = "I hear you've been cursed."

        private const val MetIthoi =
            "I've met him. He definitely thinks it's a curse, but wasn't able to say exactly what " +
                "caused it."

        private const val StillLooking = "I'm still looking into it."

        private const val NotDoneInvestigating = "I'm not done investigating yet."

        private const val ColinGreeting = "Have you found the mermaid yet? Will she forgive me?"

        private const val GnocciGreeting =
            "Have you tried to destroy Her yet? Her eyes, those terrible eyes!"

        private const val ArsenGreeting =
            "Have you returned the sacred relic to the ogres? Will they forgive me and lift the " +
                "curse?"

        private const val GnocciBuriedIt =
            "I buried Her in the sand, by that tree, to the west. But I still feel Her eyes " +
                "staring into my soul. Please find Her, and kill the demon inside!"

        private const val WalkOffSound = "synth.royal_footsteps"
    }
}

internal val Dialogue.ladOrLass: String
    get() = if (access.isBodyTypeA()) "lad" else "lass"

internal val Dialogue.tockThanks: String
    get() =
        "Anyways, ye've been a mighty help, young $ladOrLass. Ye be brave in the heart, strong " +
            "in the arm and full in the bowels, an' I be grateful to ye."

internal const val TockBankAccess: String =
    "Tell ye what, I'll have a word with Yusuf, in the bank. Ye've earned the right to use his " +
        "services. An' thanks again, very much."

internal fun ProtectedAccess.checkTheoriesRuledOut() {
    val quest = Quest.get(CorsairCurseRefs.QuestKey) ?: return
    if (quest.getQuestStage(player) != CorsairCurseStage.AtTheCove) {
        return
    }
    val resolved =
        corscursCabinBoy >= CorsairCurseCounts.ColinReassured &&
            corscursCook >= CorsairCurseCounts.GnocciReassured &&
            corscursThief >= CorsairCurseCounts.ArsenReassured &&
            corscursNavigator >= CorsairCurseCounts.IthoiInterviewed
    if (resolved) {
        quest.setQuestStage(this, CorsairCurseStage.TheoriesRuledOut)
    }
}
