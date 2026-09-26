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

class TumekensGuardianDialogue @Inject constructor(private val morphs: PetMorphs) : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(TUMEKEN, "Talk-to") { talkTumeken(it) }
        onPetOp(ELIDINIS, "Talk-to") { talkElidinis(it) }
        onPetOp(TUMEKEN_DAMAGED, "Talk-to") { talkTumekenDamaged(it) }
        onPetOp(ELIDINIS_DAMAGED, "Talk-to") { talkElidinisDamaged(it) }
        onPetOp(AKKHITO, "Talk-to") { talkAkkhito(it) }
        onPetOp(BABI, "Talk-to") { talkBabi(it) }
        onPetOp(KEPHRITI, "Talk-to") { talkKephriti(it) }
        onPetOp(ZEBO, "Talk-to") { talkZebo(it) }
        onPetOpU(ZEBO) { useItemZebo(it.npc, it.objType) }
    }

    private suspend fun ProtectedAccess.talkTumeken(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "So how are you doing?")
            chatNpc(
                neutral,
                "We walk in the light of Tumeken. Gone he may be, but his fire burns on through " +
                    "all who remain. For some, the fire heals. For others, it destroys. This is " +
                    "as it should be. It is all part of the plan.",
            )
            chatPlayer(confused, "Okay... never mind.")
        }

    private suspend fun ProtectedAccess.talkTumekenDamaged(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "So how are you doing?")
            chatNpc(confused, "... light... meken. Gone... fire bur...")
            chatPlayer(worried, "Are you okay?")
            chatNpc(confused, "*Brrrrrrrrr* 404: *Zkxhcz*")
            chatPlayer(worried, "Maybe you should get some rest?")
            chatNpc(confused, "Malfunction... ERROR... Malfunction...")
        }

    private suspend fun ProtectedAccess.talkElidinis(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "So how are you doing?")
            chatNpc(
                neutral,
                "We are ever faithful to the guidance of Elidinis. Her water sustains us, and " +
                    "brings life to the lifeless. We grow with her, and she with us. For as " +
                    "long as we remember her, we will never be alone.",
            )
            chatPlayer(confused, "Okay... never mind.")
        }

    private suspend fun ProtectedAccess.talkElidinisDamaged(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "So how are you doing?")
            chatNpc(confused, "... faithful... guidan... dinis... water... life...")
            chatPlayer(worried, "Are you okay?")
            chatNpc(confused, "*Brrrrrrrrr* 34: *KzZhc*")
            chatPlayer(worried, "Maybe you should get some rest?")
            chatNpc(confused, "Instructions... unclear... ERROR... stuck...")
        }

    private suspend fun ProtectedAccess.talkAkkhito(npc: Npc) =
        startDialogue(npc) {
            chatNpc(angry, "May shadows be your grave!")
            chatPlayer(neutral, "That's not a nice way to greet someone, is it?")
            chatNpc(angry, "All will be ashes!")
            chatPlayer(quiz, "Has anyone ever told you that you're a bit gloomy?")
            chatNpc(angry, "Resisting only delays the inevitable!")
            chatPlayer(bored, "Alright then.")
        }

    private suspend fun ProtectedAccess.talkBabi(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "What's life like as an baboon?")
            chatNpc(neutral, "Have you seen me? I look a bit dead. How about you tell me what's life like.")
            chatPlayer(neutral, "Well, it's just life, isn't it?")
            when (access.random.of(2)) {
                0 -> {
                    chatNpc(neutral, "Well, it's just life, isn't it?")
                    chatPlayer(confused, "Yes... That's what I said.")
                    chatNpc(neutral, "Yes... That's what I said.")
                    chatPlayer(angry, "Stop aping!")
                    chatNpc(happy, "I physically cannot do that.")
                }
                else -> {
                    chatNpc(neutral, "Then maybe the same could be said for me.")
                    chatPlayer(quiz, "I suppose. I would really like to know your story though.")
                    chatNpc(
                        neutral,
                        "Now that's a different question. I am the daughter of a most powerful matriarch.",
                    )
                    chatNpc(
                        happy,
                        "While I grew up with many siblings, she always took care of us. In fact, " +
                            "I've never been scared.",
                    )
                    chatPlayer(quiz, "Why do you think that is?")
                    chatNpc(happy, "It must be due to her motherly love and my great family.")
                    chatPlayer(shifty, "Mhm. I'm sure... That must be it.")
                }
            }
        }

    private suspend fun ProtectedAccess.talkKephriti(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(happy, "Hello, little one.")
            chatNpc(silent, "...")
            chatPlayer(quiz, "Hello?")
            chatNpc(silent, "...")
            chatPlayer(sad, "Do you not like me?")
            chatNpc(silent, "...")
            chatPlayer(quiz, "Are you shy? Why would you follow me and not say anything?")
            chatNpc(
                neutral,
                "Do not mistake my silence for shyness. Can't we just quietly share the road together?",
            )
            chatPlayer(neutral, "Oh... well, yes, I suppose we can.")
            chatNpc(silent, "...")
        }

    private suspend fun ProtectedAccess.talkZebo(npc: Npc) =
        startDialogue(npc) {
            chatNpc(angry, "Feed me.")
            chatPlayer(neutral, "That's quite demanding, but sure. What would you like?")
            chatNpc(angry, "I demand sustenance.")
            chatPlayer(quiz, "I might have some cooked lobsters somewhere... Maybe a Saradomin brew?")
            chatNpc(angry, "Food.")
            chatPlayer(bored, "You really have a one-track mind, don't you?")
            chatNpc(angry, "Hungry.")
        }

    private suspend fun ProtectedAccess.useItemZebo(npc: Npc, obj: ItemServerType) {
        if (morphs.tryUse(this, npc, obj)) {
            return
        }
        val name = obj.name
        val raw = name.equals("Raw beef", ignoreCase = true) || name.equals("Raw chicken", ignoreCase = true)
        if (!raw) {
            return
        }
        startDialogue(npc) {
            mesbox("You wave the $name in front of Zebo.")
            chatPlayer(happy, "Look what I have!")
            chatNpc(neutral, "I'm on a diet.")
            chatPlayer(shocked, "You're joking aren't you?!")
            chatNpc(shifty, "I only eat adventurers... Like you.")
            chatPlayer(worried, "Oh... okay, as you were.")
            chatNpc(angry, "Feeeed me!")
        }
    }

    private companion object {
        const val TUMEKEN = "npc.warden_pet_tumeken"
        const val ELIDINIS = "npc.warden_pet_elidinis"
        const val TUMEKEN_DAMAGED = "npc.warden_pet_tumeken_destroyed"
        const val ELIDINIS_DAMAGED = "npc.warden_pet_elidinis_destroyed"
        const val AKKHITO = "npc.warden_pet_akkha"
        const val BABI = "npc.warden_pet_baba"
        const val KEPHRITI = "npc.warden_pet_kephri"
        const val ZEBO = "npc.warden_pet_zebak"
    }
}
