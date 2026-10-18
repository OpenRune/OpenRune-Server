package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

internal var Player.heardOfTreasure by boolVarBit("varbit.waterfall_heard_of_treasure")
internal var Player.metGolrie by boolVarBit("varbit.waterfall_met_golrie")
internal var Player.pillarRunes by intVarBit("varbit.waterfall_pillar_runes")

@Singleton
class WaterfallQuest @Inject constructor() :
    QuestScript(
        QUEST_KEY,
        "varp.waterfall_quest",
        rewards {
            xp("stat.strength", COMBAT_XP)
            xp("stat.attack", COMBAT_XP)
            item("obj.diamond", 2, "2 Diamonds")
            item("obj.gold_bar", 2, "2 Gold Bars")
            item("obj.mithril_seed", 40, "40 Mithril Seeds")
        },
        ItemRewardDisplay(URN_FULL),
        questVarbit = "varbit.waterfall_progress",
    ) {

    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE) {
            "Waterfall Quest end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at $COMPLETE."
        }
    }

    fun stage(player: Player): Int = quest.getQuestStage(player)

    fun isComplete(player: Player): Boolean = quest.isQuestCompleted(player)

    fun advanceTo(access: ProtectedAccess, stage: Int) {
        val current = stage(access.player)
        if (current > 0 && stage > current) {
            quest.setQuestStage(access, stage)
        }
    }

    fun allRunesPlaced(player: Player): Boolean = player.pillarRunes == ALL_PILLAR_RUNES

    override fun subTitle(): String =
        "talking to <col=800000>Almera</col> in her house on top of <col=800000>Baxtorian " +
            "Falls</col>, south of the <col=800000>Barbarian Outpost</col>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            val stage = stage(access.player)
            if (stage == STARTED) {
                line(
                    "I spoke to <red>Almera</red> in her home next to <red>Baxtorian Falls</red>. " +
                        "She asked me to check on her son, <red>Hudon</red>, who has been " +
                        "searching for treasure near <red>Baxtorian Falls</red>. I can use the " +
                        "raft behind <red>Almera's</red> house to get down the river."
                )
                return@questJournal
            }
            strike(SPOKE_TO_ALMERA)
            if (stage == MET_HUDON) {
                line(
                    "I found <red>Hudon</red> down the river from <red>Almera's</red> house. He " +
                        "seems to be convinced that there's treasure nearby. Maybe there's " +
                        "somewhere I can learn more about this supposed treasure."
                )
                return@questJournal
            }
            strike(FOUND_HUDON)
            if (stage == READ_BOOK) {
                val pebble =
                    if (access.player.metGolrie) {
                        " I recovered <red>Glarial's Pebble</red>, the key to her tomb, from " +
                            "beneath <red>Tree Gnome Village</red>. I should be able to use it " +
                            "to enter her tomb."
                    } else {
                        " Apparently, <red>Glarial's Pebble</red>, the key to her tomb, was " +
                            "taken by gnomes and hidden beneath <red>Tree Gnome Village</red>."
                    }
                line(
                    "I found a book in the nearby <red>Tourist Office</red>. The book detailed " +
                        "the sad story of <red>Baxtorian</red> and <red>Glarial</red>, two elves " +
                        "that once ruled the area around <red>Baxtorian Falls</red>. According " +
                        "to the book, <red>Baxtorian</red> and <red>Glarial</red> were the only " +
                        "ones able to enter the waterfall but <red>Baxtorian</red> sealed himself " +
                        "inside after <red>Glarial's</red> apparent death. The book also " +
                        "mentioned that <red>Glarial</red> herself is buried near the waterfall." +
                        pebble
                )
                return@questJournal
            }
            strike(READ_THE_BOOK)
            if (stage == ENTERED_TOMB) {
                line(
                    "I recovered <red>Glarial's Pebble</red> from beneath <red>Tree Gnome " +
                        "Village</red> and used it to enter her tomb. I should see if there's " +
                        "anything in the tomb that can help me."
                )
                return@questJournal
            }
            strike(ENTERED_THE_TOMB)
            if (stage == ENTERED_FALLS) {
                line(
                    "I found <red>Glarial's Amulet</red> within her tomb and used it to enter " +
                        "<red>Baxtorian Falls</red>. I should look around and see if I can find " +
                        "the treasure."
                )
                return@questJournal
            }
            strike(ENTERED_THE_FALLS)
            if (stage == FOUND_CHALICE) {
                line(
                    "I found a <red>Chalice</red> within <red>Baxtorian Falls</red> which looks " +
                        "like it contains <red>Baxtorian's Treasure</red>. However, it is " +
                        "floating out of reach. I'll need to work out how to get to it."
                )
                return@questJournal
            }
            line(
                "I found a <red>Chalice</red> within <red>Baxtorian Falls</red> which looks like " +
                    "it contains <red>Baxtorian's Treasure</red>. The <red>Chalice</red> was " +
                    "floating out of reach but I worked out how to raise the floor to get to it. " +
                    "Now I just need to retrieve the treasure from it."
            )
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(SPOKE_TO_ALMERA)
            line(FOUND_HUDON)
            line(READ_THE_BOOK)
            line(ENTERED_THE_TOMB)
            line(ENTERED_THE_FALLS)
            line(
                "I found a Chalice within Baxtorian Falls which contained Baxtorian's Treasure. " +
                    "The Chalice was floating out of reach but I worked out how to raise the " +
                    "floor to get to it. I then managed to obtain the treasure by swapping it " +
                    "for Glarial's Ashes."
            )
        }

    companion object {
        const val QUEST_KEY = "quest_waterfall"

        const val STARTED = 1
        const val MET_HUDON = 2
        const val READ_BOOK = 3
        const val ENTERED_TOMB = 4
        const val ENTERED_FALLS = 5
        const val FOUND_CHALICE = 6
        const val FLOOR_RISEN = 8
        const val COMPLETE = 10

        const val COMBAT_XP = 13750.0
        const val RECOMMENDED_COMBAT = 25
        const val PILLAR_COUNT = 6
        const val RUNES_PER_PILLAR = 3
        const val ALL_PILLAR_RUNES = (1 shl (PILLAR_COUNT * RUNES_PER_PILLAR)) - 1

        const val BOOK = "obj.baxtorian_book_waterfall_quest"
        const val GOLRIE_KEY = "obj.golrie_key_waterfall_quest"
        const val PEBBLE = "obj.glarials_pebble_waterfall_quest"
        const val AMULET = "obj.glarials_amulet_waterfall_quest"
        const val URN_FULL = "obj.glarials_urn_full_waterfall_quest"
        const val URN_EMPTY = "obj.glarials_urn_empty_waterfall_quest"
        const val BAXTORIAN_KEY = "obj.baxtorian_key_waterfall_quest"
        const val ROPE = "obj.rope"

        const val ALMERA_NPC = "npc.almera_waterfall_quest"
        const val HUDON_NPC = "npc.hudon_waterfall_quest"
        const val GERALD_NPC = "npc.gerald_waterfall_quest"
        const val HADLEY_NPC = "npc.hadley_waterfall_quest"
        const val GOLRIE_NPC = "npc.golrie_waterfall_quest"

        private const val SPOKE_TO_ALMERA =
            "I spoke to Almera in her home next to Baxtorian Falls. She asked me to check on " +
                "her son, Hudon, who has been searching for treasure near Baxtorian Falls."
        private const val FOUND_HUDON =
            "I found Hudon down the river from Almera's house. He was convinced that there was " +
                "some treasure hidden nearby."
        private const val READ_THE_BOOK =
            "I found a book in the nearby tourist office. The book detailed the sad story of " +
                "Baxtorian and Glarial, two elves that once ruled the area around Baxtorian " +
                "Falls. According to the book, Baxtorian and Glarial were the only ones able to " +
                "enter the waterfall but Baxtorian sealed himself inside after Glarial's apparent " +
                "death. The book also mentioned that Glarial herself is buried near the " +
                "waterfall, and that the key to her tomb, Glarial's Pebble, was taken by gnomes " +
                "and hidden beneath Tree Gnome Village."
        private const val ENTERED_THE_TOMB =
            "I recovered Glarial's Pebble from beneath Tree Gnome Village and used it to enter " +
                "her tomb."
        private const val ENTERED_THE_FALLS =
            "I found Glarial's Amulet within her tomb and used it to enter Baxtorian Falls."
    }
}
