package org.rsmod.content.other.pets

import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PetScript
@Inject
constructor(
    private val followers: PetFollowers,
    private val rewards: PetRewards,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript(), PlayerPostTickHook {
    override fun ScriptContext.startup() {
        for (pet in Pets.all) {
            for (form in pet.forms) {
                onOpHeld5(form.obj) { dropPet(it.slot, form) }
                onPetOp(form.npc, PICK_UP_OP) { pickUp(it, form) }
            }
        }
        onPlayerLogout { followers.onLogout(player) }
        onIfOverlayButton("component.wornitems:call_follower") {
            if (!followers.call(player)) {
                mes("You do not have a follower.")
            }
        }
        onCommand("pet") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Award a pet through the normal pet-obtain rules"
            invalidArgs = "Use as ::pet to pick from a list, or ::pet objDebugName (ex: ::pet hell_pet)"
            cheat { givePet() }
        }
    }

    override fun onPostTick(player: Player) {
        followers.onPostTick(player)
    }

    private fun ProtectedAccess.dropPet(slot: Int, form: PetForm) {
        if (followers.hasFollower(player)) {
            mes("You already have a follower.")
            return
        }
        val result = invDel(inv, form.obj, count = 1, slot = slot)
        if (!result.success) {
            return
        }
        followers.spawn(player, form)
    }

    private fun ProtectedAccess.pickUp(npc: Npc, form: PetForm) {
        if (!followers.requireOwned(this, npc)) {
            return
        }
        if (inv.isFull()) {
            mes("You don't have enough inventory space to pick up your follower.")
            return
        }
        followers.dismiss(player)
        invAdd(inv, form.obj, 1)
    }

    private fun Cheat.givePet() {
        if (args.isEmpty()) {
            protectedAccess.launch(player) { choosePet() }
            return
        }
        val obj = "obj.${args[0]}"
        if (Pets.forObj(obj) == null) {
            player.mes("That obj is not a pet: $obj")
            return
        }
        rewards.give(player, obj)
    }

    private suspend fun ProtectedAccess.choosePet() {
        val pets = Pets.all.sortedBy { it.name }
        val labels = pets.map { it.name } + CANCEL
        val pet = pets.getOrNull(menu(PET_MENU, *labels.toTypedArray())) ?: return
        rewards.give(player, pet.base.obj)
    }

    private companion object {
        const val PICK_UP_OP = "Pick-up"
        const val PET_MENU = "Select a pet"
        const val CANCEL = "Cancel"
    }
}
