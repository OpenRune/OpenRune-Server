package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SmolHereditDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            if (access.random.of(RARE_CHANCE) == 0) {
                chatPlayer(happy, "Hello, Smol Heredit.")
                chatNpc(worried, "Mankind's variation frightens me.")
                chatNpc(
                    neutral,
                    "The way they saw this universe, their modes of impression, differed so " +
                        "greatly from one another that to read their stories, to know their " +
                        "lives, is to wonder if they were not many species in one.",
                )
                chatNpc(
                    neutral,
                    "It was as if they had ranged across the actions of a thousand species and " +
                        "taken them for their own.",
                )
                chatNpc(
                    neutral,
                    "For nearly two thousand years they dreamed in a creative mania without antecedent.",
                )
                chatNpc(
                    neutral,
                    "They looked into the eyes and minds of other species and with their own " +
                        "intellect superseded them.",
                )
                chatNpc(
                    neutral,
                    "They destroyed many, coopted others, and in the space between necessity and " +
                        "carelessness, left some to grow as untended as any species could in the " +
                        "wake of their ubiquity.",
                )
                chatNpc(
                    neutral,
                    "They swarmed across the continents and oceans of a Gielinor they called " +
                        "their own, and shattered it into a billion imbricate worlds.",
                )
                chatNpc(
                    neutral,
                    "And they were, by the twisted jungles of their slaughterhouses, ruthless " +
                        "and aloof beyond measure.",
                )
                chatNpc(sad, "The worst of us.")
                chatNpc(sad, "A superpredator.")
                chatNpc(sad, "A bringer of mass extinction.")
                chatNpc(neutral, "All mountains move and all giants are mountains.")
                chatNpc(neutral, "We rest upon their shoulders.")
                chatNpc(neutral, "The world has grown older, senescent.")
                chatNpc(
                    neutral,
                    "We are chimpanzee, we are gorilla, we are orangutan, and we are baboon and more.",
                )
                chatNpc(neutral, "We are, whether we like it or not, the blood legatees of humanity.")
                chatNpc(
                    neutral,
                    "We are the children of men, but our mistakes will not be their mistakes.",
                )
                chatNpc(neutral, "We are telling a story.")
                chatNpc(
                    neutral,
                    "We are telling a story of life, of what it means to live together in this " +
                        "universe, with vastly different modes of being, of seeing, of knowing.",
                )
                chatNpc(
                    neutral,
                    "Of sharing our own internal worlds with each other and making the universe " +
                        "we live in better for it. The empathy of a mind speaking to another " +
                        "mind, a vastly different mind, and yet each understanding each.",
                )
                chatNpc(neutral, "And if not, if the barriers seem too great, then striving to overcome them.")
                chatNpc(
                    sad,
                    "Sorrow in what humanity did to this world, in the horrors they unleashed, " +
                        "in their cataclysm of despair, in the tens of thousands of species that " +
                        "will never walk or swim or fly again.",
                )
                chatNpc(
                    happy,
                    "Extinctions abounded in their wake, and yet look now at this sunbright " +
                        "land, at the joys we have brought to it, to have bound the chaotic " +
                        "climate to our will.",
                )
                chatNpc(
                    happy,
                    "To have thought deeply of the future of this world, a sense of respect, of " +
                        "shared reverence for it and all who live within it.",
                )
                chatNpc(happy, "We have strung the close sky with life.")
                chatNpc(happy, "Be proud.")
                chatNpc(
                    happy,
                    "And empathy, once limited only to mankind and some few species, now " +
                        "extends across all lands, across all time.",
                )
                chatPlayer(silent, "...")
                chatPlayer(confused, "I'm sorry, but what?!")
            } else {
                chatPlayer(happy, "How you doing, Smol Heredit?")
                chatNpc(
                    sad,
                    "I'm still disappointed that my father failed against you. I will suffer in " +
                        "humiliation alongside him for the rest of time.",
                )
                chatPlayer(neutral, "Understandable.")
            }
        }

    private companion object {
        const val NPC = "npc.solheredit_pet"
        const val RARE_CHANCE = 20
    }
}
