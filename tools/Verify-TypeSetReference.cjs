// Independent PDF/ZIP container checks; does not execute the reference renderer.
const fs = require('node:fs'), path = require('node:path'), crypto = require('node:crypto');
const { PDFDocument } = require('pdf-lib');
const JSZip = require('jszip');
(async () => {
  const root = path.resolve(process.argv[2]);
  const pdf = await PDFDocument.load(fs.readFileSync(path.join(root, 'TS-P01-source.pdf')));
  const pages = pdf.getPages().map((page, i) => {
    const metrics = JSON.parse(fs.readFileSync(path.join(root, `TS-S01-${i + 1}.json`))).metrics;
    const size = page.getSize();
    if (Math.abs(size.width - metrics.w * .75) > .01 || Math.abs(size.height - metrics.h * .75) > .01) throw Error('PDF geometry mismatch');
    return { page: i + 1, pdfPoints: size, sourcePixels: { width: metrics.w, height: metrics.h }, lines: metrics.lines.length };
  });
  if (pages.length !== 3) throw Error('PDF page count');
  const zip = await JSZip.loadAsync(fs.readFileSync(path.join(root, 'TS-P01-source.zip')), { checkCRC32: true });
  const entries = Object.values(zip.files).filter(entry => !entry.dir);
  if (entries.length !== pages.length) throw Error('ZIP page count');
  const zippedPages = [];
  for (const [i, entry] of entries.sort((a, b) => a.name.localeCompare(b.name)).entries()) {
    const bytes = await entry.async('nodebuffer');
    if (bytes.subarray(0, 8).toString('hex') !== '89504e470d0a1a0a') throw Error('ZIP non-PNG');
    if (bytes.readUInt32BE(16) !== pages[i].sourcePixels.width || bytes.readUInt32BE(20) !== pages[i].sourcePixels.height) throw Error('ZIP geometry mismatch');
    const expected = fs.readFileSync(path.join(root, `TS-S01-${i + 1}.png`));
    if (!bytes.equals(expected)) throw Error('ZIP page differs from source preview');
    zippedPages.push({ name: entry.name, bytes: bytes.length, sha256: crypto.createHash('sha256').update(bytes).digest('hex') });
  }
  const result = { status: 'PASS', scope: 'Independent PDF dimensions/page count and ZIP CRC/order/PNG bytes; not visual acceptance', parsers: ['pdf-lib', 'jszip'], pages, zippedPages };
  fs.writeFileSync(path.join(root, 'CONTAINER_VERIFICATION.json'), JSON.stringify(result, null, 2));
  console.log(JSON.stringify(result));
})().catch(error => { console.error(error); process.exitCode = 1; });
