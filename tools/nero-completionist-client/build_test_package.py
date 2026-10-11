"""Build a guarded local test/deployment package. Reads the accepted installation; never writes it."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import zipfile

TOOLS = Path(__file__).resolve().parent

def sha(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()

def mappings(root):
    for folder in ("api", "content"):
        for path in sorted((root/folder).rglob("gamevals.toml")):
            if not {"build", "out", "target"}.intersection(path.relative_to(root).parts): yield path

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--checkout", type=Path, required=True)
    parser.add_argument("--target", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--baseline", type=Path, required=True)
    parser.add_argument("--installer-template", type=Path, required=True)
    args = parser.parse_args()
    checkout = args.checkout.resolve(strict=True); target = args.target.resolve(strict=True)
    output = args.output.resolve(); baseline = json.loads(args.baseline.read_text())
    if output.exists(): raise RuntimeError("Use a new empty output directory")
    if output == target or target in output.parents or checkout in output.parents: raise RuntimeError("Package must be outside checkout and live installation")
    original = target/"server/app/build/libs/server.jar"
    if sha(original) != baseline["acceptedServerSha256"]: raise RuntimeError("Accepted server changed")
    for relative, expected in baseline["acceptedSources"].items():
        if sha(checkout/relative) != expected: raise RuntimeError("Accepted source changed: " + relative)
    proof = json.loads((checkout/".data/nero-completionist-pack-proof.json").read_text())
    if proof.get("passed") is not True or proof.get("revision") != 240: raise RuntimeError("Native cache proof required")
    output.mkdir(parents=True)
    payload = output/"payload"; payload.mkdir()
    files = []
    def entry(relative, source, test_relative=None):
        old = target/relative
        before = sha(old) if old.is_file() else "MISSING"
        after = sha(source)
        if before == after: return
        destination = payload/relative; destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)
        files.append(dict(id=f"{len(files):04d}", relative=relative, testRelative=test_relative or relative,
                          target=str(old), payload=destination.relative_to(output).as_posix(), beforeSha256=before, afterSha256=after))
    # The accepted private Studio pack is not published as new feature source. Preserve its exact entries.
    jar = output/"packaged-server.jar"
    preserved = {}
    candidate = checkout/"server/app/build/libs/server.jar"
    with zipfile.ZipFile(original) as old, zipfile.ZipFile(candidate) as new, zipfile.ZipFile(jar, "w", zipfile.ZIP_DEFLATED) as packed:
        names = {name for name in old.namelist() if name.startswith("org/rsmod/content/devtools/nerostudio/")}
        for item in new.infolist():
            if item.filename not in names: packed.writestr(item, new.read(item.filename))
        for name in sorted(names):
            data = old.read(name); packed.writestr(old.getinfo(name), data)
            preserved[name] = hashlib.sha256(data).hexdigest()
    entry("server/app/build/libs/server.jar", jar, "server.jar")
    jar.unlink()
    for folder in (".data/cache", ".data/gamevals", ".data/gamevals-binary"):
        for file in sorted((checkout/folder).rglob("*")):
            if file.is_file(): entry(file.relative_to(checkout).as_posix(), file)
    for file in mappings(checkout): entry(file.relative_to(checkout).as_posix(), file)
    base_files = []
    base_paths = set(mappings(target))
    for folder in (".data/cache", ".data/gamevals", ".data/gamevals-binary", "plugins"):
        base_paths.update(p for p in (target/folder).rglob("*") if p.is_file())
    for relative in (".data/game.key", ".data/client.key", ".data/nero-studio/world-edits.json"):
        file = target/relative
        if file.is_file(): base_paths.add(file)
    for file in sorted(base_paths):
        relative = file.relative_to(target).as_posix()
        base_files.append(dict(relative=relative, testRelative=relative, sha256=sha(file)))
    server_sha = next(file["afterSha256"] for file in files if file["testRelative"] == "server.jar")
    manifest = dict(serverRoot=str(target), serverSha256=server_sha, files=files, baseFiles=base_files,
                    cacheProof=proof, preservedNeroEntries=preserved, rawCandidateSha256=sha(candidate))
    (output/"test-package.json").write_text(json.dumps(manifest, indent=2))
    install = dict(serverRoot=str(target), allowedRoots=[str(target)], files=files,
                   prerequisites=[dict(target=str(target/file["relative"]), sha256=file["sha256"]) for file in base_files if file["relative"] not in {e["relative"] for e in files}])
    (output/"install-manifest.json").write_text(json.dumps(install, indent=2))
    client = output/"client"
    shutil.copytree(TOOLS, client, ignore=shutil.ignore_patterns("__pycache__", "classes", "build_test_package.py", "Test-Completionist.ps1"))
    shutil.copyfile(TOOLS/"Test-Completionist.ps1", output/"TESTEN.ps1")
    text = args.installer_template.read_text(encoding="utf-8-sig").replace("Interface- en lootupdate geinstalleerd", "Completionist-update geinstalleerd")
    first_end = text.index("\n") + 1
    text = text[:first_end] + 'Import-Module (Join-Path $PSHOME "Modules/Microsoft.PowerShell.Utility/Microsoft.PowerShell.Utility.psd1")\n' + text[first_end:]
    start = text.index("$running = @(")
    end = text.index("Assert-RegularPath $backupRoot", start)
    text = text[:start] + (TOOLS/"installer_process_guard.ps1").read_text() + "\n" + text[end:]
    (output/"INSTALLEREN.ps1").write_text(text, encoding="utf-8")
    (output/"TESTEN.cmd").write_text('@echo off\r\n"%SystemRoot%\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0TESTEN.ps1"\r\npause\r\n')
    (output/"LEESMIJ.md").write_text("""# Completionist cape testpakket

Start `TESTEN.cmd` of voer `TESTEN.ps1` uit. Het pakket maakt een eigen testserver,
database en clientprofiel. De bestaande installatie blijft behouden.
Gebruik een testaccount; bestaande spelersgegevens worden niet gekopieerd.

- `::curator` — naar de Curator in Edgeville.
- `::compcapes` / `::compcapes trimmed` — reguliere/trimmed set.
- `::compcapes all` — alle 22 vaste capes + de reguliere set (24 vrije slots).
- `::compcapes recipe infernal` — drie ingrediënten voor een echte combinatie.
- `::compcapes list` — alle variantnamen; bijvoorbeeld `dizana`, `assembler_locked`.

Controleer Customise: vier kleurkanalen, particles, schaal 25–300%, presets,
cape/hood-preview, Done/Cancel en opnieuw inloggen. Controleer beide genders,
lopen, idle en emote. Controleer Ardougne/Mythical teleports, Ava Commune en
Dizana Check/Empty/ammunition. Gebruik capes op Perdu voor lock/repair/unlock.
Nieuwe broken/mangled states zijn niet draagbaar.

Stop de aparte testserver met `TESTEN.ps1 -Action Stop` en sluit het testclientvenster.
`-Action Prepare` bereidt alleen voor; `-NoClient` start alleen de testserver.

De feature is technisch getest. Native visuele acceptatie en HD-rendering moeten
nog in de client worden beoordeeld. De gewone client toont geen custom particles;
dit aparte profiel gebruikt de gekwalificeerde opt-in renderer uit het handoffpakket.

Na acceptatie: sluit client/server/launcher en voer `INSTALLEREN.ps1 -Action Install`
uit. Eerst controleren kan met `-Action Check`. `-Action Rollback` herstelt de
vorige software/cache; spelersvoortgang wordt niet teruggedraaid. De installer
bewaart en controleert hashes en originele backups en weigert gewijzigde baselines.
""", encoding="utf-8")
    print(json.dumps(dict(package=str(output), files=len(files), baseFiles=len(base_files), preservedNeroEntries=len(preserved), serverSha256=server_sha)))

if __name__ == "__main__": main()
