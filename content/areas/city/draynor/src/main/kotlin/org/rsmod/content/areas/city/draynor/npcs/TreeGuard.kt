package org.rsmod.content.areas.city.draynor.npcs

import dev.openrune.types.MesAnimType
import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Tree_(Draynor_guard)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:timers] The bowl drops about 10 seconds after the stew
//  (https://oldschool.runescape.wiki/w/Tree_(Draynor_guard)); it drops at the end of the
//  dialogue here, so "I think I should leave him in peace to finish his stew!" is unreachable.
class TreeGuard @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    private var Player.bankJob by intVarBit("varbit.wom_bankjob")

    override fun ScriptContext.startup() {
        onOpLoc1(TREE) { startDialogue { chop() } }
        onOpLoc2(TREE) { startDialogue { talk() } }
        onOpLocU(TREE, STEW) { startDialogue { useStew() } }
        onOpLocU(TREE) { startDialogue { useOther() } }
    }

    private suspend fun Dialogue.talk() {
        chatPlayer(quiz, "Hello?")
        if (access.inv.count(STEW) > 0) {
            guard(happy, "Yes, what do you... I say, is that stew for me?")
            if (choice2("Ok, take the stew!", true, "No, I want to keep this stew!", false)) {
                chatPlayer(happy, "Ok, take the stew!")
                takeStew()
            } else {
                chatPlayer(neutral, "No, I want to keep this stew!")
                guard(
                    neutral,
                    "Fair enough. But if you change your mind, I'll probably still be here.",
                )
            }
            return
        }
        guard(angry, "Ssshhh! What do you want?")
        chatPlayer(laugh, "Well, it's not every day you see a man up a tree.")
        guard(angry, "I'm trying to observe a suspect. Leave me alone!")
        if (player.bankJob == 0) {
            chatPlayer(quiz, "What's this all about?")
            guard(
                neutral,
                "Hadn't you heard? You'd better go and talk to the other guard, the one by the " +
                    "bank door. He'll tell you all about it.",
            )
            guard(
                neutral,
                "Go on, ask him what's wrong with the bank wall. He loves when people ask him " +
                    "that.",
            )
            chatPlayer(neutral, "Ok, if you say so..")
            return
        }
        var askedRobbery = false
        while (true) {
            val options = buildList {
                if (!askedRobbery) add("This is about the bank robbery, right?" to 1)
                add("You're not being very subtle up there." to 2)
                add("Can I do anything to help?" to 3)
            }
            when (menuOf(options)) {
                1 -> {
                    askedRobbery = true
                    chatPlayer(quiz, "This is about the bank robbery, right?")
                    guard(
                        neutral,
                        "Yes, that's right. We're keeping the suspect under tight observation " +
                            "for the moment.",
                    )
                    chatPlayer(quiz, "Can't you just... I dunno... arrest him?")
                    guard(
                        neutral,
                        "I'm not meant to discuss the case. You know what confidentiality rules " +
                            "are like.",
                    )
                    chatPlayer(neutral, "Fair enough.")
                }
                2 -> {
                    chatPlayer(laugh, "You're not being very subtle up there.")
                    guard(
                        angry,
                        "I'd be doing a lot better if nits like you didn't come crowding around " +
                            "me all day!",
                    )
                    chatPlayer(laugh, "But your legs are hanging down!")
                    guard(angry, "Go away!")
                    chatPlayer(neutral, "Please yourself!")
                    return
                }
                else -> {
                    chatPlayer(quiz, "Can I do anything to help?")
                    guard(
                        happy,
                        "That's very kind of you. I'd rather like a nice bowl of stew, if you " +
                            "could fetch me one. I don't get many meal breaks.",
                    )
                    chatPlayer(quiz, "You want a bowl of stew?")
                    guard(happy, "If you wouldn't mind...")
                    chatPlayer(neutral, "I'll think about it!")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.menuOf(options: List<Pair<String, Int>>): Int =
        if (options.size == 2) {
            choice2(options[0].first, options[0].second, options[1].first, options[1].second)
        } else {
            choice3(
                options[0].first,
                options[0].second,
                options[1].first,
                options[1].second,
                options[2].first,
                options[2].second,
            )
        }

    private suspend fun Dialogue.useStew() {
        chatPlayer(happy, "Here's some stew.")
        takeStew()
    }

    private suspend fun Dialogue.takeStew() {
        if (access.invDel(access.inv, STEW).failure) return
        guard(happy, "Gosh, that's very kind of you! Here, have a few coins for your trouble...")
        player.invAddOrDrop(objRepo, COINS, STEW_REWARD)
        mesbox("The guard eats the stew")
        mesbox("The guard drops the empty bowl out of the tree!")
        val bowl = Obj.fromOwner(player, player.coords, BOWL, 1)
        objRepo.add(bowl, player.lootDropDuration ?: constants.lootdrop_duration)
    }

    private suspend fun Dialogue.useOther() {
        guard(angry, "Why the demoninikin are you waving THAT at me?")
        chatPlayer(sad, "Sorry, I thought you'd like it.")
        guard(
            angry,
            "Erm... No thanks! Now, if you don't mind, I'm trying to observe this suspect!",
        )
        chatPlayer(sad, "Sorry!")
    }

    private suspend fun Dialogue.chop() {
        if (!access.playerContainsContent(AXE_CONTENT)) {
            access.mes("You need an axe to chop down this tree.")
            return
        }
        guard(angry, CHOP_LINES[access.random.of(0, CHOP_LINES.lastIndex)])
    }

    private suspend fun Dialogue.guard(mesanim: MesAnimType, text: String) =
        chatNpcSpecific("Guard", GUARD_CHATHEAD, mesanim, text)

    private companion object {
        const val TREE = "loc.wom_tree"
        const val STEW = "obj.stew"
        const val BOWL = "obj.bowl_empty"
        const val COINS = "obj.coins"
        const val STEW_REWARD = 30
        const val AXE_CONTENT = "content.woodcutting_axe"
        const val GUARD_CHATHEAD = "npc.wom_bankguard"
        val CHOP_LINES =
            listOf(
                "Ooooch!",
                "Oi!",
                "Watch what you're doing with that axe, you nit!",
                "You'll blow my cover! I'm meant to be hidden!",
                "Will you stop that?",
                "Don't draw attention to me!",
                "Hey - gerroff me!",
                "Ow! That really hurt!",
            )
    }
}
