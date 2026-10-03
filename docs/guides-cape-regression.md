# Max cape indices and guide interfaces

The revision 240 client sends zero-based `IfSubOp.subop` values. Resolve these
without subtracting one; the parent inventory/equipment op mapping is unchanged.
Index 3 in the held Teleports submenu is Farming Guild. Index 0 is a valid entry,
and negative/out-of-range submenu indices must not execute an action.

Skill buttons always select the native `skill_guide_v2`, including upgrading the
saved guide preference at login. Its revision 240 initializer needs four integer
arguments: skill guide bit, section, 0, 0. Native tables, models and tabs are retained.

The quest guide packs `interface.quest_guide` with the same native steel border,
stone buttons and metal scrollbar used by the other custom menus. Overview reuses
the native quest requirement renderer and adapts its colours to the dark panel;
its parchment resize timer is removed. Journal uses the registered active/completed
quest log and wraps/scrolls inside the viewport. The map button uses the registered
quest start location. This changes presentation, not quest progression or rewards.

Regression tests cover actual cape packets, every skill button, saved preference,
quest view switching, long logs, completed logs and map location. A cache build is
required together with the server JAR. Verify fixed/resized layout, scrolling and
close buttons in the client after installing the paired software/cache package.
