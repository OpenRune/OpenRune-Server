package org.rsmod.content.skills.fishing.scripts

/** Texts the fish barrel shows, from the OSRS wiki's Fish barrel page and its item destroy notes. */
internal object FishBarrelMessages {
    const val CHECK_EMPTY: String = "The barrel is empty."
    const val CHECK_HEADER: String = "The barrel contains:"
    const val FULL: String = "The barrel is full. It may be emptied at a bank."
    const val DESTROY_EMPTY: String = "You may win another barrel from Tempoross."

    /**
     * Fish per message box. The chatbox message box holds two pages of text; twelve entries such
     * as `1 x Raw slimy swamp eel` stay within them, so more fish types continue in another box.
     */
    const val ENTRIES_PER_BOX: Int = 10

    /** The message box texts for Check: `The barrel contains:` then `<count> x <name>` entries. */
    fun checkTexts(entries: List<Pair<String, Int>>): List<String> {
        if (entries.isEmpty()) {
            return listOf(CHECK_EMPTY)
        }
        return entries.chunked(ENTRIES_PER_BOX).mapIndexed { index, chunk ->
            val list = chunk.joinToString(", ") { (name, count) -> "$count x $name" }
            if (index == 0) "$CHECK_HEADER<br>$list" else list
        }
    }
}
