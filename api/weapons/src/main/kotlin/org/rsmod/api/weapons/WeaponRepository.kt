package org.rsmod.api.weapons

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack

public class WeaponRepository @Inject constructor(private val registry: WeaponRegistry) {
    private val mappedContentGroups by lazy { loadMappedContentGroups() }

    public fun <T : CombatAttack> register(internal: String, weapon: Weapon<T>) {
        val obj = ServerCacheManager.getItem(internal.asRSCM(RSCMType.OBJ))
            ?: error("No item mapped: $internal")

        val result = registry.add(obj, weapon)
        assertValidResult(obj, result)
    }

    public fun <T : CombatAttack> registerByContent(content: String, weapon: Weapon<T>) {
        val weapons = mappedContentGroups[content.asRSCM(RSCMType.CONTENT)] ?: emptyList()
        require(weapons.isNotEmpty()) { "No weapons associated with content group: $content" }
        for (obj in weapons) {
            registry.add(obj, weapon)
        }
    }

    public fun <T : CombatAttack> replace(obj: ItemServerType, weapon: Weapon<T>) {
        registry.remove(obj)
        registry.add(obj, weapon)
    }

    private fun loadMappedContentGroups(): Map<Int, List<ItemServerType>> {
        val categorized =
            ServerCacheManager.getItems().values.filter {
                it.contentGroup != -1 && it.weaponCategory?.id != 0
            }
        return categorized.groupBy { it.contentGroup }
    }

    private fun assertValidResult(weapon: ItemServerType, result: WeaponRegistry.Result.Add) {
        when (result) {
            WeaponRegistry.Result.Add.AlreadyAdded -> error("Weapon already mapped: $weapon")
            WeaponRegistry.Result.Add.Success -> {
                /* no-op */
            }
        }
    }
}
