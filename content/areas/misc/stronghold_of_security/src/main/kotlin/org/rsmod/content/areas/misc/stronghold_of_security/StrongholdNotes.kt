package org.rsmod.content.areas.misc.stronghold_of_security

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.misc.stronghold_of_security.StrongholdDoors.Companion.MesboxLimit
import org.rsmod.content.areas.misc.stronghold_of_security.StrongholdDoors.Companion.splitForChatbox
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.metSolztun by boolVarBit("varbit.sos_brother_found")

class StrongholdNotes @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.sos_skelly_bag") { searchExplorer() }
        onOpHeld1(Notes) { readNotes() }
        onOpNpc1("npc.sos_barb_spirit") { startDialogue(it.npc) { solztun() } }
    }

    private suspend fun ProtectedAccess.searchExplorer() {
        arriveDelay()
        if (inv.count(Notes) > 0 || bank.count(Notes) > 0) {
            mes("You search the dead explorer, but there is nothing else of use on him.")
            return
        }
        if (inv.isFull()) {
            mes("You don't have enough inventory space to take the explorer's notes.")
            return
        }
        invAddOrDrop(objRepo, Notes)
        startDialogue { objbox(Notes, "You find a book of notes on the dead explorer's body.") }
    }

    private suspend fun ProtectedAccess.readNotes() {
        startDialogue {
            while (true) {
                val section: NoteSection? =
                    choice5(
                        "Introduction",
                        NoteSection.Introduction,
                        "Levels 1 and 2",
                        NoteSection.UpperLevels,
                        "Levels 3 and 4",
                        NoteSection.LowerLevels,
                        "Navigation and diary",
                        NoteSection.NavigationAndDiary,
                        "Close the book",
                        null,
                        title = "Stronghold notes",
                    )
                val page = section ?: return@startDialogue
                for (part in page.text.replace("\n\n", " ").splitForChatbox(MesboxLimit)) {
                    mesbox(part)
                }
            }
        }
    }

    private suspend fun Dialogue.solztun() {
        chatNpc(neutral, "Can you hear me?")
        if (player.worn.count(Ghostspeak) > 0) {
            chatPlayer(happy, "Of course! I'm wearing an amulet of ghostspeak.")
        } else {
            chatPlayer(
                worried,
                "...I can, but I'm not wearing an amulet of ghostspeak... A-am I dead?",
            )
        }
        chatNpc(
            angry,
            "I'm no ghost! I'm a spirit, I'm injecting my thoughts into your head.",
        )
        chatPlayer(confused, "Oh, that's... interesting. Also rather disturbing...")
        chatNpc(neutral, "Try not to think about it.")
        while (true) {
            when (
                choice3(
                    "Who are you?",
                    SolztunOption.Who,
                    "Why are you here?",
                    SolztunOption.Why,
                    "Bye!",
                    SolztunOption.Bye,
                )
            ) {
                SolztunOption.Who -> whoAreYou()
                SolztunOption.Why -> whyAreYouHere()
                SolztunOption.Bye -> {
                    chatPlayer(happy, "Bye!")
                    chatNpc(happy, "Stay safe, adventurer.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.whoAreYou() {
        chatPlayer(quiz, "Who are you?")
        if (!player.metSolztun) {
            player.metSolztun = true
            chatNpc(happy, "I am Solztun, the greatest barbarian explorer!")
            chatPlayer(happy, "It's very nice to meet you, Solztun! I'm ${player.displayName}.")
            chatNpc(neutral, "I know.")
            chatPlayer(quiz, "... so, you were exploring this place?")
            chatNpc(
                sad,
                "Yes, my sister told me not to go, she said it was dangerous and that I'm no " +
                    "fighter. Unfortunately... she was correct.",
            )
            chatPlayer(
                quiz,
                "Oh! Your sister is Litara, the woman out on the surface? How come she was " +
                    "correct?",
            )
            chatNpc(
                sad,
                "I died... Of course, I tried escaping, long before I'd made it to the " +
                    "treasure, but I was shot with arrows just as I approached the exit.",
            )
        } else {
            chatNpc(
                happy,
                "As I said, I am Solztun, the greatest barbarian explorer! I came to explore " +
                    "this place, but unfortunately I died long before I made it to the treasure.",
            )
        }
        chatPlayer(shocked, "Nobody came to help?!")
        chatNpc(
            sad,
            "Oh no, they had no idea I was hurt, I guess they assumed I'd found the treasure " +
                "and made a new life for myself.",
        )
        chatPlayer(quiz, "Were you not a fan of the barbarian life style?")
        chatNpc(
            neutral,
            "It was all death and pillaging, I prefer the finer things in life, like " +
                "exploration and discovery!",
        )
        chatPlayer(happy, "Ah, I too enjoy those.")
    }

    private suspend fun Dialogue.whyAreYouHere() {
        chatPlayer(quiz, "Why are you here?")
        chatNpc(happy, "I came for treasure!")
        chatPlayer(quiz, "Oh right, you were looking for what's in that cradle?")
        chatNpc(sad, "Yes, but alas, I'm dead.")
        chatPlayer(shifty, "I guess I could show you... for a price.")
        chatNpc(
            neutral,
            "Hmm... I don't have much to offer, but if you happen upon a skull sceptre, I " +
                "could make it stronger.",
        )
        chatPlayer(quiz, "Stronger you say... In what way?")
        chatNpc(
            neutral,
            "Currently the sceptre is fragile, it breaks once all charges have been used. " +
                "Show me the treasure and I could imbue it so it can be recharged with any " +
                "sceptre piece.",
        )
        chatPlayer(happy, "You, my dead friend, have a deal!")
    }

    private enum class SolztunOption {
        Who,
        Why,
        Bye,
    }

    private enum class NoteSection(val text: String) {
        Introduction(
            "This stronghold was unearthed by a miner prospecting for new ores around the " +
                "Barbarian Village. After gathering some equipment he ventured into the maze " +
                "of tunnels and was missing for a long time. He finally emerged along with " +
                "copious notes regarding the new beasts and strange experiences which had " +
                "befallen him. He also mentioned that there was treasure to be had, but no " +
                "one has been able to wring a word from him about this, he simply flapped his " +
                "arms and slapped his head. This book details his notes and my diary of " +
                "exploration. I am exploring to see if I can find out more..."
        ),
        UpperLevels(
            "Level 1: As well as goblins, creatures like a man but also like a cow infest this " +
                "place! The area itself is reminiscent of frontline castles, with many walls, " +
                "doors and skeletons of dead enemies. I'm sure I hear voices in my head each " +
                "time I pass through the gates. I have dubbed this level War as it seems like " +
                "an eternal battleground. I found only one small peaceful area here.\n\n" +
                "Level 2: My supplies are running low and I find myself in barren passages " +
                "with seemingly endless malnourished beasts attacking me, ravenous for food. " +
                "Nothing appears to be able to grow, many adventurers have died through lack " +
                "of food and the very air appears to suck vitality from me. I've come to call " +
                "this place famine."
        ),
        LowerLevels(
            "Level 3: Just breathing in this place makes me shudder at the thought of what " +
                "foul disease I may contract. The walls and floor ooze and pulsate like " +
                "something pox ridden. There is a very strange beast whom I narrowly escaped " +
                "from. Luckily I found a small place where I could heal myself and rest a " +
                "while. I have named this area pestilence for it reeks with decay.\n\n" +
                "Level 4: Nothing truly alive exists here, even those beings who do wander " +
                "the halls are not alive as such, but they do know that I am and I get the " +
                "distinct impression that were they to have their way, I would not be for " +
                "long! Death is everywhere and thus I shall name this place. There is one " +
                "small place of life, which was gladdening to find and very worth my while!"
        ),
        NavigationAndDiary(
            "After getting lost several times I finally worked out the key to all the " +
                "ladders and chains around this death infested place. All ropes and chains " +
                "will take you to the start of the level that you are on. However most " +
                "ladders will simply take you to the level above. The one exception is the " +
                "ladder in the bottom level treasure room, which appears to lead through " +
                "several extremely twisty passages and eventually takes you out of the " +
                "dungeon completely. The portals may be used if you are of sufficient level " +
                "or have already claimed your reward from the treasure room.\n\n" +
                "Day 1: Today I set out to find out more about this place. From my research I " +
                "knew about the sentient doors, imbued by some unknown force to talk to you " +
                "and ask questions before they will let you pass. I have so far passed these " +
                "doors without incident, giving the correct answer seems to work a treat.\n\n" +
                "Day 2: I have fought my way through the fearsome beasts on the first level " +
                "and am preparing myself to journey deeper. I hope that things are not too " +
                "difficult further on as I am already sick of bread and cheese for dinner.\n\n" +
                "Day 3: I ventured down into the famine level today... I was wounded and have " +
                "returned to the relative safety of the level above. I am going to try to " +
                "make my way out through the goblins and mancow things... I hope I make it....."
        ),
    }

    private companion object {
        const val Notes = "obj.sos_stronghold_book"
        const val Ghostspeak = "obj.amulet_of_ghostspeak"
    }
}
