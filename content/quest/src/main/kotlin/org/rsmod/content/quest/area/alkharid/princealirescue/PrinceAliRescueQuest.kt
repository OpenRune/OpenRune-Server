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

/**
 * Prince Ali Rescue.
 *
 * The stage is the whole cache varp `varp.princequest` (endstate 110), which also drives the jail
 * multinpcs and the toll gate's multilocs: Lady Keli is shown up to [StageJoeDrunk], the
 * imprisoned Prince up to [StageKeliTied], Joe until Hassan pays, the Prince in the palace from
 * [StageAliEscaped], and the toll gate loses its Pay-toll option from [StageAliEscaped] as well.
 * - [StageStarted]: Hassan sent the player to Osman.
 * - [StageBriefed]: Osman explained the plan; the key and the disguise are being gathered.
 * - [StagePrepared]: Leela has seen the key and disguise and pointed the player at Joe.
 * - [StageJoeDrunk]: Joe has had his three beers.
 * - [StageKeliTied]: Keli is tied up in the cupboard and the cell door can be unlocked.
 * - [StageAliEscaped]: the Prince has escaped in disguise.
 * - [StageComplete]: Hassan has paid the reward.
 *
 * Progress the stage cannot hold sits on the server-only `varp.prince_ali_state`: whether Keli has
 * heard the player wants to join her gang, whether Osman is having a key copied, whether the
 * player has ever been given a key, whether Leela has been met, and whether Joe has had his first
 * beer.
 */
class PrinceAliRescueQuest @Inject constructor() :
    QuestScript(
        QuestKey,
        "varp.princequest",
        rewards {
            item(Coins, CoinReward, label = "$CoinReward Coins")
            extra("Free passage through the Al Kharid toll gate")
        },
        ItemRewardDisplay(Key, zoom = 300),
        questVarbit = "varbit.prince_ali_progress",
    ) {

    private var Player.keliAsked by boolVarBit("varbit.prince_ali_keli_asked")
    private var Player.keyOrdered by boolVarBit("varbit.prince_ali_key_ordered")
    private var Player.keyObtained by boolVarBit("varbit.prince_ali_key_obtained")
    private var Player.metLeela by boolVarBit("varbit.prince_ali_met_leela")
    private var Player.joeHadBeer by boolVarBit("varbit.prince_ali_joe_beer")

    override fun ScriptContext.init() {
        check(quest.maxSteps == StageComplete) {
            "Prince Ali Rescue end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at $StageComplete."
        }
        onOpHeldU(YellowDye, Wig) { dyeWig() }
        onOpLocCategoryU("category.furnace", KeyPrint) { copyKeyAtFurnace() }
    }

    private fun ProtectedAccess.dyeWig() {
        if (invDel(inv, YellowDye, 1, Wig, 1).failure) {
            return
        }
        invAdd(inv, BlondWig)
        mes("You dye the wig blond.")
    }

    private suspend fun ProtectedAccess.copyKeyAtFurnace() {
        arriveDelay()
        if (BronzeBar !in inv) {
            mes("You need a bronze bar to make a copy of the key.")
            return
        }
        startDialogue {
            if (!choice2("Yes", true, "No", false, title = "Create a key using the key print?")) {
                return@startDialogue
            }
            anim("seq.human_furnace")
            if (invDel(inv, KeyPrint, 1, BronzeBar, 1).failure) {
                return@startDialogue
            }
            invAdd(inv, Key)
            player.keyObtained = true
            statAdvance("stat.crafting", KeyCraftingXp)
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
            if (stage == StageStarted) {
                line(start)
                return@questJournal
            }
            strike(start)
            val briefing =
                "Prince Ali has been kidnapped by Lady Keli's bandits and is held in the abandoned " +
                    "jail east of Draynor Village. I need to get him out without any bloodshed."
            if (stage == StageBriefed) {
                line(briefing)
                gathering(p)
                return@questJournal
            }
            strike(briefing)
            strike("I gathered a copy of the cell key and a disguise to make the Prince look like Keli.")
            if (stage == StagePrepared) {
                line(
                    "Leela told me to deal with the Prince's guard, <red>Joe</red>, without " +
                        "violence. Maybe three beers would do the trick."
                )
                return@questJournal
            }
            strike("I got Joe the guard too drunk to notice anything.")
            if (stage == StageJoeDrunk) {
                line("I need to tie <red>Lady Keli</red> up with a rope before I free the Prince.")
                return@questJournal
            }
            strike("I tied Lady Keli up and hid her in a cupboard.")
            if (stage < StageAliEscaped) {
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
            KeyPrint in p.inv ->
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
            BlondWig in p.inv -> strike("I have a blond wig.")
            Wig in p.inv -> line("I have a wig. I need to dye it yellow.")
            else -> line("<red>Ned</red>, the old sailor in Draynor Village, might make a wig.")
        }
        if (SkinPaste in p.inv) {
            strike("I have some skin paste.")
        } else {
            line("<red>Aggie</red>, the witch in Draynor Village, could make skin paste.")
        }
        if (PinkSkirt in p.inv) {
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
        setStage(access, StageStarted)
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
        !player.keyOrdered && !player.keyObtained && KeyPrint !in player.inv

    fun hasKey(player: Player): Boolean = Key in player.inv

    fun hasDisguise(player: Player): Boolean =
        BlondWig in player.inv && SkinPaste in player.inv && PinkSkirt in player.inv

    fun ProtectedAccess.lostKey(): Boolean =
        player.keyObtained && Key !in inv && Key !in bank

    fun gatheringDisguise(player: Player): Boolean = stage(player) in StageStarted until StageAliEscaped

    companion object {
        const val QuestKey = "quest_princealirescue"
        const val ShieldOfArrav = "quest_shieldofarrav"

        const val StageStarted = 10
        const val StageBriefed = 20
        const val StagePrepared = 30
        const val StageJoeDrunk = 40
        const val StageKeliTied = 50
        const val StageAliEscaped = 100
        const val StageComplete = 110

        const val CoinReward = 700
        const val LostKeyPrice = 15
        const val BeersNeeded = 3
        const val WoolPerWig = 3
        const val KeyCraftingXp = 2.0

        const val Coins = "obj.coins"
        const val Key = "obj.princeskey"
        const val KeyPrint = "obj.keyprint"
        const val SoftClay = "obj.softclay"
        const val BronzeBar = "obj.bronze_bar"
        const val Wig = "obj.plainwig"
        const val BlondWig = "obj.blondwig"
        const val YellowDye = "obj.yellowdye"
        const val SkinPaste = "obj.skinpaste"
        const val PinkSkirt = "obj.pink_skirt"
        const val Beer = "obj.beer"
        const val Rope = "obj.rope"
        const val BallOfWool = "obj.ball_of_wool"
        const val JugOfWater = "obj.jug_water"
        const val BucketOfWater = "obj.bucket_water"
        const val Ashes = "obj.ashes"
        const val PotOfFlour = "obj.pot_flour"
        const val Redberries = "obj.redberries"

        const val NpcHassan = "npc.hassan"
        const val NpcOsman = "npc.osman"
        const val NpcLeela = "npc.leela"
        const val NpcKeli = "npc.lady_keli_vis"
        const val NpcJoe = "npc.joe_vis"
        const val NpcPrinceCell = "npc.prince_ali_vis_blackeye"
        const val NpcPrincePalace = "npc.prince_ali_vis"

        const val CellDoor = "loc.alidoor"
    }
}
