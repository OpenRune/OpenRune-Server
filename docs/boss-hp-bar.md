# Boss HP Bar

How to add a boss HP bar to an NPC, what the different modes mean, and how to call it from Kotlin when you need custom behaviour.

---

## Quick mental model

Setting `param.boss_hp_bar_mode` on an NPC's server params is all you need for the common cases. The bar is shown to the player, updates automatically as the NPC takes damage, and closes when the fight ends. You only need to call the Kotlin API directly if you want to open, close, or update the bar outside of those normal triggers.

---

## Param values

| Value | Constant | When the bar opens |
|-------|----------|--------------------|
| `0` | `NEVER` | Never — the bar is never shown. This is the default when the param is absent. |
| `1` | `ON_ATTACK` | Opens the first time the player deals damage to the NPC. Closes automatically ~10 ticks (6 s) after the player's last attack, or when the NPC dies. Each player sees their own bar independently. |
| `2` | `ON_ENTER` | Opens immediately when the player enters the instance that contains the NPC (if the NPC is alive). Closes when the player leaves the instance or the NPC dies. All occupants of the instance share the same bar update. |

`ON_ATTACK` is right for overworld bosses and anything players can fight without entering a dedicated arena (e.g. Lava Dragon). `ON_ENTER` is right for instanced bosses where the whole party should see the bar the moment they zone in (e.g. Scurrius).

---

## Adding the param to an NPC

Open `.data/raw-cache/server/npcs.toml` and add or edit the entry for your NPC:

```toml
[[npc]]
id = "npc.my_boss"
inherit = "npc.my_boss"

[npc.params]
"param.boss_hp_bar_mode" = 1   # 1 = ON_ATTACK, 2 = ON_ENTER
```

That is the only change needed for the two built-in modes. The bar's HP values are driven by `npc.hitpoints` and `npc.baseHitpointsLvl` automatically.

---

## Custom Kotlin usage

If you need to open, close, or update the bar from a script — for example to drive a multi-phase boss — inject `BossHpBarScript` and call its public methods directly.

```kotlin
class MyBossScript @Inject constructor(
    private val bar: BossHpBarScript,
) : PluginScript() {

    override fun ScriptContext.startup() {
        // open the bar when the player enters a custom trigger
        onEvent<SomeCustomEvent> {
            bar.onOpen(player, npc)
        }

        // update after a phase change (optional — damage events do this automatically)
        onEvent<PhaseChangeEvent> {
            bar.onUpdate(player, npc, currentHp = npc.hitpoints, maxHp = npc.baseHitpointsLvl)
        }

        // close the bar early
        onEvent<ArenaClearedEvent> {
            bar.onClose(player, npc)
        }
    }
}
```

### Method reference

| Method | Description |
|--------|-------------|
| `onOpen(player, npc)` | Displays the bar for `player`, bound to `npc`. Sets NPC ID, current HP, and base HP varps, then runs the client-side open scripts. No-ops if the player has disabled the HUD (`varbit.hpbar_hud_boss_disabled`). |
| `onUpdate(player, npc, currentHp, maxHp)` | Pushes a new HP value to the player's varps. `currentHp` and `maxHp` default to `npc.hitpoints` / `npc.baseHitpointsLvl` if omitted. No-ops if the player has disabled the HUD. |
| `onClose(player, npc)` | Hides the bar with a 2-tick fade, using `ProtectedAccessLauncher`. Safe to call from any context. |

---

## Colours

The bar uses three colours by default, stored in `BossHpBarScript.ORIGINAL_COLORS`:

| Slot | Component | Default colour |
|------|-----------|----------------|
| 0 | `hpbar_hud:inner` | `Color(204, 0, 0)` — deep red |
| 1 | `hpbar_hud:health_bar_back` | `Color(149, 0, 0)` — dark red background |
| 2 | `hpbar_hud:health_bar_sliding` | `Color(0, 245, 0)` — bright green remaining HP |

`onOpen` always resets to these colours. If you want a different colour scheme (e.g. purple for a magic boss), call `player.setColour(component, colour)` immediately after `bar.onOpen(player, npc)` to override them.
