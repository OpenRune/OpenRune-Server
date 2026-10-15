package org.rsmod.content.areas.misc.dwarvenmine.npcs

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The Oracle on Ice Mountain, and Rolad studying outside the mine's north entrance. */
class IceMountainNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.oracle") { consultOracle(it.npc) }
        onOpNpc1("npc.dwarfrock_rolad") {
            startDialogue(it.npc) {
                chatNpc(neutral, "Can you leave me alone please? I'm trying to study.")
            }
        }
    }

    private suspend fun ProtectedAccess.consultOracle(npc: Npc) {
        val wisdom = WISDOM[random.of(maxExclusive = WISDOM.size)]
        startDialogue(npc) {
            chatPlayer(quiz, "Can you impart your wise knowledge to me, O Oracle?")
            chatNpc(neutral, wisdom)
        }
    }

    private companion object {
        private val WISDOM =
            listOf(
                "Capes are always in fashion!",
                "The light at the end of the tunnel is the demon-infested lava pit.",
                "Beware the cabbage: it is both green AND leafy.",
                "They say that ham does not mix well with other kinds of meat.",
                "A woodchuck does not chuck wood.",
                "No. I'm not in the mood.",
                "It's not you; it's me.",
                "Too many cooks spoil the anchovy pizza.",
                "The God Wars are over...as long as the thing they were fighting over remains hidden.",
                "He who uses the power of custard mixes it with his tears.",
                "Nothing like a tasty fish.",
                "Don't judge a book by its cover - judge it on its' grammar and, punctuation.",
                "Yes, I can. But I'm not going to.",
                "A bird in the hand can make a tasty snack.",
                "Pies...they're great, aren't they?",
                "Who guards the guardsmen?",
                "The great snake of Guthix guards more than she knows.",
                "Do not fear the dragons...fear their kin.",
                "Everyone you know will one day be dead.",
                "If a tree falls in the forest and no one is around, then nobody gets Woodcutting xp.",
                "Sometimes you get lucky, sometimes you don't.",
                "The chicken came before the egg.",
                "Is it time to wake up? I am not sure...",
                "When in Asgarnia, do as the Asgarnians do.",
                "An answer is unimportant; it is the question that matters.",
                "The goblins will never make up their minds on their own.",
                "Jas left a stone behind.",
                "Many secrets are buried under this land.",
                "There are no crisps at the party.",
                "Help wanted? Enquire within.",
            )
    }
}
