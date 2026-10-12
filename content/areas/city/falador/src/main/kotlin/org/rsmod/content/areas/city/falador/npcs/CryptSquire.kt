package org.rsmod.content.areas.city.falador.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Squire_(Crypt) (before Recruitment Drive)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:recruitment_drive] Temple Knight greeting and replies
class CryptSquire : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.white_knight_crypt_squire") { startDialogue(it.npc) { vicks() } }
    }

    private suspend fun Dialogue.vicks() {
        chatNpc(angry, "What do you want?")
        var next = otherOptions()
        while (true) {
            next =
                when (next) {
                    WHO -> whoAreYou()
                    PLACE -> thisPlace()
                    PORTAL -> portal()
                    else -> {
                        chatPlayer(neutral, "I'll leave you alone.")
                        return
                    }
                }
        }
    }

    private suspend fun Dialogue.otherOptions(): Int =
        choice4(
            "Who are you and what are you doing?",
            WHO,
            "Tell me about this place.",
            PLACE,
            "What's with the glowing portal thing?",
            PORTAL,
            "I'll leave you alone.",
            LEAVE,
        )

    private suspend fun Dialogue.whoAreYou(): Int {
        chatNpc(
            neutral,
            "I'm Vicks, squire to Sir Culler Flife. He's away on a surveillance mission to " +
                "Burthorpe, so he sent me down here to do some cleaning.",
        )
        val option =
            choice5(
                "You don't seem to be cleaning much.",
                CLEANING,
                "What's your master doing in Burthorpe?",
                MASTER,
                "Tell me about this place.",
                PLACE,
                "What's with the glowing portal thing?",
                PORTAL,
                "I'll leave you alone.",
                LEAVE,
            )
        return when (option) {
            CLEANING -> {
                chatNpc(
                    sad,
                    "Don't tell the knights, but I'm just not in the mood for it! I've served " +
                        "for years, and worked really hard, but they're not even thinking of " +
                        "making me a knight.",
                )
                chatNpc(
                    confused,
                    "I don't know what the problem is. At my last review, I heard the examiner " +
                        "muttering something about my name, but I've no idea what he meant by " +
                        "that.",
                )
                otherOptions()
            }
            MASTER -> {
                chatNpc(
                    shifty,
                    "Keeping an eye on those Imperial Guard. We can't trust 'em, you know. But I " +
                        "shouldn't talk about his mission.",
                )
                choice3(
                    "Tell me about this place.",
                    PLACE,
                    "What's with the glowing portal thing?",
                    PORTAL,
                    "I'll leave you alone.",
                    LEAVE,
                )
            }
            else -> option
        }
    }

    private suspend fun Dialogue.thisPlace(): Int {
        chatPlayer(quiz, "Tell me about this place.")
        chatNpc(
            neutral,
            "This place? It's the castle crypt. The greatest White Knights are buried here, " +
                "after a lifetime in the service of Saradomin. At least, that's how it used to be.",
        )
        chatNpc(
            neutral,
            "When I was a new recruit, they brought me here and told me that if I served " +
                "Saradomin faithfully, and with distinction, maybe I'd earn the honour of burial " +
                "in this place too.",
        )
        chatNpc(
            neutral,
            "But if you think about it, that's never going to happen - there just isn't space " +
                "for more tombs! Anyway, my plan is to avoid dying, and maybe marry one of the " +
                "barmaids when I retire.",
        )
        return otherOptions()
    }

    private suspend fun Dialogue.portal(): Int {
        chatPlayer(quiz, "What's with the glowing portal thing?")
        chatNpc(
            neutral,
            "It's because of something the Temple Knights have, called the Gaze of Saradomin. " +
                "They get the option of being brought to Falador when they die, so Death has to " +
                "maintain a presence in the area.",
        )
        chatNpc(
            worried,
            "That portal takes you to Death's own realm. He's got portals in all the places " +
                "where people go when they die - Lumbridge, Edgeville and the rest. I hate it - " +
                "the thing's terrifying!",
        )
        return otherOptions()
    }

    private companion object {
        const val WHO = 1
        const val PLACE = 2
        const val PORTAL = 3
        const val LEAVE = 4
        const val CLEANING = 5
        const val MASTER = 6
    }
}
