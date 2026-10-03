# OpenRune Fork Development & Upstream Workflow

## Doel

Deze repository is een persoonlijke fork van:

- **Upstream / official:** `OpenRune/OpenRune-Server`
- **Eigen fork:** `EvolvedMind/OpenRune-Server`

De repository moet langdurig onderhoudbaar blijven terwijl:

1. **OpenRune upstream makkelijk gevolgd en geïntegreerd kan worden.**
2. **Eigen werk messcherp gescheiden, traceerbaar en debugbaar blijft.**
3. **`main` altijd een bekende, stabiele staat vertegenwoordigt.**
4. **Experimenten, tijdelijke Codex-branches en oude tests niet blijven opstapelen.**
5. **Bij regressies snel zichtbaar is welke feature, fix of upstream-update verantwoordelijk is.**

Dit document is leidend voor toekomstige Git- en repositorywerkzaamheden.

---

# 1. Kernprincipes

## 1.1 `main` is de stabiele server

Werk niet rechtstreeks langdurig op `main`.

`main` bevat alleen:

- geteste eigen features;
- geteste fixes;
- gecontroleerde upstream-syncs;
- documentatie;
- bekende werkende configuratie.

Experimentele of onafgewerkte code hoort niet permanent op `main`.

---

## 1.2 Branches zijn tijdelijk

Branches zijn **geen archiefmappen**.

Gebruik branches voor actief werk en verwijder ze nadat het werk correct in `main` zit.

Gewenste toestand:

```text
main
feature/doom
```

Of tijdens meerdere actieve werkzaamheden:

```text
main
feature/doom
fix/pet-relog
```

Niet gewenst:

```text
main
17 oude feature/test/fix/codex branches
```

Commitgeschiedenis en tags bewaren het verleden. Oude branches hoeven niet permanent te blijven bestaan.

---

## 1.3 Eigen werk moet herkenbaar zijn

Iedere logische wijziging krijgt een duidelijke commit.

Gebruik bij voorkeur Conventional Commits:

```text
feat(zulrah): add encounter skeleton
feat(zulrah): add four combat rotations
feat(pets): add follower teleport handling
feat(commands): add boss debug commands
fix(pets): prevent follower duplication
fix(zulrah): preserve phase state after transition
docs(custom): document Zulrah architecture
chore(repo): clean stale development branches
merge(upstream): sync OpenRune revision 241
```

Vermijd commits zoals:

```text
update
stuff
fix things
working version
changes
more fixes
```

---

## 1.4 Eén commit = één logisch doel

Een commit moet zelfstandig te begrijpen zijn.

Goed:

```text
feat(doom): add encounter skeleton
feat(doom): add phase transitions
feat(doom): add projectile attack
```

Slecht:

```text
add doom + pets + commands + misc fixes
```

Dit maakt `git bisect`, `git show`, `git revert` en debugging veel eenvoudiger.

---

# 2. Eerste taak: huidige branches onderzoeken en opruimen

Voordat nieuwe repositorystructuur wordt toegevoegd, moeten bestaande branches worden onderzocht.

Op dit moment bestaan meerdere oude branches, waaronder feature-, fix-, test-, fresh- en codex-branches.

## Belangrijke regel

**Verwijder geen branch voordat is vastgesteld dat er geen waardevol uniek werk verloren gaat.**

Een branch die "ahead" staat ten opzichte van `main` bevat mogelijk nog unieke commits.

Dat betekent niet automatisch dat de code ontbreekt op `main`; dezelfde wijziging kan later anders gecommit zijn. Daarom moet inhoud worden vergeleken.

---

## 2.1 Inventariseer alle branches

Gebruik bijvoorbeeld:

```bash
git fetch --all --prune
git branch -a
```

Controleer voor elke remote branch:

```bash
git log --oneline main..origin/<branch>
git diff --stat main...origin/<branch>
git diff main...origin/<branch>
```

Waar nuttig:

```bash
git cherry main origin/<branch>
```

---

## 2.2 Classificeer iedere branch

Iedere branch moet in exact één van deze categorieën terechtkomen:

### SAFE TO DELETE

Branch bevat:

- tijdelijke experimenten;
- probes;
- oude tests;
- werk dat volledig is vervangen;
- commits waarvan functioneel gelijkwaardige code al in `main` staat;
- mislukte tussenstappen zonder blijvende waarde.

Deze mag na verificatie worden verwijderd.

---

### REVIEW FIRST

Branch bevat:

- unieke commits;
- code die mogelijk nog niet in `main` zit;
- alternatieve implementaties;
- fixes waarvan niet duidelijk is of ze later opnieuw zijn toegepast.

Voor deze branches:

1. vergelijk code met `main`;
2. bepaal welke stukken waardevol zijn;
3. neem alleen gewenste wijzigingen over;
4. maak daarvoor nieuwe, nette commits indien nodig;
5. verwijder daarna de oude branch.

Gebruik waar passend:

```bash
git cherry-pick <commit>
```

Of neem handmatig alleen de gewenste verandering over wanneer een oude commit te veel ongewenste wijzigingen bevat.

---

### KEEP TEMPORARILY

Alleen voor branches waarop daadwerkelijk nog actief wordt gewerkt.

Voorbeelden:

```text
feature/doom
fix/pet-relog
```

Ook deze worden na afronding gemerged en verwijderd.

---

## 2.3 Oude geschiedenis NIET herschrijven alleen voor cosmetiek

Voer geen grote interactieve rebase uit op bestaande gedeelde geschiedenis puur om oude commitnamen mooier te maken.

Dus standaard niet:

```bash
git rebase -i ...
git push --force
```

tenzij daar een zeer specifieke technische reden voor bestaat.

Historische rommel mag in het verleden blijven bestaan.

Het doel is:

> **Vanaf nu een nette, consistente geschiedenis bouwen.**

---

# 3. Gewenste repositorystructuur

Voeg na de branch-audit duidelijke documentatie toe.

Aanbevolen structuur:

```text
CUSTOM_CONTENT.md

docs/
└── custom/
    ├── README.md
    ├── architecture.md
    ├── core-modifications.md
    ├── zulrah.md
    ├── pets.md
    ├── commands.md
    └── doom.md
```

Niet ieder leeg toekomstbestand hoeft direct te bestaan. Voeg documenten toe zodra het systeem bestaat of actief wordt ontwikkeld.

---

# 4. `CUSTOM_CONTENT.md`

Dit bestand is het centrale overzicht van alles dat specifiek voor deze fork is toegevoegd.

Voorbeeldstructuur:

```markdown
# EvolvedMind Custom Content

This fork tracks OpenRune upstream while adding custom server content.

## Upstream

Official repository:

OpenRune/OpenRune-Server

Custom repository:

EvolvedMind/OpenRune-Server

## Custom systems

| System | Status | Origin | Documentation |
|---|---|---|---|
| Zulrah | Working | EvolvedMind custom | docs/custom/zulrah.md |
| Pet followers | Working | EvolvedMind custom | docs/custom/pets.md |
| Personal/debug commands | Working | EvolvedMind custom | docs/custom/commands.md |
| Doom | Planned / WIP | EvolvedMind custom | docs/custom/doom.md |

## Repository rules

- `main` should remain stable.
- New development happens on short-lived feature/fix branches.
- One logical change per commit.
- Prefer native OpenRune APIs.
- Avoid modifying OpenRune core unless necessary.
- Every custom core modification must be documented.
- Test custom systems after every upstream merge.
- Upstream must never silently overwrite custom functionality.
```

Actual content must reflect the real repository state, not assumptions.

---

# 5. Documenteer ieder eigen systeem

Voor ieder belangrijk custom systeem komt een eigen document.

Voorbeeld:

```markdown
# Zulrah

## Status

Working

## Origin

EvolvedMind custom implementation.

## Purpose

Implements the Zulrah boss encounter.

## Main files

- `content/bosses/zulrah/...`
- ...

## Dependencies

- Boss DSL
- Instance system
- NPC death / drops
- Kill count
- Collection log
- Venom
- Timers

## Commands

- `::zulrah`
- ...

## Upstream sensitivity

Changes to the following upstream systems may require retesting or migration:

- Boss DSL
- Instance API
- NPC combat
- Drop handling

## Important commits

- `<sha>` `feat(zulrah): ...`
- `<sha>` `feat(zulrah): ...`

## Test checklist

- arena entry
- all rotations
- protection prayers
- venom clouds
- snakelings
- death
- loot
- kill count
- collection log
- instance cleanup
```

Het document hoeft geen roman te worden.

Het moet vooral beantwoorden:

> Wat is dit?

> Waar staat het?

> Waar hangt het vanaf?

> Welke upstream-wijzigingen kunnen het breken?

> Hoe test ik het?

---

# 6. Core modifications apart bijhouden

Eigen wijzigingen in generieke OpenRune/core-code zijn risicovoller dan losse contentmodules.

Houd daarom bij:

```text
docs/custom/core-modifications.md
```

Voor iedere custom core-wijziging:

```markdown
## `path/to/File.kt`

### Reason

Waarom moest deze OpenRune core-file worden aangepast?

### Custom behaviour

Wat verandert er functioneel?

### Related system

Bijvoorbeeld:

- pets
- Zulrah
- commands

### Introduced in

Commit:

`<sha>`

### Upstream risk

Wat kan conflicteren tijdens een toekomstige upstream update?

### Removal path

Kan deze wijziging later verdwijnen wanneer OpenRune zelf een geschikte API/hook krijgt?
```

Doel:

Als over zes maanden een core-file conflicteert, moet direct zichtbaar zijn **waarom wij hem ooit gewijzigd hebben**.

---

# 7. Branch naming

Gebruik voorspelbare namen.

## Features

```text
feature/doom
feature/custom-raids
feature/pet-followers
```

## Fixes

```text
fix/pet-relog
fix/zulrah-transition
fix/commands-menu
```

## Onderzoek / experiment

Alleen indien echt nodig:

```text
experiment/new-instance-api
```

Experimentbranches worden na conclusie verwijderd.

Vermijd permanent groeiende namen zoals:

```text
fresh/...
codex/...
test/...
feature-v2-final-new-final2
```

Codex mag zelf tijdelijke branches maken, maar moet ze opruimen wanneer het werk klaar is.

---

# 8. Normale feature-workflow

Start altijd vanaf actuele `main`.

```bash
git switch main
git pull
git status
```

Maak daarna een featurebranch:

```bash
git switch -c feature/doom
```

Werk in kleine logische stappen.

Bijvoorbeeld:

```bash
git add <relevante bestanden>
git commit -m "feat(doom): add encounter skeleton"
```

Daarna:

```bash
git add <relevante bestanden>
git commit -m "feat(doom): add phase transitions"
```

Daarna:

```bash
git add <relevante bestanden>
git commit -m "feat(doom): add projectile attack"
```

Test de branch volledig.

Pas daarna mergen naar `main`.

Na succesvolle merge:

```bash
git branch -d feature/doom
git push origin --delete feature/doom
```

Alleen verwijderen wanneer de feature veilig in `main` zit.

---

# 9. Upstream OpenRune blijven volgen

Configureer remotes duidelijk:

```bash
git remote -v
```

Gewenst:

```text
origin    https://github.com/EvolvedMind/OpenRune-Server.git
upstream  https://github.com/OpenRune/OpenRune-Server.git
```

Als `upstream` ontbreekt:

```bash
git remote add upstream https://github.com/OpenRune/OpenRune-Server.git
```

---

# 10. Veilige upstream-sync

## 10.1 Nooit blind resetten naar upstream

Niet gebruiken voor normale updates:

```bash
git reset --hard upstream/main
```

Dat kan custom werk uit de actieve geschiedenis verwijderen.

Ook geen force push naar `main` als normale sync-strategie.

---

## 10.2 Eerst actuele status controleren

```bash
git switch main
git pull
git status
git fetch upstream
```

Controleer wat upstream toevoegt:

```bash
git log --oneline main..upstream/main
git diff --stat main...upstream/main
```

Waar nuttig:

```bash
git diff main...upstream/main
```

---

## 10.3 Maak vóór belangrijke upstream-sync een safety tag

Bijvoorbeeld:

```bash
git tag pre-upstream-2026-10-02
git push origin pre-upstream-2026-10-02
```

Of wanneer dit een bekende stabiele serverbuild is:

```bash
git tag server-v0.4-stable
git push origin server-v0.4-stable
```

---

## 10.4 Merge upstream in onze `main`

Gebruik een echte merge wanneer de histories zijn gedivergeerd:

```bash
git merge upstream/main
```

Los conflicts bewust op.

Belangrijk:

> Bij een conflict wint niet automatisch upstream en ook niet automatisch onze versie.

Onderzoek:

1. Wat heeft upstream veranderd?
2. Waarom bestaat onze custom wijziging?
3. Kunnen beide gecombineerd worden?
4. Is onze wijziging inmiddels overbodig door een nieuwe upstream API?
5. Welke tests moeten opnieuw worden uitgevoerd?

---

## 10.5 Upstream merge herkenbaar houden

Gebruik een herkenbare mergeboodschap wanneer mogelijk:

```text
merge(upstream): sync OpenRune revision 241
```

Of:

```text
merge(upstream): sync OpenRune 2026-10-02
```

Daardoor is in de geschiedenis duidelijk waar upstream-updates binnenkwamen.

---

# 11. Na iedere upstream-sync

Controleer minimaal:

```bash
git status
```

Daarna build/tests uitvoeren volgens de projectinstructies.

Test bovendien expliciet custom systemen die afhankelijk zijn van gewijzigde upstream-componenten.

Voorbeeld:

Als upstream `Boss DSL` wijzigt:

- Zulrah opnieuw testen;
- Doom opnieuw testen indien die Boss DSL gebruikt;
- andere custom bosses testen.

Als upstream instance-code wijzigt:

- Zulrah entry/exit;
- respawn;
- logout/relog;
- instance cleanup.

Als upstream pet-code wijzigt:

- follower spawn;
- relog;
- teleport;
- metamorphosis;
- despawn.

---

# 12. Upstream sensitivity

Custom documentatie moet aangeven van welke upstream subsystemen een feature afhankelijk is.

Voorbeelden:

```text
Zulrah
├── Boss DSL
├── instances
├── NPC combat
├── venom
├── drop tables
└── kill timers

Pets
├── NPC lifecycle
├── player login/logout
├── teleport handling
└── persistence
```

Wanneer upstream een van die subsystemen wijzigt, weten we meteen welke custom modules extra aandacht nodig hebben.

---

# 13. Debuggingstrategie

De commitgeschiedenis moet debugging ondersteunen.

Gebruik:

```bash
git log --oneline
```

Om één commit te bekijken:

```bash
git show <sha>
```

Om één commit met zijn parent te vergelijken:

```bash
git diff <sha>^ <sha>
```

Om te bepalen wanneer een regressie ontstond kan `git bisect` worden gebruikt.

Voorbeeld:

```bash
git bisect start
git bisect bad
git bisect good <bekende-goede-tag-of-commit>
```

Test iedere geselecteerde commit en markeer:

```bash
git bisect good
```

of:

```bash
git bisect bad
```

Na afloop:

```bash
git bisect reset
```

Dit werkt alleen goed wanneer commits klein en logisch zijn.

---

# 14. Stable tags

Gebruik tags voor bekende goede builds.

Voorbeelden:

```text
server-v0.1-stable
server-v0.2-zulrah
server-v0.3-pets
server-v0.4-doom
```

Tag alleen een toestand die daadwerkelijk als herstelpunt kan dienen.

Bijvoorbeeld:

```bash
git tag server-v0.3-pets
git push origin server-v0.3-pets
```

Zo kan altijd een bekende werkende toestand worden teruggevonden.

---

# 15. Geen onnodige custom core-wijzigingen

Voorkeur:

```text
custom content
    ↓
bestaande OpenRune API
```

Niet:

```text
custom content
    ↓
massale wijzigingen aan OpenRune core
```

Pas core alleen aan wanneer noodzakelijk.

Als upstream later een native oplossing aanbiedt:

1. vergelijk upstream oplossing met onze custom oplossing;
2. migreer waar verstandig;
3. verwijder overbodige custom core-code;
4. documenteer de migratie in een commit.

---

# 16. Als upstream dezelfde feature toevoegt als wij

Niet automatisch onze code verwijderen.

Bijvoorbeeld wanneer upstream later zelf Zulrah, Doom of een vergelijkbare module toevoegt.

Vergelijk:

- correctness;
- volledigheid;
- native integratie;
- performance;
- onderhoudbaarheid;
- test coverage;
- compatibiliteit met de nieuwste OpenRune APIs.

Daarna kan de beste technische onderdelencombinatie worden gekozen.

Belangrijk:

De beslissing moet technisch onderbouwd zijn, niet alleen gebaseerd op "upstream is official".

Documenteer zo'n migratie duidelijk.

Bijvoorbeeld:

```text
refactor(zulrah): migrate encounter to upstream Boss DSL timers
```

of:

```text
refactor(pets): replace custom lifecycle hook with upstream API
```

---

# 17. Codex-regels voor repositorywerk

Wanneer Codex in deze repository werkt:

## Codex moet

- eerst huidige repo-status inspecteren;
- bestaande implementaties lezen voordat nieuwe code wordt toegevoegd;
- native OpenRune APIs verkiezen boven duplicatie;
- kleine logische commits maken;
- relevante tests uitvoeren;
- nieuwe custom code documenteren;
- core-modificaties expliciet registreren;
- tijdelijke branches opruimen na succesvolle merge;
- na upstream-sync controleren welke custom systemen risico lopen;
- nooit aannemen dat een oude branch waardeloos is zonder vergelijking.

## Codex mag niet

- `main` hard resetten naar upstream;
- custom werk stilzwijgend vervangen;
- force-pushen naar `main` voor normale onderhoudstaken;
- tientallen tijdelijke branches laten staan;
- grote ongerelateerde veranderingen in één commit stoppen;
- bestaande code herschrijven alleen omdat een andere stijl mooier lijkt;
- oude branches verwijderen zonder unieke commits/code te controleren;
- automatisch upstream boven custom code verkiezen bij conflicts.

---

# 18. Aanbevolen workflow voor Codex bij iedere nieuwe sessie

Start met:

```text
1. Inspect repository state.
2. Inspect current branch.
3. Fetch origin/upstream where appropriate.
4. Determine whether working tree is clean.
5. Read CUSTOM_CONTENT.md.
6. Read relevant docs/custom/*.md.
7. Inspect recent commits touching the target system.
8. Create/reuse exactly one appropriate working branch.
9. Implement in logical slices.
10. Test.
11. Review diff.
12. Commit with clear scoped messages.
13. Update documentation when architecture/behaviour changed.
14. Merge only when stable.
15. Clean obsolete development branches.
```

---

# 19. Acceptance criteria voor de repository cleanup

De eerste cleanup is pas klaar wanneer:

- [ ] Iedere bestaande branch onderzocht is.
- [ ] Iedere branch is geclassificeerd als `SAFE TO DELETE`, `REVIEW FIRST` of `KEEP TEMPORARILY`.
- [ ] Waardevolle unieke code uit oude branches veilig is behouden.
- [ ] Overbodige branches verwijderd zijn.
- [ ] `main` niet beschadigd of herschreven is.
- [ ] `CUSTOM_CONTENT.md` bestaat.
- [ ] `docs/custom/README.md` bestaat.
- [ ] Actuele custom systemen gedocumenteerd zijn.
- [ ] Custom core-modificaties gedocumenteerd zijn.
- [ ] Commit-conventie vanaf nu consequent wordt gebruikt.
- [ ] Upstream remote correct is ingesteld.
- [ ] Upstream-syncprocedure is vastgelegd.
- [ ] Minstens één bekende stabiele tag bestaat zodra een betrouwbare build is bevestigd.
- [ ] De repository na cleanup overzichtelijk genoeg is dat iedere overblijvende branch een actuele reden heeft om te bestaan.

---

# 20. Belangrijkste einddoel

De repository moet uiteindelijk zo leesbaar worden dat iemand met alleen Git en de documentatie kan beantwoorden:

```text
Wat is official OpenRune?
Wat hebben wij zelf toegevoegd?
Waarom hebben wij dit toegevoegd?
Welke bestanden horen bij die feature?
Welke commit introduceerde het?
Welke upstream subsystemen kunnen het breken?
Hoe test ik het?
Welke build was voor het laatst stabiel?
```

Als deze vragen snel te beantwoorden zijn, is de repository correct georganiseerd.

---

# 21. Richtlijn voor toekomstige keuzes

Bij twijfel geldt:

> **Maak eigen functionaliteit modulair, houd de geschiedenis klein en begrijpelijk, laat upstream via gecontroleerde merges binnenkomen, documenteer uitzonderingen en behandel `main` als een productiebare serverstaat.**

Het uiteindelijke doel is niet alleen dat de server vandaag werkt.

Het doel is dat de server over maanden of jaren nog steeds begrijpelijk, uitbreidbaar, upgradebaar en herstelbaar is.
---

# 22. Belangrijke regel: overlap met nieuwe upstream content

Een upstream merge mag **nooit automatisch bepalen welke implementatie behouden blijft** wanneer upstream een feature toevoegt die in deze fork al custom bestaat.

Voorbeelden:

- upstream voegt Zulrah toe terwijl deze fork al een eigen Zulrah heeft;
- upstream voegt Doom of Mokhaiotl toe terwijl deze fork al een eigen Doom heeft;
- upstream voegt een pet-mechanic toe die hier custom is geïmplementeerd;
- upstream voegt commands, instances, drops, raids, interfaces of andere systemen toe die hier al bestaan.

In zo'n geval geldt een verplichte:

# FEATURE EQUIVALENCE REVIEW

De upstream-versie wordt eerst apart onderzocht voordat deze functioneel in onze custom implementatie wordt opgenomen.

---

## 22.1 Overlap detecteren vóór de merge

Bij iedere upstream-sync moet Codex controleren welke gewijzigde/toegevoegde modules overlappen met custom content.

Vergelijk minimaal:

```text
upstream changed files
        ↓
CUSTOM_CONTENT.md
        ↓
docs/custom/*.md
        ↓
CUSTOM_PROGRESS.md / roadmap
```

Voorbeelden van overlap:

```text
upstream/content/bosses/zulrah
                  ↕
fork/content/bosses/zulrah
```

of:

```text
upstream modifies api/bosses
                  ↕
custom Zulrah depends on api/bosses
```

De tweede vorm is óók relevant: zelfs als upstream geen tweede Zulrah-map toevoegt, kan een gewijzigde Boss DSL jouw implementatie beïnvloeden.

---

## 22.2 Bij directe feature-overlap: niet blind mergen

Wanneer upstream dezelfde gameplay-feature bevat als onze fork:

```text
STOP FEATURE AUTO-ADOPTION
```

De Git-merge mag technisch worden voorbereid, maar de feature zelf moet inhoudelijk worden vergeleken.

Maak een vergelijking van:

| Onderdeel | Custom | Upstream | Actie |
|---|---|---|---|
| Encounter logic | ... | ... | keep / adopt / combine |
| Mechanics | ... | ... | keep / adopt / combine |
| Boss DSL usage | ... | ... | keep / migrate |
| Instances | ... | ... | keep / adopt |
| Drops | ... | ... | compare |
| Kill count | ... | ... | compare |
| Collection log | ... | ... | compare |
| Tests | ... | ... | combine |
| Performance | ... | ... | measure |
| Maintainability | ... | ... | assess |
| Native integration | ... | ... | assess |

Niet alleen LOC of "official" status vergelijken.

---

## 22.3 Mogelijke uitkomsten van een overlap-review

Er zijn vijf geldige uitkomsten.

### A. CUSTOM WINS

Onze implementatie is technisch geschikter.

Dan:

- custom implementatie behouden;
- nuttige upstream fixes eventueel handmatig integreren;
- upstream duplicaat niet actief naast onze implementatie laten draaien;
- documenteren waarom custom is behouden.

Voorbeeld commit:

```text
merge(zulrah): retain custom encounter after upstream comparison
```

---

### B. UPSTREAM WINS

Upstream is duidelijk beter geïntegreerd of onderhoudbaarder.

Dan:

- eerst alle custom-only functionaliteit inventariseren;
- migreren wat nog waardevol is;
- custom implementatie gecontroleerd verwijderen;
- documentatie aanpassen;
- tests behouden of migreren waar mogelijk.

Voorbeeld:

```text
refactor(zulrah): migrate custom encounter to upstream implementation
```

---

### C. HYBRID

Beide implementaties bevatten sterke onderdelen.

Bijvoorbeeld:

```text
upstream:
+ betere Boss DSL integration
+ betere instance lifecycle

custom:
+ completere rotations
+ betere debug commands
+ uitgebreidere tests
```

Dan bouwen we één combinatie:

```text
upstream architecture
+
custom mechanics
+
custom tests
=
hybrid implementation
```

Dit is vaak de beste uitkomst.

Commit bijvoorbeeld:

```text
refactor(zulrah): combine upstream lifecycle with custom encounter mechanics
```

---

### D. DEFERRED REVIEW

Upstream heeft dezelfde feature toegevoegd, maar vergelijking kan nog niet betrouwbaar worden afgerond.

Dan markeren:

```text
⚠ NEEDS REVIEW
```

en de feature niet stilzwijgend vervangen.

Documenteer:

- upstream commit/PR;
- onze huidige implementatie;
- waarom review nog openstaat;
- wat nog getest moet worden.

---

### E. PARALLEL EXPERIMENT

Alleen tijdelijk toegestaan wanneer beide implementaties getest moeten worden.

Bijvoorbeeld op:

```text
experiment/zulrah-upstream-comparison
```

Nooit langdurig twee actieve productie-implementaties voor hetzelfde contentonderdeel in `main` laten bestaan.

---

# 23. Verplichte vergelijking bij upstream overlap

Codex moet voor overlappende features minimaal beoordelen:

## Functionaliteit

- Welke mechanics zijn geïmplementeerd?
- Welke ontbreken?
- Zijn attack rotations correct?
- Zijn special attacks correct?
- Zijn edge cases afgedekt?
- Werken deaths, resets, logout/relog en instance cleanup?

## Architectuur

- Gebruikt de implementatie native OpenRune APIs?
- Zijn er onnodige core modifications?
- Is het modulair?
- Zijn responsibilities goed gescheiden?
- Is toekomstige upstream-sync waarschijnlijk eenvoudiger?

## Correctheid

- Komt gedrag overeen met OSRS waar dat het doel is?
- Zijn timings correct?
- Zijn coordinates/IDs/configs correct?
- Zijn drops correct?
- Zijn states correct?

## Tests

- Welke unit/integration tests bestaan?
- Welke regressietests bestaan?
- Welke handmatige scenario's zijn gecontroleerd?

## Performance

- Onnodige tick work?
- Onbegrensde state?
- Onnodige allocations?
- Slechte scans/loops?
- Instance leaks?

## Onderhoudbaarheid

- Leesbaarheid;
- code duplication;
- afhankelijkheden;
- complexiteit;
- debugbaarheid.

## Custom value

- Heeft onze versie extra commands?
- developer tooling?
- logging?
- metrics?
- extra safety?
- betere documentation?

Pas na deze review wordt besloten welke delen worden behouden.

---

# 24. Merge policy voor overlappende content

Git-conflicts en feature-conflicts zijn niet hetzelfde.

Een Git-merge kan technisch zonder conflict slagen terwijl er functioneel twee concurrerende implementaties ontstaan.

Daarom geldt:

```text
NO GIT CONFLICT
≠
NO FUNCTIONAL CONFLICT
```

Codex moet dus na iedere upstream-sync ook semantische overlap zoeken.

Voorbeeld:

```text
upstream adds:
content/bosses/zulrah2/

fork already has:
content/bosses/zulrah/
```

Git kan dit probleemloos samenvoegen.

Maar functioneel hebben we dan mogelijk twee Zulrah-systemen.

Dat moet worden gedetecteerd en beoordeeld.

---

# 25. Eigen progress- en roadmapbestand

Naast `CUSTOM_CONTENT.md` moet deze fork een eigen voortgangsoverzicht hebben.

Aanbevolen:

```text
CUSTOM_PROGRESS.md
```

Dit bestand is geïnspireerd op OpenRune's `PROGRESS.md`, maar bevat meer informatie die relevant is voor deze fork.

De official `PROGRESS.md` detecteert grotendeels of content **bestaat**. Dat is nuttig, maar "added" betekent niet automatisch "compleet", "correct", "getest" of "productieklaar".

Onze voortgang moet daarom meerdere statussen ondersteunen.

---

# 26. Statusmodel voor `CUSTOM_PROGRESS.md`

Gebruik consequent:

```text
⚪ NOT STARTED
🟣 PLANNED
🔵 IN PROGRESS
🟡 IMPLEMENTED / NEEDS TESTING
🟢 VERIFIED
🟠 NEEDS REVIEW
🔴 BLOCKED
⚫ NOT PLANNED
```

Optioneel voor migraties:

```text
🔁 UPSTREAM REVIEW
♻ HYBRID / MIGRATING
```

Betekenis:

### ⚪ NOT STARTED

Nog geen werk aanwezig.

### 🟣 PLANNED

Staat bewust op de roadmap.

### 🔵 IN PROGRESS

Er wordt actief aan gewerkt.

Vermeld actieve branch.

### 🟡 IMPLEMENTED / NEEDS TESTING

Code bestaat, maar is nog niet voldoende getest om als stabiel te gelden.

### 🟢 VERIFIED

Geïmplementeerd en volgens afgesproken testcriteria gecontroleerd.

### 🟠 NEEDS REVIEW

Bestaande implementatie vereist inhoudelijke beoordeling.

Bijvoorbeeld na upstream overlap.

### 🔴 BLOCKED

Kan momenteel niet verder.

Vermeld blocker.

### ⚫ NOT PLANNED

Bewust buiten scope.

### 🔁 UPSTREAM REVIEW

Upstream heeft dezelfde of sterk overlappende functionaliteit toegevoegd.

Custom en upstream moeten eerst worden vergeleken.

### ♻ HYBRID / MIGRATING

Actieve migratie/combinatie tussen custom en upstream implementaties.

---

# 27. Herkomst per feature bijhouden

Iedere belangrijke feature krijgt een `Origin`.

Gebruik:

```text
UPSTREAM
CUSTOM
HYBRID
```

En eventueel:

```text
UPSTREAM + CUSTOM EXTENSIONS
```

Voorbeeld:

| Feature | Status | Origin | Branch | Notes |
|---|---|---|---|---|
| Zulrah | 🟢 VERIFIED | CUSTOM | — | Four rotations, custom debug commands |
| Pets | 🟢 VERIFIED | UPSTREAM + CUSTOM EXTENSIONS | — | custom follower fixes |
| Doom of Mokhaiotl | 🔵 IN PROGRESS | CUSTOM | `feature/doom` | encounter skeleton |
| Vorkath | 🟣 PLANNED | — | — | roadmap |
| Leviathan | 🟢 VERIFIED | UPSTREAM | — | official implementation |
| Example boss | 🔁 UPSTREAM REVIEW | CUSTOM + UPSTREAM | `review/example-boss` | compare implementations |

Hierdoor is in één oogopslag duidelijk:

```text
wat bestaat
wat van upstream komt
wat van ons komt
wat actief wordt gebouwd
wat nog gepland staat
wat review nodig heeft
```

---

# 28. Aanbevolen opbouw van `CUSTOM_PROGRESS.md`

```markdown
# EvolvedMind OpenRune Progress

## Summary

- Verified:
- Implemented / testing:
- In progress:
- Planned:
- Upstream reviews:
- Blocked:

## Active development

| Feature | Status | Branch | Next step |
|---|---|---|---|
| Doom of Mokhaiotl | 🔵 IN PROGRESS | feature/doom | implement phase 2 |

## Bosses

| Feature | Status | Origin | Quality | Notes |
|---|---|---|---|---|
| Zulrah | 🟢 VERIFIED | CUSTOM | complete | four rotations |
| Leviathan | 🟢 VERIFIED | UPSTREAM | upstream | |
| Vorkath | 🟣 PLANNED | — | — | roadmap |
| Doom of Mokhaiotl | 🔵 IN PROGRESS | CUSTOM | WIP | |

## Skills

...

## Raids

...

## Minigames

...

## Systems

| System | Status | Origin | Notes |
|---|---|---|---|
| Pet followers | 🟢 VERIFIED | HYBRID | upstream base + custom fixes |
| Commands | 🟢 VERIFIED | CUSTOM | personal/debug commands |
| Instances | 🟢 VERIFIED | UPSTREAM | custom consumers |
| Collection log | ... | ... | ... |

## Roadmap

### Now

- [ ] Doom encounter
- [ ] ...

### Next

- [ ] ...
- [ ] ...

### Later

- [ ] ...
- [ ] ...

## Upstream review queue

- [ ] Compare custom Zulrah with upstream Zulrah if/when upstream adds it.
- [ ] ...
```

---

# 29. `CUSTOM_PROGRESS.md` versus `CUSTOM_CONTENT.md`

Deze bestanden hebben verschillende doelen.

## `CUSTOM_CONTENT.md`

Beantwoordt:

```text
Welke custom systemen hebben wij?
Waar staan ze?
Waarom bestaan ze?
Welke documentatie hoort erbij?
```

Dit is vooral architectuur/inventaris.

## `CUSTOM_PROGRESS.md`

Beantwoordt:

```text
Wat is klaar?
Wat wordt gebouwd?
Wat staat gepland?
Wat ontbreekt?
Wat komt uit upstream?
Wat moet met upstream worden vergeleken?
```

Dit is vooral voortgang/roadmap.

Beide blijven bestaan.

---

# 30. Synchronisatie met official `PROGRESS.md`

Bij upstream-sync mag de official `PROGRESS.md` worden gebruikt als informatiebron.

Maar onze `CUSTOM_PROGRESS.md` is leidend voor de toestand van onze fork.

Bijvoorbeeld:

Official:

```text
Zulrah 🔴 not added
```

Onze fork:

```text
Zulrah 🟢 VERIFIED | CUSTOM
```

Dan blijft onze status groen.

Wanneer official later wordt:

```text
Zulrah 🟢 added
```

dan verandert onze status niet automatisch naar upstream.

In plaats daarvan:

```text
Zulrah 🔁 UPSTREAM REVIEW | CUSTOM + UPSTREAM
```

Daarna volgt de feature-equivalence review.

Na beslissing wordt het bijvoorbeeld:

```text
Zulrah 🟢 VERIFIED | CUSTOM
```

of:

```text
Zulrah 🟢 VERIFIED | UPSTREAM + CUSTOM EXTENSIONS
```

of:

```text
Zulrah 🟢 VERIFIED | HYBRID
```

Dit mechanisme voorkomt dat custom content stilzwijgend verdwijnt.

---

# 31. Automatisch detecteren van nieuwe overlap

Op termijn mag tooling worden toegevoegd die bij een upstream-sync:

1. upstream commits sinds laatste sync ophaalt;
2. gewijzigde `content/*` modules detecteert;
3. deze vergelijkt met `CUSTOM_CONTENT.md`;
4. `CUSTOM_PROGRESS.md` controleert;
5. mogelijke overlap rapporteert;
6. **geen automatische feature-keuze maakt**.

Voorbeeldrapport:

```text
UPSTREAM SYNC REVIEW

New upstream content:
- content/bosses/zulrah
- content/bosses/vorkath
- content/skills/agility

Detected custom overlap:
⚠ Zulrah
  Local status: VERIFIED / CUSTOM
  Action: FEATURE EQUIVALENCE REVIEW REQUIRED

No overlap:
✓ Vorkath
✓ Agility
```

De tool mag dus detecteren en adviseren, maar niet zelfstandig beslissen welke concurrerende implementatie wordt verwijderd.

---

# 32. Roadmap moet actief onderhouden worden

Wanneer nieuw werk start:

```text
🟣 PLANNED
↓
🔵 IN PROGRESS
```

Wanneer code klaar is maar nog niet voldoende getest:

```text
🔵 IN PROGRESS
↓
🟡 IMPLEMENTED / NEEDS TESTING
```

Na verificatie:

```text
🟡 IMPLEMENTED / NEEDS TESTING
↓
🟢 VERIFIED
```

Wanneer upstream overlap verschijnt:

```text
🟢 VERIFIED / CUSTOM
↓
🔁 UPSTREAM REVIEW
```

Na vergelijking:

```text
🔁 UPSTREAM REVIEW
↓
🟢 VERIFIED / CUSTOM
```

of:

```text
🔁 UPSTREAM REVIEW
↓
♻ HYBRID / MIGRATING
↓
🟢 VERIFIED / HYBRID
```

---

# 33. Roadmap als planning, niet als belofte

`CUSTOM_PROGRESS.md` moet de actuele technische toestand weergeven.

Gebruik bijvoorbeeld:

```text
NOW
NEXT
LATER
BACKLOG
```

Geen kunstmatige deadlines nodig tenzij er echt een deadline bestaat.

Aanbevolen:

```markdown
## Roadmap

### NOW
Werk dat actief wordt uitgevoerd.

### NEXT
Eerstvolgende logische features.

### LATER
Gewenste toekomstige content.

### BACKLOG
Ideeën / lage prioriteit / nog te onderzoeken.
```

Zo blijft de roadmap bruikbaar zonder voortdurend verouderde datums.

---

# 34. Nieuwe acceptance criteria voor upstream-sync

Een upstream-sync is pas volledig klaar wanneer:

- [ ] upstream commits zijn geïnspecteerd;
- [ ] technische Git-conflicts zijn opgelost;
- [ ] semantische/feature-overlap is gecontroleerd;
- [ ] nieuwe upstream bosses/features zijn vergeleken met bestaande custom content;
- [ ] overlappende features zijn gemarkeerd als `UPSTREAM REVIEW` indien nodig;
- [ ] geen custom feature stilzwijgend is vervangen;
- [ ] relevante custom tests zijn uitgevoerd;
- [ ] `CUSTOM_CONTENT.md` zo nodig is bijgewerkt;
- [ ] `CUSTOM_PROGRESS.md` zo nodig is bijgewerkt;
- [ ] `docs/custom/*` zo nodig is bijgewerkt;
- [ ] core modifications opnieuw zijn beoordeeld op noodzaak;
- [ ] de uiteindelijke implementatie één duidelijke actieve codepath heeft;
- [ ] `main` weer een bekende stabiele serverstaat is.

---

# 35. Gewenste eindstructuur

De repository moet uiteindelijk ongeveer deze informatiearchitectuur hebben:

```text
OpenRune-Server/
│
├── README.md
├── PROGRESS.md
│   └── official/upstream generated progress
│
├── CUSTOM_CONTENT.md
│   └── wat wij zelf toegevoegd/aangepast hebben
│
├── CUSTOM_PROGRESS.md
│   └── actuele fork-status + roadmap
│
├── docs/
│   └── custom/
│       ├── README.md
│       ├── architecture.md
│       ├── core-modifications.md
│       ├── zulrah.md
│       ├── pets.md
│       ├── commands.md
│       └── ...
│
└── content/
```

`PROGRESS.md` mag upstream blijven volgen.

`CUSTOM_PROGRESS.md` vertelt de waarheid over **onze fork**.

---

# 36. Kritische hoofdregel

Bij iedere toekomstige upstream update:

```text
UPSTREAM IS A SOURCE OF IMPROVEMENTS
NOT AN AUTOMATIC SOURCE OF TRUTH FOR OUR CUSTOM IMPLEMENTATIONS
```

Upstream veranderingen worden serieus genomen en waar mogelijk gebruikt.

Maar wanneer dezelfde functionaliteit al custom bestaat:

```text
DETECT
↓
COMPARE
↓
TEST
↓
CHOOSE OR COMBINE
↓
DOCUMENT
```

Nooit:

```text
UPSTREAM EXISTS
↓
DELETE CUSTOM
```

De repository moet hierdoor zowel upstream-vriendelijk als onafhankelijk onderhoudbaar blijven.
