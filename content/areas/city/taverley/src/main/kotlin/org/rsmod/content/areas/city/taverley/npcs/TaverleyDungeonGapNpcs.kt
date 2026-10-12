package org.rsmod.content.areas.city.taverley.npcs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [skill:slayer] Slieve and Eve stopping players without a task from attacking
//  their dragons
class TaverleyDungeonGapNpcs @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.slayer_blackdragons_guardian") { startDialogue(it.npc) { slieve() } }
        onOpNpc1("npc.slayer_bluedragons_guardian") { startDialogue(it.npc) { eve() } }
        onOpNpc1("npc.velrak_the_explorer") { startDialogue(it.npc) { velrak() } }
    }

    private fun Dialogue.hasSlayerTask(npc: String): Boolean {
        val task = access.vars["varp.slayer_target"]
        if (task == 0 || access.vars["varp.slayer_count"] <= 0) {
            return false
        }
        val type = ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)) ?: return false
        return type.paramOrNull(BaseParams.slayer_task_id) == task
    }

    // https://oldschool.runescape.wiki/w/Transcript:Slieve
    private suspend fun Dialogue.slieve() {
        chatNpc(quiz, "Hello. Are you here for my black dragons?")
        val onTask = hasSlayerTask(BLACK_DRAGON)
        while (true) {
            val option =
                choice3(
                    if (onTask) {
                        "I have a Slayer assignment to kill those, yes."
                    } else {
                        "They're not my Slayer target."
                    },
                    1,
                    "Why are you here asking about that?",
                    2,
                    "Excuse me, I've got to go.",
                    3,
                )
            when (option) {
                1 -> {
                    if (onTask) {
                        chatPlayer(neutral, "I have a Slayer assignment to kill those, yes.")
                        chatNpc(neutral, "Fair enough. Do what you have to do.")
                        return
                    }
                    chatPlayer(neutral, "They're not my Slayer target.")
                    chatNpc(
                        happy,
                        "Oh, good. In that case you're welcome to go and admire them, but don't " +
                            "get any ideas about killing them.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Why are you here asking about that?")
                    chatNpc(
                        neutral,
                        "Because I like my black dragons, and I had to work hard to get access " +
                            "to their lair, and I don't want people using them for training " +
                            "without a reason.",
                    )
                    chatNpc(
                        neutral,
                        "So if one of the Slayer Masters sent you to slay black dragons, that's " +
                            "fine, but otherwise you should go and train elsewhere.",
                    )
                    chatNpc(
                        neutral,
                        "I'd rather people didn't train on my dragons at all, but I've got a " +
                            "distant cousin who's a Slayer Master, so I've got to support the " +
                            "family business.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "Excuse me, I've got to go.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Eve
    private suspend fun Dialogue.eve() {
        chatNpc(neutral, "What is it you want?")
        while (true) {
            val option =
                choice4(
                    "Can I kill the dragons up here?",
                    1,
                    "What are you here for?",
                    2,
                    "Why are you dressed like that?",
                    3,
                    "Never mind.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Can I kill the dragons up here?")
                    if (hasSlayerTask(BLUE_DRAGON)) {
                        chatNpc(
                            neutral,
                            "I suppose that's okay since you've got a Slayer assignment. But " +
                                "when you've finished it, leave the rest for me!",
                        )
                    } else {
                        chatNpc(
                            angry,
                            "Don't even think about it - these dragons up here are for me!",
                        )
                    }
                    chatNpc(
                        neutral,
                        "There's a Slayer Master in my family, so I don't stop people doing " +
                            "their Slayer assignments, but otherwise these dragons are for me, " +
                            "not you.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What are you here for?")
                    chatNpc(
                        neutral,
                        "Dragon-slaying, of course. Do I look like one of those serfs who " +
                            "wanders around scavenging scales off the floor? I'm just having a " +
                            "rest now, before I get back to the fight.",
                    )
                    chatNpc(
                        neutral,
                        "My cousin Slieve built the access routes through this blue dragon " +
                            "lair. But she's only really interested in black dragons, not blue " +
                            "ones, so she said I could have this area for myself.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Why are you dressed like that?")
                    chatNpc(neutral, "It's hot in here with all the dragonfire.")
                    chatPlayer(quiz, "Don't you need armour, though?")
                    chatNpc(neutral, "I've got wizard boots on, see? What more would I want?")
                    chatPlayer(
                        neutral,
                        "Well, maybe you'd want something to help you fight the dragons? " +
                            "Wizard boots don't sound very useful.",
                    )
                    chatNpc(
                        neutral,
                        "They're useful enough for what I'm doing. Anyway, moving on...",
                    )
                }
                else -> {
                    chatPlayer(neutral, "Never mind.")
                    chatNpc(neutral, "Off you go.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Velrak_the_explorer
    private suspend fun Dialogue.velrak() {
        if (player.inv.count(DUSTY_KEY) > 0) {
            chatPlayer(quiz, "Are you still here?")
            chatNpc(
                worried,
                "Yes... I'm still plucking up the courage to run out past those Black Knights...",
            )
            return
        }
        chatNpc(happy, "Thank you for rescuing me! It isn't very comfy in this cell.")
        val option =
            choice2(
                "So... do you know anywhere good to explore?",
                1,
                "Do I get a reward for freeing you?",
                2,
            )
        if (option == 2) {
            chatPlayer(quiz, "Do I get a reward? For freeing you and all...")
            chatNpc(
                sad,
                "Well... not really... The Black Knights took all of my stuff before throwing " +
                    "me in here to rot!",
            )
            return
        }
        chatPlayer(quiz, "So... do you know anywhere good to explore?")
        chatNpc(
            neutral,
            "Well, this dungeon was quite good to explore ...until I got captured, anyway. I was " +
                "given a key to an inner part of this dungeon by a mysterious cloaked stranger!",
        )
        chatNpc(
            neutral,
            "It's rather tough for me to get that far into the dungeon however... I just keep " +
                "getting captured! Would you like to give it a go?",
        )
        val accept = choice2("Yes please!", true, "No, it's too dangerous for me too.", false)
        if (!accept) {
            chatPlayer(worried, "No, it's too dangerous for me too.")
            chatNpc(neutral, "I don't blame you!")
            return
        }
        chatPlayer(happy, "Yes please!")
        player.invAddOrDrop(objRepo, DUSTY_KEY)
        objbox(DUSTY_KEY, "Velrak reaches somewhere mysterious and passes you a key.")
    }

    private companion object {
        const val BLACK_DRAGON = "npc.black_dragon"
        const val BLUE_DRAGON = "npc.blue_dragon"
        const val DUSTY_KEY = "obj.dusty_key"
    }
}
