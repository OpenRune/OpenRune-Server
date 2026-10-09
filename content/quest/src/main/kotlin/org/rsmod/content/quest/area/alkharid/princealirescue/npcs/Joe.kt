package org.rsmod.content.quest.area.alkharid.princealirescue.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.BEER
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.BEERS_NEEDED
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.NPC_JOE
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_ALI_ESCAPED
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_JOE_DRUNK
import org.rsmod.content.quest.area.alkharid.princealirescue.PrinceAliRescueQuest.Companion.STAGE_PREPARED
import org.rsmod.content.quest.manager.menu
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Joe, the Prince's guard. He won't say a word until Leela has the player looking for his
 * weakness, which turns out to be cold beer: three of them leave him too drunk to notice anything.
 */
class Joe @Inject constructor(private val princeAli: PrinceAliRescueQuest) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1(NPC_JOE) { startDialogue(it.npc) { joe() } }
    }

    private suspend fun Dialogue.joe() {
        val stage = princeAli.stage(player)
        when {
            stage < STAGE_PREPARED -> {
                chatPlayer(quiz, "Hi. Who are you guarding here?")
                chatNpc(
                    neutral,
                    "Can't say. It's all very secret. You should get out of here. I am not supposed " +
                        "to talk while I guard.",
                )
            }
            stage == STAGE_PREPARED -> {
                chatPlayer(happy, "Hi there.")
                chatNpc(quiz, "What do you want?")
                topics()
            }
            stage < STAGE_ALI_ESCAPED -> {
                chatNpc(drunk, "Halt! Who goes there?")
                chatPlayer(happy, "Hello friend. I'm just here to rescue the Prince, if thats okay?")
                chatNpc(drunk, "Thatsh a funny joke. You are lucky I'm shober. Go in peace, friend.")
            }
            else -> {
                chatNpc(drunk, "Did yoush say something about shome Prince?")
                chatPlayer(neutral, "No.")
                chatNpc(drunk, "Oh... okay.")
            }
        }
    }

    private suspend fun Dialogue.topics() {
        while (true) {
            val topic =
                menu(
                    buildList {
                        if (BEER in player.inv) add("I have some beer here. Fancy one?" to Topic.Beer)
                        add("Tell me about the life of a guard." to Topic.Life)
                        add("What did you want to be when you were a boy?" to Topic.Boy)
                        add("I'd better go." to Topic.Leave)
                    },
                )
            when (topic) {
                Topic.Beer -> {
                    beer()
                    return
                }
                Topic.Life -> {
                    if (lifeOfAGuard()) return
                }
                Topic.Boy -> {
                    if (boyhood()) return
                }
                Topic.Leave -> {
                    leave()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.beer() {
        chatPlayer(happy, "I have some beer here. Fancy one?")
        if (!princeAli.joeHadBeer(player)) {
            chatNpc(happy, "Ah, that would be lovely. Only one though, just to wet my throat.")
            chatPlayer(neutral, "Of course. It must be tough being here without a drink.")
            if (access.invDel(access.inv, BEER).failure) {
                return
            }
            princeAli.setJoeHadBeer(player)
            objbox(BEER, "You hand a beer to the guard. He drinks it in seconds.")
            chatNpc(happy, "That was perfect! I can't thank you enough.")
        }
        chatPlayer(quiz, "How are you? Still okay? Not too drunk?")
        if (player.inv.count(BEER) < BEERS_NEEDED - 1) {
            chatNpc(
                neutral,
                "No, I don't get drunk from only one drink. I reckon I'd need at least two more " +
                    "for that. Still, thanks for the beer.",
            )
            return
        }
        chatPlayer(happy, "Would you care for another beer, my friend?")
        chatNpc(neutral, "I'd better not. I don't want to be drunk on duty.")
        chatPlayer(happy, "Here, just keep these for later. I hate to see a thirsty guard.")
        if (access.invDel(access.inv, BEER, BEERS_NEEDED - 1).failure) {
            return
        }
        princeAli.setStage(access, STAGE_JOE_DRUNK)
        doubleobjbox(
            BEER,
            BEER,
            "You hand two more beers to the guard. He takes a sip of one, and then he quickly " +
                "drinks them both.",
        )
        chatNpc(
            drunk,
            "Franksh! That wash jusht what I need to shtay on guard. No more beersh, I don't want " +
                "to get drunk.",
        )
    }

    /** Returns true once the conversation has ended. */
    private suspend fun Dialogue.lifeOfAGuard(): Boolean {
        chatPlayer(quiz, "Tell me about the life of a guard.")
        chatNpc(neutral, "Well, the hours are good, but most of those hours are a drag.")
        chatNpc(
            sad,
            "Sometimes I wonder if I should have spent more time learning when I was a young boy. " +
                "Maybe I wouldn't be here now, scared of Keli.",
        )
        return if (
            choice2(
                "What did you want to be when you were a boy?",
                true,
                "I'd better go.",
                false,
            )
        ) {
            boyhood()
        } else {
            leave()
            true
        }
    }

    /** Returns true once the conversation has ended. */
    private suspend fun Dialogue.boyhood(): Boolean {
        chatPlayer(quiz, "What did you want to be when you were a boy?")
        chatNpc(
            happy,
            "Well, I loved to sit by the lake, with my toes in the water. I'd shoot the fish with " +
                "my bow and arrow.",
        )
        chatPlayer(confused, "That's a strange hobby for a boy.")
        chatNpc(neutral, "It kept us from goblin hunting, which was what most boys did.")
        chatNpc(shifty, "Hang on... Why do you ask? What do you want?")
        when (
            choice3(
                "Hey, chill out. I won't cause you trouble.", 1,
                "Tell me about the life of a guard.", 2,
                "I'd better go.", 3,
            )
        ) {
            1 -> return chillOut()
            2 -> return lifeOfAGuard()
            else -> {
                leave()
                return true
            }
        }
    }

    private suspend fun Dialogue.chillOut(): Boolean {
        chatPlayer(neutral, "Hey, chill out. I won't cause you trouble.")
        chatNpc(neutral, "Sorry, it's hard to relax when I'm on duty. Stress of the job, and all.")
        chatPlayer(quiz, "So why do you do it?")
        chatNpc(happy, "There's good money in it, and some of the shouting I rather like.")
        chatNpc(angry, "RESISTANCE IS USELESS!")
        while (true) {
            when (
                choice4(
                    "So what do you buy with your great wages?", 1,
                    "Tell me about the life of a guard.", 2,
                    "Would you be interested in making a little more money?", 3,
                    "I'd better go.", 4,
                )
            ) {
                1 -> {
                    chatPlayer(quiz, "So what do you buy with your great wages?")
                    chatNpc(
                        happy,
                        "Really, after working here, there's only time for a drink or three. All us " +
                            "guards go to the same pub and drink ourselves stupid.",
                    )
                    chatNpc(happy, "It's what I enjoy these days. I can't resist the sight of a really cold beer.")
                    return false
                }
                2 -> return lifeOfAGuard()
                3 -> {
                    chatPlayer(shifty, "Would you be interested in making a little more money?")
                    chatNpc(
                        angry,
                        "What? Are you trying to bribe me? I may not be a great guard, but I am " +
                            "loyal. How dare you try to bribe me!",
                    )
                    chatPlayer(
                        worried,
                        "No, no, you've got the wrong idea, totally. I just wondered if you wanted " +
                            "some part-time bodyguard work.",
                    )
                    chatNpc(neutral, "Oh... sorry. No, I don't need money. As long as you were not offering me a bribe.")
                }
                else -> {
                    leave()
                    return true
                }
            }
        }
    }

    private suspend fun Dialogue.leave() {
        chatPlayer(neutral, "I'd better go.")
        chatNpc(
            worried,
            "Thanks, I appreciate that. Talking on duty can be punished by having your mouth " +
                "stitched up. These are tough people, make no mistake.",
        )
    }

    private enum class Topic {
        Beer,
        Life,
        Boy,
        Leave,
    }
}
