"""Reuse the qualified cape harness for an isolated Gauntlet + cape test package."""
import json
from pathlib import Path
import sys

tools = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(tools / "nero-completionist-client"))
import build_test_package

def argument(name):
    return Path(sys.argv[sys.argv.index(name) + 1]).resolve()

def main():
    checkout = argument("--checkout")
    output = argument("--output")
    proof = json.loads((checkout / ".data/gauntlet-pack-proof.json").read_text(encoding="utf-8"))
    if proof.get("passed") is not True or proof.get("revision") != 240:
        raise RuntimeError("Bounded rev240 Gauntlet cache proof required")
    build_test_package.main()
    manifest = output / "test-package.json"
    data = json.loads(manifest.read_text(encoding="utf-8"))
    data["gauntletCacheProof"] = proof
    manifest.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")
    launcher = output / "TESTEN.ps1"
    text = launcher.read_text(encoding="utf-8").replace('name: "Completionist cape test"', 'name: "Gauntlet and completionist test"')
    text = text.replace("Gebruik ::curator en ::compcapes.", "Gebruik ::testgauntlet, ::gauntletkit en ::compcapes.")
    launcher.write_text(text, encoding="utf-8", newline="\n")
    (output / "LEESMIJ.md").write_text("""# Gauntlet en completionist cape v0.17

Start TESTEN.cmd of TESTEN.ps1. Dit pakket gebruikt een eigen testserver,
database en clientprofiel met revisie 240. De bestaande installatie wordt alleen
gelezen. Gebruik een testaccount; bestaande accounts worden niet gekopieerd.
De aparte testhelper geeft administratorrechten alleen aan testsessies.

## Gauntlet

- ::testgauntlet - naar de Prifddinas-ingang; binnen een run verlaat je de run.
- ::gauntletkit - een normale run met tier 3 uitrusting en paddlefish.
- ::gauntletkithm - dezelfde test voor de Corrupted Gauntlet.
- ::testloot gauntlet 100 / ::testloot cg 100 - echte chest-tabellen en Collection Log.
  Dit simuleert geen overwinning en verandert geen completion count, actieve run,
  bewaarde uitrusting of nog te claimen chest.

Test ook de normale ingang zonder kit: praat met Bryn, kies de ingang, ontdek
kamers, verzamel resources, maak wapens/armour/Egniol en kook paddlefish.
De voorbereiding duurt 10 minuten normaal en 7:30 corrupted. De timer en bossdeur
starten Hunllef. Controleer ranged/magic na vier aanvallen, protection na zes
aanvallen met een andere stijl (ook misses tellen), prayer disable, stomp,
tornados en vloerfasen. Controleer winst, chest, opnieuw deelnemen, dood,
vertrek en logout. Je oorspronkelijke inventory en equipment moeten terugkeren.
Een pending chest moet ook na logout blijven bestaan en slechts eenmaal belonen.

## Cape

- ::compcapes hoods - hoods; controleer hoofd/gezicht bij beide genders.
- ::compcapes dizana / ::compcapes dizana_locked - de originele gevouwen
  completionist inventory-vorm met de quiver erbovenop. Bruin/beige panelen,
  lichte Dizana V-details en de oorspronkelijke bordeaux/gouden quiver.
- ::compcapes fire / ::compcapes infernal - bewegende native textures.
- ::curator - customiser en particles.

Controleer camera draaien, lopen, idle, emotes, software en HD. Cape-verlichting
van de omgeving is geparkeerd. Native offline renders en tests zijn uitgevoerd;
ingame visuele en volledige runacceptatie staan nog open.

Stop de eigen server met TESTEN.ps1 -Action Stop en sluit de aparte client.
Prepare bereidt alleen voor; -NoClient start alleen de testserver. INSTALLEREN.ps1
is een apart, bewaakt installatiepad voor na acceptatie en is hier niet uitgevoerd
tegen de bestaande installatie.
""", encoding="utf-8", newline="\n")

if __name__ == "__main__":
    main()
