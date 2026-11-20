"""Turns the cache's Leagues VI task list into demonic_pacts_league_tasks.toml with trackers."""
import json
import re
import sys
from collections import Counter

SKILLS = [
    "attack", "defence", "strength", "hitpoints", "ranged", "prayer", "magic", "cooking",
    "woodcutting", "fletching", "fishing", "firemaking", "crafting", "smithing", "mining",
    "herblore", "agility", "thieving", "slayer", "farming", "runecraft", "hunter",
    "construction", "sailing",
]
STAT_NAME = {s: f"stat.{s}" for s in SKILLS}
STAT_NAME["runecraft"] = "stat.runecrafting"
STAT_NAME["sailing"] = "stat.sailing"

ARTICLES = ("a ", "an ", "the ", "some ", "any ", "either ", "your ", "full ", "one ")
QUALIFIERS = [
    " in the ", " in a ", " in an ", " in ", " with the ", " with a ", " with an ", " with ",
    " on ", " at the ", " at ", " inside ", " within ", " whilst ", " while ", " using ",
    " from ", " without ", " near ", " anywhere ", " for ", " during ", " after ", " by ",
    " to ", ",", " that ", " which ", " as ",
]
WORD_NUMBERS = {"a": 1, "an": 1, "some": 1, "any": 1, "one": 1}


def strip_articles(text):
    text = text.strip()
    changed = True
    while changed:
        changed = False
        for art in ARTICLES:
            if text.lower().startswith(art):
                text = text[len(art):].strip()
                changed = True
    return text


def cut_qualifiers(text):
    lower = text.lower()
    cut = len(text)
    for q in QUALIFIERS:
        i = lower.find(q)
        if i > 0:
            cut = min(cut, i)
    return text[:cut].strip(" .")


def count_from(desc):
    m = re.search(r"(\d[\d,]*) times", desc)
    if m:
        return int(m.group(1).replace(",", ""))
    return None


def parse_level(desc):
    d = desc.lower()
    m = re.match(r"reach level (\d+) in your (\w+) skill", d)
    if m and m.group(2) in STAT_NAME:
        return dict(track="level", stat=STAT_NAME[m.group(2)], value=int(m.group(1)))
    m = re.match(r"reach level (\d+) in any skill( \(not including runecraft\))?", d)
    if m:
        return dict(track="level_any", value=int(m.group(1)))
    m = re.match(r"reach level (\d+) in every skill", d)
    if m:
        return dict(track="level_all", value=int(m.group(1)))
    m = re.match(r"reach a total level of ([\d,]+)", d)
    if m:
        return dict(track="total_level", value=int(m.group(1).replace(",", "")))
    m = re.match(r"reach combat level (\d+)", d)
    if m:
        return dict(track="combat_level", value=int(m.group(1)))
    if d.startswith("level up any of your skills for the first time"):
        return dict(track="level_any", value=2)
    return None


def parse_xp(desc):
    d = desc.lower()
    m = re.match(r"obtain ([\d,.]+)( million)? experience in your (\w+) skill", d)
    if m and m.group(3) in STAT_NAME:
        value = float(m.group(1).replace(",", "")) * (1_000_000 if m.group(2) else 1)
        return dict(track="xp", stat=STAT_NAME[m.group(3)], value=int(value))
    m = re.match(r"obtain ([\d,.]+)( million)? experience in any non-combat skill", d)
    if m:
        value = float(m.group(1).replace(",", "")) * (1_000_000 if m.group(2) else 1)
        return dict(track="xp_noncombat", value=int(value))
    m = re.match(r"obtain ([\d,.]+)( million)? experience in either attack, strength, defence, magic, ranged or hitpoints", d)
    if m:
        value = float(m.group(1).replace(",", "")) * (1_000_000 if m.group(2) else 1)
        return dict(track="xp_combat", value=int(value))
    return None


def split_items(text):
    """'an A, a B and either C or D' -> [[A], [B], [C, D]] (all groups needed, any item per group)."""
    text = re.sub(r"\s+", " ", text).strip()
    if text.lower().startswith("either "):
        options = [strip_articles(cut_qualifiers(strip_articles(o))) for o in re.split(r",? or |, ", text[7:])]
        options = [o for o in options if o]
        return [options] if options else []
    groups = []
    for part in re.split(r",? and (?=(?:a|an|the|some|either|any)\b)|, (?=(?:a|an|the|some|either|any)\b)", text):
        part = part.strip()
        if not part:
            continue
        options = [strip_articles(cut_qualifiers(strip_articles(o))) for o in re.split(r",? or ", part)]
        options = [o for o in options if o]
        if options:
            groups.append(options)
    return groups


def parse_equip(desc):
    d = desc
    m = re.match(r"(?:Craft and equip|Equip|Wear) (.*)", d, re.I)
    if not m:
        return None
    rest = m.group(1).strip().rstrip(".")
    low = rest.lower()
    m2 = re.match(r"(?:a |any )?piece of (?:the |an |a )?(.*?)(?: armour| robe| robes| outfit)?(?: set)?(?: from .*)?$", low)
    if m2:
        return dict(track="equip_piece", match=m2.group(1).strip())
    m2 = re.match(r"(?:a )?full set of (?:the )?(.*?)(?: armour| robes?)?$", low) or re.match(r"full (.*?)(?: armour| robes?)?(?: set)?$", low)
    if m2:
        return dict(track="equip_set", match=m2.group(1).strip())
    if re.match(r"\d", low):
        return None
    rest = re.sub(r"^(?:a |an )?pair of ", "", rest, flags=re.I)
    groups = split_items(cut_qualifiers_keep_lists(rest))
    if not groups:
        return None
    return dict(track="equip", items=groups)


def cut_qualifiers_keep_lists(text):
    lower = text.lower()
    cut = len(text)
    for q in [" in the ", " in a ", " in ", " whilst ", " while ", " at ", " during ", " inside ", " within "]:
        i = lower.find(q)
        if i > 0:
            cut = min(cut, i)
    return text[:cut]


def singular(name):
    n = name
    if n.endswith("men"):
        return n[:-3] + "man"
    if n.endswith("ies"):
        return n[:-3] + "y"
    if n.endswith("ses") or n.endswith("xes"):
        return n[:-2]
    if n.endswith("s") and not n.endswith("ss"):
        return n[:-1]
    return n


def parse_kill(desc):
    d = desc
    count = count_from(d) or 1
    m = re.match(r"(?:Defeat|Kill|Slay) (.*)", d)
    if not m:
        m = re.match(r"Help the .*? (?:defeat|by dealing with) (.*)", d)
    if not m:
        return None
    rest = m.group(1)
    rest = re.sub(r"\b\d[\d,]* times\b", "", rest).strip()
    m2 = re.match(r"(\d[\d,]*) (.*)", rest)
    if m2:
        count = int(m2.group(1).replace(",", ""))
        rest = m2.group(2)
    rest = cut_qualifiers(strip_articles(rest))
    if not rest or rest.lower() in ("any", "monster", "monsters"):
        return None
    names = []
    for part in re.split(r",? or | and ", rest):
        part = strip_articles(part).strip().lower()
        if part:
            names.append(part)
            s = singular(part)
            if s != part:
                names.append(s)
    return dict(track="kill", npcs=sorted(set(names)), count=count)


ACTIONS = {
    "chop": ("gather", "stat.woodcutting"),
    "mine": ("gather", "stat.mining"),
    "catch": ("gather", "stat.fishing"),
    "fish": ("gather", "stat.fishing"),
    "cook": ("cook", None),
    "smelt": ("smelt", None),
    "make": ("make", None),
    "craft": ("craft", None),
    "clean": ("clean", None),
    "steal": ("steal", None),
    "thieve": ("steal", None),
    "pickpocket": ("pickpocket", None),
    "bury": ("bury", None),
    "smith": ("smith", None),
}


def parse_action(desc):
    d = desc
    m = re.match(r"Use a Furnace to smelt (.*)", d, re.I)
    m_anvil = re.match(r"Use an Anvil to smith (.*)", d, re.I)
    if m:
        verb, rest = "smelt", m.group(1)
    elif m_anvil:
        verb, rest = "smith", m_anvil.group(1)
    else:
        m = re.match(r"(\w+) (.*)", d)
        if not m:
            return None
        verb, rest = m.group(1).lower(), m.group(2)
    count = count_from(rest) or 1
    rest = re.sub(r",? \d[\d,]* times", "", rest)
    if verb == "burn":
        low = rest.lower()
        if "food" in low:
            return dict(track="action", action="burn_food", items=["*"], count=1)
        if "log" in low and "firelighter" not in low:
            m2 = re.match(r"(\d[\d,]*) (.*)", rest)
            if m2:
                count = int(m2.group(1).replace(",", ""))
                rest = m2.group(2)
            item = cut_qualifiers(strip_articles(rest))
            if item.lower() in ("logs", "kind of logs"):
                item = "*logs"
            if item.lower() == "normal logs":
                item = "logs"
            return dict(track="action", action="burn_log", items=[item.lower()], count=count)
        return None
    if verb not in ACTIONS:
        return None
    action, stat = ACTIONS[verb]
    if verb == "catch" and "fishing" not in desc.lower() and not re.search(r"raw ", desc.lower()) and "barehand" not in desc.lower():
        return None
    if verb == "pickpocket":
        m2 = re.match(r"(.*?)(?: (\d[\d,]*) times)?$", cut_qualifiers(strip_articles(rest)))
        names = []
        for part in re.split(r" or ", m2.group(1)):
            part = strip_articles(part).lower()
            if part:
                names += [part, singular(part)]
        return dict(track="action", action="pickpocket", items=sorted(set(names)), count=count)
    if verb in ("steal", "thieve"):
        low = rest.lower()
        m2 = re.match(r"from (?:the |a |an )?(.*)", low)
        if m2:
            return dict(track="action", action="steal_from", items=[cut_qualifiers(m2.group(1))], count=count)
    m2 = re.match(r"(\d[\d,]*) (.*)", rest)
    if m2:
        count = int(m2.group(1).replace(",", ""))
        rest = m2.group(2)
    item = cut_qualifiers(strip_articles(rest)).lower()
    if not item:
        return None
    if item in ("kind of logs", "logs", "ore", "any ore", "kind of ore"):
        item = "*" + ("logs" if "log" in item else "ore")
    if verb == "bury":
        if "bone" in item:
            item = "*bones" if item.startswith(("bones", "6 bones", "bones of any")) else item
    items = []
    for part in re.split(r",? or ", item):
        part = strip_articles(part).strip()
        if part == "normal logs":
            part = "logs"
        if part:
            items += [part, singular(part)]
    if action in ("make",):
        items = [re.sub(r"\(\d\)", "", i).strip() for i in items]
    return dict(track="action", action=action, stat=stat, items=sorted(set(items)), count=count)


def parse_obtain(desc):
    m = re.match(r"Obtain (.*)", desc)
    if not m:
        return None
    rest = m.group(1)
    if re.search(r"collection log|experience|points|glory|unique", rest, re.I):
        return None
    count = 1
    m2 = re.match(r"(\d[\d,]*) (.*)", rest)
    if m2:
        count = int(m2.group(1).replace(",", ""))
        rest = m2.group(2)
    groups = split_items(cut_qualifiers_keep_lists(rest))
    if not groups or len(groups) != 1:
        return None
    return dict(track="obtain", items=groups, count=count)


def parse_quest(desc):
    m = re.match(r"Complete (?:the )?(.*?) (?:quest|miniquest)$", desc, re.I)
    if m:
        return dict(track="quest", quest=m.group(1).strip())
    return None


SPECIAL = {
    "Open the Leagues Menu found within the Journal Panel": dict(track="event", event="open_league_menu"),
    "Complete the Leagues Tutorial and begin your adventure": dict(track="event", event="login"),
    "Switch to the Lunar Spellbook": dict(track="spellbook", value=2),
    "Turn off your run": dict(track="event", event="run_off"),
}


REGION_PHRASE = re.compile(
    r"\s*(?:anywhere )?(?:in|within|on) (?:the )?(?:varlamore|wilderness|karamja|asgarnia|kandarin|"
    r"morytania|tirannwn|kourend|kebos|fremennik provinces?|kharidian desert|desert|misthalin)"
    r"(?: underground)?\b",
    re.I,
)
HARMLESS_PHRASES = re.compile(
    r"\s*(?:whilst|while) fishing|\s*while trying to cook it|\s*from essence(?: at the [\w ]+? altar)?|"
    r",? at any dosage|\s*whilst (?:cutting down trees|mining a rock)",
    re.I,
)
CONSTRAINT = re.compile(
    r"\b(?:using|with|wielding|wearing|at|in|inside|around|near|while|whilst|without|during|on|"
    r"from the|within|before|after|under|seconds?|in a row|same time|final hit)\b",
    re.I,
)
TRACKED_WITH_CONSTRAINTS = ("action", "kill", "obtain")
# A named boss only lives in its lair, so "Defeat the Kraken boss in Kraken Cove" names no extra
# condition; plain monsters ("a Fire Giant in the Wilderness") still need the place checked.
NAMED_BOSS = re.compile(r"^(?:[Dd]efeat|[Kk]ill) (?:the )?[A-Z]")
BOSS_LAIR = re.compile(r" (?:in|at|on) (?:the )?[A-Z][\w' -]*?(?= \d[\d,]* times$|$)")


def unverifiable(desc, track):
    """Whether the task asks for something the server can't check (a tool, a place, a spell)."""
    if track not in TRACKED_WITH_CONSTRAINTS:
        return False
    rest = HARMLESS_PHRASES.sub("", REGION_PHRASE.sub("", desc))
    if track == "kill" and NAMED_BOSS.match(rest):
        rest = BOSS_LAIR.sub("", rest)
    if track == "action" and re.match(r"(steal|thieve) from ", rest, re.I):
        rest = re.sub(r"^(steal|thieve) from ", "", rest, flags=re.I)
    if re.match(r"(use a furnace|use an anvil) to ", rest, re.I):
        rest = re.sub(r"^use an? \w+ to ", "", rest, flags=re.I)
    return CONSTRAINT.search(rest) is not None


def classify(task):
    result = classify_text(task)
    if result and unverifiable(task["desc"], result["track"]):
        return None
    return result


def classify_text(task):
    d = task["desc"]
    if d in SPECIAL:
        return SPECIAL[d]
    for parser in (parse_level, parse_xp, parse_quest):
        r = parser(d)
        if r:
            return r
    low = d.lower()
    if low.startswith(("equip", "craft and equip", "wear")):
        return parse_equip(d)
    if low.startswith(("defeat", "kill", "slay", "help the")):
        return parse_kill(d)
    if low.startswith("obtain"):
        return parse_obtain(d)
    return parse_action(d)


def toml_str(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def toml_value(v):
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, str):
        return toml_str(v)
    if isinstance(v, list):
        return "[" + ", ".join(toml_value(x) for x in v) + "]"
    raise TypeError(v)


def existing_counters(path):
    """Task index -> counter varp from an earlier output, so saved kill counts keep their varp."""
    counters = {}
    index = None
    try:
        lines = open(path).read().splitlines()
    except FileNotFoundError:
        return counters
    for line in lines:
        m = re.match(r"index = (\d+)", line)
        if m:
            index = int(m.group(1))
        m = re.match(r'counter = "varp.dp_task_count_(\d+)"', line)
        if m and index is not None:
            counters[index] = int(m.group(1))
    return counters


def main(src, dst):
    tasks = json.load(open(src))
    previous = existing_counters(dst)
    next_counter = max(previous.values(), default=-1) + 1
    out = ["# Generated from the cache's Leagues VI task list (enum_5950, dbtable action).",
           "# index = the task's bit in varp league_task_completed_<index / 32>.",
           "# tier = action_difficulty: 1 easy, 2 medium, 3 hard, 4 elite, 5 master.",
           "# track = how the server notices completion; \"manual\" tasks need ::taskdone.", ""]
    stats = Counter()
    counters = []
    for t in tasks:
        tr = None
        try:
            tr = classify(t)
        except Exception as e:  # noqa: BLE001
            print("parse error", t["desc"], e, file=sys.stderr)
        tr = tr or dict(track="manual")
        if tr.get("count", 1) > 1 and tr["track"] in ("kill", "action"):
            number = previous.get(t["index"])
            if number is None:
                number = next_counter
                next_counter += 1
            tr["counter"] = f"varp.dp_task_count_{number}"
            counters.append(t["index"])
        stats[tr["track"]] += 1
        out.append("[[task]]")
        out.append(f"index = {t['index']}")
        out.append(f"name = {toml_str(t['name'])}")
        out.append(f"desc = {toml_str(t['desc'])}")
        out.append(f"tier = {t['diff']}")
        out.append(f"area = {t['area']}")
        for k, v in tr.items():
            if v is None:
                continue
            if k == "items" and v and isinstance(v[0], list):
                out.append(f"items = {toml_value([' | '.join(g) for g in v])}")
            else:
                out.append(f"{k} = {toml_value(v)}")
        out.append("")
    open(dst, "w").write("\n".join(out))
    json.dump(counters, open("counters.json", "w"))
    print("counters", len(counters))
    for k, v in stats.most_common():
        print(k, v)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
