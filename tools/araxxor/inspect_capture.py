"""Summarize capture evidence without extracting or importing foreign cache assets."""
import argparse
import collections
import hashlib
import json
from pathlib import Path
import zipfile


def inspect(pack, bridge):
    snapshot = json.loads(bridge.read_text(encoding="utf-8"))
    with zipfile.ZipFile(pack) as archive:
        entry = archive.getinfo("observations/events.ndjson")
        if entry.file_size > 32 * 1024 * 1024:
            raise ValueError("Observation file exceeds the inspection limit")
        events = [json.loads(line) for line in archive.read(entry).splitlines() if line.strip()]
    actors = [e for e in snapshot["entities"] if e["type"] == "npc" and 13668 <= e["id"] <= 13675]
    boss_animations = [dict(tick=e["tick"], animation=int(e["attributes"]["animation"]))
        for e in events if e["type"] == "ANIMATION_CHANGED" and e.get("entityId") == 13668]
    first_spawn = {}
    for event in events:
        if event["type"] == "NPC_SPAWNED":
            key = (event.get("entityId"), event.get("entityIndex"))
            first_spawn.setdefault(key, event["tick"])
    retrospective = [dict(npc=e["entityId"], index=e["entityIndex"], spawnedAt=first_spawn[key])
        for e in events if e["type"] == "NPC_INITIAL_STATE"
        and (key := (e.get("entityId"), e.get("entityIndex"))) in first_spawn
        and e["tick"] < first_spawn[key]]
    return {
        "packSha256": hashlib.sha256(pack.read_bytes()).hexdigest(),
        "bridgeSha256": hashlib.sha256(bridge.read_bytes()).hexdigest(),
        "source": snapshot["source"],
        "eventCount": len(events),
        "eventTypes": dict(collections.Counter(e["type"] for e in events)),
        "snapshotActors": [dict(npc=e["id"], templateX=int(e["attributes"]["templateWorldX"]),
            templateY=int(e["attributes"]["templateWorldY"]), plane=int(e["attributes"]["templatePlane"])) for e in actors],
        "bossAnimations": boss_animations,
        "retrospectiveInitialStates": retrospective,
        "laterInitialStates": [dict(npc=e["entityId"], tick=e["tick"]) for e in events
            if e["type"] == "NPC_INITIAL_STATE" and e["tick"] > 0],
        "limitations": [
            "Foreign-server observation, not proof of OSRS mechanics.",
            "Snapshot contains only seven eggs; not a complete nine-egg placement reference.",
            "Initial-state rows after tick zero represent later actors, not the initial egg layout.",
            "NPC template coordinates and terrain coordinates differ by actor footprint; do not use one global offset.",
            "Projectile 1560 includes player attacks and must not be assigned to Araxxor.",
        ],
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("pack", type=Path)
    parser.add_argument("bridge", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    result = inspect(args.pack, args.bridge)
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(f"Inspected {result['eventCount']} events; no assets imported.")


if __name__ == "__main__":
    main()
