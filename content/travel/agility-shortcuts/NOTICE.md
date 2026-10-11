Shortcut route candidates were selectively adapted from OpenRune upstream PR227,
commit 98fe5d9a0554b202ccd63492c326dbe65793243a. Its AgilityShortcutTables source
attributes the transport pairs to Skretzo/shortest-path (BSD 2-Clause),
Copyright (c) 2021 Skretzo. The original BSD license is retained in
LICENSE-shortest-path. OpenRune's MIT license remains in the repository root.

The candidate data are audited against native revision-240 options, transforms,
placements, collision and the OSRS Wiki Shortcuts list. Wrong crate/corpse/energy
sphere references are removed, active multi-loc forms are bound to native input,
and full crossing stages replace non-clickable middle stones.

Additional pairs were selectively adapted from Skretzo/shortest-path commit
7e81a40d0312e767fd7833637e57caae80a80253, agility_shortcuts.tsv and transports.tsv.
The same BSD 2-Clause license applies; native and Wiki verification corrects
revision-specific IDs and locations.
