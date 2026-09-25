package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RockGolemDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(DEFAULT, "Talk-to") { talkDefault(it) }
        onPetOp(TIN, "Talk-to") { talkTin(it) }
        onPetOp(COPPER, "Talk-to") { talkCopper(it) }
        onPetOp(IRON, "Talk-to") { talkIron(it) }
        onPetOp(BLURITE, "Talk-to") { talkBlurite(it) }
        onPetOp(SILVER, "Talk-to") { talkSilver(it) }
        onPetOp(COAL, "Talk-to") { talkCoal(it) }
        onPetOp(GOLD, "Talk-to") { talkGold(it) }
        onPetOp(MITHRIL, "Talk-to") { talkMithril(it) }
        onPetOp(GRANITE, "Talk-to") { talkGranite(it) }
        onPetOp(ADAMANTITE, "Talk-to") { talkAdamantite(it) }
        onPetOp(RUNITE, "Talk-to") { talkRunite(it) }
        onPetOp(AMETHYST, "Talk-to") { talkAmethyst(it) }
        onPetOp(LOVAKITE, "Talk-to") { talkLovakite(it) }
        onPetOp(ELEMENTAL, "Talk-to") { talkElemental(it) }
        onPetOp(DAEYALT, "Talk-to") { talkDaeyalt(it) }
        onPetOp(LEAD, "Talk-to") { talkLead(it) }
        onPetOp(RUBIUM, "Talk-to") { talkRubium(it) }
        onPetOp(NICKEL, "Talk-to") { talkNickel(it) }
    }

    private suspend fun ProtectedAccess.talkDefault(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "So you're made entirely of rocks?")
            chatNpc(neutral, "Not quite, my body is formed mostly of minerals.")
            chatPlayer(quiz, "Aren't minerals just rocks?")
            chatNpc(
                neutral,
                "No, rocks are rocks, minerals are minerals. I am formed from minerals.",
            )
            chatPlayer(confused, "But you're a Rock Golem...")
            chatNpc(
                neutral,
                "Yes, I am. This conversation seems to be confusing you; perhaps you should stop.",
            )
        }

    private suspend fun ProtectedAccess.talkTin(npc: Npc) =
        startDialogue(npc) {
            chatNpc(
                neutral,
                "I feel strangely emotionless and empty. Maybe I should feel sad about it, but I " +
                    "can't.",
            )
            chatPlayer(quiz, "You can't feel sad?")
            chatNpc(neutral, "Correct. Now, if I only had a heart...")
            chatPlayer(neutral, "I'm not sure it works like that around here.")
        }

    private suspend fun ProtectedAccess.talkCopper(npc: Npc) =
        startDialogue(npc) {
            chatNpc(happy, "I have an idea for a song.")
            chatPlayer(quiz, "Oh?")
            chatNpc(happy, "Copper-copper-copper Cophelia - you come and go, you come and go...")
            chatPlayer(bored, "Mmmhmm.")
        }

    private suspend fun ProtectedAccess.talkIron(npc: Npc) =
        startDialogue(npc) {
            chatNpc(neutral, "Truth is, I am Iron Golem.")
            chatPlayer(neutral, "I can see that.")
        }

    private suspend fun ProtectedAccess.talkBlurite(npc: Npc) =
        startDialogue(npc) {
            chatNpc(happy, "I have an idea for a song.")
            chatPlayer(quiz, "Oh?")
            chatNpc(happy, "I'm blue, da-ba-dee da-ba-da.")
            chatPlayer(neutral, "I can see that.")
        }

    private suspend fun ProtectedAccess.talkSilver(npc: Npc) =
        startDialogue(npc) {
            chatNpc(worried, "Oh dear, I've gone all pale.")
            chatPlayer(quiz, "Are you okay?")
            chatNpc(worried, "It must be something I ate.")
        }

    private suspend fun ProtectedAccess.talkCoal(npc: Npc) =
        startDialogue(npc) {
            chatNpc(
                sad,
                "So near and yet so far... if my atoms were arranged a little differently, I " +
                    "could be diamond.",
            )
            chatPlayer(quiz, "Would you enjoy being a diamond?")
            chatNpc(
                neutral,
                "I expect I would, until someone tried chipping bits off me with a chisel. I'm " +
                    "probably safer as coal.",
            )
        }

    private suspend fun ProtectedAccess.talkGold(npc: Npc) =
        startDialogue(npc) {
            chatNpc(happy, "I'm totally showing the bling today.")
            chatPlayer(neutral, "That's nice for you.")
            chatNpc(
                sad,
                "A little. Though sometimes I just feel like I'm being used as a status symbol, " +
                    "and no-one appreciates me for me.",
            )
            chatPlayer(worried, "Oh dear.")
            chatNpc(happy, "Don't worry. At least I look good.")
        }

    private suspend fun ProtectedAccess.talkMithril(npc: Npc) =
        startDialogue(npc) {
            chatNpc(sad, "I feel sad today. Very blue.")
            chatPlayer(worried, "Oh dear.")
            chatNpc(sad, "No-one understands me.")
            chatPlayer(quiz, "Why not?")
            chatNpc(sad, "Because argle gargle gooble goop.")
            chatPlayer(confused, "... I don't understand you either.")
            chatNpc(sad, "*sigh*")
        }

    private suspend fun ProtectedAccess.talkGranite(npc: Npc) =
        startDialogue(npc) {
            chatNpc(sad, "No-one appreciates granite.")
            chatPlayer(quiz, "Why do you say that?")
            chatNpc(
                sad,
                "I know how it works. No-one actually wants granite. They just chop us up and " +
                    "throw our pieces on the floor.",
            )
            chatPlayer(worried, "Oh, I can see that must be upsetting for you.")
            chatNpc(
                angry,
                "When you've seen your relatives cut into pieces, with their severed limbs cast " +
                    "aside like junk, THEN you will understand how I feel.",
            )
            chatPlayer(neutral, "I'll bear it in mind.")
        }

    private suspend fun ProtectedAccess.talkLovakite(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "So how do you pronounce Lovakengj?")
            chatNpc(neutral, "Silly human, it's pronounced Lova-Kane.")
            chatNpc(quiz, "How did you not know that?")
            chatPlayer(neutral, "You know, it really isn't that obvious.")
        }

    private suspend fun ProtectedAccess.talkAdamantite(npc: Npc) =
        startDialogue(npc) {
            chatNpc(neutral, "I may be green, but I'm not an environmentalist.")
            chatPlayer(quiz, "Why not?")
            chatNpc(
                neutral,
                "There's no need. Whatever you may have read, even coal is a renewable energy " +
                    "source - just wait a minute and the rocks respawn.",
            )
            chatNpc(
                neutral,
                "You can burn as much as you like, too, without needing to worry about it " +
                    "affecting the climate - we don't have a lot of weather here.",
            )
            chatPlayer(happy, "That's handy.")
            chatNpc(
                neutral,
                "Yes, I pity anyone whose world doesn't work like this one. I don't know how they " +
                    "can possibly cope.",
            )
        }

    private suspend fun ProtectedAccess.talkRunite(npc: Npc) =
        startDialogue(npc) {
            chatNpc(
                confused,
                "I'm confused. It takes incredible skill to smith anything from my ore, yet the " +
                    "items you'd get are terribly mediocre.",
            )
            chatPlayer(quiz, "Is that something that worries you?")
            chatNpc(
                neutral,
                "It just feels like my world doesn't make sense, sometimes. But I suppose it's " +
                    "always been like this, and I've got used to it.",
            )
        }

    private suspend fun ProtectedAccess.talkAmethyst(npc: Npc) =
        startDialogue(npc) {
            chatNpc(happy, "Your world is amazing. I truly am in awe.")
            chatPlayer(quiz, "Did you just make an ore joke?")
            chatNpc(shifty, "Maybe...")
            chatPlayer(bored, "Well it was awful.")
            chatNpc(laugh, "Ha! Now you're making them as well.")
            chatPlayer(neutral, "Are we really doing this? Amethyst isn't an ore anyway.")
            chatNpc(happy, "I can dream, ${player.displayName}!")
        }

    private suspend fun ProtectedAccess.talkElemental(npc: Npc) =
        startDialogue(npc) {
            chatNpc(happy, "I am so in my element right now.")
            chatPlayer(bored, "Sigh...")
        }

    private suspend fun ProtectedAccess.talkDaeyalt(npc: Npc) =
        startDialogue(npc) {
            chatNpc(quiz, "Did you ever hear the tragedy of Queen Efaritay the Fair?")
            chatPlayer(quiz, "No?")
            chatNpc(neutral, "I thought not.")
            chatNpc(neutral, "It's not a story the Myreque would tell you.")
        }

    private suspend fun ProtectedAccess.talkLead(npc: Npc) =
        startDialogue(npc) {
            chatNpc(quiz, "Why am I the one always following you?")
            chatPlayer(confused, "What?")
            chatNpc(happy, "Well I'm the lead golem, so surely I should take the lead!")
            chatPlayer(
                angry,
                "That joke doesn't even make sense... Those words aren't pronounced the same!",
            )
            chatNpc(neutral, "Well, blame whoever decided they should have the same spelling then.")
        }

    private suspend fun ProtectedAccess.talkRubium(npc: Npc) =
        startDialogue(npc) {
            chatNpc(verymad, "AAAAAAARRRRGGGGHHHH!")
            chatPlayer(confused, "What's wrong? Are you doing a barbarian impression?")
            chatNpc(
                verymad,
                "I FEEL SO ALIVE, SO CHARGED UP! I'M BRIMMING WITH POTENTIAL ENERGY! LET'S RUN, GO " +
                    "FASTER!",
            )
            chatPlayer(quiz, "Maybe you should cool off? How about a swim?")
            chatNpc(worried, "THAT SOUNDS... bad.")
        }

    private suspend fun ProtectedAccess.talkNickel(npc: Npc) =
        startDialogue(npc) {
            chatNpc(shifty, "I reckon I'd make a good thief.")
            chatPlayer(neutral, "There are probably better ambitions.")
            chatNpc(laugh, "Maybe, but if I was one, I'd be able to nickel they have!")
            chatPlayer(bored, "...")
        }

    private companion object {
        const val DEFAULT = "npc.skillpet_mining_default"
        const val TIN = "npc.skillpet_mining_tin"
        const val COPPER = "npc.skillpet_mining_copper"
        const val IRON = "npc.skillpet_mining_iron"
        const val BLURITE = "npc.skillpet_mining_blurite"
        const val SILVER = "npc.skillpet_mining_silver"
        const val COAL = "npc.skillpet_mining_coal"
        const val GOLD = "npc.skillpet_mining_gold"
        const val MITHRIL = "npc.skillpet_mining_mithril"
        const val GRANITE = "npc.skillpet_mining_granite"
        const val ADAMANTITE = "npc.skillpet_mining_adamantite"
        const val RUNITE = "npc.skillpet_mining_runite"
        const val AMETHYST = "npc.skillpet_mining_amethyst"
        const val LOVAKITE = "npc.skillpet_mining_lovakite"
        const val ELEMENTAL = "npc.skillpet_mining_elemental"
        const val DAEYALT = "npc.skillpet_mining_daeyalt"
        const val LEAD = "npc.skillpet_mining_lead"
        const val RUBIUM = "npc.skillpet_mining_rubium"
        const val NICKEL = "npc.skillpet_mining_nickel"
    }
}
