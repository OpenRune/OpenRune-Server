package org.rsmod.content.other.pouches.lootingbag

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.game.entity.Player

/** The safe Enclave permits storage, but only the actual PvP Wilderness permits Destroy spills. */
class LootingBagAreas @Inject constructor(private val areas: AreaChecker) {
    fun canDeposit(player: Player): Boolean = areas.inArea("area.wilderness", player.coords) || areas.inArea("area.ferox_enclave", player.coords)
    fun spillOnDestroy(player: Player): Boolean = areas.inArea("area.wilderness", player.coords) && !areas.inArea("area.ferox_enclave", player.coords)
}
