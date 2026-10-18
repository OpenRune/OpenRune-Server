package org.rsmod.content.areas.misc.monastery.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal var Player.monasteryOrder by boolVarBit("varbit.monastery_enter")

/** The monks of Edgeville Monastery: free healing, and the Abbot who lets you upstairs. */
class MonasteryMonks @Inject constructor(private val worldRepo: WorldRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.monk") { startDialogue(it.npc) { monk(it.npc) } }
        onOpNpc1("npc.abbot_langley") { startDialogue(it.npc) { abbot(it.npc) } }
        onOpNpc1("npc.brother_althric") { startDialogue(it.npc) { althric() } }
    }

    private suspend fun Dialogue.monk(npc: Npc) {
        chatNpc(neutral, "Greetings traveller.")
        val choice =
            if (player.monasteryOrder) {
                choice2(HEAL_OPTION, 1, OUT_OF_THE_WAY, 2)
            } else {
                choice3(HEAL_OPTION, 1, OUT_OF_THE_WAY, 2, "How do I get further into the monastery?", 3)
            }
        when (choice) {
            1 -> {
                chatPlayer(quiz, HEAL_OPTION)
                chatNpc(neutral, "Ok.")
                heal(npc)
            }
            2 -> outOfTheWay()
            else -> {
                chatPlayer(quiz, "How do I get further into the monastery?")
                chatNpc(
                    neutral,
                    "You'll need to talk to Abbot Langley about that. He's usually to be found " +
                        "walking the halls of the monastery.",
                )
            }
        }
    }

    private suspend fun Dialogue.abbot(npc: Npc) {
        chatNpc(neutral, "Greetings traveller.")
        val choice =
            if (player.monasteryOrder) {
                choice2(HEAL_OPTION, 1, OUT_OF_THE_WAY, 2)
            } else {
                choice3(HEAL_OPTION, 1, OUT_OF_THE_WAY, 2, "How do I get further into the monastery?", 3)
            }
        when (choice) {
            1 -> {
                chatPlayer(quiz, HEAL_OPTION)
                chatNpc(neutral, "Ok.")
                access.mesbox("Abbot Langley places his hands on your head. You feel a little better.")
                access.statHeal("stat.hitpoints", constant = 0, percent = 100)
            }
            2 -> outOfTheWay()
            else -> joinOrder()
        }
    }

    private suspend fun Dialogue.joinOrder() {
        chatPlayer(quiz, "How do I get further into the monastery?")
        chatNpc(
            neutral,
            "I'm sorry but only members of our order are allowed in the second level of the monastery.",
        )
        if (!choice2("Well can I join your order?", true, "Oh, sorry.", false)) {
            chatPlayer(neutral, "Oh, sorry.")
            return
        }
        chatPlayer(quiz, "Well can I join your order?")
        if (player.basePrayerLvl < ORDER_PRAYER_LEVEL) {
            chatNpc(neutral, "No. I am sorry, but I feel you are not devout enough.")
            access.mes("You need a prayer level of $ORDER_PRAYER_LEVEL to join the order.")
            return
        }
        chatNpc(happy, "Ok, I see you are someone suitable for our order. You may join.")
        player.monasteryOrder = true
    }

    private suspend fun Dialogue.outOfTheWay() {
        chatPlayer(quiz, OUT_OF_THE_WAY)
        chatNpc(
            neutral,
            "We like it that way actually! We get disturbed less. We still get rather a large " +
                "amount of travellers looking for sanctuary and healing here as it is!",
        )
    }

    private suspend fun Dialogue.althric() {
        chatPlayer(neutral, "Very nice rosebushes you have here.")
        chatNpc(
            neutral,
            "Yes, it has taken me many long hours in this garden to bring them to this state of " +
                "near-perfection.",
        )
    }

    private fun Dialogue.heal(npc: Npc) {
        npc.anim("seq.human_castentangle")
        access.spotanim("spotanim.heal_casting", height = HEAL_SPOT_HEIGHT)
        worldRepo.soundArea(npc.coords, HEAL_SOUND, radius = HEAL_SOUND_RADIUS)
        access.statHeal("stat.hitpoints", constant = 0, percent = 100)
        access.mes("You feel a little better.")
    }

    private companion object {
        private const val HEAL_OPTION = "Can you heal me? I'm injured."
        private const val OUT_OF_THE_WAY = "Isn't this place built a bit out of the way?"
        private const val ORDER_PRAYER_LEVEL = 31
        private const val HEAL_SPOT_HEIGHT = 120
        private const val HEAL_SOUND = "synth.heal"
        private const val HEAL_SOUND_RADIUS = 10
    }
}
