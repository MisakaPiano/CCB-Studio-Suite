// Independent reader verification of product-generated PDF and ZIP. No network.
const fs=require('fs'),path=require('path'),{PDFDocument}=require('pdf-lib'),JSZip=require('jszip');
(async()=>{
  const spec=JSON.parse(fs.readFileSync('modules/studio-core/build/typeset-book-export/latest.json','utf8'));
  const pdf=await PDFDocument.load(fs.readFileSync(path.join(spec.directory,'book.pdf')));
  if(pdf.getPageCount()!==spec.sizes.length)throw Error('page count');
  pdf.getPages().forEach((page,i)=>{const s=page.getSize();if(s.width!==spec.sizes[i][0]||s.height!==spec.sizes[i][1])throw Error('page geometry');});
  const zip=await JSZip.loadAsync(fs.readFileSync(path.join(spec.directory,'pages.zip')),{checkCRC32:true});
  const names=Object.keys(zip.files);if(names.join(',')!=='008.png,009.png,010.png')throw Error('page order');
  for(const n of names)if(!fs.readFileSync(path.join(spec.directory,n)).equals(await zip.file(n).async('nodebuffer')))throw Error('PNG bytes');
  const result={status:'PASS',readerPdfLib:require('pdf-lib/package.json').version,readerJsZip:require('jszip/package.json').version,pdfPages:pdf.getPageCount(),sizes:spec.sizes,zipNames:names,directory:spec.directory};
  const json=JSON.stringify(result,null,2)+'\n';fs.writeFileSync('modules/studio-core/build/typeset-book-export/independent-verification.json',json);console.log(json);
})().catch(e=>{console.error(e);process.exitCode=1});
