package org.rsmod.content.areas.misc.villages.feldip.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class JiggigOgres : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.zogre_dying1") { startDialogue(it.npc) { pilg() } }
        onOpNpc1("npc.zogre_dying2") { startDialogue(it.npc) { grug() } }
        onOpNpc1(UGLUG_NAR) { startDialogue(it.npc) { uglugNar() } }
        onOpNpc3(UGLUG_NAR) { startDialogue(it.npc) { noGlugGlugs() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Pilg
    private suspend fun Dialogue.pilg() {
        chatNpc(
            sad,
            "Dey got me in da belly, mees gutsies feel like had a dead dead dog dinner.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Grug
    private suspend fun Dialogue.grug() {
        chatNpc(sad, "Ukk...I's dun fer...me's don't feel legsies anymore!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Uglug_Nar
    // TODO(content): [system:treasure_trails] medium challenge scroll
    private suspend fun Dialogue.uglugNar() {
        if (QuestRequirements.hasCompleted(player, ZOGRE_FLESH_EATERS)) {
            uglugNarAfterQuest()
            return
        }
        val option =
            choice3(
                "Hey, what's going on here?",
                1,
                "What are you selling?",
                2,
                "Ok, thanks.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Hey, what's going on here?")
                chatNpc(
                    sad,
                    "Dem's dead ogre's come out of da ground...dey's makin' da rest of us into " +
                        "sick-uns ...and dead-uns.",
                )
                chatPlayer(worried, "That doesn't sound good!")
                chatNpc(neutral, "Grish want's da person go down der - see what's what!")
            }
            2 -> noGlugGlugs()
            3 -> chatPlayer(neutral, "Ok, thanks.")
        }
    }

    private suspend fun Dialogue.uglugNarAfterQuest() {
        val option = choice3("Hello again.", 1, "What are you selling?", 2, "Ok, thanks.", 3)
        when (option) {
            1 -> {
                chatPlayer(happy, "Hello again.")
                chatNpc(
                    happy,
                    "Hey yous creature...yous did good fings gedin that glug-glugs for da " +
                        "sickies! All is ogries pepels are not gettin dead cos of you.",
                )
            }
            2 -> {
                chatPlayer(quiz, "What are you selling?")
                // TODO(content): [system:met_flags] open Uglug's stuffsies once the player has
                //  sold him a balm (needs the
                //  shop-unlock varbit)
                noGlugGlugs()
            }
            3 -> chatPlayer(neutral, "Ok, thanks.")
        }
    }

    private suspend fun Dialogue.noGlugGlugs() {
        chatNpc(
            neutral,
            "Me's not got no glug-glugs to sell, yous bring me da sickies glug-glug den me's " +
                "open da stufsies for ya.",
        )
    }

    private companion object {
        const val UGLUG_NAR = "npc.zogre_uglug_nar"
        const val ZOGRE_FLESH_EATERS = "quest_zogreflesheaters"
    }
}
