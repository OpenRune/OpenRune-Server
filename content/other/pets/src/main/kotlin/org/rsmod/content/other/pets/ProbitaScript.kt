package org.rsmod.content.other.pets

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpNpcU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ProbitaScript
@Inject
constructor(private val rewards: PetRewards, private val insurance: PetInsurance) : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(PROBITA, TALK_OP) { talk(it) }
        onPetOp(PROBITA, CHECK_OP) { openInsurance() }
        onOpNpcU(PROBITA) { useItem(it.npc, it.objType) }
        onIfModalButton(PETS_COMPONENT) { button ->
            when (button.op) {
                IfButtonOp.Op1 -> reclaim(button.comsub)
                IfButtonOp.Op10 -> examine(button.comsub)
                else -> Unit
            }
        }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            insurance.insureOwned(player)
            chatNpc(happy, "Welcome to the pet insurance bureau. How can I help you?")
            mainMenu()
        }

    private suspend fun Dialogue.mainMenu() {
        val option =
            choice4(
                "Can you tell me about pets and pet insurance?",
                1,
                "I've lost a pet. Have you got it?",
                2,
                "I have a pet that I'd like to insure.",
                3,
                "More options...",
                4,
            )
        when (option) {
            1 -> aboutInsurance()
            2 -> lostPet()
            3 -> insurePet()
            4 -> moreOptions()
        }
    }

    private suspend fun Dialogue.aboutInsurance() {
        chatPlayer(quiz, "Can you tell me about pets and pet insurance?")
        chatNpc(
            happy,
            "Certainly! You can find pets while out on your adventures. They're very rare, but " +
                "you may be lucky enough to find one.",
        )
        chatNpc(
            neutral,
            "You have a chance to find them when defeating bosses, playing minigames, and taking " +
                "part in skilling activities.",
        )
        chatNpc(happy, "If you do find one, they will be automatically insured against loss, for free!")
        chatNpc(
            neutral,
            "If you then somehow lose your pet, you can reclaim it here for free also. The service " +
                "used to cost Reclaim tokens worth 1,000,000 coins each. If you have any of those, " +
                "I can refund you.",
        )
        chatPlayer(happy, "I see, thank you.")
        mainMenu()
    }

    private suspend fun Dialogue.lostPet() {
        chatPlayer(sad, "I've lost a pet. Have you got it?")
        chatNpc(happy, "If you've lost your pet, it will be reclaimable here!")
        chatNpc(neutral, "Let's have a look...")
        access.openInsurance()
    }

    private suspend fun Dialogue.insurePet() {
        chatPlayer(quiz, "I have a pet that I'd like to insure.")
        chatNpc(
            happy,
            "You don't need to pay to insure your pet with me. They're automatically insured for " +
                "free as soon as you find them!",
        )
        chatNpc(
            neutral,
            "If you lose a pet somehow, you can come here and reclaim them. It used to cost reclaim " +
                "tokens, although we're now subsidised by the Grand Exchange and the service is free.",
        )
        chatNpc(neutral, "If you have any reclaim tokens left, let us know and we will buy them back.")
        mainMenu()
    }

    private suspend fun Dialogue.moreOptions() {
        val option =
            choice4(
                "What pets have I insured?",
                1,
                "My pet is still lost. Could you look again?",
                2,
                "Maybe another time.",
                3,
                "Previous options...",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What pets have I insured?")
                chatNpc(happy, "Every pet you've ever found is automatically insured here!")
                chatNpc(happy, "Here, take a look...")
                access.openInsurance()
            }
            2 -> {
                chatPlayer(sad, "My pet is still lost. Could you look again?")
                chatNpc(neutral, "It's possible there was an error in our records. Let me check again for you.")
                player.mes(
                    "<col=00ff00>If you believe you have some pets that are not showing on your insurance " +
                        "record, please visit Probita with your pets and she will automatically insure " +
                        "them for you.</col>"
                )
                access.openInsurance()
            }
            3 -> chatPlayer(neutral, "Maybe another time.")
            4 -> mainMenu()
        }
    }

    private suspend fun ProtectedAccess.useItem(npc: Npc, obj: ItemServerType) =
        startDialogue(npc) {
            when {
                Pets.forObj(obj.id) != null ->
                    chatNpc(
                        happy,
                        "You don't need to pay to insure your pet with me. They're automatically " +
                            "insured for free as soon as you find them!",
                    )
                obj.internalName == COINS -> chatNpc(happy, "If you keep waving money at me, I might take it!")
                else -> chatNpc(neutral, "This is a pet insurance bureau. I'm not insuring that.")
            }
        }

    private fun ProtectedAccess.openInsurance() {
        insurance.insureOwned(player)
        ifOpenMainModal(INTERFACE)
        refreshInsurance()
        ifSetEvents(PETS_COMPONENT, 0..lastRowComsub(), IfEvent.Op1, IfEvent.Op10)
    }

    private fun ProtectedAccess.refreshInsurance() {
        runClientScript(INIT_SCRIPT.asRSCM(RSCMType.CLIENTSCRIPT), *insurance.interfaceArgs(player).toTypedArray())
    }

    private fun ProtectedAccess.reclaim(comsub: Int) {
        val pet = rowPet(comsub, reclaimableSection = true) ?: return
        if (!insurance.isInsured(player, pet) || player.ownsPet(pet)) {
            return
        }
        if (!rewards.reclaim(player, pet)) {
            spam("You don't have enough inventory space to withdraw that many.")
            return
        }
        refreshInsurance()
    }

    private fun ProtectedAccess.examine(comsub: Int) {
        val pet = rowPet(comsub, reclaimableSection = false) ?: return
        val examine = ServerCacheManager.getItem(pet.base.objId)?.examine ?: return
        mes(examine)
    }

    private fun lastRowComsub(): Int = FIRST_ROW_COMSUB + insurance.slots * ROW_COMPONENTS * 2 - 1

    private fun ProtectedAccess.rowPet(comsub: Int, reclaimableSection: Boolean): Pet? {
        val offset = comsub - FIRST_ROW_COMSUB
        if (offset < 0) {
            return null
        }
        val sectionSize = insurance.slots * ROW_COMPONENTS
        val hasReclaimable = insurance.hasReclaimable(player)
        val slot =
            when {
                !hasReclaimable -> if (reclaimableSection) return null else offset / ROW_COMPONENTS
                offset < sectionSize -> offset / ROW_COMPONENTS
                reclaimableSection -> return null
                else -> (offset - sectionSize) / ROW_COMPONENTS
            }
        return insurance.pet(slot)
    }

    private companion object {
        const val PROBITA = "npc.pet_insurance_broker"
        const val TALK_OP = "Talk-to"
        const val CHECK_OP = "Check"
        const val COINS = "obj.coins"
        const val INTERFACE = "interface.pet_insurance"
        const val PETS_COMPONENT = "component.pet_insurance:pets"
        const val INIT_SCRIPT = "clientscript.[clientscript,pet_insurance_init]"
        const val FIRST_ROW_COMSUB = 6
        const val ROW_COMPONENTS = 5
    }
}
