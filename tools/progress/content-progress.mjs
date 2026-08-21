#!/usr/bin/env node
// Content progress report generator.
//
// What OSRS has  : wiki categories for the feature lists, osrs-dumps/dump.npc for npcs
// What we've got : content/ module folders, plus gameval symbols referenced in content/
//
// Status is added / not added on purpose. Anything finer would be claiming a
// level of parity with the wiki that nothing here can actually check.
//
// Runs fully offline from committed data. `--fetch-wiki` refreshes wiki-cache.json.
//
//   node tools/progress/content-progress.mjs            # write PROGRESS.md + README block
//   node tools/progress/content-progress.mjs --check    # exit 1 if outputs are stale (CI)

import { execFileSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const args = new Set(process.argv.slice(2))
const P = (...p) => path.join(ROOT, ...p)

const START = '<!-- content-progress:start -->'
const END = '<!-- content-progress:end -->'

// RSCM symbol namespaces that appear in content code and data.
const NS = ['npc', 'loc', 'obj', 'content', 'varbit', 'varp', 'inv', 'seq', 'spotanim', 'param', 'enum', 'dbtable', 'dbrow', 'component', 'interface', 'timer', 'queue', 'headbar', 'stat', 'category']
const RSCM_RE = new RegExp('\\b(' + NS.join('|') + ')\\.([a-z0-9_]+)', 'g')

const RED = '🔴', GREEN = '🟢'

// Wiki section headings that are article furniture, not game features.
const SECTION_NOISE = new Set([
  'changes', 'trivia', 'see also', 'references', 'gallery', 'history', 'notes',
  'external links', 'update history', 'level up messages', 'official worlds',
  'temporary boosts', 'skill choice', 'quests', 'quests requiring agility',
  'full list', 'general overview', 'other', 'sources', 'music', 'achievements',
])
const isNoise = (s) => SECTION_NOISE.has(s.toLowerCase())
  || /^(quests|diaries)\b/i.test(s)
  || /\b(experience|xp) (table|rates)$/i.test(s)

// ---------------------------------------------------------------- utilities

function walk(dir, filter, out = []) {
  let entries
  try { entries = fs.readdirSync(dir, { withFileTypes: true }) } catch { return out }
  for (const e of entries) {
    const full = path.join(dir, e.name)
    if (e.isDirectory()) {
      if (e.name === 'build' || e.name === '.git') continue
      walk(full, filter, out)
    } else if (filter(e.name)) {
      out.push(full)
    }
  }
  return out
}

const rel = (p) => path.relative(ROOT, p).split(path.sep).join('/')

function gitLastCommit(dir) {
  try {
    return execFileSync('git', ['log', '-1', '--format=%ad', '--date=short', '--', dir],
      { cwd: ROOT, encoding: 'utf8' }).trim() || null
  } catch { return null }
}

// ---------------------------------------------------------------- denominator

/**
 * Every NPC symbol OSRS has, plus a display-name index. dump.npc is the only
 * complete name index in the repo. Locs are not dumped anywhere (311 symbols
 * committed out of 62,400 ids), which is why obstacle-level detection for
 * things like agility courses is not possible yet.
 */
function loadNpcs() {
  const file = P('osrs-dumps', 'dump.npc')
  if (!fs.existsSync(file)) return { symbols: [], byName: new Map() }
  const symbols = []
  const byName = new Map()
  let current = null
  for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const sym = line.match(/^\[([a-z0-9_]+)\]$/)
    if (sym) { current = sym[1]; symbols.push(current); continue }
    const name = line.match(/^name=(.+)$/)
    if (!name || !current) continue
    const key = name[1].trim().toLowerCase()
    if (!byName.has(key)) byName.set(key, [])
    byName.get(key).push(current)
    current = null
  }
  return { symbols, byName }
}

// ---------------------------------------------------------------- numerator

/** Every content module, using the same rule settings.gradle.kts uses. */
function listModules() {
  const builds = walk(P('content'), (n) => n === 'build.gradle.kts')
  return builds.map((b) => {
    const dir = path.dirname(b)
    const kt = walk(path.join(dir, 'src'), (n) => n.endsWith('.kt'))
    const sep = path.sep
    const main = kt.filter((f) => f.includes(sep + 'main' + sep))
    const tests = kt.filter((f) => f.includes(sep + 'test' + sep) || f.includes(sep + 'integration' + sep))
    let loc = 0, todo = 0
    for (const f of main) {
      const text = fs.readFileSync(f, 'utf8')
      loc += text.split('\n').length
      if (/\bTODO\b|\bFIXME\b/.test(text)) todo++
    }
    return {
      path: rel(dir),
      name: rel(dir).replace(/^content\//, ''),
      files: main.length,
      loc,
      todo,
      tests: tests.length,
      lastCommit: gitLastCommit(rel(dir)),
    }
  }).sort((a, b) => a.path.localeCompare(b.path))
}

/**
 * Drop tables, keyed by normalised name. A boss with a table but no module has
 * had real work done on it, and reporting that as a flat "nothing here" hides it.
 */
function loadDropTables() {
  const files = walk(P('content', 'drops', 'src'), (n) => n.endsWith('.toml') || n.endsWith('DropTable.kt'))
  return new Set(files.map((f) => norm(path.basename(f).replace(/DropTable\.kt$|\.toml$/, ''))))
}

/**
 * Files that mention a symbol without implementing it. Every boss has a drop
 * table and a collection log entry long before anyone scripts the fight, so
 * counting these marks Cerberus and Corporeal Beast as done when they are not.
 */
const NOT_IMPLEMENTATION = /^content\/drops\/|collection-log\/.*gamevals\.toml$/

/**
 * A file that does something with an npc rather than just naming it. Without
 * this, a blocklist counts as an implementation: BraceletOfSlaughter.kt lists
 * tzkalzuk and tztokjad only to exclude them, which marked both as done.
 */
const BEHAVIOUR = /\bon[A-Z][A-Za-z0-9]*\(|PluginScript\b|\bencounter\(/

/** RSCM symbols referenced by content/ files that actually do something with them. */
function scanReferences() {
  const files = walk(P('content'), (n) => n.endsWith('.kt') || n.endsWith('.toml'))
  const implRefs = new Map()
  for (const f of files) {
    const text = fs.readFileSync(f, 'utf8')
    const name = rel(f)
    if (NOT_IMPLEMENTATION.test(name) || !BEHAVIOUR.test(text)) continue
    for (const m of text.matchAll(RSCM_RE)) {
      const key = m[1] + '.' + m[2]
      if (!implRefs.has(key)) implRefs.set(key, new Set())
      implRefs.get(key).add(name)
    }
  }
  return implRefs
}

// ---------------------------------------------------------------- wiki

const WIKI_CACHE = P('tools', 'progress', 'wiki-cache.json')

function loadWikiCache() {
  if (!fs.existsSync(WIKI_CACHE)) return {}
  try { return JSON.parse(fs.readFileSync(WIKI_CACHE, 'utf8')) } catch { return {} }
}

const API = 'https://oldschool.runescape.wiki/api.php'
const UA = { 'User-Agent': 'OpenRune-Server progress report' }

async function api(params) {
  const res = await fetch(API + '?' + new URLSearchParams({ ...params, format: 'json' }), { headers: UA })
  return res.json()
}

/** Everything in a wiki category, minus index and meta pages. */
async function fetchCategory(cat, exclude = []) {
  const json = await api({ action: 'query', list: 'categorymembers', cmtitle: cat, cmlimit: '500', cmnamespace: '0' })
  const base = cat.replace('Category:', '')
  const skip = new Set([base, base.replace(/es$/, ''), base.replace(/s$/, ''), ...exclude])
  return (json?.query?.categorymembers ?? [])
    .map((m) => m.title)
    .filter((t) => !skip.has(t))
    .sort()
}

/** Article images, 50 titles per request. Skills use their icon file instead. */
async function fetchImages(titles) {
  const out = {}
  for (let i = 0; i < titles.length; i += 50) {
    const batch = titles.slice(i, i + 50)
    const json = await api({
      action: 'query', titles: batch.join('|'),
      prop: 'pageimages', piprop: 'thumbnail', pithumbsize: '48',
    })
    for (const p of Object.values(json?.query?.pages ?? {})) {
      if (p.thumbnail?.source) out[p.title] = p.thumbnail.source
    }
  }
  return out
}

async function fetchSkillIcons(names) {
  const out = {}
  const titles = names.map((n) => 'File:' + n + ' icon.png')
  const json = await api({ action: 'query', titles: titles.join('|'), prop: 'imageinfo', iiprop: 'url' })
  for (const p of Object.values(json?.query?.pages ?? {})) {
    const name = String(p.title).replace(/^File:/, '').replace(/ icon\.png$/, '')
    if (p.imageinfo?.[0]?.url) out[name] = p.imageinfo[0].url
  }
  return out
}

/**
 * Section headings, used as sub-features. Most of this has no entity footprint
 * (run energy, formulas) so the symbol scan cannot see it.
 */
async function fetchSections(page) {
  const json = await api({ action: 'parse', page, prop: 'sections' })
  return (json?.parse?.sections ?? []).filter((s) => s.toclevel <= 2).map((s) => s.line)
}

// ---------------------------------------------------------------- scoring

const norm = (s) => s.toLowerCase().replace(/[^a-z0-9]/g, '')

/**
 * Match a wiki-titled feature to a module directory. "General Graardor" finds
 * content/bosses/graardor because the module name is a substring of the
 * normalised title. Anything that fails gets an entry in features.json aliases.
 */
function findModule(title, moduleRoot, modules, aliases) {
  const wanted = aliases[title] ? norm(aliases[title]) : null
  const candidates = modules.filter((m) => m.path.startsWith(moduleRoot + '/'))
  const leaf = (m) => norm(m.path.split('/').pop())
  if (wanted) return candidates.find((m) => leaf(m) === wanted) ?? null
  const t = norm(title)
  return candidates.find((m) => leaf(m) === t)
    ?? candidates.find((m) => t.includes(leaf(m)) && leaf(m).length >= 4)
    ?? null
}

function scoreFeature(feature, ctx) {
  const { modules, implRefs, npcSymbols, wiki } = ctx
  const subModules = feature.module
    ? modules.filter((m) => m.path === feature.module || m.path.startsWith(feature.module + '/'))
    : []
  const mod = subModules[0] ?? null
  const loc = subModules.reduce((n, m) => n + m.loc, 0)
  const lastCommit = subModules.map((m) => m.lastCommit).filter(Boolean).sort().pop() ?? null

  // Symbol matching, so a feature counts as added wherever it was written.
  // Prefixes are gamevals rather than display names: "rooftops_" appears in
  // code verbatim, "Draynor Village Rooftop Course" never does.
  let denom = 0, covered = 0
  const foundIn = new Set()
  if (feature.rscmPrefixes?.length) {
    const matched = npcSymbols.filter((s) => feature.rscmPrefixes.some((p) => s.startsWith(p)))
    denom = matched.length
    for (const s of matched) {
      const files = implRefs.get('npc.' + s)
      if (!files) continue
      covered++
      for (const f of files) foundIn.add(f.split('/').slice(0, 3).join('/'))
    }
  }
  // No prefixes configured, so fall back to the npcs whose in-game name is the
  // feature name. This is what makes a boss count wherever it was written,
  // rather than only when it sits in the folder we expected.
  if (!feature.rscmPrefixes?.length) {
    const named = ctx.npcsByName.get(feature.name.toLowerCase()) ?? []
    denom = named.length
    for (const s of named) {
      const files = implRefs.get('npc.' + s)
      if (!files) continue
      covered++
      for (const f of files) foundIn.add(f.split('/').slice(0, 3).join('/'))
    }
  }

  let tagDenom = 0, tagCovered = 0
  if (feature.contentTags?.length) {
    tagDenom = feature.contentTags.length
    for (const t of feature.contentTags) {
      const files = implRefs.get('content.' + t)
      if (!files) continue
      tagCovered++
      for (const f of files) foundIn.add(f.split('/').slice(0, 3).join('/'))
    }
  }

  const hasDropTable = ctx.dropTables.has(norm(feature.name))
  const hasModule = !!mod && loc > 0
  const hasSymbols = covered > 0 || tagCovered > 0
  const added = feature.engine ? true : hasModule || hasSymbols

  // Detail goes in the note. The status stays binary because anything finer
  // would be claiming a level of parity we cannot actually check.
  // Deliberately no ratios here. "2/129 npcs" next to a 5,000 line module reads
  // as a completeness score, and that is the parity claim we are avoiding.
  const parts = []
  if (feature.engine) parts.push('in `' + feature.engine + '`')
  if (hasModule) parts.push(loc.toLocaleString() + ' loc')
  if (!hasModule && hasSymbols) parts.push('no module, code in ' + [...foundIn].slice(0, 2).join(', '))
  if (!added && mod && loc === 0) parts.push('empty module, last touched ' + (lastCommit ?? 'never'))
  if (!added && hasDropTable) parts.push('drop table done')
  if (!parts.length) parts.push(added ? 'added' : 'nothing yet')

  return {
    ...feature,
    added,
    status: added ? GREEN : RED,
    note: parts.join(' · '),
    loc, denom, covered, tagDenom, tagCovered,
    sections: (wiki[feature.wiki]?.sections ?? []).filter((s) => !isNoise(s)),
    image: wiki[feature.wiki]?.image ?? null,
    moduleExists: !!mod,
    hasDropTable,
  }
}

// ---------------------------------------------------------------- rendering

function summarise(scored) {
  const all = scored.flatMap((c) => c.features)
  const added = all.filter((f) => f.added).length
  return { added, notAdded: all.length - added, total: all.length }
}

function wikiUrl(title) {
  return 'https://oldschool.runescape.wiki/w/' + encodeURIComponent(title.replace(/ /g, '_'))
}

const icon = (f) => f.image ? '<img src="' + f.image + '" height="20" alt="">' : ''

function renderTables(categories) {
  const L = []
  for (const cat of categories) {
    const done = cat.features.filter((f) => f.added).length
    L.push('### ' + cat.name + ' <sup>' + done + '/' + cat.features.length + '</sup>')
    L.push('')
    // Long category-driven groups list the unstarted entries separately, so the
    // table stays readable instead of being 150 rows of "no module".
    const started = cat.features.filter((f) => f.added)
    const notStarted = cat.features.filter((f) => !f.added)
    const tabled = cat.features.length > 30 ? started : cat.features

    if (tabled.length) {
      L.push('| | Feature | Status | |')
      L.push('|---|---|---|---|')
      for (const f of tabled) {
        const label = f.moduleExists ? '[' + f.name + '](' + f.module + ')' : f.name
        const name = f.image ? icon(f) + ' ' + label : label
        const link = f.wiki ? '[wiki](' + wikiUrl(f.wiki) + ')' : ''
        L.push('| ' + f.status + ' | ' + name + ' | ' + f.note + ' | ' + link + ' |')
      }
      L.push('')
    }
    if (cat.features.length > 30 && notStarted.length) {
      // Split out the ones that already have a drop table. No fight scripted
      // yet, but the loot is done, so it isn't a cold start.
      const withDrops = notStarted.filter((f) => f.hasDropTable)
      const cold = notStarted.filter((f) => !f.hasDropTable)
      const list = (fs) => fs.map((f) => '[' + f.name + '](' + wikiUrl(f.wiki) + ')').join(' · ')
      L.push('<details>')
      L.push('<summary>' + RED + ' <b>' + notStarted.length + ' not started</b></summary>')
      L.push('')
      if (withDrops.length) {
        L.push('**Drop table already done (' + withDrops.length + ')** — needs the encounter scripting.')
        L.push('')
        L.push(list(withDrops))
        L.push('')
      }
      if (cold.length) {
        L.push('**Nothing yet (' + cold.length + ')**')
        L.push('')
        L.push(list(cold))
        L.push('')
      }
      L.push('</details>')
      L.push('')
    }
  }
  return L.join('\n')
}

function renderModules(modules) {
  const L = ['### All content modules', '']
  L.push('Found the same way `settings.gradle.kts` finds them: any dir with a `build.gradle.kts`.')
  L.push('')
  L.push('| Module | Files | Lines | Tests | TODO | Last touched |')
  L.push('|---|---:|---:|---:|---:|---|')
  for (const m of modules) {
    L.push('| `' + m.name + '` | ' + m.files + ' | ' + m.loc.toLocaleString() + ' | ' + m.tests + ' | ' + m.todo + ' | ' + (m.lastCommit ?? '—') + ' |')
  }
  L.push('')
  return L.join('\n')
}

function renderMechanics(categories) {
  const mech = categories.flatMap((c) => c.features).filter((f) => f.sections.length && f.status !== GREEN)
  if (!mech.length) return ''
  const L = ['### Sub-features and mechanics', '']
  L.push('Pulled from wiki section headings. A lot of this has no cache symbol to match against')
  L.push('(run energy, failure rates, formulas), so the entity scan cannot see it at all. Listed')
  L.push('here so it at least shows up. Nothing is ticked until someone claims it.')
  L.push('')
  for (const f of mech) {
    L.push('<details>')
    L.push('<summary>' + f.status + ' <b>' + f.name + '</b> — ' + f.sections.length + ' sub-features</summary>')
    L.push('')
    for (const s of f.sections) L.push('- [ ] ' + s)
    L.push('')
    L.push('</details>')
    L.push('')
  }
  return L.join('\n')
}

function injectReadme(block) {
  const file = P('README.md')
  const text = fs.readFileSync(file, 'utf8')
  if (!text.includes(START) || !text.includes(END)) return { text, changed: false, missing: true }
  // Keep whatever line endings the file already uses.
  const eol = text.includes('\r\n') ? '\r\n' : '\n'
  const body = (START + '\n' + block + '\n' + END).replace(/\r?\n/g, eol)
  const next = text.replace(new RegExp(START + '[\\s\\S]*?' + END), () => body)
  return { text: next, changed: next !== text, missing: false }
}

// ---------------------------------------------------------------- main

const spec = JSON.parse(fs.readFileSync(P('tools', 'progress', 'features.json'), 'utf8'))
const modules = listModules()
const implRefs = scanReferences()
const { symbols: npcSymbols, byName: npcsByName } = loadNpcs()
const dropTables = loadDropTables()

let wiki = loadWikiCache()
if (args.has('--fetch-wiki')) {
  wiki.pages ??= {}
  wiki.categories ??= {}

  for (const c of spec.categories.filter((c) => c.wikiCategory)) {
    wiki.categories[c.wikiCategory] = await fetchCategory(c.wikiCategory, c.exclude)
    process.stderr.write(c.wikiCategory + ': ' + wiki.categories[c.wikiCategory].length + '\n')
  }

  // Sub-feature lists are only worth pulling for skills; doing it for 170
  // bosses would add thousands of lines to the report for little gain.
  const skills = spec.categories.find((c) => c.name === 'Skills')?.features ?? []
  for (const f of skills) {
    wiki.pages[f.wiki] ??= {}
    wiki.pages[f.wiki].sections = await fetchSections(f.wiki)
  }

  const icons = await fetchSkillIcons(skills.map((f) => f.wiki))
  for (const [name, url] of Object.entries(icons)) {
    wiki.pages[name] ??= {}
    wiki.pages[name].image = url
  }
  process.stderr.write('skill icons: ' + Object.keys(icons).length + '/' + skills.length + '\n')

  const titles = Object.values(wiki.categories).flat()
  const images = await fetchImages(titles)
  for (const [title, url] of Object.entries(images)) {
    wiki.pages[title] ??= {}
    wiki.pages[title].image = url
  }
  process.stderr.write('article images: ' + Object.keys(images).length + '/' + titles.length + '\n')

  wiki.fetched = new Date().toISOString().slice(0, 10)
  fs.writeFileSync(WIKI_CACHE, JSON.stringify(wiki, null, 2) + '\n')
}

const pages = wiki.pages ?? {}
const aliases = spec.aliases ?? {}
const ctx = { modules, implRefs, npcSymbols, npcsByName, dropTables, wiki: pages }
const categories = spec.categories.map((c) => {
  if (c.features) return { name: c.name, features: c.features.map((f) => scoreFeature(f, ctx)) }
  // Category-driven group: every wiki entry becomes a feature, matched to a module by name.
  const members = wiki.categories?.[c.wikiCategory] ?? []
  return {
    name: c.name,
    features: members.map((title) => {
      const mod = findModule(title, c.moduleRoot, modules, aliases)
      return scoreFeature({ name: title, wiki: title, module: mod?.path }, ctx)
    }),
  }
})
const summary = summarise(categories)

const legend = GREEN + ' added · ' + RED + ' not added'
// A single percentage would weight one quest boss the same as all of Agility,
// so show it per category instead.
const headline = categories
  .map((c) => c.name + ' **' + c.features.filter((f) => f.added).length
    + '/' + c.features.length + '**')
  .join(' · ')

fs.writeFileSync(P('PROGRESS.md'), [
  '# Content progress',
  '',
  '_Generated by `tools/progress/content-progress.mjs` — do not edit by hand._',
  '',
'The feature lists come from the wiki. Whether something is added comes from `content/`,',
  'either a module for it or its gameval symbols referenced somewhere in the code, so it',
  'still counts if it was written somewhere unexpected. Added does not mean it matches the',
  'wiki, only that it exists.',
  '',
  legend,
  '',
  headline,
  '',
  'These counts only cover skills, bosses, raids and minigames. Interfaces, areas,',
  'quests, drops and travel are real work that no category above points at, so check',
  'the module table at the bottom before reading a 0 as "nothing exists".',
  '',
  renderTables(categories),
  renderMechanics(categories),
  renderModules(modules),
  '---',
  '',
  'Feature lists, images and section headings come from the [OSRS Wiki](https://oldschool.runescape.wiki/),',
  'used under [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/).',
].join('\n') + '\n')

// Headline and a link only. The readme is already long and the detail belongs
// in PROGRESS.md, this is here so people know the report exists.
const readmeBlock = [
  '',
  '## 📊 Content progress',
  '',
  headline,
  '',
  'Full breakdown in **[PROGRESS.md](PROGRESS.md)**, including every content module and',
  'the bosses that already have drop tables and only need the encounter writing.',
  '',
].join('\n')

const readme = injectReadme(readmeBlock)
if (readme.missing) {
  process.stderr.write('! README.md has no ' + START + ' marker — skipping README injection.\n')
} else if (args.has('--check')) {
  if (readme.changed) {
    process.stderr.write('README.md is stale — run the generator and commit.\n')
    process.exit(1)
  }
} else if (readme.changed) {
  fs.writeFileSync(P('README.md'), readme.text)
}

process.stderr.write('PROGRESS.md written: ' + summary.added + ' added, '
  + summary.notAdded + ' not added, of ' + summary.total + ' features\n')
