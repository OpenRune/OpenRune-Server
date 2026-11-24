package org.rsmod.content.areas.misc.warriorsguild.npcs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseAttackLvl
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.misc.warriorsguild.scripts.EntranceScript.Companion.meetsGuildEntry
import org.rsmod.content.areas.misc.warriorsguild.scripts.TokenClaimScript.Companion.claimTokens
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GuildStaffScript @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.warguild_harrallak_npc") { startDialogue(it.npc) { harrallak() } }
        onOpNpc1("npc.warguild_ghommal_npc") { startDialogue(it.npc) { ghommal() } }
        onOpNpc1("npc.warguild_laidee") { startDialogue(it.npc) { laidee() } }
        onOpNpc1("npc.warguild_ajjat") { startDialogue(it.npc) { ajjat() } }
        onOpNpc1("npc.strength_referree1") { startDialogue(it.npc) { ref() } }
        onOpNpc1("npc.strength_referree2") { startDialogue(it.npc) { ref() } }

        onOpNpc1(ANTON) { startDialogue(it.npc) { anton(it.npc) } }
        onOpNpc3(ANTON) { openShop(it.npc) }
        onOpNpc1(LIDIO) { startDialogue(it.npc) { lidio(it.npc) } }
        onOpNpc3(LIDIO) { openShop(it.npc) }
        onOpNpc1(LILLY) { startDialogue(it.npc) { lilly(it.npc) } }
        onOpNpc3(LILLY) { openShop(it.npc) }
    }

    private fun ProtectedAccess.openShop(npc: Npc) {
        shopOf(player, npc)
    }

    private fun shopOf(player: Player, npc: Npc) {
        val (title, inv) = SHOPS.getValue(RSCM.getReverseMapping(RSCMType.NPC, npc.id))
        shops.open(player, npc, title, inv)
    }

    private fun Dialogue.isMan(): Boolean = player.appearance.bodyType == Constants.bodytype_a

    private fun Dialogue.sirOrMadam(): String = if (isMan()) "Sir" else "Madam"

    private suspend fun Dialogue.harrallak() {
        chatNpc(happy, "Welcome to my humble guild ${player.displayName}.")
        when (
            choice3(
                "Quite a place you've got here.",
                Topic.A,
                "You any good with a sword?",
                Topic.B,
                "Bye!",
                Topic.Bye,
            )
        ) {
            Topic.A -> {
                chatPlayer(happy, "Quite a place you've got here.")
                chatNpc(happy, "Indeed we do. Would you like to know more about it?")
                harrallakAreas()
            }
            Topic.B -> {
                chatPlayer(quiz, "You any good with a sword?")
                chatNpc(
                    confused,
                    "Am I any good with....a sword... Have you any clue who I am?",
                )
                chatPlayer(neutral, "Not really... no.")
                chatNpc(happy, "Why I could best any person alive in a rapier duel!")
                chatPlayer(happy, "Try me then!")
                chatNpc(
                    neutral,
                    "My dear ${if (isMan()) "sir" else "lady"}, I couldn't possibly duel you, I " +
                        "might hurt you and then what would happen to my reputation! Besides, I " +
                        "have this wonderful guild to run. Why don't you take a look at the various " +
                        "activities we",
                )
                chatNpc(
                    neutral,
                    "have. You might even collect enough tokens to be allowed in to kill the " +
                        "strange beasts from the east!",
                )
            }
            else -> harrallakBye()
        }
    }

    private suspend fun Dialogue.harrallakAreas() {
        when (
            choice5(
                "Tell me about the strength training area.",
                Topic.A,
                "Tell me about the attack training area.",
                Topic.B,
                "Tell me about the defence training area.",
                Topic.C,
                "Tell me about the combat training area.",
                Topic.D,
                "Tell me about tokens.",
                Topic.E,
            )
        ) {
            Topic.A -> {
                chatPlayer(quiz, "Tell me about the strength training area.")
                chatNpc(
                    neutral,
                    "Ahh the mighty warrior Sloane guards the strength training area. This " +
                        "intriguing little area consists of two shotput lanes for different " +
                        "weights of shot. It's fairly simple, the Referee or Sloane can explain " +
                        "more. You",
                )
                chatNpc(neutral, "may find yourself panting for breath though.")
                chatPlayer(quiz, "Oh? Why?")
                chatNpc(
                    neutral,
                    "Well you see my dear ${if (isMan()) "boy" else "lady"}, the games there " +
                        "really do sap your energy quite a bit. You can find it just up the stairs " +
                        "behind the bank.",
                )
            }
            Topic.B -> {
                chatPlayer(quiz, "Tell me about the attack training area.")
                chatNpc(neutral, "Ahhh, dummies.")
                chatPlayer(angry, "I'm no dummy, I just want to know what is there!")
                chatNpc(
                    neutral,
                    "Oh no my dear ${if (isMan()) "man" else "lady"}, I did not mean you at all! " +
                        "The training area has mechanical dummies which pop up out of holes in the " +
                        "floor. The noble dwarf Gamfred invented the mechanism and Ajjat can " +
                        "explain more about what",
                )
                chatNpc(neutral, "to do there.")
                chatPlayer(happy, "Oh, ok, I'll have to try it out!")
                chatNpc(neutral, "You can find it just down the corridor and on your right.")
            }
            Topic.C -> {
                chatPlayer(quiz, "Tell me about the defence training area.")
                chatNpc(
                    neutral,
                    "To polish your defensive skills to the very highest level we've employed a " +
                        "most inventive dwarf and a catapult.",
                )
                chatPlayer(confused, "You're going to throw dwarves at me?")
                chatNpc(happy, "Oh my no! I think Gamfred would object to that most strongly.")
                chatNpc(
                    neutral,
                    "He's an inventor you see and has built a marvellous contraption that can " +
                        "throw all sorts of things at you including magic missiles...",
                )
                chatPlayer(confused, "Mmmm?")
                chatNpc(neutral, "....spiked iron balls.....")
                chatPlayer(confused, "Err....")
                chatNpc(neutral, "....spinning slashing blades....")
                chatPlayer(confused, "Ummm...")
                chatNpc(neutral, "... and even anvils.")
                chatPlayer(angry, "ANVILS!?")
                chatNpc(
                    neutral,
                    "No need to be afraid, it's all under very controlled conditions! You can find " +
                        "it just up the stairs behind the bank.",
                )
            }
            Topic.D -> {
                chatPlayer(quiz, "Tell me about the combat training area.")
                chatNpc(
                    neutral,
                    "Ahh yes, our resident magician from foreign lands created a most amazing " +
                        "gadget which can turn your own armour against you! It's really quite " +
                        "intriguing.",
                )
                chatPlayer(confused, "That sounds dangerous. What if I'm wearing it at the time?")
                chatNpc(
                    neutral,
                    "So far that's not happened. You need to speak to Shanomi about the specifics " +
                        "of the process, but as I understand it, putting a suit of armour in one " +
                        "of these devices will make it come to life some how. The better",
                )
                chatNpc(neutral, "the armour, the harder it is to 'kill'.")
                chatPlayer(
                    confused,
                    "Fighting my own armour, that sounds... weird. I could be killed by it...",
                )
                chatNpc(
                    neutral,
                    "Indeed we have had a few fatalities from warriors over stretching themselves " +
                        "and not knowing their limits. Start small and work up is my motto! That " +
                        "and go see Lidio for some food if you need it.",
                )
                chatPlayer(neutral, "Ok, thanks for the warning.")
            }
            else -> {
                chatPlayer(quiz, "Tell me about tokens.")
                chatNpc(
                    neutral,
                    "Ahh, yes, our token system is designed to allow you an appropriate amount of " +
                        "time with my discovery in the very top floor of the guild. I won't spoil " +
                        "the surprise as to what that is. Go up and see for yourself. Now, the",
                )
                chatNpc(
                    neutral,
                    "amount of tokens you collect from the various activities around the guild " +
                        "will dictate how long Kamfreena will allow you in the enclosure on the " +
                        "very top floor. More tokens equals more time.",
                )
                chatPlayer(quiz, "So what's up there?")
                chatNpc(happy, "If I told you it would spoil the surprise!")
                chatPlayer(neutral, "Ok ok... so how do I earn and claim these tokens?")
                chatNpc(
                    neutral,
                    "You can earn them simply by using the training exercises around the guild, " +
                        "the staff will then enter this into a ledger as you play. You can claim " +
                        "them by simply asking any of the staff at the training areas, or",
                )
                chatNpc(neutral, "myself.")
                chatPlayer(happy, "Sounds easy enough.")
            }
        }
        harrallakAreas()
    }

    private suspend fun Dialogue.harrallakBye() {
        chatPlayer(neutral, "Bye!")
        chatNpc(
            happy,
            "Farewell ${if (isMan()) "brave warrior" else "fair lady"}, I do hope you enjoy my " +
                "guild.",
        )
    }

    private suspend fun Dialogue.ghommal() {
        chatNpc(neutral, "Can I help you?")
        when (
            choice2("What are you doing out here?", Topic.A, "No, I'm alright thanks.", Topic.Bye)
        ) {
            Topic.A -> {
                chatPlayer(quiz, "What are you doing out here?")
                chatNpc(
                    neutral,
                    "This here is the Warrior's Guild, a place where the strong may train. I'm " +
                        "the one who decides if you are allowed in or not.",
                )
                chatPlayer(quiz, "Am I qualified to enter?")
                if (player.meetsGuildEntry()) {
                    chatNpc(happy, "Yes you are. Welcome to the Warrior's Guild!")
                    chatPlayer(happy, "Thank you!")
                    return
                }
                chatNpc(neutral, "You may not enter the Warrior's Guild as you are right now.")
                chatPlayer(angry, "What? But I'm a warrior!")
                chatNpc(neutral, "While you may consider yourself a warrior, I do not.")
                chatPlayer(
                    happy,
                    "Go on, let me in, you know you want to. I could... make it worth your " +
                        "while...",
                )
                chatNpc(angry, "Go away and train small one. I do not take bribes.")
                chatPlayer(quiz, "Why not?")
                chatNpc(
                    neutral,
                    "The Warrior's Guild has a code of honour, a code that I respect. Move along.",
                )
            }
            else -> chatPlayer(neutral, "No, I'm alright thanks.")
        }
    }

    private suspend fun Dialogue.laidee() {
        chatNpc(happy, "Hello, I'm Laidee. Welcome to the Warriors' Guild.")
        when (
            choice2("What are you doing out here?", Topic.A, "I'd better get going.", Topic.Bye)
        ) {
            Topic.A -> {
                chatPlayer(quiz, "What are you doing out here?")
                chatNpc(
                    neutral,
                    "I'm here to guard the Warrior's Guild and determine who's qualified to enter.",
                )
                chatPlayer(quiz, "So you're Ghommal's replacement?")
                chatNpc(
                    sad,
                    "That I am. Ghommal and I used to adventure together before we joined the " +
                        "Guild. It felt only right that I take over from him. It is... strange not " +
                        "having him around.",
                )
                chatPlayer(sad, "I'm sorry for your loss. He was a great warrior.")
                chatNpc(sad, "And an even greater friend.")
            }
            else -> chatPlayer(neutral, "I'd better get going.")
        }
    }

    private suspend fun Dialogue.ajjat() {
        chatNpc(
            neutral,
            "Greetings, fellow warrior. I am Ajjat, former black knight and now training officer " +
                "here in the warrior guild.",
        )
        if (player.baseAttackLvl >= MAX_LEVEL) {
            chatNpc(
                happy,
                "Ah, but I can see you are already a master in the fine art of attacking, perhaps " +
                    "you have come to me to purchase a Cape of Attack, and thus join the elite " +
                    "few who have mastered this exacting skill?",
            )
            chatNpc(
                neutral,
                "When wearing this cape, Kamfreena will recognize your ability and allow you to " +
                    "access her Cyclopes without the need for guild tokens.",
            )
        }
        ajjatOptions()
    }

    private suspend fun Dialogue.ajjatOptions() {
        val master = player.baseAttackLvl >= MAX_LEVEL
        val capeOption =
            if (master) "May I buy a Skillcape of Attack, please?"
            else "Can you tell me about skillcapes, please?"
        when (
            choice5(
                capeOption,
                Topic.A,
                "Black Knight? Why are you here?",
                Topic.B,
                "What's the Dummy Room all about?",
                Topic.C,
                "May I claim my tokens please?",
                Topic.D,
                "Bye!",
                Topic.Bye,
            )
        ) {
            Topic.A -> if (master) buyAttackCape() else describeSkillcapes()
            Topic.B -> {
                chatPlayer(quiz, "Black Knight? Why are you here?")
                chatNpc(
                    neutral,
                    "Indeed I was, however their... methods... did not match with my ideals.. so I " +
                        "left. Harrallak, recognising my talent as a warrior, took me in and " +
                        "offered me a job here.",
                )
                chatPlayer(neutral, "Hmm... well if Harrallak trusts you, I guess I can.")
                ajjatOptions()
            }
            Topic.C -> {
                chatPlayer(quiz, "What's the Dummy Room all about?")
                chatNpc(
                    neutral,
                    "Ahh yes, the dummies. Another ingenious invention of the noble Dwarf Gamfred. " +
                        "They're mechanical you see and pop up out of the floor. You have to hit " +
                        "them with the correct attack mode before they disappear again.",
                )
                chatPlayer(quiz, "So how do I tell which one is which?")
                chatNpc(
                    neutral,
                    "There are two different ways. One indication is their colour, the other is " +
                        "the pose and weapons they are holding, for instance, the one holding " +
                        "daggers you will need to hit with a piercing attack.",
                )
                chatNpc(
                    neutral,
                    "In the room you will find a poster on the wall which can help you recognise " +
                        "each different dummy.",
                )
                chatPlayer(happy, "That sounds ingenious!")
                chatNpc(
                    neutral,
                    "Indeed, you may find that you need several weapons to be successful 100% of " +
                        "the time, but keep trying. The weapons shop upstairs may help you there.",
                )
                ajjatOptions()
            }
            Topic.D -> claimTokens()
            else -> {
                chatPlayer(neutral, "Bye!")
                chatNpc(neutral, "Farewell warrior. Stay away from the dark side.")
            }
        }
    }

    private suspend fun Dialogue.describeSkillcapes() {
        chatPlayer(quiz, "Can you tell me about skillcapes, please?")
        chatNpc(
            neutral,
            "Skillcapes, also known as Capes of Accomplishment, are reserved for the elite of the " +
                "elite. Only a person who has truly mastered a skill can buy one, and even then a " +
                "skillcape can only be bought from one who is",
        )
        chatNpc(neutral, "recognised as the highest skilled in the land at any particular skill.")
        chatNpc(
            neutral,
            "I have the privilege of being the person that controls access to the Skillcape of " +
                "Attack, which Kamfreena will recognize and allow you to access her Cyclopes " +
                "without the need for guild tokens.",
        )
        chatNpc(neutral, "Is there anything else I can help you with?")
        ajjatOptions()
    }

    private suspend fun Dialogue.buyAttackCape() {
        chatPlayer(quiz, "May I buy a Skillcape of Attack, please?")
        chatNpc(
            neutral,
            "Most certainly; unfortunately being such a prestigious item, they are appropriately " +
                "expensive. I'm afraid I must ask you for the princely sum of 99000 gold!",
        )
        val accept =
            choice2(
                "99000 gold! Are you mad?",
                false,
                "I would gladly pay such a paltry sum for a splendid cape.",
                true,
            )
        if (!accept) {
            chatPlayer(angry, "99000! Are you mad?")
            chatNpc(
                neutral,
                "Not at all; there are many other adventurers who would love the opportunity to " +
                    "purchase such a prestigious item! You can find me here if you change your " +
                    "mind.",
            )
            return
        }
        chatPlayer(happy, "I would gladly pay such a paltry sum for a splendid cape.")
        if (player.inv.count(COINS) < CAPE_PRICE) {
            chatPlayer(sad, "But, unfortunately, I don't have enough money with me.")
            chatNpc(neutral, "Well come back and see me when you do.")
            return
        }
        val cape = if (player.countMaxedStats() > 1) TRIMMED_CAPE else CAPE
        val tx =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete(inv = inv, obj = COINS.asRSCM(RSCMType.OBJ), count = CAPE_PRICE)
                add(inv = inv, obj = cape.asRSCM(RSCMType.OBJ), count = 1)
                add(inv = inv, obj = HOOD.asRSCM(RSCMType.OBJ), count = 1)
            }
        if (tx.failure) {
            chatNpc(
                neutral,
                "Unfortunately all Skillcapes are only available with a free hood, it's part of a " +
                    "skill promotion deal; buy one get one free, you know. So you'll need to free " +
                    "up some inventory space before I can sell you one.",
            )
            return
        }
        chatNpc(happy, "Excellent! Wear that cape with pride my friend.")
    }

    private fun Player.countMaxedStats(): Int =
        ServerCacheManager.getStats().keys.count { id ->
            statBase(RSCM.getReverseMapping(RSCMType.STAT, id)) >= MAX_LEVEL
        }

    private suspend fun Dialogue.ref() {
        chatNpc(neutral, "Greetings ${sirOrMadam()}.")
        when (
            choice4(
                "Tell me about the Shot Put area.",
                Topic.A,
                "May I claim my tokens please?",
                Topic.B,
                "Do you have any tips for me?",
                Topic.C,
                "Bye!",
                Topic.Bye,
            )
        ) {
            Topic.A -> {
                val sir = sirOrMadam()
                chatPlayer(quiz, "Tell me about the Shot Put area.")
                chatNpc(neutral, "Of course $sir. There are two different weights of shot...")
                chatPlayer(confused, "Shot?")
                chatNpc(
                    neutral,
                    "Yes $sir. Shot. The iron spheres that are propelled by the chemical energy " +
                        "stored in your body.",
                )
                chatPlayer(confused, "The... what?")
                chatNpc(
                    neutral,
                    "Your strength $sir. The stronger you are, the further you can throw the " +
                        "shot, but there are other factors of course, like your technique.",
                )
                chatPlayer(quiz, "What's that then?")
                chatNpc(neutral, "The style of the shot $sir.")
                chatPlayer(confused, "Iron has style??")
                mesbox("The Referee sighs, rolls his eyes and continues....")
                chatNpc(neutral, "$sir, the style in which you throw the shot, not the style of the iron.")
                chatPlayer(happy, "Oh! You mean the spinny round thing or the chuck it straight?")
                chatNpc(
                    neutral,
                    "Crudely put $sir, but yes. Some are more difficult than others. Experiment " +
                        "and see which you prefer.",
                )
                chatPlayer(happy, "Thanks for the help!")
                chatNpc(neutral, "You are welcome $sir.")
            }
            Topic.B -> claimTokens()
            Topic.C -> {
                val sir = sirOrMadam()
                chatPlayer(quiz, "Do you have any tips for me?")
                chatNpc(quiz, "Tips $sir?")
                chatPlayer(neutral, "Yes, like how can I do better than everyone else.")
                chatNpc(
                    neutral,
                    "$sir may find that a fine powder applied to the hands may give one an " +
                        "advantage when putting the shot.",
                )
                chatPlayer(
                    quiz,
                    "You mean if I grind something up and put dust on my hands I'll chuck the " +
                        "ball further?",
                )
                chatNpc(neutral, "Yes $sir.")
                chatPlayer(happy, "Thanks!")
                chatNpc(neutral, "You are welcome $sir.")
            }
            else -> {
                chatPlayer(neutral, "Bye!")
                chatNpc(neutral, "Good luck ${sirOrMadam()}.")
            }
        }
    }

    private suspend fun Dialogue.anton(npc: Npc) {
        chatNpc(happy, "Ahhh, hello there. How can I help?")
        chatPlayer(neutral, "Looks like you have a good selection of weapons around here...")
        chatNpc(
            happy,
            "Indeed so, specially imported from the finest smiths around the lands, take a look " +
                "at my wares.",
        )
        shopOf(player, npc)
    }

    private suspend fun Dialogue.lidio(npc: Npc) {
        chatNpc(happy, "Greetings warrior, how can I fill your stomach today?")
        chatPlayer(happy, "With food preferably.")
        shopOf(player, npc)
    }

    private suspend fun Dialogue.lilly(npc: Npc) {
        chatNpc(confused, "Uh..... hi... didn't see you there. Can.... I help?")
        chatPlayer(quiz, "Umm... do you sell potions?")
        chatNpc(neutral, "Erm... yes. When I'm not drinking them.")
        when (
            choice3(
                "I'd like to see what you have for sale.",
                Topic.A,
                "That's a pretty wall hanging.",
                Topic.B,
                "Bye!",
                Topic.Bye,
            )
        ) {
            Topic.A -> {
                chatPlayer(neutral, "I'd like to see what you have for sale.")
                chatNpc(neutral, "Of course...")
                shopOf(player, npc)
            }
            Topic.B -> {
                chatPlayer(happy, "That's a pretty wall hanging.")
                chatNpc(happy, "Do you think so? I made it myself.")
                chatPlayer(quiz, "Really? Is that why there's all this cloth and dye around?")
                chatNpc(neutral, "Yes, it's a hobby of mine when I'm.... relaxing.")
            }
            else -> {
                chatPlayer(neutral, "Bye!")
                chatNpc(happy, "Have fun and come back soon!")
            }
        }
    }

    private enum class Topic {
        A,
        B,
        C,
        D,
        E,
        Bye,
    }

    private companion object {
        const val MAX_LEVEL = 99
        const val CAPE_PRICE = 99_000
        const val COINS = "obj.coins"
        const val CAPE = "obj.skillcape_attack"
        const val TRIMMED_CAPE = "obj.skillcape_attack_trimmed"
        const val HOOD = "obj.skillcape_attack_hood"
        const val ANTON = "npc.warguild_armour_shopkeeper"
        const val LIDIO = "npc.warguild_food_shopkeeper"
        const val LILLY = "npc.warguild_potion_shopkeeper"

        val SHOPS =
            mapOf(
                ANTON to ("Warrior Guild Armoury" to "inv.warguild_armour_shop"),
                LIDIO to ("Warrior Guild Food Shop" to "inv.warguild_food_shop"),
                LILLY to ("Warrior Guild Potion Shop" to "inv.warguild_potion_shop"),
            )
    }
}
