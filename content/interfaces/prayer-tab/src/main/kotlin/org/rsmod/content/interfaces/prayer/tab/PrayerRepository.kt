package org.rsmod.content.interfaces.prayer.tab

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.enums.EnumTypeMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.rsmod.api.config.refs.params
import org.rsmod.content.interfaces.prayer.tab.configs.prayer_enums
import org.rsmod.content.interfaces.prayer.tab.configs.prayer_params

class PrayerRepository() {
    lateinit var prayerComponents: Map<ComponentType, Prayer>
        private set

    lateinit var prayerCollisions: Map<Int, List<Prayer>>
        private set

    lateinit var prayerList: List<Prayer>
        private set

    fun toPrayerList(packed: Int) = prayerList.filter { packed and (1 shl it.id) != 0 }

    fun load() {
        val components = loadMappedComponents()
        val collisions = loadMappedCollisions(components.values)

        this.prayerComponents = components
        this.prayerCollisions = collisions
        this.prayerList = components.values.toList()
    }

    private fun loadMappedComponents(): Map<ComponentType, Prayer> {
        val prayers = mutableMapOf<ComponentType, Prayer>()
        val idCollision = hashSetOf<Int>()

        val enum = prayer_enums.obj_configs.filterValuesNotNull()
        for (obj in enum.values) {
            val id = obj.param(prayer_params.id)
            val component = obj.param(prayer_params.component)
            val name = obj.param(prayer_params.name)
            val level = obj.param(prayer_params.level)
            val sound = obj.param(prayer_params.sound)
            val enabled = obj.param(prayer_params.varbit)
            val drain = obj.param(prayer_params.drain_effect)
            val overhead = obj.paramOrNull(prayer_params.overhead)
            val unlockVar = obj.paramOrNull(prayer_params.unlock_varbit)
            val unlockState = obj.param(prayer_params.unlock_state)
            val defenceReq = obj.paramOrNull(params.statreq1_level)
            val lockedMessage = obj.paramOrNull(prayer_params.locked_message)

            check(idCollision.add(id)) { "Prayer with id `$id` is already in use." }
            check(component !in prayers) { "Prayer with component `$component` is already in use." }

            val prayer =
                Prayer(
                    id = id,
                    name = name,
                    level = level,
                    sound = sound,
                    enabled = enabled,
                    drainEffect = drain,
                    overhead = overhead,
                    unlocked = unlockVar,
                    unlockState = unlockState,
                    defenceReq = defenceReq,
                    lockedMessage = lockedMessage,
                )
            prayers[component] = prayer
        }

        return prayers
    }

    private fun loadMappedCollisions(prayers: Iterable<Prayer>): Map<Int, List<Prayer>> {
        val attack = load(prayers, prayer_enums.attack_collisions)
        val strength = load(prayers, prayer_enums.strength_collisions)
        val defence = load(prayers, prayer_enums.defence_collisions)
        val overhead = load(prayers, prayer_enums.overhead_collisions)

        val collisions = Int2ObjectOpenHashMap<List<Prayer>>()
        for (prayer in prayers) {
            val collision = mutableListOf<Prayer>()
            if (prayer in attack) {
                collision += attack
            }
            if (prayer in strength) {
                collision += strength
            }
            if (prayer in defence) {
                collision += defence
            }
            if (prayer in overhead) {
                collision += overhead
            }
            if (collision.isNotEmpty()) {
                collisions[prayer.id] = collision.distinct()
            }
        }
        return collisions
    }

    private fun load(prayers: Iterable<Prayer>, enumType: EnumTypeMap<Int, Boolean>): List<Prayer> {
        val enum = enumType.filterValuesNotNull()
        val ids = hashSetOf<Int>()
        for ((id, flag) in enum) {
            if (flag) {
                ids += id
            }
        }
        return prayers.filter { it.id in ids }
    }

    operator fun get(prayer: Prayer): List<Prayer> = prayerCollisions[prayer.id] ?: emptyList()
}
