package org.rsmod.content.other.pets

import jakarta.inject.Singleton

/**
 * Follower forms owned by content outside the pets module, such as quest companions that use the
 * `follower_npc`/`follower_obj` varps. Registering a form lets [PetFollowers] keep that follower
 * alive, respawn it on login and drive its movement like any pet.
 */
@Singleton
class ExternalFollowerForms {
    private val forms = HashMap<Int, PetForm>()

    fun register(form: PetForm) {
        forms[form.objId] = form
    }

    fun formForObj(objId: Int): PetForm? = forms[objId]
}
