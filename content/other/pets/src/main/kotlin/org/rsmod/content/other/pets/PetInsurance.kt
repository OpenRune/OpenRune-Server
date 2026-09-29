package org.rsmod.content.other.pets

import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.enum
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

@Singleton
class PetInsurance @Inject constructor() {
    private val petBySlot: Map<Int, Pet> = buildMap {
        for ((slot, obj) in enum<Int, ItemServerType>("pet_insurance_pets").backing) {
            val pet = obj?.let { Pets.forObj(it.id) }?.first ?: continue
            put(slot, pet)
        }
    }

    private val slotByPet: Map<Pet, Int> = petBySlot.entries.associate { (slot, pet) -> pet to slot }

    val slots: Int = (petBySlot.keys.maxOrNull() ?: -1) + 1

    fun pet(slot: Int): Pet? = petBySlot[slot]

    fun isInsured(player: Player, pet: Pet): Boolean {
        val slot = slotByPet[pet] ?: return false
        return player.vars[maskVarp(slot)] and bit(slot) != 0
    }

    /** Returns false when the pet has no slot in the insurance enum, so it cannot be insured. */
    fun insure(player: Player, pet: Pet): Boolean {
        val slot = slotByPet[pet] ?: return false
        val varp = maskVarp(slot)
        VarPlayerIntMapSetter.set(player, varp, player.vars[varp] or bit(slot))
        return true
    }

    fun insureOwned(player: Player) {
        for (pet in petBySlot.values) {
            if (player.ownsPet(pet)) {
                insure(player, pet)
            }
        }
    }

    fun hasReclaimable(player: Player): Boolean =
        petBySlot.values.any { isInsured(player, it) && !player.ownsPet(it) }

    fun interfaceArgs(player: Player): List<Int> {
        val insured = IntArray(MASK_VARPS.size)
        val owned = IntArray(MASK_VARPS.size)
        val locations = IntArray(LOCATION_INTS)
        for ((slot, pet) in petBySlot) {
            if (!isInsured(player, pet)) {
                continue
            }
            insured[maskIndex(slot)] = insured[maskIndex(slot)] or bit(slot)
            val location = player.petLocation(pet)
            if (location == PetLocation.NONE) {
                continue
            }
            owned[maskIndex(slot)] = owned[maskIndex(slot)] or bit(slot)
            val locationInt = slot / LOCATIONS_PER_INT
            locations[locationInt] = locations[locationInt] or (location shl (slot % LOCATIONS_PER_INT * LOCATION_BITS))
        }
        return insured.toList() + owned.toList() + locations.toList()
    }

    private fun maskIndex(slot: Int): Int = slot / BITS_PER_MASK

    private fun bit(slot: Int): Int = 1 shl (slot % BITS_PER_MASK)

    private fun maskVarp(slot: Int): String = MASK_VARPS[maskIndex(slot)]

    private companion object {
        private const val BITS_PER_MASK = 31
        private const val LOCATION_BITS = 4
        private const val LOCATIONS_PER_INT = 8
        private const val LOCATION_INTS = 9
        private val MASK_VARPS =
            listOf(
                "varp.pet_insurance_bitmask1",
                "varp.pet_insurance_bitmask2",
                "varp.pet_insurance_bitmask3",
            )
    }
}
