package org.rsmod.content.areas.city.varrock.museum

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.MesAnimType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class NaturalHistoryQuiz @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    override fun ScriptContext.startup() {
        NaturalHistoryDisplays.forEachIndexed { index, display ->
            onOpLoc1(display.plaque) { studyPlaque(index) }
        }
        AnswerComponents.forEachIndexed { choice, component ->
            onIfModalButton(component) { answer(choice) }
        }
    }

    private fun ProtectedAccess.studyPlaque(index: Int) {
        if (!player.nathisAgreed) {
            mes("This looks like some sort of quiz. I wonder if I can take part...")
            return
        }
        val display = NaturalHistoryDisplays[index]
        if (vars[display.varbit] >= QUESTIONS_PER_DISPLAY) {
            openCreatureInfo(display)
            return
        }
        openQuestion(index, random.of(display.questions.size))
    }

    private fun ProtectedAccess.openCreatureInfo(display: NaturalHistoryDisplay) {
        clearQuizState()
        setDisplayHeader(display)
        ifSetHide("component.vm_natural_history:vm_nathis_creature_text", false)
        ifOpenMainModal("interface.vm_natural_history")
    }

    private fun ProtectedAccess.openQuestion(index: Int, questionIndex: Int) {
        val display = NaturalHistoryDisplays[index]
        val question = display.questions[questionIndex]
        quizDisplay = index + 1
        quizQuestion = questionIndex

        setDisplayHeader(display)
        ifSetText("component.vm_natural_history:vm_nathis_question", question.text)
        AnswerComponents.forEachIndexed { choice, component ->
            ifSetText(component, question.answers[choice])
        }
        ifSetHide("component.vm_natural_history:vm_nathis_quiz_text", false)
        ifOpenMainModal("interface.vm_natural_history")
    }

    private fun ProtectedAccess.setDisplayHeader(display: NaturalHistoryDisplay) {
        ifSetText("component.vm_natural_history:vm_nathis_text", display.title)
        ifSetText("component.vm_natural_history:vm_nathis_display_num", display.number.toString())
        for (model in display.models) {
            ifSetModel(model.component, model.model.asRSCM())
            if (display.rotates) {
                ifSetRotateSpeed(model.component, 0, MODEL_ROTATE_SPEED)
            }
        }
    }

    private suspend fun ProtectedAccess.answer(choice: Int) {
        val index = quizDisplay - 1
        if (index !in NaturalHistoryDisplays.indices) {
            return
        }
        val display = NaturalHistoryDisplays[index]
        val questionIndex = quizQuestion
        val question = display.questions[questionIndex]
        ifCloseSub("interface.vm_natural_history")

        if (choice != question.correct) {
            startDialogue {
                chatOrlando(confused, "Hmm, I don't think that's right, mate. Try again.")
            }
            openQuestion(index, questionIndex)
            return
        }

        val progress = vars[display.varbit] + 1
        vars[display.varbit] = progress
        val displayDone = progress >= QUESTIONS_PER_DISPLAY
        if (displayDone) {
            player.vmKudos += KUDOS_PER_DISPLAY
            player.openKudosOverlay(eventBus)
            mes("You have earned Kudos with the Museum!", ChatType.Spam)
        }
        soundSynth("synth.vm_nathis_correct")
        if (displayDone) {
            soundSynth("synth.vm_kudos_gain")
        }
        startDialogue {
            chatOrlando(happy, "Nice job, mate. That looks about right.")
        }

        if (!displayDone) {
            openQuestion(index, nextQuestion(display, questionIndex))
            return
        }

        clearQuizState()
        soundSynth("synth.vm_nathis_display_done")
        startDialogue {
            chatOrlando(happy, "Bonza, mate! I think that's all of them.")
        }
    }

    private fun ProtectedAccess.nextQuestion(display: NaturalHistoryDisplay, current: Int): Int {
        val count = display.questions.size
        return (current + 1 + random.of(count - 1)) % count
    }

    private fun ProtectedAccess.clearQuizState() {
        quizDisplay = 0
        quizQuestion = 0
    }

    private companion object {
        const val QUESTIONS_PER_DISPLAY = 3
        const val KUDOS_PER_DISPLAY = 2
        const val MODEL_ROTATE_SPEED = 5

        val AnswerComponents =
            listOf(
                "component.vm_natural_history:vm_nathis_answer_01",
                "component.vm_natural_history:vm_nathis_answer_02",
                "component.vm_natural_history:vm_nathis_answer_03",
            )
    }
}

internal const val NATHIS_ALL_DONE = (1 shl 28) - 1

internal suspend fun Dialogue.chatOrlando(mesanim: MesAnimType, text: String) {
    chatNpcSpecific("Orlando Smith", "npc.vm_nathis_apprentice", mesanim, text)
}

internal var Player.nathisAgreed by boolVarBit("varbit.vm_nathis_agreed")
internal val Player.nathisDisplaysDone by intVarBit("varbit.vm_nathis_display_alldone")
internal var Player.nathisRewardGiven by boolVarBit("varbit.vm_nathis_reward_given")
internal var Player.vmKudos by intVarBit("varbit.vm_kudos")

private var ProtectedAccess.quizDisplay by intVarBit("varbit.vm_nathis_quiz_display")
private var ProtectedAccess.quizQuestion by intVarBit("varbit.vm_nathis_quiz_question")
