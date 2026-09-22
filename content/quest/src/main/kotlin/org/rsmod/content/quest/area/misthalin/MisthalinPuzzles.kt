package org.rsmod.content.quest.area.misthalin

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.quest.manager.Quest
import org.rsmod.plugin.scripts.ScriptContext

internal class MisthalinPuzzles(private val quest: Quest) {

    fun register(ctx: ScriptContext): Unit =
        with(ctx) {
            onOpLoc1("loc.mistmyst_piano") { openPiano() }

            onIfModalButton("component.mistmyst_piano:close_button") {
                ifCloseSub("interface.mistmyst_piano")
            }

            for (note in PianoNotes) {
                onIfModalButton(note.component) { playNote(note) }
            }
            for (gem in GemSwitches) {
                onIfModalButton(gem.component) { flipSwitch(gem) }
            }
        }

    private fun ProtectedAccess.openPiano() {
        ifOpenMainModal("interface.mistmyst_piano")
    }

    private suspend fun ProtectedAccess.playNote(note: PianoNote) {
        mes("You play ${note.article} ${note.letter}.")
        soundSynth(note.synth)

        if (quest.getQuestStage(player) != MisthalinStage.Note2Read) {
            return
        }

        val mask = mistmystPianoMask
        val index = Integer.bitCount(mask)
        if (index < PianoAnswer.size && note.component == PianoAnswer[index]) {
            mistmystPianoMask = mask or (1 shl index)
            vars[MisthalinPianoNoteVarbits[index]] = 1
        }

        val attempts = (mistmystPianoAttempts + 1) % PianoAnswer.size
        mistmystPianoAttempts = attempts
        if (attempts != 0) {
            return
        }

        if (mistmystPianoMask == PianoSolvedMask) {
            quest.setQuestStage(this, MisthalinStage.PianoSolved)
            mes("There is a clicking sound as the compartment on the piano unlocks.")
            clearPianoBits()
            ifCloseSub("interface.mistmyst_piano")
            return
        }

        mes("There is a clicking sound as the lock on the piano's compartment resets.")
        ifCloseSub("interface.mistmyst_piano")
        startDialogue {
            chatPlayer(
                neutral,
                "I don't think that's right.. Perhaps the clue the killer left behind could help?",
            )
        }
        clearPianoBits()
    }

    private fun ProtectedAccess.clearPianoBits() {
        mistmystPianoMask = 0
        mistmystPianoAttempts = 0
        for (varbit in MisthalinPianoNoteVarbits) {
            vars[varbit] = 0
        }
    }

    private suspend fun ProtectedAccess.flipSwitch(gem: GemSwitch) {
        mes("You flip the ${gem.label} switch.")
        soundSynth("synth.interface_select")

        showFlippedGem(gem)

        if (quest.getQuestStage(player) != MisthalinStage.PanelRevealed) {
            return
        }

        val mask = mistmystGemMask
        val index = Integer.bitCount(mask)
        if (index < GemAnswer.size && gem.label == GemAnswer[index]) {
            mistmystGemMask = mask or (1 shl index)
            vars[MisthalinGemNoteVarbits[index]] = 1
        }

        val attempts = (mistmystGemAttempts + 1) % GemAnswer.size
        mistmystGemAttempts = attempts
        if (attempts != 0) {
            return
        }

        if (mistmystGemMask == GemSolvedMask) {
            quest.setQuestStage(this, MisthalinStage.GemPanelSolved)
            mes("There is a clicking sound as the compartment in the fireplace unlocks.")
            clearGemBits()
            ifCloseSub("interface.mistmyst_gem_puzzle")
            return
        }

        mes("There is a clicking sound as the lock on the panel resets.")
        ifCloseSub("interface.mistmyst_gem_puzzle")
        startDialogue {
            chatPlayer(
                neutral,
                "I don't think that's the right order.. Perhaps the clue the killer left behind " +
                    "could help?",
            )
        }
        clearGemBits()
    }

    private fun ProtectedAccess.showFlippedGem(gem: GemSwitch) {
        val slot = player.misthalinGemRowSlot
        if (slot < GemRowSlots) {
            ifSetModel("component.mistmyst_gem_puzzle:selected_${slot + 1}", gem.model)
            player.misthalinGemRowSlot = slot + 1
        }
    }

    fun openGemPanel(access: ProtectedAccess) {
        access.ifOpenMainModal("interface.mistmyst_gem_puzzle")
        access.player.misthalinGemRowSlot = 0
    }

    private fun ProtectedAccess.clearGemBits() {
        mistmystGemMask = 0
        mistmystGemAttempts = 0
        for (varbit in MisthalinGemNoteVarbits) {
            vars[varbit] = 0
        }
    }

    private class PianoNote(val component: String, val letter: Char, val synth: String) {
        val article: String
            get() = if (letter in VowelSoundedNotes) "an" else "a"
    }

    private class GemSwitch(val component: String, val label: String) {
        val model: Int get() = "models.misthalin_gem_$label".asRSCM()
    }

    private companion object {
        private val PianoAnswer =
            listOf(
                "component.mistmyst_piano:label_d1",
                "component.mistmyst_piano:label_e1",
                "component.mistmyst_piano:label_a2",
                "component.mistmyst_piano:label_d1",
            )

        private const val PianoSolvedMask = 15

        private const val GemSolvedMask = 63

        private const val GemRowSlots = 6

        private val VowelSoundedNotes = setOf('A', 'E', 'F')

        private val GemAnswer = listOf("sapphire", "diamond", "zenyte", "emerald", "onyx", "ruby")

        private val PianoNotes =
            listOf(
                PianoNote("component.mistmyst_piano:label_c1", 'C', "synth.grim_piano_c4"),
                PianoNote("component.mistmyst_piano:label_d1", 'D', "synth.grim_piano_d4"),
                PianoNote("component.mistmyst_piano:label_e1", 'E', "synth.grim_piano_e4"),
                PianoNote("component.mistmyst_piano:label_f1", 'F', "synth.grim_piano_f4"),
                PianoNote("component.mistmyst_piano:label_g1", 'G', "synth.grim_piano_g4"),
                PianoNote("component.mistmyst_piano:label_a2", 'A', "synth.grim_piano_a4"),
                PianoNote("component.mistmyst_piano:label_b2", 'B', "synth.grim_piano_b4"),
                PianoNote("component.mistmyst_piano:label_c2", 'C', "synth.grim_piano_c5"),
                PianoNote("component.mistmyst_piano:label_d2", 'D', "synth.grim_piano_d5"),
                PianoNote("component.mistmyst_piano:label_e2", 'E', "synth.grim_piano_e5"),
                PianoNote("component.mistmyst_piano:label_f2", 'F', "synth.grim_piano_f5"),
                PianoNote("component.mistmyst_piano:label_g2", 'G', "synth.grim_piano_g5"),
                PianoNote("component.mistmyst_piano:label_a3", 'A', "synth.grim_piano_a5"),
                PianoNote("component.mistmyst_piano:label_b3", 'B', "synth.grim_piano_b5"),
            )

        private val GemSwitches =
            listOf(
                GemSwitch("component.mistmyst_gem_puzzle:diamond", "diamond"),
                GemSwitch("component.mistmyst_gem_puzzle:onyx", "onyx"),
                GemSwitch("component.mistmyst_gem_puzzle:zenyte", "zenyte"),
                GemSwitch("component.mistmyst_gem_puzzle:ruby", "ruby"),
                GemSwitch("component.mistmyst_gem_puzzle:sapphire", "sapphire"),
                GemSwitch("component.mistmyst_gem_puzzle:emerald", "emerald"),
            )
    }
}
