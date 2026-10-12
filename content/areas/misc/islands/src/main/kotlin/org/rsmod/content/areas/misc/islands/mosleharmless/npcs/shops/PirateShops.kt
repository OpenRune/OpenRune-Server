package org.rsmod.content.areas.misc.islands.mosleharmless.npcs.shops

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Mos_Le%27Harmless: the inhabitants only speak plainly to a
// player carrying the Book o' Piracy, and trading needs it too.
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PirateShops
@Inject
constructor(private val shops: Shops, private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(CHARLEY) { talk(it.npc) { charley(it.npc) } }
        onOpNpc3(CHARLEY) { trade(it.npc, CHARLEY_SHOP, CHARLEY_INV) }
        onOpNpc1(MIKE) { talk(it.npc) { mike(it.npc) } }
        onOpNpc3(MIKE) { trade(it.npc, MIKE_SHOP, MIKE_INV) }
        onOpNpc1(JOE) { talk(it.npc) { joe(it.npc) } }
        onOpNpc3(JOE) { trade(it.npc, JOE_SHOP, JOE_INV) }
        onOpNpc1(MAMA) { talk(it.npc) { mama(it.npc) } }
        onOpNpc3(MAMA) { trade(it.npc, MAMA_SHOP, MAMA_INV) }
        onOpNpc1(SMITH) { talk(it.npc) { smith(it.npc) } }
        onOpNpc3(SMITH) { trade(it.npc, SMITH_SHOP, SMITH_INV) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc, dialogue: suspend Dialogue.() -> Unit) {
        if (BOOK_O_PIRACY !in player.inv) {
            startDialogue(npc) { pirateSlang() }
            return
        }
        startDialogue(npc) { dialogue() }
    }

    private suspend fun ProtectedAccess.trade(npc: Npc, title: String, inv: String) {
        if (BOOK_O_PIRACY !in player.inv) {
            startDialogue(npc) { pirateSlang() }
            return
        }
        shops.open(player, npc, title, inv)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Charley (without the Book o' Piracy)
    private suspend fun Dialogue.pirateSlang() {
        chatPlayer(happy, "Hello!")
        chatNpc(quiz, "Arr? Be ye wantin' te go on account with our gang o' fillibusters?")
        chatNpc(
            happy,
            "The powder monkey be takin' a caulk after gettin' rowdy on bumboo, so there be " +
                "plenty of room for ye.",
        )
        chatPlayer(confused, "Riiiiight...")
        chatPlayer(neutral, "I'll just be over here if you need me.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Charley
    private suspend fun Dialogue.charley(npc: Npc) {
        chatNpc(neutral, "I got fish, you got gold?")
        val option =
            choice3(
                "Yes.",
                1,
                "Yes, but I don't want your fish.",
                2,
                "What happened to your legs?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Yes.")
                shops.open(player, npc, CHARLEY_SHOP, CHARLEY_INV)
            }
            2 -> {
                chatPlayer(neutral, "Yes, but I don't want your fish.")
                chatNpc(confused, "Then what are ye doin' in a fish shop? Looking fer work?")
                chatPlayer(quiz, "Possibly, do you have any quests?")
                chatNpc(
                    confused,
                    "I dunno, I haven't gone through the last catch yet. What sort of a fish is " +
                        "it?",
                )
                chatPlayer(angry, "A quest isn't a type of fish!")
                chatNpc(angry, "Then I don't have any, and yer wastin' my time!")
            }
            3 -> charleyLegs()
        }
    }

    private suspend fun Dialogue.charleyLegs() {
        chatPlayer(quiz, "What happened to your legs?")
        chatNpc(angry, "Ye wanna know what happened to my legs?")
        chatNpc(
            angry,
            "Yer too much of a lilly-livered, hat wearin' landlubber to know what happened to my " +
                "legs!",
        )
        chatPlayer(angry, "No I'm not! I've seen some freaky stuff! I can take it!")
        chatNpc(neutral, "All right, ${lad()}, since yer so insistent, I'll tell ye.")
        chatNpc(
            neutral,
            "See, I was clingin' onto a barrel, me ship havin' just had an encounter with this " +
                "albatross.",
        )
        chatNpc(
            neutral,
            "The sea was thrashin' and wild, but not so wild that I didn't see the fins of some " +
                "sharks closin' in on me.",
        )
        chatNpc(
            neutral,
            "I managed to yank a sliver of wood from the barrel just as one of them grabbed me " +
                "from below, but I slipped down the things throat by about two feet before I " +
                "managed te kill it.",
        )
        chatPlayer(quiz, "How did you survive?")
        chatNpc(
            neutral,
            "A passin' ship saw the sharks and knew there would be survivors in the water. They " +
                "sent a longboat and picked me up, but not before the sharks had taken off my " +
                "legs.",
        )
        chatNpc(
            neutral,
            "And that ${lad()} is why they call me two feet Charley, because they found me " +
                "jammed two feet down a shark's throat.",
        )
        chatPlayer(sad, "I think I'm gonna be sick...")
        chatNpc(laugh, "I knew ye couldn't handle the truth!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mike
    private suspend fun Dialogue.mike(npc: Npc) {
        chatNpc(happy, "Wanna buy some clothes? Good quality, none of yer rags.")
        val option =
            choice4(
                "I'll take a look.",
                1,
                "No thanks.",
                2,
                "Why does that shirt have a hole in it?",
                3,
                "Where did that pillar come from?",
                4,
            )
        when (option) {
            1 -> mikeShop(npc)
            2 -> mikeNoThanks()
            3 -> {
                chatPlayer(quiz, "Why does that shirt have a hole in it?")
                chatNpc(neutral, "It's to put your head through.")
                chatPlayer(
                    confused,
                    "In the stomach? Isn't that an odd place for a neck hole?",
                )
                chatNpc(
                    shifty,
                    "Oh! It seems as if I must have slipped while cleaning it and put a bit of a " +
                        "hole in the front.",
                )
                chatNpc(shifty, "Silly me.")
                chatPlayer(quiz, "What were you cleaning it with, a blood-covered scimitar?")
                chatNpc(silent, "...")
                chatNpc(shifty, "If I say yes will you buy it?")
                chatPlayer(neutral, "No.")
                chatNpc(shifty, "Then no, I was not cleaning it with a blood-covered scimitar.")
                chatPlayer(quiz, "Where do you get these clothes?")
                chatNpc(shifty, "Found em.")
                chatPlayer(
                    confused,
                    "You found a bunch of slashed up clothing, some of which has been shoddily " +
                        "cleaned and stitched together?",
                )
                chatNpc(shifty, "Yes.")
                chatNpc(neutral, "Look, do you want in on this or not?")
                if (choice2("I'll take a look.", true, "No thanks.", false)) {
                    mikeShop(npc)
                } else {
                    mikeNoThanks()
                }
            }
            4 -> mikePillar()
        }
    }

    private suspend fun Dialogue.mikeShop(npc: Npc) {
        chatPlayer(happy, "I'll take a look.")
        shops.open(player, npc, MIKE_SHOP, MIKE_INV)
    }

    private suspend fun Dialogue.mikeNoThanks() {
        chatPlayer(neutral, "No thanks.")
        chatNpc(angry, "Then what are you doin' in my shop? Move along.")
    }

    private suspend fun Dialogue.mikePillar() {
        chatPlayer(quiz, "Where did that pillar come from?")
        chatNpc(shifty, "You wanna buy it?")
        chatPlayer(quiz, "Is it for sale?")
        chatNpc(shifty, "Would you buy it if it was?")
        chatPlayer(quiz, "If it was could you sell it to me?")
        chatNpc(shifty, "If I couldn't would you still give me the money?")
        chatPlayer(neutral, "No.")
        chatPlayer(quiz, "Where did this pillar come from?")
        chatNpc(
            neutral,
            "Dunno. There are a load of islands out this way, and a lot of them have little bits " +
                "and pieces like this scattered around.",
        )
        chatNpc(
            neutral,
            "The way I see it, there must have been some people out here at one point, then " +
                "they went away.",
        )
        chatPlayer(quiz, "Where?")
        chatNpc(neutral, "Search me.")
        chatPlayer(neutral, "I'd rather not.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Joe
    private suspend fun Dialogue.joe(npc: Npc) {
        chatNpc(angry, "Whadda ya want?")
        val option =
            choice3(
                "Whadda ya got?",
                1,
                "Nothing! Honest!",
                2,
                "I wanted to know where you got that harpoon hand.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Whadda ya got?")
                chatNpc(neutral, "We gots grog and we gots food. Here, take a look.")
                shops.open(player, npc, JOE_SHOP, JOE_INV)
            }
            2 -> {
                chatPlayer(worried, "Nothing! Honest!")
                chatNpc(angry, "Then stop takin' up me floor space!")
            }
            3 -> {
                chatPlayer(quiz, "I wanted to know where you got that harpoon hand.")
                chatNpc(neutral, "Yer new in town aintcha?")
                chatPlayer(neutral, "Newish certainly.")
                chatNpc(neutral, "Well, if ye wants te hear the story, here goes.")
                chatNpc(
                    neutral,
                    "See, I was walkin' along the docks one day, and this fierce storm blew up.",
                )
                chatNpc(
                    neutral,
                    "All of a sudden I turn an there, bearin' down on me is this albatross...",
                )
                chatPlayer(
                    neutral,
                    "Sorry to interrupt, but I would just like to clear something up.",
                )
                chatNpc(angry, "What? I was just gettin' to the good bit.")
                chatPlayer(
                    neutral,
                    "When I said 'I wanted to know where you got that harpoon hand', I meant " +
                        "where did you get it made, or buy it from?",
                )
                chatNpc(angry, "If ye don't wants to hear the story then I ain't gonna tell ye!")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mama
    // TODO(content): [verify:in_game] The Other Inn has its own inv; only Harpoon Joe's
    // (inv.joeshop) is in the cache dump, and both stock the same four items
    private suspend fun Dialogue.mama(npc: Npc) {
        chatNpc(happy, "Hello stranger, you come for a drink?")
        chatPlayer(quiz, "What sort of drinks do you have?")
        chatNpc(happy, "We got some cold beers, some warm stews and some 'rum'.")
        chatPlayer(worried, "Not, Captain Braindeath's 'rum'?")
        chatNpc(
            happy,
            "Yes! There was some sorta problem at the brewery, but they got it all sorted out " +
                "now.",
        )
        chatNpc(quiz, "You wanna buy something?")
        shops.open(player, npc, MAMA_SHOP, MAMA_INV)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Smith
    private suspend fun Dialogue.smith(npc: Npc) {
        chatNpc(
            neutral,
            "Yer standin' in me light stranger. Can I sell ye somethin' that'll make ye move?",
        )
        val hasAnchor = BROKEN_ANCHOR in player.inv
        val option =
            if (hasAnchor) {
                choice4(
                    "Probably, let's see what you have.",
                    1,
                    "I doubt it.",
                    2,
                    "That depends on if you have any cutlasses.",
                    3,
                    "I have this anchor, can you fix it?",
                    4,
                )
            } else {
                choice3(
                    "Probably, let's see what you have.",
                    1,
                    "I doubt it.",
                    2,
                    "That depends on if you have any cutlasses.",
                    3,
                )
            }
        when (option) {
            1 -> {
                chatPlayer(happy, "Probably, let's see what you have.")
                shops.open(player, npc, SMITH_SHOP, SMITH_INV)
            }
            2 -> {
                chatPlayer(neutral, "I doubt it.")
                chatNpc(angry, "Then move on stranger.")
            }
            3 -> cutlasses()
            4 -> anchor()
        }
    }

    private suspend fun Dialogue.cutlasses() {
        chatPlayer(neutral, "That depends on if you have any cutlasses.")
        chatNpc(laugh, "Cutlasses! Think yer a pirate now ${lad()}?")
        chatPlayer(happy, "Well, yes.")
        chatNpc(neutral, "Well, I have some cutlasses.")
        chatNpc(happy, "I have Brass Hand Harry's Cutlass. Well, a load of them to be honest.")
        chatNpc(happy, "Then there's Unlucky Jenkins' Lucky Cutlass. Lovely blade that.")
        chatNpc(
            happy,
            "I've also got Gentleman Mallard's Rapier. Not a cutlass I know, but the finest " +
                "blade ever wielded by a privateer.",
        )
        chatNpc(quiz, "Now, which would ye like to hear about?")
        val option =
            choice3(
                "Brass Hand Harry's Cutlass.",
                1,
                "Unlucky Jenkins' Lucky Cutlass.",
                2,
                "Gentleman Mallard's Rapier",
                3,
            )
        when (option) {
            1 -> harrysCutlass()
            2 -> luckyCutlass()
            3 -> mallardsRapier()
        }
    }

    private suspend fun Dialogue.harrysCutlass() {
        chatPlayer(neutral, "Brass Hand Harry's Cutlass.")
        chatNpc(happy, "A good choice!")
        chatNpc(neutral, "Ye may end up meeting Brass Hand Harry if ye hang around long enough.")
        chatNpc(neutral, "He lost his left arm to a canister round, but that didn't stop him much.")
        chatNpc(
            neutral,
            "Then he lost his right hand at the wrist fightin' the excise men off the coast of " +
                "Karamja.",
        )
        chatNpc(
            neutral,
            "Not bein' the sort to lie about mopin', he asked me to make him a cutlass with a " +
                "brass gauntlet fixed to the handle, and rig it up with a load of straps so he " +
                "could keep fightin'.",
        )
        chatNpc(
            neutral,
            "He's a great man is Harry, but he keeps snappin' the straps while he's at sea, so " +
                "he sells the cutlasses back to me in exchange for a new one.",
        )
        chatNpc(
            neutral,
            "It's quite a good blade, but the big, brass gauntlet is what makes it worthwhile. " +
                "It's big enough to fit over even a real gauntlet, so ye get extra protection to " +
                "yer sword hand.",
        )
        chatNpc(neutral, "I can sell ye one fer 1040 coins.")
        chatNpc(quiz, "Interested?")
        if (choice2("Yes.", true, "No.", false)) {
            chatPlayer(happy, "Yes.")
            sellBlade(
                HARRYS_CUTLASS,
                HARRYS_CUTLASS_PRICE,
                "Here ye go. I've got a load of these left over if ye need more.",
            )
        } else {
            stillHere()
        }
    }

    private suspend fun Dialogue.luckyCutlass() {
        chatPlayer(neutral, "Unlucky Jenkins' Lucky Cutlass.")
        chatNpc(sad, "Arr, tis a sad tale to this sword.")
        chatNpc(
            sad,
            "Jenkins was the lookout on the Jonah, a cursed vessel if there ever was one.",
        )
        chatNpc(
            sad,
            "One day his ship were attacked by another group of pirates durin' a fierce storm. " +
                "One of their bowmen took a shot at the Cap'n of the vessel, but it were " +
                "deflected off his compass and hit Jenkins in the back.",
        )
        chatPlayer(shocked, "How unlucky!")
        chatNpc(
            sad,
            "That weren't the worst of it ${lad()}. He survived the fight, and the ship's " +
                "surgeon was trying to get the arrow out when...",
        )
        chatNpc(quiz, "Are ye familiar with the spell bones to bananas?")
        chatPlayer(quiz, "Yes, why?")
        chatNpc(
            sad,
            "Well, due to a blow to the head he took in the fight, the surgeon cast that spell, " +
                "thinking it was some sort of new healing spell.",
        )
        chatNpc(sad, "And he did so while touching Jenkins' ribs.")
        chatPlayer(quiz, "And that killed him?")
        chatNpc(neutral, "No ${lad()}!")
        chatNpc(
            sad,
            "See, he survived, his bones turned to bananas, as a huge storm blew up and the crew " +
                "debated what to do with him.",
        )
        chatNpc(
            sad,
            "But you see, the storm was actually the tail end of a hurricane that had swept the " +
                "nearby jungle islands, and when it approached the ship it petered out, droppin' " +
                "all manner of strange things onto the deck and into the",
        )
        chatNpc(sad, "ocean.")
        chatNpc(
            sad,
            "The worst part was when the ship was bombarded with ravenous, hunger-crazed " +
                "monkeys, the majority of which landed on Jenkins as he lay recoverin'.",
        )
        chatPlayer(quiz, "And that killed him?")
        chatNpc(neutral, "No ${lad()}!")
        chatNpc(sad, "See, what happened next, after the monkeys had had their way with him was...")
        chatPlayer(
            angry,
            "Look, I don't want to hear any more. Why would I want to buy his cutlass?",
        )
        chatNpc(sad, "Well, after the poor man passed on...")
        chatNpc(sad, "In a final, terrifying, mashed potato coated tragedy...")
        chatNpc(neutral, "His shipmates found his cutlass was completely untouched.")
        chatNpc(
            happy,
            "See, having been exposed to all the bad luck that Jenkins went through, the thing " +
                "is unlikely to ever see anything that bad again.",
        )
        chatNpc(happy, "Hence, it must be lucky.")
        chatNpc(neutral, "Ye can have it fer 2560 coins.")
        chatNpc(quiz, "Interested?")
        if (choice2("Yes.", true, "No.", false)) {
            sellBlade(LUCKY_CUTLASS, LUCKY_CUTLASS_PRICE, "Here ye go. I hope it brings you luck.")
        } else {
            stillHere()
        }
    }

    private suspend fun Dialogue.mallardsRapier() {
        chatPlayer(neutral, "Gentleman Mallard's Rapier.")
        chatNpc(
            neutral,
            "Well, I'll tell ye the story, but it'll take a great deal of convincin' for me to " +
                "sell you this rapier.",
        )
        chatNpc(
            neutral,
            "See, gentleman Mallard is a legend in these parts. His family had been rich, " +
                "shipping all sorts of goods up and down the coast.",
        )
        chatNpc(
            sad,
            "Then their rivals got together and had their ships sunk, one after the other, and " +
                "his business partners were all either bought off or had...accidents.",
        )
        chatNpc(
            neutral,
            "With no money and only one ship left, he took to the life of a pirate, but no " +
                "brigand was he.",
        )
        chatNpc(
            happy,
            "He took only from those men that had brought about his downfall, was fair with his " +
                "men, and generous with his loot.",
        )
        chatNpc(
            happy,
            "Why, this island would have been wiped out by the shakes if he hadn't turned over " +
                "his stash to buy medicine.",
        )
        chatNpc(
            sad,
            "One day his ship was attacked by a huge sea creature that swept the decks of men " +
                "with its tentacles.",
        )
        chatNpc(
            sad,
            "The crew took to the lifeboats, and the last they saw of him the gentleman was " +
                "stabbin' at the thing as it hauled the ship under the water.",
        )
        chatNpc(
            sad,
            "A week later about ten foot of tentacle washed up on the beach, with his rapier " +
                "embedded in it to the hilt.",
        )
        chatNpc(neutral, "I took it, cleaned it up, and I've been lookin' after it ever since.")
        chatNpc(
            neutral,
            "I can't let it go for less than 25600 coins, and even then that's a bargain.",
        )
        chatNpc(quiz, "Ye think ye have what it takes to wield it?")
        if (choice2("Yes.", true, "No.", false)) {
            sellBlade(
                MALLARDS_RAPIER,
                MALLARDS_RAPIER_PRICE,
                "I hope yer worthy enough to wield this, stranger. I'd hate te think Gentleman " +
                    "Mallard would be disappointed in me fer sellin' it to ye.",
            )
        } else {
            stillHere()
        }
    }

    private suspend fun Dialogue.sellBlade(blade: String, price: Int, thanks: String) {
        if (!player.invTakeFee(price)) {
            chatNpc(happy, "Smart ${lad()}! Come and see me when ye have the coins fer it.")
            return
        }
        player.invAddOrDrop(objRepo, blade)
        chatNpc(happy, thanks)
    }

    private suspend fun Dialogue.stillHere() {
        chatPlayer(neutral, "No.")
        chatNpc(neutral, "Well I'll still be here if ye change yer mind.")
    }

    private suspend fun Dialogue.anchor() {
        chatPlayer(quiz, "I have this anchor, can you fix it?")
        chatNpc(
            confused,
            "Well, this is a very unusual job. This thing looks more suited to bashin' peoples " +
                "heads in than securing a ship.",
        )
        chatNpc(quiz, "Where did ye get it?")
        chatPlayer(shifty, "You wouldn't believe me if I told you.")
        chatNpc(shifty, "Stole it eh? Well I can sort it out for 230000 coins, if you like.")
        if (player.inv.count(COINS) < ANCHOR_REPAIR_PRICE) {
            chatPlayer(sad, "That's too much for me at the moment.")
            return
        }
        if (!choice2("Yes please.", true, "No", false, title = "Select an option")) {
            stillHere()
            return
        }
        chatPlayer(happy, "Yes please.")
        if (!repairAnchor()) {
            return
        }
        chatNpc(neutral, "Right, let's get to work.")
        chatNpc(happy, "There ye go, good as new!")
        chatPlayer(happy, "Thanks Smith!")
    }

    private fun Dialogue.repairAnchor(): Boolean {
        val result =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete {
                    from = inv
                    obj = BROKEN_ANCHOR.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                delete {
                    from = inv
                    obj = COINS.asRSCM(RSCMType.OBJ)
                    strictCount = ANCHOR_REPAIR_PRICE
                }
                insert {
                    into = inv
                    obj = ANCHOR.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        return result.success
    }

    private fun Dialogue.lad(): String = if (access.isBodyTypeA()) "lad" else "lass"

    private companion object {
        const val BOOK_O_PIRACY = "obj.fever_piracy_book"
        const val COINS = "obj.coins"
        const val CHARLEY = "npc.fever_pirate_two_feet_charley"
        const val CHARLEY_SHOP = "Two Feet Charley's Fish Shop."
        const val CHARLEY_INV = "inv.charleyshop"
        const val MIKE = "npc.fever_dodgy_mike"
        const val MIKE_SHOP = "Dodgy Mike's Second Hand Clothing."
        const val MIKE_INV = "inv.mikeshop"
        const val JOE = "npc.fever_harpoon_joe"
        const val JOE_SHOP = "Harpoon Joe's House of 'Rum'."
        const val JOE_INV = "inv.joeshop"
        const val MAMA = "npc.fever_mama_la_fiette"
        const val MAMA_SHOP = "The Other Inn."
        const val MAMA_INV = "inv.joeshop"
        const val SMITH = "npc.fever_smithing_smith"
        const val SMITH_SHOP = "Smithing Smith's Shop."
        const val SMITH_INV = "inv.smithsshop"
        const val HARRYS_CUTLASS = "obj.fever_cutlass_brasshand_harry"
        const val HARRYS_CUTLASS_PRICE = 1040
        const val LUCKY_CUTLASS = "obj.fever_cutlass_unlucky_jenkins_lucky_cutlass"
        const val LUCKY_CUTLASS_PRICE = 2560
        const val MALLARDS_RAPIER = "obj.fever_rapier_mallard"
        const val MALLARDS_RAPIER_PRICE = 25600
        const val BROKEN_ANCHOR = "obj.brain_broken_anchor"
        const val ANCHOR = "obj.brain_anchor"
        const val ANCHOR_REPAIR_PRICE = 230_000
    }
}
