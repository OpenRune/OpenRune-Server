package org.rsmod.content.areas.city.draynor.wom

import dev.openrune.types.MesAnimType
import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.city.draynor.wom.WomRecycling.Companion.openRecyclingCentre
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal var Player.womTask by intVarBit("varbit.wom_task")
private var Player.womNumber by intVarBit("varbit.wom_number")
private var Player.bankJob by intVarBit("varbit.wom_bankjob")
private val Player.questPoints by intVarp("varp.qp")

internal const val WOM_TASK_NONE = 1
internal const val WOM_TASK_BED = 2
internal const val WOM_TASK_BED_KILLED = 3

class WiseOldMan
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.wom_multi") { startDialogue(it.npc) { wiseOldMan() } }
        onOpNpc1("npc.wom_bed_inactive") { kickBed(it.npc) }
        onOpLoc1("loc.wom_bookshelf_old_tall_double") { searchBookshelf() }
        onOpLoc1("loc.wise_old_man_telescope") { observeTelescope() }
    }

    private suspend fun Dialogue.wiseOldMan() {
        chatNpc(happy, "Greetings, ${player.displayName}.")
        if (player.womTask == 0) return firstMeeting()
        if (handIn()) return
        val task = player.womTask
        val taskOption =
            if (task == WOM_TASK_NONE) "Is there anything I can do for you?" to WomTopic.Favour
            else "What did you ask me to do?" to WomTopic.Remind
        val topic =
            menu(
                taskOption,
                JUNK_OPTION to WomTopic.Junk,
                "I'd just like to ask you something." to WomTopic.Ask,
                title = WOM_MENU,
            )
        when (topic) {
            WomTopic.Favour -> {
                chatPlayer(happy, "Is there anything I can do for you?")
                assignTask()
            }
            WomTopic.Remind -> {
                chatPlayer(quiz, "What did you ask me to do?")
                repeatTask()
            }
            WomTopic.Ask -> askSomething()
            WomTopic.Junk -> checkJunk()
            else -> Unit
        }
    }

    private suspend fun Dialogue.firstMeeting() {
        chatPlayer(quiz, "So you're a wise old man, huh?")
        chatNpc(angry, "Less of the 'old' man, if you please!")
        chatNpc(neutral, "But yes, I suppose you could say that. I prefer to think of myself as a sage.")
        chatPlayer(quiz, "So what's a sage doing here?")
        chatNpc(
            neutral,
            "I've spent most of my life studying this world in which we live. I've strode " +
                "through the depths of the deadliest dungeons, roamed the murky jungles of " +
                "Karamja, meditated on the glories of Saradomin on Entrana,",
        )
        chatNpc(neutral, "and read some dusty tomes in the Library of Varrock.")
        chatNpc(neutral, "Now I'm not as young as I used to be, I'm settling here where it's peaceful.")
        if (!QuestRequirements.hasCompleted(player, VAMPYRE_SLAYER)) {
            chatNpc(
                worried,
                "It's a pity about that vampyre that keeps attacking the village. At least " +
                    "Saradomin protects me.",
            )
        }
        chatPlayer(happy, "That's quite an exciting life you've had.")
        chatNpc(neutral, "Exciting? Yes, I suppose so.")
        chatNpc(
            happy,
            "Now I'm here, perhaps I could offer you the benefit of my experience and wisdom?",
        )
        chatPlayer(happy, "Thanks! So you can help me?")
        chatNpc(
            neutral,
            "Well, I imagine you've gathered up quite a lot of stuff on your travels. Things you " +
                "used for quests a long time ago that you don't need now.",
        )
        player.womTask = WOM_TASK_NONE
        chatNpc(
            neutral,
            "If you like, I can look through your bank and see if there's anything you can chuck " +
                "away.",
        )
        chatNpc(
            neutral,
            "Alternatively, you can bring items here and show them to me. If I see that it's " +
                "something you don't need, I'll let you know. I might even be willing to buy it.",
        )
        chatPlayer(quiz, "So you'll help me clear junk out of my bank?")
        chatNpc(
            happy,
            "Yes, that's right. Or I'd be happy to chat with you about the wonders of this world!",
        )
        val topic =
            menu(
                "Could I have some free stuff, please?" to WomTopic.FreeStuff,
                "I'd just like to ask you something." to WomTopic.Ask,
                "Is there anything I can do for you?" to WomTopic.Favour,
                JUNK_OPTION to WomTopic.Junk,
                "Thanks, maybe some other time." to WomTopic.Leave,
            )
        when (topic) {
            WomTopic.FreeStuff -> {
                chatPlayer(quiz, "Could I have some free stuff, please?")
                chatNpc(sad, "Deary deary me...")
                chatNpc(
                    neutral,
                    "I'm not giving out free money, but I'd be happy to reward you if you'll do " +
                        "a little job for me.",
                )
                if (menu("Ok, what do you want me to do?" to true, "Thanks, maybe some other time." to false)) {
                    chatPlayer(quiz, "Ok, what do you want me to do?")
                    assignTask()
                } else {
                    chatPlayer(neutral, "Thanks, maybe some other time.")
                }
            }
            WomTopic.Ask -> askSomething()
            WomTopic.Junk -> checkJunk()
            WomTopic.Favour -> {
                chatPlayer(happy, "Is there anything I can do for you?")
                assignTask()
            }
            else -> chatPlayer(neutral, "Thanks, maybe some other time.")
        }
    }

    private suspend fun Dialogue.checkJunk() {
        chatPlayer(happy, JUNK_OPTION)
        access.openRecyclingCentre()
    }

    private suspend fun Dialogue.assignTask() {
        chatNpc(
            happy,
            "I'm sure I can think of a few little jobs. This won't be a quest, mind you, just a " +
                "little favour...",
        )
        if (access.random.of(0, BED_TASK_ODDS - 1) == 0) {
            player.womTask = WOM_TASK_BED
            player.womNumber = 0
        } else {
            val task = WomTask.entries[access.random.of(0, WomTask.entries.size - 1)]
            player.womTask = task.id
            player.womNumber = access.random.of(MIN_TASK_COUNT, MAX_TASK_COUNT)
        }
        repeatTask()
    }

    private suspend fun Dialogue.repeatTask() {
        if (player.womTask == WOM_TASK_BED) {
            chatNpc(
                worried,
                "Well, this is rather embarrassing, but I think there's some kind of monster in " +
                    "my house. Could you go upstairs and get rid of it, please?",
            )
            if (menu("Where do I need to go?" to true, SEE_YOU_LATER to false, title = WOM_MENU)) {
                chatPlayer(quiz, "Where do I need to go?")
                chatNpc(neutral, "I think it's somewhere upstairs. It kept me awake all last night.")
            }
            chatPlayer(happy, SEE_YOU_LATER)
            return
        }
        val task = WomTask.byId(player.womTask) ?: return
        chatNpc(happy, "${task.request} Please bring me ${player.womNumber}.")
        if (menu("Where can I get that?" to true, SEE_YOU_LATER to false, title = WOM_MENU)) {
            chatPlayer(quiz, "Where can I get that?")
            chatNpc(neutral, task.where)
        }
        chatPlayer(happy, SEE_YOU_LATER)
    }

    private suspend fun Dialogue.handIn(): Boolean {
        if (player.womTask == WOM_TASK_BED_KILLED) {
            chatPlayer(happy, "I've killed a creature that was under your bed.")
            chatNpc(happy, "Ah, thank you very much! Now I shall be able to sleep in peace.")
            chatNpc(happy, "Allow me to offer you an appropriate reward for your assistance...")
            access.statAdvance(HITPOINTS, access.random.of(280, 300).toDouble())
            player.womTask = WOM_TASK_NONE
            mesbox("The Wise Old Man waves his arms at you. You gain some Hitpoints xp.")
            return true
        }
        val task = WomTask.byId(player.womTask) ?: return false
        val inv = access.inv
        val carried = inv.count(task.obj)
        if (carried == 0) return false
        val needed = player.womNumber
        if (carried < needed) {
            chatPlayer(happy, "I've got some of the stuff you wanted.")
            access.invDel(inv, task.obj, carried)
            player.womNumber = needed - carried
            chatNpc(happy, "Ahh, you are very kind.")
            chatPlayer(happy, "I'll come back when I've got the rest.")
            return true
        }
        chatPlayer(happy, "I've got all the stuff you asked me to fetch.")
        if (inv.isFull() && !task.freesSpace(carried, needed)) {
            chatNpc(
                neutral,
                "I'd give you a reward, but you don't seem to have any space for it. Come back " +
                    "when you do.",
            )
            return true
        }
        access.invDel(inv, task.obj, needed)
        player.womTask = WOM_TASK_NONE
        player.womNumber = 0
        reward(task.hard)
        return true
    }

    private fun WomTask.freesSpace(carried: Int, needed: Int): Boolean =
        carried == needed || !isStackable()

    private fun WomTask.isStackable(): Boolean =
        this == WomTask.BronzeArrowtips ||
            this == WomTask.BronzeKnife ||
            this == WomTask.IronArrowtips ||
            this == WomTask.IronKnife ||
            this == WomTask.HeadlessArrow ||
            this == WomTask.Feather ||
            this == WomTask.BronzeArrow

    private suspend fun Dialogue.reward(hard: Boolean) {
        val roll = access.random.of(0, 15)
        val prayerSlots = if (player.basePrayerLvl >= MIN_PRAYER_REWARD_LEVEL) 7 else 0
        val coinSlots = if (prayerSlots == 0) 9 else 2
        when {
            roll < prayerSlots -> {
                val xp = if (hard) access.random.of(370, 430) else access.random.of(185, 215)
                chatNpc(happy, "Thank you, thank you! In thanks, I shall bestow on you a simple blessing.")
                access.statAdvance(PRAYER, xp.toDouble())
                mesbox("The Wise Old Man blesses you.<br>You gain some Prayer xp.")
            }
            roll < prayerSlots + coinSlots -> {
                val coins = if (hard) access.random.of(990, 1020) else access.random.of(180, 220)
                access.invAdd(access.inv, "obj.coins", coins)
                objbox("obj.coins_250", "<col=000080>The Wise Old Man gives you some coins.")
                chatNpc(happy, "Thank you, thank you! Please take this money as a sign of my gratitude.")
            }
            roll < prayerSlots + coinSlots + 2 -> rewardRunes(hard)
            roll < prayerSlots + coinSlots + 4 -> rewardHerbs(hard)
            roll < prayerSlots + coinSlots + 6 -> rewardSeeds()
            else -> rewardGem()
        }
    }

    private suspend fun Dialogue.rewardRunes(hard: Boolean) {
        val sets = access.random.of(1, if (hard) 25 else 10)
        val (rune, perSet) = RUNE_SETS[access.random.of(0, RUNE_SETS.size - 1)]
        access.invAdd(access.inv, rune, sets * perSet)
        doubleobjbox("obj.naturerune", "obj.waterrune", "<col=000080>The Wise Old Man gives you some runes.")
        chatNpc(happy, "Thank you, thank you! Please take these runes as a sign of my gratitude.")
    }

    private suspend fun Dialogue.rewardHerbs(hard: Boolean) {
        val herb = NOTED_HERBS[access.random.of(0, NOTED_HERBS.size - 1)]
        access.invAdd(access.inv, herb, access.random.of(1, if (hard) 10 else 3))
        objbox(
            "obj.unidentified_tarromin",
            "<col=000080>The Wise Old Man gives you some backnotes that can be exchanged for herbs.",
        )
        chatNpc(happy, "Thank you, thank you! Please take these herbs as a sign of my gratitude.")
    }

    private suspend fun Dialogue.rewardSeeds() {
        val (seed, max) = REWARD_SEEDS[access.random.of(0, REWARD_SEEDS.size - 1)]
        access.invAdd(access.inv, seed, access.random.of(1, max))
        objbox("obj.potato_seed", "<col=000080>The Wise Old Man gives you some seeds.")
        chatNpc(happy, "Thank you, thank you! Please take these seeds as a sign of my gratitude.")
    }

    private suspend fun Dialogue.rewardGem() {
        chatNpc(happy, "Thank you, thank you! Please take this gem as a sign of my gratitude.")
        var pick = access.random.of(0, REWARD_GEMS.sumOf { it.third } - 1)
        val gem = REWARD_GEMS.first { (_, _, weight) -> (pick < weight).also { pick -= weight } }
        access.invAdd(access.inv, gem.first)
        objbox(gem.first, "<col=000080>The Wise Old Man gives you ${gem.second}")
    }

    private suspend fun ProtectedAccess.kickBed(bed: Npc) {
        faceEntitySquare(bed)
        anim(KICK_SEQ)
        soundSynth(KICK_SYNTH)
        bed.anim(BED_BLOCK_SEQ)
        if (player.womTask != WOM_TASK_BED) return
        delay(1)
        npcRepo.del(bed, THING_DURATION)
        val thing = Npc(THING_UNDER_THE_BED, bed.coords)
        npcRepo.add(thing, THING_DURATION)
        thing.opPlayer2(player, aiPlayerInteractions)
    }

    private suspend fun ProtectedAccess.searchBookshelf() {
        arriveDelay()
        if (player.womTask == 0) {
            startDialogue {
                chatNpcSpecific(
                    "Wise Old Man",
                    "npc.wise_old_man",
                    angry,
                    "It'd be politer to come and talk to me before you start messing around " +
                        "with my possessions!",
                )
            }
            return
        }
        startDialogue { mesbox("The bookshelf contains lots of antiquarian books.") }
        for (book in SHELF_BOOKS) {
            if (inv.count(book) == 0 && !inv.isFull()) invAdd(inv, book)
        }
    }

    private suspend fun ProtectedAccess.observeTelescope() {
        arriveDelay()
        startDialogue {
            chatPlayer(quiz, "I see you've got your telescope pointing at the Wizards' Tower.")
            womSays(quiz, "Oh, do I? Well, why does that interest you?")
            if (player.bankJob >= BANKJOB_WATCHED) {
                chatPlayer(
                    angry,
                    "Well, you robbed a bank, and I bet you're now planning something to do with " +
                        "that Tower!",
                )
                womSays(neutral, "No, no. I'm not planning anything like that again.")
                chatPlayer(angry, "Well I'll be watching you...")
                return@startDialogue
            }
            chatPlayer(neutral, "It just seems a bit odd.")
            womSays(
                neutral,
                "Odd? There's nothing odd about a fascination with the magical arts. Besides, the " +
                    "architecture of that Tower is truly remarkable, for one who studies such " +
                    "things.",
            )
            chatPlayer(quiz, "So you're not planning to attack the Tower?")
            womSays(shocked, "No, I wouldn't dream of doing anything of the sort!")
            chatPlayer(neutral, "Hmmm...")
        }
    }

    private suspend fun Dialogue.womSays(
        mesanim: MesAnimType,
        text: String,
    ) = chatNpcSpecific("Wise Old Man", "npc.wise_old_man", mesanim, text)

    private suspend fun Dialogue.askSomething() {
        chatPlayer(quiz, "I'd just like to ask you something.")
        chatNpc(happy, "Please do!")
        loreCategories()
    }

    private suspend fun Dialogue.loreCategories() {
        val category =
            menu(
                "Distant lands" to 0,
                "Strange beasts" to 1,
                "Days gone by" to 2,
                "Gods and demons" to 3,
                "Something to do with you..." to 4,
            )
        val keepTalking =
            when (category) {
                0 -> distantLands()
                1 -> strangeBeasts()
                2 -> daysGoneBy()
                3 -> godsAndDemons()
                else -> aboutYou()
            }
        if (keepTalking) anythingElse()
    }

    private suspend fun Dialogue.anythingElse(
        prompt: String = "Is there anything else you'd like to ask?"
    ) {
        chatNpc(neutral, prompt)
        if (menu("Yes please." to true, "Thanks, maybe some other time." to false)) {
            chatPlayer(happy, "Yes please.")
            loreCategories()
        } else {
            chatPlayer(neutral, "Thanks, maybe some other time.")
            chatNpc(neutral, "As you wish. Farewell, ${player.displayName}.")
        }
    }

    private suspend fun Dialogue.distantLands(): Boolean {
        when (menu("The Wilderness" to 0, "Misty jungles" to 1, "Underground domains" to 2, "Mystical realms" to 3)) {
            0 -> {
                chatPlayer(quiz, "Could you tell me about the Wilderness, please?")
                wom(
                    "If Entrana is a land dedicated to the glory of Saradomin, the Wilderness is " +
                        "surely the land of Zamorak.",
                    "It's a dangerous place, where adventurers such as yourself may attack each " +
                        "other, using all their combat skills in the struggle for survival.",
                    "The Wilderness has different levels. In a low level area, you can only fight " +
                        "adventurers whose combat level is close to yours.",
                    "But if you venture into the high level areas in the far north, you can be " +
                        "attacked by adventurers who are significantly stronger than you.",
                    "Of course, you'd be able to attack considerably weaker people too, so it can " +
                        "be worth the risk.",
                    "If you dare to go to the far north-west of the Wilderness, there's a building " +
                        "called the Mage Arena where you can learn to summon the power of " +
                        "Saradomin himself!",
                )
            }
            1 -> {
                chatPlayer(quiz, "What can you tell me about jungles?")
                wom(
                    "If it's jungle you want, look no further than the southern regions of Karamja.",
                    "Once you get south of Brimhaven, the whole island is pretty much covered in " +
                        "exotic trees, creepers and shrubs.",
                    "There's a small settlement called Tai Bwo Wannai Village in the middle of " +
                        "the island. It's a funny place; the chieftain's an unfriendly chap and " +
                        "his sons are barking mad.",
                    "Honestly, one of them asked me to stuff a dead monkey with seaweed so he " +
                        "could EAT it!",
                )
                if (completed(TAI_BWO_WANNAI_TRIO)) chatPlayer(neutral, "Yes, I've met them.")
                wom(
                    "Further south you'll find Shilo Village. It's been under attack by " +
                        "terrifying zombies in recent months, if my sources are correct."
                )
                if (completed(SHILO_VILLAGE)) {
                    chatPlayer(
                        neutral,
                        "I've dealt with them, although there are a few still knocking about.",
                    )
                }
                wom(
                    "The jungle's filled with nasty creatures. There are vicious spiders that you " +
                        "can hardly see before they try to bite your legs off, and great big " +
                        "jungle ogres."
                )
            }
            2 -> {
                chatPlayer(quiz, "Tell me about what's underground.")
                wom(
                    "Oh, the dwarven realms?",
                    "Yes, there was a time, back in the Fourth Age, when we humans wouldn't have " +
                        "been able to venture underground. That was before we had magic; the " +
                        "dwarves were quite a threat.",
                    "Still, it's much more friendly now. You can visit the vast dwarven mine if " +
                        "you like; the entrance is on the mountain north of Falador.",
                    "If you go further west you may be able to visit the dwarven city of " +
                        "Keldagrim. But they were a bit cautious about letting humans in, last " +
                        "time I asked.",
                    "On the other hand, if you go west of Brimhaven, you'll find a huge " +
                        "underground labyrinth full of giants, demons, dogs and dragons to fight. " +
                        "It's even bigger than the caves under Taverley, although the Taverley",
                    "dungeon's pretty good for training your combat skills.",
                )
            }
            else -> {
                chatPlayer(quiz, "What mystical realms can I visit?")
                wom(
                    "Well, you've been to Zanaris. I met a fairy years ago who said she lived " +
                        "there. She was an incredible warrior, considering that she wasn't even " +
                        "a metre tall.",
                    "Also, in my research I came across ancient references to some kind of Abyss. " +
                        "Demons from the Abyss have already escaped into this land; Saradomin be " +
                        "thanked that they are very rare!",
                )
                if (completed(SHADOW_OF_THE_STORM)) {
                    chatPlayer(
                        neutral,
                        "I've been through a portal into the lair of some ancient demon. It was " +
                            "in the city of Uzer.",
                    )
                    wom(
                        "Ah, Uzer. A city much favoured by Saradomin, destroyed by Thammaron, the " +
                            "elder-demon, late in the Third Age."
                    )
                    chatPlayer(neutral, "Yeah, there isn't much left there.")
                }
            }
        }
        return true
    }

    private suspend fun Dialogue.strangeBeasts(): Boolean {
        val topic =
            menu(
                "Biggest & Baddest" to 0,
                "Poison and how to survive it" to 1,
                "Wealth through slaughter" to 2,
                "Random events" to 3,
            )
        when (topic) {
            0 -> {
                chatPlayer(quiz, "What's the biggest monster in the world?")
                wom(
                    "There's a mighty fire-breathing dragon living underground in the deep " +
                        "Wilderness, known as the King Black Dragon. It's a fearsome beast, with " +
                        "a breath that can poison you, freeze you to the ground or",
                    "incinerate you where you stand.",
                    "But even more deadly is the Queen of the Kalphites. As if her giant " +
                        "mandibles of death were not enough, she also throws her spines at her " +
                        "foes with deadly force. She can even cast rudimentary spells.",
                    "Some dark power must be protecting her, for she can block attacks using " +
                        "prayer just as humans do.",
                    "Another beast that's worthy of a special mention is the Shaikahan. It dwells " +
                        "in the eastern reaches of Karamja, and is almost impossible to kill " +
                        "except with specially prepared weapons.",
                )
                if (completed(TAI_BWO_WANNAI_TRIO)) {
                    chatPlayer(
                        neutral,
                        "Actually, that one's dead. I've been helping a barmy hunter who'd sworn " +
                            "to kill it.",
                    )
                }
            }
            1 -> {
                chatPlayer(quiz, "What does poison do?")
                wom(
                    "Many monsters use poison against their foes. If you get poisoned, you will " +
                        "not feel it at the time, but later you will begin to suffer its " +
                        "effects, and your life will drain slowly from you.",
                    "Over time the effects dwindle to nothing, but if you had already been " +
                        "wounded you might die before they wear off completely.",
                    "Fortunately, followers of Guthix have devised potions that can cure the " +
                        "poison or even give immunity to it.",
                    "Should you wish to use poison against your own enemies and those of our Lord " +
                        "Saradomin, there is a potion that you can smear on your daggers, " +
                        "arrows, spears, javelins and throwing knives.",
                )
            }
            2 -> {
                chatPlayer(quiz, "What monsters drop good items?")
                wom(
                    "As a general rule, tougher monsters drop more valuable items. But even a " +
                        "lowly hobgoblin can drop valuable gems; it just does this extremely " +
                        "rarely.",
                    "If you can persuade the Slayer Masters to train you as a Slayer, you will be " +
                        "able to fight certain monsters that drop valuable items far more often.",
                    "You might care to invest in an enchanted dragonstone ring. These are said to " +
                        "make a monster drop its most valuable items a little more often.",
                )
            }
            else -> {
                chatPlayer(
                    quiz,
                    "What are these strange monsters that keep appearing out of nowhere and " +
                        "attacking me when I'm training?",
                )
                wom(
                    "Ah, I imagine you see a lot of those.",
                    "Creatures such as the rock golem, river troll and tree spirit dwell in " +
                        "places where adventurers frequently go to train their skills. While " +
                        "you're training you will often disturb one by accident. It will then " +
                        "get angry and",
                    "attack you. The safest way to deal with them is to run away immediately, " +
                        "but they sometimes drop valuable items if you kill them.",
                )
            }
        }
        return true
    }

    private suspend fun Dialogue.daysGoneBy(): Boolean {
        val topic =
            menu(
                "Heroic figures" to 0,
                "The origin of magic" to 1,
                "Settlements" to 2,
                "The Wise Old Man of Draynor Village" to 3,
            )
        when (topic) {
            0 -> {
                chatPlayer(happy, "Tell me about valiant heroes!")
                wom(
                    "Ha ha ha... There are plenty of heroes. Always have been, always will be, " +
                        "until the fall of the world."
                )
                if (player.questPoints < NOTED_ADVENTURER_QP) {
                    wom(
                        "If you'd do a few more quests, you'd soon become a fairly noted " +
                            "adventurer yourself."
                    )
                } else {
                    wom("You're quite a noted adventurer yourself!")
                }
                wom(
                    "But I suppose I could tell you of a couple...",
                    "Yes, there was a man called Arrav. No-one knew where he came from, but he " +
                        "was a fearsome fighter, a skillful hunter and a remarkable farmer. He " +
                        "lived in the ancient settlement of Avarrocka, defending it from",
                    "goblins, until he went forth in search of some strange artefact long " +
                        "desired by the dreaded Mahjarrat.",
                    "Perhaps some day I shall be able to tell you what became of him.",
                    "But do not let your head be turned by heroics. Randas was another great man, " +
                        "but he let himself be beguiled into turning to serve Zamorak, and they " +
                        "say he is now a mindless creature deep in the Underground Pass that",
                    "leads to Isafdar.",
                )
            }
            1 -> {
                chatPlayer(quiz, "Where did humans learn to use magic?")
                wom(
                    "Ah, that was quite a discovery! It revolutionised our way of life and jolted " +
                        "us into this Fifth Age of the world.",
                    "They say a traveller in the north discovered the key, although no records " +
                        "state exactly what he found. From this he was able to summon the magic " +
                        "of the four elements, using magic as a tool and a weapon. He and",
                    "his followers learnt how to bind the power into little stones so that others " +
                        "could use it.",
                    "in the land south of here they constructed an immense tower where the power " +
                        "could be studied, but followers of Zamorak destroyed it with fire many " +
                        "years ago, and most of the knowledge was lost.",
                )
                if (completed(RUNE_MYSTERIES)) {
                    wom(
                        "I hear you've been very helpful to the wizards who are working on the " +
                            "mysteries of the rune stones."
                    )
                    chatPlayer(neutral, "Well, I didn't do much...")
                } else {
                    wom("Perhaps one day those lost secrets will be uncovered once more.")
                }
            }
            2 -> {
                chatPlayer(quiz, "I suppose you'd know about the history of today's cities?")
                wom(
                    "Yes, there are fairly good records of the formation of the cities from " +
                        "primitive settlements.",
                    "In the early part of the Fourth Age, of course, there were no permanent " +
                        "settlements. Tribes wandered the lands, staying where they could until " +
                        "the resources were exhausted.",
                    "This changed as people learnt to grow crops and breed animals, and now there " +
                        "are very few of the old nomadic tribes. There's at least one tribe " +
                        "roaming between the Troll Stronghold and Rellekka, though.",
                )
                if (completed(MOUNTAIN_DAUGHTER)) {
                    chatPlayer(neutral, "Yes, I've met them. They're all mad.")
                    wom(
                        "Truly, their faith in natural spirits is rather foolish, but they're a " +
                            "pleasant enough people."
                    )
                }
                wom(
                    "Anyway, what was I saying? Ah, yes...",
                    "One settlement was Avarrocka, a popular trading centre.",
                    "In the west, Ardougne gradually formed under the leadership of the " +
                        "Carnillean family, despite the threat of the Mahjarrat warlord Hazeel " +
                        "who dwelt in that area until his downfall.",
                )
            }
            else -> {
                chatPlayer(quiz, "Tell me about yourself, old man.")
                wom(
                    "Ah, so you want to know about me, eh?",
                    "Mmm... what could I say about myself? Let's see what I've done...",
                    "I've delved into the dungeon west of Brimhaven and heard the terrifying " +
                        "CRASH of the steel dragons battling each other for territory.",
                    "I spent some years on Entrana, where I learnt the techniques of pure " +
                        "meditation.",
                    "I've wandered through the vast desert that lies south of Al Kharid and seen " +
                        "the great walls of Menaphos and Sophanem.",
                )
                if (completed(ICTHLARINS_LITTLE_HELPER)) sphinxTalk()
                wom(
                    "Apart from all that, I've spent many a happy hour in dusty libraries, " +
                        "searching through ancient scrolls and texts for the wisdom of those who " +
                        "have passed on."
                )
                anythingElse(
                    "Plus plenty of other adventures, quests, journeys... Is there anything else " +
                        "you'd like to know?"
                )
                return false
            }
        }
        return true
    }

    private suspend fun Dialogue.sphinxTalk() {
        chatPlayer(
            quiz,
            "That feline statue you've got on your bookshelf there - I'm sure I saw something " +
                "like that when I was in Sophanem.",
        )
        wom(
            "I've always liked to collect little trinkets to remind me of the time when my life " +
                "was more - ah - exciting."
        )
        chatPlayer(quiz, "So you've spoken to the Sphinx, then?")
        wom(
            "Spoken to a sphinx, you say? My dear young ${if (isLad()) "man" else "lady"}, you " +
                "can't possibly be serious!"
        )
        chatPlayer(quiz, "What do you mean?")
        wom(
            "Ha ha ha! The people who live deep in the shifting sands of the desert will often " +
                "tell travellers tall tales of strange beasts, but that doesn't mean any of it's " +
                "remotely true!"
        )
        chatPlayer(angry, "But I've met the Sph...")
        wom(
            "Please, adventurer, there is nothing to be gained by interfering in matters you do " +
                "not understand.",
            "Even if such a creature were to exist...",
        )
        chatPlayer(neutral, "... which it does...")
        wom(
            "EVEN IF SUCH A CREATURE WERE TO EXIST it would be terribly dangerous for you to get " +
                "involved in its business.",
            "I have heard of terrible things happening to travellers in that land, and I would " +
                "not wish for you to be dragged into anything like that.",
            "Indeed, I have heard of adventurers doing the most shameful and blasphemous deeds " +
                "believing themselves to be acting on behalf of some false god!",
        )
        chatPlayer(confused, "Hmmm?")
        wom(
            "So please do not speak to me of this matter again. Now, I believe I was telling you " +
                "of my adventurous youth..."
        )
    }

    private suspend fun Dialogue.godsAndDemons(): Boolean {
        val topic =
            menu(
                "Three gods?" to 0,
                "The wars of the gods" to 1,
                "The Mahjarrat" to 2,
                "Wielding the power of the gods" to 3,
            )
        when (topic) {
            0 -> return threeGods()
            1 -> {
                chatPlayer(happy, "I wanna know about the wars of the gods!")
                wom(
                    "Ah, that was a terrible time. The armies of Saradomin fought gloriously " +
                        "against the minions of Zamorak, but many brave warriors and noble " +
                        "cities were overthrown and destroyed utterly."
                )
                if (completed(SHADOW_OF_THE_STORM)) {
                    wom(
                        "You have visited Uzer, and seen for yourself the damage that could be " +
                            "wrought by an elder-demon in its wrath."
                    )
                }
                chatPlayer(quiz, "How did it all end?")
                wom(
                    "Before the Zamorakian forces could be utterly routed, Lord Saradomin took " +
                        "pity on them and the battle- scarred world, and allowed a truce."
                )
            }
            2 -> {
                chatPlayer(quiz, "What are the Mahjarrat?")
                wom(
                    "Very little is written about the tribe of the Mahjarrat. They are believed " +
                        "to be from the realm of Freneskae, or Frenaskrae - the spelling in this " +
                        "tongue is only approximate.",
                    "One of them, the foul Zamorak, has achieved godhood, although none knows " +
                        "how this came about.",
                    "Other Mahjarrat who have been particularly active upon this plane are " +
                        "Hazeel, Lucien, Azzanadra and Zemouregal.",
                )
                if (completed(DESERT_TREASURE)) {
                    chatPlayer(
                        neutral,
                        "Azzanadra taught me some ancient magical powers after I unlocked the " +
                            "pyramid where he had been trapped.",
                    )
                    wom("Leave me, traveller; I am old, and your words fill me with horror.")
                    return false
                }
            }
            else -> {
                chatPlayer(quiz, "Can I wield the power of Saradomin myself?")
                if (completed(MAGE_ARENA)) {
                    wom("I see you are already learning to summon the power of Saradomin.")
                } else {
                    wom(
                        "If you travel to the Mage Arena in the north-west reaches of the " +
                            "Wilderness, the battle mage Kolodion may be willing to let you learn " +
                            "to summon the power of Saradomin, should you be able to pass his test."
                    )
                }
            }
        }
        return true
    }

    private suspend fun Dialogue.threeGods(): Boolean {
        chatPlayer(neutral, "I heard that Gielinor has three gods.")
        wom(
            "Indeed. This is correct: Saradomin, Guthix and Zamorak.",
            "Saradomin, the great and glorious, gives life to this world.",
            "Zamorak craves only death and destruction.",
            "Guthix, calling itself a god of 'balance', holds no allegiance, but simply aids " +
                "whatever cause suits its shifting purpose.",
        )
        if (completed(DIG_SITE)) {
            chatPlayer(quiz, "So who's Zaros?")
            wom("What did you say?")
            chatPlayer(neutral, "I discovered a buried altar to a god called Zaros.")
            wom("Please, do not speak further of this. No good can come of meddling in such things.")
            chatPlayer(quiz, "So Zaros is real?")
            wom(
                "Adventurer, my patience is limited. Speak to me no further of this - this - this " +
                    "foolishness."
            )
            if (completed(DESERT_TREASURE)) {
                chatPlayer(
                    quiz,
                    "Does that mean I shouldn't ask you about the ancient magicks I learnt in " +
                        "that pyramid north of Sophanem?",
                )
                chatNpc(angry, "ENOUGH! Begone from me, you foul trickster!")
                return false
            }
        }
        if (completed(ICTHLARINS_LITTLE_HELPER)) {
            chatPlayer(quiz, "How about Tumeken, Elindis, Icthlarin and the Devourer?")
            wom("What names are these?")
            chatPlayer(
                neutral,
                "Down in Sophanem, they've got a chief god named Tumeken, plus a fertility " +
                    "goddess named Elindis. Icthlarin is their god of the dead and the Devourer...",
            )
            wom("Adventurer, what folly leads you to think any of these - things, exist?")
            chatPlayer(angry, "But I've met Icthlarin! He'd got a jackal's head!")
            wom(
                "It is hot in that land, and the heat can affect a traveller in strange ways. " +
                    "Tell me, when you met 'Icthlarin', had you been feeling at all unwell?"
            )
            chatPlayer(
                neutral,
                "Well, now you mention it, I'd just been hypnotised by a weird woman I met in " +
                    "the desert.",
            )
            wom(
                "So a strange woman interfered with your mind, then you met a man with the head " +
                    "of a jackal, and you decided he must be a god?"
            )
            chatPlayer(confused, "Umm...")
            wom(
                "Well, make sure you don't get hypnotised again, and I'm sure you'll not meet any " +
                    "more jackal-headed men claiming to be gods!"
            )
            chatPlayer(confused, "But...?")
            wom("It'd probably be best if you don't go there again.")
            chatPlayer(
                quiz,
                "But you've been all around that area, haven't you? When you were travelling?",
            )
            wom("Indeed, I have.")
            chatPlayer(quiz, "So you'd know all about their gods?")
            wom(
                "I know what I know.",
                "And knowing what I know, I do not speak of this matter. Perhaps you would be " +
                    "wise to heed my example.",
            )
        }
        return true
    }

    private suspend fun Dialogue.aboutYou(): Boolean {
        val swanSong = completed(SWAN_SONG)
        val options = buildList {
            add("Your hat!" to 0)
            if (swanSong) {
                add("That quest we did together..." to 1)
                add("Your retirement" to 2)
            }
        }
        when (menu(options)) {
            0 -> return hatTalk()
            1 -> {
                chatPlayer(happy, "That was quite an exciting quest we did together!")
                wom(
                    "Oh yes, it was certainly a fitting end to my adventuring days. Your fight " +
                        "against the Sea Troll Queen was highly impressive!"
                )
                chatPlayer(quiz, "Are you tempted to go on another quest in the future?")
                wom(
                    "No, I feel I am a bit old for that kind of thing now. New times need new " +
                        "heroes, and I'm sure you'll always be there to answer the call!"
                )
                chatPlayer(happy, "I'll certainly see what I can do!")
            }
            else -> {
                retirementTalk()
                anythingElse("Perhaps not... But enough of this matter! Is there anything else you'd like to ask?")
                return false
            }
        }
        return true
    }

    private suspend fun Dialogue.hatTalk(): Boolean {
        chatPlayer(quiz, "I want to ask you about your hat.")
        wom("Why, thank you! I rather like it myself.")
        if (player.bankJob >= BANKJOB_WATCHED) {
            hatAccusation()
        } else {
            chatPlayer(quiz, "Did you steal it from the bank?")
            wom("Me? Steal from the bank? Whatever do you mean?")
            chatPlayer(angry, "There's been a break-in at Draynor Bank, and I bet you did it.")
            wom("Oh my goodness, what in the world could make you think I did such a thing?")
            chatPlayer(
                angry,
                "Well, you've been sitting here looking at the bank ever since you moved into " +
                    "this house. Now someone's robbed the bank, and suddenly your house is full " +
                    "of expensive stuff!",
            )
            wom(
                "Oh dear, oh dear...",
                "So what do you think I did? March across the street, smash the bank wall, knock " +
                    "out the bankers and steal all the money?",
                "I'm an old man! I walk with a stick! How could I possibly have done that?",
            )
            chatPlayer(quiz, "So you didn't rob the bank?")
            wom(
                "Just run along now, be a good ${if (isLad()) "lad" else "lass"}, and don't worry " +
                    "yourself about me or my finances!"
            )
            chatPlayer(neutral, "Hmmm... But I'll be keeping an eye on you.")
        }
        var prompt = "Now you've got that off your chest, would you like to ask me about anything else?"
        var askedHat = false
        var askedGiveBack = false
        while (true) {
            chatNpc(neutral, prompt)
            val options = buildList {
                if (!askedHat) add("How can I get a hat like that?" to 0)
                if (!askedGiveBack) add("You should give it back, you know." to 1)
                add("Yes please." to 2)
                add("Thanks, maybe some other time." to 3)
            }
            when (menu(options)) {
                0 -> {
                    askedHat = true
                    chatPlayer(quiz, "How can I get a hat like that?")
                    wom(
                        "You could buy one off another player, or wait until they're next made " +
                            "available by the Council."
                    )
                    chatPlayer(quiz, "Can I buy your hat?")
                    prompt =
                        "Ohhh no, I don't intend to part with this. Would you like to ask me " +
                            "about something else?"
                }
                1 -> {
                    askedGiveBack = true
                    chatPlayer(neutral, "You should give it back, you know.")
                    wom("No, I think I'll keep it.")
                    chatPlayer(angry, "But...")
                    prompt =
                        "Now you've got that off your chest, would you like to ask me about " +
                            "anything else?"
                }
                2 -> {
                    chatPlayer(happy, "Yes please.")
                    loreCategories()
                    return false
                }
                else -> {
                    chatPlayer(neutral, "Thanks, maybe some other time.")
                    chatNpc(neutral, "As you wish. Farewell, ${player.displayName}.")
                    return false
                }
            }
        }
    }

    private suspend fun Dialogue.hatAccusation() {
        chatPlayer(
            angry,
            "You stole it off that woman in the bank! You stole all this valuable stuff too!",
        )
        wom("Stole it? How could you possibly think I did such a thing?")
        chatPlayer(angry, "I saw you robbing the bank! You killed all those people!")
        wom(
            "Deary me, ${player.displayName}, your imagination is running wild! What could make " +
                "you think you saw me do that?"
        )
        chatPlayer(
            angry,
            "I've seen a security recording that shows you robbing the bank. The bank's guard " +
                "showed it to me.",
        )
        wom(
            "You've seen the bank's security recording?",
            "Tut tut tut... Oh well, at least you'll never be able to tell the bank about me. " +
                "They'll never listen.",
        )
        chatPlayer(quiz, "So you're just going to get away with it?")
        wom("That's my plan, yes.")
        chatPlayer(angry, "But that's... well, it's WRONG!")
        chatNpc(angry, "Wrong? WRONG? I'll tell you what's wrong!")
        chatNpc(
            angry,
            "I've spent my whole life travelling the world, doing quests for people, saving " +
                "lives, saving villages from terrifying monsters and all that sort of thing.",
        )
        chatNpc(
            angry,
            "Now I'm old, and where do I have to live? In this freezing old house next to a " +
                "pig-sty, with a bunch of yobs outside who can't keep their hands off the market " +
                "stalls! What sort of reward is that?",
        )
        chatNpc(angry, "So don't talk to me about right and wrong!")
        chatPlayer(angry, "Maybe someone SHOULD talk to you about right and wrong...")
        chatNpc(angry, "Bah!")
        chatPlayer(angry, "Hmmph!")
    }

    private suspend fun Dialogue.retirementTalk() {
        chatPlayer(
            quiz,
            "When we finished fighting those sea trolls, you said you'd finished with " +
                "adventuring, didn't you?",
        )
        wom(
            "Yes, that's right. I'm an old man, there's no denying it, and I need a nice quiet " +
                "retirement.",
            "Besides, I think you've proved that the new generation has its own heroes and " +
                "adventurers - I should step back and let you get on with it!",
        )
        chatPlayer(quiz, "Does this mean you're not going to do any more big robberies?")
        wom(
            "Well, since you saved my life when we were fighting the Sea Troll Queen, I don't " +
                "mind telling you... I trust you won't tell anyone about this?"
        )
        chatPlayer(quiz, "Go on...")
        wom("I might just have something in mind. After all, that little bank incident went very smoothly.")
        chatPlayer(shocked, "But you're retired!")
        wom(
            "I retired from adventuring and questing. The episode in the bank was hardly a " +
                "quest, more like a shopping trip!"
        )
        chatPlayer(
            angry,
            "Your shopping trip killed 3 bank staff and several innocent bystanders, not to " +
                "mention the watchman!",
        )
        chatNpc(
            angry,
            "I've saved countless villages from unspeakable terrors and slain innumerable evil " +
                "monsters - surely I deserve some sort of reward in return? Yet what did I get? " +
                "NOTHING!",
        )
        chatNpc(
            angry,
            "You yourself would get pretty angry if you did a long and dangerous quest and " +
                "didn't get any reward from it, eh? I'm sure you would!",
        )
        chatNpc(
            angry,
            "Well imagine how I felt at the end of a long life of questing when was left " +
                "penniless, and I had to move into this thief-infested village! This little shack " +
                "was falling apart when I arrived!",
        )
        wom(
            "You go speak to that hag who keeps looking through my window - she'll show you how " +
                "it used to look before I could afford to redecorate it!",
            "Being noble and virtuous simply doesn't put food on the table, and I don't intend to " +
                "sit in banks begging like so many people do these days.",
        )
        chatPlayer(
            quiz,
            "So all those quests in the past, when you saved people from monsters and stuff like " +
                "that... You only helped them because you wanted a reward?",
        )
        wom(
            "No, it wasn't always like that. I used to be satisfied just knowing that I'd helped, " +
                "and that was reward enough.",
            "But eventually I realised that people forgot about me as soon as the danger was " +
                "gone, and I wasn't even getting paid properly for my services. Sometimes they " +
                "offered training in the various skills, but that's no use to me!",
            "I eventually realised that the only way I could get a proper reward for my life's " +
                "efforts is if I took the reward myself. So I took up my cape and staff and " +
                "headed for the bank.",
        )
        chatPlayer(
            quiz,
            "And now you're planning to do another robbery? Didn't you get enough from the bank?",
        )
    }

    private suspend fun Dialogue.wom(vararg lines: String) {
        for (line in lines) chatNpc(neutral, line)
    }

    private fun Dialogue.completed(quest: String): Boolean =
        QuestRequirements.hasCompleted(player, quest)

    private fun Dialogue.isLad(): Boolean = player.appearance.bodyType == Constants.bodytype_a

    private enum class WomTopic {
        Favour,
        Remind,
        Ask,
        FreeStuff,
        Junk,
        Leave,
    }

    private companion object {
        const val WOM_MENU = "What would you like to say?"
        const val SEE_YOU_LATER = "Right, I'll see you later."
        const val JUNK_OPTION = "Could you check my items for junk, please?"

        const val MIN_TASK_COUNT = 3
        const val MAX_TASK_COUNT = 15
        const val BED_TASK_ODDS = 50
        const val MIN_PRAYER_REWARD_LEVEL = 3
        const val NOTED_ADVENTURER_QP = 80
        const val BANKJOB_WATCHED = 2

        const val HITPOINTS = "stat.hitpoints"
        const val PRAYER = "stat.prayer"

        const val KICK_SEQ = "seq.human_unarmedkick"
        const val KICK_SYNTH = "synth.human_unarmedkick"
        const val BED_BLOCK_SEQ = "seq.wom_bed_block"
        const val THING_UNDER_THE_BED = "npc.wom_bed_active"
        const val THING_DURATION = 200

        const val VAMPYRE_SLAYER = "quest_vampyreslayer"
        const val TAI_BWO_WANNAI_TRIO = "quest_taibwowannaitrio"
        const val SHILO_VILLAGE = "quest_shilovillage"
        const val SHADOW_OF_THE_STORM = "quest_shadowofthestorm"
        const val RUNE_MYSTERIES = "quest_runemysteries"
        const val MOUNTAIN_DAUGHTER = "quest_mountaindaughter"
        const val ICTHLARINS_LITTLE_HELPER = "quest_icthlarinslittlehelper"
        const val DIG_SITE = "quest_digsite"
        const val DESERT_TREASURE = "quest_deserttreasure"
        const val MAGE_ARENA = "quest_magearena1"
        const val SWAN_SONG = "quest_swansong"

        val SHELF_BOOKS = listOf("obj.wom_book_2", "obj.wom_chicken_book")

        val RUNE_SETS =
            listOf(
                "obj.chaosrune" to 1,
                "obj.naturerune" to 1,
                "obj.lawrune" to 1,
                "obj.firerune" to 1,
                "obj.mindrune" to 2,
                "obj.bodyrune" to 2,
                "obj.earthrune" to 2,
                "obj.waterrune" to 2,
                "obj.airrune" to 3,
            )

        val NOTED_HERBS =
            listOf(
                "obj.cert_unidentified_guam",
                "obj.cert_unidentified_marentill",
                "obj.cert_unidentified_tarromin",
                "obj.cert_unidentified_ranarr",
                "obj.cert_unidentified_harralander",
            )

        val REWARD_SEEDS =
            listOf(
                "obj.potato_seed" to 9,
                "obj.onion_seed" to 6,
                "obj.tomato_seed" to 4,
                "obj.cabbage_seed" to 3,
                "obj.marigold_seed" to 2,
                "obj.yanillian_hop_seed" to 6,
                "obj.cactus_seed" to 1,
                "obj.cadavaberry_bush_seed" to 1,
                "obj.hammerstone_hop_seed" to 9,
                "obj.jangerberry_bush_seed" to 1,
                "obj.barley_seed" to 13,
                "obj.nasturtium_seed" to 1,
                "obj.strawberry_seed" to 1,
                "obj.jute_seed" to 9,
                "obj.woad_seed" to 1,
                "obj.guam_seed" to 1,
                "obj.marrentill_seed" to 1,
                "obj.tarromin_seed" to 1,
                "obj.toadflax_seed" to 1,
                "obj.harralander_seed" to 1,
                "obj.watermelon_seed" to 2,
                "obj.rosemary_seed" to 1,
                "obj.krandorian_hop_seed" to 4,
                "obj.redberry_bush_seed" to 1,
                "obj.dwellberry_bush_seed" to 1,
                "obj.asgarnian_hop_seed" to 7,
                "obj.sweetcorn_seed" to 2,
                "obj.whiteberry_bush_seed" to 1,
                "obj.wildblood_hop_seed" to 3,
            )

        val REWARD_GEMS =
            listOf(
                Triple("obj.uncut_sapphire", "an Uncut sapphire.", 5),
                Triple("obj.uncut_opal", "an Uncut opal.", 5),
                Triple("obj.uncut_red_topaz", "an Uncut red topaz.", 2),
                Triple("obj.uncut_emerald", "an Uncut emerald!", 5),
                Triple("obj.uncut_ruby", "an Uncut ruby!", 4),
                Triple("obj.uncut_jade", "an Uncut jade.", 5),
                Triple("obj.uncut_diamond", "an Uncut diamond!", 1),
            )
    }
}
