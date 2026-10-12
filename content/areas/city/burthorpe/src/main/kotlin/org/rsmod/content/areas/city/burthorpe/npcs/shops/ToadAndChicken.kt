package org.rsmod.content.areas.city.burthorpe.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ToadAndChicken @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.death_barman") { startDialogue(it.npc) { tostig(it.npc) } }
        onOpNpc3("npc.death_barman") { player.openPub(it.npc) }
        onOpNpc1("npc.boardgames_barmaid") {
            startDialogue(it.npc) { gamesRoomBarmaid("Welcome to the Burthorpe Games Rooms!") }
        }
        onOpNpc3("npc.boardgames_barmaid") { player.openPub(it.npc) }
        onOpNpc1("npc.boardgames_barmaid2") {
            startDialogue(it.npc) { gamesRoomBarmaid("Welcome to the Games Zone!") }
        }
        onOpNpc3("npc.boardgames_barmaid2") { player.openPub(it.npc) }
    }

    private fun Player.openPub(npc: Npc) {
        shops.open(this, npc, "The Toad and Chicken", "inv.death_pub")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tostig
    private suspend fun Dialogue.tostig(npc: Npc) {
        val title = if (access.isBodyTypeA()) "mister" else "miss"
        chatPlayer(quiz, "Hi, what ales are you serving?")
        chatNpc(
            happy,
            "Well $title, our speciality is Asgarnian Ale, we also serve Wizard's Mind Bomb and " +
                "Dwarven Stout.",
        )
        chatNpc(quiz, "Would you like to buy a drink?")
        if (choice2("Yes, please.", true, "No, thanks.", false)) {
            chatPlayer(happy, "Yes, please.")
            player.openPub(npc)
            return
        }
        chatPlayer(neutral, "No, thanks.")
        chatNpc(shifty, "Ah well... so um... does the grey squirrel sing in the grove?")
        val option =
            choice3(
                "Huh?",
                1,
                "Yes, and the black cat dances in the sink.",
                2,
                "No, squirrels can't sing.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(confused, "Huh?")
                chatNpc(shifty, "Er... nevermind.")
            }
            2 -> {
                chatPlayer(shifty, "Yes, and the black cat dances in the sink.")
                chatNpc(shifty, "Ah you'll be wanting the trapdoor behind the bar.")
            }
            3 -> {
                chatPlayer(confused, "No, squirrels can't sing.")
                chatNpc(shifty, "No... of course they can't...")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sam_(barmaid)
    // https://oldschool.runescape.wiki/w/Transcript:Rachael
    private suspend fun Dialogue.gamesRoomBarmaid(greeting: String) {
        chatNpc(happy, greeting)
        while (true) {
            val option =
                choice4(
                    "How do I play board games?",
                    1,
                    "What games can I play?",
                    2,
                    "Can I buy a drink please?",
                    3,
                    "Thanks!",
                    4,
                )
            when (option) {
                1 -> boardGames()
                2 -> games()
                3 -> {
                    chatPlayer(quiz, "Can I buy a drink please?")
                    chatNpc(neutral, "I'm afraid I don't sell drinks.")
                    return
                }
                4 -> {
                    chatPlayer(happy, "Thanks!")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.boardGames() {
        chatPlayer(quiz, "How do I play board games?")
        chatNpc(
            neutral,
            "You can challenge someone to a game anywhere in the Games Zone (except on a PvP or " +
                "Deadman world) by using the right click option.",
        )
        chatNpc(
            neutral,
            "Choose the type of game and then choose the options you want such as time per move.",
        )
        chatNpc(
            neutral,
            "If your opponent accepts the challenge you will be taken to one of the tables " +
                "around where you will play your game of choice.",
        )
        chatNpc(
            happy,
            "Once you have finished your game you will come back here where you can challenge " +
                "again! We also have tables in a few pubs around the world!",
        )
    }

    private suspend fun Dialogue.games() {
        chatPlayer(quiz, "What games can I play?")
        chatNpc(neutral, "Currently we offer Draughts, Runelink, Runesquares, and Runeversi.")
        val option =
            choice4("Draughts?", 1, "Runelink?", 2, "Runesquares?", 3, "Runeversi?", 4)
        when (option) {
            1 -> {
                chatPlayer(quiz, "Draughts?")
                chatNpc(
                    neutral,
                    "Draughts uses standard rules, apart from: a draw is declared if no piece " +
                        "has been taken or promoted for forty moves.",
                )
            }
            2 -> {
                chatPlayer(quiz, "Runelink?")
                chatNpc(neutral, "Yup, you have to get four runes in row.")
            }
            3 -> {
                chatPlayer(quiz, "Runesquares?")
                chatNpc(
                    neutral,
                    "Yes, you take it in turns to add a line with the goal of making squares. " +
                        "Everytime you make a square you take another turn.",
                )
            }
            4 -> {
                chatPlayer(quiz, "Runeversi?")
                chatNpc(
                    neutral,
                    "Yep, the aim is to have more of your runes on the board than your " +
                        "opponent. You can take your opponents pieces by trapping them between " +
                        "your own.",
                )
            }
        }
    }
}
