package org.rsmod.content.quest.area.falador.doricsquest

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MesAnimType
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseMiningLvl
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestJournalBuilder
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.recommendedStatsWarning
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class DoricsQuest
@Inject
constructor(private val objRepo: ObjRepository, private val npcRepo: NpcRepository) :
    QuestScript(
        "quest_dorics",
        "varp.doricquest",
        rewards {
            xp("stat.mining", MiningXpReward)
            item("obj.coins", CoinReward, label = "$CoinReward Coins")
            extra("Use of Doric's Anvils")
        },
        ItemRewardDisplay("obj.steel_pickaxe", zoom = 400),
        questVarbit = "varbit.dorics_quest_progress",
    ) {

    override fun ScriptContext.init() {
        check(quest.maxSteps == StageComplete) {
            "Doric's Quest end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the hand-in writes $StageComplete."
        }

        onOpNpc1("npc.doric") { startDialogue(it.npc) { doric() } }
        onOpLoc1("loc.devious_whetstone") { whetstone() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Doric<col=000080> at his home north of " +
            "<col=800000>Falador<col=000080>."

    override fun questLog(player: ProtectedAccess): String =
        questJournal(player) {
            strike("I have spoken to <red>Doric</red>.")
            addRawLine("")
            line("I need to collect some items and bring them to <red>Doric</red>.")
            material(Clay, "Clay")
            material(CopperOre, "Copper Ore")
            material(IronOre, "Iron Ore")
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line("I have spoken to <red>Doric</red>.")
            addRawLine("")
            line("I have collected some Clay, Copper Ore, and Iron Ore.")
            addRawLine("")
            line("Doric rewarded me for all my hard work.")
            line("I can now use Doric's Anvils whenever I want.")
        }

    /**
     * Returns `true` if [loc] isn't one of Doric's anvils or the player may use them. Otherwise
     * Doric stops the player and this returns `false`.
     */
    suspend fun ProtectedAccess.mayUseAnvil(loc: ObjectServerType): Boolean {
        if (loc.id != DoricsAnvil.asRSCM(RSCMType.LOC)) {
            return true
        }
        if (QuestRequirements.hasCompleted(player, quest.key)) {
            return true
        }
        val doric = findDoric(player.coords)
        if (doric != null) {
            startDialogue(doric) { anvilDialogue() }
        } else {
            startDialogue { anvilDialogue() }
        }
        return false
    }

    private fun findDoric(near: CoordGrid): Npc? =
        npcRepo.findAll(ZoneKey.from(near), zoneRadius = 1).firstOrNull { it.isType(DoricNpc) }

    private fun QuestJournalBuilder.material(material: Material, name: String) {
        val held = access.player.inv.count(material.obj)
        if (held >= material.count) {
            strike("${material.count} $name.")
        } else {
            // UNVERIFIED: the count left when some are already carried (guess)
            line("<red>${material.count} $name</red> - I need ${material.count - held} more.")
        }
    }

    private suspend fun ProtectedAccess.whetstone() {
        if (QuestRequirements.hasCompleted(player, quest.key)) {
            // TODO(content): [quest:devious_minds] Sharpen a mithril 2h sword into a slender blade.
            mesbox("You don't have anything suitable to use with the whetstone.")
            return
        }
        mesbox("You should probably ask before using that.")
    }

    private suspend fun Dialogue.doric() {
        when {
            quest.isQuestCompleted(player) -> afterQuest()
            quest.isQuestInProgress(player) -> duringQuest()
            else -> beforeQuest()
        }
        // TODO(content): [system:clue_scrolls] Doric's beginner and easy clue step lines.
    }

    private suspend fun Dialogue.beforeQuest() {
        chatNpc(quiz, "Hello traveller, what brings you to my humble smithy?")
        when (
            choice5(
                "I wanted to use your anvils.",
                1,
                "I want to use your whetstone.",
                2,
                "Mind your own business, shortstuff!",
                3,
                "I was just checking out the landscape.",
                4,
                "What do you make here?",
                5,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "I wanted to use your anvils.")
                chatNpc(
                    neutral,
                    "My anvils get enough work with my own use. I make pickaxes, and it takes a " +
                        "lot of hard work. If you could get me some more materials, then I could " +
                        "let you use them.",
                )
                offerQuest()
            }
            2 -> {
                chatPlayer(neutral, "I wanted to use your whetstone.")
                chatNpc(
                    neutral,
                    "The whetstone is for more advanced smithing, but I could let you use it as " +
                        "well as my anvils if you could get me some more materials.",
                )
                offerQuest()
            }
            3 -> {
                chatPlayer(angry, "Mind your own business, shortstuff!")
                chatNpc(
                    angry,
                    "How nice to meet someone with such pleasant manners. Do come again when " +
                        "you need to shout at someone smaller than you!",
                )
            }
            4 -> checkingOutTheLandscape()
            5 -> whatDoYouMake()
        }
    }

    private suspend fun Dialogue.checkingOutTheLandscape() {
        chatPlayer(neutral, "I was just checking out the landscape.")
        chatNpc(
            happy,
            "Hope you like it. I do enjoy the solitude of my little home. If you get time, " +
                "please say hi to my friends in the Dwarven Mine.",
        )
        if (!choice2("Dwarven Mine?", true, "Will do!", false)) {
            chatPlayer(happy, "Will do!")
            return
        }
        chatPlayer(quiz, "Dwarven Mine?")
        chatNpc(
            happy,
            "Yep, the entrance is in the side of Ice Mountain just to the east of here. " +
                "They're a friendly bunch. Stop in at Nurmof's store and buy one of my pickaxes!",
        )
    }

    private suspend fun Dialogue.whatDoYouMake() {
        chatPlayer(quiz, "What do you make here?")
        chatNpc(happy, "I make pickaxes. I am the best maker of pickaxes in the whole of Gielinor.")
        chatPlayer(quiz, "Do you have any to sell?")
        chatNpc(neutral, "Sorry, but I've got a running order with Nurmof.")
        if (!choice2("Who's Nurmof?", true, "Ah, fair enough.", false)) {
            chatPlayer(neutral, "Ah, fair enough.")
            return
        }
        chatPlayer(quiz, "Who's Nurmof?")
        chatNpc(
            neutral,
            "Nurmof has a store over in the Dwarven Mine. You can find the entrance on the side " +
                "of Ice Mountain to the east of here.",
        )
    }

    private suspend fun Dialogue.offerQuest() {
        recommendedStatsWarning(quest)
        if (choice2("Yes.", true, "No.", false, title = "Start Doric's Quest?")) {
            acceptQuest()
        } else {
            declineQuest()
        }
    }

    private suspend fun Dialogue.anvilDialogue() {
        if (quest.isQuestInProgress(player)) {
            duringQuest()
        } else {
            anvilWarning()
        }
    }

    private suspend fun Dialogue.anvilWarning() {
        doricSays(
            angry,
            "Hey, who said you could use that? My anvils get enough work with my own use. I " +
                "make pickaxes, and it takes a lot of hard work.",
        )
        val ask =
            choice2(
                "Sorry, would it be OK if I used your anvils?",
                true,
                "I didn't want to use your anvils anyway.",
                false,
            )
        if (!ask) {
            chatPlayer(bored, "I didn't want to use your anvils anyway.")
            doricSays(confused, "That is your choice.")
            return
        }
        chatPlayer(quiz, "Sorry, would it be OK if I used your anvils?")
        doricSays(neutral, "If you could get me some more materials then I could let you use them.")
        val accept =
            choice2(
                "Yes, I will get you materials.",
                true,
                "No, hitting rocks is for the boring people, sorry.",
                false,
            )
        if (accept) acceptQuest() else declineQuest()
    }

    private suspend fun Dialogue.acceptQuest() {
        chatPlayer(happy, "Yes, I will get you the materials.")
        quest.setQuestStage(access, StageStarted)
        access.invAddOrDrop(objRepo, "obj.bronze_pickaxe")
        doricSays(
            neutral,
            "Clay is what I use more than anything, to make casts. Could you get me 6 clay, 4 " +
                "copper ore, and 2 iron ore, please? I could pay a little, and let you use my " +
                "anvils. Take this pickaxe with you just in case you need it.",
        )
        if (!hasMaterials(player)) {
            whereToFind()
            return
        }
        chatPlayer(happy, "You know, it's funny you should require those exact things!")
        doricSays(quiz, "What do you mean?")
        chatPlayer(
            happy,
            "I can usually fit 28 things in my backpack and in a world full of quite literally " +
                "limitless possibilities, a complete coincidence has occurred!",
        )
        doricSays(quiz, "I don't quite understand what you're saying?")
        chatPlayer(
            happy,
            "Well, out of pure coincidence, despite definitely not knowing what you were about " +
                "to request, I just so happened to have carried those exact items!",
        )
        if (hasExactMaterials(player)) {
            chatPlayer(shocked, "In fact, in the exact quantities too!")
        }
        doricSays(
            shocked,
            "Oh my, that is a coincidence! Pass them here, please. I can spare you some coins " +
                "for your trouble, and please use my anvils any time you want.",
        )
        handIn()
    }

    private suspend fun Dialogue.declineQuest() {
        chatPlayer(bored, "No, hitting rocks is for the boring people, sorry.")
        doricSays(confused, "That is your choice. Nice to meet you anyway.")
    }

    private suspend fun Dialogue.duringQuest() {
        doricSays(quiz, "Have you got my materials yet, traveller?")
        if (hasMaterials(player)) {
            chatPlayer(happy, "I have everything you need!")
            doricSays(
                happy,
                "Many thanks! Pass them here, please. I can spare you some coins for your " +
                    "trouble, and please use my anvils any time you want.",
            )
            handIn()
            return
        }
        chatPlayer(sad, "Sorry, I don't have them all yet.")
        doricSays(
            neutral,
            "Not to worry, stick at it. Remember, I need 6 clay, 4 copper ore, and 2 iron ore.",
        )
        whereToFind()
    }

    private suspend fun Dialogue.whereToFind() {
        val ask = choice2("Where can I find those?", true, "Certainly, I'll be right back!", false)
        if (!ask) {
            chatPlayer(happy, "Certainly, I'll be right back!")
            return
        }
        chatPlayer(quiz, "Where can I find those?")
        doricSays(
            happy,
            "You'll be able to find all those ores in the rocks just inside the Dwarven Mine. " +
                "Head east from here and you'll find the entrance in the side of Ice Mountain.",
        )
        if (player.baseMiningLvl >= IronOreMiningLevel) {
            return
        }
        chatPlayer(sad, "But I'm not a good enough miner to get iron ore.")
        doricSays(
            neutral,
            "Oh well, you could practice mining until you can. Can't beat a bit of mining - it's " +
                "a useful skill. Failing that, you might be able to find a more experienced " +
                "adventurer to buy the iron ore off.",
        )
    }

    private suspend fun Dialogue.afterQuest() {
        chatNpc(neutral, "Hello traveller, how is your metalworking coming along?")
        chatPlayer(neutral, "Not too bad, Doric.")
        chatNpc(happy, "Good, the love of metal is a thing close to my heart.")
    }

    private suspend fun Dialogue.handIn() {
        player.anim("seq.human_dwarf_recieve")
        npc?.anim("seq.dwarf_hand_over")
        objbox(CopperOre.obj, "You hand the clay, copper, and iron to Doric.")
        if (player.inv.count("obj.coins") > Int.MAX_VALUE - CoinReward) {
            return
        }
        if (!takeMaterials()) {
            return
        }
        quest.completeQuest(access)
    }

    private fun Dialogue.takeMaterials(): Boolean =
        player.invTransaction(access.inv) {
            val inventory = select(access.inv)
            for (material in Materials) {
                delete {
                    from = inventory
                    obj = material.obj.asRSCM()
                    strictCount = material.count
                }
            }
        }.success

    private suspend fun Dialogue.doricSays(mesanim: MesAnimType, text: String) {
        if (npc != null) {
            chatNpc(mesanim, text)
        } else {
            chatNpcSpecific("Doric", "npc.doric", mesanim, text)
        }
    }

    private fun hasMaterials(player: Player): Boolean =
        Materials.all { player.inv.count(it.obj) >= it.count }

    private fun hasExactMaterials(player: Player): Boolean =
        Materials.all { player.inv.count(it.obj) == it.count }

    private data class Material(val obj: String, val count: Int)

    private companion object {
        private const val StageStarted = 10
        private const val StageComplete = 100

        private const val MiningXpReward = 1300.0
        private const val CoinReward = 180
        private const val IronOreMiningLevel = 15

        private const val DoricsAnvil = "loc.dorics_anvil"
        private const val DoricNpc = "npc.doric"

        private val Clay = Material("obj.clay", 6)
        private val CopperOre = Material("obj.copper_ore", 4)
        private val IronOre = Material("obj.iron_ore", 2)
        private val Materials = listOf(Clay, CopperOre, IronOre)
    }
}
