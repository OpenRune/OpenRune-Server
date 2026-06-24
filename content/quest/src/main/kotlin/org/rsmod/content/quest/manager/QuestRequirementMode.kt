package org.rsmod.content.quest.manager

public enum class QuestRequirementMode(public val configValue: String) {
    RespectProgress("respect-progress"),
    AssumeCompleted("assume-completed"),
    VirtualCompletions("virtual-completions"),
    ;

    public companion object {
        public fun parse(value: String): QuestRequirementMode {
            val normalized = value.trim().lowercase().replace('_', '-')
            return entries.firstOrNull { it.configValue == normalized || it.name.lowercase() == normalized }
                ?: error(
                    "Invalid quest requirement mode '$value'. " +
                        "Expected one of: ${entries.joinToString { it.configValue }}.",
                )
        }
    }
}

