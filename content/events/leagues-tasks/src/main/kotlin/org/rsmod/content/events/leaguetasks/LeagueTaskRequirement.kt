package org.rsmod.content.events.leaguetasks

import dev.openrune.definition.type.VarBitType
import dev.openrune.types.varp.VarpServerType
import org.rsmod.api.table.ActionRow

/** One thing a task needs; a task completes once all of its requirements are met. */
public class LeagueTaskRequirement(
    public val task: Int,
    public val trigger: LeagueTaskTrigger,
    /** Where the requirement came from, for error messages. */
    public val source: String,
    public val stats: List<String> = emptyList(),
    public val value: Int = 0,
    public val count: Int = 1,
    public val objs: List<Int> = emptyList(),
    public val npcs: List<Int> = emptyList(),
    public val categories: List<Int> = emptyList(),
    public val slayerTask: Int? = null,
    public val counter: VarBitType? = null,
    /** Where an event must happen; empty means anywhere. */
    public val areas: List<Int> = emptyList(),
    /** Tools the action must be done with; empty means any. */
    public val tools: List<Int> = emptyList(),
    public val varp: VarpServerType? = null,
    public val varbit: VarBitType? = null,
    public val interf: Int? = null,
    public val locs: List<Int> = emptyList(),
    /** The interaction option slot, 1 to 5; 0 means any. */
    public val option: Int = 0,
    /** The emote tab name, as `PlayerEmoteEvent.emote` reports it. */
    public val emote: String? = null,
    /** `dbtable.collection_log_categories` rows; empty means the whole log. */
    public val collections: List<Int> = emptyList(),
) {
    public val baseVarp: Int?
        get() = varbit?.varp ?: varp?.id

    internal companion object {
        /** A requirement read from what Jagex filled in on the task's `action` row, if anything. */
        fun fromAction(task: Int): LeagueTaskRequirement? {
            val action = ActionRow.getRow(task)
            val source = "action row $task"
            if (action.equipItem.isNotEmpty()) {
                val objs = action.equipItem.map { it.id }
                return LeagueTaskRequirement(task, LeagueTaskTrigger.Equip, source, objs = objs)
            }
            val slayerTask = action.npcKillSlayerCategory
            val namesNpcs = action.npcKill.isNotEmpty() || action.npcKillCategory.isNotEmpty()
            if (namesNpcs || slayerTask != null) {
                return LeagueTaskRequirement(
                    task,
                    LeagueTaskTrigger.Kill,
                    source,
                    npcs = action.npcKill.map { it.id },
                    categories = action.npcKillCategory,
                    slayerTask = slayerTask,
                )
            }
            return null
        }
    }
}
