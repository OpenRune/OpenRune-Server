package org.rsmod.content.skills.agility

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.interfaces.omnishop.openOmnishop
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WormTongueScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(WORM_TONGUE_UNMET) { startDialogue(it.npc) { talk() } }
        onOpNpc1(WORM_TONGUE_MET) { startDialogue(it.npc) { talk() } }
        onOpNpc3(WORM_TONGUE_MET) { openOmnishop(SHOP) }
        // TODO(content): Stats, the start ladder's call before the first talk, and the graceful
        // crafting kit, which the omnishop cannot sell as it is a dummy item.
    }

    private suspend fun Dialogue.talk() {
        chatPlayer(neutral, "Hello.")
        chatNpc(happy, "Hello there, my short-tongued friend. What can I do for you?")
        if (player.metGuide) {
            // UNVERIFIED: a returning player's options; only a first talk is captured.
            val trade = choice2("What are you doing here?", false, "Would you like to trade?", true)
            if (trade) {
                access.openOmnishop(SHOP)
                return
            }
        }
        introduction()
    }

    private suspend fun Dialogue.introduction() {
        chatPlayer(quiz, "What are you doing here?")
        chatNpc(quiz, "What do you think I'm doing here?")
        chatPlayer(confused, "I wouldn't like to guess.")
        chatNpc(
            sad,
            "How disappointing. I enjoy a good guessing game! No matter. My colleague and I " +
                "are here are on a mission of preservation!",
        )
        chatPlayer(quiz, "Preservation of what?")
        chatNpc(
            happy,
            "Preservation of these magnificent colossal wyrm remains, of course! You see, we " +
                "have a termite problem.",
        )
        chatPlayer(happy, "I'm beginning to see your role in all of this.")
        chatNpc(
            happy,
            "Indeed! And while my archaeological skills are certainly exemplary, I also bring a " +
                "certain set of more unique skills to this troublesome assignment.",
        )
        chatNpcSpecific(
            "Construction Worker",
            WORKER,
            neutral,
            "Oh for goodness sake, he just eats the termites!",
        )
        chatNpc(shifty, "Ahem! I am able to dispose of the termites in ways others cannot.")
        chatPlayer(quiz, "I see. So can I help your preservation efforts?")
        chatNpc(
            happy,
            "Why yes! It seems our scaffolding has proven to be a useful place for adventurous " +
                "types to practice running and jumping in quick succession.",
        )
        chatNpc(
            happy,
            "Should you also wish to practice these skills, you may come across the " +
                "aforementioned termites while you traverse the ropes. Simply scoop them up and " +
                "bring them to me.",
        )
        chatNpc(
            happy,
            "I'll happily exchange them for some goodies I've collected during my time here! You " +
                "can also keep any bone shards they may have unfortunately chipped off.",
        )
        chatPlayer(neutral, "Sounds simple enough!")
        chatNpc(
            happy,
            "Wonderful! To get started, just use the ladder by the wyrm's skull to climb up onto " +
                "the scaffolding.",
        )
        chatNpc(
            happy,
            "Initially the route is the same for everyone, but part way round you'll find a " +
                "loftier section of scaffolding. If you're skilled, that route might be more " +
                "appealing to you.",
        )
        chatNpc(
            happy,
            "That said, don't go up there unless you know what you're doing. I have work to do " +
                "so I can't spend all my time patching people up when they hurt themselves!",
        )
        objbox(
            CLIMB_ICON,
            CLIMB_ICON_ZOOM,
            "You'll need at least 50 Agility to tackle the basic course and 62 Agility to take " +
                "on the advanced course.",
        )
        chatPlayer(happy, "Great, thanks!")
        player.metGuide = true
        chatNpc(happy, "Good luck!")
    }

    private companion object {
        const val WORM_TONGUE_UNMET = "npc.varlamore_wyrm_agility_guide_1op"
        const val WORM_TONGUE_MET = "npc.varlamore_wyrm_agility_guide_3op"
        const val WORKER = "npc.varlamore_wyrm_agility_worker"
        const val SHOP = "dbrow.wyrm_agility_rewards_shop"
        const val CLIMB_ICON = "obj.agility_climb"
        const val CLIMB_ICON_ZOOM = 400

        var Player.metGuide by boolVarBit("varbit.varlamore_wyrm_agility_met_guide")
    }
}
