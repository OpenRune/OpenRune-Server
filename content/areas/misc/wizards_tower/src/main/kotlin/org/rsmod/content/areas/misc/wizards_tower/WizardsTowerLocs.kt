package org.rsmod.content.areas.misc.wizards_tower

import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WizardsTowerLocs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.fai_wiztower_spiralstairs") { climb(FIRST_FLOOR) }
        onOpLoc1("loc.fai_wiztower_spiralstairs_middle") { climbMiddle() }
        onOpLoc2("loc.fai_wiztower_spiralstairs_middle") { climb(SECOND_FLOOR) }
        onOpLoc3("loc.fai_wiztower_spiralstairs_middle") { climb(GROUND_FLOOR) }
        onOpLoc1("loc.fai_wiztower_spiralstairstop") { climb(FIRST_FLOOR) }
        onOpLoc1("loc.fai_wiztower_bookcase") { searchBookcase() }
        onOpLoc1("loc.fai_wiztower_bookcase_wall") { searchBookcase() }
        onOpLoc1("loc.tote_portal_to_gotr_parent") { stepThroughPortal(GUARDIANS_OF_THE_RIFT) }
        onOpLoc1("loc.gotr_entry") { stepThroughPortal(TOWER_BASEMENT) }
    }

    private suspend fun ProtectedAccess.climb(dest: CoordGrid) {
        arriveDelay()
        telejump(dest)
    }

    private suspend fun ProtectedAccess.climbMiddle() {
        arriveDelay()
        startDialogue {
            val dest =
                choice2(
                    "Climb up the stairs.",
                    SECOND_FLOOR,
                    "Climb down the stairs.",
                    GROUND_FLOOR,
                    title = "Climb up or down the stairs?",
                )
            access.telejump(dest)
        }
    }

    private suspend fun ProtectedAccess.searchBookcase() {
        arriveDelay()
        val book = BOOKS[random.of(0, BOOKS.size - 1)]
        startDialogue {
            mesbox(
                "There's a large selection of books, the majority of which look fairly old. Some " +
                    "very strange names... You pick one at random :"
            )
            mesbox("'$book'")
            mesbox("Interesting...")
        }
    }

    private suspend fun ProtectedAccess.stepThroughPortal(dest: CoordGrid) {
        arriveDelay()
        mes("You step through the portal...", ChatType.Spam)
        soundSynth(PORTAL_SYNTH)
        telejump(dest)
    }

    private companion object {
        const val PORTAL_SYNTH = "synth.gotr_portal_step"
        val GROUND_FLOOR = CoordGrid(3104, 3161, 0)
        val FIRST_FLOOR = CoordGrid(3104, 3161, 1)
        val SECOND_FLOOR = CoordGrid(3104, 3161, 2)
        val GUARDIANS_OF_THE_RIFT = CoordGrid(3615, 9470, 0)
        val TOWER_BASEMENT = CoordGrid(3104, 9573, 0)

        val BOOKS =
            listOf(
                "The Dark Arts of Magical Wands",
                "So you think you're a Mage? Volume 28",
                "Living with a Wizard Husband - a Housewife's Story",
                "The Life & Times of a Thingummywut by Traiborn the Wizard",
                "Fire, Earth and Water - What's it all about?",
                "How to become the Ultimate Wizard of the Universe",
                "101 Ways to Impress Your Mates with Magic",
                "Wind Strike for Beginners",
            )
    }
}
