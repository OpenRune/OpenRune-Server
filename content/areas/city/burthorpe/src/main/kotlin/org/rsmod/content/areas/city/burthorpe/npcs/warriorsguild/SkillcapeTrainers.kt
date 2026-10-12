package org.rsmod.content.areas.city.burthorpe.npcs.warriorsguild

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.baseAttackLvl
import org.rsmod.api.player.stat.baseStrengthLvl
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:skillcapes] trimmed cape when the player has more than one level 99
class SkillcapeTrainers @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.warguild_ajjat") { startDialogue(it.npc) { ajjat() } }
        onOpNpc3("npc.warguild_ajjat") { startDialogue(it.npc) { claimTokens() } }
        onOpNpc1("npc.warguild_sloane_npc") { startDialogue(it.npc) { sloane() } }
        onOpNpc3("npc.warguild_sloane_npc") { startDialogue(it.npc) { claimTokens() } }
        onOpNpc1("npc.warguild_yadech_npc") { startDialogue(it.npc) { yadech() } }
        onOpNpc3("npc.warguild_yadech_npc") { startDialogue(it.npc) { claimTokens() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ajjat
    private suspend fun Dialogue.ajjat() {
        chatNpc(
            neutral,
            "Greetings, fellow warrior. I am Ajjat, former black knight and now training officer " +
                "here in the warrior guild.",
        )
        val master = player.baseAttackLvl >= MAX_LEVEL
        if (master) {
            chatNpc(
                happy,
                "Ah, but I can see you are already a master in the fine art of attacking, " +
                    "perhaps you have come to me to purchase a Cape of Attack, and thus join the " +
                    "elite few who have mastered this exacting skill?",
            )
            chatNpc(
                neutral,
                "When wearing this cape, Kamfreena will recognize your ability and allow you to " +
                    "access her Cyclopes without the need for guild tokens.",
            )
        }
        while (true) {
            val option =
                choice5(
                    if (master) {
                        "May I buy a Skillcape of Attack, please?"
                    } else {
                        "Can you tell me about skillcapes, please?"
                    },
                    1,
                    "Black Knight? Why are you here?",
                    2,
                    "What's the Dummy Room all about?",
                    3,
                    CLAIM_TOKENS,
                    4,
                    "Bye!",
                    5,
                )
            when (option) {
                1 -> {
                    if (master) {
                        ajjatCape()
                        return
                    }
                    ajjatSkillcapes()
                }
                2 -> ajjatBlackKnight()
                3 -> ajjatDummyRoom()
                4 -> {
                    claimTokens()
                    return
                }
                5 -> {
                    chatPlayer(happy, "Bye!")
                    chatNpc(neutral, "Farewell warrior. Stay away from the dark side.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.ajjatSkillcapes() {
        chatPlayer(quiz, "Can you tell me about skillcapes, please?")
        chatNpc(
            neutral,
            "Skillcapes, also known as Capes of Accomplishment, are reserved for the elite of " +
                "the elite. Only a person who has truly mastered a skill can buy one, and even " +
                "then a skillcape can only be bought from one who is",
        )
        chatNpc(neutral, "recognised as the highest skilled in the land at any particular skill.")
        chatNpc(
            neutral,
            "I have the privilege of being the person that controls access to the Skillcape of " +
                "Attack, which Kamfreena will recognize and allow you to access her Cyclopes " +
                "without the need for guild tokens.",
        )
        chatNpc(neutral, "Is there anything else I can help you with?")
    }

    private suspend fun Dialogue.ajjatBlackKnight() {
        chatPlayer(quiz, "Black Knight? Why are you here?")
        chatNpc(
            neutral,
            "Indeed I was, however their... methods... did not match with my ideals.. so I left. " +
                "Harrallak, recognising my talent as a warrior, took me in and offered me a job " +
                "here.",
        )
        chatPlayer(neutral, "Hmm... well if Harrallak trusts you, I guess I can.")
    }

    private suspend fun Dialogue.ajjatDummyRoom() {
        chatPlayer(quiz, "What's the Dummy Room all about?")
        chatNpc(
            neutral,
            "Ahh yes, the dummies. Another ingenious invention of the noble Dwarf Gamfred. " +
                "They're mechanical you see and pop up out of the floor. You have to hit them " +
                "with the correct attack mode before they disappear again.",
        )
        chatPlayer(quiz, "So how do I tell which one is which?")
        chatNpc(
            neutral,
            "There are two different ways. One indication is their colour, the other is the pose " +
                "and weapons they are holding, for instance, the one holding daggers you will " +
                "need to hit with a piercing attack.",
        )
        chatNpc(
            neutral,
            "In the room you will find a poster on the wall which can help you recognise each " +
                "different dummy.",
        )
        chatPlayer(happy, "That sounds ingenious!")
        chatNpc(
            neutral,
            "Indeed, you may find that you need several weapons to be successful 100% of the " +
                "time, but keep trying. The weapons shop upstairs may help you there.",
        )
    }

    private suspend fun Dialogue.ajjatCape() {
        if (ownsCape(ATTACK_CAPE, ATTACK_CAPE_TRIMMED)) {
            if (!choice2("Skillcape", true, "Hood", false)) {
                chatPlayer(quiz, "May I have another hood for my cape, please?")
                chatNpc(happy, "Most certainly, and free of charge!")
                player.invAddOrDrop(objRepo, ATTACK_HOOD)
                mesbox("Ajjat hands you another hood for your skillcape.")
                return
            }
        }
        chatPlayer(quiz, "May I buy a Skillcape of Attack, please?")
        chatNpc(
            neutral,
            "Most certainly; unfortunately being such a prestigious item, they are appropriately " +
                "expensive. I'm afraid I must ask you for the princely sum of 99000 gold!",
        )
        val pay =
            choice2(
                "99000 gold! Are you mad?",
                false,
                "I would gladly pay such a paltry sum for a splendid cape.",
                true,
            )
        if (!pay) {
            chatPlayer(shocked, "99000! Are you mad?")
            chatNpc(
                neutral,
                "Not at all; there are many other adventurers who would love the opportunity to " +
                    "purchase such a prestigious item! You can find me here if you change your " +
                    "mind.",
            )
            return
        }
        chatPlayer(happy, "I would gladly pay such a paltry sum for a splendid cape.")
        if (sellCape(ATTACK_CAPE, ATTACK_HOOD)) {
            chatNpc(happy, "Excellent! Wear that cape with pride my friend.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sloane
    private suspend fun Dialogue.sloane() {
        chatNpc(happy, "Ahhh, hello there, ${player.displayName}.")
        strengthTrainer(
            name = "Sloane",
            third = "That's a big axe!",
            story = { bigAxe() },
            farewell = "Be well, warrior ${player.displayName}.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Yadech_Strongarm
    private suspend fun Dialogue.yadech() {
        chatNpc(happy, "Ah, hello there, warrior.")
        strengthTrainer(
            name = "Sloane",
            third = "Why are you wielding a tree branch?",
            story = { treeBranch() },
            farewell = "Be well, warrior ${player.displayName}.",
        )
    }

    // TODO(content): [verify:in_game] the trainer's bow after "Bye!"; the wiki's "already bought
    //  a skillcape before" is read here as owning a Strength cape
    private suspend fun Dialogue.strengthTrainer(
        name: String,
        third: String,
        story: suspend Dialogue.() -> Unit,
        farewell: String,
    ) {
        val master = player.baseStrengthLvl >= MAX_LEVEL
        if (master) {
            chatNpc(
                happy,
                "Ah, but I can see you are already as strong as is possible! Perhaps you have " +
                    "come to me to purchase a Cape of Strength, and thus join the elite few who " +
                    "have mastered this exacting skill?",
            )
            chatNpc(
                neutral,
                "As an additional recognition of your achievement, the Cape of Strength can " +
                    "teleport you to our guild.",
            )
        }
        while (true) {
            val option =
                choice5(
                    if (master) {
                        "May I buy a Skillcape of Strength, please?"
                    } else {
                        "Can you tell me a bit about skillcapes?"
                    },
                    1,
                    "What can I do here?",
                    2,
                    third,
                    3,
                    CLAIM_TOKENS,
                    4,
                    "Bye!",
                    5,
                )
            when (option) {
                1 -> {
                    if (master) {
                        strengthCape(name)
                        return
                    }
                    strengthSkillcapes()
                }
                2 -> shotPut()
                3 -> story()
                4 -> {
                    claimTokens()
                    return
                }
                5 -> {
                    chatPlayer(happy, "Bye!")
                    chatNpc(happy, farewell)
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.strengthSkillcapes() {
        chatPlayer(quiz, "Can you tell me a bit about skillcapes?")
        chatNpc(
            neutral,
            "Sure. You can buy a skillcape when you reach level 99 in a skill. Apart from " +
                "boosting your statistics in that skill, they can also be used to perform some " +
                "pretty amazing emotes!",
        )
        chatNpc(
            neutral,
            "The Cape of Strength also provides you with unlimited teleports to our guild. Is " +
                "there anything else you would like to know?",
        )
    }

    private suspend fun Dialogue.shotPut() {
        chatPlayer(quiz, "What can I do here?")
        chatNpc(
            neutral,
            "Ahh, the shot put is a great test of strength and can be quite rewarding. Mind you " +
                "do it properly though, you might want to dust your hands with some powdery " +
                "substance first. It'll give better grip.",
        )
        chatPlayer(quiz, "I'll remember that. What should I use?")
        chatNpc(
            neutral,
            "I personally prefer ground ashes, it makes a nice fine powder and gives some really " +
                "good grip.",
        )
        chatPlayer(quiz, "Ok, what else?")
        chatNpc(
            neutral,
            "Basically you'll need to go in there, make sure you've got nothing cluttering up " +
                "your hands, pick up a shot and throw it.... depending upon your technique, you " +
                "can get quite long throws.",
        )
        chatPlayer(quiz, "What was your best?")
        chatNpc(laugh, "Ahh that would be telling...")
    }

    private suspend fun Dialogue.bigAxe() {
        chatPlayer(shocked, "That's a big axe!")
        chatNpc(happy, "Yes indeed it is. Have to be mighty strong to wield it too.")
        chatPlayer(confused, "But you don't look that strong!")
        chatNpc(
            neutral,
            "Maybe, maybe not, but I still had to beat a Barbarian to get it. Mind you, usually " +
                "they don't part with them. This was an unusual circumstance.",
        )
        chatPlayer(quiz, "Oh?")
        chatNpc(
            neutral,
            "I bet him he couldn't catch a squirrel whilst still holding his axe but that I " +
                "could... and that if I won I'd get his axe.",
        )
        chatPlayer(quiz, "What happened?")
        chatNpc(
            neutral,
            "He went running after the squirrel... nearly caught it too... it shot up a tree and " +
                "he tried to climb up it, only got a bit tangled up with his axe cause he " +
                "couldn't hang on to it at the same time... he fell out of the tree and it",
        )
        chatNpc(neutral, "was my turn....")
        chatNpc(
            laugh,
            "I simply went and chopped the tree down with this big old axe, still holding it. " +
                "The squirrel was so petrified it simply jumped straight out of the tree onto " +
                "me, I won the axe and let the poor creature go!",
        )
        chatPlayer(neutral, "I sense a moral in there somewhere.")
        chatNpc(
            neutral,
            "Aye indeed. Brawn isn't all you need to wield a big axe, brains are required too!",
        )
    }

    private suspend fun Dialogue.treeBranch() {
        chatPlayer(quiz, "Why are you wielding a tree branch?")
        chatNpc(neutral, "Well...there's a story to that. You see, I should have an axe but-")
        chatPlayer(quiz, "Sloane's axe?")
        chatNpc(angry, "My axe. He won it because of a squirrel.")
        chatPlayer(confused, "A squirrel?")
        chatNpc(
            angry,
            "He bet me that I couldn't catch the squirrel while still holding my axe, but that " +
                "he could... Thing is, he tricked me! When it was his turn, he just chopped the " +
                "tree down!",
        )
        chatPlayer(quiz, "I see. That doesn't explain why you have a tree branch though.")
        chatNpc(
            sad,
            "A barbarian warrior is bound to his axe. I cannot take up another, so I use part of " +
                "the tree that Sloane cut down as my weapon... It also reminds me of him.",
        )
        chatPlayer(sad, "He was a great warrior.")
        chatNpc(
            sad,
            "Yes, and a good friend, Still, he would want us to be happy that he is now with his " +
                "warrior brethren. Can I help you with anything else?",
        )
    }

    private suspend fun Dialogue.strengthCape(name: String) {
        if (ownsCape(STRENGTH_CAPE, STRENGTH_CAPE_TRIMMED)) {
            if (!choice2("Skillcape", true, "Hood", false)) {
                chatPlayer(quiz, "May I have another hood for my cape, please?")
                if (access.inv.isFull()) {
                    chatNpc(
                        neutral,
                        "You don't appear to have enough space in your inventory, come back and " +
                            "see me when you do.",
                    )
                    return
                }
                chatNpc(happy, "Most certainly, and free of charge!")
                player.invAddOrDrop(objRepo, STRENGTH_HOOD)
                mesbox("$name hands you another hood for your skillcape.")
                return
            }
        }
        chatPlayer(quiz, "May I buy a Skillcape of Strength, please?")
        chatNpc(
            neutral,
            "Hmm, yes, you certainly look strong enough. That'll be 99000 gold coins, please.",
        )
        if (!choice2("Sorry, that's just too much to pay.", false, "Sure.", true)) {
            chatPlayer(neutral, "Sorry, that's just too much to pay.")
            chatNpc(
                neutral,
                "Not at all; there are many other adventurers who would love the opportunity to " +
                    "purchase such a prestigious item! You can find me here if you change your " +
                    "mind.",
            )
            return
        }
        chatPlayer(happy, "Sure.")
        if (sellCape(STRENGTH_CAPE, STRENGTH_HOOD)) {
            chatNpc(happy, "Excellent! Wear that cape with pride my friend.")
        }
    }

    private fun Dialogue.ownsCape(cape: String, trimmed: String): Boolean =
        playerContainsObj(cape) || playerContainsObj(trimmed)

    private suspend fun Dialogue.sellCape(cape: String, hood: String): Boolean {
        if (access.inv.count(COINS) < CAPE_PRICE) {
            chatPlayer(sad, "But, unfortunately, I don't have enough money with me.")
            chatNpc(neutral, "Well, come back and see me when you do.")
            return false
        }
        if (access.inv.freeSpace() < 2) {
            chatNpc(
                neutral,
                "Unfortunately all Skillcapes are only available with a free hood, it's part of " +
                    "a skill promotion deal; buy one get one free, you know. So you'll need to " +
                    "free up some inventory space before I can sell you one.",
            )
            return false
        }
        if (!player.invTakeFee(CAPE_PRICE)) {
            return false
        }
        player.invAddOrDrop(objRepo, cape)
        player.invAddOrDrop(objRepo, hood)
        return true
    }

    private companion object {
        const val MAX_LEVEL = 99
        const val CAPE_PRICE = 99_000
        const val COINS = "obj.coins"
        const val ATTACK_CAPE = "obj.skillcape_attack"
        const val ATTACK_CAPE_TRIMMED = "obj.skillcape_attack_trimmed"
        const val ATTACK_HOOD = "obj.skillcape_attack_hood"
        const val STRENGTH_CAPE = "obj.skillcape_strength"
        const val STRENGTH_CAPE_TRIMMED = "obj.skillcape_strength_trimmed"
        const val STRENGTH_HOOD = "obj.skillcape_strength_hood"
    }
}
