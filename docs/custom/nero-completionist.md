# Nero Completionist integration

The owner requested the v0.7 completionist-cape handoff integration after parking
world shortcuts/containers/fairy rings. The preserved Forestry checkpoint is
`84b002717` on `feature/shortcuts-containers-fairy-rings`. This checkout starts at
accepted Revenant/Home commit `44f15a55f` and retains the fourteen accepted local
source changes separately. No parked container or shortcut rollout is implied.

## Handoff and target qualification

The original handoff ZIP SHA256 is
`8e0d03fa082994a4478c649dc090dba953afc67dd527a9830493200391729065`.
All seven outer-manifest hashes match. Documents describe the prototype and its
limits; the owner's direct request controls implementation. Sources/assets are
retained from the handoff; this is not a universal revision compatibility claim.

The handoff targets the older `workspace/OpenRune-Server` JAR (`fc1b3698...`).
The accepted launcher release uses `release/workspace/OpenRune-Server`, whose
JAR SHA256 is `80fb5e49ee0b910aae2feee58b4e5c0dbbcfeb54a68344ee1fdf75dfcb1b0ebc`.
The original build/checksum guards remain in the original bundle. Integration
requires new qualification against the accepted build, never bypassing guards.

A read-only preflight against that release found exactly one clash among 402
assets: varp 63020 is the existing special-staff protection timer. The 42 style
variables move to 63300-63341, checked against all native cache varps and source
varp declarations, including parked work. All wearable item IDs remain below
63487. Item/model/NPC/interface IDs are unchanged. The remapped 402-asset
preflight passes without modifying the release.

Upstream review: official tree `bbc78dfc259c988a77d5c840196946c72de862e9` has no
max-cape, quiver or completionist implementation. Open/draft and closed PR search
for cape-related work returned unrelated skill/quest/cape-merchant PRs. No
upstream branch was imported. The existing fork's max-cape behavior is the basis.

## Native integration approach

`content/other/nero-completionist` owns the Curator, chatbox purchase/fusion,
regular-only customiser, style persistence/migration and loopback appearance feed.
Its dedicated pack contains the reviewed raw models/widgets/servervars, native
symbol metadata and a cache task for both LIVE and SERVER passes. Each build
derives the fourteen bonuses from the corresponding native max variant (+2,
magic damage raw +20); hoods remain unchanged. Regeneration must be proven.

Curator startup uses the existing game lifecycle owner rather than an unbound
cycle subscription. Purchase/fusion/editor completion guard lifecycle, movement
and stale references. Only Done applies the draft; presets are independently
persistent. Style-feed bind must remain loopback, default normal port 46602.
The handed-off runtime's existing 3*percentage/100 scale and -12 model-Y hem
offset are retained; fixed styles and regular palettes remain isolated.

## Current validated chunk

The bounded native pack path preserves the exact contents and IDs of all
**538,211** existing config definitions after reopening the written caches. It
handles the revision-240 signed varp sentinel explicitly; a Displee rewrite
otherwise moves existing varp file IDs by 32768. The failed candidate was
replaced with an untouched accepted cache copy before qualification.

**23 Curator/cape integration tests + 15 existing max-cape tests pass**. The
actual NPC, held-item, pause-dialog and IF3 input paths cover purchases, all 22
recipes in both directions/full inventory, all 48 Wear options, editor
save/cancel/close/presets/migration, 24 worn-bonus calculations and Dizana
held/ammo/combat/persistence, actual PostgreSQL save/load, variant teleports
and native death/Perdu routes. Native visual acceptance is still separate.

Dizana reuses the reviewed shared ammo implementation from the parked branch;
only the necessary API changes and new-cape handlers are included. Full parked
container rollout remains separate. The extra native equipment UI handler must
be reconciled to one owner when that parked branch is resumed.

The full cache builder was attempted and fails in existing unrelated CS2
symbols. `:content:other:nero-completionist:nero-completionist-pack:packCompletionistAssets`
is the qualified bounded route for an existing accepted cache. Full fresh-cache
regeneration remains a separate open check; its failure is not hidden.

The opt-in client verifies all 34 supplied pinned local libraries, the supplied
runtime artifacts and Java 11. An actual runtime linkage check verifies the
bootstrap bridge and qualified player/NPC/model/attachment/scene hooks without
opening a client. Original revision/API/HD fingerprint guards, particle scale
and hem offset are retained.

## Variant behavior and protection

Held options come from each native max variant; regular/trimmed also retain
the native worn labels/submenus. Fixed forms keep their matching cape behavior.
Ardougne/Mythical teleports use shared teleport validation and collision checks.
Ava variants keep native recovery rates and Commune; regular forms have 72%
recovery. Guild entry and essence-pouch protection apply to all wearable max
variants; full smelting/coal/runecrafting/cooking perks apply to regular forms.
Dizana is blessed with shared extra ammunition and Ava upgrade.

Eight custom protection families have non-wearable broken, locked broken and
locked mangled forms at free IDs 63200-63223. They keep the custom inventory
model but clear bonuses/equipment slots. The revision-240 native cache already
has broken/mangled Infernal and Dizana max forms. These custom families use the
permanent lock/repair lifecycle reviewed in the parked quiver implementation;
this extends that policy to the custom families without replacing old items.
Low PvP loss returns a broken form and repair bounty. Deep unlocked loss yields
569,250 coins; locked deep loss returns a mangled form with 500k bounty/repair.
Broken items cannot consume protection slots or pay another repair bounty.
PvM locked items remain equipped; death previews stay pure. Dizana shared ammo
spills on PvP death even if its cape is protected.

Perdu revalidates selected references and pays atomically from inventory/bank.
Lock: parchment + 500k. Unlock: parchment + 475k refund. Repair keeps the family
and lock state. Locked fusion consumes a locked ingredient, so it creates no
free additional lock. Fees follow the matching max-family repair table:
Fire 150k, other non-Dizana families 99k, Dizana 400k.
References: [death table](https://oldschool.runescape.wiki/w/Death) and
[max exceptions](https://oldschool.runescape.wiki/w/Max_cape). The indexed wiki
is older than this cache and does not prove current mangled-state behavior.

## Validation and integration still to run

Exact-file ktlint hygiene passes, excluding all accepted baseline sources.
Repeated bounded regeneration passes. Native account save/load uses a fresh
temporary PostgreSQL database and the existing repository/applier: saved colors,
presets, Ava toggle and ammo survive; temporary drafts/timers do not. Touched
crafting/cooking/runecrafting modules compile.

The 217MB packaged JAR is built and preserves all nine accepted private Studio
pack entries byte-for-byte. Isolated boot loads 585 scripts, Central and a fresh
PostgreSQL database; the correct revision-240 JS5 handshake and appearance schema
1 both pass. Windows-assigned wildcard-tested TCP ports avoid reserved/busy fixed
ports; the test profile binds its own existing-key gamepack copy to those ports.
The actual Java 11 client initializes, side-loads the supplied particle plugin
and reports software particle/depth readiness. Only that owned client/server
were stopped afterward. No old profiles, RSProx, live cache/RSA/data were edited.

Installer rehearsal on an independent server copy passes Check, Install,
idempotent Install and exact Rollback of all 32 originals. An unknown target
change is refused before replacement. Process guards cover the selected server
and its game port, allowing unrelated clients to remain open. The original
live JAR and all fourteen accepted source hashes still match the baseline.

Package: `outputs/nero-completionist-v07-20261012` in the task workspace.
`TESTEN.cmd` prepares/starts the isolated test server and opt-in client;
`TESTEN.ps1 -Action Stop` stops the owned server. `VALIDATION.json` records the
actual evidence, and `LEESMIJ.md` contains the manual scenarios. The package
installer has not been run against the live installation.

Still open: native in-game visual acceptance of both genders, idle/walk/emote,
color/palette isolation and software/qualified-HD depth. The supplied renderer
still reports `sourceMaterial744=false` and `exact646Simulation=false`: it is
the v0.7 preview implementation, not a newly proven exact native particle engine.
Original render proof is reference evidence; startup does not prove appearance.
The older full-completion eligibility proposal is not part of v0.7 purchase
behavior and awaits an owner decision. Actual live installation/merge awaits
subsequent owner acceptance.
