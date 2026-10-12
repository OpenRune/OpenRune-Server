package org.rsmod.content.areas.city.varlamore.camtorum.npcs

import dev.openrune.util.Wearpos
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CamTorumTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in CITIZENS) onOpNpc1(npc) { startDialogue(it.npc) { citizen() } }
        for (npc in CHILDREN) onOpNpc1(npc) { startDialogue(it.npc) { child() } }
        for (npc in WORKERS) onOpNpc1(npc) { startDialogue(it.npc) { worker() } }
        for (npc in GUARDS) onOpNpc1(npc) { startDialogue(it.npc) { guard() } }
        onOpNpc1("npc.cam_torum_citizen_drunk_1") { startDialogue(it.npc) { drunk() } }
        onOpNpc1("npc.cam_torum_citizen_drunk_2") { startDialogue(it.npc) { drunk() } }
        onOpNpc1("npc.cam_torum_citizen_cuica") { startDialogue(it.npc) { cuica() } }
        onOpNpc1("npc.cam_torum_friendly_miner") { startDialogue(it.npc) { callio() } }
        onOpNpc1("npc.cam_torum_seer") { startDialogue(it.npc) { seer() } }
        onOpNpc1("npc.cam_torum_shop_barber") { startDialogue(it.npc) { sonalo() } }
        onOpNpc1("npc.vmq4_teumo_wife_vis") { startDialogue(it.npc) { citizen() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Citizen_(Cam_Torum)
    // https://oldschool.runescape.wiki/w/Transcript:Galna
    private suspend fun Dialogue.citizen() {
        chatPlayer(happy, "Hello there. How's it going?")
        when (access.random.of(5)) {
            0 -> chatNpc(angry, "Another human? Do what you need to and then be gone from here.")
            1 -> chatNpc(angry, "Every day they let more of your kind in.")
            2 -> chatNpc(angry, "Hmph!")
            3 -> chatNpc(angry, "I don't talk to you lot.")
            else -> chatNpc(angry, "Not interested!")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Child_(Cam_Torum)
    private suspend fun Dialogue.child() {
        chatPlayer(happy, "Hello there.")
        when (access.random.of(5)) {
            0 -> chatNpc(neutral, "I don't know you.")
            1 -> chatNpc(angry, "I don't want to talk to you.")
            2 -> chatNpc(worried, "I shouldn't speak with humans.")
            3 -> chatNpc(worried, "I'm not allowed to talk to your kind.")
            else -> chatNpc(worried, "Mummy said I should stay away from humans.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Worker_(Cam_Torum)
    private suspend fun Dialogue.worker() {
        chatPlayer(happy, "Hello there.")
        when (access.random.of(5)) {
            0 -> chatNpc(angry, "Leave me alone. I have work to do.")
            1 -> chatNpc(angry, "Whatever you want, the answer's no.")
            2 -> chatNpc(angry, "I don't have time to talk to you.")
            3 -> chatNpc(neutral, "Please don't disturb me.")
            else -> chatNpc(neutral, "I'm busy right now.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_(Cam_Torum)
    private suspend fun Dialogue.guard() {
        when (access.random.of(5)) {
            0 -> chatNpc(angry, "Be on your way.")
            1 -> chatNpc(angry, "You don't want to mess with me, human.")
            2 ->
                chatNpc(
                    angry,
                    "If you're thinking of trying anything, you'll quickly find yourself " +
                        "regretting it.",
                )
            3 -> chatNpc(angry, "I'm watching you, human.")
            else -> chatNpc(angry, "Move along.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Funbo
    // https://oldschool.runescape.wiki/w/Transcript:Kalko
    // TODO(content): [system:treasure_trails] Funbo's elite clue step
    private suspend fun Dialogue.drunk() {
        chatPlayer(happy, "Hey there.")
        when (access.random.of(5)) {
            0 -> {
                chatNpc(drunk, "Yes please! How many do you have?")
                chatPlayer(confused, "How many what?")
                chatNpc(drunk, "I'll take the lot!")
                chatPlayer(confused, "Right...")
            }
            1 -> {
                chatPlayer(happy, "Hey there.")
                chatNpc(drunk, "Hello! I've had a few to drink!")
            }
            2 -> chatNpc(drunk, "I haven't had that many, have I?")
            3 -> {
                chatNpc(
                    drunk,
                    "'Oh there once was a dwarf named Tonalo! He started a fight with a buffalo!'",
                )
                chatPlayer(confused, "What?")
                chatNpc(
                    drunk,
                    "'He kicked and he punched, it was quite the show! But he tripped on a rock " +
                        "and stubbed his toe!'",
                )
            }
            else -> chatNpc(drunk, "What? I didn't even do it!")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cuica
    private suspend fun Dialogue.cuica() {
        chatNpc(angry, "Human.")
        chatPlayer(quiz, "So what do you do?")
        chatNpc(neutral, "Drink.")
        chatPlayer(quiz, "For a living?")
        chatNpc(angry, "Is that a problem?")
        chatPlayer(neutral, "No, but I can't imagine it pays well.")
        chatNpc(
            angry,
            "As you've not taken the hint, I'm a hunter. I go out and kill things, sell the " +
                "results, and then come here to drink.",
        )
        chatPlayer(happy, "Sounds like a fun and varied routine.")
        chatNpc(angry, "It is. Now, if you'll excuse me.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Callio
    private suspend fun Dialogue.callio() {
        chatNpc(neutral, "Can I help you?")
        var asked = false
        while (true) {
            val options = buildList {
                add("What do you do here?" to 1)
                if (BLESSED_BONE_SHARD in player.inv) add("What are these weird chippings?" to 2)
                if (CALCIFIED_DEPOSIT in player.inv) {
                    add("Can you have a look at this chunk of rock?" to 3)
                }
                if (CALCIFIED_MOTH in player.inv) add("I found this weird moth." to 4)
                add((if (asked) "I'd better get going." else "No, thanks.") to 5)
            }
            when (choose(options)) {
                1 -> callioMining()
                2 -> {
                    chatPlayer(quiz, "What are these weird chippings?")
                    objbox(BLESSED_BONE_SHARD, "You show Callio the bone shards.")
                    chatNpc(
                        neutral,
                        "Bone shards! Totally pointless if you ask me, but I hear the humans up " +
                            "atop the mountain quite like them.",
                    )
                    chatNpc(
                        neutral,
                        "Apparently they offer them up as some sort of sacrifice, but I'm not " +
                            "too sure on the specifics. It's not like I ever go up there myself.",
                    )
                    chatPlayer(happy, "Thanks!")
                }
                3 -> {
                    chatPlayer(quiz, "Can you have a look at this chunk of rock?")
                    objbox(CALCIFIED_DEPOSIT, "You show Callio the calcified deposit.")
                    chatNpc(
                        neutral,
                        "That's quite the chunk indeed. It looks to be a big mass of calcified " +
                            "rock.",
                    )
                    chatNpc(
                        neutral,
                        "I reckon you'll be able to break it up on an anvil. Perhaps there might " +
                            "be something interesting inside...",
                    )
                    chatPlayer(happy, "Thanks! I'll go smash it open.")
                    chatNpc(laugh, "Smashing!")
                }
                4 -> {
                    chatPlayer(quiz, "I found this weird moth.")
                    objbox(CALCIFIED_MOTH, "You show Callio the moth.")
                    chatNpc(
                        shocked,
                        "Strange... I've never seen one of those before. That's quite the find.",
                    )
                    chatPlayer(
                        confused,
                        "Of all the creatures to find stuck in a wall, I didn't expect a moth.",
                    )
                    chatNpc(
                        neutral,
                        "Moths carry a lot of magical power within this mountain. You can " +
                            "probably crush it to consume whatever power is left inside.",
                    )
                    chatPlayer(worried, "Is that safe?")
                    chatNpc(laugh, "Only one way to find out!")
                }
                else -> {
                    chatPlayer(neutral, if (asked) "I'd better get going." else "No, thanks.")
                    return
                }
            }
            asked = true
        }
    }

    private suspend fun Dialogue.callioMining() {
        chatPlayer(quiz, "What do you do here?")
        chatNpc(
            neutral,
            "I'm just mining... Problem is, it's a struggle to get past all these fossil looking " +
                "things.",
        )
        chatPlayer(quiz, "Can I help?")
        chatNpc(
            neutral,
            "The others wouldn't like it if a human got involved... You lot tend to take too " +
                "much from the rocks.",
        )
        chatPlayer(quiz, "But what if I were to sort out these fossil things you mentioned?")
        chatNpc(neutral, "Hmm... I shouldn't... And yet...")
        chatNpc(
            neutral,
            "Okay, I'll tell you what... If you mine through these calcified deposits, you can " +
                "keep whatever you get from them.",
        )
        chatPlayer(quiz, "Are you sure?")
        chatNpc(
            neutral,
            "Yeah, why not. I'm tired of getting sprayed with all the water that leaks from " +
                "these walls anyway. Not to mention it just brings out lots of extra useless " +
                "bits with it!",
        )
        chatPlayer(quiz, "So the water makes the walls even more lucrative?")
        chatNpc(neutral, "If you consider pointless fossils lucrative, then I suppose.")
        chatPlayer(happy, "I see. I'll get to work then! See what I can do to help.")
    }

    private suspend fun Dialogue.choose(options: List<Pair<String, Int>>): Int =
        when (options.size) {
            2 -> choice2(options[0].first, options[0].second, options[1].first, options[1].second)
            3 ->
                choice3(
                    options[0].first,
                    options[0].second,
                    options[1].first,
                    options[1].second,
                    options[2].first,
                    options[2].second,
                )
            4 ->
                choice4(
                    options[0].first,
                    options[0].second,
                    options[1].first,
                    options[1].second,
                    options[2].first,
                    options[2].second,
                    options[3].first,
                    options[3].second,
                )
            else ->
                choice5(
                    options[0].first,
                    options[0].second,
                    options[1].first,
                    options[1].second,
                    options[2].first,
                    options[2].second,
                    options[3].first,
                    options[3].second,
                    options[4].first,
                    options[4].second,
                )
        }

    // https://oldschool.runescape.wiki/w/Transcript:Seer_(Cam_Torum)
    private suspend fun Dialogue.seer() {
        chatNpc(neutral, "Yes?")
        chatPlayer(quiz, "What are you doing?")
        chatNpc(neutral, "Browsing.")
        chatPlayer(quiz, "What for?")
        chatNpc(neutral, "In a magic shop? Magical items, oddly enough.")
        chatPlayer(quiz, "Anything in particular?")
        chatNpc(neutral, "Whatever Highseer Vandra requires.")
        chatPlayer(confused, "Highseer Vandra?")
        chatNpc(angry, "Enough questions. Leave me to browse in peace.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sonalo
    // TODO(content): [system:makeover] open the hairstyle and facial hair interfaces; the
    //  makeover code lives in the falador module's HairdresserScript
    private suspend fun Dialogue.sonalo() {
        chatNpc(
            neutral,
            "Hmm... You're not my usual kind of customer, but I reckon I can still make this " +
                "work. What will it be? Haircut or a shave?",
        )
        val option =
            choice3(
                "I'd like a haircut please.",
                1,
                "I'd like a shave please.",
                2,
                "I'm good, thanks.",
                3,
            )
        when (option) {
            1 -> chatPlayer(happy, "I'd like a haircut please.")
            2 -> chatPlayer(happy, "I'd like a shave please.")
            else -> {
                chatPlayer(neutral, "I'm good, thanks.")
                chatNpc(neutral, "Suit yourself.")
                return
            }
        }
        chatNpc(neutral, "Then let's get to it.")
        val hat = player.worn[Wearpos.Hat.slot] ?: return
        objbox(hat, "You should take off your hat if you want to get a haircut!")
    }

    private companion object {
        const val BLESSED_BONE_SHARD = "obj.blessed_bone_shard"
        const val CALCIFIED_DEPOSIT = "obj.calcified_deposit"
        const val CALCIFIED_MOTH = "obj.varlamore_mining_teleport"

        val CITIZENS =
            listOf(
                "npc.cam_torum_citizen_m_1",
                "npc.cam_torum_citizen_m_2",
                "npc.cam_torum_citizen_m_3",
                "npc.cam_torum_citizen_m_4",
                "npc.cam_torum_citizen_m_east",
                "npc.cam_torum_citizen_f_1",
                "npc.cam_torum_citizen_f_2",
                "npc.cam_torum_citizen_f_3",
                "npc.cam_torum_citizen_f_4",
                "npc.cam_torum_citizen_f_west",
                "npc.cam_torum_citizen_rich_m_1",
                "npc.cam_torum_citizen_rich_m_2",
                "npc.cam_torum_citizen_rich_m_3",
                "npc.cam_torum_citizen_rich_m_4",
                "npc.cam_torum_citizen_rich_m_5",
                "npc.cam_torum_citizen_rich_f_1",
                "npc.cam_torum_citizen_rich_f_2",
                "npc.cam_torum_citizen_rich_f_3",
                "npc.cam_torum_citizen_rich_f_4",
                "npc.cam_torum_citizen_rich_f_5",
            )

        val CHILDREN =
            listOf(
                "npc.cam_torum_child_m_1",
                "npc.cam_torum_child_m_2",
                "npc.cam_torum_child_f_1",
                "npc.cam_torum_child_f_2",
            )

        val WORKERS =
            listOf(
                "npc.cam_torum_worker_m_1",
                "npc.cam_torum_worker_m_2",
                "npc.cam_torum_worker_f_1",
                "npc.cam_torum_worker_f_2",
                "npc.cam_torum_worker_m_1_pickaxe",
                "npc.cam_torum_worker_m_2_pickaxe",
                "npc.cam_torum_worker_f_1_pickaxe",
                "npc.cam_torum_worker_f_2_pickaxe",
            )

        val GUARDS =
            listOf(
                "npc.cam_torum_guard_1",
                "npc.cam_torum_guard_1_north",
                "npc.cam_torum_guard_1_south",
                "npc.cam_torum_guard_1_east",
                "npc.cam_torum_guard_1_west",
                "npc.cam_torum_guard_2",
                "npc.cam_torum_guard_2_north",
                "npc.cam_torum_guard_2_south",
                "npc.cam_torum_guard_2_east",
                "npc.cam_torum_guard_2_west",
                "npc.cam_torum_guard_3",
                "npc.cam_torum_guard_3_north",
                "npc.cam_torum_guard_3_south",
                "npc.cam_torum_guard_3_east",
                "npc.cam_torum_guard_3_west",
            )
    }
}
