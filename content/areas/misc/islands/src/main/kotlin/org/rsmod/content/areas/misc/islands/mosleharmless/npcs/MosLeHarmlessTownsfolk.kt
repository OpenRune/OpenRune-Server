package org.rsmod.content.areas.misc.islands.mosleharmless.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MosLeHarmlessTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.patchy_the_pirate") { startDialogue(it.npc) { patchy() } }
        onOpNpc1("npc.harmless_island_cave_pirate") { startDialogue(it.npc) { caveyDavey() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Patchy
    // TODO(content): [system:patchy_sewing] "Let me see what you can do." and the Sew option
    private suspend fun Dialogue.patchy() {
        chatNpc(happy, "Hello there! Can I sew ye somethin' tegether?")
        val explain =
            choice2("What do you mean?", true, "No, thank you.", false, title = "Select an option")
        if (!explain) {
            patchyNoThanks()
            return
        }
        chatPlayer(quiz, "What do you mean?")
        chatNpc(
            happy,
            "Well, ye see it works like this: I can sew yer eye patches onto yer pirate hats or " +
                "pirate bandanas. Just pirate bandanas mind ye, I hate the feel of snakeskin.",
        )
        mesbox("Patchy shudders at the thought of touching snakeskin.")
        chatNpc(
            happy,
            "I also sew crab claw gauntlets on to pirate hooks, highwayman masks to black " +
                "cavaliers, mime masks to black berets, partyhats to glasses and monocles to top " +
                "hats.",
        )
        chatNpc(
            happy,
            "If ye also have some flippers and suitably evil lookin' claws, I can probably put " +
                "them together too.",
        )
        chatNpc(
            neutral,
            "It costs 500 coins te do it, and 600 coins te get them separated again afterwards " +
                "if ye change yer mind.",
        )
        while (true) {
            val option =
                choice2(
                    "Why does it cost more to get them separated again?",
                    true,
                    "No, thank you.",
                    false,
                    title = "Select an option",
                )
            if (!option) {
                patchyNoThanks()
                return
            }
            chatPlayer(quiz, "Why does it cost more to get them separated again?")
            chatNpc(neutral, "Wear and tear on scissors. So, do ye have anything ye wants doin?")
        }
    }

    private suspend fun Dialogue.patchyNoThanks() {
        chatPlayer(neutral, "No, thank you.")
        chatNpc(happy, "Fair enough. See you around!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cavey_Davey
    private suspend fun Dialogue.caveyDavey() {
        when {
            WITCHWOOD_ICON in player.worn -> caveyDaveyIconWorn()
            WITCHWOOD_ICON in player.inv -> caveyDaveyIconCarried()
            else -> caveyDaveyNoIcon()
        }
    }

    private suspend fun Dialogue.caveyDaveyNoIcon() {
        chatNpc(shocked, "Be ye mad? There be Horrors in this cave!")
        chatPlayer(quiz, "What do you mean?")
        chatNpc(
            worried,
            "Have ye ever heard of the sort of evil, flesh-eatin' horrors that dwell in the " +
                "darkest pits of the world?",
        )
        chatNpc(
            worried,
            "The sort of dark, sanity-breakin' THINGS that cause the livin' to drop to their " +
                "knees and weep for the fate of all creation?",
        )
        chatNpc(quiz, "Well, have ye?")
        chatPlayer(neutral, "Yes, I think I've killed a few of them as well.")
        chatNpc(neutral, "Well, that's ok then.")
        chatNpc(
            neutral,
            "But, ye'll need a Witchwood Icon from a slayer master if ye want te go in these " +
                "caves and live.",
        )
        chatPlayer(quiz, "Why?")
        chatNpc(
            neutral,
            "Well, ye see them Jungle Horrors? Well down in the caves there be Cave Horrors.",
        )
        chatNpc(
            worried,
            "They are bigger, badder, meaner, and have a howl that freezes the blood in yer veins.",
        )
        chatNpc(
            neutral,
            "Wearin' earmuffs or a helmet won't work, cos them masks they wear make the sound " +
                "magical. Only thing that works is wearin' a Witchwood Icon.",
        )
        chatNpc(
            neutral,
            "That is, o'course, if ye can see them, cos if ye don't have any light down there " +
                "then yer likely te be picked te bones by the insects before the Horrors get ye.",
        )
        chatPlayer(happy, "I see, thanks for the warning.")
        chatNpc(happy, "Yer welcome.")
    }

    private suspend fun Dialogue.caveyDaveyIconCarried() {
        chatNpc(
            confused,
            "Be ye some form of simpleton? Do ye not hear the howlin' of the Horrors?",
        )
        chatPlayer(confused, "Well, I can hear something now you mention it...")
        chatNpc(worried, "That be them, howlin', always howlin'!")
        chatNpc(
            worried,
            "If ye value yer limbs ye'll put that Icon round yer neck, and hope they don't come " +
                "out into the light!",
        )
        chatPlayer(neutral, "Err, all right, I'll get right on that.")
    }

    private suspend fun Dialogue.caveyDaveyIconWorn() {
        chatNpc(quiz, "Be ye here te deal with the Horrors?")
        chatPlayer(neutral, "I might well give it a shot.")
        chatNpc(neutral, "Aye, well, keep yer Icon with ye if ye wants te walk out alive again.")
        chatPlayer(happy, "Don't worry, I will.")
    }

    private companion object {
        const val WITCHWOOD_ICON = "obj.witchwood_icon"
    }
}
