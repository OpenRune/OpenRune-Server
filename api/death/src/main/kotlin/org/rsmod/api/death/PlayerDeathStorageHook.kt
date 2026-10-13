package org.rsmod.api.death

import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

/**
 * Empties a storage item's contents on death, for the "Always empty ... on death" settings
 * (https://oldschool.runescape.wiki/w/Settings): with one on, or on a PvP death, the contents go
 * with the death's lost items even when the storage item itself is kept. Returns what was taken
 * out, or nothing to leave the contents in the storage item.
 *
 * TODO(content): [system:rune_pouch] `varbit.emptyondeath_runepouch`.
 * TODO(content): [system:essence_pouch] `varbit.emptyondeath_essencepouch`.
 * TODO(content): [system:bolt_pouch] `varbit.emptyondeath_boltpouch`.
 * TODO(content): [system:herb_sack] `varbit.emptyondeath_herbsack`.
 * TODO(content): [system:seed_box] `varbit.emptyondeath_seedbox`.
 */
public fun interface PlayerDeathStorageHook {
    public fun empty(player: Player, isPvpDeath: Boolean): List<InvObj>
}
