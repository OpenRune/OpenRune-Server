package org.rsmod.content.other.pets.dialogue

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.PetMorphs
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.content.other.pets.onPetOpU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BeaverDialogue @Inject constructor(private val morphs: PetMorphs) : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NORMAL, "Talk-to") { talkNormal(it) }
        onPetOp(OAK, "Talk-to") { talkOak(it) }
        onPetOp(WILLOW, "Talk-to") { talkWillow(it) }
        onPetOp(MAPLE, "Talk-to") { talkMaple(it) }
        onPetOp(YEW, "Talk-to") { talkYew(it) }
        onPetOp(MAGIC, "Talk-to") { talkMagic(it) }
        onPetOp(REDWOOD, "Talk-to") { talkRedwood(it) }
        onPetOp(TEAK, "Talk-to") { talkTeak(it) }
        onPetOp(MAHOGANY, "Talk-to") { talkMahogany(it) }
        onPetOp(ARCTIC_PINE, "Talk-to") { talkArcticPine(it) }
        onPetOp(CAMPHOR, "Talk-to") { talkCamphor(it) }
        onPetOp(IRONWOOD, "Talk-to") { talkIronwood(it) }
        onPetOp(JATOBA, "Talk-to") { talkJatoba(it) }
        onPetOp(ROSEWOOD, "Talk-to") { talkRosewood(it) }
        onPetOp(FOX, "Talk-to") { talkFox(it) }
        onPetOp(PHEASANT, "Talk-to") { talkPheasant(it) }
        for (npc in BEAVER_FORMS) {
            onPetOpU(npc) { useItemBeaver(it.npc, it.objType) }
        }
        onPetOpU(FOX) { useItemFox(it.npc, it.objType) }
        onPetOpU(PHEASANT) { useItemPheasant(it.npc, it.objType) }
    }

    private suspend fun ProtectedAccess.talkNormal(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(
                quiz,
                "How much wood would a woodchuck chuck if a woodchuck could chuck wood?",
            )
            chatNpc(neutral, "Approximately 32,768 depending on his woodcutting level.")
        }

    private suspend fun ProtectedAccess.talkOak(npc: Npc) =
        startDialogue(npc) {
            chatNpc(quiz, "What did the oak tree drop?")
            chatPlayer(quiz, "What?")
            chatNpc(laugh, "A-corny joke!")
        }

    private suspend fun ProtectedAccess.talkWillow(npc: Npc) =
        startDialogue(npc) { chatNpc(happy, "Aaaaaaaan I, willow-ays love youuu.") }

    private suspend fun ProtectedAccess.talkMaple(npc: Npc) =
        startDialogue(npc) {
            chatNpc(quiz, "Why do maple trees not try to explore new lands?")
            chatPlayer(quiz, "Why?")
            chatNpc(laugh, "They're afraid of treespassing.")
        }

    private suspend fun ProtectedAccess.talkYew(npc: Npc) =
        startDialogue(npc) {
            chatNpc(happy, "Yew are a great companion.")
            chatPlayer(happy, "Yew are too kind, my furry little friend.")
        }

    private suspend fun ProtectedAccess.talkMagic(npc: Npc) =
        startDialogue(npc) {
            chatNpc(sad, "I'm feeling blue today...")
            chatPlayer(quiz, "Anything I can do to cheer you up?")
            chatNpc(neutral, "A dam and some logs could be some fun....")
        }

    private suspend fun ProtectedAccess.talkRedwood(npc: Npc) =
        startDialogue(npc) {
            chatNpc(
                laugh,
                "It sure is hard to cut the top off of a redwood tree... You can't take any short " +
                    "cuts!",
            )
            chatPlayer(bored, "...")
            chatPlayer(bored, "Why do I even try talking to you.")
        }

    private suspend fun ProtectedAccess.talkTeak(npc: Npc) =
        startDialogue(npc) { chatNpc(bored, "Can we teak a break? My legs are tired.") }

    private suspend fun ProtectedAccess.talkMahogany(npc: Npc) =
        startDialogue(npc) {
            chatNpc(laugh, "If you had a pet pig, you could call it ma-hog-eat-any.")
        }

    private suspend fun ProtectedAccess.talkArcticPine(npc: Npc) =
        startDialogue(npc) { chatNpc(quiz, "Is there anything I can't eat?") }

    private suspend fun ProtectedAccess.talkCamphor(npc: Npc) =
        startDialogue(npc) {
            chatNpc(angry, "I almost choked on a log! I don't know if I camphor-give you for that!")
        }

    private suspend fun ProtectedAccess.talkIronwood(npc: Npc) =
        startDialogue(npc) {
            chatNpc(neutral, "The truth is...")
            chatPlayer(quiz, "...")
            chatNpc(happy, "I am ironwood.")
        }

    private suspend fun ProtectedAccess.talkJatoba(npc: Npc) =
        startDialogue(npc) {
            chatNpc(
                worried,
                "Everyone always asks why, what and when but never how. I'm jatoba-whelmed with " +
                    "all of this!",
            )
        }

    private suspend fun ProtectedAccess.talkRosewood(npc: Npc) =
        startDialogue(npc) {
            chatNpc(
                happy,
                "Eating logs is like enjoying wine. They all have a nuance of flavour. That last " +
                    "one for example was earthy, with hints of nuts and redberries, along with a " +
                    "scent of rose.",
            )
        }

    private suspend fun ProtectedAccess.talkFox(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatNpc(neutral, "...maybe if we all made an underground village together...")
                    chatPlayer(quiz, "What are you pondering, my furry friend?")
                    chatNpc(happy, "Oh, just a grand plan to save all the wildlife from being hunted.")
                    chatPlayer(happy, "That is quite fantastic, fox!")
                }
                1 -> {
                    chatPlayer(quiz, "Hello fox, what do you say?")
                    chatNpc(angry, "I do wish people would stop asking me that!")
                }
                else -> {
                    chatNpc(sad, "I'm famished! Do you have any food?")
                    chatPlayer(happy, "I'm sure we could find you something.")
                    chatNpc(shifty, "How about we visit a chicken coop to, uh, admire the scenery!")
                    chatPlayer(worried, "I better find you something to eat quickly.")
                }
            }
        }

    private suspend fun ProtectedAccess.talkPheasant(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatNpc(sad, "I can't believe people try to eat us.")
                    chatPlayer(quiz, "What would you taste like? Chicken?")
                    chatNpc(angry, "I'd taste fowl, you wouldn't like it at all...")
                }
                1 -> {
                    chatPlayer(quiz, "What's the Freaky Forester like?")
                    chatNpc(
                        angry,
                        "You mean the odd fellow who recruits random adventurers to butcher an " +
                            "exorbitant amount of my kind daily in exchange for his spare clothing?",
                    )
                    chatNpc(bored, "Why yes, he's simply delightful...")
                }
                else -> {
                    chatPlayer(quiz, "You look a bit on edge. Are you okay?")
                    chatNpc(
                        worried,
                        "I have the strangest urge to run infront of a fast moving adventurer...",
                    )
                }
            }
        }

    private suspend fun ProtectedAccess.useItemBeaver(npc: Npc, obj: ItemServerType) {
        if (morphs.tryUse(this, npc, obj)) {
            return
        }
        val name = obj.name
        when {
            name in COLOURED_LOGS ->
                startDialogue(npc) {
                    chatNpc(
                        angry,
                        "Eww! What is that spell? Why would I want to chew on that? It stinks!",
                    )
                }
            name.endsWith("pyre logs", ignoreCase = true) ->
                startDialogue(npc) {
                    chatNpc(quiz, "What's this strange coating on the log?")
                    chatPlayer(
                        neutral,
                        "Olive oil blessed at an alter deep in Morytania. The oil helps with " +
                            "burning the shades in the area.",
                    )
                    chatNpc(confused, "...and you want me to chew on it?")
                    chatPlayer(worried, "Well...")
                    chatNpc(angry, "That's a hard pass from me.")
                }
            name.equals("Achey tree logs", ignoreCase = true) ->
                startDialogue(npc) {
                    chatNpc(quiz, "There's something not quite right about this log.")
                    chatPlayer(quiz, "What do you mean?")
                    chatNpc(
                        neutral,
                        "I'm not sure, it looks like it's in pain, maybe a little... achey?",
                    )
                    chatPlayer(bored, "Sigh...")
                }
            name.equals("Blisterwood logs", ignoreCase = true) ->
                startDialogue(npc) {
                    chatNpc(
                        angry,
                        "*sniff* *sniff* No thanks. These may look like logs but they stink of " +
                            "iron, what do I look like a rock golem?",
                    )
                }
            name.equals("Juniper logs", ignoreCase = true) ->
                startDialogue(npc) {
                    chatNpc(
                        neutral,
                        "*sniff* *sniff* No thanks. These logs smell just a bit too botanical for " +
                            "my liking. More of a beer beaver myself.",
                    )
                }
        }
    }

    private suspend fun ProtectedAccess.useItemFox(npc: Npc, obj: ItemServerType) {
        if (morphs.tryUse(this, npc, obj)) {
            return
        }
        when {
            obj.name in CHICKEN ->
                startDialogue(npc) {
                    chatNpc(happy, "Ooh, don't mind if I do!")
                    mesbox("You feed the chicken to your fox.")
                }
        }
    }

    private suspend fun ProtectedAccess.useItemPheasant(npc: Npc, obj: ItemServerType) {
        if (morphs.tryUse(this, npc, obj)) {
            return
        }
        when {
            obj.name.equals("Redberries", ignoreCase = true) ->
                startDialogue(npc) {
                    chatNpc(happy, "Redberries! Please and thank you!")
                    mesbox("You feed the berries to your pheasant.")
                }
        }
    }

    private companion object {
        const val NORMAL = "npc.skillpetwc"
        const val OAK = "npc.skillpet_wc_oak"
        const val WILLOW = "npc.skillpet_wc_willow"
        const val MAPLE = "npc.skillpet_wc_maple"
        const val YEW = "npc.skillpet_wc_yew"
        const val MAGIC = "npc.skillpet_wc_magic"
        const val REDWOOD = "npc.skillpet_wc_redwood"
        const val TEAK = "npc.skillpet_wc_teak"
        const val MAHOGANY = "npc.skillpet_wc_mahogany"
        const val ARCTIC_PINE = "npc.skillpet_wc_arctic"
        const val CAMPHOR = "npc.skillpet_wc_camphor"
        const val IRONWOOD = "npc.skillpet_wc_ironwood"
        const val JATOBA = "npc.skillpet_wc_jatoba"
        const val ROSEWOOD = "npc.skillpet_wc_rosewood"
        const val FOX = "npc.skillpet_wc_fox"
        const val PHEASANT = "npc.skillpet_wc_pheasant"

        val BEAVER_FORMS =
            listOf(
                NORMAL,
                OAK,
                WILLOW,
                MAPLE,
                YEW,
                MAGIC,
                REDWOOD,
                TEAK,
                MAHOGANY,
                ARCTIC_PINE,
                CAMPHOR,
                IRONWOOD,
                JATOBA,
                ROSEWOOD,
            )

        val COLOURED_LOGS = setOf("Red logs", "Green logs", "Blue logs", "White logs", "Purple logs")
        val CHICKEN = setOf("Raw chicken", "Cooked chicken")
    }
}
