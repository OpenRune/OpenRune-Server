package org.rsmod.content.quest.manager

sealed interface QuestClose {
    val delayCycles: Int?

    object Never : QuestClose {
        override val delayCycles: Int? = null
    }

    class OnFinished(override val delayCycles: Int) : QuestClose {
        companion object : QuestClose {
            override val delayCycles: Int = 0
        }
    }
}
