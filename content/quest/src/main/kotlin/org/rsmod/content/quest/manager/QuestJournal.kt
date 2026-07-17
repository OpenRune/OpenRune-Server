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

enum class PreserveObjectivePlacement {
    /**
     * Preserved condition text appears above the original objective text.
     * The preserved line is only struck when [LineContext.ConditionHandle.strike] is used.
     */
    Before,

    /**
     * Preserved condition text appears below the original objective text.
     * The original objective is struck by default because it is the completed step.
     */
    After,
}

@QuestJournalDsl
class QuestJournalBuilder internal constructor(
    val access: ProtectedAccess,
    val quest: Quest,
    private val isCompletion: Boolean = false,
) {
    private val lines = mutableListOf<String>()

    fun description(
        text: String,
        colour: String = DEFAULT_UNUSED_COLOUR,
        builder: DescriptionContext.() -> Unit = {},
    ) {
        val context = DescriptionContext()
        context.builder()
        if (context.shouldHide) {
            return
        }
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
        if (!context.isVisible) {
            return
        }
        val rendered = objective.render()
        lines += rendered
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
    inner class DescriptionContext {
        val access: ProtectedAccess
            get() = this@QuestJournalBuilder.access

        val quest: Quest
            get() = this@QuestJournalBuilder.quest

        private var hidePredicate: (DescriptionContext.() -> Boolean)? = null

        /**
         * Hides this description when [predicate] returns true.
         */
        fun hideWhen(predicate: DescriptionContext.() -> Boolean) {
            hidePredicate = predicate
        }

        /** Hides once the quest has been started (in progress or completed). */
        fun hideWhenQuestStarted() {
            hideWhen {
                val state = quest.questState(access.player)
                state != QuestProgressState.NOT_STARTED
            }
        }

        /** Hides while the quest is actively in progress. */
        fun hideWhenQuestInProgress() {
            hideWhen { quest.questState(access.player) == QuestProgressState.IN_PROGRESS }
        }

        /** Hides once the quest has been completed. */
        fun hideWhenQuestCompleted() {
            hideWhen { quest.questState(access.player) == QuestProgressState.FINISHED }
        }

        internal val shouldHide: Boolean
            get() {
                if (this@QuestJournalBuilder.isCompletion) {
                    return false
                }
                return hidePredicate?.invoke(this) ?: false
            }
    }

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
        private val prefixLines = mutableListOf<JournalFragmentLine>()
        private val suffixLines = mutableListOf<JournalFragmentLine>()
        private val pendingHandles = mutableListOf<ConditionHandle>()
        private val visibilityPredicates = mutableListOf<() -> Boolean>()

        internal val isFinalised: Boolean
            get() = finalised

        internal val isVisible: Boolean
            get() {
                if (isCompletion) {
                    return true
                }
                return visibilityPredicates.all { it() }
            }

        internal fun addVisibilityPredicate(predicate: () -> Boolean) {
            visibilityPredicates += predicate
        }

        fun attribute(
            attribute: QuestAttribute<Boolean>,
            text: String,
            strike: Boolean = false,
            colour: String = DEFAULT_UNUSED_COLOUR,
            finalise: Boolean = true,
        ): ConditionHandle {
            if (finalised) {
                return registerCondition(text, strike, finalise, applied = false)
            }
            val applied = attribute.getOrNull(access.player) == true
            return registerCondition(text, strike, finalise, applied)
        }

        fun hasItem(
            item: String,
            text: String,
            strike: Boolean = false,
            colour: String = DEFAULT_UNUSED_COLOUR,
            finalise: Boolean = false,
        ): ConditionHandle {
            if (finalised) {
                return registerCondition(text, strike, finalise, applied = false)
            }
            val applied = access.inv.contains("obj.${item}")
            return registerCondition(text, strike, finalise, applied)
        }

        fun stageAtLeast(
            stage: Int,
            text: String,
            strike: Boolean = false,
            colour: String = DEFAULT_UNUSED_COLOUR,
            finalise: Boolean = false,
        ): ConditionHandle {
            if (finalised) {
                return registerCondition(text, strike, finalise, applied = false)
            }
            val applied = quest.getQuestStage(access.player) >= stage
            return registerCondition(text, strike, finalise, applied)
        }

        fun questState(
            state: QuestProgressState,
            text: String,
            strike: Boolean = false,
            colour: String = DEFAULT_UNUSED_COLOUR,
            finalise: Boolean = false,
        ): ConditionHandle {
            if (finalised) {
                return registerCondition(text, strike, finalise, applied = false)
            }
            val applied = quest.questState(access.player) == state
            return registerCondition(text, strike, finalise, applied)
        }

        fun custom(
            condition: Boolean,
            text: String,
            strike: Boolean = false,
            colour: String = DEFAULT_UNUSED_COLOUR,
            finalise: Boolean = false,
        ): ConditionHandle {
            if (finalised) {
                return registerCondition(text, strike, finalise, applied = false)
            }
            return registerCondition(text, strike, finalise, condition)
        }

        fun finalise(strike: Boolean = false, strikeColour: String = DEFAULT_UNUSED_COLOUR) {
            struck = struck || strike
            finalised = true
        }

        internal fun markFinalised() {
            finalised = true
        }

        private fun registerCondition(
            text: String,
            strike: Boolean,
            finalise: Boolean,
            applied: Boolean,
        ): ConditionHandle {
            val handle =
                ConditionHandle(
                    context = this,
                    applied = applied,
                    text = text,
                    strike = strike,
                    finalise = finalise,
                )
            pendingHandles += handle
            return handle
        }

        private fun commitHandles() {
            for (handle in pendingHandles) {
                handle.commit()
            }
        }

        private fun setLine(
            value: String,
            strike: Boolean,
        ) {
            text = value
            struck = strike
        }

        private fun addPrefix(
            value: String,
            strike: Boolean,
        ) {
            prefixLines += JournalFragmentLine(value, strike)
        }

        private fun addSuffix(
            value: String,
            strike: Boolean,
        ) {
            suffixLines += JournalFragmentLine(value, strike)
        }

        private fun strikeInitialLine() {
            struck = true
        }

        fun render(): String {
            commitHandles()
            val parts = mutableListOf<String>()
            for (prefix in prefixLines) {
                parts += formatLine(prefix.text, prefix.struck)
            }
            parts += formatLine(text, struck)
            for (suffix in suffixLines) {
                parts += formatLine(suffix.text, suffix.struck)
            }
            return parts.joinToString("\n")
        }

        private fun formatLine(
            value: String,
            strike: Boolean,
        ): String {
            val shouldStrike = strike || isCompletion
            return when {
                shouldStrike && isCompletion -> "<str>$value</str>"
                shouldStrike -> "<black><str>${stripColourTags(value)}</str></black>"
                else -> "<blue>$value</blue>"
            }
        }

        private data class JournalFragmentLine(
            val text: String,
            val struck: Boolean,
        )

        class ConditionHandle internal constructor(
            private val context: LineContext,
            private val applied: Boolean,
            private val text: String,
            private val strike: Boolean,
            private val finalise: Boolean,
        ) {
            private var preserveInitial = false
            private var preservePlacement = PreserveObjectivePlacement.After
            private var strikeObjectiveText = false
            private var overrideStrike: Boolean? = null
            private var overrideText: String? = null
            private var overrideFinalise: Boolean? = null
            private var overrideFinaliseStrike: Boolean = false

            internal fun commit() {
                if (!applied || context.finalised) {
                    return
                }
                val effectiveText = overrideText ?: text
                val effectiveStrike = overrideStrike ?: strike
                val shouldFinalise = overrideFinalise ?: finalise
                if (preserveInitial) {
                    if (strikeObjectiveText) {
                        context.strikeInitialLine()
                    }
                    when (preservePlacement) {
                        PreserveObjectivePlacement.Before -> context.addPrefix(effectiveText, effectiveStrike)
                        PreserveObjectivePlacement.After -> context.addSuffix(effectiveText, effectiveStrike)
                    }
                } else {
                    context.setLine(effectiveText, effectiveStrike)
                }
                if (shouldFinalise) {
                    if (overrideFinaliseStrike) {
                        context.finalise(strike = true)
                    } else {
                        context.markFinalised()
                    }
                }
            }

            fun strike(colour: String = DEFAULT_UNUSED_COLOUR): ConditionHandle {
                overrideStrike = true
                return this
            }

            fun colour(colour: String): ConditionHandle = this

            /**
             * Keeps the objective's initial text and adds this condition's text as a preserved
             * journal line. Use [strike] to strike only the preserved line.
             *
             * With [PreserveObjectivePlacement.After], the original objective text is struck by
             * default because it represents the completed step. With [PreserveObjectivePlacement.Before],
             * the original objective stays active and the preserved line is only struck when
             * [strike] is used.
             */
            fun preserveObjective(
                placement: PreserveObjectivePlacement = PreserveObjectivePlacement.After,
                strikeObjective: Boolean? = null,
            ): ConditionHandle {
                preserveInitial = true
                preservePlacement = placement
                strikeObjectiveText =
                    strikeObjective ?: (placement == PreserveObjectivePlacement.After)
                return this
            }

            /** Strikes the original objective text when this preserved condition applies. */
            fun strikeObjective(): ConditionHandle {
                strikeObjectiveText = true
                return this
            }

            /** Keeps the original objective text active when this preserved condition applies. */
            fun keepObjective(): ConditionHandle {
                strikeObjectiveText = false
                return this
            }

            fun text(text: String): ConditionHandle {
                overrideText = text
                return this
            }

            fun finalise(
                strike: Boolean = false,
                strikeColour: String = DEFAULT_UNUSED_COLOUR,
            ): ConditionHandle {
                overrideFinalise = true
                overrideFinaliseStrike = strike
                return this
            }
        }
    }

    @QuestJournalDsl
    inner class ObjectiveContext internal constructor(
        private val lineContext: LineContext,
    ) {
        val access: ProtectedAccess
            get() = this@QuestJournalBuilder.access

        val quest: Quest
            get() = this@QuestJournalBuilder.quest

        /**
         * Hides this objective unless [predicate] returns true.
         */
        fun visibleWhen(predicate: ObjectiveContext.() -> Boolean) {
            lineContext.addVisibilityPredicate { predicate() }
        }

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

        private val COLOUR_TAG_REGEX =
            Regex("</?(?:col(?:=[0-9A-Fa-f]{6})?|red|blue|black|green|yellow|white|purple|orange|maroon|ruby|lime|teal|cyan|str)>")

        private fun stripColourTags(text: String): String = COLOUR_TAG_REGEX.replace(text, "")
    }
}
