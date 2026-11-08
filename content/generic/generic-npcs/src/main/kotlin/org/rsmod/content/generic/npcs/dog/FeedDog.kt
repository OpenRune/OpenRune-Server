package org.rsmod.content.generic.npcs.dog

import dev.openrune.types.ItemServerType
import org.rsmod.api.config.Constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.table.prayer.SkillPrayerRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Stray dogs eat bones. A stray dog is meant to follow the player for a while afterwards, which is
 * not implemented.
 */
class FeedDog : PluginScript() {
    private val bones =
        SkillPrayerRow.all().filterNot { it.ashes }.map { it.item.internalName }.toSet()

    override fun ScriptContext.startup() {
        STRAY_DOGS.forEach { dog -> onOpNpcU(dog) { feed(it.objType) } }
    }

    private fun ProtectedAccess.feed(obj: ItemServerType) {
        if (obj.internalName == SHAIKAHAN_BONES) {
            mes("That's disgusting - you can't give it to the poor dog!")
            return
        }
        if (obj.internalName !in bones) {
            mes(Constants.dm_default)
            return
        }
        if (invDel(inv, obj.internalName, 1).failure) {
            return
        }
        // UNVERIFIED: the dog is documented as barking happily, without a message.
        mes("You feed the dog the bones.")
    }

    private companion object {
        const val SHAIKAHAN_BONES = "obj.tbwt_beast_bones"

        val STRAY_DOGS =
            listOf(
                "npc.dog_stray",
                "npc.dog_stray2",
                "npc.varlamore_dog_chiribaya_1",
                "npc.varlamore_dog_chiribaya_1_indoors",
                "npc.varlamore_dog_chiribaya_2",
            )
    }
}
