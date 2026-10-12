package org.rsmod.content.areas.wilderness.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc4
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Skully (no loot keys access)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:loot_keys] take 1,000,000 coins to unlock loot keys, the "Can I change
//  how these loot keys work?" settings, the claimed loot totals and the Value option
// TODO(content): [system:pvp_kill_count] "Yeah?" for players with a Wilderness player kill
// TODO(content): [system:ironman] the ironman variant of "How does the chest work?"
class Skully : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.wildy_hub_refugee1") { startDialogue(it.npc) { skully() } }
        onOpNpc4("npc.wildy_hub_refugee1") { startDialogue(it.npc) { settings() } }
    }

    private suspend fun Dialogue.skully() {
        chatNpc(
            happy,
            "Eyup. They call me Skully. I run this Wilderness Loot Chest. What can I do for you?",
        )
        while (true) {
            val option =
                choice5(
                    "How does the chest work?",
                    1,
                    "Can I have access to the chest?",
                    2,
                    "How much loot have I claimed?",
                    3,
                    "Who are you, exactly?",
                    4,
                    "Goodbye.",
                    5,
                )
            when (option) {
                1 -> howDoesTheChestWork()
                2 -> if (!chestAccess()) return
                3 -> {
                    chatPlayer(quiz, "How much loot have I claimed?")
                    chatNpc(
                        neutral,
                        "Well, seeing as you haven't claimed a key yet, I reckon you've claimed " +
                            "a total of 0gp worth of loot.",
                    )
                }
                4 -> whoAreYou()
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    chatNpc(neutral, "I'll be here if you need anything.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.howDoesTheChestWork() {
        chatNpc(
            neutral,
            "Well, you know how when you kill someone all of their stuff just ends up in a pile " +
                "on the floor and it's really hard to pick it all up quickly?",
        )
        chatPlayer(neutral, "Actually, I've never killed anyone else.")
        chatNpc(
            confused,
            "...Oh. Well, when you do, all of their stuff just ends up in a pile on the floor " +
                "and it's really hard to pick it all up quickly.",
        )
        chatNpc(
            happy,
            "So, I made an enchantment that teleports the loot to another dimension instead!",
        )
        chatPlayer(quiz, "How does that help?")
        chatNpc(
            happy,
            "Well, instead of getting a pile of rubbish, you get a single key. You just take the " +
                "key to this chest and BAM! There's your loot!",
        )
        chatPlayer(quiz, "So the loot is safe in this other dimension?")
        chatNpc(
            neutral,
            "Eh, seems like it. As long as they've had the key, I've never had anyone unable to " +
                "get the stuff back out. Though if you lose the key, you can't get the stuff out.",
        )
        chatNpc(
            worried,
            "Oh, and be careful running about with too many keys on you. Other people will know " +
                "you've got them, and I'm not gonna stop them opening the chest!",
        )
        chatPlayer(shifty, "So if I kill someone and take their keys, I can use them myself?")
        chatNpc(neutral, "...Eh, if you wanna think about it that way, sure.")
        chatNpc(
            neutral,
            "I got nothing better to do, so I'll keep track of the value of the stuff you unlock.",
        )
        chatNpc(neutral, "And anything you don't want, you can have the chest destroy it.")
        chatNpc(happy, "Except the food. I'll eat that instead.")
        chatNpc(
            neutral,
            "Now, if you want your own keys instead of relying on the misfortune of others, I " +
                "can put the enchantment on you as well, but it's gonna cost ya.",
        )
    }

    private suspend fun Dialogue.chestAccess(): Boolean {
        chatPlayer(quiz, "Can I have access to the chest?")
        chatNpc(
            neutral,
            "So, if you have keys from someone else, you're more than welcome to put them in the " +
                "chest and see what comes out.",
        )
        chatNpc(
            neutral,
            "If you want the enchantment that makes the keys, I'm afraid it's gonna cost you 1 " +
                "million coins.",
        )
        while (true) {
            val option =
                choice3(
                    "Sure, I'll pay for that.",
                    1,
                    "Not right now, thanks.",
                    2,
                    "How much?!",
                    3,
                )
            when (option) {
                1 -> {
                    payForAccess()
                    return false
                }
                2 -> {
                    chatPlayer(neutral, "Not right now, thanks.")
                    chatNpc(neutral, "Fair enough mate, want to talk about something else?")
                    return true
                }
                else -> howMuch()
            }
        }
    }

    private suspend fun Dialogue.payForAccess() {
        chatPlayer(happy, "Sure, I'll pay for that.")
        chatNpc(
            worried,
            "Are you sure? 1 million is a lot of coins, and you won't get them back!",
        )
        val option =
            choice2(
                "Yes, pay 1 million coins!",
                1,
                "No, I've changed my mind!",
                2,
                title = "Pay 1 million coins to unlock Loot Keys?",
            )
        if (option == 2) {
            chatNpc(neutral, "Fair enough mate, talk to me again if you change your mind.")
            return
        }
        if (access.invTotal(access.inv, COINS) < ACCESS_FEE) {
            chatNpc(neutral, "Hmm... I don't think you have enough money on you there, mate.")
        }
    }

    private suspend fun Dialogue.howMuch() {
        chatPlayer(shocked, "How much?!")
        chatNpc(neutral, "1 million coins.")
        chatPlayer(angry, "Why do you charge so much?!")
        chatNpc(
            neutral,
            "Runes. People don't wanna deliver across the Wilderness, and I'm in no shape to go " +
                "to them, so I have to pay through the nose.",
        )
        chatNpc(
            neutral,
            "I make maybe a couple hundred coins of profit, which at least lets me buy a few " +
                "beers.",
        )
        chatPlayer(
            confused,
            "Why is the beer so cheap if it's so expensive to get deliveries out here?",
        )
        chatNpc(
            neutral,
            "The barman makes it himself with whatever scraps he can get his hands on.",
        )
        chatNpc(
            happy,
            "You'd think it'd turn out horrible, but compared to every other 2 coin beer out " +
                "there, it's not half bad.",
        )
        chatNpc(
            neutral,
            "But enough about beer. If you want access to the loot keys, it's gonna cost you 1 " +
                "million coins.",
        )
    }

    private suspend fun Dialogue.whoAreYou() {
        chatPlayer(quiz, "Who are you, exactly?")
        chatNpc(
            neutral,
            "Ah, used to be an adventurer like you, I guess. Used to roam the world, killing " +
                "monsters for a living. 'Cept then, I discovered the Wilderness.",
        )
        chatNpc(
            happy,
            "Used to take on anyone and everyone who stood in me way, and made myself a small " +
                "fortune doing it too.",
        )
        chatNpc(
            laugh,
            "It's how I got the name, used to go anywhere and everywhere with a skull over my " +
                "head!",
        )
        chatPlayer(quiz, "So how'd you end up like... this?")
        chatNpc(
            sad,
            "Ah, you live a life feeding on others, eventually something higher up the food " +
                "chain finds you.",
        )
        chatNpc(
            sad,
            "I used to have a hidden outpost out near the Bone Yard, somewhere to keep my stuff " +
                "in an emergency. One day all I come back to is a smouldering pile of rubble.",
        )
        chatNpc(
            sad,
            "Next thing I know, someone's snuck up behind me and has put a spear through my back.",
        )
        chatPlayer(shocked, "And you survived?!")
        chatNpc(happy, "Always carry a ring of life, friend! Never know when you'll need it.")
        chatNpc(
            sad,
            "Still, the damage was done. Everything I ever owned up in smoke, and even though " +
                "the wound's fully healed, I can still feel that spear in my back to this day.",
        )
        chatNpc(
            neutral,
            "Thankfully, Ferox was kind enough to take me in, and I managed to repurpose some of " +
                "my old storage magic for adventurers like you.",
        )
        chatPlayer(quiz, "But why come back to the Wilderness?")
        chatNpc(
            laugh,
            "What, go life a normal life behind a shop counter so I can pedal swamp paste for a " +
                "living instead? I'll pass!",
        )
        chatNpc(neutral, "Anyhow, enough about me. What can I do for you?")
    }

    private suspend fun Dialogue.settings() {
        chatNpc(
            angry,
            "Don't waste my time trying to change settings on a thing you haven't bought yet!",
        )
    }

    private companion object {
        const val COINS = "obj.coins"
        const val ACCESS_FEE = 1_000_000
    }
}
