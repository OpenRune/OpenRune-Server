package org.rsmod.content.areas.city.barbarianvillage.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.brotherFound by intVarBit("varbit.sos_brother_found")

class Litara : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.sos_barb") { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) = startDialogue(npc) { litara() }

    private suspend fun Dialogue.litara() {
        chatNpc(quiz, "Hello there. You look lost, are you okay?")
        when (player.brotherFound) {
            BROTHER_FOUND -> brotherFound()
            BROTHER_REPORTED -> brotherReported()
            else -> standard()
        }
    }

    private suspend fun Dialogue.standard() {
        val choice =
            choice2(
                "I'm looking for a stronghold or something...",
                1,
                "I'm fine, just passing through.",
                2,
            )
        when (choice) {
            1 -> stronghold()
            2 -> chatPlayer(neutral, "I'm fine, just passing through.")
        }
    }

    private suspend fun Dialogue.stronghold() {
        chatPlayer(quiz, "I'm looking for a stronghold or something...")
        chatNpc(neutral, "Ahh... the Stronghold of Security. It's down there.")
        mesbox(
            "Litara point's on the hole in the ground that looks like you could squeeze through."
        )
        chatPlayer(worried, "Looks kind of... deep and dark.")
        chatNpc(sad, "Yeah... tell that to my brother, he still hasn't come back.")
        chatPlayer(quiz, "Your brother?")
        chatNpc(
            sad,
            "He's an explorer too. When the miner fell down that hole he'd made and came back " +
                "babbling about doors, questions and treasure, my brother went to explore. " +
                "No-one has seen him since.",
        )
        chatPlayer(worried, "Oh... that's not good.")
        chatNpc(
            sad,
            "Lots of people have been down there, but none of them have seen him, Let me know " +
                "if you do, will you?",
        )
        chatPlayer(neutral, "I'll certainly keep my eyes open.")
    }

    private suspend fun Dialogue.brotherFound() {
        chatPlayer(happy, "I travelled to the bottom of the stronghold, and I emerge victorious!")
        chatNpc(happy, "Well done! You didn't happen to find my brother?")
        chatPlayer(worried, "About that...")
        chatNpc(
            sad,
            "I sense bad news. It's okay, he's been gone so long. I believe I've moved on.",
        )
        chatPlayer(sad, "Well, at least he died doing what he loved.")
        chatNpc(
            sad,
            "True, I will raise a drink for him tonight, he was a good man, even if he was a " +
                "pacifist.",
        )
        chatPlayer(neutral, "As will I. See you around, Litara.")
        chatNpc(neutral, "Goodbye, ${player.displayName}.")
        player.brotherFound = BROTHER_REPORTED
    }

    private suspend fun Dialogue.brotherReported() {
        chatPlayer(happy, "I travelled to the bottom of the stronghold, and I emerge victorious!")
        chatNpc(
            happy,
            "I am glad the challenges of the stronghold have been overcome; it feels like my " +
                "brother has somehow been avenged.",
        )
    }

    private companion object {
        const val BROTHER_FOUND = 1
        const val BROTHER_REPORTED = 2
    }
}
