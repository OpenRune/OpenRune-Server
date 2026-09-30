package org.rsmod.content.other.pets

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal fun nextUnlockedFormIndex(
    formCount: Int,
    currentIndex: Int,
    isUnlocked: (Int) -> Boolean,
): Int? {
    if (formCount < 2 || currentIndex !in 0 until formCount) {
        return null
    }
    for (offset in 1 until formCount) {
        val index = (currentIndex + offset) % formCount
        if (isUnlocked(index)) {
            return index
        }
    }
    return null
}

class PetMetamorphosisScript
@Inject
constructor(private val followers: PetFollowers, private val morphs: PetMorphs) : PluginScript() {
    override fun ScriptContext.startup() {
        for (pet in Pets.all) {
            if (pet.forms.size < 2 || pet.key in CUSTOM) {
                continue
            }
            for (form in pet.forms) {
                val op = OPS.firstOrNull { petOpIndex(form.npc, it) != null } ?: continue
                onPetOp(form.npc, op) { metamorphose(it, pet) }
            }
        }
        registerChinchompa()
        registerRiftGuardianLock()
    }

    private fun ScriptContext.registerChinchompa() {
        val pet = Pets[CHINCHOMPA]
        for (form in pet.forms) {
            val op = OPS.firstOrNull { petOpIndex(form.npc, it) != null } ?: continue
            onPetOp(form.npc, op) { metamorphoseChinchompa(it, pet, form) }
        }
    }

    private fun ScriptContext.registerRiftGuardianLock() {
        for (form in Pets[RIFT_GUARDIAN].forms) {
            onPetOpIfPresent(form.npc, LOCKING_OP) { toggleRiftGuardianLock(it) }
        }
    }

    private suspend fun ProtectedAccess.metamorphose(npc: Npc, pet: Pet) {
        if (!followers.requireOwned(this, npc)) {
            return
        }
        // Read the current form from the follower item when clicked instead of the form
        // captured when the NPC operation was registered.
        val (followingPet, current) = Pets.forObj(player.followerObj) ?: return
        if (followingPet.key != pet.key) {
            return
        }
        val next = nextForm(pet, current)
        if (next == null) {
            if (pet.key == PHOENIX) {
                mesbox(
                    "You haven't yet unlocked any alternative colours for your phoenix. Use 250 gnomish " +
                        "firelighters of any one colour on your pet to unlock that colour. Your phoenix is " +
                        "currently ${morphs.phoenixColour(current).lowercase()}.",
                )
            } else {
                mes("Your ${pet.name.lowercase()} has no other forms unlocked.")
            }
            return
        }
        followers.spawn(player, next)
    }

    private fun ProtectedAccess.nextForm(pet: Pet, current: PetForm): PetForm? {
        val start = pet.forms.indexOfFirst { it.objId == current.objId }
        val nextIndex =
            nextUnlockedFormIndex(pet.forms.size, start) { index -> morphs.unlocked(player, pet.forms[index]) }
                ?: return null
        return pet.forms[nextIndex]
    }

    private suspend fun ProtectedAccess.metamorphoseChinchompa(npc: Npc, pet: Pet, current: PetForm) {
        if (!followers.requireOwned(this, npc)) {
            return
        }
        val gold = pet.forms.first { it.obj == CHINCHOMPA_GOLD }
        if (current === gold) {
            var confirm = false
            startDialogue(npc) {
                confirm = choice2("Yes", true, "No", false, title = "Really change its colour?")
            }
            if (confirm) {
                followers.spawn(player, pet.base)
            }
            return
        }
        if (random.of(CHINCHOMPA_GOLD_CHANCE) == 0) {
            followers.spawn(player, gold)
            return
        }
        val colours = pet.forms.filter { it !== gold }
        followers.spawn(player, colours[(colours.indexOf(current) + 1) % colours.size])
    }

    private fun ProtectedAccess.toggleRiftGuardianLock(npc: Npc) {
        if (!followers.requireOwned(this, npc)) {
            return
        }
        val locked = player.vars[RIFT_GUARDIAN_LOCKED] != 0
        VarPlayerIntMapSetter.set(player, RIFT_GUARDIAN_LOCKED, if (locked) 0 else 1)
        if (locked) {
            mes("Your rift guardian will now change colour with the altars you use.")
        } else {
            mes("Your rift guardian will now keep its current colour.")
        }
    }

    private companion object {
        const val PHOENIX = "phoenix"
        const val CHINCHOMPA = "baby_chinchompa"
        const val CHINCHOMPA_GOLD = "obj.skillpethunter_gold"
        const val CHINCHOMPA_GOLD_CHANCE = 10_000
        const val RIFT_GUARDIAN = "rift_guardian"
        const val RIFT_GUARDIAN_LOCKED = "varbit.skillpet_runecrafting_locked"
        const val LOCKING_OP = "Locking"
        val OPS = listOf("Metamorphosis", "Metamorph")
        val CUSTOM = setOf("heron", CHINCHOMPA)
    }
}
