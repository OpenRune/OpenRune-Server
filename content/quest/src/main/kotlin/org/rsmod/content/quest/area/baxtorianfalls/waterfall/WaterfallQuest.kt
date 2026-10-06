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

/**
 * The stage lives in `varp.waterfall_quest` through the server-only progress varbit. The values
 * follow the stages the client's quest helper expects: 1 once Almera asks for help, 2 after
 * meeting Hudon, 3 once the book on Baxtorian has been read, 4 on entering Glarial's tomb, 5 on
 * entering the waterfall, 6 when all six pillars hold their runes and 8 once the floor has risen
 * to the chalice. Nothing in the cache transforms on the stage, so the gaps are harmless.
 *
 * The side flags (Gerald's rumour, Golrie's pebble, the runes on each pillar) are server-only
 * varbits on `varp.waterfall_state`.
 */
@Singleton
class WaterfallQuest @Inject constructor() :
    QuestScript(
        QuestKey,
        "varp.waterfall_quest",
        rewards {
            xp("stat.attack", CombatXp)
            xp("stat.strength", CombatXp)
            item("obj.diamond", 2)
            item("obj.gold_bar", 2)
            item("obj.mithril_seed", 40)
        },
        ItemRewardDisplay(UrnFull),
        questVarbit = "varbit.waterfall_progress",
    ) {

    override fun ScriptContext.init() {
        check(quest.maxSteps == Complete) {
            "Waterfall Quest end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at $Complete."
        }
    }

    fun stage(player: Player): Int = quest.getQuestStage(player)

    fun isComplete(player: Player): Boolean = quest.isQuestCompleted(player)

    /** Only moves a started quest on: walking into the tomb or the falls never starts it. */
    fun advanceTo(access: ProtectedAccess, stage: Int) {
        val current = stage(access.player)
        if (current > 0 && stage > current) {
            quest.setQuestStage(access, stage)
        }
    }

    fun allRunesPlaced(player: Player): Boolean = player.pillarRunes == AllPillarRunes

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
                visibleWhen { stage(access.player) == Started }
            }

            objective(
                "I found Hudon on an island in the river, but he would not come home. He " +
                    "thinks there is treasure hidden in the falls. <red>Hadley</red>, the " +
                    "tourist guide south of the falls, might know more about it."
            ) {
                visibleWhen { stage(access.player) == MetHudon }
                hasItem(
                        Book.removePrefix(ObjPrefix),
                        "I found a book on Baxtorian in the tourist centre.",
                    )
                    .strike()
            }

            objective(
                "The book says <red>Glarial's pebble</red> opens her tomb, and that a gnome " +
                    "family living beneath the <red>Tree Gnome Village</red> may still have it."
            ) {
                visibleWhen { stage(access.player) == ReadBook }
                hasItem(Pebble.removePrefix(ObjPrefix), "I have Glarial's pebble.").strike()
            }

            objective(
                "I should use the pebble on <red>Glarial's Tombstone</red> north-west of the " +
                    "Fishing Guild. Only visitors with peaceful intent may enter, so I must " +
                    "leave weapons, armour and runes behind."
            ) {
                visibleWhen {
                    stage(access.player) == ReadBook && access.player.inv.contains(Pebble)
                }
            }

            objective(
                "Inside Glarial's tomb I need to find her <red>amulet</red> and her <red>urn</red>."
            ) {
                visibleWhen { stage(access.player) == EnteredTomb }
                custom(access.player.hasAmulet(), "I have Glarial's amulet.").strike()
                hasItem(UrnFull.removePrefix(ObjPrefix), "I have Glarial's urn.").strike()
            }

            objective(
                "With Glarial's amulet I should be able to enter the waterfall. The book says " +
                    "Baxtorian used <red>air, water and earth runes</red> to command nature, " +
                    "so I should bring six of each, and a <red>rope</red> to reach the ledge."
            ) {
                visibleWhen {
                    stage(access.player) == EnteredTomb &&
                        access.player.hasAmulet() &&
                        access.player.inv.contains(UrnFull)
                }
            }

            objective(
                "I am inside the waterfall. A key somewhere in these caves should open the way " +
                    "to Baxtorian's tomb."
            ) {
                visibleWhen { stage(access.player) == EnteredFalls }
                hasItem(BaxtorianKey.removePrefix(ObjPrefix), "I have a key from the caves.")
                    .strike()
            }

            objective(
                "Six small pillars stand before the statues of Baxtorian and Glarial. Each " +
                    "has a dent shaped for a rune."
            ) {
                visibleWhen { stage(access.player) == EnteredFalls }
                custom(
                        allRunesPlaced(access.player),
                        "Every pillar holds an air, water and earth rune.",
                    )
                    .strike()
            }

            objective(
                "The pillars are charged. I should place <red>Glarial's amulet</red> on her statue."
            ) {
                visibleWhen { stage(access.player) == RunesPlaced }
            }

            objective(
                "The floor rose up to the <red>chalice</red>. I should lay Glarial to rest " +
                    "beside Baxtorian by pouring her ashes into it."
            ) {
                visibleWhen { stage(access.player) == FloorRisen }
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
        const val QuestKey = "quest_waterfall"

        const val Started = 1
        const val MetHudon = 2
        const val ReadBook = 3
        const val EnteredTomb = 4
        const val EnteredFalls = 5
        const val RunesPlaced = 6
        const val FloorRisen = 8
        const val Complete = 10

        const val CombatXp = 13750.0
        const val RecommendedCombat = 25
        const val PillarCount = 6
        const val RunesPerPillar = 3
        const val AllPillarRunes = (1 shl (PillarCount * RunesPerPillar)) - 1

        const val ObjPrefix = "obj."
        const val Book = "obj.baxtorian_book_waterfall_quest"
        const val GolrieKey = "obj.golrie_key_waterfall_quest"
        const val Pebble = "obj.glarials_pebble_waterfall_quest"
        const val Amulet = "obj.glarials_amulet_waterfall_quest"
        const val UrnFull = "obj.glarials_urn_full_waterfall_quest"
        const val UrnEmpty = "obj.glarials_urn_empty_waterfall_quest"
        const val BaxtorianKey = "obj.baxtorian_key_waterfall_quest"
        const val Rope = "obj.rope"

        const val AlmeraNpc = "npc.almera_waterfall_quest"
        const val HudonNpc = "npc.hudon_waterfall_quest"
        const val GeraldNpc = "npc.gerald_waterfall_quest"
        const val HadleyNpc = "npc.hadley_waterfall_quest"
        const val GolrieNpc = "npc.golrie_waterfall_quest"
    }
}
