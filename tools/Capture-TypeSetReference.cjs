/* Isolated reference execution only. Never shipped in Studio or applied to the source file.
 * NODE_PATH points to the approved local Playwright installation. All page requests are blocked.
 * jsPDF/JSZip are fixed local copies of the versions requested by the reference HTML. */
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { chromium } = require('playwright');
const root = path.resolve(__dirname, '..');
const out = path.join(root, '.local/typeset/runs', new Date().toISOString().replace(/[:.]/g, '-'));
const source = path.join(root, '.reference-assets/typeset-forge-1.1/TypesetForge_1.1.html');
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
fs.mkdirSync(out, { recursive: true });
const log = { status: 'RUNNING', cases: [], errors: [], blocked: [], humanReview: 'NOT TESTED' };
const save = () => fs.writeFileSync(path.join(out, 'manifest.json'), JSON.stringify(log, null, 2));
let browser;
(async () => {
  const raw = fs.readFileSync(source);
  log.sourceSha256 = hash(raw);
  if (log.sourceSha256 !== '892ea08f0070bdfd264b8895d38f6239f32f436b68a74ab42a2ef3336e28bb63') throw Error('Unexpected source identity');
  let html = raw.toString('utf8');
  const start = html.indexOf('const DEFAULT_PREFS = {');
  const end = html.indexOf('\n};', start) + 3;
  const defaults = html.slice(start, end);
  if (start < 0 || end < start || !defaults.includes('STORYBOOK_PRESETS.traditional')) throw Error('Bootstrap patch precondition');
  html = html.slice(0, start) + html.slice(end);
  html = html.replace('function defaultStorybookConfig(){', defaults + '\nfunction defaultStorybookConfig(){');
  html = html.replace('await import("https://cdn.jsdelivr.net/npm/jspdf@2.5.2/+esm")', 'window.jspdf');
  html = html.replace('await import("https://cdn.jsdelivr.net/npm/jszip@3.10.1/+esm")', 'window.JSZip');
  const hook = '\nwindow.__tsReference={state,canvas,ctx,defaultText,textMetrics,textContentRect,drawText,render,loadImageFiles,loadEntry,drawStorybookPage,storybookMetrics,STORYBOOK_PRESETS,setWorkspaceMode,syncTextControls,applyStoryStyleToEntries};\n';
  html = html.replace('</script>', hook + '</script>');
  log.instrumentedSha256 = hash(Buffer.from(html));
  log.instrumentation = ['Move DEFAULT_PREFS after STORYBOOK_PRESETS (original startup throws TDZ ReferenceError)', 'Resolve only pinned jsPDF/JSZip dependencies from local UMD globals', 'Expose original functions/state for repeatable synthetic fixtures; layout/draw/export algorithms unchanged'];
  log.fonts = ['msyh.ttc', 'msyhbd.ttc', 'simsun.ttc'].map(name => {
    const data = fs.readFileSync(path.join(process.env.WINDIR || 'C:/Windows', 'Fonts', name));
    return { name, bytes: data.length, sha256: hash(data), redistribution: 'NOT COPIED; installed Windows font used locally' };
  });
  log.dependencies = ['jspdf-2.5.2.umd.min.js', 'jszip-3.10.1.min.js'].map(name => {
    const data = fs.readFileSync(path.join(root, '.local/typeset/deps', name));
    return { name, bytes: data.length, sha256: hash(data) };
  });
  browser = await chromium.launch({ channel: 'msedge', headless: true, args: ['--disable-gpu', '--disable-extensions', '--no-first-run'] });
  log.browser = browser.version(); log.viewport = { width: 1400, height: 950, deviceScaleFactor: 1 };
  const context = await browser.newContext({ viewport: { width: 1400, height: 950 }, deviceScaleFactor: 1, acceptDownloads: true });
  const page = await context.newPage();
  page.on('pageerror', e => log.errors.push(e.message));
  await page.route('**/*', route => route.request().url() === 'https://typeset.invalid/'
    ? route.fulfill({ contentType: 'text/html', body: html })
    : (log.blocked.push(route.request().url()), route.abort()));
  await page.goto('https://typeset.invalid/');
  await page.waitForFunction(() => !!window.__tsReference, { timeout: 10000 });
  for (const dep of log.dependencies) await page.addScriptTag({ content: fs.readFileSync(path.join(root, '.local/typeset/deps', dep.name), 'utf8') });
  log.fontLoad = await page.evaluate(async () => {
    for (const [family, source] of [['CCBRef', 'Microsoft YaHei'], ['CCBRefSerif', 'SimSun']]) {
      const face = new FontFace(family, `local("${source}")`); await face.load(); document.fonts.add(face);
    }
    await document.fonts.ready;
    return ['CCBRef', 'CCBRefSerif'].map(family => ({ family, loaded: document.fonts.check(`32px ${family}`, '中文日本語') }));
  });
  const fixture = await page.evaluate(async () => {
    const canvas = document.createElement('canvas'); canvas.width = 600; canvas.height = 400;
    const c = canvas.getContext('2d'); const g = c.createLinearGradient(0, 0, 600, 400); g.addColorStop(0, '#29405d'); g.addColorStop(1, '#e0bb8a'); c.fillStyle = g; c.fillRect(0, 0, 600, 400);
    c.fillStyle = '#f5efd9'; c.beginPath(); c.arc(440, 280, 90, 0, Math.PI * 2); c.fill();
    const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/png'));
    await window.__tsReference.loadImageFiles([new File([blob], 'synthetic-reference.png', { type: 'image/png' })]);
    return canvas.toDataURL('image/png').split(',')[1];
  });
  const fixtureBytes = Buffer.from(fixture, 'base64'); fs.writeFileSync(path.join(out, 'synthetic-reference.png'), fixtureBytes); log.fixtureSha256 = hash(fixtureBytes);
  const cases = [
    { id: 'TS-V01', text: '「你好，世界！」（测试）……123ABC', writingMode: 'vertical', width: 240, fontSize: 32 },
    { id: 'TS-V02', text: '『今日は、晴れ。』【東京】！？ー…﹁縦︐﹂', writingMode: 'vertical', width: 240, fontSize: 32 },
    { id: 'TS-R01', text: '普通[c=#ffcc00]黄色[/][b]粗体[/][size=48]大字[/]\n[f=CCBRefSerif][i][u]宋体斜线[/][/][/][strike]删除[/][[转义', width: 500, fontSize: 32 },
    { id: 'TS-R01-vertical', text: '普通[c=#ffcc00]黄色[/][b]粗体[/][size=48]大字[/][f=CCBRefSerif][u]宋体[/][/]', writingMode: 'vertical', width: 280, fontSize: 32 },
    ...[96, 192, 384].map(width => ({ id: `TS-H01-${width}`, text: '第一行自动换行测试AB12\n第二段', width, fontSize: 24, letterSpacing: 2 }))
  ];
  for (const sample of cases) {
    const result = await page.evaluate(sample => {
      const r = window.__tsReference; r.setWorkspaceMode('image', { save: false });
      const t = { ...r.defaultText(), x: 300, y: 35, fontFamily: 'CCBRef', fill: '#ffffff', strokeWidth: 0, lineHeight: 1.2, ...sample };
      delete t.id; r.state.texts = [t]; r.state.entries[0].texts = r.state.texts; r.state.selected = { type: 'text', index: 0 }; r.syncTextControls(true); r.render();
      const m = r.textMetrics(r.ctx, t); const rect = r.textContentRect(r.ctx, t, m);
      const exportCanvas = document.createElement('canvas'); exportCanvas.width = 600; exportCanvas.height = 400;
      const c = exportCanvas.getContext('2d'); c.drawImage(r.state.image, 0, 0); r.drawText(c, t, false);
      return { input: t, metrics: m, rect, png: exportCanvas.toDataURL('image/png').split(',')[1] };
    }, sample);
    fs.writeFileSync(path.join(out, sample.id + '.png'), Buffer.from(result.png, 'base64')); delete result.png;
    fs.writeFileSync(path.join(out, sample.id + '.json'), JSON.stringify(result, null, 2));
    await page.screenshot({ path: path.join(out, sample.id + '-source-ui.png') });
    log.cases.push({ id: sample.id, status: 'SOURCE_OUTPUT_CAPTURED_WITH_DISCLOSED_BOOTSTRAP_PATCH' }); save();
  }
  const story = await page.evaluate(async () => {
    const r = window.__tsReference; r.state.texts = []; r.state.entries[0].texts = [];
    const captions = ['', '窗边的一束光。', '第一段讲述合成故事，保留中文换行与标点。'.repeat(15) + '\n\n第二段。'];
    r.state.entries = captions.map((caption, index) => ({ ...r.state.entries[0], id: `source-page-${index}`, name: `page-${index + 1}`, texts: [], masks: [], storybook: { ...r.STORYBOOK_PRESETS.traditional, fontFamily: 'CCBRef', caption } }));
    r.state.activeIndex = 0; r.loadEntry(0); r.setWorkspaceMode('storybook', { save: false });
    return [true, false].flatMap(showPageNumber => r.state.entries.map((original, index) => {
      const entry = { ...original, storybook: { ...original.storybook, showPageNumber } };
      const canvas = document.createElement('canvas'); const metrics = r.drawStorybookPage(entry, canvas, index);
      return { id: `TS-S01-${index + 1}${showPageNumber ? '' : '-no-number'}`, metrics, png: canvas.toDataURL('image/png').split(',')[1] };
    }));
  });
  for (const item of story) {
    fs.writeFileSync(path.join(out, item.id + '.png'), Buffer.from(item.png, 'base64')); delete item.png;
    fs.writeFileSync(path.join(out, item.id + '.json'), JSON.stringify(item, null, 2)); log.cases.push({ id: item.id, status: 'SOURCE_OUTPUT_CAPTURED_WITH_DISCLOSED_BOOTSTRAP_PATCH' });
  }
  await page.screenshot({ path: path.join(out, 'TS-S01-source-ui.png') });
  for (const [selector, name] of [['#exportBookPdfBtn', 'TS-P01-source.pdf'], ['#exportAllBtn', 'TS-P01-source.zip']]) {
    const downloadPromise = page.waitForEvent('download', { timeout: 20000 }); await page.locator(selector).click();
    const download = await downloadPromise; await download.saveAs(path.join(out, name));
    log.cases.push({ id: name, status: 'SOURCE_EXPORT_CAPTURED_WITH_LOCAL_PINNED_DEPENDENCIES' }); save();
  }
  const styles = await page.evaluate(() => {
    const r = window.__tsReference; const captions = r.state.entries.map(e => e.storybook.caption); const records = [];
    for (const key of ['traditional', 'clean', 'vintage']) {
      document.querySelector('#storybookPreset').value = key;
      document.querySelector('#storybookPreset').dispatchEvent(new Event('change', { bubbles: true }));
      r.applyStoryStyleToEntries(r.state.entries.slice(0, 2));
      records.push({ key, captionsRetained: r.state.entries.every((e, i) => e.storybook.caption === captions[i]), selected: r.state.entries.map(e => e.storybook.preset) });
      r.applyStoryStyleToEntries(r.state.entries);
      records.push({ key, captionsRetained: r.state.entries.every((e, i) => e.storybook.caption === captions[i]), all: r.state.entries.map(e => e.storybook.preset) });
      const fontFamily = key === 'clean' ? 'CCBRef' : 'CCBRefSerif';
      const visual = { ...r.state.entries[1], storybook: { ...r.state.entries[1].storybook, fontFamily } };
      const canvas = document.createElement('canvas'); const metrics = r.drawStorybookPage(visual, canvas, 1);
      records.push({ key, captionsRetained: true, fontOverrideForReproducibleVisual: fontFamily, metrics, png: canvas.toDataURL('image/png').split(',')[1] });
    }
    return records;
  });
  if (styles.some(s => !s.captionsRetained)) throw Error('Source style application changed captions');
  for (const item of styles) if (item.png) { fs.writeFileSync(path.join(out, `TS-S02-${item.key}.png`), Buffer.from(item.png, 'base64')); delete item.png; }
  fs.writeFileSync(path.join(out, 'TS-S02.json'), JSON.stringify(styles, null, 2));
  log.cases.push({ id: 'TS-S02', status: 'SOURCE_STYLE_APPLICATION_OBSERVED' });
  log.status = log.errors.length ? 'FAILED' : 'CAPTURED_NOT_HUMAN_REVIEWED';
})().catch(error => { log.status = 'FAILED'; log.errors.push(error.stack); process.exitCode = 1; }).finally(async () => {
  if (browser) await browser.close();
  log.files = fs.readdirSync(out).filter(n => n !== 'manifest.json').map(name => { const bytes = fs.readFileSync(path.join(out, name)); return { name, bytes: bytes.length, sha256: hash(bytes) }; });
  save(); console.log(JSON.stringify({ out, status: log.status, cases: log.cases.length, errors: log.errors }));
});
