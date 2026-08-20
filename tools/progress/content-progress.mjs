#!/usr/bin/env node
// Content progress report generator.
//
// Denominator (what OSRS has)   : osrs-dumps/dump.npc + wiki section headings
// Numerator   (what we've built): RSCM references inside content/**
// Verified    (what's proven)   : tests under content/**/src/{test,integration}
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

const RED = '🔴', YELLOW = '🟡', GREEN = '🟢', GREY = '⚪'

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

/** Every NPC symbol OSRS has. dump.npc is the only complete name index in-repo. */
function loadNpcSymbols() {
  const file = P('osrs-dumps', 'dump.npc')
  if (!fs.existsSync(file)) return []
  const out = []
  for (const m of fs.readFileSync(file, 'utf8').matchAll(/^\[([a-z0-9_]+)\]$/gm)) out.push(m[1])
  return out
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

/** Which RSCM symbols content/** actually references. */
function scanReferences() {
  const files = walk(P('content'), (n) => n.endsWith('.kt') || n.endsWith('.toml'))
  const refs = new Map()
  for (const f of files) {
    const text = fs.readFileSync(f, 'utf8')
    for (const m of text.matchAll(RSCM_RE)) {
      const key = m[1] + '.' + m[2]
      if (!refs.has(key)) refs.set(key, new Set())
      refs.get(key).add(rel(f))
    }
  }
  return refs
}

// ---------------------------------------------------------------- wiki

const WIKI_CACHE = P('tools', 'progress', 'wiki-cache.json')

function loadWikiCache() {
  if (!fs.existsSync(WIKI_CACHE)) return {}
  try { return JSON.parse(fs.readFileSync(WIKI_CACHE, 'utf8')) } catch { return {} }
}

/**
 * Seed mechanic sub-nodes from wiki section headings. These are the features
 * with no entity footprint (run energy, formulas) that the cache cannot see.
 */
async function fetchWikiSections(pages) {
  const cache = loadWikiCache()
  for (const page of pages) {
    const url = 'https://oldschool.runescape.wiki/api.php?action=parse&page='
      + encodeURIComponent(page) + '&prop=sections&format=json'
    try {
      const res = await fetch(url, { headers: { 'User-Agent': 'OpenRune-Server progress report' } })
      const json = await res.json()
      const sections = (json?.parse?.sections ?? []).filter((s) => s.toclevel <= 2).map((s) => s.line)
      cache[page] = { sections, fetched: new Date().toISOString().slice(0, 10) }
      process.stderr.write('  wiki: ' + page + ' -> ' + sections.length + ' sections\n')
    } catch (err) {
      process.stderr.write('  wiki: ' + page + ' FAILED (' + err.message + ')\n')
    }
  }
  fs.writeFileSync(WIKI_CACHE, JSON.stringify(cache, null, 2) + '\n')
  return cache
}

// ---------------------------------------------------------------- scoring

function scoreFeature(feature, ctx) {
  const { modules, refs, npcSymbols, wiki } = ctx
  const subModules = feature.module
    ? modules.filter((m) => m.path === feature.module || m.path.startsWith(feature.module + '/'))
    : []
  const mod = subModules[0] ?? null
  const loc = subModules.reduce((n, m) => n + m.loc, 0)
  const tests = subModules.reduce((n, m) => n + m.tests, 0)
  const lastCommit = subModules.map((m) => m.lastCommit).filter(Boolean).sort().pop() ?? null

  // Entity denominator: NPC symbols matching the feature's prefixes.
  let denom = 0, covered = 0
  if (feature.rscmPrefixes?.length) {
    const matched = npcSymbols.filter((s) => feature.rscmPrefixes.some((p) => s.startsWith(p)))
    denom = matched.length
    covered = matched.filter((s) => refs.has('npc.' + s)).length
  }
  // Content-tag denominator: is each tag handled anywhere in content/?
  let tagDenom = 0, tagCovered = 0
  if (feature.contentTags?.length) {
    tagDenom = feature.contentTags.length
    tagCovered = feature.contentTags.filter((t) => refs.has('content.' + t)).length
  }

  let status, note
  if (feature.engine) {
    status = GREY
    note = 'engine-owned (`' + feature.engine + '`)'
  } else if (!mod) {
    status = RED
    note = 'no module'
  } else if (loc === 0) {
    status = RED
    note = 'stub — 0 lines, last touched ' + (lastCommit ?? 'never')
  } else {
    const parts = [loc.toLocaleString() + ' loc']
    if (tagDenom) parts.push(tagCovered + '/' + tagDenom + ' tags')
    if (denom) parts.push(covered + '/' + denom + ' npcs')
    const complete = (!tagDenom || tagCovered === tagDenom) && (!denom || covered === denom)
    if (complete && tests > 0) { status = GREEN; parts.push(tests + ' tests') }
    else if (tests === 0) { status = YELLOW; parts.push('no tests') }
    else { status = YELLOW; parts.push(tests + ' tests') }
    note = parts.join(' · ')
  }

  return {
    ...feature,
    status, note, loc, tests, denom, covered, tagDenom, tagCovered,
    sections: (wiki[feature.wiki]?.sections ?? []).filter((s) => !isNoise(s)),
    moduleExists: !!mod,
  }
}

// ---------------------------------------------------------------- rendering

function summarise(scored) {
  const all = scored.flatMap((c) => c.features).filter((f) => f.status !== GREY)
  const green = all.filter((f) => f.status === GREEN).length
  const yellow = all.filter((f) => f.status === YELLOW).length
  const red = all.filter((f) => f.status === RED).length
  const pct = all.length ? Math.round(((green + yellow * 0.5) / all.length) * 100) : 0
  return { green, yellow, red, total: all.length, pct }
}

function wikiUrl(title) {
  return 'https://oldschool.runescape.wiki/w/' + encodeURIComponent(title.replace(/ /g, '_'))
}

function renderTables(categories) {
  const L = []
  for (const cat of categories) {
    L.push('### ' + cat.name)
    L.push('')
    L.push('| | Feature | Status | |')
    L.push('|---|---|---|---|')
    for (const f of cat.features) {
      const name = f.moduleExists ? '[' + f.name + '](' + f.module + ')' : f.name
      const link = f.wiki ? '[wiki](' + wikiUrl(f.wiki) + ')' : ''
      L.push('| ' + f.status + ' | ' + name + ' | ' + f.note + ' | ' + link + ' |')
    }
    L.push('')
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
const refs = scanReferences()
const npcSymbols = loadNpcSymbols()

let wiki = loadWikiCache()
if (args.has('--fetch-wiki')) {
  const pages = spec.categories.flatMap((c) => c.features.map((f) => f.wiki)).filter(Boolean)
  process.stderr.write('fetching ' + pages.length + ' wiki pages...\n')
  wiki = await fetchWikiSections(pages)
}

const ctx = { modules, refs, npcSymbols, wiki }
const categories = spec.categories.map((c) => ({
  name: c.name,
  features: c.features.map((f) => scoreFeature(f, ctx)),
}))
const summary = summarise(categories)

const legend = GREEN + ' implemented & tested · ' + YELLOW + ' partial or untested · ' + RED + ' missing or stub · ' + GREY + ' engine-owned'
const headline = '**' + summary.pct + '% coverage** — ' + GREEN + ' ' + summary.green + ' done · '
  + YELLOW + ' ' + summary.yellow + ' partial · ' + RED + ' ' + summary.red + ' missing, of '
  + summary.total + ' tracked features'

fs.writeFileSync(P('PROGRESS.md'), [
  '# Content progress',
  '',
  '_Generated by `tools/progress/content-progress.mjs` — do not edit by hand._',
  '',
  'What OSRS has comes from `osrs-dumps/dump.npc` (' + npcSymbols.length.toLocaleString() + ' npcs) and wiki',
  'section headings. What we have comes from the ' + refs.size.toLocaleString() + ' RSCM symbols referenced',
  'across `content/`. ' + GREEN + ' also needs tests.',
  '',
  legend,
  '',
  headline,
  '',
  renderTables(categories),
  renderMechanics(categories),
  renderModules(modules),
].join('\n') + '\n')

const readmeBlock = [
  '',
  '## 📊 Content progress',
  '',
  headline,
  '',
  legend,
  '',
  renderTables(categories.filter((c) => c.name === 'Skills')),
  'See **[PROGRESS.md](PROGRESS.md)** for bosses, every content module, and untracked mechanics.',
  '',
  'Want to help? Grab a ' + RED + ' row, nobody is on those.',
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

process.stderr.write('PROGRESS.md written — ' + summary.pct + '% ('
  + summary.green + ' green / ' + summary.yellow + ' yellow / ' + summary.red + ' red of ' + summary.total + ')\n')
