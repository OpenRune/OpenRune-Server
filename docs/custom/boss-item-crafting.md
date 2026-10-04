# Corp and Zulrah item assembly

Requested direct inventory recipes:
- Spirit shield + holy elixir -> blessed spirit shield.
- Blessed spirit shield + spectral/arcane/elysian sigil -> matching shield.
These direct recipes add no extra skill/tool requirements or XP.

Zulrah:
- Chisel + serpentine visage -> uncharged serpentine helm (existing 52 Crafting recipe).
- Chisel + tanzanite fang -> empty toxic blowpipe (78 Fletching, 120 base XP).
- Chisel, magic fang and uncharged trident -> uncharged toxic trident (59 Crafting).
  Enhanced and ornamented trident identities are preserved.
- Existing magic fang + staff of the dead recipe remains available with a chisel.
- Magma/tanzanite mutagen + plain serpentine helm -> coloured helm; charge state is
  preserved. Restore returns the plain helm and consumes the cosmetic permanently,
  with an explicit confirmation stating that the mutagen is lost.

Dismantle (confirmed):
- Tanzanite fang, magic fang, serpentine visage, uncharged plain helm and empty
  plain blowpipe each yield 20,000 Zulrah scales.
- Uncharged standard/enhanced/ornamented toxic trident and toxic staff return the original
  base weapon and magic fang. The fang can then be dismantled into scales.
- Empty blazing blowpipe separates into its normal blowpipe and ornament kit.
- Charged weapons use their existing Uncharge route first. Blowpipe returns darts
  and scales; trident returns remaining runes/scales. Helm and toxic staff receive
  scale charging/check/uncharge handlers (11,000 capacity). No charge consumption
  or combat behaviour is changed by this assembly update.

All inventory changes use native atomic transactions. Cancellation, insufficient
space and stale item references preserve the inventory. Mutagens, pets, jars,
teleports and onyx are not converted into scales.

References: OSRS Wiki Toxic blowpipe and Magic fang; revision-240 cache item menus
and existing crafting database recipes. The native menu distinguishes Restore,
Uncharge and Dismantle; these operations retain their distinct meanings.

Validation: 60 crafting and special-weapon tests pass, both module formatting checks pass, and the full server JAR builds. Isolated server startup, bridge request and clean shutdown pass. Merge is authorized by the user; in-game verification of these new recipes remains pending.
