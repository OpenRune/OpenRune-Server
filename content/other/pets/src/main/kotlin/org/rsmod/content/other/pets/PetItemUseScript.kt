package org.rsmod.content.other.pets

import jakarta.inject.Inject
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PetItemUseScript @Inject constructor(private val morphs: PetMorphs) : PluginScript() {
    override fun ScriptContext.startup() {
        for (pet in Pets.all) {
            for (form in pet.forms.filter { it.itemUse }) {
                onPetOpU(form.npc) { morphs.tryUse(this, it.npc, it.objType) }
            }
            for ((item, petObj) in morphs.heldPairs(pet)) {
                onOpHeldU(item, petObj) { morphs.tryUseHeld(this, petObj, it.first) }
            }
        }
    }
}
