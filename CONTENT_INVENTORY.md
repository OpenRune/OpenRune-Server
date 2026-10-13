# Content inventory

Generated technical index. [PROGRESS.md](PROGRESS.md) contains current status and the roadmap.

**Module** = source module found; **Module stub** = module without Kotlin source in `src/main`.
**References only** = symbol mentioned in other code; **Drop table only** = only a loot definition found.
**Not detected** = no match in this scan. None of these labels verifies gameplay or completeness.

The catalog matches stored wiki lists against module names/aliases and NPC/content symbols. References may be pet or shop code; unmatched implementations may exist elsewhere.
Edit source/configuration, then run `node tools/progress/content-progress.mjs`.

## Skills

| Feature | Evidence | Source |
|---|---|---|
| [Attack](https://oldschool.runescape.wiki/w/Attack) | Module | [api/combat](api/combat) |
| [Strength](https://oldschool.runescape.wiki/w/Strength) | Module | [api/combat](api/combat) |
| [Defence](https://oldschool.runescape.wiki/w/Defence) | Module | [api/combat](api/combat) |
| [Ranged](https://oldschool.runescape.wiki/w/Ranged) | Module | [api/combat](api/combat) |
| [Hitpoints](https://oldschool.runescape.wiki/w/Hitpoints) | Module | [api/combat](api/combat) |
| [Magic](https://oldschool.runescape.wiki/w/Magic) | Module | [content/skills/magic](content/skills/magic) |
| [Prayer](https://oldschool.runescape.wiki/w/Prayer) | Module | [content/skills/prayer](content/skills/prayer) |
| [Runecraft](https://oldschool.runescape.wiki/w/Runecraft) | Module | [content/skills/runecrafting](content/skills/runecrafting) |
| [Slayer](https://oldschool.runescape.wiki/w/Slayer) | Module | [content/skills/slayer](content/skills/slayer) |
| [Mining](https://oldschool.runescape.wiki/w/Mining) | Module | [content/skills/mining](content/skills/mining) |
| [Smithing](https://oldschool.runescape.wiki/w/Smithing) | Module | [content/skills/smithing](content/skills/smithing) |
| [Woodcutting](https://oldschool.runescape.wiki/w/Woodcutting) | Module | [content/skills/woodcutting](content/skills/woodcutting) |
| [Firemaking](https://oldschool.runescape.wiki/w/Firemaking) | Module | [content/skills/firemaking](content/skills/firemaking) |
| [Cooking](https://oldschool.runescape.wiki/w/Cooking) | Module | [content/skills/cooking](content/skills/cooking) |
| [Herblore](https://oldschool.runescape.wiki/w/Herblore) | Module | [content/skills/herblore](content/skills/herblore) |
| [Fishing](https://oldschool.runescape.wiki/w/Fishing) | Module | [content/skills/fishing](content/skills/fishing) |
| [Agility](https://oldschool.runescape.wiki/w/Agility) | Module | [content/skills/agility](content/skills/agility) |
| [Thieving](https://oldschool.runescape.wiki/w/Thieving) | Module | [content/skills/thieving](content/skills/thieving) |
| [Crafting](https://oldschool.runescape.wiki/w/Crafting) | Module | [content/skills/crafting](content/skills/crafting) |
| [Fletching](https://oldschool.runescape.wiki/w/Fletching) | Module | [content/skills/fletching](content/skills/fletching) |
| [Farming](https://oldschool.runescape.wiki/w/Farming) | Module | [content/skills/farming](content/skills/farming) |
| [Hunter](https://oldschool.runescape.wiki/w/Hunter) | Module | [content/skills/hunter](content/skills/hunter) |

<details>
<summary>Other catalog entries (1)</summary>

| Feature | Evidence | Source |
|---|---|---|
| [Construction](https://oldschool.runescape.wiki/w/Construction) | References only | [FaladorShopkeepersScript.kt](content/areas/city/falador/src/main/kotlin/org/rsmod/content/areas/city/falador/npcs/FaladorShopkeepersScript.kt) |

</details>

## Bosses

| Feature | Evidence | Source |
|---|---|---|
| [Amoxliatl](https://oldschool.runescape.wiki/w/Amoxliatl) | Module | [content/bosses/amoxliatl](content/bosses/amoxliatl) |
| [Araxxor](https://oldschool.runescape.wiki/w/Araxxor) | Module | [content/bosses/araxxor](content/bosses/araxxor) |
| [Barrows](https://oldschool.runescape.wiki/w/Barrows) | Module | [content/bosses/barrows](content/bosses/barrows) |
| [Callisto](https://oldschool.runescape.wiki/w/Callisto) | Module | [content/bosses/callisto](content/bosses/callisto) |
| [Commander Zilyana](https://oldschool.runescape.wiki/w/Commander_Zilyana) | Module | [content/bosses/zilyana](content/bosses/zilyana) |
| [Corporeal Beast](https://oldschool.runescape.wiki/w/Corporeal_Beast) | Module | [content/bosses/corporeal-beast](content/bosses/corporeal-beast) |
| [Doom of Mokhaiotl](https://oldschool.runescape.wiki/w/Doom_of_Mokhaiotl) | Module | [content/bosses/doom-of-mokhaiotl](content/bosses/doom-of-mokhaiotl) |
| [Duke Sucellus](https://oldschool.runescape.wiki/w/Duke_Sucellus) | Module | [content/bosses/duke-sucellus](content/bosses/duke-sucellus) |
| [Gemstone Crab](https://oldschool.runescape.wiki/w/Gemstone_Crab) | Module | [content/bosses/gemstone-crab](content/bosses/gemstone-crab) |
| [General Graardor](https://oldschool.runescape.wiki/w/General_Graardor) | Module | [content/bosses/graardor](content/bosses/graardor) |
| [K'ril Tsutsaroth](https://oldschool.runescape.wiki/w/K'ril_Tsutsaroth) | Module | [content/bosses/kril](content/bosses/kril) |
| [King Black Dragon](https://oldschool.runescape.wiki/w/King_Black_Dragon) | Module | [content/bosses/kbd](content/bosses/kbd) |
| [Kraken](https://oldschool.runescape.wiki/w/Kraken) | Module | [content/bosses/kraken](content/bosses/kraken) |
| [Kree'arra](https://oldschool.runescape.wiki/w/Kree'arra) | Module | [content/bosses/kreearra](content/bosses/kreearra) |
| [Phantom Muspah](https://oldschool.runescape.wiki/w/Phantom_Muspah) | Module | [content/bosses/muspah](content/bosses/muspah) |
| [Scurrius](https://oldschool.runescape.wiki/w/Scurrius) | Module | [content/bosses/scurrius](content/bosses/scurrius) |
| [Spindel](https://oldschool.runescape.wiki/w/Spindel) | Module | [content/bosses/spindel](content/bosses/spindel) |
| [The Leviathan](https://oldschool.runescape.wiki/w/The_Leviathan) | Module | [content/bosses/leviathan](content/bosses/leviathan) |
| [The Whisperer](https://oldschool.runescape.wiki/w/The_Whisperer) | Module | [content/bosses/whisperer](content/bosses/whisperer) |
| [Vardorvis](https://oldschool.runescape.wiki/w/Vardorvis) | Module | [content/bosses/vardorvis](content/bosses/vardorvis) |
| [Zulrah](https://oldschool.runescape.wiki/w/Zulrah) | Module | [content/bosses/zulrah](content/bosses/zulrah) |

<details>
<summary>Other catalog entries (148)</summary>

| Feature | Evidence | Source |
|---|---|---|
| [Abyssal Sire](https://oldschool.runescape.wiki/w/Abyssal_Sire) | Drop table only | [abyssal_sire.toml](content/drops/src/main/resources/drops/tables/monsters/abyssal_sire.toml) |
| [Agrith Naar](https://oldschool.runescape.wiki/w/Agrith_Naar) | Not detected | — |
| [Agrith-Na-Na](https://oldschool.runescape.wiki/w/Agrith-Na-Na) | Not detected | — |
| [Ahrim the Blighted](https://oldschool.runescape.wiki/w/Ahrim_the_Blighted) | Drop table only | [ahrim_the_blighted.toml](content/drops/src/main/resources/drops/tables/monsters/ahrim_the_blighted.toml) |
| [Akkha](https://oldschool.runescape.wiki/w/Akkha) | Drop table only | [AkkhaDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/AkkhaDropTable.kt) |
| [Alchemical Hydra](https://oldschool.runescape.wiki/w/Alchemical_Hydra) | Drop table only | [AlchemicalHydraDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/AlchemicalHydraDropTable.kt) |
| [Arrg](https://oldschool.runescape.wiki/w/Arrg) | Drop table only | [arrg.toml](content/drops/src/main/resources/drops/tables/monsters/arrg.toml) |
| [Artio](https://oldschool.runescape.wiki/w/Artio) | References only | [Callisto.kt](content/bosses/callisto/src/main/kotlin/org/rsmod/content/bosses/callisto/Callisto.kt) |
| [Arzinian Avatar of Magic](https://oldschool.runescape.wiki/w/Arzinian_Avatar_of_Magic) | Not detected | — |
| [Arzinian Avatar of Ranging](https://oldschool.runescape.wiki/w/Arzinian_Avatar_of_Ranging) | Not detected | — |
| [Arzinian Avatar of Strength](https://oldschool.runescape.wiki/w/Arzinian_Avatar_of_Strength) | Not detected | — |
| [Arzinian Being of Bordanzan](https://oldschool.runescape.wiki/w/Arzinian_Being_of_Bordanzan) | Not detected | — |
| [Ba-Ba](https://oldschool.runescape.wiki/w/Ba-Ba) | Drop table only | [BaBaDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/BaBaDropTable.kt) |
| [Barrelchest](https://oldschool.runescape.wiki/w/Barrelchest) | Not detected | — |
| [Black Knight Titan](https://oldschool.runescape.wiki/w/Black_Knight_Titan) | Drop table only | [BlackKnightTitanDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/BlackKnightTitanDropTable.kt) |
| [Black demon](https://oldschool.runescape.wiki/w/Black_demon) | Not detected | — |
| [Black golem](https://oldschool.runescape.wiki/w/Black_golem) | Not detected | — |
| [Blood Moon](https://oldschool.runescape.wiki/w/Blood_Moon) | Not detected | — |
| [Blue Moon](https://oldschool.runescape.wiki/w/Blue_Moon) | Not detected | — |
| [Bouncer](https://oldschool.runescape.wiki/w/Bouncer) | Not detected | — |
| [Bouncer (ghost)](https://oldschool.runescape.wiki/w/Bouncer_(ghost)) | Not detected | — |
| [Branda the Fire Queen](https://oldschool.runescape.wiki/w/Branda_the_Fire_Queen) | Drop table only | [BrandaTheFireQueenDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/BrandaTheFireQueenDropTable.kt) |
| [Brutus](https://oldschool.runescape.wiki/w/Brutus) | Drop table only | [brutus.toml](content/drops/src/main/resources/drops/tables/monsters/brutus.toml) |
| [Bryophyta](https://oldschool.runescape.wiki/w/Bryophyta) | Not detected | — |
| [Calvar'ion](https://oldschool.runescape.wiki/w/Calvar'ion) | Drop table only | [calvarion.toml](content/drops/src/main/resources/drops/tables/monsters/calvarion.toml) |
| [Cerberus](https://oldschool.runescape.wiki/w/Cerberus) | Drop table only | [CerberusDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/CerberusDropTable.kt) |
| [Chaos Elemental](https://oldschool.runescape.wiki/w/Chaos_Elemental) | Drop table only | [chaos_elemental.toml](content/drops/src/main/resources/drops/tables/monsters/chaos_elemental.toml) |
| [Chaos Fanatic](https://oldschool.runescape.wiki/w/Chaos_Fanatic) | Drop table only | [ChaosFanaticDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/ChaosFanaticDropTable.kt) |
| [Chronozon](https://oldschool.runescape.wiki/w/Chronozon) | Drop table only | [chronozon.toml](content/drops/src/main/resources/drops/tables/monsters/chronozon.toml) |
| [Corrupted Hunllef](https://oldschool.runescape.wiki/w/Corrupted_Hunllef) | References only | [Hunllef.kt](content/minigames/gauntlet/src/main/kotlin/org/rsmod/content/minigames/gauntlet/hunllef/Hunllef.kt) |
| [Count Draynor](https://oldschool.runescape.wiki/w/Count_Draynor) | Not detected | — |
| [Crazy archaeologist](https://oldschool.runescape.wiki/w/Crazy_archaeologist) | Drop table only | [CrazyArchaeologistDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/CrazyArchaeologistDropTable.kt) |
| [Crystalline Hunllef](https://oldschool.runescape.wiki/w/Crystalline_Hunllef) | References only | [Hunllef.kt](content/minigames/gauntlet/src/main/kotlin/org/rsmod/content/minigames/gauntlet/hunllef/Hunllef.kt); [HunllefSpecs.kt](content/minigames/gauntlet/src/main/kotlin/org/rsmod/content/minigames/gauntlet/hunllef/HunllefSpecs.kt) |
| [Culinaromancer](https://oldschool.runescape.wiki/w/Culinaromancer) | Drop table only | [culinaromancer.toml](content/drops/src/main/resources/drops/tables/monsters/culinaromancer.toml) |
| [Dad](https://oldschool.runescape.wiki/w/Dad) | Drop table only | [dad.toml](content/drops/src/main/resources/drops/tables/monsters/dad.toml) |
| [Dagannoth Prime](https://oldschool.runescape.wiki/w/Dagannoth_Prime) | Drop table only | [dagannoth_prime.toml](content/drops/src/main/resources/drops/tables/monsters/dagannoth_prime.toml) |
| [Dagannoth Rex](https://oldschool.runescape.wiki/w/Dagannoth_Rex) | Drop table only | [dagannoth_rex.toml](content/drops/src/main/resources/drops/tables/monsters/dagannoth_rex.toml) |
| [Dagannoth Supreme](https://oldschool.runescape.wiki/w/Dagannoth_Supreme) | Drop table only | [dagannoth_supreme.toml](content/drops/src/main/resources/drops/tables/monsters/dagannoth_supreme.toml) |
| [Dagannoth mother](https://oldschool.runescape.wiki/w/Dagannoth_mother) | Drop table only | [dagannoth_mother.toml](content/drops/src/main/resources/drops/tables/monsters/dagannoth_mother.toml) |
| [Damis](https://oldschool.runescape.wiki/w/Damis) | Drop table only | [damis.toml](content/drops/src/main/resources/drops/tables/monsters/damis.toml) |
| [Dawn](https://oldschool.runescape.wiki/w/Dawn) | Not detected | — |
| [Delrith](https://oldschool.runescape.wiki/w/Delrith) | Not detected | — |
| [Demonic Brutus](https://oldschool.runescape.wiki/w/Demonic_Brutus) | Drop table only | [demonic_brutus.toml](content/drops/src/main/resources/drops/tables/monsters/demonic_brutus.toml) |
| [Deranged archaeologist](https://oldschool.runescape.wiki/w/Deranged_archaeologist) | Drop table only | [deranged_archaeologist.toml](content/drops/src/main/resources/drops/tables/monsters/deranged_archaeologist.toml) |
| [Dessourt](https://oldschool.runescape.wiki/w/Dessourt) | Not detected | — |
| [Dessous](https://oldschool.runescape.wiki/w/Dessous) | Not detected | — |
| [Dharok the Wretched](https://oldschool.runescape.wiki/w/Dharok_the_Wretched) | Drop table only | [dharok_the_wretched.toml](content/drops/src/main/resources/drops/tables/monsters/dharok_the_wretched.toml) |
| [Dusk](https://oldschool.runescape.wiki/w/Dusk) | Not detected | — |
| [Eclipse Moon](https://oldschool.runescape.wiki/w/Eclipse_Moon) | Not detected | — |
| [Eldric the Ice King](https://oldschool.runescape.wiki/w/Eldric_the_Ice_King) | Drop table only | [EldricTheIceKingDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/EldricTheIceKingDropTable.kt) |
| [Elidinis' Warden](https://oldschool.runescape.wiki/w/Elidinis'_Warden) | Drop table only | [ElidinisWardenDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/ElidinisWardenDropTable.kt) |
| [Elvarg](https://oldschool.runescape.wiki/w/Elvarg) | Not detected | — |
| [Evil spirit](https://oldschool.runescape.wiki/w/Evil_spirit) | Not detected | — |
| [Fareed](https://oldschool.runescape.wiki/w/Fareed) | Not detected | — |
| [Flambeed](https://oldschool.runescape.wiki/w/Flambeed) | Not detected | — |
| [Gadderanks](https://oldschool.runescape.wiki/w/Gadderanks) | Not detected | — |
| [Galvek](https://oldschool.runescape.wiki/w/Galvek) | Not detected | — |
| [Gelatinnoth Mother](https://oldschool.runescape.wiki/w/Gelatinnoth_Mother) | Not detected | — |
| [General Khazard](https://oldschool.runescape.wiki/w/General_Khazard) | Not detected | — |
| [Giant Mole](https://oldschool.runescape.wiki/w/Giant_Mole) | Drop table only | [giant_mole.toml](content/drops/src/main/resources/drops/tables/monsters/giant_mole.toml) |
| [Giant Roc](https://oldschool.runescape.wiki/w/Giant_Roc) | Drop table only | [giant_roc.toml](content/drops/src/main/resources/drops/tables/monsters/giant_roc.toml) |
| [Giant Scarab](https://oldschool.runescape.wiki/w/Giant_Scarab) | Drop table only | [giant_scarab.toml](content/drops/src/main/resources/drops/tables/monsters/giant_scarab.toml) |
| [Giant Sea Snake](https://oldschool.runescape.wiki/w/Giant_Sea_Snake) | Drop table only | [giant_sea_snake.toml](content/drops/src/main/resources/drops/tables/monsters/giant_sea_snake.toml) |
| [Glod](https://oldschool.runescape.wiki/w/Glod) | Drop table only | [glod.toml](content/drops/src/main/resources/drops/tables/monsters/glod.toml) |
| [Glough](https://oldschool.runescape.wiki/w/Glough) | Not detected | — |
| [Great Olm](https://oldschool.runescape.wiki/w/Great_Olm) | Not detected | — |
| [Grey golem](https://oldschool.runescape.wiki/w/Grey_golem) | Not detected | — |
| [Grotesque Guardians](https://oldschool.runescape.wiki/w/Grotesque_Guardians) | Not detected | — |
| [Guthan the Infested](https://oldschool.runescape.wiki/w/Guthan_the_Infested) | Drop table only | [guthan_the_infested.toml](content/drops/src/main/resources/drops/tables/monsters/guthan_the_infested.toml) |
| [Hespori](https://oldschool.runescape.wiki/w/Hespori) | Drop table only | [HesporiDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/HesporiDropTable.kt) |
| [Ice Troll King](https://oldschool.runescape.wiki/w/Ice_Troll_King) | Not detected | — |
| [Ice demon](https://oldschool.runescape.wiki/w/Ice_demon) | Drop table only | [ice_demon.toml](content/drops/src/main/resources/drops/tables/monsters/ice_demon.toml) |
| [Judge of Yama (A Kingdom Divided)](https://oldschool.runescape.wiki/w/Judge_of_Yama_(A_Kingdom_Divided)) | Not detected | — |
| [Jungle Demon](https://oldschool.runescape.wiki/w/Jungle_Demon) | Not detected | — |
| [Kalphite Queen](https://oldschool.runescape.wiki/w/Kalphite_Queen) | Drop table only | [kalphite_queen.toml](content/drops/src/main/resources/drops/tables/monsters/kalphite_queen.toml) |
| [Kamil](https://oldschool.runescape.wiki/w/Kamil) | Drop table only | [kamil.toml](content/drops/src/main/resources/drops/tables/monsters/kamil.toml) |
| [Karamel](https://oldschool.runescape.wiki/w/Karamel) | Not detected | — |
| [Karil the Tainted](https://oldschool.runescape.wiki/w/Karil_the_Tainted) | Drop table only | [karil_the_tainted.toml](content/drops/src/main/resources/drops/tables/monsters/karil_the_tainted.toml) |
| [Kephri](https://oldschool.runescape.wiki/w/Kephri) | Drop table only | [KephriDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/KephriDropTable.kt) |
| [Koschei the deathless](https://oldschool.runescape.wiki/w/Koschei_the_deathless) | Not detected | — |
| [Lowerniel Drakan](https://oldschool.runescape.wiki/w/Lowerniel_Drakan) | Not detected | — |
| [Mad Angel](https://oldschool.runescape.wiki/w/Mad_Angel) | Not detected | — |
| [Maggot King](https://oldschool.runescape.wiki/w/Maggot_King) | Not detected | — |
| [Me](https://oldschool.runescape.wiki/w/Me) | Not detected | — |
| [Melzar the Mad](https://oldschool.runescape.wiki/w/Melzar_the_Mad) | Drop table only | [melzar_the_mad.toml](content/drops/src/main/resources/drops/tables/monsters/melzar_the_mad.toml) |
| [Moons of Peril](https://oldschool.runescape.wiki/w/Moons_of_Peril) | Not detected | — |
| [Moss Guardian](https://oldschool.runescape.wiki/w/Moss_Guardian) | Drop table only | [moss_guardian.toml](content/drops/src/main/resources/drops/tables/monsters/moss_guardian.toml) |
| [Muttadile](https://oldschool.runescape.wiki/w/Muttadile) | Drop table only | [MuttadileDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/MuttadileDropTable.kt) |
| [Nex](https://oldschool.runescape.wiki/w/Nex) | Drop table only | [nex.toml](content/drops/src/main/resources/drops/tables/monsters/nex.toml) |
| [Nezikchened](https://oldschool.runescape.wiki/w/Nezikchened) | Not detected | — |
| [Nylocas Vasilias](https://oldschool.runescape.wiki/w/Nylocas_Vasilias) | Drop table only | [NylocasVasiliasDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/NylocasVasiliasDropTable.kt) |
| [Obor](https://oldschool.runescape.wiki/w/Obor) | Not detected | — |
| [Penance Queen](https://oldschool.runescape.wiki/w/Penance_Queen) | Not detected | — |
| [Pestilent Bloat](https://oldschool.runescape.wiki/w/Pestilent_Bloat) | Drop table only | [PestilentBloatDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/PestilentBloatDropTable.kt) |
| [Phosani's Nightmare](https://oldschool.runescape.wiki/w/Phosani's_Nightmare) | Drop table only | [PhosanisNightmareDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/PhosanisNightmareDropTable.kt) |
| [Revenant maledictus](https://oldschool.runescape.wiki/w/Revenant_maledictus) | References only | [ForinthrySurgeKillHook.kt](content/areas/wilderness/src/main/kotlin/org/rsmod/content/areas/wilderness/ForinthrySurgeKillHook.kt) |
| [Royal Titans](https://oldschool.runescape.wiki/w/Royal_Titans) | Not detected | — |
| [Salarin the twisted](https://oldschool.runescape.wiki/w/Salarin_the_twisted) | Drop table only | [salarin_the_twisted.toml](content/drops/src/main/resources/drops/tables/monsters/salarin_the_twisted.toml) |
| [Sarachnis](https://oldschool.runescape.wiki/w/Sarachnis) | Drop table only | [SarachnisDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/SarachnisDropTable.kt) |
| [Scorpia](https://oldschool.runescape.wiki/w/Scorpia) | Drop table only | [ScorpiaDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/ScorpiaDropTable.kt) |
| [Sea Troll Queen](https://oldschool.runescape.wiki/w/Sea_Troll_Queen) | Not detected | — |
| [Shellbane gryphon](https://oldschool.runescape.wiki/w/Shellbane_gryphon) | Drop table only | [ShellbaneGryphonDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/ShellbaneGryphonDropTable.kt) |
| [Sigmund](https://oldschool.runescape.wiki/w/Sigmund) | Not detected | — |
| [Sir Leye](https://oldschool.runescape.wiki/w/Sir_Leye) | Not detected | — |
| [Sir Mordred](https://oldschool.runescape.wiki/w/Sir_Mordred) | Not detected | — |
| [Skotizo](https://oldschool.runescape.wiki/w/Skotizo) | Drop table only | [skotizo.toml](content/drops/src/main/resources/drops/tables/monsters/skotizo.toml) |
| [Slagilith](https://oldschool.runescape.wiki/w/Slagilith) | Drop table only | [slagilith.toml](content/drops/src/main/resources/drops/tables/monsters/slagilith.toml) |
| [Slash Bash](https://oldschool.runescape.wiki/w/Slash_Bash) | Drop table only | [slash_bash.toml](content/drops/src/main/resources/drops/tables/monsters/slash_bash.toml) |
| [Slug Prince](https://oldschool.runescape.wiki/w/Slug_Prince) | Not detected | — |
| [Sol Heredit](https://oldschool.runescape.wiki/w/Sol_Heredit) | Not detected | — |
| [Sotetseg](https://oldschool.runescape.wiki/w/Sotetseg) | Drop table only | [SotetsegDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/SotetsegDropTable.kt) |
| [Tarn](https://oldschool.runescape.wiki/w/Tarn) | Not detected | — |
| [Tekton](https://oldschool.runescape.wiki/w/Tekton) | Drop table only | [TektonDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/TektonDropTable.kt) |
| [Tempoross](https://oldschool.runescape.wiki/w/Tempoross) | Not detected | — |
| [The Draugen](https://oldschool.runescape.wiki/w/The_Draugen) | Not detected | — |
| [The Everlasting](https://oldschool.runescape.wiki/w/The_Everlasting) | Not detected | — |
| [The Hueycoatl](https://oldschool.runescape.wiki/w/The_Hueycoatl) | Drop table only | [TheHueycoatlDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/TheHueycoatlDropTable.kt) |
| [The Illusive](https://oldschool.runescape.wiki/w/The_Illusive) | Not detected | — |
| [The Inadequacy](https://oldschool.runescape.wiki/w/The_Inadequacy) | Not detected | — |
| [The Maiden of Sugadinti](https://oldschool.runescape.wiki/w/The_Maiden_of_Sugadinti) | Drop table only | [TheMaidenOfSugadintiDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/TheMaidenOfSugadintiDropTable.kt) |
| [The Mimic](https://oldschool.runescape.wiki/w/The_Mimic) | Not detected | — |
| [The Nightmare](https://oldschool.runescape.wiki/w/The_Nightmare) | Drop table only | [TheNightmareDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/TheNightmareDropTable.kt) |
| [The Untouchable](https://oldschool.runescape.wiki/w/The_Untouchable) | Not detected | — |
| [Thermonuclear smoke devil](https://oldschool.runescape.wiki/w/Thermonuclear_smoke_devil) | Drop table only | [thermonuclear_smoke_devil.toml](content/drops/src/main/resources/drops/tables/monsters/thermonuclear_smoke_devil.toml) |
| [Tolna](https://oldschool.runescape.wiki/w/Tolna) | Not detected | — |
| [Torag the Corrupted](https://oldschool.runescape.wiki/w/Torag_the_Corrupted) | Drop table only | [torag_the_corrupted.toml](content/drops/src/main/resources/drops/tables/monsters/torag_the_corrupted.toml) |
| [Tree spirit (Lost City)](https://oldschool.runescape.wiki/w/Tree_spirit_(Lost_City)) | Not detected | — |
| [Treus Dayth](https://oldschool.runescape.wiki/w/Treus_Dayth) | Drop table only | [treus_dayth.toml](content/drops/src/main/resources/drops/tables/monsters/treus_dayth.toml) |
| [Tumeken's Warden](https://oldschool.runescape.wiki/w/Tumeken's_Warden) | Drop table only | [TumekensWardenDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/TumekensWardenDropTable.kt) |
| [TzKal-Zuk](https://oldschool.runescape.wiki/w/TzKal-Zuk) | Not detected | — |
| [TzTok-Jad](https://oldschool.runescape.wiki/w/TzTok-Jad) | Drop table only | [tz_tok_jad.toml](content/drops/src/main/resources/drops/tables/monsters/tz_tok_jad.toml) |
| [Ulfric](https://oldschool.runescape.wiki/w/Ulfric) | Not detected | — |
| [Vanguard](https://oldschool.runescape.wiki/w/Vanguard) | References only | [OlmletDialogue.kt](content/other/pets/src/main/kotlin/org/rsmod/content/other/pets/dialogue/OlmletDialogue.kt) |
| [Vasa Nistirio](https://oldschool.runescape.wiki/w/Vasa_Nistirio) | Drop table only | [VasaNistirioDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/VasaNistirioDropTable.kt) |
| [Venenatis](https://oldschool.runescape.wiki/w/Venenatis) | References only | [Venenatis.kt](content/bosses/spindel/src/main/kotlin/org/rsmod/content/bosses/spindel/Venenatis.kt) |
| [Verac the Defiled](https://oldschool.runescape.wiki/w/Verac_the_Defiled) | Drop table only | [verac_the_defiled.toml](content/drops/src/main/resources/drops/tables/monsters/verac_the_defiled.toml) |
| [Verzik Vitur](https://oldschool.runescape.wiki/w/Verzik_Vitur) | Drop table only | [VerzikViturDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/VerzikViturDropTable.kt) |
| [Vespula](https://oldschool.runescape.wiki/w/Vespula) | Drop table only | [VespulaDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/VespulaDropTable.kt) |
| [Vet'ion](https://oldschool.runescape.wiki/w/Vet'ion) | Drop table only | [vetion.toml](content/drops/src/main/resources/drops/tables/monsters/vetion.toml) |
| [Vorkath](https://oldschool.runescape.wiki/w/Vorkath) | Drop table only | [VorkathDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/VorkathDropTable.kt) |
| [White golem](https://oldschool.runescape.wiki/w/White_golem) | Not detected | — |
| [Wintertodt](https://oldschool.runescape.wiki/w/Wintertodt) | Not detected | — |
| [Wrathmaw](https://oldschool.runescape.wiki/w/Wrathmaw) | Not detected | — |
| [Xamphur](https://oldschool.runescape.wiki/w/Xamphur) | Not detected | — |
| [Xarpus](https://oldschool.runescape.wiki/w/Xarpus) | Drop table only | [XarpusDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/XarpusDropTable.kt) |
| [Yama](https://oldschool.runescape.wiki/w/Yama) | Drop table only | [YamaDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/YamaDropTable.kt) |
| [Zalcano](https://oldschool.runescape.wiki/w/Zalcano) | Drop table only | [zalcano.toml](content/drops/src/main/resources/drops/tables/monsters/zalcano.toml) |
| [Zebak](https://oldschool.runescape.wiki/w/Zebak) | Drop table only | [ZebakDropTable.kt](content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters/ZebakDropTable.kt) |

</details>

## Raids

<details>
<summary>Other catalog entries (4)</summary>

| Feature | Evidence | Source |
|---|---|---|
| [Chambers of Xeric](https://oldschool.runescape.wiki/w/Chambers_of_Xeric) | Not detected | — |
| [The Fractured Archive](https://oldschool.runescape.wiki/w/The_Fractured_Archive) | Not detected | — |
| [Theatre of Blood](https://oldschool.runescape.wiki/w/Theatre_of_Blood) | Not detected | — |
| [Tombs of Amascut](https://oldschool.runescape.wiki/w/Tombs_of_Amascut) | Not detected | — |

</details>

## Minigames

| Feature | Evidence | Source |
|---|---|---|
| [The Gauntlet](https://oldschool.runescape.wiki/w/The_Gauntlet) | Module | [content/minigames/gauntlet](content/minigames/gauntlet) |

<details>
<summary>Other catalog entries (50)</summary>

| Feature | Evidence | Source |
|---|---|---|
| [Barbarian Assault](https://oldschool.runescape.wiki/w/Barbarian_Assault) | Not detected | — |
| [Blast Furnace](https://oldschool.runescape.wiki/w/Blast_Furnace) | Not detected | — |
| [Bounty Hunter](https://oldschool.runescape.wiki/w/Bounty_Hunter) | Not detected | — |
| [Brimhaven Agility Arena](https://oldschool.runescape.wiki/w/Brimhaven_Agility_Arena) | Not detected | — |
| [Burthorpe Games Room](https://oldschool.runescape.wiki/w/Burthorpe_Games_Room) | Not detected | — |
| [Castle Wars](https://oldschool.runescape.wiki/w/Castle_Wars) | Not detected | — |
| [Chompy bird hunting](https://oldschool.runescape.wiki/w/Chompy_bird_hunting) | Not detected | — |
| [Clan Wars](https://oldschool.runescape.wiki/w/Clan_Wars) | Not detected | — |
| [Dorgesh-Kaan market trading](https://oldschool.runescape.wiki/w/Dorgesh-Kaan_market_trading) | Not detected | — |
| [Duel Arena](https://oldschool.runescape.wiki/w/Duel_Arena) | Not detected | — |
| [Emir's Arena](https://oldschool.runescape.wiki/w/Emir's_Arena) | Not detected | — |
| [Farming contracts](https://oldschool.runescape.wiki/w/Farming_contracts) | Not detected | — |
| [Fishing Trawler](https://oldschool.runescape.wiki/w/Fishing_Trawler) | Not detected | — |
| [Fortis Colosseum](https://oldschool.runescape.wiki/w/Fortis_Colosseum) | Not detected | — |
| [Giants' Foundry](https://oldschool.runescape.wiki/w/Giants'_Foundry) | Not detected | — |
| [Gnome Ball](https://oldschool.runescape.wiki/w/Gnome_Ball) | Not detected | — |
| [Gnome Restaurant](https://oldschool.runescape.wiki/w/Gnome_Restaurant) | Not detected | — |
| [Golem crafting](https://oldschool.runescape.wiki/w/Golem_crafting) | Not detected | — |
| [Guardians of the Rift](https://oldschool.runescape.wiki/w/Guardians_of_the_Rift) | Not detected | — |
| [Hallowed Sepulchre](https://oldschool.runescape.wiki/w/Hallowed_Sepulchre) | Not detected | — |
| [Impetuous Impulses](https://oldschool.runescape.wiki/w/Impetuous_Impulses) | Not detected | — |
| [Inferno](https://oldschool.runescape.wiki/w/Inferno) | Not detected | — |
| [Intelligence Gathering](https://oldschool.runescape.wiki/w/Intelligence_Gathering) | Not detected | — |
| [Keldagrim tasks](https://oldschool.runescape.wiki/w/Keldagrim_tasks) | Not detected | — |
| [Last Man Standing](https://oldschool.runescape.wiki/w/Last_Man_Standing) | Not detected | — |
| [Mage Arena](https://oldschool.runescape.wiki/w/Mage_Arena) | Not detected | — |
| [Mage Training Arena](https://oldschool.runescape.wiki/w/Mage_Training_Arena) | Not detected | — |
| [Mahogany Homes](https://oldschool.runescape.wiki/w/Mahogany_Homes) | Not detected | — |
| [Mastering Mixology](https://oldschool.runescape.wiki/w/Mastering_Mixology) | Not detected | — |
| [Mess](https://oldschool.runescape.wiki/w/Mess) | Not detected | — |
| [Nightmare Zone](https://oldschool.runescape.wiki/w/Nightmare_Zone) | Not detected | — |
| [Pest Control](https://oldschool.runescape.wiki/w/Pest_Control) | Not detected | — |
| [Pyramid Plunder](https://oldschool.runescape.wiki/w/Pyramid_Plunder) | Not detected | — |
| [Rat Pits](https://oldschool.runescape.wiki/w/Rat_Pits) | Not detected | — |
| [Rogues' Den](https://oldschool.runescape.wiki/w/Rogues'_Den) | Not detected | — |
| [Shades of Mort'ton (minigame)](https://oldschool.runescape.wiki/w/Shades_of_Mort'ton_(minigame)) | Not detected | — |
| [Sorceress's Garden](https://oldschool.runescape.wiki/w/Sorceress's_Garden) | Not detected | — |
| [Soul Wars](https://oldschool.runescape.wiki/w/Soul_Wars) | Not detected | — |
| [Stealing artefacts](https://oldschool.runescape.wiki/w/Stealing_artefacts) | Not detected | — |
| [Stealing valuables](https://oldschool.runescape.wiki/w/Stealing_valuables) | Not detected | — |
| [Tai Bwo Wannai Cleanup](https://oldschool.runescape.wiki/w/Tai_Bwo_Wannai_Cleanup) | Not detected | — |
| [Tears of Guthix (minigame)](https://oldschool.runescape.wiki/w/Tears_of_Guthix_(minigame)) | Not detected | — |
| [Temple Trekking](https://oldschool.runescape.wiki/w/Temple_Trekking) | Not detected | — |
| [Tithe Farm](https://oldschool.runescape.wiki/w/Tithe_Farm) | Not detected | — |
| [Trouble Brewing](https://oldschool.runescape.wiki/w/Trouble_Brewing) | Not detected | — |
| [TzHaar Fight Cave](https://oldschool.runescape.wiki/w/TzHaar_Fight_Cave) | Not detected | — |
| [TzHaar Fight Pit](https://oldschool.runescape.wiki/w/TzHaar_Fight_Pit) | Not detected | — |
| [TzHaar-Ket-Rak's Challenges](https://oldschool.runescape.wiki/w/TzHaar-Ket-Rak's_Challenges) | Not detected | — |
| [Vale Totems](https://oldschool.runescape.wiki/w/Vale_Totems) | Not detected | — |
| [Volcanic Mine](https://oldschool.runescape.wiki/w/Volcanic_Mine) | Not detected | — |

</details>

## Other content modules

<details>
<summary>Modules outside the catalog matches</summary>

| Module |
|---|
| [content/activities/shades-of-mortton](content/activities/shades-of-mortton) |
| [content/activities/skullball](content/activities/skullball) |
| [content/areas/city/ardougne](content/areas/city/ardougne) |
| [content/areas/city/draynor](content/areas/city/draynor) |
| [content/areas/city/draynor/pack](content/areas/city/draynor/pack) |
| [content/areas/city/falador](content/areas/city/falador) |
| [content/areas/city/falador/pack](content/areas/city/falador/pack) |
| [content/areas/city/lumbridge](content/areas/city/lumbridge) |
| [content/areas/city/lumbridge/pack](content/areas/city/lumbridge/pack) |
| [content/areas/city/port-sarim](content/areas/city/port-sarim) |
| [content/areas/city/port-sarim/pack](content/areas/city/port-sarim/pack) |
| [content/areas/city/prifddinas](content/areas/city/prifddinas) |
| [content/areas/city/rimmington](content/areas/city/rimmington) |
| [content/areas/city/rimmington/pack](content/areas/city/rimmington/pack) |
| [content/areas/city/taverley](content/areas/city/taverley) |
| [content/areas/city/varrock](content/areas/city/varrock) |
| [content/areas/godwars](content/areas/godwars) |
| [content/areas/misc/dog_shelter](content/areas/misc/dog_shelter) |
| [content/areas/misc/dwarven-mine](content/areas/misc/dwarven-mine) |
| [content/areas/misc/mining-guild](content/areas/misc/mining-guild) |
| [content/areas/misc/motherlode-mine](content/areas/misc/motherlode-mine) |
| [content/areas/misc/motherlode-mine/pack](content/areas/misc/motherlode-mine/pack) |
| [content/areas/misc/multiways](content/areas/misc/multiways) |
| [content/areas/misc/ver_sinhaza](content/areas/misc/ver_sinhaza) |
| [content/areas/misc/wizards_tower](content/areas/misc/wizards_tower) |
| [content/areas/misc/wizards_tower/pack](content/areas/misc/wizards_tower/pack) |
| [content/areas/revenant-caves](content/areas/revenant-caves) |
| [content/areas/revenant-caves/pack](content/areas/revenant-caves/pack) |
| [content/areas/wilderness](content/areas/wilderness) |
| [content/areas/zeah](content/areas/zeah) |
| [content/bosses/demonic-gorilla](content/bosses/demonic-gorilla) |
| [content/bosses/demonic-gorilla/pack](content/bosses/demonic-gorilla/pack) |
| [content/bosses/lizardman-shaman](content/bosses/lizardman-shaman) |
| [content/bosses/lizardman-shaman/pack](content/bosses/lizardman-shaman/pack) |
| [content/bosses/tormented-demon](content/bosses/tormented-demon) |
| [content/bosses/tormented-demon/pack](content/bosses/tormented-demon/pack) |
| [content/drops](content/drops) |
| [content/events/shooting-stars](content/events/shooting-stars) |
| [content/events/shooting-stars/pack](content/events/shooting-stars/pack) |
| [content/generic/generic-locs](content/generic/generic-locs) |
| [content/generic/generic-npcs](content/generic/generic-npcs) |
| [content/generic/generic-npcs/pack](content/generic/generic-npcs/pack) |
| [content/generic/killcount](content/generic/killcount) |
| [content/interfaces/bank](content/interfaces/bank) |
| [content/interfaces/collection-log](content/interfaces/collection-log) |
| [content/interfaces/combat-tab](content/interfaces/combat-tab) |
| [content/interfaces/deposit-box](content/interfaces/deposit-box) |
| [content/interfaces/emotes](content/interfaces/emotes) |
| [content/interfaces/equipment](content/interfaces/equipment) |
| [content/interfaces/fade-overlay](content/interfaces/fade-overlay) |
| [content/interfaces/gameframe](content/interfaces/gameframe) |
| [content/interfaces/journal-tab](content/interfaces/journal-tab) |
| [content/interfaces/logout-tab](content/interfaces/logout-tab) |
| [content/interfaces/menu](content/interfaces/menu) |
| [content/interfaces/monster-info](content/interfaces/monster-info) |
| [content/interfaces/monster-info/pack](content/interfaces/monster-info/pack) |
| [content/interfaces/notifications](content/interfaces/notifications) |
| [content/interfaces/omnishop](content/interfaces/omnishop) |
| [content/interfaces/prayer-tab](content/interfaces/prayer-tab) |
| [content/interfaces/prayer-tab/pack](content/interfaces/prayer-tab/pack) |
| [content/interfaces/settings](content/interfaces/settings) |
| [content/interfaces/skill-guides](content/interfaces/skill-guides) |
| [content/interfaces/spellbook](content/interfaces/spellbook) |
| [content/interfaces/worldmap](content/interfaces/worldmap) |
| [content/interfaces/xp-drops](content/interfaces/xp-drops) |
| [content/other/combat-achievements](content/other/combat-achievements) |
| [content/other/combat-achievements/pack](content/other/combat-achievements/pack) |
| [content/other/commands](content/other/commands) |
| [content/other/commands/pack](content/other/commands/pack) |
| [content/other/consumables](content/other/consumables) |
| [content/other/dave/pack](content/other/dave/pack) |
| [content/other/discord](content/other/discord) |
| [content/other/home-teleporter](content/other/home-teleporter) |
| [content/other/home-teleporter/pack](content/other/home-teleporter/pack) |
| [content/other/ironman](content/other/ironman) |
| [content/other/login](content/other/login) |
| [content/other/mapclock](content/other/mapclock) |
| [content/other/max-cape](content/other/max-cape) |
| [content/other/max-cape/pack](content/other/max-cape/pack) |
| [content/other/nero-completionist](content/other/nero-completionist) |
| [content/other/nero-completionist/pack](content/other/nero-completionist/pack) |
| [content/other/npc-animations](content/other/npc-animations) |
| [content/other/npc-animations/pack](content/other/npc-animations/pack) |
| [content/other/pets](content/other/pets) |
| [content/other/pets/pack](content/other/pets/pack) |
| [content/other/sandstorm](content/other/sandstorm) |
| [content/other/spawn](content/other/spawn) |
| [content/other/spawn/pack](content/other/spawn/pack) |
| [content/other/special-attacks](content/other/special-attacks) |
| [content/other/special-attacks/pack](content/other/special-attacks/pack) |
| [content/other/special-weapons](content/other/special-weapons) |
| [content/other/special-weapons/pack](content/other/special-weapons/pack) |
| [content/other/treasure-trails](content/other/treasure-trails) |
| [content/other/treasure-trails/pack](content/other/treasure-trails/pack) |
| [content/other/windmill](content/other/windmill) |
| [content/other/windmill/pack](content/other/windmill/pack) |
| [content/quest](content/quest) |
| [content/quest/pack](content/quest/pack) |
| [content/skills/utils](content/skills/utils) |
| [content/travel/canoe](content/travel/canoe) |

</details>

Catalog: [OSRS Wiki](https://oldschool.runescape.wiki/), [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/).
