# Contributing to Alter

Thanks for your interest in helping build Alter! Contributions from the community keep the project healthy and moving forward. This guide outlines the process, expectations, and best practices so we can collaborate effectively.

---

## 👩‍💻 Behavior & Expectations

- We follow the [Contributor Covenant 2.1](https://github.com/Atlas-OS/.github/blob/main/profile/CODE_OF_CONDUCT.md); be respectful, constructive, and inclusive.
- Use the public Discord (`https://discord.com/invite/kdhBuRaduw`) for quick questions, coordination, and to find mentors for first contributions.
- Trello write access and contributor status are reserved for active maintainers. Reach out to Chris on Discord with a short summary of your work if you need access.

---

## 🧭 Ways to Contribute

- **Report bugs** via [GitHub Issues](https://github.com/Mark7625/Alter-custom/issues/new?template=bug_report.md). Provide reproduction steps, logs (if any), and the game revision you are targeting.
- **Propose features** using the [feature request template](https://github.com/Mark7625/Alter-custom/issues/new?template=feature_request.md) or discuss ideas first in Discord.
- **Improve documentation** (README, setup docs, in-code comments). Small doc PRs are welcome without prior approval.
- **Submit code changes** through pull requests after discussing larger changes with maintainers.

---

## 🛠️ Local Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/Mark7625/Alter-custom.git
   cd Alter-custom
   ```
2. **Install prerequisites**
    - Java 17 (Temurin or an equivalent distribution)
    - Gradle (wrapper is included)
    - Node.js 18+ and npm (required for the `http-api` package)
3. **Bootstrap the Kotlin backend**
   ```bash
   ./gradlew clean build
   ```
4. **Start the game server**
   ```bash
   ./gradlew :game-server:run
   ```
5. **Run the web client (optional)**
   ```bash
   cd http-api
   npm install
   npm run dev
   ```

---

## 🧪 Testing & Quality Checks

- Run unit tests before pushing:
  ```bash
  ./gradlew test
  ```
- For frontend packages, run:
  ```bash
  cd http-api
  npm test
  ```
- Keep pull requests focused. Large changes should be split into multiple PRs with independent tests.
- If you modify game data (`data/` folder), include validation scripts or manual verification steps in the PR description.

---

## 🧱 Coding Guidelines

### Kotlin (backend, plugins, utilities)
- Aim for idiomatic Kotlin. Prefer `val` over `var`, extension functions where appropriate, and data classes for simple models.
- Follow existing naming conventions and module boundaries (`game-server`, `game-api`, `content`, `util`, `cache`, etc.).
- Avoid long monolithic functions; refactor into smaller composable functions.
- Document non-trivial business logic with KDoc comments.


### Data definitions (`data/`, `content/`, configs)
- Maintain sorted keys in JSON/YAML files to minimize merge conflicts.
- Include notes about data sources when adding new cache or spawn definitions.

---

## 📦 Branches & Workflow

1. **Fork the repository** (if you do not have write access).
2. **Create a feature branch** from `main`:
   ```bash
   git checkout -b feat/my-feature
   ```
3. **Commit using clear messages**. We recommend the `type: summary` format (e.g., `fix: correct prayer drain calculation`).
4. **Push your branch** and open a pull request early if you would like feedback.
5. **Link to related issues** in the PR description using `Fixes #123` or `Closes #123`.

---

## ✅ Pull Request Checklist

- [ ] The change compiles (`./gradlew build`).
- [ ] Tests related to the change pass (or are added).
- [ ] Documentation/examples updated where relevant.
- [ ] No unrelated formatting or dependency changes.
- [ ] PR description includes context, screenshots/logs if useful, and manual testing notes.

Maintainers review PRs as time permits. Expect review comments focused on stability, readability, and maintainability. We may ask you to rebase and squash commits before merging.

---

## 🔄 Release Notes & Changelog

- Major features and fixes should include a short summary for release notes in the PR body.
- Maintainers will curate the changelog prior to tagged releases.

---

## 🙌 Getting Help

- Join the [Discord server](https://discord.com/invite/kdhBuRaduw) and visit the `#development` channel.
- Mention the appropriate module maintainers for guidance on Trello items, architecture decisions, or review blockers.

Thank you again for contributing to Alter! Your ideas, fixes, and feedback help shape the project for the entire community.
