package org.rsmod.content.generic.npcs.dog

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.player.events.interact.NpcUEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.table.prayer.SkillPrayerRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FeedDog : PluginScript() {
    private val refused: List<ItemServerType> = REFUSED.map(::item)

    private val food: List<ItemServerType> =
        (SkillPrayerRow.all().filterNot { it.ashes }.map { it.item } + MEAT.map(::item))
            .filterNot { obj -> refused.any { it.id == obj.id } }
            .distinctBy { it.id }

    override fun ScriptContext.startup() {
        for (name in STRAY_DOGS) {
            val dog = checkNotNull(ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC)))
            food.forEach { obj -> onOpNpcU(dog, obj) { eat(it) } }
            refused.forEach { obj -> onOpNpcU(dog, obj) { mes(REFUSAL) } }
        }
    }

    private fun ProtectedAccess.eat(op: NpcUEvents.Op) {
        invDel(inv, op.objType.internalName, 1, slot = op.invSlot)
    }

    private fun item(name: String): ItemServerType =
        checkNotNull(ServerCacheManager.getItem(name.asRSCM(RSCMType.OBJ)))

    private companion object {
        const val REFUSAL = "That's disgusting - you can't give it to the poor dog!"

        val REFUSED =
            listOf(
                "obj.tbwt_beast_bones",
                "obj.tbwt_jogre_bones_marinated_in_karambwanji",
                "obj.tbwt_burnt_jogre_bones_marinated_in_karambwanji",
            )

        val MEAT = listOf("obj.raw_beef", "obj.raw_rat_meat", "obj.raw_bear_meat", "obj.cooked_meat")

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
