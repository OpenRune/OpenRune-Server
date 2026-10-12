package org.rsmod.content.areas.city.varlamore.sunsetcoast.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.content.quest.manager.QuestRequirements

internal const val FREMENNIK_TRIALS = "quest_fremenniktrials"

internal val Dialogue.isFremennik: Boolean
    get() = QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)

// TODO(content): [system:fremennik_name] the player's Fremennik name from The Fremennik Trials;
// until then this placeholder fills the [Fremennik name] slot
internal fun fremennikName(): String = "outerlander"
