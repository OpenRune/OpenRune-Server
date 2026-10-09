package org.rsmod.content.quest.area.alkharid.princealirescue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLocCategoryU
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestJournalBuilder
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class PrinceAliRescueQuest @Inject constructor() :
    QuestScript(
        QUEST_KEY,
        "varp.princequest",
        rewards {
            item(COINS, COIN_REWARD, label = "$COIN_REWARD Coins")
            extra("Free passage through the Al Kharid toll gate")
        },
        ItemRewardDisplay(KEY, zoom = 300),
        questVarbit = "varbit.prince_ali_progress",
    ) {

    private var Player.keliAsked by boolVarBit("varbit.prince_ali_keli_asked")
    private var Player.keyOrdered by boolVarBit("varbit.prince_ali_key_ordered")
    private var Player.keyObtained by boolVarBit("varbit.prince_ali_key_obtained")
    private var Player.metLeela by boolVarBit("varbit.prince_ali_met_leela")
    private var Player.joeHadBeer by boolVarBit("varbit.prince_ali_joe_beer")

    override fun ScriptContext.init() {
        check(quest.maxSteps == STAGE_COMPLETE) {
            "Prince Ali Rescue end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at $STAGE_COMPLETE."
        }
        onOpHeldU(YELLOW_DYE, WIG) { dyeWig() }
        onOpLocCategoryU("category.furnace", KEY_PRINT) { copyKeyAtFurnace() }
    }

    private fun ProtectedAccess.dyeWig() {
        if (invDel(inv, YELLOW_DYE, 1, WIG, 1).failure) {
            return
        }
        invAdd(inv, BLOND_WIG)
        mes("You dye the wig blond.")
    }

    private suspend fun ProtectedAccess.copyKeyAtFurnace() {
        arriveDelay()
        if (BRONZE_BAR !in inv) {
            mes("You need a bronze bar to make a copy of the key.")
            return
        }
        startDialogue {
            if (!choice2("Yes", true, "No", false, title = "Create a key using the key print?")) {
                return@startDialogue
            }
            anim("seq.human_furnace")
            if (invDel(inv, KEY_PRINT, 1, BRONZE_BAR, 1).failure) {
                return@startDialogue
            }
            invAdd(inv, KEY)
            player.keyObtained = true
            statAdvance("stat.crafting", KEY_CRAFTING_XP)
        }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Chancellor Hassan</col> in the <col=800000>Al Kharid Palace</col>."

    override fun questLog(player: ProtectedAccess): String =
        questJournal(player) {
            val p = access.player
            val stage = stage(p)
            val start =
                "Chancellor Hassan of Al Kharid needs help with an urgent problem. He told me to " +
                    "speak to <red>Osman</red>, the Emir's spymaster, just outside the palace."
            if (stage == STAGE_STARTED) {
                line(start)
                return@questJournal
            }
            strike(start)
            val briefing =
                "Prince Ali has been kidnapped by Lady Keli's bandits and is held in the abandoned " +
                    "jail east of Draynor Village. I need to get him out without any bloodshed."
            if (stage == STAGE_BRIEFED) {
                line(briefing)
                gathering(p)
                return@questJournal
            }
            strike(briefing)
            strike("I gathered a copy of the cell key and a disguise to make the Prince look like Keli.")
            if (stage == STAGE_PREPARED) {
                line(
                    "Leela told me to deal with the Prince's guard, <red>Joe</red>, without " +
                        "violence. Maybe three beers would do the trick."
                )
                return@questJournal
            }
            strike("I got Joe the guard too drunk to notice anything.")
            if (stage == STAGE_JOE_DRUNK) {
                line("I need to tie <red>Lady Keli</red> up with a rope before I free the Prince.")
                return@questJournal
            }
            strike("I tied Lady Keli up and hid her in a cupboard.")
            if (stage < STAGE_ALI_ESCAPED) {
                line(
                    "I should unlock the <red>cell door</red> with the key and give the Prince " +
                        "his disguise so he can slip past the guards."
                )
                return@questJournal
            }
            strike("Prince Ali escaped in his disguise.")
            line("I should return to <red>Chancellor Hassan</red> in Al Kharid for my reward.")
        }

    private fun QuestJournalBuilder.gathering(p: Player) {
        when {
            hasKey(p) && p.keyObtained -> strike("I have a copy of the cell key.")
            p.keyObtained ->
                line("I have lost the key. <red>Leela</red> can sell me another for 15 coins.")
            p.keyOrdered ->
                line("Osman is having a copy of the key made. He will send it to <red>Leela</red>.")
            KEY_PRINT in p.inv ->
                line(
                    "I have an imprint of the key. I can make a copy at a <red>furnace</red> with " +
                        "a bronze bar."
                )
            else ->
                line(
                    "<red>Lady Keli</red> keeps the only key to the cell. If I can get her to show " +
                        "it to me, I could take an imprint with some soft clay."
                )
        }
        when {
            BLOND_WIG in p.inv -> strike("I have a blond wig.")
            WIG in p.inv -> line("I have a wig. I need to dye it yellow.")
            else -> line("<red>Ned</red>, the old sailor in Draynor Village, might make a wig.")
        }
        if (SKIN_PASTE in p.inv) {
            strike("I have some skin paste.")
        } else {
            line("<red>Aggie</red>, the witch in Draynor Village, could make skin paste.")
        }
        if (PINK_SKIRT in p.inv) {
            strike("I have a pink skirt.")
        } else {
            line("I need a <red>pink skirt</red>. Thessalia in Varrock sells them.")
        }
        if (hasKey(p) && hasDisguise(p)) {
            line("I have everything. I should let <red>Leela</red> know.")
        }
    }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(
                "Chancellor Hassan and the spymaster Osman asked me to rescue Prince Ali, " +
                    "kidnapped by Lady Keli and held in the jail east of Draynor Village."
            )
            line(
                "With the help of Osman's daughter Leela I made a copy of the cell key and a " +
                    "disguise, got the guard drunk and tied Keli up."
            )
            line("Prince Ali escaped disguised as Keli, and the Emir rewarded me.")
        }

    fun stage(player: Player): Int = quest.getQuestStage(player)

    fun setStage(access: ProtectedAccess, stage: Int) {
        quest.setQuestStage(access, stage)
    }

    fun begin(access: ProtectedAccess) {
        access.player.keliAsked = false
        access.player.keyOrdered = false
        access.player.keyObtained = false
        access.player.metLeela = false
        access.player.joeHadBeer = false
        setStage(access, STAGE_STARTED)
    }

    fun keliAsked(player: Player): Boolean = player.keliAsked

    fun setKeliAsked(player: Player) {
        player.keliAsked = true
    }

    fun metLeela(player: Player): Boolean = player.metLeela

    fun setMetLeela(player: Player) {
        player.metLeela = true
    }

    fun joeHadBeer(player: Player): Boolean = player.joeHadBeer

    fun setJoeHadBeer(player: Player) {
        player.joeHadBeer = true
    }

    fun keyOrdered(player: Player): Boolean = player.keyOrdered

    fun orderKey(player: Player, ordered: Boolean) {
        player.keyOrdered = ordered
    }

    fun keyObtained(player: Player): Boolean = player.keyObtained

    fun setKeyObtained(player: Player) {
        player.keyObtained = true
    }

    fun needsImprint(player: Player): Boolean =
        !player.keyOrdered && !player.keyObtained && KEY_PRINT !in player.inv

    fun hasKey(player: Player): Boolean = KEY in player.inv

    fun hasDisguise(player: Player): Boolean =
        BLOND_WIG in player.inv && SKIN_PASTE in player.inv && PINK_SKIRT in player.inv

    fun ProtectedAccess.lostKey(): Boolean =
        player.keyObtained && KEY !in inv && KEY !in bank

    fun gatheringDisguise(player: Player): Boolean = stage(player) in STAGE_STARTED until STAGE_ALI_ESCAPED

    companion object {
        const val QUEST_KEY = "quest_princealirescue"
        const val SHIELD_OF_ARRAV = "quest_shieldofarrav"

        const val STAGE_STARTED = 10
        const val STAGE_BRIEFED = 20
        const val STAGE_PREPARED = 30
        const val STAGE_JOE_DRUNK = 40
        const val STAGE_KELI_TIED = 50
        const val STAGE_ALI_ESCAPED = 100
        const val STAGE_COMPLETE = 110

        const val COIN_REWARD = 700
        const val LOST_KEY_PRICE = 15
        const val BEERS_NEEDED = 3
        const val WOOL_PER_WIG = 3
        const val KEY_CRAFTING_XP = 2.0

        const val COINS = "obj.coins"
        const val KEY = "obj.princeskey"
        const val KEY_PRINT = "obj.keyprint"
        const val SOFT_CLAY = "obj.softclay"
        const val BRONZE_BAR = "obj.bronze_bar"
        const val WIG = "obj.plainwig"
        const val BLOND_WIG = "obj.blondwig"
        const val YELLOW_DYE = "obj.yellowdye"
        const val SKIN_PASTE = "obj.skinpaste"
        const val PINK_SKIRT = "obj.pink_skirt"
        const val BEER = "obj.beer"
        const val ROPE = "obj.rope"
        const val BALL_OF_WOOL = "obj.ball_of_wool"
        const val JUG_OF_WATER = "obj.jug_water"
        const val BUCKET_OF_WATER = "obj.bucket_water"
        const val ASHES = "obj.ashes"
        const val POT_OF_FLOUR = "obj.pot_flour"
        const val REDBERRIES = "obj.redberries"

        const val NPC_HASSAN = "npc.hassan"
        const val NPC_OSMAN = "npc.contact_osman_multi"
        const val NPC_LEELA = "npc.leela"
        const val NPC_KELI = "npc.lady_keli"
        const val NPC_JOE = "npc.joe"
        const val NPC_PRINCE_CELL = "npc.prince_ali_prison"
        const val NPC_PRINCE_PALACE = "npc.prince_ali_palace"

        const val CELL_DOOR = "loc.alidoor"
    }
}
