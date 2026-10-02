package org.rsmod.content.skills.magic.arceuus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.prayerLvl
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.table.prayer.SkillPrayerRow
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal var Player.arceuusOfferingCooldown by boolVarBit("varbit.arceuus_offering_cooldown")

class OfferingScript
@Inject
constructor(private val spells: MagicSpellRegistry, private val runes: MagicRuneManager) :
    PluginScript() {
    private val demonicAshes: Map<Int, SkillPrayerRow> by lazy {
        DEMONIC_ASH_ROWS.map { SkillPrayerRow.getRow(it) }.associateBy { it.item.id }
    }

    private val bones: Map<Int, SkillPrayerRow> by lazy {
        SkillPrayerRow.all().filter { !it.ashes }.associateBy { it.item.id }
    }

    private val doublePrayerRestoreIds: Set<Int> by lazy {
        DOUBLE_RESTORE_ROWS.map { SkillPrayerRow.getRow(it).item.id }.toSet() +
            SkillPrayerRow.getRow(INFERNAL_ASH_ROW).item.id
    }

    override fun ScriptContext.startup() {
        onPlayerQueue(COOLDOWN_QUEUE) { player.arceuusOfferingCooldown = false }
        onIfOverlayButton("component.magic_spellbook:demonic_offering") {
            cast(DEMONIC_OFFERING_OBJ, demonicAshes, "You have no demonic ashes to offer.")
        }
        onIfOverlayButton("component.magic_spellbook:sinister_offering") {
            cast(SINISTER_OFFERING_OBJ, bones, "You have no bones to offer.")
        }
    }

    private fun ProtectedAccess.cast(
        spellObjSymbol: String,
        offerable: Map<Int, SkillPrayerRow>,
        noneMessage: String,
    ) {
        if (player.arceuusOfferingCooldown) {
            return
        }

        val picked = pick(offerable)
        if (picked.isEmpty()) {
            mes(noneMessage)
            return
        }
        if (picked.any { (slotRow, _) -> slotRow.item.id == SUPERIOR_BONES_ID } && player.prayerLvl < 70) {
            mes("You require a Prayer level of 70 to do that.")
            return
        }

        val spellObj = ServerCacheManager.getItem(spellObjSymbol.asRSCM(RSCMType.OBJ)) ?: return
        val spell = spells.getObjSpell(spellObj) ?: return
        if (runes.attemptCast(player, spell).isFailure()) {
            return
        }

        var xp = 0.0
        var restore = 0
        for ((row, slot) in picked) {
            if (invDel(inv, row.item.internalName, 1, slot).failure) {
                continue
            }
            xp += row.exp * XP_MULTIPLIER
            restore += if (row.item.id in doublePrayerRestoreIds) 2 else 1
        }

        player.arceuusOfferingCooldown = true
        queue(COOLDOWN_QUEUE, COOLDOWN_TICKS)
        anim(if (spellObjSymbol == DEMONIC_OFFERING_OBJ) DEMONIC_ANIM else SINISTER_ANIM)
        spotanim(if (spellObjSymbol == DEMONIC_OFFERING_OBJ) DEMONIC_SPOTANIM else SINISTER_SPOTANIM)
        statAdvance("stat.magic", spell.castXp)
        statAdvance("stat.prayer", xp)
        statHeal("stat.prayer", restore, 0)
    }

    private fun ProtectedAccess.pick(offerable: Map<Int, SkillPrayerRow>): List<Pair<SkillPrayerRow, Int>> {
        val picked = ArrayList<Pair<SkillPrayerRow, Int>>(MAX_OFFERED)
        for (slot in inv.indices) {
            val obj = inv[slot] ?: continue
            val row = offerable[obj.id] ?: continue
            repeat(minOf(obj.count, MAX_OFFERED - picked.size)) { picked += row to slot }
            if (picked.size >= MAX_OFFERED) break
        }
        return picked
    }

    internal companion object {
        const val DEMONIC_OFFERING_OBJ = "obj.placeholder_skillpethunter_gold"
        const val SINISTER_OFFERING_OBJ = "obj.poh_temp_limestonebrick"
        const val DEMONIC_ANIM = "seq.human_cast_offering"
        const val SINISTER_ANIM = "seq.human_cast_offering"
        const val DEMONIC_SPOTANIM = "spotanim.demonic_offering_cast_spotanim"
        const val SINISTER_SPOTANIM = "spotanim.sinister_offering_cast_spotanim"
        const val COOLDOWN_QUEUE = "queue.arceuus_offering_cooldown"
        const val COOLDOWN_TICKS = 9
        const val MAX_OFFERED = 3
        const val XP_MULTIPLIER = 3
        const val SUPERIOR_BONES_ID_ROW = "dbrow.superiordragonbones"
        val SUPERIOR_BONES_ID: Int by lazy { SkillPrayerRow.getRow(SUPERIOR_BONES_ID_ROW).item.id }
        const val INFERNAL_ASH_ROW = "dbrow.infernalashes"
        val DEMONIC_ASH_ROWS =
            listOf(
                "dbrow.infernalashes",
                "dbrow.abyssalashes",
                "dbrow.maliciousashes",
                "dbrow.vileashes",
                "dbrow.fiendishashes",
            )
        val DOUBLE_RESTORE_ROWS =
            listOf(
                "dbrow.ourgbones",
                "dbrow.dagannothbones",
                "dbrow.hydrabones",
                "dbrow.superiordragonbones",
            )
    }
}
