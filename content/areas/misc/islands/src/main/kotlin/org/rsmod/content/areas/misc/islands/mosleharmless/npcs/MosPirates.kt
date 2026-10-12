package org.rsmod.content.areas.misc.islands.mosleharmless.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Pirate_(Mos_Le%27Harmless)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] pirates 1 to 10 are the island pirates and 11 to 20 the militia,
// in npc id order
class MosPirates : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.fever_pirate_island_01") { talk(it.npc) { pirate1() } }
        onOpNpc1("npc.fever_pirate_island_02") { talk(it.npc) { pirate2() } }
        onOpNpc1("npc.fever_pirate_island_03") { talk(it.npc) { pirate3() } }
        onOpNpc1("npc.fever_pirate_island_04") { talk(it.npc) { pirate4() } }
        onOpNpc1("npc.fever_pirate_island_05") { talk(it.npc) { pirate5() } }
        onOpNpc1("npc.fever_pirate_island_06") { talk(it.npc) { pirate6() } }
        onOpNpc1("npc.fever_pirate_island_07") { talk(it.npc) { pirate7() } }
        onOpNpc1("npc.fever_pirate_island_08") { talk(it.npc) { pirate8() } }
        onOpNpc1("npc.fever_pirate_island_09") { talk(it.npc) { pirate9() } }
        onOpNpc1("npc.fever_pirate_island_10") { talk(it.npc) { pirate10() } }
        onOpNpc1("npc.fever_pirate_millitia_01") { talk(it.npc) { pirate11() } }
        onOpNpc1("npc.fever_pirate_millitia_02") { talk(it.npc) { pirate12() } }
        onOpNpc1("npc.fever_pirate_millitia_03") { talk(it.npc) { pirate13() } }
        onOpNpc1("npc.fever_pirate_millitia_04") { talk(it.npc) { pirate14() } }
        onOpNpc1("npc.fever_pirate_millitia_05") { talk(it.npc) { pirate15() } }
        onOpNpc1("npc.fever_pirate_millitia_06") { talk(it.npc) { pirate16() } }
        onOpNpc1("npc.fever_pirate_millitia_07") { talk(it.npc) { pirate17() } }
        onOpNpc1("npc.fever_pirate_millitia_08") { talk(it.npc) { pirate18() } }
        onOpNpc1("npc.fever_pirate_millitia_09") { talk(it.npc) { pirate19() } }
        onOpNpc1("npc.fever_pirate_millitia_10") { talk(it.npc) { pirate20() } }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc, dialogue: suspend Dialogue.() -> Unit) {
        if (BOOK_O_PIRACY !in player.inv) {
            startDialogue(npc) { pirateSlang() }
            return
        }
        startDialogue(npc) { dialogue() }
    }

    private suspend fun Dialogue.pirateSlang() {
        chatPlayer(happy, "Hello!")
        chatNpc(quiz, "Arr? Be ye wantin' te go on account with our gang o' fillibusters?")
        chatNpc(
            happy,
            "The powder monkey be takin' a caulk after gettin' rowdy on bumboo, so there be " +
                "plenty of room for ye.",
        )
        chatPlayer(confused, "Riiiiight...")
        chatPlayer(neutral, "I'll just be over here if you need me.")
    }

    private suspend fun Dialogue.pirate1() {
        chatNpc(quiz, "What's a landlubber like ye doin' here?")
        chatPlayer(happy, "Helping Cap'n Teach shoot his way through a little blockade.")
        chatNpc(happy, "Well done to ye. I'll see ye around.")
    }

    private suspend fun Dialogue.pirate2() {
        chatPlayer(quiz, "Why is this place walled off?")
        chatNpc(neutral, "Well ${lad()}, it's 'cos there be foul things in the jungle beyond.")
        chatPlayer(quiz, "What sort of foul things?")
        chatNpc(worried, "Big 'uns, nasty 'uns. You name it, we got it.")
        chatNpc(worried, "There be Jungle Horrors in the jungles, and Cave Horrors in the caves.")
        chatNpc(neutral, "Tis no place te be havin' a picnic and no mistake.")
    }

    private suspend fun Dialogue.pirate3() {
        chatNpc(angry, "Watch where yer goin'!")
        chatPlayer(angry, "I was!")
        chatNpc(neutral, "Then it must have been my fault and I apologise ${lad()}.")
        chatPlayer(happy, "And I accept your apology. Good day.")
        chatNpc(happy, "Aye, good day te ye too.")
    }

    private suspend fun Dialogue.pirate4() {
        chatPlayer(happy, "Arr!")
        chatNpc(quiz, "Arr?")
        chatPlayer(quiz, "Arr?")
        chatNpc(happy, "Arr!")
        chatPlayer(neutral, "Arr...")
        chatNpc(neutral, "Arr.")
    }

    private suspend fun Dialogue.pirate5() {
        chatPlayer(happy, "Good morning!")
        chatNpc(bored, "I suppose it is. What of it?")
        chatPlayer(neutral, "I was just striking up a conversation.")
        chatNpc(confused, "Well...why would ye do a thing like that?")
        chatPlayer(neutral, "No reason.")
    }

    private suspend fun Dialogue.pirate6() {
        chatNpc(quiz, "What ship did ye pull in on, stranger?")
        chatPlayer(happy, "The Adventurous.")
        chatNpc(happy, "That's a good ship, that it be.")
    }

    private suspend fun Dialogue.pirate7() {
        chatPlayer(happy, "Hello!")
        chatNpc(drunk, "Keep yer voice down! Can't ye see I'm busy?")
        chatPlayer(
            confused,
            "Busy? Other than busy being drunk, I can't see what you are talking about.",
        )
        chatNpc(drunk, "I can't feel me brains no more...")
        chatPlayer(
            neutral,
            "Wait...I recognise that smell. You've been drinking 'rum' haven't you?",
        )
        chatNpc(drunk, "Arr...ye shufflin' muffle burble...")
        chatPlayer(neutral, "I think that can be filed as a 'yes'.")
    }

    private suspend fun Dialogue.pirate8() {
        chatNpc(shifty, "Are ye here for...you know...the stuff...")
        chatPlayer(confused, "The what?")
        chatNpc(shifty, "The stuff...you know...the stuff?")
        chatPlayer(confused, "I'm not sure I follow you.")
        val name = if (access.isBodyTypeA()) "Kevin" else "Lisa"
        chatNpc(quiz, "Aren't ye Toothless $name?")
        chatPlayer(neutral, "No.")
        chatNpc(worried, "Sorry, sorry, my mistake!")
    }

    private suspend fun Dialogue.pirate9() {
        chatNpc(angry, "Ain't ye the scurvy curr that killed my brother?")
        chatPlayer(neutral, "Well I'm not sure.")
        chatPlayer(
            neutral,
            "I kill so many things on a daily basis that it all tends to blur together.",
        )
        chatPlayer(
            quiz,
            "Tell me, did they have any unusual features that would help me to recall them?",
        )
        chatNpc(confused, "How many things de ye have te kill in a day te lose count?")
        chatPlayer(neutral, "I don't know, if I did I wouldn't have lost count!")
        chatPlayer(quiz, "Now, about your brother...")
        chatNpc(worried, "Never mind! I never liked him that much anyway!")
    }

    private suspend fun Dialogue.pirate10() {
        chatPlayer(quiz, "It's rather quiet around here for a pirate haven isn't it?")
        chatNpc(happy, "Arr, we like te keep the peace around here.")
        chatNpc(
            happy,
            "Some of the older pirates don't like it, but Mos Le'Harmless be a hive of sense and " +
                "civility.",
        )
        chatPlayer(happy, "How nice!")
        chatNpc(happy, "Arr, that it be.")
    }

    private suspend fun Dialogue.pirate11() {
        chatNpc(quiz, "What's yer business?")
        chatPlayer(quiz, "I was wondering who you are.")
        chatNpc(
            neutral,
            "Well, I'm one of the militia, we try and keep the rest of the pirates in line.",
        )
        chatNpc(neutral, "Well, as in line as we reckon they need to be.")
        chatPlayer(quiz, "So how do you decide who becomes a member of the militia?")
        chatNpc(
            neutral,
            "Well, if yer ship is in need of repairs and yer in port, then yer a member, simple " +
                "as that.",
        )
        chatNpc(
            neutral,
            "See, those of us that are here for a long haul tend to want to keep things " +
                "peaceful, so we keep an eye on everyone else as and when they come in.",
        )
        chatNpc(
            neutral,
            "Given the number of skirmishes we have with customs, there is never a shortage of " +
                "repairs needed.",
        )
        chatPlayer(happy, "Thanks, that was very informative.")
    }

    private suspend fun Dialogue.pirate12() {
        chatNpc(quiz, "What's yer business?")
        chatPlayer(neutral, "Just passing through.")
        chatNpc(neutral, "See to it ye keeps outs trouble, stranger.")
    }

    private suspend fun Dialogue.pirate13() {
        chatNpc(quiz, "What's yer business?")
        chatPlayer(neutral, "Just looking about.")
        chatNpc(neutral, "Well good luck with that, landlubber.")
    }

    private suspend fun Dialogue.pirate14() {
        chatNpc(quiz, "What's yer business?")
        chatPlayer(happy, "Adventuring!")
        chatNpc(
            shifty,
            "Adventurin' eh...that sounds like customs and excise work to my ears.",
        )
        chatPlayer(worried, "No, no, I'm a pirate, honest. I have the book and everything.")
        val customs = if (access.isBodyTypeA()) "boy" else "girl"
        chatNpc(
            angry,
            "I'll have me eye on ye, customs $customs. One step out of line and bam.",
        )
        chatNpc(angry, "They won't find enough of ye to feed a hungry sea slug.")
    }

    private suspend fun Dialogue.pirate15() {
        chatNpc(quiz, "What's yer business?")
        chatPlayer(quiz, "Just looking around. What is there to do here?")
        chatNpc(
            neutral,
            "Well, if yeve got some coins in yer purse ye can head over to Joe's house of 'Rum' " +
                "and get yerself drunk.",
        )
        chatPlayer(quiz, "Anything else?")
        chatNpc(
            neutral,
            "Well, Smithin' Smith and Dodgy Mike have some stuff to sell, and ye can always try " +
                "grabbin' some fish from Two Feet Charley if yer hungry.",
        )
        chatPlayer(quiz, "Is that it?")
        chatNpc(
            neutral,
            "Well, ye can head out to the brewery and have a word with Fancy Dan or San Fan. " +
                "They always need a hand out there.",
        )
        chatPlayer(quiz, "Doing what?")
        chatNpc(laugh, "Doin' each others heads in most of the time.")
    }

    private suspend fun Dialogue.pirate16() {
        chatNpc(quiz, "What is your business?")
        chatPlayer(neutral, "Nothing much, just passing through.")
        val address = if (access.isBodyTypeA()) ", sir" else ", miss"
        chatNpc(happy, "Well have a good day$address.")
        chatPlayer(
            confused,
            "I'm sorry, but you don't sound much like a pirate, how did you come to be here?",
        )
        chatNpc(
            neutral,
            "I used to be a language tutor, teaching in a small school just outside of Lumbridge.",
        )
        chatNpc(
            sad,
            "However the place closed down due to ill attendance, and so I was forced to take up " +
                "piracy.",
        )
        chatPlayer(quiz, "Wasn't that a bit of a big jump in careers?")
        chatNpc(neutral, "Not really, I made my way through via a short stint as a lawyer.")
        chatPlayer(neutral, "Well, that makes sense.")
        chatNpc(happy, "I must return to my duties. Have a nice day!")
        chatPlayer(happy, "You too!")
    }

    private suspend fun Dialogue.pirate17() {
        chatNpc(quiz, "What's yer business?")
        chatPlayer(quiz, "What's yours?")
        chatNpc(angry, "I asked first!")
        chatPlayer(angry, "And very rudely too I might add.")
        chatNpc(angry, "Fine! Go about yer 'business'!")
        chatPlayer(angry, "I will!")
    }

    private suspend fun Dialogue.pirate18() {
        chatNpc(drunk, "Whaz yer bushinessssss...ssss?")
        chatPlayer(quiz, "Are you drunk on duty?")
        chatNpc(
            drunk,
            "I'm not drunk! I'll knock down any man as sayin' I'm been having a drinking on my " +
                "duties!",
        )
        chatPlayer(
            neutral,
            "Well, I think you'll have a hard time explaining your slurred speech, odd " +
                "behaviour, bitter, nostril dissolving stench and rolling, bloodshot eyes if " +
                "your not drunk.",
        )
        chatNpc(drunk, "Arrr, ye got me. I just can't leave the stuff alone.")
        chatNpc(
            drunk,
            "Cap'n Braindeath's Ten X With Added Cattle Medication just keeps singin' its siren " +
                "song to me.",
        )
        chatNpc(drunk, "Would ye care fer a drop?")
        chatPlayer(angry, "No.  I would most certainly not.")
        chatNpc(drunk, "Suit yerself.")
    }

    private suspend fun Dialogue.pirate19() {
        chatNpc(quiz, "What's yer business?")
        chatPlayer(angry, "None of yours.")
        chatNpc(neutral, "Fair enough. Off ye go.")
    }

    private suspend fun Dialogue.pirate20() {
        chatNpc(quiz, "What's yer business?")
        chatPlayer(neutral, "I'm just helping out a friend.")
        chatNpc(quiz, "Which friend?")
        chatPlayer(neutral, "Bill Teach.")
        chatNpc(laugh, "Bill hired you eh? He must have been scrapin' the barrel and no mistake.")
        chatPlayer(angry, "Hey! I resent that!")
    }

    private fun Dialogue.lad(): String = if (access.isBodyTypeA()) "lad" else "lass"

    private companion object {
        const val BOOK_O_PIRACY = "obj.fever_piracy_book"
    }
}
