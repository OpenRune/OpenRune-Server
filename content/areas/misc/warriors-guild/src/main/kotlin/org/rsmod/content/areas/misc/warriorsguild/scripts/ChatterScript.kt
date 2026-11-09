package org.rsmod.content.areas.misc.warriorsguild.scripts

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.script.onAiConTimer
import org.rsmod.api.script.onGameStartup
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.NpcList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ChatterScript
@Inject
constructor(private val conRepo: ControllerRepository, private val npcs: NpcList) :
    PluginScript() {
    private var ticks = 0
    private var shanomiLine = 0

    override fun ScriptContext.startup() {
        onGameStartup {
            val controller = Controller(CONTROLLER, CoordGrid(2860, 3545, 0))
            conRepo.add(controller, Int.MAX_VALUE)
            controller.aiTimer(1)
        }
        onAiConTimer(CONTROLLER) { chatter() }
    }

    private fun chatter() {
        ticks++
        if (ticks % SHANOMI_EVERY == 0) {
            say("npc.warguild_shanomi", SHANOMI_LINES[shanomiLine++ % SHANOMI_LINES.size])
        }
        for (speaker in RANDOM_SPEAKERS) {
            if (ticks % speaker.every == 0) say(speaker.npc, speaker.lines.random())
        }
    }

    private fun say(npc: String, line: String) {
        val id = npc.asRSCM(RSCMType.NPC)
        npcs.firstOrNull { it.id == id }?.say(line)
    }

    private data class Speaker(val npc: String, val every: Int, val lines: List<String>)

    private companion object {
        const val CONTROLLER = "controller.warguild_chatter"
        const val SHANOMI_EVERY = 15

        val SHANOMI_LINES =
            listOf(
                "Think not dishonestly.",
                "The Way in training is.",
                "Acquainted with every art become.",
                "Ways of all professions know you.",
                "Gain and loss between you must distinguish.",
                "Judgment and understanding for everything develop you must.",
                "Those things which cannot be seen, perceive them.",
                "Trifles pay attention even to.",
                "Do nothing which is of no use.",
                "Way of the Warrior this is.",
            )

        val RANDOM_SPEAKERS =
            listOf(
                Speaker(
                    "npc.warguild_kamfreena",
                    50,
                    listOf(
                        "Life isn't fair, that doesn't mean you can't win.",
                        "Be master of your mind rather than mastered by your mind.",
                        "A reflection on a pool of water does not reveal its depth.",
                        "When you aim for perfection, you discover it's a moving target.",
                        "He who speaks in anger makes his anger heard, but his words forgotten.",
                        "Patience and persistence can bring down the tallest trees.",
                    ),
                ),
                Speaker(
                    "npc.warguild_armour_shopkeeper",
                    35,
                    listOf(
                        "A fine selection of blades for you to peruse, come take a look!",
                        "Armours and axes to suit your needs.",
                        "Imported weapons from the finest smithys around the lands!",
                        "Ow my toe! That armour is heavy.",
                    ),
                ),
                Speaker(
                    "npc.warguild_food_shopkeeper",
                    40,
                    listOf(
                        "Come try my lovely pizza or maybe some fish!",
                        "Stew to fill the belly, on sale here!",
                        "Potatoes are filling and healthy too!",
                    ),
                ),
                Speaker(
                    "npc.warguild_potion_shopkeeper",
                    30,
                    listOf(
                        "Pink bananas... mmmmmm.",
                        "Demons are a Ghoul's best Friend.",
                        "It's not an optical illusion, it just looks like one.",
                        "Don't look back, they might be gaining on you.",
                        "If you don't care where you are, then you ain't lost.",
                        "Madness takes its toll. Please have exact change.",
                    ),
                ),
                Speaker("npc.warguild_cyclops_pet", 50, listOf("Mooom!", "Rawr", "I'm hungry!")),
            )
    }
}
