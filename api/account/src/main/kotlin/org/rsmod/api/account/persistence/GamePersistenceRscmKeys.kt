package org.rsmod.api.account.persistence

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType

/**
 * DB stores RSCM string keys (`varp.*`, `inv.*`, `obj.*`). Legacy rows may still have plain
 * integer strings from older schemas; those decode via [RSCM.getReverseMapping].
 *
 * Inventories are keyed in SQL by [`character_id`][org.rsmod.api.account.character.inv.CharacterInventoryData.Inventory.characterId]
 * + [`inv`][org.rsmod.api.account.character.inv.CharacterInventoryData.Inventory.invDbKey] (no composite `id` string).
 */
internal object GamePersistenceRscmKeys {
    fun decodeVarpKey(db: String): Int {
        val t = db.trim()
        if (t.isEmpty()) {
            return -1
        }
        if (isPlainIntKey(t)) {
            return t.toInt()
        }
        return t.asRSCM(RSCMType.VARP)
    }

    fun encodeVarpKey(varpId: Int): String = RSCM.getReverseMapping(RSCMType.VARP, varpId)

    /** Returns the inv map key string (same as [org.rsmod.game.inv.Inventory.internalName]). */
    fun decodeInvTypeKey(db: String): String {
        val t = db.trim()
        if (t.isEmpty()) {
            return t
        }
        if (isPlainIntKey(t)) {
            return RSCM.getReverseMapping(RSCMType.INV, t.toInt())
        }
        return t
    }

    /** RSCM key for [org.rsmod.game.inv.Inventory.internalName]. */
    fun encodeInvTypeKey(internalName: String): String = internalName

    /** String suitable for [org.rsmod.game.inv.InvObj] string constructor. */
    fun decodeObjKey(db: String): String {
        val t = db.trim()
        if (t.isEmpty()) {
            return t
        }
        if (isPlainIntKey(t)) {
            return RSCM.getReverseMapping(RSCMType.OBJ, t.toInt())
        }
        return t
    }

    fun encodeObjKey(objId: Int): String = RSCM.getReverseMapping(RSCMType.OBJ, objId)

    private fun isPlainIntKey(t: String): Boolean {
        if (t == "-1") {
            return true
        }
        val body = if (t.startsWith('-')) t.drop(1) else t
        return body.isNotEmpty() && body.all { it.isDigit() }
    }
}
