package org.rsmod.content.other.pets

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.PetMorphsRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@Singleton
class PetMorphs @Inject constructor(private val followers: PetFollowers) {
    private class ItemMorph(row: PetMorphsRow) {
        val item: String = row.item.internalName
        val form: String? = row.form?.internalName
        val unlocks: List<String> = row.unlocks.map { RSCM.getReverseMapping(RSCMType.VARBIT, it) }
        val requires: String? = row.requires?.let { RSCM.getReverseMapping(RSCMType.VARBIT, it) }
        val count: Int = row.count
        val consume: Boolean = row.consume
        val held: Boolean = row.held
        val label: String? = row.label
    }

    private val itemMorphs: Map<String, List<ItemMorph>> =
        PetMorphsRow.all().groupBy { it.pet }.mapValues { (_, rows) -> rows.map(::ItemMorph) }

    fun unlocked(player: Player, form: PetForm): Boolean {
        val gate = form.unlock ?: return true
        return player.vars[gate] != 0
    }

    fun heldPairs(pet: Pet): List<Pair<String, String>> {
        val items = itemMorphs[pet.key].orEmpty().filter { it.held && it.form != null }.map { it.item }
        return pet.forms.flatMap { form -> items.map { it to form.obj } }
    }

    suspend fun tryUse(access: ProtectedAccess, npc: Npc, obj: ItemServerType): Boolean {
        val (pet, _) = Pets.forNpc(npc.id) ?: return false
        val morph = itemMorphs[pet.key]?.firstOrNull { it.item == obj.internalName } ?: return false
        if (!followers.requireOwned(access, npc)) {
            return true
        }
        access.apply(pet, morph) { form -> followers.spawn(player, form) }
        return true
    }

    suspend fun tryUseHeld(access: ProtectedAccess, petObj: String, obj: ItemServerType): Boolean {
        val (pet, _) = Pets.forObj(petObj) ?: return false
        val morph = itemMorphs[pet.key]?.firstOrNull { it.item == obj.internalName } ?: return false
        access.apply(pet, morph) { form -> invReplace(inv, petObj, 1, form.obj) }
        return true
    }

    private suspend fun ProtectedAccess.apply(pet: Pet, morph: ItemMorph, deliver: ProtectedAccess.(PetForm) -> Unit) {
        if (pet.key == PHOENIX) {
            applyPhoenix(morph)
            return
        }
        if (morph.requires != null && player.vars[morph.requires] == 0) {
            mes("Your ${pet.name.lowercase()} can't take that form yet.")
            return
        }
        val alreadyUnlocked = morph.unlocks.isNotEmpty() && morph.unlocks.all { player.vars[it] != 0 }
        if (alreadyUnlocked && morph.form == null) {
            mes("You have already unlocked that.")
            return
        }
        if (morph.consume && !alreadyUnlocked) {
            if (invTotal(inv, morph.item) < morph.count) {
                mes("You need ${morph.count} of those to do that.")
                return
            }
            invDel(inv, morph.item, morph.count)
        }
        if (!alreadyUnlocked && morph.unlocks.isNotEmpty()) {
            for (varbit in morph.unlocks) {
                VarPlayerIntMapSetter.set(player, varbit, 1)
            }
            mes("You have unlocked a new metamorphosis for your ${pet.name.lowercase()}.")
        }
        if (morph.form != null) {
            deliver(Pets.forObj(morph.form)!!.second)
        }
    }

    private suspend fun ProtectedAccess.applyPhoenix(morph: ItemMorph) {
        val colour = morph.label ?: return
        val varbit = morph.unlocks.single()
        if (player.vars[varbit] != 0) {
            mesbox("Your phoenix has already been coated in ${colour.lowercase()} chemicals.")
            return
        }
        if (invTotal(inv, morph.item) < morph.count) {
            mesbox("You need at least ${morph.count} firelighters to recolour your phoenix.")
            return
        }
        invDel(inv, morph.item, morph.count)
        VarPlayerIntMapSetter.set(player, varbit, 1)
        objbox(
            morph.item,
            "You coat your phoenix in the $colour chemical, turning it a brilliant $colour! You have now " +
                "unlocked $colour as a colour for your pet.",
        )
    }

    fun phoenixColour(form: PetForm): String =
        itemMorphs[PHOENIX].orEmpty().firstOrNull { it.form == form.obj }?.label ?: "red"

    private companion object {
        const val PHOENIX = "phoenix"
    }
}
