package org.rsmod.content.areas.city.varlamore.civitas.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Curator_Herminius
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CuratorHerminius : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.fortis_museum_curator") { startDialogue(it.npc) { curator() } }
    }

    private suspend fun Dialogue.curator() {
        chatPlayer(happy, "Hello.")
        chatNpc(
            happy,
            "Nilsal to you, iknami. Welcome to the Grand Museum. I hope your visit is a pleasant " +
                "one. If you have any questions about Varlamore, I'm happy to help.",
        )
        while (true) {
            val option =
                choice4(
                    "Could you tell me more about the early history of Varlamore?",
                    1,
                    "Could you tell me more about religion in Varlamore?",
                    2,
                    "Could you tell me more about Varlamore's recent history?",
                    3,
                    "Thanks. If I have any questions, I'll be sure to ask.",
                    4,
                )
            when (option) {
                1 -> if (!earlyHistory()) return
                2 -> religion()
                3 -> recentHistory()
                else -> {
                    chatPlayer(happy, "Thanks. If I have any questions, I'll be sure to ask.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.earlyHistory(): Boolean {
        chatPlayer(quiz, "Could you tell me more about the early history of Varlamore?")
        chatNpc(
            happy,
            "Certainly! When it comes to the existence of Varlamore, it is Xeric we have to " +
                "thank. During his Age of Strife, many refugees fled Kourend over the previously " +
                "unexplored Custodia Mountains.",
        )
        chatNpc(
            neutral,
            "There were three notable groups of these refugees. The first group, the Talkasti, " +
                "settled in the Tlati Rainforest.",
        )
        chatNpc(
            neutral,
            "While they initially lived nomadic lives, they eventually started to establish " +
                "more permanent settlements, with Tal Teklan being the first.",
        )
        chatNpc(
            neutral,
            "The next group were the Pericans. They first settled right here, building the town " +
                "of Fortis, or Civitas illa Fortis as it eventually became.",
        )
        chatNpc(
            neutral,
            "Finally we have the dwarves. These were refugees from Lovakengj who established a " +
                "home beneath Ralos' Rise, becoming known as the Dwarves of Cam Torum.",
        )
        chatPlayer(quiz, "Was there any friction between these groups initially?")
        chatNpc(
            neutral,
            "Oh yes, very much so. The dwarves, just as they do today, mostly kept to " +
                "themselves. However, the Talkasti and Pericans came into regular conflict over " +
                "various matters.",
        )
        chatPlayer(quiz, "But that's not the case now, right? What brought them together?")
        chatNpc(
            neutral,
            "As you may well have already guessed, it was religion. Long before leaving Kourend, " +
                "the Talkasti had taken to secretly worshipping Ralos and Ranul.",
        )
        chatNpc(
            neutral,
            "After arriving here, they were able to openly practice their religion for the first " +
                "time. Over time, the Pericans started to adopt this religion as well.",
        )
        chatNpc(
            neutral,
            "As the two groups came together, the population of the region started to rapidly " +
                "expand. This eventually led to the construction of Tempestus, and with it, the " +
                "founding of Varlamore.",
        )
        chatPlayer(
            quiz,
            "So are there still distinctions between the Talkasti and the Pericans now?",
        )
        chatNpc(
            neutral,
            "Yes and no. Those terms are no longer used to describe people today. Since we " +
                "united, we have all been Varlamorians. However, you'll still find many traces " +
                "of our previous divide.",
        )
        chatNpc(
            neutral,
            "For example, here in Fortis, you'll find yourself surrounded by Perican " +
                "architecture, whereas over in the Tlati Rainforest, the influence of the " +
                "Talkasti remains clear.",
        )
        chatNpc(
            neutral,
            "Names are another good example. Indeed, look no further than our rulers. For most " +
                "of our history, we were ruled by the Tullus dynasty, with Tullus very much " +
                "being a name of Perican origin.",
        )
        chatNpc(
            neutral,
            "However, we are now ruled by the Arkan dynasty, whose name can be traced back to " +
                "early Talkasti nomads.",
        )
        chatPlayer(quiz, "I see. So where did the name Varlamore come from.")
        chatNpc(
            neutral,
            "The name comes from the River Varla that separated Talkasti and Perican territory. " +
                "What was once a symbol of our divide, became the symbol of our unification.",
        )
        chatPlayer(happy, "That was all super interesting! Thanks for the information.")
        chatNpc(happy, "Not a problem, iknami!")
        val option =
            choice3(
                "Could you tell me more about religion in Varlamore?",
                2,
                "Could you tell me more about Varlamore's recent history?",
                3,
                "I'd better get going. Thanks again.",
                4,
            )
        when (option) {
            2 -> religion()
            3 -> recentHistory()
            else -> {
                chatPlayer(happy, "I'd better get going. Thanks again.")
                return false
            }
        }
        return true
    }

    private suspend fun Dialogue.religion() {
        chatPlayer(quiz, "Could you tell me more about religion in Varlamore?")
        chatNpc(
            happy,
            "Of course! Whereas in other parts of the world you'll likely find followers of many " +
                "gods, here in Varlamore, you'll only really find those dedicated to Ralos and " +
                "Ranul.",
        )
        chatNpc(
            neutral,
            "Losanium is the official term for this religion, though it is not a term you'll " +
                "find used very often.",
        )
        chatPlayer(quiz, "How come?")
        chatNpc(
            neutral,
            "Losanium encompasses the worship of both Ralos and Ranul, and while they do indeed " +
                "go hand in hand, it is rare to find individuals equally dedicated to both.",
        )
        chatNpc(
            neutral,
            "Instead, most people focus their worship on one of the two, with Ralos being by far " +
                "the more popular. Something that isn't too surprising given Ranul's association " +
                "with death.",
        )
        chatNpc(
            neutral,
            "Now that said, you might actually find a higher than normal number of Ranul " +
                "worshippers right here in the museum.",
        )
        chatNpc(
            neutral,
            "Something that is easily forgotten is that while Ranul is indeed a god of death, he " +
                "is also a god of learning and knowledge.",
        )
        chatNpc(
            neutral,
            "If you wish to learn more, you may well want to seek out one of the teoki in the " +
                "temple to the south. Though of course, they may well have somewhat biased takes " +
                "on their religious practices.",
        )
        chatPlayer(happy, "I'll keep that in mind. Thanks.")
        chatNpc(happy, "Don't mention it.")
    }

    private suspend fun Dialogue.recentHistory() {
        chatPlayer(quiz, "Could you tell me more about Varlamore's recent history?")
        chatNpc(
            happy,
            "I can indeed! When it comes to recent events in the kingdom, the best place to " +
                "start is with the War of Betrayal. It all started around 100 years ago when " +
                "Imafore Tullus ascended to the throne.",
        )
        chatNpc(
            neutral,
            "He soon reorganised the kingdom into the Varlamorian Empire, a controversial choice " +
                "to say the least, though it soon became clear that controversial choices were " +
                "quite popular with Imafore.",
        )
        chatNpc(
            neutral,
            "While public opinion was always mixed, nothing he did resulted in outright conflict " +
                "until his sudden and brutal murder of the Kourend Royal Family. War was quickly " +
                "declared.",
        )
        chatNpc(
            neutral,
            "Until that day, Varlamore had successfully managed to avoid conflict since before " +
                "the kingdom was officially established. To say the war was unpopular would be " +
                "an understatement.",
        )
        chatNpc(
            neutral,
            "Imafore soon found himself facing as much resistance from within as he did from " +
                "Kourend. The end result was predictable.",
        )
        chatNpc(
            neutral,
            "Imafore and his forces found themselves trapped between two armies at the Battle of " +
                "the Proudspire. He was captured and executed, bringing an end to the Tullus " +
                "dynasty.",
        )
        chatNpc(
            happy,
            "This brings us to a fun little fact. After his death, Imafore was succeeded by King " +
                "Mezpah Arkan. One of his first acts was to call for the beheading of every " +
                "statue of Imafore in the kingdom.",
        )
        chatNpc(
            neutral,
            "The statues remain to this day. It's actually a crime to try and remove them. They " +
                "exist as a reminder of our history, and a warning to any who would try to " +
                "follow in Imafore's footsteps.",
        )
        chatPlayer(happy, "I'll have to keep an eye out for them.")
        chatNpc(happy, "Indeed. I often like to challenge people to try and find them all!")
        chatPlayer(quiz, "So how has the kingdom changed since Imafore died?")
        chatNpc(
            neutral,
            "The War of Betrayal was very much seen as a wake- up call. While Varlamore had once " +
                "flourished under the traditions of the Tullus dynasty, by the end, it's fair to " +
                "say we were stagnating.",
        )
        chatNpc(
            neutral,
            "King Mezpah set to work on transforming the kingdom. His ambitions were high, to " +
                "the point that much of what he desired never came to pass in his lifetime.",
        )
        chatNpc(
            happy,
            "One of his most notable goals was to see Varlamore open more widely to the outer " +
                "world. A goal which thanks to his great-granddaughter, has now finally been " +
                "achieved!",
        )
        chatPlayer(
            happy,
            "Well then I should be very thankful. Without him, I might never have been allowed " +
                "to visit!",
        )
        chatNpc(
            laugh,
            "Indeed. We would often send our students on trips to the east prior to the opening " +
                "of the borders. The amount of paperwork needed was astounding! Now we can just " +
                "hop on a ship any time we like!",
        )
        chatPlayer(happy, "That definitely sounds much more fun. Thanks for all the information.")
        chatNpc(happy, "It's not a problem, iknami.")
    }
}
