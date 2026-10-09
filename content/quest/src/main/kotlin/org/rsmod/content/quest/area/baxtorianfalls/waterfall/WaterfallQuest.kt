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
            xp("stat.attack", COMBAT_XP)
            xp("stat.strength", COMBAT_XP)
            item("obj.diamond", 2)
            item("obj.gold_bar", 2)
            item("obj.mithril_seed", 40)
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
            objective(
                "<red>Almera</red> is worried about her son <red>Hudon</red>, who has gone " +
                    "treasure hunting on the river. She said I could take the " +
                    "<red>log raft</red> behind her house to find him."
            ) {
                visibleWhen { stage(access.player) == STARTED }
            }

            objective(
                "I found Hudon on an island in the river, but he would not come home. He " +
                    "thinks there is treasure hidden in the falls. <red>Hadley</red>, the " +
                    "tourist guide south of the falls, might know more about it."
            ) {
                visibleWhen { stage(access.player) == MET_HUDON }
                hasItem(
                        BOOK.removePrefix(OBJ_PREFIX),
                        "I found a book on Baxtorian in the tourist centre.",
                    )
                    .strike()
            }

            objective(
                "The book says <red>Glarial's pebble</red> opens her tomb, and that a gnome " +
                    "family living beneath the <red>Tree Gnome Village</red> may still have it."
            ) {
                visibleWhen { stage(access.player) == READ_BOOK }
                hasItem(PEBBLE.removePrefix(OBJ_PREFIX), "I have Glarial's pebble.").strike()
            }

            objective(
                "I should use the pebble on <red>Glarial's Tombstone</red> north-west of the " +
                    "Fishing Guild. Only visitors with peaceful intent may enter, so I must " +
                    "leave weapons, armour and runes behind."
            ) {
                visibleWhen {
                    stage(access.player) == READ_BOOK && access.player.inv.contains(PEBBLE)
                }
            }

            objective(
                "Inside Glarial's tomb I need to find her <red>amulet</red> and her <red>urn</red>."
            ) {
                visibleWhen { stage(access.player) == ENTERED_TOMB }
                custom(access.player.hasAmulet(), "I have Glarial's amulet.").strike()
                hasItem(URN_FULL.removePrefix(OBJ_PREFIX), "I have Glarial's urn.").strike()
            }

            objective(
                "With Glarial's amulet I should be able to enter the waterfall. The book says " +
                    "Baxtorian used <red>air, water and earth runes</red> to command nature, " +
                    "so I should bring six of each, and a <red>rope</red> to reach the ledge."
            ) {
                visibleWhen {
                    stage(access.player) == ENTERED_TOMB &&
                        access.player.hasAmulet() &&
                        access.player.inv.contains(URN_FULL)
                }
            }

            objective(
                "I am inside the waterfall. A key somewhere in these caves should open the way " +
                    "to Baxtorian's tomb."
            ) {
                visibleWhen { stage(access.player) == ENTERED_FALLS }
                hasItem(BAXTORIAN_KEY.removePrefix(OBJ_PREFIX), "I have a key from the caves.")
                    .strike()
            }

            objective(
                "Six small pillars stand before the statues of Baxtorian and Glarial. Each " +
                    "has a dent shaped for a rune."
            ) {
                visibleWhen { stage(access.player) == ENTERED_FALLS }
                custom(
                        allRunesPlaced(access.player),
                        "Every pillar holds an air, water and earth rune.",
                    )
                    .strike()
            }

            objective(
                "The pillars are charged. I should place <red>Glarial's amulet</red> on her statue."
            ) {
                visibleWhen { stage(access.player) == RUNES_PLACED }
            }

            objective(
                "The floor rose up to the <red>chalice</red>. I should lay Glarial to rest " +
                    "beside Baxtorian by pouring her ashes into it."
            ) {
                visibleWhen { stage(access.player) == FLOOR_RISEN }
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(
                "Almera asked me to look for her son Hudon, who was hunting for treasure on the " +
                    "river. He would not come home, but he set me on the trail of the elf king " +
                    "Baxtorian, who sealed himself beneath the falls."
            )
            line(
                "A gnome called Golrie gave me Glarial's pebble. With it I entered her tomb and " +
                    "took her amulet and the urn holding her ashes."
            )
            line(
                "Inside the waterfall I charged the six pillars with runes, placed the amulet on " +
                    "Glarial's statue and poured her ashes into the chalice of eternity, uniting " +
                    "her with Baxtorian. I kept the treasure the chalice held."
            )
        }

    companion object {
        const val QUEST_KEY = "quest_waterfall"

        // Stage values follow the client quest helper, hence the gaps.
        const val STARTED = 1
        const val MET_HUDON = 2
        const val READ_BOOK = 3
        const val ENTERED_TOMB = 4
        const val ENTERED_FALLS = 5
        const val RUNES_PLACED = 6
        const val FLOOR_RISEN = 8
        const val COMPLETE = 10

        const val COMBAT_XP = 13750.0
        const val RECOMMENDED_COMBAT = 25
        const val PILLAR_COUNT = 6
        const val RUNES_PER_PILLAR = 3
        const val ALL_PILLAR_RUNES = (1 shl (PILLAR_COUNT * RUNES_PER_PILLAR)) - 1

        const val OBJ_PREFIX = "obj."
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
    }
}
