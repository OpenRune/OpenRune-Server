package org.rsmod.content.areas.city.prifddinas.npcs

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PrifddinasTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.elena_prif") { startDialogue(it.npc) { elena() } }
        onOpNpc1("npc.eluned_prif") { startDialogue(it.npc) { eluned() } }
        onOpNpc1("npc.islwyn_prif") { startDialogue(it.npc) { islwyn() } }
        onOpNpc1("npc.morvran_prif") { startDialogue(it.npc) { morvran() } }
        onOpNpc1("npc.prif_raven") { startDialogue(it.npc) { raven() } }
        onOpNpc1("npc.zalcano_instructor") { startDialogue(it.npc) { rhiannon() } }
        onOpNpc1("npc.prif_agility") { startDialogue(it.npc) { ystwyth() } }
        onOpNpc1("npc.prif_priest") { startDialogue(it.npc) { crefydd() } }
        onOpNpc1("npc.prif_city_guard_multi") { startDialogue(it.npc) { gatekeeper() } }
        onOpNpc1("npc.prif_singer1_multi") { startDialogue(it.npc) { reese() } }
        onOpNpc1("npc.prif_singer2_multi") { startDialogue(it.npc) { conwenna() } }
        onOpNpc1("npc.prif_acorn_trader") { startDialogue(it.npc) { pennant() } }
        onOpNpc1("npc.prif_citizen_amrod") { startDialogue(it.npc) { amrod() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Elena (after Song of the Elves)
    private suspend fun Dialogue.elena() {
        chatNpc(happy, "Hey ${player.displayName}.")
        chatPlayer(quiz, "Hey Elena. How are things?")
        chatNpc(
            happy,
            "Great! I've learnt so much from the Meilyr elves here. Their ways of doing things " +
                "are unlike anything I've seen before.",
        )
        chatNpc(
            happy,
            "And Prifddinas... The city is so beautiful. It's an honour to be the first human " +
                "living here.",
        )
        chatPlayer(happy, "That's fantastic! Anyway I won't keep you, see you later.")
        chatNpc(happy, "Bye.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Eluned (after Song of the Elves)
    // TODO(content): [system:teleport_crystal] re-enchant offer when carrying an uncharged crystal
    // teleport seed, and the Enchant option
    // TODO(content): [system:treasure_trails] hard challenge scroll
    private suspend fun Dialogue.eluned() {
        chatNpc(happy, "Hey ${player.displayName}, good to see you.")
        chatPlayer(happy, "And you Eluned.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Islwyn (after Song of the Elves)
    // TODO(content): [verify:in_game] buying crystal equipment with enough coins and the Trade
    // option (the wiki transcript is incomplete)
    private suspend fun Dialogue.islwyn() {
        chatNpc(
            happy,
            "Good to see you, ${player.displayName}. Are you after some crystal equipment?",
        )
        val option =
            choice5(
                "I'd like to buy a bow.",
                1,
                "I'd like to buy a shield.",
                2,
                "I'd like to buy a halberd.",
                3,
                "What should I do with crystal seeds?",
                4,
                "No I'm just passing by.",
                5,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "I'd like to buy a bow.")
                crystalPrice(900_000)
            }
            2 -> {
                chatPlayer(happy, "I'd like to buy a shield.")
                crystalPrice(750_000)
            }
            3 -> {
                chatPlayer(happy, "I'd like to buy a halberd.")
                crystalPrice(750_000)
            }
            4 -> {
                chatPlayer(quiz, "What should I do with crystal seeds?")
                chatNpc(neutral, "Ilfeen can chant them into new items for you.")
                chatPlayer(happy, "I'll keep an eye out for her, thanks.")
            }
            5 -> chatPlayer(neutral, "No, I'm just passing by.")
        }
    }

    private suspend fun Dialogue.crystalPrice(price: Int) {
        if (access.invTotal(player.inv, "obj.coins") >= price) {
            return
        }
        val coins = "%,d".format(price)
        chatNpc(sad, "Well sorry, but you'll have to get $coins coins. Oh well... See you later.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Morvran
    private suspend fun Dialogue.morvran() {
        chatNpc(laugh, "Well look who it is! The hero of Prifddinas!")
        chatPlayer(confused, "Why do I feel like you're being sarcastic?")
        chatNpc(neutral, "That's likely due to the fact that I am being sarcastic.")
        chatPlayer(
            angry,
            "Well you're no hero either. You blindly served Lord Iorwerth right to the very end. " +
                "You're lucky the other elves pardoned you.",
        )
        chatNpc(
            neutral,
            "You seem upset by that. I served Lord Iorwerth because I didn't have a choice. The " +
                "other elves realised that, hence the pardon.",
        )
        chatPlayer(angry, "We always have a choice.")
        chatNpc(quiz, "Do we?")
        chatPlayer(neutral, "I did.")
        chatNpc(
            neutral,
            "Yes, but you're a mercenary. You're lucky enough to be able to side with the " +
                "highest bidder. Not all of us are so privileged.",
        )
        chatNpc(
            neutral,
            "I was born into the Iorwerth clan, Lord Iorwerth was my uncle. Do you think I had a " +
                "say in what my life would be? Do you think any of us did?",
        )
        chatNpc(
            neutral,
            "We served Lord Iorwerth because he was our leader. We may not have agreed with all " +
                "that he did, but that didn't matter. He was in charge and we were not.",
        )
        chatNpc(
            sad,
            "Had I been born into the Cadarn clan, I'd be considered a hero right now. But I " +
                "wasn't, so instead I am the villain.",
        )
        chatPlayer(
            angry,
            "You can make excuses, but Iestin served Lord Iorwerth and he switched sides.",
        )
        chatNpc(
            neutral,
            "He did. Iestin was brave, I respect that. His bravery has rewarded him with " +
                "leadership over our clan. But bravery comes easy when you have nothing left to " +
                "lose.",
        )
        chatNpc(neutral, "But I can see I will not change your mind. That's fine, I don't need to.")
        chatNpc(
            neutral,
            "That said, you'd do well to remember that the actions of Lord Iorwerth should not " +
                "be used to judge our entire clan.",
        )
        chatPlayer(neutral, "Hmm.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Raven
    private suspend fun Dialogue.raven() {
        chatPlayer(happy, "Oh, look! A raven!")
        chatNpc(angry, "Nope, we're not doing that again!")
        chatPlayer(confused, "Wait... What? Doing what?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Rhiannon
    // TODO(content): [verify:in_game] varbit.zalcano_loot_customization values (0 resource
    // focused, 1 XP focused are guessed)
    private suspend fun Dialogue.rhiannon() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Oh, hellow adventurer. Can I help you with anything?")
        while (true) {
            val option =
                choice4(
                    "What is this place?",
                    1,
                    "What can I do here?",
                    2,
                    "What do I get for helping out here?",
                    3,
                    "No thanks.",
                    4,
                )
            when (option) {
                1 -> zalcanoPrison()
                2 -> zalcanoHelp()
                3 -> zalcanoRewards()
                else -> {
                    chatPlayer(neutral, "No thanks.")
                    return
                }
            }
            chatNpc(happy, "Is there anything else I can help you with?")
        }
    }

    private suspend fun Dialogue.zalcanoPrison() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(neutral, "This is the prison of Zalcano.")
        chatPlayer(confused, "Zalcano?")
        chatNpc(neutral, "That's right. She's a powerful demon, one bearing an ancient curse.")
        chatPlayer(worried, "A curse? That doesn't sound good.")
        chatNpc(
            neutral,
            "Don't worry, the curse only affects her. Because of it, her entire body has been " +
                "turned to stone.",
        )
        chatPlayer(quiz, "I see. So why is she here?")
        chatNpc(
            neutral,
            "She came to our lands long ago, before the curse had fully taken hold. We " +
                "imprisoned her here for study, so that we might learn more about her affliction.",
        )
        chatNpc(
            neutral,
            "If you want to learn more, you should be able to find a book in the Grand Library " +
                "about her.",
        )
    }

    private suspend fun Dialogue.zalcanoHelp() {
        chatPlayer(quiz, "What can I do here?")
        chatNpc(neutral, "Well we could always use some help in keeping Zalcano under control.")
        chatPlayer(quiz, "How would I do that?")
        chatNpc(
            neutral,
            "Zalcano is formed completely from stone. Because of this, normal weapons won't work " +
                "on her.",
        )
        chatNpc(
            neutral,
            "That said, when she's vulnerable, you can still do some damage to her if you use a " +
                "pickaxe.",
        )
        chatPlayer(quiz, "When she's vulnerable? How do I make her vulnerable?")
        chatNpc(
            neutral,
            "The ore found within these caverns can be rather volatile, especially when heated. " +
                "We've found that when imbued, this ore can damage her armour.",
        )
        chatNpc(
            neutral,
            "You'll want to head on in and mine the ore from the rocks around the prison. Once " +
                "you have some, you can heat it using the furnace and then imbue it using the " +
                "altar.",
        )
        chatNpc(
            neutral,
            "Once you've got some imbued ore, just give it a throw her way and she'll definitely " +
                "feel it.",
        )
    }

    private suspend fun Dialogue.zalcanoRewards() {
        chatPlayer(quiz, "What do I get for helping out here?")
        chatNpc(
            neutral,
            "Well assuming you are successful of subdueing Zalcano, she has accumulated a lot of " +
                "ores and magical runes from her time as a demon.",
        )
        chatPlayer(
            quiz,
            "Not bad, what if I didn't want these resource? Is there anything you can give me " +
                "instead?",
        )
        chatNpc(
            neutral,
            "Well we would always appreciate these resources, in exchange we could bestow upon " +
                "you our elven knowledge for mining.",
        )
        chatNpc(
            neutral,
            "We wouldn't be asking for anything special though, just the common resources are " +
                "what we require.",
        )
        when (choice2("Resource focused", 1, "XP Focused.", 2, title = "Customise Zalcano Loot")) {
            1 -> {
                vars[ZALCANO_LOOT] = 0
                chatNpc(
                    neutral,
                    "We won't take any resources from your successes, the resources are all your " +
                        "own.",
                )
            }
            2 -> {
                vars[ZALCANO_LOOT] = 1
                chatNpc(
                    neutral,
                    "We will automatically collect the resources and bestow you with our elven " +
                        "mining knowledge.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ystwyth (no laps completed)
    // TODO(content): [skill:agility] lap count, best time and the Statistics option once the
    // Prifddinas Agility Course exists
    private suspend fun Dialogue.ystwyth() {
        chatNpc(happy, "Hey, fancy having a go at our agility course?")
        chatPlayer(quiz, "What do I need to do?")
        chatNpc(happy, "Just head up the ladder to get started. Try not to fall!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Crefydd (respawn point not in Prifddinas)
    // TODO(content): [system:respawn_point] set the Prifddinas respawn point on "Yes please." and
    // offer to revert it to Lumbridge once set
    private suspend fun Dialogue.crefydd() {
        chatNpc(happy, "Hello there. Have you come to reflect?")
        chatPlayer(confused, "I'm not sure.")
        chatNpc(
            neutral,
            "It's not for everyone. You are always welcome to pray to Seren here if you wish " +
                "though. If you prefer, I can offer you the ability to respawn in Prifddinas " +
                "upon death.",
        )
        when (choice2("Yes please.", 1, "No thanks.", 2)) {
            1 -> chatPlayer(happy, "Yes please.")
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "As you wish. I'll be here if you change your mind.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_(Prifddinas_gatekeeper)
    private suspend fun Dialogue.gatekeeper() {
        if (npcVisType.id == "npc.prif_city_guard".asRSCM()) {
            chatNpc(neutral, "Welcome, adventurer.")
            return
        }
        chatPlayer(quiz, "Hello, can I go in?")
        chatNpc(
            neutral,
            "I'm afraid not. The city is on lockdown until the rebel elves are dealt with.",
        )
        chatPlayer(sad, "Please, I won't cause any trouble.")
        chatNpc(neutral, "Rules are rules. No one enters until the criminals are gone.")
        chatPlayer(
            neutral,
            "Fine, doesn't look very exciting anyway, it's just an empty field.",
        )
        chatNpc(
            angry,
            "Mind your tongue human. Our great city may be lost to us but that doesn't give you " +
                "an excuse to talk like that.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Reese
    // TODO(content): [skill:smithing] crystal singing by the elf ("Could you do it for me?" and
    // "Yes please.") and the Sing option
    private suspend fun Dialogue.reese() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Welcome, traveller.")
        val option =
            choice2(
                "Could you tell me about crystal singing?",
                1,
                "Could you sing a song for me?",
                2,
            )
        when (option) {
            1 -> crystalSinging()
            2 -> {
                chatNpc(
                    happy,
                    "Of course! Would you like me to craft something for you? For some crystal " +
                        "shards, of course.",
                )
                singSong()
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Conwenna
    // TODO(content): [skill:smithing] crystal singing by the elf ("Could you do it for me?" and
    // "Yes please.") and the Sing option
    private suspend fun Dialogue.conwenna() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Welcome, traveller.")
        val option =
            choice2(
                "Could you tell me about crystal singing?",
                1,
                "Could you sing a song for me?",
                2,
            )
        when (option) {
            1 -> crystalSinging()
            2 -> {
                chatPlayer(quiz, "Could you sing a song for me?")
                chatNpc(
                    happy,
                    "Of course! Would you like me to craft something for you? For some crystal " +
                        "shards, of course.",
                )
                singSong()
            }
        }
    }

    private suspend fun Dialogue.crystalSinging() {
        chatPlayer(quiz, "Could you tell me about crystal singing?")
        chatNpc(
            happy,
            "Of course! We elves learned the art long ago, allowing us to form crystals in any " +
                "way we would like, using just our voices.",
        )
        chatNpc(
            happy,
            "It's allowed us to make weapons, armour and tools using the finest crystals.",
        )
        chatPlayer(quiz, "Wow. Is there any way I could do it?")
        chatNpc(
            neutral,
            "You might be able to with the help of the crystal bowl over there. As long as " +
                "you've got the right knowledge of what you're trying to craft.",
        )
        when (choice2("Could you do it for me?", 1, "Thanks!", 2)) {
            1 -> chatPlayer(quiz, "Could you do it for me?")
            2 -> chatPlayer(happy, "Thanks!")
        }
    }

    private suspend fun Dialogue.singSong() {
        val option = choice2("Yes please.", 1, "No, thank you. I'd just like to hear you sing.", 2)
        when (option) {
            1 -> chatPlayer(happy, "Yes please.")
            2 -> {
                chatPlayer(happy, "No, thank you. I'd just like to hear you sing.")
                mesbox("${npcVisType.name} sings you a beautiful song of the elves.")
                chatPlayer(happy, "Wow! Thank you.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Pennant
    // TODO(content): [system:item_exchange] trading crystal weapon, armour and tool seeds for a
    // crystal acorn, and the Trade option
    private suspend fun Dialogue.pennant() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Hello! What can I do for ya' today?")
        val option =
            if (vars[ACORN_TRADE] == 0) {
                choice2("What can I grow here?", 1, "Nothing.", 3)
            } else {
                choice3(
                    "What can I grow here?",
                    1,
                    "Can I trade you for some crystal acorns?",
                    2,
                    "Nothing.",
                    3,
                )
            }
        when (option) {
            1 -> {
                chatPlayer(quiz, "What can I grow here?")
                chatNpc(
                    happy,
                    "Well, you can grow a crystal tree, if you've got the right kind of seed. " +
                        "Great for getting a few shards out of.",
                )
                chatPlayer(quiz, "And where do you get the right kind of seed?")
                chatNpc(
                    neutral,
                    "They're pretty hard to come by... But I might be able to exchange some, for " +
                        "any other kinds of crystal seed you might have.",
                )
                vars[ACORN_TRADE] = 1
                chatNpc(quiz, "What do you say?")
                when (choice2("Sure.", 1, "No thanks.", 2)) {
                    1 -> offerSeeds()
                    2 -> chatPlayer(neutral, "No thanks. Maybe another time.")
                }
            }
            2 -> {
                chatPlayer(quiz, "Can I trade you for some crystal acorns?")
                chatNpc(quiz, "What kind of crystal seeds do you have to offer?")
                offerSeeds()
            }
            3 -> chatPlayer(neutral, "Nothing. Just passing by.")
        }
    }

    private suspend fun Dialogue.offerSeeds() {
        chatPlayer(neutral, "Sure. Let's see...")
        if (CRYSTAL_SEEDS.none { player.inv.physicalCount(it) > 0 }) {
            chatPlayer(
                sad,
                "Actually... I don't think I've got any crystal seeds to trade, right now.",
            )
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Amrod
    // TODO(content): [system:item_exchange] crystal seed for crystal shard exchange interface on
    // "Yes." and the Trade option
    private suspend fun Dialogue.amrod() {
        chatPlayer(happy, "Hello there.")
        chatNpc(neutral, "Adventurer.")
        chatPlayer(quiz, "What do you do?")
        chatNpc(
            shifty,
            "I'm a trader of sorts, but only to those who don't ask too many questions.",
        )
        chatPlayer(quiz, "Interesting, what do you offer?")
        chatNpc(neutral, "I can give you crystal shards in return for crystal seeds.")
        if (choice2("Yes.", 1, "No.", 2) == 2) {
            chatPlayer(neutral, "I'll pass, maybe another time.")
        }
    }

    private companion object {
        const val ZALCANO_LOOT = "varbit.zalcano_loot_customization"
        const val ACORN_TRADE = "varbit.prif_acorn_trade"
        val CRYSTAL_SEEDS =
            listOf(
                "obj.crystal_seed_old",
                "obj.cert_crystal_seed_old",
                "obj.prif_armour_seed",
                "obj.cert_prif_armour_seed",
                "obj.prif_tool_seed",
                "obj.cert_prif_tool_seed",
            )
    }
}
