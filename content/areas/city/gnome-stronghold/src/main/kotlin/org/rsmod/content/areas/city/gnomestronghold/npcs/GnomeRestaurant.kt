package org.rsmod.content.areas.city.gnomestronghold.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class GnomeRestaurant : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.aluft_gianne") { startDialogue(it.npc) { aluftGianneSnr() } }
        onOpNpc1("npc.aluft_gnome_delivery_controller") { startDialogue(it.npc) { gianneJnr() } }
        onOpNpc1("npc.blurberry") { startDialogue(it.npc) { blurberry() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Aluft_Gianne_snr.
    // TODO(content): [system:gnome_restaurant] "Ok, I'll give it a go." gives Gianne's cook book
    //  and starts the cooking training (toad crunchies, chocolate chip crunchies, toad batta,
    //  cheese and tomato batta) and the after-training dialogue
    private suspend fun Dialogue.aluftGianneSnr() {
        chatPlayer(happy, "Hello.")
        chatNpc(
            happy,
            "Well hello there. Are you hungry? If so you've come to the right place. Eat green, " +
                "eat gnome cuisine. My waiter will be glad to take your order.",
        )
        chatPlayer(happy, "Thanks.")
        chatNpc(
            happy,
            "On the other hand, if you're looking for some gnome cooking training, this is also " +
                "the place to be. My son Lufty is looking for staff at the moment.",
        )
        when (choice2("No thanks, I'm no cook.", 1, "Ok, I'll give it a go.", 2)) {
            1 -> {
                chatPlayer(neutral, "No thank, I'm no cook.")
                chatNpc(happy, "In that case, please eat and enjoy.")
            }
            2 -> chatPlayer(happy, "Ok, I'll give it a go.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gianne_jnr.
    // TODO(content): [system:gnome_restaurant] taking an easy or hard job, the "Get job" ops,
    //  delivery tokens and the subsequent dialogue
    private suspend fun Dialogue.gianneJnr() {
        chatNpc(
            happy,
            "Hello, welcome to Aluft Aloft, the gnome food delivery service. We deliver gnome " +
                "foods to customers all over Gielinor. Do you want a job? We are always looking " +
                "for new staff.",
        )
        if (choice2("Yes, tell me more.", true, "Not right now, thanks.", false)) {
            chatPlayer(happy, "Yes, tell me more.")
            gianneJnrJobInfo()
        }
    }

    private suspend fun Dialogue.gianneJnrJobInfo() {
        chatNpc(
            happy,
            "First of all you will need some basic Gnome Cooking and Cocktail making training. " +
                "Go and talk to my Father, Aluft Gianne snr. and Blurberry, if you haven't " +
                "already, and they'll tell you what you need to do.",
        )
        chatNpc(
            happy,
            "Once you have got recommendations from both of them I'd be happy to take you on " +
                "at Aluft Aloft!",
        )
        gianneJnrHowItWorks()
        while (true) {
            val option =
                choice5(
                    "What's the difference between an easy and a hard job?",
                    1,
                    "What are these free delivery tokens?",
                    2,
                    "Will this help me with my cooking?",
                    3,
                    "I'm ready to take a job on now.",
                    4,
                    "I've changed my mind, there's something else I need to do now.",
                    5,
                )
            when (option) {
                1 -> easyOrHardJob()
                2 -> deliveryTokens()
                3 -> {
                    chatPlayer(quiz, "Will this help me with my cooking?")
                    chatNpc(
                        happy,
                        "You will be able to develop your cooking skills during deliveries - " +
                            "you'll get the normal training for making the items, but also a " +
                            "bonus for successfully delivering the goods.",
                    )
                }
                4 -> {
                    chatPlayer(happy, "I'm ready to take an order now...")
                    return
                }
                else -> return
            }
        }
    }

    private suspend fun Dialogue.gianneJnrHowItWorks() {
        chatNpc(
            happy,
            "Once you are working for us, I will give you a choice of either an easy or hard " +
                "job. I will then ask you to make a piece of Gnome Cuisine, and tell you who " +
                "ordered it, with some information about how to find him",
        )
        chatNpc(
            happy,
            "or her. Don't take too long, the order is only valid for a short period of time.",
        )
        chatNpc(
            happy,
            "If you complete the delivery successfully, you will be given a tip by the customer " +
                "and credit towards your own 'free delivery token'.",
        )
    }

    private suspend fun Dialogue.easyOrHardJob() {
        chatPlayer(quiz, "What's the difference between an easy and a hard job?")
        chatNpc(
            happy,
            "The foods that you need to cook for the easy jobs are more straight forward than " +
                "the hard ones, and the customers will be within the Gnome Stronghold, or one of " +
                "the Gnome Glider Pilots on duty. You will get one",
        )
        chatNpc(
            happy,
            "credit for completing a delivery, and the tips you get will be a small amount of " +
                "cash.",
        )
        chatNpc(
            happy,
            "The hard jobs could require you to cook any piece of gnome cuisine or cocktail. The " +
                "customers could be anywhere in Gielinor, If you are going to complete the " +
                "delivery in time, you'll probably have to use some form",
        )
        chatNpc(happy, "of transport. Gnome gliders are especially useful here.")
        chatNpc(
            happy,
            "For the hard jobs you will be given more time to complete the delivery though. You " +
                "get three credits for completing it, and the tips you get tend to be useful " +
                "objects, and even a few unusual items, that you won't",
        )
        chatNpc(happy, "be able to get hold of anywhere else!")
    }

    private suspend fun Dialogue.deliveryTokens() {
        chatPlayer(quiz, "What are these free delivery tokens?")
        chatNpc(
            happy,
            "I'll give you a free delivery token when you earn 12 credits. You can then use the " +
                "token to make a food order yourself! This is a great privilege, we normally " +
                "only deliver to gnomes. You may only have one token",
        )
        chatNpc(happy, "at a time, and they are non-transferrable.")
        chatNpc(
            happy,
            "If you gain more credits then for every 12 extra credits you earn, you will receive " +
                "an extra item of food or drink per delivery, up to a maximum of ten items. You " +
                "can continue to make deliveries after this, but you",
        )
        chatNpc(
            happy,
            "will not get any more credits - just the normal tip and cooking training.",
        )
        chatNpc(
            happy,
            "Once you call for a delivery your token will be used up, and you'll have to earn " +
                "another one, if you want another delivery.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Blurberry
    // TODO(content): [system:gnome_restaurant] "Ok then I'll give it a go." gives the cocktail
    //  guide and starts the cocktail training (fruit blast, pineapple punch, wizard blizzard,
    //  short green guy) and the after-training dialogue
    // TODO(content): [miniquest:alfred_grimhands_barcrawl] Fire Toad Blast
    private suspend fun Dialogue.blurberry() {
        chatPlayer(happy, "Hello.")
        chatNpc(
            happy,
            "Well hello there traveller. If you're looking for a cocktail the barman will " +
                "happily make you one.",
        )
        chatNpc(
            happy,
            "Or if you're looking for some training in cocktail making, then I'm your gnome! " +
                "Aluft Gianne jnr. is looking for gnome cooks, barmen and barwomen to help in " +
                "his new venture, so it's a useful skill to have. I'm",
        )
        val humans = if (access.isBodyTypeA()) "boys" else "girls"
        chatNpc(happy, "sure the human $humans will like it as well.")
        val option =
            choice2(
                "No thanks, I prefer to stay this side of the bar.",
                1,
                "Ok then I'll give it a go.",
                2,
            )
        when (option) {
            1 -> chatPlayer(neutral, "No thanks, I prefer to stay this side of the bar.")
            2 -> chatPlayer(happy, "Ok then I'll give it a go.")
        }
    }
}
