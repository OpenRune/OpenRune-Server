package org.rsmod.content.quest.manager

import org.rsmod.api.player.protect.ProtectedAccess

internal fun buildQuestJournal(
    access: ProtectedAccess,
    quest: Quest,
    builder: QuestJournalBuilder.() -> Unit,
): String = QuestJournalBuilder(access, quest, isCompletion = false).apply(builder).build()

internal fun buildCompletionJournal(
    access: ProtectedAccess,
    quest: Quest,
    builder: QuestJournalBuilder.() -> Unit,
): String {
    val journalBuilder = QuestJournalBuilder(access, quest, isCompletion = true).apply(builder)
    journalBuilder.addRawLine("<br><br><col=FF0000>QUEST COMPLETE!</col>")
    return journalBuilder.build()
}

@QuestJournalDsl
class QuestJournalBuilder internal constructor(
    val access: ProtectedAccess,
    val quest: Quest,
    private val isCompletion: Boolean = false,
) {
    private val lines = mutableListOf<String>()

    fun description(text: String, colour: String = DEFAULT_UNUSED_COLOUR) {
        line(text)
    }

    fun objective(
        text: String,
        colour: String = DEFAULT_UNUSED_COLOUR,
        builder: ObjectiveContext.() -> Unit,
    ) {
        val context = LineContext(access, quest, text, isCompletion)
        val objective = ObjectiveContext(context)
        objective.builder()
        val rendered = objective.render()
        lines += if (isCompletion) "<str>$rendered</str>" else "<blue>$rendered</blue>"
    }

    fun line(text: String, colour: String = DEFAULT_UNUSED_COLOUR) {
        val content = if (isCompletion) "<str>$text</str>" else "<blue>$text</blue>"
        lines += content
    }

    fun strike(
        text: String,
        colour: String = DEFAULT_UNUSED_COLOUR,
        strikeColour: String = DEFAULT_UNUSED_COLOUR,
    ) {
        lines += "<black><str>$text</str></black>"
    }

    internal fun addRawLine(text: String) {
        lines += text
    }

    fun build(): String = lines.joinToString("\n")

    @QuestJournalDsl
    class LineContext internal constructor(
        private val access: ProtectedAccess,
        private val quest: Quest,
        initialText: String,
        private val isCompletion: Boolean = false,
    ) {
        private var text: String = initialText
        private var struck: Boolean = false
        private var finalised: Boolean = false

        internal val isFinalised: Boolean
            get() = finalised

        fun attribute(
            attribute: QuestAttribute<Boolean>,
            text: String,
            strike: Boolean = false,
            colour: String = DEFAULT_UNUSED_COLOUR,
            finalise: Boolean = true,
        ): ConditionHandle {
            if (finalised) {
                return ConditionHandle(this, applied = false)
            }
            val applied = attribute.getOrNull(access.player) == true
            if (applied) {
                setLine(text, strike)
                if (finalise) {
                    finalised = true
                }
            }
            return ConditionHandle(this, applied)
        }

        fun hasItem(
            item: String,
            text: String,
            strike: Boolean = false,
            colour: String = DEFAULT_UNUSED_COLOUR,
            finalise: Boolean = false,
        ): ConditionHandle {
            if (finalised) {
                return ConditionHandle(this, applied = false)
            }
            val applied = access.inv.contains(item)
            if (applied) {
                setLine(text, strike)
                if (finalise) {
                    finalised = true
                }
            }
            return ConditionHandle(this, applied)
        }

        fun stageAtLeast(
            stage: Int,
            text: String,
            strike: Boolean = false,
            colour: String = DEFAULT_UNUSED_COLOUR,
            finalise: Boolean = false,
        ): ConditionHandle {
            if (finalised) {
                return ConditionHandle(this, applied = false)
            }
            val applied = quest.getQuestStage(access.player) >= stage
            if (applied) {
                setLine(text, strike)
                if (finalise) {
                    finalised = true
                }
            }
            return ConditionHandle(this, applied)
        }

        fun questState(
            state: QuestProgressState,
            text: String,
            strike: Boolean = false,
            colour: String = DEFAULT_UNUSED_COLOUR,
            finalise: Boolean = false,
        ): ConditionHandle {
            if (finalised) {
                return ConditionHandle(this, applied = false)
            }
            val applied = quest.questState(access.player) == state
            if (applied) {
                setLine(text, strike)
                if (finalise) {
                    finalised = true
                }
            }
            return ConditionHandle(this, applied)
        }

        fun custom(
            condition: Boolean,
            text: String,
            strike: Boolean = false,
            colour: String = DEFAULT_UNUSED_COLOUR,
            finalise: Boolean = false,
        ): ConditionHandle {
            if (finalised) {
                return ConditionHandle(this, applied = false)
            }
            if (condition) {
                setLine(text, strike)
                if (finalise) {
                    finalised = true
                }
            }
            return ConditionHandle(this, condition)
        }

        fun finalise(strike: Boolean = false, strikeColour: String = DEFAULT_UNUSED_COLOUR) {
            struck = struck || strike
            finalised = true
        }

        private fun setLine(
            value: String,
            strike: Boolean,
        ) {
            text = value
            struck = strike
        }

        fun render(): String {
            val shouldStrike = struck || isCompletion
            return if (shouldStrike) "<str>$text</str>" else text
        }

        class ConditionHandle internal constructor(
            private val context: LineContext,
            private val applied: Boolean,
        ) {
            fun strike(colour: String = DEFAULT_UNUSED_COLOUR): ConditionHandle {
                if (applied) {
                    context.struck = true
                }
                return this
            }

            fun colour(colour: String): ConditionHandle = this

            fun text(text: String): ConditionHandle {
                if (applied) {
                    context.text = text
                }
                return this
            }

            fun finalise(
                strike: Boolean = false,
                strikeColour: String = DEFAULT_UNUSED_COLOUR,
            ) {
                if (applied) {
                    context.finalise(strike)
                }
            }
        }
    }

    @QuestJournalDsl
    inner class ObjectiveContext internal constructor(
        private val lineContext: LineContext,
    ) {
        fun attribute(
            attribute: QuestAttribute<Boolean>,
            text: String,
            strike: Boolean = false,
            colour: String = DEFAULT_UNUSED_COLOUR,
            finalise: Boolean = true,
        ): LineContext.ConditionHandle =
            lineContext.attribute(attribute, text, strike, colour, finalise)

        fun hasItem(
            item: String,
            text: String,
            strike: Boolean = false,
            colour: String = DEFAULT_UNUSED_COLOUR,
            finalise: Boolean = false,
        ): LineContext.ConditionHandle =
            lineContext.hasItem(item, text, strike, colour, finalise)

        fun stageAtLeast(
            stage: Int,
            text: String,
            strike: Boolean = false,
            colour: String = DEFAULT_UNUSED_COLOUR,
            finalise: Boolean = false,
        ): LineContext.ConditionHandle =
            lineContext.stageAtLeast(stage, text, strike, colour, finalise)

        fun questState(
            state: QuestProgressState,
            text: String,
            strike: Boolean = false,
            colour: String = DEFAULT_UNUSED_COLOUR,
            finalise: Boolean = false,
        ): LineContext.ConditionHandle =
            lineContext.questState(state, text, strike, colour, finalise)

        fun custom(
            condition: Boolean,
            text: String,
            strike: Boolean = false,
            colour: String = DEFAULT_UNUSED_COLOUR,
            finalise: Boolean = false,
        ): LineContext.ConditionHandle =
            lineContext.custom(condition, text, strike, colour, finalise)

        fun finalise(
            strike: Boolean = false,
            strikeColour: String = DEFAULT_UNUSED_COLOUR,
        ) {
            lineContext.finalise(strike)
        }

        fun render(): String = lineContext.render()
    }

    companion object {
        private const val DEFAULT_UNUSED_COLOUR = ""
    }
}
