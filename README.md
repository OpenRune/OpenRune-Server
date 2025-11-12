


<h1 align="center">
  <img src="https://raw.githubusercontent.com/AlterRSPS/Resources/main/docs/resources/ReadMe_Alter/Alter_Successfully_initialized.png" alt="Alter" width="720">
</h1>

<p align="center">
  <a href="https://github.com/AlterRSPS/Alter/blob/master/LICENSE"><img alt="License" src="https://img.shields.io/github/license/AlterRSPS/Alter?style=for-the-badge&color=6f42c1"/></a>
  <a href="https://discord.com/invite/sAzCuuwkpN"><img alt="Discord" src="https://img.shields.io/discord/871816054329118770?style=for-the-badge&logo=discord&logoColor=white&color=5865F2"/></a>
  <a href="https://trello.com/b/A0LefFDs/later"><img alt="Roadmap" src="https://img.shields.io/badge/Trello-Roadmap-026AA7?style=for-the-badge&logo=trello&logoColor=white"/></a>
  <a href="https://tokei.rs/b1/github/AlterRSPS/Alter"><img alt="Lines of Code" src="https://tokei.rs/b1/github/AlterRSPS/Alter?style=for-the-badge"/></a>
  <a href="https://adoptium.net/temurin/releases/?version=17"><img alt="JDK 17" src="https://img.shields.io/badge/JDK-17-blue?style=for-the-badge"/></a>
  <a href="https://www.youtube.com/watch?v=2Tu-NTzMbf0"><img alt="Install Guide" src="https://img.shields.io/badge/Install-Video-FF0000?style=for-the-badge&logo=youtube&logoColor=white"/></a>
  <a href="https://docs.google.com/document/d/1Wc6jDg7Lk7zlGLN2q3z7t2BKBc6SbfBJ"><img alt="Setup Docs" src="https://img.shields.io/badge/Server-Setup-0A66C2?style=for-the-badge&logo=readthedocs&logoColor=white"/></a>
  <a href="https://github.com/AlterRSPS/Alter/issues/new/choose"><img alt="Bug Reports" src="https://img.shields.io/badge/Issues-Report_Bug-DC3545?style=for-the-badge&logo=github&logoColor=white"/></a>
  <a href="https://github.com/AlterRSPS/Alter/pulls"><img alt="Pull Requests" src="https://img.shields.io/badge/Contribute-Pull_Request-28A745?style=for-the-badge&logo=github&logoColor=white"/></a>
</p>

<p align="center">Alter is a modular fork of RSMod that powers an OSRS-compatible server with a plug-and-play plugin ecosystem focused on extensibility and ease of use.</p>

<p align="center">
  <a href="https://github.com/AlterRSPS/Alter/issues/new/choose" target="_blank">🐞 Report a bug</a>
  •
  <a href="https://discord.com/invite/sAzCuuwkpN" target="_blank">☎️ Join the Discord</a>
  •
  <a href="https://trello.com/b/A0LefFDs/later" target="_blank">🗺️ View the roadmap</a>
  •
  <a href="https://www.youtube.com/watch?v=2Tu-NTzMbf0" target="_blank">▶️ Watch the setup video</a>
</p>

## 📚 Quick Links
- [Server setup tutorial](https://www.youtube.com/watch?v=2Tu-NTzMbf0)
- [Install IntelliJ IDEA](https://www.jetbrains.com/idea/download/#section=windows)
- [Alter roadmap on Trello](https://trello.com/b/A0LefFDs/later)
- [Discord community](https://discord.com/invite/sAzCuuwkpN)

## 🤔 What is Alter?
Alter builds on the foundation laid by [RSMod](https://github.com/Tomm0017/rsmod) to deliver a flexible, developer-friendly OSRS game server. Its modular architecture lets you ship new gameplay features as standalone plugins without touching core engine code. Server owners with little to no programming experience can rely on contributors to drop prebuilt plugins into the `game-plugins` module and have them load automatically at runtime.

## 🚀 Why Choose Alter?
### 🔧 Modular by design
Alter loads plugins dynamically, making it simple to extend gameplay, content, or systems while keeping the base server clean.

### ⚙️ Production-ready tooling
Gradle tasks, KSP processors, and structured modules keep development smooth for teams of any size.

### 👥 Community-driven
Active maintainers review contributions, publish roadmap updates, and support users through Discord and Trello.

### 📏 OSRS-compatible
Alter adheres to OSRS protocols, giving you the freedom to connect any compliant client and customize server-side behavior.

## 🛠️ Getting Started (Server)
1. **Clone the repository**  
   - `File → New → Project from Version Control` in IntelliJ, then paste `https://github.com/AlterRSPS/Alter`.
   - Alternatively, clone via Git CLI and open the project manually.

2. **Install dependencies**  
   - Ensure you have [IntelliJ IDEA](https://www.jetbrains.com/idea/download/#section=windows).  
   - Set the project SDK to Java 17: `File → Project Structure → SDK`.
   - Recommended: install the [rscm-plugin](https://github.com/blurite/rscm-plugin) for better entity reference tooling.

3. **Gradle bootstrap**  
   - Open the Gradle tool window.  
   - Run `Alter → other → install`.  
   - When the task completes, run `Alter → game → Tasks → application → run`.

4. **Verify startup**  
   - A successful boot prints `Alter Successfully initialized` in the terminal.  
   - If you only see `Alter Loaded up in x ms.` you likely skipped a step.

Screenshots showcasing each step are available in the repo under `Resources/main/docs/resources/ReadMe_Alter/`.

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
    name: Alter
    jav_config_url: https://client.blurite.io/jav_local_235.ws
    varp_count: 15000
    revision: 235.2
    modulus: YOUR_MODULUS_KEY_HERE
```

Find the modulus in the project root, copy it exactly, and replace `YOUR_MODULUS_KEY_HERE`. If `.rsprox` does not exist, launching RSProx once will create it.  
> [!WARNING]
> Avoid unofficial clients such as Devious; they have previously been caught shipping account stealers.

## 🗺️ Project Planning
- Public roadmap and task board: [Alter Trello](https://trello.com/b/A0LefFDs/later).  
- Trello write access and contributor listing are reserved for active maintainers—contact Chris via Discord with a short summary of your work if you need access.

## 💬 Bug Reports & Support
- Open an issue on [GitHub](https://github.com/AlterRSPS/Alter/issues) with reproduction details.
- Reach the team directly in the [Discord server](https://discord.com/invite/sAzCuuwkpN).

## 🙏 Acknowledgments
- Cache management powered by [OpenRune-FileStore](https://github.com/OpenRune/OpenRune-FileStore).
- Pathfinding based on [RsMod2 RouteFinder](https://github.com/rsmod/rsmod/tree/main/engine/routefinder).
- Additional credits and references live on the [AlterRSPS GitHub organization](https://github.com/AlterRSPS).

[patch]: https://oldschool.runescape.wiki/w/Update:Leagues_V:_Raging_Echos_Rewards_Are_Here
