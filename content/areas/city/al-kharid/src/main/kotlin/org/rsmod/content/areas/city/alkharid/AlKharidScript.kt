package org.rsmod.content.areas.city.alkharid

import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Top-level Al-Kharid plugin script (M5.F1).
 *
 * Interactive behaviour lives in dedicated per-NPC + per-loc + generic scripts:
 * - [AlKharidTollGate] — toll-gate Open/Pay, free after Prince Ali Rescue.
 * - [org.rsmod.content.areas.city.alkharid.quests.PrinceAliRescue] — the quest itself (M7.F5).
 * - [org.rsmod.content.areas.city.alkharid.npcs.Ranael] — Ranael's Super Skirt Store.
 * - [org.rsmod.content.areas.city.alkharid.npcs.LouieLegs] — Louie's Armoured Legs Bazaar.
 * - [org.rsmod.content.areas.city.alkharid.npcs.Dommik] — Dommik's Crafting Store.
 * - [org.rsmod.content.areas.city.alkharid.npcs.Zeke] — Zeke's Superior Scimitars.
 * - [org.rsmod.content.areas.city.alkharid.npcs.GemTrader] — Gem Trader shop.
 * - [org.rsmod.content.areas.city.alkharid.npcs.SilkTrader] — Talk-to-only silk haggle (no shop
 *   interface, per OSRS canon).
 * - [org.rsmod.content.areas.city.alkharid.npcs.AliMorrisane] — F2P-portion Ali dialogue.
 * - [org.rsmod.content.areas.city.alkharid.npcs.Karim] — Talk-to-only kebab sale, 1gp each.
 * - [org.rsmod.content.areas.city.alkharid.npcs.AlKharidGeneralStore] — the two general-store
 *   clerks opening `inv.generalshop1`.
 *
 * Everything the pre-sync build expressed as Kotlin config now lives as data:
 * - Bank booths use the canonical generic-loc binding in `generic-locs`; the Al-Kharid bankers are
 *   tagged `content.banker` by an inherit-self stanza in `.data/raw-cache/server/npcs.toml` so the
 *   generic Banker NPC script binds Talk-to / Bank / Collect.
 * - The palace double doors are tagged with the canonical `content.closed_*_door` /
 *   `content.opened_*_door` groups (plus a `param.next_loc_stage` round-trip) in
 *   `.data/raw-cache/server/loc/doors.toml`, so the generic `DoubleDoorScript` binds Open + Close.
 * - Shop stock comes from upstream's `.data/raw-cache/server/shops` toml corpus; each shopkeeper
 *   script simply names the matching `inv.x`. The per-shop sell/buy/change percentages are npc
 *   params in `npcs.toml`.
 * - NPC spawns come from upstream's canonical `.data/raw-cache/map/npcs/kharid_desert_region.toml`
 *   and `draynor.toml`, so this module ships no spawn file of its own.
 */
class AlKharidScript : PluginScript() {
    override fun ScriptContext.startup() {
        // Intentionally empty: interactive behaviour lives in per-NPC + per-loc + generic scripts.
    }
}
