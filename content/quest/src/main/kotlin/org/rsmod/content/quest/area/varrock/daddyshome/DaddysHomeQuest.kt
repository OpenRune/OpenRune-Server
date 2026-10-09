package org.rsmod.content.quest.area.varrock.daddyshome

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.table.DaddysHomeFurnitureRow
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class DaddysHomeQuest @Inject constructor(private val objRepo: ObjRepository) :
    QuestScript(
        questKey = "miniquest_daddyshome",
        questVarp = "varp.karam_dungeon_varbit",
        rewards = rewards {},
        completedQuestItemDisplay = ItemRewardDisplay(CRATE),
        questVarbit = "varbit.daddyshome_status",
    ) {

    private var Player.crateOpened by boolVarBit("varbit.daddyshome_crate_opened")
    private var Player.houseLocation by intVarBit("varbit.poh_house_location")
    private var Player.contractorDiscussed by intVarBit("varbit.con_contract_discussed")
    private var Player.miniquestsCompleted by intVarBit("varbit.miniquests_completed_count")

    override fun ScriptContext.init() {
        check(quest.maxSteps == COMPLETE) {
            "Daddy's Home end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at $COMPLETE."
        }
        onOpNpc1(MARLO) { startDialogue(it.npc) { marloDialogue() } }
        onOpNpc1(YARLO) { startDialogue(it.npc) { yarloDialogue() } }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Marlo</col> in the <col=800000>Estate Agent's house</col> in " +
            "north-east <col=800000>Varrock</col>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            description(
                "<red>Marlo</red> has asked me to renovate the house his father, " +
                    "<red>Old Man Yarlo</red>, has settled into."
            )

            objective(
                "I should speak to <red>Old Man Yarlo</red> in the south-east of <red>Varrock</red>, " +
                    "west of <red>Aubury's Rune Shop</red>."
            ) {
                visibleWhen { stage(access.player) == STARTED }
            }

            objective("I need to <red>remove all of the old furniture</red> from Yarlo's house.") {
                visibleWhen { stage(access.player) == REMOVING }
            }

            objective(
                "I have removed the old furniture. I should talk to <red>Old Man Yarlo</red> again."
            ) {
                visibleWhen { stage(access.player) in REMOVED..LECTURE }
            }

            objective(
                "I need to build new furniture for Yarlo's house with a <red>hammer</red> and a " +
                    "<red>saw</red>: two <red>stools</red>, two <red>tables</red>, a " +
                    "<red>chair</red>, a <red>carpet</red> and a <red>bed</red>."
            ) {
                visibleWhen { stage(access.player) in BUILDING until BUILT }
            }

            objective(
                "The bed needs three <red>waxwood planks</red>. I can take waxwood logs from the " +
                    "<red>crates</red> in the kitchen to the <red>Varrock sawmill</red> and ask " +
                    "for planks for Yarlo."
            ) {
                visibleWhen {
                    stage(access.player) in BUILDING until BUILT &&
                        furnitureState(access.player, Furniture.bed) < Furniture.BUILT
                }
            }

            objective("I have rebuilt Yarlo's furniture and should talk to <red>Old Man Yarlo</red>.") {
                visibleWhen {
                    stage(access.player) in BUILDING until BUILT &&
                        allFurnitureAtLeast(access.player, Furniture.BUILT)
                }
            }

            objective("I should return to <red>Marlo</red> in Varrock to claim my reward.") {
                visibleWhen { stage(access.player) in BUILT until COMPLETE }
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line("Marlo asked me to help his father, Old Man Yarlo, renovate his home in Varrock.")
            line(
                "I removed the broken furniture and built a carpet, two stools, two tables, a " +
                    "chair and a waxwood bed."
            )
            line("Marlo rewarded me with a crate of supplies, some money and Construction experience.")
        }

    internal fun stage(player: Player): Int = quest.getQuestStage(player)

    internal fun furnitureState(player: Player, furniture: DaddysHomeFurnitureRow): Int =
        player.vars[furniture.varbitType]

    internal fun setFurnitureState(
        player: Player,
        furniture: DaddysHomeFurnitureRow,
        state: Int,
    ) {
        VarPlayerIntMapSetter.set(player, furniture.varbitType, state)
    }

    internal fun allFurnitureAtLeast(player: Player, state: Int): Boolean =
        Furniture.all.all { furnitureState(player, it) >= state }

    internal fun markCrateOpened(player: Player) {
        player.crateOpened = true
    }

    private fun Dialogue.youngPerson(): String =
        if (player.appearance.bodyType == Constants.bodytype_a) "man" else "lady"

    private suspend fun Dialogue.marloDialogue() {
        val stage = stage(player)
        when {
            stage == 0 -> marloBeforeStart()
            stage < BUILT -> marloInProgress()
            stage < COMPLETE -> marloReward(first = stage == BUILT)
            else -> marloAfterQuest()
        }
    }

    private suspend fun Dialogue.marloBeforeStart() {
        chatNpc(
            happy,
            "Hello - it's lovely to see a Mahogany Homes contractor on their rounds! Now, can I " +
                "help you with something? Or might you be able to do me a little favour?",
        )
        while (true) {
            val options = buildList {
                if (player.contractorDiscussed == 0) add(ASK_COMPANY to MarloOption.Company)
                add(ASK_CONTRACT to MarloOption.Contract)
                add("What kind of favour do you want me to do?" to MarloOption.Favour)
                add("Maybe another time." to MarloOption.Leave)
            }
            when (menu(options)) {
                MarloOption.Company -> aboutCompany()
                MarloOption.Contract -> noContract()
                MarloOption.Favour -> {
                    if (favour()) return
                }
                else -> return chatPlayer(neutral, "Maybe another time.")
            }
        }
    }

    private suspend fun Dialogue.aboutCompany() {
        chatPlayer(quiz, ASK_COMPANY)
        chatNpc(
            happy,
            "Oh, we redecorate people's houses, fixing things and building new furniture. " +
                "Mending cabinets, building new chairs and tables - things like that.",
        )
        chatNpc(
            neutral,
            "When a client hires us to redecorate their house, we send a builder round to do the " +
                "job, and we ensure the builder's paid for their work.",
        )
        chatNpc(
            happy,
            "We can't hire you for that kind of work now, but perhaps you could do me a little " +
                "favour instead, if you're willing. I can pay!",
        )
    }

    private suspend fun Dialogue.noContract() {
        chatPlayer(quiz, ASK_CONTRACT)
        chatNpc(neutral, "You haven't got a contract at the moment.")
    }

    /** Returns true when the conversation is over. */
    private suspend fun Dialogue.favour(): Boolean {
        chatPlayer(quiz, "What kind of favour do you want me to do?")
        chatNpc(
            sad,
            "Well, you see, it's about my dad. His house is really decrepit. I need someone to " +
                "rip out the broken furniture and build him some better stuff. I'll pay you if " +
                "you do it.",
        )
        var detailed = false
        while (true) {
            val options = buildList {
                add(ASK_CONSTRUCTION to FavourOption.Company)
                add(ASK_BAD_HOUSE to FavourOption.BadHouse)
                if (detailed) {
                    add("Tell me where he lives, and I'll do the job." to FavourOption.Accept)
                } else {
                    add("Tell me more about the job." to FavourOption.MoreAboutJob)
                }
                add("I'd rather not." to FavourOption.Refuse)
            }
            when (menu(options)) {
                FavourOption.Company -> {
                    chatPlayer(quiz, ASK_CONSTRUCTION)
                    chatNpc(
                        worried,
                        "See, my dad just - um - found the house. No-one was using it, so he " +
                            "just settled in. It's all perfectly harmless, and he's a sick old " +
                            "man anyway, so...",
                    )
                    chatNpc(
                        worried,
                        "Basically, he's not really meant to be there. And we'd prefer that the " +
                            "landlord not find out, so we're keeping this job off the record. " +
                            "I'll pay you myself.",
                    )
                }
                FavourOption.BadHouse -> {
                    chatPlayer(quiz, ASK_BAD_HOUSE)
                    chatNpc(
                        sad,
                        "Some kind of economic downturn wiped out his pension plans. It happens " +
                            "more often than you'd think. But I'd rather not talk about it. Can " +
                            "you help him, though?",
                    )
                }
                FavourOption.MoreAboutJob -> {
                    chatPlayer(quiz, "Tell me more about the job.")
                    chatNpc(
                        neutral,
                        "Nothing too hard. You'll have to rip out the broken furniture, then " +
                            "build replacements. Dad will talk you through it - he's a retired " +
                            "carpenter himself. He loves talking about construction.",
                    )
                    detailed = true
                }
                FavourOption.Accept -> {
                    chatPlayer(happy, "Tell me where he lives, and I'll do the job.")
                    chatNpc(
                        happy,
                        "Fabulous! Dad lives in the south-east of Varrock, west of Aubury's " +
                            "Rune Shop. His name's Yarlo. Old Man Yarlo, they call him.",
                    )
                    quest.setQuestStage(access, STARTED)
                    return true
                }
                FavourOption.Refuse -> {
                    chatPlayer(neutral, "I'd rather not.")
                    chatNpc(neutral, "Okay, suit yourself.")
                    return true
                }
            }
        }
    }

    private suspend fun Dialogue.marloInProgress() {
        chatNpc(quiz, "How are you getting on with helping my dad?")
        val options =
            listOf(
                "Where did you say he lived?" to MarloOption.Where,
                ASK_CONTRACT to MarloOption.Contract,
            )
        when (menu(options)) {
            MarloOption.Where -> {
                chatPlayer(quiz, "Where did you say he lived?")
                chatNpc(
                    happy,
                    "Dad lives in the south-east of Varrock, west of Aubury's Rune Shop. His " +
                        "name's Yarlo. Old Man Yarlo, they call him. He's sure to be at home, so " +
                        "keep looking around.",
                )
            }
            else -> noContract()
        }
    }

    private suspend fun Dialogue.marloReward(first: Boolean) {
        if (first) {
            chatPlayer(happy, "I've finished building the new furniture for your dad.")
            chatNpc(
                happy,
                "That's wonderful news! Dad's going to be so much happier. I'm very grateful! " +
                    "Now, I promised to pay you for your work...",
            )
        } else {
            chatNpc(
                neutral,
                "I haven't forgotten that I promised to pay you for your help. Is this a good " +
                    "time for that?",
            )
        }
        val options =
            listOf(
                "Yeah, what have you got for me?" to MarloOption.Reward,
                ASK_CONTRACT to MarloOption.Contract,
                "Can I do some other kind of building work for you?" to MarloOption.OtherWork,
            )
        when (menu(options)) {
            MarloOption.Reward -> claimReward()
            MarloOption.Contract -> {
                noContract()
                rewardPending()
            }
            else -> {
                chatPlayer(quiz, "Can I do some other kind of building work for you?")
                chatNpc(
                    sad,
                    "I'm so sorry, but you're not qualified to do contracts for Mahogany Homes " +
                        "yet. Just keep helping my dad, for now, and perhaps I can sort that out " +
                        "for you.",
                )
                rewardPending()
            }
        }
    }

    private fun Dialogue.rewardPending() {
        if (stage(player) == BUILT) quest.setQuestStage(access, REWARD_PENDING)
    }

    private suspend fun Dialogue.claimReward() {
        chatPlayer(happy, "Yeah, what have you got for me?")
        val hasHouse = player.houseLocation != 0
        if (hasHouse) {
            chatNpc(
                happy,
                "Now, I can't spare very much money, but I've got some supplies in this crate " +
                    "that might help with your own Construction work.",
            )
        } else {
            chatNpc(
                happy,
                "Now, I can't spare very much money, but I've persuaded the Estate Agent to let " +
                    "me give you a house of your own. You'll be able to decorate it, like you " +
                    "did for my dad's house.",
            )
            chatNpc(
                happy,
                "Normally a new house costs 1,000 coins, so that's a bit of a saving. Also, I've " +
                    "got a crate of supplies to help you decorate it.",
            )
        }
        chatNpc(
            happy,
            "I've included some handy spell tablets that'll take you to your house, to cut down " +
                "on the walking a bit, as well as one for visiting my boss in Falador. Now, let " +
                "me tell you some tricks of the trade too...",
        )
        if (stage(player) !in BUILT until COMPLETE) return
        val payout = completeMiniquest(hasHouse)
        objbox(
            CRATE,
            if (hasHouse) {
                "Marlo rewards you with a crate of supplies, some money, and 400 Construction XP."
            } else {
                "Marlo rewards you with a crate of supplies, a house in Rimmington, and 400 " +
                    "Construction XP."
            },
        )
        showCompletionScroll(payout)
    }

    private fun Dialogue.completeMiniquest(hasHouse: Boolean): List<String> {
        VarPlayerIntMapSetter.set(player, "varbit.daddyshome_status", COMPLETE)
        player.miniquestsCompleted++
        player.midiJingle(MINIQUEST_JINGLE)
        access.statAdvance("stat.construction", CONSTRUCTION_XP)
        access.invAddOrDrop(objRepo, CRATE)
        val lines = mutableListOf("400 Construction XP", "Marlo's crate of supplies")
        if (hasHouse) {
            access.invAddOrDrop(objRepo, "obj.coins", HOUSE_COINS)
            lines += "1,000 coins"
        } else {
            player.houseLocation = RIMMINGTON_HOUSE
            lines += "A player-owned house in Rimmington"
        }
        return lines
    }

    private fun Dialogue.showCompletionScroll(lines: List<String>) {
        access.ifOpenMain("interface.questscroll")
        access.ifSetText("component.questscroll:quest_title", "You have completed ${quest.displayName}!")
        access.ifSetObj("component.questscroll:quest_model", obj = CRATE, zoom = 10)
        for (i in 1..SCROLL_REWARD_LINES) {
            access.ifSetText("component.questscroll:quest_reward$i", lines.getOrNull(i - 1) ?: "")
        }
    }

    private suspend fun Dialogue.marloAfterQuest() {
        if (!crateOwned()) {
            chatNpc(
                happy,
                "Hello again! Did you lose that crate of supplies I gave you? Here, take another.",
            )
            access.invAddOrDrop(objRepo, CRATE)
            objbox(CRATE, "Marlo gives you another crate of supplies.")
        }
        chatNpc(neutral, "Hey, how can I help you?")
        chatPlayer(quiz, "What do you do here?")
        chatNpc(
            happy,
            "This is one of the branches for Mahogany Homes, Gielinor's first construction " +
                "company.",
        )
        chatPlayer(quiz, "Oh wow, how can I get involved?")
        chatNpc(
            happy,
            "If you go see Amy in the Falador branch, she'll fill you in. Her office is just " +
                "east of the Estate Agent in Falador.",
        )
        chatPlayer(happy, "Nice, thank you.")
    }

    private fun Dialogue.crateOwned(): Boolean =
        player.crateOpened || CRATE in access.inv || CRATE in access.bank

    private suspend fun Dialogue.yarloDialogue() {
        val stage = stage(player)
        when {
            stage == 0 -> yarloStandard()
            stage == STARTED -> yarloBegin()
            stage == REMOVING -> yarloRemoving()
            stage < BUILDING -> yarloLecture()
            stage < BUILT -> yarloBuilding()
            stage < COMPLETE -> yarloAwaitingReward()
            else -> yarloAfterQuest()
        }
    }

    private suspend fun Dialogue.yarloStandard() {
        chatNpc(quiz, "What brings you here, young ${youngPerson()}?")
        when (
            choice2("I'm just wandering around.", 1, "Nothing, really.", 2)
        ) {
            1 -> {
                chatPlayer(neutral, "I'm just wandering around.")
                chatNpc(
                    happy,
                    "Oh, that's nice. I used to wander around, back when my legs were better. " +
                        "So long ago.",
                )
            }
            else -> {
                chatPlayer(neutral, "Nothing, really.")
                chatNpc(
                    neutral,
                    "That's the trouble with young people these days. Not my boy, though. He's " +
                        "a good boy, always working. That's why he never visits. Run along " +
                        "now, young ${youngPerson()}.",
                )
            }
        }
    }

    private suspend fun Dialogue.yarloBegin() {
        chatNpc(quiz, "What brings you here, young ${youngPerson()}?")
        chatPlayer(neutral, "Marlo sent me to fix your furniture.")
        chatNpc(
            happy,
            "My boy Marlo sent you? Oh yes, you've come to help with my furniture. It's in a " +
                "terrible state. You'll want to start by removing all the tatty old furniture.",
        )
        for (furniture in Furniture.all) {
            if (furnitureState(player, furniture) == Furniture.UNTOUCHED) {
                setFurnitureState(player, furniture, Furniture.BROKEN)
            }
        }
        quest.setQuestStage(access, REMOVING)
        chatNpc(
            neutral,
            "There are two stools, two tables, a chair and a rotten carpet to replace. I need a " +
                "better bed too. Let me know when you've removed all the old furniture.",
        )
    }

    private suspend fun Dialogue.yarloRemoving() {
        chatNpc(quiz, "Ah, young ${youngPerson()}, how's the redecoration coming on?")
        val remaining = Furniture.all.filter { furnitureState(player, it) < Furniture.CLEARED }
        if (remaining.isEmpty()) {
            quest.setQuestStage(access, REMOVED)
            yarloLecture()
            return
        }
        if (remaining.size == Furniture.all.size) {
            chatPlayer(quiz, "What am I supposed to be doing again?")
            chatNpc(
                neutral,
                "You'll want to start by removing all the tatty old furniture. There are two " +
                    "stools, two tables, a chair and a rotten carpet to remove, as well as the " +
                    "campbed. Let me know when you've done that.",
            )
            return
        }
        chatPlayer(neutral, "I've been removing the old furniture.")
        val lines = remaining.map { "- ${it.remaining}" }
        val firstPage = lines.take(REMAINING_PER_PAGE)
        chatNpc(neutral, "You still need to remove:<br>" + firstPage.joinToString("<br>"))
        val rest = lines.drop(REMAINING_PER_PAGE)
        if (rest.isNotEmpty()) chatNpc(neutral, rest.joinToString("<br>"))
    }

    private suspend fun Dialogue.yarloLecture() {
        chatNpc(quiz, "Ah, young ${youngPerson()}, how's the redecoration coming on?")
        chatPlayer(happy, "I've finished removing all the old furniture.")
        chatNpc(
            happy,
            "Jolly good, young ${youngPerson()}. Now, we'll need to have a talk about what " +
                "you'll build for me to replace it.",
        )
        chatNpc(
            neutral,
            "You see, I used to be a carpenter myself, before the arthritis got me. Such " +
                "wonderful things I could build. Let me tell you about the things Construction " +
                "can do for you...",
        )
        if (player.houseLocation != 0) {
            chatPlayer(
                confused,
                "Is this necessary? I've got a house of my own, in " +
                    "${houseCity(player.houseLocation)}, and I've been learning about " +
                    "Construction for a while.",
            )
        }
        val listen =
            choice2(
                "Listen to Yarlo.",
                true,
                "Skip Yarlo's lecture. He'll offer it later if you like.",
                false,
            )
        if (listen) {
            quest.setQuestStage(access, LECTURE)
            constructionLecture()
        } else {
            chatNpc(
                neutral,
                "You young people are always rushing around trying to optimise the fun out of " +
                    "life. There are other ways to enjoy the world, you know! But I'm sure you " +
                    "know best.",
            )
        }
        buildInstructions()
        quest.setQuestStage(access, BUILDING)
        chatNpc(
            happy,
            "I think that's everything - two stools, two tables, a chair, a bed and a carpet. " +
                "Off you trot, young ${youngPerson()}. I'll be here if you have any questions.",
        )
    }

    private suspend fun Dialogue.constructionLecture() {
        mesbox(
            "As a builder, in your own house, you can create your own rooms full of furniture. A " +
                "house may start out very basic..."
        )
        mesbox("... but in Building Mode, you'll see what you can add to it...")
        mesbox("... with fancier options available as your Construction level rises.")
        mesbox(
            "You can add lots more rooms, and build their furniture too, unlocking really handy " +
                "features."
        )
        mesbox(
            "For example, in a study, you could build a lectern for creating spell tablets, " +
                "powerful items that can cast spells without runes. You could sell your tablets " +
                "to other people too."
        )
        mesbox(
            "A Teleport Nexus can take you to a huge range of destinations, making your home a " +
                "powerful travel hub."
        )
        mesbox(
            "Show your dedication to the gods by building an altar, where you can sacrifice " +
                "bones for bonus Prayer XP."
        )
        mesbox(
            "With a regeneration pool, you can get healed quickly, before you teleport back to " +
                "the action."
        )
        mesbox(
            "The Estate Agents can redecorate your house in different styles, if you want " +
                "something a bit more impressive."
        )
        mesbox(
            "And, of course, to make your house feel truly like a home, you can fill it with " +
                "adorable critters..."
        )
        mesbox("... and drop your guests into the monster pit beneath your throne room.")
        mesbox("All this could be yours, if you train Construction!")
    }

    private suspend fun Dialogue.buildInstructions() {
        chatNpc(
            neutral,
            "Now, I was just getting round to telling you about the furniture I want you to " +
                "build for me. For a start, I'd like two new stools, two new tables and a new " +
                "chair.",
        )
        objbox(
            PLANK,
            "Some basic wooden planks and some nails should do, for those. I won't ask for " +
                "anything more fancy - it'd only get stolen anyway, in this neighbourhood.",
        )
        objbox(
            "obj.cloth",
            "I'll want a new carpet too. They can really change the atmosphere of a room, you " +
                "know! You can buy cloth at the sawmill to make one.",
        )
        chatNpc(
            neutral,
            "Now, for the bed, I've got something special in mind. This town's quite damp, and " +
                "I don't want the moisture rising up the bed into my blankets. But I've found " +
                "some wood that resists water.",
        )
        waxwoodExplanation(lastLine = true)
    }

    private suspend fun Dialogue.waxwoodExplanation(lastLine: Boolean) {
        objbox(
            WAXWOOD_LOGS,
            "Those abandoned crates in the kitchen were here when I moved in, and I found they " +
                "contain waxwood logs. Waxwood isn't much used in carpentry, but it resists " +
                "water, so I want a bed made from that.",
        )
        objbox(
            WAXWOOD_PLANK,
            if (lastLine) {
                "You'll have to take three logs from the crate to the sawmill. The chap there " +
                    "will turn them into planks if you mention my name - I've known his family " +
                    "for years."
            } else {
                "You'll have to take three logs from the crate to the sawmill. The chap there " +
                    "will turn them into planks if you mention my name."
            },
        )
    }

    private suspend fun Dialogue.yarloBuilding() {
        if (allFurnitureAtLeast(player, Furniture.BUILT)) {
            chatNpc(
                happy,
                "That's everything! Thank you so much, young ${youngPerson()}. Trot off back to " +
                    "my boy Marlo, now. He'll have something for you.",
            )
            quest.setQuestStage(access, BUILT)
            chatPlayer(happy, "Okay, I'll head back to Marlo.")
            return
        }
        chatNpc(quiz, "Do you need some help with the building work, young ${youngPerson()}?")
        while (true) {
            when (
                choice5(
                    "How do I build furniture?", YarloOption.How,
                    "Where do I get planks, nails and cloth?", YarloOption.Materials,
                    "I need some tools.", YarloOption.Tools,
                    "Tell me about the waxwood for the bed.", YarloOption.Waxwood,
                    "I'll get on with it.", YarloOption.Leave,
                )
            ) {
                YarloOption.How -> {
                    chatPlayer(quiz, "How do I build furniture?")
                    chatNpc(neutral, "Ah. Look in the place where the new furniture will go...")
                    mesbox(
                        "A talented builder will see a vision there of the furniture they could " +
                            "build. Interact with that, and you should find you can build " +
                            "furniture."
                    )
                    chatNpc(neutral, "You'll need a hammer and saw. I can supply those if you need them.")
                }
                YarloOption.Materials -> {
                    chatPlayer(quiz, "Where do I get planks, nails and cloth?")
                    chatNpc(
                        happy,
                        "From the sawmill! They sell nails and cloth at reasonable prices. " +
                            "You'd have to take your own logs there, though, to get planks, but " +
                            "there are plenty of trees to chop nearby.",
                    )
                    chatNpc(
                        worried,
                        "Watch out, though - nails can bend, if you're not careful. You should " +
                            "probably get some spares, just in case.",
                    )
                    mesbox("Look on your World Map to find the sawmill.")
                }
                YarloOption.Tools -> tools()
                YarloOption.Waxwood -> {
                    chatPlayer(quiz, "Tell me about the waxwood for the bed.")
                    chatNpc(
                        neutral,
                        "This town's quite damp, and I don't want the moisture rising up the bed " +
                            "into my blankets. But I've found some wood that resists water.",
                    )
                    waxwoodExplanation(lastLine = false)
                    mesbox("Look on your World Map to find the sawmill.")
                }
                YarloOption.Leave -> return chatPlayer(neutral, "I'll get on with it.")
            }
        }
    }

    private suspend fun Dialogue.tools() {
        val hammer = DaddysHomeFurniture.HAMMERS.any { it in access.inv }
        val saw = DaddysHomeFurniture.SAWS.any { it in access.inv }
        if (hammer && saw) {
            chatNpc(
                neutral,
                "You've got a hammer and a saw. I wouldn't be able to offer you anything else.",
            )
            return
        }
        chatPlayer(neutral, "I need some tools.")
        if (!hammer) offerTool("hammer", "obj.hammer")
        if (!saw) offerTool("saw", "obj.poh_saw")
    }

    private suspend fun Dialogue.offerTool(name: String, obj: String) {
        chatNpc(quiz, "Would you like a $name? I can spare one.")
        if (choice2("Yes please.", true, "No thanks.", false)) {
            chatPlayer(happy, "Yes please.")
            access.invAddOrDrop(objRepo, obj)
            objbox(obj, "Yarlo gives you a $name.")
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }

    private suspend fun Dialogue.yarloAwaitingReward() {
        chatNpc(
            quiz,
            "Hello again, young ${youngPerson()}. Has my boy Marlo rewarded you yet?",
        )
        chatPlayer(neutral, "Not yet. I'll get round to visiting him soon.")
        chatNpc(happy, "Yes, you should do that. You've earned it.")
    }

    private suspend fun Dialogue.yarloAfterQuest() {
        chatNpc(
            happy,
            "Hello again, young ${youngPerson()}. You really did wonders for my little home. I " +
                "feel so much happier living here now.",
        )
        val more =
            choice2(
                "That's good to hear. Goodbye now.",
                false,
                "Tell me about more things I can do with Construction.",
                true,
            )
        if (!more) {
            chatPlayer(happy, "That's good to hear. Goodbye now.")
            return
        }
        chatPlayer(quiz, "Tell me about more things I can do with Construction.")
        constructionLecture()
        chatPlayer(happy, "Thank you!")
    }

    private fun houseCity(location: Int): String =
        when (location) {
            1 -> "Rimmington"
            2 -> "Taverley"
            3 -> "Pollnivneach"
            4 -> "Hosidius"
            5 -> "Rellekka"
            6 -> "Brimhaven"
            7 -> "Yanille"
            8 -> "Prifddinas"
            else -> "Gielinor"
        }

    private enum class MarloOption {
        Company,
        Contract,
        Favour,
        Leave,
        Where,
        Reward,
        OtherWork,
    }

    private enum class FavourOption {
        Company,
        BadHouse,
        MoreAboutJob,
        Accept,
        Refuse,
    }

    private enum class YarloOption {
        How,
        Materials,
        Tools,
        Waxwood,
        Leave,
    }

    internal companion object {
        const val STARTED = 1
        const val REMOVING = 2
        const val REMOVED = 3
        const val LECTURE = 4
        const val BUILDING = 5
        const val BUILT = 10
        const val REWARD_PENDING = 12
        const val COMPLETE = 13

        const val MARLO = "npc.con_contractor_varrock"
        const val YARLO = "npc.daddyshome_daddy"

        const val PLANK = "obj.woodplank"
        const val WAXWOOD_LOGS = "obj.daddyshome_waxwood_logs"
        const val WAXWOOD_PLANK = "obj.daddyshome_waxwood_plank"
        const val CRATE = "obj.daddyshome_reward"
        const val CRATES = "loc.daddyshome_crates"

        private const val CONSTRUCTION_XP = 400.0
        private const val HOUSE_COINS = 1_000
        private const val RIMMINGTON_HOUSE = 1
        private const val MINIQUEST_JINGLE = 283
        private const val SCROLL_REWARD_LINES = 7
        private const val REMAINING_PER_PAGE = 3

        private const val ASK_COMPANY = "What does this Mahogany Homes company do?"
        private const val ASK_CONTRACT = "What's my current construction contract?"
        private const val ASK_CONSTRUCTION = "Aren't you in a construction company yourself though?"
        private const val ASK_BAD_HOUSE = "Why has he got such a bad house?"
    }
}
