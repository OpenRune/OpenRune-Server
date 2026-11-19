# Demonic Pacts League: setup on Windows

This is OpenRune Server with a Leagues VI: Demonic Pacts mode added. It runs entirely on your PC.
You connect to it with RSProx, which patches the official RuneLite client to talk to your server.

Built against OpenRune-Server commit `15009ceaadc28d87fac66c7cbf9b753ec852036a` (game revision 242).

## 1. Install the tools (once)

Open PowerShell and run:

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e
winget install --id Git.Git -e
```

Close and reopen PowerShell afterwards so both are on your PATH. Check with `java -version`
(it should say 21) and `git --version`.

## 2. Get the server code

Pick a folder, for example `C:\osrs`, then:

```powershell
cd C:\osrs
git clone https://github.com/OpenRune/OpenRune-Server.git
cd OpenRune-Server
git checkout 15009ceaadc28d87fac66c7cbf9b753ec852036a
```

Then add the Demonic Pacts changes, using **one** of these:

- **Patch file** (keeps git history): copy `demonic-pacts-league.patch` into the folder and run
  `git am demonic-pacts-league.patch`
- **Zip**: extract `demonic-pacts-league-files.zip` into the `OpenRune-Server` folder and allow it
  to replace files.

## 3. Install and run the server

Still in the `OpenRune-Server` folder:

```powershell
.\gradlew.bat install
.\gradlew.bat run
```

`install` downloads the game cache from OpenRS2, builds it (including the league's save
variables) and writes `game.yml`. It takes a while the first time. `run` starts the server; wait
for `OpenRune Server Successfully initialized`. The database is embedded, so there is nothing else
to install.

`game.yml` is set to `world-types: ["demonic_pacts_league"]`, a dedicated league world. To keep
normal play too, use `["main", "demonic_pacts_league"]` and switch in game with `::worldtype`.
Note that `install` rewrites `game.yml` from `game.example.yml`, so put lasting changes in
`game.example.yml`.

If the build stops with an error, copy the whole error output and send it to me.

## 4. Set up RSProx

1. Download the RSProx installer from https://github.com/blurite/rsprox/releases and run it once,
   then close it. This creates `%USERPROFILE%\.rsprox`.
2. Open `client.key` in the `OpenRune-Server\.data` folder (created by `install`) and copy the long
   value.
3. Create `%USERPROFILE%\.rsprox\proxy-targets.yaml` containing (paste your value in place of
   `YOUR_MODULUS_KEY_HERE`):

   ```yaml
   config:
     - id: 1
       name: Demonic Pacts
       jav_config_url: https://client.blurite.io/jav_local_242.ws
       varp_count: 15000
       revision: 242
       modulus: YOUR_MODULUS_KEY_HERE
   ```

4. Start RSProx, choose **Demonic Pacts** as the proxy target and **RuneLite** as the client, and
   launch. Log in with any username and password; the first login creates the account.

If the server's revision ever changes (check `revision:` in `game.yml`), change both `242`s above
to match.

## 5. Playing offline

The server never contacts the internet once installed. RSProx does fetch a few things: the
`jav_config` above, and RuneLite and the game client on first launch (cached after that). To cut
the last link, after one successful online launch:

1. Save https://client.blurite.io/jav_local_242.ws to a folder, e.g. `C:\osrs\web\jav_local_242.ws`.
2. Serve that folder locally, e.g. `python -m http.server 8080` run inside `C:\osrs\web`.
3. Change `jav_config_url` to `http://127.0.0.1:8080/jav_local_242.ws`.

This last part is untested; if RSProx still reaches out to the internet, tell me what it reports.

## 6. In game

Type `::league` to see your progress and `::pacts` to see what you can buy. The full command list
and what is and is not implemented are in `content/events/demonic-pacts-league/README.md`.

The testing commands (`::dppoints`, `::dppactpoints`, `::dpallregions`) need administrator
rights. A realm running in dev mode makes every login an administrator; otherwise, grant your
account admin rights the usual OpenRune way.
