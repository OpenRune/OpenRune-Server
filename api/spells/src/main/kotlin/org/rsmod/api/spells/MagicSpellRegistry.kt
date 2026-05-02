package org.rsmod.api.spells

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.aliases.ParamInt
import org.rsmod.api.config.aliases.ParamObj
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.config.refs.objs
import org.rsmod.api.enums.AutocastEnums.autocast_spells
import org.rsmod.api.enums.SpellbookEnums.spellbooks

public class MagicSpellRegistry {
    private lateinit var objSpells: Map<Int, MagicSpell>
    private lateinit var autocastSpells: Map<Int, MagicSpell>

    public fun getObjSpell(obj: ItemServerType): MagicSpell? = objSpells[obj.id]

    public fun getAutocastSpell(autocastId: Int): MagicSpell? = autocastSpells[autocastId]

    public fun combatSpells(): List<MagicSpell> =
        objSpells.values.filter { it.type == MagicSpellType.Combat }

    internal fun init() {
        check(!::objSpells.isInitialized) { "`init` already called for this repository." }

        val objSpells = loadObjSpells()
        this.objSpells = objSpells

        val autocastSpells = loadAutocastSpells(objSpells)
        this.autocastSpells = autocastSpells
    }

    private fun loadObjSpells(): Map<Int, MagicSpell> {
        val spells = hashMapOf<Int, MagicSpell>()

        val spellbookList = spellbooks.filterValuesNotNull()
        for (spellbookEnum in spellbookList.values) {
            val spellList = spellbookEnum.filterValuesNotNull()
            for (spellObj in spellList.values) {
                spells[spellObj.id] = spellObj.toMagicSpell()
            }
        }

        return spells
    }

    private fun loadAutocastSpells(objSpells: Map<Int, MagicSpell>): Map<Int, MagicSpell> {
        val spells = hashMapOf<Int, MagicSpell>()

        val autocastSpells = autocast_spells.filterValuesNotNull()
        for ((autocastId, spellObj) in autocastSpells) {
            val spell = objSpells[spellObj.id]
            checkNotNull(spell) { "Unexpected null spell for obj: $spellObj" }
            spells[autocastId] = spell
        }

        return spells
    }

    private fun ItemServerType.toMagicSpell(): MagicSpell {
        val unpacked = this

        // Some spells can have a default (-1) spellbook, such as `teleport_to_target_spell`.
        val spellbookId = unpacked.param(BaseParams.spell_spellbook)
        val spellbook = Spellbook[spellbookId]

        val spellTypeId = unpacked.param(BaseParams.spell_type)
        val spellType =
            MagicSpellType[spellTypeId]
                ?: error("Invalid MagicSpellType: $spellTypeId (spell=$unpacked)")

        val name = unpacked.param(BaseParams.spell_name)
        val button = unpacked.param(BaseParams.spell_button)
        val maxHit = unpacked.param(BaseParams.spell_maxhit)
        val levelReq = unpacked.param(BaseParams.spell_levelreq)
        val experience = unpacked.paramOrNull(BaseParams.spell_castxp)

        checkNotNull(experience) {
            "Cast xp not defined for spell obj: '${RSCM.getReverseMapping(RSCMType.OBJ, id)}' ($id)"
        }

        val objReqs = buildList {
            fun addRequirement(objParam: ParamObj, countParam: ParamInt) {
                val paramObj = unpacked.paramOrNull(objParam) ?: return
                val obj = paramObj.toRequirementObj()
                val worn = obj.wearpos1.takeIf { it != -1 }
                val count = unpacked.param(countParam)
                check(worn == null || count == 1) {
                    "Count for worn objs expected to be 1: spell=$this, obj=$obj, count=$count"
                }
                this += MagicSpell.ObjRequirement(obj, count, worn)
            }
            addRequirement(BaseParams.spell_runetype_1, BaseParams.spell_runecount_1)
            addRequirement(BaseParams.spell_runetype_2, BaseParams.spell_runecount_2)
            addRequirement(BaseParams.spell_runetype_3, BaseParams.spell_runecount_3)
            addRequirement(BaseParams.spell_runetype_4, BaseParams.spell_runecount_4)
        }

        // For emulation purposes: Magic Dart has a quirk where obj validation order differs.
        // Although the staff appears first in the requirement list, its count is validated last.
        // Runes are always checked before the staff. This behavior appears to be unique to Magic
        // Dart, which is why we manually shift the staff to the end of the requirement list.
        val shiftFirstRequirementToTail = isType(objs.spell_magic_dart) && objReqs.isNotEmpty()

        val sortedObjReqs =
            if (shiftFirstRequirementToTail) {
                objReqs.drop(1) + objReqs.first()
            } else {
                objReqs
            }

        return MagicSpell(
            obj = this,
            name = name,
            component = button,
            spellbook = spellbook,
            type = spellType,
            maxHit = maxHit,
            levelReq = levelReq,
            castXp = experience / 10.0,
            objReqs = sortedObjReqs,
        )
    }

    // Claws of Guthix spell lists a special, non-usable staff obj (likely for visual purposes).
    // Since we use these objs for server-side validation, we replace it with the usable staff obj.
    private fun ItemServerType.toRequirementObj(): ItemServerType =
        if (isType(objs.guthix_staff_rune)) {
            objs.guthix_staff
        } else {
            this
        }
}
