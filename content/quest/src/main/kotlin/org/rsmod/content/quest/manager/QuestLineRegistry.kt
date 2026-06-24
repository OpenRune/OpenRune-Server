package org.rsmod.content.quest.manager

public object QuestLineRegistry {
    private val questsByLine: MutableMap<String, MutableSet<String>> = hashMapOf()

    public fun register(line: String, quests: Iterable<String>) {
        val key = line.normalizedQuestKey()
        val entries = questsByLine.getOrPut(key) { hashSetOf() }
        quests.mapTo(entries) { it.normalizedQuestKey() }
    }

    public fun register(line: String, vararg quests: String): Unit = register(line, quests.asIterable())

    public fun contains(line: String, quest: String): Boolean {
        return quest.normalizedQuestKey() in questsByLine[line.normalizedQuestKey()].orEmpty()
    }

    public fun quests(line: String): Set<String> = questsByLine[line.normalizedQuestKey()].orEmpty()
}

