package org.rsmod.content.areas.misc.stronghold_of_security

internal object StrongholdNotesBook {
    const val Title = "Stronghold of Security - Notes"
    const val LinesPerPage = 15
    const val FirstChapterLine = 3

    const val PageLeft = "component.indexed_book:page_left_button"
    const val PageRight = "component.indexed_book:page_right_button"
    const val FirstPage = "component.indexed_book:index_jump_button"

    fun chapterLink(line: Int): String = "component.indexed_book:page_left_index_$line"

    val Chapters: List<Pair<String, Int>> =
        listOf(
            "Description" to 2,
            "Level 1" to 4,
            "Level 2" to 6,
            "Level 3" to 7,
            "Level 4" to 9,
            "Navigation" to 11,
            "Diary" to 13,
        )

    val Pages: List<List<String>> =
        listOf(
            listOf("Chapters"),
            listOf(
                heading("Description"),
                "This stronghold was",
                "unearthed by a miner",
                "prospecting for new ores",
                "around the Barbarian Village.",
                "After gathering some",
                "equipment he ventured into",
                "the maze of tunnels and was",
                "missing for a long time. He",
                "finally emerged along with",
                "copious notes regarding the",
                "new beasts and strange",
                "experiences which had befallen",
                "him. He also mentioned that",
                "there was treasure to be had,",
            ),
            listOf(
                "but no one has been able to",
                "wring a word from him about",
                "this, he simply flapped his",
                "arms and slapped his head.",
                "This book details his notes",
                "and my diary of exploration.",
                "I am exploring to see if I",
                "can find out more...",
            ),
            listOf(
                heading("Level 1"),
                "As well as goblins, creatures",
                "like a man but also like a cow",
                "infest this place! I have never",
                "seen anything like this before.",
                "The area itself is reminiscent",
                "of frontline castles, with",
                "many walls, doors and",
                "skeletons of dead enemies.",
                "I'm sure I hear voices in my",
                "head each time I pass",
                "through the gates. I have",
                "dubbed this level War as it",
                "seems like an eternal",
                "battleground. I found only",
            ),
            listOf("one small peaceful area here."),
            listOf(
                heading("Level 2"),
                "My supplies are running low",
                "and I find myself in barren",
                "passages with seemingly",
                "endless malnourished beasts",
                "attacking me, ravenous for",
                "food. Nothing appears to be",
                "able to grow, many",
                "adventurers have died",
                "through lack of food and the",
                "very air appears to suck",
                "vitality from me. I've come to",
                "call this place famine.",
            ),
            listOf(
                heading("Level 3"),
                "Just breathing in this place",
                "makes me shudder at the",
                "thought of what foul disease I",
                "may contract. The walls and",
                "floor ooze and pulsate like",
                "something pox ridden. There",
                "is a very strange beast whom",
                "I narrowly escaped from. At",
                "first I thought it to be a",
                "cross between a cow and a",
                "sheep, something",
                "domesticated... but when it",
                "looked up at me I was",
                "overcome with weakness and",
            ),
            listOf(
                "barely got away with my life!",
                "Luckily I found a small place",
                "where I could heal myself",
                "and rest a while. I have",
                "named this area pestilence for",
                "it reeks with decay.",
            ),
            listOf(
                heading("Level 4"),
                "On my first escapade into",
                "this place I was utterly",
                "shocked. The adventurers",
                "who had come before me",
                "must have made up a tiny",
                "proportion of the skeletons of",
                "the dead. Nothing truly alive",
                "exists here, even those beings",
                "who do wander the halls are",
                "not alive as such, but they do",
                "know that I am and I get",
                "the distinct impression that",
                "were they to have their way,",
                "I would not be for long!",
            ),
            listOf(
                "Death is everywhere and",
                "thus I shall name this place.",
                "There is one small place of",
                "life, which was gladdening to",
                "find and very worth my",
                "while!",
            ),
            listOf(
                heading("Navigation"),
                "After getting lost several",
                "times I finally worked out the",
                "key to all the ladders and",
                "chains around this death",
                "infested place. All ropes and",
                "chains will take you to the",
                "start of the level that you are",
                "on. However most ladders will",
                "simply take you to the level",
                "above. The one exception is",
                "the ladder in the bottom level",
                "treasure room, which appears",
                "to lead through several",
                "extremely twisty passages",
            ),
            listOf(
                "and eventually takes you out",
                "of the dungeon completely.",
                "The portals may be used if",
                "you are of sufficient level or",
                "have already claimed your",
                "reward from the treasure",
                "room.",
            ),
            listOf(
                heading("Diary"),
                "Day 1",
                "Today I set out to find out",
                "more about this place. From",
                "my research I knew about",
                "the sentient doors, imbued by",
                "some unknown force to talk",
                "to you and ask questions",
                "before they will let you pass.",
                "I  have so far passed these",
                "doors without incident, giving",
                "the correct answer seems to",
                "work a treat.",
                "",
                "Day 2",
            ),
            listOf(
                "I have fought my way",
                "through the fearsome beasts",
                "on the first level and am",
                "preparing myself to journey",
                "deeper. I hope that things are",
                "not too difficult further on as",
                "I am already sick of bread",
                "and cheese for dinner.",
                "",
                "Day 3",
                "I ventured down into the",
                "famine level today... I was",
                "wounded and have returned",
                "to the relative safety of the",
                "level above. I am going to",
            ),
            listOf(
                "try to make my way out",
                "through the goblins and",
                "mancow things... I hope I",
                "make it.....",
            ),
            emptyList(),
        )

    val lastSpread: Int
        get() = Pages.lastIndex / 2

    fun spread(index: Int): Pair<List<String>, List<String>> =
        Pages[index * 2] to Pages.getOrElse(index * 2 + 1) { emptyList() }

    fun spreadOf(page: Int): Int = (page - 1) / 2

    fun turn(spread: Int, pressed: String): Int {
        val target =
            when (pressed) {
                PageLeft -> spread - 1
                PageRight -> spread + 1
                FirstPage -> 0
                else -> chapterPage(pressed)?.let(::spreadOf) ?: spread
            }
        return target.coerceIn(0, lastSpread)
    }

    private fun chapterPage(pressed: String): Int? =
        Chapters.withIndex()
            .firstOrNull { (i, _) -> chapterLink(FirstChapterLine + i) == pressed }
            ?.value
            ?.second

    private fun heading(text: String): String = "<col=000080>$text</col>"
}
