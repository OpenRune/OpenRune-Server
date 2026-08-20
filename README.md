


<h1 align="center">
  <img width="333" height="82" alt="Asset 5" src="https://github.com/user-attachments/assets/0a892a2f-9ad6-43f9-affa-2e9a865a1d70" />
</h1>

<p align="center">
  <a href="https://github.com/OpenRune/OpenRune-Server/blob/main/LICENSE"><img alt="License" src="https://img.shields.io/github/license/OpenRune/OpenRune-Server?style=for-the-badge&color=6f42c1"/></a>
  <a href="https://oldschool.runescape.wiki/w/Update:Leagues_V:_Raging_Echos_Rewards_Are_Here"><img alt="Revision 240" src="https://img.shields.io/badge/Revision-240-blueviolet?style=for-the-badge"/></a>
  <a href="https://trello.com/b/A0LefFDs/later"><img alt="Roadmap" src="https://img.shields.io/badge/Trello-Roadmap-026AA7?style=for-the-badge&logo=trello&logoColor=white"/></a>
  <a href="https://github.com/Mark7625/OpenRune-Server/"><img alt="Lines of Code" src="https://img.shields.io/endpoint?url=https%3A%2F%2Fghloc.vercel.app%2Fapi%2FOpenRune%2FOpenRune-Server%2Fbadge%3Fformat%3Dhuman&style=for-the-badge&color=teal"/></a>
  <a href="https://discord.gg/v2qcXzBCwf">
    <img alt="Discord" src="https://img.shields.io/discord/1445802914156249241?label=Discord&logo=discord&logoColor=white&style=for-the-badge&color=5865F2"/>
  </a>
</p>

<p align="center">OpenRune Server is a modular fork of RSMod/Alter that powers an OSRS-compatible server with a plug-and-play plugin ecosystem focused on extensibility and ease of use.</p>

## 🤔 What is OpenRune Server?
OpenRune Server builds on the foundation laid by [RSMod](https://github.com/rsmod/rsmod) to deliver a flexible, developer-friendly OSRS game server. Its modular architecture lets you ship new gameplay features as standalone plugins without touching core engine code. Server owners with little to no programming experience can rely on contributors to drop prebuilt plugins into the `content` module and have them load automatically at runtime.

## 🚀 Why Choose OpenRune Server?
### 🔧 Modular by design
OpenRune Server loads plugins dynamically, making it simple to extend gameplay, content, or systems while keeping the base server clean.

### 👥 Community-driven
Active maintainers review contributions, publish roadmap updates, and support users through Discord and Trello.

### 📏 OSRS-compatible
OpenRune Server adheres to OSRS protocols, giving you the freedom to connect any compliant client and customize server-side behavior.


<!-- content-progress:start -->

## 📊 Content progress

Skills **10/23** · Bosses **6/170** · Raids **0/4** · Minigames **0/51**

🟢 implemented & tested · 🟡 partial or untested · 🔴 missing or stub · ⚪ engine-owned

### Skills <sup>10/23</sup>

| | Feature | Status | |
|---|---|---|---|
| ⚪ <img src="https://oldschool.runescape.wiki/images/Attack_icon.png?b4bce" height="20" alt=""> | Attack | engine-owned (`api/combat`) | [wiki](https://oldschool.runescape.wiki/w/Attack) |
| ⚪ <img src="https://oldschool.runescape.wiki/images/Strength_icon.png?e6e0c" height="20" alt=""> | Strength | engine-owned (`api/combat`) | [wiki](https://oldschool.runescape.wiki/w/Strength) |
| ⚪ <img src="https://oldschool.runescape.wiki/images/Defence_icon.png?ca0cd" height="20" alt=""> | Defence | engine-owned (`api/combat`) | [wiki](https://oldschool.runescape.wiki/w/Defence) |
| ⚪ <img src="https://oldschool.runescape.wiki/images/Ranged_icon.png?01b0e" height="20" alt=""> | Ranged | engine-owned (`api/combat`) | [wiki](https://oldschool.runescape.wiki/w/Ranged) |
| ⚪ <img src="https://oldschool.runescape.wiki/images/Hitpoints_icon.png?a4819" height="20" alt=""> | Hitpoints | engine-owned (`api/combat`) | [wiki](https://oldschool.runescape.wiki/w/Hitpoints) |
| 🟡 <img src="https://oldschool.runescape.wiki/images/Magic_icon.png?334cf" height="20" alt=""> | [Magic](content/skills/magic) | 1,031 loc · no tests | [wiki](https://oldschool.runescape.wiki/w/Magic) |
| 🟡 <img src="https://oldschool.runescape.wiki/images/Prayer_icon.png?7e70b" height="20" alt=""> | [Prayer](content/skills/prayer) | 2,569 loc · no tests | [wiki](https://oldschool.runescape.wiki/w/Prayer) |
| 🟡 <img src="https://oldschool.runescape.wiki/images/Runecraft_icon.png?c278c" height="20" alt=""> | [Runecraft](content/skills/runecrafting) | 2,556 loc · no tests | [wiki](https://oldschool.runescape.wiki/w/Runecraft) |
| 🟡 <img src="https://oldschool.runescape.wiki/images/Slayer_icon.png?cd34f" height="20" alt=""> | [Slayer](content/skills/slayer) | 5,494 loc · 95/129 npcs · no tests | [wiki](https://oldschool.runescape.wiki/w/Slayer) |
| 🟡 <img src="https://oldschool.runescape.wiki/images/Mining_icon.png?00870" height="20" alt=""> | [Mining](content/skills/mining) | 1,050 loc · 1/1 tags · no tests | [wiki](https://oldschool.runescape.wiki/w/Mining) |
| 🟡 <img src="https://oldschool.runescape.wiki/images/Smithing_icon.png?d26c5" height="20" alt=""> | [Smithing](content/skills/smithing) | 2,045 loc · no tests | [wiki](https://oldschool.runescape.wiki/w/Smithing) |
| 🟢 <img src="https://oldschool.runescape.wiki/images/Woodcutting_icon.png?6ead4" height="20" alt=""> | [Woodcutting](content/skills/woodcutting) | 409 loc · 2/2 tags · 2 tests | [wiki](https://oldschool.runescape.wiki/w/Woodcutting) |
| 🟡 <img src="https://oldschool.runescape.wiki/images/Firemaking_icon.png?45ea0" height="20" alt=""> | [Firemaking](content/skills/firemaking) | 589 loc · no tests | [wiki](https://oldschool.runescape.wiki/w/Firemaking) |
| 🟡 <img src="https://oldschool.runescape.wiki/images/Cooking_icon.png?a0156" height="20" alt=""> | [Cooking](content/skills/cooking) | 1,358 loc · 4/4 tags · no tests | [wiki](https://oldschool.runescape.wiki/w/Cooking) |
| 🟡 <img src="https://oldschool.runescape.wiki/images/Herblore_icon.png?ffa9e" height="20" alt=""> | [Herblore](content/skills/herblore) | 1,248 loc · 1/1 tags · no tests | [wiki](https://oldschool.runescape.wiki/w/Herblore) |
| 🔴 <img src="https://oldschool.runescape.wiki/images/Fishing_icon.png?15a98" height="20" alt=""> | [Fishing](content/skills/fishing) | stub — 0 lines, last touched 2024-09-07 | [wiki](https://oldschool.runescape.wiki/w/Fishing) |
| 🔴 <img src="https://oldschool.runescape.wiki/images/Agility_icon.png?389e0" height="20" alt=""> | Agility | no module | [wiki](https://oldschool.runescape.wiki/w/Agility) |
| 🔴 <img src="https://oldschool.runescape.wiki/images/Thieving_icon.png?973fe" height="20" alt=""> | Thieving | no module | [wiki](https://oldschool.runescape.wiki/w/Thieving) |
| 🔴 <img src="https://oldschool.runescape.wiki/images/Crafting_icon.png?a1f71" height="20" alt=""> | Crafting | no module | [wiki](https://oldschool.runescape.wiki/w/Crafting) |
| 🔴 <img src="https://oldschool.runescape.wiki/images/Fletching_icon.png?15cda" height="20" alt=""> | Fletching | no module | [wiki](https://oldschool.runescape.wiki/w/Fletching) |
| 🔴 <img src="https://oldschool.runescape.wiki/images/Construction_icon.png?f9bf7" height="20" alt=""> | Construction | no module | [wiki](https://oldschool.runescape.wiki/w/Construction) |
| 🔴 <img src="https://oldschool.runescape.wiki/images/Farming_icon.png?558fa" height="20" alt=""> | Farming | no module | [wiki](https://oldschool.runescape.wiki/w/Farming) |
| 🔴 <img src="https://oldschool.runescape.wiki/images/Hunter_icon.png?8762f" height="20" alt=""> | Hunter | no module | [wiki](https://oldschool.runescape.wiki/w/Hunter) |

See **[PROGRESS.md](PROGRESS.md)** for bosses, every content module, and untracked mechanics.

Want to help? Grab a 🔴 row, nobody is on those.

<!-- content-progress:end -->
## 🛠️ Getting Started



1. **Clone the repository**
   - `File → New → Project from Version Control` in IntelliJ, then paste `https://github.com/OpenRune/OpenRune-Server.git`.
   - OpenRune Servernatively, clone via Git CLI and open the project manually.

2. **Install dependencies**
   - Ensure you have [IntelliJ IDEA](https://www.jetbrains.com/idea/download/#section=windows).
   - Set the project SDK to Java 21: `File → Project Structure → SDK`. (The `:tools:osrs-mcp` module is built with Java 17 via Gradle’s JVM toolchain; other modules target Java 21.)
   - Recommended: install the Rsc plugin in root of the project [OpenRune IntelliJ Tools-1.0.zip](https://github.com/OpenRune/OpenRune-Server/blob/main/OpenRune%20IntelliJ%20Tools-1.0.zip) for better entity reference tooling.

   - #### You may need to point the settings file [openRune-intelliJ-tools.toml](https://github.com/OpenRune/OpenRune-Server/blob/main/openRune-intelliJ-tools.toml) like so


   <img width="300" height="300" alt="image" src="https://github.com/user-attachments/assets/7aa46983-1f84-4c08-abf8-2f17bc72f073" />


4. **Gradle bootstrap**
   - Open the Gradle tool window.
   - Run `OpenRune Server → Tasks → installation → install`.
   - When the task completes, run `OpenRune Server → Tasks → application → run`.

5. **Verify startup**
   - A successful boot prints `OpenRune Server Successfully initialized` in the terminal.
   - If you only see `OpenRune Server Loaded up in x ms.` you likely skipped a step.

Screenshots showcasing each step are available in the repo under `Resources/main/docs/resources/ReadMe_OpenRune Server/`.

## 🎮 Client Setup
> [!TIP]
> Use [RSProx](https://github.com/blurite/rsprox/releases) to connect; it is actively maintained by trusted developers and supports the required OSRS protocols.

For Windows:
1. Press `⊞ + R` and enter `%USERPROFILE%`.
2. Locate (or create) the `.rsprox` directory.
3. Create `proxy-targets.yaml` with:

```yaml
config:
  - id: 1
    name: OpenRune Server
    jav_config_url: https://client.blurite.io/jav_local_240.ws
    varp_count: 15000
    revision: 240
    modulus: YOUR_MODULUS_KEY_HERE
```

Find the modulus in the project root `.data/client.key`, copy it exactly, and replace `YOUR_MODULUS_KEY_HERE`. If `.rsprox` does not exist, launching RSProx once will create it.
Note: RSprox for Private Servers only works currently on Windows and Linux, NOT MacOS!

> [!WARNING]
> And stay away from client's like Devious, as they have been caught adding Account Stealer into their client.
## 📦 Release builds

CI can produce a self-contained `openrune-server-release.zip` with `server.jar`, `game.yml`, and compiled `.data/`. Pushes to `production` publish automatically; other branches can be built manually from **Actions → Release Server**.

See [.github/docs/RELEASE_CI.md](.github/docs/RELEASE_CI.md) for what the workflow does, how to run it manually, and how to build from `production` vs `main`/feature branches.

## 🗺️ Project Planning
- Public roadmap and task board: [OpenRune Server Trello](https://trello.com/b/A0LefFDs/later).
- Trello write access and contributor listing are reserved for active maintainers—contact Chris via Discord with a short summary of your work if you need access.

## 💬 Bug Reports & Support
- Open an issue on [GitHub](https://github.com/OpenRune/OpenRune-Server/issues) with reproduction details.
- Reach the team directly in the [Discord server](https://discord.gg/HAwN6N8F).

## 🙏 Acknowledgments
- Cache management powered by [OpenRune-FileStore](https://github.com/OpenRune/OpenRune-FileStore).
- Original Base [RsMod2](https://github.com/rsmod/rsmod).

## 💙 Contributors
<a href="https://github.com/OpenRune/OpenRune-Server/graphs/contributors" target="_blank"><img src="https://contrib.rocks/image?repo=OpenRune/OpenRune-Server&columns=18" alt="Avatars of all contributors"></a>
